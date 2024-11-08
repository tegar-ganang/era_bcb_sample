package org.vrspace.vrmlclient;

import org.vrspace.util.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import vrml.external.*;
import vrml.external.field.*;
import vrml.external.exception.*;

public class NodeManager extends Observable implements EventOutObserver {

    public static final Hashtable primitives = new Hashtable();

    public static final Hashtable primitiveMap = new Hashtable();

    static {
        primitives.put("boolean", new Boolean(false));
        primitives.put("char", new Character(' '));
        primitives.put("byte", new Byte("0"));
        primitives.put("short", new Short("0"));
        primitives.put("int", new Integer(0));
        primitives.put("long", new Long(0));
        primitives.put("float", new Float(0));
        primitives.put("double", new Float(0));
        primitiveMap.put(Boolean.class, "boolean");
        primitiveMap.put(Character.class, "char");
        primitiveMap.put(Byte.class, "byte");
        primitiveMap.put(Short.class, "short");
        primitiveMap.put(Integer.class, "int");
        primitiveMap.put(Long.class, "long");
        primitiveMap.put(Float.class, "double");
        primitiveMap.put(Double.class, "double");
    }

    public Transform parent;

    public Node parentNode;

    public NodeManager childNode;

    public VRMLSceneManager applet;

    public Dispatcher dispatcher;

    public String name;

    public String className;

    public long id;

    public Node node;

    public String[] actualURL;

    public String[] url;

    protected boolean initialized = false;

    boolean parentMode = false;

    public org.vrspace.util.Queue requests = new org.vrspace.util.Queue();

    public Hashtable functionReturnTargets = new Hashtable();

    public EventOutMFNode children_changed;

    EventOutMFString init;

    EventOutSFTime inspect;

    EventInMFString systemIn;

    /**
   Records the last value for events sent by this node. (distributed eventOut)
   May be null; most objects don't send anything to the network.
   */
    Hashtable net_events;

    /**
   Records the last value for events sent to this node. (distributed and local eventIn)
   */
    Hashtable eventsState = new Hashtable();

    /** state of this node */
    public int state = 0;

    /** desired state */
    public int targetState = 0;

    /** reference count */
    public int references = 1;

    /**
   * Total number of nodes the Transform currently has as children.
   */
    public int size = 0;

    public NodeManager() {
    }

    public void setParameters(VRMLSceneManager applet, Transform parent, String className, long id) {
        this.applet = applet;
        dispatcher = applet.getDispatcher();
        this.className = className;
        this.id = id;
        this.parent = parent;
        if (parent != null) {
            parentNode = parent.node;
            parent.childNode = this;
        }
    }

    public String getID() {
        return className + " " + id;
    }

    public String getClassName() {
        return className;
    }

    public long getId() {
        return id;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getParentNode() {
        return parentNode;
    }

    /**
  Adds a network event to the queue
  */
    public void addEvent(Message req) {
        if (req.getEventName().equals("url")) {
            applet.sceneMgr.loadNode(req.getClassName(), req.getId(), req.getEventValue());
        } else {
            requests.add(req);
        }
    }

    /**
   * Creates new instance of <b>className</b>
   */
    public static NodeManager newInstance(String className) {
        Object obj = null;
        if (className.equals("Administrator") || className.equals("Alice")) {
            className = "User";
        }
        try {
            obj = Class.forName("org.vrspace.vrmlclient." + className).newInstance();
        } catch (Throwable e) {
            try {
                obj = Class.forName("org.vrspace.vrmlclient.object." + className).newInstance();
            } catch (Throwable t1) {
                return new NodeManager();
            }
        }
        return (NodeManager) obj;
    }

    public void load() {
        if (actualURL != null && actualURL.length > 0 && actualURL[0] != null && !actualURL[0].equals("vrspace:")) {
            applet.browser.createVrmlFromURL(url, parentNode, "children");
        } else {
            ((EventInMFNode) parentNode.getEventIn("addChildren")).setValue(applet.browser.createVrmlFromString(toVrml()));
        }
    }

    public void unload() {
        ((EventInMFNode) parentNode.getEventIn("removeChildren")).setValue(new Node[] { node });
    }

    /**
   * Set EventOut listener to EventOutMFString VRObject_Init.
   * Event value specifies events of interest.
   * Called from Transform.childrenChanged()
   */
    public synchronized void listen() {
        try {
            if (initialized) {
                Logger.logDebug(className + " " + id + ": listen aborted - already initialized");
                return;
            } else {
                initialized = true;
            }
            try {
                node.getEventOut("SystemOut").advise(this, "SystemOut");
            } catch (Exception e) {
            }
            try {
                systemIn = (EventInMFString) node.getEventIn("SystemIn");
            } catch (Exception e) {
            }
            if (node != null && node.getType().equals("Background")) {
                if (applet.bg != null) {
                    try {
                        ((EventInMFFloat) applet.bg.getEventIn("set_groundAngle")).setValue(((EventOutMFFloat) node.getEventOut("groundAngle_changed")).getValue());
                        ((EventInMFFloat) applet.bg.getEventIn("set_skyAngle")).setValue(((EventOutMFFloat) node.getEventOut("skyAngle_changed")).getValue());
                        ((EventInMFColor) applet.bg.getEventIn("set_groundColor")).setValue(((EventOutMFColor) node.getEventOut("groundColor_changed")).getValue());
                        ((EventInMFColor) applet.bg.getEventIn("set_skyColor")).setValue(((EventOutMFColor) node.getEventOut("skyColor_changed")).getValue());
                        ((EventInMFString) applet.bg.getEventIn("set_backUrl")).setValue(((EventOutMFString) node.getEventOut("backUrl_changed")).getValue());
                        ((EventInMFString) applet.bg.getEventIn("set_bottomUrl")).setValue(((EventOutMFString) node.getEventOut("bottomUrl_changed")).getValue());
                        ((EventInMFString) applet.bg.getEventIn("set_frontUrl")).setValue(((EventOutMFString) node.getEventOut("frontUrl_changed")).getValue());
                        ((EventInMFString) applet.bg.getEventIn("set_leftUrl")).setValue(((EventOutMFString) node.getEventOut("leftUrl_changed")).getValue());
                        ((EventInMFString) applet.bg.getEventIn("set_rightUrl")).setValue(((EventOutMFString) node.getEventOut("rightUrl_changed")).getValue());
                        ((EventInMFString) applet.bg.getEventIn("set_topUrl")).setValue(((EventOutMFString) node.getEventOut("topUrl_changed")).getValue());
                    } catch (Throwable t) {
                        Logger.logError(t);
                    }
                }
            }
            try {
                init = (EventOutMFString) node.getEventOut("VRObject_Init");
                init.advise(this, "VRObject_Init");
                EventInSFBool online = (EventInSFBool) node.getEventIn("VRObject_Online");
                online.setValue(true);
            } catch (Exception e) {
            }
            if (systemIn != null && parent != null) {
                systemIn.setValue(new String[] { "init", getClassName() + " " + getId(), parent.getClassName() + " " + parent.getId(), MyUser.myClass + " " + MyUser.myId });
            }
            try {
                EventInSFVec3f parentPos = (EventInSFVec3f) node.getEventIn("parent_pos");
                float[] tran = { parent.x, parent.y, parent.z };
                parentPos.setValue(tran);
            } catch (Exception e) {
            }
            try {
                EventInSFRotation parentRot = (EventInSFRotation) node.getEventIn("parent_rot");
                float[] rot = { parent.rotx, parent.roty, parent.rotz, parent.angle };
                parentRot.setValue(rot);
            } catch (Exception e) {
            }
            try {
                inspect = (EventOutSFTime) node.getEventOut("inspect");
                inspect.advise(this, "inspect");
            } catch (Exception e) {
            }
            if (url != null && url[0].indexOf('#') > 0) {
                try {
                    EventInMFString paramUrl = (EventInMFString) node.getEventIn("set_url");
                    String[] tmp = { url[0].substring(url[0].indexOf('#') + 1) };
                    paramUrl.setValue(tmp);
                } catch (Exception e) {
                }
            }
            if (parent != null && parent.hasBBox()) {
                try {
                    EventInSFVec3f set_bBoxCenter = (EventInSFVec3f) node.getEventIn("set_bBoxCenter");
                    EventInSFVec3f set_bBoxSize = (EventInSFVec3f) node.getEventIn("set_bBoxSize");
                    set_bBoxCenter.setValue(parent.bBoxCenter);
                    set_bBoxSize.setValue(parent.bBoxSize);
                } catch (Exception e) {
                }
            }
        } catch (Throwable e) {
            Logger.logDebug(className + " " + id + " passive");
            Logger.logError(e);
        }
    }

    /**
  Send all queued events to the VRML Node
  */
    public void flush() {
        while (requests.size() > 0) {
            send((Message) requests.remove());
        }
    }

    /**
   * Send an event specified by <req> to the VRML Node.
   * Supported event types:
   * SFBool, SFInt32, SFFloat, SFString, SFTime, SFVec3f, SFRotation, SFColor, SFVec2f
   * MFInt32, MFFloat, MFString, MFTime
   *
   * Should this method be synchronized to ensure event ordering?
  */
    public void send(Message req) {
        boolean isFunctionCall = false;
        String eventName = req.getEventName();
        String eventValue = req.getEventValue();
        try {
            eventsState.put(req.getEventName(), req.getEventValue());
            if (req instanceof Request) {
                Request request = (Request) req;
                if (eventName.equals("get")) {
                    Enumeration e = eventsState.keys();
                    String toReturn = "";
                    while (e.hasMoreElements()) {
                        Object o = e.nextElement();
                        if (((String) o).equals(eventValue)) {
                            toReturn = (String) eventsState.get(o);
                        }
                    }
                    Message messageToSend = new Message(request.getCaller() + " " + request.getCallerMethod() + " " + toReturn);
                    dispatcher.dispatch(messageToSend);
                    return;
                }
                functionReturnTargets.put(req.getEventName(), request.getCaller() + " " + request.getCallerMethod());
                isFunctionCall = true;
            }
            EventIn ev = null;
            try {
                ev = node.getEventIn("set_" + req.getEventName());
            } catch (Exception e) {
                ev = node.getEventIn(req.getEventName());
            }
            if (ev instanceof EventInSFString) {
                if (req.getEventName().equals("name")) {
                    name = req.getEventValue();
                }
                ((EventInSFString) ev).setValue(req.getEventValue());
            } else if (ev instanceof EventInSFVec3f) {
                float[] tmp = { 0, 0, 0 };
                tmp[0] = new Float(req.getArguments()[0]).floatValue();
                tmp[1] = new Float(req.getArguments()[1]).floatValue();
                tmp[2] = new Float(req.getArguments()[2]).floatValue();
                ((EventInSFVec3f) ev).setValue(tmp);
            } else if (ev instanceof EventInSFRotation) {
                float[] tmp = { 0, 0, 0, 0 };
                tmp[0] = new Float(req.getArguments()[0]).floatValue();
                tmp[1] = new Float(req.getArguments()[1]).floatValue();
                tmp[2] = new Float(req.getArguments()[2]).floatValue();
                tmp[3] = new Float(req.getArguments()[3]).floatValue();
                ((EventInSFRotation) ev).setValue(tmp);
            } else if (ev instanceof EventInSFInt32) {
                ((EventInSFInt32) ev).setValue(new Integer(req.getEventValue()).intValue());
            } else if (ev instanceof EventInSFFloat) {
                ((EventInSFFloat) ev).setValue(new Float(req.getEventValue()).floatValue());
            } else if (ev instanceof EventInSFBool) {
                if (req.getEventValue().equals("true")) {
                    ((EventInSFBool) ev).setValue(true);
                } else {
                    ((EventInSFBool) ev).setValue(false);
                }
            } else if (ev instanceof EventInSFColor) {
                float[] tmp = { 0, 0, 0 };
                tmp[0] = new Float(req.getArguments()[0]).floatValue();
                tmp[1] = new Float(req.getArguments()[1]).floatValue();
                tmp[2] = new Float(req.getArguments()[2]).floatValue();
                ((EventInSFColor) ev).setValue(tmp);
            } else if (ev instanceof EventInSFVec2f) {
                float[] tmp = { 0, 0 };
                tmp[0] = new Float(req.getArguments()[0]).floatValue();
                tmp[1] = new Float(req.getArguments()[1]).floatValue();
                ((EventInSFVec3f) ev).setValue(tmp);
            } else if (ev instanceof EventInSFTime) {
                ((EventInSFTime) ev).setValue(new Double(req.getEventValue()).doubleValue());
            } else if (ev instanceof EventInMFInt32) {
                int[] tmp = (int[]) stringToArray(null, req.getEventValue());
                ((EventInMFInt32) ev).setValue(tmp);
            } else if (ev instanceof EventInMFFloat) {
                float[] tmp = (float[]) stringToArray(null, req.getEventValue());
                ((EventInMFFloat) ev).setValue(tmp);
            } else if (ev instanceof EventInMFString) {
                String[] tmp = (String[]) stringToArray(null, req.getEventValue());
                ((EventInMFString) ev).setValue(tmp);
            } else if (ev instanceof EventInMFTime) {
                double[] tmp = (double[]) stringToArray(null, req.getEventValue());
                ((EventInMFTime) ev).setValue(tmp);
            } else if (ev instanceof EventInMFNode) {
                String[] tmp = (String[]) stringToArray(null, req.getEventValue());
                Node[] nodes = new Node[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    NodeManager nodeManager = applet.getSceneManager().getNodeManager(tmp[i]);
                    if (eventName.equals("addChildren") || eventName.equals("removeChildren")) {
                        if (nodeManager.getParentNode() == null) {
                            nodes[i] = nodeManager.getNode();
                            if (eventName.equals("addChildren")) {
                                try {
                                    nodeManager.parent = (Transform) this;
                                } catch (Exception e) {
                                    Logger.logError("Error adding/removing children for: " + toString() + " Parent is not a transform. ");
                                }
                            } else {
                                nodeManager.parent = null;
                            }
                        } else {
                            nodes[i] = nodeManager.getParentNode();
                            if (eventName.equals("addChildren")) {
                                try {
                                    nodeManager.parent.parent = (Transform) this;
                                } catch (Exception e) {
                                    Logger.logError("Error adding/removing children for: " + toString() + " Parent is not a transform. ");
                                }
                            } else {
                                nodeManager.parent.parent = null;
                            }
                        }
                    } else {
                        nodes[i] = nodeManager.getNode();
                    }
                }
                ((EventInMFNode) ev).setValue(nodes);
            } else {
                Logger.logError(className + " " + id + " Unsupported field type: " + ev.getType() + " " + ev.getClass().getName());
                return;
            }
            Util.sleep(10);
        } catch (Exception e) {
            if (isFunctionCall) {
                functionReturnTargets.remove(req.getEventName());
            }
            if (childNode != null) {
                childNode.send(req);
            }
        }
    }

    /**
   * Converts String in arrayToString() format back to array.
   */
    public static Object stringToArray(Class cl, String val) {
        Object ret = null;
        StringTokenizer st = new StringTokenizer(val, "[()]", true);
        String token = null;
        boolean lastWasStart = false;
        try {
            Vector vec = new Vector();
            if (!st.hasMoreTokens()) {
                return new Object[0];
            }
            st.nextToken();
            String className = st.nextToken();
            Class c = null;
            try {
                Object primitive = (Object) primitives.get(className);
                if (primitive == null) {
                    c = Class.forName(className);
                } else {
                    c = primitive.getClass();
                }
                Class[] params = { String.class };
                Constructor constructor = c.getConstructor(params);
                Object[] args = new Object[1];
                Constructor emptyConstructor = null;
                Object[] noArgs = {};
                try {
                    emptyConstructor = c.getConstructor(new Class[] {});
                } catch (Exception e) {
                }
                while (st.hasMoreElements()) {
                    token = st.nextToken();
                    if (token.equals("(")) {
                        lastWasStart = true;
                    } else if (token.equals(")")) {
                        if (lastWasStart && emptyConstructor != null) {
                            vec.addElement(emptyConstructor.newInstance(noArgs));
                        }
                        lastWasStart = false;
                    } else if (token.equals("]")) {
                    } else {
                        args[0] = token;
                        vec.addElement(constructor.newInstance(args));
                        lastWasStart = false;
                    }
                }
                if (primitive != null) {
                    ret = Array.newInstance((Class) primitive.getClass().getField("TYPE").get(null), vec.size());
                } else {
                    ret = Array.newInstance(c, vec.size());
                }
                for (int i = 0; i < vec.size(); i++) {
                    Array.set(ret, i, vec.elementAt(i));
                }
            } catch (Exception e) {
                Logger.logError("Error constructing array", e);
            }
        } catch (NoSuchElementException e) {
            Logger.logError("Error processing " + val, e);
        }
        return ret;
    }

    public boolean isLoaded() {
        return url != null;
    }

    public synchronized void callback(EventOut ev, double timestamp, Object userdata) {
        try {
            if (userdata instanceof Long) {
                if (ev instanceof EventOutMFNode) {
                    childrenChanged(((EventOutMFNode) ev).getValue());
                }
            } else if (userdata.equals("VRObject_Init")) {
                String[] events = ((EventOutMFString) ev).getValue();
                handleDeclareEvents(events);
                flush();
            } else if (userdata.equals("logDebug")) {
                Logger.logDebug(className + " " + id + ": " + ((EventOutSFString) ev).getValue());
            } else if (userdata.equals("logInfo")) {
                Logger.logInfo(className + " " + id + ": " + ((EventOutSFString) ev).getValue());
            } else if (userdata.equals("logWarning")) {
                Logger.logWarning(className + " " + id + ": " + ((EventOutSFString) ev).getValue());
            } else if (userdata.equals("logError")) {
                Logger.logError(className + " " + id + ": " + ((EventOutSFString) ev).getValue());
            } else if (userdata.equals("parent")) {
                parentMode = ((EventOutSFBool) ev).getValue();
            } else if (userdata.equals("browserLoadURL")) {
                String[] urls = { ((EventOutSFString) ev).getValue() };
                String[] parameters = { "target=_top" };
                applet.browser.loadURL(urls, parameters);
            } else if (userdata.equals("gateToVRSpace")) {
                Logger.stopStaticLogger();
                String url = applet.rewriteURL(((EventOutSFString) ev).getValue() + applet.getLogin() + "+" + applet.getPassword());
                applet.getConnection().close();
                applet.getApplet().getAppletContext().showDocument(new URL(url));
            } else if (userdata.equals("SystemOut")) {
                handleSystemOut((String[]) ((EventOutMFString) ev).getValue(), timestamp);
            } else {
                try {
                    Message req = null;
                    if (parentMode) {
                        req = new Message("Transform " + parent.id + " " + userdata + " " + toString(ev));
                    } else {
                        req = new Message(className + " " + id + " " + userdata + " " + toString(ev));
                    }
                    applet.getSceneManager().update(this, req);
                } catch (Throwable e) {
                    setChanged();
                    notifyObservers(e);
                }
            }
        } catch (Throwable t) {
            Logger.logError(t);
        }
    }

    public void handleSystemOut(String[] request, double timestamp) {
        String command = request[0];
        if (command.equals("route")) {
            dispatcher.dispatch(new Message(request[2] + " " + packageEventValue(request[1], request, 3)));
        } else if (command.equals("routeRemote")) {
            Message message = new Message(request[2] + " " + packageEventValue(request[1], request, 3));
            setChanged();
            notifyObservers(message);
        } else if (command.equals("function")) {
            dispatcher.dispatch(new Request(getClassName() + " " + getId(), request[1], request[3] + " " + packageEventValue(request[2], request, 4)));
        } else if (command.equals("return")) {
            Logger.logDebug((String) functionReturnTargets.get(request[1]) + " " + packageEventValue(request[2], request, 3));
            dispatcher.dispatch(new Message((String) functionReturnTargets.get(request[1]) + " " + packageEventValue(request[2], request, 3)));
            functionReturnTargets.remove(request[1]);
        } else if (command.equals("declareEvents")) {
            String[] events = new String[request.length - 1];
            for (int i = 0; i < request.length - 1; i++) {
                events[i] = request[i + 1];
            }
            handleDeclareEvents(events);
            flush();
        }
    }

    private void handleDeclareEvents(String[] events) {
        net_events = new Hashtable();
        for (int i = 0; i < events.length; i++) {
            try {
                EventOut event = node.getEventOut(events[i]);
                net_events.put(events[i], event);
                event.advise(this, events[i]);
            } catch (Throwable t) {
                Logger.logInfo(className + " " + id + ": EventOut " + events[i] + ": " + t.toString());
            }
        }
    }

    protected String packageEventValue(String type, String[] values, int offset) {
        if (values.length == offset) return "";
        StringBuffer packaged = new StringBuffer();
        if (type.substring(0, 2).equals("SF")) {
            packaged.append(values[offset]);
        } else {
            if (type.equals("MFNode") || type.equals("SFNode")) {
                packaged.append("[java.lang.String");
            } else {
                if (type.equals("MFString")) {
                    packaged.append("[java.lang.String");
                } else if (type.equals("MFInt32")) {
                    packaged.append("[int");
                } else if (type.equals("MFFloat")) {
                    packaged.append("[double");
                }
            }
            for (int i = offset; i < values.length; i++) {
                packaged.append("(" + values[i] + ")");
            }
            packaged.append("]");
        }
        return packaged.toString();
    }

    /**
  Returns string representation of an EventOut, in order to send it to the
  server.
  Supported event types:
  SFBool, SFInt32, SFFloat, SFString, SFTime, SFVec3f, SFRotation, SFColor, SFVec2f
  MFInt32, MFFloat, MFString, MFTime
  */
    public String toString(EventOut ev) {
        String ret = null;
        if (ev instanceof EventOutSFString) {
            ret = ((EventOutSFString) ev).getValue();
        } else if (ev instanceof EventOutSFVec3f) {
            float[] tmp = ((EventOutSFVec3f) ev).getValue();
            ret = tmp[0] + " " + tmp[1] + " " + tmp[2];
        } else if (ev instanceof EventOutSFRotation) {
            float[] tmp = ((EventOutSFRotation) ev).getValue();
            ret = tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + tmp[3];
        } else if (ev instanceof EventOutSFInt32) {
            ret = "" + ((EventOutSFInt32) ev).getValue();
        } else if (ev instanceof EventOutSFFloat) {
            ret = "" + ((EventOutSFFloat) ev).getValue();
        } else if (ev instanceof EventOutSFColor) {
            float[] tmp = ((EventOutSFColor) ev).getValue();
            ret = tmp[0] + " " + tmp[1] + " " + tmp[2];
        } else if (ev instanceof EventOutSFBool) {
            ret = "" + ((EventOutSFBool) ev).getValue();
        } else if (ev instanceof EventOutSFVec2f) {
            float[] tmp = ((EventOutSFVec2f) ev).getValue();
            ret = tmp[0] + " " + tmp[1];
        } else if (ev instanceof EventOutSFTime) {
            ret = "" + ((EventOutSFTime) ev).getValue();
        } else if (ev instanceof EventOutMFInt32) {
            int[] tmp = (int[]) ((EventOutMFInt32) ev).getValue();
            ret = arrayToString(tmp);
        } else if (ev instanceof EventOutMFFloat) {
            float[] tmp = (float[]) ((EventOutMFFloat) ev).getValue();
            ret = arrayToString(tmp);
        } else if (ev instanceof EventOutMFString) {
            String[] tmp = (String[]) ((EventOutMFString) ev).getValue();
            ret = arrayToString(tmp);
        } else if (ev instanceof EventOutMFTime) {
            double[] tmp = (double[]) ((EventOutMFTime) ev).getValue();
            ret = arrayToString(tmp);
        } else {
            Logger.logError(className + " " + id + " Unsupported field type: " + ev.getType() + " " + ev.getClass().getName());
        }
        return ret;
    }

    public static String arrayToString(Object array) {
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Argument is not an array!");
        }
        StringBuffer ret = new StringBuffer();
        String name = array.getClass().getName();
        int len = Array.getLength(array);
        if (len > 0) {
            if (name.length() > 2) {
                name = name.substring(2, name.length() - 1);
            } else {
                Object obj = Array.get(array, 0);
                name = (String) primitiveMap.get(obj.getClass());
            }
            try {
                ret.append("[");
                ret.append(name);
                for (int i = 0; i < len; i++) {
                    ret.append("(" + Array.get(array, i) + ")");
                }
                ret.append("]");
            } catch (NullPointerException e) {
                ret.append("[()]");
            }
        }
        return ret.toString();
    }

    /**
  returns this node name
  */
    public String getName() {
        if (name == null && childNode != null) {
            return childNode.getName();
        }
        return name;
    }

    void setInit(boolean init) {
        if (childNode != null) {
            childNode.setInit(init);
        }
        initialized = init;
    }

    public synchronized void setState(int state) {
        this.state = state;
    }

    public synchronized void setTargetState(int state) {
        this.targetState = state;
    }

    public String toString() {
        String rep = "";
        rep = className + " " + id;
        if (url != null) {
            rep += " URL: " + url[0];
        }
        if (childNode != null) {
            rep += " Child: " + childNode.toString();
        }
        return rep;
    }

    /**
   * called from callback() or loader when nodes are loaded
   */
    public void childrenChanged(Node[] children) {
    }

    public void selected(boolean state) {
    }

    public String toVrml() {
        StringBuffer ret = new StringBuffer();
        toVrml(ret);
        return ret.toString();
    }

    public void toVrml(StringBuffer ret) {
        ret.append("DEF ");
        ret.append(this.className);
        ret.append("_");
        ret.append(this.id);
        ret.append(" ");
        if (url != null && url.length > 0) {
            ret.append("Inline { url [ ");
            for (int i = 0; i < actualURL.length; i++) {
                ret.append("\"");
                ret.append(actualURL[i]);
                ret.append("\",");
            }
            for (int i = 0; i < url.length; i++) {
                ret.append("\"");
                ret.append(url[i]);
                ret.append("\",");
            }
            for (int i = 0; i < actualURL.length - 1; i++) {
                ret.append("\"http://www.vrspace.org/vrspace/");
                ret.append(actualURL[i]);
                ret.append("\",");
            }
            if (actualURL.length > 0) {
                ret.append("\"http://www.vrspace.org/vrspace/");
                ret.append(actualURL[actualURL.length - 1]);
                ret.append("\"");
            }
            ret.append(" ] }");
        } else {
            ret.append("Group{}");
        }
    }
}
