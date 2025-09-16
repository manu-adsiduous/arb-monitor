package com.arbmonitor.api.service;

import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.repository.ScrapedAdRepository;
import com.arbmonitor.api.repository.DomainRepository;
import com.arbmonitor.api.repository.AdAnalysisRepository;
import com.arbmonitor.api.service.MediaStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApifyScrapingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApifyScrapingService.class);
    
    @Autowired
    private ScrapedAdRepository scrapedAdRepository;
    
    @Autowired
    private DomainRepository domainRepository;
    
    @Autowired
    private AdAnalysisRepository adAnalysisRepository;
    
    @Autowired
    private MediaStorageService mediaStorageService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;
    
    @Autowired
    private MediaProcessingService mediaProcessingService;
    
    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.apify.token:}")
    private String apifyToken;
    
    @Value("${app.apify.results-limit:0}")
    private int resultsLimit;
    
    private static final String APIFY_API_BASE = "https://api.apify.com/v2";
    private static final String FACEBOOK_ADS_SCRAPER_ID = "apify~facebook-ads-scraper";
    
    /**
     * Update domain processing status
     */
    private void updateDomainProcessingStatus(String domainName, Domain.ProcessingStatus status, String message) {
        try {
            Domain domain = domainRepository.findByDomainName(domainName);
            if (domain != null) {
                domain.setProcessingStatus(status);
                domain.setProcessingMessage(message);
                domainRepository.save(domain);
                logger.info("Updated domain {} processing status to: {} - {}", domainName, status, message);
            }
        } catch (Exception e) {
            logger.error("Error updating domain processing status for {}: {}", domainName, e.getMessage());
        }
    }
    
    // Track active scraping tasks
    private final Map<String, CompletableFuture<String>> activeScrapingTasks = new ConcurrentHashMap<>();
    
    /**
     * Stop scraping for a domain
     */
    public void stopScrapingForDomain(String domainName) {
        CompletableFuture<String> task = activeScrapingTasks.get(domainName);
        if (task != null && !task.isDone()) {
            task.cancel(true);
            activeScrapingTasks.remove(domainName);
            logger.info("Cancelled Apify scraping for domain: {}", domainName);
        }
    }
    
    /**
     * Pause scraping for a domain
     */
    public boolean pauseScrapingForDomain(String domainName) {
        CompletableFuture<String> task = activeScrapingTasks.get(domainName);
        logger.info("Attempting to pause domain: {}, task exists: {}, task done: {}", 
                   domainName, task != null, task != null ? task.isDone() : "N/A");
        
        if (task != null && !task.isDone()) {
            boolean cancelled = task.cancel(true);
            activeScrapingTasks.remove(domainName);
            updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.PAUSED, "Scraping paused by user");
            logger.info("Paused Apify scraping for domain: {}, cancellation success: {}", domainName, cancelled);
            return true;
        }
        logger.info("No active task to pause for domain: {}", domainName);
        return false;
    }
    
    /**
     * Resume scraping for a domain
     */
    public CompletableFuture<String> resumeScrapingForDomain(String domainName) {
        // Check if domain is in paused state
        Domain domain = domainRepository.findByDomainName(domainName);
        if (domain != null && domain.getProcessingStatus() == Domain.ProcessingStatus.PAUSED) {
            logger.info("Resuming Apify scraping for domain: {}", domainName);
            return scrapeAdsUsingApify(domainName);
        }
        return CompletableFuture.completedFuture("Domain not in paused state");
    }
    
    /**
     * Scrape ads using Apify's Facebook Ads Scraper
     */
    @Async
    @Transactional
    public CompletableFuture<String> scrapeAdsUsingApify(String domainName) {
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting Apify scraping for domain: {}", domainName);
                
                if (apifyToken == null || apifyToken.isEmpty()) {
                    updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FAILED, "Apify token not configured");
                    logger.warn("Apify token not configured. Cannot use Apify service.");
                    return "Error: Apify token not configured";
                }
                
                // Delete existing ads and analyses for this domain (within same transaction)
                try {
                    // First clear ad analyses
                    Domain domain = domainRepository.findByDomainName(domainName);
                    if (domain != null) {
                        adAnalysisRepository.deleteByDomain(domain);
                        logger.info("Cleared existing ad analyses for domain: {}", domainName);
                    }
                    
                    // Then clear scraped ads
                    scrapedAdRepository.deleteByDomainName(domainName);
                    logger.info("Cleared existing ads for domain: {}", domainName);
                } catch (Exception e) {
                    logger.warn("Could not clear existing data for domain {}: {}", domainName, e.getMessage());
                }
                
                // Start Apify actor run
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FETCHING_ADS, "Starting ad scraper...");
                String runId = startApifyRun(domainName);
                if (runId == null) {
                    updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FAILED, "Failed to start ad scraper");
                    return "Error: Failed to start Apify run";
                }
                
                // Wait for completion and get results
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FETCHING_ADS, "Scraping ads from Facebook...");
                List<ScrapedAd> ads = waitForResultsAndProcess(runId, domainName);
                
                // Save to database with duplicate handling
                if (!ads.isEmpty()) {
                    updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.SCANNING_COMPLIANCE, 
                        String.format("Analyzing %d ads for compliance...", ads.size()));
                    
                    // Save ads one by one to handle duplicates gracefully
                    int savedCount = 0;
                    for (ScrapedAd ad : ads) {
                        try {
                            // Check if ad already exists
                            Optional<ScrapedAd> existingAd = scrapedAdRepository.findByMetaAdId(ad.getMetaAdId());
                            if (existingAd.isPresent()) {
                                // Update existing ad
                                ScrapedAd existing = existingAd.get();
                                existing.setHeadline(ad.getHeadline());
                                existing.setPrimaryText(ad.getPrimaryText());
                                existing.setDescription(ad.getDescription());
                                existing.setLandingPageUrl(ad.getLandingPageUrl());
                                existing.setLastUpdated(ad.getLastUpdated());
                                existing.setIsActive(ad.getIsActive());
                                scrapedAdRepository.save(existing);
                                savedCount++;
                            } else {
                                // Save new ad
                                scrapedAdRepository.save(ad);
                                savedCount++;
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to save ad {}: {}", ad.getMetaAdId(), e.getMessage());
                        }
                    }
                    logger.info("Successfully processed {} ads from Apify for domain: {}", savedCount, domainName);
                    
            // Perform compliance analysis on all ads
            Domain domain = domainRepository.findByDomainName(domainName);
            if (domain != null) {
                complianceAnalysisService.analyzeDomainAds(domain);
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.COMPLETED,
                    String.format("Found %d ads - compliance analysis completed", ads.size()));
            } else {
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.COMPLETED,
                    String.format("Found %d ads - ready for monitoring", ads.size()));
            }
                } else {
                    logger.info("No ads found via Apify for domain: {}", domainName);
                    updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.COMPLETED, "No ads found - monitoring ready");
                }
                
                return String.format("Apify scraping completed for domain: %s. Found %d ads.", 
                                   domainName, ads.size());
                
            } catch (Exception e) {
                logger.error("Error in Apify scraping for domain: {}", domainName, e);
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FAILED, 
                    "Error occurred during scraping: " + e.getMessage());
                return "Error occurred while scraping domain via Apify: " + domainName;
            } finally {
                // Remove task from tracking when done
                activeScrapingTasks.remove(domainName);
            }
        });
        
        // Track the task
        activeScrapingTasks.put(domainName, task);
        return task;
    }
    
    private String startApifyRun(String domainName) {
        try {
            String url = String.format("%s/acts/%s/runs?token=%s", 
                                     APIFY_API_BASE, FACEBOOK_ADS_SCRAPER_ID, apifyToken);
            
            // Prepare input for Apify actor
            Map<String, Object> input = new HashMap<>();
            
            // Try both Facebook page URL and direct domain search
            List<Map<String, String>> startUrls = Arrays.asList(
                Map.of("url", "https://www.facebook.com/" + domainName + "/", "method", "GET"),
                Map.of("url", String.format(
                    "https://www.facebook.com/ads/library/?active_status=active&ad_type=all&country=ALL&media_type=all&q=%s&search_type=keyword_unordered",
                    domainName), "method", "GET")
            );
            
            input.put("startUrls", startUrls);
            // Use configurable limit (0 means no limit)
            if (resultsLimit > 0) {
                input.put("resultsLimit", resultsLimit);
                logger.info("Using configured results limit: {}", resultsLimit);
            } else {
                logger.info("No results limit configured - scraping all available ads");
            }
            input.put("isDetailsPerAd", true);
            input.put("onlyTotal", false);
            input.put("activeStatus", "active");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(input, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String runId = responseJson.path("data").path("id").asText();
                logger.info("Started Apify run with ID: {}", runId);
                return runId;
            }
            
        } catch (Exception e) {
            logger.error("Error starting Apify run for domain: {}", domainName, e);
        }
        
        return null;
    }
    
    private List<ScrapedAd> waitForResultsAndProcess(String runId, String domainName) {
        List<ScrapedAd> ads = new ArrayList<>();
        
        try {
            // Wait for run to complete (with timeout)
            int maxWaitSeconds = 300; // 5 minutes
            int waitedSeconds = 0;
            String status = "RUNNING";
            
            while (!"SUCCEEDED".equals(status) && waitedSeconds < maxWaitSeconds) {
                // Check if task was cancelled
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("Apify scraping task was cancelled for domain: {}", domainName);
                    throw new InterruptedException("Task was cancelled");
                }
                
                Thread.sleep(5000); // Wait 5 seconds
                waitedSeconds += 5;
                
                status = checkRunStatus(runId);
                logger.info("Apify run {} status: {} (waited {}s)", runId, status, waitedSeconds);
                
                if ("FAILED".equals(status)) {
                    logger.error("Apify run failed: {}", runId);
                    throw new RuntimeException("Apify scraping failed - run status: FAILED");
                }
            }
            
            if ("SUCCEEDED".equals(status)) {
                // Get dataset results
                ads = getDatasetResults(runId, domainName);
            } else if (waitedSeconds >= maxWaitSeconds) {
                logger.warn("Apify run timed out after {} seconds: {}", waitedSeconds, runId);
                throw new RuntimeException("Apify scraping timed out after " + waitedSeconds + " seconds");
            } else {
                logger.warn("Apify run didn't complete with status: {} after {} seconds", status, waitedSeconds);
                throw new RuntimeException("Apify scraping failed with status: " + status);
            }
            
        } catch (InterruptedException e) {
            logger.info("Apify scraping was interrupted for domain: {}", domainName);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return ads; // Return empty list when interrupted
        } catch (Exception e) {
            logger.error("Error waiting for Apify results: {}", e.getMessage());
        }
        
        return ads;
    }
    
    private String checkRunStatus(String runId) {
        try {
            String url = String.format("%s/actor-runs/%s?token=%s", APIFY_API_BASE, runId, apifyToken);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                return responseJson.path("data").path("status").asText();
            }
        } catch (Exception e) {
            logger.error("Error checking run status: {}", e.getMessage());
        }
        
        return "UNKNOWN";
    }
    
    private List<ScrapedAd> getDatasetResults(String runId, String domainName) {
        List<ScrapedAd> ads = new ArrayList<>();
        
        try {
            // Get dataset ID from run
            String runUrl = String.format("%s/actor-runs/%s?token=%s", APIFY_API_BASE, runId, apifyToken);
            ResponseEntity<String> runResponse = restTemplate.getForEntity(runUrl, String.class);
            
            if (!runResponse.getStatusCode().is2xxSuccessful()) {
                return ads;
            }
            
            JsonNode runData = objectMapper.readTree(runResponse.getBody());
            String datasetId = runData.path("data").path("defaultDatasetId").asText();
            
            if (datasetId.isEmpty()) {
                logger.warn("No dataset ID found for run: {}", runId);
                return ads;
            }
            
            // Get dataset items
            String datasetUrl = String.format("%s/datasets/%s/items?token=%s&format=json", 
                                            APIFY_API_BASE, datasetId, apifyToken);
            ResponseEntity<String> datasetResponse = restTemplate.getForEntity(datasetUrl, String.class);
            
            if (datasetResponse.getStatusCode().is2xxSuccessful()) {
                JsonNode items = objectMapper.readTree(datasetResponse.getBody());
                
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        ScrapedAd ad = parseApifyAdData(item, domainName);
                        if (ad != null) {
                            ads.add(ad);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error getting dataset results: {}", e.getMessage());
        }
        
        return ads;
    }
    
    private ScrapedAd parseApifyAdData(JsonNode adData, String domainName) {
        try {
            // Simple approach: if it has snapshot data, it's likely an ad
            if (!adData.has("snapshot") || adData.has("error")) {
                return null; // Skip non-ad entries
            }
            
            JsonNode snapshot = adData.path("snapshot");
            String adText = snapshot.path("body").path("text").asText();
            String title = snapshot.path("title").asText();
            
            // If there's no meaningful content, skip
            if (adText.trim().isEmpty() && title.trim().isEmpty()) {
                return null;
            }
            
            logger.info("Processing ad with text: '{}' and title: '{}'", 
                       adText.length() > 50 ? adText.substring(0, 50) + "..." : adText,
                       title.length() > 50 ? title.substring(0, 50) + "..." : title);
            
            ScrapedAd ad = new ScrapedAd();
            
            // Map Apify data to our ScrapedAd model
            String adArchiveId = adData.path("adArchiveID").asText();
            if (adArchiveId.isEmpty()) {
                adArchiveId = adData.path("adArchiveId").asText(); // fallback
            }
            ad.setMetaAdId(!adArchiveId.isEmpty() ? adArchiveId : 
                          "apify_" + domainName + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            ad.setDomainName(domainName);
            ad.setPageName(adData.path("pageName").asText());
            ad.setPageId(adData.path("pageId").asText());
            ad.setPrimaryText(adData.path("snapshot").path("body").path("text").asText());
            ad.setHeadline(adData.path("snapshot").path("title").asText());
            ad.setCallToAction(adData.path("snapshot").path("ctaText").asText());
            ad.setLandingPageUrl(adData.path("snapshot").path("linkUrl").asText());
            ad.setDisplayUrl(domainName);
            ad.setFundingEntity(""); // Not available in this format
            
            // Parse and download images
            JsonNode snapshotImages = adData.path("snapshot").path("images");
            if (snapshotImages.isArray() && snapshotImages.size() > 0) {
                List<String> imageUrls = new ArrayList<>();
                List<String> localImagePaths = new ArrayList<>();
                
                for (JsonNode imageNode : snapshotImages) {
                    // Prefer original image URL, fallback to resized
                    String originalImageUrl = imageNode.path("originalImageUrl").asText();
                    String resizedImageUrl = imageNode.path("resizedImageUrl").asText();
                    
                    String imageUrl = !originalImageUrl.isEmpty() ? originalImageUrl : resizedImageUrl;
                    
                    if (!imageUrl.isEmpty()) {
                        imageUrls.add(imageUrl);
                        
                        // Download and store locally
                        String localPath = mediaStorageService.downloadAndStoreImage(imageUrl, domainName, ad.getMetaAdId());
                        if (localPath != null) {
                            localImagePaths.add(localPath);
                        }
                    }
                }
                
                if (!imageUrls.isEmpty()) {
                    ad.setImageUrls(imageUrls);
                    ad.setLocalImagePaths(localImagePaths);
                    ad.setAdFormat("SINGLE_IMAGE");
                }
            }
            
            // Parse and download videos
            JsonNode snapshotVideos = adData.path("snapshot").path("videos");
            if (snapshotVideos.isArray() && snapshotVideos.size() > 0) {
                List<String> videoUrls = new ArrayList<>();
                List<String> localVideoPaths = new ArrayList<>();
                
                for (JsonNode videoNode : snapshotVideos) {
                    String videoUrl = videoNode.asText(); // Videos might be stored as strings in array
                    
                    if (!videoUrl.isEmpty()) {
                        videoUrls.add(videoUrl);
                        
                        // Download and store locally
                        String localPath = mediaStorageService.downloadAndStoreVideo(videoUrl, domainName, ad.getMetaAdId());
                        if (localPath != null) {
                            localVideoPaths.add(localPath);
                        }
                    }
                }
                
                if (!videoUrls.isEmpty()) {
                    ad.setVideoUrls(videoUrls);
                    ad.setLocalVideoPaths(localVideoPaths);
                    ad.setAdFormat("VIDEO");
                }
            }
            
            // Parse spend range
            JsonNode spendLower = adData.path("spend_lower_bound");
            JsonNode spendUpper = adData.path("spend_upper_bound");
            if (!spendLower.isMissingNode() && !spendUpper.isMissingNode()) {
                ad.setSpendRangeLower((int) spendLower.asLong(0L));
                ad.setSpendRangeUpper((int) spendUpper.asLong(0L));
            }
            
            // Parse impressions range
            JsonNode impressionsLower = adData.path("impressions_lower_bound");
            JsonNode impressionsUpper = adData.path("impressions_upper_bound");
            if (!impressionsLower.isMissingNode() && !impressionsUpper.isMissingNode()) {
                ad.setImpressionsRangeLower(impressionsLower.asLong(0L));
                ad.setImpressionsRangeUpper(impressionsUpper.asLong(0L));
            }
            
            // Process media files and extract text content (OCR + transcription)
            processMediaForAd(ad);
            
            // RAC extraction will be done separately via RacExtractionService
            // processLandingPageForAd(ad);
            
            // Parse dates
            long startDateTimestamp = adData.path("startDate").asLong(0);
            if (startDateTimestamp > 0) {
                try {
                    ad.setAdDeliveryStartDate(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(startDateTimestamp), 
                        java.time.ZoneId.systemDefault()));
                } catch (Exception e) {
                    logger.debug("Could not parse start date timestamp: {}", startDateTimestamp);
                }
            }
            
            long endDateTimestamp = adData.path("endDate").asLong(0);
            if (endDateTimestamp > 0) {
                try {
                    ad.setAdDeliveryStopDate(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(endDateTimestamp), 
                        java.time.ZoneId.systemDefault()));
                } catch (Exception e) {
                    logger.debug("Could not parse end date timestamp: {}", endDateTimestamp);
                }
            }
            
            ad.setIsActive(true);
            ad.setScrapedAt(LocalDateTime.now());
            ad.setLastUpdated(LocalDateTime.now());
            
            return ad;
            
        } catch (Exception e) {
            logger.error("Error parsing Apify ad data: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get pricing estimate for scraping
     */
    public Map<String, Object> getScrapingEstimate(String domainName, int estimatedAds) {
        Map<String, Object> estimate = new HashMap<>();
        
        // Apify pricing: $5.00 per 1,000 ads
        double costPer1000 = 5.00;
        double estimatedCost = (estimatedAds / 1000.0) * costPer1000;
        
        estimate.put("domain", domainName);
        estimate.put("estimatedAds", estimatedAds);
        estimate.put("costPer1000Ads", costPer1000);
        estimate.put("estimatedCost", Math.round(estimatedCost * 100.0) / 100.0);
        estimate.put("currency", "USD");
        estimate.put("provider", "Apify");
        
        return estimate;
    }
    
    /**
     * Get scraped ads for a domain with pagination
     */
    public Page<ScrapedAd> getScrapedAdsForDomain(String domainName, Pageable pageable) {
        try {
            List<ScrapedAd> ads = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domainName);
            
            // Manual pagination since we're getting all results
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), ads.size());
            
            List<ScrapedAd> pageContent = ads.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, ads.size());
            
        } catch (Exception e) {
            logger.error("Error fetching ads for domain: {}", domainName, e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }
    
    /**
     * Get ad count for a domain
     */
    public long getAdCountForDomain(String domainName) {
        try {
            return scrapedAdRepository.countByDomainName(domainName);
        } catch (Exception e) {
            logger.error("Error counting ads for domain: {}", domainName, e);
            return 0;
        }
    }

    /**
     * Create mock ads for demo purposes when Apify credits are exhausted
     */
    @Transactional
    public String createMockAdsForDomain(String domainName) {
        try {
            logger.info("Creating mock ads for domain: {}", domainName);
            
            // Check if real ads already exist - don't overwrite them
            List<ScrapedAd> existingAds = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domainName);
            boolean hasRealAds = existingAds.stream().anyMatch(ad -> 
                ad.getScrapeSource() != null && !ad.getScrapeSource().equals("DEMO_DATA"));
            
            if (hasRealAds) {
                logger.info("Real ads already exist for domain: {}. Skipping mock data creation.", domainName);
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.COMPLETED, 
                    "Real ads preserved - no mock data needed");
                return "Real ads already exist for domain: " + domainName + ". Mock data creation skipped.";
            }
            
            // Delete existing mock ads only (within same transaction)
            try {
                scrapedAdRepository.deleteByDomainName(domainName);
                logger.info("Cleared existing mock ads for domain: {}", domainName);
            } catch (Exception e) {
                logger.warn("Could not clear existing mock ads for domain {}: {}", domainName, e.getMessage());
            }
            
            // Update domain status
            updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FETCHING_ADS, "Creating demo ads...");
            
            List<ScrapedAd> mockAds = new ArrayList<>();
            
            // Create 5 mock ads with realistic data
            String[] headlines = {
                "Exclusive Deals on " + domainName,
                "Limited Time Offers - Shop Now!",
                "Save Big with Our Special Promotions",
                "Don't Miss Out - Best Prices Guaranteed",
                "Premium Quality at Unbeatable Prices"
            };
            
            String[] primaryTexts = {
                "Discover amazing deals and exclusive offers on our platform. Shop now and save up to 50% on selected items!",
                "Join thousands of satisfied customers who trust us for the best deals online. Free shipping on orders over $50.",
                "Quality products at competitive prices. Browse our extensive catalog and find exactly what you're looking for.",
                "Experience premium shopping with our curated selection of top-rated products and exceptional customer service.",
                "Transform your shopping experience with our innovative platform designed for modern consumers."
            };
            
            String[] imageUrls = {
                "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1556909045-f7c9c6badb3d?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1556909045-4d5c2f4745d8?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1556909045-4d5c2f4745d9?w=400&h=300&fit=crop"
            };
            
            for (int i = 0; i < 5; i++) {
                ScrapedAd ad = new ScrapedAd();
                ad.setDomainName(domainName);
                ad.setMetaAdId("demo_ad_" + domainName + "_" + (i + 1));
                ad.setHeadline(headlines[i]);
                ad.setPrimaryText(primaryTexts[i]);
                ad.setImageUrls(Arrays.asList(imageUrls[i]));
                ad.setVideoUrls(new ArrayList<>());
                ad.setLocalImagePaths(new ArrayList<>());
                ad.setLocalVideoPaths(new ArrayList<>());
                ad.setLandingPageUrl("https://" + domainName);
                ad.setScrapedAt(LocalDateTime.now().minusHours(i));
                ad.setAdDeliveryStartDate(LocalDateTime.now().minusDays(7 + i));
                ad.setAdDeliveryStopDate(LocalDateTime.now().plusDays(30 - i));
                ad.setAdCreationDate(LocalDateTime.now().minusDays(10 + i));
                ad.setPageName(domainName.substring(0, domainName.indexOf('.')).toUpperCase() + " Official");
                ad.setScrapeSource("DEMO_DATA");
                
                mockAds.add(ad);
            }
            
            // Save mock ads
            scrapedAdRepository.saveAll(mockAds);
            
            // Perform compliance analysis on mock ads
            Domain domain = domainRepository.findByDomainName(domainName);
            if (domain != null) {
                // TODO: Re-enable compliance analysis when service is fixed
                // updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.SCANNING_COMPLIANCE, 
                //     "Analyzing demo ads for compliance...");
                // complianceAnalysisService.analyzeDomainAds(domain);
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.COMPLETED, 
                    String.format("Demo: Created %d sample ads", mockAds.size()));
            } else {
                updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.COMPLETED, 
                    String.format("Demo: Created %d sample ads for testing", mockAds.size()));
            }
            
            logger.info("Successfully created {} mock ads for domain: {}", mockAds.size(), domainName);
            return String.format("Successfully created %d mock ads for domain: %s", mockAds.size(), domainName);
            
        } catch (Exception e) {
            logger.error("Error creating mock ads for domain: {}", domainName, e);
            updateDomainProcessingStatus(domainName, Domain.ProcessingStatus.FAILED, "Failed to create demo ads");
            return "Error creating mock ads: " + e.getMessage();
        }
    }
    
    /**
     * Process media files for an ad and extract text content
     */
    private void processMediaForAd(ScrapedAd ad) {
        try {
            // Process media asynchronously and update the ad with extracted text
            mediaProcessingService.processAdMedia(
                ad.getLocalImagePaths(), 
                ad.getLocalVideoPaths(), 
                ad.getMetaAdId()
            ).thenAccept(result -> {
                if (result.isSuccess()) {
                    // Update the ad with extracted text
                    if (result.hasImageText()) {
                        ad.setExtractedImageText(result.getExtractedImageText());
                        logger.info("Set extracted image text for ad {}: {} characters", 
                                   ad.getMetaAdId(), result.getExtractedImageText().length());
                    }
                    if (result.hasVideoText()) {
                        ad.setExtractedVideoText(result.getExtractedVideoText());
                        logger.info("Set extracted video text for ad {}: {} characters", 
                                   ad.getMetaAdId(), result.getExtractedVideoText().length());
                    }
                    
                    // Save updated ad with extracted text
                    scrapedAdRepository.save(ad);
                    logger.info("Updated ad {} with extracted media text", ad.getMetaAdId());
                } else {
                    logger.warn("Media processing failed for ad {}: {}", ad.getMetaAdId(), result.getError());
                }
            }).exceptionally(throwable -> {
                logger.error("Error in media processing for ad {}: {}", ad.getMetaAdId(), throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            logger.error("Failed to initiate media processing for ad {}: {}", ad.getMetaAdId(), e.getMessage());
        }
    }
    
    /**
     * Fetch and analyze landing page content for an ad
     */
    private void processLandingPageForAd(ScrapedAd ad) {
        String landingPageUrl = ad.getLandingPageUrl();
        if (landingPageUrl == null || landingPageUrl.trim().isEmpty()) {
            logger.debug("No landing page URL for ad: {}", ad.getMetaAdId());
            return;
        }
        
        try {
            logger.info("🔍 Extracting RAC from landing page for ad {}: {}", ad.getMetaAdId(), landingPageUrl);
            
            // Fetch the landing page content
            String pageContent = fetchLandingPageContent(landingPageUrl);
            
            if (pageContent != null && !pageContent.trim().isEmpty() && !pageContent.contains("301 Moved Permanently")) {
                // Try direct iframe extraction first (most reliable)
                String iframeRac = extractRacFromIframe(pageContent);
                if (iframeRac != null && !iframeRac.trim().isEmpty()) {
                    ad.setReferrerAdCreative(iframeRac);
                    logger.info("✅ Direct iframe extraction successful for ad {}: '{}'", ad.getMetaAdId(), iframeRac);
                    return; // Success, skip other methods
                } else {
                    logger.debug("❌ No iframe found for direct RAC extraction for ad {}", ad.getMetaAdId());
                }
            } else {
                logger.warn("❌ Landing page not accessible or redirected for ad {}: {} - trying URL parameter extraction", ad.getMetaAdId(), landingPageUrl);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to analyze landing page for ad {}: {}", ad.getMetaAdId(), e.getMessage());
        }
        
        // Fallback to URL parameter extraction
        try {
            if (ad.getReferrerAdCreative() == null || ad.getReferrerAdCreative().trim().isEmpty()) {
                String kwParam = extractKwParameter(null, landingPageUrl); // Pass null for pageContent to focus on URL
                if (kwParam != null && !kwParam.trim().isEmpty()) {
                    ad.setReferrerAdCreative(kwParam);
                    logger.info("🔄 URL parameter extraction successful for ad {}: '{}'", ad.getMetaAdId(), kwParam);
                } else {
                    logger.warn("❌ No RAC could be extracted for ad {} from URL: {}", ad.getMetaAdId(), landingPageUrl);
                }
            }
        } catch (Exception fallbackError) {
            logger.warn("URL parameter extraction failed for ad {}: {}", ad.getMetaAdId(), fallbackError.getMessage());
        }
    }
    
    /**
     * Fetch landing page content with redirect handling
     */
    private String fetchLandingPageContent(String url) {
        try {
            // Add comprehensive headers to avoid blocking and handle redirects
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.set("Accept-Language", "en-US,en;q=0.5");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("DNT", "1");
            headers.set("Connection", "keep-alive");
            headers.set("Upgrade-Insecure-Requests", "1");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            // Configure RestTemplate to follow redirects
            org.springframework.web.client.RestTemplate redirectTemplate = new org.springframework.web.client.RestTemplate();
            redirectTemplate.getInterceptors().add((request, body, execution) -> {
                logger.debug("Fetching: {} with headers: {}", request.getURI(), request.getHeaders());
                return execution.execute(request, body);
            });
            
            org.springframework.http.ResponseEntity<String> response = redirectTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode().is3xxRedirection()) {
                String location = response.getHeaders().getFirst("Location");
                if (location != null) {
                    logger.info("Following redirect from {} to {}", url, location);
                    return fetchLandingPageContent(location); // Recursive call for redirect
                }
            }
            
            String content = response.getBody();
            if (content != null && content.contains("301 Moved Permanently")) {
                logger.warn("Received 301 redirect page for {}, content might be dynamically loaded", url);
                // For now, we'll extract RAC from URL parameters since page content is not accessible
                return null;
            }
            
            return content;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.warn("HTTP error fetching landing page from {}: {} - {}", url, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("Failed to fetch landing page content from {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Intelligently extract the referrer ad creative from landing page content and URL
     * This method analyzes both URL parameters and page content to find the most likely RAC value
     */
    private String extractKwParameter(String pageContent, String url) {
        try {
            logger.debug("🔍 Analyzing landing page for RAC: {}", url);
            
            // Step 1: Try common URL parameters that might contain the referrer ad creative
            String[] commonParams = {"adtitle", "kw", "q", "query", "keyword", "search", "term", "phrase", "text", "content", "title", "headline"};
            
            for (String param : commonParams) {
                String paramPattern = param + "=";
                if (url.contains(paramPattern)) {
                    try {
                        String[] parts = url.split(java.util.regex.Pattern.quote(paramPattern));
                        if (parts.length > 1) {
                            String value = parts[1].split("&")[0]; // Get value before next parameter
                            String decoded = java.net.URLDecoder.decode(value, "UTF-8");
                            if (decoded.length() > 3 && isLikelyAdCreative(decoded)) { // Filter out short/meaningless values
                                logger.debug("🎯 Found RAC in URL parameter '{}': '{}'", param, decoded);
                                return decoded;
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Error parsing parameter {}: {}", param, e.getMessage());
                    }
                }
            }
            
            // Step 2: Analyze page content if no suitable URL parameter found
            if (pageContent != null && !pageContent.trim().isEmpty()) {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(pageContent);
                
                // Look for JavaScript variables that might contain the search term/ad creative
                String jsRac = extractFromJavaScript(doc);
                if (jsRac != null) {
                    logger.debug("🎯 Found RAC in JavaScript: '{}'", jsRac);
                    return jsRac;
                }
                
                // Look for meta tags that might contain the search term
                String metaRac = extractFromMetaTags(doc);
                if (metaRac != null) {
                    logger.debug("🎯 Found RAC in meta tags: '{}'", metaRac);
                    return metaRac;
                }
                
                // Look for data attributes
                String dataRac = extractFromDataAttributes(doc);
                if (dataRac != null) {
                    logger.debug("🎯 Found RAC in data attributes: '{}'", dataRac);
                    return dataRac;
                }
                
                // Look for form inputs or hidden fields
                String formRac = extractFromFormInputs(doc);
                if (formRac != null) {
                    logger.debug("🎯 Found RAC in form inputs: '{}'", formRac);
                    return formRac;
                }
                
                // Last resort: look for the page title or main heading as potential RAC
                String titleRac = extractFromTitleOrHeading(doc);
                if (titleRac != null) {
                    logger.debug("🎯 Found potential RAC in title/heading: '{}'", titleRac);
                    return titleRac;
                }
            }
            
            logger.debug("❌ No RAC parameter found for URL: {}", url);
            
        } catch (Exception e) {
            logger.warn("Error extracting RAC parameter: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Check if a string is likely to be an ad creative (not just random parameters)
     */
    private boolean isLikelyAdCreative(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        
        // Filter out common non-ad-creative values
        String lower = value.toLowerCase().trim();
        String[] excludePatterns = {"true", "false", "1", "0", "yes", "no", "null", "undefined", "test"};
        
        for (String pattern : excludePatterns) {
            if (lower.equals(pattern)) return false;
        }
        
        // Must have some meaningful content (letters)
        return value.matches(".*[a-zA-Z].*") && value.length() >= 3;
    }
    
    /**
     * Extract RAC from JavaScript variables in the page
     */
    private String extractFromJavaScript(org.jsoup.nodes.Document doc) {
        org.jsoup.select.Elements scripts = doc.select("script");
        
        // Common JavaScript variable names that might contain the search term
        String[] jsPatterns = {
            "(?:search|query|keyword|kw|q|term|phrase|title|headline|content)\\s*[:=]\\s*[\"']([^\"']{4,100})[\"']",
            "(?:referrer|ref|source).*?[\"']([^\"']{4,100})[\"']",
            "window\\.(?:search|query|keyword)\\s*=\\s*[\"']([^\"']{4,100})[\"']"
        };
        
        for (org.jsoup.nodes.Element script : scripts) {
            String scriptText = script.html();
            
            for (String pattern : jsPatterns) {
                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = regex.matcher(scriptText);
                if (matcher.find()) {
                    String value = matcher.group(1);
                    if (isLikelyAdCreative(value)) {
                        return value;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract RAC from meta tags
     */
    private String extractFromMetaTags(org.jsoup.nodes.Document doc) {
        // Look for meta tags with relevant names/properties
        String[] metaSelectors = {
            "meta[name*=keyword]", "meta[name*=search]", "meta[name*=query]", "meta[name*=title]",
            "meta[property*=keyword]", "meta[property*=search]", "meta[property*=query]",
            "meta[name='description']", "meta[property='og:title']", "meta[property='og:description']"
        };
        
        for (String selector : metaSelectors) {
            org.jsoup.select.Elements metas = doc.select(selector);
            for (org.jsoup.nodes.Element meta : metas) {
                String content = meta.attr("content");
                if (isLikelyAdCreative(content)) {
                    return content;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract RAC from data attributes
     */
    private String extractFromDataAttributes(org.jsoup.nodes.Document doc) {
        String[] dataAttributes = {"data-search", "data-query", "data-keyword", "data-kw", "data-term", "data-title", "data-content"};
        
        for (String attr : dataAttributes) {
            org.jsoup.select.Elements elements = doc.select("[" + attr + "]");
            for (org.jsoup.nodes.Element element : elements) {
                String value = element.attr(attr);
                if (isLikelyAdCreative(value)) {
                    return value;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract RAC from form inputs (including hidden fields)
     */
    private String extractFromFormInputs(org.jsoup.nodes.Document doc) {
        String[] inputNames = {"search", "query", "keyword", "kw", "q", "term", "phrase", "title"};
        
        for (String name : inputNames) {
            org.jsoup.select.Elements inputs = doc.select("input[name=" + name + "], input[id=" + name + "]");
            for (org.jsoup.nodes.Element input : inputs) {
                String value = input.attr("value");
                if (isLikelyAdCreative(value)) {
                    return value;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract RAC from page title or main heading as last resort
     */
    private String extractFromTitleOrHeading(org.jsoup.nodes.Document doc) {
        // Try page title first
        String title = doc.title();
        if (isLikelyAdCreative(title) && title.length() <= 100) {
            return title;
        }
        
        // Try main headings
        org.jsoup.select.Elements headings = doc.select("h1, h2");
        for (org.jsoup.nodes.Element heading : headings) {
            String text = heading.text();
            if (isLikelyAdCreative(text) && text.length() <= 100) {
                return text;
            }
        }
        
        return null;
    }
    
    /**
     * Extract RAC directly from Google iframe using the name attribute
     * Based on: JSON.parse(document.querySelector("#master-1").name)["master-1"].kw
     */
    private String extractRacFromIframe(String pageContent) {
        try {
            // Parse the HTML content
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(pageContent);
            
            // Look for iframe with id="master-1" or similar Google AdSense iframes
            org.jsoup.select.Elements iframes = doc.select("iframe[id*='master'], iframe[id*='google'], iframe[id*='ads']");
            
            for (org.jsoup.nodes.Element iframe : iframes) {
                String iframeName = iframe.attr("name");
                if (iframeName != null && !iframeName.trim().isEmpty()) {
                    try {
                        // Parse the JSON from the name attribute
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(iframeName);
                        
                        // Try to extract the kw parameter from the JSON structure
                        // The structure might be: {"master-1": {"kw": "value"}} or similar
                        for (com.fasterxml.jackson.databind.JsonNode child : jsonNode) {
                            if (child.has("kw")) {
                                String kwValue = child.get("kw").asText();
                                if (kwValue != null && !kwValue.trim().isEmpty() && isLikelyAdCreative(kwValue)) {
                                    logger.debug("🎯 Found RAC in iframe JSON: '{}'", kwValue);
                                    return kwValue;
                                }
                            }
                        }
                        
                        // Alternative: try to find kw at the root level
                        if (jsonNode.has("kw")) {
                            String kwValue = jsonNode.get("kw").asText();
                            if (kwValue != null && !kwValue.trim().isEmpty() && isLikelyAdCreative(kwValue)) {
                                logger.debug("🎯 Found RAC in iframe JSON root: '{}'", kwValue);
                                return kwValue;
                            }
                        }
                        
                    } catch (Exception jsonError) {
                        logger.debug("Could not parse iframe name as JSON: {}", iframeName);
                    }
                }
            }
            
            // If no iframe found, try to extract from script tags that might contain the iframe data
            org.jsoup.select.Elements scripts = doc.select("script");
            for (org.jsoup.nodes.Element script : scripts) {
                String scriptContent = script.html();
                if (scriptContent.contains("master-1") && scriptContent.contains("kw")) {
                    // Try to extract kw value using regex
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"kw\"\\s*:\\s*\"([^\"]+)\"");
                    java.util.regex.Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String kwValue = matcher.group(1);
                        if (kwValue != null && !kwValue.trim().isEmpty() && isLikelyAdCreative(kwValue)) {
                            logger.debug("🎯 Found RAC in script content: '{}'", kwValue);
                            return kwValue;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting RAC from iframe: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Analyze domain RAC pattern using GPT-4 to identify how RAC is stored
     */
    @Async
    public CompletableFuture<Map<String, Object>> analyzeDomainRacPattern(String domainName) {
        try {
            logger.info("🔍 Starting RAC pattern analysis for domain: {}", domainName);
            
            // Get sample URLs from this domain (limit to 5 for cost efficiency)
            List<ScrapedAd> sampleAds = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domainName)
                .stream()
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
            
            if (sampleAds.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("error", "No ads found for domain: " + domainName);
                return CompletableFuture.completedFuture(result);
            }
            
            // Extract URLs and fetch landing page content
            List<String> sampleUrls = new ArrayList<>();
            List<String> samplePageContents = new ArrayList<>();
            
            for (ScrapedAd ad : sampleAds) {
                String url = ad.getLandingPageUrl();
                if (url != null && !url.trim().isEmpty()) {
                    sampleUrls.add(url);
                    
                    // Fetch landing page content
                    String pageContent = fetchLandingPageContent(url);
                    samplePageContents.add(pageContent != null ? pageContent : "");
                }
            }
            
            if (sampleUrls.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("error", "No valid landing page URLs found for domain: " + domainName);
                return CompletableFuture.completedFuture(result);
            }
            
            logger.info("📊 Analyzing {} sample URLs for domain: {}", sampleUrls.size(), domainName);
            
            // Use GPT-4 to analyze the pattern
            OpenAIAnalysisService.DomainRacPatternResult patternResult = 
                openAIAnalysisService.identifyDomainRacPattern(domainName, sampleUrls);
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("domain", domainName);
            result.put("samplesAnalyzed", sampleUrls.size());
            result.put("racParameter", patternResult.getRacParameter());
            result.put("extractionType", patternResult.getExtractionType());
            result.put("extractionPattern", patternResult.getExtractionPattern());
            result.put("confidence", patternResult.getConfidence());
            result.put("explanation", patternResult.getExplanation());
            result.put("sampleUrls", sampleUrls);
            
            logger.info("✅ RAC pattern analysis completed for domain: {}. Pattern: {} ({})", 
                       domainName, patternResult.getRacParameter(), patternResult.getConfidence());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Error during RAC pattern analysis for domain {}: {}", domainName, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Analysis failed: " + e.getMessage());
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * Re-process existing ads for a domain to extract RAC values using GPT-4
     */
    @Async
    public CompletableFuture<Integer> reprocessRacForDomain(String domainName) {
        try {
            logger.info("🔄 Starting RAC re-processing for domain: {}", domainName);
            
            // Find all ads for this domain that don't have RAC values
            List<ScrapedAd> adsToProcess = scrapedAdRepository.findByDomainNameAndReferrerAdCreativeIsNull(domainName);
            
            logger.info("Found {} ads without RAC values for domain: {}", adsToProcess.size(), domainName);
            
            int processedCount = 0;
            
            for (ScrapedAd ad : adsToProcess) {
                try {
                    logger.info("🔍 Processing RAC for ad: {} (URL: {})", ad.getMetaAdId(), ad.getLandingPageUrl());
                    
                    // Process the landing page for this ad
                    processLandingPageForAd(ad);
                    
                    // Save the updated ad
                    scrapedAdRepository.save(ad);
                    processedCount++;
                    
                    // Small delay to avoid overwhelming OpenAI API
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    logger.error("Failed to process RAC for ad {}: {}", ad.getMetaAdId(), e.getMessage());
                }
            }
            
            logger.info("✅ RAC re-processing completed for domain: {}. Processed {} ads", domainName, processedCount);
            return CompletableFuture.completedFuture(processedCount);
            
        } catch (Exception e) {
            logger.error("Error during RAC re-processing for domain {}: {}", domainName, e.getMessage());
            return CompletableFuture.completedFuture(0);
        }
    }

}
