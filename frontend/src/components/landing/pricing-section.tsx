"use client";

import { motion } from "framer-motion";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { 
  Check, 
  Zap, 
  Star,
  ArrowRight
} from "lucide-react";

const features = [
  "Meta Ad Library Integration",
  "Google AFS & RSOC Compliance",
  "OCR Text Analysis",
  "Compliance Scoring (0-100)",
  "Real-time Monitoring",
  "24/7 Support"
];

export function PricingSection() {
  return (
    <section id="pricing" className="py-16 sm:py-20">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="text-center mb-12"
        >
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-6 text-gradient">
            Simple, Transparent
            <br />
            Pricing
          </h2>
          <p className="text-xl text-slate-600 dark:text-slate-400 max-w-3xl mx-auto">
            No hidden fees, no complex tiers. Pay per domain and get complete access to all features.
          </p>
        </motion.div>

        {/* Pricing card */}
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2 }}
          viewport={{ once: true }}
          className="max-w-lg mx-auto"
        >
          <div className="glass-panel rounded-3xl p-8">
            <div className="text-center mb-6">
              <div className="bg-white/20 dark:bg-slate-800/20 rounded-2xl px-4 py-2 text-sm font-semibold text-slate-900 dark:text-white inline-flex items-center gap-2 mb-6">
                <div className="w-4 h-4 gradient-primary rounded-full flex items-center justify-center">
                  <Star className="w-3 h-3 text-white" />
                </div>
                Simple Pricing
              </div>
              
              <div className="mb-4">
                <div className="inline-flex items-center justify-center w-16 h-16 gradient-primary rounded-2xl mb-4">
                  <Zap className="w-8 h-8 text-white" />
                </div>
              </div>
              <h3 className="text-2xl font-bold mb-2 text-slate-900 dark:text-white">Per Domain Monitoring</h3>
              <div className="mb-4">
                <span className="text-5xl font-bold text-gradient">$50</span>
                <span className="text-slate-600 dark:text-slate-400 ml-2">/domain</span>
              </div>
              <p className="text-slate-600 dark:text-slate-400 max-w-md mx-auto">
                Complete compliance monitoring for one domain with all features included.
              </p>
            </div>

            <div className="mb-8">
              {/* Features in single column */}
              <div className="grid grid-cols-1 gap-3 max-w-md mx-auto">
                {features.map((feature, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <div className="flex-shrink-0 w-5 h-5 gradient-primary rounded-full flex items-center justify-center">
                      <Check className="w-3 h-3 text-white" />
                    </div>
                    <span className="text-sm text-slate-900 dark:text-white">{feature}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* CTA Button */}
            <div className="text-center space-y-4">
              <div className="bg-gradient-to-r from-purple-500 to-blue-600 text-white font-semibold py-4 px-8 rounded-2xl cursor-pointer shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-[1.02] inline-flex items-center gap-2">
                Start Monitoring Now
                <ArrowRight className="h-5 w-5" />
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-400">
                No setup fees • Cancel anytime • 7-day money-back guarantee
              </p>
            </div>
          </div>
        </motion.div>


      </div>
    </section>
  );
}
