package com.adobe.aem.guides.wknd.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

/**
 * Sling Model for the WKND Custom High-Performance Carousel Component.
 *
 * <p>Addresses enterprise performance and UX requirements:</p>
 * <ul>
 *   <li><b>Defensive authoring limit:</b> Enforces a strict maximum of 8 slides to protect
 *       DOM performance, user attention, and Core Web Vitals (CWV).</li>
 *   <li><b>High-resolution asset mitigation (&gt;4 MB):</b> Prevents downloading multiple 4MB
 *       originals by configuring adaptive responsive breakpoints (srcset), modern web renditions,
 *       and strategic lazy loading (Slide 1 eager with high fetchpriority; Slides 2..N lazy).</li>
 *   <li><b>Cumulative Layout Shift (CLS) prevention:</b> Emits CSS aspect-ratio reservations.</li>
 * </ul>
 */
@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
        resourceType = "wknd/components/carousel"
)
public class CustomCarouselModel {

    private static final Logger LOG = LoggerFactory.getLogger(CustomCarouselModel.class);

    /**
     * Absolute recommended maximum number of slides in a carousel
     * based on Nielsen Norman Group UX research and Core Web Vitals budget.
     */
    public static final int ABSOLUTE_MAX_SLIDES = 8;
    public static final int DEFAULT_MAX_SLIDES = 6;

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
        // Look for standard AEM web renditions first
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

        // If specific named renditions do not exist, utilize AEM core image responsive selector URLs
        if (srcSetEntries.isEmpty()) {
            return imagePath + ".coreimg.80.480.jpeg 480w, "
                 + imagePath + ".coreimg.80.768.jpeg 768w, "
                 + imagePath + ".coreimg.80.1200.jpeg 1200w, "
                 + imagePath + ".coreimg.80.1600.jpeg 1600w, "
                 + imagePath + " 1920w";
        }

        return String.join(", ", srcSetEntries);
    }

    public List<CarouselSlide> getSlides() {
        return slides != null ? Collections.unmodifiableList(slides) : Collections.emptyList();
    }

    public int getSlideCount() {
        return slides != null ? slides.size() : 0;
    }

    public boolean isEmpty() {
        return slides == null || slides.isEmpty();
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public Long getDelay() {
        return delay;
    }

    public boolean isAutogpauseDisabled() {
        return autogpauseDisabled;
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

    public String getId() {
        return id;
    }

    public int getMaxAllowedSlides() {
        return maxAllowedSlides;
    }

    public boolean isExceedingRecommendedLimit() {
        return exceedingRecommendedLimit;
    }
}
