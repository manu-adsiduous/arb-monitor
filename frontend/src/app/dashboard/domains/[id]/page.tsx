"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { motion } from "framer-motion"
import { 
  ArrowLeft,
  Globe, 
  AlertTriangle,
  CheckCircle2,
  Clock,
  ExternalLink,
  Share2,
  RefreshCw,
  Loader2,
  Copy,
  BarChart3
} from "lucide-react"
import { domainApi, analysisApi, type Domain, ApiError } from "@/lib/api"
import { toast } from "sonner"
import { AdGrid } from "@/components/dashboard/AdGrid"
import { useDomainScraping } from "@/hooks/useDomainScraping"

// For development - using hardcoded user ID
const DEMO_USER_ID = 1;

export default function DomainDetailPage() {
  const params = useParams()
  const domainId = parseInt(params.id as string)
  
  const [domain, setDomain] = useState<Domain | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [analyzing, setAnalyzing] = useState(false)
  const [updatingRacParameter, setUpdatingRacParameter] = useState(false)
  const [showRacConfig, setShowRacConfig] = useState(false)
  const [detectingRacParameter, setDetectingRacParameter] = useState(false)
  const { isDomainRefreshing, startDomainScraping } = useDomainScraping()

  useEffect(() => {
    if (domainId) {
      loadDomain()
    }
  }, [domainId])

  const loadDomain = async () => {
    try {
      setLoading(true)
      setError(null)
      const domainData = await domainApi.getDomain(DEMO_USER_ID, domainId)
      setDomain(domainData)
    } catch (error) {
      console.error('Error loading domain:', error)
      if (error instanceof ApiError) {
        setError(error.message)
        toast.error(`Failed to load domain: ${error.message}`)
      } else {
        setError('Failed to load domain')
        toast.error('Failed to load domain')
      }
    } finally {
      setLoading(false)
    }
  }

  const refreshAds = async () => {
    if (!domain) return
    
    await startDomainScraping(domain.domainName, domain.id)
    
    // Reload domain data after starting scraping to get updated status
    setTimeout(() => loadDomain(), 1000)
  }

  const updateRacParameter = async (racParameter: string) => {
    if (!domain) return

    try {
      setUpdatingRacParameter(true)
      
      const response = await fetch(`/api/domains/${domain.id}/rac-parameter`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': DEMO_USER_ID.toString()
        },
        body: JSON.stringify({ racParameter })
      })

      if (response.ok) {
        const result = await response.json()
        toast.success('RAC parameter updated!', {
          description: `Set to '${racParameter}' for ${domain.domainName}`
        })
        
        // Update local domain state
        setDomain(prev => prev ? { ...prev, racParameter } : null)
      } else {
        const error = await response.json()
        toast.error('Failed to update RAC parameter', {
          description: error.error || 'An error occurred'
        })
      }
    } catch (error) {
      console.error('Error updating RAC parameter:', error)
      toast.error('Failed to update RAC parameter', {
        description: 'An unexpected error occurred'
      })
    } finally {
      setUpdatingRacParameter(false)
    }
  }

  const detectRacParameter = async () => {
    if (!domain) return

    try {
      setDetectingRacParameter(true)
      
      // First, check if there are any scraped ads for this domain
      const adsResponse = await fetch(`/api/ads/domain/${domain.domainName}`, {
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': DEMO_USER_ID.toString()
        }
      })

      if (!adsResponse.ok) {
        toast.error('Failed to check for scraped ads', {
          description: 'Please try again later'
        })
        return
      }

      const adsData = await adsResponse.json()
      const ads = adsData.ads || []

      if (ads.length === 0) {
        toast.error('No ads found for auto-detection', {
          description: 'Please scrape some ads first, then try auto-detection again'
        })
        return
      }

      toast.info('Detecting RAC parameter...', {
        description: `Analyzing AFS code from ${ads.length} scraped ads`
      })

      // First, clear the current parameter to force auto-detection
      await updateRacParameter('')
      
      // Then trigger RAC extraction which will use auto-detection
      const response = await fetch(`/api/compliance/extract-rac/${domain.id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-ID': DEMO_USER_ID.toString()
        }
      })

      if (response.ok) {
        toast.success('RAC parameter detected!', {
          description: 'Parameter has been auto-detected and applied'
        })
        
        // Reload domain to get the detected parameter
        await loadDomain()
      } else {
        const error = await response.json()
        toast.error('Failed to detect RAC parameter', {
          description: error.error || 'Auto-detection failed'
        })
      }
    } catch (error) {
      console.error('Error detecting RAC parameter:', error)
      toast.error('Failed to detect RAC parameter', {
        description: 'An unexpected error occurred'
      })
    } finally {
      setDetectingRacParameter(false)
    }
  }

  const runAnalysis = async () => {
    if (!domain) return

    try {
      setAnalyzing(true)
      toast.info('Starting ad analysis...', {
        description: 'This may take a few moments to complete.'
      })

      const result = await analysisApi.analyzeDomain(DEMO_USER_ID, domain.id)
      
      if (result.success) {
        toast.success('Analysis completed!', {
          description: `Processed ${result.processedAds} ads for ${domain.domainName}`
        })
        
        // Reload domain data to show updated compliance score
        await loadDomain()
      } else {
        toast.error('Analysis failed', {
          description: result.message
        })
      }
    } catch (error) {
      console.error('Error running analysis:', error)
      if (error instanceof ApiError) {
        toast.error(`Analysis failed: ${error.message}`)
      } else {
        toast.error('Analysis failed', {
          description: 'An unexpected error occurred during analysis.'
        })
      }
    } finally {
      setAnalyzing(false)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active': return 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
      case 'paused': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400'
      case 'inactive': return 'bg-gray-100 text-gray-800 dark:bg-gray-900/20 dark:text-gray-400'
      default: return 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400'
    }
  }

  const formatLastUpdated = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffHours / 24)

    if (diffHours < 1) return 'Less than 1 hour ago'
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
    return date.toLocaleDateString()
  }

  const getComplianceColor = (score: number | null) => {
    if (score === null) return 'text-gray-500'
    if (score >= 90) return 'text-green-600'
    if (score >= 75) return 'text-blue-600'
    if (score >= 60) return 'text-yellow-600'
    return 'text-red-600'
  }

  const copyShareLink = () => {
    if (domain?.shareToken) {
      navigator.clipboard.writeText(`${window.location.origin}/report/${domain.shareToken}`)
      toast.success('Share link copied!')
    }
  }

  if (loading) {
    return (
      <div className="py-6">
        <div className="flex items-center justify-center py-20">
          <div className="flex items-center space-x-3">
            <Loader2 className="h-6 w-6 animate-spin text-slate-500" />
            <span className="text-lg text-slate-600 dark:text-slate-400">Loading domain details...</span>
          </div>
        </div>
      </div>
    )
  }

  if (error || !domain) {
    return (
      <div className="py-6">
        <div className="p-12 text-center">
          <AlertTriangle className="h-16 w-16 text-red-500 mx-auto mb-6" />
          <h3 className="text-2xl font-bold text-slate-900 dark:text-white mb-4">Domain Not Found</h3>
          <p className="text-slate-600 dark:text-slate-400 mb-8 max-w-md mx-auto">
            {error || 'The requested domain could not be found.'}
          </p>
          <div className="flex items-center justify-center space-x-4">
            <Link href="/dashboard/domains">
              <button className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-2xl px-6 py-3 text-slate-900 dark:text-white font-medium transition-colors">
                <ArrowLeft className="h-4 w-4 mr-2 inline" />
                Back to Domains
              </button>
            </Link>
            <button 
              onClick={loadDomain}
              className="bg-gradient-to-r from-purple-500 to-blue-600 text-white px-6 py-3 rounded-2xl font-medium hover:opacity-90 transition-all duration-300"
            >
              <RefreshCw className="h-4 w-4 mr-2 inline" />
              Try Again
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="py-6">
      {/* Main Content - Flat Design */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-8"
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center space-x-4">
            <Link href="/dashboard/domains">
              <button className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl px-4 py-2 text-sm font-medium text-slate-900 dark:text-white transition-colors flex items-center">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Domains
              </button>
            </Link>
            
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 gradient-primary rounded-2xl flex items-center justify-center">
                <Globe className="h-7 w-7 text-white" />
              </div>
              <div>
                <div className="flex items-center space-x-3">
                  <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
                    {domain.domainName}
                  </h1>
                  <button
                    onClick={refreshAds}
                    disabled={isDomainRefreshing(domainId) || domain?.processingStatus === 'FETCHING_ADS' || domain?.processingStatus === 'SCANNING_COMPLIANCE'}
                    className="p-2 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl transition-colors group"
                    title="Refresh ads from Facebook"
                  >
                    <RefreshCw className={`h-5 w-5 text-slate-600 dark:text-slate-400 group-hover:text-slate-900 dark:group-hover:text-white transition-colors ${isDomainRefreshing(domainId) || domain?.processingStatus === 'FETCHING_ADS' || domain?.processingStatus === 'SCANNING_COMPLIANCE' ? 'animate-spin' : ''}`} />
                  </button>
                </div>
                <div className="flex items-center space-x-3 mt-1">
                  <span className={`px-3 py-1 text-sm font-medium rounded-full ${getStatusColor(domain.status)}`}>
                    {domain.status}
                  </span>
                  <span className="text-slate-600 dark:text-slate-400 text-sm">
                    Added {new Date(domain.createdAt).toLocaleDateString()}
                  </span>
                  <span className="text-slate-600 dark:text-slate-400 text-sm">
                    • Last updated {formatLastUpdated(domain.updatedAt)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <a 
              href={`https://${domain.domainName}`} 
              target="_blank" 
              rel="noopener noreferrer" 
              className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl p-2 text-slate-900 dark:text-white transition-colors flex items-center"
              title="Visit Site"
            >
              <ExternalLink className="h-4 w-4" />
            </a>
            <button 
              onClick={copyShareLink}
              className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl p-2 text-slate-900 dark:text-white transition-colors flex items-center"
              title="Share Report"
            >
              <Share2 className="h-4 w-4" />
            </button>
            <button 
              onClick={runAnalysis}
              disabled={analyzing}
              className="bg-gradient-to-r from-purple-500 to-blue-600 text-white px-4 py-2 rounded-xl text-sm font-medium hover:opacity-90 transition-all duration-300 flex items-center disabled:opacity-75"
            >
              <RefreshCw className={`h-4 w-4 mr-2 ${analyzing ? 'animate-spin' : ''}`} />
              {analyzing ? 'Analyzing...' : 'Run Analysis'}
            </button>
          </div>
        </div>

        {/* RAC Parameter Configuration */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h3 className={`text-lg font-semibold text-slate-900 dark:text-white transition-all duration-300 ${showRacConfig ? 'opacity-100' : 'opacity-0'}`}>
              Advanced Settings
            </h3>
            <button
              onClick={() => setShowRacConfig(!showRacConfig)}
              className="text-xs text-slate-500 dark:text-slate-400 bg-slate-100/50 dark:bg-slate-700/50 px-2 py-1 rounded-lg hover:bg-slate-200/50 dark:hover:bg-slate-600/50 transition-colors"
            >
              {showRacConfig ? 'Hide Advanced' : 'Show Advanced'}
            </button>
          </div>
          
          <div className={`overflow-hidden transition-all duration-300 ease-in-out ${showRacConfig ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'}`}>
            <div className="bg-slate-50/30 dark:bg-slate-800/20 rounded-3xl p-6">
              <div className="space-y-4">
                <div className="flex items-center space-x-4">
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                      Specify URL Parameter for RAC
                    </label>
                    <input
                      type="text"
                      placeholder="e.g., q, kw, adtitle, search"
                      value={domain.racParameter || ''}
                      onChange={(e) => {
                        const newValue = e.target.value
                        setDomain(prev => prev ? { ...prev, racParameter: newValue } : null)
                      }}
                      onBlur={(e) => {
                        const newValue = e.target.value.trim()
                        if (newValue !== (domain.racParameter || '')) {
                          updateRacParameter(newValue)
                        }
                      }}
                      className="w-full px-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white placeholder-slate-500 dark:placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all"
                    />
                  </div>
                  <div className="flex flex-col items-end space-y-2">
                    <button
                      onClick={detectRacParameter}
                      disabled={detectingRacParameter}
                      className="text-xs text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300 font-medium underline transition-colors disabled:opacity-50"
                    >
                      {detectingRacParameter ? 'Detecting...' : 'Auto-detect RAC parameter'}
                    </button>
                    <div className="text-xs text-slate-500 dark:text-slate-400 max-w-xs text-right">
                      Leave empty to auto-detect from AFS code. Set this if auto-detection fails.
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Compliance Overview */}
        <div className="mb-12 bg-slate-50/30 dark:bg-slate-800/20 rounded-3xl p-8">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-8">Compliance Overview</h2>
          
          {/* Main Score Section */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
            <div className="text-center lg:text-left">
              <div className={`text-8xl font-bold ${getComplianceColor(domain.complianceScore)} mb-4`}>
                {domain.complianceScore ? `${Math.round(domain.complianceScore)}%` : 'N/A'}
              </div>
              <div className="text-slate-600 dark:text-slate-400 text-lg">Compliance Score</div>
            </div>
            
            <div className="flex flex-col justify-center">
              <div className="text-4xl font-bold text-slate-900 dark:text-white mb-2">
                {domain.complianceScore === null ? 'Not analyzed' : 
                 domain.complianceScore >= 90 ? 'Excellent' :
                 domain.complianceScore >= 75 ? 'Good' :
                 domain.complianceScore >= 60 ? 'Warning' : 'Needs attention'}
              </div>
              <div className="text-slate-600 dark:text-slate-400 text-lg">Current Status</div>
            </div>
          </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center p-6 bg-blue-50/50 dark:bg-blue-900/20 rounded-2xl">
              <div className="w-16 h-16 bg-blue-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <BarChart3 className="h-8 w-8 text-blue-500" />
              </div>
              <div className="text-3xl font-bold text-blue-600 mb-2">{domain.activeAds || 0}</div>
              <div className="text-slate-600 dark:text-slate-400 font-medium">Active Ads</div>
            </div>
            
            <div className="text-center p-6 bg-red-50/50 dark:bg-red-900/20 rounded-2xl">
              <div className="w-16 h-16 bg-red-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <AlertTriangle className="h-8 w-8 text-red-500" />
              </div>
              <div className="text-3xl font-bold text-red-600 mb-2">{domain.violations || 0}</div>
              <div className="text-slate-600 dark:text-slate-400 font-medium">Violations</div>
            </div>
            
            <div className="text-center p-6 bg-green-50/50 dark:bg-green-900/20 rounded-2xl">
              <div className="w-16 h-16 bg-green-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <CheckCircle2 className="h-8 w-8 text-green-500" />
              </div>
              <div className="text-3xl font-bold text-green-600 mb-2">
                {domain.complianceScore ? Math.round(((domain.complianceScore / 100) * (domain.activeAds || 0))) : 0}
              </div>
              <div className="text-slate-600 dark:text-slate-400 font-medium">Compliant Ads</div>
            </div>
          </div>
        </div>

        {/* Ad Library Section */}
        <div className="mb-12">
          <AdGrid domainName={domain.domainName} />
        </div>

        {/* Recent Analysis Results */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-12">
          {/* Recent Ad Analysis */}
          <div>
            <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Recent Ad Analysis</h3>
            <div className="bg-white/10 dark:bg-slate-800/10 rounded-2xl p-6 text-center">
              {domain.activeAds === 0 ? (
                <>
                  <Clock className="h-12 w-12 text-slate-400 mx-auto mb-4" />
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-white mb-2">
                    No ads analyzed yet
                  </h4>
                  <p className="text-slate-600 dark:text-slate-400 mb-4">
                    Run an analysis to see ads for this domain.
                  </p>
                  <button 
                    onClick={runAnalysis}
                    disabled={analyzing}
                    className="bg-white/20 dark:bg-slate-800/20 hover:bg-white/30 dark:hover:bg-slate-700/30 rounded-xl px-4 py-2 text-sm font-medium text-slate-900 dark:text-white transition-colors disabled:opacity-75"
                  >
                    {analyzing ? 'Analyzing...' : 'Run Analysis'}
                  </button>
                </>
              ) : (
                <p className="text-slate-600 dark:text-slate-400">
                  Ad analysis data will be displayed here once available.
                </p>
              )}
            </div>
          </div>

          {/* Policy Violations */}
          <div>
            <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Policy Violations</h3>
            <div className="bg-white/10 dark:bg-slate-800/10 rounded-2xl p-6 text-center">
              {domain.violations === 0 ? (
                <>
                  <CheckCircle2 className="h-12 w-12 text-green-500 mx-auto mb-4" />
                  <h4 className="text-lg font-semibold text-slate-900 dark:text-white mb-2">
                    No violations found
                  </h4>
                  <p className="text-slate-600 dark:text-slate-400">
                    All analyzed ads appear to be compliant with policies.
                  </p>
                </>
              ) : (
                <p className="text-slate-600 dark:text-slate-400">
                  Violation details will be displayed here once available.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Share Link */}
        <div>
          <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Public Report</h3>
          <div className="bg-white/10 dark:bg-slate-800/10 rounded-2xl p-6">
            <div className="flex items-center justify-between">
              <div className="flex-1 mr-4">
                <div className="bg-white/20 dark:bg-slate-800/20 rounded-xl px-4 py-3 font-mono text-sm text-slate-700 dark:text-slate-300 break-all">
                  {typeof window !== 'undefined' ? `${window.location.origin}/report/${domain.shareToken}` : `/report/${domain.shareToken}`}
                </div>
                <p className="text-sm text-slate-600 dark:text-slate-400 mt-2">
                  Public link to share compliance report with stakeholders
                </p>
              </div>
              <button 
                onClick={copyShareLink}
                className="bg-white/20 dark:bg-slate-800/20 hover:bg-white/30 dark:hover:bg-slate-700/30 rounded-xl px-4 py-2 text-sm font-medium text-slate-900 dark:text-white transition-colors flex items-center"
              >
                <Copy className="h-4 w-4 mr-2" />
                Copy
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}