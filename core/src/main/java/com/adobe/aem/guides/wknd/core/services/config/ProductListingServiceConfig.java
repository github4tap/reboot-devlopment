package com.adobe.aem.guides.wknd.core.services.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi Configuration definition for ProductListingService.
 */
@ObjectClassDefinition(
        name = "WKND Product Listing Service Configuration",
        description = "Configuration for Product Listing API endpoint, limits, and timeouts"
)
public @interface ProductListingServiceConfig {

    @AttributeDefinition(
            name = "API Endpoint URL",
            description = "External or internal endpoint URL for retrieving product JSON. If blank, default seed data will be used.",
            type = AttributeType.STRING
    )
    String apiEndpointUrl() default "";

    @AttributeDefinition(
            name = "Default Product Limit",
            description = "Default number of products to return if not specified in request (e.g. 9 for 3x3 grid)",
            type = AttributeType.INTEGER
    )
    int defaultLimit() default 9;

    @AttributeDefinition(
            name = "Connection Timeout (ms)",
            description = "Connection timeout in milliseconds when hitting external endpoints",
            type = AttributeType.INTEGER
    )
    int connectionTimeoutMs() default 5000;

    @AttributeDefinition(
            name = "Read Timeout (ms)",
            description = "Read timeout in milliseconds when hitting external endpoints",
            type = AttributeType.INTEGER
    )
    int readTimeoutMs() default 5000;
}

