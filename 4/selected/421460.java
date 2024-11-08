package org.openremote.controller.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.openremote.controller.Constants;
import org.openremote.controller.exception.ControllerException;
import org.openremote.controller.rest.support.json.JSONTranslator;
import org.openremote.controller.service.StatusCommandService;
import org.openremote.controller.spring.SpringContext;

/**
 * The Class Status Command REST Servlet.
 * 
 * This servlet is responsible for 
 *   parsing RESTful url "http://xxx.xxx.xxx/controller/rest/status/{sensor_id},{sensor_id}...",
 *   building StatusCommand,
 *   status query with stateful StatusCommand,
 *   and conpose status result into XML formatted data to RESTful service client.
 * 
 * @author Handy.Wang
 */
@SuppressWarnings("serial")
public class StatusCommandRESTServlet extends HttpServlet {

    /** The logger. */
    private Logger logger = Logger.getLogger(StatusCommandRESTServlet.class.getName());

    /** The control command service. */
    private static StatusCommandService statusCommandService = (StatusCommandService) SpringContext.getInstance().getBean("statusCommandService");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding(Constants.CHARACTER_ENCODING_UTF8);
        response.setContentType(Constants.MIME_APPLICATION_XML);
        String acceptHeader = request.getHeader(Constants.HTTP_ACCEPT_HEADER);
        String url = request.getRequestURL().toString();
        String regexp = "rest\\/status\\/(.*)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(url);
        String unParsedSensorIDs = null;
        PrintWriter printWriter = response.getWriter();
        if (matcher.find()) {
            unParsedSensorIDs = matcher.group(1);
            try {
                if (unParsedSensorIDs != null && !"".equals(unParsedSensorIDs)) {
                    printWriter.write(JSONTranslator.translateXMLToJSON(acceptHeader, response, statusCommandService.readFromCache(unParsedSensorIDs)));
                }
            } catch (ControllerException e) {
                logger.error("CommandException occurs", e);
                printWriter.print(JSONTranslator.translateXMLToJSON(acceptHeader, response, e.getErrorCode(), RESTAPI.composeXMLErrorDocument(e.getErrorCode(), e.getMessage())));
            }
        } else {
            printWriter.print(JSONTranslator.translateXMLToJSON(acceptHeader, response, 400, RESTAPI.composeXMLErrorDocument(400, "Bad REST Request, should be /rest/status/{sensor_id},{sensor_id}...")));
        }
        printWriter.flush();
    }
}
