-- Add is_anonymous column to player audit table for Hibernate Envers
ALTER TABLE player_aud ADD COLUMN is_anonymous BOOLEAN DEFAULT FALSE;