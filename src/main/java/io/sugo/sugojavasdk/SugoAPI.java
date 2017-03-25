package io.sugo.sugojavasdk;

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

/**
 * Sugo 数据采集简易接口, 用于服务器端应用程序.
 * 本 Java API 没有提供和假设任何线程模型, 这样设计的目的是为了可以轻松地将记录数据和发送数据的操作分开。
 */
public class SugoAPI {

    private static final int BUFFER_SIZE = 256; // Small, we expect small responses.

    private static final int CONNECT_TIMEOUT_MILLIS = 2000;
    private static final int READ_TIMEOUT_MILLIS = 10000;

    private Sender mSender;

    /**
     * 构造一个 SugoAPI 对象用于生产环境
     */
    public SugoAPI() {

    }

    /**
     * 构造一个 SugoAPI 用于自定义 URL
     * 这对于测试和代理是很有用的
     *
     * @param sender
     * @see #SugoAPI()
     */
    public SugoAPI(Sender sender) {
        mSender = sender;
    }

    /**
     * sendMessage 发送单条数据
     * 每一次发送数据到远程 Sugo servers 都是阻塞的
     * 想要一次性发送多条数据, see #{@link #deliver(MessagePackage)}
     *
     * @param message A JSONObject formatted by #{@link MessageBuilder}
     * @throws SugoMessageException if the given JSONObject is not (apparently) a Sugo message. This is a RuntimeException, callers should take care to submit only correctly formatted messages.
     * @throws IOException          if
     */
    public void sendMessage(JSONObject message)
            throws SugoMessageException, IOException {
        MessagePackage delivery = new MessagePackage();
        delivery.addMessage(message);
        deliver(delivery);
    }

    /**
     * 试图发送一个给定的 MessagePackage 到 Sugo servers. 该方法是阻塞的,
     * 可能在多个服务器请求。
     * 对于大多数应用程序, 这个方法应该被消费在一个单独的线程或队列。
     *
     * @param toSend a MessagePackage containing a number of Sugo messages
     * @throws IOException
     * @see MessagePackage
     */
    public void deliver(MessagePackage toSend) throws IOException {
        List<JSONObject> events = toSend.getEventsMessages();

        sendMessages(events);

    }

    /**
     * 发送一组消息到终端
     *
     * @param messages
     * @throws IOException
     */
    private void sendMessages(List<JSONObject> messages) throws IOException {
        for (int i = 0; i < messages.size(); i += SugoConfig.MAX_MESSAGE_SIZE) {
            int endIndex = i + SugoConfig.MAX_MESSAGE_SIZE;
            endIndex = Math.min(endIndex, messages.size());
            List<JSONObject> batch = messages.subList(i, endIndex);

            if (batch.size() > 0) {
                String messagesString = dataString(batch);
                boolean accepted = sendData(messagesString);

                if (!accepted) {
                    throw new SugoServerException("Server refused to accept messages, they may be malformed.", batch);
                }
            }
        }
    }

    /**
     * 转成 JSONArray 的字符串
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
    private static String encodeDataString(String dataString) {
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
    /* package */ boolean sendData(String dataString) throws IOException {
        return mSender.sendData(dataString);
    }


    /************************************************************/

    public interface Sender {
        boolean sendData(String dataString) throws IOException;
    }

    public static class HttpSender implements Sender {

        private final String mEventsEndpoint;

        public HttpSender(String eventsEndpoint) {
            mEventsEndpoint = eventsEndpoint;
        }

        public boolean sendData(String dataString) throws IOException {
            URL endpoint = new URL(mEventsEndpoint);
            URLConnection conn = endpoint.openConnection();
            conn.setReadTimeout(READ_TIMEOUT_MILLIS);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf8");

            String encodedData = encodeDataString(dataString);
            String encodedQuery = "data=" + encodedData;

            OutputStream postStream = null;
            try {
                postStream = conn.getOutputStream();
                postStream.write(encodedQuery.getBytes());
            } finally {
                if (postStream != null) {
                    try {
                        postStream.close();
                    } catch (IOException e) {
                        // ignore, in case we've already thrown
                    }
                }
            }

            InputStream responseStream = null;
            String response = null;
            try {
                responseStream = conn.getInputStream();
                response = slurp(responseStream);
            } finally {
                if (responseStream != null) {
                    try {
                        responseStream.close();
                    } catch (IOException e) {
                        // ignore, in case we've already thrown
                    }
                }
            }

            return ((response != null) && response.equals("1"));
        }

        private String slurp(InputStream in) throws IOException {
            final StringBuilder out = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(in, "utf8");

            char[] readBuffer = new char[BUFFER_SIZE];
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
        private final SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss");

        static class FileDataWriter {

            private final String fileName;
            private final FileOutputStream outputStream;

            FileDataWriter(final String fileName) throws FileNotFoundException {
                this.outputStream = new FileOutputStream(fileName, true);
                this.fileName = fileName;
            }

            void close() {
                try {
                    outputStream.close();
//                    log.info("closed old file. [filename={}]", fileName);
                } catch (Exception e) {
//                    log.warn("fail to close output stream.", e);
                }
            }

            boolean isValid(final String fileName) {
                return this.fileName.equals(fileName);
            }

            boolean write(final StringBuilder sb) {
                boolean result = true;
                FileLock lock = null;
                try {
                    final FileChannel channel = outputStream.getChannel();
                    lock = channel.lock(0, Long.MAX_VALUE, false);
                    outputStream.write(sb.toString().getBytes("UTF-8"));
                } catch (Exception e) {
//                    log.warn("fail to write file.", e);
                    result = false;
                } finally {
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e) {
//                            log.warn("fail to release file lock.", e);
                        }
                    }
                }
                return result;
            }
        }

        private final String mMsgDirectory;

        public FileSender(String msgDirectory) {
            mMsgDirectory = msgDirectory;
        }

        public boolean sendData(String dataString) throws IOException {
            boolean result = true;
            if (dataString == null) {
                return false;
            }

            String file = generateFile();
            FileOutputStream outputStream = new FileOutputStream(file, false);
            FileLock lock = null;
            try {
                final FileChannel channel = outputStream.getChannel();
                lock = channel.lock(0, Long.MAX_VALUE, false);
                outputStream.write(dataString.getBytes("UTF-8"));
            } catch (IOException e) {
//                log.warn("fail to write file.", e);
                result = false;
            } finally {
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e) {
//                        log.warn("fail to release file lock.", e);
                    }
                }
                try {
                    outputStream.close();
//                    log.info("closed old file. [filename={}]", fileName);
                } catch (Exception e) {
//                    log.warn("fail to close output stream.", e);
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
            return dir + File.separator + time;
        }

    }

}
