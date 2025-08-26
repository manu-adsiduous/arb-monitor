'use client'

import { useState, useEffect } from 'react'
import { X, ExternalLink, CheckCircle, XCircle, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react'

// Ad Image Carousel Component
function AdImageCarousel({ images }: { images: string[] }) {
  const [currentIndex, setCurrentIndex] = useState(0)

  const nextImage = () => {
    setCurrentIndex((prev) => (prev + 1) % images.length)
  }

  const prevImage = () => {
    setCurrentIndex((prev) => (prev - 1 + images.length) % images.length)
  }

  if (!images || images.length === 0) return null

  return (
    <div className="relative">
      {/* Main Image */}
      <div className="aspect-[4/3] bg-gray-50 dark:bg-gray-800 rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
        <img 
          src={`http://localhost:8080/api/media/${images[currentIndex].replace('./media/', '')}`}
          alt={`Ad image ${currentIndex + 1}`}
          className="w-full h-full object-cover"
          onError={(e) => {
            e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjlmYWZiIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPkltYWdlIG5vdCBmb3VuZDwvdGV4dD48L3N2Zz4='
          }}
        />
        
        {/* Navigation Arrows (only show if multiple images) */}
        {images.length > 1 && (
          <>
            <button
              onClick={prevImage}
              className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={nextImage}
              className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </>
        )}
        
        {/* Image Counter */}
        {images.length > 1 && (
          <div className="absolute bottom-2 right-2 bg-black/50 text-white text-xs px-2 py-1 rounded">
            {currentIndex + 1} of {images.length}
          </div>
        )}
      </div>
      
      {/* Thumbnail Strip (only show if multiple images) */}
      {images.length > 1 && (
        <div className="flex gap-2 mt-2 overflow-x-auto">
          {images.map((imagePath, index) => (
            <button
              key={index}
              onClick={() => setCurrentIndex(index)}
              className={`flex-shrink-0 w-16 h-12 rounded border-2 overflow-hidden transition-colors ${
                index === currentIndex 
                  ? 'border-blue-500' 
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
              }`}
            >
              <img 
                src={`http://localhost:8080/api/media/${imagePath.replace('./media/', '')}`}
                alt={`Thumbnail ${index + 1}`}
                className="w-full h-full object-cover"
                onError={(e) => {
                  e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNDgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI2Y5ZmFmYiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTAiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj4/PC90ZXh0Pjwvc3ZnPg=='
                }}
              />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

interface AdDetailModalProps {
  adId: string | null
  isOpen: boolean
  onClose: () => void
  onNavigate?: (direction: 'prev' | 'next') => void
  canNavigatePrev?: boolean
  canNavigateNext?: boolean
  currentIndex?: number
  totalCount?: number
}

interface AdDetails {
  id: number
  metaAdId: string
  headline: string
  primaryText: string
  description: string
  callToAction: string
  landingPageUrl: string
  imageUrls: string[]
  videoUrls: string[]
  localImagePaths: string[]
  localVideoPaths: string[]
  pageName: string
  extractedImageText?: string
  extractedVideoText?: string
  referrerAdCreative?: string
}

interface ComplianceAnalysis {
  id: number
  complianceScore: number
  complianceStatus: string
  analysisNotes: string
  adCreativeCompliant?: boolean
  adCreativeReason?: string
  landingPageRelevant?: boolean
  landingPageReason?: string
  racRelevant?: boolean
  racReason?: string
  overallCompliant?: boolean
  landingPageContent?: string
}

interface AdDetailsResponse {
  ad: AdDetails
  analysis: ComplianceAnalysis | null
}

export default function AdDetailModal({ 
  adId, 
  isOpen, 
  onClose, 
  onNavigate, 
  canNavigatePrev = false, 
  canNavigateNext = false, 
  currentIndex = 0, 
  totalCount = 0 
}: AdDetailModalProps) {
  const [adDetails, setAdDetails] = useState<AdDetailsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [navigating, setNavigating] = useState(false)

  useEffect(() => {
    if (isOpen && adId) {
      fetchAdDetails()
    }
  }, [isOpen, adId])

  // Set navigating state when adId changes due to navigation
  useEffect(() => {
    if (adId && adDetails && adId !== adDetails.ad.id.toString()) {
      setNavigating(true)
    }
  }, [adId, adDetails])

  const fetchAdDetails = async () => {
    if (!adId) return
    
    setLoading(true)
    setError(null)
    
    try {
      const response = await fetch(`http://localhost:8080/api/ads/ad/${adId}/details`)
      if (!response.ok) {
        throw new Error('Failed to fetch ad details')
      }
      
      const data = await response.json()
      setAdDetails(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error occurred')
      console.error('Error fetching ad details:', err)
    } finally {
      setLoading(false)
      setNavigating(false)
    }
  }

  const handleClose = () => {
    setAdDetails(null)
    setError(null)
    onClose()
  }

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        handleClose()
      } else if (e.key === 'ArrowLeft' && canNavigatePrev && onNavigate) {
        e.preventDefault()
        onNavigate('prev')
      } else if (e.key === 'ArrowRight' && canNavigateNext && onNavigate) {
        e.preventDefault()
        onNavigate('next')
      }
    }

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
      return () => document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen, canNavigatePrev, canNavigateNext, onNavigate])

  const getComplianceIcon = (isCompliant: boolean | undefined) => {
    if (isCompliant === undefined) return <AlertCircle className="w-5 h-5 text-yellow-500" />
    return isCompliant ? 
      <CheckCircle className="w-5 h-5 text-green-500" /> : 
      <XCircle className="w-5 h-5 text-red-500" />
  }

  const getComplianceText = (isCompliant: boolean | undefined) => {
    if (isCompliant === undefined) return 'Not analyzed'
    return isCompliant ? 'Compliant' : 'Non-compliant'
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-16 pb-4 px-4 overflow-y-auto">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black/40 backdrop-blur-md"
        onClick={handleClose}
      />
      
      {/* Modal Container with Navigation */}
      <div className="relative w-full max-w-5xl my-auto flex items-center justify-center">
        {/* Left Arrow */}
        {totalCount > 1 && onNavigate && (
          <button
            onClick={() => onNavigate('prev')}
            disabled={!canNavigatePrev}
            className="flex-shrink-0 mr-6 text-white/80 hover:text-white transition-all duration-200 disabled:opacity-30 disabled:cursor-not-allowed z-60"
            title="Previous ad (Left arrow)"
          >
            <ChevronLeft className="w-12 h-12 drop-shadow-lg" />
          </button>
        )}
        
        {/* Modal - Clean white background like notifications panel */}
        <div className="relative w-full max-w-4xl bg-white dark:bg-gray-900 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700 flex-shrink min-w-0">
        {/* Header */}
        <div className="flex items-center justify-between p-6">
          <div className="flex items-center gap-4">
            <h2 className="text-2xl font-semibold text-gray-900 dark:text-white">
              Ad Details
            </h2>
            {totalCount > 1 && (
              <span className="text-sm text-gray-500 dark:text-gray-400">
                {currentIndex} of {totalCount}
              </span>
            )}
          </div>
          
          {/* Close button */}
          <button
            onClick={handleClose}
            className="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
            title="Close (Escape)"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Content - Scrollable within modal */}
        <div className="px-6 pb-6 max-h-[70vh] overflow-y-auto border-t border-gray-200 dark:border-gray-700 pt-6">
          {(loading || navigating) && (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 dark:border-white"></div>
              <span className="ml-3 text-gray-600 dark:text-gray-400">
                {navigating ? 'Loading next ad...' : 'Loading...'}
              </span>
            </div>
          )}

          {error && (
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6">
              <p className="text-red-800 dark:text-red-300">Error: {error}</p>
            </div>
          )}

          {adDetails && (
            <div className="space-y-8">
              {/* Ad Creative Section */}
              <section>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Ad Creative</h3>
                
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Ad Content */}
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Headline</label>
                      <p className="text-gray-900 dark:text-gray-100 text-sm">{adDetails.ad.headline}</p>
                    </div>
                    
                    {adDetails.ad.primaryText && (
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Primary Text</label>
                        <p className="text-gray-900 dark:text-gray-100 text-sm">{adDetails.ad.primaryText}</p>
                      </div>
                    )}
                    
                    {adDetails.ad.description && (
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Description</label>
                        <p className="text-gray-900 dark:text-gray-100 text-sm">{adDetails.ad.description}</p>
                      </div>
                    )}
                    
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Call to Action</label>
                        <p className="text-gray-900 dark:text-gray-100 text-sm">{adDetails.ad.callToAction || 'None'}</p>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Page</label>
                        <p className="text-gray-900 dark:text-gray-100 text-sm break-all">{adDetails.ad.pageName}</p>
                      </div>
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Landing Page</label>
                      <a 
                        href={adDetails.ad.landingPageUrl} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 break-all text-sm inline-flex items-center gap-1"
                      >
                        <span className="break-all">{adDetails.ad.landingPageUrl}</span>
                        <ExternalLink className="w-4 h-4 flex-shrink-0" />
                      </a>
                    </div>
                  </div>

                  {/* Ad Media */}
                  <div className="space-y-4">
                    {adDetails.ad.localImagePaths?.length > 0 && (
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-2">Ad Images</label>
                        <AdImageCarousel images={adDetails.ad.localImagePaths} />
                      </div>
                    )}

                    {adDetails.ad.extractedImageText && (
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Extracted Text (OCR)</label>
                        <p className="text-gray-700 dark:text-gray-300 text-sm italic">{adDetails.ad.extractedImageText}</p>
                      </div>
                    )}
                    
                    {adDetails.ad.extractedVideoText && (
                      <div>
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Video Text (OCR)</label>
                        <p className="text-gray-700 dark:text-gray-300 text-sm italic">{adDetails.ad.extractedVideoText}</p>
                      </div>
                    )}
                  </div>
                </div>

              </section>

              {/* Compliance Analysis Section */}
              <section>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Compliance Analysis</h3>
                  <div className="text-xs text-gray-500 dark:text-gray-400">
                    Google AdSense for Search (AFS) Compliance
                  </div>
                </div>
                
                {adDetails.analysis ? (
                  <div className="space-y-4">
                    {/* Context Information */}
                    {(adDetails.analysis.adCreativeReason || adDetails.analysis.landingPageReason || adDetails.analysis.racReason) && (
                      <div className="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                        <h4 className="text-sm font-medium text-blue-900 dark:text-blue-100 mb-2">📋 Analysis Context</h4>
                        <div className="text-xs text-blue-800 dark:text-blue-200 space-y-2">
                          <div>
                            <span className="font-medium">Landing Page URL:</span> {adDetails.ad.landingPageUrl}
                          </div>
                          {adDetails.ad.referrerAdCreative && (
                            <div>
                              <span className="font-medium">Extracted RAC:</span> "{adDetails.ad.referrerAdCreative}"
                            </div>
                          )}
                          <div>
                            <span className="font-medium">Ad Content:</span> "{adDetails.ad.headline}" | "{adDetails.ad.primaryText}"
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Binary Compliance Status */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div className="flex items-start gap-3">
                        {getComplianceIcon(adDetails.analysis.adCreativeCompliant)}
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-gray-900 dark:text-white">Ad Creative</p>
                          <p className="text-xs text-gray-600 dark:text-gray-400">
                            {getComplianceText(adDetails.analysis.adCreativeCompliant)}
                          </p>
                          {adDetails.analysis.adCreativeReason && (
                            <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                              <p className="font-medium">Issue:</p>
                              <p>{adDetails.analysis.adCreativeReason}</p>
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="flex items-start gap-3">
                        {getComplianceIcon(adDetails.analysis.landingPageRelevant)}
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-gray-900 dark:text-white">Landing Page</p>
                          <p className="text-xs text-gray-600 dark:text-gray-400">
                            {getComplianceText(adDetails.analysis.landingPageRelevant)}
                          </p>
                          {adDetails.analysis.landingPageReason && (
                            <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                              <p className="font-medium">Issue:</p>
                              <p>{adDetails.analysis.landingPageReason}</p>
                              {adDetails.analysis.landingPageReason.includes('not accessible') && (
                                <div className="mt-2 p-2 bg-yellow-50 dark:bg-yellow-900/20 rounded border-l-2 border-yellow-300">
                                  <p className="text-yellow-800 dark:text-yellow-200 text-xs">
                                    <strong>Note:</strong> Some landing pages use redirects or bot protection that prevents automated analysis. 
                                    The page may work fine for real users but appear inaccessible to our compliance checker.
                                  </p>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="flex items-start gap-3">
                        {adDetails.analysis.racReason && adDetails.analysis.racReason.includes("turned off") ? (
                          <div className="flex-shrink-0 w-5 h-5 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                            <span className="text-xs text-gray-500 dark:text-gray-400">⚪</span>
                          </div>
                        ) : (
                          getComplianceIcon(adDetails.analysis.racRelevant)
                        )}
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-gray-900 dark:text-white">RAC Relevance</p>
                          <p className="text-xs text-gray-600 dark:text-gray-400">
                            {adDetails.analysis.racReason && adDetails.analysis.racReason.includes("turned off") 
                              ? "Turned off" 
                              : getComplianceText(adDetails.analysis.racRelevant)}
                          </p>
                          {adDetails.analysis.racReason && !adDetails.analysis.racReason.includes("turned off") && (
                            <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                              <p className="font-medium">Issue:</p>
                              <p>{adDetails.analysis.racReason}</p>
                              {adDetails.ad.referrerAdCreative && (
                                <div className="mt-2 p-2 bg-gray-50 dark:bg-gray-800 rounded border-l-2 border-orange-300">
                                  <p className="font-medium text-gray-700 dark:text-gray-300">RAC Value:</p>
                                  <p className="text-gray-600 dark:text-gray-400 font-mono text-xs break-all">
                                    "{adDetails.ad.referrerAdCreative}"
                                  </p>
                                  <p className="text-gray-500 dark:text-gray-500 text-xs mt-1">
                                    This should match the ad's main message for proper tracking
                                  </p>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Overall Status */}
                    <div className="flex items-start gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                      {getComplianceIcon(adDetails.analysis.overallCompliant)}
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold text-gray-900 dark:text-white">Overall Compliance</p>
                        <p className="text-xs text-gray-600 dark:text-gray-400">
                          {getComplianceText(adDetails.analysis.overallCompliant)}
                        </p>
                      </div>
                    </div>

                    {/* Analysis Notes */}
                    {adDetails.analysis.analysisNotes && (
                      <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Analysis Notes</label>
                        <p className="text-gray-900 dark:text-gray-100 text-sm">{adDetails.analysis.analysisNotes}</p>
                      </div>
                    )}

                    {/* RAC Information */}
                    {adDetails.ad.referrerAdCreative && (
                      <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
                        <label className="block text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Referrer Ad Creative (RAC)</label>
                        <p className="text-gray-900 dark:text-gray-100 text-sm font-mono break-all">{adDetails.ad.referrerAdCreative}</p>
                      </div>
                    )}

                    {/* Compliance Guide */}
                    <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
                      <details className="group">
                        <summary className="cursor-pointer text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200">
                          📖 What do these compliance checks mean?
                        </summary>
                        <div className="mt-3 space-y-3 text-xs text-gray-600 dark:text-gray-400">
                          <div>
                            <span className="font-medium text-gray-700 dark:text-gray-300">Ad Creative:</span> Checks for misleading claims, false promises, clickbait language, or medical/financial guarantees that violate Google AdSense policies.
                          </div>
                          <div>
                            <span className="font-medium text-gray-700 dark:text-gray-300">Landing Page:</span> Ensures the page content matches the ad promise, loads properly, and provides relevant information to users.
                          </div>
                          <div>
                            <span className="font-medium text-gray-700 dark:text-gray-300">RAC Relevance:</span> Verifies that the Referrer Ad Creative (tracking parameter) matches the ad content for accurate performance tracking and compliance. This feature is optional and can be enabled in Advanced Settings.
                          </div>
                        </div>
                      </details>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <AlertCircle className="w-12 h-12 text-yellow-500 mx-auto mb-4" />
                    <p className="text-gray-900 dark:text-gray-100">No compliance analysis available for this ad.</p>
                    <p className="text-gray-600 dark:text-gray-400 text-sm mt-2">Run the compliance analysis to see detailed results.</p>
                  </div>
                )}
              </section>

              {/* Landing Page Preview (if available) */}
              {adDetails.analysis?.landingPageContent && (
                <section>
                  <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-6">Landing Page Content</h3>
                  <div className="max-h-64 overflow-y-auto">
                    <pre className="text-gray-900 dark:text-white text-xs whitespace-pre-wrap break-words">
                      {adDetails.analysis.landingPageContent.substring(0, 1000)}
                      {adDetails.analysis.landingPageContent.length > 1000 && '...'}
                    </pre>
                  </div>
                </section>
              )}
            </div>
          )}
        </div>
        </div>
        
        {/* Right Arrow */}
        {totalCount > 1 && onNavigate && (
          <button
            onClick={() => onNavigate('next')}
            disabled={!canNavigateNext}
            className="flex-shrink-0 ml-6 text-white/80 hover:text-white transition-all duration-200 disabled:opacity-30 disabled:cursor-not-allowed z-60"
            title="Next ad (Right arrow)"
          >
            <ChevronRight className="w-12 h-12 drop-shadow-lg" />
          </button>
        )}
      </div>
    </div>
  )
}
