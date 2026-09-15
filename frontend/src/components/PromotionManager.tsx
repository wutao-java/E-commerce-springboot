import { FormEvent, useEffect, useState } from "react";
import { Megaphone, Pencil, Plus, X } from "lucide-react";

import { adminApi } from "../api";
import type { Product, Promotion, PromotionPayload } from "../types";

const emptyPromotion: PromotionPayload = {
  productId: 0,
  promotionName: "",
  promotionType: "instant_discount",
  discountSummary: "",
  promotionPrice: 0,
  requiredMemberLevel: null,
  conditionSummary: "",
  startAt: null,
  endAt: null,
  active: true,
};

export function PromotionManager({ showNotice, showError }: {
  showNotice: (message: string) => void;
  showError: (error: unknown) => void;
}) {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [editing, setEditing] = useState<Promotion | "new" | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const [promotionData, productData] = await Promise.all([adminApi.listPromotions(), adminApi.listProducts()]);
      setPromotions(promotionData);
      setProducts(productData);
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void load(); }, []);

  async function save(payload: PromotionPayload) {
    try {
      if (editing === "new") await adminApi.createPromotion(payload);
      else if (editing) await adminApi.updatePromotion(editing.id, payload);
      setEditing(null);
      await load();
      showNotice("促销活动已保存");
    } catch (error) {
      showError(error);
    }
  }

  return (
    <>
      <div className="admin-heading"><div><span className="section-kicker">会员、时间与参与商品</span><h1>活动管理</h1></div><button className="primary-button" onClick={() => setEditing("new")}><Plus size={17} />新增活动</button></div>
      {loading ? <div className="state-panel">正在读取活动...</div> : promotions.length === 0 ? <div className="state-panel"><Megaphone size={28} /><strong>暂无活动</strong></div> : (
        <div className="data-table"><table><thead><tr><th>活动</th><th>商品</th><th>活动价</th><th>会员条件</th><th>有效期</th><th>状态</th><th>操作</th></tr></thead><tbody>{promotions.map((promotion) => {
          const product = products.find((item) => item.id === promotion.productId);
          return <tr key={promotion.id}><td><strong>{promotion.promotionName}</strong><small className="block-muted">{promotion.discountSummary}</small></td><td>{product?.name || promotion.productId}</td><td><strong>¥{promotion.promotionPrice.toFixed(2)}</strong></td><td>{promotion.requiredMemberLevel || "全部会员"}</td><td><small>{formatDate(promotion.startAt)}<br />至 {formatDate(promotion.endAt)}</small></td><td><span className={`status-badge ${promotion.active ? "approved" : "canceled"}`}>{promotion.active ? "启用" : "停用"}</span></td><td><button className="icon-button" onClick={() => setEditing(promotion)} title="编辑活动" aria-label={`编辑${promotion.promotionName}`}><Pencil size={16} /></button></td></tr>;
        })}</tbody></table></div>
      )}
      {editing && <PromotionEditor promotion={editing === "new" ? null : editing} products={products} onClose={() => setEditing(null)} onSave={save} />}
    </>
  );
}

function PromotionEditor({ promotion, products, onClose, onSave }: {
  promotion: Promotion | null;
  products: Product[];
  onClose: () => void;
  onSave: (payload: PromotionPayload) => Promise<void>;
}) {
  const [form, setForm] = useState<PromotionPayload>(promotion ? {
    productId: promotion.productId,
    promotionName: promotion.promotionName,
    promotionType: promotion.promotionType,
    discountSummary: promotion.discountSummary,
    promotionPrice: promotion.promotionPrice,
    requiredMemberLevel: promotion.requiredMemberLevel,
    conditionSummary: promotion.conditionSummary,
    startAt: toInputDate(promotion.startAt),
    endAt: toInputDate(promotion.endAt),
    active: promotion.active,
  } : { ...emptyPromotion, productId: products[0]?.id || 0 });
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    await onSave({ ...form, startAt: form.startAt || null, endAt: form.endAt || null });
    setSaving(false);
  }

  return <div className="modal-backdrop"><form className="action-dialog promotion-editor" onSubmit={submit}><button className="icon-button modal-close" type="button" onClick={onClose} title="关闭" aria-label="关闭"><X size={19} /></button><span className="section-kicker">独立促销规则</span><h2>{promotion ? "编辑活动" : "新增活动"}</h2><label><span>参与商品</span><select disabled={Boolean(promotion)} value={form.productId} onChange={(event) => setForm({ ...form, productId: Number(event.target.value) })}>{products.map((product) => <option value={product.id} key={product.id}>{product.name}</option>)}</select></label><div className="form-grid"><label><span>活动名称</span><input required maxLength={100} value={form.promotionName} onChange={(event) => setForm({ ...form, promotionName: event.target.value })} /></label><label><span>活动类型</span><select value={form.promotionType} onChange={(event) => setForm({ ...form, promotionType: event.target.value })}><option value="instant_discount">限时直降</option><option value="member_discount">会员专享</option><option value="category_coupon">品类优惠</option><option value="subsidy_discount">平台补贴</option></select></label><label><span>活动价</span><input required min="0.01" step="0.01" type="number" value={form.promotionPrice} onChange={(event) => setForm({ ...form, promotionPrice: Number(event.target.value) })} /></label><label><span>会员门槛</span><select value={form.requiredMemberLevel || ""} onChange={(event) => setForm({ ...form, requiredMemberLevel: event.target.value || null })}><option value="">全部会员</option><option value="silver">银卡会员</option><option value="gold">金卡会员</option></select></label><label><span>开始时间</span><input type="datetime-local" value={form.startAt || ""} onChange={(event) => setForm({ ...form, startAt: event.target.value })} /></label><label><span>结束时间</span><input type="datetime-local" value={form.endAt || ""} onChange={(event) => setForm({ ...form, endAt: event.target.value })} /></label></div><label><span>优惠说明</span><textarea required rows={2} value={form.discountSummary} onChange={(event) => setForm({ ...form, discountSummary: event.target.value })} /></label><label><span>使用条件</span><textarea rows={2} value={form.conditionSummary} onChange={(event) => setForm({ ...form, conditionSummary: event.target.value })} /></label><label className="checkbox-row"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /><span>启用活动</span></label><div className="dialog-actions"><button className="secondary-button" type="button" onClick={onClose}>取消</button><button className="primary-button" disabled={saving || !form.productId}>{saving ? "正在保存" : "保存活动"}</button></div></form></div>;
}

function toInputDate(value: string | null) {
  return value ? value.slice(0, 16) : null;
}

function formatDate(value: string | null) {
  return value ? value.replace("T", " ").slice(0, 16) : "不限";
}
