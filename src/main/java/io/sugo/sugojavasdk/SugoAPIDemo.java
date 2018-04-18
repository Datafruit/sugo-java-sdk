package io.sugo.sugojavasdk;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Administrator
 * @date 2017/3/27
 */
public class SugoAPIDemo {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(SugoAPIDemo.class);
        logger.info("sugo java sdk demo starting ...");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                SugoAPI sugoAPI = new SugoAPI(new SugoAPI.FileSender("/home/fengxj/test/access.log", true, "yyyyMMddHH"));

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("test", "value");
                for (int i = 0; i < 100000; i++) {
                    sugoAPI.event("TestEvent", jsonObject);
                }
            }
        };
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }

        File file = new File("/home/fengxj/test/access.log");

    }


}
