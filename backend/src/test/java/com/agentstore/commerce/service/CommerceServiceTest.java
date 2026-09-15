package com.agentstore.commerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentstore.commerce.domain.CartItem;
import com.agentstore.commerce.domain.BalanceRecordType;
import com.agentstore.commerce.domain.AfterSaleStatus;
import com.agentstore.commerce.domain.AfterSaleType;
import com.agentstore.commerce.domain.OrderStatus;
import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.domain.UserRole;
import com.agentstore.commerce.dto.ApiModels.CartItemCreateRequest;
import com.agentstore.commerce.dto.ApiModels.ConfirmReceiptRequest;
import com.agentstore.commerce.dto.ApiModels.CreateAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.CreateOrderRequest;
import com.agentstore.commerce.dto.ApiModels.OrderResponse;
import com.agentstore.commerce.dto.ApiModels.RegisterRequest;
import com.agentstore.commerce.dto.ApiModels.ReturnShipmentRequest;
import com.agentstore.commerce.dto.ApiModels.ReviewAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.ShipOrderRequest;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.AfterSaleRepository;
import com.agentstore.commerce.repository.BalanceRecordRepository;
import com.agentstore.commerce.repository.CartItemRepository;
import com.agentstore.commerce.repository.OrderRepository;
import com.agentstore.commerce.repository.ProductRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommerceServiceTest {

    @Autowired
    private CommerceService commerceService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private BalanceRecordRepository balanceRecordRepository;

    @Autowired
    private AfterSaleRepository afterSaleRepository;

    @Autowired
    private AdminService adminService;

    private Product product;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        afterSaleRepository.deleteAll();
        orderRepository.deleteAll();
        balanceRecordRepository.deleteAll();
        userAccountRepository.deleteAll();
        productRepository.deleteAll();
        user = userAccountRepository.save(new UserAccount(
            "buyer", "encoded", "张三", "13800138000", "上海市浦东新区 Agent 路 1 号",
            UserRole.CUSTOMER, new BigDecimal("1000.00"), LocalDateTime.now()));
        product = productRepository.save(new Product(
            "TEST-001", "Agent 调试键盘", "数码办公", "用于接口联调的测试商品",
            new BigDecimal("299.00"), 5, "https://example.com/keyboard.jpg", true));
    }

    @Test
    void shouldCreateCustomerFromAdminUserManagement() {
        var created = adminService.createUser(new RegisterRequest(
            "operator", "operator123", "运营用户", "13900139000"));

        assertThat(created.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(userAccountRepository.findById(created.id()).orElseThrow().getPasswordHash())
            .isNotEqualTo("operator123");
    }

    @Test
    void shouldExcludeAdministratorsFromUserManagement() {
        userAccountRepository.save(new UserAccount(
            "admin", "encoded", "商城管理员", "13800000000", "",
            UserRole.ADMIN, BigDecimal.ZERO, LocalDateTime.now()));

        assertThat(adminService.listUsers(null))
            .extracting(response -> response.role())
            .containsOnly(UserRole.CUSTOMER);
    }

    @Test
    void shouldRejectQuantityThatExceedsStock() {
        CartItemCreateRequest request = new CartItemCreateRequest(product.getId(), 6);

        assertThatThrownBy(() -> commerceService.addCartItem(user.getId(), request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("库存");
    }

    @Test
    void shouldCreateOrderDeductStockAndClearCart() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 2));

        OrderResponse order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.totalAmount()).isEqualByComparingTo("598.00");
        assertThat(order.items()).hasSize(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
        assertThat(cartItemRepository.findByUserIdOrderByIdAsc(user.getId())).isEmpty();
    }

    @Test
    void shouldRejectOrderWhenCartIsEmpty() {
        CreateOrderRequest request = new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号");

        assertThatThrownBy(() -> commerceService.createOrder(user.getId(), request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("购物车");
    }

    @Test
    void shouldRestoreStockWhenPendingOrderIsCanceled() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 2));
        OrderResponse created = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));

        OrderResponse canceled = commerceService.cancelOrder(created.orderNo(), user.getId());

        assertThat(canceled.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void shouldPayWithBalanceAndWriteBalanceRecord() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 2));
        OrderResponse created = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));

        OrderResponse paid = commerceService.payOrder(created.orderNo(), user.getId());

        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getBalance())
            .isEqualByComparingTo("402.00");
        assertThat(balanceRecordRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
            .singleElement()
            .satisfies(record -> {
                assertThat(record.getType()).isEqualTo(BalanceRecordType.PAYMENT);
                assertThat(record.getAmount()).isEqualByComparingTo("-598.00");
            });
    }

    @Test
    void shouldKeepOrderPendingWhenBalanceIsInsufficient() {
        product = productRepository.save(new Product(
            "TEST-002", "高价测试商品", "数码办公", "用于余额不足测试",
            new BigDecimal("1200.00"), 1, "https://example.com/device.jpg", true));
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 1));
        OrderResponse created = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));

        assertThatThrownBy(() -> commerceService.payOrder(created.orderNo(), user.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("余额不足");

        assertThat(orderRepository.findByOrderNoAndUserId(created.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getBalance())
            .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldAllowRefundOnlyForPaidOrderAndRestorePaidStatusWhenRejected() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 1));
        OrderResponse order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));
        commerceService.payOrder(order.orderNo(), user.getId());

        var afterSale = commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                order.orderNo(), AfterSaleType.REFUND_ONLY, "付款后不再需要"));

        assertThat(afterSale.status()).isEqualTo(AfterSaleStatus.PENDING);
        assertThat(orderRepository.findByOrderNoAndUserId(order.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.AFTER_SALE);

        var reviewed = adminService.reviewAfterSale(
            afterSale.id(), new ReviewAfterSaleRequest(false, "商品已备货"));

        assertThat(reviewed.status()).isEqualTo(AfterSaleStatus.REJECTED);
        assertThat(orderRepository.findByOrderNoAndUserId(order.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void shouldAllowRefundOnlyForShippedOrderAndRestoreShippedStatusWhenRejected() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 1));
        OrderResponse order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));
        commerceService.payOrder(order.orderNo(), user.getId());
        adminService.shipOrder(order.orderNo(), new ShipOrderRequest("SF1234567890"));

        var afterSale = commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                order.orderNo(), AfterSaleType.REFUND_ONLY, "物流长时间未更新"));
        adminService.reviewAfterSale(afterSale.id(), new ReviewAfterSaleRequest(false, "物流运输中"));

        assertThat(orderRepository.findByOrderNoAndUserId(order.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldRejectReturnRefundBeforeOrderIsCompleted() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 1));
        OrderResponse order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));
        commerceService.payOrder(order.orderNo(), user.getId());

        assertThatThrownBy(() -> commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                order.orderNo(), AfterSaleType.RETURN_REFUND, "申请退货")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("已完成");
        assertThat(orderRepository.findByOrderNoAndUserId(order.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void shouldRefundWithoutRestoringStockForRefundOnlyApplication() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 2));
        OrderResponse created = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));
        commerceService.payOrder(created.orderNo(), user.getId());
        adminService.shipOrder(created.orderNo(), new ShipOrderRequest("SF1234567890"));
        commerceService.confirmOrder(created.orderNo(), user.getId());
        var afterSale = commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                created.orderNo(), AfterSaleType.REFUND_ONLY, "商品与描述不符"));

        var reviewed = adminService.reviewAfterSale(
            afterSale.id(), new ReviewAfterSaleRequest(true, "同意退款"));

        assertThat(reviewed.status()).isEqualTo(AfterSaleStatus.APPROVED);
        assertThat(orderRepository.findByOrderNoAndUserId(created.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.REFUNDED);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getBalance())
            .isEqualByComparingTo("1000.00");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
        assertThat(balanceRecordRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
            .extracting(record -> record.getType())
            .containsExactly(BalanceRecordType.REFUND, BalanceRecordType.PAYMENT);
    }

    @Test
    void shouldRefundAndRestoreStockAfterReturnedGoodsAreReceived() {
        OrderResponse order = createCompletedOrder();
        var afterSale = commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                order.orderNo(), AfterSaleType.RETURN_REFUND, "商品存在质量问题"));

        var reviewed = adminService.reviewAfterSale(
            afterSale.id(), new ReviewAfterSaleRequest(true, "同意退货退款"));

        assertThat(reviewed.status()).isEqualTo(AfterSaleStatus.WAITING_RETURN);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getBalance())
            .isEqualByComparingTo("402.00");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);

        var returned = commerceService.submitReturnShipment(
            afterSale.id(), user.getId(), new ReturnShipmentRequest("顺丰速运", "SF9876543210"));
        assertThat(returned.status()).isEqualTo(AfterSaleStatus.WAITING_RECEIPT);
        assertThat(returned.returnCarrier()).isEqualTo("顺丰速运");
        assertThat(returned.returnTrackingNo()).isEqualTo("SF9876543210");

        var completed = adminService.confirmAfterSaleReceipt(
            afterSale.id(), new ConfirmReceiptRequest("退货商品验收通过"));
        assertThat(completed.status()).isEqualTo(AfterSaleStatus.APPROVED);
        assertThat(orderRepository.findByOrderNoAndUserId(order.orderNo(), user.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.REFUNDED);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getBalance())
            .isEqualByComparingTo("1000.00");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void shouldRejectReceiptConfirmationBeforeBuyerShipsReturn() {
        OrderResponse order = createCompletedOrder();
        var afterSale = commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                order.orderNo(), AfterSaleType.RETURN_REFUND, "商品存在质量问题"));
        adminService.reviewAfterSale(
            afterSale.id(), new ReviewAfterSaleRequest(true, "同意退货退款"));

        assertThatThrownBy(() -> adminService.confirmAfterSaleReceipt(
            afterSale.id(), new ConfirmReceiptRequest("提前确认")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("待收货");
    }

    @Test
    void shouldRejectReturnShipmentFromAnotherUser() {
        OrderResponse order = createCompletedOrder();
        var afterSale = commerceService.createAfterSale(
            user.getId(), new CreateAfterSaleRequest(
                order.orderNo(), AfterSaleType.RETURN_REFUND, "商品存在质量问题"));
        adminService.reviewAfterSale(
            afterSale.id(), new ReviewAfterSaleRequest(true, "同意退货退款"));
        UserAccount otherUser = userAccountRepository.save(new UserAccount(
            "other", "encoded", "李四", "13900139000", "北京市海淀区 Agent 路 2 号",
            UserRole.CUSTOMER, new BigDecimal("1000.00"), LocalDateTime.now()));

        assertThatThrownBy(() -> commerceService.submitReturnShipment(
            afterSale.id(), otherUser.getId(), new ReturnShipmentRequest("顺丰速运", "SF9876543210")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("售后申请不存在");
    }

    private OrderResponse createCompletedOrder() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 2));
        OrderResponse order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));
        commerceService.payOrder(order.orderNo(), user.getId());
        adminService.shipOrder(order.orderNo(), new ShipOrderRequest("SF1234567890"));
        return commerceService.confirmOrder(order.orderNo(), user.getId());
    }
}
