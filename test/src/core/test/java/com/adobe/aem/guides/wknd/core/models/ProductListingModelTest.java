package com.adobe.aem.guides.wknd.core.models;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.wknd.core.services.impl.ProductListingServiceImpl;
import com.adobe.aem.guides.wknd.core.testcontext.AppAemContext;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class ProductListingModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    private Page page;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(ProductListingModel.class);
        context.registerInjectActivateService(new ProductListingServiceImpl());
        page = context.create().page("/content/wknd/us/en/products");
    }

    @Test
    void testModelWithDefaultProperties() {
        Resource resource = context.create().resource(page, "productlisting",
                "sling:resourceType", "wknd/components/productlisting");

        context.currentResource(resource);
        ProductListingModel model = resource.adaptTo(ProductListingModel.class);

        assertNotNull(model);
        assertEquals("Product Listing Page using Html/CSS/JS", model.getTitle());
        assertEquals("/bin/wknd/products", model.getApiEndpoint());
        assertEquals(9, model.getLimit());
        assertEquals(3, model.getColumns());
        assertTrue(model.hasProducts());
        assertEquals(9, model.getInitialProducts().size());
        assertEquals("grid-template-columns: repeat(3, 1fr);", model.getGridStyle());
    }

    @Test
    void testModelWithPreset2x2() {
        Resource resource = context.create().resource(page, "productlisting-2x2",
                "sling:resourceType", "wknd/components/productlisting",
                "gridPreset", "2x2");

        context.currentResource(resource);
        ProductListingModel model = resource.adaptTo(ProductListingModel.class);

        assertNotNull(model);
        assertEquals(4, model.getLimit());
        assertEquals(2, model.getColumns());
        assertEquals("grid-template-columns: repeat(2, 1fr);", model.getGridStyle());
    }

    @Test
    void testModelWithAuthoredCustomProperties() {
        Resource resource = context.create().resource(page, "productlisting-custom",
                "sling:resourceType", "wknd/components/productlisting",
                "title", "Featured Products",
                "gridPreset", "custom",
                "sourceMode", "custom",
                "apiEndpoint", "/bin/custom/products",
                "limit", 6,
                "columns", 2);

        context.currentResource(resource);
        ProductListingModel model = resource.adaptTo(ProductListingModel.class);

        assertNotNull(model);
        assertEquals("Featured Products", model.getTitle());
        assertEquals("/bin/custom/products", model.getApiEndpoint());
        assertEquals(6, model.getLimit());
        assertEquals(2, model.getColumns());
        assertEquals(6, model.getInitialProducts().size());
        assertEquals("grid-template-columns: repeat(2, 1fr);", model.getGridStyle());
    }
}
