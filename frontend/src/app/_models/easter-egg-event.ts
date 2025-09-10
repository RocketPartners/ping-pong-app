import { EggType } from './easter-egg';

export interface EasterEggEvent {
  eventType: 'EGG_SPAWNED' | 'EGG_CLAIMED' | 'EGG_EXPIRED';
  eggId: string;
  pageLocation?: string;
  cssSelector?: string;
  coordinates?: string;
  eggType?: EggType;
  pointValue?: number;
  claimedByPlayer?: string;
  timestamp: number;
}