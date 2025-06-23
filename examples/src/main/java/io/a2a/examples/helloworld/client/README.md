# A2A Hello World Example

This example demonstrates how to use the A2A Java SDK to communicate with an A2A server. The example includes a Java client that sends both regular and streaming messages to a Python A2A server.

## Prerequisites

- Java 11 or higher
- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html) (see [INSTALL_JBANG.md](INSTALL_JBANG.md) for quick installation instructions)
- Python 3.8 or higher
- [uv](https://github.com/astral-sh/uv) (recommended) or pip
- Git

## Setup and Run the Python A2A Server

The Python A2A server is part of the [a2a-samples](https://github.com/google-a2a/a2a-samples) project. To set it up and run it:

1. Clone the a2a-samples repository:
   ```bash
   git clone https://github.com/google-a2a/a2a-samples.git
   cd a2a-samples/samples/python/agents/helloworld
   ```

2. **Recommended method**: Install dependencies using uv (much faster Python package installer):
   ```bash
   # Install uv if you don't have it already
   # On macOS and Linux
   curl -LsSf https://astral.sh/uv/install.sh | sh
   # On Windows
   powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

   # Install the package using uv
   uv venv
   source .venv/bin/activate  # On Windows: .venv\Scripts\activate
   uv pip install -e .
   ```

4. Run the server with uv (recommended):
   ```bash
   uv run .
   ```

The server will start running on `http://localhost:9999`.

## Run the Java A2A Client with JBang

The Java client can be run using JBang, which allows you to run Java source files directly without any manual compilation.

### Build the A2A Java SDK

First, ensure you have built the `a2a-java` project:

```bash
cd /path/to/a2a-java
mvn clean install
```

### Option 1: Using the JBang script (Recommended)

A JBang script is provided in the example directory to make running the client easy:

1. Make sure you have JBang installed. If not, follow the [JBang installation guide](https://www.jbang.dev/documentation/guide/latest/installation.html).

2. Navigate to the example directory:
   ```bash
   cd examples/src/main/java/io/a2a/examples/helloworld/client
   ```

3. Run the client using the JBang script:
   ```bash
   jbang HelloWorldRunner.java
   ```

This script automatically handles the dependencies and sources for you.

### Option 2: Running with explicit classpath

Alternatively, you can run the Java client with JBang by specifying the classpath explicitly:

1. First, build the project with Maven:
   ```bash
   mvn clean package
   ```

2. Run the Java client with JBang from the root of the a2a-java project:
   ```bash
   jbang --cp target/classes examples/src/main/java/io/a2a/examples/helloworld/client/HelloWorldClient.java
   ```

## What the Example Does

The Java client (`HelloWorldClient.java`) performs the following actions:

1. Fetches the server's public agent card
2. Fetches the server's extended agent card 
3. Creates an A2A client using the extended agent card that connects to the Python server at `http://localhost:9999`.
4. Sends a regular message asking "how much is 10 USD in INR?".
5. Prints the server's response.
6. Sends the same message as a streaming request.
7. Prints each chunk of the server's streaming response as it arrives.

## Notes

- Make sure the Python server is running before starting the Java client.
- The client will wait for 10 seconds to collect streaming responses before exiting.
- You can modify the message text or server URL in the `HelloWorldClient.java` file if needed. 