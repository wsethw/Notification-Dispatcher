const http = require('http');
const { createServer, shutdown } = require('../../src/index');

function request(server, path) {
  const address = server.address();

  return new Promise((resolve, reject) => {
    const req = http.get(
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
            headers: response.headers,
            body
          });
        });
      }
    );

    req.on('error', reject);
  });
}

describe('Health endpoint', () => {
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

  it('returns JSON content with the service name', async () => {
    const response = await request(server, '/api/v1/health');

    expect(response.statusCode).toBe(200);
    expect(response.headers['content-type']).toContain('application/json');
    expect(JSON.parse(response.body)).toEqual({
      status: 'UP',
      service: 'notification-service-nodejs'
    });
  });

  it('keeps returning a healthy payload across repeated requests', async () => {
    const [firstResponse, secondResponse] = await Promise.all([
      request(server, '/api/v1/health'),
      request(server, '/api/v1/health')
    ]);

    expect(firstResponse.statusCode).toBe(200);
    expect(secondResponse.statusCode).toBe(200);
    expect(JSON.parse(firstResponse.body)).toEqual(JSON.parse(secondResponse.body));
  });
});
