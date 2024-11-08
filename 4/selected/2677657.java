package de.jochenbrissier.backyard;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class BayardTestServlet
 */
@WebServlet(asyncSupported = true, urlPatterns = "/bts")
public class BayardTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Backyard ba = new Backyard(req, resp);
        ba.listenToChannel("kein/plan");
        System.out.println(ba.getChannel("kein/plan").getMembers());
        System.out.println(ba.getChannel("kein/plan").isMember(req.getSession().getId()));
    }
}
