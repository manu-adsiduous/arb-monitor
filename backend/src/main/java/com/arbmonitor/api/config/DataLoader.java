package com.arbmonitor.api.config;

import com.arbmonitor.api.model.ComplianceRule;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.User;
import com.arbmonitor.api.repository.ComplianceRuleRepository;
import com.arbmonitor.api.repository.DomainRepository;
import com.arbmonitor.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ComplianceRuleRepository complianceRuleRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Loading demo data...");

        // Create demo user if not exists
        if (!userRepository.existsById(1L)) {
            User demoUser = new User();
            demoUser.setEmail("demo@example.com");
            demoUser.setName("Demo User");
            demoUser.setSubscriptionStatus(User.SubscriptionStatus.ACTIVE);
            demoUser.setRole(User.UserRole.USER);
            demoUser.setCognitoSub("demo-123");
            
            demoUser = userRepository.save(demoUser);
            logger.info("Created demo user with ID: {}", demoUser.getId());
            
            // Create admin user
            User adminUser = new User();
            adminUser.setEmail("admin@example.com");
            adminUser.setName("Admin User");
            adminUser.setSubscriptionStatus(User.SubscriptionStatus.ACTIVE);
            adminUser.setRole(User.UserRole.ADMIN);
            adminUser.setCognitoSub("admin-123");
            
            adminUser = userRepository.save(adminUser);
            logger.info("Created admin user with ID: {}", adminUser.getId());

            // Create demo domains
            Domain domain1 = new Domain();
            domain1.setDomainName("bestoptions.net");
            domain1.setUser(demoUser);
            domain1.setStatus(Domain.MonitoringStatus.ACTIVE);
            domain1.setShareToken(UUID.randomUUID().toString());
            domainRepository.save(domain1);

            Domain domain2 = new Domain();
            domain2.setDomainName("dealsbysearch.com");
            domain2.setUser(demoUser);
            domain2.setStatus(Domain.MonitoringStatus.ACTIVE);
            domain2.setShareToken(UUID.randomUUID().toString());
            domainRepository.save(domain2);

            logger.info("Created {} demo domains", 2);
            
            // Create sample compliance rules
            createSampleComplianceRules();
        } else {
            logger.info("Demo data already exists, skipping...");
        }
    }
    
    private void createSampleComplianceRules() {
        if (complianceRuleRepository.count() == 0) {
            // Critical rules
            ComplianceRule rule1 = new ComplianceRule();
            rule1.setRuleName("Misleading Claims");
            rule1.setDescription("Ad contains misleading or false claims about product benefits or effectiveness");
            rule1.setExamples("'Lose 50 pounds in 1 week guaranteed', 'Cure cancer with this miracle pill'");
            rule1.setCategory(ComplianceRule.RuleCategory.CREATIVE_CONTENT);
            rule1.setSeverity(ComplianceRule.RuleSeverity.CRITICAL);
            rule1.setRulePattern("(?i)(guarantee|cure|miracle|instant|overnight|magic)");
            complianceRuleRepository.save(rule1);

            ComplianceRule rule2 = new ComplianceRule();
            rule2.setRuleName("Medical Claims Without Substantiation");
            rule2.setDescription("Making medical or health claims without proper FDA approval or scientific backing");
            rule2.setExamples("'FDA approved' (when not true), 'Clinically proven' (without studies)");
            rule2.setCategory(ComplianceRule.RuleCategory.MEDICAL_CLAIMS);
            rule2.setSeverity(ComplianceRule.RuleSeverity.CRITICAL);
            rule2.setRulePattern("(?i)(fda approved|clinically proven|doctor recommended)");
            complianceRuleRepository.save(rule2);

            // Major rules
            ComplianceRule rule3 = new ComplianceRule();
            rule3.setRuleName("Before/After Images Manipulation");
            rule3.setDescription("Using doctored, misleading, or unrepresentative before/after transformation images");
            rule3.setExamples("Heavily photoshopped results, using different people, unrealistic transformations");
            rule3.setCategory(ComplianceRule.RuleCategory.IMAGE_TEXT);
            rule3.setSeverity(ComplianceRule.RuleSeverity.MAJOR);
            complianceRuleRepository.save(rule3);

            ComplianceRule rule4 = new ComplianceRule();
            rule4.setRuleName("Missing Referrer Parameters");
            rule4.setDescription("Landing page URL missing required referrer tracking parameters for compliance monitoring");
            rule4.setExamples("URL should include ref=fb_ad or similar tracking parameter");
            rule4.setCategory(ComplianceRule.RuleCategory.REFERRER_PARAMETER);
            rule4.setSeverity(ComplianceRule.RuleSeverity.MAJOR);
            rule4.setRulePattern("ref=|utm_source=|fbclid=");
            complianceRuleRepository.save(rule4);

            // Minor rules
            ComplianceRule rule5 = new ComplianceRule();
            rule5.setRuleName("Excessive Text in Images");
            rule5.setDescription("Ad images contain more than 20% text overlay, violating Facebook's text guidelines");
            rule5.setExamples("Images with large text blocks, heavy promotional text overlay");
            rule5.setCategory(ComplianceRule.RuleCategory.IMAGE_TEXT);
            rule5.setSeverity(ComplianceRule.RuleSeverity.MINOR);
            complianceRuleRepository.save(rule5);

            ComplianceRule rule6 = new ComplianceRule();
            rule6.setRuleName("Generic Landing Page");
            rule6.setDescription("Landing page content doesn't match ad creative or lacks specific product information");
            rule6.setExamples("Ad shows specific product but lands on generic homepage");
            rule6.setCategory(ComplianceRule.RuleCategory.LANDING_PAGE);
            rule6.setSeverity(ComplianceRule.RuleSeverity.MINOR);
            complianceRuleRepository.save(rule6);

            logger.info("Created {} sample compliance rules", 6);
        }
    }
}


