using notification_worker_dotnet;
using notification_worker_dotnet.Models;

var builder = Host.CreateApplicationBuilder(args);

builder.Services
    .AddOptions<RedisOptions>()
    .Bind(builder.Configuration.GetSection(RedisOptions.SectionName))
    .ValidateDataAnnotations();

builder.Services.AddSingleton<RedisSubscriber>();
builder.Services.AddHostedService<Worker>();

await builder.Build().RunAsync();
