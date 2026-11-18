package com.adobe.aem.guides.wknd.core.models;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.wknd.core.testcontext.AppAemContext;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class CustomCarouselModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    private Page page;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(CustomCarouselModel.class, CarouselSlide.class);
        page = context.create().page("/content/wknd/en/home");

        // Mock a DAM asset structure in JCR (4.5 MB original size)
        Resource asset = context.create().resource("/content/dam/wknd/hero-large.jpg",
                "jcr:primaryType", "dam:Asset");
        Resource original = context.create().resource(asset, "jcr:content/renditions/original",
                "jcr:primaryType", "nt:file");
        context.create().resource(original, "jcr:content",
                "jcr:primaryType", "nt:resource",
                "jcr:dataSize", 4718592L); // 4.5 MB in bytes

        // Mock web rendition
        context.create().resource(asset, "jcr:content/renditions/cq5dam.web.1280.1280.jpeg",
                "jcr:primaryType", "nt:file");
    }

    @Test
    void testCarouselWithAuthoredSlidesAndPerformanceOptimization() {
        Resource carouselRes = context.create().resource(page, "carousel",
                "sling:resourceType", "wknd/components/carousel",
                "autoplay", true,
                "delay", 6000L,
                "aspectRatio", "16:9",
                "maxAllowedSlides", 6);

        // Slide 1 (Eager / LCP candidate with high-res 4.5MB asset)
        context.create().resource(carouselRes, "slides/slide1",
                "imagePath", "/content/dam/wknd/hero-large.jpg",
                "title", "Summer Adventures",
                "description", "Discover your next summit",
                "alt", "Mountain landscape",
                "linkUrl", "/content/wknd/en/adventures",
                "buttonText", "Explore");

        // Slide 2 (Lazy loaded)
        context.create().resource(carouselRes, "slides/slide2",
                "imagePath", "/content/dam/wknd/slide2.jpg",
                "title", "Surf Camp",
                "alt", "Ocean waves");

        // Slide 3 (Lazy loaded)
        context.create().resource(carouselRes, "slides/slide3",
                "imagePath", "/content/dam/wknd/slide3.jpg",
                "title", "Desert Trek",
                "alt", "Sand dunes");

        context.currentResource(carouselRes);
        CustomCarouselModel model = carouselRes.adaptTo(CustomCarouselModel.class);

        assertNotNull(model);
        assertFalse(model.isEmpty());
        assertEquals(3, model.getSlideCount());
        assertTrue(model.isAutoplay());
        assertEquals(6000L, model.getDelay());
        assertEquals("16:9", model.getAspectRatio());
        assertEquals("aspect-ratio: 16 / 9;", model.getAspectRatioStyle());
        assertFalse(model.isExceedingRecommendedLimit());

        // Check Slide 1: Eager loading & fetchpriority high
        CarouselSlide slide0 = model.getSlides().get(0);
        assertEquals(0, slide0.getIndex());
        assertTrue(slide0.isActive());
        assertEquals("eager", slide0.getLoading());
        assertEquals("high", slide0.getFetchPriority());
        assertEquals("Mountain landscape", slide0.getAlt());
        assertEquals("Summer Adventures", slide0.getTitle());
        assertTrue(slide0.hasLink());
        assertTrue(slide0.hasCaption());
        assertTrue(slide0.isHighResolution(), "Asset > 2MB should be detected as high resolution");
        assertEquals("4.50 MB", slide0.getFormattedOriginalSize());
        assertEquals("/content/dam/wknd/hero-large.jpg/jcr:content/renditions/cq5dam.web.1280.1280.jpeg", slide0.getSrc());

        // Check Slide 2: Lazy loading & fetchpriority low
        CarouselSlide slide1 = model.getSlides().get(1);
        assertEquals(1, slide1.getIndex());
        assertFalse(slide1.isActive());
        assertEquals("lazy", slide1.getLoading());
        assertEquals("low", slide1.getFetchPriority());
        assertEquals("/content/dam/wknd/slide2.jpg", slide1.getSrc());
        assertNotNull(slide1.getSrcSet());
        assertTrue(slide1.getSrcSet().contains("480w"));
    }

    @Test
    void testDefensiveMaxSlidesEnforcement() {
        Resource carouselRes = context.create().resource(page, "carousel-large",
                "sling:resourceType", "wknd/components/carousel",
                "maxAllowedSlides", 6);

        for (int i = 1; i <= 12; i++) {
            context.create().resource(carouselRes, "slides/slide" + i,
                    "imagePath", "/content/dam/wknd/image" + i + ".jpg",
                    "title", "Slide " + i);
        }

        context.currentResource(carouselRes);
        CustomCarouselModel model = carouselRes.adaptTo(CustomCarouselModel.class);

        assertNotNull(model);
        assertTrue(model.isExceedingRecommendedLimit(), "Should flag that author exceeded recommended limit");
        assertEquals(6, model.getSlideCount(), "Should defensively cap slides to maxAllowedSlides");
    }

    @Test
    void testEmptyCarouselReturnsEmpty() {
        Resource emptyCarousel = context.create().resource(page, "carousel-empty",
                "sling:resourceType", "wknd/components/carousel");

        context.currentResource(emptyCarousel);
        CustomCarouselModel model = emptyCarousel.adaptTo(CustomCarouselModel.class);

        assertNotNull(model);
        assertTrue(model.isEmpty());
        assertEquals(0, model.getSlideCount());
    }
}
