package org.commonlibrary.lcms.web.springmvc.scorm.export;

import org.commonlibrary.lcms.curriculum.service.CurriculumService;
import org.commonlibrary.lcms.model.Curriculum;
import org.commonlibrary.lcms.scorm.service.ScormService;
import org.commonlibrary.lcms.support.content.ContentSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author jorge.elizondo
 *         Date: 05.12.2008
 *         Time: 15:50:17
 *         <p/>
 */
@Controller
public class ExportToScormController {

    @Autowired
    private CurriculumService curriculumService;

    @Autowired
    private ScormService scormService;

    @RequestMapping("/exportCurriculumToScorm.spr")
    public void showBackdoorStandardAlignment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuffer output = new StringBuffer();
        output.append("<html>").append("<body>").append(String.format("<form action='%s/%s' method='POST'>", request.getContextPath(), "/doExportCurriculumToScorm.spr")).append("<li> Curriculum id: ").append("<input type='text' id='curriculumId' name='curriculumId'/>").append("</li>").append("<input type='submit' value='Export'/>").append("</form>").append("</body>").append("</html>");
        response.getOutputStream().println(output.toString());
    }

    @RequestMapping("/doExportCurriculumToScorm.spr")
    public void doBackdoorStandardAlignment(@RequestParam("curriculumId") String curriculumId, HttpServletResponse response) throws IOException {
        Curriculum curriculum = curriculumService.findById(curriculumId);
        response.setContentType(ContentSupport.ZIP_TYPE);
        response.setHeader("Content-disposition", "inline; filename=\"" + curriculum.getName() + ".zip" + "\"");
        writeContent(scormService.exportCurriculum(curriculum), response.getOutputStream());
    }

    /**
     * Write the InputStream's content in the OutputStream
     * @param inputStream
     * @return outputStream
     */
    protected void writeContent(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();
    }
}
