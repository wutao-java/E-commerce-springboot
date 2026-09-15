import { FormEvent, useEffect, useRef, useState } from "react";
import { MapPin, X } from "lucide-react";

import type { Cart, CheckoutPayload, User } from "../types";

type CheckoutDialogProps = {
  open: boolean;
  cart: Cart;
  submitting: boolean;
  user: User;
  onClose: () => void;
  onSubmit: (payload: CheckoutPayload) => void;
};

export function CheckoutDialog({ open, cart, submitting, user, onClose, onSubmit }: CheckoutDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [form, setForm] = useState<CheckoutPayload>({
    receiverName: user.displayName,
    receiverPhone: user.phone,
    shippingAddress: user.address,
  });

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  useEffect(() => {
    if (open) {
      setForm({ receiverName: user.displayName, receiverPhone: user.phone, shippingAddress: user.address });
    }
  }, [open, user]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit(form);
  }

  return (
    <dialog ref={dialogRef} className="checkout-dialog" onCancel={onClose} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <header className="dialog-header">
          <div>
            <span className="section-kicker">订单确认</span>
            <h2>填写收货信息</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="关闭" title="关闭">
            <X size={19} />
          </button>
        </header>

        <div className="checkout-products">
          <MapPin size={18} />
          <span>{cart.itemCount} 件商品</span>
          <strong>¥{cart.totalAmount.toFixed(2)}</strong>
        </div>

        <label>
          <span>收货人</span>
          <input
            required
            maxLength={50}
            value={form.receiverName}
            onChange={(event) => setForm({ ...form, receiverName: event.target.value })}
          />
        </label>
        <label>
          <span>联系电话</span>
          <input
            required
            pattern="[0-9+ -]{6,30}"
            value={form.receiverPhone}
            onChange={(event) => setForm({ ...form, receiverPhone: event.target.value })}
          />
        </label>
        <label>
          <span>收货地址</span>
          <textarea
            required
            maxLength={300}
            rows={3}
            value={form.shippingAddress}
            onChange={(event) => setForm({ ...form, shippingAddress: event.target.value })}
          />
        </label>

        <footer className="dialog-actions">
          <button className="secondary-button" type="button" onClick={onClose}>返回购物车</button>
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? "正在提交" : `提交订单 ¥${cart.totalAmount.toFixed(2)}`}
          </button>
        </footer>
      </form>
    </dialog>
  );
}
