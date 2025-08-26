"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { motion } from "framer-motion"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { 
  Plus, 
  BarChart3, 
  AlertTriangle, 
  CheckCircle2, 
  Clock, 
  Globe,
  TrendingUp,
  Shield,
  Zap,
  Eye,
  Users,
  Loader2
} from "lucide-react"
import { domainApi, type Domain, ApiError } from "@/lib/api"
import { toast } from "sonner"
import { useDomainScraping } from "@/hooks/useDomainScraping"

// For development - using hardcoded user ID
const DEMO_USER_ID = 1

export default function DashboardPage() {
  const [domains, setDomains] = useState<Domain[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const { scrapingStatus } = useDomainScraping()

  // Load domains on component mount
  useEffect(() => {
    loadDomains()
  }, [])

  const loadDomains = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await domainApi.getDomains(DEMO_USER_ID)
      setDomains(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error("Error loading domains:", error)
      if (error instanceof ApiError) {
        setError(error.message)
      } else {
        setError("Failed to load domains")
      }
    } finally {
      setLoading(false)
    }
  }

  // Calculate stats
  const totalDomains = domains.length
  const activeDomains = domains.filter(d => d.status === 'ACTIVE').length
  const avgCompliance = domains.length > 0 
    ? Math.round(domains.reduce((sum, d) => sum + (d.complianceScore || 0), 0) / domains.length)
    : 0
  const activeScans = Object.values(scrapingStatus).filter(s => s.isRefreshing).length

  // Generate recent activity from actual domain data
  const recentActivity = domains
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 3)
    .map(domain => {
      const timeDiff = Date.now() - new Date(domain.updatedAt).getTime()
      const hoursAgo = Math.floor(timeDiff / (1000 * 60 * 60))
      const timeText = hoursAgo < 1 ? 'Less than 1 hour ago' : 
                     hoursAgo === 1 ? '1 hour ago' : 
                     `${hoursAgo} hours ago`
      
      if (domain.processingStatus === 'COMPLETED') {
        return {
          icon: CheckCircle2,
          message: `Domain analysis completed for`,
          domainName: domain.domainName,
          domainId: domain.id,
          time: timeText
        }
      } else if (domain.processingStatus === 'FAILED') {
        return {
          icon: AlertTriangle,
          message: `Analysis failed for`,
          domainName: domain.domainName,
          domainId: domain.id,
          time: timeText
        }
      } else {
        return {
          icon: Clock,
          message: `Processing started for`,
          domainName: domain.domainName,
          domainId: domain.id,
          time: timeText
        }
      }
    })

  // Show different content based on domain count
  const hasNoDomains = !loading && totalDomains === 0
  const hasMultipleDomains = totalDomains > 0

  return (
    <div className="py-6">
      {/* Main Dashboard Container - Flat Design */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-8"
      >
        {/* Welcome Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gradient mb-3">
            Welcome back, Demo User!
          </h1>
          <p className="text-slate-600 dark:text-slate-400 text-lg">
            Monitor your ad compliance across all domains from one unified dashboard.
          </p>
        </div>

        {/* Quick Actions and Stats - Better Alignment */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {/* Quick Action - Add Domain */}
          <Link href="/dashboard/add-domain">
            <div className="group cursor-pointer hover:opacity-80 transition-all duration-200 p-6 rounded-2xl hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
              <div className="flex items-center space-x-4">
                <div className="w-14 h-14 gradient-primary rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform shadow-lg">
                  <Plus className="h-7 w-7 text-white" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white text-lg">Add Domain</h3>
                  <p className="text-sm text-slate-600 dark:text-slate-400">Start monitoring</p>
                </div>
              </div>
            </div>
          </Link>

          {/* Stat - Total Domains */}
          <div className="p-6 rounded-2xl hover:bg-slate-50/30 dark:hover:bg-slate-800/20 transition-colors">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600 dark:text-slate-400 mb-1">Total Domains</p>
                <p className="text-3xl font-bold text-slate-900 dark:text-white">
                  {loading ? <Loader2 className="h-8 w-8 animate-spin" /> : totalDomains}
                </p>
              </div>
              <div className="w-14 h-14 bg-blue-500/20 rounded-2xl flex items-center justify-center">
                <Globe className="h-7 w-7 text-blue-500" />
              </div>
            </div>
          </div>

          {/* Stat - Compliance Score */}
          <div className="p-6 rounded-2xl hover:bg-slate-50/30 dark:hover:bg-slate-800/20 transition-colors">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600 dark:text-slate-400 mb-1">Avg. Compliance</p>
                <p className="text-3xl font-bold text-green-600 dark:text-green-400">
                  {loading ? <Loader2 className="h-8 w-8 animate-spin" /> : 
                   totalDomains === 0 ? 'N/A' : `${avgCompliance}%`}
                </p>
              </div>
              <div className="w-14 h-14 bg-green-500/20 rounded-2xl flex items-center justify-center">
                <Shield className="h-7 w-7 text-green-500" />
              </div>
            </div>
          </div>

          {/* Stat - Active Scans */}
          <div className="p-6 rounded-2xl hover:bg-slate-50/30 dark:hover:bg-slate-800/20 transition-colors">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600 dark:text-slate-400 mb-1">Active Scans</p>
                <p className="text-3xl font-bold text-purple-600 dark:text-purple-400">
                  {loading ? <Loader2 className="h-8 w-8 animate-spin" /> : activeScans}
                </p>
              </div>
              <div className="w-14 h-14 bg-purple-500/20 rounded-2xl flex items-center justify-center">
                <Eye className="h-7 w-7 text-purple-500" />
              </div>
            </div>
          </div>
        </div>

        {/* Content Sections - Flat Design */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mt-8 pt-8 border-t border-slate-200/20 dark:border-slate-700/20">
          {/* Getting Started */}
          <div>
            <div className="mb-6">
              <div className="flex items-center space-x-3 mb-3">
                <div className="w-10 h-10 gradient-primary rounded-2xl flex items-center justify-center">
                  <CheckCircle2 className="h-6 w-6 text-white" />
                </div>
                <h3 className="text-xl font-bold text-slate-900 dark:text-white">Getting Started</h3>
              </div>
              <p className="text-slate-600 dark:text-slate-400">
                Follow these steps to start monitoring your ad compliance
              </p>
            </div>
            
            <div className="space-y-6">
              {/* Step 1: Add Domain */}
              <div className={`group cursor-pointer ${totalDomains > 0 ? 'opacity-60' : ''}`}>
                <div className="flex items-center justify-between py-2">
                  <div className="flex items-center space-x-4">
                    <div className={`w-8 h-8 ${totalDomains > 0 ? 'bg-green-500' : 'gradient-primary'} text-white rounded-xl flex items-center justify-center text-sm font-bold group-hover:scale-110 transition-transform`}>
                      {totalDomains > 0 ? <CheckCircle2 className="h-5 w-5" /> : '1'}
                    </div>
                    <span className="font-semibold text-slate-900 dark:text-white">
                      {totalDomains > 0 ? `${totalDomains} domain${totalDomains > 1 ? 's' : ''} added` : 'Add your first domain'}
                    </span>
                  </div>
                  <div className={`rounded-xl px-4 py-2 text-sm font-medium ${
                    totalDomains > 0 
                      ? 'bg-green-500/20 text-green-700 dark:text-green-300' 
                      : 'bg-blue-500/20 text-blue-700 dark:text-blue-300'
                  }`}>
                    {totalDomains > 0 ? 'Complete' : 'Start'}
                  </div>
                </div>
              </div>
              
              {/* Step 2: Monitor Ads */}
              <div className={`py-2 ${totalDomains === 0 ? 'opacity-60' : ''}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <div className={`w-8 h-8 ${totalDomains > 0 ? 'gradient-primary' : 'bg-slate-400/50'} text-white rounded-xl flex items-center justify-center text-sm font-bold`}>
                      2
                    </div>
                    <span className="font-semibold text-slate-900 dark:text-white">Monitor ad compliance</span>
                  </div>
                  <div className={`rounded-xl px-3 py-1 text-xs font-medium ${
                    totalDomains > 0 
                      ? 'bg-blue-500/20 text-blue-700 dark:text-blue-300' 
                      : 'bg-slate-200/50 dark:bg-slate-700/50 text-slate-600 dark:text-slate-400'
                  }`}>
                    {totalDomains > 0 ? 'Active' : 'Pending'}
                  </div>
                </div>
              </div>
              
              {/* Step 3: Review Reports */}
              <div className={`py-2 ${totalDomains === 0 ? 'opacity-60' : ''}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <div className={`w-8 h-8 ${totalDomains > 0 ? 'gradient-primary' : 'bg-slate-400/50'} text-white rounded-xl flex items-center justify-center text-sm font-bold`}>
                      3
                    </div>
                    <span className="font-semibold text-slate-900 dark:text-white">Review compliance reports</span>
                  </div>
                  <div className={`rounded-xl px-3 py-1 text-xs font-medium ${
                    totalDomains > 0 
                      ? 'bg-green-500/20 text-green-700 dark:text-green-300' 
                      : 'bg-slate-200/50 dark:bg-slate-700/50 text-slate-600 dark:text-slate-400'
                  }`}>
                    {totalDomains > 0 ? 'Available' : 'Pending'}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Recent Activity */}
          <div>
            <div className="mb-6">
              <div className="flex items-center space-x-3 mb-3">
                <div className="w-10 h-10 gradient-primary rounded-2xl flex items-center justify-center">
                  <Clock className="h-6 w-6 text-white" />
                </div>
                <h3 className="text-xl font-bold text-slate-900 dark:text-white">Recent Activity</h3>
              </div>
              <p className="text-slate-600 dark:text-slate-400">
                Latest updates and notifications
              </p>
            </div>
            
            <div className="space-y-6">
              {loading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-slate-500" />
                </div>
              ) : recentActivity.length > 0 ? (
                recentActivity.map((activity, index) => (
                  <div key={index} className="hover:opacity-80 transition-opacity py-2">
                    <div className="flex items-start space-x-4">
                      <div className="w-10 h-10 gradient-primary rounded-xl flex items-center justify-center flex-shrink-0">
                        <activity.icon className="h-5 w-5 text-white" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-slate-900 dark:text-slate-100 mb-1">
                          {activity.message}{' '}
                          <Link 
                            href={`/dashboard/domains/${activity.domainId}`}
                            className="text-primary hover:text-primary/80 transition-colors font-semibold"
                          >
                            {activity.domainName}
                          </Link>
                        </p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {activity.time}
                        </p>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8">
                  <Clock className="h-12 w-12 mx-auto mb-4 text-slate-400" />
                  <p className="text-slate-600 dark:text-slate-400">No recent activity</p>
                  <p className="text-sm text-slate-500 dark:text-slate-500 mt-1">
                    Add a domain to start seeing activity
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* CTA Section - Only show when no domains */}
        {hasNoDomains && (
          <div className="text-center py-12 mt-8 pt-8 border-t border-slate-200/20 dark:border-slate-700/20">
            <div className="w-20 h-20 gradient-primary rounded-3xl flex items-center justify-center mx-auto mb-6">
              <Globe className="h-10 w-10 text-white" />
            </div>
            <h3 className="text-2xl font-bold text-gradient mb-4">
              Ready to monitor your first domain?
            </h3>
            <p className="text-lg text-slate-600 dark:text-slate-300 mb-8 max-w-md mx-auto">
              Add a domain to start analyzing ad compliance and get detailed insights with our powerful monitoring tools.
            </p>
            <Link href="/dashboard/add-domain">
              <div className="inline-flex items-center space-x-3 group cursor-pointer hover:opacity-80 transition-opacity">
                <div className="w-10 h-10 gradient-primary rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                  <Plus className="h-6 w-6 text-white" />
                </div>
                <span className="text-lg font-semibold text-slate-900 dark:text-white">
                  Add Your First Domain
                </span>
              </div>
            </Link>
          </div>
        )}

        {/* Success Section - Show when domains exist */}
        {hasMultipleDomains && (
          <div className="text-center py-12 mt-8 pt-8 border-t border-slate-200/20 dark:border-slate-700/20">
            <div className="w-20 h-20 bg-green-500/20 rounded-3xl flex items-center justify-center mx-auto mb-6">
              <CheckCircle2 className="h-10 w-10 text-green-500" />
            </div>
            <h3 className="text-2xl font-bold text-gradient mb-4">
              Great! You're monitoring {totalDomains} domain{totalDomains > 1 ? 's' : ''}
            </h3>
            <p className="text-lg text-slate-600 dark:text-slate-300 mb-8 max-w-md mx-auto">
              Your domains are being actively monitored for ad compliance. Check the domains page for detailed insights.
            </p>
            <div className="flex items-center justify-center space-x-4">
              <Link href="/dashboard/domains">
                <div className="inline-flex items-center space-x-3 group cursor-pointer hover:opacity-80 transition-opacity">
                  <div className="w-10 h-10 gradient-primary rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                    <BarChart3 className="h-6 w-6 text-white" />
                  </div>
                  <span className="text-lg font-semibold text-slate-900 dark:text-white">
                    View All Domains
                  </span>
                </div>
              </Link>
              <Link href="/dashboard/add-domain">
                <div className="inline-flex items-center space-x-3 group cursor-pointer hover:opacity-80 transition-opacity">
                  <div className="w-10 h-10 bg-slate-200/50 dark:bg-slate-700/50 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                    <Plus className="h-6 w-6 text-slate-600 dark:text-slate-400" />
                  </div>
                  <span className="text-lg font-semibold text-slate-900 dark:text-white">
                    Add Another Domain
                  </span>
                </div>
              </Link>
            </div>
          </div>
        )}
      </motion.div>
    </div>
  )
}