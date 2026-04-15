using StackExchange.Redis;
using System.Text.Json;

namespace notification_worker_dotnet;

public class RedisSubscriber
{
    private readonly ILogger<RedisSubscriber> _logger;
    private IConnectionMultiplexer _redis = null!;
    private ISubscriber _subscriber = null!;

    public RedisSubscriber(ILogger<RedisSubscriber> logger)
    {
        _logger = logger;
    }

    public async Task StartSubscribing(CancellationToken cancellationToken)
    {
        try
        {
            var redisHost = Environment.GetEnvironmentVariable("REDIS_HOST") ?? "redis";
            var redisPort = Environment.GetEnvironmentVariable("REDIS_PORT") ?? "6379";

            var options = ConfigurationOptions.Parse($"{redisHost}:{redisPort}");
            options.AbortOnConnectFail = false;

            _redis = await ConnectionMultiplexer.ConnectAsync(options);
            _subscriber = _redis.GetSubscriber();

            _logger.LogInformation($"✅ Connected to Redis at {redisHost}:{redisPort}");

            // Subscribe to email and SMS channels
            await _subscriber.SubscribeAsync("channel:email:request", ProcessNotification);
            await _subscriber.SubscribeAsync("channel:sms:request", ProcessNotification);

            _logger.LogInformation("✅ Subscribed to channel:email:request and channel:sms:request");

            // Keep the subscription alive
            while (!cancellationToken.IsCancellationRequested)
            {
                await Task.Delay(1000, cancellationToken);
            }

            await _subscriber.UnsubscribeAllAsync();
            _redis.Dispose();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Error in Redis subscription");
            throw;
        }
    }

    private async void ProcessNotification(RedisChannel channel, RedisValue message)
    {
        try
        {
            var json = message.ToString();
            using (JsonDocument doc = JsonDocument.Parse(json))
            {
                var root = doc.RootElement;
                var notificationId = root.GetProperty("notificationId").GetString();
                var subject = root.GetProperty("subject").GetString();
                var body = root.GetProperty("body").GetString();
                var channelType = root.GetProperty("channel").GetString();

                _logger.LogInformation($"📨 Received {channelType.ToUpper()} notification: {notificationId}");
                _logger.LogInformation($"   Subject: {subject}");
                _logger.LogInformation($"   Body: {body}");

                // Simulate CPU-bound template rendering
                await SimulateTemplateRendering(notificationId, subject, body, channelType);

                // Publish delivery log
                await PublishDeliveryLog(notificationId, subject, body, channelType);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Error processing notification");
        }
    }

    private async Task SimulateTemplateRendering(string notificationId, string? subject, string? body, string? channelType)
    {
        // CPU-bound work: simulate template rendering with string replacements
        _logger.LogInformation($"🔧 Rendering template for notification {notificationId}");

        // Simulate heavy processing
        var renderedSubject = subject;
        var renderedBody = body;

        for (int i = 0; i < 1000; i++)
        {
            renderedSubject = renderedSubject?.Replace("{{index}}", i.ToString()) ?? "";
            renderedBody = renderedBody?.Replace("{{data}}", $"iteration-{i}") ?? "";
        }

        _logger.LogInformation($"✅ Template rendered. Subject: {renderedSubject?.Substring(0, Math.Min(50, renderedSubject?.Length ?? 0))}...");

        // Simulate async I/O (e.g., sending to mail provider)
        await Task.Delay(200);
    }

    private async Task PublishDeliveryLog(string notificationId, string? subject, string? body, string? channelType)
    {
        try
        {
            var deliveryLog = new
            {
                notificationId,
                subject,
                body,
                channelType,
                status = "DELIVERED",
                timestamp = DateTime.UtcNow,
                processedBy = "dotnet-worker"
            };

            var json = System.Text.Json.JsonSerializer.Serialize(deliveryLog);
            await _subscriber!.PublishAsync("channel:delivery:log", json);

            _logger.LogInformation($"📤 Delivery log published for notification {notificationId}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Error publishing delivery log");
        }
    }
}