// Test brackets-viewer integration
import { BracketsManager } from 'brackets-manager';
import { JsonDatabase } from 'brackets-json-db';

console.log('=== Brackets Viewer Test ===');

async function testBracketsViewer() {
  try {
    // Create tournament data first
    const storage = new JsonDatabase();
    const manager = new BracketsManager(storage);
    
    const participants = ['Alice', 'Bob', 'Charlie', 'David'];
    
    const stage = await manager.create.stage({
      name: 'Test Tournament',
      tournamentId: 1,
      type: 'single_elimination',
      seeding: participants,
      settings: {
        seedOrdering: ['natural']
      }
    });
    
    // Update one match result
    const data = await manager.get.stageData(stage.id);
    await manager.update.match({
      id: data.match[0].id,
      opponent1: { score: 2, result: 'win' },
      opponent2: { score: 1, result: 'loss' }
    });
    
    const updatedData = await manager.get.stageData(stage.id);
    
    console.log('✅ Tournament data prepared');
    console.log('Bracket data structure:', {
      stages: updatedData.stage.length,
      matches: updatedData.match.length,
      participants: updatedData.participant.length,
      groups: updatedData.group?.length || 0,
      rounds: updatedData.round?.length || 0
    });
    
    // Test brackets-viewer import
    console.log('\n=== Testing brackets-viewer import ===');
    
    try {
      const { BracketsViewer } = await import('brackets-viewer');
      console.log('✅ BracketsViewer imported successfully');
      console.log('BracketsViewer:', typeof BracketsViewer);
      
      // Test creating viewer instance (without DOM)
      const viewer = new BracketsViewer();
      console.log('✅ BracketsViewer instance created');
      console.log('Viewer methods:', Object.getOwnPropertyNames(Object.getPrototypeOf(viewer)));
      
    } catch (viewerError) {
      console.log('❌ BracketsViewer import failed:', viewerError.message);
      
      // Try alternative import methods
      try {
        const bracketsViewer = await import('brackets-viewer');
        console.log('Alternative import successful. Available exports:', Object.keys(bracketsViewer));
      } catch (altError) {
        console.log('❌ Alternative import also failed:', altError.message);
      }
    }
    
    // Output the exact data structure for Angular integration
    console.log('\n=== Data for Angular Integration ===');
    console.log('Final bracket data format:');
    console.log(JSON.stringify({
      stages: updatedData.stage,
      matches: updatedData.match,
      matchGames: updatedData.match_game || [],
      participants: updatedData.participant,
      groups: updatedData.group || [],
      rounds: updatedData.round || []
    }, null, 2));
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error('Stack:', error.stack);
  }
}

testBracketsViewer();