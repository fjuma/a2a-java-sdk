package io.a2a.spec;

import java.util.Collections;
import java.util.Map;

/**
 * Constants and utility methods related to the A2A protocol.
 */
public class A2A {

    public static final String CANCEL_TASK_METHOD = "tasks/cancel";
    public static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD = "tasks/pushNotificationConfig/get";
    public static final String GET_TASK_METHOD = "tasks/get";
    public static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD = "tasks/pushNotificationConfig/set";
    public static final String SEND_TASK_RESUBSCRIPTION_METHOD = "tasks/resubscribe";
    public static final String SEND_STREAMING_MESSAGE_METHOD = "message/stream";
    public static final String SEND_MESSAGE_METHOD = "message/send";

    public static final String JSONRPC_VERSION = "2.0";


    /**
     * Convert the given text to a user message.
     *
     * @param text the message text
     * @return the user message
     */
    public static Message toUserMessage(String text) {
        return toMessage(text, Message.Role.USER, null);
    }

    /**
     * Convert the given text to a user message.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @return the user message
     */
    public static Message toUserMessage(String text, String messageId) {
        return toMessage(text, Message.Role.USER, messageId);
    }

    /**
     * Convert the given text to an agent message.
     *
     * @param text the message text
     * @return the agent message
     */
    public static Message toAgentMessage(String text) {
        return toMessage(text, Message.Role.AGENT, null);
    }

    /**
     * Convert the given text to an agent message.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @return the agent message
     */
    public static Message toAgentMessage(String text, String messageId) {
        return toMessage(text, Message.Role.AGENT, messageId);
    }


    private static Message toMessage(String text, Message.Role role, String messageId) {
        Message.Builder messageBuilder = new Message.Builder()
                .role(role)
                .parts(Collections.singletonList(new TextPart(text)));
        if (messageId != null) {
            messageBuilder.messageId(messageId);
        }
        return messageBuilder.build();
    }

    protected static boolean isValidMethodName(String methodName) {
        return methodName != null && (methodName.equals(CANCEL_TASK_METHOD)
                || methodName.equals(GET_TASK_METHOD)
                || methodName.equals(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                || methodName.equals(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                || methodName.equals(SEND_TASK_RESUBSCRIPTION_METHOD)
                || methodName.equals(SEND_MESSAGE_METHOD)
                || methodName.equals(SEND_STREAMING_MESSAGE_METHOD));

    }

}
