import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/sonner";
import AuthProvider from "@/components/providers/session-provider";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "Arb Monitor - Ad Compliance Monitoring for Google AFS & RSOC",
  description: "Ensure your Meta ads comply with Google AFS and RSOC terms. Get detailed compliance analysis, scoring, and shareable reports for just $50 per domain.",
  keywords: ["ad compliance", "Google AFS", "RSOC", "Meta ads", "compliance monitoring", "advertising"],
  authors: [{ name: "Arb Monitor Team" }],
  openGraph: {
    title: "Arb Monitor - Ad Compliance Monitoring",
    description: "Monitor your Meta ads for Google AFS & RSOC compliance. Professional analysis for $50 per domain.",
    type: "website",
    locale: "en_US",
  },
  twitter: {
    card: "summary_large_image",
    title: "Arb Monitor - Ad Compliance Monitoring",
    description: "Monitor your Meta ads for Google AFS & RSOC compliance. Professional analysis for $50 per domain.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${inter.variable} ${jetbrainsMono.variable} font-sans antialiased`}
      >
        <AuthProvider>
          <ThemeProvider
            attribute="class"
            defaultTheme="light"
            enableSystem
            disableTransitionOnChange
          >
            {children}
            <Toaster />
          </ThemeProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
