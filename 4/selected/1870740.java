package org.sgodden.echo.ext20.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;
import nextapp.echo.webcontainer.Connection;
import nextapp.echo.webcontainer.ContentType;
import nextapp.echo.webcontainer.Service;
import nextapp.echo.webcontainer.UserInstance;
import org.apache.commons.lang.StringEscapeUtils;
import org.sgodden.echo.ext20.models.RemoteAutocompleteModel;
import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

/**
 * Implementation of a Echo3 service for processing auto-complete field requests and returning
 * JSON-formatted data.
 * 
 * @author Lloyd Colling
 */
public class RemoteListModelService implements Service {

    /** <code>Service</code> identifier. */
    private static final String SERVICE_ID = "Echo3Ext20.RemoteListModel";

    /** Singleton instance of this <code>Service</code>. */
    public static final RemoteListModelService INSTANCE = new RemoteListModelService();

    /** List model identifier URL parameter. */
    private static final String PARAMETER_LIST_MODEL_UID = "rlm";

    /**
     * Parameter for the text to search on
     */
    private static final String PARAMETER_STARTS_WITH = "query";

    /**
     * Parameter for the number of results to return
     */
    private static final String PARAMETER_LIMIT = "limit";

    /**
     * Parameter for the start index of the results to return
     */
    private static final String PARAMETER_START = "start";

    /** URL parameters (used for creating URIs). */
    private static final String[] URL_PARAMETERS = new String[] { PARAMETER_LIST_MODEL_UID };

    /**
     * @see nextapp.echo.webcontainer.Service#getId()
     */
    public String getId() {
        return SERVICE_ID;
    }

    /**
     * @see nextapp.echo.webcontainer.Service#getVersion()
     */
    public int getVersion() {
        return DO_NOT_CACHE;
    }

    /**
     * Creates a URI to retrieve a specific list for a specific component 
     * from the server.
     * 
     * @param userInstance the relevant application user instance
     * @param listId the unique id to retrieve the list model from the
     *        <code>ContainerInstance</code>
     */
    public String createUri(UserInstance userInstance, String listId) {
        return userInstance.getServiceUri(this, URL_PARAMETERS, new String[] { listId });
    }

    /**
     * Renders the specified list model to the given connection.
     * 
     * @param conn the <code>Connection</code> on which to render the image
     * @param listModel the image to be rendered
     * @throws IOException if the list model cannot be rendered
     */
    public void renderModel(Connection conn, RemoteAutocompleteModel listModel, String startsWith, Integer maxResults, Integer startIndex) throws IOException {
        String[] data = listModel.getHits(startsWith, maxResults, startIndex);
        Integer totalResults = listModel.getHitCount(startsWith);
        JSONModel model = new JSONModel();
        JSONEntry[] entries = new JSONEntry[data.length];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new JSONEntry();
            entries[i].id = data[i];
            entries[i].popup = StringEscapeUtils.escapeHtml(data[i]);
            entries[i].value = data[i];
        }
        model.data = entries;
        model.size = totalResults;
        conn.getResponse().setStatus(HttpServletResponse.SC_OK);
        conn.setContentType(ContentType.TEXT_PLAIN);
        try {
            conn.getWriter().write(JSONMapper.toJSON(model).render(false));
        } catch (MapperException e) {
            throw new IOException("Failed to map the model to JSON");
        }
    }

    /**
     * Gets the list model with the specified id.
     * 
     * @param userInstance the <code>UserInstance</code> from which the list model was requested
     * @param listModelId the id of the list model
     * @return the list model if found, <code>null</code> otherwise.
     */
    public RemoteAutocompleteModel getModel(UserInstance userInstance, String listModelId) {
        return (RemoteAutocompleteModel) userInstance.getIdTable().getObject(listModelId);
    }

    /**
     * @see nextapp.echo.webcontainer.Service#service(nextapp.echo.webcontainer.Connection)
     */
    public void service(Connection conn) throws IOException {
        UserInstance userInstance = (UserInstance) conn.getUserInstance();
        if (userInstance == null) {
            serviceBadRequest(conn, "No container available.");
            return;
        }
        String listModelId = conn.getRequest().getParameter(PARAMETER_LIST_MODEL_UID);
        if (listModelId == null) {
            serviceBadRequest(conn, "List Model UID not specified.");
            return;
        }
        String query = conn.getRequest().getParameter(PARAMETER_STARTS_WITH);
        String limit = conn.getRequest().getParameter(PARAMETER_LIMIT);
        String start = conn.getRequest().getParameter(PARAMETER_START);
        if (query == null || limit == null || start == null) {
            InputStream in = conn.getRequest().getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int read = -1;
            do {
                read = in.read(buf);
                if (read > 0) {
                    baos.write(buf, 0, read);
                }
            } while (read > 0);
            String text = baos.toString();
            if (text != null) {
                Hashtable<String, String[]> table = HttpUtils.parseQueryString(text);
                if (query == null) {
                    String[] vals = table.get(PARAMETER_STARTS_WITH);
                    query = vals[0];
                }
                if (limit == null) {
                    String[] vals = table.get(PARAMETER_LIMIT);
                    limit = vals[0];
                }
                if (start == null) {
                    String[] vals = table.get(PARAMETER_START);
                    start = vals[0];
                }
            }
        }
        Integer limitVal = limit == null ? -1 : Integer.valueOf(limit);
        Integer startVal = start == null ? 0 : Integer.valueOf(start);
        RemoteAutocompleteModel imageReference = getModel(userInstance, listModelId);
        if (imageReference == null) {
            serviceBadRequest(conn, "List Model UID is not valid.");
            return;
        }
        renderModel(conn, imageReference, query, limitVal, startVal);
    }

    /**
     * Services a bad request.
     * 
     * @param conn the <code>Connection</code>
     * @param message the error message
     */
    private void serviceBadRequest(Connection conn, String message) {
        conn.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
        conn.setContentType(ContentType.TEXT_PLAIN);
        conn.getWriter().write(message);
    }

    public static class JSONModel {

        boolean success = true;

        JSONEntry[] data;

        Integer size;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public JSONEntry[] getData() {
            return data;
        }

        public void setData(JSONEntry[] data) {
            this.data = data;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }
    }

    public static class JSONEntry {

        String id;

        String popup;

        String value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getPopup() {
            return popup;
        }

        public void setPopup(String popup) {
            this.popup = popup;
        }
    }
}
