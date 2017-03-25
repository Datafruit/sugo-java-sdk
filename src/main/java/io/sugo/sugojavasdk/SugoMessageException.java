package io.sugo.sugojavasdk;

import org.json.JSONObject;

/**
 * Thrown when the library detects malformed or invalid Sugo messages.
 *
 * Sugo messages are represented as JSONObjects, but not all JSONObjects represent valid Sugo messages.
 * SugoMessageExceptions are thrown when a JSONObject is passed to the Sugo library that can't be
 * passed on to the Sugo service.
 *
 * This is a runtime exception, since in most cases it is thrown due to errors in your application architecture.
 */
public class SugoMessageException extends RuntimeException {

    private static final long serialVersionUID = -6256936727567434262L;

    private JSONObject mBadMessage = null;

    /* package */ SugoMessageException(String message, JSONObject cause) {
        super(message);
        mBadMessage = cause;
    }

    /**
     * @return the (possibly null) JSONObject message associated with the failure
     */
    public JSONObject getBadMessage() {
        return mBadMessage;
    }

}
