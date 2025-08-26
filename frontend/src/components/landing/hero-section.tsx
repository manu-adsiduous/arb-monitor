"use client";

import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowRight, Shield, Zap, TrendingUp } from "lucide-react";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden pt-8 pb-20 sm:pt-16 sm:pb-32">

      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          {/* Glass Badge */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="mb-8"
          >
            <div className="glass-card rounded-2xl px-6 py-3 inline-flex items-center space-x-2 shadow-glass">
              <div className="w-8 h-8 gradient-primary rounded-xl flex items-center justify-center">
                <Shield className="w-4 h-4 text-white" />
              </div>
              <span className="text-sm font-semibold text-slate-900 dark:text-white">
                Google AFS & RSOC Compliance Monitoring
              </span>
            </div>
          </motion.div>

          {/* Main heading */}
          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="text-4xl sm:text-6xl lg:text-7xl font-bold tracking-tight mb-8"
          >
            <span className="text-slate-900 dark:text-white">
              Monitor Ad Compliance
            </span>
            <br />
            <span className="text-gradient">
              Like a Pro
            </span>
          </motion.h1>

          {/* Subtitle */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            className="text-xl sm:text-2xl text-muted-foreground max-w-3xl mx-auto mb-12 leading-relaxed"
          >
            Ensure your Meta ads comply with Google AFS and RSOC terms. 
            Get detailed compliance analysis, scoring, and actionable insights 
            for just <span className="font-semibold text-primary">$50 per domain</span>.
          </motion.p>

          {/* Glass CTA Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.8 }}
            className="flex flex-col sm:flex-row gap-6 justify-center items-center mb-16"
          >
            <div className="glass-button rounded-2xl px-8 py-4 group cursor-pointer">
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 gradient-primary rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                  <ArrowRight className="h-6 w-6 text-white" />
                </div>
                <span className="text-lg font-bold text-slate-900 dark:text-white">
                  Start Free Analysis
                </span>
              </div>
            </div>
            
            <div className="glass-button rounded-2xl px-8 py-4 group cursor-pointer">
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-slate-200/50 dark:bg-slate-700/50 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                  <div className="w-0 h-0 border-l-[6px] border-l-slate-600 dark:border-l-slate-300 border-y-[4px] border-y-transparent ml-1"></div>
                </div>
                <span className="text-lg font-bold text-slate-900 dark:text-white">
                  Watch Demo
                </span>
              </div>
            </div>
          </motion.div>


        </div>
      </div>
    </section>
  );
}
