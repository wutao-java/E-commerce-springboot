package com.agentstore.commerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentstore.commerce.domain.AfterSaleStatus;
import com.agentstore.commerce.domain.AfterSaleType;
import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.ProductPromotion;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.domain.UserRole;
import com.agentstore.commerce.dto.ApiModels.CartItemCreateRequest;
import com.agentstore.commerce.dto.ApiModels.CartItemUpdateRequest;
import com.agentstore.commerce.dto.ApiModels.CommerceProfileRequest;
import com.agentstore.commerce.dto.ApiModels.ConfirmReceiptRequest;
import com.agentstore.commerce.dto.ApiModels.CreateAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.CreateOrderRequest;
import com.agentstore.commerce.dto.ApiModels.ReviewAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.ShipOrderRequest;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CourseParityServiceTest {

    @Autowired
    private CommerceService commerceService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPromotionRepository promotionRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LogisticsEventRepository logisticsEventRepository;

    @Autowired
    private AfterSaleRepository afterSaleRepository;

    @Autowired
    private ApprovalRecordRepository approvalRecordRepository;

    @Autowired
    private BalanceRecordRepository balanceRecordRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private Product product;
    private Product secondProduct;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        approvalRecordRepository.deleteAll();
        afterSaleRepository.deleteAll();
        logisticsEventRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        balanceRecordRepository.deleteAll();
        promotionRepository.deleteAll();
        userAccountRepository.deleteAll();
        productRepository.deleteAll();

        user = new UserAccount("buyer", "encoded", "张三", "13800138000",
            "上海市浦东新区 Agent 路 1 号", UserRole.CUSTOMER,
            new BigDecimal("2000.00"), LocalDateTime.now());
        user.updateCommerceProfile("U1001", "gold", "low", "数码办公", "顺丰速运",
            new BigDecimal("100.00"), new BigDecimal("1000.00"), true);
        user = userAccountRepository.save(user);
        product = productRepository.save(new Product(
            "TEST-001", "Agent 调试键盘", "数码办公", "用于接口联调的测试商品",
            new BigDecimal("299.00"), 5, "https://example.com/keyboard.jpg", true));
        secondProduct = productRepository.save(new Product(
            "TEST-002", "Agent 调试耳机", "数码办公", "用于接口联调的第二件商品",
            new BigDecimal("399.00"), 5, "https://example.com/earbuds.jpg", true));
    }

    @Test
    void shouldApplyActivePromotionWhenMemberLevelMatches() {
        promotionRepository.save(new ProductPromotion(product.getId(), "金卡会员专享", "member_discount",
            "金卡会员立减 100 元", new BigDecimal("199.00"), "gold", "仅限金卡会员",
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), true));

        var response = commerceService.getProduct(product.getId(), user.getId());

        assertThat(response.salePrice()).isEqualByComparingTo("199.00");
        assertThat(response.promotionApplied()).isTrue();
        assertThat(response.promotion().promotionName()).isEqualTo("金卡会员专享");
    }

    @Test
    void shouldPreserveMemberLevelWhenCustomerUpdatesCommerceProfile() {
        var updated = userService.updateCommerceProfile(user.getId(),
            new CommerceProfileRequest(
                "数码办公,居家生活",
                "顺丰速运",
                new BigDecimal("300.00"),
                new BigDecimal("3000.00"),
                true));

        assertThat(updated.memberLevel()).isEqualTo("gold");
    }

    @Test
    void shouldCheckoutOnlySelectedCartItemsAndKeepOthers() {
        var firstCart = commerceService.addCartItem(user.getId(),
            new CartItemCreateRequest(product.getId(), 1, true));
        var secondCart = commerceService.addCartItem(user.getId(),
            new CartItemCreateRequest(secondProduct.getId(), 1, false));
        Long selectedItemId = firstCart.items().get(0).id();
        Long unselectedItemId = secondCart.items().stream()
            .filter(item -> item.productId().equals(secondProduct.getId()))
            .findFirst().orElseThrow().id();

        var noneSelected = commerceService.updateCartItem(user.getId(), selectedItemId,
            new CartItemUpdateRequest(null, false));
        assertThat(noneSelected.selectedItemCount()).isZero();
        commerceService.updateCartItem(user.getId(), selectedItemId,
            new CartItemUpdateRequest(null, true));

        var order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号",
            "CART", List.of(selectedItemId), null, null, "课程部分结算"));

        assertThat(order.items()).singleElement()
            .satisfies(item -> assertThat(item.productId()).isEqualTo(product.getId()));
        assertThat(cartItemRepository.findById(unselectedItemId)).isPresent();
        assertThat(cartItemRepository.findById(selectedItemId)).isEmpty();
    }

    @Test
    void shouldCreateDirectBuyOrderWithoutChangingCart() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(secondProduct.getId(), 1, true));

        var order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号",
            "DIRECT_BUY", null, product.getId(), 2, "立即购买"));

        assertThat(order.items()).singleElement()
            .satisfies(item -> {
                assertThat(item.productId()).isEqualTo(product.getId());
                assertThat(item.quantity()).isEqualTo(2);
            });
        assertThat(cartItemRepository.findByUserIdOrderByIdAsc(user.getId())).hasSize(1);
    }

    @Test
    void shouldExposeLogisticsEventsAfterShipment() {
        var order = createPaidOrder();

        var shipped = adminService.shipOrder(order.orderNo(),
            new ShipOrderRequest("顺丰速运", "SF1234567890"));

        assertThat(shipped.fulfillmentStatus()).isEqualTo("SHIPPED");
        assertThat(shipped.logisticsEvents()).singleElement()
            .satisfies(event -> {
                assertThat(event.carrier()).isEqualTo("顺丰速运");
                assertThat(event.content()).contains("已发货");
            });
    }

    @Test
    void shouldRecordNeedMoreInfoAndApprovalHistory() {
        var order = createPaidOrder();
        adminService.shipOrder(order.orderNo(), new ShipOrderRequest("顺丰速运", "SF1234567890"));
        commerceService.confirmOrder(order.orderNo(), user.getId());
        var afterSale = commerceService.createAfterSale(user.getId(), new CreateAfterSaleRequest(
            order.orderNo(), AfterSaleType.RETURN_REFUND, "商品存在质量问题"));

        var waiting = adminService.requestAfterSaleInfo(afterSale.id(), "请补充商品照片");
        assertThat(waiting.status()).isEqualTo(AfterSaleStatus.NEED_MORE_INFO);

        var supplemented = commerceService.supplementAfterSale(
            afterSale.id(), user.getId(), "已补充商品照片");
        assertThat(supplemented.status()).isEqualTo(AfterSaleStatus.PENDING);

        var reviewed = adminService.reviewAfterSale(afterSale.id(),
            new ReviewAfterSaleRequest(true, "材料完整，同意退货", order.totalAmount()));
        assertThat(reviewed.status()).isEqualTo(AfterSaleStatus.WAITING_RETURN);
        assertThat(reviewed.approvalRecords())
            .extracting(record -> record.action())
            .containsExactly("NEED_MORE_INFO", "SUPPLEMENT", "APPROVE_RETURN");
    }

    private com.agentstore.commerce.dto.ApiModels.OrderResponse createPaidOrder() {
        commerceService.addCartItem(user.getId(), new CartItemCreateRequest(product.getId(), 1, true));
        var order = commerceService.createOrder(user.getId(), new CreateOrderRequest(
            "张三", "13800138000", "上海市浦东新区 Agent 路 1 号"));
        return commerceService.payOrder(order.orderNo(), user.getId());
    }
}
