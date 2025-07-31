-- Add is_anonymous column to player table
ALTER TABLE player ADD COLUMN is_anonymous BOOLEAN DEFAULT FALSE;

-- Add index for anonymous player queries
CREATE INDEX idx_player_anonymous ON player(is_anonymous);