// Test brackets-manager integration
import { BracketsManager } from 'brackets-manager';
import { JsonDatabase } from 'brackets-json-db';

console.log('=== Brackets Manager API Test ===');

async function testBracketsManager() {
  try {
    // Initialize the manager
    const storage = new JsonDatabase();
    const manager = new BracketsManager(storage);
    
    console.log('✅ BracketsManager initialized successfully');
    console.log('Manager methods:', Object.getOwnPropertyNames(manager));
    console.log('Manager.create methods:', Object.getOwnPropertyNames(manager.create));
    
    // Test creating a tournament
    const participants = ['Alice', 'Bob', 'Charlie', 'David'];
    
    console.log('\n=== Creating Tournament ===');
    
    // Create a stage (this is the main method)
    const stage = await manager.create.stage({
      name: 'Test Tournament',
      tournamentId: 1,
      type: 'single_elimination',
      seeding: participants,
      settings: {
        seedOrdering: ['natural']
      }
    });
    
    console.log('✅ Stage created:', stage);
    
    // Get tournament data
    console.log('\n=== Getting Tournament Data ===');
    const data = await manager.get.stageData(stage.id);
    console.log('Stage data structure:', Object.keys(data));
    console.log('Participants:', data.participant);
    console.log('Matches:', data.match);
    console.log('Stage:', data.stage);
    
    // Test updating a match result
    if (data.match && data.match.length > 0) {
      console.log('\n=== Updating Match Result ===');
      const firstMatch = data.match[0];
      console.log('First match:', firstMatch);
      
      // Update match result
      await manager.update.match({
        id: firstMatch.id,
        opponent1: { score: 2, result: 'win' },
        opponent2: { score: 1, result: 'loss' }
      });
      
      console.log('✅ Match result updated');
      
      // Get updated data
      const updatedData = await manager.get.stageData(stage.id);
      console.log('Updated first match:', updatedData.match[0]);
    }
    
    console.log('\n=== SUCCESS: All tests passed! ===');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error('Stack:', error.stack);
  }
}

testBracketsManager();