"use client"

import { useState, useEffect } from "react"
import { motion } from "framer-motion"
import Link from "next/link"
import { 
  Bell, 
  CheckCircle2, 
  AlertTriangle, 
  Clock, 
  Globe, 
  Eye, 
  Trash2, 
  Check, 
  X,
  Search,
  Filter,
  MoreHorizontal
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu"

interface Notification {
  id: number
  type: 'success' | 'warning' | 'error' | 'info'
  icon: any
  title: string
  message: string
  domainName?: string
  domainId?: number
  time: string
  isRead: boolean
  createdAt: Date
}

// Mock notifications with domain information
const mockNotifications: Notification[] = [
  { 
    id: 1, 
    type: 'success', 
    icon: CheckCircle2, 
    title: 'Domain analysis completed', 
    message: 'needtheinfo.com analysis is complete with 92% compliance', 
    domainName: 'needtheinfo.com',
    domainId: 1,
    time: '5 minutes ago', 
    isRead: false,
    createdAt: new Date(Date.now() - 5 * 60 * 1000)
  },
  { 
    id: 2, 
    type: 'warning', 
    icon: AlertTriangle, 
    title: 'Compliance violations found', 
    message: '3 violations detected in pricefinderpro.com ads requiring immediate attention', 
    domainName: 'pricefinderpro.com',
    domainId: 2,
    time: '2 hours ago', 
    isRead: false,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000)
  },
  { 
    id: 3, 
    type: 'info', 
    icon: Clock, 
    title: 'Scheduled scan started', 
    message: 'Daily compliance check initiated for livelocally.net and will complete within 30 minutes', 
    domainName: 'livelocally.net',
    domainId: 3,
    time: '4 hours ago', 
    isRead: false,
    createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000)
  },
  { 
    id: 4, 
    type: 'success', 
    icon: Globe, 
    title: 'New domain added', 
    message: 'bestoptions.net has been successfully added to monitoring with initial compliance check scheduled', 
    domainName: 'bestoptions.net',
    domainId: 4,
    time: '1 day ago', 
    isRead: true,
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000)
  },
  { 
    id: 5, 
    type: 'info', 
    icon: Eye, 
    title: 'Weekly report ready', 
    message: 'Your compliance summary for this week is available with detailed insights and recommendations', 
    time: '2 days ago', 
    isRead: true,
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000)
  },
  { 
    id: 6, 
    type: 'error', 
    icon: AlertTriangle, 
    title: 'Scan failed', 
    message: 'Unable to complete scan for example.com due to network connectivity issues', 
    domainName: 'example.com',
    domainId: 5,
    time: '3 days ago', 
    isRead: true,
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000)
  }
]

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>(mockNotifications)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [filterType, setFilterType] = useState<string>("all")
  const [showUnreadOnly, setShowUnreadOnly] = useState(false)

  const unreadCount = notifications.filter(n => !n.isRead).length
  
  // Filter notifications based on search and filters
  const filteredNotifications = notifications.filter(notification => {
    const matchesSearch = notification.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         notification.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (notification.domainName && notification.domainName.toLowerCase().includes(searchTerm.toLowerCase()))
    
    const matchesType = filterType === "all" || notification.type === filterType
    const matchesReadStatus = !showUnreadOnly || !notification.isRead
    
    return matchesSearch && matchesType && matchesReadStatus
  })

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedIds(filteredNotifications.map(n => n.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectNotification = (id: number, checked: boolean) => {
    if (checked) {
      setSelectedIds(prev => [...prev, id])
    } else {
      setSelectedIds(prev => prev.filter(nId => nId !== id))
    }
  }

  const markAsRead = (ids: number[]) => {
    setNotifications(prev => prev.map(n => 
      ids.includes(n.id) ? { ...n, isRead: true } : n
    ))
  }

  const markAsUnread = (ids: number[]) => {
    setNotifications(prev => prev.map(n => 
      ids.includes(n.id) ? { ...n, isRead: false } : n
    ))
  }

  const deleteNotifications = (ids: number[]) => {
    setNotifications(prev => prev.filter(n => !ids.includes(n.id)))
    setSelectedIds([])
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

  const getBadgeColor = (type: string) => {
    switch (type) {
      case 'success':
        return 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
      case 'warning':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400'
      case 'error':
        return 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400'
      case 'info':
      default:
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400'
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Notifications</h1>
          <p className="text-slate-600 dark:text-slate-400 mt-1">
            Manage your alerts and updates
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Badge variant="secondary" className="px-3 py-1">
            {unreadCount} unread
          </Badge>
          {unreadCount > 0 && (
            <Button onClick={markAllAsRead} variant="outline" size="sm">
              <Check className="h-4 w-4 mr-2" />
              Mark all read
            </Button>
          )}
        </div>
      </div>

      {/* Search and Filters */}
      <div className="flex items-center space-x-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            placeholder="Search notifications..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 glass-input"
          />
        </div>
        
        <DropdownMenu modal={false}>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm">
              <Filter className="h-4 w-4 mr-2" />
              {filterType === "all" ? "All Types" : filterType.charAt(0).toUpperCase() + filterType.slice(1)}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="glass-thin rounded-2xl">
            <DropdownMenuItem onClick={() => setFilterType("all")}>All Types</DropdownMenuItem>
            <DropdownMenuItem onClick={() => setFilterType("success")}>Success</DropdownMenuItem>
            <DropdownMenuItem onClick={() => setFilterType("warning")}>Warning</DropdownMenuItem>
            <DropdownMenuItem onClick={() => setFilterType("error")}>Error</DropdownMenuItem>
            <DropdownMenuItem onClick={() => setFilterType("info")}>Info</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <div className="flex items-center space-x-2">
          <Checkbox
            id="unread-only"
            checked={showUnreadOnly}
            onCheckedChange={(checked) => setShowUnreadOnly(checked === true)}
          />
          <label 
            htmlFor="unread-only" 
            className="text-sm text-slate-600 dark:text-slate-400 cursor-pointer"
          >
            Unread only
          </label>
        </div>
      </div>

      {/* Bulk Actions */}
      {selectedIds.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between p-4 bg-blue-50 dark:bg-blue-900/20 rounded-2xl"
        >
          <span className="text-sm font-medium text-blue-900 dark:text-blue-300">
            {selectedIds.length} notification{selectedIds.length > 1 ? 's' : ''} selected
          </span>
          <div className="flex items-center space-x-2">
            <Button
              size="sm"
              variant="outline"
              onClick={() => markAsRead(selectedIds)}
            >
              <Check className="h-4 w-4 mr-1" />
              Mark Read
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => markAsUnread(selectedIds)}
            >
              <X className="h-4 w-4 mr-1" />
              Mark Unread
            </Button>
            <Button
              size="sm"
              variant="destructive"
              onClick={() => deleteNotifications(selectedIds)}
            >
              <Trash2 className="h-4 w-4 mr-1" />
              Delete
            </Button>
          </div>
        </motion.div>
      )}

      {/* Notifications List */}
      <div className="space-y-3">
        {/* Select All */}
        {filteredNotifications.length > 0 && (
          <div className="flex items-center space-x-3 px-4 py-2">
            <Checkbox
              checked={selectedIds.length === filteredNotifications.length}
              onCheckedChange={handleSelectAll}
            />
            <span className="text-sm text-slate-600 dark:text-slate-400">
              Select all ({filteredNotifications.length})
            </span>
          </div>
        )}

        {filteredNotifications.length === 0 ? (
          <div className="text-center py-16">
            <Bell className="h-16 w-16 mx-auto mb-6 text-slate-400" />
            <h3 className="text-xl font-semibold text-slate-900 dark:text-white mb-2">
              No notifications found
            </h3>
            <p className="text-slate-600 dark:text-slate-400">
              {searchTerm || filterType !== "all" || showUnreadOnly
                ? "Try adjusting your search or filters"
                : "You're all caught up! New notifications will appear here."}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filteredNotifications.map((notification) => (
              <motion.div
                key={notification.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className={`p-4 rounded-2xl border transition-all hover:shadow-md ${
                  !notification.isRead 
                    ? 'bg-blue-50/50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800' 
                    : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700'
                }`}
              >
                <div className="flex items-start space-x-4">
                  <Checkbox
                    checked={selectedIds.includes(notification.id)}
                    onCheckedChange={(checked) => handleSelectNotification(notification.id, checked as boolean)}
                  />
                  
                  <div className={`mt-0.5 ${getNotificationColor(notification.type)}`}>
                    <notification.icon className="h-5 w-5" />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2">
                          <h3 className="text-base font-semibold text-slate-900 dark:text-white">
                            {notification.title}
                          </h3>
                          <Badge className={`text-xs ${getBadgeColor(notification.type)}`}>
                            {notification.type}
                          </Badge>
                          {!notification.isRead && (
                            <div className="w-2 h-2 bg-blue-500 rounded-full" />
                          )}
                        </div>
                        
                        <p className="text-sm text-slate-600 dark:text-slate-400 mt-1 leading-relaxed">
                          {notification.message}
                        </p>
                        
                        <div className="flex items-center space-x-4 mt-3">
                          {notification.domainName && (
                            <Link
                              href={`/dashboard/domains/${notification.domainId}`}
                              className="text-sm font-medium text-primary hover:text-primary/80 transition-colors"
                            >
                              {notification.domainName}
                            </Link>
                          )}
                          <span className="text-xs text-slate-500">
                            {notification.time}
                          </span>
                        </div>
                      </div>
                      
                      <DropdownMenu modal={false}>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="glass-thin rounded-2xl">
                          <DropdownMenuItem
                            onClick={() => markAsRead([notification.id])}
                            disabled={notification.isRead}
                          >
                            <Check className="h-4 w-4 mr-2" />
                            Mark as read
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => markAsUnread([notification.id])}
                            disabled={!notification.isRead}
                          >
                            <X className="h-4 w-4 mr-2" />
                            Mark as unread
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            onClick={() => deleteNotifications([notification.id])}
                            className="text-red-600 dark:text-red-400"
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Delete
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
