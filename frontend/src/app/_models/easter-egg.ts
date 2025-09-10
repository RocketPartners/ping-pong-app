export enum EggType {
  COMMON = 'COMMON',
  UNCOMMON = 'UNCOMMON',
  RARE = 'RARE',
  EPIC = 'EPIC',
  LEGENDARY = 'LEGENDARY',
  MYTHICAL = 'MYTHICAL'
}

export interface EggTypeConfig {
  emoji: string;
  imageFilenames: string[];
  color: string;
  visualEffect: string;
  sizeMultiplier: number;
  displayName: string;
  rarityPercent: number;
}

export const EGG_TYPE_CONFIG: Record<EggType, EggTypeConfig> = {
  [EggType.COMMON]: {
    emoji: '🥚',
    imageFilenames: ['common-egg-01.png', 'common-egg-02.png', 'common-egg-03.png'],
    color: '#8BC34A',
    visualEffect: 'none',
    sizeMultiplier: 1.0,
    displayName: 'Common Egg',
    rarityPercent: 45.0
  },
  [EggType.UNCOMMON]: {
    emoji: '🥚',
    imageFilenames: ['uncommon-egg-01.png', 'uncommon-egg-02.png', 'uncommon-egg-03.png'],
    color: '#FF9800',
    visualEffect: 'glow',
    sizeMultiplier: 1.2,
    displayName: 'Uncommon Egg',
    rarityPercent: 30.0
  },
  [EggType.RARE]: {
    emoji: '🌟',
    imageFilenames: ['rare-egg-01.png', 'rare-egg-02.png', 'rare-egg-03.png'],
    color: '#9C27B0',
    visualEffect: 'sparkle',
    sizeMultiplier: 1.5,
    displayName: 'Rare Star Egg',
    rarityPercent: 15.0
  },
  [EggType.EPIC]: {
    emoji: '💎',
    imageFilenames: ['epic-egg-01.png', 'epic-egg-02.png', 'epic-egg-03.png'],
    color: '#3F51B5',
    visualEffect: 'pulse',
    sizeMultiplier: 1.8,
    displayName: 'Epic Diamond Egg',
    rarityPercent: 7.0
  },
  [EggType.LEGENDARY]: {
    emoji: '✨',
    imageFilenames: ['legendary-egg-01.png', 'legendary-egg-02.png', 'legendary-egg-03.png'],
    color: '#F44336',
    visualEffect: 'rainbow',
    sizeMultiplier: 2.0,
    displayName: 'Legendary Sparkle Egg',
    rarityPercent: 2.5
  },
  [EggType.MYTHICAL]: {
    emoji: '🔮',
    imageFilenames: ['mythical-egg-01.png', 'mythical-egg-02.png', 'mythical-egg-03.png'],
    color: '#E91E63',
    visualEffect: 'ethereal',
    sizeMultiplier: 2.5,
    displayName: 'Mythical Crystal Egg',
    rarityPercent: 0.5
  }
};

export interface EasterEgg {
  id: string;
  pageLocation: string;
  cssSelector: string;
  coordinates?: string;
  type: EggType;
  pointValue: number;
  isActive: boolean;
  spawnedAt: Date;
  foundByPlayerId?: string;
}

export interface EasterEggStats {
  playerId: string;
  totalEggsFound: number;
  totalPointsEarned: number;
  lastEggFound?: Date;
  commonEggsFound: number;
  uncommonEggsFound: number;
  rareEggsFound: number;
  epicEggsFound: number;
  legendaryEggsFound: number;
  mythicalEggsFound: number;
  longestStreak: number;
  currentStreak: number;
  averageTimeToFindMinutes: number;
  fastestFindSeconds: number;
}

export interface EggHunterLeaderboardDto {
  playerId: string;
  playerName: string;
  username: string;
  totalEggsFound: number;
  totalPoints: number;
  rank: number;
  lastEggFound?: Date;
  firstEggFound?: Date;
  commonEggsFound: number;
  uncommonEggsFound: number;
  rareEggsFound: number;
  epicEggsFound: number;
  legendaryEggsFound: number;
  mythicalEggsFound: number;
  longestStreak: number;
  favoriteHuntingPage?: string;
  specialBadge?: string;
  badgeEmoji?: string;
}

export interface RecentEggFindDto {
  playerName: string;
  eggType: EggType;
  pointsEarned: number;
  foundAt: Date;
  timeToFindMinutes: number;
}

export enum ClaimReason {
  SUCCESS = 'SUCCESS',
  EGG_NOT_FOUND = 'EGG_NOT_FOUND',
  EGG_ALREADY_CLAIMED = 'EGG_ALREADY_CLAIMED',
  EGG_EXPIRED = 'EGG_EXPIRED',
  PLAYER_NOT_FOUND = 'PLAYER_NOT_FOUND',
  ALREADY_CLAIMED_BY_PLAYER = 'ALREADY_CLAIMED_BY_PLAYER',
  SERVER_ERROR = 'SERVER_ERROR'
}

export interface EggClaimResult {
  success: boolean;
  reason: ClaimReason;
  message: string;
  pointsEarned?: number;
  newTotal?: number;
  eggType?: EggType;
  achievementsUnlocked?: string[];
}