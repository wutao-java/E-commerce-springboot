import type {
  AfterSale,
  AfterSaleType,
  BalanceRecord,
  Cart,
  CheckoutPayload,
  Order,
  OrderStatus,
  Product,
  ProductPayload,
  User,
  UserCreatePayload,
} from "./types";

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });
  let body: ApiEnvelope<T> | null = null;
  try {
    body = (await response.json()) as ApiEnvelope<T>;
  } catch {
    throw new ApiError("服务返回了无法识别的数据", response.status);
  }
  if (!response.ok || body.code !== 0) {
    throw new ApiError(body.message || "请求失败，请稍后重试", response.status);
  }
  return body.data;
}

function query(params: Record<string, string | undefined>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value) search.set(key, value);
  });
  const result = search.toString();
  return result ? `?${result}` : "";
}

export const authApi = {
  me: () => request<User>("/api/auth/me"),
  login: (username: string, password: string) =>
    request<User>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    }),
  register: (payload: { username: string; password: string; displayName: string; phone: string }) =>
    request<User>("/api/auth/register", { method: "POST", body: JSON.stringify(payload) }),
  logout: () => request<null>("/api/auth/logout", { method: "POST" }),
};

export const commerceApi = {
  listProducts: (keyword?: string, category?: string) =>
    request<Product[]>(`/api/products${query({ keyword, category })}`),
  getProduct: (productId: number) => request<Product>(`/api/products/${productId}`),
  getCart: () => request<Cart>("/api/cart"),
  addCartItem: (productId: number) =>
    request<Cart>("/api/cart/items", {
      method: "POST",
      body: JSON.stringify({ productId, quantity: 1 }),
    }),
  updateCartItem: (itemId: number, quantity: number) =>
    request<Cart>(`/api/cart/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify({ quantity }),
    }),
  deleteCartItem: (itemId: number) =>
    request<Cart>(`/api/cart/items/${itemId}`, { method: "DELETE" }),
  createOrder: (payload: CheckoutPayload) =>
    request<Order>("/api/orders", { method: "POST", body: JSON.stringify(payload) }),
  listOrders: () => request<Order[]>("/api/orders"),
  cancelOrder: (orderNo: string) => request<Order>(`/api/orders/${orderNo}/cancel`, { method: "POST" }),
  payOrder: (orderNo: string) => request<Order>(`/api/orders/${orderNo}/pay`, { method: "POST" }),
  confirmOrder: (orderNo: string) => request<Order>(`/api/orders/${orderNo}/confirm`, { method: "POST" }),
  listAfterSales: () => request<AfterSale[]>("/api/after-sales"),
  createAfterSale: (orderNo: string, type: AfterSaleType, reason: string) =>
    request<AfterSale>("/api/after-sales", {
      method: "POST",
      body: JSON.stringify({ orderNo, type, reason }),
    }),
  submitReturnShipment: (afterSaleId: number, carrier: string, trackingNo: string) =>
    request<AfterSale>(`/api/after-sales/${afterSaleId}/return-shipment`, {
      method: "POST",
      body: JSON.stringify({ carrier, trackingNo }),
    }),
  updateProfile: (payload: Pick<User, "displayName" | "phone" | "address">) =>
    request<User>("/api/users/me", { method: "PUT", body: JSON.stringify(payload) }),
  listBalanceRecords: () => request<BalanceRecord[]>("/api/users/me/balance-records"),
};

export const adminApi = {
  listProducts: () => request<Product[]>("/api/admin/products"),
  createProduct: (payload: ProductPayload) =>
    request<Product>("/api/admin/products", { method: "POST", body: JSON.stringify(payload) }),
  updateProduct: (productId: number, payload: ProductPayload) =>
    request<Product>(`/api/admin/products/${productId}`, { method: "PUT", body: JSON.stringify(payload) }),
  listOrders: (keyword?: string, status?: OrderStatus | "") =>
    request<Order[]>(`/api/admin/orders${query({ keyword, status: status || undefined })}`),
  shipOrder: (orderNo: string, trackingNo: string) =>
    request<Order>(`/api/admin/orders/${orderNo}/ship`, {
      method: "POST",
      body: JSON.stringify({ trackingNo }),
    }),
  listAfterSales: () => request<AfterSale[]>("/api/admin/after-sales"),
  reviewAfterSale: (afterSaleId: number, approved: boolean, remark: string) =>
    request<AfterSale>(`/api/admin/after-sales/${afterSaleId}/review`, {
      method: "POST",
      body: JSON.stringify({ approved, remark }),
    }),
  confirmAfterSaleReceipt: (afterSaleId: number, remark: string) =>
    request<AfterSale>(`/api/admin/after-sales/${afterSaleId}/confirm-receipt`, {
      method: "POST",
      body: JSON.stringify({ remark }),
    }),
  listUsers: (keyword?: string) => request<User[]>(`/api/admin/users${query({ keyword })}`),
  createUser: (payload: UserCreatePayload) =>
    request<User>("/api/admin/users", { method: "POST", body: JSON.stringify(payload) }),
  adjustBalance: (userId: number, amount: number, description: string) =>
    request<User>(`/api/admin/users/${userId}/balance`, {
      method: "POST",
      body: JSON.stringify({ amount, description }),
    }),
};
