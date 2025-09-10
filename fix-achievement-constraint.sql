-- Fix achievement trigger constraint to allow EASTER_EGG_FOUND
-- This addresses the constraint violation preventing Easter egg achievements

-- First, find and drop the existing constraint
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Find the constraint name
    SELECT conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE t.relname = 'achievement_trigger'
    AND c.contype = 'c'  -- check constraint
    AND consrc LIKE '%trigger_type%' OR conbin::text LIKE '%trigger_type%';
    
    -- Drop the old constraint if it exists
    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE achievement_trigger DROP CONSTRAINT ' || constraint_name;
        RAISE NOTICE 'Dropped constraint: %', constraint_name;
    ELSE
        RAISE NOTICE 'No trigger_type constraint found to drop';
    END IF;
    
    -- Add the new constraint with all enum values
    ALTER TABLE achievement_trigger ADD CONSTRAINT achievement_trigger_trigger_type_check 
    CHECK (trigger_type IN (
        'GAME_COMPLETED',
        'RATING_UPDATED', 
        'STREAK_CHANGED',
        'MATCH_COMPLETED',
        'TOURNAMENT_EVENT',
        'EASTER_EGG_FOUND',
        'MANUAL_TRIGGER',
        'PERIODIC'
    ));
    
    RAISE NOTICE 'Added new constraint with EASTER_EGG_FOUND support';
END
$$;