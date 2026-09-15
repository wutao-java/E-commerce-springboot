package com.agentstore.commerce.service;

import com.agentstore.commerce.domain.AfterSale;
import com.agentstore.commerce.domain.AfterSaleStatus;
import com.agentstore.commerce.domain.AfterSaleType;
import com.agentstore.commerce.domain.ApprovalRecord;
import com.agentstore.commerce.domain.BalanceRecord;
import com.agentstore.commerce.domain.BalanceRecordType;
import com.agentstore.commerce.domain.CartItem;
import com.agentstore.commerce.domain.CustomerOrder;
import com.agentstore.commerce.domain.LogisticsEvent;
import com.agentstore.commerce.domain.OrderItem;
import com.agentstore.commerce.domain.OrderStatus;
import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.ProductPromotion;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.dto.ApiModels.AfterSaleResponse;
import com.agentstore.commerce.dto.ApiModels.ApprovalRecordResponse;
import com.agentstore.commerce.dto.ApiModels.CartItemCreateRequest;
import com.agentstore.commerce.dto.ApiModels.CartItemResponse;
import com.agentstore.commerce.dto.ApiModels.CartItemUpdateRequest;
import com.agentstore.commerce.dto.ApiModels.CartResponse;
import com.agentstore.commerce.dto.ApiModels.CreateAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.CreateOrderRequest;
import com.agentstore.commerce.dto.ApiModels.LogisticsEventResponse;
import com.agentstore.commerce.dto.ApiModels.OrderItemResponse;
import com.agentstore.commerce.dto.ApiModels.OrderResponse;
import com.agentstore.commerce.dto.ApiModels.ProductResponse;
import com.agentstore.commerce.dto.ApiModels.PromotionResponse;
import com.agentstore.commerce.dto.ApiModels.ReturnShipmentRequest;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.AfterSaleRepository;
import com.agentstore.commerce.repository.ApprovalRecordRepository;
import com.agentstore.commerce.repository.BalanceRecordRepository;
import com.agentstore.commerce.repository.CartItemRepository;
import com.agentstore.commerce.repository.LogisticsEventRepository;
import com.agentstore.commerce.repository.OrderRepository;
import com.agentstore.commerce.repository.ProductPromotionRepository;
import com.agentstore.commerce.repository.ProductRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
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
    private final ProductPromotionRepository promotionRepository;
    private final LogisticsEventRepository logisticsEventRepository;
    private final ApprovalRecordRepository approvalRecordRepository;

    public CommerceService(ProductRepository productRepository, CartItemRepository cartItemRepository,
                           OrderRepository orderRepository, UserAccountRepository userAccountRepository,
                           BalanceRecordRepository balanceRecordRepository,
                           AfterSaleRepository afterSaleRepository,
                           ProductPromotionRepository promotionRepository,
                           LogisticsEventRepository logisticsEventRepository,
                           ApprovalRecordRepository approvalRecordRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.userAccountRepository = userAccountRepository;
        this.balanceRecordRepository = balanceRecordRepository;
        this.afterSaleRepository = afterSaleRepository;
        this.promotionRepository = promotionRepository;
        this.logisticsEventRepository = logisticsEventRepository;
        this.approvalRecordRepository = approvalRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String keyword, String category) {
        return listProducts(keyword, category, null);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String keyword, String category, Long userId) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        List<Product> products = productRepository.search(normalizedKeyword, normalizedCategory);
        Map<Long, ProductPromotion> promotions = activePromotions(products);
        UserAccount user = findUser(userId);
        return products.stream()
            .map(product -> toProductResponse(product, promotions.get(product.getId()), user))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        return getProduct(productId, null);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId, Long userId) {
        Product product = requireProduct(productId);
        if (!product.getActive()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        }
        return toProductResponse(product, activePromotion(productId), findUser(userId));
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        Map<Long, Product> products = productMap(items.stream().map(CartItem::getProductId).toList(), false);
        return toCartResponse(userId, items, products, requireUser(userId));
    }

    @Transactional
    public CartResponse addCartItem(Long userId, CartItemCreateRequest request) {
        Product product = requireActiveProduct(request.productId());
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, request.productId())
            .orElseGet(() -> new CartItem(userId, request.productId(), 0, request.selected()));
        int nextQuantity = item.getQuantity() + request.quantity();
        validateStock(product, nextQuantity);
        item.update(nextQuantity, request.selected());
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, CartItemUpdateRequest request) {
        if (request.quantity() == null && request.selected() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "数量和选中状态至少填写一项");
        }
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "购物车商品不存在"));
        if (request.quantity() != null) {
            Product product = requireActiveProduct(item.getProductId());
            validateStock(product, request.quantity());
        }
        item.update(request.quantity(), request.selected());
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
        UserAccount user = requireUser(userId);
        List<OrderDraft> drafts = buildOrderDrafts(userId, request);
        if (drafts.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要结算的购物车商品");
        }
        Map<Long, Product> products = productMap(
            drafts.stream().map(OrderDraft::productId).toList(), true);
        Map<Long, ProductPromotion> promotions = activePromotions(products.values().stream().toList());
        for (OrderDraft draft : drafts) {
            Product product = products.get(draft.productId());
            if (product == null || !product.getActive()) {
                throw new BusinessException(HttpStatus.CONFLICT, "结算商品已下架");
            }
            validateStock(product, draft.quantity());
        }

        BigDecimal totalAmount = drafts.stream()
            .map(draft -> effectivePrice(products.get(draft.productId()), promotions.get(draft.productId()), user)
                .multiply(BigDecimal.valueOf(draft.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = new CustomerOrder(createNumber("EC", now), userId, totalAmount,
            request.receiverName().trim(), request.receiverPhone().trim(), request.shippingAddress().trim(),
            trimToEmpty(request.remark()), now);

        for (OrderDraft draft : drafts) {
            Product product = products.get(draft.productId());
            BigDecimal unitPrice = effectivePrice(product, promotions.get(product.getId()), user);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(draft.quantity()));
            order.addItem(new OrderItem(product.getId(), product.getSku(), product.getName(), product.getImageUrl(),
                unitPrice, draft.quantity(), subtotal));
            product.decreaseStock(draft.quantity());
        }

        CustomerOrder savedOrder = orderRepository.save(order);
        List<Long> checkedOutCartItemIds = drafts.stream()
            .map(OrderDraft::cartItemId)
            .filter(java.util.Objects::nonNull)
            .toList();
        if (!checkedOutCartItemIds.isEmpty()) {
            cartItemRepository.deleteAllById(checkedOutCartItemIds);
        }
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
        LocalDateTime now = LocalDateTime.now();
        order.complete(now);
        logisticsEventRepository.save(new LogisticsEvent(order.getId(), "系统", order.getTrackingNo(),
            "DELIVERED", "买家已确认收货", now));
        return toOrderResponse(order);
    }

    @Transactional
    public AfterSaleResponse createAfterSale(Long userId, CreateAfterSaleRequest request) {
        CustomerOrder order = requireOrderForUpdate(request.orderNo(), userId);
        boolean allowed = switch (request.type()) {
            case RETURN_REFUND -> order.getStatus() == OrderStatus.COMPLETED;
            case CANCEL_ORDER -> order.getStatus() == OrderStatus.PAID;
            case COMPENSATION -> order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED;
            case REFUND_ONLY -> order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED;
        };
        if (!allowed) {
            String message = request.type() == AfterSaleType.RETURN_REFUND
                ? "仅已完成订单可以申请退货退款"
                : "当前订单状态不支持该售后类型";
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
    public AfterSaleResponse supplementAfterSale(Long afterSaleId, Long userId, String content) {
        AfterSale afterSale = afterSaleRepository.findByIdForUpdate(afterSaleId)
            .filter(item -> item.getUserId().equals(userId))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "售后申请不存在"));
        if (afterSale.getStatus() != AfterSaleStatus.NEED_MORE_INFO) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅待补充材料的售后申请可以提交材料");
        }
        LocalDateTime now = LocalDateTime.now();
        afterSale.supplement(content.trim(), now);
        approvalRecordRepository.save(new ApprovalRecord(afterSaleId, "SUPPLEMENT", content.trim(), null, now));
        return toAfterSaleResponse(afterSale);
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
        return toProductResponse(product, activePromotion(product.getId()), null);
    }

    private ProductResponse toProductResponse(Product product, ProductPromotion promotion, UserAccount user) {
        boolean promotionApplied = promotion != null && promotionEligible(promotion, user);
        BigDecimal salePrice = promotionApplied ? promotion.getPromotionPrice() : product.getSalePrice();
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getCategory(),
            product.getDescription(), product.getPrice(), product.getPromotionPrice(), salePrice,
            product.getStock(), product.getImageUrl(), product.getActive(), product.getHighlights(),
            product.getSupportsSevenDayReturn(), product.getAfterSaleNote(), product.getScenarioTags(),
            toPromotionResponse(promotion), promotionApplied, promotionCondition(promotion, promotionApplied, user));
    }

    PromotionResponse toPromotionResponse(ProductPromotion promotion) {
        if (promotion == null) {
            return null;
        }
        return new PromotionResponse(promotion.getId(), promotion.getProductId(), promotion.getPromotionName(),
            promotion.getPromotionType(), promotion.getDiscountSummary(), promotion.getPromotionPrice(),
            promotion.getRequiredMemberLevel(), promotion.getConditionSummary(), promotion.getStartAt(),
            promotion.getEndAt(), promotion.getActive());
    }

    OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderItemResponse(item.getProductId(), item.getSku(), item.getProductName(),
                item.getImageUrl(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal()))
            .toList();
        List<LogisticsEventResponse> logisticsEvents = logisticsEventRepository
            .findByOrderIdOrderByOccurredAtAsc(order.getId()).stream()
            .map(event -> new LogisticsEventResponse(event.getId(), event.getCarrier(), event.getTrackingNo(),
                event.getStatus(), event.getContent(), event.getOccurredAt()))
            .toList();
        List<AfterSaleType> types = availableAfterSaleTypes(order);
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getUserId(), order.getStatus(),
            order.getTotalAmount(), order.getReceiverName(), order.getReceiverPhone(), order.getShippingAddress(),
            order.getTrackingNo(), order.getPaidAt(), order.getShippedAt(), order.getCompletedAt(),
            order.getCreatedAt(), order.getUpdatedAt(), items, order.getPaymentStatus(),
            order.getFulfillmentStatus(), order.getRemark(), logisticsEvents, !types.isEmpty(), types);
    }

    AfterSaleResponse toAfterSaleResponse(AfterSale afterSale) {
        List<ApprovalRecordResponse> records = approvalRecordRepository
            .findByAfterSaleIdOrderByCreatedAtAsc(afterSale.getId()).stream()
            .map(record -> new ApprovalRecordResponse(record.getId(), record.getAction(), record.getRemark(),
                record.getApprovedAmount(), record.getCreatedAt()))
            .toList();
        return new AfterSaleResponse(afterSale.getId(), afterSale.getAfterSaleNo(), afterSale.getOrderNo(),
            afterSale.getUserId(), afterSale.getType(), afterSale.getStatus(), afterSale.getReason(),
            afterSale.getAdminRemark(), afterSale.getReturnCarrier(), afterSale.getReturnTrackingNo(),
            afterSale.getRefundAmount(), afterSale.getApprovedAmount(), afterSale.getCreatedAt(),
            afterSale.getUpdatedAt(), records);
    }

    private CartResponse toCartResponse(Long userId, List<CartItem> items, Map<Long, Product> products,
                                        UserAccount user) {
        Map<Long, ProductPromotion> promotions = activePromotions(products.values().stream().toList());
        List<CartItemResponse> responses = items.stream().map(item -> {
            Product product = products.get(item.getProductId());
            if (product == null) {
                return new CartItemResponse(item.getId(), item.getProductId(), "-", "商品不存在", "",
                    BigDecimal.ZERO, item.getQuantity(), BigDecimal.ZERO, 0, item.getSelected(), false,
                    "商品不存在", null, null);
            }
            ProductPromotion promotion = promotions.get(product.getId());
            boolean promotionApplied = promotion != null && promotionEligible(promotion, user);
            BigDecimal unitPrice = effectivePrice(product, promotion, user);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            boolean available = product.getActive() && product.getStock() >= item.getQuantity();
            String reason = !product.getActive() ? "商品已下架" :
                product.getStock() < item.getQuantity() ? "库存不足" : null;
            return new CartItemResponse(item.getId(), product.getId(), product.getSku(), product.getName(),
                product.getImageUrl(), unitPrice, item.getQuantity(), subtotal, product.getStock(),
                item.getSelected(), available, reason, promotion == null ? null : promotion.getPromotionName(),
                promotionCondition(promotion, promotionApplied, user));
        }).toList();
        int itemCount = responses.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal totalAmount = responses.stream().map(CartItemResponse::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int selectedItemCount = responses.stream()
            .filter(item -> item.selected() && item.settlementAvailable())
            .mapToInt(CartItemResponse::quantity).sum();
        BigDecimal selectedTotalAmount = responses.stream()
            .filter(item -> item.selected() && item.settlementAvailable())
            .map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(userId, responses, itemCount, totalAmount, selectedItemCount, selectedTotalAmount);
    }

    private List<OrderDraft> buildOrderDrafts(Long userId, CreateOrderRequest request) {
        if ("DIRECT_BUY".equalsIgnoreCase(request.source())) {
            if (request.productId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "立即购买必须指定商品");
            }
            return List.of(new OrderDraft(request.productId(), request.quantity() == null ? 1 : request.quantity(), null));
        }
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "购物车为空，无法创建订单");
        }
        Collection<Long> requestedIds = request.cartItemIds();
        List<CartItem> selected = cartItems.stream()
            .filter(item -> requestedIds == null || requestedIds.isEmpty()
                ? item.getSelected() : requestedIds.contains(item.getId()))
            .toList();
        if (requestedIds != null && !requestedIds.isEmpty() && selected.size() != requestedIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "结算商品不属于当前购物车");
        }
        return selected.stream()
            .map(item -> new OrderDraft(item.getProductId(), item.getQuantity(), item.getId()))
            .toList();
    }

    private List<AfterSaleType> availableAfterSaleTypes(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.PAID) {
            return List.of(AfterSaleType.REFUND_ONLY, AfterSaleType.CANCEL_ORDER);
        }
        if (order.getStatus() == OrderStatus.SHIPPED) {
            return List.of(AfterSaleType.REFUND_ONLY, AfterSaleType.COMPENSATION);
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return List.of(AfterSaleType.REFUND_ONLY, AfterSaleType.RETURN_REFUND, AfterSaleType.COMPENSATION);
        }
        return List.of();
    }

    private Map<Long, ProductPromotion> activePromotions(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByProductIdInAndActiveTrue(products.stream().map(Product::getId).toList())
            .stream()
            .filter(promotion -> promotion.isEffective(now))
            .collect(Collectors.toMap(ProductPromotion::getProductId, Function.identity(),
                (left, right) -> left.getId() > right.getId() ? left : right));
    }

    private ProductPromotion activePromotion(Long productId) {
        return activePromotions(productRepository.findAllById(List.of(productId))).get(productId);
    }

    private BigDecimal effectivePrice(Product product, ProductPromotion promotion, UserAccount user) {
        return promotion != null && promotionEligible(promotion, user)
            ? promotion.getPromotionPrice() : product.getSalePrice();
    }

    private boolean promotionEligible(ProductPromotion promotion, UserAccount user) {
        if (!StringUtils.hasText(promotion.getRequiredMemberLevel())) {
            return true;
        }
        return user != null && memberRank(user.getMemberLevel()) >= memberRank(promotion.getRequiredMemberLevel());
    }

    private String promotionCondition(ProductPromotion promotion, boolean applied, UserAccount user) {
        if (promotion == null) {
            return null;
        }
        String condition = StringUtils.hasText(promotion.getConditionSummary())
            ? promotion.getConditionSummary() : "满足活动条件后可用";
        if (applied) {
            return "已满足：" + condition;
        }
        return user == null ? condition : "未满足：" + condition + "，当前会员等级为 " + user.getMemberLevel();
    }

    private int memberRank(String memberLevel) {
        if ("gold".equalsIgnoreCase(memberLevel)) {
            return 3;
        }
        if ("silver".equalsIgnoreCase(memberLevel)) {
            return 2;
        }
        return "normal".equalsIgnoreCase(memberLevel) ? 1 : 0;
    }

    private UserAccount findUser(Long userId) {
        return userId == null ? null : userAccountRepository.findById(userId).orElse(null);
    }

    private UserAccount requireUser(Long userId) {
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
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

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String createNumber(String prefix, LocalDateTime now) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + NUMBER_TIME_FORMAT.format(now) + suffix;
    }

    private record OrderDraft(Long productId, int quantity, Long cartItemId) {
    }
}
