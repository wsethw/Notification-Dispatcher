const http = require('http');
const { createServer, shutdown } = require('../../src/index');

function httpGet(server, path) {
  const address = server.address();

  return new Promise((resolve, reject) => {
    const request = http.get(
      {
        hostname: '127.0.0.1',
        port: address.port,
        path
      },
      (response) => {
        let body = '';
        response.on('data', (chunk) => {
          body += chunk;
        });
        response.on('end', () => {
          resolve({
            statusCode: response.statusCode,
            body
          });
        });
      }
    );

    request.on('error', reject);
  });
}

describe('HTTP server', () => {
  let server;
  let subscriber;

  beforeEach(async () => {
    const created = createServer();
    server = created.server;
    subscriber = created.subscriber;
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  });

  afterEach(async () => {
    await shutdown(server, subscriber);
  });

  it('returns a healthy JSON payload', async () => {
    const response = await httpGet(server, '/api/v1/health');

    expect(response.statusCode).toBe(200);
    expect(JSON.parse(response.body)).toEqual({
      status: 'UP',
      service: 'notification-service-nodejs'
    });
  });

  it('serves the realtime client page', async () => {
    const response = await httpGet(server, '/');

    expect(response.statusCode).toBe(200);
    expect(response.body).toContain('Notification Dispatcher');
  });
});
