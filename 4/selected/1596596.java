package com.velociti.ikarus.ui.widget.demo.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.vaadin.terminal.gwt.server.ApplicationServlet;

/**
 * ######JQueryServlet#####
 *  -This class is used to insert, JQuery js files to the pages of the application.
 * 
 * 
 * @author Alper Turkyilmaz - VelocitiSoftware - 2011
 * @version 1.0.2
 * 
 */
public class JQueryServlet extends ApplicationServlet {

    protected void writeAjaxPageHtmlHeader(BufferedWriter page, String title, String themeUri, HttpServletRequest request) throws IOException {
        super.writeAjaxPageHtmlHeader(page, title, themeUri, request);
        page.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n");
        page.write("<style type=\"text/css\">" + "html, body {height:100%;margin:0;}</style>");
        page.write("<script type=\"text/javascript\" src=\"" + themeUri + "/../js/jquery-1.6.2.min.js\"></script>");
        page.write("<script type=\"text/javascript\" src=\"" + themeUri + "/../js/jquery.jqDock.min.js\"></script>");
        page.write("<script type=\"text/javascript\" src=\"" + themeUri + "/../js/ikarusbreadcrumbs.js\"></script>");
        page.write("<title>" + title + "</title>");
    }
}
