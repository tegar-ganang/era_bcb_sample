package annone.server.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import annone.engine.Channel;
import annone.engine.Engine;
import annone.engine.ids.ElementId;
import annone.http.HttpContext;
import annone.http.HttpNotFoundException;
import annone.http.HttpRequest;
import annone.http.HttpResponse;
import annone.http.HttpServer;
import annone.server.EndPoint;
import annone.util.Checks;

public class WebServer extends HttpServer {

    private final Map<String, PathBind> binds;

    private PathBind genericBind;

    private final Map<ElementId, String> elementIdToId;

    private final Map<String, ElementId> idToElementId;

    private final AtomicLong lastId;

    private final Map<String, Channel> channels;

    public WebServer() {
        this.binds = new TreeMap<String, PathBind>();
        this.elementIdToId = new IdentityHashMap<ElementId, String>();
        this.idToElementId = new HashMap<String, ElementId>();
        this.lastId = new AtomicLong();
        this.channels = new HashMap<String, Channel>();
        addBind("/", new RootBind());
        addBind("/c", new StyleSheetBind());
        addBind("/j", new JavascriptBind());
        addBind("/d", new DebugBind());
        setGenericBind(new ResourceBind());
    }

    @Override
    protected String getServerName() {
        return "Annone-Web-Server/1.0";
    }

    public Map<String, PathBind> getBinds() {
        return Collections.unmodifiableMap(binds);
    }

    public PathBind getBind(String path) {
        return binds.get(path);
    }

    public void addBind(String path, PathBind bind) {
        binds.put(path, bind);
    }

    public PathBind getGenericBind() {
        return genericBind;
    }

    public void setGenericBind(PathBind genericBind) {
        this.genericBind = genericBind;
    }

    protected Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    protected HttpContext createContext(HttpServer server, EndPoint endPoint, HttpRequest request, HttpResponse response) {
        return new WebContext(server, endPoint, request, response, Engine.getDefault());
    }

    @Override
    protected void perform(HttpContext context) {
        HttpRequest request = context.getRequest();
        HttpResponse response = context.getResponse();
        String path = request.getUri().getPath();
        path = (path == null) ? "/" : path.toLowerCase(Locale.US);
        PathBind bind = getBind(path);
        if (bind == null) bind = getGenericBind();
        if (bind != null) bind.perform((WebContext) context); else throw new HttpNotFoundException(String.format("Resource '%s' not found.", path));
    }

    public String elementIdToId(ElementId elementId) {
        Checks.notNull("elementId", elementId);
        String id = elementIdToId.get(elementId);
        if (id == null) {
            id = String.valueOf(lastId.incrementAndGet());
            elementIdToId.put(elementId, id);
        }
        return id;
    }

    public ElementId idToElementId(String id) {
        Checks.notEmpty("id", id);
        return idToElementId.get(id);
    }
}
