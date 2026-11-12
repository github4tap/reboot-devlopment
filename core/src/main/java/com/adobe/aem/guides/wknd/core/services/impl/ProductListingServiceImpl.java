package com.adobe.aem.guides.wknd.core.services.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.wknd.core.dto.Product;
import com.adobe.aem.guides.wknd.core.services.ProductListingService;
import com.adobe.aem.guides.wknd.core.services.config.ProductListingServiceConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Implementation of {@link ProductListingService}.
 * Fetches product catalog from an external REST API or falls back to
 * a resilient local seed dataset.
 */
@Component(
        service = ProductListingService.class,
        immediate = true
)
@Designate(ocd = ProductListingServiceConfig.class)
public class ProductListingServiceImpl implements ProductListingService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductListingServiceImpl.class);

    private static final String DEFAULT_SEED_JSON = "["
            + "{\"id\":1,\"name\":\"Thai Peanut Noodles\",\"price\":4.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/189x105.png/ff4444/ffffff\"},"
            + "{\"id\":2,\"name\":\"Floral Summer Dress\",\"price\":39.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/222x176.png/cc0000/ffffff\"},"
            + "{\"id\":3,\"name\":\"Gardening Gloves\",\"price\":15.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/178x201.png/dddddd/000000\"},"
            + "{\"id\":4,\"name\":\"Ginger Turmeric Shots\",\"price\":3.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/223x181.png/ff4444/ffffff\"},"
            + "{\"id\":5,\"name\":\"Desk Organizer Set\",\"price\":29.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/188x136.png/cc0000/ffffff\"},"
            + "{\"id\":6,\"name\":\"Honey Lemon Tea\",\"price\":3.29,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/182x154.png/dddddd/000000\"},"
            + "{\"id\":7,\"name\":\"Frozen Cauliflower Rice\",\"price\":2.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/118x235.png/5fa2dd/ffffff\"},"
            + "{\"id\":8,\"name\":\"Nut Mix\",\"price\":5.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/117x239.png/dddddd/000000\"},"
            + "{\"id\":9,\"name\":\"Woven Storage Baskets\",\"price\":24.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/235x223.png/dddddd/000000\"},"
            + "{\"id\":10,\"name\":\"Guitar Tuner\",\"price\":19.99,\"currency\":\"USD\",\"image_url\":\"http://dummyimage.com/232x130.png/5fa2dd/ffffff\"}"
            + "]";

    private final Gson gson = new GsonBuilder().create();
    private final Type productListType = new TypeToken<List<Product>>() {}.getType();

    private ProductListingServiceConfig config;

    @Activate
    @Modified
    public void activate(final ProductListingServiceConfig config) {
        this.config = config;
        LOG.info("ProductListingServiceImpl activated with endpoint: {}, default limit: {}",
                config != null ? config.apiEndpointUrl() : "none",
                config != null ? config.defaultLimit() : 9);
    }

    @Override
    public List<Product> getProducts() {
        int limit = (config != null && config.defaultLimit() > 0) ? config.defaultLimit() : 9;
        return getProducts(limit);
    }

    @Override
    public List<Product> getProducts(int limit) {
        List<Product> products = null;

        if (config != null && StringUtils.isNotBlank(config.apiEndpointUrl())) {
            products = fetchFromExternalEndpoint(config.apiEndpointUrl());
        }

        if (products == null || products.isEmpty()) {
            LOG.debug("Using local seed dataset for products");
            products = parseJson(DEFAULT_SEED_JSON);
        }

        if (limit > 0 && products.size() > limit) {
            return products.stream().limit(limit).collect(Collectors.toList());
        }

        return products;
    }

    @Override
    public String getProductsAsJson(int limit) {
        List<Product> products = getProducts(limit);
        return gson.toJson(products);
    }

    private List<Product> fetchFromExternalEndpoint(String endpointUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpointUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int connectTimeout = (config != null && config.connectionTimeoutMs() > 0) ? config.connectionTimeoutMs() : 5000;
            int readTimeout = (config != null && config.readTimeoutMs() > 0) ? config.readTimeoutMs() : 5000;
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return parseJson(response.toString());
                }
            } else {
                LOG.warn("Failed to fetch products from {}: HTTP status {}", endpointUrl, responseCode);
            }
        } catch (Exception e) {
            LOG.error("Exception while fetching products from external endpoint: {}", endpointUrl, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return Collections.emptyList();
    }

    private List<Product> parseJson(String json) {
        try {
            List<Product> list = gson.fromJson(json, productListType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            LOG.error("Failed to parse product JSON", e);
            return Collections.emptyList();
        }
    }
}
