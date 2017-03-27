package io.sugo.sugojavasdk;

import java.util.logging.Logger;

/* package */ class SugoConfig {

    static final int BUFFER_SIZE = 256; // Small, we expect small responses.

    static final int CONNECT_TIMEOUT_MILLIS = 2000;
    static final int READ_TIMEOUT_MILLIS = 10000;

    static final int MAX_MESSAGE_SIZE = 50;

    static final Logger log = Logger.getLogger("SugoAPI");
}
