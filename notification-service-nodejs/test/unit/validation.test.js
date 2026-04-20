const {
  isValidUserId,
  normalizeNotification
} = require('../../src/socketHandler');

describe('notification validation', () => {
  it('accepts valid user ids and trims surrounding whitespace', () => {
    expect(isValidUserId('user-123')).toBe(true);
    expect(
      normalizeNotification({
        userId: ' user-123 '
      }).userId
    ).toBe('user-123');
  });

  it('rejects missing or blank user ids', () => {
    expect(isValidUserId('')).toBe(false);
    expect(isValidUserId('   ')).toBe(false);
    expect(isValidUserId(null)).toBe(false);
    expect(() => normalizeNotification({ userId: '   ' })).toThrow(
      'Notification payload must include a valid userId'
    );
  });

  it('applies safe defaults when optional fields are omitted', () => {
    const notification = normalizeNotification({
      userId: 'user-1'
    });

    expect(notification).toMatchObject({
      notificationId: null,
      userId: 'user-1',
      subject: 'Notification',
      body: '',
      channel: 'push'
    });
    expect(notification.timestamp).toEqual(expect.any(String));
  });

  it('parses JSON payloads and preserves explicit values', () => {
    const notification = normalizeNotification(
      JSON.stringify({
        notificationId: 'notif-abc123',
        userId: 'user-7',
        subject: 'Welcome',
        body: 'Portfolio-ready message',
        channel: 'push',
        timestamp: '2026-04-20T10:00:00.000Z'
      })
    );

    expect(notification).toEqual({
      notificationId: 'notif-abc123',
      userId: 'user-7',
      subject: 'Welcome',
      body: 'Portfolio-ready message',
      channel: 'push',
      timestamp: '2026-04-20T10:00:00.000Z'
    });
  });

  it('rejects invalid payload types before emitting notifications', () => {
    expect(() => normalizeNotification(null)).toThrow(
      'Notification payload must be an object'
    );
    expect(() => normalizeNotification('not-json')).toThrow();
  });
});
