package com.adobe.aem.guides.wknd.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.wknd.core.services.ProductListingService;

/**
 * Sling Servlet exposing the product catalog as a JSON endpoint.
 *
 * <p>Can be invoked either via:</p>
 * <ul>
 *   <li><b>Resource type selector:</b> <code>&lt;path-to-productlisting-component&gt;.products.json</code></li>
 *   <li><b>Path-based endpoint:</b> <code>/bin/wknd/products</code></li>
 * </ul>
 *
 * Supports query parameter: <code>limit</code> (default 9 for 3x3 grid).
 */
@Component(
        service = { Servlet.class },
        property = {
                "sling.servlet.paths=/bin/wknd/products",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
@SlingServletResourceTypes(
        resourceTypes = "wknd/components/productlisting",
        methods = HttpConstants.METHOD_GET,
        selectors = "products",
        extensions = "json"
)
@ServiceDescription("WKND Product Listing JSON Servlet")
public class ProductListingServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ProductListingServlet.class);
    private static final int DEFAULT_LIMIT = 9;
    private static final int MAX_LIMIT = 100;

    @Reference
    private transient ProductListingService productListingService;

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "public, max-age=60");

        try {
            int limit = parseLimit(request.getParameter("limit"));
            LOG.debug("ProductListingServlet handling request with limit={}", limit);

            String jsonResponse = productListingService.getProductsAsJson(limit);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonResponse);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid limit parameter provided", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":true,\"message\":\"Invalid 'limit' query parameter. Must be an integer.\"}");
        } catch (Exception e) {
            LOG.error("Error generating product listing response", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":true,\"message\":\"Unable to load product catalog.\"}");
        }
    }

    private int parseLimit(String limitParam) {
        if (StringUtils.isBlank(limitParam)) {
            return DEFAULT_LIMIT;
        }
        int parsed = Integer.parseInt(limitParam.trim());
        if (parsed <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(parsed, MAX_LIMIT);
    }
}

