package com.arbmonitor.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a Meta Ad from the Ad Library API
 */
public class MetaAdDTO {
    
    private String id;
    
    @JsonProperty("ad_creative_bodies")
    private List<String> adCreativeBodies;
    
    @JsonProperty("ad_creative_link_captions")
    private List<String> adCreativeLinkCaptions;
    
    @JsonProperty("ad_creative_link_descriptions")
    private List<String> adCreativeLinkDescriptions;
    
    @JsonProperty("ad_creative_link_titles")
    private List<String> adCreativeLinkTitles;
    
    @JsonProperty("page_name")
    private String pageName;
    
    @JsonProperty("page_id")
    private String pageId;
    
    @JsonProperty("ad_creation_time")
    private String adCreationTime;
    
    @JsonProperty("ad_delivery_start_time")
    private String adDeliveryStartTime;
    
    @JsonProperty("ad_delivery_stop_time")
    private String adDeliveryStopTime;
    
    private Map<String, Object> impressions;
    
    private Map<String, Object> spend;
    
    private List<Map<String, Object>> images;
    
    @JsonProperty("ad_snapshot_url")
    private String adSnapshotUrl;
    
    // Constructors
    public MetaAdDTO() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public List<String> getAdCreativeBodies() {
        return adCreativeBodies;
    }
    
    public void setAdCreativeBodies(List<String> adCreativeBodies) {
        this.adCreativeBodies = adCreativeBodies;
    }
    
    public List<String> getAdCreativeLinkCaptions() {
        return adCreativeLinkCaptions;
    }
    
    public void setAdCreativeLinkCaptions(List<String> adCreativeLinkCaptions) {
        this.adCreativeLinkCaptions = adCreativeLinkCaptions;
    }
    
    public List<String> getAdCreativeLinkDescriptions() {
        return adCreativeLinkDescriptions;
    }
    
    public void setAdCreativeLinkDescriptions(List<String> adCreativeLinkDescriptions) {
        this.adCreativeLinkDescriptions = adCreativeLinkDescriptions;
    }
    
    public List<String> getAdCreativeLinkTitles() {
        return adCreativeLinkTitles;
    }
    
    public void setAdCreativeLinkTitles(List<String> adCreativeLinkTitles) {
        this.adCreativeLinkTitles = adCreativeLinkTitles;
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
    
    public String getAdCreationTime() {
        return adCreationTime;
    }
    
    public void setAdCreationTime(String adCreationTime) {
        this.adCreationTime = adCreationTime;
    }
    
    public String getAdDeliveryStartTime() {
        return adDeliveryStartTime;
    }
    
    public void setAdDeliveryStartTime(String adDeliveryStartTime) {
        this.adDeliveryStartTime = adDeliveryStartTime;
    }
    
    public String getAdDeliveryStopTime() {
        return adDeliveryStopTime;
    }
    
    public void setAdDeliveryStopTime(String adDeliveryStopTime) {
        this.adDeliveryStopTime = adDeliveryStopTime;
    }
    
    public Map<String, Object> getImpressions() {
        return impressions;
    }
    
    public void setImpressions(Map<String, Object> impressions) {
        this.impressions = impressions;
    }
    
    public Map<String, Object> getSpend() {
        return spend;
    }
    
    public void setSpend(Map<String, Object> spend) {
        this.spend = spend;
    }
    
    public List<Map<String, Object>> getImages() {
        return images;
    }
    
    public void setImages(List<Map<String, Object>> images) {
        this.images = images;
    }
    
    public String getAdSnapshotUrl() {
        return adSnapshotUrl;
    }
    
    public void setAdSnapshotUrl(String adSnapshotUrl) {
        this.adSnapshotUrl = adSnapshotUrl;
    }
    
    // Helper methods
    public String getPrimaryText() {
        if (adCreativeBodies != null && !adCreativeBodies.isEmpty()) {
            return adCreativeBodies.get(0);
        }
        return null;
    }
    
    public String getHeadline() {
        if (adCreativeLinkTitles != null && !adCreativeLinkTitles.isEmpty()) {
            return adCreativeLinkTitles.get(0);
        }
        return null;
    }
    
    public String getDescription() {
        if (adCreativeLinkDescriptions != null && !adCreativeLinkDescriptions.isEmpty()) {
            return adCreativeLinkDescriptions.get(0);
        }
        return null;
    }
    
    public String getImageUrl() {
        if (images != null && !images.isEmpty()) {
            Map<String, Object> firstImage = images.get(0);
            if (firstImage.containsKey("original_image_url")) {
                return (String) firstImage.get("original_image_url");
            }
        }
        return null;
    }
}



