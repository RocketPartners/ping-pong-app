-- V2__Create_challenge_table.sql
-- Create challenge table for the interactive challenge system

CREATE TABLE challenge (
    challenge_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenger_id UUID NOT NULL,
    challenged_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    is_ranked BOOLEAN NOT NULL DEFAULT false,
    is_singles BOOLEAN NOT NULL DEFAULT true,
    slack_channel_id VARCHAR(50),
    challenger_slack_id VARCHAR(50),
    challenged_slack_id VARCHAR(50),
    slack_message_ts VARCHAR(50),
    decline_reason TEXT,
    completed_game_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_challenger FOREIGN KEY (challenger_id) REFERENCES player(player_id) ON DELETE CASCADE,
    CONSTRAINT fk_challenged FOREIGN KEY (challenged_id) REFERENCES player(player_id) ON DELETE CASCADE,
    CONSTRAINT fk_completed_game FOREIGN KEY (completed_game_id) REFERENCES game(game_id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_challenge_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'COMPLETED')),
    CONSTRAINT chk_different_players CHECK (challenger_id != challenged_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_challenge_challenger_id ON challenge(challenger_id);
CREATE INDEX idx_challenge_challenged_id ON challenge(challenged_id);
CREATE INDEX idx_challenge_status ON challenge(status);
CREATE INDEX idx_challenge_created_at ON challenge(created_at);
CREATE INDEX idx_challenge_expires_at ON challenge(expires_at);

-- Create composite index for finding challenges between players
CREATE INDEX idx_challenge_players ON challenge(challenger_id, challenged_id);