namespace notification_worker_dotnet.Models;

public sealed record DeliveryLog(
    string NotificationId,
    string Subject,
    string Body,
    string Channel,
    string Status,
    DateTime Timestamp,
    string ProcessedBy
);
