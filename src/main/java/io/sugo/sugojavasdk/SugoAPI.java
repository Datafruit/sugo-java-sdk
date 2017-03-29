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

        static {
            Properties properties = new Properties();
            properties.put("log4j.rootLogger", "INFO,consoleAppender,fileAppender");
//            properties.put("log4j.rootLogger","INFO,consoleAppender,dailyFileAppender");

            properties.put("log4j.appender.consoleAppender", "org.apache.log4j.ConsoleAppender");
            properties.put("log4j.appender.consoleAppender.Threshold", "INFO");
            properties.put("log4j.appender.consoleAppender.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.consoleAppender.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss SSS} ->[%t]--[%-5p]--[%c{1}]--%m%n");

            properties.put("log4j.appender.fileAppender", "org.apache.log4j.RollingFileAppender");
            properties.put("log4j.appender.fileAppender.File", "./sugo_message/message");
            properties.put("log4j.appender.fileAppender.Threshold", "INFO");
            properties.put("log4j.appender.fileAppender.Encoding", "UTF-8");
            properties.put("log4j.appender.fileAppender.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.fileAppender.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss}--%m%n");
            properties.put("log4j.appender.fileAppender.MaxBackupIndex", "50");
            properties.put("log4j.appender.fileAppender.MaxFileSize", "10MB");
            properties.put("log4j.appender.fileAppender.Append", "true");

            properties.put("log4j.appender.dailyFileAppender", "org.apache.log4j.DailyRollingFileAppender");
            properties.put("log4j.appender.dailyFileAppender.File", "./sugo_message/message");
            properties.put("log4j.appender.dailyFileAppender.DatePattern", "'_'yyyy-MM-dd'.log'");
            properties.put("log4j.appender.dailyFileAppender.Threshold", "INFO");
            properties.put("log4j.appender.dailyFileAppender.Encoding", "UTF-8");
            properties.put("log4j.appender.dailyFileAppender.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.dailyFileAppender.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss}--%m%n");
            properties.put("log4j.appender.dailyFileAppender.Append", "true");

            PropertyConfigurator.configure(properties);

        }

        private final org.slf4j.Logger mLogger = org.slf4j.LoggerFactory.getLogger(FileSender.class);

        public FileSender() {

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
