using StackExchange.Redis;
using notification_worker_dotnet.Models;
using System.Text.Json;
using Microsoft.Extensions.Options;

namespace notification_worker_dotnet;

public class RedisSubscriber
{
    private const string EmailChannel = "channel:email:request";
    private const string SmsChannel = "channel:sms:request";
    private const string DeliveryLogChannel = "channel:delivery:log";
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    private readonly ILogger<RedisSubscriber> _logger;
    private readonly RedisOptions _options;
    private IConnectionMultiplexer _redis = null!;
    private ISubscriber _subscriber = null!;

    public RedisSubscriber(ILogger<RedisSubscriber> logger, IOptions<RedisOptions> options)
    {
        _logger = logger;
        _options = options.Value;
    }

    public async Task StartSubscribingAsync(CancellationToken cancellationToken)
    {
        try
        {
            var redisHost = Environment.GetEnvironmentVariable("REDIS_HOST") ?? _options.Host;
            var redisPort = Environment.GetEnvironmentVariable("REDIS_PORT") ?? _options.Port.ToString();

            var options = ConfigurationOptions.Parse($"{redisHost}:{redisPort}");
            options.AbortOnConnectFail = false;

            _redis = await ConnectionMultiplexer.ConnectAsync(options);
            _subscriber = _redis.GetSubscriber();

            _logger.LogInformation("Connected to Redis at {RedisHost}:{RedisPort}", redisHost, redisPort);

            await _subscriber.SubscribeAsync(RedisChannel.Literal(EmailChannel), (_, message) => QueueProcessing(message));
            await _subscriber.SubscribeAsync(RedisChannel.Literal(SmsChannel), (_, message) => QueueProcessing(message));

            _logger.LogInformation("Subscribed to {EmailChannel} and {SmsChannel}", EmailChannel, SmsChannel);

            try
            {
                await Task.Delay(Timeout.Infinite, cancellationToken);
            }
            catch (OperationCanceledException)
            {
                _logger.LogInformation("Cancellation requested, unsubscribing from Redis channels");
            }

            await _subscriber.UnsubscribeAllAsync();
            await _redis.CloseAsync();
            _redis.Dispose();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in Redis subscription");
            throw;
        }
    }

    private void QueueProcessing(RedisValue message)
    {
        _ = ProcessNotificationAsync(message);
    }

    private async Task ProcessNotificationAsync(RedisValue message)
    {
        try
        {
            if (!TryParseNotification(message.ToString(), out var notification))
            {
                _logger.LogWarning("Ignoring invalid notification payload: {Payload}", message.ToString());
                return;
            }

            _logger.LogInformation(
                "Received {Channel} notification {NotificationId} for user {UserId}",
                notification.Channel,
                notification.NotificationId,
                notification.UserId
            );

            var rendered = RenderTemplate(notification);
            await PublishDeliveryLogAsync(notification, rendered.subject, rendered.body);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing notification");
        }
    }

    internal static bool TryParseNotification(string json, out NotificationMessage? notification)
    {
        notification = null;

        if (string.IsNullOrWhiteSpace(json))
        {
            return false;
        }

        NotificationMessage? parsed;

        try
        {
            parsed = JsonSerializer.Deserialize<NotificationMessage>(json, JsonOptions);
        }
        catch (JsonException)
        {
            return false;
        }

        if (parsed is null ||
            string.IsNullOrWhiteSpace(parsed.NotificationId) ||
            string.IsNullOrWhiteSpace(parsed.UserId) ||
            string.IsNullOrWhiteSpace(parsed.Channel))
        {
            return false;
        }

        notification = parsed;
        return true;
    }

    private (string subject, string body) RenderTemplate(NotificationMessage notification)
    {
        _logger.LogInformation("Rendering template for notification {NotificationId}", notification.NotificationId);

        var renderedSubject = notification.Subject;
        var renderedBody = notification.Body;

        for (var index = 0; index < 1000; index++)
        {
            renderedSubject = renderedSubject.Replace("{{index}}", index.ToString());
            renderedBody = renderedBody.Replace("{{data}}", $"iteration-{index}");
        }

        return (renderedSubject, renderedBody);
    }

    private async Task PublishDeliveryLogAsync(NotificationMessage notification, string renderedSubject, string renderedBody)
    {
        try
        {
            var deliveryLog = new DeliveryLog(
                notification.NotificationId,
                renderedSubject,
                renderedBody,
                notification.Channel,
                "DELIVERED",
                DateTime.UtcNow,
                "dotnet-worker"
            );

            var json = JsonSerializer.Serialize(deliveryLog, JsonOptions);
            await _subscriber.PublishAsync(RedisChannel.Literal(DeliveryLogChannel), json);

            _logger.LogInformation("Delivery log published for notification {NotificationId}", notification.NotificationId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error publishing delivery log");
        }
    }
}
