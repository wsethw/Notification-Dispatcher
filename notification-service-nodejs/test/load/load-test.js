const io = require('socket.io-client');
const axios = require('axios');

describe('Load Testing', () => {
  it('should handle 1000 concurrent connections', async () => {
    const NUM_CLIENTS = 1000;
    const connections = [];
    
    const startTime = Date.now();

    for (let i = 0; i < NUM_CLIENTS; i++) {
      const socket = io('http://localhost:3000');
      connections.push(
        new Promise((resolve) => {
          socket.on('connect', () => {
            socket.emit('subscribe', { userId: `load-test-user-${i}` });
            socket.on('subscribed', resolve);
          });
        })
      );
    }

    await Promise.all(connections);
    const elapsed = Date.now() - startTime;

    console.log(`Connected ${NUM_CLIENTS} clients in ${elapsed}ms`);
    expect(elapsed).toBeLessThan(30000); // Should complete in 30 seconds

    connections.forEach(c => c.disconnect?.());
  }, 60000);

  it('should process 10k messages without memory leak', async () => {
    const socket = io('http://localhost:3000');
    let messageCount = 0;
    
    const memBefore = process.memoryUsage().heapUsed;

    socket.on('notification', () => {
      messageCount++;
      if (messageCount === 10000) {
        const memAfter = process.memoryUsage().heapUsed;
        const memIncrease = (memAfter - memBefore) / 1024 / 1024; // MB
        
        console.log(`Memory increase: ${memIncrease.toFixed(2)}MB`);
        expect(memIncrease).toBeLessThan(100); // Should not exceed 100MB
        
        socket.disconnect();
      }
    });
  }, 120000);
});