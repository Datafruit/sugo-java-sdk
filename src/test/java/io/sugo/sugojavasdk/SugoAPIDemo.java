package io.sugo.sugojavasdk;

import org.json.JSONObject;

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
            mSugoAPI = new SugoAPI(new SugoAPI.FileSender("E:\\testLog"));
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
                            System.out.println("WILL SEND MESSAGE:\n" + message.toString());
                            messageCount = messageCount + 1;
                            messages.addMessage(message);
                        }
                    } while (message != null);
                    // 将刚才暂存数据的 MessagePackage 发送出去
                    mSugoAPI.sendMessages(messages);
                    System.out.println("Sent " + messageCount + " messages.");
                    // 每隔一段时间去读取数据队列
                    Thread.sleep(MILLIS_TO_WAIT);
                }
            } catch (IOException e) {
                throw new RuntimeException("Can't communicate with Mixpanel.", e);
            } catch (InterruptedException e) {
                System.out.println("Message process interrupted.");
            }
        }
    }

    public static void main(String[] args) {
        MessageBuilder messageBuilder = new MessageBuilder(PROJECT_TOKEN);
        Queue<JSONObject> messageQueue = new ConcurrentLinkedQueue<JSONObject>();
        SendingThread worker = new SendingThread(messageQueue);
        worker.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String value = scanner.nextLine();
            JSONObject props = new JSONObject();
            props.put("key", value);
            // 产生数据
            JSONObject message = messageBuilder.event("your distinct id", "testEventName", props);
            messageQueue.add(message);
        }

    }

}
