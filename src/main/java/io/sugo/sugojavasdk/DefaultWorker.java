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
    private CustomerWorker mCustomerWorker;
    private ThreadPoolExecutor mExecutor;

    private BlockingQueue<JSONObject> mMessageQueue;


    DefaultWorker(SugoAPI.Sender sender, String token) {
        mSender = sender;
        mMessageBuilder = new MessageBuilder(token);
        mMessagePackage = new MessagePackage();
        mCustomerWorker = new CustomerWorker();

        mMessageQueue = new LinkedBlockingQueue<>();
        mExecutor = new ThreadPoolExecutor(8, 12, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(10000), new ThreadPoolExecutor.AbortPolicy());

        for (int i = 0; i < SugoConfig.CUSTOMER_THREAD_COUNT; i++) {
            Executors.newSingleThreadExecutor().submit(mCustomerWorker);
        }
    }

    void event(String eventName, JSONObject properties) {
        JSONObject eventObj = mMessageBuilder.event(eventName, properties);
        if (mMessagePackage.isValidMessage(eventObj)) {
            mExecutor.execute(new ProducerWorker(eventObj));
        } else {
            throw new SugoMessageException("Given JSONObject was not a valid Sugo message", eventObj);
        }
    }

    private boolean senderSendData(String dataString) {
        return mSender.sendData(dataString);
    }

    class ProducerWorker implements Runnable {

        JSONObject message;

        ProducerWorker(JSONObject message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                mMessageQueue.put(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class CustomerWorker implements Runnable {

        CustomerWorker() {
        }

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
