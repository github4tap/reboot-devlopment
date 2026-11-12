package com.adobe.aem.guides.wknd.core.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.wknd.core.services.ProductListingService;
import com.adobe.aem.guides.wknd.core.services.impl.ProductListingServiceImpl;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class ProductListingServletTest {

    private ProductListingServlet servlet;

    @BeforeEach
    void setUp(AemContext context) {
        context.registerInjectActivateService(new ProductListingServiceImpl());
        servlet = context.registerInjectActivateService(new ProductListingServlet());
    }

    @Test
    void testDoGetDefaultLimit(AemContext context) throws ServletException, IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String output = response.getOutputAsString();
        assertNotNull(output);
        assertTrue(output.contains("\"id\":1"));
        assertTrue(output.contains("\"name\":\"Thai Peanut Noodles\""));
        assertTrue(output.contains("\"id\":9"));
        assertFalse(output.contains("\"id\":10"), "By default, limit=9 so 10th item is omitted");
    }

    @Test
    void testDoGetWithCustomLimit(AemContext context) throws ServletException, IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        request.addRequestParameter("limit", "3");

        servlet.doGet(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        String output = response.getOutputAsString();
        assertNotNull(output);
        assertTrue(output.contains("\"id\":1"));
        assertTrue(output.contains("\"id\":3"));
        assertFalse(output.contains("\"id\":4"));
    }

    @Test
    void testDoGetWithInvalidLimitParam(AemContext context) throws ServletException, IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        request.addRequestParameter("limit", "abc");

        servlet.doGet(request, response);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getOutputAsString().contains("Invalid 'limit' query parameter"));
    }
}

