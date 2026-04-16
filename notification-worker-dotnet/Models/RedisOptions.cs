using System.ComponentModel.DataAnnotations;

namespace notification_worker_dotnet.Models;

public sealed class RedisOptions
{
    public const string SectionName = "Redis";

    [Required]
    public string Host { get; init; } = "redis";

    [Range(1, 65535)]
    public int Port { get; init; } = 6379;
}
