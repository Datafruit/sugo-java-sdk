package io.sugo.sugojavasdk;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Unit test for simple App.
 */
public class SugoAPITest extends TestCase {

    private MessageBuilder mBuilder;
    private JSONObject mSampleProps;
    private String mEventsMessages;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SugoAPITest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(SugoAPITest.class);
    }

    @Override
    public void setUp() {
        mBuilder = new MessageBuilder("a token");

        try {
            mSampleProps = new JSONObject();
            mSampleProps.put("prop key", "prop value");
        } catch (JSONException e) {
            throw new RuntimeException("Error in test setup");
        }

        SugoAPI api = new SugoAPI(new SugoAPI.ConsoleSender()) {
            @Override
            boolean senderSendData(String dataString) {
                mEventsMessages = dataString;
                return true;
            }
        };

        MessagePackage msgPkg = new MessagePackage();

        JSONObject event = mBuilder.event("a distinct id", "login", mSampleProps);
        msgPkg.addMessage(event);

        try {
            api.sendMessages(msgPkg);
        } catch (IOException e) {
            throw new RuntimeException("Impossible IOException", e);
        }

    }

    public void testMessageFormat() {
        MessagePackage c = new MessagePackage();
        assertFalse(c.isValidMessage(mSampleProps));

        JSONObject event = mBuilder.event("a distinct id", "login", mSampleProps);
        assertTrue(c.isValidMessage(event));

    }

    public void testEmptyMessageFormat() {
        MessagePackage c = new MessagePackage();
        JSONObject eventMessage = mBuilder.event("a distinct id", "empty event", null);
        assertTrue(c.isValidMessage(eventMessage));
    }

    public void testValidate() {
        MessagePackage c = new MessagePackage();
        JSONObject event = mBuilder.event("a distinct id", "login", mSampleProps);
        assertTrue(c.isValidMessage(event));
        try {
            JSONObject rebuiltMessage = new JSONObject(event.toString());
            assertTrue(c.isValidMessage(rebuiltMessage));
            assertEquals(c.getEventsMessages().size(), 0);
            c.addMessage(rebuiltMessage);
            assertEquals(c.getEventsMessages().size(), 1);
        } catch (JSONException e) {
            fail("Failed to build JSONObject");
        }
    }

    public void testClientDelivery() {
        MessagePackage c = new MessagePackage();
        try {
            c.addMessage(mSampleProps);
            fail("addMessage did not throw");
        } catch (SugoMessageException e) {
            // This is expected, we pass
        }

        try {
            JSONObject event = mBuilder.event("a distinct id", "login", mSampleProps);
            c.addMessage(event);

        } catch (SugoMessageException e) {
            fail("Threw exception on valid message");
        }
    }

    public void testApiSendEvent() {
        try {
            JSONArray messageArray = new JSONArray(mEventsMessages);
            assertTrue("Only one message sent", messageArray.length() == 1);

            JSONObject eventSent = messageArray.getJSONObject(0);
            String eventName = eventSent.getString("event");
            assertTrue("Event name had expected value", "login".equals(eventName));

            JSONObject eventProps = eventSent.getJSONObject("properties");
            String propValue = eventProps.getString("prop key");
            assertTrue("Property had expected value", "prop value".equals(propValue));
        } catch (JSONException e) {
            fail("Data message can't be interpreted as expected: " + mEventsMessages);
        }
    }

    public void testEmptyDelivery() {
        SugoAPI api = new SugoAPI(new SugoAPI.ConsoleSender()) {
            @Override
            public boolean senderSendData(String dataString) {
                fail("Data sent when no data should be sent");
                return true;
            }
        };

        MessagePackage c = new MessagePackage();
        try {
            api.sendMessages(c);
        } catch (IOException e) {
            throw new RuntimeException("Apparently impossible IOException thrown", e);
        }
    }

    public void testLargeDelivery() {
        final List<String> sends = new ArrayList<String>();

        SugoAPI api = new SugoAPI(new SugoAPI.ConsoleSender()) {
            @Override
            public boolean senderSendData(String dataString) {
                sends.add(dataString);
                return true;
            }
        };

        MessagePackage c = new MessagePackage();
        int expectLeftovers = SugoConfig.MAX_MESSAGE_SIZE - 1;
        int totalToSend = (SugoConfig.MAX_MESSAGE_SIZE * 2) + expectLeftovers;
        for (int i = 0; i < totalToSend; i++) {
            Map<String, Integer> propsMap = new HashMap<String, Integer>();
            propsMap.put("count", i);
            JSONObject props = new JSONObject(propsMap);
            JSONObject message = mBuilder.event("a distinct id", "counted", props);
            c.addMessage(message);
        }

        try {
            api.sendMessages(c);
        } catch (IOException e) {
            throw new RuntimeException("Apparently impossible IOException", e);
        }

        assertTrue("More than one message", sends.size() == 3);

        try {
            JSONArray firstMessage = new JSONArray(sends.get(0));
            assertTrue("First message has max elements", firstMessage.length() == SugoConfig.MAX_MESSAGE_SIZE);

            JSONArray secondMessage = new JSONArray(sends.get(1));
            assertTrue("Second message has max elements", secondMessage.length() == SugoConfig.MAX_MESSAGE_SIZE);

            JSONArray thirdMessage = new JSONArray(sends.get(2));
            assertTrue("Third message has all leftover elements", thirdMessage.length() == expectLeftovers);
        } catch (JSONException e) {
            fail("Can't interpret sends appropriately when sending large messages");
        }
    }

    public void testEncodeDataString() {
        SugoAPI api = new SugoAPI(new SugoAPI.ConsoleSender()) {
            @Override
            public boolean senderSendData(String dataString) {
                fail("Data sent when no data should be sent");
                return true;
            }
        };

        try {
            api.encodeDataString(null);
            fail("encodeDataString doesn't accept null string");
        } catch (NullPointerException e) {
            // ok
        }

        // empty string
        assertEquals("", api.encodeDataString(""));
        // empty JSON
        assertEquals("e30%3D", api.encodeDataString(new JSONObject().toString()));
        // empty Array
        assertEquals("W10%3D", api.encodeDataString(new JSONArray().toString()));
        // JSON Object
        assertEquals("eyJwcm9wIGtleSI6InByb3AgdmFsdWUifQ%3D%3D", api.encodeDataString(mSampleProps.toString()));
        // JSON Array
        JSONArray jsonArray = new JSONArray(Arrays.asList(mSampleProps));
        assertEquals("W3sicHJvcCBrZXkiOiJwcm9wIHZhbHVlIn1d", api.encodeDataString(jsonArray.toString()));
    }

}
