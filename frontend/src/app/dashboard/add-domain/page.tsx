"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { useRouter } from "next/navigation"
import { motion } from "framer-motion"
import { 
  Plus, 
  Globe, 
  AlertCircle, 
  CheckCircle2, 
  Loader2,
  ArrowRight,
  Shield,
  BarChart3
} from "lucide-react"
import { toast } from "sonner"
import { domainApi, ApiError } from "@/lib/api"
import { useDomainScraping } from "@/hooks/useDomainScraping"

// For development - using hardcoded user ID
const DEMO_USER_ID = 1;

export default function AddDomainPage() {
  const router = useRouter()
  const [domain, setDomain] = useState("")
  const [isAdding, setIsAdding] = useState(false)
  const [addedDomain, setAddedDomain] = useState<any>(null)
  const [validationResult, setValidationResult] = useState<{
    valid: boolean
    message: string
    suggestions?: string[]
  } | null>(null)
  const { updateScrapingStatus } = useDomainScraping()
  const inputRef = useRef<HTMLInputElement>(null)
  const validationTimeoutRef = useRef<NodeJS.Timeout | null>(null)

  const validateDomain = (domainValue: string) => {
    // Domain validation that supports subdomains
    const domainRegex = /^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]?(\.[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]?)*\.[a-zA-Z]{2,}$/
    
    if (!domainValue.trim()) {
      return { valid: false, message: "Domain is required" }
    }
    
    if (!domainRegex.test(domainValue)) {
      return { 
        valid: false, 
        message: "Please enter a valid domain name",
        suggestions: ["example.com", "sub.example.com", "mywebsite.org", "search.citizensadvisors.com"]
      }
    }
    
    return { valid: true, message: "Domain looks good!" }
  }

  // Completely avoid state changes during typing to preserve focus
  useEffect(() => {
    // Clear previous timeout
    if (validationTimeoutRef.current) {
      clearTimeout(validationTimeoutRef.current)
    }

    if (!domain.trim()) {
      setValidationResult(null)
      return
    }

    // ONLY validate after user stops typing - NO intermediate state changes
    validationTimeoutRef.current = setTimeout(() => {
      const result = validateDomain(domain)
      setValidationResult(result)
    }, 800)

    return () => {
      if (validationTimeoutRef.current) {
        clearTimeout(validationTimeoutRef.current)
      }
    }
  }, [domain])

  const handleDomainChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setDomain(e.target.value)
  }, [])

  // Cleanup timeouts on unmount
  useEffect(() => {
    return () => {
      if (validationTimeoutRef.current) {
        clearTimeout(validationTimeoutRef.current)
      }
    }
  }, [])

  const handleAddDomain = async () => {
    if (!validationResult?.valid) return

    setIsAdding(true)
    try {
      const response = await domainApi.addDomain(DEMO_USER_ID, domain.trim())
      setAddedDomain(response)
      toast.success(`Successfully added ${domain} for monitoring!`)
      
      // Initialize global scraping status
      updateScrapingStatus(response.id, {
        isRefreshing: true,
        processingStatus: response.processingStatus || 'FETCHING_ADS',
        processingMessage: response.processingMessage || 'Starting ad scraper...'
      })
      
      // Start polling for progress updates
      pollDomainProgress(response.id)
    } catch (error) {
      console.error("Error adding domain:", error)
      if (error instanceof ApiError) {
        toast.error(`Failed to add domain: ${error.message}`)
      } else {
        toast.error("Failed to add domain. Please try again.")
      }
      setIsAdding(false)
    }
  }

  const pollDomainProgress = async (domainId: number) => {
    const maxPolls = 60 // Poll for up to 5 minutes
    let pollCount = 0
    
    const poll = async () => {
      try {
        const domainData = await domainApi.getDomain(DEMO_USER_ID, domainId)
        setAddedDomain(domainData)
        
        // Update global scraping status
        updateScrapingStatus(domainId, {
          isRefreshing: domainData.processingStatus !== 'COMPLETED' && domainData.processingStatus !== 'FAILED',
          processingStatus: domainData.processingStatus,
          processingMessage: domainData.processingMessage
        })
        
        // If completed or failed, stop polling and redirect
        if (domainData.processingStatus === 'COMPLETED' || domainData.processingStatus === 'FAILED') {
          setIsAdding(false)
          setTimeout(() => {
            router.push(`/dashboard/domains/${domainId}`)
          }, 2000)
          return
        }
        
        // Continue polling if still processing
        if (pollCount < maxPolls) {
          pollCount++
          setTimeout(poll, 5000) // Poll every 5 seconds
        } else {
          // Timeout reached
          setIsAdding(false)
          updateScrapingStatus(domainId, { isRefreshing: false })
          toast.error("Processing is taking longer than expected. Check your domain details page.")
          router.push(`/dashboard/domains/${domainId}`)
        }
      } catch (error) {
        console.error("Error polling domain progress:", error)
        setIsAdding(false)
        updateScrapingStatus(domainId, { isRefreshing: false })
        toast.error("Error checking progress. Please check your domain details page.")
        router.push(`/dashboard/domains/${domainId}`)
      }
    }
    
    poll()
  }

  return (
    <div className="py-6">
      {/* Flat design container */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-8"
      >
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gradient mb-3">
            Add Domain
          </h1>
          <p className="text-slate-600 dark:text-slate-400 text-lg">
            Start monitoring ad compliance for a new domain. We'll analyze all active Meta ads and provide detailed compliance reports.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Add Domain Form or Progress Display */}
          <div>
            {!addedDomain ? (
              // Domain Form
              <>
                <div className="flex items-center space-x-3 mb-6">
                  <div className="p-3 gradient-primary rounded-2xl">
                    <Globe className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-slate-900 dark:text-white">Domain Information</h2>
                    <p className="text-slate-600 dark:text-slate-400">Enter the domain you want to monitor for ad compliance</p>
                  </div>
                </div>
                
                <div className="space-y-6">
                  <div className="space-y-3">
                    <label className="text-sm font-medium text-slate-900 dark:text-white block">
                      Domain Name
                    </label>
                    <div className="relative">
                      <input
                        ref={inputRef}
                        type="text"
                        placeholder="example.com"
                        value={domain}
                        onChange={handleDomainChange}
                        className="w-full px-6 py-4 text-lg bg-slate-100/50 dark:bg-slate-800/50 border border-slate-200/50 dark:border-slate-700/50 rounded-2xl focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary/50 transition-all placeholder-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
                        disabled={isAdding}
                        autoComplete="off"
                        autoFocus
                      />

                    </div>
                    
                    {/* Validation feedback */}
                    {validationResult && (
                      <div className={`flex items-center space-x-2 text-sm ${
                        validationResult.valid 
                          ? 'text-green-600 dark:text-green-400' 
                          : 'text-red-600 dark:text-red-400'
                      }`}>
                        {validationResult.valid ? (
                          <CheckCircle2 className="h-4 w-4" />
                        ) : (
                          <AlertCircle className="h-4 w-4" />
                        )}
                        <span>{validationResult.message}</span>
                      </div>
                    )}
                    
                    {/* Suggestions */}
                    {validationResult?.suggestions && (
                      <div className="space-y-2">
                        <p className="text-xs text-slate-600 dark:text-slate-400">Suggestions:</p>
                        <div className="flex flex-wrap gap-2">
                          {validationResult.suggestions.map((suggestion, index) => (
                            <button
                              key={index}
                              onClick={() => setDomain(suggestion)}
                              className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 px-4 py-2 text-sm rounded-xl text-slate-700 dark:text-slate-300 transition-colors border border-slate-200/30 dark:border-slate-700/30"
                            >
                              {suggestion}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                  
                  <button
                    onClick={handleAddDomain}
                    disabled={!validationResult?.valid || isAdding}
                    className="w-full bg-gradient-to-r from-purple-500 to-blue-600 text-white py-4 px-6 rounded-2xl font-semibold text-lg disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-all duration-300 flex items-center justify-center gap-3"
                  >
                    {isAdding ? (
                      <>
                        <Loader2 className="h-5 w-5 animate-spin" />
                        Adding Domain...
                      </>
                    ) : (
                      <>
                        <Plus className="h-5 w-5" />
                        Add Domain
                      </>
                    )}
                  </button>
                </div>
              </>
            ) : (
              // Progress Display
              <>
                <div className="flex items-center space-x-3 mb-6">
                  <div className="p-3 gradient-primary rounded-2xl">
                    <CheckCircle2 className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-slate-900 dark:text-white">Processing {addedDomain.domainName}</h2>
                    <p className="text-slate-600 dark:text-slate-400">We're setting up monitoring for your domain</p>
                  </div>
                </div>
                
                <div className="space-y-6">
                  {/* Progress Steps */}
                  <div className="space-y-4">
                    {/* Step 1: Fetching Ads */}
                    <div className={`flex items-center space-x-4 p-4 rounded-2xl transition-all ${
                      addedDomain.processingStatus === 'FETCHING_ADS' 
                        ? 'bg-blue-50 dark:bg-blue-900/20 border-2 border-blue-200 dark:border-blue-800' 
                        : addedDomain.processingStatus === 'SCANNING_COMPLIANCE' || addedDomain.processingStatus === 'COMPLETED'
                        ? 'bg-green-50 dark:bg-green-900/20 border-2 border-green-200 dark:border-green-800'
                        : 'bg-slate-50 dark:bg-slate-800/20 border-2 border-slate-200 dark:border-slate-700'
                    }`}>
                      <div className={`flex items-center justify-center w-10 h-10 rounded-full ${
                        addedDomain.processingStatus === 'FETCHING_ADS'
                          ? 'bg-blue-500 text-white'
                          : addedDomain.processingStatus === 'SCANNING_COMPLIANCE' || addedDomain.processingStatus === 'COMPLETED'
                          ? 'bg-green-500 text-white'
                          : 'bg-slate-300 dark:bg-slate-600 text-slate-600 dark:text-slate-300'
                      }`}>
                        {addedDomain.processingStatus === 'FETCHING_ADS' ? (
                          <Loader2 className="h-5 w-5 animate-spin" />
                        ) : addedDomain.processingStatus === 'SCANNING_COMPLIANCE' || addedDomain.processingStatus === 'COMPLETED' ? (
                          <CheckCircle2 className="h-5 w-5" />
                        ) : (
                          <span className="text-sm font-bold">1</span>
                        )}
                      </div>
                      <div className="flex-1">
                        <h3 className="font-semibold text-slate-900 dark:text-white">Fetching ads from Facebook</h3>
                        <p className="text-sm text-slate-600 dark:text-slate-400">
                          {addedDomain.processingStatus === 'FETCHING_ADS' 
                            ? addedDomain.processingMessage || "Scanning Meta Ad Library..."
                            : addedDomain.processingStatus === 'SCANNING_COMPLIANCE' || addedDomain.processingStatus === 'COMPLETED'
                            ? "✓ Ads fetched successfully"
                            : "Waiting to start..."}
                        </p>
                      </div>
                    </div>

                    {/* Step 2: Scanning Compliance */}
                    <div className={`flex items-center space-x-4 p-4 rounded-2xl transition-all ${
                      addedDomain.processingStatus === 'SCANNING_COMPLIANCE' 
                        ? 'bg-blue-50 dark:bg-blue-900/20 border-2 border-blue-200 dark:border-blue-800' 
                        : addedDomain.processingStatus === 'COMPLETED'
                        ? 'bg-green-50 dark:bg-green-900/20 border-2 border-green-200 dark:border-green-800'
                        : 'bg-slate-50 dark:bg-slate-800/20 border-2 border-slate-200 dark:border-slate-700'
                    }`}>
                      <div className={`flex items-center justify-center w-10 h-10 rounded-full ${
                        addedDomain.processingStatus === 'SCANNING_COMPLIANCE'
                          ? 'bg-blue-500 text-white'
                          : addedDomain.processingStatus === 'COMPLETED'
                          ? 'bg-green-500 text-white'
                          : 'bg-slate-300 dark:bg-slate-600 text-slate-600 dark:text-slate-300'
                      }`}>
                        {addedDomain.processingStatus === 'SCANNING_COMPLIANCE' ? (
                          <Loader2 className="h-5 w-5 animate-spin" />
                        ) : addedDomain.processingStatus === 'COMPLETED' ? (
                          <CheckCircle2 className="h-5 w-5" />
                        ) : (
                          <span className="text-sm font-bold">2</span>
                        )}
                      </div>
                      <div className="flex-1">
                        <h3 className="font-semibold text-slate-900 dark:text-white">Scanning for compliance</h3>
                        <p className="text-sm text-slate-600 dark:text-slate-400">
                          {addedDomain.processingStatus === 'SCANNING_COMPLIANCE' 
                            ? addedDomain.processingMessage || "Analyzing ads for violations..."
                            : addedDomain.processingStatus === 'COMPLETED'
                            ? "✓ Compliance analysis complete"
                            : "Waiting for ads to be fetched..."}
                        </p>
                      </div>
                    </div>

                    {/* Step 3: Completed */}
                    <div className={`flex items-center space-x-4 p-4 rounded-2xl transition-all ${
                      addedDomain.processingStatus === 'COMPLETED'
                        ? 'bg-green-50 dark:bg-green-900/20 border-2 border-green-200 dark:border-green-800'
                        : 'bg-slate-50 dark:bg-slate-800/20 border-2 border-slate-200 dark:border-slate-700'
                    }`}>
                      <div className={`flex items-center justify-center w-10 h-10 rounded-full ${
                        addedDomain.processingStatus === 'COMPLETED'
                          ? 'bg-green-500 text-white'
                          : 'bg-slate-300 dark:bg-slate-600 text-slate-600 dark:text-slate-300'
                      }`}>
                        {addedDomain.processingStatus === 'COMPLETED' ? (
                          <CheckCircle2 className="h-5 w-5" />
                        ) : (
                          <span className="text-sm font-bold">3</span>
                        )}
                      </div>
                      <div className="flex-1">
                        <h3 className="font-semibold text-slate-900 dark:text-white">Ready for monitoring</h3>
                        <p className="text-sm text-slate-600 dark:text-slate-400">
                          {addedDomain.processingStatus === 'COMPLETED'
                            ? "✓ Domain is ready! Redirecting to results..."
                            : "Waiting for compliance scan..."}
                        </p>
                      </div>
                    </div>
                  </div>
                  
                  {/* Overall progress message */}
                  <div className="text-center p-4 bg-slate-50 dark:bg-slate-800/30 rounded-2xl">
                    <p className="text-slate-600 dark:text-slate-400">
                      {addedDomain.processingStatus === 'COMPLETED' 
                        ? "🎉 All done! Taking you to your domain dashboard..."
                        : addedDomain.processingStatus === 'FAILED'
                        ? "❌ Processing failed. Please try again or contact support."
                        : "⏳ This usually takes 30-90 seconds. Please don't close this page."}
                    </p>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* What Happens Next */}
          <div>
            <div className="flex items-center space-x-3 mb-6">
              <div className="p-3 gradient-primary rounded-2xl">
                <CheckCircle2 className="h-6 w-6 text-white" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-white">What Happens Next?</h2>
                <p className="text-slate-600 dark:text-slate-400">Here's what we'll do once you add your domain</p>
              </div>
            </div>
            
            <div className="space-y-6">
              <div className="flex items-start space-x-4">
                <div className="flex items-center justify-center w-10 h-10 gradient-primary rounded-full text-white text-lg font-bold flex-shrink-0">
                  1
                </div>
                <div>
                  <h3 className="font-semibold text-slate-900 dark:text-white mb-2 text-lg">Fetch Active Ads</h3>
                  <p className="text-slate-600 dark:text-slate-400">We'll scan Meta Ad Library for all active ads running for your domain</p>
                </div>
              </div>
              
              <div className="flex items-start space-x-4">
                <div className="flex items-center justify-center w-10 h-10 gradient-primary rounded-full text-white text-lg font-bold flex-shrink-0">
                  2
                </div>
                <div>
                  <h3 className="font-semibold text-slate-900 dark:text-white mb-2 text-lg">Analyze Compliance</h3>
                  <p className="text-slate-600 dark:text-slate-400">Each ad will be checked against Google AFS/RSOC compliance rules</p>
                </div>
              </div>
              
              <div className="flex items-start space-x-4">
                <div className="flex items-center justify-center w-10 h-10 gradient-primary rounded-full text-white text-lg font-bold flex-shrink-0">
                  3
                </div>
                <div>
                  <h3 className="font-semibold text-slate-900 dark:text-white mb-2 text-lg">Generate Reports</h3>
                  <p className="text-slate-600 dark:text-slate-400">Get detailed compliance scores and shareable reports</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Pricing Info */}
        <div className="mt-12 pt-8 border-t border-white/10">
          <div className="text-center">
            <div className="inline-flex items-center space-x-6 bg-white/10 dark:bg-slate-800/10 rounded-2xl px-8 py-4">
              <div className="flex items-center space-x-3">
                <BarChart3 className="h-6 w-6 text-slate-600 dark:text-slate-400" />
                <span className="text-3xl font-bold text-slate-900 dark:text-white">$50</span>
                <span className="text-slate-600 dark:text-slate-400">per domain</span>
              </div>
              <div className="h-8 w-px bg-slate-300 dark:bg-slate-600"></div>
              <div className="text-slate-600 dark:text-slate-400">
                <div className="font-medium">One-time payment • Unlimited compliance checks</div>
                <div className="flex items-center justify-center mt-1 text-sm">
                  <Shield className="h-4 w-4 mr-1" />
                  24/7 monitoring included
                </div>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}