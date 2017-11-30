package io.sugo.sugojavasdk;

import org.json.JSONObject;

import java.util.concurrent.*;

/**
 * @author ouwenjie
 * @date 17-11-29
 */
class DefaultWorker {

    private SugoAPI.Sender mSender;
    private MessageBuilder mMessageBuilder;
    private MessagePackage mMessagePackage;

    private BlockingQueue<JSONObject> mMessageQueue;

    DefaultWorker(SugoAPI.Sender sender, String token) {
        mSender = sender;
        mMessageBuilder = new MessageBuilder(token);
        mMessagePackage = new MessagePackage();

        mMessageQueue = new LinkedBlockingQueue<>(SugoConfig.DEFAULT_WORKER_QUEＵE_CAPACITY);

        for (int i = 0; i < SugoConfig.DEFAULT_WORKER_CUSTOMER_COUNT; i++) {
            Executors.newSingleThreadExecutor().submit(new CustomerWorker());
        }
    }

    void event(String eventName, JSONObject properties) {
        JSONObject eventObj = mMessageBuilder.event(eventName, properties);
        if (mMessagePackage.isValidMessage(eventObj)) {
            try {
                mMessageQueue.put(eventObj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            throw new SugoMessageException("Given JSONObject was not a valid Sugo message", eventObj);
        }
    }

    private boolean senderSendData(String dataString) {
        return mSender.sendData(dataString);
    }

    class CustomerWorker implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    JSONObject message = mMessageQueue.take();
                    String dataString = message.toString();
                    senderSendData(dataString);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
