package io.sugo.sugojavasdk;

import java.util.logging.Logger;

/* package */ class SugoConfig {

    static final Logger log = Logger.getLogger("SugoAPI");

    // 一行数据最大数量
    static final int MAX_MESSAGE_SIZE = 50;

    /**
     * HttpSender
     */
    static final int BUFFER_SIZE = 256; // Small, we expect small responses.
    static final int CONNECT_TIMEOUT_MILLIS = 2000;
    static final int READ_TIMEOUT_MILLIS = 10000;

    /**
     * FileSender
     */
    static final String sMessageFile = "./sugo_message/message";
    static final String sMaxBackupIndex = "50";
    static final String sMaxFileSize = "10MB";


    /**
     * defaultWorker 中，消费日志队列的线程数，如果
     */
    public static final int DEFAULT_WORKER_CUSTOMER_COUNT = 2;

    /**
     * defaultWorker 中，队列的最大长度，当队列满的时候，put 将被阻塞
     */
    public static final int DEFAULT_WORKER_QUEＵE_CAPACITY = 1000000;

}
