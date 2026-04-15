const EventEmitter = require('events');

class MockRedis extends EventEmitter {
  constructor() {
    super();
    // Simula o estado 'connect' após um pequeno delay (opcional)
    process.nextTick(() => {
      this.emit('connect');
    });
  }

  // Métodos ioredis usados no projeto
  on(event, callback) {
    if (event === 'connect') {
      // Já emitimos 'connect' no constructor, mas se quiser armazenar o callback:
      super.on(event, callback);
    } else if (event === 'error') {
      // Não faz nada para evitar logs feios
    } else {
      super.on(event, callback);
    }
    return this;
  }

  once(event, callback) {
    super.once(event, callback);
    return this;
  }

  publish(channel, message) {
    return Promise.resolve(1);
  }

  subscribe(...channels) {
    return Promise.resolve();
  }

  unsubscribe(...channels) {
    return Promise.resolve();
  }

  quit() {
    return Promise.resolve('OK');
  }

  disconnect() {
    return;
  }

  // Métodos auxiliares que podem ser usados em outros lugares
  get(key) {
    return Promise.resolve(null);
  }

  set(key, value, ...args) {
    return Promise.resolve('OK');
  }

  del(key) {
    return Promise.resolve(1);
  }

  duplicate() {
    // Retorna outra instância mockada (para testes que chamam redis.duplicate)
    return new MockRedis();
  }
}

// Exporta uma ÚNICA instância (como no original)
const mockRedisInstance = new MockRedis();

module.exports = mockRedisInstance;