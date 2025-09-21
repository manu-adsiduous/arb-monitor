package com.arbmonitor.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "scraped_ads")
public class ScrapedAd {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "meta_ad_id", unique = true, nullable = false)
    private String metaAdId;
    
    @Column(name = "domain_name", nullable = false)
    private String domainName;
    
    @Column(name = "page_name")
    private String pageName;
    
    @Column(name = "page_id")
    private String pageId;
    
    @Column(name = "primary_text", columnDefinition = "TEXT")
    private String primaryText;
    
    @Column(name = "headline", columnDefinition = "TEXT")
    private String headline;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "call_to_action")
    private String callToAction;
    
    @Column(name = "landing_page_url", columnDefinition = "TEXT")
    private String landingPageUrl;
    
    @Column(name = "display_url")
    private String displayUrl;
    
    @ElementCollection
    @CollectionTable(name = "scraped_ad_images", joinColumns = @JoinColumn(name = "ad_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> imageUrls;
    
    @ElementCollection
    @CollectionTable(name = "scraped_ad_videos", joinColumns = @JoinColumn(name = "ad_id"))
    @Column(name = "video_url", columnDefinition = "TEXT")
    private List<String> videoUrls;
    
    // Local file paths for downloaded media
    @ElementCollection
    @CollectionTable(name = "scraped_ad_local_images", joinColumns = @JoinColumn(name = "ad_id"))
    @Column(name = "local_image_path", columnDefinition = "TEXT")
    private List<String> localImagePaths;
    
    @ElementCollection
    @CollectionTable(name = "scraped_ad_local_videos", joinColumns = @JoinColumn(name = "ad_id"))
    @Column(name = "local_video_path", columnDefinition = "TEXT")
    private List<String> localVideoPaths;
    
    @Column(name = "ad_format")
    private String adFormat; // SINGLE_IMAGE, CAROUSEL, VIDEO, etc.
    
    // Pre-extracted text content (populated during scraping)
    @Column(name = "extracted_image_text", columnDefinition = "TEXT")
    private String extractedImageText; // OCR text from images
    
    @Column(name = "extracted_video_text", columnDefinition = "TEXT")
    private String extractedVideoText; // OCR text from video frames + audio transcript
    
    @Column(name = "referrer_ad_creative", columnDefinition = "TEXT")
    private String referrerAdCreative; // The 'kw' parameter from landing page
    
    @Column(name = "funding_entity")
    private String fundingEntity;
    
    @Column(name = "ad_creation_date")
    private LocalDateTime adCreationDate;
    
    @Column(name = "ad_delivery_start_date")
    private LocalDateTime adDeliveryStartDate;
    
    @Column(name = "ad_delivery_stop_date")
    private LocalDateTime adDeliveryStopDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "spend_range_lower")
    private Integer spendRangeLower;
    
    @Column(name = "spend_range_upper")
    private Integer spendRangeUpper;
    
    @Column(name = "impressions_range_lower")
    private Long impressionsRangeLower;
    
    @Column(name = "impressions_range_upper")
    private Long impressionsRangeUpper;
    
    @Column(name = "target_locations", columnDefinition = "TEXT")
    private String targetLocations; // JSON string
    
    @Column(name = "target_demographics", columnDefinition = "TEXT")
    private String targetDemographics; // JSON string
    
    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "scrape_source")
    private String scrapeSource; // META_AD_LIBRARY, etc.
    
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData; // JSON string of raw scraped data
    
    // Constructors
    public ScrapedAd() {
        this.scrapedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    public ScrapedAd(String metaAdId, String domainName) {
        this();
        this.metaAdId = metaAdId;
        this.domainName = domainName;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMetaAdId() {
        return metaAdId;
    }
    
    public void setMetaAdId(String metaAdId) {
        this.metaAdId = metaAdId;
    }
    
    public String getDomainName() {
        return domainName;
    }
    
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
    public String getPageName() {
        return pageName;
    }
    
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
    
    public String getPageId() {
        return pageId;
    }
    
    public void setPageId(String pageId) {
        this.pageId = pageId;
    }
    
    public String getPrimaryText() {
        return primaryText;
    }
    
    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }
    
    public String getHeadline() {
        return headline;
    }
    
    public void setHeadline(String headline) {
        this.headline = headline;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCallToAction() {
        return callToAction;
    }
    
    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }
    
    public String getLandingPageUrl() {
        return landingPageUrl;
    }
    
    public void setLandingPageUrl(String landingPageUrl) {
        this.landingPageUrl = landingPageUrl;
    }
    
    public String getDisplayUrl() {
        return displayUrl;
    }
    
    public void setDisplayUrl(String displayUrl) {
        this.displayUrl = displayUrl;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public List<String> getVideoUrls() {
        return videoUrls;
    }
    
    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
    }
    
    public List<String> getLocalImagePaths() {
        return localImagePaths;
    }
    
    public void setLocalImagePaths(List<String> localImagePaths) {
        this.localImagePaths = localImagePaths;
    }
    
    public List<String> getLocalVideoPaths() {
        return localVideoPaths;
    }
    
    public void setLocalVideoPaths(List<String> localVideoPaths) {
        this.localVideoPaths = localVideoPaths;
    }
    
        public String getAdFormat() {
        return adFormat;
    }

    public void setAdFormat(String adFormat) {
        this.adFormat = adFormat;
    }
    
    public String getExtractedImageText() {
        return extractedImageText;
    }

    public void setExtractedImageText(String extractedImageText) {
        this.extractedImageText = extractedImageText;
    }

    public String getExtractedVideoText() {
        return extractedVideoText;
    }

    public void setExtractedVideoText(String extractedVideoText) {
        this.extractedVideoText = extractedVideoText;
    }
    
    public String getReferrerAdCreative() {
        return referrerAdCreative;
    }

    public void setReferrerAdCreative(String referrerAdCreative) {
        this.referrerAdCreative = referrerAdCreative;
    }
    
    public String getFundingEntity() {
        return fundingEntity;
    }
    
    public void setFundingEntity(String fundingEntity) {
        this.fundingEntity = fundingEntity;
    }
    
    public LocalDateTime getAdCreationDate() {
        return adCreationDate;
    }
    
    public void setAdCreationDate(LocalDateTime adCreationDate) {
        this.adCreationDate = adCreationDate;
    }
    
    public LocalDateTime getAdDeliveryStartDate() {
        return adDeliveryStartDate;
    }
    
    public void setAdDeliveryStartDate(LocalDateTime adDeliveryStartDate) {
        this.adDeliveryStartDate = adDeliveryStartDate;
    }
    
    public LocalDateTime getAdDeliveryStopDate() {
        return adDeliveryStopDate;
    }
    
    public void setAdDeliveryStopDate(LocalDateTime adDeliveryStopDate) {
        this.adDeliveryStopDate = adDeliveryStopDate;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getSpendRangeLower() {
        return spendRangeLower;
    }
    
    public void setSpendRangeLower(Integer spendRangeLower) {
        this.spendRangeLower = spendRangeLower;
    }
    
    public Integer getSpendRangeUpper() {
        return spendRangeUpper;
    }
    
    public void setSpendRangeUpper(Integer spendRangeUpper) {
        this.spendRangeUpper = spendRangeUpper;
    }
    
    public Long getImpressionsRangeLower() {
        return impressionsRangeLower;
    }
    
    public void setImpressionsRangeLower(Long impressionsRangeLower) {
        this.impressionsRangeLower = impressionsRangeLower;
    }
    
    public Long getImpressionsRangeUpper() {
        return impressionsRangeUpper;
    }
    
    public void setImpressionsRangeUpper(Long impressionsRangeUpper) {
        this.impressionsRangeUpper = impressionsRangeUpper;
    }
    
    public String getTargetLocations() {
        return targetLocations;
    }
    
    public void setTargetLocations(String targetLocations) {
        this.targetLocations = targetLocations;
    }
    
    public String getTargetDemographics() {
        return targetDemographics;
    }
    
    public void setTargetDemographics(String targetDemographics) {
        this.targetDemographics = targetDemographics;
    }
    
    public LocalDateTime getScrapedAt() {
        return scrapedAt;
    }
    
    public void setScrapedAt(LocalDateTime scrapedAt) {
        this.scrapedAt = scrapedAt;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public String getScrapeSource() {
        return scrapeSource;
    }
    
    public void setScrapeSource(String scrapeSource) {
        this.scrapeSource = scrapeSource;
    }
    
    public String getRawData() {
        return rawData;
    }
    
    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}

