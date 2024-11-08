package cn.vlabs.duckling.vwb;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.BeanFactory;
import cn.vlabs.duckling.vwb.services.resource.Resource;
import cn.vlabs.duckling.vwb.ui.VWBContainerImpl;
import cn.vlabs.duckling.vwb.ui.map.IRequestMapper;

/**
 * Introduction Here.
 * 
 * @date Feb 3, 2010
 * @author xiejj@cnic.cn
 */
public class VWBDriverServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public VWBDriverServlet() {
        super();
    }

    public void destroy() {
        super.destroy();
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        URIInfo info = new URIInfo(request.getRequestURI());
        int vid = VWBSite.DEFAULT_FRONT_PAGE;
        if (info.getId() != null) {
            try {
                vid = Integer.parseInt(info.getId());
            } catch (NumberFormatException e) {
            }
        }
        request.setAttribute("contextPath", request.getContextPath());
        Resource vp = container.getSite("").getResource(vid);
        if (vp == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } else {
            try {
                IRequestMapper mapper = container.getMapFactory().getRequestMapper(vp.getType());
                if (mapper != null) {
                    mapper.map(vp, info.getParams(), request, response);
                } else {
                    VWBSession session = VWBSession.findSession(request);
                    session.addMessage(getMessage(request, "driver.type.nosupport", null));
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            } catch (ServletException e) {
                if (e.getRootCause() != null) {
                    sendError(request, response, e.getRootCause());
                } else sendError(request, response, e);
            } catch (Throwable e) {
                sendError(request, response, e);
            }
        }
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        VWBSession session = VWBSession.findSession(request);
        session.setAttrbute(Attributes.EXCEPTION_KEY, e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private String getMessage(HttpServletRequest request, String key, Object[] args) {
        ResourceBundle rb = container.getI18nService().getBundle("CoreResources", request.getLocale(), request.getLocales());
        if (args != null && args.length > 0) return MessageFormat.format(rb.getString(key), args); else return rb.getString(key);
    }

    private static class URIInfo {

        private String m_id;

        private String[] m_params;

        public URIInfo(String uri) {
            m_id = null;
            m_params = null;
            String[] ids = extractID(uri);
            if (ids != null && ids.length >= 1) {
                m_id = ids[0];
                if (ids.length > 1) {
                    m_params = new String[ids.length - 1];
                    for (int i = 0; i < m_params.length; i++) {
                        m_params[i] = ids[i + 1];
                    }
                }
            }
        }

        public String[] getParams() {
            return m_params;
        }

        public String getId() {
            return m_id;
        }

        private String[] extractID(String uri) {
            int pageindex = uri.indexOf(URI_PREFIX);
            if (pageindex == -1) return null;
            uri = uri.substring(pageindex + URI_PREFIX.length());
            if (uri.startsWith("/")) {
                uri = uri.substring(1);
            }
            return uri.split("/");
        }
    }

    public void init() throws ServletException {
        BeanFactory factory = (BeanFactory) this.getServletContext().getAttribute(Attributes.APPLICATION_CONTEXT_KEY);
        container = (VWBContainerImpl) factory.getBean("container");
        container.getMapFactory().init(getServletContext());
    }

    private VWBContainerImpl container;

    private static final String URI_PREFIX = "/page";
}
