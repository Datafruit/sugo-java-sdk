package io.sugo.sugojavasdk;

import com.sun.istack.internal.NotNull;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Sugo 数据采集简易接口（发送数据）, 用于服务器端应用程序.
 * 本 Java API 没有提供和假设任何线程模型, 这样设计的目的是为了可以轻松地将记录数据和发送数据的操作分开。
 */
public class SugoAPI {

    private Sender mSender;

    /**
     * @param sender
     */
    public SugoAPI(Sender sender) {
        mSender = sender;
    }

    /**
     * sendMessage 发送单条数据
     * 该方法是阻塞的
     * 想要一次性发送多条数据, see #{@link #sendMessages(MessagePackage)}
     *
     * @param message A JSONObject formatted by #{@link MessageBuilder}
     * @throws SugoMessageException if the given JSONObject is not (apparently) a Sugo message. This is a RuntimeException, callers should take care to submit only correctly formatted messages.
     * @throws IOException          if
     */
    public void sendMessage(JSONObject message)
            throws SugoMessageException, IOException {
        MessagePackage messagePackage = new MessagePackage();
        messagePackage.addMessage(message);
        sendMessages(messagePackage);
    }

    /**
     * 发送一组消息到终端，该方法是阻塞的
     *
     * @param toSend a MessagePackage containing a number of Sugo messages
     * @throws IOException
     */
    public void sendMessages(MessagePackage toSend) throws IOException {
        List<JSONObject> messages = toSend.getEventsMessages();
        for (int i = 0; i < messages.size(); i += SugoConfig.MAX_MESSAGE_SIZE) {
            int endIndex = i + SugoConfig.MAX_MESSAGE_SIZE;
            endIndex = Math.min(endIndex, messages.size());
            List<JSONObject> batch = messages.subList(i, endIndex);

            if (batch.size() > 0) {
                String messagesString = dataString(batch);
                boolean accepted = senderSendData(messagesString);
                if (!accepted) {
                    throw new SugoServerException("Server refused to accept messages, they may be malformed.", batch);
                }
            }
        }
    }

    /**
     * List<JSONObject> 对象转成 JSONArray 的字符串
     *
     * @param messages
     * @return
     */
    private String dataString(List<JSONObject> messages) {
        JSONArray array = new JSONArray();
        for (JSONObject message : messages) {
            array.put(message);
        }
        return array.toString();
    }

    /**
     * 使用 Base64 编码 之后再 URL 编码
     *
     * @param dataString JSON formatted string
     * @return encoded string for <b>data</b> parameter in API call
     * @throws NullPointerException If {@code dataString} is {@code null}
     */
    static String encodeDataString(String dataString) {
        try {
            byte[] utf8data = dataString.getBytes("utf-8");
            String base64data = new String(Base64.encode(utf8data));
            return URLEncoder.encode(base64data, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Sugo library requires utf-8 support", e);
        }
    }

    /**
     * 包访问权限是为了 mock 测试
     */
    /* package */ boolean senderSendData(String dataString) {
        return mSender.sendData(dataString);
    }


    /************************************************************/

    public interface Sender {
        boolean sendData(String dataString);
    }

    public static class HttpSender implements Sender {

        private final String mEventsEndpoint;

        public HttpSender(String eventsEndpoint) {
            mEventsEndpoint = eventsEndpoint;
        }

        public boolean sendData(String dataString) {
            URL endpoint = null;
            OutputStream postStream = null;
            URLConnection conn = null;

            try {
                endpoint = new URL(mEventsEndpoint);
                conn = endpoint.openConnection();
                conn.setReadTimeout(SugoConfig.READ_TIMEOUT_MILLIS);
                conn.setConnectTimeout(SugoConfig.CONNECT_TIMEOUT_MILLIS);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf8");
                String encodedData = encodeDataString(dataString);
                String encodedQuery = "data=" + encodedData;
                postStream = conn.getOutputStream();
                postStream.write(encodedQuery.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (postStream != null) {
                    try {
                        postStream.close();
                    } catch (IOException e) {
                        SugoConfig.log.warning("fail closed postStream :" + e);
                    }
                }
            }

            InputStream responseStream = null;
            String response = null;
            try {
                if (conn != null) {
                    responseStream = conn.getInputStream();
                    response = slurp(responseStream);
                }
            } catch (IOException e) {
                SugoConfig.log.warning("fail get responseStream :" + e);
            } finally {
                if (responseStream != null) {
                    try {
                        responseStream.close();
                    } catch (IOException e) {
                        SugoConfig.log.warning("fail closed responseStream :" + e);
                    }
                }
            }

            return ((response != null) && response.equals("1"));
        }

        private String slurp(InputStream in) throws IOException {
            final StringBuilder out = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(in, "utf8");

            char[] readBuffer = new char[SugoConfig.BUFFER_SIZE];
            int readCount = 0;
            do {
                readCount = reader.read(readBuffer);
                if (readCount > 0) {
                    out.append(readBuffer, 0, readCount);
                }
            } while (readCount != -1);

            return out.toString();
        }

    }

    public static class FileSender implements Sender {

        private final org.slf4j.Logger mLogger;

        public FileSender(boolean daily) {
            this(SugoConfig.sMessageFile, daily);
        }

        public FileSender(String messageFile, boolean daily) {
            this(messageFile, SugoConfig.sMaxBackupIndex, SugoConfig.sMaxFileSize, daily, "yyyyMMdd");
        }

        public FileSender(String messageFile, String maxBackup, String maxFileSize) {
            this(messageFile, maxBackup, maxFileSize, false, "");
        }

        public FileSender(String messageFile, boolean daily, String dataPattern) {
            this(messageFile, "", "", daily, dataPattern);
        }

        /**
         * @param messageFile 存放 message 数据的文件名字（超过存量之后，会被转移到新生成的文件里）
         * @param maxBackup   如果是按容量存储数据，该参数表示最大的文件数量
         * @param maxFileSize 如果是按容量存储数据，该参数表示数据文件的容量限度（单位是 KB MB）
         * @param daily       是否是按时间存储数据，false 则是按容量存储（默认是 false）
         * @param dataPattern 如果是按时间存储数据，该参数表示生成的文件的时间后缀（默认是按天 yyyyMMdd ）
         */
        public FileSender(String messageFile, String maxBackup, String maxFileSize, boolean daily, String dataPattern) {
            if (messageFile == null || messageFile.equals("")) {
                messageFile = SugoConfig.sMessageFile;
            }
            if (maxBackup == null || maxBackup.equals("")) {
                maxBackup = SugoConfig.sMaxBackupIndex;
            }
            if (maxFileSize == null || maxFileSize.equals("")) {
                maxFileSize = SugoConfig.sMaxFileSize;
            }
            Properties properties = new Properties();
//            properties.put("log4j.logger.io.sugo.sugojavasdk", "INFO,sugoConsole");               // 只输出到控制台
            if (daily) {
                properties.put("log4j.logger.io.sugo.sugojavasdk", "INFO,sugoConsole,sugoDailyFile");  // 输出到控制台和文件（按日期增加）
            } else {
                properties.put("log4j.logger.io.sugo.sugojavasdk", "INFO,sugoConsole,sugoFile");        // 输出到控制台和文件（按容量增加）
            }
            properties.put("log4j.additivity.io.sugo.sugojavasdk", "false");  // 设置这个子 Logger 输出日志不在父级别 Logger 里面输出

            properties.put("log4j.appender.sugoConsole", "org.apache.log4j.ConsoleAppender");
            properties.put("log4j.appender.sugoConsole.Threshold", "INFO");
            properties.put("log4j.appender.sugoConsole.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.sugoConsole.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss SSS} ->[%t]--[%-5p]--[%c{1}]--%m%n");

            properties.put("log4j.appender.sugoFile", "org.apache.log4j.RollingFileAppender");
            properties.put("log4j.appender.sugoFile.File", messageFile);
            properties.put("log4j.appender.sugoFile.Threshold", "INFO");
            properties.put("log4j.appender.sugoFile.Encoding", "UTF-8");
            properties.put("log4j.appender.sugoFile.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.sugoFile.layout.ConversionPattern", "%m%n");
            properties.put("log4j.appender.sugoFile.MaxBackupIndex", maxBackup);
            properties.put("log4j.appender.sugoFile.MaxFileSize", maxFileSize);

            properties.put("log4j.appender.sugoDailyFile", "org.apache.log4j.DailyRollingFileAppender");
            properties.put("log4j.appender.sugoDailyFile.File", messageFile);
            properties.put("log4j.appender.sugoDailyFile.DatePattern", "'_'" + dataPattern);
            properties.put("log4j.appender.sugoDailyFile.Threshold", "INFO");
            properties.put("log4j.appender.sugoDailyFile.Encoding", "UTF-8");
            properties.put("log4j.appender.sugoDailyFile.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.sugoDailyFile.layout.ConversionPattern", "%m%n");

            PropertyConfigurator.configure(properties);

            mLogger = org.slf4j.LoggerFactory.getLogger(FileSender.class);
        }

        public boolean sendData(String dataString) {
            if (dataString == null) {
                return false;
            }
            mLogger.info(dataString);
            return true;
        }

    }

    public static class ConsoleSender implements Sender {

        private final Logger mLogger;

        public ConsoleSender() {
            mLogger = Logger.getLogger("SugoAPI");
        }

        public ConsoleSender(@NotNull String logTag) {
            mLogger = Logger.getLogger(logTag);
        }

        public boolean sendData(String dataString) {
            mLogger.info(dataString);
            return true;
        }

    }
}
