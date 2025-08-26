"use client";

import { motion } from "framer-motion";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { 
  Search, 
  TrendingUp, 
  AlertCircle, 
  CheckCircle2, 
  ExternalLink,
  BarChart3,
  Eye
} from "lucide-react";

const mockAds = [
  {
    id: 1,
    headline: "Best Weight Loss Supplement - Proven Results!",
    description: "Lose 30 pounds in 30 days with our miracle formula. No diet or exercise required!",
    score: 25,
    status: "critical",
    violations: ["Unrealistic claims", "Medical claims", "No substantiation"]
  },
  {
    id: 2,
    headline: "Premium Web Hosting - 50% Off First Year",
    description: "Reliable hosting with 99.9% uptime guarantee. Perfect for small businesses.",
    score: 92,
    status: "excellent",
    violations: []
  },
  {
    id: 3,
    headline: "Learn Digital Marketing Online",
    description: "Master digital marketing with our comprehensive course. Start your career today.",
    score: 78,
    status: "good",
    violations: ["Minor: Generic claims"]
  },
  {
    id: 4,
    headline: "Crypto Trading Bot - 500% Returns Guaranteed",
    description: "Our AI bot guarantees massive returns. Join thousands of successful traders!",
    score: 15,
    status: "critical",
    violations: ["Financial guarantees", "Unrealistic returns", "Misleading claims"]
  }
];

const getStatusColor = (status: string) => {
  switch (status) {
    case "excellent": return "bg-green-50 border-green-200 text-green-800 dark:bg-green-950/50 dark:border-green-800 dark:text-green-200";
    case "good": return "bg-lime-50 border-lime-200 text-lime-800 dark:bg-lime-950/50 dark:border-lime-800 dark:text-lime-200";
    case "warning": return "bg-yellow-50 border-yellow-200 text-yellow-800 dark:bg-yellow-950/50 dark:border-yellow-800 dark:text-yellow-200";
    case "poor": return "bg-orange-50 border-orange-200 text-orange-800 dark:bg-orange-950/50 dark:border-orange-800 dark:text-orange-200";
    case "critical": return "bg-red-50 border-red-200 text-red-800 dark:bg-red-950/50 dark:border-red-800 dark:text-red-200";
    default: return "bg-gray-50 border-gray-200 text-gray-800 dark:bg-gray-950/50 dark:border-gray-800 dark:text-gray-200";
  }
};

const getScoreColor = (score: number) => {
  if (score >= 90) return "text-green-600 dark:text-green-400";
  if (score >= 75) return "text-lime-600 dark:text-lime-400";
  if (score >= 60) return "text-yellow-600 dark:text-yellow-400";
  if (score >= 40) return "text-orange-600 dark:text-orange-400";
  return "text-red-600 dark:text-red-400";
};

export function DashboardMockup() {
  return (
    <section id="dashboard" className="py-20 sm:py-32 bg-gradient-to-b from-transparent to-purple-50/30 dark:to-slate-900/30">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-6">
            <span className="text-foreground">
              Interactive Dashboard
            </span>
            <br />
            <span className="text-primary">
              Preview
            </span>
          </h2>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto mb-8">
            See how easy it is to monitor your ad compliance with our intuitive dashboard.
            Click around and explore the interface!
          </p>
        </motion.div>

        {/* Dashboard mockup */}
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2 }}
          viewport={{ once: true }}
          className="max-w-6xl mx-auto"
        >
          <Card className="border-0 shadow-2xl glass-card overflow-hidden">
            {/* Dashboard header */}
            <CardHeader className="border-b bg-gradient-to-r from-primary/5 to-purple-600/5 p-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                  <h3 className="text-2xl font-bold mb-2">example.com Compliance Report</h3>
                  <p className="text-muted-foreground">Last updated: 2 minutes ago</p>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-center">
                    <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">67</div>
                    <div className="text-sm text-muted-foreground">Overall Score</div>
                  </div>
                  <Button size="sm" variant="outline" className="gap-2">
                    <ExternalLink className="h-4 w-4" />
                    Share Report
                  </Button>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-6">
              {/* Search and filters */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.6, delay: 0.4 }}
                viewport={{ once: true }}
                className="mb-6 flex flex-col sm:flex-row gap-4"
              >
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <input
                    type="text"
                    placeholder="Search ads..."
                    className="w-full pl-10 pr-4 py-2 border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary/20"
                  />
                </div>
                <div className="flex gap-2">
                  <Badge variant="secondary" className="cursor-pointer hover:bg-primary/10">
                    All (4)
                  </Badge>
                  <Badge variant="outline" className="cursor-pointer hover:bg-red-50 dark:hover:bg-red-950/20">
                    Critical (2)
                  </Badge>
                  <Badge variant="outline" className="cursor-pointer hover:bg-green-50 dark:hover:bg-green-950/20">
                    Compliant (1)
                  </Badge>
                </div>
              </motion.div>

              {/* Stats overview */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: 0.6 }}
                viewport={{ once: true }}
                className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8"
              >
                <div className="text-center p-4 bg-blue-50 dark:bg-blue-950/50 rounded-lg border border-blue-200 dark:border-blue-800">
                  <BarChart3 className="h-6 w-6 text-blue-600 dark:text-blue-400 mx-auto mb-2" />
                  <div className="text-xl font-bold text-blue-900 dark:text-blue-100">4</div>
                  <div className="text-xs text-blue-700 dark:text-blue-300">Total Ads</div>
                </div>
                <div className="text-center p-4 bg-red-50 dark:bg-red-950/50 rounded-lg border border-red-200 dark:border-red-800">
                  <AlertCircle className="h-6 w-6 text-red-600 dark:text-red-400 mx-auto mb-2" />
                  <div className="text-xl font-bold text-red-900 dark:text-red-100">2</div>
                  <div className="text-xs text-red-700 dark:text-red-300">Violations</div>
                </div>
                <div className="text-center p-4 bg-green-50 dark:bg-green-950/50 rounded-lg border border-green-200 dark:border-green-800">
                  <CheckCircle2 className="h-6 w-6 text-green-600 dark:text-green-400 mx-auto mb-2" />
                  <div className="text-xl font-bold text-green-900 dark:text-green-100">1</div>
                  <div className="text-xs text-green-700 dark:text-green-300">Compliant</div>
                </div>
                <div className="text-center p-4 bg-purple-50 dark:bg-purple-950/50 rounded-lg border border-purple-200 dark:border-purple-800">
                  <TrendingUp className="h-6 w-6 text-purple-600 dark:text-purple-400 mx-auto mb-2" />
                  <div className="text-xl font-bold text-purple-900 dark:text-purple-100">67%</div>
                  <div className="text-xs text-purple-700 dark:text-purple-300">Avg Score</div>
                </div>
              </motion.div>

              {/* Ads grid */}
              <div className="grid gap-4">
                {mockAds.map((ad, index) => (
                  <motion.div
                    key={ad.id}
                    initial={{ opacity: 0, x: -20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.6, delay: 0.8 + index * 0.1 }}
                    viewport={{ once: true }}
                    whileHover={{ scale: 1.02 }}
                    className="cursor-pointer"
                  >
                    <Card className={`border transition-all duration-300 hover:shadow-md ${getStatusColor(ad.status)}`}>
                      <CardContent className="p-4">
                        <div className="flex flex-col sm:flex-row items-start justify-between gap-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                              <h4 className="font-semibold text-sm">{ad.headline}</h4>
                              <Badge variant="secondary" className="text-xs">
                                Meta Ad
                              </Badge>
                            </div>
                            <p className="text-sm text-muted-foreground mb-3">
                              {ad.description}
                            </p>
                            {ad.violations.length > 0 && (
                              <div className="flex flex-wrap gap-1">
                                {ad.violations.map((violation, idx) => (
                                  <Badge key={idx} variant="destructive" className="text-xs">
                                    {violation}
                                  </Badge>
                                ))}
                              </div>
                            )}
                          </div>
                          <div className="flex items-center gap-4">
                            <div className="text-center">
                              <div className={`text-2xl font-bold ${getScoreColor(ad.score)}`}>
                                {ad.score}
                              </div>
                              <div className="text-xs text-muted-foreground">Score</div>
                            </div>
                            <Button size="sm" variant="ghost" className="gap-2">
                              <Eye className="h-4 w-4" />
                              View
                            </Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </motion.div>
                ))}
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Bottom note */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 1.2 }}
          viewport={{ once: true }}
          className="text-center mt-12"
        >
          <p className="text-muted-foreground text-sm">
            This is a live preview of the dashboard. The actual dashboard includes more detailed analysis, 
            landing page verification, and real-time monitoring capabilities.
          </p>
        </motion.div>
      </div>
    </section>
  );
}
