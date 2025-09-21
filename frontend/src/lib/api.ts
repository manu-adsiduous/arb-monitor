const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

// Types for our API responses
export interface Domain {
  id: number;
  domainName: string;
  complianceScore: number | null;
  lastChecked: string | null;
  shareToken: string;
  status: string;
  processingStatus: string;
  processingMessage: string | null;
  racParameter: string | null;
  createdAt: string;
  updatedAt: string;
  activeAds: number;
  violations: number;
}

export interface DomainStats {
  totalDomains: number;
  averageCompliance: number;
  activeDomains: number;
  inactiveDomains: number;
}

export interface User {
  id: number;
  email: string;
  name: string;
  subscriptionStatus: string;
  createdAt: string;
  hasValidApiKey: boolean;
}

export interface UserSettings {
  userId: number;
  email: string;
  name: string;
  subscriptionStatus: string;
  metaApiKey: string | null;
  apiKeyValid: boolean | null;
  apiKeyLastVerified: string | null;
  emailNotifications: boolean;
  weeklyReports: boolean;
  criticalAlerts: boolean;
}

// API Error handling
export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

// Helper function to make API calls
async function apiCall<T>(
  endpoint: string,
  options: RequestInit = {},
  userId?: number
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  // Add existing headers
  if (options.headers) {
    Object.entries(options.headers).forEach(([key, value]) => {
      if (typeof value === 'string') {
        headers[key] = value;
      }
    });
  }

  // Add user ID header if provided (for development)
  if (userId) {
    headers['X-User-ID'] = userId.toString();
  }

  try {
    
    const response = await fetch(url, {
      ...options,
      headers,
    });


    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.error || errorMessage;
      } catch {
        // If we can't parse the error response, use the default message
      }
      throw new ApiError(response.status, errorMessage);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(0, 'Network error occurred');
  }
}

// Domain API functions
export const domainApi = {
  // Get domain statistics
  async getStats(userId: number): Promise<DomainStats> {
    return apiCall<DomainStats>('/api/domains/stats', {}, userId);
  },

  // Get all domains for user
  async getDomains(userId: number, search?: string): Promise<Domain[]> {
    const params = new URLSearchParams();
    if (search) {
      params.append('search', search);
    }
    
    const endpoint = `/api/domains${params.toString() ? `?${params.toString()}` : ''}`;
    const response = await apiCall<{ domains: Domain[]; totalPages: number; currentPage: number; totalElements: number }>(endpoint, {}, userId);
    return response.domains;
  },

  // Get domain by ID
  async getDomain(userId: number, domainId: number): Promise<Domain> {
    return apiCall<Domain>(`/api/domains/${domainId}`, {}, userId);
  },

  // Add new domain
  async addDomain(userId: number, domainName: string): Promise<Domain> {
    return apiCall<Domain>(
      '/api/domains',
      {
        method: 'POST',
        body: JSON.stringify({ domainName }),
      },
      userId
    );
  },

  // Update domain status
  async updateDomainStatus(
    userId: number,
    domainId: number,
    status: string
  ): Promise<Domain> {
    return apiCall<Domain>(
      `/api/domains/${domainId}/status`,
      {
        method: 'PUT',
        body: JSON.stringify({ status }),
      },
      userId
    );
  },

  // Delete domain
  async deleteDomain(userId: number, domainId: number): Promise<{ message: string }> {
    return apiCall<{ message: string }>(
      `/api/domains/${domainId}`,
      {
        method: 'DELETE',
      },
      userId
    );
  },

  // Regenerate share token
  async regenerateShareToken(
    userId: number,
    domainId: number
  ): Promise<{ shareToken: string; message: string }> {
    return apiCall<{ shareToken: string; message: string }>(
      `/api/domains/${domainId}/regenerate-token`,
      {
        method: 'POST',
      },
      userId
    );
  },
};

// User API functions
export const userApi = {
  // Get current user
  async getCurrentUser(userId: number): Promise<User> {
    return apiCall<User>('/api/users/me', {}, userId);
  },

  // Update user profile
  async updateProfile(userId: number, name: string): Promise<User> {
    return apiCall<User>(
      '/users/me',
      {
        method: 'PUT',
        body: JSON.stringify({ name }),
      },
      userId
    );
  },

  // Get user settings
  async getSettings(userId: number): Promise<UserSettings> {
    return apiCall<UserSettings>('/api/users/me/settings', {}, userId);
  },

  // Update user settings
  async updateSettings(userId: number, settings: Partial<UserSettings>): Promise<{ message: string }> {
    return apiCall<{ message: string }>(
      '/users/me/settings',
      {
        method: 'PUT',
        body: JSON.stringify(settings),
      },
      userId
    );
  },

  // Update Meta API key
  async updateMetaApiKey(userId: number, metaApiKey: string): Promise<{ message: string }> {
    return apiCall<{ message: string }>(
      '/api/users/me/meta-api-key',
      {
        method: 'PUT',
        body: JSON.stringify({ metaApiKey }),
      },
      userId
    );
  },

  // Validate Meta API key
  async validateApiKey(userId: number): Promise<{ valid: boolean; message: string }> {
    return apiCall<{ valid: boolean; message: string }>(
      '/api/users/me/validate-api-key',
      {
        method: 'POST',
      },
      userId
    );
  },
};

// Analysis API functions
export const analysisApi = {
  // Trigger ad analysis for a domain
  async analyzeDomain(userId: number, domainId: number): Promise<{ 
    success: boolean; 
    message: string; 
    processedAds: number;
    domainId: number;
  }> {
    return apiCall<{ 
      success: boolean; 
      message: string; 
      processedAds: number;
      domainId: number;
    }>(
      `/api/compliance/analyze/domain/${domainId}`,
      {
        method: 'POST',
      },
      userId
    );
  },

  // Get analysis status for a domain
  async getAnalysisStatus(userId: number, domainId: number): Promise<{
    domainId: number;
    domainName: string;
    lastChecked: string | null;
    complianceScore: number | null;
    status: string;
  }> {
    return apiCall<{
      domainId: number;
      domainName: string;
      lastChecked: string | null;
      complianceScore: number | null;
      status: string;
    }>(
      `/api/analysis/domains/${domainId}/status`,
      {},
      userId
    );
  },
};

// Health check
export const healthApi = {
  async check(): Promise<{ status: string }> {
    return apiCall<{ status: string }>('/actuator/health');
  },
};
