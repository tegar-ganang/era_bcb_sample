package com.knowgate.scheduler.jobs;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.sql.SQLException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.net.MalformedURLException;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.BodyPart;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.htmlparser.Parser;
import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.ParserException;
import org.htmlparser.tags.ImageTag;
import org.apache.oro.text.regex.*;
import com.knowgate.debug.DebugFile;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataxslt.FastStreamReplacer;
import com.knowgate.dfs.FileSystem;
import com.knowgate.scheduler.Atom;
import com.knowgate.scheduler.Job;

/**
 * <p>Add database fields to a document template and send it to a mail recipient</p>
 * <p>Mails are send using Sun JavaMail</p>
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class EmailSender extends Job {

    private boolean bHasReplacements;

    private SoftReference oFileStr;

    private FastStreamReplacer oReplacer;

    Session oMailSession;

    Transport oMailTransport;

    HashMap oDocumentImages;

    private SoftReference oHTMLStr;

    public EmailSender() {
        bHasReplacements = true;
        oFileStr = null;
        oHTMLStr = null;
        oReplacer = new FastStreamReplacer();
        oDocumentImages = new HashMap();
        oMailSession = null;
        oMailTransport = null;
    }

    /**
   * <p>Set Job Status</p>
   * <p>If Status if set to Job.STATUS_FINISHED then dt_finished is set to current
   * system date.</p>
   * <p>If Status if set to any value other than Job.STATUS_RUNNING then the MailTransport is closed.
   * @param oConn Database Connection
   * @param iStatus Job Status
   * @throws SQLException
   */
    public void setStatus(JDCConnection oConn, int iStatus) throws SQLException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin EmailSender.setStatus([Connection], " + String.valueOf(iStatus) + ")");
            DebugFile.incIdent();
        }
        super.setStatus(oConn, iStatus);
        if (Job.STATUS_RUNNING != iStatus) {
            if (oMailTransport != null) {
                try {
                    if (oMailTransport.isConnected()) oMailTransport.close();
                } catch (MessagingException msge) {
                    if (DebugFile.trace) DebugFile.writeln("Transport.close() MessagingException " + msge.getMessage());
                }
                oMailTransport = null;
            }
            if (null != oMailSession) oMailSession = null;
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End EMailSender.setStatus()");
        }
    }

    private String attachFiles(String sHTMLPath) throws FileNotFoundException, IOException {
        String sHtml = null;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin EmailSender.attachFiles(" + sHTMLPath + ")");
            DebugFile.incIdent();
            DebugFile.writeln("new File(" + sHTMLPath + ")");
        }
        try {
            FileSystem oFS = new FileSystem();
            sHtml = oFS.readfilestr(sHTMLPath, null);
            oFS = null;
        } catch (com.enterprisedt.net.ftp.FTPException ftpe) {
        }
        PatternMatcher oMatcher = new Perl5Matcher();
        PatternCompiler oCompiler = new Perl5Compiler();
        Parser parser = Parser.createParser(sHtml);
        StringBuffer oRetVal = new StringBuffer(sHtml.length());
        try {
            for (NodeIterator i = parser.elements(); i.hasMoreNodes(); ) {
                Node node = i.nextNode();
                if (node instanceof ImageTag) {
                    ImageTag oImgNode = (ImageTag) node;
                    String sSrc = oImgNode.extractImageLocn();
                    String sTag = oImgNode.getText();
                    Pattern oPattern;
                    try {
                        oPattern = oCompiler.compile(sSrc);
                    } catch (MalformedPatternException neverthrown) {
                        oPattern = null;
                    }
                    if (!oDocumentImages.containsKey(sSrc)) {
                        int iSlash = sSrc.lastIndexOf('/');
                        String sCid;
                        if (iSlash >= 0) {
                            while (sSrc.charAt(iSlash) == '/') {
                                if (++iSlash == sSrc.length()) break;
                            }
                            sCid = sSrc.substring(iSlash);
                        } else sCid = sSrc;
                        oDocumentImages.put(sSrc, sCid);
                    }
                    oRetVal.append(Util.substitute(oMatcher, oPattern, new Perl5Substitution("cid:" + oDocumentImages.get(sSrc), Perl5Substitution.INTERPOLATE_ALL), sTag, Util.SUBSTITUTE_ALL));
                } else {
                    oRetVal.append(node.getText());
                }
            }
        } catch (ParserException pe) {
            if (DebugFile.trace) {
                DebugFile.writeln("ParserException " + pe.getMessage());
            }
            oRetVal = new StringBuffer(sHtml.length());
            oRetVal.append(sHtml);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End EmailSender.attachFiles()");
        }
        return oRetVal.toString();
    }

    /**
   * <p>Send PageSet document instance by e-mail.</p>
   * <p>Transforming and sending aPageSet is a two stages task. First the PageSet
   * stylesheet is combined via XSLT with user defined XML data and an XHTML
   * document is pre-generated. This document still contains fixed database reference
   * tags. At second stage the database reference tags are replaced for each document
   * using FastStreamReplacer. Thus PageSet templates must have been previously
   * transformed via XSLT before sending the PageSet instance by e-mail.</p>
   * <p>This method uses javax.mail package for e-mail sending</p>
   * <p>Parameters for locating e-mail server are stored at properties
   * mail.transport.protocol, mail.host, mail.user from hipergate.cnf</p>
   * <p>If parameter bo_attachimages is set to "1" then any &lt;IMG SRC=""&gt; tag
   * will be replaced by a cid: reference to an attached file.</p>
   * @param oAtm Atom containing reference to PageSet.<br>
   * Atom must have the following parameters set:<br>
   * <table border=1 cellpadding=4>
   * <tr><td>gu_workarea</td><td>GUID of WorkArea owner of document to be sent</td></tr>
   * <tr><td>gu_pageset</td><td>GUID of PageSet to be sent</td></tr>
   * <tr><td>nm_pageset</td><td>Name of PageSet to be sent</td></tr>
   * <tr><td>bo_attachimages</td><td>"1" if must attach images on document,<br>"0" if images must be absolute references</td></tr>
   * <tr><td>tx_sender</td><td>Full Name of sender to be displayed</td></tr>
   * <tr><td>tx_from</td><td>Sender e-mail address</td></tr>
   * <tr><td>tx_subject</td><td>e-mail subject</td></tr>
   * </table>
   * @return String with document template after replacing database tags
   * @throws FileNotFoundException
   * @throws IOException
   * @throws MessagingException
   * @see com.knowgate.dataxslt.FastStreamReplacer
   */
    public Object process(Atom oAtm) throws FileNotFoundException, IOException, MessagingException {
        File oFile;
        FileReader oFileRead;
        String sPathHTML;
        char cBuffer[];
        StringBufferInputStream oInStrm;
        Object oReplaced;
        final String Yes = "1";
        final String sSep = System.getProperty("file.separator");
        if (DebugFile.trace) {
            DebugFile.writeln("Begin EMailSender.process([Job:" + getStringNull(DB.gu_job, "") + ", Atom:" + String.valueOf(oAtm.getInt(DB.pg_atom)) + "])");
            DebugFile.incIdent();
        }
        if (bHasReplacements) {
            sPathHTML = getProperty("workareasput");
            if (!sPathHTML.endsWith(sSep)) sPathHTML += sSep;
            sPathHTML += getParameter("gu_workarea") + sSep + "apps" + sSep + "Mailwire" + sSep + "html" + sSep + getParameter("gu_pageset") + sSep;
            sPathHTML += getParameter("nm_pageset").replace(' ', '_') + ".html";
            if (DebugFile.trace) DebugFile.writeln("PathHTML = " + sPathHTML);
            if (Yes.equals(getParameter("bo_attachimages"))) {
                if (DebugFile.trace) DebugFile.writeln("bo_attachimages=true");
                oInStrm = null;
                if (null != oHTMLStr) {
                    if (null != oHTMLStr.get()) oInStrm = new StringBufferInputStream((String) oHTMLStr.get());
                }
                if (null == oInStrm) oInStrm = new StringBufferInputStream(attachFiles(sPathHTML));
                oHTMLStr = new SoftReference(oInStrm);
                oReplaced = oReplacer.replace(oInStrm, oAtm.getItemMap());
            } else {
                if (DebugFile.trace) DebugFile.writeln("bo_attachimages=false");
                oReplaced = oReplacer.replace(sPathHTML, oAtm.getItemMap());
            }
            bHasReplacements = (oReplacer.lastReplacements() > 0);
        } else {
            oReplaced = null;
            if (null != oFileStr) oReplaced = oFileStr.get();
            if (null == oReplaced) {
                sPathHTML = getProperty("workareasput");
                if (!sPathHTML.endsWith(sSep)) sPathHTML += sSep;
                sPathHTML += getParameter("gu_workarea") + sSep + "apps" + sSep + "Mailwire" + sSep + "html" + sSep + getParameter("gu_pageset") + sSep + getParameter("nm_pageset").replace(' ', '_') + ".html";
                if (DebugFile.trace) DebugFile.writeln("PathHTML = " + sPathHTML);
                if (DebugFile.trace) DebugFile.writeln("new File(" + sPathHTML + ")");
                oFile = new File(sPathHTML);
                cBuffer = new char[new Long(oFile.length()).intValue()];
                oFileRead = new FileReader(oFile);
                oFileRead.read(cBuffer);
                oFileRead.close();
                if (DebugFile.trace) DebugFile.writeln(String.valueOf(cBuffer.length) + " characters readed");
                if (Yes.equals(getParameter("bo_attachimages"))) oReplaced = attachFiles(new String(cBuffer)); else oReplaced = new String(cBuffer);
                oFileStr = new SoftReference(oReplaced);
            }
        }
        if (null == oMailSession) {
            if (DebugFile.trace) DebugFile.writeln("Session.getInstance(Job.getProperties(), null)");
            java.util.Properties oMailProps = getProperties();
            if (oMailProps.getProperty("mail.transport.protocol") == null) oMailProps.put("mail.transport.protocol", "smtp");
            if (oMailProps.getProperty("mail.host") == null) oMailProps.put("mail.host", "localhost");
            oMailSession = Session.getInstance(getProperties(), null);
            if (null != oMailSession) {
                oMailTransport = oMailSession.getTransport();
                try {
                    oMailTransport.connect();
                } catch (NoSuchProviderException nspe) {
                    if (DebugFile.trace) DebugFile.writeln("MailTransport.connect() NoSuchProviderException " + nspe.getMessage());
                    throw new MessagingException(nspe.getMessage(), nspe);
                }
            }
        }
        MimeMessage oMsg;
        InternetAddress oFrom, oTo;
        try {
            if (null == getParameter("tx_sender")) oFrom = new InternetAddress(getParameter("tx_from")); else oFrom = new InternetAddress(getParameter("tx_from"), getParameter("tx_sender"));
            if (DebugFile.trace) DebugFile.writeln("to: " + oAtm.getStringNull(DB.tx_email, "ERROR Atom[" + String.valueOf(oAtm.getInt(DB.pg_atom)) + "].tx_email is null!"));
            oTo = new InternetAddress(oAtm.getString(DB.tx_email), oAtm.getStringNull(DB.tx_name, "") + " " + oAtm.getStringNull(DB.tx_surname, ""));
        } catch (AddressException adre) {
            if (DebugFile.trace) DebugFile.writeln("AddressException " + adre.getMessage() + " job " + getString(DB.gu_job) + " atom " + String.valueOf(oAtm.getInt(DB.pg_atom)));
            oFrom = null;
            oTo = null;
            throw new MessagingException("AddressException " + adre.getMessage() + " job " + getString(DB.gu_job) + " atom " + String.valueOf(oAtm.getInt(DB.pg_atom)));
        }
        if (DebugFile.trace) DebugFile.writeln("new MimeMessage([Session])");
        oMsg = new MimeMessage(oMailSession);
        oMsg.setSubject(getParameter("tx_subject"));
        oMsg.setFrom(oFrom);
        if (DebugFile.trace) DebugFile.writeln("MimeMessage.addRecipient(MimeMessage.RecipientType.TO, " + oTo.getAddress());
        oMsg.addRecipient(MimeMessage.RecipientType.TO, oTo);
        String sSrc = null, sCid = null;
        try {
            if (Yes.equals(getParameter("bo_attachimages"))) {
                BodyPart oMsgBodyPart = new MimeBodyPart();
                oMsgBodyPart.setContent(oReplaced, "text/html");
                MimeMultipart oMultiPart = new MimeMultipart("related");
                oMultiPart.addBodyPart(oMsgBodyPart);
                Iterator oImgs = oDocumentImages.keySet().iterator();
                while (oImgs.hasNext()) {
                    BodyPart oImgBodyPart = new MimeBodyPart();
                    sSrc = (String) oImgs.next();
                    sCid = (String) oDocumentImages.get(sSrc);
                    if (sSrc.startsWith("www.")) sSrc = "http://" + sSrc;
                    if (sSrc.startsWith("http://") || sSrc.startsWith("https://")) {
                        oImgBodyPart.setDataHandler(new DataHandler(new URL(sSrc)));
                    } else {
                        oImgBodyPart.setDataHandler(new DataHandler(new FileDataSource(sSrc)));
                    }
                    oImgBodyPart.setHeader("Content-ID", sCid);
                    oMultiPart.addBodyPart(oImgBodyPart);
                }
                if (DebugFile.trace) DebugFile.writeln("MimeMessage.setContent([MultiPart])");
                oMsg.setContent(oMultiPart);
            } else {
                if (DebugFile.trace) DebugFile.writeln("MimeMessage.setContent([String], \"text/html\")");
                oMsg.setContent(oReplaced, "text/html");
            }
            oMsg.saveChanges();
            if (DebugFile.trace) DebugFile.writeln("Transport.sendMessage([MimeMessage], MimeMessage.getAllRecipients())");
            oMailTransport.sendMessage(oMsg, oMsg.getAllRecipients());
            iPendingAtoms--;
        } catch (MalformedURLException urle) {
            if (DebugFile.trace) DebugFile.writeln("MalformedURLException " + sSrc);
            throw new MessagingException("MalformedURLException " + sSrc);
        }
        if (DebugFile.trace) {
            DebugFile.writeln("End EMailSender.process([Job:" + getStringNull(DB.gu_job, "") + ", Atom:" + String.valueOf(oAtm.getInt(DB.pg_atom)) + "])");
            DebugFile.decIdent();
        }
        return oReplaced;
    }
}
