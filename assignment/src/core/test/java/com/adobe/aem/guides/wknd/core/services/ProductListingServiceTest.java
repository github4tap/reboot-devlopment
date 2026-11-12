package com.adobe.aem.guides.wknd.core.services;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adobe.aem.guides.wknd.core.dto.Product;
import com.adobe.aem.guides.wknd.core.services.config.ProductListingServiceConfig;
import com.adobe.aem.guides.wknd.core.services.impl.ProductListingServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

class ProductListingServiceTest {

    private ProductListingServiceImpl fixture;

    @BeforeEach
    void setUp() {
        fixture = new ProductListingServiceImpl();
    }

    private ProductListingServiceConfig createConfig(String endpoint, int defaultLimit) {
        return new ProductListingServiceConfig() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ProductListingServiceConfig.class;
            }

            @Override
            public String apiEndpointUrl() {
                return endpoint;
            }

            @Override
            public int defaultLimit() {
                return defaultLimit;
            }

            @Override
            public int connectionTimeoutMs() {
                return 3000;
            }

            @Override
            public int readTimeoutMs() {
                return 3000;
            }
        };
    }

    @Test
    void testGetDefaultProductsReturns9For3x3Grid() {
        fixture.activate(createConfig("", 9));
        List<Product> products = fixture.getProducts();

        assertNotNull(products);
        assertEquals(9, products.size(), "Should return exactly 9 products for 3x3 grid");

        Product first = products.get(0);
        assertEquals(1L, first.getId());
        assertEquals("Thai Peanut Noodles", first.getName());
        assertEquals(4.99, first.getPrice());
        assertEquals("USD", first.getCurrency());
        assertEquals("http://dummyimage.com/189x105.png/ff4444/ffffff", first.getImageUrl());
        assertEquals("4.99USD", first.getFormattedPrice());
    }

    @Test
    void testGetProductsCustomLimit() {
        fixture.activate(createConfig("", 9));
        List<Product> products = fixture.getProducts(3);

        assertNotNull(products);
        assertEquals(3, products.size());
    }

    @Test
    void testGetProductsJsonSerialization() {
        fixture.activate(createConfig("", 9));
        String json = fixture.getProductsAsJson(9);

        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"Thai Peanut Noodles\""));
        assertTrue(json.contains("\"price\":4.99"));
        assertTrue(json.contains("\"currency\":\"USD\""));
        assertTrue(json.contains("\"id\":9"));
        assertFalse(json.contains("\"id\":10"), "10th product should be excluded when limit is 9");
    }

    @Test
    void testFallbackWhenExternalEndpointIsInvalid() {
        fixture.activate(createConfig("http://invalid-endpoint-domain-1234567.test/api", 9));
        List<Product> products = fixture.getProducts(9);

        assertNotNull(products);
        assertEquals(9, products.size(), "Should fallback to seed catalog on network error");
    }

    @Test
    void testProductDtoEqualsAndHashCode() {
        Product p1 = new Product(1L, "Item", 10.0, "USD", "http://image.png");
        Product p2 = new Product(1L, "Item", 10.0, "USD", "http://image.png");
        Product p3 = new Product(2L, "Item 2", 20.0, "EUR", "http://image2.png");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
        assertTrue(p1.toString().contains("id=1"));
    }
}

