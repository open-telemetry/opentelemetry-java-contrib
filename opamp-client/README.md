# OpAMP Client

Java implementation of the OpAMP
client [spec](https://github.com/open-telemetry/opamp-spec/blob/main/specification.md).

> [!WARNING]
> This is an incubating feature. Breaking changes can happen on a new release without previous
> notice and without backward compatibility guarantees.

## Usage

```java
// Initializing it

RequestService requestService = HttpRequestService.create(OkHttpSender.create("[OPAMP_SERVICE_URL]"));
// RequestService requestService = WebSocketRequestService.create(OkHttpWebSocket.create("[OPAMP_SERVICE_URL]")); // Use this instead to connect to the server via WebSocket.
OpampClient client =
    OpampClient.builder()
        .putIdentifyingAttribute("service.name", "My service name")
        .enableRemoteConfig()
        .setRequestService(requestService)
        .build(
            new OpampClient.Callbacks() {
              @Override
              public void onConnect() {}

              @Override
              public void onConnectFailed(@Nullable Throwable throwable) {}

              @Override
              public void onErrorResponse(ServerErrorResponse errorResponse) {}

              @Override
              public void onMessage(MessageData messageData) {
                AgentRemoteConfig remoteConfig = messageData.getRemoteConfig();
                if (remoteConfig != null) {
                  // A remote config was received

                  // After applying it...
                  client.setRemoteConfigStatus(
                      new RemoteConfigStatus.Builder()
                          .status(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED)
                          .build());
                }
              }
            });

// State update
client.setAgentDescription(new AgentDescription.Builder().build());

// App shutdown
client.close();

```

## Component owners

- [Cesar Munoz](https://github.com/LikeTheSalad), Elastic
- [Jack Shirazi](https://github.com/jackshirazi), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
