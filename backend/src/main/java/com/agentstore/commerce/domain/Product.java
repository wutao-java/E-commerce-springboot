package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "promotion_price", precision = 12, scale = 2)
    private BigDecimal promotionPrice;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, length = 500)
    private String highlights = "";

    @Column(name = "supports_seven_day_return", nullable = false)
    private Boolean supportsSevenDayReturn = true;

    @Column(name = "after_sale_note", nullable = false, length = 500)
    private String afterSaleNote = "";

    @Column(name = "scenario_tags", nullable = false, length = 300)
    private String scenarioTags = "";

    protected Product() {
    }

    public Product(String sku, String name, String category, String description, BigDecimal price,
                   Integer stock, String imageUrl, Boolean active) {
        this(sku, name, category, description, price, null, stock, imageUrl, active);
    }

    public Product(String sku, String name, String category, String description, BigDecimal price,
                   BigDecimal promotionPrice, Integer stock, String imageUrl, Boolean active) {
        this(sku, name, category, description, price, promotionPrice, stock, imageUrl, active,
            "", true, "", "");
    }

    public Product(String sku, String name, String category, String description, BigDecimal price,
                   BigDecimal promotionPrice, Integer stock, String imageUrl, Boolean active,
                   String highlights, Boolean supportsSevenDayReturn, String afterSaleNote,
                   String scenarioTags) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.promotionPrice = promotionPrice;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.active = active;
        this.highlights = highlights == null ? "" : highlights;
        this.supportsSevenDayReturn = supportsSevenDayReturn == null || supportsSevenDayReturn;
        this.afterSaleNote = afterSaleNote == null ? "" : afterSaleNote;
        this.scenarioTags = scenarioTags == null ? "" : scenarioTags;
    }

    public void update(String sku, String name, String category, String description, BigDecimal price,
                       BigDecimal promotionPrice, Integer stock, String imageUrl, Boolean active) {
        update(sku, name, category, description, price, promotionPrice, stock, imageUrl, active,
            highlights, supportsSevenDayReturn, afterSaleNote, scenarioTags);
    }

    public void update(String sku, String name, String category, String description, BigDecimal price,
                       BigDecimal promotionPrice, Integer stock, String imageUrl, Boolean active,
                       String highlights, Boolean supportsSevenDayReturn, String afterSaleNote,
                       String scenarioTags) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.promotionPrice = promotionPrice;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.active = active;
        this.highlights = highlights == null ? "" : highlights;
        this.supportsSevenDayReturn = supportsSevenDayReturn == null || supportsSevenDayReturn;
        this.afterSaleNote = afterSaleNote == null ? "" : afterSaleNote;
        this.scenarioTags = scenarioTags == null ? "" : scenarioTags;
    }

    public BigDecimal getSalePrice() {
        return promotionPrice == null ? price : promotionPrice;
    }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getPromotionPrice() {
        return promotionPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public String getHighlights() {
        return highlights == null ? "" : highlights;
    }

    public Boolean getSupportsSevenDayReturn() {
        return supportsSevenDayReturn == null || supportsSevenDayReturn;
    }

    public String getAfterSaleNote() {
        return afterSaleNote == null ? "" : afterSaleNote;
    }

    public String getScenarioTags() {
        return scenarioTags == null ? "" : scenarioTags;
    }
}
