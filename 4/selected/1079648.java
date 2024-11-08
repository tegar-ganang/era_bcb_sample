package org.jmantis.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jmantis.core.utils.MimeUtils;
import org.jmantis.core.utils.TimeSpan;
import org.jmantis.server.Config;
import org.jmantis.server.Context;
import org.jmantis.web.config.ActionInfo;
import org.jmantis.web.invoke.Invoke;
import org.jmantis.web.listener.WebRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher {

    private static final Dispatcher dp = new Dispatcher();

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private Context context = Context.getInstance();

    private Dispatcher() {
    }

    public static final Dispatcher getInstance() {
        return dp;
    }

    /**
	 * TODO 需要重构和优化
	 * @param path
	 * @param resp
	 * @return 是否找到文件
	 * @throws Exception
	 */
    public final boolean disposeStaticFile(String path, Response resp) throws Exception {
        File dir = new File(context.getConfig().getStaticFilePath());
        File file = new File(dir, path);
        if (file.exists()) {
            if (file.isDirectory()) {
                file = new File(file, "index.html");
                if (!file.exists()) {
                    return false;
                }
            }
            FileInputStream fi = new FileInputStream(file);
            try {
                byte[] data = IOUtils.toByteArray(fi);
                String contentType = MimeUtils.getMimeType(FilenameUtils.getExtension(file.getName()));
                resp.setContentType(contentType);
                resp.oneWrite(data);
            } finally {
                fi.close();
            }
            return true;
        }
        return false;
    }

    public final void service(final ChannelHandlerContext chc, final MessageEvent evt, final byte[] post) throws Exception {
        org.jboss.netty.handler.codec.http.HttpRequest request = (org.jboss.netty.handler.codec.http.HttpRequest) evt.getMessage();
        if (log.isDebugEnabled()) {
            String uri = request.getUri();
            log.debug("处理请求[method:{},uri:{}]", request.getMethod(), uri);
        }
        TimeSpan ts = new TimeSpan().start();
        Response resp = new HttpResponse(evt.getChannel(), chc);
        chc.setAttachment(resp);
        Invoke invoke = context.getInvoke();
        try {
            Request req = HttpRequest.create(request, post, evt.getChannel());
            context.setRequest(req);
            String path = req.getPath();
            ActionInfo action = context.getAction(path);
            if (null == action) {
                if (!disposeStaticFile(path, resp)) {
                    resp.sendError(HttpResponseStatus.NOT_FOUND);
                    log.debug("文件未找到path:{}", path);
                }
            } else {
                WebRequestEvent webEvt = context.fireRequestStartEvent(req);
                try {
                    Object result = invoke.invoke(path, context.getIocFactory(), req, resp);
                    ViewHandler viewHandler = action.getViewHandler();
                    viewHandler.doView(action, result, req, resp);
                } finally {
                    context.fireRequestEndEvent(webEvt);
                }
            }
        } catch (FileNotFoundException e) {
            resp.sendError(HttpResponseStatus.NOT_FOUND);
            log.error("文件没找到" + e.getMessage(), e);
        } catch (Exception e) {
            resp.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            log.error("服务器错误", e);
        }
        context.clearRequest();
        resp.flushAndClose();
        log.debug("请求处理结束,耗时[{}]", ts.end());
    }
}
