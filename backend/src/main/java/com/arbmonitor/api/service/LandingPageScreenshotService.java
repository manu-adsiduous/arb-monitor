package com.arbmonitor.api.service;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for capturing screenshots of landing pages using headless browser
 */
@Service
public class LandingPageScreenshotService {
    
    private static final Logger logger = LoggerFactory.getLogger(LandingPageScreenshotService.class);
    
    @Value("${app.media.storage.path:./media}")
    private String mediaStoragePath;
    
    /**
     * Capture a screenshot of the landing page
     * @param landingPageUrl The URL to capture
     * @param adId The ad ID for file naming
     * @return The local file path of the screenshot or null if failed
     */
    public String captureScreenshot(String landingPageUrl, String adId) {
        if (landingPageUrl == null || landingPageUrl.trim().isEmpty()) {
            logger.warn("No landing page URL provided for screenshot capture");
            return null;
        }
        
        // Clean URL by removing template variables (similar to landing page scraping)
        String cleanUrl = cleanUrlTemplateVariables(landingPageUrl);
        logger.info("Capturing screenshot for URL: {} (cleaned: {})", landingPageUrl, cleanUrl);
        
        WebDriver driver = null;
        try {
            // Configure Chrome options for headless mode
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1200,800"); // Standard desktop size
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-extensions");
            
            // Set Chrome binary path for macOS (adjust as needed for different environments)
            try {
                options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            } catch (Exception e) {
                logger.debug("Chrome binary path not found at default location, using system default");
            }
            
            // Initialize WebDriver
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            // Navigate to the URL
            driver.get(cleanUrl);
            logger.info("Page loaded, waiting for content to render...");
            
            // Wait for page to fully load and render
            Thread.sleep(3000);
            
            // Take screenshot
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            File screenshotFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
            
            // Create storage directory structure
            String domainName = extractDomainFromUrl(cleanUrl);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path screenshotDir = Paths.get(mediaStoragePath, "screenshots", domainName, dateStr);
            
            try {
                Files.createDirectories(screenshotDir);
            } catch (IOException e) {
                logger.error("Failed to create screenshot directory: {}", screenshotDir, e);
                return null;
            }
            
            // Generate filename
            String filename = String.format("%s_landing_page.png", adId);
            Path targetPath = screenshotDir.resolve(filename);
            
            // Move screenshot to target location
            Files.move(screenshotFile.toPath(), targetPath);
            
            String relativePath = String.format("screenshots/%s/%s/%s", domainName, dateStr, filename);
            logger.info("Screenshot captured successfully: {}", relativePath);
            
            return relativePath;
            
        } catch (Exception e) {
            logger.error("Failed to capture screenshot for URL: {} - {}", cleanUrl, e.getMessage(), e);
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    logger.warn("Error closing WebDriver: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Clean URL by removing template variables like {{campaign.id}}
     */
    private String cleanUrlTemplateVariables(String url) {
        if (url == null) return null;
        
        try {
            // Split URL into base and query parameters
            String[] urlParts = url.split("\\?", 2);
            if (urlParts.length == 2) {
                String baseUrl = urlParts[0];
                String queryString = urlParts[1];
                
                // Split query parameters
                String[] params = queryString.split("&");
                StringBuilder cleanParams = new StringBuilder();
                
                for (String param : params) {
                    // Skip parameters that contain template variables
                    if (!param.contains("{{") && !param.contains("}}")) {
                        if (cleanParams.length() > 0) {
                            cleanParams.append("&");
                        }
                        cleanParams.append(param);
                    }
                }
                
                // Reconstruct URL
                if (cleanParams.length() > 0) {
                    return baseUrl + "?" + cleanParams.toString();
                } else {
                    return baseUrl;
                }
            }
            
            return url; // No query parameters
            
        } catch (Exception e) {
            logger.warn("Error cleaning URL template variables: {}", e.getMessage());
            return url; // Return original URL if cleaning fails
        }
    }
    
    /**
     * Extract domain name from URL for directory organization
     */
    private String extractDomainFromUrl(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost();
            // Remove 'www.' prefix if present
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            logger.warn("Failed to extract domain from URL: {}", url);
            return "unknown";
        }
    }
}


