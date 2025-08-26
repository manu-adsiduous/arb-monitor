"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Menu, X, Target } from "lucide-react";

export function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 w-full glass-nav shadow-glass">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-20 items-center justify-between">
          {/* Glass Logo */}
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 gradient-primary rounded-2xl flex items-center justify-center shadow-glass float-gentle">
              <Target className="h-6 w-6 text-white" />
            </div>
            <div>
              <span className="text-xl font-bold text-gradient">Arb Monitor</span>
              <div className="text-xs text-slate-500 dark:text-slate-400">Compliance Monitoring</div>
            </div>
          </div>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center space-x-8">
            <a 
              href="#features" 
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Features
            </a>
            <a 
              href="#dashboard" 
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Dashboard
            </a>
            <a 
              href="#pricing" 
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Pricing
            </a>
            <a 
              href="#contact" 
              className="text-sm font-medium hover:text-primary transition-colors"
            >
              Contact
            </a>
          </nav>

          {/* Glass CTA Buttons */}
          <div className="hidden md:flex items-center space-x-4">
            <Link href="/auth/signin">
              <div className="glass-button rounded-xl px-4 py-2 text-sm font-semibold text-slate-900 dark:text-white hover:scale-105 transition-transform cursor-pointer">
                Sign In
              </div>
            </Link>
            <Link href="/auth/signup">
              <div className="glass-button rounded-xl px-6 py-2 group cursor-pointer">
                <div className="flex items-center space-x-2">
                  <div className="w-6 h-6 gradient-primary rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                    <div className="w-2 h-2 bg-white rounded-full"></div>
                  </div>
                  <span className="text-sm font-bold text-slate-900 dark:text-white">
                    Start Free Trial
                  </span>
                </div>
              </div>
            </Link>
          </div>

          {/* Glass Mobile menu button */}
          <button
            className="md:hidden glass-button rounded-xl p-3"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            aria-label="Toggle menu"
          >
            {isMenuOpen ? (
              <X className="h-5 w-5 text-slate-900 dark:text-white" />
            ) : (
              <Menu className="h-5 w-5 text-slate-900 dark:text-white" />
            )}
          </button>
        </div>

        {/* Mobile Navigation */}
        {isMenuOpen && (
          <div className="md:hidden py-4 border-t animate-fade-in">
            <nav className="flex flex-col space-y-4">
              <a 
                href="#features" 
                className="text-sm font-medium hover:text-primary transition-colors py-2"
                onClick={() => setIsMenuOpen(false)}
              >
                Features
              </a>
              <a 
                href="#dashboard" 
                className="text-sm font-medium hover:text-primary transition-colors py-2"
                onClick={() => setIsMenuOpen(false)}
              >
                Dashboard
              </a>
              <a 
                href="#pricing" 
                className="text-sm font-medium hover:text-primary transition-colors py-2"
                onClick={() => setIsMenuOpen(false)}
              >
                Pricing
              </a>
              <a 
                href="#contact" 
                className="text-sm font-medium hover:text-primary transition-colors py-2"
                onClick={() => setIsMenuOpen(false)}
              >
                Contact
              </a>
              <div className="flex flex-col space-y-2 pt-4 border-t">
                <Button variant="ghost" size="sm" asChild>
                  <Link href="/auth/signin">
                    Sign In
                  </Link>
                </Button>
                <Button size="sm" className="bg-primary hover:bg-primary/90" asChild>
                  <Link href="/auth/signup">
                    Start Free Trial
                  </Link>
                </Button>
              </div>
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}
