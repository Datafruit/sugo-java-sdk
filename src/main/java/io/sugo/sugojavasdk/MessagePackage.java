package io.sugo.sugojavasdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个 MessagePackage 可以用来发送多个 message.（装载数据）
 */
public class MessagePackage {

    private final List<JSONObject> mEventsMessages = new ArrayList<JSONObject>();

    /**
     * 添加一条 message 到 package. 批量发送 Messages 往往更有效率
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
        mEventsMessages.add(message);
    }

    /**
     * Returns true if the given JSONObject appears to be a valid Sugo message, created with #{@link MessageBuilder}.
     *
     * @param message a JSONObject to be tested
     * @return true if the argument appears to be a Sugo message
     */
    public boolean isValidMessage(JSONObject message) {
        // 可以看 MessageBuilder 了解 message 是怎么格式化的
        boolean ret = true;
        try {
            String eventName = message.getString("event");
            JSONObject propertiesObj = message.getJSONObject("properties");
            if (eventName == null) ret = false;
            else if (!propertiesObj.has("token")) ret = false;
            else if (!propertiesObj.has("time")) ret = false;
            else if (!propertiesObj.has("sugo_lib")) ret = false;

        } catch (JSONException e) {
            ret = false;
        }

        return ret;
    }

    List<JSONObject> getEventsMessages() {
        return mEventsMessages;
    }

}
