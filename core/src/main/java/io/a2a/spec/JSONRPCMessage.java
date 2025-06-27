package io.a2a.spec;

/**
 * Represents a JSONRPC message.
 */
public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {

    String getJsonrpc();
    Object getId();

}
