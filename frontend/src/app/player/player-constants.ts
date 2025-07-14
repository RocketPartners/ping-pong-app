import {Player} from '../_models/models';
import {PlayerStyle} from '../_models/player-style';

/**
 * Constants used across player-related components
 */
export const PLAYER_STYLE_COLOR_MAP = {
  [PlayerStyle.SPIN]: '#81d4fa',        // Sky Blue
  [PlayerStyle.POWER]: '#ef9a9a',       // Soft Red
  [PlayerStyle.CREATIVE]: '#ce93d8',    // Lilac
  [PlayerStyle.AGGRESSIVE]: '#ffab91',  // Light Orange
  [PlayerStyle.RESILIENT]: '#a5d6a7',   // Mint Green
  [PlayerStyle.ACE_MASTER]: '#fff176',  // Pastel Yellow
  [PlayerStyle.RALLY_KING]: '#bcaaa4',  // Light Brown
  [PlayerStyle.TACTICIAN]: '#b0bec5',   // Cool Grey
  [PlayerStyle.SPORTSMANSHIP]: '#dcedc8', // Pale Lime
  [PlayerStyle.AURA]: '#b39ddb'         // Lavender Mist
};


export const RADAR_STYLE_ORDER: PlayerStyle[] = [
  PlayerStyle.POWER,
  PlayerStyle.SPIN,
  PlayerStyle.CREATIVE,
  PlayerStyle.AGGRESSIVE,
  PlayerStyle.RESILIENT,
  PlayerStyle.TACTICIAN,
  PlayerStyle.RALLY_KING,
  PlayerStyle.ACE_MASTER,
  PlayerStyle.AURA, // Add or remove based on your PlayerStyle enum
  PlayerStyle.SPORTSMANSHIP,
];

export const PLAYER_STYLE_DESCRIPTIONS: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: 'Ability to create and handle spin effectively',
  [PlayerStyle.POWER]: 'Powerful shots that are difficult to return',
  [PlayerStyle.CREATIVE]: 'Unpredictable and creative play style',
  [PlayerStyle.AGGRESSIVE]: 'Aggressive approach that puts pressure on opponents',
  [PlayerStyle.RESILIENT]: 'Mental toughness and ability to recover from setbacks',
  [PlayerStyle.ACE_MASTER]: 'Excellent serving technique',
  [PlayerStyle.RALLY_KING]: 'Ability to maintain long rallies',
  [PlayerStyle.TACTICIAN]: 'Strategic thinking and game management',
  [PlayerStyle.SPORTSMANSHIP]: 'Fair play and positive attitude',
  [PlayerStyle.AURA]: 'Presence and ability to intimidate opponents'
};


export const STYLE_RATING_LEVELS = {
  BEGINNER: 0,
  NOVICE: 25,
  INTERMEDIATE: 50,
  ADVANCED: 75,
  EXPERT: 90
};

export const PLAYER_STYLE_RECOMMENDATIONS = {
  [PlayerStyle.SPIN]: 'Focus on developing more complex spin variations and practicing deceptive serves.',
  [PlayerStyle.POWER]: 'Work on your footwork to generate more power from your core and lower body.',
  [PlayerStyle.CREATIVE]: 'Continue mixing up your shots and developing unexpected strategies to keep opponents guessing.',
  [PlayerStyle.AGGRESSIVE]: 'Practice your attack timing and shot placement to maximize the effectiveness of your offensive play.',
  [PlayerStyle.RESILIENT]: 'Develop your ability to transition from defense to attack with strategic counter-hits.',
  [PlayerStyle.ACE_MASTER]: 'Refine your serve accuracy and work on developing more spin variations.',
  [PlayerStyle.RALLY_KING]: 'Work on maintaining concentration during extended rallies and identifying the right moment to attack.',
  [PlayerStyle.TACTICIAN]: 'Study your opponents more closely and develop specialized strategies for different play styles.'
};

export const RATING_THRESHOLDS = {
  BEGINNER: 0,
  NOVICE: 1000,
  INTERMEDIATE: 1500,
  ADVANCED: 1800,
  EXPERT: 2000
};

export const WIN_RATE_THRESHOLDS = {
  STRUGGLING: 0,
  POOR: 30,
  AVERAGE: 45,
  GOOD: 55,
  EXCELLENT: 70
};

/**
 * Creates a mock player for testing and development
 */
export function createMockPlayer(): Player {
  return {
    playerId: 'mock-id-123',
    firstName: 'John',
    lastName: 'Doe',
    username: 'pingpong_pro',
    email: 'john.doe@example.com',
    password: '',
    matchingPassword: '',
    birthday: new Date(1990, 1, 1),
    singlesRankedRating: 1750,
    doublesRankedRating: 1820,
    singlesNormalRating: 1700,
    doublesNormalRating: 1780,
    singlesRankedWins: 45,
    singlesRankedLoses: 20,
    singlesNormalWins: 30,
    singlesNormalLoses: 15,
    doublesRankedWins: 38,
    doublesRankedLoses: 17,
    doublesNormalWins: 25,
    doublesNormalLoses: 10,
    matchHistory: [],
    gameHistory: [],
    styleRatings: [
      {styleType: PlayerStyle.SPIN, rating: 75},
      {styleType: PlayerStyle.RALLY_KING, rating: 80},
      {styleType: PlayerStyle.TACTICIAN, rating: 65},
      {styleType: PlayerStyle.POWER, rating: 50},
      {styleType: PlayerStyle.CREATIVE, rating: 45},
      {styleType: PlayerStyle.AGGRESSIVE, rating: 40},
      {styleType: PlayerStyle.RESILIENT, rating: 60},
      {styleType: PlayerStyle.ACE_MASTER, rating: 55}
    ],
    created: new Date(2023, 1, 15),
    updated: new Date(2023, 9, 30)
  } as unknown as Player;
}

export function getStyleDescription(style: PlayerStyle): string {
  const descriptions: Record<PlayerStyle, string> = {
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
  return descriptions[style] || 'Unknown style description.';
}

export function getStyleRecommendation(styleType: PlayerStyle): string {
  const recommendations: Record<PlayerStyle, string> = {
    [PlayerStyle.SPIN]: 'Right now your shots are flatter than week-old soda. Start working on brushing up the back of the ball—topspin and slice aren’t just for show, they open up angles and add control.',
    [PlayerStyle.POWER]: 'Your shots aren’t exactly striking fear yet. Time to hit the gym *and* the court—build strength, improve timing, and stop babying the ball when you need to swing big.',
    [PlayerStyle.CREATIVE]: 'If your game is feeling a little... predictable, it probably is. Experiment more in practice—mix up pace, use drop shots, change your patterns. Creativity can be trained, not just born.',
    [PlayerStyle.AGGRESSIVE]: 'You play like you’re allergic to finishing points. Start stepping inside the baseline, take balls early, and learn to go for winners. Being aggressive doesn’t mean reckless—it means *intentional* pressure.',
    [PlayerStyle.RESILIENT]: 'You crumble the second a rally goes past five shots. Build mental toughness and train recovery footwork. Being resilient means staying calm when things get ugly, not hoping they don’t.',
    [PlayerStyle.ACE_MASTER]: 'Your serve isn’t a weapon—it’s a warning shot. Time to fix your toss, add variety, and learn to hit spots. A strong serve doesn’t have to be 130 mph, but it can’t be a gift either.',
    [PlayerStyle.RALLY_KING]: 'You run out of patience after three shots. Learn to grind—consistency is a skill, not a punishment. Rally tolerance wins matches when the flashy stuff breaks down.',
    [PlayerStyle.TACTICIAN]: 'You’re playing checkers while others play chess. Start studying match footage, think about shot patterns, and stop just reacting. A smarter game beats a stronger one—if you actually think.',
    [PlayerStyle.SPORTSMANSHIP]: 'Your on-court attitude could use a tune-up. Respect opponents, own your mistakes, and quit the excuses. Real competitors bring character, not just complaints.',
    [PlayerStyle.AURA]: 'Right now your presence is more “lost freshman” than “intimidating pro.” Confidence is a skill—work on body language, stay composed, and project belief even when you’re struggling.'
  };
  return recommendations[styleType] || 'Identify where your game is weakest and start attacking that weakness in training—no shortcuts, no ego.';
}


