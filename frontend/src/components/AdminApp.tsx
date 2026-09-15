import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  CircleDollarSign,
  ClipboardCheck,
  LayoutDashboard,
  LogOut,
  Package,
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Store,
  Truck,
  Users,
  X,
} from "lucide-react";

import { adminApi } from "../api";
import type { AfterSale, Order, OrderStatus, Product, ProductPayload, User, UserCreatePayload } from "../types";
import { formatTime, statusLabel } from "./OrderList";

type AdminAppProps = {
  user: User;
  onLogout: () => void;
  showNotice: (message: string) => void;
  showError: (error: unknown) => void;
};

type AdminView = "dashboard" | "products" | "orders" | "afterSales" | "users";

export function AdminApp({ user, onLogout, showNotice, showError }: AdminAppProps) {
  const [view, setView] = useState<AdminView>("dashboard");
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [afterSales, setAfterSales] = useState<AfterSale[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [orderStatus, setOrderStatus] = useState<OrderStatus | "">("");
  const [editingProduct, setEditingProduct] = useState<Product | null | "new">(null);
  const [shippingOrder, setShippingOrder] = useState<Order | null>(null);
  const [reviewing, setReviewing] = useState<{ item: AfterSale; approved: boolean } | null>(null);
  const [receivingAfterSale, setReceivingAfterSale] = useState<AfterSale | null>(null);
  const [adjustingUser, setAdjustingUser] = useState<User | null>(null);
  const [creatingUser, setCreatingUser] = useState(false);

  async function loadAll() {
    setLoading(true);
    try {
      const [productData, orderData, afterSaleData, userData] = await Promise.all([
        adminApi.listProducts(),
        adminApi.listOrders(),
        adminApi.listAfterSales(),
        adminApi.listUsers(),
      ]);
      setProducts(productData);
      setOrders(orderData);
      setAfterSales(afterSaleData);
      setUsers(userData);
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadAll(); }, []);

  const filteredProducts = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return products.filter((product) => !normalized || [product.name, product.sku, product.category].some((value) => value.toLowerCase().includes(normalized)));
  }, [keyword, products]);

  const filteredOrders = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return orders.filter((order) => (!orderStatus || order.status === orderStatus)
      && (!normalized || [order.orderNo, order.receiverName, order.receiverPhone].some((value) => value.toLowerCase().includes(normalized))));
  }, [keyword, orderStatus, orders]);

  const filteredUsers = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return users.filter((item) => !normalized || [item.username, item.displayName, item.phone].some((value) => value.toLowerCase().includes(normalized)));
  }, [keyword, users]);

  const salesAmount = orders.filter((order) => !["PENDING_PAYMENT", "CANCELED"].includes(order.status))
    .reduce((sum, order) => sum + order.totalAmount, 0);

  async function saveProduct(payload: ProductPayload) {
    try {
      const saved = editingProduct === "new"
        ? await adminApi.createProduct(payload)
        : await adminApi.updateProduct((editingProduct as Product).id, payload);
      setProducts((current) => editingProduct === "new"
        ? [saved, ...current]
        : current.map((product) => product.id === saved.id ? saved : product));
      setEditingProduct(null);
      showNotice(editingProduct === "new" ? "商品已创建" : "商品已更新");
    } catch (error) {
      showError(error);
    }
  }

  async function toggleProduct(product: Product) {
    try {
      const updated = await adminApi.updateProduct(product.id, toProductPayload(product, !product.active));
      setProducts((current) => current.map((item) => item.id === updated.id ? updated : item));
      showNotice(updated.active ? "商品已上架" : "商品已下架");
    } catch (error) {
      showError(error);
    }
  }

  async function shipOrder(trackingNo: string) {
    if (!shippingOrder) return;
    try {
      const updated = await adminApi.shipOrder(shippingOrder.orderNo, trackingNo);
      setOrders((current) => current.map((order) => order.id === updated.id ? updated : order));
      setShippingOrder(null);
      showNotice("订单已发货");
    } catch (error) {
      showError(error);
    }
  }

  async function reviewAfterSale(remark: string) {
    if (!reviewing) return;
    try {
      const updated = await adminApi.reviewAfterSale(reviewing.item.id, reviewing.approved, remark);
      setAfterSales((current) => current.map((item) => item.id === updated.id ? updated : item));
      setOrders(await adminApi.listOrders());
      setUsers(await adminApi.listUsers());
      setReviewing(null);
      showNotice(reviewing.approved
        ? reviewing.item.type === "REFUND_ONLY" ? "退款已完成" : "已同意退货，等待买家寄回"
        : "售后申请已拒绝");
    } catch (error) {
      showError(error);
    }
  }

  async function confirmAfterSaleReceipt(remark: string) {
    if (!receivingAfterSale) return;
    try {
      const updated = await adminApi.confirmAfterSaleReceipt(receivingAfterSale.id, remark);
      setAfterSales((current) => current.map((item) => item.id === updated.id ? updated : item));
      setOrders(await adminApi.listOrders());
      setUsers(await adminApi.listUsers());
      setReceivingAfterSale(null);
      showNotice("已确认收货并完成退款");
    } catch (error) {
      showError(error);
    }
  }

  async function adjustBalance(amount: number, description: string) {
    if (!adjustingUser) return;
    try {
      const updated = await adminApi.adjustBalance(adjustingUser.id, amount, description);
      setUsers((current) => current.map((item) => item.id === updated.id ? updated : item));
      setAdjustingUser(null);
      showNotice("用户余额已调整");
    } catch (error) {
      showError(error);
    }
  }

  async function createUser(payload: UserCreatePayload) {
    try {
      const created = await adminApi.createUser(payload);
      setUsers((current) => [created, ...current]);
      setCreatingUser(false);
      showNotice("用户已创建");
    } catch (error) {
      showError(error);
    }
  }

  return (
    <div className="app-shell admin-shell">
      <header className="topbar admin-topbar">
        <button className="brand" type="button" onClick={() => setView("dashboard")}><span className="brand-mark"><Store size={19} /></span><span>Agent Store 管理台</span></button>
        <nav aria-label="管理导航">
          <AdminNav active={view === "dashboard"} icon={<LayoutDashboard size={17} />} label="概览" onClick={() => setView("dashboard")} />
          <AdminNav active={view === "products"} icon={<Package size={17} />} label="商品" onClick={() => { setView("products"); setKeyword(""); }} />
          <AdminNav active={view === "orders"} icon={<ClipboardCheck size={17} />} label="订单" onClick={() => { setView("orders"); setKeyword(""); }} />
          <AdminNav active={view === "afterSales"} icon={<RotateCcw size={17} />} label="售后" onClick={() => { setView("afterSales"); setKeyword(""); }} />
          <AdminNav active={view === "users"} icon={<Users size={17} />} label="用户" onClick={() => { setView("users"); setKeyword(""); }} />
        </nav>
        <div className="user-chip"><span>{user.displayName}</span><strong>ADMIN</strong><button className="icon-button" type="button" onClick={onLogout} title="退出登录" aria-label="退出登录"><LogOut size={17} /></button></div>
      </header>

      <main className="admin-page">
        {view === "dashboard" && (
          <><AdminHeading kicker="今日运营工作台" title="商城概览" onRefresh={loadAll} />
            <div className="stat-grid">
              <Stat label="在售商品" value={products.filter((item) => item.active).length.toString()} note={`共 ${products.length} 件商品`} icon={<Package size={19} />} />
              <Stat label="待发货订单" value={orders.filter((item) => item.status === "PAID").length.toString()} note={`总订单 ${orders.length} 笔`} icon={<Truck size={19} />} />
              <Stat label="待处理售后" value={afterSales.filter((item) => ["PENDING", "WAITING_RECEIPT"].includes(item.status)).length.toString()} note={`售后共 ${afterSales.length} 笔`} icon={<RotateCcw size={19} />} />
              <Stat label="已支付金额" value={`¥${salesAmount.toFixed(2)}`} note={`${users.length} 个买家账户`} icon={<CircleDollarSign size={19} />} />
            </div>
            <section className="dashboard-band"><div><span className="section-kicker">优先处理</span><h2>待发货订单</h2></div>{orders.filter((order) => order.status === "PAID").length === 0 ? <div className="compact-empty">当前没有待发货订单</div> : <div className="mini-order-list">{orders.filter((order) => order.status === "PAID").slice(0, 5).map((order) => <div key={order.id}><strong>{order.orderNo}</strong><span>{order.receiverName} · ¥{order.totalAmount.toFixed(2)}</span><button className="secondary-button" onClick={() => setShippingOrder(order)}><Truck size={15} />发货</button></div>)}</div>}</section>
          </>
        )}

        {view === "products" && (
          <><AdminHeading kicker="库存、售价与上下架" title="商品管理" action={<button className="primary-button" onClick={() => setEditingProduct("new")}><Plus size={17} />新增商品</button>} /><AdminToolbar keyword={keyword} setKeyword={setKeyword} placeholder="搜索名称、SKU 或分类" />
            <DataTable loading={loading} empty={filteredProducts.length === 0}><table><thead><tr><th>商品</th><th>价格</th><th>库存</th><th>状态</th><th>操作</th></tr></thead><tbody>{filteredProducts.map((product) => <tr key={product.id}><td><div className="table-product"><img src={product.imageUrl} alt="" /><span><strong>{product.name}</strong><small>{product.sku} · {product.category}</small></span></div></td><td><strong>¥{product.salePrice.toFixed(2)}</strong>{product.promotionPrice !== null && <small className="block-muted">原价 ¥{product.price.toFixed(2)}</small>}</td><td>{product.stock}</td><td><span className={`status-badge ${product.active ? "approved" : "canceled"}`}>{product.active ? "已上架" : "已下架"}</span></td><td><div className="table-actions"><button className="icon-button" onClick={() => setEditingProduct(product)} title="编辑商品" aria-label="编辑商品"><Pencil size={16} /></button><button className="secondary-button compact-button" onClick={() => toggleProduct(product)}>{product.active ? "下架" : "上架"}</button></div></td></tr>)}</tbody></table></DataTable>
          </>
        )}

        {view === "orders" && (
          <><AdminHeading kicker="支付与物流状态" title="订单管理" /><div className="admin-toolbar"><label className="search-box"><Search size={17} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索订单、收货人或手机" /></label><select value={orderStatus} onChange={(event) => setOrderStatus(event.target.value as OrderStatus | "")}><option value="">全部状态</option>{Object.entries(statusLabel).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></div>
            <DataTable loading={loading} empty={filteredOrders.length === 0}><table><thead><tr><th>订单</th><th>收货信息</th><th>金额</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead><tbody>{filteredOrders.map((order) => <tr key={order.id}><td><strong>{order.orderNo}</strong><small className="block-muted">{order.items.length} 种商品</small></td><td>{order.receiverName}<small className="block-muted">{order.receiverPhone}</small></td><td><strong>¥{order.totalAmount.toFixed(2)}</strong></td><td><span className={`status-badge ${order.status.toLowerCase()}`}>{statusLabel[order.status]}</span></td><td>{formatTime(order.createdAt)}</td><td>{order.status === "PAID" ? <button className="primary-button compact-button" onClick={() => setShippingOrder(order)}><Truck size={15} />发货</button> : order.trackingNo ? <small>{order.trackingNo}</small> : <span className="block-muted">--</span>}</td></tr>)}</tbody></table></DataTable>
          </>
        )}

        {view === "afterSales" && (
          <><AdminHeading kicker="仅退款直接到账，退货退款验收后到账" title="售后管理" /><DataTable loading={loading} empty={afterSales.length === 0}><table><thead><tr><th>售后单</th><th>订单</th><th>类型</th><th>原因</th><th>退款金额</th><th>状态</th><th>操作</th></tr></thead><tbody>{afterSales.map((item) => <tr key={item.id}><td><strong>{item.afterSaleNo}</strong><small className="block-muted">{formatTime(item.createdAt)}</small></td><td>{item.orderNo}</td><td>{afterSaleTypeLabel[item.type]}</td><td className="reason-cell">{item.reason}{item.returnTrackingNo && <small className="block-muted">{item.returnCarrier} · {item.returnTrackingNo}</small>}</td><td><strong>¥{item.refundAmount.toFixed(2)}</strong></td><td><span className={`status-badge ${item.status.toLowerCase()}`}>{afterSaleLabel[item.status]}</span>{item.adminRemark && <small className="block-muted">{item.adminRemark}</small>}</td><td>{item.status === "PENDING" ? <div className="table-actions"><button className="primary-button compact-button" onClick={() => setReviewing({ item, approved: true })}>{item.type === "REFUND_ONLY" ? "批准退款" : "同意退货"}</button><button className="danger-button compact-button" onClick={() => setReviewing({ item, approved: false })}>拒绝</button></div> : item.status === "WAITING_RECEIPT" ? <button className="primary-button compact-button" onClick={() => setReceivingAfterSale(item)}><Truck size={15} />确认收货</button> : <span className="block-muted">{item.status === "WAITING_RETURN" ? "等待买家寄回" : "已处理"}</span>}</td></tr>)}</tbody></table></DataTable>
          </>
        )}

        {view === "users" && (
          <><AdminHeading kicker="买家账户资料与本地余额" title="用户管理" action={<button className="primary-button" onClick={() => setCreatingUser(true)}><Plus size={17} />新增用户</button>} /><AdminToolbar keyword={keyword} setKeyword={setKeyword} placeholder="搜索用户名、姓名或手机" /><DataTable loading={loading} empty={filteredUsers.length === 0}><table><thead><tr><th>用户</th><th>联系方式</th><th>余额</th><th>注册时间</th><th>操作</th></tr></thead><tbody>{filteredUsers.map((item) => <tr key={item.id}><td><strong>{item.displayName}</strong><small className="block-muted">@{item.username}</small></td><td>{item.phone || "--"}</td><td><strong>¥{item.balance.toFixed(2)}</strong></td><td>{formatTime(item.createdAt)}</td><td><button className="secondary-button compact-button" onClick={() => setAdjustingUser(item)}><CircleDollarSign size={15} />调整余额</button></td></tr>)}</tbody></table></DataTable></>
        )}
      </main>

      {editingProduct && <ProductEditor product={editingProduct === "new" ? null : editingProduct} onClose={() => setEditingProduct(null)} onSave={saveProduct} />}
      {shippingOrder && <TextActionDialog kicker={`订单 ${shippingOrder.orderNo}`} title="填写物流单号" label="物流单号" placeholder="例如 SF1234567890" submitText="确认发货" onClose={() => setShippingOrder(null)} onSubmit={shipOrder} />}
      {reviewing && <TextActionDialog kicker={reviewing.item.afterSaleNo} title={reviewing.approved ? reviewing.item.type === "REFUND_ONLY" ? "批准仅退款" : "同意退货退款" : "拒绝售后申请"} label="处理说明" placeholder="填写审核意见" submitText={reviewing.approved ? reviewing.item.type === "REFUND_ONLY" ? "确认退款" : "确认同意" : "确认拒绝"} danger={!reviewing.approved} onClose={() => setReviewing(null)} onSubmit={reviewAfterSale} />}
      {receivingAfterSale && <TextActionDialog kicker={receivingAfterSale.afterSaleNo} title="确认收到退货" label="验收说明" placeholder="填写退货商品验收结果" submitText="确认收货并退款" onClose={() => setReceivingAfterSale(null)} onSubmit={confirmAfterSaleReceipt} />}
      {adjustingUser && <BalanceDialog user={adjustingUser} onClose={() => setAdjustingUser(null)} onSubmit={adjustBalance} />}
      {creatingUser && <UserCreator onClose={() => setCreatingUser(false)} onSave={createUser} />}
    </div>
  );
}

const afterSaleLabel = { PENDING: "待处理", WAITING_RETURN: "待买家寄回", WAITING_RECEIPT: "待确认收货", APPROVED: "已退款", REJECTED: "已拒绝" } as const;
const afterSaleTypeLabel = { REFUND_ONLY: "仅退款", RETURN_REFUND: "退货退款" } as const;

function AdminNav({ active, icon, label, onClick }: { active: boolean; icon: React.ReactNode; label: string; onClick: () => void }) {
  return <button className={active ? "active" : ""} type="button" onClick={onClick}>{icon}{label}</button>;
}

function AdminHeading({ kicker, title, action, onRefresh }: { kicker: string; title: string; action?: React.ReactNode; onRefresh?: () => void }) {
  return <div className="admin-heading"><div><span className="section-kicker">{kicker}</span><h1>{title}</h1></div>{action || (onRefresh && <button className="secondary-button" onClick={onRefresh}><RefreshCw size={16} />刷新数据</button>)}</div>;
}

function AdminToolbar({ keyword, setKeyword, placeholder }: { keyword: string; setKeyword: (value: string) => void; placeholder: string }) {
  return <div className="admin-toolbar"><label className="search-box"><Search size={17} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder={placeholder} /></label></div>;
}

function Stat({ label, value, note, icon }: { label: string; value: string; note: string; icon: React.ReactNode }) {
  return <article className="stat-item"><span>{icon}{label}</span><strong>{value}</strong><small>{note}</small></article>;
}

function DataTable({ loading, empty, children }: { loading: boolean; empty: boolean; children: React.ReactNode }) {
  if (loading) return <div className="state-panel">正在读取数据...</div>;
  if (empty) return <div className="state-panel orders-empty"><Search size={28} /><strong>没有匹配的数据</strong><span>调整筛选条件后再试</span></div>;
  return <div className="data-table">{children}</div>;
}

function ProductEditor({ product, onClose, onSave }: { product: Product | null; onClose: () => void; onSave: (payload: ProductPayload) => Promise<void> }) {
  const [form, setForm] = useState<ProductPayload>(product ? toProductPayload(product) : { sku: "", name: "", category: "", description: "", price: 0, promotionPrice: null, stock: 0, imageUrl: "", active: true });
  const [saving, setSaving] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); setSaving(true); await onSave(form); setSaving(false); }
  return <div className="modal-backdrop"><form className="action-dialog product-editor" onSubmit={submit}><button className="icon-button modal-close" type="button" onClick={onClose} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">{product ? product.sku : "新商品"}</span><h2>{product ? "编辑商品" : "新增商品"}</h2><div className="form-grid"><label><span>SKU</span><input required maxLength={50} value={form.sku} onChange={(event) => setForm({ ...form, sku: event.target.value })} /></label><label><span>商品名称</span><input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label><span>分类</span><input required maxLength={50} value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} /></label><label><span>库存</span><input required min="0" type="number" value={form.stock} onChange={(event) => setForm({ ...form, stock: Number(event.target.value) })} /></label><label><span>原价</span><input required min="0.01" step="0.01" type="number" value={form.price} onChange={(event) => setForm({ ...form, price: Number(event.target.value) })} /></label><label><span>促销价（可空）</span><input min="0.01" step="0.01" type="number" value={form.promotionPrice ?? ""} onChange={(event) => setForm({ ...form, promotionPrice: event.target.value ? Number(event.target.value) : null })} /></label></div><label><span>图片地址</span><input required type="url" maxLength={500} value={form.imageUrl} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} /></label><label><span>商品描述</span><textarea required rows={4} maxLength={1000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><label className="checkbox-row"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /><span>立即上架</span></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={onClose}>取消</button><button className="primary-button" disabled={saving}>{saving ? "正在保存" : "保存商品"}</button></div></form></div>;
}

function TextActionDialog({ kicker, title, label, placeholder, submitText, danger, onClose, onSubmit }: { kicker: string; title: string; label: string; placeholder: string; submitText: string; danger?: boolean; onClose: () => void; onSubmit: (value: string) => Promise<void> }) {
  const [value, setValue] = useState(""); const [submitting, setSubmitting] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); setSubmitting(true); await onSubmit(value); setSubmitting(false); }
  return <div className="modal-backdrop"><form className="action-dialog" onSubmit={submit}><button className="icon-button modal-close" type="button" onClick={onClose} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">{kicker}</span><h2>{title}</h2><label><span>{label}</span><textarea required rows={3} maxLength={500} placeholder={placeholder} value={value} onChange={(event) => setValue(event.target.value)} /></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={onClose}>取消</button><button className={danger ? "danger-button" : "primary-button"} disabled={submitting}>{submitting ? "正在处理" : submitText}</button></div></form></div>;
}

function BalanceDialog({ user, onClose, onSubmit }: { user: User; onClose: () => void; onSubmit: (amount: number, description: string) => Promise<void> }) {
  const [amount, setAmount] = useState(""); const [description, setDescription] = useState(""); const [submitting, setSubmitting] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); setSubmitting(true); await onSubmit(Number(amount), description); setSubmitting(false); }
  return <div className="modal-backdrop"><form className="action-dialog" onSubmit={submit}><button className="icon-button modal-close" type="button" onClick={onClose} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">@{user.username} · 当前 ¥{user.balance.toFixed(2)}</span><h2>调整用户余额</h2><label><span>调整金额</span><input required step="0.01" type="number" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="充值填正数，扣减填负数" /></label><label><span>调整说明</span><textarea required rows={3} maxLength={200} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="例如：线下充值" /></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={onClose}>取消</button><button className="primary-button" disabled={submitting}>{submitting ? "正在处理" : "确认调整"}</button></div></form></div>;
}

function UserCreator({ onClose, onSave }: { onClose: () => void; onSave: (payload: UserCreatePayload) => Promise<void> }) {
  const [form, setForm] = useState<UserCreatePayload>({ username: "", password: "", displayName: "", phone: "" });
  const [saving, setSaving] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); setSaving(true); await onSave(form); setSaving(false); }
  return <div className="modal-backdrop"><form className="action-dialog" onSubmit={submit}><button className="icon-button modal-close" type="button" onClick={onClose} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">普通用户账户</span><h2>新增用户</h2><div className="form-grid"><label><span>用户名</span><input required minLength={4} maxLength={30} pattern="[A-Za-z0-9_]+" autoComplete="off" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></label><label><span>姓名</span><input required maxLength={50} autoComplete="name" value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label><label><span>手机号</span><input required minLength={6} maxLength={30} pattern="[0-9+ -]+" autoComplete="tel" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label><label><span>初始密码</span><input required minLength={6} maxLength={50} type="password" autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label></div><div className="dialog-actions"><button className="secondary-button" type="button" onClick={onClose}>取消</button><button className="primary-button" disabled={saving}>{saving ? "正在创建" : "创建用户"}</button></div></form></div>;
}

function toProductPayload(product: Product, active = product.active): ProductPayload {
  return { sku: product.sku, name: product.name, category: product.category, description: product.description, price: product.price, promotionPrice: product.promotionPrice, stock: product.stock, imageUrl: product.imageUrl, active };
}
