package net.ontopia.topicmaps.webed;

import net.ontopia.topicmaps.webed.impl.basic.Constants;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;

/**
 * INTERNAL: Tests for the <webed:form> tag.
 */
public class FormTagTest extends AbstractWebBasedTestCase {

    public FormTagTest(String aName) {
        super(aName);
    }

    public void testRelativeActionURI() throws Exception {
        WebResponse resp = wc.getResponse(webedTestLocation + "/test/FormTag/testRelativeActionURI.jsp");
        WebForm form = resp.getForms()[0];
        assertEquals("Incorrect action", "TestTarget", form.getAction());
    }

    public void testAbsoluteActionURI() throws Exception {
        WebResponse resp = wc.getResponse(webedTestLocation + "/test/FormTag/testAbsoluteActionURI.jsp");
        WebForm form = resp.getForms()[0];
        assertEquals("Incorrect action", "/TestTarget", form.getAction());
    }

    public void testAttributes() throws Exception {
        WebResponse resp = wc.getResponse(webedTestLocation + "/test/FormTag/testAttributes.jsp");
        WebForm form = resp.getForms()[0];
        Node formNode = form.getDOMSubtree();
        checkType(formNode, "form");
        checkAttribute(formNode, "id", "ID");
        checkAttribute(formNode, "method", "POST");
        checkAttribute(formNode, "name", Constants.FORM_EDIT_NAME);
        checkAttribute(formNode, "action", webedTestApplication + "/process");
        checkAttribute(formNode, "onsubmit", "return true;");
        checkForExtraAttributes(formNode);
        Node tm = getNthElementChild(formNode, 0);
        checkType(tm, "input");
        checkAttribute(tm, "type", "hidden");
        checkAttribute(tm, "name", "tm");
        checkAttribute(tm, "value", "test.ltm");
        checkForExtraAttributes(tm);
        Node actionGroup = getNthElementChild(formNode, 1);
        checkType(actionGroup, "input");
        checkAttribute(actionGroup, "type", "hidden");
        checkAttribute(actionGroup, "name", "ag");
        checkAttribute(actionGroup, "value", "testActionGroup");
        checkForExtraAttributes(actionGroup);
        Node requestId = getNthElementChild(formNode, 2);
        checkType(requestId, "input");
        checkAttribute(requestId, "type", "hidden");
        checkAttribute(requestId, "name", "requestid");
        checkAttributeStartsWith(requestId, "value", "rid");
        checkForExtraAttributes(requestId);
        Node linkForward = getNthElementChild(formNode, 3);
        checkType(requestId, "input");
        checkAttribute(linkForward, "type", "hidden");
        checkAttribute(linkForward, "name", "linkforward");
        checkAttributeStartsWith(linkForward, "id", "linkforwardrid");
        checkForExtraAttributes(linkForward);
        assertNull("Unexpected element", getNthElementChild(formNode, 4));
        form.submit();
        assertEquals("Incorrect Result", webedTestApplication + "/test/defaultForward.html", wc.getCurrentPage().getURL().getPath());
    }

    public void testReadonlyFalse() throws Exception {
        WebResponse success = wc.getResponse(webedTestLocation + "/test/FormTag/testReadonlyFalse.jsp");
        assertTrue("No form element found in read-write page", success.getForms().length == 1);
    }
}
