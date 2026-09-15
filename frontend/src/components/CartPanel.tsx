import { Minus, Plus, ShoppingBag, Trash2 } from "lucide-react";

import type { Cart } from "../types";

type CartPanelProps = {
  cart: Cart;
  busyItemId: number | null;
  onChangeQuantity: (itemId: number, quantity: number) => void;
  onToggleSelected: (itemId: number, selected: boolean) => void;
  onRemove: (itemId: number) => void;
  onCheckout: () => void;
};

export function CartPanel({
  cart,
  busyItemId,
  onChangeQuantity,
  onToggleSelected,
  onRemove,
  onCheckout,
}: CartPanelProps) {
  return (
    <aside id="cart" className="cart-panel" aria-label="购物车">
      <div className="cart-heading">
        <div>
          <span className="section-kicker">当前清单</span>
          <h2>购物车</h2>
        </div>
        <span className="cart-count">{cart.itemCount}</span>
      </div>

      <div className={`order-track ${cart.itemCount > 0 ? "has-items" : ""}`} aria-label="订单进度">
        <span className="active">选品</span>
        <span>确认</span>
        <span>下单</span>
      </div>

      {cart.items.length === 0 ? (
        <div className="empty-cart">
          <ShoppingBag size={28} strokeWidth={1.5} />
          <strong>购物车还是空的</strong>
          <span>从商品列表选择需要的商品</span>
        </div>
      ) : (
        <div className="cart-items">
          {cart.items.map((item) => (
            <div className={`cart-item ${item.selected ? "selected" : ""}`} key={item.id}>
              <input
                className="cart-select"
                type="checkbox"
                checked={item.selected}
                disabled={busyItemId === item.id || !item.settlementAvailable}
                onChange={(event) => onToggleSelected(item.id, event.target.checked)}
                aria-label={`选择${item.productName}`}
              />
              <img src={item.imageUrl} alt="" />
              <div className="cart-item-copy">
                <strong>{item.productName}</strong>
                <span>¥{item.unitPrice.toFixed(2)}</span>
                {item.promotionName && <small className="promotion-note">{item.promotionName}</small>}
                {!item.settlementAvailable && <small className="unavailable-note">{item.unavailableReason}</small>}
                <div className="quantity-control" aria-label={`${item.productName}数量`}>
                  <button
                    type="button"
                    disabled={busyItemId === item.id || item.quantity <= 1}
                    onClick={() => onChangeQuantity(item.id, item.quantity - 1)}
                    aria-label="减少数量"
                    title="减少数量"
                  >
                    <Minus size={15} />
                  </button>
                  <span>{item.quantity}</span>
                  <button
                    type="button"
                    disabled={busyItemId === item.id || item.quantity >= item.stock}
                    onClick={() => onChangeQuantity(item.id, item.quantity + 1)}
                    aria-label="增加数量"
                    title="增加数量"
                  >
                    <Plus size={15} />
                  </button>
                </div>
              </div>
              <div className="cart-item-total">
                <strong>¥{item.subtotal.toFixed(2)}</strong>
                <button
                  className="icon-button"
                  type="button"
                  disabled={busyItemId === item.id}
                  onClick={() => onRemove(item.id)}
                  aria-label={`移除${item.productName}`}
                  title="移除商品"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="cart-summary">
        <div>
          <span>已选 {cart.selectedItemCount} 件</span>
          <strong>¥{cart.selectedTotalAmount.toFixed(2)}</strong>
        </div>
        <small>下单后可使用账户余额支付</small>
        <button className="primary-button checkout-button" type="button" disabled={!cart.selectedItemCount} onClick={onCheckout}>
          确认订单
        </button>
      </div>
    </aside>
  );
}
