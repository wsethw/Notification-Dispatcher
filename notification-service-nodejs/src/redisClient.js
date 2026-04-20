const Redis = require('ioredis');

function resolveRedisHost() {
  if (process.env.REDIS_HOST) {
    return process.env.REDIS_HOST;
  }

  return process.env.NODE_ENV === 'production' ? 'redis' : '127.0.0.1';
}

function resolveRedisPort() {
  const parsedPort = Number.parseInt(process.env.REDIS_PORT || '6379', 10);
  return Number.isFinite(parsedPort) ? parsedPort : 6379;
}

function buildRedisOptions() {
  const host = resolveRedisHost();
  const port = resolveRedisPort();

  return {
    host,
    port,
    lazyConnect: process.env.NODE_ENV === 'test',
    enableReadyCheck: true,
    maxRetriesPerRequest: null,
    retryStrategy(times) {
      return Math.min(times * 50, 2000);
    }
  };
}

const redis = new Redis(buildRedisOptions());

redis.on('connect', () => {
  console.log(
    `[redis] connected to ${resolveRedisHost()}:${resolveRedisPort()}`
  );
});

redis.on('error', (error) => {
  console.error('[redis] connection error', error);
});

module.exports = redis;
module.exports.buildRedisOptions = buildRedisOptions;
module.exports.resolveRedisHost = resolveRedisHost;
module.exports.resolveRedisPort = resolveRedisPort;
