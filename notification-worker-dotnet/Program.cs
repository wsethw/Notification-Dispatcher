using notification_worker_dotnet;

var host = Host.CreateDefaultBuilder(args)
    .ConfigureServices(services =>
    {
        services.AddHostedService<Worker>();
        services.AddSingleton<RedisSubscriber>();
    })
    .Build();

await host.RunAsync();