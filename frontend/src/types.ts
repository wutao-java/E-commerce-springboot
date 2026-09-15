export type UserRole = "CUSTOMER" | "ADMIN";

export type User = {
  id: number;
  username: string;
  displayName: string;
  phone: string;
  address: string;
  role: UserRole;
  balance: number;
  createdAt: string;
  businessUserId: string;
  memberLevel: string;
  riskLevel: string;
  preferredCategories: string;
  preferredDelivery: string;
  budgetMin: number | null;
  budgetMax: number | null;
  invoiceRequired: boolean;
};

export type UserCreatePayload = {
  username: string;
  password: string;
  displayName: string;
  phone: string;
};

export type Product = {
  id: number;
  sku: string;
  name: string;
  category: string;
  description: string;
  price: number;
  promotionPrice: number | null;
  salePrice: number;
  stock: number;
  imageUrl: string;
  active: boolean;
  highlights: string;
  supportsSevenDayReturn: boolean;
  afterSaleNote: string;
  scenarioTags: string;
  promotion: Promotion | null;
  promotionApplied: boolean;
  promotionCondition: string | null;
};

export type Promotion = {
  id: number;
  productId: number;
  promotionName: string;
  promotionType: string;
  discountSummary: string;
  promotionPrice: number;
  requiredMemberLevel: string | null;
  conditionSummary: string;
  startAt: string | null;
  endAt: string | null;
  active: boolean;
};

export type CartItem = {
  id: number;
  productId: number;
  sku: string;
  productName: string;
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  stock: number;
  selected: boolean;
  settlementAvailable: boolean;
  unavailableReason: string | null;
  promotionName: string | null;
  promotionCondition: string | null;
};

export type Cart = {
  userId: number;
  items: CartItem[];
  itemCount: number;
  totalAmount: number;
  selectedItemCount: number;
  selectedTotalAmount: number;
};

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PAID"
  | "SHIPPED"
  | "COMPLETED"
  | "AFTER_SALE"
  | "REFUNDED"
  | "CANCELED";

export type OrderItem = {
  productId: number;
  sku: string;
  productName: string;
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
};

export type Order = {
  id: number;
  orderNo: string;
  userId: number;
  status: OrderStatus;
  totalAmount: number;
  receiverName: string;
  receiverPhone: string;
  shippingAddress: string;
  trackingNo: string | null;
  paidAt: string | null;
  shippedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  paymentStatus: string;
  fulfillmentStatus: string;
  remark: string;
  logisticsEvents: LogisticsEvent[];
  afterSaleAvailable: boolean;
  availableAfterSaleTypes: AfterSaleType[];
};

export type LogisticsEvent = {
  id: number;
  carrier: string;
  trackingNo: string;
  status: string;
  content: string;
  occurredAt: string;
};

export type CheckoutPayload = {
  receiverName: string;
  receiverPhone: string;
  shippingAddress: string;
  source?: "CART" | "DIRECT_BUY";
  cartItemIds?: number[];
  productId?: number;
  quantity?: number;
  remark?: string;
};

export type AfterSaleType = "REFUND_ONLY" | "RETURN_REFUND" | "COMPENSATION" | "CANCEL_ORDER";

export type AfterSaleStatus =
  | "PENDING"
  | "NEED_MORE_INFO"
  | "WAITING_RETURN"
  | "WAITING_RECEIPT"
  | "APPROVED"
  | "REJECTED";

export type AfterSale = {
  id: number;
  afterSaleNo: string;
  orderNo: string;
  userId: number;
  type: AfterSaleType;
  status: AfterSaleStatus;
  reason: string;
  adminRemark: string | null;
  returnCarrier: string | null;
  returnTrackingNo: string | null;
  refundAmount: number;
  approvedAmount: number | null;
  createdAt: string;
  updatedAt: string;
  approvalRecords: ApprovalRecord[];
};

export type ApprovalRecord = {
  id: number;
  action: string;
  remark: string;
  approvedAmount: number | null;
  createdAt: string;
};

export type BalanceRecordType = "PAYMENT" | "REFUND" | "ADJUSTMENT";

export type BalanceRecord = {
  id: number;
  type: BalanceRecordType;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
};

export type ProductPayload = Omit<Product, "id" | "salePrice" | "promotion" | "promotionApplied" | "promotionCondition">;

export type PromotionPayload = Omit<Promotion, "id">;

export type AfterSalePolicy = {
  id: number;
  sceneKey: string;
  title: string;
  content: string;
  applicableConditions: string;
  exclusionConditions: string;
  requiredEvidence: string;
  requiresManualReview: boolean;
};

export type FaqEntry = {
  id: number;
  category: string;
  question: string;
  answer: string;
};

export type CustomerServiceResponse = {
  answer: string;
  sessionId: string;
  fallback: boolean;
  sessionState: Record<string, unknown>;
};
