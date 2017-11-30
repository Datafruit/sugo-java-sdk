package io.sugo.sugojavasdk;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Administrator
 * @date 2017/3/27
 */
public class SugoAPIDemo {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(SugoAPIDemo.class);
        logger.info("sugo java sdk demo starting ...");

        SugoAPI sugoAPI = new SugoAPI(new SugoAPI.FileSender(true));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "value");
        for (int i = 0; i < 100; i++) {
            sugoAPI.event("TestEvent" + i, jsonObject);
        }

    }

}
