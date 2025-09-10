-- Fix for production database: Add missing easter_egg_hunting_enabled column
-- This script addresses the error: column p1_0.easter_egg_hunting_enabled does not exist

-- Add missing columns to player table if they don't exist
DO $$
BEGIN
    -- Check and add easter_egg_hunting_enabled column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'player' AND column_name = 'easter_egg_hunting_enabled') THEN
        ALTER TABLE player ADD COLUMN easter_egg_hunting_enabled BOOLEAN NOT NULL DEFAULT true;
        RAISE NOTICE 'Added easter_egg_hunting_enabled column to player table';
    ELSE
        RAISE NOTICE 'easter_egg_hunting_enabled column already exists';
    END IF;

    -- Check and add easter_egg_points column (if missing)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'player' AND column_name = 'easter_egg_points') THEN
        ALTER TABLE player ADD COLUMN easter_egg_points INTEGER NOT NULL DEFAULT 0;
        RAISE NOTICE 'Added easter_egg_points column to player table';
    ELSE
        RAISE NOTICE 'easter_egg_points column already exists';
    END IF;

    -- Check and add total_eggs_found column (if missing)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'player' AND column_name = 'total_eggs_found') THEN
        ALTER TABLE player ADD COLUMN total_eggs_found INTEGER NOT NULL DEFAULT 0;
        RAISE NOTICE 'Added total_eggs_found column to player table';
    ELSE
        RAISE NOTICE 'total_eggs_found column already exists';
    END IF;

    -- Check and add last_egg_found column (if missing)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'player' AND column_name = 'last_egg_found') THEN
        ALTER TABLE player ADD COLUMN last_egg_found TIMESTAMP;
        RAISE NOTICE 'Added last_egg_found column to player table';
    ELSE
        RAISE NOTICE 'last_egg_found column already exists';
    END IF;
END
$$;

-- Verify the columns were added successfully
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'player' 
AND column_name IN ('easter_egg_hunting_enabled', 'easter_egg_points', 'total_eggs_found', 'last_egg_found')
ORDER BY column_name;