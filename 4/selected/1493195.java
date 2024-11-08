package com.trackerdogs.ui.servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.net.*;
import com.trackerdogs.ui.servlet.skin.*;
import com.trackerdogs.search.*;
import com.trackerdogs.websources.dispatcher.*;
import com.trackerdogs.websources.results.*;
import com.trackerdogs.websources.*;
import com.trackerdogs.search.*;
import com.trackerdogs.*;

/**********************************************************************
 * A Servlet that gets keywords and returns search results in HTML
 * <br> Parameters:
 * <dl>
 *  <li>textonly
 *  <li>keywords
 *  <li>page
 *  <li>type
 * </ul>
 *
 * @author Koen Witters
 *
 * @version $Header: /cvsroot/trackerdogs/trackerdogs/src/com/trackerdogs/ui/servlet/ClientSearch.java,v 1.3 2002/09/03 14:36:04 kwitters Exp $
 */
public class ClientSearch implements SkinInterface {

    private static final String VERSION_ = "1.5";

    private static final String BANNER_FILE = "../html/skins/banner.html";

    private boolean inError_;

    private Writer out_;

    private Results fResults;

    private boolean fResultsReady;

    private String fMessages;

    private int fPageNr;

    private boolean recursive;

    private String fSkin;

    private int internSkin_;

    private String fBanner;

    private HttpServletResponse httpResponse_;

    private int fLoopBlock;

    private Date fBegin;

    private int resultIt_;

    /**
     * Gets Keywords, gets pages from search engines, extracts the results, puts results in list,
     * and returns results in HTML.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        fLoopBlock = -1;
        inError_ = false;
        this.resultIt_ = -1;
        try {
            response.setContentType("text/html");
            this.httpResponse_ = response;
            String keywords = request.getParameter("keywords");
            if (keywords == null) {
                keywords = new String();
            }
            URL skinUrl = null;
            String skinStr = request.getParameter("skin");
            if (skinStr != null && (skinStr.indexOf("trackerdogs.com:8080/servlet/trackerdogs.Search") == -1)) {
                skinUrl = new URL(skinStr);
            }
            internSkin_ = 0;
            if (request.getParameter("text") != null) {
                internSkin_ = 1;
            }
            if (keywords == null || keywords.equals("")) {
                fResults = new Results(new Keywords("", Keywords.ALL_OF_WORDS));
                MakeHtml(0, skinUrl, internSkin_);
            } else {
                WebSourceSet set = WebSourceSet.getGlobalWebSourceSet();
                try {
                    set.importXmlFile("data/websourceset.xml");
                } catch (XMLMarkupException ex) {
                    System.out.println(ex);
                } catch (WebSourceDuplicateException ex) {
                    System.out.println(ex);
                }
                int pageNr = Integer.parseInt(request.getParameter("page"));
                if (request.getParameter("new") != null) {
                    pageNr = 1;
                }
                int type = Integer.parseInt(request.getParameter("type"));
                Keywords keys = new Keywords(keywords, type);
                fResults = DataManager.getResultsOf(keys);
                com.trackerdogs.search.Search search = new com.trackerdogs.search.Search(fResults);
                search.getResults(pageNr * 10);
                MakeHtml(pageNr, skinUrl, internSkin_);
                search.getResults(pageNr * 10 + 10);
                DataManager.save();
            }
        } catch (IOException ex) {
            System.out.println("Search.doGet() IOException: " + ex);
        }
    }

    public void MakeHtml(int pageNr, URL skin, int internSkin) {
        try {
            out_ = this.httpResponse_.getWriter();
        } catch (IOException ex) {
            System.out.println("Search.MakeHtml() IOException of Writer: " + ex);
        }
        fBanner = new String();
        fSkin = null;
        recursive = false;
        fPageNr = pageNr;
        fResultsReady = false;
        fMessages = new String();
        try {
            Reader skinIn;
            if (skin == null) {
                if (internSkin == 1) {
                    skinIn = new FileReader("../html/skins/textSkin.html");
                } else {
                    skinIn = new FileReader("../html/skins/search.html");
                }
            } else {
                fSkin = skin.toString();
                skinIn = new InputStreamReader(skin.openStream());
                writeBanner();
            }
            Skin skinGen = new Skin(this);
            fBegin = new Date();
            try {
                skinGen.generate(skinIn, out_);
            } catch (IOException ex) {
                System.out.println("MakeHtml generate() io: " + ex);
            }
            try {
                this.httpResponse_.flushBuffer();
            } catch (IOException ex) {
                System.out.println("MakeHtml flushBuffer() io: " + ex);
            }
            skinIn.close();
            out_.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MakeHtml file not found: " + ex);
        } catch (IOException ex) {
            System.out.println("MakeHtml io: " + ex);
        }
    }

    /**********************************************************************
     * Write the advertising banner to output stream
     */
    public void writeBanner() {
        try {
            FileReader banner = new FileReader(BANNER_FILE);
            while (banner.ready()) {
                out_.write(banner.read());
            }
        } catch (FileNotFoundException ex) {
            System.out.println("ClientSearch.writeBanner(): " + ex);
        } catch (IOException ex) {
            System.out.println("ClientSearch.writeBanner(): " + ex);
        }
    }

    /**********************************************************************
     * Returns the value of the variable
     *
     * @param variable the variable name
     */
    public String getVariableValue(String variable) {
        if (variable.equals("version")) {
            return VERSION_;
        } else if (variable.equals("skin")) {
            return fSkin;
        } else if (variable.equals("keywords")) {
            if (fResults.getKeywords().getType() == Keywords.OPERATORS) {
                return fResults.getKeywords().toString(true, true);
            } else {
                return fResults.getKeywords().toString(false, false);
            }
        } else if (variable.equals("details")) {
            return "trackerdogs.control.ResultController?action=display&key=" + Keywords.stringToUrlForm(fResults.getKeywords().toString(true, true));
        } else if (variable.equals("page")) {
            return (new Integer(fPageNr)).toString();
        } else if (variable.equals("resultNr")) {
            return (new Integer((fPageNr - 1) * 10 + resultIt_ + 1)).toString();
        } else if (variable.equals("title")) {
            Result wp = fResults.getResultAt(((fPageNr - 1) * 10) + resultIt_);
            return setKeysInTab(wp.getTitle(), fResults.getKeywords().toVector(), "<b>", "</b>");
        } else if (variable.equals("score")) {
            Result wp = fResults.getResultAt(((fPageNr - 1) * 10) + resultIt_);
        } else if (variable.equals("desc")) {
            Result wp = fResults.getResultAt(((fPageNr - 1) * 10) + resultIt_);
            return wp.getDesc();
        } else if (variable.equals("source")) {
            Result wp = fResults.getResultAt(((fPageNr - 1) * 10) + resultIt_);
            return wp.getSource();
        } else if (variable.equals("url")) {
            Result wp = fResults.getResultAt(((fPageNr - 1) * 10) + resultIt_);
            return wp.getURL().toString();
        } else if (variable.equals("urlbold")) {
            Result wp = fResults.getResultAt(((fPageNr - 1) * 10) + resultIt_);
            return wp.getURL().toString();
        } else if (variable.equals("banner")) {
            return banner();
        }
        return new String();
    }

    /**********************************************************************
     * A quick fix of the banner. Will be extended in the future
     */
    private String banner() {
        String b = new String();
        try {
            URL bannerUrl = new URL("http://sukkelvlieg.servebeer.com/~erik/wof/submit.php?pub=koen@trackerdogs.com&keywords=" + fResults.getKeywords().toString(false, false));
            BufferedReader is = new BufferedReader(new InputStreamReader(bannerUrl.openStream()));
            String href = is.readLine();
            String src = is.readLine();
            b += "<!--- Begin WOF Code =--><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" summary=\"banner\">\n";
            b += "<tr><td><table border=\"0\" bgcolor=\"#0000FF\" cellpadding=\"1\" cellspacing=\"0\" summary=\"banner\">\n";
            b += "<tr><td><a href=\"" + href + "\" target=\"_top\">\n";
            b += "<img src=\"" + src + "\" border=\"0\" hspace=\"0\" width=\"468\" height=\"60\" alt=\"Banner Ad\" /></a>\n";
            b += "</td></tr></table></td></tr><tr><td align=\"center\"><a href=\"http://sukkelvlieg.servebeer.com/~erik/wof/\" target=\"_top\"><img\n";
            b += "src=\"http://sukkelvlieg.servebeer.com/~erik/wof/img/wof_subbanner.gif\" border=\"0\" hspace=\"0\" width=\"468\" height=\"15\"\n";
            b += "alt=\"Click!\" /></a></td></tr></table><!--- End WOF Code =-->";
        } catch (MalformedURLException ex) {
            b += "No banner available: " + ex.getMessage();
        } catch (IOException ex) {
            b += "No banner available: " + ex.getMessage();
        }
        return b;
    }

    /************************************************************
     * Excecute the specified function with parameters
     *
     * @param func the function name
     * @param params an array of Strings (the parameters)
     *
     * @param return the result
     */
    public String getFunctionValue(String func, String[] params) {
        if (func.equals("select")) {
            int type = fResults.getKeywords().getType();
            int param = -1;
            try {
                param = (Integer.valueOf(params[0])).intValue();
            } catch (NumberFormatException ex) {
            }
            if (type == param) {
                return "selected";
            }
        } else if (func.equals("wait")) {
            if (!fResults.getKeywords().toString(false, false).equals("")) {
                waitUntilReady();
            }
        } else if (func.equals("pagenr")) {
            if (!fResults.getKeywords().toString(false, false).equals("")) {
                return generatePageNr(fResults.getKeywords(), fPageNr, params[0], params[1]);
            }
        }
        return new String();
    }

    /************************************************************
     */
    public boolean loopBlock(String blockName) {
        if (blockName.equals("results")) {
            try {
                this.httpResponse_.flushBuffer();
            } catch (IOException ex) {
                System.out.println("MakeHtml flushBuffer() io: " + ex);
            }
            resultIt_++;
            if (fPageNr == 0) {
                return false;
            }
            if (resultIt_ == 0) {
                waitUntilReady();
            }
            if (fResults.allPolled()) {
                if (fResults.resultNo() == 0) {
                    printNotFound();
                    return false;
                } else if (((fPageNr - 1) * 10) + resultIt_ < fResults.resultNo() && resultIt_ < 10) {
                    fResults.setResultsStaticUntil(((fPageNr - 1) * 10) + resultIt_ + 1);
                    return true;
                } else {
                    return false;
                }
            } else if (resultIt_ < 10) {
                fResults.waitOnResult(((fPageNr - 1) * 10) + resultIt_, 2000);
                if (fResults.resultPolledAt(((fPageNr - 1) * 10) + resultIt_)) {
                    fResults.setResultsStaticUntil(((fPageNr - 1) * 10) + resultIt_ + 1);
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        } else if (blockName.equals("notOnPage0")) {
            if (fLoopBlock != 1) {
                fLoopBlock = 1;
                return fPageNr != 0;
            } else {
                fLoopBlock = -1;
                return false;
            }
        } else if (blockName.equals("onlyOnPage1")) {
            if (fLoopBlock != 2) {
                fLoopBlock = 2;
                return fPageNr == 1;
            } else {
                fLoopBlock = -1;
                return false;
            }
        }
        return false;
    }

    private void waitUntilReady() {
        try {
            this.httpResponse_.flushBuffer();
        } catch (IOException ex) {
            System.out.println("MakeHtml.waitUntilReady() flushBuffer: " + ex);
        }
        fResults.waitOnResult((((fPageNr - 1) * 10) + resultIt_), 5000);
    }

    private void printNotFound() {
        inError_ = true;
        try {
            out_.write("<b>Sorry, the Tracker Dogs did not find any results.<b>");
        } catch (IOException ex) {
            System.out.println("Search.printNotFound() " + ex);
        }
    }

    private String StringToURLForm(String keywords) {
        String result = new String();
        char ch;
        for (int j = 0; j < keywords.length(); j++) {
            ch = keywords.charAt(j);
            if (ch == ' ') {
                result = result + '+';
            } else if ((ch < 48) || (ch > 57 && ch < 65) || (ch > 90 && ch < 97) || (ch > 122)) {
                result = result + '%';
                if (((int) ch) < 16) result = result + '0';
                result = result + (Integer.toHexString((int) ch));
            } else {
                result = result + ch;
            }
        }
        return result;
    }

    /**
     * Error: if second keyword is font, bold
     */
    private String setKeysInTab(String str, Vector keys, String prefix, String postfix) {
        String stringBold = new String();
        int startpoint;
        int endpoint = 0;
        String key;
        boolean lastKeywordReached;
        for (int i = 0; i < keys.size(); i++) {
            stringBold = "";
            startpoint = 0;
            key = (String) keys.elementAt(i);
            if (key.charAt(0) == '+') {
                key = key.substring(1);
            } else if (key.charAt(0) == '-') {
                key = key.substring(1);
            }
            key = key.toLowerCase();
            lastKeywordReached = false;
            while (!lastKeywordReached) {
                endpoint = str.toLowerCase().indexOf(key, startpoint);
                if (endpoint > -1) {
                    stringBold = stringBold + str.substring(startpoint, endpoint) + prefix + str.substring(endpoint, endpoint + key.length()) + postfix;
                    startpoint = endpoint + key.length();
                } else {
                    stringBold = stringBold + str.substring(startpoint);
                    lastKeywordReached = true;
                }
            }
            str = stringBold;
        }
        return stringBold;
    }

    private String generatePageNr(Keywords keywords, int page, String prev, String next) {
        String out = new String();
        int type = keywords.getType();
        String keys;
        if (keywords.getType() == 2) {
            keys = Keywords.stringToUrlForm(keywords.toString(true, true));
        } else {
            keys = Keywords.stringToUrlForm(keywords.toString(false, false));
        }
        if (page > 1) {
            out += "<font size=+1><a href=\"Search?type=" + type;
            out += "&keywords=" + keys;
            out += "&page=" + (page - 1);
            if (fSkin != null) {
                out += "&skin=" + fSkin;
            }
            if (internSkin_ == 1) {
                out += "&text=on";
            }
            out += "\">" + prev + "</a></font> - \n";
        }
        boolean allPolled = fResults.allPolled();
        int lowEdge = Math.max(1, page - 10);
        int highEdge = Math.min((fResults.resultNo() / 10), page + 10);
        if (lowEdge != 1) {
            out += " ...\n";
        }
        for (int i = lowEdge; i <= highEdge; i++) {
            if (i != page) {
                out += "<a href=\"Search?type=" + type;
                out += "&keywords=" + keys;
                out += "&page=" + i;
                if (fSkin != null) {
                    out += "&skin=" + fSkin;
                }
                if (internSkin_ == 1) {
                    out += "&text=on";
                }
                out += "\">" + i + "</a>\n";
            } else {
                out += "<font size=+1><b>" + page + "</b></font>\n";
            }
        }
        if ((!inError_ && !allPolled) || (highEdge != (fResults.resultNo() / 10))) {
            out += " ...\n";
        }
        if (!inError_ && !(allPolled && ((fResults.resultNo() / 10) + 1) <= page)) {
            out += " - <font size=+1><a href=\"Search?type=" + type;
            out += "&keywords=" + keys;
            out += "&page=" + (page + 1);
            if (fSkin != null) {
                out += "&skin=" + fSkin;
            }
            if (internSkin_ == 1) {
                out += "&text=on";
            }
            out += "\">" + next + "</a></font>\n";
        }
        out += "<br><br>" + fResults.resultNo() + " results found";
        if (!allPolled) {
            out += ", and still searching";
        }
        return out;
    }
}
