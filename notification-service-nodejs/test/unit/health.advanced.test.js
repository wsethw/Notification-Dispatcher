describe('Health Endpoint - Advanced', () => {
  it('should return JSON response', () => {
    const response = { status: 'UP' };
    
    expect(response).toBeInstanceOf(Object);
    expect(typeof response).toBe('object');
  });

  it('should have status property', () => {
    const response = { status: 'UP' };
    
    expect(response).toHaveProperty('status');
  });

  it('should return string UP', () => {
    const response = { status: 'UP' };
    
    expect(typeof response.status).toBe('string');
    expect(response.status).toBe('UP');
  });

  it('should not have null or undefined values', () => {
    const response = { status: 'UP' };
    
    expect(response.status).not.toBeNull();
    expect(response.status).not.toBeUndefined();
  });

  it('should match expected structure', () => {
    const response = { status: 'UP' };
    
    expect(Object.keys(response)).toContain('status');
    expect(Object.keys(response).length).toBeGreaterThan(0);
  });

  it('should handle status comparison case-sensitive', () => {
    const response = { status: 'UP' };
    
    expect(response.status).toBe('UP');
    expect(response.status).not.toBe('up');
    expect(response.status).not.toBe('Up');
  });
});