package org.osmius.webapp.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osmius.model.OsmUserscripts;
import org.osmius.service.OsmTypplatformManager;
import org.osmius.service.OsmUserscriptTypplatformManager;
import org.osmius.service.OsmUserscriptsManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the instanceEvents/eventDirDetailAJAX view
 */
public class UploadEditionController implements Controller {

    private final Log log = LogFactory.getLog(UploadEditionController.class);

    private OsmTypplatformManager osmTypplatformManager;

    private OsmUserscriptsManager osmUserscriptsManager;

    private OsmUserscriptTypplatformManager osmUserscriptTypplatformManager;

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
        model = new ModelAndView("interfaces/editUploadAJAX");
        String objID = httpServletRequest.getParameter("obj");
        String value = httpServletRequest.getParameter("value");
        String save = httpServletRequest.getParameter("save");
        List typPlatforms = osmTypplatformManager.getOsmTypplatforms();
        model.addObject("objID", objID);
        if (save != null && !save.equals("")) {
            String txtScript = httpServletRequest.getParameter("txtScript");
            String desc = httpServletRequest.getParameter("desScript");
            String command = httpServletRequest.getParameter("command");
            String platforms = httpServletRequest.getParameter("platforms");
            try {
                long time = new Date().getTime();
                String uploadDir = httpServletRequest.getSession().getServletContext().getRealPath("/upload/");
                zipFiles(uploadDir, "tmp" + time, txtScript, txtScript + ".zip");
                deleteDir(new File("." + File.separator + "tmp" + time));
                byte[] file = getBytesFromFile(new File("." + File.separator + txtScript + ".zip"));
                OsmUserscripts osmUserscripts = new OsmUserscripts();
                osmUserscripts.setTxtScript(txtScript);
                osmUserscripts.setDesScript(desc);
                osmUserscripts.setTxtCommand(command);
                osmUserscripts.setBinScript(file);
                osmUserscriptsManager.saveOsmUserscripts(osmUserscripts, platforms.split(","));
                new File("." + File.separator + txtScript + ".zip").delete();
                new File(uploadDir + File.separator + txtScript).delete();
            } catch (Exception e) {
                model.addObject("error", "true");
                return model;
            }
        } else {
            if (value != null && !value.equals("")) {
                String hTxtScript = httpServletRequest.getParameter("hTxtScript");
                OsmUserscripts osmUserscripts = osmUserscriptsManager.getOsmUserscript(hTxtScript);
                if (osmUserscripts != null) {
                    model.addObject("txtScript", osmUserscripts.getTxtScript());
                    model.addObject("txtCommand", osmUserscripts.getTxtCommand());
                    model.addObject("desScript", osmUserscripts.getDesScript());
                    List userTypplat = osmUserscriptTypplatformManager.getOsmUserscriptTypplatform(osmUserscripts.getTxtScript());
                    model.addObject("selPlatforms", userTypplat);
                    byte[] file = osmUserscripts.getBinScript();
                    FileOutputStream fos = new FileOutputStream("." + File.separator + osmUserscripts.getTxtScript() + ".zip");
                    fos.write(file);
                    fos.flush();
                    fos.close();
                    unzip(new File("." + File.separator + osmUserscripts.getTxtScript() + ".zip"));
                    String uploadDir = httpServletRequest.getSession().getServletContext().getRealPath("/upload/");
                    NioCopier nioCopier = new NioCopier();
                    try {
                        nioCopier.copy(new File("." + File.separator + "user" + File.separator + "scripts" + File.separator + osmUserscripts.getTxtScript()), new File(uploadDir + File.separator + osmUserscripts.getTxtScript()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    new File("." + File.separator + osmUserscripts.getTxtScript() + ".zip").delete();
                }
            }
        }
        model.addObject("platforms", typPlatforms);
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

    private void zipDir(String zipFileName, String dir) {
        File dirObj = new File(dir);
        if (!dirObj.isDirectory()) {
            System.err.println(dir + " is not a directory");
        }
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
            addDir(dirObj, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDir(files[i], out);
                continue;
            }
            String dir = dirObj.toString();
            dir = dir.substring(dir.indexOf(File.separator) + 1);
            FileInputStream in = new FileInputStream(dirObj + File.separator + files[i].getName());
            out.putNextEntry(new ZipEntry(dir + File.separator + files[i].getName()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }

    private void zipFiles(String uploadDir, String dir, String file, String zipfile) {
        new File("." + File.separator + dir + File.separator + "user" + File.separator + "scripts").mkdirs();
        NioCopier nioCopier = new NioCopier();
        try {
            nioCopier.copy(new File(uploadDir + File.separator + file), new File("." + File.separator + dir + File.separator + "user" + File.separator + "scripts" + File.separator + file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        zipDir(zipfile, dir);
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
}
