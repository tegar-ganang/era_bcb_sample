package net.homeip.tin.extension.tag;

import java.io.IOException;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Writes out <a href="http://www.webopedia.com/TERM/B/bread_crumbs.html">bread
 * crumbs</a> based upon the browser URI requested by the browser.
 * 
 * www.tin.homeip.net/contextRoot/Administration/Add_User.jsp will generate the bread crumb,
 * Adminitration >> Add_User
 * 
 * @author Tin Pham
 * @version 0.1
 */
public class UrlBasedBreadCrumb extends TagSupport {

    public int doStartTag() throws JspTagException {
        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspTagException {
        HttpServletRequest request = null;
        String breadCrumb = null;
        String deliminator = null;
        String deliminatorExtension = null;
        String seperator = null;
        String pageURI = null;
        StringTokenizer st = null;
        request = (HttpServletRequest) pageContext.getRequest();
        pageURI = request.getRequestURI();
        deliminator = "/";
        deliminatorExtension = ".";
        seperator = " &rarr; ";
        st = new StringTokenizer(pageURI, deliminator);
        breadCrumb = "";
        for (int i = 0; st.hasMoreTokens() && i < 3; i++) {
            st.nextToken();
        }
        if (st.hasMoreTokens()) {
            breadCrumb += st.nextToken();
        }
        while (st.hasMoreTokens()) {
            breadCrumb += seperator;
            breadCrumb += st.nextToken();
        }
        st = new StringTokenizer(breadCrumb, deliminatorExtension);
        if (st.hasMoreTokens()) {
            breadCrumb = st.nextToken();
        } else {
            System.out.println("Something went wrong processing UrlBasedBreadCrumb.");
        }
        breadCrumb = breadCrumb.replaceAll("_", " ");
        try {
            pageContext.getOut().write(breadCrumb);
        } catch (IOException ex) {
            throw new JspTagException("Fatal error: Custom tag could not write to JSP out");
        }
        return EVAL_PAGE;
    }
}
