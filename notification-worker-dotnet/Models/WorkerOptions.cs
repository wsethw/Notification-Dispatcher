namespace notification_worker_dotnet.Models;

public sealed class WorkerOptions
{
    public const string SectionName = "Worker";

    public string ProcessedBy { get; init; } = "dotnet-worker";
}