package com.arbmonitor.api.service;

import com.arbmonitor.api.model.ComplianceRule;
import com.arbmonitor.api.repository.ComplianceRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Service for managing compliance rules based on Google AFS & RSOC guidelines
 * Rules sourced from: https://bestoptions.net/creative-compliance/
 */
@Service
@Transactional
public class ComplianceRuleService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceRuleService.class);

    @Autowired
    private ComplianceRuleRepository complianceRuleRepository;

    /**
     * Initialize compliance rules based on Best Options guidelines
     */
    @PostConstruct
    public void initializeComplianceRules() {
        if (complianceRuleRepository.count() == 0) {
            logger.info("Initializing Google AFS & RSOC compliance rules...");
            createDefaultRules();
            logger.info("Successfully initialized {} compliance rules", complianceRuleRepository.count());
        }
    }

    /**
     * Create default compliance rules based on Best Options Creative Compliance guidelines
     * Source: https://bestoptions.net/creative-compliance/
     */
    private void createDefaultRules() {
        List<ComplianceRule> rules = Arrays.asList(
            // 1. General Guidelines - No Inducement to Click
            new ComplianceRule(
                "No Inducement to Click",
                "Ads must not contain language encouraging users to click, search, or interact with keywords. " +
                "Forbidden phrases include 'search now,' 'click here,' 'support us,' 'tap here,' etc. " +
                "Instead use 'discover' or 'learn more.'",
                ComplianceRule.RuleCategory.CREATIVE_CONTENT,
                ComplianceRule.RuleSeverity.CRITICAL
            ),

            // 2. No False Promises
            new ComplianceRule(
                "No False Promises",
                "Misleading or outlandish claims like 'Free X' or 'Jeep for $10,000' must be avoided. " +
                "Avoid claiming anything as free or no cost unless completely verifiable. " +
                "Avoid mentioning prices unless completely true.",
                ComplianceRule.RuleCategory.CLAIM_SUBSTANTIATION,
                ComplianceRule.RuleSeverity.CRITICAL
            ),

            // 3. No Implied Functionality
            new ComplianceRule(
                "No Implied Functionality", 
                "Ads must not imply that functionality (e.g., 'see prices' or 'get quotes') is available " +
                "if it's not present on the landing page. If the ad promises a feature like 'check rates,' " +
                "the user should be able to perform that action on the landing page.",
                ComplianceRule.RuleCategory.LANDING_PAGE,
                ComplianceRule.RuleSeverity.MAJOR
            ),

            // 4. Landing Page Alignment
            new ComplianceRule(
                "Landing Page Alignment",
                "Always fulfill in the article whatever you promise in the creative. " +
                "Ensure the content on the landing page aligns with user expectations based on the ad " +
                "(no bait-and-switch). Make sure your claim is verifiable in the article.",
                ComplianceRule.RuleCategory.LANDING_PAGE,
                ComplianceRule.RuleSeverity.CRITICAL
            ),

            // 5. No Misleading Claims
            new ComplianceRule(
                "No Misleading Claims",
                "Avoid sensational phrases like 'doctors hate,' 'one weird trick,' 'secret that,' " +
                "'they don't want you to know,' etc. Focus on promoting content naturally rather than " +
                "using clickbait tactics.",
                ComplianceRule.RuleCategory.CREATIVE_CONTENT,
                ComplianceRule.RuleSeverity.MAJOR
            ),

            // 6. Verifiable Claims Requirement
            new ComplianceRule(
                "No Unverifiable Claims",
                "Claims like 'top-rated' or references to 'research' need a clear source on the landing page. " +
                "If the ad states 'Top-rated smartphones of 2024,' the landing page should list the source " +
                "of these ratings. All information published must be true and verifiable.",
                ComplianceRule.RuleCategory.CLAIM_SUBSTANTIATION,
                ComplianceRule.RuleSeverity.MAJOR
            ),

            // 7. Automotive Specific Rules
            new ComplianceRule(
                "Automotive AI Image Compliance",
                "Avoid using badges or logos on AI-generated images for cars, as they may mislead users. " +
                "When referencing a specific make or model, ensure AI-generated images closely resemble " +
                "the actual vehicle. Futuristic designs are acceptable for generic automotive content only.",
                ComplianceRule.RuleCategory.IMAGE_TEXT,
                ComplianceRule.RuleSeverity.MINOR
            ),

            // 8. No Fake Scarcity
            new ComplianceRule(
                "No Fake Scarcity",
                "Ads must not imply time-limited offers or scarcity (e.g., 'only 10 left') when the " +
                "scarcity isn't real. Avoid creating false urgency unless the limitation is genuine.",
                ComplianceRule.RuleCategory.CREATIVE_CONTENT,
                ComplianceRule.RuleSeverity.MAJOR
            ),

            // 9. Realistic Pricing
            new ComplianceRule(
                "No Extreme Pricing Claims",
                "Avoid unverifiable claims such as 'Jeep for $1500' or insurance rates that are abnormally low. " +
                "Pricing claims must be realistic and achievable. If the ad says 'affordable' or 'cheap,' " +
                "the landing page should validate this claim.",
                ComplianceRule.RuleCategory.CLAIM_SUBSTANTIATION,
                ComplianceRule.RuleSeverity.MAJOR
            ),

            // 10. Content Flow and Placement
            new ComplianceRule(
                "Proper Content Structure",
                "Ensure that the first paragraph of any landing page content is at least 50 words long " +
                "to prevent ads from appearing too high on the page. Avoid phrases in content that " +
                "encourage ad interaction, especially above keyword blocks.",
                ComplianceRule.RuleCategory.LANDING_PAGE,
                ComplianceRule.RuleSeverity.MINOR
            ),

            // 11. RSOC Specific - Promise Information Discovery
            new ComplianceRule(
                "RSOC Information Discovery",
                "For RSOC pages, ads should tell users they can learn more, discover, or find out more " +
                "about a topic. Example: 'Find out more about different types of dental implants.' " +
                "Avoid implying transactions if only informational content is provided.",
                ComplianceRule.RuleCategory.GENERAL_COMPLIANCE,
                ComplianceRule.RuleSeverity.MINOR
            ),

            // 12. No Leading Text Violations
            new ComplianceRule(
                "No Leading Text",
                "Avoid phrases in content (especially above keyword blocks) that encourage ad interaction, " +
                "such as 'Check out these top deals' or 'Click below to learn more.' " +
                "There must be no leading text before keyword blocks like 'Here are some ways to do it.'",
                ComplianceRule.RuleCategory.LANDING_PAGE,
                ComplianceRule.RuleSeverity.MINOR
            )
        );

        // Set rule patterns for automated detection
        setRulePatterns(rules);

        // Save all rules
        complianceRuleRepository.saveAll(rules);
    }

    /**
     * Set regex patterns for automated rule detection
     */
    private void setRulePatterns(List<ComplianceRule> rules) {
        for (ComplianceRule rule : rules) {
            switch (rule.getRuleName()) {
                case "No Inducement to Click":
                    rule.setRulePattern("(?i)(click\\s+(here|now|below)|search\\s+now|tap\\s+(here|now)|support\\s+us|visit\\s+these\\s+links|select\\s+your|choose\\s+your)");
                    break;
                    
                case "No False Promises":
                    rule.setRulePattern("(?i)(free\\s+(phone|iphone|car|truck|laptop)|100%\\s+free|completely\\s+free|no\\s+cost|zero\\s+cost|\\$0\\s+cost)");
                    break;
                    
                case "No Implied Functionality":
                    rule.setRulePattern("(?i)(see\\s+prices|check\\s+(prices|rates)|get\\s+(quotes|rates)|compare\\s+prices|apply\\s+now|shop\\s+now|buy\\s+now)");
                    break;
                    
                case "No Misleading Claims":
                    rule.setRulePattern("(?i)(doctors\\s+hate|one\\s+weird\\s+trick|secret\\s+that|they\\s+don't\\s+want|miracle\\s+cure|instant\\s+results|guaranteed\\s+results)");
                    break;
                    
                case "No Unverifiable Claims":
                    rule.setRulePattern("(?i)(top\\s+rated|best\\s+rated|#1\\s+rated|research\\s+shows|studies\\s+show|experts\\s+say|clinically\\s+proven)");
                    break;
                    
                case "No Fake Scarcity":
                    rule.setRulePattern("(?i)(only\\s+\\d+\\s+left|limited\\s+time|expires\\s+(today|soon)|hurry|act\\s+fast|while\\s+supplies\\s+last|few\\s+remaining)");
                    break;
                    
                case "No Extreme Pricing Claims":
                    rule.setRulePattern("(?i)(car|truck|suv|jeep|ford|toyota|honda).{0,20}\\$([1-9]\\d{0,2}|1[0-4]\\d{2}|1500)");
                    break;
                    
                case "No Leading Text":
                    rule.setRulePattern("(?i)(check\\s+out\\s+these|explore\\s+this|see\\s+below|click\\s+below|here\\s+are\\s+some\\s+ways)");
                    break;
            }
        }
    }

    /**
     * Get all active compliance rules
     */
    public List<ComplianceRule> getActiveRules() {
        return complianceRuleRepository.findByActiveTrue();
    }

    /**
     * Get rules by category
     */
    public List<ComplianceRule> getRulesByCategory(ComplianceRule.RuleCategory category) {
        return complianceRuleRepository.findByCategoryAndActiveTrue(category);
    }

    /**
     * Get rules by severity
     */
    public List<ComplianceRule> getRulesBySeverity(ComplianceRule.RuleSeverity severity) {
        return complianceRuleRepository.findBySeverityAndActiveTrue(severity);
    }

    /**
     * Create a new custom compliance rule
     */
    public ComplianceRule createRule(String ruleName, String description, 
                                   ComplianceRule.RuleCategory category, 
                                   ComplianceRule.RuleSeverity severity,
                                   String rulePattern) {
        ComplianceRule rule = new ComplianceRule(ruleName, description, category, severity);
        rule.setRulePattern(rulePattern);
        return complianceRuleRepository.save(rule);
    }

    /**
     * Update an existing rule
     */
    public ComplianceRule updateRule(Long ruleId, String description, String rulePattern, Boolean active) {
        ComplianceRule rule = complianceRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
        
        if (description != null) rule.setDescription(description);
        if (rulePattern != null) rule.setRulePattern(rulePattern);
        if (active != null) rule.setActive(active);
        
        return complianceRuleRepository.save(rule);
    }

    /**
     * Deactivate a rule
     */
    public void deactivateRule(Long ruleId) {
        ComplianceRule rule = complianceRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
        rule.setActive(false);
        complianceRuleRepository.save(rule);
    }

    /**
     * Get rule statistics
     */
    public ComplianceRuleStats getRuleStats() {
        List<ComplianceRule> allRules = complianceRuleRepository.findAll();
        long activeCount = allRules.stream().filter(ComplianceRule::getActive).count();
        long criticalCount = complianceRuleRepository.findBySeverityAndActiveTrue(ComplianceRule.RuleSeverity.CRITICAL).size();
        long majorCount = complianceRuleRepository.findBySeverityAndActiveTrue(ComplianceRule.RuleSeverity.MAJOR).size();
        long minorCount = complianceRuleRepository.findBySeverityAndActiveTrue(ComplianceRule.RuleSeverity.MINOR).size();
        
        return new ComplianceRuleStats(allRules.size(), (int) activeCount, 
                                     (int) criticalCount, (int) majorCount, (int) minorCount);
    }

    /**
     * Stats class for rule information
     */
    public static class ComplianceRuleStats {
        private final int totalRules;
        private final int activeRules;
        private final int criticalRules;
        private final int majorRules;
        private final int minorRules;

        public ComplianceRuleStats(int totalRules, int activeRules, int criticalRules, int majorRules, int minorRules) {
            this.totalRules = totalRules;
            this.activeRules = activeRules;
            this.criticalRules = criticalRules;
            this.majorRules = majorRules;
            this.minorRules = minorRules;
        }

        // Getters
        public int getTotalRules() { return totalRules; }
        public int getActiveRules() { return activeRules; }
        public int getCriticalRules() { return criticalRules; }
        public int getMajorRules() { return majorRules; }
        public int getMinorRules() { return minorRules; }
    }
}
