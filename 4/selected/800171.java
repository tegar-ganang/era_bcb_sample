package com.sun.j2me.content;

import javax.microedition.content.ActionNameMap;
import com.sun.midp.i3test.TestCase;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Test RegistryStore functionality.
 * Test write and read of ContentHandlerImpl.
 */
public class TestRegReadWrite extends TestCase {

    /** Constant application ID for testing. */
    private static final int suiteId = 60000;

    /** The name of the GraphicalInstaller. */
    private static final String name = "Test Suite";

    /** Test ID. */
    private static final String ID = "TestID";

    /** The class of the GraphicalInstaller. */
    private static final String classname = "com.sun.content.test.CHStaticMIDlet";

    /** The types registered. */
    private String[] types = { "app/chs1", "app/chs2" };

    /** The suffixes registered. */
    private String[] suffixes = { ".chs1", ".chs2" };

    /** The actions registered. */
    private String[] actions = { "edit", "save", "send" };

    /** The english action names registered. */
    private String[] enActionNames = { "en-edit", "en-save", "en-send" };

    /** The french action names registered. */
    private String[] frActionNames = { "fr_CH-edit", "fr_CH-save", "fr_CH-send" };

    /** The action name maps to verify. */
    private String[][] actionmaps = { enActionNames, frActionNames };

    /** The locale supported. */
    private String[] actionlocales = { "en", "fr_CH" };

    /** The restricted access. */
    private String[] accessRestricted = { "me", "justme" };

    /** A zero length array of strings. */
    private String[] ZERO_STRINGS = new String[0];

    /** Empty ActionNameMap to return when needed. */
    private static final ActionNameMap[] ZERO_ACTIONNAMES = new ActionNameMap[0];

    /**
     * Run the tests.
     */
    public void runTests() {
        test001();
    }

    /**
     * Test that the built-in suite is registered.
     */
    void test001() {
        boolean b;
        ContentHandlerImpl[] chArr;
        ContentHandlerImpl chTst;
        String[] arr;
        String caller = accessRestricted[0];
        String badStr = "_";
        declare("Verify the write and read of RegistryStore");
        ContentHandlerImpl ch = new ContentHandlerImpl(ZERO_STRINGS, ZERO_STRINGS, ZERO_STRINGS, ZERO_ACTIONNAMES, null, ZERO_STRINGS, "");
        assertNotNull("Verify handler created", ch);
        b = RegistryStore.register(ch);
        assertFalse("Verify empty handler is not registered", b);
        ActionNameMap[] actionnames = new ActionNameMap[2];
        actionnames[0] = new ActionNameMap(actions, enActionNames, "en_US");
        actionnames[1] = new ActionNameMap(actions, frActionNames, "fr_CH");
        ch = new ContentHandlerImpl(types, suffixes, actions, actionnames, ID, accessRestricted, "authority");
        ch.storageId = suiteId;
        ch.classname = classname;
        ch.appname = name;
        b = RegistryStore.register(ch);
        assertTrue("Verify right handler is registered", b);
        chArr = RegistryStore.findHandler(caller, RegistryStore.FIELD_TYPES, ch.getType(0));
        if (chArr != null && chArr.length == 1) {
            assertEquals("Verify handler ID after search by type", ch.ID, chArr[0].ID);
        } else {
            fail("Verify search handler by type");
        }
        chArr = RegistryStore.findHandler(caller, RegistryStore.FIELD_TYPES, badStr);
        assertTrue("Verify empty search results by type", chArr == null || chArr.length == 0);
        chArr = RegistryStore.findHandler(caller, RegistryStore.FIELD_ID, ch.ID + ch.ID.substring(0, 3));
        if (chArr != null && chArr.length == 1) {
            assertEquals("Verify handler ID after partial search", ch.ID, chArr[0].ID);
        } else {
            fail("Verify handler ID after search by cut ID");
        }
        chTst = RegistryStore.getHandler(caller, ch.ID, RegistryStore.SEARCH_EXACT);
        if (chTst != null) {
            assertEquals("Verify handler search by ID exact", ch.ID, chTst.ID);
        } else {
            fail("Verify handler search by ID exact");
        }
        arr = RegistryStore.getValues(caller, RegistryStore.FIELD_TYPES);
        assertTrue("Verify getValues by type", arr != null && arr.length >= 2);
        b = testId(ch.ID);
        assertFalse("Verify test equal ID", b);
        b = testId(ch.ID.substring(0, 3));
        assertFalse("Verify test prefixed ID", b);
        b = testId(ch.ID + "qqq");
        assertFalse("Verify test prefixing ID", b);
        b = testId("qqq" + ch.ID);
        assertTrue("Verify test good ID", b);
        ContentHandlerImpl ch2 = RegistryStore.getHandler(null, ch.ID, RegistryStore.SEARCH_EXACT);
        assertNotNull("Verify loadHandler by ID", ch2);
        if (ch2 != null) {
            ch2.appname = ch.appname;
            ch2.version = ch.version;
            assertEquals("Verify loadHandler by ID", ch, ch2);
        }
        b = RegistryStore.unregister(ch.ID);
        assertTrue("Verify right handler is unregistered " + "/CHECK: if JSR 211 database has the correct format!/", b);
    }

    private boolean testId(String ID) {
        ContentHandlerImpl[] arr = RegistryStore.findHandler(null, RegistryStore.FIELD_ID, ID);
        return arr == null || arr.length == 0;
    }

    /**
     * Compare two Content Handlers.
     * @param msg describing the test
     * @param expected the expected ContentHandler
     * @param actual the actual ContentHandler
     */
    void assertEquals(String msg, ContentHandlerImpl expected, ContentHandlerImpl actual) {
        assertEquals("Verify storageId", expected.storageId, actual.storageId);
        assertEquals("Verify classname", expected.classname, actual.classname);
        assertEquals("Verify ID", expected.ID, actual.ID);
        assertTrue("Verify dynamic flag", expected.registrationMethod == actual.registrationMethod);
    }

    /**
     * Verify string arrays; asserting members are equals.
     * @param msg describing the test
     * @param expected the expected array of strings.
     * @param actual the actual array of strings.
     */
    void assertEquals(String msg, String[] expected, String[] actual) {
        assertEquals(msg + " length", expected.length, actual.length);
        int len = expected.length;
        if (len > actual.length) {
            len = actual.length;
        }
        for (int i = 0; i < len; i++) {
            assertEquals(msg + "[" + i + "]", expected[i], actual[i]);
        }
        for (int i = len; i < actual.length; i++) {
            assertEquals(msg + "[" + i + "]", null, actual[i]);
        }
        for (int i = len; i < expected.length; i++) {
            assertEquals(msg + "[" + i + "]", actual[i], null);
        }
    }

    /**
     * Verify that two ActionNameMaps are equal.
     * @param msg the message to print if a compare fails
     * @param expected the expected map
     * @param actual the actual map
     */
    void assertEquals(String msg, ActionNameMap expected, ActionNameMap actual) {
        assertEquals("Verify locale", expected.getLocale(), actual.getLocale());
        int size = expected.size();
        assertEquals(msg + " size ", size, actual.size());
        if (size == actual.size()) {
            for (int i = 0; i < size; i++) {
                assertEquals(msg + " action ", expected.getAction(i), actual.getAction(i));
                assertEquals(msg + " action name", expected.getActionName(i), actual.getActionName(i));
            }
        }
    }

    /**
     * Make a contenthandlerImpl with everything set.
     *
     * @return content handler object
     */
    ContentHandlerImpl makeFull() {
        ActionNameMap[] actionnames = new ActionNameMap[2];
        actionnames[0] = new ActionNameMap(actions, enActionNames, "en_US");
        actionnames[1] = new ActionNameMap(actions, frActionNames, "fr_CH");
        ContentHandlerImpl ch = new ContentHandlerImpl(types, suffixes, actions, actionnames, "ID-1", accessRestricted, "authority");
        ch.storageId = 61000;
        ch.appname = "Application Name";
        ch.version = "1.1.1";
        ch.classname = "classname";
        ch.registrationMethod = ContentHandlerImpl.REGISTERED_STATIC;
        return ch;
    }
}
