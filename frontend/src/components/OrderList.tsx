import { Ban, CheckCircle2, Clock3, CreditCard, PackageOpen, RotateCcw, Truck } from "lucide-react";

import type { Order, OrderStatus } from "../types";

type OrderListProps = {
  orders: Order[];
  loading: boolean;
  busyOrderNo: string | null;
  onCancel: (orderNo: string) => void;
  onPay: (orderNo: string) => void;
  onConfirm: (orderNo: string) => void;
  onAfterSale: (orderNo: string) => void;
};

export const statusLabel: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "待支付",
  PAID: "待发货",
  SHIPPED: "待收货",
  COMPLETED: "已完成",
  AFTER_SALE: "售后中",
  REFUNDED: "已退款",
  CANCELED: "已取消",
};

export function OrderList({
  orders,
  loading,
  busyOrderNo,
  onCancel,
  onPay,
  onConfirm,
  onAfterSale,
}: OrderListProps) {
  if (loading) {
    return <div className="state-panel">正在读取订单...</div>;
  }
  if (orders.length === 0) {
    return (
      <div className="state-panel orders-empty">
        <PackageOpen size={30} strokeWidth={1.5} />
        <strong>暂无订单</strong>
        <span>提交购物车后，订单会出现在这里</span>
      </div>
    );
  }

  return (
    <div className="order-list">
      {orders.map((order) => (
        <article className="order-card" key={order.orderNo}>
          <header className="order-header">
            <div>
              <span className={`status-badge ${order.status.toLowerCase()}`}>{statusLabel[order.status]}</span>
              <strong>{order.orderNo}</strong>
            </div>
            <div className="order-time">
              <Clock3 size={15} />
              <span>{formatTime(order.createdAt)}</span>
            </div>
          </header>

          <div className="order-state-rail" aria-label={`订单状态：${statusLabel[order.status]}`}>
            {orderSteps(order.status).map((step, index) => (
              <span key={step.label} className={step.done ? "done" : ""}>
                <i>{index + 1}</i>{step.label}
              </span>
            ))}
          </div>

          <div className="order-body">
            <div className="order-products">
              {order.items.map((item) => (
                <div className="order-product" key={`${order.orderNo}-${item.productId}`}>
                  <img src={item.imageUrl} alt="" />
                  <div>
                    <strong>{item.productName}</strong>
                    <span>¥{item.unitPrice.toFixed(2)} × {item.quantity}</span>
                  </div>
                  <strong>¥{item.subtotal.toFixed(2)}</strong>
                </div>
              ))}
            </div>
            <dl className="shipping-info">
              <div><dt>收货人</dt><dd>{order.receiverName} · {order.receiverPhone}</dd></div>
              <div><dt>地址</dt><dd>{order.shippingAddress}</dd></div>
              {order.trackingNo && <div><dt>物流单号</dt><dd>{order.trackingNo}</dd></div>}
            </dl>
          </div>

          <footer className="order-footer">
            <div className="order-actions">
              {order.status === "PENDING_PAYMENT" && (
                <>
                  <button className="primary-button" type="button" disabled={busyOrderNo === order.orderNo} onClick={() => onPay(order.orderNo)}>
                    <CreditCard size={16} />余额支付
                  </button>
                  <button className="danger-button" type="button" disabled={busyOrderNo === order.orderNo} onClick={() => onCancel(order.orderNo)}>
                    <Ban size={16} />取消订单
                  </button>
                </>
              )}
              {order.status === "SHIPPED" && (
                <button className="primary-button" type="button" disabled={busyOrderNo === order.orderNo} onClick={() => onConfirm(order.orderNo)}>
                  <CheckCircle2 size={16} />确认收货
                </button>
              )}
              {(order.status === "PAID" || order.status === "SHIPPED" || order.status === "COMPLETED") && (
                <button className="secondary-button" type="button" disabled={busyOrderNo === order.orderNo} onClick={() => onAfterSale(order.orderNo)}>
                  <RotateCcw size={16} />申请售后
                </button>
              )}
              {order.status === "PAID" && <span className="muted-action"><Truck size={15} />等待商家发货</span>}
            </div>
            <div><span>订单金额</span><strong>¥{order.totalAmount.toFixed(2)}</strong></div>
          </footer>
        </article>
      ))}
    </div>
  );
}

function orderSteps(status: OrderStatus) {
  const rank: Record<OrderStatus, number> = {
    PENDING_PAYMENT: 0,
    PAID: 1,
    SHIPPED: 2,
    COMPLETED: 3,
    AFTER_SALE: 3,
    REFUNDED: 3,
    CANCELED: -1,
  };
  const labels = ["已下单", "已支付", "已发货", status === "REFUNDED" ? "已退款" : "已完成"];
  return labels.map((label, index) => ({ label, done: status !== "CANCELED" && index <= rank[status] }));
}

export function formatTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
