package io.sugo.sugojavasdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 这个类是给 SugoAPI 发送的 message 事件创建适当形式的 JSONObjects（生产数据）
 * 这个类的实例可以单独实例化，和 SugoAPI 的对象分开
 * 因此这些 message 可以从适当的队列中得到，或者通过本地网络发送
 *
 * @author root
 */
public class MessageBuilder {

    private final String mToken;

    public MessageBuilder(String token) {
        mToken = token;
    }

    public JSONObject event(String eventName, JSONObject properties) {
        return event(null, eventName, properties);
    }

    /***
     * 创建 message 给 SugoAPI 消费
     *
     * @param distinctId a string uniquely identifying the individual cause associated with this event
     *           (for example, the user id of a signing-in user, or the hostname of a server)
     * @param eventName a human readable name for the event, for example "Purchase", or "Threw Exception"
     * @param properties a JSONObject associating properties with the event. These are useful
     *           for reporting and segmentation of events. It is often useful not only to include
     *           properties of the event itself (for example { 'Item Purchased' : 'Hat' } or
     *           { 'ExceptionType' : 'OutOfMemory' }), but also properties associated with the
     *           identified user (for example { 'MemberSince' : '2012-01-10' } or { 'TotalMemory' : '10TB' })
     */
    public JSONObject event(String distinctId, String eventName, JSONObject properties) {
        long time = System.currentTimeMillis() / 1000;

        // Nothing below should EVER throw a JSONException.
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("event", eventName);

            JSONObject propertiesObj = null;
            if (properties == null) {
                propertiesObj = new JSONObject();
            } else {
                propertiesObj = new JSONObject(properties.toString());
            }

            if (!propertiesObj.has("token")) {
                propertiesObj.put("token", mToken);
            }
            if (!propertiesObj.has("time")) {
                propertiesObj.put("time", time);
            }
            if (!propertiesObj.has("sugo_lib")) {
                propertiesObj.put("sugo_lib", "jdk");
            }

            if (distinctId != null) {
                propertiesObj.put("distinct_id", distinctId);
            }

            dataObj.put("properties", propertiesObj);

            return dataObj;
        } catch (JSONException e) {
            throw new RuntimeException("Can't construct a Sugo message", e);
        }
    }

}
