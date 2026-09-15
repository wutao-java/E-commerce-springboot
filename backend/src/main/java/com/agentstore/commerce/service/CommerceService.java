package com.agentstore.commerce.service;

import com.agentstore.commerce.domain.AfterSale;
import com.agentstore.commerce.domain.AfterSaleStatus;
import com.agentstore.commerce.domain.AfterSaleType;
import com.agentstore.commerce.domain.BalanceRecord;
import com.agentstore.commerce.domain.BalanceRecordType;
import com.agentstore.commerce.domain.CartItem;
import com.agentstore.commerce.domain.CustomerOrder;
import com.agentstore.commerce.domain.OrderItem;
import com.agentstore.commerce.domain.OrderStatus;
import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.dto.ApiModels.AfterSaleResponse;
import com.agentstore.commerce.dto.ApiModels.CartItemCreateRequest;
import com.agentstore.commerce.dto.ApiModels.CartItemResponse;
import com.agentstore.commerce.dto.ApiModels.CartItemUpdateRequest;
import com.agentstore.commerce.dto.ApiModels.CartResponse;
import com.agentstore.commerce.dto.ApiModels.CreateAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.CreateOrderRequest;
import com.agentstore.commerce.dto.ApiModels.OrderItemResponse;
import com.agentstore.commerce.dto.ApiModels.OrderResponse;
import com.agentstore.commerce.dto.ApiModels.ProductResponse;
import com.agentstore.commerce.dto.ApiModels.ReturnShipmentRequest;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.AfterSaleRepository;
import com.agentstore.commerce.repository.BalanceRecordRepository;
import com.agentstore.commerce.repository.CartItemRepository;
import com.agentstore.commerce.repository.OrderRepository;
import com.agentstore.commerce.repository.ProductRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommerceService {

    private static final DateTimeFormatter NUMBER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final BalanceRecordRepository balanceRecordRepository;
    private final AfterSaleRepository afterSaleRepository;

    public CommerceService(ProductRepository productRepository, CartItemRepository cartItemRepository,
                           OrderRepository orderRepository, UserAccountRepository userAccountRepository,
                           BalanceRecordRepository balanceRecordRepository,
                           AfterSaleRepository afterSaleRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.userAccountRepository = userAccountRepository;
        this.balanceRecordRepository = balanceRecordRepository;
        this.afterSaleRepository = afterSaleRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String keyword, String category) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        return productRepository.search(normalizedKeyword, normalizedCategory).stream()
            .map(this::toProductResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = requireProduct(productId);
        if (!product.getActive()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        }
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        Map<Long, Product> products = productMap(items.stream().map(CartItem::getProductId).toList(), false);
        return toCartResponse(userId, items, products);
    }

    @Transactional
    public CartResponse addCartItem(Long userId, CartItemCreateRequest request) {
        Product product = requireActiveProduct(request.productId());
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, request.productId())
            .orElseGet(() -> new CartItem(userId, request.productId(), 0));
        int nextQuantity = item.getQuantity() + request.quantity();
        validateStock(product, nextQuantity);
        item.changeQuantity(nextQuantity);
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, CartItemUpdateRequest request) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "购物车商品不存在"));
        Product product = requireActiveProduct(item.getProductId());
        validateStock(product, request.quantity());
        item.changeQuantity(request.quantity());
        return getCart(userId);
    }

    @Transactional
    public CartResponse deleteCartItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "购物车商品不存在"));
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "购物车为空，无法创建订单");
        }

        Map<Long, Product> products = productMap(
            cartItems.stream().map(CartItem::getProductId).toList(), true);
        for (CartItem cartItem : cartItems) {
            Product product = products.get(cartItem.getProductId());
            if (product == null || !product.getActive()) {
                throw new BusinessException(HttpStatus.CONFLICT, "购物车中存在已下架商品");
            }
            validateStock(product, cartItem.getQuantity());
        }

        BigDecimal totalAmount = cartItems.stream()
            .map(item -> products.get(item.getProductId()).getSalePrice()
                .multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = new CustomerOrder(createNumber("EC", now), userId, totalAmount,
            request.receiverName().trim(), request.receiverPhone().trim(), request.shippingAddress().trim(), now);

        for (CartItem cartItem : cartItems) {
            Product product = products.get(cartItem.getProductId());
            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            order.addItem(new OrderItem(product.getId(), product.getSku(), product.getName(), product.getImageUrl(),
                unitPrice, cartItem.getQuantity(), subtotal));
            product.decreaseStock(cartItem.getQuantity());
        }

        CustomerOrder savedOrder = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toOrderResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderNo, Long userId) {
        return toOrderResponse(requireOrder(orderNo, userId));
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNo, Long userId) {
        CustomerOrder order = requireOrderForUpdate(orderNo, userId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅待支付订单可以取消");
        }
        restoreStock(order);
        order.cancel(LocalDateTime.now());
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse payOrder(String orderNo, Long userId) {
        CustomerOrder order = requireOrderForUpdate(orderNo, userId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅待支付订单可以支付");
        }
        UserAccount account = userAccountRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (account.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "余额不足，请联系管理员充值");
        }
        BigDecimal amount = order.getTotalAmount().negate();
        account.changeBalance(amount);
        balanceRecordRepository.save(new BalanceRecord(userId, BalanceRecordType.PAYMENT, amount,
            account.getBalance(), "支付订单 " + order.getOrderNo(), LocalDateTime.now()));
        order.pay(LocalDateTime.now());
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse confirmOrder(String orderNo, Long userId) {
        CustomerOrder order = requireOrderForUpdate(orderNo, userId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅已发货订单可以确认收货");
        }
        order.complete(LocalDateTime.now());
        return toOrderResponse(order);
    }

    @Transactional
    public AfterSaleResponse createAfterSale(Long userId, CreateAfterSaleRequest request) {
        CustomerOrder order = requireOrderForUpdate(request.orderNo(), userId);
        boolean allowed = request.type() == AfterSaleType.RETURN_REFUND
            ? order.getStatus() == OrderStatus.COMPLETED
            : order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED;
        if (!allowed) {
            String message = request.type() == AfterSaleType.RETURN_REFUND
                ? "仅已完成订单可以申请退货退款"
                : "仅已支付、已发货或已完成订单可以申请仅退款";
            throw new BusinessException(HttpStatus.CONFLICT, message);
        }
        if (afterSaleRepository.existsByOrderId(order.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "该订单已提交过售后申请");
        }
        LocalDateTime now = LocalDateTime.now();
        AfterSale afterSale = new AfterSale(createNumber("AS", now), order.getId(), order.getOrderNo(),
            userId, request.type(), request.reason().trim(), order.getTotalAmount(), now);
        order.startAfterSale(now);
        return toAfterSaleResponse(afterSaleRepository.save(afterSale));
    }

    @Transactional
    public AfterSaleResponse submitReturnShipment(Long afterSaleId, Long userId, ReturnShipmentRequest request) {
        AfterSale afterSale = afterSaleRepository.findByIdForUpdate(afterSaleId)
            .filter(item -> item.getUserId().equals(userId))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "售后申请不存在"));
        if (afterSale.getStatus() != AfterSaleStatus.WAITING_RETURN) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅待寄回的退货退款申请可以提交物流");
        }
        afterSale.submitReturn(request.carrier().trim(), request.trackingNo().trim(), LocalDateTime.now());
        return toAfterSaleResponse(afterSale);
    }

    @Transactional(readOnly = true)
    public List<AfterSaleResponse> listAfterSales(Long userId) {
        return afterSaleRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toAfterSaleResponse)
            .toList();
    }

    ProductResponse toProductResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getCategory(),
            product.getDescription(), product.getPrice(), product.getPromotionPrice(), product.getSalePrice(),
            product.getStock(), product.getImageUrl(), product.getActive());
    }

    OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderItemResponse(item.getProductId(), item.getSku(), item.getProductName(),
                item.getImageUrl(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal()))
            .toList();
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getUserId(), order.getStatus(),
            order.getTotalAmount(), order.getReceiverName(), order.getReceiverPhone(), order.getShippingAddress(),
            order.getTrackingNo(), order.getPaidAt(), order.getShippedAt(), order.getCompletedAt(),
            order.getCreatedAt(), order.getUpdatedAt(), items);
    }

    AfterSaleResponse toAfterSaleResponse(AfterSale afterSale) {
        return new AfterSaleResponse(afterSale.getId(), afterSale.getAfterSaleNo(), afterSale.getOrderNo(),
            afterSale.getUserId(), afterSale.getType(), afterSale.getStatus(), afterSale.getReason(),
            afterSale.getAdminRemark(), afterSale.getReturnCarrier(), afterSale.getReturnTrackingNo(),
            afterSale.getRefundAmount(), afterSale.getCreatedAt(), afterSale.getUpdatedAt());
    }

    private CartResponse toCartResponse(Long userId, List<CartItem> items, Map<Long, Product> products) {
        List<CartItemResponse> responses = items.stream().map(item -> {
            Product product = products.get(item.getProductId());
            if (product == null) {
                throw new BusinessException(HttpStatus.CONFLICT, "购物车关联的商品不存在");
            }
            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemResponse(item.getId(), product.getId(), product.getSku(), product.getName(),
                product.getImageUrl(), unitPrice, item.getQuantity(), subtotal, product.getStock());
        }).toList();
        int itemCount = responses.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal totalAmount = responses.stream()
            .map(CartItemResponse::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(userId, responses, itemCount, totalAmount);
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
    }

    private Product requireActiveProduct(Long productId) {
        Product product = requireProduct(productId);
        if (!product.getActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品已下架");
        }
        return product;
    }

    private CustomerOrder requireOrder(String orderNo, Long userId) {
        return orderRepository.findByOrderNoAndUserId(orderNo, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    private CustomerOrder requireOrderForUpdate(String orderNo, Long userId) {
        return orderRepository.findByOrderNoAndUserIdForUpdate(orderNo, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    private void validateStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                "商品“" + product.getName() + "”库存不足，当前仅剩 " + product.getStock() + " 件");
        }
    }

    private void restoreStock(CustomerOrder order) {
        Map<Long, Product> products = productMap(
            order.getItems().stream().map(OrderItem::getProductId).toList(), true);
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProductId());
            if (product != null) {
                product.increaseStock(item.getQuantity());
            }
        }
    }

    private Map<Long, Product> productMap(Collection<Long> productIds, boolean lock) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<Product> products = lock
            ? productRepository.findAllByIdForUpdate(productIds)
            : productRepository.findAllById(productIds);
        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String createNumber(String prefix, LocalDateTime now) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + NUMBER_TIME_FORMAT.format(now) + suffix;
    }
}
