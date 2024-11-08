package net.jwpa.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.jwpa.config.Config;
import net.jwpa.config.LogUtil;
import net.jwpa.model.Folder;
import net.jwpa.model.UploadProgress;
import net.jwpa.tools.ActivityTracker;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class UpServlet extends HttpJwpaServletBase {

    private static final Logger logger = LogUtil.getLogger();

    public static final String MAPPING_NAME = "upload/pics";

    public void doGetImpl(HttpServletRequest req, HttpServletResponse response) throws IOException {
        response.setStatus(405);
    }

    public void doPostImpl(HttpServletRequest req, HttpServletResponse response) throws IOException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if (!isMultipart) {
            response.setStatus(405);
            return;
        }
        String s = req.getParameter("numFiles");
        String pk = req.getParameter("progressKey");
        UploadMonitor up = new UploadMonitor(req.getSession(true), Integer.parseInt(s), pk);
        String contextPath = req.getContextPath();
        String prefixPath = MAPPING_NAME;
        String uri = req.getRequestURI();
        String relFile = "";
        if (contextPath.length() + prefixPath.length() + 2 < uri.length()) {
            relFile = uri.substring(contextPath.length() + prefixPath.length() + 2);
            relFile = URLTools.decodeUrlToPath(relFile);
        }
        String absFile = new File(Config.getCurrentConfig().getAbsoluteRootFolderName(), relFile).getAbsolutePath();
        Folder fol = (Folder) Folder.getAlbum(null, absFile, null);
        ServletFileUpload upload = new ServletFileUpload();
        long readTotal = 0;
        long readItem = 0;
        int itemNum = 0;
        try {
            ActivityTracker.openServlet();
            FileItemIterator iter = upload.getItemIterator(req);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                InputStream stream = item.openStream();
                if (item.isFormField()) {
                } else {
                    if (!Utils.isStringEmpty(item.getName())) {
                        itemNum++;
                        ZipInputStream zis = null;
                        if (Utils.isZipFile(new File(item.getName()))) {
                            zis = new ZipInputStream(stream);
                        }
                        String outFileName = Utils.getFileNameFromUploadName(item.getName());
                        InputStream readFrom = stream;
                        File outf = Utils.getNewFile(new File(fol.getDir(), outFileName));
                        try {
                            while (true) {
                                ZipEntry ze = null;
                                long itemsize = -1;
                                if (zis != null) {
                                    ze = zis.getNextEntry();
                                    if (ze == null) break;
                                    outFileName = ze.getName();
                                    outf = new File(fol.getDir(), outFileName);
                                    if (ze.isDirectory()) {
                                        outf.mkdirs();
                                        Folder f = (Folder) Folder.getAlbum(null, outf.getAbsolutePath(), null);
                                        f.contentChanged();
                                        f.sync();
                                        continue;
                                    }
                                    outf = Utils.getNewFile(outf);
                                    itemsize = ze.getSize();
                                    readFrom = zis;
                                }
                                OutputStream os = new FileOutputStream(outf);
                                try {
                                    byte[] buffer = new byte[1024];
                                    int read;
                                    while (itemsize != 0 && (read = readFrom.read(buffer, 0, (int) ((itemsize < 0) ? buffer.length : Math.min(buffer.length, itemsize)))) >= 0) {
                                        if (read == 0) Thread.sleep(100); else {
                                            os.write(buffer, 0, read);
                                            readTotal += read;
                                            readItem += read;
                                            if (itemsize >= 0) itemsize -= read;
                                            up.update(readItem, readTotal, itemNum, item.getName(), (ze == null) ? null : ze.getName());
                                        }
                                    }
                                } catch (Exception e) {
                                    LogUtil.logWarn(logger, "", e);
                                } finally {
                                    os.close();
                                }
                                if (zis == null) break;
                            }
                        } catch (Exception e) {
                            LogUtil.logWarn(logger, "", e);
                        } finally {
                            if (zis != null) zis.close();
                        }
                        readItem = 0;
                        fol.contentChanged();
                        fol.sync();
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logError(logger, "", e);
        } finally {
            ActivityTracker.closeServlet();
        }
        up.close();
        response.setStatus(200);
        response.setContentType("text/html");
        Writer w = response.getWriter();
        w.write("<html><head><title></title></head><body>Upload complete</body></html>");
    }

    public static UploadProgress getProgress(HttpSession session, String key) {
        synchronized (session) {
            Map<String, UploadProgress> progressMap = (Map<String, UploadProgress>) session.getAttribute("UploadProgresses");
            if (progressMap == null) {
                progressMap = new HashMap<String, UploadProgress>();
                session.setAttribute("UploadProgresses", progressMap);
            }
            UploadProgress progress = (UploadProgress) progressMap.get(key);
            if (progress == null) {
                progress = new UploadProgress();
                progressMap.put(key, progress);
            }
            return progress;
        }
    }
}

class UploadMonitor {

    HttpSession session = null;

    public String key;

    public UploadMonitor(HttpSession _session, int numFiles, String pk) {
        session = _session;
        key = pk;
        UploadProgress progress = getProgress();
        synchronized (progress) {
            progress.setNumFiles(numFiles);
        }
    }

    public void close() {
        UploadProgress progress = getProgress();
        synchronized (progress) {
            progress.setRunning(false);
        }
    }

    private UploadProgress getProgress() {
        return UpServlet.getProgress(session, key);
    }

    public void update(long done, long total, int itemProcessed, String file, String subfile) {
        UploadProgress progress = getProgress();
        synchronized (progress) {
            progress.setTotalUploaded(total);
            progress.setUploaded(done);
            progress.setItemNumber(itemProcessed);
            progress.setFile(file);
            progress.setSubfile(subfile);
        }
    }
}
