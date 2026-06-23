import { apiClient } from "./apiClient";
import { ContextualAdOpportunity, MonetizationSettings } from "../types/ads";
import { mockAdOpportunities, defaultMonetizationSettings } from "../mocks/adsIntelligence.mock";

const MONETIZATION_SETTINGS_KEY = "signallq_monetization_settings_v1";

export class AdsIntelligenceService {
  private delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async getAdOpportunities(): Promise<ContextualAdOpportunity[]> {
    if (!apiClient.isMockEnabled()) return [];
    await this.delay(200);
    return mockAdOpportunities;
  }

  async getEligibilitySummary(): Promise<{ eligibleTotal: number } | null> {
    if (!apiClient.isMockEnabled()) return null;
    await this.delay(150);
    const total = mockAdOpportunities.reduce((s, o) => s + o.eligibleDiagnostics, 0);
    return { eligibleTotal: total };
  }

  async getMonetizationSettings(): Promise<MonetizationSettings> {
    await this.delay(100);
    const stored = localStorage.getItem(MONETIZATION_SETTINGS_KEY);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch (e) {
        console.error("Failed to parse monetization settings", e);
      }
    }
    return defaultMonetizationSettings;
  }

  async saveMonetizationSettings(settings: MonetizationSettings): Promise<MonetizationSettings> {
    await this.delay(100);
    localStorage.setItem(MONETIZATION_SETTINGS_KEY, JSON.stringify(settings));
    return settings;
  }
}

export const adsIntelligenceService = new AdsIntelligenceService();
