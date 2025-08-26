"use client"

import { useState, useEffect, useRef, useCallback } from "react"
import Link from "next/link"
import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { 
  Plus, 
  Globe, 
  Search, 
  BarChart3,
  AlertTriangle,
  CheckCircle2,
  Clock,
  ExternalLink,
  Share2,
  MoreVertical,
  Eye,
  Trash2,
  RefreshCw,
  Loader2
} from "lucide-react"
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { domainApi, type Domain, ApiError } from "@/lib/api"
import { toast } from "sonner"
import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { useDomainScraping } from "@/hooks/useDomainScraping"

// For development - using hardcoded user ID
const DEMO_USER_ID = 1;

// Custom hook for long press detection
const useLongPress = (onLongPress: () => void, delay = 800) => {
  const [longPressTriggered, setLongPressTriggered] = useState(false)
  const timeout = useRef<NodeJS.Timeout>()
  const target = useRef<EventTarget>()

  const start = useCallback((event: React.MouseEvent | React.TouchEvent) => {
    if (event.target !== target.current) {
      target.current = event.target
    }
    setLongPressTriggered(false) // Reset on start
    timeout.current = setTimeout(() => {
      onLongPress()
      setLongPressTriggered(true)
    }, delay)
  }, [onLongPress, delay])

  const clear = useCallback((event: React.MouseEvent | React.TouchEvent) => {
    timeout.current && clearTimeout(timeout.current)
    
    // Use a small delay to ensure longPressTriggered is properly set
    setTimeout(() => {
      setLongPressTriggered(false)
    }, 50)
  }, [])

  return {
    onMouseDown: (e: React.MouseEvent) => start(e),
    onTouchStart: (e: React.TouchEvent) => start(e),
    onMouseUp: (e: React.MouseEvent) => clear(e),
    onMouseLeave: (e: React.MouseEvent) => clear(e),
    onTouchEnd: (e: React.TouchEvent) => clear(e),
    longPressTriggered
  }
}

export default function DomainsPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [domains, setDomains] = useState<Domain[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [deleteDialog, setDeleteDialog] = useState<{
    isOpen: boolean
    domainId: number | null
    domainName: string
  }>({
    isOpen: false,
    domainId: null,
    domainName: ""
  })
  const [openDropdownId, setOpenDropdownId] = useState<number | null>(null)
  const { isDomainRefreshing, startDomainScraping, syncDomainStatus, clearStuckStates } = useDomainScraping()

  // Load domains on component mount
  useEffect(() => {
    loadDomains()
  }, [])

  // Clear stuck refreshing states on component mount
  useEffect(() => {
    const timer = setTimeout(() => {
      clearStuckStates()
    }, 2000) // Wait 2 seconds after component mount
    
    return () => clearTimeout(timer)
  }, [clearStuckStates])

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (openDropdownId !== null) {
        const target = event.target as Element
        // Only close if clicking outside the dropdown content and trigger
        if (!target.closest('[data-radix-popper-content-wrapper]') && 
            !target.closest('[data-radix-popper-trigger]')) {
          setOpenDropdownId(null)
        }
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [openDropdownId])

  const loadDomains = async () => {
    try {
      setLoading(true)
      setError(null)
      // Clear any existing domains first
      setDomains([])
      const data = await domainApi.getDomains(DEMO_USER_ID)
      const domainsArray = Array.isArray(data) ? data : []
      console.log(`📋 Loaded ${domainsArray.length} domains:`, domainsArray.map(d => ({ id: d.id, name: d.domainName })))
      console.log(`🔍 Full domain objects:`, domainsArray)
      
      // Simple, direct state update
      setDomains(domainsArray)
    } catch (error) {
      console.error("Error loading domains:", error)
      if (error instanceof ApiError) {
        setError(error.message)
        toast.error(`Failed to load domains: ${error.message}`)
      } else {
        setError("Failed to load domains")
        toast.error("Failed to load domains. Please try again.")
      }
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteDomain = (domainId: number, domainName: string) => {
    console.log(`Opening delete dialog for domain: ${domainName} (ID: ${domainId})`)
    setDeleteDialog({
      isOpen: true,
      domainId,
      domainName
    })
  }

  const confirmDeleteDomain = async () => {
    if (!deleteDialog.domainId) return

    console.log(`Confirming deletion of domain: ${deleteDialog.domainName} (ID: ${deleteDialog.domainId})`)

    try {
      await domainApi.deleteDomain(DEMO_USER_ID, deleteDialog.domainId)
      toast.success(`"${deleteDialog.domainName}" has been successfully deleted`)
      loadDomains() // Reload the list
    } catch (error) {
      console.error("Error deleting domain:", error)
      if (error instanceof ApiError) {
        toast.error(`Failed to delete domain: ${error.message}`)
      } else {
        toast.error("Failed to delete domain. Please try again.")
      }
    } finally {
      setDeleteDialog({ isOpen: false, domainId: null, domainName: "" })
    }
  }

  const handleRefreshDomain = async (domain: Domain, event: React.MouseEvent) => {
    event.preventDefault() // Prevent navigation to domain details
    event.stopPropagation()
    
    // Handle different states
    if (domain.processingStatus === 'FETCHING_ADS') {
      // Pause scraping
      try {
        const response = await fetch(`http://localhost:8080/api/domains/${domain.id}/pause`, {
          method: 'POST',
        })
        
        if (response.ok) {
          toast.success('Scraping paused')
          loadDomains() // Refresh to get updated status
        } else {
          toast.error('Failed to pause scraping')
        }
      } catch (error) {
        console.error('Error pausing scraping:', error)
        toast.error('Failed to pause scraping')
      }
    } else if (domain.processingStatus === 'PAUSED') {
      // Resume scraping
      try {
        const response = await fetch(`http://localhost:8080/api/domains/${domain.id}/resume`, {
          method: 'POST',
        })
        
        if (response.ok) {
          toast.success('Scraping resumed')
          loadDomains() // Refresh to get updated status
        } else {
          toast.error('Failed to resume scraping')
        }
      } catch (error) {
        console.error('Error resuming scraping:', error)
        toast.error('Failed to resume scraping')
      }
    } else {
      // Start new scraping
      if (isDomainRefreshing(domain.id)) {
        // If frontend thinks it's refreshing but we're trying to start new scraping,
        // sync with backend first
        await syncDomainStatus(domain.id)
        return
      }
      await startDomainScraping(domain.domainName, domain.id)
    }
  }

  const handleCancelScraping = async (domain: Domain) => {
    try {
      const response = await fetch(`http://localhost:8080/api/domains/${domain.id}/cancel`, {
        method: 'POST',
      })
      
      if (response.ok) {
        toast.success('Scraping cancelled')
        loadDomains() // Refresh to get updated status
      } else {
        toast.error('Failed to cancel scraping')
      }
    } catch (error) {
      console.error('Error cancelling scraping:', error)
      toast.error('Failed to cancel scraping')
    }
  }

  const filteredDomains = Array.isArray(domains) ? domains.filter(domain => {
    // Validate domain object structure
    if (!domain || typeof domain.id !== 'number' || !domain.domainName) {
      console.error(`❌ Invalid domain object:`, domain)
      return false
    }
    return domain.domainName.toLowerCase().includes(searchTerm.toLowerCase())
  }) : []

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
        return 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
      case 'pending':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400'
      case 'error':
        return 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400'
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/20 dark:text-gray-400'
    }
  }

  const getComplianceScoreColor = (score: number | null) => {
    if (score === null) return 'text-slate-500 dark:text-slate-400'
    if (score >= 80) return 'text-green-600 dark:text-green-400'
    if (score >= 60) return 'text-yellow-600 dark:text-yellow-400'
    return 'text-red-600 dark:text-red-400'
  }

  const getRefreshIconState = (domain: Domain) => {
    const isRefreshing = isDomainRefreshing(domain.id)
    
    switch (domain.processingStatus) {
      case 'FETCHING_ADS':
        return {
          color: 'text-blue-600 dark:text-blue-400',
          animate: true,
          title: 'Press to pause, long press to cancel',
          canLongPress: true
        }
      case 'PAUSED':
        return {
          color: 'text-orange-600 dark:text-orange-400',
          animate: false,
          title: 'Press to resume, long press to cancel',
          canLongPress: true
        }
      case 'FAILED':
        return {
          color: 'text-red-600 dark:text-red-400',
          animate: false,
          title: 'Scraping failed - Click to retry',
          canLongPress: false
        }
      case 'SCANNING_COMPLIANCE':
        return {
          color: 'text-purple-600 dark:text-purple-400',
          animate: true,
          title: 'Analyzing compliance...',
          canLongPress: false
        }
      default:
        return {
          color: 'text-slate-600 dark:text-slate-400 group-hover:text-slate-900 dark:group-hover:text-white',
          animate: isRefreshing,
          title: 'Refresh ads from Facebook',
          canLongPress: false
        }
    }
  }

  // Separate component for domain card to avoid hooks rule violation
  function DomainCard({ domain }: { domain: Domain }) {
    const iconState = getRefreshIconState(domain)
    const longPressProps = useLongPress(
      () => {
        if (iconState.canLongPress) {
          handleCancelScraping(domain)
        }
      },
      800 // 800ms long press
    )

    const handleRefreshClick = (e: React.MouseEvent) => {
      if (!longPressProps.longPressTriggered) {
        handleRefreshDomain(domain, e)
      }
    }

    return (
      <div className="group block rounded-2xl p-6 hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30 last:border-b-0">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="p-3 gradient-primary rounded-xl">
              <Globe className="h-6 w-6 text-white" />
            </div>
            <div className="flex-1">
              <div className="flex items-center space-x-3">
                <Link 
                  href={`/dashboard/domains/${domain.id}`}
                  className="text-lg font-semibold text-slate-900 dark:text-white hover:text-primary transition-colors"
                >
                  {domain.domainName}
                </Link>
                <button
                  {...longPressProps}
                  onClick={handleRefreshClick}
                  className="relative z-20 p-1.5 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-lg transition-colors group"
                  title={iconState.title}
                >
                  <RefreshCw className={`h-4 w-4 transition-colors ${iconState.color} ${iconState.animate ? 'animate-spin' : ''}`} />
                </button>
              </div>
              <div className="flex items-center space-x-4 mt-1">
                <Badge className={getStatusColor(domain.status)}>
                  {domain.status}
                </Badge>
                <span className="text-sm text-slate-600 dark:text-slate-400">
                  Added {new Date(domain.createdAt).toLocaleDateString()}
                </span>
                {domain.lastChecked && (
                  <span className="text-sm text-slate-600 dark:text-slate-400">
                    Last checked {new Date(domain.lastChecked).toLocaleDateString()}
                  </span>
                )}
              </div>
            </div>
          </div>
          
          <div className="flex items-center space-x-6">
            <div className="text-right">
              <div className="text-sm text-slate-600 dark:text-slate-400 mb-1">
                Compliance Score
              </div>
              <div className={`text-2xl font-bold ${getComplianceScoreColor(domain.complianceScore)}`}>
                {domain.complianceScore ? `${Math.round(domain.complianceScore)}%` : 'N/A'}
              </div>
            </div>
            
            <DropdownMenu 
              modal={false}
              open={openDropdownId === domain.id}
              onOpenChange={(open) => {
                setOpenDropdownId(open ? domain.id : null)
              }}
            >
              <DropdownMenuTrigger asChild>
                <button 
                  className="relative z-20 p-2 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-lg transition-colors"
                  onClick={(e) => {
                    e.preventDefault()
                    e.stopPropagation()
                  }}
                >
                  <MoreVertical className="h-5 w-5 text-slate-600 dark:text-slate-400" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent 
                align="end" 
                className="glass-thin rounded-2xl shadow-lg z-30"
                onPointerDownOutside={(e) => e.preventDefault()}
                onInteractOutside={(e) => e.preventDefault()}
              >
                <DropdownMenuLabel>Actions</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link 
                    href={`/dashboard/domains/${domain.id}`}
                    onClick={(e) => {
                      e.stopPropagation()
                      setOpenDropdownId(null) // Close dropdown
                    }}
                  >
                    <Eye className="mr-2 h-4 w-4" />
                    View Details
                  </Link>
                </DropdownMenuItem>
                {domain.shareToken && (
                  <DropdownMenuItem asChild>
                    <a 
                      href={`/report/${domain.shareToken}`} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      onClick={(e) => {
                        e.stopPropagation()
                        setOpenDropdownId(null) // Close dropdown
                      }}
                    >
                      <ExternalLink className="mr-2 h-4 w-4" />
                      Public Report
                    </a>
                  </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem 
                  onClick={(e) => {
                    e.preventDefault()
                    e.stopPropagation()
                    setOpenDropdownId(null) // Close dropdown
                    handleDeleteDomain(domain.id, domain.domainName)
                  }}
                  className="text-red-600 focus:text-red-600"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete Domain
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>
    )
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
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gradient mb-2">
              Your Domains
            </h1>
            <p className="text-slate-600 dark:text-slate-400 text-lg">
              Monitor and manage ad compliance for all your domains
            </p>
          </div>
          <div className="flex items-center space-x-4 mt-4 md:mt-0">
            <button
              onClick={() => {
                console.log("🔄 Manual refresh clicked")
                loadDomains()
              }}
              disabled={loading}
              className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl px-4 py-2 flex items-center gap-2 transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <Link href="/dashboard/add-domain">
              <button className="bg-gradient-to-r from-purple-500 to-blue-600 text-white px-6 py-2 rounded-xl font-semibold hover:opacity-90 transition-all duration-300 flex items-center gap-2">
                <Plus className="h-4 w-4" />
                Add Domain
              </button>
            </Link>
          </div>
        </div>

        {/* Search */}
        <div className="mb-8">
          <div className="relative max-w-md">
            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-500" />
            <input
              type="text"
              placeholder="Search domains..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-slate-100/50 dark:bg-slate-800/50 border border-slate-200/50 dark:border-slate-700/50 rounded-2xl focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary/50 transition-all placeholder-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
            />
          </div>
        </div>

        {/* Content */}
        {loading ? (
          <div className="text-center py-12">
            <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-slate-500" />
            <p className="text-slate-600 dark:text-slate-400">Loading domains...</p>
          </div>
        ) : error ? (
          <div className="text-center py-12">
            <AlertTriangle className="h-12 w-12 mx-auto mb-4 text-red-500" />
            <p className="text-red-600 dark:text-red-400 mb-4">{error}</p>
            <button
              onClick={loadDomains}
              className="hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl px-6 py-2 text-slate-900 dark:text-white transition-colors"
            >
              Try Again
            </button>
          </div>
        ) : filteredDomains.length === 0 ? (
          <div className="text-center py-16">
            <Globe className="h-16 w-16 mx-auto mb-6 text-slate-400" />
            <h3 className="text-xl font-semibold text-slate-900 dark:text-white mb-2">
              {searchTerm ? "No domains found" : "No domains yet"}
            </h3>
            <p className="text-slate-600 dark:text-slate-400 mb-6 max-w-md mx-auto">
              {searchTerm 
                ? `No domains match "${searchTerm}". Try adjusting your search.`
                : "Start by adding your first domain to monitor ad compliance."
              }
            </p>
            {!searchTerm && (
              <Link href="/dashboard/add-domain">
                <button className="bg-gradient-to-r from-purple-500 to-blue-600 text-white px-8 py-3 rounded-2xl font-semibold hover:opacity-90 transition-all duration-300 flex items-center gap-2 mx-auto">
                  <Plus className="h-5 w-5" />
                  Add Your First Domain
                </button>
              </Link>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {filteredDomains.map((domain, index) => {
              // Validate domain before rendering
              if (!domain || typeof domain.id !== 'number' || !domain.domainName) {
                console.error(`❌ Invalid domain at index ${index}:`, domain)
                return null
              }

              return <DomainCard key={`domain-${domain.id}`} domain={domain} />
            })}
          </div>
        )}
      </motion.div>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        onClose={() => setDeleteDialog({ isOpen: false, domainId: null, domainName: "" })}
        onConfirm={confirmDeleteDomain}
        title="Delete Domain"
        message={`Are you sure you want to delete "${deleteDialog.domainName}"?

⚠️  This will permanently delete:
• All scraped ads for this domain
• All compliance analysis results  
• Domain configuration and settings

This action cannot be undone.`}
        confirmText="Delete Domain"
        cancelText="Cancel"
        type="danger"
      />
    </div>
  )
}