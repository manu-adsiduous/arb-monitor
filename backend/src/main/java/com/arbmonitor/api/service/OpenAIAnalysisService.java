package com.arbmonitor.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIAnalysisService.class);
    
    @Value("${app.openai.api-key}")
    private String openaiApiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenAIAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Analyze landing page content and extract referrer ad creative using GPT-4
     */
    public LandingPageAnalysisResult analyzeLandingPage(String landingPageContent, String landingPageUrl) {
        try {
            String prompt = buildLandingPagePrompt(landingPageContent, landingPageUrl);
            String gptResponse = callOpenAI(prompt);
            return parseLandingPageResponse(gptResponse);
            
        } catch (Exception e) {
            logger.error("Error analyzing landing page with OpenAI: {}", e.getMessage());
            return new LandingPageAnalysisResult(null, "Failed to analyze landing page: " + e.getMessage());
        }
    }
    
    /**
     * Analyze multiple URLs from a domain to identify the RAC parameter pattern
     */
    public DomainRacPatternResult identifyDomainRacPattern(String domainName, List<String> sampleUrls) {
        try {
            String prompt = buildDomainPatternPrompt(domainName, sampleUrls);
            String gptResponse = callOpenAI(prompt);
            return parseDomainPatternResponse(gptResponse);
            
        } catch (Exception e) {
            logger.error("Error identifying domain RAC pattern with OpenAI: {}", e.getMessage());
            return new DomainRacPatternResult(null, null, null, "LOW", "Failed to identify pattern: " + e.getMessage());
        }
    }
    
    /**
     * Analyze ad compliance using GPT-4
     */
    public ComplianceAnalysisResult analyzeAdCompliance(String adText, String landingPageContent, String referrerAdCreative, boolean racEnabled) {
        try {
            String prompt = buildCompliancePrompt(adText, landingPageContent, referrerAdCreative, racEnabled);
            String gptResponse = callOpenAI(prompt);
            return parseComplianceResponse(gptResponse, racEnabled);
            
        } catch (Exception e) {
            logger.error("Error analyzing ad compliance with OpenAI: {}", e.getMessage());
            return createFallbackResult("Analysis failed: " + e.getMessage(), racEnabled);
        }
    }
    
    /**
     * Analyze ad compliance using GPT-4 (backward compatibility)
     */
    public ComplianceAnalysisResult analyzeAdCompliance(String adText, String landingPageContent, String referrerAdCreative) {
        return analyzeAdCompliance(adText, landingPageContent, referrerAdCreative, false);
    }
    
    private String buildDomainPatternPrompt(String domainName, List<String> sampleUrls) {
        StringBuilder urlList = new StringBuilder();
        for (int i = 0; i < sampleUrls.size(); i++) {
            urlList.append((i + 1) + ". URL: " + sampleUrls.get(i) + "\n");
        }
        
        return "You are an expert web analyst. Analyze these sample URLs from domain " + domainName + " to identify the consistent pattern for extracting the referrer ad creative (RAC) parameter.\n\n" +
            "SAMPLE URLS FROM " + domainName.toUpperCase() + ":\n" +
            urlList.toString() + "\n\n" +
            "NOTE: Focus on URL parameters first, as landing page content analysis will be done separately if needed.\n\n" +
            "Please analyze these URLs and their landing page content, then respond in this EXACT JSON format:\n" +
            "{\n" +
            "  \"racParameter\": \"the parameter/element name that contains the RAC\",\n" +
            "  \"extractionType\": \"URL_PARAMETER|JS_VARIABLE|META_TAG|DATA_ATTRIBUTE|FORM_INPUT|JSON_LD\",\n" +
            "  \"extractionPattern\": \"specific pattern to extract the value\",\n" +
            "  \"confidence\": \"HIGH/MEDIUM/LOW based on consistency across samples\",\n" +
            "  \"explanation\": \"Brief explanation of where and how the RAC is stored in this domain\"\n" +
            "}\n\n" +
            "INSTRUCTIONS:\n" +
            "1. FIRST: Check URL parameters for RAC (adtitle, kw, q, query, search, keyword, term, title, headline)\n" +
            "2. IF NO URL PARAMETERS: Analyze the landing page HTML content for RAC patterns\n" +
            "3. Look for these patterns in page content:\n" +
            "   - JavaScript variables: window.searchTerm, var keyword, let query, etc.\n" +
            "   - Meta tags: <meta name=\"keyword\" content=\"...\">, <meta property=\"search\" content=\"...\">\n" +
            "   - Data attributes: data-search, data-keyword, data-query\n" +
            "   - Hidden form inputs: <input type=\"hidden\" name=\"search\" value=\"...\">\n" +
            "   - JSON-LD structured data: searchAction, query properties\n" +
            "4. The RAC should contain descriptive text that represents what users searched for\n" +
            "5. Choose the pattern that appears most consistently across all samples\n" +
            "6. Return confidence HIGH only if the pattern is consistent across 80%+ of samples\n\n" +
            "EXAMPLES:\n" +
            "- If URLs contain \"adtitle=Product+Name\", return racParameter: \"adtitle\"\n" +
            "- If URLs contain \"q=search+term\", return racParameter: \"q\"\n" +
            "- If URLs contain \"kw=keyword+phrase\", return racParameter: \"kw\"";
    }
    
    private String buildLandingPagePrompt(String landingPageContent, String landingPageUrl) {
        return String.format("""
            You are an expert web content analyst. Analyze this landing page to understand its content and extract the referrer ad creative value.
            
            LANDING PAGE URL:
            %s
            
            LANDING PAGE HTML CONTENT:
            %s
            
            Please analyze the page and respond in this EXACT JSON format:
            {
              "referrerAdCreative": "The main search term, keyword, or ad creative that brought users to this page",
              "pageContent": "Brief summary of what this page is about (1-2 sentences)"
            }
            
            INSTRUCTIONS:
            1. Look for URL parameters like 'q', 'query', 'search', 'kw', 'keyword', 'term' that contain the search term
            2. If no URL parameters, examine the page content, title, headings, and meta tags
            3. The referrerAdCreative should be the main topic/keyword that someone would search for to find this page
            4. If you can't find a specific search term, extract the main topic from the page title or primary heading
            5. Keep the referrerAdCreative concise but descriptive (3-15 words)
            6. Return null for referrerAdCreative only if the page content is completely unrelated to any searchable topic
            
            Examples of good referrerAdCreative values:
            - "Best supportive bikini tops for big busts"
            - "Kitchen gadgets under $25"
            - "Alzheimer's treatment injection at home"
            - "Daily vitamins for energy over 60"
            """, 
            landingPageUrl != null ? landingPageUrl : "No URL provided",
            landingPageContent != null ? landingPageContent.substring(0, Math.min(landingPageContent.length(), 3000)) : "No content provided"
        );
    }
    
    private String buildCompliancePrompt(String adText, String landingPageContent, String referrerAdCreative, boolean racEnabled) {
        String racSection = racEnabled ? 
            String.format("""
                
                REFERRER AD CREATIVE (kw parameter):
                %s
                """, referrerAdCreative != null ? referrerAdCreative : "No RAC provided") :
            """
            
            REFERRER AD CREATIVE (RAC):
            RAC analysis is turned off for this domain.
            """;
            
        String racRule = racEnabled ?
            "3. RAC RELEVANCE: The referrerAdCreative (kw parameter) should match the ad content for proper tracking" :
            "3. RAC RELEVANCE: RAC analysis is disabled for this domain";
            
        return String.format("""
            You are an expert ad compliance analyst. Analyze this Facebook ad for Google AdSense for Search (AFS) compliance.
            
            AD CREATIVE TEXT:
            %s
            
            LANDING PAGE CONTENT:
            %s%s
            
            Please analyze these 3 specific compliance areas and respond in this EXACT JSON format:
            {
              "adCreativeCompliant": true/false,
              "adCreativeReason": "Brief reason if not compliant, or 'Compliant' if ok",
              "landingPageRelevant": true/false,
              "landingPageReason": "Brief reason if not relevant, or 'Relevant' if ok",
              "racRelevant": true/false,
              "racReason": "Brief reason if RAC doesn't match ad, or 'Matches ad content' if ok, or 'RAC analysis turned off' if disabled",
              "overallCompliant": true/false
            }
            
            COMPLIANCE RULES:
            1. AD CREATIVE: No misleading claims, no "click here" language, no false promises, no medical/financial guarantees
            2. LANDING PAGE RELEVANCE: Content must match the ad promise and be relevant to the ad text
            %s
            
            Be strict but fair. Only mark as non-compliant if there are clear violations.
            """, 
            adText != null ? adText : "No ad text provided",
            landingPageContent != null ? landingPageContent.substring(0, Math.min(landingPageContent.length(), 2000)) : "No landing page content",
            racSection,
            racRule
        );
    }
    
    private String callOpenAI(String prompt) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.1);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.path("choices").get(0).path("message").path("content").asText();
        } else {
            throw new RuntimeException("OpenAI API call failed with status: " + response.getStatusCode());
        }
    }
    
    private ComplianceAnalysisResult parseComplianceResponse(String gptResponse, boolean racEnabled) {
        try {
            // Extract JSON from the response (GPT sometimes adds extra text)
            String jsonPart = gptResponse;
            if (gptResponse.contains("{")) {
                int start = gptResponse.indexOf("{");
                int end = gptResponse.lastIndexOf("}") + 1;
                jsonPart = gptResponse.substring(start, end);
            }
            
            JsonNode json = objectMapper.readTree(jsonPart);
            
            // Handle RAC relevance based on whether it's enabled
            boolean racRelevant = racEnabled ? json.path("racRelevant").asBoolean(true) : true;
            String racReason = racEnabled ? json.path("racReason").asText("Matches ad content") : "RAC analysis turned off";
            
            return new ComplianceAnalysisResult(
                json.path("adCreativeCompliant").asBoolean(true),
                json.path("adCreativeReason").asText("Compliant"),
                json.path("landingPageRelevant").asBoolean(true),
                json.path("landingPageReason").asText("Relevant"),
                racRelevant,
                racReason,
                json.path("overallCompliant").asBoolean(true)
            );
            
        } catch (Exception e) {
            logger.error("Error parsing GPT response: {}", e.getMessage());
            return createFallbackResult("Failed to parse analysis result", racEnabled);
        }
    }
    
    private DomainRacPatternResult parseDomainPatternResponse(String gptResponse) {
        try {
            // Extract JSON from the response (GPT sometimes adds extra text)
            String jsonPart = gptResponse;
            if (gptResponse.contains("{")) {
                int start = gptResponse.indexOf("{");
                int end = gptResponse.lastIndexOf("}") + 1;
                jsonPart = gptResponse.substring(start, end);
            }
            
            JsonNode json = objectMapper.readTree(jsonPart);
            
            String racParameter = json.path("racParameter").asText(null);
            String extractionType = json.path("extractionType").asText("URL_PARAMETER");
            String extractionPattern = json.path("extractionPattern").asText(null);
            String confidence = json.path("confidence").asText("LOW");
            String explanation = json.path("explanation").asText("No explanation provided");
            
            return new DomainRacPatternResult(racParameter, extractionType, extractionPattern, confidence, explanation);
            
        } catch (Exception e) {
            logger.error("Error parsing GPT domain pattern response: {}", e.getMessage());
            return new DomainRacPatternResult(null, null, null, "LOW", "Failed to parse pattern analysis");
        }
    }
    
    private LandingPageAnalysisResult parseLandingPageResponse(String gptResponse) {
        try {
            // Extract JSON from the response (GPT sometimes adds extra text)
            String jsonPart = gptResponse;
            if (gptResponse.contains("{")) {
                int start = gptResponse.indexOf("{");
                int end = gptResponse.lastIndexOf("}") + 1;
                jsonPart = gptResponse.substring(start, end);
            }
            
            JsonNode json = objectMapper.readTree(jsonPart);
            
            String referrerAdCreative = json.path("referrerAdCreative").asText(null);
            String pageContent = json.path("pageContent").asText("Content analysis not available");
            
            return new LandingPageAnalysisResult(referrerAdCreative, pageContent);
            
        } catch (Exception e) {
            logger.error("Error parsing GPT landing page response: {}", e.getMessage());
            return new LandingPageAnalysisResult(null, "Failed to parse landing page analysis");
        }
    }
    
    private ComplianceAnalysisResult createFallbackResult(String error, boolean racEnabled) {
        return new ComplianceAnalysisResult(
            false, "Analysis error: " + error,
            false, "Analysis error: " + error,
            racEnabled ? false : true, racEnabled ? "Analysis error: " + error : "RAC analysis turned off",
            false
        );
    }
    
    private ComplianceAnalysisResult createFallbackResult(String error) {
        return createFallbackResult(error, false);
    }
    
    /**
     * Result class for compliance analysis
     */
    public static class ComplianceAnalysisResult {
        private final boolean adCreativeCompliant;
        private final String adCreativeReason;
        private final boolean landingPageRelevant;
        private final String landingPageReason;
        private final boolean racRelevant;
        private final String racReason;
        private final boolean overallCompliant;
        
        public ComplianceAnalysisResult(boolean adCreativeCompliant, String adCreativeReason,
                                      boolean landingPageRelevant, String landingPageReason,
                                      boolean racRelevant, String racReason,
                                      boolean overallCompliant) {
            this.adCreativeCompliant = adCreativeCompliant;
            this.adCreativeReason = adCreativeReason;
            this.landingPageRelevant = landingPageRelevant;
            this.landingPageReason = landingPageReason;
            this.racRelevant = racRelevant;
            this.racReason = racReason;
            this.overallCompliant = overallCompliant;
        }
        
        // Getters
        public boolean isAdCreativeCompliant() { return adCreativeCompliant; }
        public String getAdCreativeReason() { return adCreativeReason; }
        public boolean isLandingPageRelevant() { return landingPageRelevant; }
        public String getLandingPageReason() { return landingPageReason; }
        public boolean isRacRelevant() { return racRelevant; }
        public String getRacReason() { return racReason; }
        public boolean isOverallCompliant() { return overallCompliant; }
    }
    
    /**
     * Result class for domain RAC pattern analysis
     */
    public static class DomainRacPatternResult {
        private final String racParameter;
        private final String extractionType;
        private final String extractionPattern;
        private final String confidence;
        private final String explanation;
        
        public DomainRacPatternResult(String racParameter, String extractionType, String extractionPattern, String confidence, String explanation) {
            this.racParameter = racParameter;
            this.extractionType = extractionType;
            this.extractionPattern = extractionPattern;
            this.confidence = confidence;
            this.explanation = explanation;
        }
        
        // Backward compatibility constructor
        public DomainRacPatternResult(String racParameter, String extractionPattern, String confidence, String explanation) {
            this(racParameter, "URL_PARAMETER", extractionPattern, confidence, explanation);
        }
        
        public String getRacParameter() { return racParameter; }
        public String getExtractionType() { return extractionType; }
        public String getExtractionPattern() { return extractionPattern; }
        public String getConfidence() { return confidence; }
        public String getExplanation() { return explanation; }
    }
    
    /**
     * Result class for landing page analysis
     */
    public static class LandingPageAnalysisResult {
        private final String referrerAdCreative;
        private final String pageContent;
        
        public LandingPageAnalysisResult(String referrerAdCreative, String pageContent) {
            this.referrerAdCreative = referrerAdCreative;
            this.pageContent = pageContent;
        }
        
        public String getReferrerAdCreative() { return referrerAdCreative; }
        public String getPageContent() { return pageContent; }
    }
}
