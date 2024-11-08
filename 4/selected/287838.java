package edu.ucla.mbi.curator.actions.curator.file;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import edu.ucla.mbi.curator.webutils.session.SessionManager;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.EntrySet;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Jan 4, 2006
 * Time: 2:40:17 PM
 */
public class ViewFile extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SessionManager sessionManager = SessionManager.getSessionManager(request);
        EntrySet entrySet = sessionManager.getEntrySet();
        if (entrySet.getEntryList().get(0).getSource() == null) new AddSourceToFile().execute(mapping, form, request, response);
        String file = entrySet.toXML();
        if (file.length() > 0) {
            StringBuffer stringBuffer = new StringBuffer(file);
            OutputStream output = response.getOutputStream();
            response.setContentLength(stringBuffer.length());
            response.setContentType("text/xml;charset=utf-8");
            StringBufferInputStream input = new StringBufferInputStream(stringBuffer.toString());
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            output.close();
        }
        return null;
    }
}
