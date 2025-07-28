-- Add role column to player table
-- This migration adds a role column to support role-based access control

-- Add the role column with default value 'USER' (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'player' AND column_name = 'role') THEN
        ALTER TABLE player ADD COLUMN role VARCHAR(20) DEFAULT 'USER';
    END IF;
END $$;

-- Update all existing players to have USER role
UPDATE player SET role = 'USER' WHERE role IS NULL OR role = '';

-- Make the column NOT NULL after populating existing records
ALTER TABLE player ALTER COLUMN role SET NOT NULL;

-- Add index for performance on role-based queries (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_player_role') THEN
        CREATE INDEX idx_player_role ON player(role);
    END IF;
END $$;