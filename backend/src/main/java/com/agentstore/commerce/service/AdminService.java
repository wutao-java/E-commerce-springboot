package com.agentstore.commerce.service;

import com.agentstore.commerce.domain.AfterSale;
import com.agentstore.commerce.domain.AfterSaleStatus;
import com.agentstore.commerce.domain.AfterSaleType;
import com.agentstore.commerce.domain.ApprovalRecord;
import com.agentstore.commerce.domain.BalanceRecord;
import com.agentstore.commerce.domain.BalanceRecordType;
import com.agentstore.commerce.domain.CustomerOrder;
import com.agentstore.commerce.domain.LogisticsEvent;
import com.agentstore.commerce.domain.OrderItem;
import com.agentstore.commerce.domain.OrderStatus;
import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.ProductPromotion;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.domain.UserRole;
import com.agentstore.commerce.dto.ApiModels.AfterSaleResponse;
import com.agentstore.commerce.dto.ApiModels.BalanceAdjustRequest;
import com.agentstore.commerce.dto.ApiModels.ConfirmReceiptRequest;
import com.agentstore.commerce.dto.ApiModels.LogisticsEventRequest;
import com.agentstore.commerce.dto.ApiModels.OrderResponse;
import com.agentstore.commerce.dto.ApiModels.ProductResponse;
import com.agentstore.commerce.dto.ApiModels.ProductSaveRequest;
import com.agentstore.commerce.dto.ApiModels.PromotionResponse;
import com.agentstore.commerce.dto.ApiModels.PromotionSaveRequest;
import com.agentstore.commerce.dto.ApiModels.RegisterRequest;
import com.agentstore.commerce.dto.ApiModels.ReviewAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.ShipOrderRequest;
import com.agentstore.commerce.dto.ApiModels.UserResponse;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.AfterSaleRepository;
import com.agentstore.commerce.repository.ApprovalRecordRepository;
import com.agentstore.commerce.repository.BalanceRecordRepository;
import com.agentstore.commerce.repository.LogisticsEventRepository;
import com.agentstore.commerce.repository.OrderRepository;
import com.agentstore.commerce.repository.ProductPromotionRepository;
import com.agentstore.commerce.repository.ProductRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminService {

    private final ProductRepository productRepository;
    private final ProductPromotionRepository promotionRepository;
    private final OrderRepository orderRepository;
    private final LogisticsEventRepository logisticsEventRepository;
    private final AfterSaleRepository afterSaleRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final BalanceRecordRepository balanceRecordRepository;
    private final CommerceService commerceService;
    private final AuthService authService;

    public AdminService(ProductRepository productRepository, ProductPromotionRepository promotionRepository,
                        OrderRepository orderRepository, LogisticsEventRepository logisticsEventRepository,
                        AfterSaleRepository afterSaleRepository, ApprovalRecordRepository approvalRecordRepository,
                        UserAccountRepository userAccountRepository,
                        BalanceRecordRepository balanceRecordRepository, CommerceService commerceService,
                        AuthService authService) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.orderRepository = orderRepository;
        this.logisticsEventRepository = logisticsEventRepository;
        this.afterSaleRepository = afterSaleRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.userAccountRepository = userAccountRepository;
        this.balanceRecordRepository = balanceRecordRepository;
        this.commerceService = commerceService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
            .map(commerceService::toProductResponse)
            .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductSaveRequest request) {
        validatePromotion(request);
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品 SKU 已存在");
        }
        Product product = new Product(sku, request.name().trim(), request.category().trim(),
            request.description().trim(), request.price(), request.promotionPrice(), request.stock(),
            request.imageUrl().trim(), request.active(), trimToEmpty(request.highlights()),
            request.supportsSevenDayReturn(), trimToEmpty(request.afterSaleNote()),
            trimToEmpty(request.scenarioTags()));
        return commerceService.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductSaveRequest request) {
        validatePromotion(request);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
        String sku = request.sku().trim();
        if (productRepository.existsBySkuAndIdNot(sku, productId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品 SKU 已存在");
        }
        product.update(sku, request.name().trim(), request.category().trim(), request.description().trim(),
            request.price(), request.promotionPrice(), request.stock(), request.imageUrl().trim(), request.active(),
            trimToEmpty(request.highlights()), request.supportsSevenDayReturn(),
            trimToEmpty(request.afterSaleNote()), trimToEmpty(request.scenarioTags()));
        return commerceService.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listPromotions() {
        return promotionRepository.findAllByOrderByIdDesc().stream()
            .map(commerceService::toPromotionResponse)
            .toList();
    }

    @Transactional
    public PromotionResponse createPromotion(PromotionSaveRequest request) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
        validatePromotionPrice(request.promotionPrice(), product.getPrice(), request.startAt(), request.endAt());
        ProductPromotion promotion = promotionRepository.save(new ProductPromotion(product.getId(),
            request.promotionName().trim(), request.promotionType().trim(), request.discountSummary().trim(),
            request.promotionPrice(), trimToNull(request.requiredMemberLevel()),
            trimToEmpty(request.conditionSummary()), request.startAt(), request.endAt(), request.active()));
        return commerceService.toPromotionResponse(promotion);
    }

    @Transactional
    public PromotionResponse updatePromotion(Long promotionId, PromotionSaveRequest request) {
        ProductPromotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "促销活动不存在"));
        if (!promotion.getProductId().equals(request.productId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "活动创建后不能更换商品");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "商品不存在"));
        validatePromotionPrice(request.promotionPrice(), product.getPrice(), request.startAt(), request.endAt());
        promotion.update(request.promotionName().trim(), request.promotionType().trim(),
            request.discountSummary().trim(), request.promotionPrice(), trimToNull(request.requiredMemberLevel()),
            trimToEmpty(request.conditionSummary()), request.startAt(), request.endAt(), request.active());
        return commerceService.toPromotionResponse(promotion);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(String keyword, OrderStatus status) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(order -> status == null || order.getStatus() == status)
            .filter(order -> normalized == null
                || order.getOrderNo().toLowerCase().contains(normalized)
                || order.getReceiverName().toLowerCase().contains(normalized)
                || order.getReceiverPhone().toLowerCase().contains(normalized))
            .map(commerceService::toOrderResponse)
            .toList();
    }

    @Transactional
    public OrderResponse shipOrder(String orderNo, ShipOrderRequest request) {
        CustomerOrder order = orderRepository.findByOrderNoForUpdate(orderNo)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅已支付订单可以发货");
        }
        LocalDateTime now = LocalDateTime.now();
        String carrier = StringUtils.hasText(request.carrier()) ? request.carrier().trim() : "顺丰速运";
        order.ship(request.trackingNo().trim(), now);
        orderRepository.flush();
        logisticsEventRepository.save(new LogisticsEvent(order.getId(), carrier, request.trackingNo().trim(),
            "SHIPPED", "订单已发货，等待承运商揽收", now));
        return commerceService.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse addLogisticsEvent(String orderNo, LogisticsEventRequest request) {
        CustomerOrder order = orderRepository.findByOrderNoForUpdate(orderNo)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "订单不存在"));
        logisticsEventRepository.save(new LogisticsEvent(order.getId(), request.carrier().trim(),
            request.trackingNo().trim(), request.status().trim(), request.content().trim(), LocalDateTime.now()));
        return commerceService.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<AfterSaleResponse> listAfterSales() {
        return afterSaleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .map(commerceService::toAfterSaleResponse)
            .toList();
    }

    @Transactional
    public AfterSaleResponse reviewAfterSale(Long afterSaleId, ReviewAfterSaleRequest request) {
        AfterSale afterSale = requirePendingAfterSale(afterSaleId);
        CustomerOrder order = requireAfterSaleOrder(afterSale);
        BigDecimal approvedAmount = normalizeApprovedAmount(afterSale, request.approvedAmount());
        LocalDateTime now = LocalDateTime.now();
        String action;
        if (request.approved()) {
            if (afterSale.getType() == AfterSaleType.RETURN_REFUND) {
                afterSale.approveReturn(request.remark().trim(), approvedAmount, now);
                action = "APPROVE_RETURN";
            } else {
                boolean restoreStock = afterSale.getType() == AfterSaleType.CANCEL_ORDER;
                completeRefund(afterSale, order, request.remark().trim(), restoreStock, approvedAmount, now);
                action = "APPROVE_REFUND";
            }
        } else {
            order.rejectAfterSale(now);
            afterSale.reject(request.remark().trim(), now);
            action = "REJECT";
        }
        approvalRecordRepository.save(new ApprovalRecord(afterSaleId, action, request.remark().trim(),
            request.approved() ? approvedAmount : null, now));
        return commerceService.toAfterSaleResponse(afterSale);
    }

    @Transactional
    public AfterSaleResponse requestAfterSaleInfo(Long afterSaleId, String remark) {
        AfterSale afterSale = requirePendingAfterSale(afterSaleId);
        LocalDateTime now = LocalDateTime.now();
        afterSale.requestMoreInfo(remark.trim(), now);
        approvalRecordRepository.save(new ApprovalRecord(afterSaleId, "NEED_MORE_INFO", remark.trim(), null, now));
        return commerceService.toAfterSaleResponse(afterSale);
    }

    @Transactional
    public AfterSaleResponse confirmAfterSaleReceipt(Long afterSaleId, ConfirmReceiptRequest request) {
        AfterSale afterSale = afterSaleRepository.findByIdForUpdate(afterSaleId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "售后申请不存在"));
        if (afterSale.getStatus() != AfterSaleStatus.WAITING_RECEIPT) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅待收货的退货退款申请可以确认收货");
        }
        CustomerOrder order = requireAfterSaleOrder(afterSale);
        LocalDateTime now = LocalDateTime.now();
        completeRefund(afterSale, order, request.remark().trim(), true,
            afterSale.getEffectiveRefundAmount(), now);
        approvalRecordRepository.save(new ApprovalRecord(afterSaleId, "CONFIRM_RETURN",
            request.remark().trim(), afterSale.getEffectiveRefundAmount(), now));
        return commerceService.toAfterSaleResponse(afterSale);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String keyword) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return userAccountRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.CUSTOMER).stream()
            .filter(user -> normalized == null
                || user.getUsername().toLowerCase().contains(normalized)
                || user.getDisplayName().toLowerCase().contains(normalized)
                || user.getBusinessUserId().toLowerCase().contains(normalized)
                || (user.getPhone() != null && user.getPhone().contains(normalized)))
            .map(authService::toUserResponse)
            .toList();
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        return authService.register(request);
    }

    @Transactional
    public UserResponse adjustBalance(Long userId, BalanceAdjustRequest request) {
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "调整金额不能为 0");
        }
        UserAccount account = userAccountRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (account.getBalance().add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "调整后余额不能小于 0");
        }
        account.changeBalance(amount);
        balanceRecordRepository.save(new BalanceRecord(userId, BalanceRecordType.ADJUSTMENT, amount,
            account.getBalance(), request.description().trim(), LocalDateTime.now()));
        return authService.toUserResponse(account);
    }

    private AfterSale requirePendingAfterSale(Long afterSaleId) {
        AfterSale afterSale = afterSaleRepository.findByIdForUpdate(afterSaleId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "售后申请不存在"));
        if (afterSale.getStatus() != AfterSaleStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "该售后申请当前不可审批");
        }
        return afterSale;
    }

    private CustomerOrder requireAfterSaleOrder(AfterSale afterSale) {
        CustomerOrder order = orderRepository.findByOrderNoForUpdate(afterSale.getOrderNo())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "关联订单不存在"));
        if (order.getStatus() != OrderStatus.AFTER_SALE) {
            throw new BusinessException(HttpStatus.CONFLICT, "关联订单状态不允许处理售后");
        }
        return order;
    }

    private BigDecimal normalizeApprovedAmount(AfterSale afterSale, BigDecimal approvedAmount) {
        BigDecimal amount = approvedAmount == null ? afterSale.getRefundAmount() : approvedAmount;
        if (amount.compareTo(afterSale.getRefundAmount()) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "审批金额不能超过订单可退金额");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePromotion(ProductSaveRequest request) {
        if (request.promotionPrice() != null && request.promotionPrice().compareTo(request.price()) >= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "促销价必须低于原价");
        }
    }

    private void validatePromotionPrice(BigDecimal promotionPrice, BigDecimal productPrice,
                                        LocalDateTime startAt, LocalDateTime endAt) {
        if (promotionPrice.compareTo(productPrice) >= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "活动价必须低于商品原价");
        }
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "活动结束时间必须晚于开始时间");
        }
    }

    private void completeRefund(AfterSale afterSale, CustomerOrder order, String remark,
                                boolean restoreReturnedStock, BigDecimal amount, LocalDateTime now) {
        UserAccount account = userAccountRepository.findByIdForUpdate(afterSale.getUserId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        account.changeBalance(amount);
        balanceRecordRepository.save(new BalanceRecord(account.getId(), BalanceRecordType.REFUND,
            amount, account.getBalance(), "售后退款 " + order.getOrderNo(), now));
        if (restoreReturnedStock) {
            restoreStock(order);
        }
        order.refund(now);
        afterSale.completeRefund(remark, amount, now);
    }

    private void restoreStock(CustomerOrder order) {
        List<Long> productIds = order.getItems().stream().map(OrderItem::getProductId).toList();
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProductId());
            if (product != null) {
                product.increaseStock(item.getQuantity());
            }
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
