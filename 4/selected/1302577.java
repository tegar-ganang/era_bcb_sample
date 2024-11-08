package openr66.protocol.http;

import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import openr66.context.ErrorCode;
import openr66.context.R66Session;
import openr66.context.filesystem.R66Dir;
import openr66.database.DbConstant;
import openr66.database.DbPreparedStatement;
import openr66.database.DbSession;
import openr66.database.data.DbTaskRunner;
import openr66.database.data.AbstractDbData.UpdatedInfo;
import openr66.database.data.DbTaskRunner.TASKSTEP;
import openr66.database.exception.OpenR66DatabaseException;
import openr66.database.exception.OpenR66DatabaseNoConnectionError;
import openr66.database.exception.OpenR66DatabaseSqlError;
import openr66.protocol.configuration.Configuration;
import openr66.protocol.exception.OpenR66Exception;
import openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import openr66.protocol.localhandler.LocalChannelReference;
import openr66.protocol.utils.OpenR66SignalHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

/**
 * Handler for HTTP information support
 *
 * @author Frederic Bregier
 *
 */
@ChannelPipelineCoverage("one")
public class HttpHandler extends SimpleChannelUpstreamHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(HttpHandler.class);

    public static final int LIMITROW = 60;

    public final R66Session authentHttp = new R66Session();

    public static final ConcurrentHashMap<String, R66Dir> usedDir = new ConcurrentHashMap<String, R66Dir>();

    private volatile HttpRequest request;

    private final StringBuilder responseContent = new StringBuilder();

    private volatile HttpResponseStatus status;

    private volatile String uriRequest;

    private static final String sINFO = "INFO", sCOMMAND = "COMMAND", sNB = "NB";

    /**
     * The Database connection attached to this NetworkChannel shared among all
     * associated LocalChannels
     */
    private DbSession dbSession;

    /**
     * Does this dbSession is private and so should be closed
     */
    private boolean isPrivateDbSession = false;

    /**
     * Set header
     */
    private void startHeader() {
        responseContent.append("<html>");
        responseContent.append("<head>");
        responseContent.append("<title>OpenR66 Web Information Server</title>\r\n");
    }

    /**
     * Set inline header if refresh function is needed (every 10 seconds or so)
     */
    private void inlineHeader() {
        responseContent.append("<noscript><meta http-equiv='refresh' content=11></noscript>");
        responseContent.append("<script language='JavaScript'><!--\r\n");
        responseContent.append("var sURL = unescape(window.location.pathname);");
        responseContent.append("function doLoad(){setTimeout( 'refresh()', 10000 );}");
        responseContent.append("function refresh(){window.location.href = sURL;}");
        responseContent.append("//-->\r\n</script>\r\n");
        responseContent.append("<script language='JavaScript1.1'><!--\r\n");
        responseContent.append("function refresh(){window.location.replace( sURL );}");
        responseContent.append("//-->\r\n</script>\r\n");
        responseContent.append("<script language='JavaScript1.2'><!--\r\n");
        responseContent.append("function refresh(){window.location.reload( false );}");
        responseContent.append("//-->\r\n</script>\r\n");
    }

    /**
     * End header
     */
    private void endHeader() {
        responseContent.append("</head>\r\n");
    }

    /**
     * In case refresh function is used
     */
    private void replacementBody() {
        responseContent.append("<body onload='doLoad()'>\r\n");
    }

    /**
     * End Body
     */
    private void endBody() {
        responseContent.append("</body>");
        responseContent.append("</html>");
    }

    /**
     * Front of Body for valid requests
     */
    private void inlineBody() {
        responseContent.append("<table border=\"0\">");
        responseContent.append("<tr>");
        responseContent.append("<td>");
        responseContent.append("<A href='/'><h1>OpenR66 Page for Information: ");
        responseContent.append(Configuration.configuration.HOST_ID);
        responseContent.append("</h1></A>");
        responseContent.append("</td>");
        responseContent.append("<td>");
        responseContent.append("</td>");
        responseContent.append("</tr>\r\n");
        responseContent.append("<tr>");
        responseContent.append("<td>" + (new Date()).toString());
        responseContent.append("</td>");
        responseContent.append("</tr>\r\n");
        responseContent.append("<tr>");
        responseContent.append("<td>");
        responseContent.append("Number of local active connections: " + Configuration.configuration.getLocalTransaction().getNumberLocalChannel());
        responseContent.append("</td>");
        responseContent.append("<td>");
        responseContent.append("Number of network active connections: " + OpenR66SignalHandler.getNbConnection());
        responseContent.append("</td>");
        responseContent.append("</tr>");
        responseContent.append("</table>\r\n");
        responseContent.append("<CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
    }

    /**
     * Create Menu
     */
    private void createMenu() {
        startHeader();
        endHeader();
        responseContent.append("<body bgcolor=white><style>td{font-size: 12pt;}</style>");
        responseContent.append("<table border=\"0\">");
        responseContent.append("<tr>");
        responseContent.append("<td>");
        responseContent.append("<h1>OpenR66 Page for Information: " + Configuration.configuration.HOST_ID + "</h1>");
        responseContent.append("You must fill all of the following fields.");
        responseContent.append("</td>");
        responseContent.append("</tr>");
        responseContent.append("</table>\r\n");
        responseContent.append("<CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
        responseContent.append("<A HREF='/active'>Active Runners</A><BR>");
        responseContent.append("<A HREF='/error'>Error Runners</A><BR>");
        responseContent.append("<A HREF='/done'>Done Runners</A><BR>");
        responseContent.append("<A HREF='/all'>All Runners</A><BR>");
        responseContent.append("<FORM ACTION=\"" + uriRequest + "\" METHOD=\"GET\">");
        responseContent.append("<input type=hidden name=" + sCOMMAND + " value=\"GET\">");
        responseContent.append("<table border=\"0\">");
        responseContent.append("<tr><td>One Choice 0=Active 1=Error 2=Done 3=All: <br> <input type=text name=\"" + sINFO + "\" size=1></td></tr>");
        responseContent.append("<tr><td>Number of runners (0 for all): <br> <input type=text name=\"" + sNB + "\" size=5>");
        responseContent.append("</td></tr>");
        responseContent.append("<tr><td><INPUT TYPE=\"submit\" NAME=\"Send\" VALUE=\"Send\"></INPUT></td>");
        responseContent.append("<td><INPUT TYPE=\"reset\" NAME=\"Clear\" VALUE=\"Clear\" ></INPUT></td></tr>");
        responseContent.append("</table></FORM>\r\n");
        responseContent.append("<CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
        endBody();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        status = HttpResponseStatus.OK;
        try {
            if (DbConstant.admin.isConnected) {
                this.dbSession = new DbSession(DbConstant.admin, false);
                this.isPrivateDbSession = true;
            }
        } catch (OpenR66DatabaseNoConnectionError e1) {
            logger.warn("Use default database connection");
            this.dbSession = DbConstant.admin.session;
        }
        HttpRequest request = this.request = (HttpRequest) e.getMessage();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        uriRequest = queryStringDecoder.getPath();
        char cval = 'z';
        if (uriRequest.equalsIgnoreCase("/active")) {
            cval = '0';
        } else if (uriRequest.equalsIgnoreCase("/error")) {
            cval = '1';
        } else if (uriRequest.equalsIgnoreCase("/done")) {
            cval = '2';
        } else if (uriRequest.equalsIgnoreCase("/all")) {
            cval = '3';
        } else if (uriRequest.equalsIgnoreCase("/status")) {
            cval = '4';
        }
        Map<String, List<String>> params = null;
        if (request.getMethod() == HttpMethod.GET) {
            params = queryStringDecoder.getParameters();
        } else if (request.getMethod() == HttpMethod.POST) {
            ChannelBuffer content = request.getContent();
            if (content.readable()) {
                String param = content.toString("UTF-8");
                queryStringDecoder = new QueryStringDecoder("/?" + param);
            } else {
                createMenu();
                writeResponse(e);
                return;
            }
            params = queryStringDecoder.getParameters();
        }
        int nb = LIMITROW;
        boolean getMenu = (cval == 'z');
        String value = null;
        if (!params.isEmpty()) {
            if (getMenu && params.containsKey(sCOMMAND)) {
                List<String> values = params.get(sCOMMAND);
                if (values != null && params.get(sCOMMAND).get(0).equals("GET")) {
                    if (params.containsKey(sINFO)) {
                        values = params.get(sINFO);
                        if (values != null) {
                            value = values.get(0);
                            if (value == null || value.length() == 0) {
                                getMenu = true;
                            } else {
                                getMenu = false;
                                cval = value.charAt(0);
                            }
                        }
                    }
                }
            }
            if (params.containsKey(sNB)) {
                List<String> values = params.get(sNB);
                if (values != null) {
                    value = values.get(0);
                    if (value != null && value.length() != 0) {
                        nb = Integer.parseInt(value);
                    }
                }
            }
        }
        if (getMenu) {
            createMenu();
        } else {
            switch(cval) {
                case '0':
                    active(ctx, nb);
                    break;
                case '1':
                    error(ctx, nb);
                    break;
                case '2':
                    done(ctx, nb);
                    break;
                case '3':
                    all(ctx, nb);
                    break;
                case '4':
                    status(ctx, nb);
                    break;
                default:
                    createMenu();
            }
        }
        writeResponse(e);
    }

    /**
     * Add all runners from preparedStatement for type
     *
     * @param preparedStatement
     * @param type
     * @param nb
     * @throws OpenR66DatabaseNoConnectionError
     * @throws OpenR66DatabaseSqlError
     */
    private void addRunners(DbPreparedStatement preparedStatement, String type, int nb) throws OpenR66DatabaseNoConnectionError, OpenR66DatabaseSqlError {
        try {
            preparedStatement.executeQuery();
            responseContent.append("<style>td{font-size: 8pt;}</style><table border=\"2\">");
            responseContent.append("<tr><td>");
            responseContent.append(type);
            responseContent.append("</td>");
            responseContent.append(DbTaskRunner.headerHtml());
            responseContent.append("</tr>\r\n");
            int i = 0;
            while (preparedStatement.getNext()) {
                DbTaskRunner taskRunner = DbTaskRunner.getFromStatement(preparedStatement);
                responseContent.append("<tr><td>");
                responseContent.append(taskRunner.isSender() ? "S" : "R");
                responseContent.append("</td>");
                LocalChannelReference lcr = Configuration.configuration.getLocalTransaction().getFromRequest(taskRunner.getKey());
                responseContent.append(taskRunner.toHtml(authentHttp, lcr != null ? "Active" : "NotActive"));
                responseContent.append("</tr>\r\n");
                if (nb > 0) {
                    i++;
                    if (i >= nb) {
                        break;
                    }
                }
            }
            responseContent.append("</table><br>\r\n");
        } finally {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
        }
    }

    /**
     * print all active transfers
     *
     * @param ctx
     * @param nb
     */
    private void active(ChannelHandlerContext ctx, int nb) {
        startHeader();
        inlineHeader();
        endHeader();
        replacementBody();
        inlineBody();
        DbPreparedStatement preparedStatement = null;
        try {
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, ErrorCode.Running, nb);
            addRunners(preparedStatement, ErrorCode.Running.mesg, nb);
            preparedStatement = DbTaskRunner.getUpdatedPrepareStament(dbSession, UpdatedInfo.INTERRUPTED, true, nb);
            addRunners(preparedStatement, UpdatedInfo.INTERRUPTED.name(), nb);
            preparedStatement = DbTaskRunner.getUpdatedPrepareStament(dbSession, UpdatedInfo.TOSUBMIT, true, nb);
            addRunners(preparedStatement, UpdatedInfo.TOSUBMIT.name(), nb);
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, ErrorCode.InitOk, nb);
            addRunners(preparedStatement, ErrorCode.InitOk.mesg, nb);
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, ErrorCode.PreProcessingOk, nb);
            addRunners(preparedStatement, ErrorCode.PreProcessingOk.mesg, nb);
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, ErrorCode.TransferOk, nb);
            addRunners(preparedStatement, ErrorCode.TransferOk.mesg, nb);
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, ErrorCode.PostProcessingOk, nb);
            addRunners(preparedStatement, ErrorCode.PostProcessingOk.mesg, nb);
            preparedStatement = null;
        } catch (OpenR66DatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.warn("OpenR66 Web Error", e);
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
            return;
        }
        endBody();
    }

    /**
     * print all transfers in error
     *
     * @param ctx
     * @param nb
     */
    private void error(ChannelHandlerContext ctx, int nb) {
        startHeader();
        inlineHeader();
        endHeader();
        replacementBody();
        inlineBody();
        DbPreparedStatement preparedStatement = null;
        try {
            preparedStatement = DbTaskRunner.getUpdatedPrepareStament(dbSession, UpdatedInfo.INERROR, true, nb / 2);
            addRunners(preparedStatement, UpdatedInfo.INERROR.name(), nb / 2);
            preparedStatement = DbTaskRunner.getUpdatedPrepareStament(dbSession, UpdatedInfo.INTERRUPTED, true, nb / 2);
            addRunners(preparedStatement, UpdatedInfo.INTERRUPTED.name(), nb / 2);
            preparedStatement = DbTaskRunner.getStepPrepareStament(dbSession, TASKSTEP.ERRORTASK, nb / 4);
            addRunners(preparedStatement, TASKSTEP.ERRORTASK.name(), nb / 4);
        } catch (OpenR66DatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.warn("OpenR66 Web Error", e);
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
            return;
        }
        endBody();
    }

    /**
     * Print all done transfers
     *
     * @param ctx
     * @param nb
     */
    private void done(ChannelHandlerContext ctx, int nb) {
        startHeader();
        inlineHeader();
        endHeader();
        replacementBody();
        inlineBody();
        DbPreparedStatement preparedStatement = null;
        try {
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, ErrorCode.CompleteOk, nb);
            addRunners(preparedStatement, ErrorCode.CompleteOk.mesg, nb);
        } catch (OpenR66DatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.warn("OpenR66 Web Error", e);
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
            return;
        }
        endBody();
    }

    /**
     * Print all nb last transfers
     *
     * @param ctx
     * @param nb
     */
    private void all(ChannelHandlerContext ctx, int nb) {
        startHeader();
        inlineHeader();
        endHeader();
        replacementBody();
        inlineBody();
        DbPreparedStatement preparedStatement = null;
        try {
            preparedStatement = DbTaskRunner.getStatusPrepareStament(dbSession, null, nb);
            addRunners(preparedStatement, "ALL RUNNERS: " + nb, nb);
        } catch (OpenR66DatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.warn("OpenR66 Web Error", e);
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
            return;
        }
        endBody();
    }

    /**
     * print only status
     *
     * @param ctx
     * @param nb
     */
    private void status(ChannelHandlerContext ctx, int nb) {
        startHeader();
        endHeader();
        replacementBody();
        inlineBody();
        DbPreparedStatement preparedStatement = null;
        try {
            preparedStatement = DbTaskRunner.getUpdatedPrepareStament(dbSession, UpdatedInfo.INERROR, true, 1);
            try {
                preparedStatement.executeQuery();
                if (preparedStatement.getNext()) {
                    responseContent.append("<p>Some Transfers are in ERROR</p><br>");
                    status = HttpResponseStatus.CONFLICT;
                }
            } finally {
                if (preparedStatement != null) {
                    preparedStatement.realClose();
                }
            }
            preparedStatement = DbTaskRunner.getUpdatedPrepareStament(dbSession, UpdatedInfo.INTERRUPTED, true, 1);
            try {
                preparedStatement.executeQuery();
                if (preparedStatement.getNext()) {
                    responseContent.append("<p>Some Transfers are INTERRUPTED</p><br>");
                    status = HttpResponseStatus.CONFLICT;
                }
            } finally {
                if (preparedStatement != null) {
                    preparedStatement.realClose();
                }
            }
            preparedStatement = DbTaskRunner.getStepPrepareStament(dbSession, TASKSTEP.ERRORTASK, 1);
            try {
                preparedStatement.executeQuery();
                if (preparedStatement.getNext()) {
                    responseContent.append("<p>Some Transfers are in ERRORTASK</p><br>");
                    status = HttpResponseStatus.CONFLICT;
                }
            } finally {
                if (preparedStatement != null) {
                    preparedStatement.realClose();
                }
            }
            if (status != HttpResponseStatus.CONFLICT) {
                responseContent.append("<p>No problem is found in Transfers</p><br>");
            }
        } catch (OpenR66DatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.warn("OpenR66 Web Error", e);
            sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
            return;
        }
        endBody();
    }

    /**
     * Write the response
     *
     * @param e
     */
    private void writeResponse(MessageEvent e) {
        ChannelBuffer buf = ChannelBuffers.copiedBuffer(responseContent.toString(), "UTF-8");
        responseContent.setLength(0);
        boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION)) || request.getProtocolVersion().equals(HttpVersion.HTTP_1_0) && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION));
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setContent(buf);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
        if (HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION))) {
            response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        if (!close) {
            response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(buf.readableBytes()));
        }
        String cookieString = request.getHeader(HttpHeaders.Names.COOKIE);
        if (cookieString != null) {
            CookieDecoder cookieDecoder = new CookieDecoder();
            Set<Cookie> cookies = cookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                CookieEncoder cookieEncoder = new CookieEncoder(true);
                for (Cookie cookie : cookies) {
                    cookieEncoder.addCookie(cookie);
                }
                response.addHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
            }
        }
        ChannelFuture future = e.getChannel().write(response);
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
        if (this.isPrivateDbSession && dbSession != null) {
            dbSession.disconnect();
            dbSession = null;
        }
    }

    /**
     * Send an error and close
     *
     * @param ctx
     * @param status
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
        responseContent.setLength(0);
        startHeader();
        endHeader();
        responseContent.append("OpenR66 Web Failure: ");
        responseContent.append(status.toString());
        endBody();
        response.setContent(ChannelBuffers.copiedBuffer(responseContent.toString(), "UTF-8"));
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        OpenR66Exception exception = OpenR66ExceptionTrappedFactory.getExceptionFromTrappedException(e.getChannel(), e);
        if (exception != null) {
            if (!(exception instanceof OpenR66ProtocolBusinessNoWriteBackException)) {
                if (e.getCause() instanceof IOException) {
                    if (this.isPrivateDbSession && dbSession != null) {
                        dbSession.disconnect();
                        dbSession = null;
                    }
                    return;
                }
                logger.warn("Exception in HttpHandler", exception);
            }
            if (e.getChannel().isConnected()) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            }
        } else {
            if (this.isPrivateDbSession && dbSession != null) {
                dbSession.disconnect();
                dbSession = null;
            }
            return;
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelClosed(ctx, e);
        if (this.isPrivateDbSession && dbSession != null) {
            dbSession.disconnect();
        }
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        authentHttp.getAuth().specialNoSessionAuth(false, Configuration.configuration.HOST_ID);
        super.channelConnected(ctx, e);
        ChannelGroup group = Configuration.configuration.getHttpChannelGroup();
        if (group != null) {
            group.add(e.getChannel());
        }
    }
}
