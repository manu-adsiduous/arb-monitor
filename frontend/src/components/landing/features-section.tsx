"use client";

import { motion } from "framer-motion";
import { 
  Search, 
  Shield, 
  Zap, 
  BarChart3,
  Share2,
  Bell
} from "lucide-react";

const features = [
  {
    icon: Search,
    title: "Meta Ad Library Integration",
    description: "Automatically fetch and analyze all active Meta ads for any domain using the official Meta Ad Library API. Our system continuously monitors ad campaigns in real-time, ensuring you never miss a compliance issue.",
    gradient: "from-blue-500 to-cyan-500"
  },
  {
    icon: Shield,
    title: "Google AFS Compliance",
    description: "Check compliance against Google AdSense for Search and RSOC terms with detailed rule explanations. Our comprehensive rule engine covers medical claims, financial promises, misleading content, and regulatory requirements across multiple jurisdictions.",
    gradient: "from-green-500 to-emerald-500"
  },
  {
    icon: Zap,
    title: "OCR Text Analysis", 
    description: "Advanced image text extraction to analyze text within ad creatives using cutting-edge OCR technology. We scan every pixel of your ad images to detect hidden text, watermarks, and compliance violations that traditional tools miss.",
    gradient: "from-purple-500 to-violet-500"
  },
  {
    icon: BarChart3,
    title: "Compliance Scoring",
    description: "Get detailed compliance scores with weighted analysis based on violation severity and rule importance. Our proprietary scoring algorithm provides actionable insights with clear recommendations for improving your ad compliance rates.",
    gradient: "from-orange-500 to-red-500"
  },
  {
    icon: Share2,
    title: "Shareable Reports",
    description: "Generate public compliance reports with unique URLs that can be shared with stakeholders without login. Perfect for client presentations, compliance audits, and demonstrating your commitment to advertising standards.",
    gradient: "from-teal-500 to-blue-500"
  },
  {
    icon: Bell,
    title: "Real-time Monitoring",
    description: "Continuous monitoring with instant notifications when compliance issues are detected. Set up custom alerts for different violation types and receive immediate warnings before your ads are flagged by advertising platforms.",
    gradient: "from-pink-500 to-rose-500"
  }
];

export function FeaturesSection() {
  return (
    <section id="features" className="py-24 sm:py-32">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="text-center mb-20"
        >
          <h2 className="text-4xl sm:text-5xl lg:text-6xl font-bold mb-6 text-gradient">
            Powerful Features
          </h2>
          <p className="text-xl text-slate-600 dark:text-slate-400 max-w-3xl mx-auto">
            Everything you need to ensure your advertising campaigns stay compliant and perform at their best
          </p>
        </motion.div>

        {/* Features timeline */}
        <div className="relative max-w-6xl mx-auto">
          {/* Central timeline line */}
          <div className="absolute left-1/2 transform -translate-x-px h-full w-0.5 bg-gradient-to-b from-purple-500 via-blue-500 to-teal-500 opacity-30"></div>
          
          {features.map((feature, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, x: index % 2 === 0 ? -50 : 50 }}
              whileInView={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.8, delay: index * 0.2 }}
              viewport={{ once: true }}
              className={`relative flex items-center mb-20 ${
                index % 2 === 0 ? 'flex-row' : 'flex-row-reverse'
              }`}
            >
              {/* Content */}
              <div className={`w-5/12 ${index % 2 === 0 ? 'pr-12 text-right' : 'pl-12 text-left'}`}>
                <div className="glass-panel rounded-3xl p-8 hover:scale-105 transition-all duration-500">
                  <h3 className="text-2xl font-bold text-slate-900 dark:text-white mb-4">
                    {feature.title}
                  </h3>
                  <p className="text-slate-600 dark:text-slate-400 leading-relaxed">
                    {feature.description}
                  </p>
                </div>
              </div>
              
              {/* Central icon */}
              <div className="w-2/12 flex justify-center">
                <div className="relative">
                  {/* Pulse animation */}
                  <div className={`absolute inset-0 bg-gradient-to-r ${feature.gradient} rounded-full animate-ping opacity-20`}></div>
                  <div className={`relative w-16 h-16 bg-gradient-to-r ${feature.gradient} rounded-full flex items-center justify-center shadow-2xl`}>
                    <feature.icon className="h-8 w-8 text-white" />
                  </div>
                </div>
              </div>
              
              {/* Spacer */}
              <div className="w-5/12"></div>
            </motion.div>
          ))}
        </div>

        {/* Bottom CTA */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.4 }}
          viewport={{ once: true }}
          className="text-center mt-20"
        >
          <div className="glass-panel rounded-3xl p-12 max-w-2xl mx-auto">
            <h3 className="text-2xl font-bold text-gradient mb-4">
              Ready to Transform Your Ad Compliance?
            </h3>
            <p className="text-slate-600 dark:text-slate-400 mb-8">
              Join hundreds of businesses who trust us to keep their advertising compliant and their reputation protected.
            </p>
            <button className="bg-gradient-to-r from-purple-500 to-blue-600 text-white px-8 py-4 rounded-2xl font-semibold text-lg hover:shadow-2xl transition-all duration-300 hover:scale-105">
              Start Your Free Analysis
            </button>
          </div>
        </motion.div>
      </div>
    </section>
  );
}