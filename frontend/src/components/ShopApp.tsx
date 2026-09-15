import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Boxes,
  BookOpenCheck,
  CircleDollarSign,
  ClipboardList,
  LogOut,
  MessageCircle,
  PackageCheck,
  RotateCcw,
  Search,
  ShoppingCart,
  Store,
  Truck,
  UserRound,
  X,
} from "lucide-react";

import { authApi, commerceApi } from "../api";
import type { AfterSale, AfterSalePolicy, AfterSaleType, BalanceRecord, Cart, CheckoutPayload, FaqEntry, Order, Product, User } from "../types";
import { CartPanel } from "./CartPanel";
import { CheckoutDialog } from "./CheckoutDialog";
import { formatTime, OrderList } from "./OrderList";
import { ProductCard } from "./ProductCard";
import { PolicyView } from "./PolicyView";
import { CustomerServiceDrawer } from "./CustomerServiceDrawer";

type ShopAppProps = {
  user: User;
  onUserChange: (user: User) => void;
  onLogout: () => void;
  showNotice: (message: string) => void;
  showError: (error: unknown) => void;
};

type ShopView = "products" | "cart" | "orders" | "afterSales" | "policies" | "account";

export function ShopApp({ user, onUserChange, onLogout, showNotice, showError }: ShopAppProps) {
  const [view, setView] = useState<ShopView>("products");
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<Cart>({ userId: user.id, items: [], itemCount: 0, totalAmount: 0, selectedItemCount: 0, selectedTotalAmount: 0 });
  const [orders, setOrders] = useState<Order[]>([]);
  const [afterSales, setAfterSales] = useState<AfterSale[]>([]);
  const [records, setRecords] = useState<BalanceRecord[]>([]);
  const [policies, setPolicies] = useState<AfterSalePolicy[]>([]);
  const [faqs, setFaqs] = useState<FaqEntry[]>([]);
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("全部");
  const [loading, setLoading] = useState(true);
  const [sectionLoading, setSectionLoading] = useState(false);
  const [busyProductId, setBusyProductId] = useState<number | null>(null);
  const [busyItemId, setBusyItemId] = useState<number | null>(null);
  const [busyOrderNo, setBusyOrderNo] = useState<string | null>(null);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [checkoutProduct, setCheckoutProduct] = useState<Product | null>(null);
  const [submittingOrder, setSubmittingOrder] = useState(false);
  const [detailProduct, setDetailProduct] = useState<Product | null>(null);
  const [afterSaleOrderNo, setAfterSaleOrderNo] = useState<string | null>(null);
  const [afterSaleType, setAfterSaleType] = useState<AfterSaleType>("REFUND_ONLY");
  const [afterSaleReason, setAfterSaleReason] = useState("");
  const [returningAfterSale, setReturningAfterSale] = useState<AfterSale | null>(null);
  const [returnCarrier, setReturnCarrier] = useState("");
  const [returnTrackingNo, setReturnTrackingNo] = useState("");
  const [supplementingAfterSale, setSupplementingAfterSale] = useState<AfterSale | null>(null);
  const [supplementContent, setSupplementContent] = useState("");
  const [busyAfterSaleId, setBusyAfterSaleId] = useState<number | null>(null);
  const [profile, setProfile] = useState({ displayName: user.displayName, phone: user.phone, address: user.address });
  const [savingProfile, setSavingProfile] = useState(false);
  const [commerceProfile, setCommerceProfile] = useState({
    preferredCategories: user.preferredCategories,
    preferredDelivery: user.preferredDelivery,
    budgetMin: user.budgetMin,
    budgetMax: user.budgetMax,
    invoiceRequired: user.invoiceRequired,
  });
  const [serviceOpen, setServiceOpen] = useState(false);
  const [serviceContext, setServiceContext] = useState<Record<string, unknown>>({ type: "HOME" });

  useEffect(() => {
    Promise.all([commerceApi.listProducts(), commerceApi.getCart()])
      .then(([productData, cartData]) => {
        setProducts(productData);
        setCart(cartData);
      })
      .catch(showError)
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(
    () => ["全部", ...Array.from(new Set(products.map((product) => product.category)))],
    [products],
  );

  const visibleProducts = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return products.filter((product) =>
      (category === "全部" || product.category === category)
      && (!normalized || [product.name, product.description, product.sku].some((value) => value.toLowerCase().includes(normalized))),
    );
  }, [category, keyword, products]);

  async function navigate(next: ShopView) {
    setView(next);
    setSectionLoading(true);
    try {
      if (next === "orders") setOrders(await commerceApi.listOrders());
      if (next === "afterSales") setAfterSales(await commerceApi.listAfterSales());
      if (next === "account") setRecords(await commerceApi.listBalanceRecords());
      if (next === "policies") {
        const [policyData, faqData] = await Promise.all([commerceApi.listPolicies(), commerceApi.listFaqs()]);
        setPolicies(policyData);
        setFaqs(faqData);
      }
    } catch (error) {
      showError(error);
    } finally {
      setSectionLoading(false);
    }
  }

  async function addToCart(productId: number) {
    setBusyProductId(productId);
    try {
      setCart(await commerceApi.addCartItem(productId));
      showNotice("已加入购物车");
    } catch (error) {
      showError(error);
    } finally {
      setBusyProductId(null);
    }
  }

  async function changeQuantity(itemId: number, quantity: number) {
    setBusyItemId(itemId);
    try {
      setCart(await commerceApi.updateCartItem(itemId, { quantity }));
    } catch (error) {
      showError(error);
    } finally {
      setBusyItemId(null);
    }
  }

  async function toggleCartItem(itemId: number, selected: boolean) {
    setBusyItemId(itemId);
    try {
      setCart(await commerceApi.updateCartItem(itemId, { selected }));
    } catch (error) {
      showError(error);
    } finally {
      setBusyItemId(null);
    }
  }

  async function removeCartItem(itemId: number) {
    setBusyItemId(itemId);
    try {
      setCart(await commerceApi.deleteCartItem(itemId));
      showNotice("商品已移出购物车");
    } catch (error) {
      showError(error);
    } finally {
      setBusyItemId(null);
    }
  }

  async function submitOrder(payload: CheckoutPayload) {
    setSubmittingOrder(true);
    try {
      const request = checkoutProduct
        ? { ...payload, source: "DIRECT_BUY" as const, productId: checkoutProduct.id, quantity: 1 }
        : { ...payload, source: "CART" as const, cartItemIds: cart.items.filter((item) => item.selected).map((item) => item.id) };
      const order = await commerceApi.createOrder(request);
      setCart(await commerceApi.getCart());
      setCheckoutOpen(false);
      setCheckoutProduct(null);
      setOrders([order, ...orders]);
      setView("orders");
      setProducts(await commerceApi.listProducts());
      showNotice(`订单 ${order.orderNo} 已创建`);
    } catch (error) {
      showError(error);
    } finally {
      setSubmittingOrder(false);
    }
  }

  function openDirectBuy(product: Product) {
    setCheckoutProduct(product);
    setCheckoutOpen(true);
  }

  async function actOnOrder(orderNo: string, action: "pay" | "cancel" | "confirm") {
    setBusyOrderNo(orderNo);
    try {
      const updated = action === "pay"
        ? await commerceApi.payOrder(orderNo)
        : action === "cancel"
          ? await commerceApi.cancelOrder(orderNo)
          : await commerceApi.confirmOrder(orderNo);
      setOrders((current) => current.map((order) => order.orderNo === orderNo ? updated : order));
      if (action === "pay") onUserChange(await authApi.me());
      if (action === "cancel") setProducts(await commerceApi.listProducts());
      showNotice(action === "pay" ? "支付成功" : action === "cancel" ? "订单已取消，库存已恢复" : "已确认收货");
    } catch (error) {
      showError(error);
    } finally {
      setBusyOrderNo(null);
    }
  }

  function openAfterSale(orderNo: string) {
    setAfterSaleType("REFUND_ONLY");
    setAfterSaleReason("");
    setAfterSaleOrderNo(orderNo);
  }

  async function submitAfterSale(event: FormEvent) {
    event.preventDefault();
    if (!afterSaleOrderNo) return;
    setBusyOrderNo(afterSaleOrderNo);
    try {
      const created = await commerceApi.createAfterSale(afterSaleOrderNo, afterSaleType, afterSaleReason);
      setAfterSales((current) => [created, ...current]);
      setOrders(await commerceApi.listOrders());
      setAfterSaleOrderNo(null);
      setAfterSaleType("REFUND_ONLY");
      setAfterSaleReason("");
      showNotice("售后申请已提交");
    } catch (error) {
      showError(error);
    } finally {
      setBusyOrderNo(null);
    }
  }

  async function submitReturnShipment(event: FormEvent) {
    event.preventDefault();
    if (!returningAfterSale) return;
    setBusyAfterSaleId(returningAfterSale.id);
    try {
      const updated = await commerceApi.submitReturnShipment(
        returningAfterSale.id, returnCarrier, returnTrackingNo,
      );
      setAfterSales((current) => current.map((item) => item.id === updated.id ? updated : item));
      setReturningAfterSale(null);
      setReturnCarrier("");
      setReturnTrackingNo("");
      showNotice("退货物流已提交");
    } catch (error) {
      showError(error);
    } finally {
      setBusyAfterSaleId(null);
    }
  }

  async function submitSupplement(event: FormEvent) {
    event.preventDefault();
    if (!supplementingAfterSale) return;
    setBusyAfterSaleId(supplementingAfterSale.id);
    try {
      const updated = await commerceApi.supplementAfterSale(supplementingAfterSale.id, supplementContent);
      setAfterSales((current) => current.map((item) => item.id === updated.id ? updated : item));
      setSupplementingAfterSale(null);
      setSupplementContent("");
      showNotice("补充材料已提交");
    } catch (error) {
      showError(error);
    } finally {
      setBusyAfterSaleId(null);
    }
  }

  async function saveProfile(event: FormEvent) {
    event.preventDefault();
    setSavingProfile(true);
    try {
      const updated = await commerceApi.updateProfile(profile);
      onUserChange(updated);
      showNotice("个人资料已保存");
    } catch (error) {
      showError(error);
    } finally {
      setSavingProfile(false);
    }
  }

  async function saveCommerceProfile(event: FormEvent) {
    event.preventDefault();
    setSavingProfile(true);
    try {
      const updated = await commerceApi.updateCommerceProfile(commerceProfile);
      onUserChange(updated);
      showNotice("购物偏好已保存");
    } catch (error) {
      showError(error);
    } finally {
      setSavingProfile(false);
    }
  }

  function openService(context: Record<string, unknown>) {
    setServiceContext(context);
    setServiceOpen(true);
  }

  const activeOrder = orders.find((order) => order.orderNo === afterSaleOrderNo);
  const availableAfterSaleTypes = activeOrder?.availableAfterSaleTypes || ["REFUND_ONLY"];
  const checkoutCart: Cart = checkoutProduct ? {
    userId: user.id,
    items: [],
    itemCount: 1,
    totalAmount: checkoutProduct.salePrice,
    selectedItemCount: 1,
    selectedTotalAmount: checkoutProduct.salePrice,
  } : cart;

  return (
    <div className="app-shell">
      <header className="topbar shop-topbar">
        <button className="brand" type="button" onClick={() => setView("products")}><span className="brand-mark"><Store size={19} /></span><span>Agent Store</span></button>
        <nav aria-label="主导航">
          <NavButton active={view === "products"} icon={<Boxes size={17} />} label="商品" onClick={() => setView("products")} />
          <NavButton active={view === "orders"} icon={<PackageCheck size={17} />} label="订单" onClick={() => navigate("orders")} />
          <NavButton active={view === "afterSales"} icon={<RotateCcw size={17} />} label="售后" onClick={() => navigate("afterSales")} />
          <NavButton active={view === "policies"} icon={<BookOpenCheck size={17} />} label="服务" onClick={() => navigate("policies")} />
          <NavButton active={view === "account"} icon={<UserRound size={17} />} label="账户" onClick={() => navigate("account")} />
          <NavButton active={view === "cart"} icon={<ShoppingCart size={17} />} label={`购物车${cart.itemCount ? ` ${cart.itemCount}` : ""}`} onClick={() => setView("cart")} />
        </nav>
        <div className="user-chip"><button className="icon-button" type="button" onClick={() => openService({ type: "HOME" })} title="咨询客服" aria-label="咨询客服"><MessageCircle size={17} /></button><span>{user.displayName}</span><strong>¥{user.balance.toFixed(2)}</strong><button className="icon-button" type="button" onClick={onLogout} title="退出登录" aria-label="退出登录"><LogOut size={17} /></button></div>
      </header>

      {view === "products" && (
        <main className="store-layout">
          <section className="catalog-section">
            <div className="catalog-heading">
              <div><span className="section-kicker">本期在售</span><h1>日常装备，精简选择</h1><p>{products.length} 件商品 · 价格与库存实时读取</p></div>
              <label className="search-box"><Search size={18} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索商品或 SKU" aria-label="搜索商品" /></label>
            </div>
            <div className="category-tabs" role="tablist" aria-label="商品分类">
              {categories.map((item) => <button key={item} className={category === item ? "active" : ""} type="button" role="tab" aria-selected={category === item} onClick={() => setCategory(item)}>{item}</button>)}
            </div>
            {loading ? <div className="state-panel">正在读取商品...</div> : visibleProducts.length === 0 ? (
              <div className="state-panel"><Search size={28} strokeWidth={1.5} /><strong>没有匹配的商品</strong><span>尝试更换关键词或分类</span></div>
            ) : (
              <div className="product-grid">{visibleProducts.map((product) => <ProductCard key={product.id} product={product} busy={busyProductId === product.id} onAdd={addToCart} onBuy={openDirectBuy} onView={setDetailProduct} />)}</div>
            )}
          </section>
        </main>
      )}

      {view === "cart" && (
        <main className="cart-page">
          <CartPanel cart={cart} busyItemId={busyItemId} onChangeQuantity={changeQuantity} onToggleSelected={toggleCartItem} onRemove={removeCartItem} onCheckout={() => { setCheckoutProduct(null); setCheckoutOpen(true); }} />
        </main>
      )}

      {view === "orders" && (
        <main className="orders-page"><PageHeading kicker={`${user.displayName}的购买记录`} title="我的订单" action={<button className="secondary-button" type="button" onClick={() => setView("products")}><ShoppingCart size={17} />继续选购</button>} />
          <OrderList orders={orders} loading={sectionLoading} busyOrderNo={busyOrderNo} onPay={(no) => actOnOrder(no, "pay")} onCancel={(no) => actOnOrder(no, "cancel")} onConfirm={(no) => actOnOrder(no, "confirm")} onAfterSale={openAfterSale} />
        </main>
      )}

      {view === "afterSales" && (
        <main className="content-page"><PageHeading kicker="退款与售后进度" title="我的售后" />
          {sectionLoading ? <div className="state-panel">正在读取售后记录...</div> : afterSales.length === 0 ? <EmptyState icon={<RotateCcw size={28} />} title="暂无售后记录" text="暂无进行中的退款或退货记录" /> : (
            <div className="after-sale-list">{afterSales.map((item) => <article className="after-sale-row" key={item.id}><div><span className={`status-badge ${item.status.toLowerCase()}`}>{afterSaleLabel[item.status]}</span><strong>{item.afterSaleNo}</strong><small>订单 {item.orderNo} · {afterSaleTypeLabel[item.type]}</small></div><p>{item.reason}</p><div className="after-sale-result"><strong>¥{(item.approvedAmount ?? item.refundAmount).toFixed(2)}</strong><span>{afterSaleMessage(item)}</span>{item.returnTrackingNo && <small>{item.returnCarrier} · {item.returnTrackingNo}</small>}<small>{formatTime(item.updatedAt)} · 审批记录 {item.approvalRecords?.length || 0} 条</small>{item.status === "WAITING_RETURN" && <button className="secondary-button compact-button" type="button" disabled={busyAfterSaleId === item.id} onClick={() => setReturningAfterSale(item)}><Truck size={15} />填写退货物流</button>}{item.status === "NEED_MORE_INFO" && <button className="secondary-button compact-button" type="button" onClick={() => setSupplementingAfterSale(item)}>补充材料</button>}</div></article>)}</div>
          )}
        </main>
      )}

      {view === "policies" && <PolicyView policies={policies} faqs={faqs} loading={sectionLoading} />}

      {view === "account" && (
        <main className="content-page"><PageHeading kicker={`账号 ${user.username}`} title="账户与资料" />
          <div className="account-layout">
            <form className="profile-form surface-panel" onSubmit={saveProfile}><div className="panel-title"><UserRound size={19} /><h2>个人资料</h2></div><label><span>姓名</span><input required maxLength={50} value={profile.displayName} onChange={(event) => setProfile({ ...profile, displayName: event.target.value })} /></label><label><span>手机号</span><input required pattern="[0-9+ -]{6,30}" value={profile.phone} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} /></label><label><span>默认地址</span><textarea rows={4} maxLength={300} value={profile.address} onChange={(event) => setProfile({ ...profile, address: event.target.value })} /></label><button className="primary-button" disabled={savingProfile}>{savingProfile ? "正在保存" : "保存资料"}</button></form>
            <form className="profile-form surface-panel" onSubmit={saveCommerceProfile}><div className="panel-title"><Boxes size={19} /><h2>会员与购物偏好</h2></div><div className="readonly-field"><span>会员等级</span><strong>{memberLevelLabel[user.memberLevel] || user.memberLevel}</strong></div><label><span>偏好品类</span><input value={commerceProfile.preferredCategories} onChange={(event) => setCommerceProfile({ ...commerceProfile, preferredCategories: event.target.value })} /></label><label><span>配送偏好</span><input value={commerceProfile.preferredDelivery} onChange={(event) => setCommerceProfile({ ...commerceProfile, preferredDelivery: event.target.value })} /></label><div className="form-grid"><label><span>预算下限</span><input type="number" min="0" value={commerceProfile.budgetMin ?? ""} onChange={(event) => setCommerceProfile({ ...commerceProfile, budgetMin: event.target.value ? Number(event.target.value) : null })} /></label><label><span>预算上限</span><input type="number" min="0" value={commerceProfile.budgetMax ?? ""} onChange={(event) => setCommerceProfile({ ...commerceProfile, budgetMax: event.target.value ? Number(event.target.value) : null })} /></label></div><label className="checkbox-row"><input type="checkbox" checked={commerceProfile.invoiceRequired} onChange={(event) => setCommerceProfile({ ...commerceProfile, invoiceRequired: event.target.checked })} /><span>默认需要电子发票</span></label><button className="primary-button" disabled={savingProfile}>保存偏好</button></form>
            <section className="balance-panel surface-panel"><div className="balance-summary"><span><CircleDollarSign size={18} />账户余额</span><strong>¥{user.balance.toFixed(2)}</strong><small>余额充值由管理员在用户管理中调整</small></div><div className="panel-title"><ClipboardList size={19} /><h2>余额流水</h2></div>{sectionLoading ? <div className="compact-empty">正在读取...</div> : records.length === 0 ? <div className="compact-empty">暂无余额变动</div> : <div className="record-list">{records.map((record) => <div key={record.id}><span className={record.amount > 0 ? "amount-in" : "amount-out"}>{record.amount > 0 ? "+" : ""}{record.amount.toFixed(2)}</span><p>{record.description}</p><small>{formatTime(record.createdAt)} · 余额 ¥{record.balanceAfter.toFixed(2)}</small></div>)}</div>}</section>
          </div>
        </main>
      )}

      <CheckoutDialog open={checkoutOpen} cart={checkoutCart} submitting={submittingOrder} user={user} onClose={() => { setCheckoutOpen(false); setCheckoutProduct(null); }} onSubmit={submitOrder} />

      {detailProduct && <div className="modal-backdrop" role="presentation" onMouseDown={() => setDetailProduct(null)}><section className="detail-dialog" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}><button className="icon-button modal-close" onClick={() => setDetailProduct(null)} title="关闭" aria-label="关闭"><X size={19} /></button><img src={detailProduct.imageUrl} alt={detailProduct.name} /><div><span className="section-kicker">{detailProduct.category} / {detailProduct.sku}</span><h2>{detailProduct.name}</h2><p>{detailProduct.description}</p>{detailProduct.highlights && <p className="product-highlights">{detailProduct.highlights}</p>}<small>{detailProduct.afterSaleNote}</small><div className="detail-meta"><span>当前库存 {detailProduct.stock}</span><strong>¥{detailProduct.salePrice.toFixed(2)}</strong>{detailProduct.salePrice < detailProduct.price && <del>¥{detailProduct.price.toFixed(2)}</del>}</div><div className="dialog-actions"><button className="secondary-button" onClick={() => openService({ type: "PRODUCT_DETAIL", productId: detailProduct.id, productName: detailProduct.name })}><MessageCircle size={17} />咨询</button><button className="secondary-button" disabled={detailProduct.stock === 0} onClick={() => { openDirectBuy(detailProduct); setDetailProduct(null); }}>立即购买</button><button className="primary-button" disabled={detailProduct.stock === 0} onClick={() => { addToCart(detailProduct.id); setDetailProduct(null); }}><ShoppingCart size={17} />加入购物车</button></div></div></section></div>}

      {afterSaleOrderNo && <div className="modal-backdrop"><form className="action-dialog" onSubmit={submitAfterSale}><button className="icon-button modal-close" type="button" onClick={() => setAfterSaleOrderNo(null)} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">订单 {afterSaleOrderNo}</span><h2>申请售后</h2><div className="form-field"><span>售后类型</span><div className="after-sale-type-control" role="group" aria-label="售后类型">{availableAfterSaleTypes.map((type) => <button className={afterSaleType === type ? "active" : ""} type="button" key={type} aria-pressed={afterSaleType === type} onClick={() => setAfterSaleType(type)}>{afterSaleTypeLabel[type]}</button>)}</div></div><label><span>申请原因</span><textarea required rows={5} maxLength={500} value={afterSaleReason} onChange={(event) => setAfterSaleReason(event.target.value)} placeholder="请描述需要售后的原因" /></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={() => setAfterSaleOrderNo(null)}>取消</button><button className="primary-button" disabled={busyOrderNo === afterSaleOrderNo}>提交申请</button></div></form></div>}

      {returningAfterSale && <div className="modal-backdrop"><form className="action-dialog" onSubmit={submitReturnShipment}><button className="icon-button modal-close" type="button" onClick={() => setReturningAfterSale(null)} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">{returningAfterSale.afterSaleNo}</span><h2>填写退货物流</h2><label><span>物流公司</span><input required maxLength={50} value={returnCarrier} onChange={(event) => setReturnCarrier(event.target.value)} placeholder="例如 顺丰速运" /></label><label><span>物流单号</span><input required maxLength={80} value={returnTrackingNo} onChange={(event) => setReturnTrackingNo(event.target.value)} placeholder="例如 SF1234567890" /></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={() => setReturningAfterSale(null)}>取消</button><button className="primary-button" disabled={busyAfterSaleId === returningAfterSale.id}>提交物流</button></div></form></div>}
      {supplementingAfterSale && <div className="modal-backdrop"><form className="action-dialog" onSubmit={submitSupplement}><button className="icon-button modal-close" type="button" onClick={() => setSupplementingAfterSale(null)} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">{supplementingAfterSale.afterSaleNo}</span><h2>补充售后材料</h2><label><span>补充说明</span><textarea required rows={5} maxLength={500} value={supplementContent} onChange={(event) => setSupplementContent(event.target.value)} /></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={() => setSupplementingAfterSale(null)}>取消</button><button className="primary-button" disabled={busyAfterSaleId === supplementingAfterSale.id}>提交材料</button></div></form></div>}
      <CustomerServiceDrawer open={serviceOpen} context={serviceContext} onClose={() => setServiceOpen(false)} />
    </div>
  );
}

const afterSaleLabel = { PENDING: "待处理", NEED_MORE_INFO: "待补充材料", WAITING_RETURN: "待寄回", WAITING_RECEIPT: "待商家收货", APPROVED: "已退款", REJECTED: "已拒绝" } as const;
const afterSaleTypeLabel = { REFUND_ONLY: "仅退款", RETURN_REFUND: "退货退款", COMPENSATION: "物流补偿", CANCEL_ORDER: "取消订单" } as const;
const memberLevelLabel: Record<string, string> = { normal: "普通会员", silver: "银卡会员", gold: "金卡会员" };

function afterSaleMessage(item: AfterSale) {
  if (item.status === "WAITING_RETURN") return "审核已通过，等待寄回商品";
  if (item.status === "WAITING_RECEIPT") return "退货运输中，等待商家确认收货";
  if (item.status === "NEED_MORE_INFO") return item.adminRemark || "请补充售后材料";
  return item.adminRemark || "等待管理员处理";
}

function NavButton({ active, icon, label, onClick }: { active: boolean; icon: React.ReactNode; label: string; onClick: () => void }) {
  return <button className={active ? "active" : ""} type="button" onClick={onClick}>{icon}{label}</button>;
}

function PageHeading({ kicker, title, action }: { kicker: string; title: string; action?: React.ReactNode }) {
  return <div className="orders-title"><div><span className="section-kicker">{kicker}</span><h1>{title}</h1></div>{action}</div>;
}

function EmptyState({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="state-panel orders-empty">{icon}<strong>{title}</strong><span>{text}</span></div>;
}
