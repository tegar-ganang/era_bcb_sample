import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

class KBEpgItemsRes extends HTTPResponse {

    public KBEpgItemsRes() throws Exception {
        super();
    }

    public void getResponse(HTTPurl urlData, OutputStream outStream, HashMap<String, String> headers) throws Exception {
        if ("01".equals(urlData.getParameter("action"))) {
            outStream.write(showNames(urlData, headers));
            return;
        } else if ("02".equals(urlData.getParameter("action"))) {
            outStream.write(showItemInstances(urlData, headers));
            return;
        } else if ("03".equals(urlData.getParameter("action"))) {
            outStream.write(showSearchForm(urlData, headers));
            return;
        } else if ("04".equals(urlData.getParameter("action"))) {
            outStream.write(showSearchResults(urlData, headers));
            return;
        } else {
            outStream.write(showFirstLetter(urlData, headers));
            return;
        }
    }

    private byte[] showItemInstances(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        String name = urlData.getParameter("name");
        if (name == null) name = "";
        String start = urlData.getParameter("start");
        if (start == null) start = "0";
        String show = urlData.getParameter("show");
        if (show == null) show = "10";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("title", name);
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        root.setAttribute("back", "/servlet/ApplyTransformRes?xml=epg-index&xsl=kb-buttons");
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("mainurl");
        text = doc.createTextNode("/servlet/KBEpgItemsRes?action=02&name=" + URLEncoder.encode(name, "UTF-8") + "&");
        button.appendChild(text);
        root.appendChild(button);
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM d h:mm aa");
        GuideStore guide = GuideStore.getInstance();
        Vector<String[]> chanMap = guide.getChannelMap();
        int total = 0;
        for (int q = 0; q < chanMap.size(); q++) {
            String[] map = (String[]) chanMap.get(q);
            GuideItem[] items = guide.getItems(name, map[1]);
            for (int x = 0; x < items.length; x++) {
                String butText = df.format(items[x].getStart());
                if (map[0] != null) butText += " (" + map[0] + ")"; else butText += " (Not Mapped)";
                button = doc.createElement("button");
                button.setAttribute("name", butText);
                elm = doc.createElement("url");
                Calendar cal = Calendar.getInstance();
                cal.setTime(items[x].getStart());
                String buttonUrl = "/servlet/KBEpgDataRes?action=05&channel=" + URLEncoder.encode(map[0], "UTF-8") + "&id=" + items[x].toString();
                text = doc.createTextNode(buttonUrl);
                elm.appendChild(text);
                button.appendChild(elm);
                root.appendChild(button);
                total++;
            }
        }
        root.setAttribute("total", new Integer(total).toString());
        XSL transformer = new XSL(doc, "kb-list.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] showSearchForm(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "search", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("back", "/servlet/ApplyTransformRes?xml=epg-index&xsl=kb-buttons");
        Element formEl = doc.createElement("channel");
        getChannelList(doc, formEl);
        formEl.setAttribute("value", "Any");
        root.appendChild(formEl);
        formEl = doc.createElement("category");
        getCatList(doc, formEl);
        formEl.setAttribute("value", "Any");
        root.appendChild(formEl);
        XSL transformer = new XSL(doc, "kb-SearchEpg.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private void getChannelList(Document doc, Element formEl) {
        Element option = null;
        Text text = null;
        HashMap<String, Channel> channels = store.getChannels();
        String[] keys = (String[]) channels.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        option = doc.createElement("option");
        text = doc.createTextNode("Any");
        option.appendChild(text);
        formEl.appendChild(option);
        for (int x = 0; x < keys.length; x++) {
            option = doc.createElement("option");
            text = doc.createTextNode(keys[x]);
            option.appendChild(text);
            formEl.appendChild(option);
        }
    }

    private void getCatList(Document doc, Element formEl) {
        Element option = null;
        Text text = null;
        GuideStore guide = GuideStore.getInstance();
        String[] cats = guide.getCategoryStrings();
        option = doc.createElement("option");
        text = doc.createTextNode("Any");
        option.appendChild(text);
        formEl.appendChild(option);
        for (int x = 0; x < cats.length; x++) {
            option = doc.createElement("option");
            text = doc.createTextNode(cats[x]);
            option.appendChild(text);
            formEl.appendChild(option);
        }
    }

    private byte[] showSearchResults(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        String name = urlData.getParameter("name");
        if (name == null || name.length() == 0) name = "";
        String type = urlData.getParameter("type");
        if (type == null || type.length() == 0) type = "Title";
        int searchType = 0;
        if ("title".equalsIgnoreCase(type)) searchType = 1; else if ("description".equalsIgnoreCase(type)) searchType = 2; else searchType = 0;
        String cat = urlData.getParameter("cat");
        if (cat == null || cat.length() == 0) cat = "any";
        String chan = urlData.getParameter("chan");
        if (chan == null || chan.length() == 0) chan = "any";
        String start = urlData.getParameter("start");
        if (start == null || start.length() == 0) start = "0";
        String show = urlData.getParameter("show");
        if (show == null || show.length() == 0) show = "10";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("back", "/servlet/KBEpgItemsRes?action=03");
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("mainurl");
        String actionText = "/servlet/KBEpgItemsRes?action=04" + "&name=" + URLEncoder.encode(name, "UTF-8") + "&type=" + URLEncoder.encode(type, "UTF-8") + "&cat=" + URLEncoder.encode(cat, "UTF-8") + "&chan=" + URLEncoder.encode(chan, "UTF-8") + "&";
        text = doc.createTextNode(actionText);
        button.appendChild(text);
        root.appendChild(button);
        actionText = "(" + name + ") Category(" + cat + ") Channel(" + chan + ")";
        root.setAttribute("title", actionText);
        GuideStore guide = GuideStore.getInstance();
        HashMap<String, Vector<GuideItem>> results = new HashMap<String, Vector<GuideItem>>();
        guide.simpleEpgSearch(name, searchType, cat, chan, 0, null, results);
        String[] keys = (String[]) results.keySet().toArray(new String[0]);
        int total = 0;
        for (int y = 0; y < keys.length; y++) {
            Vector<GuideItem> result = results.get(keys[y]);
            if (result != null && result.size() > 0) {
                for (int x = 0; x < result.size(); x++) {
                    GuideItem item = (GuideItem) result.get(x);
                    button = doc.createElement("button");
                    button.setAttribute("name", item.getName());
                    elm = doc.createElement("url");
                    String actionURL = "/servlet/KBEpgItemsRes?action=02&name=" + URLEncoder.encode(item.getName(), "UTF-8");
                    text = doc.createTextNode(actionURL);
                    elm.appendChild(text);
                    button.appendChild(elm);
                    root.appendChild(button);
                    total++;
                }
            }
        }
        root.setAttribute("total", new Integer(total).toString());
        XSL transformer = new XSL(doc, "kb-list.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] showNames(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        String letter = urlData.getParameter("letter");
        String start = urlData.getParameter("start");
        if (start == null) start = "0";
        String show = urlData.getParameter("show");
        if (show == null) show = "10";
        GuideStore guide = GuideStore.getInstance();
        String[] progs = guide.getNamesStartingWith(letter);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        root.setAttribute("back", "/servlet/KBEpgItemsRes");
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("mainurl");
        text = doc.createTextNode("/servlet/KBEpgItemsRes?action=01&letter=" + letter + "&");
        button.appendChild(text);
        root.appendChild(button);
        int total = 0;
        for (int x = 0; x < progs.length; x++) {
            button = doc.createElement("button");
            button.setAttribute("name", progs[x]);
            elm = doc.createElement("url");
            String actionURL = "/servlet/KBEpgItemsRes?action=02&name=" + URLEncoder.encode(progs[x], "UTF-8");
            text = doc.createTextNode(actionURL);
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
            total++;
        }
        root.setAttribute("total", new Integer(total).toString());
        XSL transformer = new XSL(doc, "kb-list.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] showFirstLetter(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        GuideStore guide = GuideStore.getInstance();
        String[] firsts = guide.getFirstLetters();
        String start = urlData.getParameter("start");
        if (start == null) start = "0";
        String show = urlData.getParameter("show");
        if (show == null) show = "10";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        root.setAttribute("back", store.getTargetURL("epg-sub", "/servlet/ApplyTransformRes?xml=root&xsl=kb-buttons"));
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("mainurl");
        text = doc.createTextNode("/servlet/KBEpgItemsRes?");
        button.appendChild(text);
        root.appendChild(button);
        int total = 0;
        if (firsts.length == 0) {
            button = doc.createElement("button");
            button.setAttribute("name", "None");
            elm = doc.createElement("url");
            text = doc.createTextNode("/servlet/KBEpgItemsRes");
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
            total++;
        }
        for (int x = 0; x < firsts.length; x++) {
            button = doc.createElement("button");
            button.setAttribute("name", firsts[x]);
            elm = doc.createElement("url");
            text = doc.createTextNode("/servlet/KBEpgItemsRes?action=01&letter=" + firsts[x]);
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
            total++;
        }
        root.setAttribute("total", new Integer(total).toString());
        XSL transformer = new XSL(doc, "kb-list.xsl", urlData, headers);
        return transformer.doTransform();
    }
}
