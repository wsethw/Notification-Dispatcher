module.exports = {
  testEnvironment: 'node',
  clearMocks: true,
  detectOpenHandles: true,
  setupFilesAfterEnv: ['<rootDir>/test/setup.js'],
  testMatch: ['<rootDir>/test/**/*.test.js'],
  collectCoverageFrom: [
    'src/**/*.js',
    '!src/__mocks__/**'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'html']
};
