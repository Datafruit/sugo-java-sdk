package io.sugo.sugojavasdk;

import com.sun.istack.internal.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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

        private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        private final SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss-SSS");

        private final String mMsgDirectory;

        public FileSender(String msgDirectory) {
            mMsgDirectory = msgDirectory;
        }

        public boolean sendData(String dataString) {
            boolean result = true;
            if (dataString == null) {
                return false;
            }

            String file = generateFile();
            // 因为文件生成最细粒度是毫秒，而文件是以覆盖形式写入，
            // 所以不能 1ms 内生成两次文件，所以推荐使用 MessagePackage 批量写入。
            FileOutputStream outputStream = null;// = new FileOutputStream(file, false);
            FileLock lock = null;
            try {
                outputStream = new FileOutputStream(file, false);
                final FileChannel channel = outputStream.getChannel();
                lock = channel.lock(0, Long.MAX_VALUE, false);
                outputStream.write(dataString.getBytes("UTF-8"));
            } catch (IOException e) {
                SugoConfig.log.warning("fail to write file: " + e);
                result = false;
            } finally {
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e) {
                        SugoConfig.log.warning("fail to release file lock: " + e);
                    }
                }
                try {
                    if (outputStream != null) {
                        outputStream.close();
                        SugoConfig.log.info("closed file. filename=" + file);
                    }
                } catch (IOException e) {
                    SugoConfig.log.warning("fail to close output stream: " + e);
                }
            }
            return result;
        }

        private String generateFile() {
            Date now = new Date();
            String today = simpleDateFormat.format(now);
            String dir = mMsgDirectory + File.separator + today;
            File dirFile = new File(dir);
            if (!dirFile.exists() && !dirFile.isDirectory()) {
                dirFile.mkdirs();
            }
            String time = simpleTimeFormat.format(now);
            String fileName = dir + File.separator + time;
            SugoConfig.log.info("generate file. filename=" + fileName);
            return fileName;
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
