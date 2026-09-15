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
};

export type Cart = {
  userId: number;
  items: CartItem[];
  itemCount: number;
  totalAmount: number;
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
};

export type CheckoutPayload = {
  receiverName: string;
  receiverPhone: string;
  shippingAddress: string;
};

export type AfterSaleType = "REFUND_ONLY" | "RETURN_REFUND";

export type AfterSaleStatus =
  | "PENDING"
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
  createdAt: string;
  updatedAt: string;
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

export type ProductPayload = Omit<Product, "id" | "salePrice">;
