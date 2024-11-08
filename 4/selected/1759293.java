package daam;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.ServletLifecycle;
import org.json.JSONObject;
import daam.ui.ContainerComponent;
import daam.ui.FileUpload.UploadedFile;

public class DaamServlet extends DaamServletBase {

    @Override
    protected void beginRequest(HttpServletRequest req) {
        ServletLifecycle.beginRequest(req);
    }

    @Override
    protected void endRequest(HttpServletRequest req) {
        ServletLifecycle.endRequest(req);
    }

    @Override
    protected boolean handleFileUploads(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().startsWith("/fileupload")) {
            try {
                beginRequest(req);
                Map<String, String[]> parameters = req.getParameterMap();
                String containerName = parameters.get("container")[0];
                int controlId = Integer.parseInt(parameters.get("control")[0]);
                FileItemFactory factory = new DiskFileItemFactory(fileUploadThreshold, new File(fileUploadRepository));
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(req);
                FileItem item = items.get(0);
                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.name = item.getName();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final int BUFFER_SIZE = 10240;
                InputStream is = item.getInputStream();
                int read = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (0 < (read = is.read(buffer, 0, BUFFER_SIZE))) {
                    bos.write(buffer, 0, read);
                }
                uploadedFile.content = bos.toByteArray();
                List<JSONObject> eventsToSend = dispatchEvent(getApplication(req), containerName, controlId, "uploaded", uploadedFile);
                resp.setCharacterEncoding("UTF-8");
                PrintWriter pw = resp.getWriter();
                String response = eventsToSend.toString();
                System.out.println("responding: " + response);
                pw.write("<html>");
                pw.write(response);
            } catch (FileUploadException e) {
                throw new RuntimeException(e);
            } finally {
                endRequest(req);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void _log(String message) {
        log(message);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        serviceInner(req, resp);
    }

    @Override
    protected ContainerComponent getApplication(HttpServletRequest request) {
        return (ContainerComponent) Component.getInstance(rootContainerName);
    }

    @Override
    protected List<JSONObject> dispatchApplicaitonEvents(ContainerComponent component) {
        List<JSONObject> events = new Vector<JSONObject>();
        EventDispatcher dispatcher = (EventDispatcher) Component.getInstance(EventDispatcher.INSTANCE_NAME);
        if (dispatcher != null) events.addAll(dispatcher.fireEvents(component.getFiredEvents()));
        return events;
    }

    public List<JSONObject> dispatchEvent(ContainerComponent component, String containerName, int controlId, String event, Object data) {
        component = (ContainerComponent) Component.getInstance(containerName);
        List<JSONObject> events = component.receiveEvent(controlId, event, data);
        events.addAll(dispatchApplicaitonEvents(component));
        return events;
    }
}
