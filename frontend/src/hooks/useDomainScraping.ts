import { useState, useEffect, useCallback } from 'react'
import { domainApi, type Domain } from '@/lib/api'
import { toast } from 'sonner'

interface ScrapingStatus {
  [domainId: number]: {
    isRefreshing: boolean
    processingStatus: string
    processingMessage: string | null
    pollInterval?: NodeJS.Timeout
  }
}

// Storage key for cross-tab synchronization
const SCRAPING_STATUS_KEY = 'arb-monitor-scraping-status'

// Global state for scraping status
let globalScrapingStatus: ScrapingStatus = {}
let statusSubscribers: Set<() => void> = new Set()

// Load initial state from localStorage
if (typeof window !== 'undefined') {
  try {
    const stored = localStorage.getItem(SCRAPING_STATUS_KEY)
    if (stored) {
      globalScrapingStatus = JSON.parse(stored)
    }
  } catch (error) {
    console.warn('Failed to load scraping status from localStorage:', error)
  }
}

const DEMO_USER_ID = 1

export function useDomainScraping() {
  const [scrapingStatus, setScrapingStatus] = useState<ScrapingStatus>(globalScrapingStatus)

  // Subscribe to global status changes and cross-tab synchronization
  useEffect(() => {
    const updateStatus = () => setScrapingStatus({ ...globalScrapingStatus })
    statusSubscribers.add(updateStatus)
    
    // Listen for storage changes from other tabs
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === SCRAPING_STATUS_KEY && e.newValue) {
        try {
          const newStatus = JSON.parse(e.newValue)
          globalScrapingStatus = newStatus
          updateStatus()
        } catch (error) {
          console.warn('Failed to parse scraping status from storage event:', error)
        }
      }
    }
    
    window.addEventListener('storage', handleStorageChange)
    
    return () => {
      statusSubscribers.delete(updateStatus)
      window.removeEventListener('storage', handleStorageChange)
    }
  }, [])

  // Notify all subscribers of status changes
  const notifySubscribers = useCallback(() => {
    statusSubscribers.forEach(callback => callback())
  }, [])

  // Update global status and sync to localStorage
  const updateScrapingStatus = useCallback((domainId: number, updates: Partial<ScrapingStatus[number]>) => {
    globalScrapingStatus[domainId] = {
      ...globalScrapingStatus[domainId],
      ...updates
    }
    
    // Save to localStorage for cross-tab synchronization
    try {
      // Filter out pollInterval from localStorage (can't serialize functions)
      const storableStatus: ScrapingStatus = {}
      Object.keys(globalScrapingStatus).forEach(key => {
        const domainId = parseInt(key)
        const status = globalScrapingStatus[domainId]
        storableStatus[domainId] = {
          isRefreshing: status.isRefreshing,
          processingStatus: status.processingStatus,
          processingMessage: status.processingMessage
        }
      })
      localStorage.setItem(SCRAPING_STATUS_KEY, JSON.stringify(storableStatus))
    } catch (error) {
      console.warn('Failed to save scraping status to localStorage:', error)
    }
    
    notifySubscribers()
  }, [notifySubscribers])

  // Check if a domain is currently being scraped
  const isDomainRefreshing = useCallback((domainId: number) => {
    return globalScrapingStatus[domainId]?.isRefreshing || false
  }, [])

  // Sync frontend state with backend status
  const syncDomainStatus = useCallback(async (domainId: number) => {
    try {
      const updatedDomain = await domainApi.getDomain(DEMO_USER_ID, domainId)
      
      // If backend shows completed/failed but frontend shows refreshing, fix it
      if ((updatedDomain.processingStatus === 'COMPLETED' || updatedDomain.processingStatus === 'FAILED') 
          && globalScrapingStatus[domainId]?.isRefreshing) {
        
        // Clear any existing polling
        if (globalScrapingStatus[domainId]?.pollInterval) {
          clearInterval(globalScrapingStatus[domainId].pollInterval!)
        }
        
        updateScrapingStatus(domainId, { 
          isRefreshing: false,
          processingStatus: updatedDomain.processingStatus,
          processingMessage: updatedDomain.processingMessage,
          pollInterval: undefined
        })
        
        console.log(`🔄 Synced domain ${domainId} status: ${updatedDomain.processingStatus}`)
      }
    } catch (error) {
      console.error('Error syncing domain status:', error)
    }
  }, [updateScrapingStatus])

  // Clear all stuck refreshing states
  const clearStuckStates = useCallback(async () => {
    const stuckDomains = Object.keys(globalScrapingStatus)
      .map(id => parseInt(id))
      .filter(domainId => globalScrapingStatus[domainId]?.isRefreshing)
    
    for (const domainId of stuckDomains) {
      await syncDomainStatus(domainId)
    }
  }, [syncDomainStatus])

  // Start scraping for a domain
  const startDomainScraping = useCallback(async (domainName: string, domainId: number) => {
    try {
      // Mark as refreshing
      updateScrapingStatus(domainId, { 
        isRefreshing: true,
        processingStatus: 'FETCHING_ADS',
        processingMessage: 'Starting ad scraper...'
      })

      toast.info('Refreshing ads...', {
        description: 'Fetching latest ads from Facebook Ad Library'
      })

      const response = await fetch(
        `http://localhost:8080/api/ads/refresh/${encodeURIComponent(domainName)}`,
        {
          method: 'POST',
          headers: {
            'X-User-ID': '1'
          }
        }
      )

      if (!response.ok) {
        throw new Error(`Failed to refresh ads: ${response.statusText}`)
      }

      toast.success('Ads refresh started!', {
        description: 'This may take 1-2 minutes. The page will update automatically.'
      })
      
      // Start polling for updates
      const pollInterval = setInterval(async () => {
        try {
          const updatedDomain = await domainApi.getDomain(DEMO_USER_ID, domainId)
          
          updateScrapingStatus(domainId, {
            processingStatus: updatedDomain.processingStatus,
            processingMessage: updatedDomain.processingMessage
          })

          // Check if processing is complete
          if (updatedDomain.processingStatus === 'COMPLETED' || updatedDomain.processingStatus === 'FAILED') {
            // Clear polling
            if (globalScrapingStatus[domainId]?.pollInterval) {
              clearInterval(globalScrapingStatus[domainId].pollInterval!)
            }
            
            updateScrapingStatus(domainId, { 
              isRefreshing: false,
              pollInterval: undefined
            })
            
            if (updatedDomain.processingStatus === 'COMPLETED') {
              toast.success('Ads refreshed successfully!')
            } else {
              toast.error('Failed to refresh ads')
            }
          }
        } catch (error) {
          console.error('Error polling domain status:', error)
          // Clear polling on error
          if (globalScrapingStatus[domainId]?.pollInterval) {
            clearInterval(globalScrapingStatus[domainId].pollInterval!)
          }
          updateScrapingStatus(domainId, { 
            isRefreshing: false,
            pollInterval: undefined
          })
        }
      }, 10000) // Poll every 10 seconds

      // Store the interval for cleanup
      updateScrapingStatus(domainId, { pollInterval })

      // Stop polling after 5 minutes
      setTimeout(() => {
        if (globalScrapingStatus[domainId]?.pollInterval) {
          clearInterval(globalScrapingStatus[domainId].pollInterval!)
          updateScrapingStatus(domainId, { 
            isRefreshing: false,
            pollInterval: undefined
          })
        }
      }, 300000)

    } catch (error) {
      console.error('Error starting domain scraping:', error)
      toast.error('Failed to refresh ads')
      updateScrapingStatus(domainId, { isRefreshing: false })
    }
  }, [updateScrapingStatus])

  // Clean up polling intervals when component unmounts
  useEffect(() => {
    return () => {
      Object.values(globalScrapingStatus).forEach(status => {
        if (status.pollInterval) {
          clearInterval(status.pollInterval)
        }
      })
    }
  }, [])

  return {
    scrapingStatus,
    isDomainRefreshing,
    startDomainScraping,
    updateScrapingStatus,
    syncDomainStatus,
    clearStuckStates
  }
}
