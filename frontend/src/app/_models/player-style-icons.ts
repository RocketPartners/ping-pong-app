import { PlayerStyle } from './player-style';

// Style icon mappings - using Material Icons that best represent each style
export const PLAYER_STYLE_ICONS: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: 'sync',            // Rotating icon for spin
  [PlayerStyle.POWER]: 'bolt',           // Lightning bolt for power
  [PlayerStyle.CREATIVE]: 'auto_awesome', // Stars/sparkles for creative
  [PlayerStyle.AGGRESSIVE]: 'flash_on',   // Lightning for aggressive
  [PlayerStyle.RESILIENT]: 'shield',      // Shield for resilient/defensive
  [PlayerStyle.ACE_MASTER]: 'sports_tennis', // Tennis for ace master
  [PlayerStyle.RALLY_KING]: 'repeat',     // Repeating/loop icon for rally king
  [PlayerStyle.TACTICIAN]: 'psychology',  // Brain/strategy for tactician
  [PlayerStyle.SPORTSMANSHIP]: 'emoji_events', // Trophy/medal for sportsmanship
  [PlayerStyle.AURA]: 'local_fire_department' // Fire for aura/presence
};

// Alternative icons if needed
export const PLAYER_STYLE_ALT_ICONS: Record<PlayerStyle, string> = {
  [PlayerStyle.SPIN]: 'rotate_right',
  [PlayerStyle.POWER]: 'electric_bolt',
  [PlayerStyle.CREATIVE]: 'auto_fix_high',
  [PlayerStyle.AGGRESSIVE]: 'trending_up',
  [PlayerStyle.RESILIENT]: 'health_and_safety',
  [PlayerStyle.ACE_MASTER]: 'drag_handle',
  [PlayerStyle.RALLY_KING]: 'loop',
  [PlayerStyle.TACTICIAN]: 'lightbulb',
  [PlayerStyle.SPORTSMANSHIP]: 'handshake',
  [PlayerStyle.AURA]: 'whatshot'
};

/**
 * Gets the icon for a player style
 */
export function getStyleIcon(styleType: PlayerStyle): string {
  return PLAYER_STYLE_ICONS[styleType] || 'sports_tennis';
}