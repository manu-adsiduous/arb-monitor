"use client"

import { useState, useEffect } from "react"

interface Domain {
  id: number;
  domainName: string;
  status: string;
  complianceScore?: number;
  activeAds?: number;
  violations?: number;
}

const DEMO_USER_ID = 1;

const domainApi = {
  getDomains: async (userId: number): Promise<Domain[]> => {
    // Return empty array for now - this can be implemented later
    return [];
  }
};
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { useSession, signOut } from "next-auth/react"
import { motion } from "framer-motion"
import { 
  BarChart3, 
  Settings, 
  Home, 
  Plus,
  Menu,
  X,
  Bell,
  Search,
  User,
  LogOut,
  ChevronDown,
  Loader2,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Globe,
  Eye,
  Mail
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { toast } from "sonner"

const navigation = [
  { name: "Dashboard", href: "/dashboard", icon: Home },
  { name: "Domains", href: "/dashboard/domains", icon: BarChart3 },
  { name: "Add Domain", href: "/dashboard/add-domain", icon: Plus },
  { name: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { name: "Settings", href: "/dashboard/settings", icon: Settings },
]

// Mock notifications data - in a real app, this would come from your API
const mockNotifications = [
  {
    id: 1,
    type: 'success',
    icon: CheckCircle2,
    title: 'Domain analysis completed',
    message: 'needtheinfo.com analysis is complete with 92% compliance',
    time: '5 minutes ago',
    isRead: false
  },
  {
    id: 2,
    type: 'warning',
    icon: AlertTriangle,
    title: 'Compliance violations found',
    message: '3 violations detected in pricefinderpro.com ads',
    time: '2 hours ago',
    isRead: false
  },
  {
    id: 3,
    type: 'info',
    icon: Clock,
    title: 'Scheduled scan started',
    message: 'Daily compliance check initiated for livelocally.net',
    time: '4 hours ago',
    isRead: false
  },
  {
    id: 4,
    type: 'success',
    icon: Globe,
    title: 'New domain added',
    message: 'bestoptions.net has been added to monitoring',
    time: '1 day ago',
    isRead: true
  },
  {
    id: 5,
    type: 'info',
    icon: Eye,
    title: 'Weekly report ready',
    message: 'Your compliance summary for this week is available',
    time: '2 days ago',
    isRead: true
  }
]

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [notifications, setNotifications] = useState(mockNotifications)
  const [searchTerm, setSearchTerm] = useState("")
  const [searchResults, setSearchResults] = useState<Domain[]>([])
  const [allDomains, setAllDomains] = useState<Domain[]>([])
  const [showSearchResults, setShowSearchResults] = useState(false)
  const [searchLoading, setSearchLoading] = useState(false)
  const pathname = usePathname()
  const router = useRouter()
  const { data: session, status } = useSession()

  // Load domains for search functionality
  useEffect(() => {
    if (status === "authenticated" || (status === "loading" && session)) {
      console.log("Loading domains for search...")
      loadDomainsForSearch()
    }
  }, [status, session])

  // Keyboard shortcut for search (Cmd/Ctrl + K)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        const input = document.getElementById('dashboard-search') as HTMLInputElement
        input?.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  // Redirect to sign in if not authenticated
  useEffect(() => {
    if (status === "unauthenticated") {
      router.push("/auth/signin")
    }
  }, [status, router])

  const loadDomainsForSearch = async () => {
    try {
      console.log("Loading domains for search...")
      setSearchLoading(true)
      const data = await domainApi.getDomains(DEMO_USER_ID)
      
      if (Array.isArray(data)) {
        setAllDomains(data)
      } else {
        setAllDomains([])
      }
    } catch (error) {
      setAllDomains([])
    } finally {
      setSearchLoading(false)
    }
  }

  // Also load domains on mount regardless of auth status (for debugging)
  useEffect(() => {
    loadDomainsForSearch()
  }, [])

  const handleSignOut = async () => {
    try {
      await signOut({ callbackUrl: "/" })
      toast.success("Signed out successfully")
    } catch (error) {
      toast.error("Error signing out")
    }
  }

  // Notification helpers
  const unreadCount = notifications.filter(n => !n.isRead).length
  
  const markAsRead = (notificationId: number) => {
    setNotifications(prev => 
      prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
    )
  }

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
  }

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'success':
        return 'text-green-500'
      case 'warning':
        return 'text-yellow-500'
      case 'error':
        return 'text-red-500'
      case 'info':
      default:
        return 'text-blue-500'
    }
  }

  // Search functionality
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setSearchTerm(value)
    
    if (value.trim()) {
      const filtered = allDomains.filter(domain =>
        domain.domainName.toLowerCase().includes(value.toLowerCase())
      )
      setSearchResults(filtered)
      setShowSearchResults(true)
    } else {
      setSearchResults([])
      setShowSearchResults(false)
    }
  }

  const handleSearchFocus = () => {
    if (searchTerm.trim()) {
      setShowSearchResults(true)
    }
  }

  const handleSearchBlur = () => {
    // Delay hiding to allow clicking on results
    setTimeout(() => setShowSearchResults(false), 200)
  }

  const handleSearchResultClick = (domainId: number) => {
    setSearchTerm("")
    setShowSearchResults(false)
    router.push(`/dashboard/domains/${domainId}`)
  }

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      setSearchTerm("")
      setShowSearchResults(false)
      const input = document.getElementById('dashboard-search') as HTMLInputElement
      input?.blur()
    }
  }

  const userInitials = session?.user?.name
    ?.split(" ")
    .map(n => n[0])
    .join("")
    .toUpperCase() || "U"

  // Show loading state while checking authentication
  if (status === "loading") {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">Loading...</p>
        </div>
      </div>
    )
  }

  // Don't render dashboard if not authenticated
  if (status === "unauthenticated") {
    return null
  }

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

      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-black/30 backdrop-blur-sm z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <div className={`fixed inset-y-0 left-0 z-30 w-80 transform ${
        sidebarOpen ? "translate-x-0" : "-translate-x-full"
      } transition-transform duration-300 ease-in-out lg:translate-x-0 flex flex-col`}>
        
        {/* Sidebar content - single glass container */}
        <div className="h-full m-6 glass-card rounded-3xl flex flex-col">
          
          {/* Sidebar header */}
          <div className="flex items-center justify-between px-8 py-6">
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 gradient-primary rounded-2xl flex items-center justify-center shadow-glass">
                <span className="text-white font-bold text-lg">A</span>
              </div>
              <div>
                <span className="text-xl font-bold text-slate-800 dark:text-white">Arb Monitor</span>
                <p className="text-xs text-slate-500 dark:text-slate-400">Admin Dashboard</p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="lg:hidden hover:bg-white/20 dark:hover:bg-slate-700/20 rounded-xl"
              onClick={() => setSidebarOpen(false)}
            >
              <X className="h-5 w-5 text-slate-600 dark:text-slate-300" />
            </Button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-6">
            <div className="space-y-1">
              {navigation.map((item) => {
                const isActive = pathname === item.href
                const isNotifications = item.name === "Notifications"
                return (
                  <Link key={item.name} href={item.href}>
                    <div
                      className={`group flex items-center px-4 py-3 text-sm font-medium rounded-2xl transition-all duration-200 ${
                        isActive
                          ? "bg-gradient-to-r from-purple-500 to-blue-600 text-white shadow-lg"
                          : "text-slate-700 hover:bg-white/10 dark:text-slate-300 dark:hover:bg-slate-700/10"
                      }`}
                    >
                      <item.icon
                        className={`h-5 w-5 flex-shrink-0 mr-3 ${
                          isActive ? "text-white" : "text-slate-500 group-hover:text-slate-700 dark:text-slate-400 dark:group-hover:text-slate-200"
                        }`}
                      />
                      <span className="font-medium">{item.name}</span>
                      {isNotifications && unreadCount > 0 && (
                        <span className={`ml-auto text-xs font-bold px-2 py-0.5 rounded-full min-w-[1.25rem] h-5 flex items-center justify-center ${
                          isActive 
                            ? "bg-white/20 text-white" 
                            : "bg-red-500 text-white"
                        }`}>
                          {unreadCount}
                        </span>
                      )}
                    </div>
                  </Link>
                )
              })}
            </div>
          </nav>

          {/* User info at bottom */}
          <div className="mt-auto px-6 pb-6">
            <div className="flex items-center space-x-3 px-4 py-3 rounded-2xl hover:bg-white/5 dark:hover:bg-slate-700/10 transition-colors">
              <div className="w-10 h-10 gradient-primary rounded-xl flex items-center justify-center">
                <span className="text-white font-bold text-sm">{userInitials}</span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-slate-800 dark:text-white truncate">
                  {session?.user?.name}
                </p>
                <div className="flex items-center space-x-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                    {session?.user?.email}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

            {/* Top bar - Full width header that extends behind sidebar */}
      <div className="sticky top-0 z-20 bg-white/80 dark:bg-slate-900/80 backdrop-blur-xl border-b border-slate-200/20 dark:border-slate-700/20">
        <div className="flex items-center justify-between h-20 px-8 lg:pl-96">
          {/* Left side - Search area */}
          <div className="flex items-center flex-1 min-w-0 relative"
               onClick={() => {
                 const input = document.getElementById('dashboard-search');
                 if (input) input.focus();
               }}>
            <Button
              variant="ghost"
              size="sm"
              className="lg:hidden hover:bg-slate-100/50 dark:hover:bg-slate-700/20 rounded-xl flex-shrink-0"
              onClick={(e) => {
                e.stopPropagation();
                setSidebarOpen(true);
              }}
            >
              <Menu className="h-5 w-5 text-slate-600 dark:text-slate-300" />
            </Button>
            
            <div className="ml-4 flex items-center flex-1 min-w-0 relative">
              <Search className="h-5 w-5 text-slate-400 flex-shrink-0" />
              <input
                id="dashboard-search"
                type="text"
                placeholder="Search domains (⌘K)"
                value={searchTerm}
                onChange={handleSearchChange}
                onFocus={handleSearchFocus}
                onBlur={handleSearchBlur}
                onKeyDown={handleSearchKeyDown}
                className="ml-3 bg-transparent border-0 outline-none text-base placeholder-slate-400 dark:placeholder-slate-500 text-slate-700 dark:text-slate-200 flex-1 min-w-0"
              />
              
              {/* Search Results Dropdown */}
              {showSearchResults && (
                <motion.div 
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  transition={{ duration: 0.2 }}
                  className="absolute top-full left-0 right-0 mt-2 glass-thin rounded-2xl shadow-lg max-h-80 overflow-y-auto z-50"
                >
                  {searchLoading ? (
                    <div className="p-4 text-center">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-2"></div>
                      <p className="text-sm text-slate-600 dark:text-slate-400">Loading domains...</p>
                    </div>
                  ) : searchResults.length === 0 ? (
                    <div className="p-4 text-center">
                      <Globe className="h-8 w-8 mx-auto mb-2 text-slate-400" />
                      <p className="text-sm text-slate-600 dark:text-slate-400">
                        No domains found for "{searchTerm}"
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-500 mt-1">
                        Total domains loaded: {allDomains.length}
                      </p>
                    </div>
                  ) : (
                    <div className="py-2">
                      <div className="px-4 py-2 text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide">
                        Domains ({searchResults.length} of {allDomains.length})
                      </div>
                      {searchResults.map((domain) => (
                        <div
                          key={domain.id}
                          onClick={() => handleSearchResultClick(domain.id)}
                          className="px-4 py-3 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors mx-2 rounded-xl"
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-3 flex-1 min-w-0">
                              <div className="w-10 h-10 gradient-primary rounded-xl flex items-center justify-center flex-shrink-0">
                                <Globe className="h-5 w-5 text-white" />
                              </div>
                              <div className="flex-1 min-w-0">
                                <p className="text-sm font-semibold text-slate-900 dark:text-white truncate">
                                  {domain.domainName}
                                </p>
                                <div className="flex items-center space-x-2 mt-1">
                                  <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                                    domain.status === 'ACTIVE' 
                                      ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
                                      : 'bg-slate-100 text-slate-800 dark:bg-slate-900/20 dark:text-slate-400'
                                  }`}>
                                    {domain.status}
                                  </span>
                                  <span className="text-xs text-slate-600 dark:text-slate-400">
                                    {domain.activeAds || 0} ads
                                  </span>
                                  {(domain.violations || 0) > 0 && (
                                    <span className="text-xs text-red-600 dark:text-red-400">
                                      {domain.violations} violations
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                            
                            {/* Compliance Score - Right Side */}
                            <div className="text-right flex-shrink-0 ml-4">
                              <div className="text-xs text-slate-500 dark:text-slate-400 mb-1">
                                Compliance
                              </div>
                              <div className={`text-lg font-bold ${
                                domain.complianceScore === null || domain.complianceScore === undefined
                                  ? 'text-slate-400 dark:text-slate-500'
                                  : (domain.complianceScore || 0) >= 80 
                                    ? 'text-green-600 dark:text-green-400'
                                    : (domain.complianceScore || 0) >= 60 
                                      ? 'text-yellow-600 dark:text-yellow-400'
                                      : 'text-red-600 dark:text-red-400'
                              }`}>
                                {domain.complianceScore ? `${Math.round(domain.complianceScore)}%` : 'N/A'}
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}
            </div>
          </div>

          {/* Right side - Actions */}
          <div className="flex items-center space-x-4">
            <DropdownMenu modal={false}>
              <DropdownMenuTrigger asChild>
                <div className="rounded-xl p-2 relative hover:bg-slate-100/50 dark:hover:bg-slate-700/20 transition-colors cursor-pointer">
                  <Bell className="h-5 w-5 text-slate-500 dark:text-slate-400" />
                  {unreadCount > 0 && (
                    <div className="absolute -top-1 -right-1 w-4 h-4 gradient-primary rounded-full flex items-center justify-center">
                      <span className="text-xs font-bold text-white">{unreadCount}</span>
                    </div>
                  )}
                </div>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-80 glass-thin rounded-2xl shadow-lg max-h-96 overflow-y-auto">
                <div className="flex items-center justify-between p-4 pb-2">
                  <DropdownMenuLabel className="font-semibold text-base">Notifications</DropdownMenuLabel>
                  {unreadCount > 0 && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={markAllAsRead}
                      className="text-xs text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white h-auto p-1"
                    >
                      Mark all read
                    </Button>
                  )}
                </div>
                <DropdownMenuSeparator />
                
                {notifications.length === 0 ? (
                  <div className="p-6 text-center">
                    <Bell className="h-8 w-8 mx-auto mb-2 text-slate-400" />
                    <p className="text-sm text-slate-600 dark:text-slate-400">No notifications</p>
                  </div>
                ) : (
                  <div className="max-h-64 overflow-y-auto">
                    {notifications.map((notification) => (
                      <DropdownMenuItem
                        key={notification.id}
                        className={`p-4 cursor-pointer rounded-xl mx-2 my-1 ${
                          !notification.isRead ? 'bg-blue-50/50 dark:bg-blue-900/20' : ''
                        }`}
                        onClick={() => markAsRead(notification.id)}
                      >
                        <div className="flex items-start space-x-3 w-full">
                          <div className={`mt-0.5 ${getNotificationColor(notification.type)}`}>
                            <notification.icon className="h-4 w-4" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center space-x-2">
                              <p className="text-sm font-medium text-slate-900 dark:text-white truncate">
                                {notification.title}
                              </p>
                              {!notification.isRead && (
                                <div className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0" />
                              )}
                            </div>
                            <p className="text-xs text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                              {notification.message}
                            </p>
                            <p className="text-xs text-slate-500 dark:text-slate-500 mt-1">
                              {notification.time}
                            </p>
                          </div>
                        </div>
                      </DropdownMenuItem>
                    ))}
                  </div>
                )}
                
                              <DropdownMenuSeparator />
              <div className="p-2">
                <Link 
                  href="/dashboard/notifications" 
                  className="flex items-center justify-center w-full px-3 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl transition-colors"
                >
                  View all notifications
                </Link>
              </div>
              </DropdownMenuContent>
            </DropdownMenu>

            <DropdownMenu modal={false}>
              <DropdownMenuTrigger asChild>
                <div className="hover:bg-slate-100/50 dark:hover:bg-slate-700/20 rounded-xl px-3 py-2 flex items-center space-x-2 cursor-pointer group transition-colors">
                  <Avatar className="h-8 w-8">
                    <AvatarImage src={session?.user?.image || ""} />
                    <AvatarFallback className="gradient-primary text-white text-sm font-bold">
                      {userInitials}
                    </AvatarFallback>
                  </Avatar>
                  <div className="hidden md:flex flex-col items-start">
                    <span className="text-sm font-medium text-slate-700 dark:text-white">{session?.user?.name}</span>
                    <span className="text-xs text-slate-500 dark:text-slate-400">Admin</span>
                  </div>
                  <ChevronDown className="h-3 w-3 text-slate-400 group-hover:text-slate-600 dark:text-slate-500 dark:group-hover:text-slate-300 transition-colors" />
                </div>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56 glass-thin rounded-2xl shadow-lg">
                <DropdownMenuLabel className="font-semibold">My Account</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild className="rounded-xl">
                  <Link href="/dashboard/settings">
                    <Settings className="mr-2 h-4 w-4 text-slate-600 dark:text-slate-300" />
                    Settings
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleSignOut} className="text-red-600 rounded-xl">
                  <LogOut className="mr-2 h-4 w-4 text-red-600" />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="lg:pl-80">
        {/* Page content */}
        <main className="relative min-h-screen px-8 py-8 max-w-7xl mx-auto">
          {children}
        </main>
      </div>
    </div>
  )
}