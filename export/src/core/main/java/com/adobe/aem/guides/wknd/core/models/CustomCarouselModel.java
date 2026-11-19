package com.adobe.aem.guides.wknd.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.Carousel;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.PanelContainerItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

/**
 * Sling Model for the WKND Custom High-Performance Carousel Component.
 *
 * <p>Implements the Adobe Core Components <b>Sling Model Delegation Pattern</b>:
 * extends the Core Carousel ({@code core/wcm/components/carousel/v1/carousel}) using
 * {@code @Self @Via(type = ResourceSuperType.class) private Carousel delegate;}.</p>
 *
 * <p>Key architectural benefits & enterprise requirements addressed:</p>
 * <ul>
 *   <li><b>Sling Model Delegation:</b> Delegates standard WCM Core Carousel functionality
 *       (accessibility labels, data layer, SPA JSON export, active item tracking) to the underlying
 *       Core Component model, avoiding re-inventing the wheel.</li>
 *   <li><b>Defensive slide limit:</b> Enforces an authored maximum (default 6, hard ceiling 8)
 *       to prevent DOM bloat, user attention fatigue, and Core Web Vitals (CWV) degradation.</li>
 *   <li><b>High-resolution asset mitigation (&gt;4 MB):</b> Prevents downloading multiple 4MB
 *       originals by configuring adaptive responsive breakpoints (srcset), modern web renditions,
 *       and strategic loading (Slide 1 eager with fetchpriority="high"; Slides 2..N lazy).</li>
 *   <li><b>Cumulative Layout Shift (CLS) prevention:</b> Emits CSS aspect-ratio reservation styles.</li>
 * </ul>
 */
@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        adapters = { Carousel.class, ComponentExporter.class, CustomCarouselModel.class },
        resourceType = "wknd/components/carousel",
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class CustomCarouselModel implements Carousel {

    private static final Logger LOG = LoggerFactory.getLogger(CustomCarouselModel.class);

    /**
     * Absolute recommended maximum number of slides in a carousel
     * based on Nielsen Norman Group UX research and Core Web Vitals budget.
     */
    public static final int ABSOLUTE_MAX_SLIDES = 8;
    public static final int DEFAULT_MAX_SLIDES = 6;

    /**
     * Core Component Sling Model Delegation.
     * Injects the underlying Core Carousel implementation using ResourceSuperType.
     */
    @Self
    @Via(type = ResourceSuperType.class)
    private Carousel delegate;

    @ValueMapValue
    @Default(booleanValues = true)
    private boolean autoplay;

    @ValueMapValue
    @Default(longValues = 5000L)
    private Long delay;

    @ValueMapValue
    @Default(booleanValues = false)
    private boolean autogpauseDisabled;

    @ValueMapValue
    @Default(values = "16:9")
    private String aspectRatio;

    @ValueMapValue
    @Default(intValues = DEFAULT_MAX_SLIDES)
    private int maxAllowedSlides;

    @ValueMapValue
    private String accessibilityLabel;

    @ChildResource(name = "slides")
    private List<Resource> slideResources;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource currentResource;

    private List<CarouselSlide> slides;
    private String id;
    private boolean exceedingRecommendedLimit;

    @PostConstruct
    protected void init() {
        if (delay == null || delay < 1000L) {
            delay = 5000L;
        }

        int effectiveMax = (maxAllowedSlides > 0 && maxAllowedSlides <= ABSOLUTE_MAX_SLIDES)
                ? maxAllowedSlides
                : ABSOLUTE_MAX_SLIDES;

        this.id = "carousel-" + (currentResource != null ? Math.abs(currentResource.getPath().hashCode()) : UUID.randomUUID().toString().substring(0, 8));

        this.slides = new ArrayList<>();

        if (slideResources != null && !slideResources.isEmpty()) {
            if (slideResources.size() > effectiveMax) {
                this.exceedingRecommendedLimit = true;
                LOG.warn("Carousel at {} authored with {} slides, capping to {} for performance budget.",
                        currentResource != null ? currentResource.getPath() : "unknown",
                        slideResources.size(), effectiveMax);
            }

            int index = 0;
            for (Resource slideRes : slideResources) {
                if (index >= effectiveMax) {
                    break;
                }

                ValueMap vm = slideRes.getValueMap();
                String imagePath = vm.get("imagePath", String.class);

                if (StringUtils.isNotBlank(imagePath)) {
                    CarouselSlide slide = buildSlide(vm, imagePath, index);
                    slides.add(slide);
                    index++;
                }
            }
        }
    }

    private CarouselSlide buildSlide(ValueMap vm, String imagePath, int index) {
        CarouselSlide slide = new CarouselSlide();
        slide.setIndex(index);
        slide.setImagePath(imagePath);
        slide.setAlt(vm.get("alt", String.class));
        slide.setTitle(vm.get("title", String.class));
        slide.setDescription(vm.get("description", String.class));
        slide.setLinkUrl(vm.get("linkUrl", String.class));
        slide.setButtonText(vm.get("buttonText", String.class));

        // ── Performance Mitigation: Eager vs Lazy Loading Strategy ────────
        // Slide 0 is the Largest Contentful Paint (LCP) candidate:
        // Must load eagerly with high priority to achieve green LCP score.
        // Slides 1..N must load lazily to avoid downloading 4MB+ assets on initial page load.
        if (index == 0) {
            slide.setActive(true);
            slide.setLoading("eager");
            slide.setFetchPriority("high");
        } else {
            slide.setActive(false);
            slide.setLoading("lazy");
            slide.setFetchPriority("low");
        }

        // ── Asset Resolution & Responsive Web Optimization ───────────────
        resolveAssetPerformance(slide, imagePath);

        return slide;
    }

    private void resolveAssetPerformance(CarouselSlide slide, String imagePath) {
        if (resourceResolver == null || StringUtils.isBlank(imagePath)) {
            slide.setSrc(imagePath);
            return;
        }

        Resource assetResource = resourceResolver.getResource(imagePath);
        if (assetResource != null) {
            // Check direct child renditions in JCR
            Resource web1280Res = assetResource.getChild("jcr:content/renditions/cq5dam.web.1280.1280.jpeg");
            Resource originalRes = assetResource.getChild("jcr:content/renditions/original");

            if (originalRes != null) {
                Resource originalContent = originalRes.getChild("jcr:content");
                if (originalContent != null) {
                    ValueMap vm = originalContent.getValueMap();
                    Long size = vm.get("jcr:dataSize", Long.class);
                    if (size != null) {
                        slide.setOriginalSizeBytes(size);
                    }
                }
            }

            Asset asset = assetResource.adaptTo(Asset.class);
            if (asset != null) {
                Rendition original = asset.getOriginal();
                if (original != null && slide.getOriginalSizeBytes() <= 0) {
                    slide.setOriginalSizeBytes(original.getSize());
                }

                Rendition web1280 = asset.getRendition("cq5dam.web.1280.1280.jpeg");
                if (web1280 != null) {
                    slide.setSrc(web1280.getPath());
                } else if (web1280Res != null) {
                    slide.setSrc(web1280Res.getPath());
                } else {
                    slide.setSrc(imagePath);
                }

                String srcSet = buildResponsiveSrcSet(asset, imagePath);
                slide.setSrcSet(srcSet);
                return;
            } else if (web1280Res != null) {
                slide.setSrc(web1280Res.getPath());
                slide.setSrcSet(web1280Res.getPath() + " 1280w");
                return;
            }
        }

        slide.setSrc(imagePath);
        slide.setSrcSet(imagePath + ".coreimg.80.480.jpeg 480w, "
                      + imagePath + ".coreimg.80.768.jpeg 768w, "
                      + imagePath + ".coreimg.80.1200.jpeg 1200w, "
                      + imagePath + ".coreimg.80.1600.jpeg 1600w, "
                      + imagePath + " 1920w");
    }

    private String buildResponsiveSrcSet(Asset asset, String imagePath) {
        List<String> srcSetEntries = new ArrayList<>();

        Rendition web480 = asset.getRendition("cq5dam.web.480.480.jpeg");
        Rendition web768 = asset.getRendition("cq5dam.web.768.768.jpeg");
        Rendition web1280 = asset.getRendition("cq5dam.web.1280.1280.jpeg");

        if (web480 != null) {
            srcSetEntries.add(web480.getPath() + " 480w");
        }
        if (web768 != null) {
            srcSetEntries.add(web768.getPath() + " 768w");
        }
        if (web1280 != null) {
            srcSetEntries.add(web1280.getPath() + " 1280w");
        }

        if (srcSetEntries.isEmpty()) {
            return imagePath + ".coreimg.80.480.jpeg 480w, "
                 + imagePath + ".coreimg.80.768.jpeg 768w, "
                 + imagePath + ".coreimg.80.1200.jpeg 1200w, "
                 + imagePath + ".coreimg.80.1600.jpeg 1600w, "
                 + imagePath + " 1920w";
        }

        return String.join(", ", srcSetEntries);
    }

    // =========================================================================
    // Core Carousel Delegated / Overridden Methods
    // =========================================================================

    @Override
    public boolean getAutoplay() {
        if (delegate != null) {
            return delegate.getAutoplay();
        }
        return autoplay;
    }

    public boolean isAutoplay() {
        return getAutoplay();
    }

    @Override
    public Long getDelay() {
        if (delegate != null && delegate.getDelay() != null) {
            return delegate.getDelay();
        }
        return delay != null ? delay : 5000L;
    }

    @Override
    public boolean getAutopauseDisabled() {
        if (delegate != null) {
            return delegate.getAutopauseDisabled();
        }
        return autogpauseDisabled;
    }

    public boolean isAutogpauseDisabled() {
        return getAutopauseDisabled();
    }

    @Override
    public String getAccessibilityLabel() {
        if (delegate != null && StringUtils.isNotBlank(delegate.getAccessibilityLabel())) {
            return delegate.getAccessibilityLabel();
        }
        return accessibilityLabel;
    }

    @Override
    public String getAccessibilityPrevious() {
        return delegate != null ? delegate.getAccessibilityPrevious() : null;
    }

    @Override
    public String getAccessibilityNext() {
        return delegate != null ? delegate.getAccessibilityNext() : null;
    }

    @Override
    public String getAccessibilityPlay() {
        return delegate != null ? delegate.getAccessibilityPlay() : null;
    }

    @Override
    public String getAccessibilityPause() {
        return delegate != null ? delegate.getAccessibilityPause() : null;
    }

    @Override
    public String getAccessibilityTablist() {
        return delegate != null ? delegate.getAccessibilityTablist() : null;
    }

    @Override
    public boolean getAccessibilityAutoItemTitles() {
        return delegate != null && delegate.getAccessibilityAutoItemTitles();
    }

    @Override
    public boolean isControlsPrepended() {
        return delegate != null && delegate.isControlsPrepended();
    }

    @Override
    public String getActiveItem() {
        return delegate != null ? delegate.getActiveItem() : null;
    }

    @Override
    public List<ListItem> getItems() {
        return delegate != null ? delegate.getItems() : Collections.emptyList();
    }

    @Override
    public List<? extends PanelContainerItem> getChildren() {
        return delegate != null ? delegate.getChildren() : Collections.emptyList();
    }

    @Override
    public String getBackgroundStyle() {
        return delegate != null ? delegate.getBackgroundStyle() : null;
    }

    @Override
    public Map<String, ? extends ComponentExporter> getExportedItems() {
        return delegate != null ? delegate.getExportedItems() : Collections.emptyMap();
    }

    @Override
    public String[] getExportedItemsOrder() {
        return delegate != null ? delegate.getExportedItemsOrder() : new String[0];
    }

    @Override
    public String getId() {
        if (delegate != null && StringUtils.isNotBlank(delegate.getId())) {
            return delegate.getId();
        }
        return this.id;
    }

    @Override
    public String getExportedType() {
        if (delegate != null && StringUtils.isNotBlank(delegate.getExportedType())) {
            return delegate.getExportedType();
        }
        return "wknd/components/carousel";
    }

    @Override
    public ComponentData getData() {
        return delegate != null ? delegate.getData() : null;
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate != null ? delegate.getAppliedCssClasses() : null;
    }

    public Carousel getDelegate() {
        return delegate;
    }

    // =========================================================================
    // Custom Performance & Layout Methods
    // =========================================================================

    public List<CarouselSlide> getSlides() {
        return slides != null ? Collections.unmodifiableList(slides) : Collections.emptyList();
    }

    public int getSlideCount() {
        return slides != null ? slides.size() : 0;
    }

    public boolean isEmpty() {
        return slides == null || slides.isEmpty();
    }

    public String getAspectRatio() {
        return StringUtils.isNotBlank(aspectRatio) ? aspectRatio : "16:9";
    }

    public String getAspectRatioStyle() {
        if ("21:9".equalsIgnoreCase(aspectRatio)) {
            return "aspect-ratio: 21 / 9;";
        } else if ("4:3".equalsIgnoreCase(aspectRatio)) {
            return "aspect-ratio: 4 / 3;";
        } else if ("1:1".equalsIgnoreCase(aspectRatio)) {
            return "aspect-ratio: 1 / 1;";
        }
        return "aspect-ratio: 16 / 9;";
    }

    public int getMaxAllowedSlides() {
        return maxAllowedSlides;
    }

    public boolean isExceedingRecommendedLimit() {
        return exceedingRecommendedLimit;
    }
}
