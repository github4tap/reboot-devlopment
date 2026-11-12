package com.adobe.aem.guides.wknd.core.services;

import java.util.List;

import com.adobe.aem.guides.wknd.core.dto.Product;

/**
 * Service providing product listing data.
 */
public interface ProductListingService {

    /**
     * Retrieve all available products up to the default configured limit.
     *
     * @return List of products.
     */
    List<Product> getProducts();

    /**
     * Retrieve products up to the specified limit.
     *
     * @param limit maximum number of products to return (e.g. 9 for a 3x3 grid).
     * @return List of products.
     */
    List<Product> getProducts(int limit);

    /**
     * Retrieve products serialized as a JSON string.
     *
     * @param limit maximum number of products to return.
     * @return JSON string representing an array of products.
     */
    String getProductsAsJson(int limit);
}

