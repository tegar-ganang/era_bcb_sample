package com.google.code.sagetvaddons.hdsched;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HdSchedServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 2585830436849783022L;

    public HdSchedServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", "text/plain");
        resp.setDateHeader("Expires", 0);
        Writer w = resp.getWriter();
        File log = new File("shd.out");
        if (log.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(log));
            String s;
            while ((s = r.readLine()) != null) w.write(s + "\n");
            r.close();
        } else w.write("Sage HD has not completed a run yet, please check back in a few minutes...\n");
        w.close();
    }
}
