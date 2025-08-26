"use client";

import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { ArrowRight, Shield, Zap } from "lucide-react";

export function CTASection() {
  return (
    <section className="py-20 sm:py-32 relative overflow-hidden">
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-purple-600/10 to-pink-600/10" />
      <div className="absolute inset-0">
        <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-primary/20 rounded-full blur-3xl animate-float" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-purple-600/20 rounded-full blur-3xl animate-float" style={{ animationDelay: '2s' }} />
      </div>

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          viewport={{ once: true }}
          className="text-center max-w-4xl mx-auto"
        >
          {/* Main heading */}
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-6">
            <span className="text-purple-900 dark:text-purple-200">
              Ready to Ensure Your Ads
            </span>
            <br />
            <span className="text-primary">
              Are Compliant?
            </span>
          </h2>

          <p className="text-xl text-muted-foreground mb-12 leading-relaxed">
            Join hundreds of businesses who trust Arb Monitor to keep their ads compliant with Google's terms. 
            Start monitoring your first domain today and protect your advertising investments.
          </p>

          {/* Features highlight */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            viewport={{ once: true }}
            className="flex flex-col sm:flex-row items-center justify-center gap-8 mb-12"
          >
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 bg-primary/10 rounded-full">
                <Shield className="h-5 w-5 text-primary" />
              </div>
              <span className="text-foreground font-medium">100% Compliant Analysis</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 bg-primary/10 rounded-full">
                <Zap className="h-5 w-5 text-primary" />
              </div>
              <span className="text-foreground font-medium">Instant Setup</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 bg-primary/10 rounded-full">
                <ArrowRight className="h-5 w-5 text-primary" />
              </div>
              <span className="text-foreground font-medium">No Long-term Contracts</span>
            </div>
          </motion.div>

          {/* CTA Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            viewport={{ once: true }}
            className="flex flex-col sm:flex-row gap-4 justify-center items-center"
          >
            <button className="glass-button gradient-primary text-white px-8 py-4 text-lg font-semibold rounded-2xl hover:scale-105 transition-all duration-300 shadow-lg hover:shadow-xl flex items-center gap-2">
              Start Your Free Analysis
              <ArrowRight className="h-5 w-5" />
            </button>
            <button className="glass-button border-2 border-slate-300/50 dark:border-slate-600/50 text-slate-700 dark:text-slate-200 hover:border-primary/50 px-8 py-4 text-lg font-semibold rounded-2xl hover:scale-105 transition-all duration-300">
              Contact Sales
            </button>
          </motion.div>

          {/* Trust indicators */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.6 }}
            viewport={{ once: true }}
            className="mt-12 pt-8 border-t border-border/50"
          >
            <p className="text-sm text-muted-foreground mb-4">
              Trusted by agencies and businesses worldwide
            </p>
            <div className="flex flex-wrap items-center justify-center gap-8 opacity-60">
              {/* Placeholder for client logos */}
              <div className="h-8 w-24 bg-muted rounded" />
              <div className="h-8 w-20 bg-muted rounded" />
              <div className="h-8 w-28 bg-muted rounded" />
              <div className="h-8 w-22 bg-muted rounded" />
              <div className="h-8 w-26 bg-muted rounded" />
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
