package net.sourceforge.cruisecontrol.dashboard.web;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.utils.DashboardUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class PanopticodeController implements Controller {

    private final Configuration configuration;

    private static final Logger LOGGER = Logger.getLogger(PanopticodeController.class);

    public PanopticodeController(Configuration configuration) {
        this.configuration = configuration;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] url = DashboardUtils.urlToParams(request.getRequestURI());
        String projectName = url[url.length - 2];
        String category = url[url.length - 1];
        File svgFile = getPanopticodeOutput(projectName, category);
        if (!svgFile.exists()) {
            return pictureNotExist(projectName, response);
        }
        response.setContentType("image/svg+xml");
        response.getWriter().write(FileUtils.readFileToString(svgFile, "UTF-8"));
        return null;
    }

    private File getPanopticodeOutput(String projectName, String category) {
        return new File(configuration.getCruiseConfigDirLocation() + "/projects/" + projectName + "/target/reports/svg/interactive-" + category + "-treemap.svg");
    }

    private ModelAndView pictureNotExist(String projectName, HttpServletResponse response) {
        response.setContentType("text/plain");
        try {
            response.getWriter().write("No panopticode output found for " + projectName);
        } catch (IOException e) {
            LOGGER.warn(e);
        }
        return null;
    }
}
