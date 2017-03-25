package io.sugo.sugojavasdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个 MessagePackage 可以用来发送多个 messages 到 Sugo.
 */
public class MessagePackage {

    private final List<JSONObject> mEventsMessages = new ArrayList<JSONObject>();

    /**
     * 添加一条 message 到 package. 批量发送 Messages to Sugo 往往更有效率
     *
     * @param message a JSONObject produced by #{@link MessageBuilder}. Arguments not from MessageBuilder will throw an exception.
     * @throws SugoMessageException if the given JSONObject is not formatted appropriately.
     * @see MessageBuilder
     */
    public void addMessage(JSONObject message) {
        if (!isValidMessage(message)) {
            throw new SugoMessageException("Given JSONObject was not a valid Sugo message", message);
        }
        // ELSE message is valid

        try {
            String messageType = message.getString("message_type");
            JSONObject messageContent = message.getJSONObject("message");

            if (messageType.equals("event")) {
                mEventsMessages.add(messageContent);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Apparently valid sugo message could not be interpreted.", e);
        }
    }

    /**
     * Returns true if the given JSONObject appears to be a valid Sugo message, created with #{@link MessageBuilder}.
     *
     * @param message a JSONObject to be tested
     * @return true if the argument appears to be a Sugo message
     */
    public boolean isValidMessage(JSONObject message) {
        // 可以看 MessageBuilder 类了解这些 message 是怎么格式化的
        boolean ret = true;
        try {
            int envelopeVersion = message.getInt("envelope_version");
            if (envelopeVersion > 0) {
                String messageType = message.getString("message_type");
                JSONObject messageContents = message.getJSONObject("message");

                if (messageContents == null) {
                    ret = false;
                } else if (!messageType.equals("event")) {
                    ret = false;
                }
            }
        } catch (JSONException e) {
            ret = false;
        }

        return ret;
    }

    List<JSONObject> getEventsMessages() {
        return mEventsMessages;
    }

}
