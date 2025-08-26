package com.arbmonitor.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the response from Meta Ad Library API
 */
public class MetaAdLibraryResponseDTO {
    
    private List<MetaAdDTO> data;
    
    private Map<String, Object> paging;
    
    // Constructors
    public MetaAdLibraryResponseDTO() {}
    
    public MetaAdLibraryResponseDTO(List<MetaAdDTO> data, Map<String, Object> paging) {
        this.data = data;
        this.paging = paging;
    }
    
    // Getters and Setters
    public List<MetaAdDTO> getData() {
        return data;
    }
    
    public void setData(List<MetaAdDTO> data) {
        this.data = data;
    }
    
    public Map<String, Object> getPaging() {
        return paging;
    }
    
    public void setPaging(Map<String, Object> paging) {
        this.paging = paging;
    }
    
    // Helper methods
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }
    
    public int getDataCount() {
        return data != null ? data.size() : 0;
    }
    
    public boolean hasNextPage() {
        if (paging != null && paging.containsKey("next")) {
            return paging.get("next") != null;
        }
        return false;
    }
    
    public String getNextPageUrl() {
        if (hasNextPage()) {
            return (String) paging.get("next");
        }
        return null;
    }
}



