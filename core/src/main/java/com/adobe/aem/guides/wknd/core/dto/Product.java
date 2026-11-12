package com.adobe.aem.guides.wknd.core.dto;

import java.io.Serializable;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing a Product.
 * Matches the required product listing JSON structure.
 */
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("price")
    private Double price;

    @SerializedName("currency")
    private String currency;

    @SerializedName("image_url")
    private String imageUrl;

    public Product() {
    }

    public Product(Long id, String name, Double price, String currency, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFormattedPrice() {
        if (price == null) {
            return "";
        }
        String curr = currency != null ? currency : "";
        return String.format("%.2f%s", price, curr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id) &&
               Objects.equals(name, product.name) &&
               Objects.equals(price, product.price) &&
               Objects.equals(currency, product.currency) &&
               Objects.equals(imageUrl, product.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, currency, imageUrl);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}

