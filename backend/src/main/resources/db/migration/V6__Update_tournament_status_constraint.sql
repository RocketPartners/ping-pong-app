-- Update tournament status constraint to include all enum values
-- Drop the existing constraint
ALTER TABLE tournament DROP CONSTRAINT IF EXISTS tournament_status_check;

-- Add the updated constraint with all TournamentStatus enum values
ALTER TABLE tournament ADD CONSTRAINT tournament_status_check 
CHECK (status::text = ANY (ARRAY[
    'CREATED'::character varying,
    'READY_TO_START'::character varying,
    'IN_PROGRESS'::character varying,
    'ROUND_COMPLETE'::character varying,
    'COMPLETED'::character varying,
    'CANCELLED'::character varying
]::text[]));

-- Also update seeding_method constraint to include MANUAL
ALTER TABLE tournament DROP CONSTRAINT IF EXISTS tournament_seeding_method_check;
ALTER TABLE tournament ADD CONSTRAINT tournament_seeding_method_check 
CHECK (seeding_method::text = ANY (ARRAY[
    'RATING_BASED'::character varying,
    'RANDOM'::character varying,
    'MANUAL'::character varying
]::text[]));