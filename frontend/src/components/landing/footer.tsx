"use client";

import { Target, Mail, Twitter, Github, Linkedin } from "lucide-react";
import { Button } from "@/components/ui/button";

const footerLinks = {
  product: [
    { name: "Features", href: "#features" },
    { name: "Dashboard", href: "#dashboard" },
    { name: "Pricing", href: "#pricing" },
    { name: "API Documentation", href: "/docs" },
  ],
  company: [
    { name: "About Us", href: "/about" },
    { name: "Blog", href: "/blog" },
    { name: "Careers", href: "/careers" },
    { name: "Contact", href: "/contact" },
  ],
  legal: [
    { name: "Privacy Policy", href: "/privacy" },
    { name: "Terms of Service", href: "/terms" },
    { name: "Cookie Policy", href: "/cookies" },
    { name: "Compliance", href: "/compliance" },
  ],
  support: [
    { name: "Help Center", href: "/help" },
    { name: "Status", href: "/status" },
    { name: "Community", href: "/community" },
    { name: "Contact Support", href: "/support" },
  ],
};

export function Footer() {
  return (
    <footer id="contact" className="glass-panel">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Main footer content */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-8 mb-8">
          {/* Brand section */}
          <div className="lg:col-span-2">
            <div className="flex items-center space-x-2 mb-4">
              <div className="relative">
                <div className="w-8 h-8 gradient-primary rounded-xl flex items-center justify-center">
                  <Target className="h-5 w-5 text-white" />
                </div>
                <div className="absolute -top-1 -right-1 h-3 w-3 bg-green-400 rounded-full" />
              </div>
              <span className="text-xl font-bold text-gradient">
                Arb Monitor
              </span>
            </div>
            <p className="text-slate-600 dark:text-slate-400 mb-6 max-w-sm">
              Ensure your Meta ads comply with Google AFS and RSOC terms. 
              Professional compliance monitoring for just $50 per domain.
            </p>
            <div className="flex space-x-4">
              <div className="glass-button p-3 rounded-xl hover:scale-110 transition-transform cursor-pointer">
                <Twitter className="h-4 w-4 text-slate-600 dark:text-slate-300" />
              </div>
              <div className="glass-button p-3 rounded-xl hover:scale-110 transition-transform cursor-pointer">
                <Linkedin className="h-4 w-4 text-slate-600 dark:text-slate-300" />
              </div>
              <div className="glass-button p-3 rounded-xl hover:scale-110 transition-transform cursor-pointer">
                <Github className="h-4 w-4 text-slate-600 dark:text-slate-300" />
              </div>
              <div className="glass-button p-3 rounded-xl hover:scale-110 transition-transform cursor-pointer">
                <Mail className="h-4 w-4 text-slate-600 dark:text-slate-300" />
              </div>
            </div>
          </div>

          {/* Links sections */}
          <div>
            <h3 className="font-semibold text-slate-900 dark:text-white mb-4">Product</h3>
            <ul className="space-y-2">
              {footerLinks.product.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-slate-600 dark:text-slate-400 hover:text-primary transition-colors text-sm"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-slate-900 dark:text-white mb-4">Company</h3>
            <ul className="space-y-2">
              {footerLinks.company.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-slate-600 dark:text-slate-400 hover:text-primary transition-colors text-sm"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-slate-900 dark:text-white mb-4">Support</h3>
            <ul className="space-y-2">
              {footerLinks.support.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-slate-600 dark:text-slate-400 hover:text-primary transition-colors text-sm"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-slate-900 dark:text-white mb-4">Legal</h3>
            <ul className="space-y-2">
              {footerLinks.legal.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-slate-600 dark:text-slate-400 hover:text-primary transition-colors text-sm"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Newsletter signup */}
        <div className="border-t border-slate-200/20 dark:border-slate-700/40 pt-8 mb-8">
          <div className="max-w-md mx-auto text-center">
            <h3 className="font-semibold text-slate-900 dark:text-white mb-2">Stay Updated</h3>
            <p className="text-slate-600 dark:text-slate-400 text-sm mb-4">
              Get the latest compliance updates and feature announcements.
            </p>
            <div className="flex gap-3 max-w-sm mx-auto">
              <input
                type="email"
                placeholder="Enter your email"
                className="glass-input flex-1 px-4 py-3 text-sm rounded-2xl border border-slate-200/30 dark:border-slate-700/50 text-slate-700 dark:text-slate-200 placeholder:text-slate-500 dark:placeholder:text-slate-400 focus:border-primary/50 focus:ring-2 focus:ring-primary/20 transition-all"
              />
              <button className="glass-button gradient-primary text-white px-6 py-3 rounded-2xl text-sm font-semibold hover:scale-105 transition-all duration-300 shadow-md hover:shadow-lg">
                Subscribe
              </button>
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="w-full h-px bg-white/20 dark:bg-slate-700/40 pt-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-slate-600 dark:text-slate-400 text-sm">
            © 2024 Arb Monitor. All rights reserved.
          </p>
          <div className="flex items-center gap-4 text-sm text-slate-600 dark:text-slate-400">
            <span>Made with ❤️ for compliance professionals</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
