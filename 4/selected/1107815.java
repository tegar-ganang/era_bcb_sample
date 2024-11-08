package org.osmius.webapp.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osmius.Constants;
import org.osmius.model.OsmUserscripts;
import org.osmius.service.OsmTypplatformManager;
import org.osmius.service.OsmUserscriptTypplatformManager;
import org.osmius.service.OsmUserscriptsManager;
import org.osmius.service.UtilsManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the instanceEvents/eventDirDetailAJAX view
 */
public class UploadUpdateEditionController implements Controller {

    private final Log log = LogFactory.getLog(UploadUpdateEditionController.class);

    private OsmTypplatformManager osmTypplatformManager;

    private OsmUserscriptsManager osmUserscriptsManager;

    private OsmUserscriptTypplatformManager osmUserscriptTypplatformManager;

    private UtilsManager utilsManager;

    public void setUtilsManager(UtilsManager utilsManager) {
        this.utilsManager = utilsManager;
    }

    public void setOsmTypplatformManager(OsmTypplatformManager osmTypplatformManager) {
        this.osmTypplatformManager = osmTypplatformManager;
    }

    public void setOsmUserscriptsManager(OsmUserscriptsManager osmUserscriptsManager) {
        this.osmUserscriptsManager = osmUserscriptsManager;
    }

    public void setOsmUserscriptTypplatformManager(OsmUserscriptTypplatformManager osmUserscriptTypplatformManager) {
        this.osmUserscriptTypplatformManager = osmUserscriptTypplatformManager;
    }

    /**
    * From Spring documentation:
    * <p/>
    * Process the request and return a ModelAndView object which the DispatcherServlet
    * will render. A <code>null</code> return value is not an error: It indicates that
    * this object completed request processing itself, thus there is no ModelAndView
    * to render.
    *
    * @param httpServletRequest  current HTTP request
    * @param httpServletResponse current HTTP response
    * @return a ModelAndView to render, or <code>null</code> if handled directly
    * @throws Exception in case of errors
    */
    public ModelAndView handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("--> 'handleRequest' method...");
        }
        ModelAndView model = null;
        model = new ModelAndView("interfaces/editUpdateUploadAJAX");
        List typPlatforms = osmTypplatformManager.getOsmTypplatforms();
        String save = httpServletRequest.getParameter("save");
        if (save == null || "".equals(save)) {
            String hTxtScript = httpServletRequest.getParameter("hTxtScript");
            OsmUserscripts osmUserscripts = osmUserscriptsManager.getOsmUserscript(hTxtScript);
            if (osmUserscripts != null) {
                model.addObject("txtScript", osmUserscripts.getTxtScript());
                model.addObject("txtCommand", osmUserscripts.getTxtCommand());
                model.addObject("desScript", osmUserscripts.getDesScript());
                List userTypplat = osmUserscriptTypplatformManager.getOsmUserscriptTypplatform(osmUserscripts.getTxtScript());
                model.addObject("selPlatforms", userTypplat);
                model.addObject("event", httpServletRequest.getParameter("event"));
            }
            model.addObject("platforms", typPlatforms);
        } else {
            model = new ModelAndView("genericView");
            String txtScript = httpServletRequest.getParameter("txtScript");
            String desc = httpServletRequest.getParameter("desScript");
            String command = httpServletRequest.getParameter("command");
            String platforms = httpServletRequest.getParameter("platforms");
            String event = httpServletRequest.getParameter("event");
            if (event.indexOf(".") != -1) {
                event = event.substring(0, event.indexOf("."));
            }
            String ext = "";
            int index = command.indexOf(event);
            if (index != -1) {
                int index2 = command.indexOf(".", index);
                if (index2 != -1) {
                    if (command.indexOf(" ", index2) != -1) {
                        ext = command.substring(index2 + 1, command.indexOf(" ", index2));
                    } else {
                        ext = command.substring(index2 + 1, command.indexOf("\"", index2));
                    }
                }
            }
            if (!"".equals(ext)) {
                if (txtScript.lastIndexOf(".") != -1) {
                    if (!txtScript.substring(txtScript.lastIndexOf(".") + 1).equals(ext)) {
                        model.addObject("mdl", "61");
                        model.addObject("ok", false);
                        model.addObject("error", getText("msgSameExt", httpServletRequest.getLocale()));
                        String uploadDir = httpServletRequest.getSession().getServletContext().getRealPath("/upload/");
                        new File(uploadDir + File.separator + txtScript).delete();
                        return model;
                    }
                } else {
                    model.addObject("mdl", "61");
                    model.addObject("ok", false);
                    model.addObject("error", getText("msgSameExt", httpServletRequest.getLocale()));
                    String uploadDir = httpServletRequest.getSession().getServletContext().getRealPath("/upload/");
                    new File(uploadDir + File.separator + txtScript).delete();
                    return model;
                }
            }
            OsmUserscripts finalScript = new OsmUserscripts();
            finalScript.setTxtScript((event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : "")));
            finalScript.setDesScript(desc);
            finalScript.setTxtCommand(command.replace(txtScript, (event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : ""))));
            byte[] file;
            try {
                long time = new Date().getTime();
                String uploadDir = httpServletRequest.getSession().getServletContext().getRealPath("/upload/");
                File oldFile = new File(uploadDir + File.separator + txtScript);
                File newFile = new File(uploadDir + File.separator + event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : ""));
                if (!oldFile.getName().equals(newFile.getName())) {
                    oldFile.renameTo(newFile);
                    new File(uploadDir + File.separator + txtScript).delete();
                }
                String workdir = httpServletRequest.getSession().getServletContext().getRealPath("/WEB-INF/work");
                zipFiles(uploadDir, workdir, "tmp" + time, event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : ""), event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : "") + ".zip");
                file = getBytesFromFile(new File(workdir + File.separator + event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : "") + ".zip"));
                deleteDir(new File(workdir + File.separator + "tmp" + time));
                new File(workdir + File.separator + event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : "") + ".zip").delete();
                new File(uploadDir + File.separator + event + (txtScript.indexOf(".") != -1 ? txtScript.substring(txtScript.indexOf(".")) : "")).delete();
            } catch (Exception e) {
                model.addObject("mdl", "61");
                model.addObject("ok", false);
                model.addObject("error", e.getMessage());
                return model;
            }
            finalScript.setBinScript(file);
            finalScript.setDtiUpload(new Date(utilsManager.getActualTimestamp().getTime()));
            osmUserscriptsManager.renameOsmUserscripts(finalScript, platforms.split(","));
            model.addObject("mdl", "61");
            model.addObject("ok", true);
            model.addObject("error", "");
        }
        return model;
    }

    private class NioCopier {

        public void copy(File s, File t) throws IOException {
            FileChannel in = (new FileInputStream(s)).getChannel();
            FileChannel out = (new FileOutputStream(t)).getChannel();
            in.transferTo(0, s.length(), out);
            in.close();
            out.close();
        }
    }

    private void zipFiles(String uploadDir, String workdir, String dir, String file, String zipfile) {
        new File(workdir + File.separator + dir + File.separator + "user" + File.separator + "scripts").mkdirs();
        NioCopier nioCopier = new NioCopier();
        try {
            nioCopier.copy(new File(uploadDir + File.separator + file), new File(workdir + File.separator + dir + File.separator + "user" + File.separator + "scripts" + File.separator + file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            zipDir(new File(workdir + File.separator + dir), new File(workdir + File.separator + zipfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File fileList[] = dir.listFiles();
            for (int index = 0; index < fileList.length; index++) {
                File file = fileList[index];
                deleteDir(file);
            }
        }
        dir.delete();
    }

    private byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            return null;
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    private void unzip(File f) throws IOException {
        ZipFile zip;
        zip = new ZipFile(f);
        Enumeration e = zip.entries();
        while (e.hasMoreElements()) {
            ZipEntry zen = (ZipEntry) e.nextElement();
            if (zen.isDirectory()) {
                continue;
            }
            int size = (int) zen.getSize();
            InputStream zis = zip.getInputStream(zen);
            String extractfile = f.getParentFile().getAbsolutePath() + File.separator + zen.getName();
            writeFile(zis, new File(extractfile), size);
            zis.close();
        }
        zip.close();
    }

    private void writeFile(InputStream zis, File file, int size) throws IOException {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            byte[] byteStream = new byte[(int) size];
            int buf = -1;
            int rb = 0;
            while ((((int) size - rb) > 0)) {
                buf = zis.read(byteStream, rb, (int) size - rb);
                if (buf == -1) {
                    break;
                }
                rb += buf;
            }
            fos.write(byteStream);
        } catch (IOException e) {
            throw new IOException("UNZIP_ERROR");
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    private void zipDir(File directory, File zipfile) throws IOException {
        URI base = directory.toURI();
        Queue<File> queue = new LinkedList<File>();
        queue.add(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.poll();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.add(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else {
                        zout.putNextEntry(new ZipEntry(name));
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    private void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    private String getText(String msgKey, Locale locale) {
        String text;
        try {
            text = ResourceBundle.getBundle(Constants.BUNDLE_KEY, locale).getString(msgKey);
        } catch (MissingResourceException mse) {
            try {
                text = ResourceBundle.getBundle(Constants.BUNDLE_KEY).getString(msgKey);
            } catch (MissingResourceException msex) {
                text = "";
            }
        }
        return text;
    }
}
