package io.a2a.spec;

/**
 * Represents a JSONRPC message.
 */
public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {

    String JSONRPC_VERSION = "2.0";

    String getJsonrpc();
    Object getId();

}
