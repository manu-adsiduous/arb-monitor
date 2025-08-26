package com.arbmonitor.api.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting RAC using domain-specific URL parameter detection
 */
@Service
public class DomainSpecificRacExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainSpecificRacExtractionService.class);
    
    // Cache for domain parameter mappings
    private final Map<String, String> domainParameterCache = new HashMap<>();
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Extract RAC from URL using domain-specific parameter detection
     */
    public String extractRacFromUrl(String url, String userDefinedRacParameter) {
        try {
            String domain = extractDomain(url);
            String racParameter = null;
            
            // First, try user-defined parameter if provided
            if (userDefinedRacParameter != null && !userDefinedRacParameter.trim().isEmpty()) {
                racParameter = userDefinedRacParameter.trim();
                logger.info("🔧 Using user-defined RAC parameter '{}' for domain: {}", racParameter, domain);
            } else {
                // Fall back to auto-detection
                racParameter = detectRacParameter(domain, url);
            }
            
            if (racParameter != null) {
                String racValue = extractParameterValue(url, racParameter);
                if (racValue != null && !racValue.trim().isEmpty()) {
                    logger.info("🎯 Extracted RAC using parameter '{}' for domain {}: '{}'", 
                              racParameter, domain, racValue);
                    return racValue;
                }
            }
            
            logger.debug("No RAC parameter found for domain: {}", domain);
            return null;
            
        } catch (Exception e) {
            logger.debug("Error extracting RAC from URL {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract RAC from URL using domain-specific parameter detection (backward compatibility)
     */
    public String extractRacFromUrl(String url) {
        return extractRacFromUrl(url, null);
    }
    
    /**
     * Detect which URL parameter is used as referrerAdCreative for a domain
     */
    private String detectRacParameter(String domain, String sampleUrl) {
        // Check cache first
        if (domainParameterCache.containsKey(domain)) {
            return domainParameterCache.get(domain);
        }
        
        try {
            logger.info("🔍 Analyzing AFS code for domain: {}", domain);
            
            // Fetch the landing page to analyze AFS code
            String pageContent = fetchPageContent(sampleUrl);
            if (pageContent == null) {
                return null;
            }
            
            // Look for referrerAdCreative assignment in JavaScript
            String racParameter = findRacParameterInCode(pageContent);
            
            if (racParameter != null) {
                // Cache the result
                domainParameterCache.put(domain, racParameter);
                logger.info("✅ Detected RAC parameter '{}' for domain: {}", racParameter, domain);
                return racParameter;
            }
            
            logger.info("❌ No RAC parameter found in AFS code for domain: {}", domain);
            return null;
            
        } catch (Exception e) {
            logger.error("Error detecting RAC parameter for domain {}: {}", domain, e.getMessage());
            return null;
        }
    }
    
    /**
     * Find which URL parameter is assigned to referrerAdCreative in the AFS code
     */
    private String findRacParameterInCode(String pageContent) {
        try {
            Document doc = Jsoup.parse(pageContent);
            
            // Look for JavaScript code that assigns to referrerAdCreative
            String scriptContent = doc.select("script").html();
            
            // Pattern 1: Look for "referrerAdCreative: variableName" or "referrerAdCreative: urlParams.get('param')"
            Pattern pattern1 = Pattern.compile("referrerAdCreative\\s*:\\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*[,}]");
            Matcher matcher1 = pattern1.matcher(scriptContent);
            
            if (matcher1.find()) {
                String variableName = matcher1.group(1);
                logger.info("🔍 Found referrerAdCreative assigned to variable: {}", variableName);
                
                // Now find where this variable gets its value from URL parameters
                String paramPattern = variableName + "\\s*=\\s*urlParams\\.get\\('([^']+)'\\)";
                Pattern paramMatcher = Pattern.compile(paramPattern);
                Matcher paramMatch = paramMatcher.matcher(scriptContent);
                
                if (paramMatch.find()) {
                    String paramName = paramMatch.group(1);
                    logger.info("🎯 Found RAC parameter: {} (assigned to variable {})", paramName, variableName);
                    return paramName;
                }
            }
            
            // Pattern 2: Look for direct assignment like "referrerAdCreative: urlParams.get('param')"
            Pattern pattern2 = Pattern.compile("referrerAdCreative\\s*:\\s*urlParams\\.get\\('([^']+)'\\)");
            Matcher matcher2 = pattern2.matcher(scriptContent);
            
            if (matcher2.find()) {
                String paramName = matcher2.group(1);
                logger.info("🎯 Found direct RAC parameter: {}", paramName);
                return paramName;
            }
            
            // Pattern 3: Look for other common patterns
            Pattern pattern3 = Pattern.compile("referrerAdCreative\\s*:\\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\.");
            Matcher matcher3 = pattern3.matcher(scriptContent);
            
            if (matcher3.find()) {
                String variableName = matcher3.group(1);
                logger.info("🔍 Found referrerAdCreative with variable: {}", variableName);
                
                // Look for this variable's assignment
                String varPattern = variableName + "\\s*=\\s*[^;]+";
                Pattern varMatcher = Pattern.compile(varPattern);
                Matcher varMatch = varMatcher.matcher(scriptContent);
                
                if (varMatch.find()) {
                    String assignment = varMatch.group(0);
                    logger.info("🔍 Variable assignment: {}", assignment);
                    
                    // Extract parameter name from assignment
                    Pattern paramExtract = Pattern.compile("get\\('([^']+)'\\)");
                    Matcher paramExtractMatch = paramExtract.matcher(assignment);
                    
                    if (paramExtractMatch.find()) {
                        String paramName = paramExtractMatch.group(1);
                        logger.info("🎯 Extracted RAC parameter: {}", paramName);
                        return paramName;
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error parsing AFS code: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            logger.error("Error extracting domain from URL {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract parameter value from URL
     */
    private String extractParameterValue(String url, String parameterName) {
        try {
            URL urlObj = new URL(url);
            String query = urlObj.getQuery();
            
            if (query != null && query.contains(parameterName + "=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith(parameterName + "=")) {
                        String value = param.substring(parameterName.length() + 1); // Remove "paramName="
                        String decodedValue = URLDecoder.decode(value, "UTF-8");
                        return decodedValue;
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error extracting parameter {} from URL {}: {}", parameterName, url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Fetch page content
     */
    private String fetchPageContent(String url) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.set("Accept-Language", "en-US,en;q=0.5");
            headers.set("Accept-Encoding", "gzip, deflate");
            headers.set("Connection", "keep-alive");
            headers.set("Upgrade-Insecure-Requests", "1");
            
            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            logger.debug("Failed to fetch page content for {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedDomains", domainParameterCache.size());
        stats.put("cachedParameters", domainParameterCache);
        return stats;
    }
}
