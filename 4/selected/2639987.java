package net.sf.mareco.m2cc.view;

import java.io.OutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.mareco.m2cc.services.ComponentInputStream;
import net.sf.mareco.m2cc.services.M2ccService;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * renders an HTTP response with the contents and MIME type of a requested
 * artifact component
 * 
 * @author amirk
 */
public class ArtifactView extends AbstractUrlBasedView {

    private M2ccService m2ccService;

    public void setM2ccService(M2ccService service) {
        m2ccService = service;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String resource = getUrl();
        ComponentInputStream in = m2ccService.getComponentAsStream(resource);
        if (in == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setContentType(in.getContentType());
            OutputStream out = response.getOutputStream();
            while (in.available() > 0) {
                out.write(in.read());
            }
            in.close();
            out.flush();
        }
    }
}
