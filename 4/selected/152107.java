package org.homedns.krolain.MochaJournal;

import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.nio.charset.Charset;
import org.homedns.krolain.MochaJournal.LJData.OldEventInfo;
import java.util.regex.*;
import java.net.URL;
import org.homedns.krolain.util.InstallInfo;
import org.homedns.krolain.MochaJournal.LJData.*;
import org.homedns.krolain.MochaJournal.Protocol.XMLProtocol;
import org.homedns.krolain.MochaJournal.Protocol.ProtProgress;

/**
 *
 * @author  jsmith
 */
public class HTMLExport extends DefaultHandler implements java.lang.Runnable {

    private static final String DEF_TEMPLT = "/org/homedns/krolain/MochaJournal/template.xml";

    String m_szXMLText = "";

    String m_szDocHead = "";

    String m_szDocFoot = "";

    String m_szPostHead = "";

    String m_szPostFoot = "";

    String m_szPostEvent = "";

    String m_szPrevLink = "";

    String m_szNextLink = "";

    String m_szPageLink = "";

    String m_szPic = "";

    String m_szCommHead = "";

    String m_szThreadHead = "";

    String m_szCommIndent = "";

    String m_szCommBody = "";

    String m_szThreadFoot = "";

    String m_szCommFoot = "";

    String m_szCommLink = "";

    String m_szDateFormat = null;

    int m_iIndent = 0;

    int m_iCurElem = -1;

    int m_iCurPage = -1;

    int m_iEpP = -1;

    boolean m_bAscend = false;

    boolean m_bExportAll = true;

    String m_szBasePath = null;

    String m_szUsername = null;

    String m_szPageName = null;

    Object[] m_EventstoExport = null;

    boolean m_bExternal = true;

    String m_szUserDir = "";

    boolean m_bExportComments = false;

    int m_iExportTo = 0;

    java.awt.Frame m_Parent;

    LJeventsTable m_EventTable = null;

    OldEventInfo[] m_EventsExport = null;

    ProtProgress m_PM = null;

    /** Creates a new instance of HTMLExport */
    Writer m_Writer = null;

    public HTMLExport(boolean bExternal, java.awt.Frame parent) {
        m_iCurElem = -1;
        m_iCurPage = -1;
        m_szDocHead = "";
        m_szDocFoot = "";
        m_szPostHead = "";
        m_szPostFoot = "";
        m_szPostEvent = "";
        m_bExternal = bExternal;
        m_Parent = parent;
    }

    public boolean NewDocument(String szFilename, String szUname, java.awt.Frame parent) {
        InputStream inTemplate = null;
        m_iCurPage = -1;
        if (szFilename != null) {
            int idx = szFilename.lastIndexOf(System.getProperty("file.separator"));
            m_szBasePath = szFilename.substring(0, idx + 1);
            m_szUserDir = szFilename.substring(0, idx + 1);
            m_szPageName = szFilename.substring(idx + 1);
        }
        m_szDocHead = "";
        m_szDocFoot = "";
        m_szPostHead = "";
        m_szPostFoot = "";
        m_szPostEvent = "";
        m_szUsername = szUname;
        m_bExportAll = true;
        if (m_bExternal) {
            JHTMLExportDlg dlg = new JHTMLExportDlg(parent, true, null);
            dlg.setBasePath(m_szBasePath);
            dlg.show();
            if (dlg.getExitCode() == dlg.CANCEL) return false;
            m_szBasePath = dlg.getBasePath();
            m_bExportAll = dlg.exportAllEvents();
            if (!m_bExportAll) m_EventstoExport = dlg.getExportSecurity(); else m_EventstoExport = null;
            m_iEpP = dlg.getEntriespPage();
            m_bAscend = dlg.getOrder();
            m_bExportComments = dlg.getExportComments();
            m_iExportTo = dlg.getExportTo();
            pack();
            try {
                if (dlg.useDefTemplate()) inTemplate = getClass().getResourceAsStream(DEF_TEMPLT); else if (dlg.getTemplateFile() != null) inTemplate = new FileInputStream(new File(dlg.getTemplateFile())); else return false;
            } catch (FileNotFoundException e) {
                javax.swing.JOptionPane.showMessageDialog(parent, e, InstallInfo.getString("app.title"), javax.swing.JOptionPane.ERROR_MESSAGE);
                System.err.println(e);
            }
        } else inTemplate = getClass().getResourceAsStream(DEF_TEMPLT);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inTemplate, this);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        insertNewPage();
        return true;
    }

    private String translateComment(String template, LJComment comment) {
        String szSource;
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        nf.setMinimumIntegerDigits(2);
        String szText = XMLProtocol.cleanHTML(comment.m_szBody);
        szText = szText.replaceAll("\n", "<BR>");
        int idx = template.indexOf("%COMMENTTEXT%");
        szSource = template.substring(0, idx);
        szSource += szText;
        szSource += template.substring(idx + 15);
        szSource = szSource.replaceAll("%POSTER%", getUserCode(m_EventTable.getUserName(comment.m_iUserID), false));
        String subject = "";
        if (comment.m_szSubject != null) subject = comment.m_szSubject;
        szSource = szSource.replaceAll("%SUBJECT%", subject);
        String szDate;
        if (comment.m_Date != null) szDate = new java.text.SimpleDateFormat(m_szDateFormat).format(comment.m_Date); else szDate = "";
        szSource = szSource.replaceAll("%COMMENTDATE%", szDate);
        return szSource;
    }

    private String insertThread(LJComment comment, int iLevel) {
        String szResult = "";
        String szTemp;
        String szText = translateComment(m_szCommBody, comment);
        int idx = m_szCommIndent.indexOf("%COMMENT%");
        szTemp = m_szCommIndent.substring(0, idx);
        szTemp += szText;
        szTemp += m_szCommIndent.substring(idx + 11);
        szTemp = szTemp.replaceAll("%WIDE%", Integer.toString(iLevel * m_iIndent));
        szResult = szTemp;
        LJComment[] childs = comment.getChildren();
        for (int i = 0; i < childs.length; i++) {
            if (childs[i].m_iState != LJComment.DELETED) szResult += insertThread(childs[i], iLevel + 1);
        }
        return szResult;
    }

    private String getPicEntry(String uName, String pic) {
        if (pic == null) return "";
        String szResult = m_szPic;
        szResult = szResult.replaceAll("\\|USERNAME\\|", uName);
        String ljIcon = "";
        if (m_bExternal) {
            ljIcon = "Images/" + pic;
            String szImageDir = m_szBasePath + ljIcon;
            File fImageDir = new File(szImageDir);
            if (!fImageDir.exists()) return "";
        } else {
            ljIcon = m_szUserDir + "Cache" + System.getProperty("file.separator") + pic;
            File file = new File(ljIcon);
            ljIcon = file.toURI().toString();
        }
        szResult = szResult.replaceAll("\\|PICKWURL\\|", ljIcon);
        return szResult;
    }

    private String translate(String template, OldEventInfo info) {
        JLJSettings settings = JLJSettings.GetSettings();
        String szSource = template;
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        nf.setMinimumIntegerDigits(2);
        String szMood = "";
        if (info.m_szMood != null) szMood = info.m_szMood; else if (info.m_iMoodID > -1) {
            LJMoods.MoodInfo moodinfo = settings.m_Moods.getMoodID(info.m_iMoodID);
            szMood = moodinfo.m_szMoodName;
        }
        szSource = szSource.replaceAll("\\|MOOD\\|", szMood);
        String szPrivate = "";
        if (info.m_iSecurity == info.SEC_PRIVATE) {
            if (m_bExternal) szPrivate = "<img src=\"images/icon_private.gif\">"; else szPrivate = "<img src=\"" + getClass().getResource("/org/homedns/krolain/MochaJournal/Images/icon_private.gif").toString() + "\">";
        } else if (info.m_iSecurity == info.SEC_MASK) {
            if (m_bExternal) szPrivate = "<img src=\"images/icon_protected.gif\">"; else szPrivate = "<img src=\"" + getClass().getResource("/org/homedns/krolain/MochaJournal/Images/icon_protected.gif").toString() + "\">";
        }
        szSource = szSource.replaceAll("\\|PRIVACY\\|", szPrivate);
        String szMusic = "";
        if (info.m_szMusic != null) szMusic = info.m_szMusic;
        szSource = szSource.replaceAll("\\|MUSIC\\|", szMusic);
        String szSubject = "";
        if (info.m_szSubject != null) szSubject = info.m_szSubject;
        szSource = szSource.replaceAll("\\|TITLE\\|", szSubject);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(info.m_Date);
        szSource = szSource.replaceAll("\\|YEAR\\|", new java.lang.Integer(cal.get(cal.YEAR)).toString());
        szSource = szSource.replaceAll("\\|MONTH\\|", nf.format(cal.get(cal.MONTH) + 1));
        szSource = szSource.replaceAll("\\|DAY\\|", nf.format(cal.get(cal.DAY_OF_MONTH)));
        String szTime = "";
        szTime = nf.format(cal.get(cal.HOUR_OF_DAY));
        szTime += ":" + nf.format(cal.get(cal.MINUTE));
        szTime += ":" + nf.format(cal.get(cal.SECOND));
        szSource = szSource.replaceAll("\\|TIME\\|", szTime);
        String szUName = null;
        if (info.m_szPoster == null) szUName = m_szUsername; else szUName = info.m_szPoster;
        if (szUName != null) szSource = szSource.replaceAll("\\|POSTERNAME\\|", szUName); else szSource = szSource.replaceAll("\\|POSTERNAME\\|", "");
        if (m_bExternal) szSource = szSource.replaceAll("\\|USERLJICON\\|", "Images/user.gif"); else szSource = szSource.replaceAll("\\|USERLJICON\\|", getClass().getResource("/org/homedns/krolain/MochaJournal/Images/userinfo.gif").toString());
        String ljURL = "http://www.livejournal.com/users/" + szUName + "/";
        szSource = szSource.replaceAll("\\|POSTERLJ\\|", ljURL);
        ljURL = "http://www.livejournal.com/users/" + m_szUsername + "/";
        szSource = szSource.replaceAll("\\|USERLJ\\|", ljURL);
        szSource = szSource.replaceAll("\\|USERNAME\\|", m_szUsername);
        String szPickKW = null;
        if (info.m_szPickKW != null) szPickKW = info.m_szPickKW; else szPickKW = "default";
        String szTemp = szSource;
        int idx = szSource.indexOf("|PIC|");
        if (idx > -1) {
            szSource = szTemp.substring(0, idx);
            szSource += getPicEntry(szUName, szPickKW);
            szSource += szTemp.substring(idx + 7);
        }
        return szSource;
    }

    private boolean exportPost(OldEventInfo event) {
        int iSize = m_EventstoExport.length;
        for (int i = 0; i < iSize; i++) {
            Object obj = m_EventstoExport[i];
            if (obj instanceof String) {
                String str = (String) obj;
                if (str.compareTo(InstallInfo.getString("group.public")) == 0) {
                    if (event.m_iSecurity == event.SEC_PUBLIC) return true;
                } else if (str.compareTo(InstallInfo.getString("group.private")) == 0) {
                    if (event.m_iSecurity == event.SEC_PRIVATE) return true;
                } else if (str.compareTo(InstallInfo.getString("group.friends")) == 0) {
                    if ((event.m_iSecurity == event.SEC_MASK) && ((event.m_iSec_Mask & 1) == 1)) return true;
                }
            } else if (obj instanceof LJGroups.LJGroup) {
                LJGroups.LJGroup group = (LJGroups.LJGroup) obj;
                if ((event.m_iSec_Mask == event.SEC_MASK) && (((group.m_iBit * 2) & event.m_iSec_Mask) == (group.m_iBit * 2))) return true;
            }
        }
        return false;
    }

    public void insertEntries(LJeventsTable table) {
        m_EventTable = table;
        insertEntries(table.getEventList());
    }

    public String getUserCode(String user, boolean bComm) {
        String szIcon;
        if (user == null) return "(anonymous)";
        if (!bComm) {
            if (m_bExternal) szIcon = "Images/user.gif"; else szIcon = getClass().getResource("/org/homedns/krolain/MochaJournal/Images/userinfo.gif").toString();
        } else {
            if (m_bExternal) szIcon = "Images/comm.gif"; else szIcon = getClass().getResource("/org/homedns/krolain/MochaJournal/Images/communitynfo.gif").toString();
        }
        String szRepl = "<a href=\"http://www.livejournal.com/userinfo.bml?user=" + user + "\">";
        szRepl += "<IMG SRC=\"" + szIcon + "\" alt='[info]' width='17' height='17' border=0 align=bottom></a>";
        szRepl += "<a href=\"http://www.livejournal.com/users/" + user + "/\">" + user + "</a>";
        return szRepl;
    }

    private void writeEntry(Writer writer, OldEventInfo event) {
        try {
            writer.write(translate(m_szPostHead, event));
            writer.flush();
            String szText = new String(event.m_szEvent);
            szText = XMLProtocol.cleanHTML(szText);
            szText = szText.replaceAll("\n", "<BR>");
            Pattern p = Pattern.compile("<LJ\\s+(USER|COMM)\\s*=\\s*\"([\\S&&[^\"]]*)\">", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(szText);
            while (m.find()) {
                String s = m.group(2);
                String type = m.group(1);
                szText = m.replaceFirst(getUserCode(s, !type.equalsIgnoreCase("user")));
                m = p.matcher(szText);
            }
            String szSource = m_szPostEvent;
            int idx = szSource.indexOf("|ENTRY|");
            writer.write(szSource.substring(0, idx));
            writer.write(szText);
            writer.write(szSource.substring(idx + 9));
            writer.flush();
        } catch (java.io.IOException e) {
            System.err.println(e);
        }
    }

    private void writeComment(Writer writer, OldEventInfo event) {
        try {
            int iSize2 = event.m_Comments.size();
            writer.write(m_szCommHead);
            for (int i2 = 0; i2 < iSize2; i2++) {
                LJComment comment = (LJComment) event.m_Comments.get(i2);
                if (comment.m_iState != comment.DELETED) {
                    writer.write(m_szThreadHead);
                    String szComment = insertThread(comment, 0);
                    writer.write(szComment);
                    writer.write(m_szThreadFoot);
                }
            }
            writer.write(m_szCommFoot);
        } catch (java.io.IOException e) {
            System.err.println(e);
        }
    }

    public void insertEntries(OldEventInfo[] events) {
        if (events.length == 0) return;
        m_EventsExport = events;
        if (m_bExternal) {
            m_PM = new ProtProgress(m_Parent, true);
            java.lang.Thread td = new java.lang.Thread(this);
            m_PM.reset(1, events.length, "", "Exporting Entries", td);
            m_PM.show();
        } else run();
    }

    private void insertNewPage() {
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        nf.setMinimumIntegerDigits(2);
        try {
            if ((m_Writer != null) && m_bExternal) {
                if (m_iCurPage > -1) {
                    String szTemp = m_szPageLink;
                    String szPrev = "";
                    String szNext = m_szNextLink;
                    szNext = szNext.replaceAll("\\|LINK\\|", m_szPageName + nf.format(m_iCurPage + 1) + ".htm");
                    if (m_iCurPage > 0) {
                        szPrev = m_szPrevLink;
                        if ((m_iCurPage - 1) != 0) szPrev = szPrev.replaceAll("\\|LINK\\|", m_szPageName + nf.format(m_iCurPage - 1) + ".htm"); else szPrev = szPrev.replaceAll("\\|LINK\\|", m_szPageName + ".htm");
                    }
                    szTemp = szTemp.replaceAll("\\|PREVLINK\\|", szPrev);
                    szTemp = szTemp.replaceAll("\\|NEXTLINK\\|", szNext);
                    m_Writer.write(szTemp);
                }
            }
            if (m_Writer != null) {
                m_Writer.write(m_szDocFoot);
                m_Writer.flush();
                if (m_bExternal) m_Writer.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        if (m_bExternal) {
            m_iCurPage++;
            m_Writer = null;
            String szFile = m_szBasePath + m_szPageName;
            if (m_iCurPage > 0) szFile += nf.format(m_iCurPage);
            szFile += ".htm";
            try {
                File file = new File(szFile);
                file.createNewFile();
                m_Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            } catch (IOException e2) {
                System.err.println(e2);
            }
        } else if (m_Writer == null) m_Writer = new StringWriter();
        try {
            m_Writer.write(m_szDocHead);
        } catch (IOException e3) {
            System.err.println(e3);
        }
    }

    public String getHTMLCode() {
        if ((m_Writer != null) && (m_Writer instanceof StringWriter)) {
            return ((StringWriter) m_Writer).toString();
        }
        return null;
    }

    public void closeDocument() {
        try {
            if ((m_Writer != null) && m_bExternal) {
                if (m_iCurPage > -1) {
                    java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
                    nf.setMinimumIntegerDigits(2);
                    String szTemp = m_szPageLink;
                    String szPrev = "";
                    String szNext = "";
                    if (m_iCurPage > 0) {
                        szPrev = m_szPrevLink;
                        if ((m_iCurPage - 1) != 0) szPrev = szPrev.replaceAll("\\|LINK\\|", m_szPageName + nf.format(m_iCurPage - 1) + ".htm"); else szPrev = szPrev.replaceAll("\\|LINK\\|", m_szPageName + ".htm");
                    }
                    szTemp = szTemp.replaceAll("\\|PREVLINK\\|", szPrev);
                    szTemp = szTemp.replaceAll("\\|NEXTLINK\\|", szNext);
                    m_Writer.write(szTemp);
                }
            }
            if (m_Writer != null) {
                m_Writer.write(m_szDocFoot);
                m_Writer.flush();
                m_Writer.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void pack() {
        String szImageDir = m_szBasePath + "Images";
        File fImageDir = new File(szImageDir);
        fImageDir.mkdirs();
        String ljIcon = System.getProperty("user.home");
        ljIcon += System.getProperty("file.separator") + "MochaJournal" + System.getProperty("file.separator") + m_szUsername + System.getProperty("file.separator") + "Cache";
        File fUserDir = new File(ljIcon);
        File[] fIcons = fUserDir.listFiles();
        int iSize = fIcons.length;
        for (int i = 0; i < iSize; i++) {
            try {
                File fOutput = new File(fImageDir, fIcons[i].getName());
                if (!fOutput.exists()) {
                    fOutput.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(fOutput);
                    FileInputStream fIn = new FileInputStream(fIcons[i]);
                    while (fIn.available() > 0) fOut.write(fIn.read());
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        try {
            FileOutputStream fOut;
            InputStream fLJIcon = getClass().getResourceAsStream("/org/homedns/krolain/MochaJournal/Images/userinfo.gif");
            File fLJOut = new File(fImageDir, "user.gif");
            if (!fLJOut.exists()) {
                fOut = new FileOutputStream(fLJOut);
                while (fLJIcon.available() > 0) fOut.write(fLJIcon.read());
            }
            fLJIcon = getClass().getResourceAsStream("/org/homedns/krolain/MochaJournal/Images/communitynfo.gif");
            fLJOut = new File(fImageDir, "comm.gif");
            if (!fLJOut.exists()) {
                fOut = new FileOutputStream(fLJOut);
                while (fLJIcon.available() > 0) fOut.write(fLJIcon.read());
            }
            fLJIcon = getClass().getResourceAsStream("/org/homedns/krolain/MochaJournal/Images/icon_private.gif");
            fLJOut = new File(fImageDir, "icon_private.gif");
            if (!fLJOut.exists()) {
                fOut = new FileOutputStream(fLJOut);
                while (fLJIcon.available() > 0) fOut.write(fLJIcon.read());
            }
            fLJIcon = getClass().getResourceAsStream("/org/homedns/krolain/MochaJournal/Images/icon_protected.gif");
            fLJOut = new File(fImageDir, "icon_protected.gif");
            if (!fLJOut.exists()) {
                fOut = new FileOutputStream(fLJOut);
                while (fLJIcon.available() > 0) fOut.write(fLJIcon.read());
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        m_szXMLText = m_szXMLText.replaceAll("\\\n", "");
        if (qName.compareToIgnoreCase("pic") == 0) m_szPic = m_szXMLText; else if (qName.compareToIgnoreCase("pageheader") == 0) m_szDocHead = m_szXMLText; else if (qName.compareToIgnoreCase("header") == 0) m_szPostHead = m_szXMLText; else if (qName.compareToIgnoreCase("body") == 0) m_szPostEvent = m_szXMLText; else if (qName.compareToIgnoreCase("pagefooter") == 0) m_szDocFoot = m_szXMLText; else if (qName.compareToIgnoreCase("footer") == 0) m_szPostFoot = m_szXMLText; else if (qName.compareToIgnoreCase("next") == 0) m_szNextLink = m_szXMLText; else if (qName.compareToIgnoreCase("prev") == 0) m_szPrevLink = m_szXMLText; else if (qName.compareToIgnoreCase("htmlcode") == 0) m_szPageLink = m_szXMLText; else if (qName.compareToIgnoreCase("commentheader") == 0) m_szCommHead = m_szXMLText; else if (qName.compareToIgnoreCase("threadheader") == 0) m_szThreadHead = m_szXMLText; else if (qName.compareToIgnoreCase("commentindent") == 0) m_szCommIndent = m_szXMLText; else if (qName.compareToIgnoreCase("comment") == 0) m_szCommBody = m_szXMLText; else if (qName.compareToIgnoreCase("threadfooter") == 0) m_szThreadFoot = m_szXMLText; else if (qName.compareToIgnoreCase("commentfooter") == 0) m_szCommFoot = m_szXMLText; else if (qName.compareToIgnoreCase("commentlink") == 0) m_szCommLink = m_szXMLText;
    }

    public void startElement(java.lang.String uri, java.lang.String localName, java.lang.String qName, Attributes attributes) throws SAXException {
        m_szXMLText = "";
        String szTemp;
        if (qName.compareToIgnoreCase("commentindent") == 0) {
            szTemp = attributes.getValue("indent");
            m_iIndent = Integer.parseInt(szTemp);
        } else if (qName.compareToIgnoreCase("comment") == 0) m_szDateFormat = attributes.getValue("dateformat");
    }

    public void characters(char buf[], int offset, int len) throws SAXException {
        m_szXMLText += new String(buf, offset, len);
    }

    public void run() {
        int i;
        boolean bAvail = true;
        int iSize = m_EventsExport.length;
        if (!m_bAscend) i = 0; else i = iSize - 1;
        while (bAvail) {
            OldEventInfo event = m_EventsExport[i];
            if (m_bExternal) {
                if (m_bAscend) {
                    m_PM.update(iSize - i, "Exporting post: " + (iSize - i));
                } else {
                    m_PM.update(i, "Exporting post: " + i);
                }
            }
            if ((m_bExportAll) || (exportPost(event))) {
                try {
                    writeEntry(m_Writer, event);
                    if (m_bExportComments) {
                        int iSize2 = event.m_Comments.size();
                        if (iSize2 > 0) {
                            if (m_iExportTo == 0) writeComment(m_Writer, event); else {
                                Writer CommWriter = null;
                                String szFile = m_szBasePath + Integer.toString(event.m_iItemID);
                                szFile += ".htm";
                                try {
                                    File file = new File(szFile);
                                    file.createNewFile();
                                    CommWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
                                    CommWriter.write(m_szDocHead);
                                    writeEntry(CommWriter, event);
                                    writeComment(CommWriter, event);
                                    CommWriter.write(translate(m_szPostFoot, event));
                                    CommWriter.write(m_szDocFoot);
                                    CommWriter.flush();
                                    CommWriter.close();
                                    String szLink = m_szCommLink;
                                    szLink = szLink.replaceAll("%LINK%", Integer.toString(event.m_iItemID) + ".htm");
                                    m_Writer.write(szLink);
                                } catch (IOException e2) {
                                    System.err.println(e2);
                                }
                            }
                        }
                    }
                    m_Writer.write(translate(m_szPostFoot, event));
                    m_Writer.flush();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            if (!m_bAscend) {
                i++;
                if ((m_iEpP > 0) && (i % m_iEpP == 0) && (i < iSize)) insertNewPage();
                bAvail = (i < iSize);
            } else {
                i--;
                if ((m_iEpP > 0) && ((iSize - i) % m_iEpP == 0) && (i > -1)) insertNewPage();
                bAvail = (i > -1);
            }
        }
        if (m_bExternal) {
            m_PM.setVisible(false);
            m_PM.dispose();
        }
    }
}
