import { Eye, Plus, ShoppingBag } from "lucide-react";

import type { Product } from "../types";

type ProductCardProps = {
  product: Product;
  busy: boolean;
  onAdd: (productId: number) => void;
  onView: (product: Product) => void;
};

export function ProductCard({ product, busy, onAdd, onView }: ProductCardProps) {
  return (
    <article className="product-card">
      <div className="product-image-wrap">
        <img src={product.imageUrl} alt={product.name} className="product-image" />
        <span className="category-label">{product.category}</span>
      </div>
      <div className="product-copy">
        <div className="product-title-row">
          <h3>{product.name}</h3>
          <span className="stock-text">库存 {product.stock}</span>
        </div>
        <p>{product.description}</p>
        <div className="product-footer">
          <div className="price-stack">
            <strong>¥{product.salePrice.toFixed(2)}</strong>
            {product.promotionPrice !== null && <del>¥{product.price.toFixed(2)}</del>}
          </div>
          <div className="product-actions">
            <button className="icon-button view-button" type="button" onClick={() => onView(product)} title="查看详情" aria-label={`查看${product.name}详情`}>
              <Eye size={17} />
            </button>
            <button
              className="add-button"
              type="button"
              disabled={busy || product.stock === 0}
              onClick={() => onAdd(product.id)}
              aria-label={`将${product.name}加入购物车`}
              title="加入购物车"
            >
              {product.stock === 0 ? <ShoppingBag size={18} /> : <Plus size={18} />}
              <span>{product.stock === 0 ? "售罄" : "加入"}</span>
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}
