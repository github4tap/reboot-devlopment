package com.adobe.aem.guides.wknd.core.models;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents an individual slide within the high-performance Carousel component.
 * Encapsulates responsive image parameters (srcset, sizes, loading, fetchpriority)
 * to prevent performance degradation from high-resolution (>4MB) DAM assets.
 */
public class CarouselSlide implements Serializable {

    private static final long serialVersionUID = 1L;

    private int index;
    private String imagePath;
    private String alt;
    private String title;
    private String description;
    private String linkUrl;
    private String buttonText;
    private String src;
    private String srcSet;
    private String sizes;
    private String loading;
    private String fetchPriority;
    private boolean active;
    private long originalSizeBytes;
    private boolean highResolution;

    public CarouselSlide() {
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAlt() {
        return StringUtils.isNotBlank(alt) ? alt : (StringUtils.isNotBlank(title) ? title : "Carousel Slide");
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getSrcSet() {
        return srcSet;
    }

    public void setSrcSet(String srcSet) {
        this.srcSet = srcSet;
    }

    public String getSizes() {
        return StringUtils.isNotBlank(sizes) ? sizes : "(max-width: 768px) 100vw, (max-width: 1200px) 100vw, 1920px";
    }

    public void setSizes(String sizes) {
        this.sizes = sizes;
    }

    public String getLoading() {
        return loading;
    }

    public void setLoading(String loading) {
        this.loading = loading;
    }

    public String getFetchPriority() {
        return fetchPriority;
    }

    public void setFetchPriority(String fetchPriority) {
        this.fetchPriority = fetchPriority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public void setOriginalSizeBytes(long originalSizeBytes) {
        this.originalSizeBytes = originalSizeBytes;
        this.highResolution = originalSizeBytes > (2 * 1024 * 1024); // > 2MB
    }

    public boolean isHighResolution() {
        return highResolution;
    }

    public String getFormattedOriginalSize() {
        if (originalSizeBytes <= 0) {
            return "N/A";
        }
        double mb = originalSizeBytes / (1024.0 * 1024.0);
        return String.format("%.2f MB", mb);
    }

    public boolean hasLink() {
        return StringUtils.isNotBlank(linkUrl);
    }

    public boolean hasCaption() {
        return StringUtils.isNotBlank(title) || StringUtils.isNotBlank(description) || hasLink();
    }
}
