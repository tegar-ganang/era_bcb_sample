package com.xmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.appspot.gossipscity.server.AppMisc;
import com.appspot.gossipscity.server.Channels;
import com.news.dto.AppConstants;

@SuppressWarnings("serial")
public class XmlParserServlet extends HttpServlet {

    List<Channels> lstChannels;

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        performAction(req, resp);
    }

    private void performAction(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String strAction = null;
        try {
            if (req.getParameterMap().containsKey(AppConstants.ACTION)) strAction = req.getParameter(AppConstants.ACTION);
            if (strAction.equalsIgnoreCase(AppConstants.GET_RSS)) {
                getRSS(resp, req);
            } else if (strAction.equalsIgnoreCase(AppConstants.GET_MORE_RSS)) {
                getMoreRSS(resp, req);
            } else if (strAction.equalsIgnoreCase(AppConstants.GET_CHANNELS)) {
                getChannels(resp);
            } else if (strAction.equalsIgnoreCase(AppConstants.GET_MORE_CHANNELS)) {
                getMoreChannels(resp, req);
            } else if (strAction.equalsIgnoreCase(AppConstants.CREATE_CHANNEL)) {
                createChannel(req);
            }
        } catch (Exception e) {
        }
    }

    private void createChannel(HttpServletRequest req) {
        AppMisc.createNewPost(req.getParameter("name"), req.getParameter("rssLink"), Integer.parseInt(req.getParameter("ratingCount")));
    }

    private void getChannels(HttpServletResponse resp) throws IOException {
        lstChannels = AppMisc.getChannels();
        if (lstChannels.size() == 0) return;
        for (int index = 0; index < lstChannels.size(); index++) {
            resp.getWriter().print(lstChannels.get(index).getName() + "|");
            resp.getWriter().print(lstChannels.get(index).getRssLink() + "|");
            resp.getWriter().print(lstChannels.get(index).getRatingCount() + "|");
            resp.getWriter().print("~");
        }
        if (!AppMisc.cursorString.isEmpty()) {
            resp.getWriter().print(AppMisc.cursorString);
        }
    }

    private void getMoreChannels(HttpServletResponse resp, HttpServletRequest req) throws IOException {
        lstChannels = AppMisc.getMorePosts(req.getParameter("cursorStr").toString());
        if (lstChannels.size() == 0) return;
        for (int index = 0; index < lstChannels.size(); index++) {
            resp.getWriter().print(lstChannels.get(index).getName() + "|");
            resp.getWriter().print(lstChannels.get(index).getRssLink() + "|");
            resp.getWriter().print(lstChannels.get(index).getRatingCount() + "|");
            resp.getWriter().print("~");
        }
        if (!AppMisc.cursorString.isEmpty()) {
            resp.getWriter().print(AppMisc.cursorString);
        }
    }

    private void getRSS(HttpServletResponse resp, HttpServletRequest req) throws IOException {
        List<String> lsMain = new ArrayList<String>();
        int index = 0;
        lsMain = mainParser(req.getParameter("rssUrl").toString());
        if (lsMain.size() >= 15) {
            index = 15;
        } else index = lsMain.size();
        for (int i = 0; i < index; i++) {
            resp.getWriter().print(lsMain.get(i));
        }
        resp.getWriter().print(index);
    }

    private List<String> temp_method() {
        List<String> ls = new ArrayList<String>();
        for (int i = 0; i < 50; i++) {
            ls.add(" " + i + ". this is heading|and this is the description|~");
        }
        return ls;
    }

    private void getMoreRSS(HttpServletResponse resp, HttpServletRequest req) throws IOException {
        List<String> lsMain = new ArrayList<String>();
        int cursorPosition = Integer.parseInt(req.getParameter("cursorStr").toString());
        int index = 0;
        lsMain = mainParser(req.getParameter("rssUrl").toString());
        if (lsMain.size() - cursorPosition >= 15) {
            index = 15;
        } else index = lsMain.size() - cursorPosition;
        for (int i = cursorPosition; i < (cursorPosition + index); i++) {
            resp.getWriter().print(lsMain.get(i));
        }
        resp.getWriter().print(cursorPosition + index);
    }

    private List<String> mainParser(String feedId) {
        List<String> ls = new ArrayList<String>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(feedId);
            NodeList Std = doc.getElementsByTagName("item");
            Transformer tFormer = TransformerFactory.newInstance().newTransformer();
            tFormer.setOutputProperty(OutputKeys.METHOD, "text");
            for (int i = 0; i < Std.getLength(); i++) {
                Element StudentData = (Element) (Std.item(i));
                NodeList headings = StudentData.getElementsByTagName("title");
                NodeList discription = StudentData.getElementsByTagName("description");
                String id = headings.item(0).getFirstChild().getNodeValue();
                String dob = discription.item(0).getFirstChild().getNodeValue();
                id = RemoveHtml(id);
                dob = RemoveHtml(dob);
                ls.add(id + "|" + dob + "|~");
            }
        } catch (Exception e) {
            System.exit(0);
        }
        return ls;
    }

    private String RemoveHtml(String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        return sb.toString().replaceAll("\\<.*?>", "");
    }
}
