package edu.ucla.mbi.curator.actions.curator.ajax;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import edu.ucla.mbi.curator.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Mar 25, 2006
 * Time: 8:34:03 PM
 */
public class SendResponse extends Action {

    private Log log = LogFactory.getLog(SendResponse.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String responseString = (String) request.getAttribute("responseString");
        Integer bufferSize;
        try {
            bufferSize = Integer.valueOf((String) request.getAttribute("bufferSize"));
        } catch (ClassCastException cce) {
            bufferSize = Constants.DEFAULT_AJAX_BUFFER_OUTPUT_SIZE;
        } catch (NumberFormatException nfe) {
            bufferSize = Constants.DEFAULT_AJAX_BUFFER_OUTPUT_SIZE;
        }
        sendResponse(response, responseString, bufferSize);
        return null;
    }

    private void sendResponse(HttpServletResponse response, String responseString, Integer bufferSize) throws Exception {
        OutputStream out = response.getOutputStream();
        responseString = stripNewlinesAndTabs(responseString);
        StringBufferInputStream in = new StringBufferInputStream(responseString);
        response.setContentType("text/xml;charset=utf-8");
        byte[] buffer = new byte[bufferSize];
        int count = 0;
        while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
        out.close();
    }

    private String stripNewlinesAndTabs(String str) {
        char[] buf = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : buf) {
            if (c != '\t' && c != '\n') sb.append(c);
        }
        return sb.toString();
    }
}
