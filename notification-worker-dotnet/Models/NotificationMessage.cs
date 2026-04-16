namespace notification_worker_dotnet.Models;

public sealed record NotificationMessage(
    string NotificationId,
    string UserId,
    string Subject,
    string Body,
    string Channel
);
