package com.adobe.aem.guides.wknd.core.models;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.wknd.core.dto.Product;
import com.adobe.aem.guides.wknd.core.services.ProductListingService;

/**
 * Sling Model for the Product Listing Component.
 *
 * Exposes authored component configuration, grid dimensions, endpoint details,
 * and optional initial product data for progressive enhancement.
 */
@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
        resourceType = "wknd/components/productlisting"
)
public class ProductListingModel {

    private static final Logger LOG = LoggerFactory.getLogger(ProductListingModel.class);
    private static final String DEFAULT_TITLE = "Product Listing Page using Html/CSS/JS";
    private static final String DEFAULT_ENDPOINT = "/bin/wknd/products";
    private static final int DEFAULT_LIMIT = 9;
    private static final int DEFAULT_COLUMNS = 3;

    @ValueMapValue
    @Default(values = DEFAULT_TITLE)
    private String title;

    @ValueMapValue
    @Default(values = "3x3")
    private String gridPreset;

    @ValueMapValue
    @Default(values = "osgi")
    private String sourceMode;

    @ValueMapValue
    @Default(values = DEFAULT_ENDPOINT)
    private String apiEndpoint;

    @ValueMapValue
    @Default(intValues = DEFAULT_LIMIT)
    private int limit;

    @ValueMapValue
    @Default(intValues = DEFAULT_COLUMNS)
    private int columns;

    @OSGiService
    private ProductListingService productListingService;

    private List<Product> initialProducts;

    @PostConstruct
    protected void init() {
        if (StringUtils.isBlank(title)) {
            title = DEFAULT_TITLE;
        }

        // Apply Grid Preset presets if configured
        if ("3x3".equalsIgnoreCase(gridPreset)) {
            this.columns = 3;
            this.limit = 9;
        } else if ("3x2".equalsIgnoreCase(gridPreset)) {
            this.columns = 3;
            this.limit = 6;
        } else if ("2x2".equalsIgnoreCase(gridPreset)) {
            this.columns = 2;
            this.limit = 4;
        } else if ("3x1".equalsIgnoreCase(gridPreset)) {
            this.columns = 3;
            this.limit = 3;
        } else {
            // "custom" or unspecified
            if (this.columns <= 0) {
                this.columns = DEFAULT_COLUMNS;
            }
            if (this.limit <= 0) {
                this.limit = DEFAULT_LIMIT;
            }
        }

        // Apply Data Source Strategy
        if ("osgi".equalsIgnoreCase(sourceMode) || StringUtils.isBlank(apiEndpoint)) {
            this.apiEndpoint = DEFAULT_ENDPOINT;
        }

        // Optional server-side prefetch via OSGi Service
        if (productListingService != null) {
            try {
                this.initialProducts = productListingService.getProducts(limit);
            } catch (Exception e) {
                LOG.error("Failed to load initial products in model init", e);
                this.initialProducts = Collections.emptyList();
            }
        } else {
            this.initialProducts = Collections.emptyList();
        }
    }

    public String getTitle() {
        return title;
    }

    public String getGridPreset() {
        return gridPreset;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public int getLimit() {
        return limit;
    }

    public int getColumns() {
        return columns;
    }

    public List<Product> getInitialProducts() {
        return initialProducts != null ? Collections.unmodifiableList(initialProducts) : Collections.emptyList();
    }

    public boolean hasProducts() {
        return initialProducts != null && !initialProducts.isEmpty();
    }

    public String getGridStyle() {
        return String.format("grid-template-columns: repeat(%d, 1fr);", columns > 0 ? columns : 3);
    }
}
