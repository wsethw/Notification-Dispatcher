namespace notification_worker_dotnet;

public class Worker : BackgroundService
{
    private readonly ILogger<Worker> _logger;
    private readonly RedisSubscriber _redisSubscriber;

    public Worker(ILogger<Worker> logger, RedisSubscriber redisSubscriber)
    {
        _logger = logger;
        _redisSubscriber = redisSubscriber;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation(".NET worker service starting");

        try
        {
            await _redisSubscriber.StartSubscribingAsync(stoppingToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Worker service error");
            throw;
        }
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation(".NET worker service stopping");
        await base.StopAsync(cancellationToken);
    }
}
