module.exports = {
  testEnvironment: 'node',
  clearMocks: true,
  detectOpenHandles: true,
  setupFilesAfterEnv: ['<rootDir>/test/setup.js'],
  testMatch: [
    '<rootDir>/test/unit/index.test.js',
    '<rootDir>/test/unit/redisClient.test.js',
    '<rootDir>/test/unit/socketHandler.test.js',
    '<rootDir>/test/integration/push-flow.integration.test.js'
  ],
  collectCoverageFrom: [
    'src/**/*.js',
    '!src/__mocks__/**'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'html']
};
