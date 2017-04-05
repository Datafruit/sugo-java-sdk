package io.sugo.sugojavasdkdemo;

import io.sugo.sugojavasdk.MessageBuilder;
import io.sugo.sugojavasdk.MessagePackage;
import io.sugo.sugojavasdk.SugoAPI;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Administrator on 2017/3/27.
 */
public class SugoAPIDemo {

    public static String PROJECT_TOKEN = "project token"; // "YOUR TOKEN";
    public static long MILLIS_TO_WAIT = 10 * 1000;

    /**
     * 该线程用于处理数据队列，自动批量发送数据
     */
    private static class SendingThread extends Thread {

        private final SugoAPI mSugoAPI;
        private final Queue<JSONObject> mMessageQueue;

        public SendingThread(Queue<JSONObject> messageQueue) {
            mSugoAPI = new SugoAPI(new SugoAPI.FileSender("./sugo_daily_message/message", true, "yyyy-MM-dd_HH-mm"));
//            mSugoAPI = new SugoAPI(new SugoAPI.FileSender(false));
            mMessageQueue = messageQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int messageCount = 0;
                    MessagePackage messages = new MessagePackage();
                    JSONObject message = null;
                    do {
                        // 读取队列中数据暂存到 MessagePackage 中
                        message = mMessageQueue.poll();
                        if (message != null) {
                            System.out.println("将要发送的数据:\n" + message.toString());
                            messageCount = messageCount + 1;
                            messages.addMessage(message);
                        }
                    } while (message != null);
                    // 将刚才暂存数据的 MessagePackage 发送出去
                    mSugoAPI.sendMessages(messages);
                    System.out.println("发送了 " + messageCount + " 条数据.");
                    // 每隔一段时间去读取数据队列
                    Thread.sleep(MILLIS_TO_WAIT);
                }
            } catch (IOException e) {
                throw new RuntimeException("发送数据失败.", e);
            } catch (InterruptedException e) {
                System.out.println("数据发送线程已经中断.");
            }
        }
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(SugoAPIDemo.class);
        logger.info("sugo java sdk demo starting ...");

        MessageBuilder messageBuilder = new MessageBuilder(PROJECT_TOKEN);
        Queue<JSONObject> messageQueue = new ConcurrentLinkedQueue<JSONObject>();
        SendingThread worker = new SendingThread(messageQueue);
        worker.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String name = scanner.nextLine();  // 不断从控制台输入读取一行数据
            JSONObject props = new JSONObject();
            props.put(name + "_key", name + "_value");
            // 产生数据
            JSONObject message = messageBuilder.event("your distinct id", name, props);
            messageQueue.add(message);
        }

    }

}
