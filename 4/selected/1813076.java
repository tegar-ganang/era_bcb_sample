package com.c2b2.ipoint.presentation.taglib;

import com.c2b2.ipoint.model.Page;
import com.c2b2.ipoint.presentation.PresentationException;
import com.c2b2.ipoint.presentation.SchemeManager;
import java.io.IOException;
import java.util.LinkedList;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
  * $Id: BreadCrumbs.java,v 1.2 2006/10/12 12:18:30 steve Exp $
  * 
  * Copyright 2006 C2B2 Consulting Limited. All rights reserved.
  * Use of this code is subject to license.
  * Please check your license agreement for usage restrictions
  * 
  * This class implements the ipoint:breadcrumbs tag to render a set of breadcrumbs from the current page. 
  * 
  * @author $Author: steve $
  * @version $Revision: 1.2 $
  * $Date: 2006/10/12 12:18:30 $
  * 
  */
public class BreadCrumbs extends TagSupport {

    public BreadCrumbs() {
    }

    public int doEndTag() throws JspException {
        LinkedList<Page> result = new LinkedList<Page>();
        Page current = getCurrentPage();
        while (current != null) {
            result.addFirst(current);
            current = current.getParent();
        }
        JspWriter out = pageContext.getOut();
        try {
            out.write("<div class='breadcrumbs_area'>");
            out.write("<span class=\"breadcrumb_message\">" + myInitialText + "&nbsp;</span>");
            for (Page page : result) {
                out.write("<span class=\"breadcrumb_label\">");
                if (page.isContent()) {
                    out.write("<a href='" + page.getID() + ".page' title='" + page.getTitle() + "'>");
                }
                out.write(page.getLabel());
                if (page.isContent()) {
                    out.write("</a>");
                }
                out.write("</span>");
                out.write("&nbsp;<img src=\"" + SchemeManager.getSchemeManager().getSchemeFile(myImageName) + "\" title='Bread Crumb' alt='bread crumb' align=\"middle\" valign='top'/>&nbsp;\n");
            }
            out.write("</div>");
        } catch (IOException e) {
            throw new JspException(e);
        } catch (PresentationException e) {
            throw new JspException(e);
        }
        return EVAL_PAGE;
    }

    public void setImageName(String imageName) {
        this.myImageName = imageName;
    }

    public String getImageName() {
        return myImageName;
    }

    public void setCurrentPage(Page currentPage) {
        this.myCurrentPage = currentPage;
    }

    public Page getCurrentPage() {
        return myCurrentPage;
    }

    public void setInitialText(String initialText) {
        this.myInitialText = initialText;
    }

    public String getInitialText() {
        return myInitialText;
    }

    private String myInitialText;

    private String myImageName;

    private Page myCurrentPage;
}
