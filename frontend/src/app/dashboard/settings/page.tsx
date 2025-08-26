"use client";

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Toggle } from "@/components/ui/toggle";
import { 
  Settings, 
  CreditCard, 
  Save, 
  Eye, 
  EyeOff, 
  AlertCircle,
  CheckCircle2,
  ExternalLink,
  Info,
  DollarSign,
  Calendar,
  TrendingUp,
  Download,
  Check
} from "lucide-react";

export default function SettingsPage() {
  const [showApiKey, setShowApiKey] = useState(false);
  const [apiKey, setApiKey] = useState("");
  const [isApiKeyValid, setIsApiKeyValid] = useState<boolean | null>(null);
  const [isTestingKey, setIsTestingKey] = useState(false);
  
  // Notification settings state
  const [notificationSettings, setNotificationSettings] = useState({
    emailNotifications: true,
    weeklyReports: true,
    criticalAlerts: true
  });
  
  // Auto-save state
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');

  // Mock billing data
  const currentUsage = {
    adsScraped: 2847,
    baseFee: 50,
    adFee: 28.47, // 2847 ads * $10/1000
    total: 78.47,
    billingPeriod: "Nov 1 - Nov 30, 2024"
  };

  const recentCharges = [
    { date: "Nov 15, 2024", description: "1,245 ads scraped", amount: 12.45 },
    { date: "Nov 10, 2024", description: "892 ads scraped", amount: 8.92 },
    { date: "Nov 5, 2024", description: "710 ads scraped", amount: 7.10 },
  ];

  const handleTestApiKey = async () => {
    if (!apiKey) return;
    
    setIsTestingKey(true);
    // Simulate API key validation
    setTimeout(() => {
      setIsApiKeyValid(apiKey.length > 20); // Simple validation
      setIsTestingKey(false);
    }, 2000);
  };

  // Auto-save notification settings
  useEffect(() => {
    const saveSettings = async () => {
      setSaveStatus('saving');
      
      try {
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // Save to localStorage for persistence
        localStorage.setItem('notificationSettings', JSON.stringify(notificationSettings));
        
        setSaveStatus('saved');
        setTimeout(() => setSaveStatus('idle'), 1000);
      } catch (error) {
        setSaveStatus('error');
        setTimeout(() => setSaveStatus('idle'), 2000);
      }
    };

    // Debounce auto-save
    const timeoutId = setTimeout(saveSettings, 500);
    return () => clearTimeout(timeoutId);
  }, [notificationSettings]);

  // Load saved settings on mount
  useEffect(() => {
    const saved = localStorage.getItem('notificationSettings');
    if (saved) {
      try {
        setNotificationSettings(JSON.parse(saved));
      } catch (error) {
        console.error('Failed to load saved settings:', error);
      }
    }
  }, []);

  const handleNotificationChange = (key: keyof typeof notificationSettings, value: boolean) => {
    setNotificationSettings(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const handleSaveSettings = () => {
    // Save settings logic here
    console.log("Saving settings:", { apiKey });
  };

  return (
    <div className="py-6">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-8"
      >
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gradient mb-2">
              Settings
            </h1>
            <p className="text-slate-600 dark:text-slate-400 text-lg">
              Manage your account settings and billing
            </p>
          </div>
          <div className="flex items-center space-x-4 mt-4 md:mt-0">
            <Badge className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">
              Active Account
            </Badge>
          </div>
        </div>

        {/* Billing & Usage Section */}
        <div className="space-y-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-3 gradient-primary rounded-xl">
              <CreditCard className="h-6 w-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-900 dark:text-white">Billing & Usage</h2>
              <p className="text-slate-600 dark:text-slate-400">Track your usage and manage billing</p>
            </div>
          </div>

          {/* Usage Stats Row */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-blue-500/20 rounded-xl flex items-center justify-center">
                <TrendingUp className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Ads Scraped</p>
                <p className="text-2xl font-bold text-slate-900 dark:text-white">{currentUsage.adsScraped.toLocaleString()}</p>
              </div>
            </div>
            
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-green-500/20 rounded-xl flex items-center justify-center">
                <DollarSign className="h-6 w-6 text-green-600 dark:text-green-400" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Current Bill</p>
                <p className="text-2xl font-bold text-slate-900 dark:text-white">${currentUsage.total}</p>
              </div>
            </div>
            
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-purple-500/20 rounded-xl flex items-center justify-center">
                <Calendar className="h-6 w-6 text-purple-600 dark:text-purple-400" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Billing Period</p>
                <p className="text-sm font-semibold text-slate-900 dark:text-white">{currentUsage.billingPeriod}</p>
              </div>
            </div>
          </div>

          {/* Pricing Breakdown */}
          <div className="p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30">
            <h3 className="font-semibold text-slate-900 dark:text-white mb-4">Pricing Breakdown</h3>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-slate-600 dark:text-slate-400">Base subscription fee</span>
                <span className="font-medium text-slate-900 dark:text-white">${currentUsage.baseFee}.00</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-600 dark:text-slate-400">
                  {currentUsage.adsScraped.toLocaleString()} ads × $10/1000
                </span>
                <span className="font-medium text-slate-900 dark:text-white">${currentUsage.adFee}</span>
              </div>
              <div className="border-t border-slate-200/20 dark:border-slate-700/20 pt-3 mt-3">
                <div className="flex justify-between items-center font-semibold">
                  <span className="text-slate-900 dark:text-white">Total</span>
                  <span className="text-slate-900 dark:text-white">${currentUsage.total}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Recent Charges */}
          <div className="p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-slate-900 dark:text-white">Recent Charges</h3>
              <Button variant="outline" size="sm" className="flex items-center gap-2">
                <Download className="h-4 w-4" />
                Download Invoice
              </Button>
            </div>
            <div className="space-y-3">
              {recentCharges.map((charge, index) => (
                <div key={index} className="flex justify-between items-center py-2">
                  <div>
                    <p className="text-sm font-medium text-slate-900 dark:text-white">{charge.description}</p>
                    <p className="text-xs text-slate-600 dark:text-slate-400">{charge.date}</p>
                  </div>
                  <span className="text-sm font-medium text-slate-900 dark:text-white">${charge.amount}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Account Settings Section */}
        <div className="space-y-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-3 gradient-primary rounded-xl">
              <Settings className="h-6 w-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-900 dark:text-white">Account Settings</h2>
              <p className="text-slate-600 dark:text-slate-400">Manage your account preferences</p>
            </div>
          </div>

          {/* Account Info */}
          <div className="p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  Email
                </label>
                <input
                  type="email"
                  value="user@example.com"
                  disabled
                  className="w-full px-4 py-2 rounded-xl bg-slate-100/50 dark:bg-slate-800/50 text-slate-600 dark:text-slate-400 cursor-not-allowed border-0 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  Name
                </label>
                <input
                  type="text"
                  defaultValue="John Doe"
                  className="w-full px-4 py-2 rounded-xl bg-transparent border border-slate-200/50 dark:border-slate-700/50 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50"
                />
              </div>
            </div>
            
            <div className="flex items-center justify-between">
              <div>
                <h4 className="font-medium text-slate-900 dark:text-white">Subscription Status</h4>
                <p className="text-sm text-slate-600 dark:text-slate-400">Active - 5 domains monitored</p>
              </div>
              <Badge className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">
                Active
              </Badge>
            </div>
          </div>
        </div>

        {/* Notification Settings Section */}
        <div className="space-y-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-3 gradient-primary rounded-xl">
              <AlertCircle className="h-6 w-6 text-white" />
            </div>
            <div className="flex-1">
              <h2 className="text-xl font-bold text-slate-900 dark:text-white">Notification Settings</h2>
              <p className="text-slate-600 dark:text-slate-400">Configure when and how you receive notifications</p>
            </div>
            {/* Subtle auto-save status indicator */}
            {saveStatus === 'saving' && (
              <div className="text-xs text-slate-500 dark:text-slate-400">
                Saving...
              </div>
            )}
            {saveStatus === 'saved' && (
              <div className="text-xs text-green-600 dark:text-green-400">
                Saved
              </div>
            )}
            {saveStatus === 'error' && (
              <div className="text-xs text-red-600 dark:text-red-400">
                Error
              </div>
            )}
          </div>

          {/* Subtle auto-save note */}
          <div className="p-3 rounded-lg bg-slate-50/50 dark:bg-slate-800/20 border border-slate-200/30 dark:border-slate-700/30">
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-slate-500 dark:text-slate-400" />
              <p className="text-xs text-slate-600 dark:text-slate-400">Changes are automatically saved</p>
            </div>
          </div>

          {/* Notification Options */}
          <div className="space-y-4">
            <div className="flex items-center justify-between p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30 last:border-b-0">
              <div>
                <h4 className="font-medium text-slate-900 dark:text-white">Email Notifications</h4>
                <p className="text-sm text-slate-600 dark:text-slate-400">Receive email alerts for compliance violations</p>
              </div>
              <Toggle 
                checked={notificationSettings.emailNotifications}
                onChange={(checked) => handleNotificationChange('emailNotifications', checked)}
              />
            </div>
            
            <div className="flex items-center justify-between p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30 last:border-b-0">
              <div>
                <h4 className="font-medium text-slate-900 dark:text-white">Weekly Reports</h4>
                <p className="text-sm text-slate-600 dark:text-slate-400">Get weekly compliance summary reports</p>
              </div>
              <Toggle 
                checked={notificationSettings.weeklyReports}
                onChange={(checked) => handleNotificationChange('weeklyReports', checked)}
              />
            </div>
            
            <div className="flex items-center justify-between p-6 rounded-2xl hover:bg-slate-100/30 dark:hover:bg-slate-800/30 transition-colors border-b border-slate-200/30 dark:border-slate-700/30 last:border-b-0">
              <div>
                <h4 className="font-medium text-slate-900 dark:text-white">Critical Alerts</h4>
                <p className="text-sm text-slate-600 dark:text-slate-400">Immediate alerts for critical compliance issues</p>
              </div>
              <Toggle 
                checked={notificationSettings.criticalAlerts}
                onChange={(checked) => handleNotificationChange('criticalAlerts', checked)}
              />
            </div>
          </div>
        </div>

      </motion.div>
    </div>
  );
}
