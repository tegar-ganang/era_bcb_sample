package com.j2biz.blogunity.web.actions.secure.blog.feeds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.j2biz.blogunity.BlogunityManager;
import com.j2biz.blogunity.dao.BlogDAO;
import com.j2biz.blogunity.exception.BlogunityException;
import com.j2biz.blogunity.i18n.I18N;
import com.j2biz.blogunity.i18n.I18NStatusFactory;
import com.j2biz.blogunity.pojo.Blog;
import com.j2biz.blogunity.pojo.Category;
import com.j2biz.blogunity.pojo.Entry;
import com.j2biz.blogunity.web.IActionResult;
import com.j2biz.blogunity.web.actions.secure.MyAbstractAction;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @author michelson
 * @version $$
 * @since 0.1
 * 
 *  
 */
public class ExportFeedAction extends MyAbstractAction {

    /**
     * Logger for this class
     */
    private static final Log log = LogFactory.getLog(ExportFeedAction.class);

    private static final String MIME_TYPE = "application/xml; charset=UTF-8";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public IActionResult execute(HttpServletRequest request, HttpServletResponse response) throws BlogunityException {
        String blogid = request.getParameter("blogid");
        if (StringUtils.isEmpty(blogid)) {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.ID_NOT_SETTED, "Blog"));
        }
        String feedType = request.getParameter("type");
        if (StringUtils.isEmpty(feedType)) throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.FEED_TYPE_NOT_SETTED));
        String compression = request.getParameter("compression");
        if (StringUtils.isEmpty(compression)) {
            compression = "none";
        }
        Blog blog = null;
        try {
            blog = (new BlogDAO()).getBlogByID(Long.parseLong(blogid));
        } catch (Exception e1) {
            blog = null;
        }
        if (blog == null) throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.NOT_FOUND));
        if (blog.getFounder().getId().longValue() != user.getId().longValue() && !user.isAdministrator()) throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.USER_NOT_AUTHORIZED_FOR_EXECUTION));
        try {
            SyndFeed feed = getFeed(request, blog);
            feed.setFeedType(feedType);
            response.setContentType(MIME_TYPE);
            SyndFeedOutput output = new SyndFeedOutput();
            String filename = blog.getUrlName();
            if (compression.equals("zip")) {
                filename += "-" + feedType + ".zip";
            } else if (compression.equals("gzip")) {
                filename += "-" + feedType + ".gz";
            } else {
                filename += "-" + feedType + ".xml";
            }
            response.setHeader("Content-disposition", "attachment; filename=" + filename);
            response.setContentType("application/Octet-stream");
            if (compression.equals("zip") || compression.equals("gzip")) {
                File temp = writeFeedToTempFile(feed, blog);
                File compressedFile;
                if (compression.equals("zip")) {
                    compressedFile = zipTempFile(temp);
                } else {
                    compressedFile = gzipTempFile(temp);
                }
                removeTempFile(temp);
                ServletOutputStream op = response.getOutputStream();
                FileInputStream in = new FileInputStream(compressedFile);
                int length = 0;
                byte[] buf = new byte[4096];
                while ((in != null) && ((length = in.read(buf)) != -1)) {
                    op.write(buf, 0, length);
                }
                in.close();
                removeTempFile(compressedFile);
            } else {
                output.output(feed, response.getWriter());
            }
        } catch (Exception ex) {
            log.error("Error generating blog!", ex);
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.COULD_NOT_GENERATE_FEED, ex));
        }
        return null;
    }

    private synchronized File zipTempFile(File tempFile) throws BlogunityException {
        try {
            File zippedFile = new File(BlogunityManager.getSystemConfiguration().getTempDir(), tempFile.getName() + ".zip");
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedFile));
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            FileInputStream fis = new FileInputStream(tempFile);
            ZipEntry anEntry = new ZipEntry(tempFile.getName());
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
            zos.close();
            return zippedFile;
        } catch (Exception e) {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.FEED_ZIP_FAILED, e));
        }
    }

    private synchronized File gzipTempFile(File tempFile) throws BlogunityException {
        try {
            File gzippedFile = new File(BlogunityManager.getSystemConfiguration().getTempDir(), tempFile.getName() + ".gz");
            GZIPOutputStream zos = new GZIPOutputStream(new FileOutputStream(gzippedFile));
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            FileInputStream fis = new FileInputStream(tempFile);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
            zos.close();
            return gzippedFile;
        } catch (Exception e) {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.FEED_GZIP_FAILED, e));
        }
    }

    private synchronized File writeFeedToTempFile(SyndFeed feed, Blog b) throws BlogunityException {
        String tempDir = BlogunityManager.getSystemConfiguration().getTempDir();
        File tempFile = new File(tempDir, b.getUrlName() + "_" + System.currentTimeMillis() + ".xml");
        if (!tempFile.exists()) {
            boolean result;
            try {
                result = tempFile.createNewFile();
            } catch (IOException e) {
                throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.CREATE_DIRECTORY, tempFile.getAbsolutePath(), e));
            }
            if (!result) {
                throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.CREATE_DIRECTORY, tempFile.getAbsolutePath()));
            }
        } else {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.CREATE_DIRECTORY, tempFile.getAbsolutePath()));
        }
        try {
            FileWriter writer = new FileWriter(tempFile);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
            writer.close();
        } catch (Exception e) {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.WRITE_DIRECTORY, tempFile.getAbsolutePath(), e));
        }
        return tempFile;
    }

    private synchronized void removeTempFile(File tempFile) throws BlogunityException {
        boolean result = tempFile.delete();
        if (!result) {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.DELETE_DIRECTORY, tempFile.getAbsolutePath()));
        }
    }

    private SyndFeed getFeed(HttpServletRequest request, Blog b) throws BlogunityException {
        DateFormat dateParser = new SimpleDateFormat(DATE_FORMAT);
        SyndFeed feed = new SyndFeedImpl();
        String blogUrl = BlogunityManager.getBase() + "/blogs/" + b.getUrlName();
        feed.setTitle(b.getUrlName());
        feed.setLink(blogUrl);
        feed.setDescription((b.getDescription() != null) ? b.getDescription() : "");
        feed.setCopyright("Copyright by " + b.getFounder().getNickname());
        feed.setAuthor(b.getFounder().getNickname());
        feed.setPublishedDate(b.getLastModified());
        List categories = new ArrayList();
        for (Iterator i = b.getCategories().iterator(); i.hasNext(); ) {
            Category cat = (Category) i.next();
            SyndCategory c = new SyndCategoryImpl();
            c.setName(cat.getName());
            categories.add(c);
        }
        feed.setCategories(categories);
        List syndEntries = new ArrayList();
        for (Iterator i = b.getEntries().iterator(); i.hasNext(); ) {
            Entry entry = (Entry) i.next();
            SyndEntry e = new SyndEntryImpl();
            e.setTitle(entry.getTitle());
            e.setLink(blogUrl + entry.getPermalink());
            e.setPublishedDate(entry.getCreateTime());
            SyndContent description = new SyndContentImpl();
            description.setType("text/html");
            String value = "";
            if (StringUtils.isNotEmpty(entry.getExcerpt())) value = entry.getExcerpt() + "<br/>" + entry.getBody(); else value = entry.getBody();
            description.setValue(value);
            e.setDescription(description);
            syndEntries.add(e);
        }
        feed.setEntries(syndEntries);
        return feed;
    }
}
