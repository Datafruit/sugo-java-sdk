package io.sugo.sugojavasdk;

import org.json.JSONObject;

/**
 * @author ouwenjie
 * @date 17-11-29
 */
class DefaultWorker {

    private SugoAPI.Sender mSender;
    private MessageBuilder mMessageBuilder;
    private MessagePackage mMessagePackage;

    DefaultWorker(SugoAPI.Sender sender) {
        mSender = sender;
        mMessageBuilder = new MessageBuilder();
        mMessagePackage = new MessagePackage();

    }

    void event(String eventName, JSONObject properties) {
        JSONObject eventObj = mMessageBuilder.event(eventName, properties);
        if (mMessagePackage.isValidMessage(eventObj)) {
            senderSendData(eventObj.toString());
        } else {
            throw new SugoMessageException("Given JSONObject was not a valid Sugo message", eventObj);
        }
    }

    private boolean senderSendData(String dataString) {
        return mSender.sendData(dataString);
    }

}
