import { HeroSection } from "@/components/landing/hero-section";

import { FeaturesSection } from "@/components/landing/features-section";
import { PricingSection } from "@/components/landing/pricing-section";
import { CTASection } from "@/components/landing/cta-section";
import { Footer } from "@/components/landing/footer";
import { Header } from "@/components/landing/header";

export default function Home() {
  return (
    <div className="min-h-screen relative">
      {/* Full-width gradient background with fade effect */}
      <div className="fixed inset-0 pointer-events-none">
        {/* Base subtle background */}
        <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-br from-blue-50/30 via-purple-50/20 to-pink-50/30 dark:from-slate-900 dark:via-slate-800 dark:to-slate-900" />
        
        {/* Full-width animated gradient with horizontal fade */}
        <div className="absolute inset-0 overflow-hidden">
          {/* Top-left white enhancement for logo visibility */}
          <div className="absolute top-0 left-0 w-[40rem] h-56 z-10" 
               style={{
                 background: `radial-gradient(ellipse at top left, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.6) 20%, rgba(255, 255, 255, 0.3) 40%, rgba(255, 255, 255, 0.15) 60%, rgba(255, 255, 255, 0.05) 80%, transparent 100%)`
               }} />
          
          {/* Gradient fade overlay - strongest on left, fading to transparent on right */}
          <div className="absolute inset-0 animate-pulse" 
               style={{
                 background: `linear-gradient(to right, rgba(168, 85, 247, 0.4) 0%, rgba(59, 130, 246, 0.3) 5%, rgba(236, 72, 153, 0.25) 8%, rgba(168, 85, 247, 0.15) 10%, rgba(59, 130, 246, 0.08) 15%, rgba(236, 72, 153, 0.04) 20%, transparent 25%)`
               }} />
          
          {/* Dark mode gradient overlay */}
          <div className="absolute inset-0 animate-pulse dark:block hidden" 
               style={{
                 background: `linear-gradient(to right, rgba(147, 51, 234, 0.5) 0%, rgba(37, 99, 235, 0.35) 5%, rgba(236, 72, 153, 0.3) 8%, rgba(147, 51, 234, 0.2) 10%, rgba(37, 99, 235, 0.1) 15%, rgba(236, 72, 153, 0.05) 20%, transparent 25%)`
               }} />
          
          {/* Dark mode top-left enhancement */}
          <div className="absolute top-0 left-0 w-[40rem] h-56 dark:block hidden z-10" 
               style={{
                 background: `radial-gradient(ellipse at top left, rgba(15, 23, 42, 0.7) 0%, rgba(30, 41, 59, 0.5) 20%, rgba(51, 65, 85, 0.3) 40%, rgba(71, 85, 105, 0.15) 60%, rgba(100, 116, 139, 0.05) 80%, transparent 100%)`
               }} />
          
          {/* Floating orbs with horizontal positioning and fade */}
          <div className="absolute -top-10 -left-10 w-96 h-96 bg-gradient-to-br from-violet-500/50 via-purple-400/40 to-transparent rounded-full blur-3xl animate-float opacity-100" style={{animationDelay: '0s', animationDuration: '8s'}} />
          <div className="absolute top-1/3 left-1/4 w-80 h-80 bg-gradient-to-br from-blue-500/45 via-cyan-400/35 to-transparent rounded-full blur-3xl animate-float opacity-90" style={{animationDelay: '2s', animationDuration: '10s'}} />
          <div className="absolute bottom-1/4 -left-5 w-72 h-72 bg-gradient-to-br from-pink-500/40 via-rose-400/30 to-transparent rounded-full blur-3xl animate-float opacity-100" style={{animationDelay: '4s', animationDuration: '12s'}} />
          <div className="absolute bottom-10 left-1/3 w-60 h-60 bg-gradient-to-br from-indigo-500/35 via-purple-500/25 to-transparent rounded-full blur-2xl animate-float opacity-80" style={{animationDelay: '6s', animationDuration: '14s'}} />
          
          {/* Mid-screen floating orbs with reduced opacity */}
          <div className="absolute top-20 left-1/2 w-64 h-64 bg-gradient-to-br from-violet-400/20 via-purple-300/15 to-transparent rounded-full blur-3xl animate-float opacity-40" style={{animationDelay: '1s', animationDuration: '9s'}} />
          <div className="absolute bottom-32 left-2/5 w-48 h-48 bg-gradient-to-br from-blue-400/15 via-cyan-300/10 to-transparent rounded-full blur-2xl animate-float opacity-30" style={{animationDelay: '3s', animationDuration: '11s'}} />
          
          {/* Subtle sparkle layer that fades horizontally */}
          <div className="absolute inset-0 bg-gradient-to-r from-white/8 via-white/4 via-white/2 to-transparent animate-pulse opacity-60" style={{animationDuration: '3s'}} />
        </div>
        
        {/* Corner accent orbs - very subtle */}
        <div className="absolute -top-96 -left-96 w-96 h-96 bg-primary/8 rounded-full blur-3xl" />
        <div className="absolute -bottom-96 -right-96 w-96 h-96 bg-purple-500/3 rounded-full blur-3xl" />
      </div>

      {/* Page content */}
      <div className="relative z-10">
        <Header />
        <main className="pt-24">
          <HeroSection />

          <FeaturesSection />
          <PricingSection />
          <CTASection />
        </main>
        <Footer />
      </div>
    </div>
  );
}
