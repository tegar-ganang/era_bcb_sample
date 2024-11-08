package vqwiki.plugin.export2html;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import vqwiki.Change;
import vqwiki.Constants;
import vqwiki.Environment;
import vqwiki.Topic;
import vqwiki.WikiBase;
import vqwiki.lex.LinkLexConvert;
import vqwiki.plugin.export2html.LinkLexConvertExportHtml;
import vqwiki.search.SearchEngine;
import vqwiki.search.SearchResultEntry;
import vqwiki.servlets.LongOperationThread;
import vqwiki.svc.ChangeLog;
import vqwiki.utils.Utilities;

public class ExportHTMLThread extends LongOperationThread implements Constants {

    private static final Logger logger = Logger.getLogger(ExportHTMLThread.class.getName());

    private static final List ignoreTheseTopicsList = Arrays.asList(new String[] { "WikiSearch", "RecentChanges", "WikiSiteMap", "WikiSiteMapIE", "WikiSiteMapNS" });

    private String imageDir;

    private Exception exception;

    private File tempFile;

    private String tempdir;

    private String virtualWiki = DEFAULT_VWIKI;

    private ServletContext ctx;

    private String leftMenu;

    private String topArea;

    private String bottomArea;

    private WikiBase wb;

    private LinkLexConvert linkLexConvert = new LinkLexConvertExportHtml();

    public ExportHTMLThread(String virtualWiki, String tempdir, String imageDir, ServletContext ctx) {
        if (virtualWiki != null && virtualWiki.length() > 0) {
            this.virtualWiki = virtualWiki;
        }
        this.tempdir = tempdir;
        this.imageDir = imageDir;
        this.ctx = ctx;
        this.wb = (WikiBase) ctx.getAttribute(WIKI_BASE_KEY);
    }

    /**
     * Do the long lasting operation
     */
    protected void onRun() {
        Environment env = wb.getEnvironment();
        this.exception = null;
        try {
            this.leftMenu = wb.getParser().parseHTML(virtualWiki, "LeftMenu", linkLexConvert);
            this.topArea = wb.getParser().parseHTML(virtualWiki, "TopArea", linkLexConvert);
            this.topArea = this.topArea.replaceAll("../images", "images");
            this.bottomArea = wb.getParser().parseHTML(virtualWiki, "BottomArea", linkLexConvert);
            this.tempFile = File.createTempFile("htmlexport", "zip", new File(tempdir));
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile));
            ZipOutputStream zipout = new ZipOutputStream(fos);
            zipout.setMethod(ZipOutputStream.DEFLATED);
            addAllTopics(env, zipout, 0, 80);
            addAllSpecialPages(env, zipout, 80, 10);
            addAllUploadedFiles(env, zipout, 90, 5);
            addAllImages(env, zipout, 95, 5);
            zipout.close();
            logger.fine("Closing zip and sending to user");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception", e);
            this.exception = e;
        }
        super.progress = PROGRESS_DONE;
    }

    private void addAllTopics(Environment env, ZipOutputStream zipout, int progressStart, int progressLength) throws Exception, IOException {
        HashMap containingTopics = new HashMap();
        SearchEngine se = wb.getSearchEngine();
        Collection all = se.getAllTopicNames(virtualWiki);
        String defaultTopic = env.getDefaultTopic();
        String tpl = null;
        logger.fine("Logging Wiki " + virtualWiki + " starting at " + defaultTopic);
        int count = 0;
        for (Iterator allIterator = all.iterator(); allIterator.hasNext(); ) {
            progress = Math.min(progressStart + (int) ((double) count * (double) progressLength / all.size()), 99);
            count++;
            String topicname = (String) allIterator.next();
            try {
                addTopicToZip(env, zipout, containingTopics, se, defaultTopic, ignoreTheseTopicsList, topicname);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while adding topic: " + topicname, e);
            }
        }
        logger.fine("Done adding all topics.");
        List sitemapLines = new ArrayList();
        Vector visitedPages = new Vector();
        List startingList = new ArrayList(1);
        startingList.add(SitemapThread.LAST_IN_LIST);
        parsePages(defaultTopic, containingTopics, startingList, "1", sitemapLines, visitedPages);
        StringBuffer ieView = new StringBuffer();
        StringBuffer nsView = new StringBuffer();
        Vector childNodes = new Vector();
        for (Iterator lineIterator = sitemapLines.iterator(); lineIterator.hasNext(); ) {
            SitemapLineBean line = (SitemapLineBean) lineIterator.next();
            if (childNodes.size() > 0) {
                String myGroup = line.getGroup();
                String lastNode = (String) childNodes.get(childNodes.size() - 1);
                while (myGroup.length() <= lastNode.length() + 1 && childNodes.size() > 0) {
                    ieView.append("</div><!-- " + lastNode + "-->");
                    childNodes.remove(childNodes.size() - 1);
                    if (childNodes.size() > 0) {
                        lastNode = (String) childNodes.get(childNodes.size() - 1);
                    }
                }
            }
            ieView.append("<div id=\"node_" + line.getGroup() + "_Parent\" class=\"parent\">");
            for (Iterator levelsIterator = line.getLevels().iterator(); levelsIterator.hasNext(); ) {
                String level = (String) levelsIterator.next();
                if (line.isHasChildren()) {
                    if ("x".equalsIgnoreCase(level)) {
                        ieView.append("<a href=\"#\" onClick=\"expandIt('node_" + line.getGroup() + "_'); return false;\"><img src=\"images/x-.png\" widht=\"30\" height=\"30\" align=\"top\"  name=\"imEx\" border=\"0\"></a>");
                    } else if ("e".equalsIgnoreCase(level)) {
                        ieView.append("<a href=\"#\" onClick=\"expandItE('node_" + line.getGroup() + "_'); return false;\"><img src=\"images/e-.png\" widht=\"30\" height=\"30\" align=\"top\"  name=\"imEx\" border=\"0\"></a>");
                    } else {
                        ieView.append("<img src=\"images/" + level + ".png\" widht=\"30\" height=\"30\" align=\"top\">");
                    }
                } else {
                    ieView.append("<img src=\"images/" + level + ".png\" widht=\"30\" height=\"30\" align=\"top\">");
                }
            }
            ieView.append("<a href=\"" + safename(line.getTopic()) + ".html\">" + line.getTopic() + "</a></div>" + IOUtils.LINE_SEPARATOR);
            if (line.isHasChildren()) {
                ieView.append("<div id=\"node_" + line.getGroup() + "_Child\" class=\"child\">");
                childNodes.add(line.getGroup());
            }
        }
        for (int i = childNodes.size() - 1; i >= 0; i--) {
            ieView.append("</div><!-- " + (String) childNodes.get(i) + "-->");
        }
        nsView.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");
        for (Iterator lineIterator = sitemapLines.iterator(); lineIterator.hasNext(); ) {
            SitemapLineBean line = (SitemapLineBean) lineIterator.next();
            nsView.append("<tr><td height=\"30\" valign=\"top\">");
            for (Iterator levelsIterator = line.getLevels().iterator(); levelsIterator.hasNext(); ) {
                String level = (String) levelsIterator.next();
                nsView.append("<img src=\"images/" + level + ".png\" widht=\"30\" height=\"30\" align=\"top\">");
            }
            nsView.append("<a href=\"" + safename(line.getTopic()) + ".html\">" + line.getTopic() + "</a></td></tr>" + IOUtils.LINE_SEPARATOR);
        }
        nsView.append("</table>" + IOUtils.LINE_SEPARATOR);
        tpl = getTemplateFilledWithContent("sitemap");
        tpl = tpl.replaceAll("@@NSVIEW@@", nsView.toString());
        addTopicEntry(zipout, tpl, "WikiSiteMap", "WikiSiteMap.html");
        tpl = getTemplateFilledWithContent("sitemap_ie");
        tpl = tpl.replaceAll("@@IEVIEW@@", ieView.toString());
        addTopicEntry(zipout, tpl, "WikiSiteMap", "WikiSiteMapIE.html");
        tpl = getTemplateFilledWithContent("sitemap_ns");
        tpl = tpl.replaceAll("@@NSVIEW@@", nsView.toString());
        addTopicEntry(zipout, tpl, "WikiSiteMap", "WikiSiteMapNS.html");
    }

    private void addTopicEntry(ZipOutputStream zipout, String tpl, String topicName, String filename) throws IOException {
        tpl = tpl.replaceAll("@@TOPICNAME@@", topicName);
        tpl = tpl.replaceAll("@@REDIRECT@@", "");
        addZipEntry(zipout, filename, tpl);
    }

    private void addZipEntry(ZipOutputStream zipout, String filename, String content) throws IOException {
        zipout.putNextEntry(new ZipEntry(filename));
        StringReader strin = new StringReader(content);
        int read;
        while ((read = strin.read()) != -1) {
            zipout.write(read);
        }
        zipout.closeEntry();
        zipout.flush();
    }

    /**
     * Add a single topic to the Zip stream
     *
     * @param env The current environment
     * @param zipout The Zip to add the topic to
     * @param containingTopics List of all containing topic
     * @param se The search engine
     * @param defaultTopic The default topics
     * @param ignoreTheseTopicsList Ignore these topics
     * @param topicname The name of this topic
     * @throws Exception
     * @throws IOException
     */
    private void addTopicToZip(Environment env, ZipOutputStream zipout, HashMap containingTopics, SearchEngine se, String defaultTopic, List ignoreTheseTopicsList, String topicname) throws Exception, IOException {
        String tpl = getTemplateFilledWithContent(null);
        tpl = tpl.replaceAll("@@CHARSET@@", env.getForceEncoding());
        StringBuffer oneline = new StringBuffer();
        if (!ignoreTheseTopicsList.contains(topicname)) {
            oneline.append(topicname);
            tpl = tpl.replaceAll("@@TOPICNAME@@", topicname);
            Topic topicObject = new Topic(topicname);
            logger.fine("Adding topic " + topicname);
            String author = null;
            if (env.isVersioningOn()) {
                Date lastRevisionDate = topicObject.getMostRecentRevisionDate(virtualWiki);
                author = topicObject.getMostRecentAuthor(virtualWiki);
                if (author != null || lastRevisionDate != null) {
                    tpl = tpl.replaceAll("@@SHOWVERSIONING1@@", "-->");
                    if (author != null) {
                        tpl = tpl.replaceAll("@@AUTHOR@@", author);
                    }
                    if (lastRevisionDate != null) {
                        tpl = tpl.replaceAll("@@MODIFYDATE@@", Utilities.formatDate(lastRevisionDate));
                    }
                    tpl = tpl.replaceAll("@@SHOWVERSIONING2@@", "<!--");
                }
            }
            StringBuffer content = new StringBuffer();
            content.append(wb.getParser().parseHTML(virtualWiki, topicname, linkLexConvert));
            String redirect = "redirect:";
            if (content.toString().startsWith(redirect)) {
                StringBuffer link = new StringBuffer(content.toString().substring(redirect.length()).trim());
                while (link.toString().indexOf("<") != -1) {
                    int startpos = link.toString().indexOf("<");
                    int endpos = link.toString().indexOf(">");
                    if (endpos == -1) {
                        endpos = link.length();
                    } else {
                        endpos++;
                    }
                    link.delete(startpos, endpos);
                }
                link = new StringBuffer(safename(link.toString().trim()));
                link = link.append(".html");
                String nl = System.getProperty("line.separator");
                tpl = tpl.replaceAll("@@REDIRECT@@", "<script>" + nl + "location.replace(\"" + link.toString() + "\");" + nl + "</script>" + nl + "<meta http-equiv=\"refresh\" content=\"1; " + link.toString() + "\">" + nl);
            } else {
                tpl = tpl.replaceAll("@@REDIRECT@@", "");
            }
            Collection searchresult = se.find(virtualWiki, topicname, false);
            if (searchresult != null && searchresult.size() > 0) {
                String divider = "";
                StringBuffer backlinks = new StringBuffer();
                for (Iterator it = searchresult.iterator(); it.hasNext(); ) {
                    SearchResultEntry result = (SearchResultEntry) it.next();
                    if (!result.getTopic().equals(topicname)) {
                        backlinks.append(divider);
                        backlinks.append("<a href=\"");
                        backlinks.append(safename(result.getTopic()));
                        backlinks.append(".html\">");
                        backlinks.append(result.getTopic());
                        backlinks.append("</a>");
                        divider = " | ";
                        List l = (List) containingTopics.get(result.getTopic());
                        if (l == null) {
                            l = new ArrayList();
                        }
                        if (!l.contains(topicname)) {
                            l.add(topicname);
                        }
                        containingTopics.put(result.getTopic(), l);
                    }
                }
                if (backlinks.length() > 0) {
                    ResourceBundle messages = ResourceBundle.getBundle("ApplicationResources", locale);
                    content.append("<br /><br /><span class=\"backlinks\">");
                    content.append(topicname);
                    content.append(" ");
                    content.append(messages.getString("topic.ismentionedon"));
                    content.append(" ");
                    content.append(backlinks.toString());
                    content.append("</span>");
                }
            }
            tpl = tpl.replaceAll("@@CONTENTS@@", content.toString());
            addZipEntry(zipout, safename(topicname) + ".html", tpl);
            if (topicname.equals(defaultTopic)) {
                addZipEntry(zipout, "index.html", tpl);
            }
        }
    }

    /**
     * Create a safe name of this topic for the file system.
     *
     * @param topic The original topic name
     * @return The safe topic name
     */
    private String safename(String topic) {
        return Utilities.encodeSafeExportFileName(topic);
    }

    /**
     * Parse the pages starting with startTopic. The results are stored in the
     * list sitemapLines. This functions is called recursivly, but the list is
     * filled in the correct order.
     *
     * @param currentWiki name of the wiki to refer to
     * @param startTopic Start with this page
     * @param level A list indicating the images to use to represent certain levels
     * @param group The group, we are representing
     * @param sitemapLines A list of all lines, which results in the sitemap
     * @param visitedPages A vector of all pages, which already have been visited
     * @param endString Beyond this text we do not search for links
     */
    private void parsePages(String topic, HashMap wiki, List levelsIn, String group, List sitemapLines, Vector visitedPages) {
        try {
            List result = new ArrayList();
            List levels = new ArrayList(levelsIn.size());
            for (int i = 0; i < levelsIn.size(); i++) {
                if (i + 1 < levelsIn.size()) {
                    if (SitemapThread.MORE_TO_COME.equals(levelsIn.get(i))) {
                        levels.add(SitemapThread.HORIZ_LINE);
                    } else if (SitemapThread.LAST_IN_LIST.equals(levelsIn.get(i))) {
                        levels.add(SitemapThread.NOTHING);
                    } else {
                        levels.add(levelsIn.get(i));
                    }
                } else {
                    levels.add(levelsIn.get(i));
                }
            }
            List l = (List) wiki.get(topic);
            if (l == null) {
                l = new ArrayList();
            }
            for (Iterator listIterator = l.iterator(); listIterator.hasNext(); ) {
                String link = (String) listIterator.next();
                if (link.indexOf('&') > -1) {
                    link = link.substring(0, link.indexOf('&'));
                }
                if (link.length() > 3 && !link.startsWith("topic=") && !link.startsWith("action=") && !visitedPages.contains(link) && !wb.getPseudoTopicHandler().isPseudoTopic(link)) {
                    result.add(link);
                    visitedPages.add(link);
                }
            }
            SitemapLineBean slb = new SitemapLineBean();
            slb.setTopic(topic);
            slb.setLevels(new ArrayList(levels));
            slb.setGroup(group);
            slb.setHasChildren(result.size() > 0);
            sitemapLines.add(slb);
            for (int i = 0; i < result.size(); i++) {
                String link = (String) result.get(i);
                String newGroup = group + "_" + String.valueOf(i);
                boolean isLast = i + 1 == result.size();
                if (isLast) {
                    levels.add(SitemapThread.LAST_IN_LIST);
                } else {
                    levels.add(SitemapThread.MORE_TO_COME);
                }
                parsePages(link, wiki, levels, newGroup, sitemapLines, visitedPages);
                levels.remove(levels.size() - 1);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception", e);
        }
    }

    /**
     *
     */
    private void addAllSpecialPages(Environment env, ZipOutputStream zipout, int progressStart, int progressLength) throws Exception, IOException {
        ResourceBundle messages = ResourceBundle.getBundle("ApplicationResources", locale);
        String tpl;
        int count = 0;
        int numberOfSpecialPages = 7;
        progress = Math.min(progressStart + (int) ((double) count * (double) progressLength / numberOfSpecialPages), 99);
        count++;
        String cssContent = wb.readRaw(virtualWiki, "StyleSheet");
        addZipEntry(zipout, "css/vqwiki.css", cssContent);
        progress = Math.min(progressStart + (int) ((double) count * (double) progressLength / numberOfSpecialPages), 99);
        count++;
        tpl = getTemplateFilledWithContent("search");
        addTopicEntry(zipout, tpl, "WikiSearch", "WikiSearch.html");
        progress = Math.min(progressStart + (int) ((double) count * (double) progressLength / numberOfSpecialPages), 99);
        count++;
        zipout.putNextEntry(new ZipEntry("applets/export2html-applet.jar"));
        IOUtils.copy(new FileInputStream(ctx.getRealPath("/WEB-INF/classes/export2html/export2html-applet.jar")), zipout);
        zipout.closeEntry();
        zipout.flush();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JarOutputStream indexjar = new JarOutputStream(bos);
            JarEntry jarEntry;
            File searchDir = new File(wb.getSearchEngine().getSearchIndexPath(virtualWiki));
            String files[] = searchDir.list();
            StringBuffer listOfAllFiles = new StringBuffer();
            for (int i = 0; i < files.length; i++) {
                if (listOfAllFiles.length() > 0) {
                    listOfAllFiles.append(",");
                }
                listOfAllFiles.append(files[i]);
                jarEntry = new JarEntry("lucene/index/" + files[i]);
                indexjar.putNextEntry(jarEntry);
                IOUtils.copy(new FileInputStream(new File(searchDir, files[i])), indexjar);
                indexjar.closeEntry();
            }
            indexjar.flush();
            indexjar.putNextEntry(new JarEntry("lucene/index.dir"));
            IOUtils.copy(new StringReader(listOfAllFiles.toString()), indexjar);
            indexjar.closeEntry();
            indexjar.flush();
            indexjar.close();
            zipout.putNextEntry(new ZipEntry("applets/index.jar"));
            zipout.write(bos.toByteArray());
            zipout.closeEntry();
            zipout.flush();
            bos.reset();
        } catch (Exception e) {
            logger.log(Level.FINE, "Exception while adding lucene index: ", e);
        }
        progress = Math.min(progressStart + (int) ((double) count * (double) progressLength / numberOfSpecialPages), 99);
        count++;
        StringBuffer content = new StringBuffer();
        content.append("<table><tr><th>" + messages.getString("common.date") + "</th><th>" + messages.getString("common.topic") + "</th><th>" + messages.getString("common.user") + "</th></tr>" + IOUtils.LINE_SEPARATOR);
        Collection all = null;
        try {
            Calendar cal = Calendar.getInstance();
            ChangeLog cl = wb.getChangeLog();
            int n = env.getIntSetting(Environment.PROPERTY_RECENT_CHANGES_DAYS);
            if (n == 0) {
                n = 5;
            }
            all = new ArrayList();
            for (int i = 0; i < n; i++) {
                Collection col = cl.getChanges(virtualWiki, cal.getTime());
                if (col != null) {
                    all.addAll(col);
                }
                cal.add(Calendar.DATE, -1);
            }
        } catch (Exception e) {
        }
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        for (Iterator iter = all.iterator(); iter.hasNext(); ) {
            Change change = (Change) iter.next();
            content.append("<tr><td class=\"recent\">" + df.format(change.getTime()) + "</td><td class=\"recent\"><a href=\"" + safename(change.getTopic()) + ".html\">" + change.getTopic() + "</a></td><td class=\"recent\">" + change.getUser() + "</td></tr>");
        }
        content.append("</table>" + IOUtils.LINE_SEPARATOR);
        tpl = getTemplateFilledWithContent(null);
        tpl = tpl.replaceAll("@@CONTENTS@@", content.toString());
        addTopicEntry(zipout, tpl, "RecentChanges", "RecentChanges.html");
        logger.fine("Done adding all special topics.");
    }

    /**
     *
     */
    private void addAllImages(Environment en, ZipOutputStream zipout, int progressStart, int progressLength) throws IOException {
        String[] files = new File(imageDir).list();
        int bytesRead = 0;
        byte byteArray[] = new byte[4096];
        FileInputStream in = null;
        for (int i = 0; i < files.length; i++) {
            progress = Math.min(progressStart + (int) ((double) i * (double) progressLength / files.length), 99);
            File fileToHandle = new File(imageDir, files[i]);
            if (fileToHandle.isFile() && fileToHandle.canRead()) {
                try {
                    logger.fine("Adding image file " + files[i]);
                    ZipEntry entry = new ZipEntry("images/" + files[i]);
                    zipout.putNextEntry(entry);
                    in = new FileInputStream(fileToHandle);
                    while (in.available() > 0) {
                        bytesRead = in.read(byteArray, 0, Math.min(4096, in.available()));
                        zipout.write(byteArray, 0, bytesRead);
                    }
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                } finally {
                    try {
                        zipout.closeEntry();
                    } catch (IOException e1) {
                    }
                    try {
                        zipout.flush();
                    } catch (IOException e1) {
                    }
                    try {
                        if (in != null) {
                            in.close();
                            in = null;
                        }
                    } catch (IOException e1) {
                    }
                }
            }
        }
    }

    private void addAllUploadedFiles(Environment en, ZipOutputStream zipout, int progressStart, int progressLength) throws IOException, FileNotFoundException {
        File uploadPath = en.uploadPath(virtualWiki, "");
        String[] files = uploadPath.list();
        int bytesRead = 0;
        byte byteArray[] = new byte[4096];
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            File file = en.uploadPath(virtualWiki, fileName);
            if (file.isDirectory()) {
                continue;
            }
            progress = Math.min(progressStart + (int) ((double) i * (double) progressLength / files.length), 99);
            logger.fine("Adding uploaded file " + fileName);
            ZipEntry entry = new ZipEntry(safename(fileName));
            try {
                FileInputStream in = new FileInputStream(file);
                zipout.putNextEntry(entry);
                while (in.available() > 0) {
                    bytesRead = in.read(byteArray, 0, Math.min(4096, in.available()));
                    zipout.write(byteArray, 0, bytesRead);
                }
                zipout.closeEntry();
                zipout.flush();
            } catch (FileNotFoundException e) {
                logger.log(Level.WARNING, "Could not open file!", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "IOException!", e);
                try {
                    zipout.closeEntry();
                    zipout.flush();
                } catch (IOException e1) {
                }
            }
        }
    }

    private String getTemplateFilledWithContent(String contentName) throws Exception {
        String template = IOUtils.toString(wb.getResourceAsStream("export2html/mastertemplate.tpl"));
        template = template.replaceAll("@@VERSION@@", Environment.WIKI_VERSION);
        template = template.replaceAll("@@LEFTMENU@@", leftMenu);
        template = template.replaceAll("@@TOPAREA@@", topArea);
        template = template.replaceAll("@@BOTTOMAREA@@", bottomArea);
        if (contentName != null) {
            String content = IOUtils.toString(wb.getResourceAsStream("export2html/" + contentName + ".content"));
            template = template.replaceAll("@@CONTENTS@@", content);
        }
        return template;
    }

    /**
     * We are done. Go to result page.
     *
     * @see vqwiki.servlets.LongLastingOperationServlet#dispatchDone(
     *          javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void dispatchDone(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (exception != null) {
            throw exception;
        }
        try {
            response.setContentType("application/zip");
            response.setHeader("Expires", "0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Keep-Alive", "timeout=15, max=100");
            response.setHeader("Connection", "Keep-Alive");
            response.setHeader("Content-Disposition", "attachment" + ";filename=" + virtualWiki + "HtmlExport.zip;");
            FileInputStream in = new FileInputStream(tempFile);
            response.setContentLength((int) tempFile.length());
            OutputStream out = response.getOutputStream();
            int bytesRead = 0;
            byte byteArray[] = new byte[4096];
            while (in.available() > 0) {
                bytesRead = in.read(byteArray, 0, Math.min(4096, in.available()));
                out.write(byteArray, 0, bytesRead);
            }
            out.flush();
            out.close();
            tempFile.delete();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }
}
