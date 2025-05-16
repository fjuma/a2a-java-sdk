package io.a2a.client;

/**
 * Contains JSON strings for testing SSE streaming.
 */
public class JsonStreamingMessages {

    public static final String STREAMING_TASK_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "type": "task",
                    "id": "task-123",
                    "contextId": "context-456",
                    "status": {
                      "state": "working"
                    }
                  }
            }
            """;


    public static final String STREAMING_MESSAGE_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "type": "message",
                    "role": "agent",
                    "messageId": "msg-123",
                    "contextId": "context-456",
                    "parts": [
                      {
                        "type": "text",
                        "text": "Hello, world!"
                      }
                    ]
                  }
            }""";

    public static final String STREAMING_STATUS_UPDATE_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "taskId": "1",
                    "contextId": "2",
                    "status": {
                        "state": "submitted"
                    },
                    "final": false,
                    "type": "status-update"
                  }
            }""";

    public static final String STREAMING_ARTIFACT_UPDATE_EVENT = """
             data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "type": "artifact-update",
                    "taskId": "1",
                    "contextId": "2",
                    "append": false,
                    "lastChunk": true,
                    "artifact": {
                        "artifactId": "artifact-1",
                        "parts": [
                         {
                            "type": "text",
                            "text": "Why did the chicken cross the road? To get to the other side!"
                         }
                        ]
                    }
                  }
               }
            }""";

    public static final String STREAMING_ERROR_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "error": {
                    "code": -32602,
                    "message": "Invalid parameters",
                    "data": "Missing required field"
                  }
             }""";

    public static final String SEND_MESSAGE_STREAMING_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234",
             "method": "message/stream",
             "params": {
              "id": "1234",
              "message": {
               "role": "user",
               "parts": [
                {
                 "type": "text",
                 "text": "tell me some jokes"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "type": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": false
              },
             }
            }""";

    static final String SEND_MESSAGE_STREAMING_TEST_RESPONSE =
            "event: message\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"id\":\"2\",\"contextId\":\"context-1234\",\"status\":{\"state\":\"completed\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"type\":\"text\",\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{},\"type\":\"task\"}}\n\n";

} 