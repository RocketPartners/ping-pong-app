export enum PlayerStyle {
  SPIN = "SPIN",
  POWER = "POWER",
  CREATIVE = "CREATIVE",
  AGGRESSIVE = "AGGRESSIVE",
  RESILIENT = "RESILIENT",
  ACE_MASTER = "ACE_MASTER",
  RALLY_KING = "RALLY_KING",
  TACTICIAN = "TACTICIAN",
  SPORTSMANSHIP = "SPORTSMANSHIP",
  AURA = "AURA"
}

export interface StyleRating {
  styleType: PlayerStyle;
  rating: number;
}

export interface AverageStyleRating {
  style: PlayerStyle;
  averageRating: number;
}

export interface TopStyleRating {
  style: PlayerStyle;
  rating: number;
  playerId: string;
  playerUsername: string;
  playerFullName: string;
}

// Constants for style rating presentation
export const STYLE_RATING = {
  MIN: 0,
  MAX: 100,
  DEFAULT: 50,
  NOVICE: 25,
  INTERMEDIATE: 50,
  ADVANCED: 75,
  EXPERT: 90
};

// Style name mappings
export const PLAYER_STYLE_NAMES: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: 'Spin',
  [PlayerStyle.POWER]: 'Power',
  [PlayerStyle.CREATIVE]: 'Creative',
  [PlayerStyle.AGGRESSIVE]: 'Aggressive',
  [PlayerStyle.RESILIENT]: 'Resilient',
  [PlayerStyle.ACE_MASTER]: 'Ace Master',
  [PlayerStyle.RALLY_KING]: 'Rally King',
  [PlayerStyle.TACTICIAN]: 'Tactician',
  [PlayerStyle.SPORTSMANSHIP]: 'Sportsmanship',
  [PlayerStyle.AURA]: 'Aura'
};

// Style color mappings
export const PLAYER_STYLE_COLORS: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: '#2196f3',        // Blue
  [PlayerStyle.POWER]: '#f44336',       // Red
  [PlayerStyle.CREATIVE]: '#9c27b0',    // Purple
  [PlayerStyle.AGGRESSIVE]: '#ff5722',  // Deep Orange
  [PlayerStyle.RESILIENT]: '#4caf50',   // Green
  [PlayerStyle.ACE_MASTER]: '#ffc107',  // Amber
  [PlayerStyle.RALLY_KING]: '#795548',  // Brown
  [PlayerStyle.TACTICIAN]: '#607d8b',   // Blue Grey
  [PlayerStyle.SPORTSMANSHIP]: '#8bc34a', // Light Green
  [PlayerStyle.AURA]: '#673ab7'         // Deep Purple
};

// Style descriptions
export const PLAYER_STYLE_DESCRIPTIONS: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: 'Masterful control of spin techniques that create challenging ball trajectories',
  [PlayerStyle.POWER]: 'Powerful shots that overpower opponents and create opportunities for winners',
  [PlayerStyle.CREATIVE]: 'Unpredictable shots and strategies that keep opponents guessing',
  [PlayerStyle.AGGRESSIVE]: 'Offensive play that dominates the game by taking control of rallies',
  [PlayerStyle.RESILIENT]: 'Exceptional defensive play and ability to return difficult shots under pressure',
  [PlayerStyle.ACE_MASTER]: 'Exceptional serving skills that create pressure and earn free points',
  [PlayerStyle.RALLY_KING]: 'Excels in extended rallies with consistent ground strokes and patience',
  [PlayerStyle.TACTICIAN]: 'Strategic gameplay that outsmarts opponents through careful shot selection',
  [PlayerStyle.SPORTSMANSHIP]: 'Demonstrates exemplary fair play, respect, and positive attitude during matches',
  [PlayerStyle.AURA]: 'Projects a commanding presence that influences match dynamics and opponent confidence'
};

// Style recommendations
export const PLAYER_STYLE_RECOMMENDATIONS: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: 'Focus on varying your spin techniques in practice. Try combining topspin with slice shots to keep opponents off balance.',
  [PlayerStyle.POWER]: 'Build on your power game by working on shot accuracy. Practice hitting powerful shots to specific court zones.',
  [PlayerStyle.CREATIVE]: 'Continue developing unexpected shot patterns. Mix drop shots with deep court placement to disrupt opponent rhythm.',
  [PlayerStyle.AGGRESSIVE]: 'Maintain your aggressive approach but work on shot selection. Look for opportunities to end points with winners.',
  [PlayerStyle.RESILIENT]: 'Your defensive skills are strong - now work on transitioning from defense to offense more quickly.',
  [PlayerStyle.ACE_MASTER]: 'Practice different serve placements to complement your powerful serves. Develop a reliable second serve.',
  [PlayerStyle.RALLY_KING]: 'Build on your rally consistency by adding more variety in shot depth and placement.',
  [PlayerStyle.TACTICIAN]: 'Continue analyzing opponent patterns. Practice specific tactics against different play styles.',
  [PlayerStyle.SPORTSMANSHIP]: 'Your excellent sportsmanship creates positive match environments. Use this quality to maintain focus during challenging situations.',
  [PlayerStyle.AURA]: 'Leverage your commanding presence by strategically using body language and court positioning to influence opponent decisions.'
};

// Helper functions for working with player styles

/**
 * Gets a human-readable label for a style rating level
 */
export function getStyleLevelLabel(rating: number): string {
  if (rating >= STYLE_RATING.EXPERT) return 'Expert';
  if (rating >= STYLE_RATING.ADVANCED) return 'Advanced';
  if (rating >= STYLE_RATING.INTERMEDIATE) return 'Intermediate';
  if (rating >= STYLE_RATING.NOVICE) return 'Novice';
  return 'Beginner';
}


/**
 * Gets the description for a player style
 */
export function getStyleDescription(styleType: PlayerStyle): string {
  return PLAYER_STYLE_DESCRIPTIONS[styleType] || 'Unknown style description';
}

/**
 * Gets the recommendation for a player style
 */
export function getStyleRecommendation(styleType: PlayerStyle): string {
  return PLAYER_STYLE_RECOMMENDATIONS[styleType] || 'Continue refining your technique to improve your overall game.';
}

/**
 * Gets the color for a player style
 */
export function getStyleColor(styleType: PlayerStyle): string {
  return PLAYER_STYLE_COLORS[styleType] || '#999999';
}

