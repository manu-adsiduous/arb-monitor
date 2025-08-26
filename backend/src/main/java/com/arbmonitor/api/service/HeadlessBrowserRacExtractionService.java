package com.arbmonitor.api.service;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Service for extracting RAC using headless browser that can execute JavaScript
 */
@Service
public class HeadlessBrowserRacExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(HeadlessBrowserRacExtractionService.class);
    
    /**
     * Extract RAC from iframe using headless browser with JavaScript execution
     */
    public String extractRacFromIframeWithBrowser(String url) {
        WebDriver driver = null;
        try {
            logger.info("🌐 Starting headless browser for URL: {}", url);
            
            // Configure Chrome options for headless mode
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            // Set Chrome binary path for macOS
            options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            
            // Initialize WebDriver
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            // Navigate to the URL
            driver.get(url);
            logger.info("📄 Page loaded, waiting for JavaScript execution...");
            
            // Wait for page to load and JavaScript to execute
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // Wait for iframes to be present
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("iframe")));
            
            // Give additional time for JavaScript to populate iframe data
            Thread.sleep(3000);
            
            // Try to execute the JavaScript command that works in browser console
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // First, try the exact command from the user's example
            try {
                String result = (String) js.executeScript(
                    "try { " +
                    "  var element = document.querySelector('#master-1'); " +
                    "  if (element && element.name) { " +
                    "    var data = JSON.parse(element.name); " +
                    "    return data['master-1'] ? data['master-1'].kw : null; " +
                    "  } " +
                    "  return null; " +
                    "} catch(e) { return null; }"
                );
                
                if (result != null && !result.trim().isEmpty()) {
                    logger.info("🎯 Found RAC using master-1 selector: '{}'", result);
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Could not extract RAC using master-1 selector: {}", e.getMessage());
            }
            
            // If that didn't work, try to find any iframe with kw data
            try {
                List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
                logger.info("🔍 Found {} iframes on page", iframes.size());
                
                for (WebElement iframe : iframes) {
                    try {
                        String iframeId = iframe.getAttribute("id");
                        String iframeName = iframe.getAttribute("name");
                        
                        logger.info("🔍 Checking iframe: id='{}', name='{}'", iframeId, iframeName);
                        
                        if (iframeName != null && !iframeName.trim().isEmpty()) {
                            // Try to parse the name attribute as JSON
                            String result = (String) js.executeScript(
                                "try { " +
                                "  var data = JSON.parse(arguments[0]); " +
                                "  for (var key in data) { " +
                                "    if (data[key] && data[key].kw) { " +
                                "      return data[key].kw; " +
                                "    } " +
                                "  } " +
                                "  return null; " +
                                "} catch(e) { return null; }",
                                iframeName
                            );
                            
                            if (result != null && !result.trim().isEmpty()) {
                                logger.info("🎯 Found RAC in iframe {}: '{}'", iframeId, result);
                                return result;
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Error checking iframe: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.debug("Error finding iframes: {}", e.getMessage());
            }
            
            // If no iframe found, try to extract from script tags
            try {
                String result = (String) js.executeScript(
                    "try { " +
                    "  var scripts = document.querySelectorAll('script'); " +
                    "  for (var i = 0; i < scripts.length; i++) { " +
                    "    var content = scripts[i].textContent || scripts[i].innerHTML; " +
                    "    if (content.includes('master-1') && content.includes('kw')) { " +
                    "      var match = content.match(/\"kw\"\\s*:\\s*\"([^\"]+)\"/); " +
                    "      if (match) return match[1]; " +
                    "    } " +
                    "  } " +
                    "  return null; " +
                    "} catch(e) { return null; }"
                );
                
                if (result != null && !result.trim().isEmpty()) {
                    logger.info("🎯 Found RAC in script content: '{}'", result);
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Error extracting from scripts: {}", e.getMessage());
            }
            
            logger.info("❌ No RAC found in iframe or script content");
            return null;
            
        } catch (Exception e) {
            logger.error("Error extracting RAC with headless browser: {}", e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    logger.info("🌐 Headless browser closed");
                } catch (Exception e) {
                    logger.debug("Error closing browser: {}", e.getMessage());
                }
            }
        }
    }
}
