package org.rhinojetty.brazil;

import org.rhinojetty.*;
import org.rhinojetty.iou.socket;
import org.rhinojetty.bin.shell;
import org.mozilla.javascript.*;
import com.syntelos.iou.*;
import java.net.InetAddress;
import java.io.*;

/**
 * A simple "Simple Mail Transfer Protocol" client representing a
 * single html, jsp or plain text message to a list of "To" recipients.
 *
 * @author John Pritchard
 */
public class email extends base {

    public static final String Mailer = chbuf.cat("X-Mailer: ", shell.RJ_VERSION_INFO);

    /**
     * Scriptable object constructor function.
     */
    public static Scriptable jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) throws JavaScriptException {
        email _email = new email();
        Scriptable scope = (Scriptable) ((null != ctorObj) ? (ctorObj.getParentScope()) : (null));
        if (null != scope) _email.setParentScope(scope);
        if (null != args) {
            int alen = args.length;
            if (0 < alen) {
                Object o = args[0];
                if (o instanceof NativeObject) {
                    NativeObject so = (NativeObject) o;
                    Object[] ids = so.getIds();
                    if (null != ids) {
                        int ilen = ids.length, fidx;
                        Object ido;
                        String idstr;
                        for (int cc = 0; cc < ilen; cc++) {
                            ido = ids[cc];
                            if (ido instanceof String) {
                                idstr = (String) ido;
                                fidx = _email.mapNameToId(idstr);
                                if (0 < fidx) _email.setIdValue(fidx, so.get(idstr, scope));
                            }
                        }
                    }
                }
            }
        }
        return _email;
    }

    private static final String MIMETYPE_DEFAULT = "text/plain ; charset='UTF-8'";

    private String from = null;

    private String replyto = null;

    private String subject = null;

    private String to = null;

    private bbuf body_buf = new bbuf();

    private file body_pg = null;

    private String mimetype = MIMETYPE_DEFAULT;

    private String mailserver = null;

    private String maildomain = null;

    private String ticket = null;

    private boolean bodyHeaders = false;

    private boolean debug = base.sysdebug_email;

    private static final boolean qmail = true;

    public email() {
        super();
    }

    public OutputStream body_bbo() {
        if (this.qmail) return new bbo.crlf(body_buf); else return new bbo(body_buf);
    }

    public Object jsGet_from() {
        return from;
    }

    public void jsSet_from(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        from = ScriptRuntime.toString(value);
    }

    public Object jsGet_replyto() {
        return replyto;
    }

    public void jsSet_replyto(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        replyto = ScriptRuntime.toString(value);
    }

    public Object jsGet_subject() {
        return subject;
    }

    public void jsSet_subject(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        subject = ScriptRuntime.toString(value);
    }

    public Object jsGet_to() {
        return to;
    }

    public void jsSet_to(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        to = ScriptRuntime.toString(value);
    }

    public Object jsGet_body() {
        if (1 > body_buf.length()) return Undefined.instance; else return body_buf.toString();
    }

    public void jsSet_body(Object value) throws JavaScriptException {
        if (value instanceof file) body_pg = (file) value; else body_buf.append(value);
    }

    public Object jsGet_mimetype() {
        return mimetype;
    }

    public void jsSet_mimetype(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        mimetype = ScriptRuntime.toString(value);
    }

    public Object jsGet_mailserver() {
        return mailserver;
    }

    public void jsSet_mailserver(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        mailserver = ScriptRuntime.toString(value);
    }

    public Object jsGet_maildomain() {
        return maildomain;
    }

    public void jsSet_maildomain(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        maildomain = ScriptRuntime.toString(value);
    }

    public Object jsGet_ticket() {
        return ticket;
    }

    public void jsSet_ticket(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        ticket = ScriptRuntime.toString(value);
    }

    public Object jsGet_bodyHeaders() {
        return wrap_boolean(bodyHeaders);
    }

    public void jsSet_bodyHeaders(Object value) throws JavaScriptException {
        if (value instanceof Wrapper) value = ((Wrapper) value).unwrap();
        bodyHeaders = ScriptRuntime.toBoolean(value);
    }

    public static Object jsFunction_send(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        email _email = this_email(thisObj);
        byte[] rewritten_html = null;
        if (null != _email.body_pg) {
            file pagef = _email.body_pg;
            if (pagef.canRead()) {
                String type = mimes.instance.getTypeFile(pagef.path());
                if (null != type && type.startsWith("text/html")) {
                    _email.mimetype = type;
                    OutputStream out = _email.body_bbo();
                    try {
                        request req = null;
                        boolean urlrewrite = true;
                        if (null != args && 0 < args.length) {
                            Object arg = args[0];
                            if (arg instanceof request) req = (request) arg; else throw new JavaScriptException("Sending HTML email requires a `req' request object argument.");
                            if (1 < args.length) {
                                arg = args[1];
                                if (arg instanceof Boolean) urlrewrite = ((Boolean) arg).booleanValue(); else throw new JavaScriptException("Second argument to `email.send(req,rewrite)' not recognized, must be boolean.");
                            }
                        } else throw new JavaScriptException("Sending HTML email requires a `req' request object argument.");
                        if (page.PC_page(req, pagef)) {
                            page.PC_send(cx, req, pagef, out);
                        } else {
                            InputStream in = pagef.getInputStream();
                            try {
                                long flen = pagef.length();
                                int read, buflen;
                                if (flen > 102400) buflen = 4096; else if (flen < 8192) buflen = 512; else buflen = 1024;
                                byte[] buf = new byte[buflen];
                                while (0 < (read = in.read(buf, 0, buflen))) out.write(buf, 0, read);
                            } finally {
                                in.close();
                            }
                        }
                        if (urlrewrite) {
                            rewritten_html = _email.body_buf.toByteArray();
                            if (null != rewritten_html) {
                                String sessid = req.getRequestSessionID();
                                if (null != sessid) rewritten_html = request.HtmlUrlRewrite(rewritten_html, req.srv.jsGet_defaultUrl(), req.urlTermSessid()); else throw new JavaScriptException("BBBUGGGG! Email page resource `" + pagef + "' error; no session identifier in request.");
                            } else throw new JavaScriptException("BBBUGGGG! Email page resource `" + pagef + "' error; no content in body?");
                        }
                    } catch (IOException iox) {
                        throw new JavaScriptException("Email page resource `" + pagef + "' error (" + iox + ").");
                    }
                } else {
                    if (null == type) throw new JavaScriptException("Email page resource `" + pagef + "' content type not found."); else throw new JavaScriptException("Email page resource `" + pagef + "' content type `" + type + "' not supported (not HTML).");
                }
            } else throw new JavaScriptException("Email page resource `" + pagef + "' not found.");
        }
        if (null == _email.mailserver) {
            InetAddress host = aprops.ServerMail();
            if (null != host) _email.mailserver = host.getHostName(); else throw new JavaScriptException("Sending an email requires a `mailserver'.");
        }
        if (null == _email.from || 0 > _email.from.indexOf("@")) throw new JavaScriptException("Sending an email requires a complete `from' address."); else if (null == _email.to) throw new JavaScriptException("Sending an email requires one or more addressees in the `to' field."); else {
            String[] to_list = address(_email.to);
            String[] from_list = address(_email.from);
            socket sock = new socket();
            if (_email.debug) sock.debug = true;
            sock.hostname = _email.mailserver;
            sock.portnum = 25;
            String smtp_req, smtp_rep, s;
            int cc, len;
            Object[] args0 = new Object[0];
            Object[] args1 = new Object[1];
            try {
                smtp_rep = (String) sock._readLine(cx, args0, null);
            } catch (JavaScriptException iox) {
                throw new JavaScriptException("Mailhost (" + _email.mailserver + ") is not an SMTP server.");
            }
            if ('2' != smtp_rep.charAt(0)) {
                args1[0] = "QUIT";
                try {
                    sock._println(cx, args1, null);
                } catch (Throwable t) {
                }
                sock.jsFunction_close(cx, sock, args0, null);
                throw new JavaScriptException("SMTP HELO? " + smtp_rep);
            }
            if (null == _email.maildomain) {
                _email.maildomain = aprops.instance.getProperty("mail-domain");
                if (null == _email.maildomain) {
                    _email.maildomain = aprops.LocalHostNameDomain();
                }
            }
            if (null != _email.maildomain) {
                smtp_req = chbuf.cat("HELO ", _email.maildomain);
                args1[0] = smtp_req;
                sock._println(cx, args1, null);
                smtp_rep = (String) sock._readLine(cx, args0, null);
                if ('2' != smtp_rep.charAt(0)) {
                    args1[0] = "QUIT";
                    try {
                        sock._println(cx, args1, null);
                    } catch (Throwable t) {
                    }
                    sock.jsFunction_close(cx, sock, args0, null);
                    throw new JavaScriptException(smtp_req + ", SMTP response " + smtp_rep);
                }
            }
            String tmp;
            tmp = from_list[0];
            if (-1 < tmp.indexOf('<')) smtp_req = chbuf.cat("MAIL FROM: ", tmp); else smtp_req = chbuf.cat("MAIL FROM: <", tmp, ">");
            args1[0] = smtp_req;
            sock._println(cx, args1, null);
            smtp_rep = (String) sock._readLine(cx, args0, null);
            if ('2' != smtp_rep.charAt(0)) {
                args1[0] = "QUIT";
                try {
                    sock._println(cx, args1, null);
                } catch (Throwable t) {
                }
                sock.jsFunction_close(cx, sock, args0, null);
                throw new JavaScriptException(smtp_req + ", SMTP response " + smtp_rep);
            }
            if (null != to_list) {
                len = to_list.length;
                for (cc = 0; cc < len; cc++) {
                    tmp = to_list[cc];
                    if (-1 < tmp.indexOf('<')) smtp_req = chbuf.cat("RCPT TO: ", tmp); else smtp_req = chbuf.cat("RCPT TO: <", tmp, ">");
                    args1[0] = smtp_req;
                    sock._println(cx, args1, null);
                    smtp_rep = (String) sock._readLine(cx, args0, null);
                    if ('2' != smtp_rep.charAt(0)) {
                        args1[0] = "QUIT";
                        try {
                            sock._println(cx, args1, null);
                        } catch (Throwable t) {
                        }
                        sock.jsFunction_close(cx, sock, args0, null);
                        throw new JavaScriptException(smtp_req + ", SMTP response " + smtp_rep);
                    }
                }
            } else {
                args1[0] = "QUIT";
                try {
                    sock._println(cx, args1, null);
                } catch (Throwable t) {
                }
                sock.jsFunction_close(cx, sock, args0, null);
                throw new JavaScriptException("Missing Mail To.");
            }
            args1[0] = "DATA";
            sock._println(cx, args1, null);
            smtp_rep = (String) sock._readLine(cx, args0, null);
            if ('2' != smtp_rep.charAt(0) && '3' != smtp_rep.charAt(0)) {
                args1[0] = "QUIT";
                try {
                    sock._println(cx, args1, null);
                } catch (Throwable t) {
                }
                sock.jsFunction_close(cx, sock, args0, null);
                throw new JavaScriptException("DATA Start, SMTP response " + smtp_rep);
            }
            args1[0] = chbuf.cat("From: ", _email.from);
            sock._println(cx, args1, null);
            if (null != _email.replyto) {
                args1[0] = chbuf.cat("Reply-To: ", _email.replyto);
                sock._println(cx, args1, null);
            }
            args1[0] = "To: " + _email.to;
            sock._println(cx, args1, null);
            if (null != _email.subject) {
                args1[0] = chbuf.cat("Subject: ", _email.subject);
                sock._println(cx, args1, null);
            }
            args1[0] = Mailer;
            sock._println(cx, args1, null);
            if (_email.mimetype.equals("text/html")) {
                args1[0] = "Content-Type: text/html ;charset=us-ascii";
                sock._println(cx, args1, null);
                args1[0] = "Content-Transfer-Encoding: 7bit";
                sock._println(cx, args1, null);
            } else {
                args1[0] = chbuf.cat("Content-Type: ", _email.mimetype);
                sock._println(cx, args1, null);
            }
            args1[0] = "MIME-Version: 1.0";
            sock._println(cx, args1, null);
            args1[0] = chbuf.cat("Date: ", new java.util.Date().toGMTString());
            sock._println(cx, args1, null);
            if (!_email.bodyHeaders) {
                sock._println(cx, args0, null);
            }
            if (null != rewritten_html) args1[0] = rewritten_html; else args1[0] = _email.body_buf.toByteArray();
            Object contentlength = sock._println(cx, args1, null);
            sock._println(cx, args0, null);
            args1[0] = ".";
            sock._println(cx, args1, null);
            smtp_rep = (String) sock._readLine(cx, args0, null);
            if (smtp_rep.startsWith("250")) {
                String[] endline = linebuf.toStringArray(smtp_rep, " ");
                if (null != endline) {
                    if (4 < endline.length) _email.ticket = endline[2]; else if (2 < endline.length) _email.ticket = endline[1];
                }
                args1[0] = "QUIT";
                sock._println(cx, args1, null);
                smtp_rep = (String) sock._readLine(cx, args0, null);
                sock.jsFunction_close(cx, sock, args0, null);
                if (null != _email.ticket) return _email.ticket; else return null;
            } else {
                args1[0] = "QUIT";
                sock._println(cx, args1, null);
                sock.jsFunction_close(cx, sock, args0, null);
                throw new JavaScriptException("DATA Terminal, SMTP response " + smtp_rep);
            }
        }
    }

    public static Object jsFunction_println(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        email _email = this_email(thisObj);
        int bytes = _email.body_buf.length();
        _email.body_buf.println(args);
        return new Integer(_email.body_buf.length() - bytes);
    }

    public static Object jsFunction_print(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        email _email = this_email(thisObj);
        int bytes = _email.body_buf.length();
        _email.body_buf.append(args);
        return new Integer(_email.body_buf.length() - bytes);
    }

    public static Object jsFunction_debug(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        email _email = this_email(thisObj);
        _email.debug = (!_email.debug);
        if (_email.debug) return Boolean.TRUE; else return Boolean.FALSE;
    }

    public String[][] helpary() {
        return HELPARY;
    }

    private static final String[][] HELPARY = { { "", "An `email' object can send a message to one or more addresses.  This does not support attachments, but does support HTML and plain text email.  See `mimetype' which is also set automatically for file based message bodies (see `body')." }, null, { "from", "The sender's email address." }, { "replyto", "An optional email address that any reply to this message should go to -- typically employed when that address would not be identical to the `from' sender." }, { "subject", "The short message descriptor." }, { "to", "The `to' list of comma delimited email addressees." }, { "body", "The message body (text) for sending.  This can be set to a `file' object to use a file or JSP page for the message body.  A JSP page file body must be sent using the send function with the server request object argument, as in `em.send(req)'." }, { "mimetype", "The content type for the message text, default `text/plain'." }, { "mailserver", "An SMTP mailserver that will relay outbound mail from the server host." }, { "maildomain", "The domain name of this web- servlet host.  Without it, the recipient receives an authentication warning." }, { "ticket", "An optional `sent' mail receipt from the mail server." }, { "bodyHeaders", "A boolean that can be enabled to allow the body text to include mail headers.  After the mail headers must be a CRLF before the proper body." }, { "send()", "Send the plain text message to its addressees." }, { "send(req)", "Send the html page message to its addressees, rewriting the html for the sender's session." }, { "send(req,rw)", "Send the html page message to its addressees, with a second boolean argument for whether to rewrite the page for the sender's session." }, { "print(s)", "Print string argument into message body." }, { "println(s)", "Print string argument with a new line into message body." }, { "debug()", "Toggle SMTP conversation tracing for this email.  The global \"debug('email')\" command toggles debugging for new emails, while this command \"em.debug()\" toggles debugging for a email object." } };

    public String getClassName() {
        return "email";
    }

    protected static email this_email(Scriptable thisObj) throws JavaScriptException {
        Scriptable obj = thisObj, m;
        while (obj != null) {
            m = obj;
            do {
                if (m instanceof email) return (email) m;
                m = m.getPrototype();
            } while (m != null);
            obj = obj.getParentScope();
        }
        throw new JavaScriptException("Can't resolve email instance.");
    }

    public static final String[] address(String addrfield) {
        if (null == addrfield) {
            if (base.sysdebug_email) System.err.println("EMAIL ADDRESS is null.");
            return null;
        } else {
            linebuf lb = new linebuf();
            String[] src = linebuf.toStringArray(addrfield, ",\r\n");
            if (null != src) {
                int srclen = src.length;
                String s;
                for (int cc = 0; cc < srclen; cc++) {
                    s = src[cc].trim();
                    if (1 < s.indexOf("@")) lb.append(s); else if (base.sysdebug_email) System.err.println("EMAIL ADDRESS dropping token (" + s + ").");
                }
            } else if (base.sysdebug_email) System.err.println("EMAIL ADDRESS empty (" + addrfield + ").");
            return lb.toStringArray();
        }
    }

    public int mapNameToId(String s) {
        int id = 0;
        L0: {
            id = 0;
            String X = null;
            int c;
            L: switch(s.length()) {
                case 2:
                    if (s.charAt(0) == 't' && s.charAt(1) == 'o') {
                        id = Id_to;
                        break L0;
                    }
                    break L;
                case 4:
                    c = s.charAt(0);
                    if (c == 'b') {
                        X = "body";
                        id = Id_body;
                    } else if (c == 'f') {
                        X = "from";
                        id = Id_from;
                    } else if (c == 's') {
                        X = "send";
                        id = Id_send;
                    }
                    break L;
                case 5:
                    c = s.charAt(0);
                    if (c == 'd') {
                        X = "debug";
                        id = Id_debug;
                    } else if (c == 'p') {
                        X = "print";
                        id = Id_print;
                    }
                    break L;
                case 6:
                    X = "ticket";
                    id = Id_ticket;
                    break L;
                case 7:
                    c = s.charAt(0);
                    if (c == 'p') {
                        X = "println";
                        id = Id_println;
                    } else if (c == 'r') {
                        X = "replyto";
                        id = Id_replyto;
                    } else if (c == 's') {
                        X = "subject";
                        id = Id_subject;
                    }
                    break L;
                case 8:
                    X = "mimetype";
                    id = Id_mimetype;
                    break L;
                case 10:
                    c = s.charAt(4);
                    if (c == 'd') {
                        X = "maildomain";
                        id = Id_maildomain;
                    } else if (c == 's') {
                        X = "mailserver";
                        id = Id_mailserver;
                    }
                    break L;
                case 11:
                    c = s.charAt(0);
                    if (c == 'b') {
                        X = "bodyHeaders";
                        id = Id_bodyHeaders;
                    } else if (c == 'c') {
                        X = "constructor";
                        id = Id_constructor;
                    }
                    break L;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
        }
        return id;
    }

    private static final int Id_constructor = 1, Id_send = 2, Id_println = 3, Id_print = 4, Id_debug = 5, LAST_METHOD_ID = 5, Id_from = 6, Id_replyto = 7, Id_subject = 8, Id_to = 9, Id_body = 10, Id_mimetype = 11, Id_mailserver = 12, Id_maildomain = 13, Id_ticket = 14, Id_bodyHeaders = 15, MAX_INSTANCE_ID = 15;

    public String getIdName(int id) {
        switch(id) {
            case Id_constructor:
                return "constructor";
            case Id_send:
                return "send";
            case Id_println:
                return "println";
            case Id_print:
                return "print";
            case Id_debug:
                return "debug";
            case Id_from:
                return "from";
            case Id_replyto:
                return "replyto";
            case Id_subject:
                return "subject";
            case Id_to:
                return "to";
            case Id_body:
                return "body";
            case Id_mimetype:
                return "mimetype";
            case Id_mailserver:
                return "mailserver";
            case Id_maildomain:
                return "maildomain";
            case Id_ticket:
                return "ticket";
            case Id_bodyHeaders:
                return "bodyHeaders";
            default:
                return null;
        }
    }

    public Object execMethod(int methodId, IdFunction f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) throws JavaScriptException {
        switch(methodId) {
            case Id_constructor:
                return jsConstructor(cx, args, f, (null == thisObj));
            case Id_send:
                return jsFunction_send(cx, this, args, f);
            case Id_println:
                return jsFunction_println(cx, this, args, f);
            case Id_print:
                return jsFunction_print(cx, this, args, f);
            case Id_debug:
                return jsFunction_debug(cx, this, args, f);
            default:
                return super.execMethod(methodId, f, cx, scope, thisObj, args);
        }
    }

    public int methodArity(int methodId) {
        switch(methodId) {
            case Id_constructor:
                return -1;
            case Id_send:
                return -1;
            case Id_println:
                return -1;
            case Id_print:
                return -1;
            case Id_debug:
                return -1;
            default:
                return super.methodArity(methodId);
        }
    }

    public Object getIdValue(int id) {
        if (id > LAST_METHOD_ID) {
            try {
                switch(id) {
                    case Id_from:
                        return jsGet_from();
                    case Id_replyto:
                        return jsGet_replyto();
                    case Id_subject:
                        return jsGet_subject();
                    case Id_to:
                        return jsGet_to();
                    case Id_body:
                        return jsGet_body();
                    case Id_mimetype:
                        return jsGet_mimetype();
                    case Id_mailserver:
                        return jsGet_mailserver();
                    case Id_maildomain:
                        return jsGet_maildomain();
                    case Id_ticket:
                        return jsGet_ticket();
                    case Id_bodyHeaders:
                        return jsGet_bodyHeaders();
                    default:
                        return super.getIdValue(id);
                }
            } catch (Exception exc) {
                return null;
            }
        } else return super.getIdValue(id);
    }

    public void setIdValue(int id, Object value) {
        if (id > LAST_METHOD_ID) {
            try {
                switch(id) {
                    case Id_body:
                        jsSet_body(value);
                        return;
                    case Id_bodyHeaders:
                        jsSet_bodyHeaders(value);
                        return;
                    case Id_from:
                        jsSet_from(value);
                        return;
                    case Id_maildomain:
                        jsSet_maildomain(value);
                        return;
                    case Id_mailserver:
                        jsSet_mailserver(value);
                        return;
                    case Id_mimetype:
                        jsSet_mimetype(value);
                        return;
                    case Id_replyto:
                        jsSet_replyto(value);
                        return;
                    case Id_subject:
                        jsSet_subject(value);
                        return;
                    case Id_ticket:
                        jsSet_ticket(value);
                        return;
                    case Id_to:
                        jsSet_to(value);
                        return;
                    default:
                        super.setIdValue(id, value);
                        return;
                }
            } catch (Exception exc) {
                return;
            }
        }
    }

    public int getIdDefaultAttributes(int id) {
        switch(id) {
            case Id_body:
                return PERMANENT | DONTENUM;
            case Id_bodyHeaders:
                return PERMANENT | DONTENUM;
            case Id_from:
                return PERMANENT | DONTENUM;
            case Id_maildomain:
                return PERMANENT | DONTENUM;
            case Id_mailserver:
                return PERMANENT | DONTENUM;
            case Id_mimetype:
                return PERMANENT | DONTENUM;
            case Id_replyto:
                return PERMANENT | DONTENUM;
            case Id_subject:
                return PERMANENT | DONTENUM;
            case Id_ticket:
                return PERMANENT | DONTENUM;
            case Id_to:
                return PERMANENT | DONTENUM;
            default:
                return super.getIdDefaultAttributes(id);
        }
    }

    public int maxInstanceId() {
        return MAX_INSTANCE_ID;
    }
}
