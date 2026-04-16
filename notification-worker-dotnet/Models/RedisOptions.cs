namespace notification_worker_dotnet.Models;

public sealed class RedisOptions
{
    public const string SectionName = "Redis";

    public string Host { get; init; } = "redis";

    public int Port { get; init; } = 6379;
}