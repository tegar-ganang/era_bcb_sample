package org.compiere.util;

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import javax.mail.internet.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.ecs.*;
import org.apache.ecs.xhtml.*;
import org.compiere.model.*;

/**
 *  Servlet Utilities
 *
 *  @author Jorg Janke
 *  @version  $Id: WebUtil.java,v 1.7 2006/09/24 12:11:54 comdivision Exp $
 */
public final class WebUtil {

    /**	Static Logger	*/
    private static CLogger log = CLogger.getCLogger(WebUtil.class);

    /**
	 *  Create Timeout Message
	 *
	 *  @param request request
	 *  @param response response
	 *  @param servlet servlet
	 *  @param message - optional message
	 *  @throws ServletException
	 *  @throws IOException
	 */
    public static void createTimeoutPage(HttpServletRequest request, HttpServletResponse response, HttpServlet servlet, String message) throws ServletException, IOException {
        log.info(message);
        WebSessionCtx wsc = WebSessionCtx.get(request);
        String windowTitle = "Timeout";
        if (wsc != null) windowTitle = Msg.getMsg(wsc.ctx, "Timeout");
        WebDoc doc = WebDoc.create(windowTitle);
        body body = doc.getBody();
        if (message != null && message.length() > 0) body.addElement(new p(message, AlignType.CENTER));
        body.addElement(getLoginButton(wsc == null ? null : wsc.ctx));
        body.addElement(new hr());
        body.addElement(new small(servlet.getClass().getName()));
        createResponse(request, response, servlet, null, doc, false);
    }

    /**
	 *  Create Error Message
	 *
	 *  @param request request
	 *  @param response response
	 *  @param servlet servlet
	 *  @param message message
	 *  @throws ServletException
	 *  @throws IOException
	 */
    public static void createErrorPage(HttpServletRequest request, HttpServletResponse response, HttpServlet servlet, String message) throws ServletException, IOException {
        log.info(message);
        WebSessionCtx wsc = WebSessionCtx.get(request);
        String windowTitle = "Error";
        if (wsc != null) windowTitle = Msg.getMsg(wsc.ctx, "Error");
        if (message != null) windowTitle += ": " + message;
        WebDoc doc = WebDoc.create(windowTitle);
        body b = doc.getBody();
        b.addElement(new p(servlet.getServletName(), AlignType.CENTER));
        b.addElement(new br());
        createResponse(request, response, servlet, null, doc, false);
    }

    /**
	 *  Create Exit Page "Log-off".
	 *  <p>
	 *  - End Session
	 *  - Go to start page (e.g. /adempiere/index.html)
	 *
	 *  @param request request
	 *  @param response response
	 *  @param servlet servlet
	 *  @param ctx context
	 *  @param AD_Message messahe
	 *  @throws ServletException
	 *  @throws IOException
	 */
    public static void createLoginPage(HttpServletRequest request, HttpServletResponse response, HttpServlet servlet, Properties ctx, String AD_Message) throws ServletException, IOException {
        request.getSession().invalidate();
        String url = WebEnv.getBaseDirectory("index.html");
        WebDoc doc = null;
        if (ctx != null && AD_Message != null && !AD_Message.equals("")) doc = WebDoc.create(Msg.getMsg(ctx, AD_Message)); else if (AD_Message != null) doc = WebDoc.create(AD_Message); else doc = WebDoc.create(false);
        script script = new script("window.top.location.replace('" + url + "');");
        doc.getBody().addElement(script);
        createResponse(request, response, servlet, null, doc, false);
    }

    /**
	 *  Create Login Button - replace Window
	 *
	 *  @param ctx context
	 *  @return Button
	 */
    public static input getLoginButton(Properties ctx) {
        String text = "Login";
        if (ctx != null) text = Msg.getMsg(ctx, "Login");
        input button = new input("button", text, "  " + text);
        button.setID(text);
        button.setClass("loginbtn");
        StringBuffer cmd = new StringBuffer("window.top.location.replace('");
        cmd.append(WebEnv.getBaseDirectory("index.html"));
        cmd.append("');");
        button.setOnClick(cmd.toString());
        return button;
    }

    /**************************************************************************
	 *  Get Cookie Properties
	 *
	 *  @param request request
	 *  @return Properties
	 */
    public static Properties getCookieProprties(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(WebEnv.COOKIE_INFO)) return propertiesDecode(cookies[i].getValue());
            }
        }
        return new Properties();
    }

    /**
	 *  Get String Parameter.
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return string or null
	 */
    public static String getParameter(HttpServletRequest request, String parameter) {
        if (request == null || parameter == null) return null;
        String enc = request.getCharacterEncoding();
        try {
            if (enc == null) {
                request.setCharacterEncoding(WebEnv.ENCODING);
                enc = request.getCharacterEncoding();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, e);
            enc = request.getCharacterEncoding();
        }
        String data = request.getParameter(parameter);
        if (data == null || data.length() == 0) return data;
        if (enc != null && !WebEnv.ENCODING.equals(enc)) {
            try {
                String dataEnc = new String(data.getBytes(enc), WebEnv.ENCODING);
                log.log(Level.FINER, "Convert " + data + " (" + enc + ")-> " + dataEnc + " (" + WebEnv.ENCODING + ")");
                data = dataEnc;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Convert " + data + " (" + enc + ")->" + WebEnv.ENCODING);
            }
        }
        String inStr = data;
        StringBuffer outStr = new StringBuffer();
        int i = inStr.indexOf("&#");
        while (i != -1) {
            outStr.append(inStr.substring(0, i));
            inStr = inStr.substring(i + 2, inStr.length());
            int j = inStr.indexOf(';');
            if (j < 0) {
                inStr = "&#" + inStr;
                break;
            }
            String token = inStr.substring(0, j);
            try {
                int intToken = Integer.parseInt(token);
                outStr.append((char) intToken);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Token=" + token, e);
                outStr.append("&#").append(token).append(";");
            }
            inStr = inStr.substring(j + 1, inStr.length());
            i = inStr.indexOf("&#");
        }
        outStr.append(inStr);
        String retValue = outStr.toString();
        log.finest(parameter + "=" + data + " -> " + retValue);
        return retValue;
    }

    /**
	 *  Get integer Parameter - 0 if not defined.
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return int result or 0
	 */
    public static int getParameterAsInt(HttpServletRequest request, String parameter) {
        if (request == null || parameter == null) return 0;
        String data = getParameter(request, parameter);
        if (data == null || data.length() == 0) return 0;
        try {
            return Integer.parseInt(data);
        } catch (Exception e) {
            log.warning(parameter + "=" + data + " - " + e);
        }
        return 0;
    }

    /**
	 *  Get numeric Parameter - 0 if not defined
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return big decimal result or 0
	 */
    public static BigDecimal getParameterAsBD(HttpServletRequest request, String parameter) {
        if (request == null || parameter == null) return Env.ZERO;
        String data = getParameter(request, parameter);
        if (data == null || data.length() == 0) return Env.ZERO;
        try {
            return new BigDecimal(data);
        } catch (Exception e) {
        }
        try {
            DecimalFormat format = DisplayType.getNumberFormat(DisplayType.Number);
            Object oo = format.parseObject(data);
            if (oo instanceof BigDecimal) return (BigDecimal) oo; else if (oo instanceof Number) return new BigDecimal(((Number) oo).doubleValue());
            return new BigDecimal(oo.toString());
        } catch (Exception e) {
            log.fine(parameter + "=" + data + " - " + e);
        }
        return Env.ZERO;
    }

    /**
	 *  Get date Parameter - null if not defined.
	 *	Date portion only
	 *  @param request request
	 *  @param parameter parameter
	 *  @return timestamp result or null
	 */
    public static Timestamp getParameterAsDate(HttpServletRequest request, String parameter) {
        return getParameterAsDate(request, parameter, null);
    }

    /**
	 *  Get date Parameter - null if not defined.
	 *	Date portion only
	 *  @param request request
	 *  @param parameter parameter
	 *  @param language optional language
	 *  @return timestamp result or null
	 */
    public static Timestamp getParameterAsDate(HttpServletRequest request, String parameter, Language language) {
        if (request == null || parameter == null) return null;
        String data = getParameter(request, parameter);
        if (data == null || data.length() == 0) return null;
        if (language != null) {
            try {
                DateFormat format = DisplayType.getDateFormat(DisplayType.Date, language);
                java.util.Date date = format.parse(data);
                if (date != null) return new Timestamp(date.getTime());
            } catch (Exception e) {
            }
        }
        try {
            SimpleDateFormat format = DisplayType.getDateFormat(DisplayType.Date);
            java.util.Date date = format.parse(data);
            if (date != null) return new Timestamp(date.getTime());
        } catch (Exception e) {
        }
        try {
            return Timestamp.valueOf(data);
        } catch (Exception e) {
        }
        log.warning(parameter + " - cannot parse: " + data);
        return null;
    }

    /**
	 *  Get boolean Parameter.
	 *  @param request request
	 *  @param parameter parameter
	 *  @return true if found
	 */
    public static boolean getParameterAsBoolean(HttpServletRequest request, String parameter) {
        return getParameterAsBoolean(request, parameter, null);
    }

    /**
	 *  Get boolean Parameter.
	 *  @param request request
	 *  @param parameter parameter
	 *  @param expected optional expected value
	 *  @return true if found and if optional value matches
	 */
    public static boolean getParameterAsBoolean(HttpServletRequest request, String parameter, String expected) {
        if (request == null || parameter == null) return false;
        String data = getParameter(request, parameter);
        if (data == null || data.length() == 0) return false;
        if (expected == null) return true;
        return expected.equalsIgnoreCase(data);
    }

    /**
     * 	get Parameter or Null fi empty
     *	@param request request
     *	@param parameter parameter
     *	@return Request Value or null
     */
    public static String getParamOrNull(HttpServletRequest request, String parameter) {
        String value = WebUtil.getParameter(request, parameter);
        if (value == null) return value;
        if (value.length() == 0) return null;
        return value;
    }

    /**
     * 	reload
     *	@param logMessage
     *	@param jsp
     *	@param session
     *	@param request
     *	@param response
     *	@param thisContext
     *	@throws ServletException
     *	@throws IOException
     */
    public static void reload(String logMessage, String jsp, HttpSession session, HttpServletRequest request, HttpServletResponse response, ServletContext thisContext) throws ServletException, IOException {
        session.setAttribute(WebSessionCtx.HDR_MESSAGE, logMessage);
        log.warning(" - " + logMessage + " - update not confirmed");
        thisContext.getRequestDispatcher(jsp).forward(request, response);
    }

    /**************************************************************************
	 *  Create Standard Response Header with optional Cookie and print document.
	 *  D:\j2sdk1.4.0\docs\guide\intl\encoding.doc.html
	 *
	 *  @param request request
	 *  @param response response
	 *  @param servlet servlet
	 *  @param cookieProperties cookie properties
	 *  @param doc doc
	 *  @param debug debug
	 *  @throws IOException
	 */
    public static void createResponse(HttpServletRequest request, HttpServletResponse response, HttpServlet servlet, Properties cookieProperties, WebDoc doc, boolean debug) throws IOException {
        response.setHeader("Cache-Control", "no-cache");
        response.setContentType("text/html; charset=UTF-8");
        if (cookieProperties != null) {
            Cookie cookie = new Cookie(WebEnv.COOKIE_INFO, propertiesEncode(cookieProperties));
            cookie.setComment("(c) adempiere, Inc - Jorg Janke");
            cookie.setSecure(false);
            cookie.setPath("/");
            if (cookieProperties.size() == 0) cookie.setMaxAge(0); else cookie.setMaxAge(2592000);
            response.addCookie(cookie);
        }
        if (debug && WebEnv.DEBUG) {
            WebEnv.addFooter(request, response, servlet, doc.getBody());
        }
        PrintWriter out = response.getWriter();
        doc.output(out);
        out.flush();
        if (out.checkError()) log.log(Level.SEVERE, "error writing");
        out.close();
    }

    /**************************************************************************
	 *  Create Java Script to clear Target frame
	 *
	 *  @param targetFrame target frame
	 *  @return Clear Frame Script
	 */
    public static script getClearFrame(String targetFrame) {
        StringBuffer cmd = new StringBuffer();
        cmd.append("// <!-- clear frame\n").append("var d = parent.").append(targetFrame).append(".document;\n").append("d.open();\n").append("d.write('<link href=\"").append(WebEnv.getStylesheetURL()).append("\" type=\"text/css\" rel=\"stylesheet\">');\n").append("d.write('<link href=\"/adempiere/css/window.css\" type=\"text/css\" rel=\"stylesheet\">');\n").append("d.write('<br><br><br><br><br><br><br>');").append("d.write('<div style=\"text-align: center;\"><img class=\"CenterImage\" style=\"vertical-align: middle; filter:alpha(opacity=50); -moz-opacity:0.5;\" src=\"Logo.gif\" /></div>');\n").append("d.close();\n").append("// -- clear frame -->");
        return new script(cmd.toString());
    }

    /**
	 * 	Return a link and script with new location.
	 * 	@param url forward url
	 * 	@param delaySec delay in seconds (default 3)
	 * 	@return html
	 */
    public static HtmlCode getForward(String url, int delaySec) {
        if (delaySec <= 0) delaySec = 3;
        HtmlCode retValue = new HtmlCode();
        a a = new a(url);
        a.addElement(url);
        retValue.addElement(a);
        script script = new script("setTimeout(\"window.top.location.replace('" + url + "')\"," + (delaySec + 1000) + ");");
        retValue.addElement(script);
        return retValue;
    }

    /**
	 * 	Create Forward Page
	 * 	@param response response
	 * 	@param title page title
	 * 	@param forwardURL url
	 * 	@param delaySec delay in seconds (default 3)
	 * 	@throws ServletException
	 * 	@throws IOException
	 */
    public static void createForwardPage(HttpServletResponse response, String title, String forwardURL, int delaySec) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        WebDoc doc = WebDoc.create(title);
        body b = doc.getBody();
        b.addElement(getForward(forwardURL, delaySec));
        PrintWriter out = response.getWriter();
        doc.output(out);
        out.flush();
        if (out.checkError()) log.log(Level.SEVERE, "Error writing");
        out.close();
        log.fine(forwardURL + " - " + title);
    }

    /**
	 * 	Does Test exist
	 *	@param test string
	 *	@return true if String with data
	 */
    public static boolean exists(String test) {
        if (test == null) return false;
        return test.length() > 0;
    }

    /**
	 * 	Does Parameter exist
	 * 	@param request request
	 *	@param parameter string
	 *	@return true if String with data
	 */
    public static boolean exists(HttpServletRequest request, String parameter) {
        if (request == null || parameter == null) return false;
        try {
            String enc = request.getCharacterEncoding();
            if (enc == null) request.setCharacterEncoding(WebEnv.ENCODING);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, e);
        }
        return exists(request.getParameter(parameter));
    }

    /**
	 *	Is EMail address valid
	 * 	@param email mail address
	 * 	@return true if valid
	 */
    public static boolean isEmailValid(String email) {
        if (email == null || email.length() == 0) return false;
        try {
            InternetAddress ia = new InternetAddress(email, true);
            if (ia != null) return true;
        } catch (AddressException ex) {
            log.warning(email + " - " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**************************************************************************
	 *  Decode Properties into String (URL encoded)
	 *
	 *  @param pp properties
	 *  @return Encoded String
	 */
    public static String propertiesEncode(Properties pp) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            pp.store(bos, "adempiere");
        } catch (IOException e) {
            log.log(Level.SEVERE, "store", e);
        }
        String result = new String(bos.toByteArray());
        try {
            result = URLEncoder.encode(result, WebEnv.ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "encode" + WebEnv.ENCODING, e);
            String enc = System.getProperty("file.encoding");
            try {
                result = URLEncoder.encode(result, enc);
                log.info("encode: " + enc);
            } catch (Exception ex) {
                log.log(Level.SEVERE, "encode", ex);
            }
        }
        return result;
    }

    /**
	 *  Decode data String (URL encoded) into Properties
	 *
	 *  @param data data
	 *  @return Properties
	 */
    public static Properties propertiesDecode(String data) {
        String result = null;
        try {
            result = URLDecoder.decode(data, WebEnv.ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "decode" + WebEnv.ENCODING, e);
            String enc = System.getProperty("file.encoding");
            try {
                result = URLEncoder.encode(data, enc);
                log.log(Level.SEVERE, "decode: " + enc);
            } catch (Exception ex) {
                log.log(Level.SEVERE, "decode", ex);
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(result.getBytes());
        Properties pp = new Properties();
        try {
            pp.load(bis);
        } catch (IOException e) {
            log.log(Level.SEVERE, "load", e);
        }
        return pp;
    }

    /**************************************************************************
	 *  Convert Array of NamePair to HTTP Option Array.
	 *  <p>
	 *  If the ArrayList does not contain NamePairs, the String value is used
	 *  @see org.compiere.util.NamePair
	 *  @param  list    ArrayList containing NamePair values
	 *  @param  default_ID  Sets the default if the key/ID value is found.
	 *      If the value is null or empty, the first value is selected
	 *  @return Option Array
	 */
    public static option[] convertToOption(NamePair[] list, String default_ID) {
        int size = list.length;
        option[] retValue = new option[size];
        for (int i = 0; i < size; i++) {
            boolean selected = false;
            if (i == 0 && (default_ID == null || default_ID.length() == 0)) selected = true;
            String name = Util.maskHTML(list[i].getName());
            retValue[i] = new option(list[i].getID()).addElement(name);
            if (default_ID != null && default_ID.equals(list[i].getID())) selected = true;
            retValue[i].setSelected(selected);
        }
        return retValue;
    }

    /**
	 *  Create label/field table row
	 *
	 *  @param line - null for new line (table row)
	 *  @param FORMNAME form name
	 *  @param PARAMETER parameter name
	 *  @param labelText label
	 *  @param inputType HTML input type
	 *  @param value data value
	 *  @param sizeDisplay display size
	 *  @param size data size
	 *  @param longField field spanning two columns
	 *  @param mandatory mark as mandatory
	 *  @param onChange onChange call
	 *  @param script script
	 *  @return tr table row
	 */
    public static tr createField(tr line, String FORMNAME, String PARAMETER, String labelText, String inputType, Object value, int sizeDisplay, int size, boolean longField, boolean mandatory, String onChange, StringBuffer script) {
        if (line == null) line = new tr();
        String labelInfo = labelText;
        if (mandatory) {
            labelInfo += "&nbsp;<font color=\"red\">*</font>";
            String fName = "document." + FORMNAME + "." + PARAMETER;
            script.append(fName).append(".required=true; ");
        }
        label llabel = new label().setFor(PARAMETER).addElement(labelInfo);
        llabel.setID("ID_" + PARAMETER + "_Label");
        line.addElement(new td().addElement(llabel).setAlign(AlignType.RIGHT));
        input iinput = new input(inputType, PARAMETER, value == null ? "" : value.toString());
        iinput.setSize(sizeDisplay).setMaxlength(size);
        iinput.setID("ID_" + PARAMETER);
        if (onChange != null && onChange.length() > 0) iinput.setOnChange(onChange);
        iinput.setTitle(labelText);
        td field = new td().addElement(iinput).setAlign(AlignType.LEFT);
        if (longField) field.setColSpan(3);
        line.addElement(field);
        return line;
    }

    /**
	 * 	Get Close PopUp Buton
	 *	@return button
	 */
    public static input createClosePopupButton(Properties ctx) {
        String text = "Close";
        if (ctx != null) text = Msg.getMsg(ctx, "Close");
        input close = new input("button", text, "  " + text);
        close.setID(text);
        close.setClass("closebtn");
        close.setTitle("Close PopUp");
        close.setOnClick("self.close();return false;");
        return close;
    }

    /**
	 * 	Stream Attachment Entry
	 *	@param response response
	 *	@param attachment attachment
	 *	@param attachmentIndex logical index
	 *	@return error message or null
	 */
    public static String streamAttachment(HttpServletResponse response, MAttachment attachment, int attachmentIndex) {
        if (attachment == null) return "No Attachment";
        int realIndex = -1;
        MAttachmentEntry[] entries = attachment.getEntries();
        for (int i = 0; i < entries.length; i++) {
            MAttachmentEntry entry = entries[i];
            if (entry.getIndex() == attachmentIndex) {
                realIndex = i;
                break;
            }
        }
        if (realIndex < 0) {
            log.fine("No Attachment Entry for Index=" + attachmentIndex + " - " + attachment);
            return "Attachment Entry not found";
        }
        MAttachmentEntry entry = entries[realIndex];
        if (entry.getData() == null) {
            log.fine("Empty Attachment Entry for Index=" + attachmentIndex + " - " + attachment);
            return "Attachment Entry empty";
        }
        try {
            int bufferSize = 2048;
            int fileLength = entry.getData().length;
            response.setContentType(entry.getContentType());
            response.setBufferSize(bufferSize);
            response.setContentLength(fileLength);
            log.fine(entry.toString());
            long time = System.currentTimeMillis();
            ServletOutputStream out = response.getOutputStream();
            out.write(entry.getData());
            out.flush();
            out.close();
            time = System.currentTimeMillis() - time;
            double speed = (fileLength / 1024) / ((double) time / 1000);
            log.info("Length=" + fileLength + " - " + time + " ms - " + speed + " kB/sec - " + entry.getContentType());
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.toString());
            return "Streaming error - " + ex;
        }
        return null;
    }

    /**
	 * 	Stream File
	 *	@param response response
	 *	@param file file to stream
	 *	@return error message or null
	 */
    public static String streamFile(HttpServletResponse response, File file) {
        if (file == null) return "No File";
        if (!file.exists()) return "File not found: " + file.getAbsolutePath();
        MimeType mimeType = MimeType.get(file.getAbsolutePath());
        try {
            int bufferSize = 2048;
            int fileLength = (int) file.length();
            response.setContentType(mimeType.getMimeType());
            response.setBufferSize(bufferSize);
            response.setContentLength(fileLength);
            log.fine(file.toString());
            long time = System.currentTimeMillis();
            FileInputStream in = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream();
            int c = 0;
            while ((c = in.read()) != -1) out.write(c);
            out.flush();
            out.close();
            in.close();
            time = System.currentTimeMillis() - time;
            double speed = (fileLength / 1024) / ((double) time / 1000);
            log.info("Length=" + fileLength + " - " + time + " ms - " + speed + " kB/sec - " + mimeType);
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.toString());
            return "Streaming error - " + ex;
        }
        return null;
    }

    /**
	 * 	Remove Cookie with web user by setting user to _
	 * 	@param request request (for context path)
	 * 	@param response response to add cookie
	 */
    public static void deleteCookieWebUser(HttpServletRequest request, HttpServletResponse response, String COOKIE_NAME) {
        Cookie cookie = new Cookie(COOKIE_NAME, " ");
        cookie.setComment("adempiere Web User");
        cookie.setPath(request.getContextPath());
        cookie.setMaxAge(1);
        response.addCookie(cookie);
    }

    /**************************************************************************
	 * 	Send EMail
	 *	@param request request
	 *	@param to web user
	 *	@param msgType see MMailMsg.MAILMSGTYPE_*
	 *	@param parameter object array with parameters
	 * 	@return mail EMail.SENT_OK or error message 
	 */
    public static String sendEMail(HttpServletRequest request, WebUser to, String msgType, Object[] parameter) {
        WebSessionCtx wsc = WebSessionCtx.get(request);
        MStore wStore = wsc.wstore;
        MMailMsg mailMsg = wStore.getMailMsg(msgType);
        StringBuffer subject = new StringBuffer(mailMsg.getSubject());
        if (parameter.length > 0 && parameter[0] != null) subject.append(parameter[0]);
        StringBuffer message = new StringBuffer();
        String hdr = wStore.getEMailFooter();
        if (hdr != null && hdr.length() > 0) message.append(hdr).append("\n");
        message.append(mailMsg.getMessage());
        if (parameter.length > 1 && parameter[1] != null) message.append(parameter[1]);
        if (mailMsg.getMessage2() != null) {
            message.append("\n").append(mailMsg.getMessage2());
            if (parameter.length > 2 && parameter[2] != null) message.append(parameter[2]);
        }
        if (mailMsg.getMessage3() != null) {
            message.append("\n").append(mailMsg.getMessage3());
            if (parameter.length > 3 && parameter[3] != null) message.append(parameter[3]);
        }
        message.append(MRequest.SEPARATOR).append("http://").append(request.getServerName()).append(request.getContextPath()).append("/ - ").append(wStore.getName()).append("\n").append("Request from: ").append(getFrom(request)).append("\n");
        String ftr = wStore.getEMailFooter();
        if (ftr != null && ftr.length() > 0) message.append(ftr);
        EMail email = wStore.createEMail(to.getEmail(), subject.toString(), message.toString());
        if (msgType.equals(MMailMsg.MAILMSGTYPE_OrderAcknowledgement)) {
            String orderEMail = wStore.getWebOrderEMail();
            String storeEMail = wStore.getWStoreEMail();
            if (orderEMail != null && orderEMail.length() > 0 && !orderEMail.equals(storeEMail)) email.addBcc(orderEMail);
        }
        String retValue = email.send();
        MUserMail um = new MUserMail(mailMsg, to.getAD_User_ID(), email);
        um.save();
        return retValue;
    }

    /**
	 * 	Get Remote From info
	 * 	@param request request
	 * 	@return remore info
	 */
    public static String getFrom(HttpServletRequest request) {
        String host = request.getRemoteHost();
        if (!host.equals(request.getRemoteAddr())) host += " (" + request.getRemoteAddr() + ")";
        return host;
    }

    /**
	 * 	Add Cookie with web user
	 * 	@param request request (for context path)
	 * 	@param response response to add cookie
	 * 	@param webUser email address
	 */
    public static void addCookieWebUser(HttpServletRequest request, HttpServletResponse response, String webUser, String COOKIE_NAME) {
        Cookie cookie = new Cookie(COOKIE_NAME, webUser);
        cookie.setComment("adempiere Web User");
        cookie.setPath(request.getContextPath());
        cookie.setMaxAge(2592000);
        response.addCookie(cookie);
    }

    /**
	 * 	Resend Validation Code
	 * 	@param request request
	 *	@param wu user
	 */
    public static void resendCode(HttpServletRequest request, WebUser wu) {
        String msg = sendEMail(request, wu, MMailMsg.MAILMSGTYPE_UserVerification, new Object[] { request.getServerName(), wu.getName(), wu.getEMailVerifyCode() });
        if (EMail.SENT_OK.equals(msg)) wu.setPasswordMessage("EMail sent"); else wu.setPasswordMessage("Problem sending EMail: " + msg);
    }

    /**
	 * 	Update Web User
	 * 	@param request request
	 * 	@param wu user
	 * 	@param updateEMailPwd if true, change email/password
	 * 	@return true if saved
	 */
    public static boolean updateFields(HttpServletRequest request, WebUser wu, boolean updateEMailPwd) {
        if (updateEMailPwd) {
            String s = WebUtil.getParameter(request, "PasswordNew");
            wu.setPasswordMessage(null);
            wu.setPassword(s);
            if (wu.getPasswordMessage() != null) {
                return false;
            }
            s = WebUtil.getParameter(request, "EMail");
            if (!WebUtil.isEmailValid(s)) {
                wu.setPasswordMessage("EMail Invalid");
                return false;
            }
            wu.setEmail(s.trim());
        }
        StringBuffer mandatory = new StringBuffer();
        String s = WebUtil.getParameter(request, "Name");
        if (s != null && s.length() != 0) wu.setName(s.trim()); else mandatory.append(" - Name");
        s = WebUtil.getParameter(request, "Company");
        if (s != null && s.length() != 0) wu.setCompany(s);
        s = WebUtil.getParameter(request, "Title");
        if (s != null && s.length() != 0) wu.setTitle(s);
        s = WebUtil.getParameter(request, "Address");
        if (s != null && s.length() != 0) wu.setAddress(s); else mandatory.append(" - Address");
        s = WebUtil.getParameter(request, "Address2");
        if (s != null && s.length() != 0) wu.setAddress2(s);
        s = WebUtil.getParameter(request, "City");
        if (s != null && s.length() != 0) wu.setCity(s); else mandatory.append(" - City");
        s = WebUtil.getParameter(request, "Postal");
        if (s != null && s.length() != 0) wu.setPostal(s); else mandatory.append(" - Postal");
        s = WebUtil.getParameter(request, "C_Country_ID");
        if (s != null && s.length() != 0) wu.setC_Country_ID(s);
        s = WebUtil.getParameter(request, "C_Region_ID");
        if (s != null && s.length() != 0) wu.setC_Region_ID(s);
        s = WebUtil.getParameter(request, "RegionName");
        if (s != null && s.length() != 0) wu.setRegionName(s);
        s = WebUtil.getParameter(request, "Phone");
        if (s != null && s.length() != 0) wu.setPhone(s);
        s = WebUtil.getParameter(request, "Phone2");
        if (s != null && s.length() != 0) wu.setPhone2(s);
        s = WebUtil.getParameter(request, "C_BP_Group_ID");
        if (s != null && s.length() != 0) wu.setC_BP_Group_ID(s);
        s = WebUtil.getParameter(request, "Fax");
        if (s != null && s.length() != 0) wu.setFax(s);
        if (mandatory.length() > 0) {
            mandatory.insert(0, "Enter Mandatory");
            wu.setSaveErrorMessage(mandatory.toString());
            return false;
        }
        return wu.save();
    }
}
