package io.sugo.sugojavasdk;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Administrator on 2017/3/25.
 */
public class SugoAPITest {

    public static void main(String[] args) {
        SugoAPI api = new SugoAPI(new SugoAPI.FileSender("E:\\testLog"));
//        SugoAPI api = new SugoAPI(new SugoAPI.ConsoleSender());
        JSONObject props = new JSONObject();
        MessagePackage messagePackage = new MessagePackage();
        System.out.println(System.currentTimeMillis());
        for (int i = 0; i < 10; i++) {
            props.put("key", "value" + i);
            JSONObject jsonObject = new MessageBuilder("token").event("distinct id", "testEventName", props);
            messagePackage.addMessage(jsonObject);
        }
        try {
            api.sendMessages(messagePackage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(System.currentTimeMillis());
    }
}