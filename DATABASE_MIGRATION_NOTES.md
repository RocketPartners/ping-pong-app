# Database Migration Notes for Tournament Feature

This document outlines all database changes made during tournament feature development that need to be applied to production environments.

## Summary
We had to clean up old tournament data and fix schema mismatches between the database and our updated Tournament/TournamentPlayer models.

## Issues Encountered
1. **Schema Mismatch**: The `enable_reseeding` column in the tournament table was nullable but our Tournament model expected a primitive boolean (non-null)
2. **Foreign Key Constraints**: Complex cascade of tournament-related tables prevented simple deletion
3. **Default Values Missing**: Several boolean columns lacked proper default values
4. **Old Test Data**: Legacy tournament data with incompatible structure

## Database Changes Applied

### 1. Data Cleanup (Development Only - DO NOT RUN IN PRODUCTION WITHOUT BACKUP)

```sql
-- WARNING: This deletes all tournament data - only run if acceptable
-- Disable foreign key checks temporarily
SET session_replication_role = replica;

-- Clear all tournament related data in dependency order
DELETE FROM tournament_match_loser;
DELETE FROM tournament_match_team1;
DELETE FROM tournament_match_team2;
DELETE FROM tournament_match_winner;
DELETE FROM tournament_match;
DELETE FROM tournament_player;
DELETE FROM tournament_round;
DELETE FROM tournament;

-- Re-enable foreign key checks
SET session_replication_role = DEFAULT;
```

### 2. Schema Fixes (SAFE FOR PRODUCTION)

```sql
-- Fix boolean columns to have proper defaults and NOT NULL constraints
ALTER TABLE tournament ALTER COLUMN enable_reseeding SET DEFAULT true;
ALTER TABLE tournament ALTER COLUMN enable_reseeding SET NOT NULL;

ALTER TABLE tournament ALTER COLUMN round_ready SET DEFAULT false;  
ALTER TABLE tournament ALTER COLUMN round_ready SET NOT NULL;

ALTER TABLE tournament ALTER COLUMN is_public SET DEFAULT true;
ALTER TABLE tournament ALTER COLUMN is_public SET NOT NULL;

-- Set default for current_round
ALTER TABLE tournament ALTER COLUMN current_round SET DEFAULT 0;
```

### 3. Verification Queries

```sql
-- Verify schema changes
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'tournament' 
  AND column_name IN ('enable_reseeding', 'round_ready', 'is_public', 'current_round')
ORDER BY column_name;

-- Check tournament table structure
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'tournament' 
ORDER BY ordinal_position;

-- Verify data cleanup (should all return 0)
SELECT 'tournament' as table_name, COUNT(*) as count FROM tournament
UNION ALL
SELECT 'tournament_player', COUNT(*) FROM tournament_player
UNION ALL
SELECT 'tournament_match', COUNT(*) FROM tournament_match
UNION ALL
SELECT 'tournament_round', COUNT(*) FROM tournament_round;
```

## Production Migration Steps

### Pre-Migration Checklist
- [ ] Create full database backup
- [ ] Verify backup can be restored
- [ ] Schedule maintenance window
- [ ] Notify users of potential downtime

### Migration Steps for Production

1. **Backup Current State**
   ```bash
   pg_dump -h [host] -U [username] -d [database] > tournament_migration_backup.sql
   ```

2. **Update Existing Data (if tournament data exists in prod)**
   ```sql
   -- Update NULL values to proper defaults BEFORE applying constraints
   UPDATE tournament SET enable_reseeding = true WHERE enable_reseeding IS NULL;
   UPDATE tournament SET round_ready = false WHERE round_ready IS NULL;
   UPDATE tournament SET is_public = true WHERE is_public IS NULL;
   UPDATE tournament SET current_round = 0 WHERE current_round IS NULL;
   ```

3. **Apply Schema Changes**
   ```sql
   -- Apply the schema fixes (same as above)
   ALTER TABLE tournament ALTER COLUMN enable_reseeding SET DEFAULT true;
   ALTER TABLE tournament ALTER COLUMN enable_reseeding SET NOT NULL;
   ALTER TABLE tournament ALTER COLUMN round_ready SET DEFAULT false;  
   ALTER TABLE tournament ALTER COLUMN round_ready SET NOT NULL;
   ALTER TABLE tournament ALTER COLUMN is_public SET DEFAULT true;
   ALTER TABLE tournament ALTER COLUMN is_public SET NOT NULL;
   ALTER TABLE tournament ALTER COLUMN current_round SET DEFAULT 0;
   ```

4. **Verify Migration**
   ```sql
   -- Run verification queries (see above)
   ```

5. **Test Application Startup**
   - Deploy updated application code
   - Verify no database-related errors in logs
   - Test tournament list endpoint: `GET /api/tournaments`

### Rollback Plan
If issues occur:
1. Stop application
2. Restore from backup:
   ```bash
   psql -h [host] -U [username] -d [database] < tournament_migration_backup.sql
   ```
3. Deploy previous application version

## Model Changes Made

### Tournament Model
- Added proper defaults for boolean fields
- Ensured all primitive boolean fields are non-nullable
- Updated builder pattern usage in TournamentController

### TournamentPlayer Model
- Uses UUID for all ID fields
- Has proper foreign key relationships
- Eliminated deprecated setter methods

### TournamentController Changes
- Updated to use UUID instead of Long for all IDs
- Fixed repository method calls to match actual signatures
- Simplified bracket data handling
- Added proper error handling and logging

## Repository Method Updates

### TournamentPlayerRepository
- Using `findByTournament_Id(UUID id)` instead of `findByTournamentId(Long id)`
- All methods now use UUID parameters

### TournamentRepository  
- Standard JPA repository methods work correctly
- Using `findById(UUID id)` 

## Environment Variables / Configuration
No configuration changes required - all changes are code and database schema.

## Testing Checklist Post-Migration
- [ ] Tournament list loads without errors
- [ ] Tournament creation works
- [ ] Tournament details page loads  
- [ ] Tournament participants display correctly
- [ ] No null pointer exceptions in logs
- [ ] Frontend bracket component receives data correctly

## Files Modified
- `backend/src/main/java/com/example/javapingpongelo/controllers/TournamentController.java` - Complete rewrite
- `frontend/src/app/_models/tournament-new.ts` - Fixed enum values to match backend
- `frontend/src/app/tournament/tournament-create/tournament-create.component.ts` - Updated to use correct enums and added seeding method
- `frontend/src/app/tournament/tournament-create/tournament-create.component.html` - Added seeding method selection
- Frontend tournament services - API integration updates

## Additional Fixes Applied

### API Endpoint Issues
- **Added missing `/bracket` endpoint**: Frontend was calling `GET /api/tournaments/{id}/bracket` which didn't exist
- **Fixed enum value mismatches**: TournamentType values now match between frontend and backend
- **Added seeding method selection**: Tournament creation form now includes seeding method options

### Frontend Enum Fixes
```typescript
// Fixed TournamentType enum values
export enum TournamentType {
  SINGLE_ELIMINATION = 'SINGLE_ELIMINATION',  // Was 'single_elimination'
  DOUBLE_ELIMINATION = 'DOUBLE_ELIMINATION',  // Was 'double_elimination' 
  ROUND_ROBIN = 'ROUND_ROBIN'                // Added
}

// Added proper seeding method enum
export enum SeedingMethod {
  RATING_BASED = 'RATING_BASED',
  RANDOM = 'RANDOM', 
  MANUAL = 'MANUAL'
}
```

## Notes
- The Tournament engine and bracket generation logic is still TODO - this migration focuses on basic CRUD operations
- The `bracketData` field is currently returned as `null` in the API response
- Full tournament bracket functionality will be implemented in subsequent phases

---
**Created**: 2025-08-06  
**Last Updated**: 2025-08-06  
**Environment**: Development (Local PostgreSQL)