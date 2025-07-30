-- V3__Add_slack_message_ts_to_challenge.sql
-- Add slack_message_ts column to challenge table for tracking Slack message timestamps
-- This migration is conditional to avoid errors if column already exists

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'challenge' 
        AND column_name = 'slack_message_ts'
    ) THEN
        ALTER TABLE challenge ADD COLUMN slack_message_ts VARCHAR(50);
    END IF;
END $$;