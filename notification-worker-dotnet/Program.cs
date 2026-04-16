using notification_worker_dotnet;
using notification_worker_dotnet.Models;

var builder = Host.CreateApplicationBuilder(args);

builder.Services
    .AddOptions<RedisOptions>()
    .Bind(builder.Configuration.GetSection(RedisOptions.SectionName));

builder.Services
    .AddOptions<WorkerOptions>()
    .Bind(builder.Configuration.GetSection(WorkerOptions.SectionName));

builder.Services.AddSingleton<RedisSubscriber>();
builder.Services.AddHostedService<Worker>();

await builder.Build().RunAsync();