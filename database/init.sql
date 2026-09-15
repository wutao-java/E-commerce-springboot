CREATE DATABASE IF NOT EXISTS `agent_commerce`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `agent_commerce`;

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username` VARCHAR(30) NOT NULL COMMENT '登录用户名',
    `password_hash` VARCHAR(100) NOT NULL COMMENT '登录密码哈希值',
    `display_name` VARCHAR(50) NOT NULL COMMENT '用户显示名称',
    `phone` VARCHAR(30) NULL COMMENT '联系电话',
    `address` VARCHAR(300) NULL COMMENT '联系地址',
    `role` VARCHAR(20) NOT NULL COMMENT '用户角色: CUSTOMER-普通用户, ADMIN-管理员',
    `balance` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    `created_at` DATETIME(6) NOT NULL COMMENT '创建时间',
    `business_user_id` VARCHAR(30) NULL COMMENT '课程业务用户编号',
    `member_level` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '会员等级',
    `risk_level` VARCHAR(20) NOT NULL DEFAULT 'low' COMMENT '风险等级',
    `preferred_categories` VARCHAR(300) NOT NULL DEFAULT '' COMMENT '偏好品类',
    `preferred_delivery` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '偏好配送方式',
    `budget_min` DECIMAL(12, 2) NULL COMMENT '预算下限',
    `budget_max` DECIMAL(12, 2) NULL COMMENT '预算上限',
    `invoice_required` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否需要发票',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_business_user_id` (`business_user_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `products` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品主键',
    `sku` VARCHAR(50) NOT NULL COMMENT '商品SKU编码',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `category` VARCHAR(50) NOT NULL COMMENT '商品分类',
    `description` VARCHAR(1000) NOT NULL COMMENT '商品描述',
    `price` DECIMAL(12, 2) NOT NULL COMMENT '商品原价',
    `promotion_price` DECIMAL(12, 2) NULL COMMENT '商品促销价, 为空表示无促销',
    `stock` INT NOT NULL COMMENT '商品库存数量',
    `image_url` VARCHAR(500) NOT NULL COMMENT '商品图片地址',
    `active` BIT(1) NOT NULL COMMENT '是否上架: 1-是, 0-否',
    `highlights` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '商品亮点',
    `supports_seven_day_return` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否支持七天无理由退货',
    `after_sale_note` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '售后说明',
    `scenario_tags` VARCHAR(300) NOT NULL DEFAULT '' COMMENT '适用场景标签',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_products_sku` (`sku`),
    KEY `idx_products_active_category` (`active`, `category`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `cart_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车项主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL COMMENT '商品数量',
    `selected` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否选中结算',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_user_product` (`user_id`, `product_id`),
    KEY `idx_cart_product` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `customer_orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单主键',
    `order_no` VARCHAR(40) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
    `status` VARCHAR(30) NOT NULL COMMENT '订单状态: PENDING_PAYMENT-待支付, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, AFTER_SALE-售后中, REFUNDED-已退款, CANCELED-已取消',
    `payment_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态',
    `fulfillment_status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_SHIPMENT' COMMENT '履约状态',
    `total_amount` DECIMAL(12, 2) NOT NULL COMMENT '订单总金额',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(30) NOT NULL COMMENT '收货人联系电话',
    `shipping_address` VARCHAR(300) NOT NULL COMMENT '收货地址',
    `tracking_no` VARCHAR(80) NULL COMMENT '物流单号',
    `remark` VARCHAR(500) NULL COMMENT '订单备注',
    `paid_at` DATETIME(6) NULL COMMENT '支付时间',
    `shipped_at` DATETIME(6) NULL COMMENT '发货时间',
    `completed_at` DATETIME(6) NULL COMMENT '订单完成时间',
    `created_at` DATETIME(6) NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_orders_order_no` (`order_no`),
    KEY `idx_customer_orders_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `order_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单明细主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku` VARCHAR(50) NOT NULL COMMENT '下单时的商品SKU编码',
    `product_name` VARCHAR(100) NOT NULL COMMENT '下单时的商品名称',
    `image_url` VARCHAR(500) NOT NULL COMMENT '下单时的商品图片地址',
    `unit_price` DECIMAL(12, 2) NOT NULL COMMENT '下单时的商品单价',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `subtotal` DECIMAL(12, 2) NOT NULL COMMENT '商品小计金额',
    PRIMARY KEY (`id`),
    KEY `idx_order_items_order_id` (`order_id`),
    CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `customer_orders` (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `balance_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '余额流水主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '流水类型: PAYMENT-支付, REFUND-退款, ADJUSTMENT-余额调整',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '余额变动金额',
    `balance_after` DECIMAL(12, 2) NOT NULL COMMENT '变动后账户余额',
    `description` VARCHAR(200) NOT NULL COMMENT '流水说明',
    `created_at` DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_balance_records_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `after_sales` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后申请主键',
    `after_sale_no` VARCHAR(40) NOT NULL COMMENT '售后申请编号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(40) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
    `after_sale_type` VARCHAR(20) NOT NULL COMMENT '售后类型: REFUND_ONLY-仅退款, RETURN_REFUND-退货退款, COMPENSATION-物流补偿, CANCEL_ORDER-取消订单',
    `status` VARCHAR(20) NOT NULL COMMENT '售后状态: PENDING-待审核, NEED_MORE_INFO-待补充材料, WAITING_RETURN-待退货, WAITING_RECEIPT-待收货, APPROVED-已通过, REJECTED-已拒绝',
    `reason` VARCHAR(500) NOT NULL COMMENT '售后申请原因',
    `admin_remark` VARCHAR(500) NULL COMMENT '管理员处理备注',
    `return_carrier` VARCHAR(50) NULL COMMENT '退货物流承运商',
    `return_tracking_no` VARCHAR(80) NULL COMMENT '退货物流单号',
    `refund_amount` DECIMAL(12, 2) NOT NULL COMMENT '退款金额',
    `approved_amount` DECIMAL(12, 2) NULL COMMENT '审批通过金额',
    `created_at` DATETIME(6) NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sales_no` (`after_sale_no`),
    UNIQUE KEY `uk_after_sales_order` (`order_id`),
    KEY `idx_after_sales_user_created` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product_promotions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动主键',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `promotion_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `promotion_type` VARCHAR(40) NOT NULL COMMENT '活动类型',
    `discount_summary` VARCHAR(300) NOT NULL COMMENT '优惠摘要',
    `promotion_price` DECIMAL(12, 2) NOT NULL COMMENT '活动价',
    `required_member_level` VARCHAR(20) NULL COMMENT '会员等级门槛',
    `condition_summary` VARCHAR(300) NULL COMMENT '使用条件',
    `start_at` DATETIME(6) NULL COMMENT '开始时间',
    `end_at` DATETIME(6) NULL COMMENT '结束时间',
    `active` BIT(1) NOT NULL COMMENT '是否启用',
    PRIMARY KEY (`id`),
    KEY `idx_product_promotions_product_active` (`product_id`, `active`),
    CONSTRAINT `fk_product_promotions_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `logistics_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '物流轨迹主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `carrier` VARCHAR(50) NOT NULL COMMENT '承运商',
    `tracking_no` VARCHAR(80) NOT NULL COMMENT '物流单号',
    `status` VARCHAR(30) NOT NULL COMMENT '物流状态',
    `content` VARCHAR(300) NOT NULL COMMENT '轨迹内容',
    `occurred_at` DATETIME(6) NOT NULL COMMENT '发生时间',
    PRIMARY KEY (`id`),
    KEY `idx_logistics_events_order_time` (`order_id`, `occurred_at`),
    CONSTRAINT `fk_logistics_events_order` FOREIGN KEY (`order_id`) REFERENCES `customer_orders` (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `approval_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批记录主键',
    `after_sale_id` BIGINT NOT NULL COMMENT '售后申请ID',
    `action` VARCHAR(30) NOT NULL COMMENT '审批动作',
    `remark` VARCHAR(500) NOT NULL COMMENT '审批说明',
    `approved_amount` DECIMAL(12, 2) NULL COMMENT '审批金额',
    `created_at` DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_approval_records_after_sale_time` (`after_sale_id`, `created_at`),
    CONSTRAINT `fk_approval_records_after_sale` FOREIGN KEY (`after_sale_id`) REFERENCES `after_sales` (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `after_sale_policies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后政策主键',
    `scene_key` VARCHAR(60) NOT NULL COMMENT '业务场景标识',
    `title` VARCHAR(100) NOT NULL COMMENT '政策标题',
    `content` VARCHAR(1000) NOT NULL COMMENT '政策内容',
    `applicable_conditions` VARCHAR(500) NOT NULL COMMENT '适用条件',
    `exclusion_conditions` VARCHAR(500) NOT NULL COMMENT '排除条件',
    `required_evidence` VARCHAR(500) NOT NULL COMMENT '所需材料',
    `requires_manual_review` BIT(1) NOT NULL COMMENT '是否需要人工审核',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_policies_scene_key` (`scene_key`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `faq_entries` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '常见问题主键',
    `category` VARCHAR(50) NOT NULL COMMENT '问题分类',
    `question` VARCHAR(300) NOT NULL COMMENT '问题',
    `answer` VARCHAR(1000) NOT NULL COMMENT '答案',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS `_add_column_if_missing`$$
CREATE PROCEDURE `_add_column_if_missing`(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @column_ddl = CONCAT(
            'ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', column_definition_value
        );
        PREPARE column_statement FROM @column_ddl;
        EXECUTE column_statement;
        DEALLOCATE PREPARE column_statement;
    END IF;
END$$
DELIMITER ;

CALL `_add_column_if_missing`('users', 'business_user_id', 'VARCHAR(30) NULL COMMENT ''课程业务用户编号''');
CALL `_add_column_if_missing`('users', 'member_level', 'VARCHAR(20) NOT NULL DEFAULT ''normal'' COMMENT ''会员等级''');
CALL `_add_column_if_missing`('users', 'risk_level', 'VARCHAR(20) NOT NULL DEFAULT ''low'' COMMENT ''风险等级''');
CALL `_add_column_if_missing`('users', 'preferred_categories', 'VARCHAR(300) NOT NULL DEFAULT '''' COMMENT ''偏好品类''');
CALL `_add_column_if_missing`('users', 'preferred_delivery', 'VARCHAR(100) NOT NULL DEFAULT '''' COMMENT ''偏好配送方式''');
CALL `_add_column_if_missing`('users', 'budget_min', 'DECIMAL(12, 2) NULL COMMENT ''预算下限''');
CALL `_add_column_if_missing`('users', 'budget_max', 'DECIMAL(12, 2) NULL COMMENT ''预算上限''');
CALL `_add_column_if_missing`('users', 'invoice_required', 'BIT(1) NOT NULL DEFAULT b''0'' COMMENT ''是否需要发票''');
CALL `_add_column_if_missing`('products', 'highlights', 'VARCHAR(500) NOT NULL DEFAULT '''' COMMENT ''商品亮点''');
CALL `_add_column_if_missing`('products', 'supports_seven_day_return', 'BIT(1) NOT NULL DEFAULT b''1'' COMMENT ''是否支持七天无理由退货''');
CALL `_add_column_if_missing`('products', 'after_sale_note', 'VARCHAR(500) NOT NULL DEFAULT '''' COMMENT ''售后说明''');
CALL `_add_column_if_missing`('products', 'scenario_tags', 'VARCHAR(300) NOT NULL DEFAULT '''' COMMENT ''适用场景标签''');
CALL `_add_column_if_missing`('cart_items', 'selected', 'BIT(1) NOT NULL DEFAULT b''1'' COMMENT ''是否选中结算''');
CALL `_add_column_if_missing`('customer_orders', 'payment_status', 'VARCHAR(20) NOT NULL DEFAULT ''UNPAID'' COMMENT ''支付状态''');
CALL `_add_column_if_missing`('customer_orders', 'fulfillment_status', 'VARCHAR(30) NOT NULL DEFAULT ''PENDING_SHIPMENT'' COMMENT ''履约状态''');
CALL `_add_column_if_missing`('customer_orders', 'remark', 'VARCHAR(500) NULL COMMENT ''订单备注''');
CALL `_add_column_if_missing`('after_sales', 'approved_amount', 'DECIMAL(12, 2) NULL COMMENT ''审批通过金额''');

DROP PROCEDURE `_add_column_if_missing`;

UPDATE `customer_orders`
SET `payment_status` = CASE
    WHEN `status` = 'REFUNDED' THEN 'REFUNDED'
    WHEN `status` IN ('PENDING_PAYMENT', 'CANCELED') THEN 'UNPAID'
    ELSE 'PAID'
END,
`fulfillment_status` = CASE
    WHEN `status` = 'SHIPPED' THEN 'SHIPPED'
    WHEN `status` = 'COMPLETED' THEN 'DELIVERED'
    WHEN `status` = 'CANCELED' THEN 'CANCELED'
    ELSE 'PENDING_SHIPMENT'
END;

START TRANSACTION;

INSERT IGNORE INTO `products`
    (`sku`, `name`, `category`, `description`, `price`, `promotion_price`, `stock`, `image_url`, `active`)
VALUES
    ('DIGI-KEY-01', 'Flow 机械键盘', '数码办公', '紧凑 84 键布局，热插拔轴体与三模连接。', 699.00, 599.00, 24,
     '/products/custom-mechanical-keyboard.jpg', b'1'),
    ('DIGI-AUD-02', 'Orbit 降噪耳塞', '数码办公', '入耳式主动降噪与通透模式，适合通勤和专注工作。', 1299.00, NULL, 18,
     '/products/noise-cancelling-earbuds.jpg', b'1'),
    ('LIFE-COF-03', 'Barista 自动咖啡机', '居家生活', '研磨、萃取与奶泡一体，快速完成日常咖啡。', 1899.00, 1699.00, 31,
     '/products/automatic-espresso-machine.jpg', b'1'),
    ('TRIP-BAG-04', 'Field 户外电源', '户外出行', '多接口便携储能电源，支持露营照明与数码设备供电。', 2999.00, NULL, 15,
     '/products/portable-power-station.jpg', b'1'),
    ('PHOTO-CAM-05', 'Frame 运动相机套装', '影像设备', '轻量防抖相机与常用配件，适合旅行和户外记录。', 2599.00, 2299.00, 7,
     '/products/action-camera-bundle.jpg', b'1'),
    ('SPORT-RUN-06', 'Trail 户外智能手表', '运动装备', '运动轨迹、心率与睡眠监测，适合日常训练。', 1299.00, NULL, 22,
     '/products/outdoor-smartwatch.jpg', b'1'),
    ('HOME-LAMP-07', 'Breeze 智能空气净化器', '居家生活', '实时空气质量显示与自动净化，适合卧室和书房。', 1599.00, 1499.00, 19,
     '/products/smart-air-purifier.jpg', b'1'),
    ('LIFE-WAT-08', 'Pocket 氮化镓快充', '数码办公', '小巧多口充电器，可为手机与轻薄电脑快速供电。', 239.00, NULL, 42,
     '/products/gan-fast-charger.jpg', b'1'),
    ('DIGI-MON-09', 'Arc 曲面电竞显示器', '数码办公', '34 英寸带鱼屏与高刷新率，适合多任务办公和沉浸式游戏。', 2199.00, 1999.00, 12,
     '/products/curved-gaming-monitor.jpg', b'1'),
    ('DIGI-TAB-10', 'View 护眼平板', '数码办公', '高分辨率护眼屏幕配合手写笔，兼顾阅读、学习与轻办公。', 2599.00, NULL, 14,
     '/products/eye-care-tablet.jpg', b'1'),
    ('DIGI-PHO-11', 'Galaxy X1 智能手机', '数码办公', '轻薄机身搭配高清影像系统，满足日常沟通与移动创作。', 3999.00, 3699.00, 21,
     '/products/galaxy-x1-phone.jpg', b'1'),
    ('DIGI-SPK-12', 'Pulse 便携蓝牙音箱', '数码办公', '防泼水机身与长续航设计，适合居家播放和户外携带。', 499.00, 429.00, 36,
     '/products/portable-bluetooth-speaker.jpg', b'1'),
    ('HOME-VAC-13', 'Clean 扫拖机器人', '居家生活', '激光导航与自动回充，规划路线完成日常吸尘和湿拖。', 2299.00, 2099.00, 18,
     '/products/robot-vacuum-pro.jpg', b'1'),
    ('HOME-DRY-14', 'Aero 高速吹风机', '居家生活', '高速气流配合多档温控，快速干发并减少过热损伤。', 899.00, 799.00, 27,
     '/products/high-speed-hair-dryer.jpg', b'1'),
    ('HOME-AIR-15', 'Breeze 变频空调', '居家生活', '智能温控与节能变频运行，为卧室提供安静舒适的环境。', 3299.00, NULL, 10,
     '/products/inverter-air-conditioner.jpg', b'1'),
    ('HOME-LOCK-16', 'Guard 智能门锁', '居家生活', '支持指纹、密码与临时授权，多重验证守护入户安全。', 1799.00, 1599.00, 15,
     '/products/smart-door-lock.jpg', b'1');

COMMIT;
