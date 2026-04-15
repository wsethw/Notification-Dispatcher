module.exports = {
  testEnvironment: 'node',
  forceExit: true,
  detectOpenHandles: true,
  moduleNameMapper: {
    // Mapeia TODAS as formas de importar o redisClient para o mock
    '^./redisClient$': '<rootDir>/src/__mocks__/redisClient.js',
    '^../src/redisClient$': '<rootDir>/src/__mocks__/redisClient.js',
    '^../../src/redisClient$': '<rootDir>/src/__mocks__/redisClient.js',
    '^src/redisClient$': '<rootDir>/src/__mocks__/redisClient.js'
  },
  collectCoverageFrom: [
    'src/**/*.js',
    '!src/__mocks__/**'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'html']
};