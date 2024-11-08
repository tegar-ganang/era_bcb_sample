package jacky.lanlan.song.extension.struts.interceptor;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;
import jacky.lanlan.song.extension.struts.InfrastructureKeys;
import jacky.lanlan.song.extension.struts.action.ValueStack;
import jacky.lanlan.song.extension.struts.annotation.Download;
import jacky.lanlan.song.extension.struts.annotation.Forward;
import jacky.lanlan.song.extension.struts.resource.Downloadable;
import jacky.lanlan.song.io.IOUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理资源下载的拦截器。优先级500。
 * 
 * @author Jacky.Song
 */
public class DownloadInterceptor extends ActionInterceptorAdapter {

    @Override
    @SuppressWarnings("unchecked")
    public String preHandle(HttpServletRequest request, HttpServletResponse response, Object action) throws Exception {
        Method method = (Method) request.getAttribute(InfrastructureKeys.EXECUTION);
        if (method.isAnnotationPresent(Download.class)) {
            ValueStack vs = (ValueStack) request.getAttribute(InfrastructureKeys.VSAC);
            Map<String, String> reqData = (Map) request.getAttribute(InfrastructureKeys.REQ_DATA);
            Downloadable downloadable = (Downloadable) method.invoke(action, new Object[] { vs, reqData });
            this.download(downloadable, response);
            return Forward.INPUT;
        }
        return null;
    }

    private void download(Downloadable downloadable, HttpServletResponse response) throws Exception {
        String contentType = downloadable.getContentType();
        try {
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;" + " filename=" + new String(downloadable.getName().getBytes(), "ISO-8859-1"));
            OutputStream os = response.getOutputStream();
            IOUtils.copy(downloadable.getInputStream(), os);
            os.flush();
        } catch (IOException e) {
        }
    }

    public int priority() {
        return 500;
    }
}
