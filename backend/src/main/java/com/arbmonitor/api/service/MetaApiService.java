package com.arbmonitor.api.service;

import com.arbmonitor.api.dto.MetaAdDTO;
import com.arbmonitor.api.dto.MetaAdLibraryResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Collections;

@Service
public class MetaApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetaApiService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${meta.api.base-url:https://graph.facebook.com/v18.0}")
    private String metaApiBaseUrl;
    
    public MetaApiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com/v18.0")
            .build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * Validates a Meta Ad Library API access token
     */
    public Mono<Boolean> validateApiKey(String accessToken) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/ads_archive")
                .queryParam("access_token", accessToken)
                .queryParam("search_terms", "test")
                .queryParam("ad_reached_countries", "US")
                .queryParam("limit", "1")
                .build())
            .retrieve()
            .onStatus(HttpStatus.UNAUTHORIZED::equals, response -> {
                logger.warn("Invalid Meta API access token provided");
                return Mono.error(new RuntimeException("Invalid access token"));
            })
            .onStatus(HttpStatus.FORBIDDEN::equals, response -> {
                logger.warn("Meta API access token has insufficient permissions");
                return Mono.error(new RuntimeException("Insufficient permissions"));
            })
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .map(response -> {
                // If we get a successful response, the token is valid
                logger.info("Meta API access token validated successfully");
                return true;
            })
            .onErrorReturn(false);
    }
    
    /**
     * Fetches ads for a specific domain from Meta Ad Library
     */
    public Mono<List<MetaAdDTO>> fetchAdsForDomain(String accessToken, String domain) {
        return fetchAdsForDomain(accessToken, domain, 50);
    }
    
    /**
     * Fetches ads for a specific domain from Meta Ad Library with custom limit
     */
    public Mono<List<MetaAdDTO>> fetchAdsForDomain(String accessToken, String domain, int limit) {
        logger.info("Fetching ads for domain: {} with limit: {}", domain, limit);
        
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/ads_archive")
                .queryParam("access_token", accessToken)
                .queryParam("search_terms", domain)
                .queryParam("ad_reached_countries", "US")
                .queryParam("ad_active_status", "ALL")
                .queryParam("limit", Math.min(limit, 1000)) // Meta API max limit
                .queryParam("fields", "id,ad_creative_bodies,ad_creative_link_captions,ad_creative_link_descriptions,ad_creative_link_titles,images,page_name,page_id,ad_creation_time,ad_delivery_start_time,ad_delivery_stop_time,impressions,spend,ad_snapshot_url")
                .build())
            .retrieve()
            .onStatus(status -> status.isError(), response -> {
                logger.error("Meta API returned error status: {}", response.statusCode());
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        logger.error("Meta API error response: {}", errorBody);
                        return Mono.error(new RuntimeException("Meta API error: " + errorBody));
                    });
            })
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .flatMap(this::parseAdLibraryResponse)
            .doOnSuccess(ads -> logger.info("Successfully fetched {} ads for domain: {}", ads.size(), domain))
            .doOnError(error -> logger.error("Error fetching ads for domain: {}", domain, error));
    }
    
    /**
     * Searches for ads by page name
     */
    public Mono<List<MetaAdDTO>> searchAdsByPageName(String accessToken, String pageName) {
        return searchAdsByPageName(accessToken, pageName, 50);
    }
    
    /**
     * Searches for ads by page name with custom limit
     */
    public Mono<List<MetaAdDTO>> searchAdsByPageName(String accessToken, String pageName, int limit) {
        logger.info("Searching ads by page name: {} with limit: {}", pageName, limit);
        
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/ads_archive")
                .queryParam("access_token", accessToken)
                .queryParam("search_page_ids", pageName)
                .queryParam("ad_reached_countries", "US")
                .queryParam("ad_active_status", "ALL")
                .queryParam("limit", Math.min(limit, 1000))
                .queryParam("fields", "id,ad_creative_bodies,ad_creative_link_captions,ad_creative_link_descriptions,ad_creative_link_titles,images,page_name,page_id,ad_creation_time,ad_delivery_start_time,ad_delivery_stop_time,impressions,spend,ad_snapshot_url")
                .build())
            .retrieve()
            .onStatus(status -> status.isError(), response -> {
                logger.error("Meta API returned error status: {}", response.statusCode());
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        logger.error("Meta API error response: {}", errorBody);
                        return Mono.error(new RuntimeException("Meta API error: " + errorBody));
                    });
            })
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .flatMap(this::parseAdLibraryResponse)
            .doOnSuccess(ads -> logger.info("Successfully found {} ads for page: {}", ads.size(), pageName))
            .doOnError(error -> logger.error("Error searching ads for page: {}", pageName, error));
    }
    
    /**
     * Parses the raw JSON response from Meta Ad Library API
     */
    private Mono<List<MetaAdDTO>> parseAdLibraryResponse(String jsonResponse) {
        try {
            MetaAdLibraryResponseDTO response = objectMapper.readValue(jsonResponse, MetaAdLibraryResponseDTO.class);
            
            if (response.hasData()) {
                logger.debug("Parsed {} ads from Meta API response", response.getDataCount());
                return Mono.just(response.getData());
            } else {
                logger.info("No ads found in Meta API response");
                return Mono.just(Collections.emptyList());
            }
            
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Meta API response: {}", e.getMessage());
            logger.debug("Raw response: {}", jsonResponse);
            return Mono.error(new RuntimeException("Failed to parse Meta API response", e));
        }
    }
    
    /**
     * Tests the Meta API connection with a simple request
     */
    public Mono<Boolean> testConnection(String accessToken) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/me")
                .queryParam("access_token", accessToken)
                .queryParam("fields", "id,name")
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .map(response -> {
                logger.info("Meta API connection test successful");
                return true;
            })
            .doOnError(throwable -> logger.warn("Meta API connection test failed: {}", throwable.getMessage()))
            .onErrorReturn(false);
    }
}
