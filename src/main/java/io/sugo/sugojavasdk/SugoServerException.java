package io.sugo.sugojavasdk;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Thrown when the Sugo server refuses to accept a set of messages.
 *
 * This exception can be thrown when messages are too large,
 * event times are too old to accept, the api key is invalid, etc.
 */
public class SugoServerException extends IOException {

    private static final long serialVersionUID = 8230724556897575457L;

    private final List<JSONObject> mBadDelivery;

    public SugoServerException(String message, List<JSONObject> badDelivery) {
        super(message);
        mBadDelivery = badDelivery;
    }

    public List<JSONObject> getBadDeliveryContents() {
        return mBadDelivery;
    }

}
