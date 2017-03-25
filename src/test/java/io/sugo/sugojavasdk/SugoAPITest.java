package io.sugo.sugojavasdk;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Administrator on 2017/3/25.
 */
public class SugoAPITest {

    public static void main(String[] args) {
        SugoAPI api = new SugoAPI(new SugoAPI.FileSender("E:\\testLog"));
        JSONObject props = new JSONObject();
        props.put("key1", "value1");
        JSONObject jsonObject = new MessageBuilder("token").event("distinct id", "testEventName", props);
        try {
            api.sendMessage(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}