package org.melati.test;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import org.melati.Melati;
import org.melati.MelatiConfig;
import org.melati.login.HttpBasicAuthenticationAccessHandler;
import org.melati.poem.Table;
import org.melati.poem.Capability;
import org.melati.poem.AccessToken;
import org.melati.poem.AccessPoemException;
import org.melati.poem.PoemThread;
import org.melati.servlet.MelatiContext;
import org.melati.servlet.PoemServlet;
import org.melati.servlet.PathInfoException;
import org.melati.util.MelatiBugMelatiException;
import org.melati.util.MelatiWriter;
import org.melati.util.MelatiException;

/**
 * Test a Melati configuration which accesses a POEM database 
 * without using a Template Engine.
 */
public class PoemServletTest extends PoemServlet {

    protected void doPoemRequest(Melati melati) throws ServletException, IOException {
        melati.getResponse().setContentType("text/html");
        MelatiWriter output = melati.getWriter();
        output.write("<html><head><title>PoemServlet Test</title></head>\n");
        output.write("<body>\n");
        output.write("<h2>PoemServlet " + "Test</h2>\n");
        output.write("<p>This servlet tests your melati/poem configuration. " + "</p>\n");
        output.write("<p>If you can read this message, it means that you have " + "successfully created a  POEM session using the configurations given in " + "org.melati.LogicalDatabase.properties. </p>\n");
        output.write("<p>Please note that this " + "servlet does not initialise a template engine.</p>\n");
        output.write("<p>Your " + "<b>MelatiContext</b> is set up as: " + melati.getContext() + "<br>, \n");
        output.write("try playing with the PathInfo to see the results (or click " + "<a href=" + melati.getZoneURL() + "/org.melati.test.PoemServletTest/" + melati.getContext().logicalDatabase + "/user/1/View>user/1/View" + "</a>).</p>\n");
        output.write("<h4>Your Database has the following tables:</h4>\n");
        output.write("<table>");
        for (Enumeration e = melati.getDatabase().getDisplayTables(); e.hasMoreElements(); ) {
            output.write(new StringBuffer("<br>").append(((Table) e.nextElement()).getDisplayName()).toString());
        }
        output.write("<h3>Further Testing:</h3>\n");
        output.write("<h4>Template Engine Testing:</h4>\n");
        output.write("You are currently using: <b>" + melati.getConfig().getTemplateEngine().getClass().getName() + "</b>.<br>\n");
        output.write("You can test your WebMacro installation by clicking <a href=" + melati.getZoneURL() + "/org.melati.test.WebmacroStandalone/>WebmacroStandalone</a>" + "<br>\n");
        output.write("You can test your Template Engine working with " + "Melati by clicking <a href=" + melati.getZoneURL() + "/org.melati.test.TemplateServletTest/" + melati.getContext().logicalDatabase + ">" + "org.melati.test.TemplateServletTest/" + melati.getContext().logicalDatabase + "</a><br/>\n");
        String method = melati.getMethod();
        if (method != null) {
            output.write("Test melati Exception handling" + "handling by clicking <a href=Exception>Exception</a><br>\n");
            output.write("Test " + "melati Access Poem Exception handling (requiring you to log-in as an " + "administrator) by clicking <a href=AccessPoemException>Access Poem " + "Exception</a><br>\n");
            output.write("Current method:" + method + "<br/>\n");
            Capability admin = PoemThread.database().getCanAdminister();
            AccessToken token = PoemThread.accessToken();
            if (method.equals("AccessPoemException")) throw new AccessPoemException(token, admin);
            if (method.equals("Exception")) throw new MelatiBugMelatiException("It got caught!");
            if (method.equals("Redirect")) melati.getResponse().sendRedirect("http://www.melati.org");
        }
    }

    /**
 * How to use a different melati configuration.
 *
 */
    protected MelatiConfig melatiConfig() throws MelatiException {
        MelatiConfig config = super.melatiConfig();
        config.setAccessHandler(new HttpBasicAuthenticationAccessHandler());
        return config;
    }

    /**
 * Set up the melati context so we don't have to specify the 
 * logicaldatabase on the pathinfo.  
 *
 * This is a very good idea when
 * writing your appications where you are typically only accessing
 * a single database
 */
    protected MelatiContext melatiContext(Melati melati) throws PathInfoException {
        String[] parts = melati.getPathInfoParts();
        if (parts.length == 0) return melatiContextWithLDB(melati, "melatitest"); else return super.melatiContext(melati);
    }
}
