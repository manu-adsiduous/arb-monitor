"use client";

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { 
  Play, 
  ExternalLink, 
  Calendar, 
  DollarSign, 
  Eye, 
  RefreshCw,
  Image as ImageIcon,
  Video,
  AlertCircle,
  Loader2,
  CheckCircle2,
  Clock
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import AdDetailModal from "./AdDetailModal";

interface ScrapedAd {
  id: number;
  metaAdId: string;
  domainName: string;
  pageName: string;
  pageId: string;
  primaryText: string;
  headline: string;
  description: string;
  callToAction: string;
  landingPageUrl: string;
  displayUrl: string;
  imageUrls: string[];
  videoUrls: string[];
  localImagePaths: string[];
  localVideoPaths: string[];
  adFormat: string;
  fundingEntity: string;
  adCreationDate: string;
  adDeliveryStartDate: string;
  adDeliveryStopDate: string | null;
  isActive: boolean;
  spendRangeLower: number;
  spendRangeUpper: number;
  impressionsRangeLower: number;
  impressionsRangeUpper: number;
  scrapedAt: string;
  lastUpdated: string;
  // Compliance analysis data
  complianceAnalysis?: {
    id: number;
    complianceScore: number;
    complianceStatus: 'EXCELLENT' | 'GOOD' | 'WARNING' | 'POOR' | 'CRITICAL';
    analysisNotes: string;
    violations: Array<{
      id: number;
      description: string;
      severity: 'CRITICAL' | 'MAJOR' | 'MINOR';
      violatedText: string;
      rule: {
        ruleName: string;
        description: string;
        category: string;
      };
    }>;
    // New binary compliance fields
    adCreativeCompliant?: boolean;
    adCreativeReason?: string;
    landingPageRelevant?: boolean;
    landingPageReason?: string;
    racRelevant?: boolean;
    racReason?: string;
    overallCompliant?: boolean;
  };
}

interface AdGridProps {
  domainName: string;
  className?: string;
}

export function AdGrid({ domainName, className = "" }: AdGridProps) {
  const [ads, setAds] = useState<ScrapedAd[]>([]);
  const [loading, setLoading] = useState(true);
  const [scraping, setScraping] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalAds, setTotalAds] = useState(0);
  const [selectedAdId, setSelectedAdId] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleAdClick = (adId: number) => {
    setSelectedAdId(adId.toString());
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedAdId(null);
  };

  const handleNavigateAd = (direction: 'prev' | 'next') => {
    if (!selectedAdId || ads.length === 0) return;
    
    const currentIndex = ads.findIndex(ad => ad.id.toString() === selectedAdId);
    if (currentIndex === -1) return;
    
    let newIndex;
    if (direction === 'next') {
      newIndex = currentIndex === ads.length - 1 ? 0 : currentIndex + 1;
    } else {
      newIndex = currentIndex === 0 ? ads.length - 1 : currentIndex - 1;
    }
    
    setSelectedAdId(ads[newIndex].id.toString());
  };

  useEffect(() => {
    fetchAds();
  }, [domainName]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchAds = async (pageNum = 0, append = false) => {
    try {
      setLoading(!append);
      setError(null);

      // Fetch ads with compliance analysis data
      const response = await fetch(
        `http://localhost:8080/api/ads/domain/${encodeURIComponent(domainName)}?page=${pageNum}&size=20&includeAnalysis=true`,
        {
          headers: {
            'X-User-ID': '1'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch ads: ${response.statusText}`);
      }

      const data = await response.json();
      
      if (append) {
        setAds(prev => [...prev, ...data.ads]);
      } else {
        setAds(data.ads);
      }
      
      setHasMore(data.hasNext);
      setTotalAds(data.totalElements);
      setPage(pageNum);

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch ads');
    } finally {
      setLoading(false);
    }
  };

  const triggerScraping = async () => {
    try {
      setScraping(true);
      setError(null);

      const response = await fetch(
        `http://localhost:8080/api/ads/scrape/${encodeURIComponent(domainName)}`,
        { 
          method: 'POST',
          headers: {
            'X-User-ID': '1'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to start scraping: ${response.statusText}`);
      }

      // Apify takes time to process, wait 30 seconds then refresh
      setTimeout(() => {
        fetchAds();
        setScraping(false);
      }, 30000);

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start scraping');
      setScraping(false);
    }
  };

  const loadMore = () => {
    if (hasMore && !loading) {
      fetchAds(page + 1, true);
    }
  };

  const getComplianceColor = (status?: string) => {
    switch (status) {
      case 'EXCELLENT': return 'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800';
      case 'GOOD': return 'bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-900/20 dark:text-blue-400 dark:border-blue-800';
      case 'WARNING': return 'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-400 dark:border-yellow-800';
      case 'POOR': return 'bg-orange-100 text-orange-800 border-orange-200 dark:bg-orange-900/20 dark:text-orange-400 dark:border-orange-800';
      case 'CRITICAL': return 'bg-red-100 text-red-800 border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-800';
      default: return 'bg-gray-100 text-gray-800 border-gray-200 dark:bg-gray-900/20 dark:text-gray-400 dark:border-gray-800';
    }
  };

  const getComplianceIcon = (status?: string) => {
    switch (status) {
      case 'EXCELLENT': 
      case 'GOOD': return <CheckCircle2 className="h-3 w-3" />;
      case 'WARNING': 
      case 'POOR': 
      case 'CRITICAL': return <AlertCircle className="h-3 w-3" />;
      default: return <Clock className="h-3 w-3" />;
    }
  };

  const getComplianceTips = (violations: any[]) => {
    if (!violations || violations.length === 0) return [];
    
    return violations.map(violation => ({
      severity: violation.severity,
      tip: getComplianceTip(violation.rule?.category, violation.description)
    }));
  };

  const getComplianceTip = (category?: string, description?: string) => {
    // Generate helpful tips based on violation category
    switch (category?.toLowerCase()) {
      case 'misleading_claims':
        return 'Use specific, verifiable claims with proper disclaimers';
      case 'medical_claims':
        return 'Remove medical claims or add FDA disclaimer';
      case 'financial_guarantees':
        return 'Replace guarantees with "potential" or "up to" language';
      case 'unrealistic_results':
        return 'Use realistic timeframes and include "results may vary"';
      case 'missing_disclosures':
        return 'Add required disclosures and terms & conditions';
      default:
        return description || 'Review ad content for compliance';
    }
  };

  const getAdMedia = (ad: ScrapedAd) => {
    // Prefer local media files over remote URLs
    if (ad.localVideoPaths && ad.localVideoPaths.length > 0) {
      // Convert local path to API endpoint
      const localPath = ad.localVideoPaths[0];
      const apiUrl = `http://localhost:8080/api/media${localPath.replace('./media', '')}`;
      return { type: 'video', url: apiUrl };
    }
    if (ad.localImagePaths && ad.localImagePaths.length > 0) {
      // Convert local path to API endpoint
      const localPath = ad.localImagePaths[0];
      const apiUrl = `http://localhost:8080/api/media${localPath.replace('./media', '')}`;
      return { type: 'image', url: apiUrl };
    }
    
    // Fallback to remote URLs if local files aren't available
    if (ad.videoUrls && ad.videoUrls.length > 0) {
      return { type: 'video', url: ad.videoUrls[0] };
    }
    if (ad.imageUrls && ad.imageUrls.length > 0) {
      return { type: 'image', url: ad.imageUrls[0] };
    }
    return null;
  };

  if (loading && ads.length === 0) {
    return (
      <div className={`${className}`}>
        <div className="glass-card rounded-3xl p-8 text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-primary" />
          <p className="text-slate-600 dark:text-slate-400">Loading ads...</p>
        </div>
      </div>
    );
  }

  if (error && ads.length === 0) {
    return (
      <div className={`${className}`}>
        <div className="glass-card rounded-3xl p-8 text-center">
          <AlertCircle className="h-8 w-8 mx-auto mb-4 text-red-500" />
          <p className="text-red-600 dark:text-red-400 mb-4">{error}</p>
          <Button onClick={() => fetchAds()} className="glass-button">
            Try Again
          </Button>
        </div>
      </div>
    );
  }

    if (ads.length === 0) {
    return (
      <div className={`${className}`}>
        <div className="glass-card rounded-3xl p-8 text-center">
          <ImageIcon className="h-12 w-12 mx-auto mb-4 text-slate-400" />
          <h3 className="text-lg font-semibold text-slate-800 dark:text-white mb-2">
            No ads found for this domain
          </h3>
          <p className="text-slate-600 dark:text-slate-400 mb-6">
            No active ads were found for {domainName}. Use the refresh icon next to the domain name to fetch the latest ads.
          </p>
          <div className="text-sm text-slate-500 dark:text-slate-400 text-center">
            Our system automatically checks for new ads daily.
          </div>
        </div>
      </div>
    );
  }

  const createMockData = () => {
    // Create mock ad data for demonstration
    const mockAds = [
      {
        id: 1,
        metaAdId: `mock_ad_${domainName}_001`,
        domainName: domainName,
        pageName: "Best Deals Hub",
        pageId: "123456789",
        primaryText: "🔥 FLASH SALE: Up to 70% OFF on Premium Products! Limited time offer - don't miss out on these incredible deals. Shop now and save big!",
        headline: "Massive Flash Sale - 70% OFF",
        description: "Discover unbeatable prices on top-rated products. Free shipping on orders over $50. Shop now before it's too late!",
        callToAction: "Shop Now",
        landingPageUrl: `https://${domainName}/flash-sale`,
        displayUrl: domainName,
        imageUrls: [
          "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=600&fit=crop",
          "https://images.unsplash.com/photo-1563013544-824ae1b704d3?w=800&h=600&fit=crop"
        ],
        videoUrls: [],
        localImagePaths: [],
        localVideoPaths: [],
        adFormat: "SINGLE_IMAGE",
        fundingEntity: "Deal Finder LLC",
        adCreationDate: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
        adDeliveryStartDate: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toLocaleDateString(),
        adDeliveryStopDate: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toLocaleDateString(),
        isActive: true,
        spendRangeLower: 1000,
        spendRangeUpper: 5000,
        impressionsRangeLower: 50000,
        impressionsRangeUpper: 100000,
        scrapedAt: new Date().toISOString(),
        lastUpdated: new Date().toISOString()
      },
      {
        id: 2,
        metaAdId: `mock_ad_${domainName}_002`,
        domainName: domainName,
        pageName: "Smart Shoppers",
        pageId: "987654321",
        primaryText: "💎 Exclusive VIP Access: Get early access to our premium collection. Members save an extra 20% on already discounted items!",
        headline: "VIP Early Access - Extra 20% OFF",
        description: "Join thousands of smart shoppers who never pay full price. Exclusive deals, early access, and premium customer service.",
        callToAction: "Join VIP",
        landingPageUrl: `https://${domainName}/vip-access`,
        displayUrl: domainName,
        imageUrls: [],
        videoUrls: ["https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4"],
        localImagePaths: [],
        localVideoPaths: [],
        adFormat: "VIDEO",
        fundingEntity: "Smart Shopping Inc",
        adCreationDate: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
        adDeliveryStartDate: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toLocaleDateString(),
        adDeliveryStopDate: null,
        isActive: true,
        spendRangeLower: 2000,
        spendRangeUpper: 8000,
        impressionsRangeLower: 75000,
        impressionsRangeUpper: 150000,
        scrapedAt: new Date().toISOString(),
        lastUpdated: new Date().toISOString()
      },
      {
        id: 3,
        metaAdId: `mock_ad_${domainName}_003`,
        domainName: domainName,
        pageName: "Deal Masters",
        pageId: "456789123",
        primaryText: "🛍️ Black Friday Prices All Year Round! Why wait? Get the best deals on electronics, fashion, home & garden, and more. Free returns guaranteed!",
        headline: "Black Friday Prices Every Day",
        description: "No need to wait for sales - enjoy Black Friday prices 365 days a year. Top brands, lowest prices, satisfaction guaranteed.",
        callToAction: "Browse Deals",
        landingPageUrl: `https://${domainName}/year-round-deals`,
        displayUrl: domainName,
        imageUrls: [
          "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=800&h=600&fit=crop",
          "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&h=600&fit=crop",
          "https://images.unsplash.com/photo-1534452203293-494d7ddbf7e0?w=800&h=600&fit=crop"
        ],
        videoUrls: [],
        localImagePaths: [],
        localVideoPaths: [],
        adFormat: "CAROUSEL",
        fundingEntity: "Deal Masters Corp",
        adCreationDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
        adDeliveryStartDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toLocaleDateString(),
        adDeliveryStopDate: null,
        isActive: true,
        spendRangeLower: 5000,
        spendRangeUpper: 15000,
        impressionsRangeLower: 100000,
        impressionsRangeUpper: 250000,
        scrapedAt: new Date().toISOString(),
        lastUpdated: new Date().toISOString()
      },
      {
        id: 4,
        metaAdId: `mock_ad_${domainName}_004`,
        domainName: domainName,
        pageName: "Bargain Central",
        pageId: "789123456",
        primaryText: "🎯 Targeted Savings Just for You! Our AI finds the best deals based on your interests. Personalized recommendations, unbeatable prices!",
        headline: "AI-Powered Personal Deals",
        description: "Let our smart technology find the perfect deals for you. Personalized shopping experience with guaranteed savings on every purchase.",
        callToAction: "Get My Deals",
        landingPageUrl: `https://${domainName}/personal-deals`,
        displayUrl: domainName,
        imageUrls: [
          "https://images.unsplash.com/photo-1556742111-a301076d9d18?w=800&h=600&fit=crop"
        ],
        videoUrls: [],
        localImagePaths: [],
        localVideoPaths: [],
        adFormat: "COLLECTION",
        fundingEntity: "Bargain Central LLC",
        adCreationDate: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
        adDeliveryStartDate: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toLocaleDateString(),
        adDeliveryStopDate: null,
        isActive: true,
        spendRangeLower: 1500,
        spendRangeUpper: 6000,
        impressionsRangeLower: 40000,
        impressionsRangeUpper: 90000,
        scrapedAt: new Date().toISOString(),
        lastUpdated: new Date().toISOString()
      }
    ];

    setAds(mockAds);
    setTotalAds(mockAds.length);
    setLoading(false);
  };

  return (
    <div className={`${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-slate-800 dark:text-white">
            Active Ads ({totalAds})
          </h2>
          <p className="text-sm text-slate-600 dark:text-slate-400">
            Latest ads from Meta Ad Library for {domainName}
          </p>
        </div>
        <Button 
          onClick={triggerScraping} 
          disabled={scraping}
          className="glass-button border border-slate-200/50 dark:border-slate-700/50"
        >
          {scraping ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Scraping...
            </>
          ) : (
            <>
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh Ads
            </>
          )}
        </Button>
      </div>

      {/* Ad Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {ads.map((ad, index) => {
          const media = getAdMedia(ad);
          
          return (
            <motion.div
              key={ad.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
              className="glass-card rounded-2xl overflow-hidden hover:scale-105 transition-all duration-300 cursor-pointer"
              onClick={() => handleAdClick(ad.id)}
            >
              {/* Media Section */}
              <div className="relative aspect-square bg-slate-100 dark:bg-slate-800">
                {media ? (
                  media.type === 'video' ? (
                    <div className="relative w-full h-full">
                      <video
                        src={media.url}
                        className="w-full h-full object-cover"
                        controls={false}
                        muted
                        preload="metadata"
                      />
                      <div className="absolute inset-0 bg-black/20 flex items-center justify-center">
                        <Play className="h-12 w-12 text-white drop-shadow-lg" />
                      </div>
                      <Badge className="absolute top-2 right-2 bg-red-500 text-white">
                        <Video className="h-3 w-3 mr-1" />
                        Video
                      </Badge>
                    </div>
                  ) : (
                    <div className="relative w-full h-full">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={media.url}
                        alt={ad.headline || 'Ad creative'}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                        }}
                      />
                      <Badge className="absolute top-2 right-2 bg-blue-500 text-white">
                        <ImageIcon className="h-3 w-3 mr-1" />
                        Image
                      </Badge>
                    </div>
                  )
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <ImageIcon className="h-16 w-16 text-slate-400" />
                  </div>
                )}
              </div>

              {/* Content Section */}
              <div className="p-4">
                {/* Page Info */}
                <div className="flex items-center justify-between mb-3">
                  <p className="text-sm font-semibold text-slate-800 dark:text-white truncate">
                    {ad.pageName || ad.fundingEntity || 'Unknown Page'}
                  </p>
                  <Badge variant="outline" className="text-xs">
                    {ad.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                </div>

                {/* Ad Text */}
                <div className="space-y-2 mb-4">
                  {ad.headline && (
                    <h4 className="font-semibold text-sm text-slate-800 dark:text-white line-clamp-2">
                      {ad.headline}
                    </h4>
                  )}
                  {ad.primaryText && (
                    <p className="text-xs text-slate-600 dark:text-slate-400 line-clamp-3">
                      {ad.primaryText}
                    </p>
                  )}
                  {ad.callToAction && (
                    <Badge variant="secondary" className="text-xs">
                      {ad.callToAction}
                    </Badge>
                  )}
                </div>

                {/* Compliance Status */}
                <div className="space-y-3">
                  {ad.complianceAnalysis ? (
                    <>
                      {/* Binary Compliance Status */}
                      <div className="space-y-2">
                        <div className="text-xs font-medium text-slate-700 dark:text-slate-300 mb-2">Compliance Check:</div>
                        
                        {/* Ad Creative */}
                        <div className="flex items-center justify-between">
                          <span className="text-xs text-slate-600 dark:text-slate-400">ad creative</span>
                          <div className="flex items-center space-x-1">
                            {ad.complianceAnalysis.adCreativeCompliant ? (
                              <CheckCircle2 className="h-3 w-3 text-green-500" />
                            ) : (
                              <AlertCircle className="h-3 w-3 text-red-500" />
                            )}
                            <span className="text-xs">
                              {ad.complianceAnalysis.adCreativeCompliant ? '✅' : '❌'}
                            </span>
                          </div>
                        </div>
                        
                        {/* Landing Page Relevance */}
                        <div className="flex items-center justify-between">
                          <span className="text-xs text-slate-600 dark:text-slate-400">landing page relevance</span>
                          <div className="flex items-center space-x-1">
                            {ad.complianceAnalysis.landingPageRelevant ? (
                              <CheckCircle2 className="h-3 w-3 text-green-500" />
                            ) : (
                              <AlertCircle className="h-3 w-3 text-red-500" />
                            )}
                            <span className="text-xs">
                              {ad.complianceAnalysis.landingPageRelevant ? '✅' : '❌'}
                            </span>
                          </div>
                        </div>
                        
                        {/* RAC Relevance */}
                        <div className="flex items-center justify-between">
                          <span className="text-xs text-slate-600 dark:text-slate-400">rac relevance</span>
                          <div className="flex items-center space-x-1">
                            {ad.complianceAnalysis.racReason && ad.complianceAnalysis.racReason.includes("turned off") ? (
                              <>
                                <div className="w-3 h-3 rounded-full bg-gray-300 dark:bg-gray-600"></div>
                                <span className="text-xs text-gray-500 dark:text-gray-400">off</span>
                              </>
                            ) : (
                              <>
                                {ad.complianceAnalysis.racRelevant ? (
                                  <CheckCircle2 className="h-3 w-3 text-green-500" />
                                ) : (
                                  <AlertCircle className="h-3 w-3 text-red-500" />
                                )}
                                <span className="text-xs">
                                  {ad.complianceAnalysis.racRelevant ? '✅' : '❌'}
                                </span>
                              </>
                            )}
                          </div>
                        </div>
                        
                        {/* Overall Status */}
                        <div className="pt-2 border-t border-slate-200/50 dark:border-slate-700/50">
                          <Badge className={`text-xs px-2 py-1 border ${ad.complianceAnalysis.overallCompliant ? 'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800' : 'bg-red-100 text-red-800 border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-800'}`}>
                            {ad.complianceAnalysis.overallCompliant ? (
                              <><CheckCircle2 className="h-3 w-3 mr-1" />COMPLIANT</>
                            ) : (
                              <><AlertCircle className="h-3 w-3 mr-1" />NOT COMPLIANT</>
                            )}
                          </Badge>
                        </div>
                      </div>

                      {/* Violations */}
                      {ad.complianceAnalysis.violations && ad.complianceAnalysis.violations.length > 0 && (
                        <div className="space-y-2">
                          <span className="text-xs font-medium text-red-600 dark:text-red-400">
                            {ad.complianceAnalysis.violations.length} Issue{ad.complianceAnalysis.violations.length > 1 ? 's' : ''} Found:
                          </span>
                          <div className="space-y-1">
                            {ad.complianceAnalysis.violations.slice(0, 2).map((violation, idx) => (
                              <div key={idx} className="text-xs">
                                <Badge variant="destructive" className="text-xs mb-1">
                                  {violation.severity}: {violation.rule?.ruleName}
                                </Badge>
                                <p className="text-xs text-slate-600 dark:text-slate-400 italic">
                                  💡 {getComplianceTip(violation.rule?.category, violation.description)}
                                </p>
                              </div>
                            ))}
                            {ad.complianceAnalysis.violations.length > 2 && (
                              <p className="text-xs text-slate-500 dark:text-slate-400">
                                +{ad.complianceAnalysis.violations.length - 2} more issue{ad.complianceAnalysis.violations.length - 2 > 1 ? 's' : ''}
                              </p>
                            )}
                          </div>
                        </div>
                      )}

                      {/* Ad Date */}
                      {ad.adDeliveryStartDate && (
                        <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 pt-2 border-t border-slate-200/50 dark:border-slate-700/50">
                          <span className="flex items-center">
                            <Calendar className="h-3 w-3 mr-1" />
                            Started
                          </span>
                          <span>{new Date(ad.adDeliveryStartDate).toLocaleDateString()}</span>
                        </div>
                      )}
                    </>
                  ) : (
                    <div className="text-center py-3">
                      <Badge className={`text-xs px-2 py-1 border ${getComplianceColor()}`}>
                        {getComplianceIcon()}
                        <span className="ml-1">Not Analyzed</span>
                      </Badge>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                        Run analysis to check compliance
                      </p>
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="flex gap-2 mt-4">
                  {ad.landingPageUrl && (
                    <Button
                      size="sm"
                      variant="outline"
                      className="flex-1 text-xs"
                      onClick={() => window.open(ad.landingPageUrl, '_blank')}
                    >
                      <ExternalLink className="h-3 w-3 mr-1" />
                      Visit
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 text-xs"
                    onClick={() => window.open(`https://www.facebook.com/ads/library/?id=${ad.metaAdId}`, '_blank')}
                  >
                    View on Meta
                  </Button>
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>

      {/* Load More */}
      {hasMore && (
        <div className="text-center mt-8">
          <Button 
            onClick={loadMore} 
            disabled={loading}
            className="glass-button"
          >
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Loading...
              </>
            ) : (
              'Load More Ads'
            )}
          </Button>
        </div>
      )}

      {error && ads.length > 0 && (
        <div className="mt-4 p-4 bg-red-50 dark:bg-red-900/20 rounded-xl">
          <p className="text-red-600 dark:text-red-400 text-sm">{error}</p>
        </div>
      )}

            {/* Ad Detail Modal */}
      <AdDetailModal
        adId={selectedAdId}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onNavigate={handleNavigateAd}
        canNavigatePrev={ads.length > 1}
        canNavigateNext={ads.length > 1}
        currentIndex={selectedAdId ? ads.findIndex(ad => ad.id.toString() === selectedAdId) + 1 : 0}
        totalCount={ads.length}
      />
    </div>
  );
}
