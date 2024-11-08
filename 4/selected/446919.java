package com.j2biz.compote.plugins.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.hibernate.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.j2biz.compote.IConstants;
import com.j2biz.compote.model.HibernateSession;
import com.j2biz.compote.pojos.Layout;
import com.j2biz.compote.util.SystemUtils;

/**
 * @author michelson
 * @version $$
 * @since 0.1
 * 
 * 
 */
public class LayoutDownloadServlet extends HttpServlet {

    private static Log log = LogFactory.getFactory().getInstance(LayoutDownloadServlet.class);

    protected void doGet(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        doService(arg0, arg1);
    }

    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        doService(arg0, arg1);
    }

    /**
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        try {
            SystemUtils.checkIsLoggedUserInAdministratorsGroup(request);
            if (StringUtils.isEmpty(id)) throw new Exception("Not enougth parameters found!");
            downloadLayout(id, request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param id
     */
    private void downloadLayout(String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Session s = HibernateSession.openSession();
        Layout layout = (Layout) s.load(Layout.class, new Long(id));
        HibernateSession.closeSession(s);
        String realPath = null;
        if (layout != null) {
            String layoutDirPath = layout.getPath().substring(0, layout.getPath().lastIndexOf("/"));
            realPath = getServletContext().getRealPath(IConstants.CONFIG.SYSTEM_LAYOUTS_DIR + layoutDirPath);
            if (realPath == null) throw new Exception("Path for requested layout-directory not found!");
            String zipFile = zipDirectory(request, realPath);
            returnZipStream(zipFile, response);
            removeZipFile(zipFile);
        } else throw new Exception("Layout with id=" + id + " not found!");
    }

    /**
     * @param zipFile
     */
    private void removeZipFile(String zipFile) {
        File f = new File(zipFile);
        boolean result = f.delete();
        if (log.isDebugEnabled()) {
            log.debug("Removing " + f + " .... " + result);
        }
    }

    /**
     * @param zipFile
     * @param response
     * @throws Exception
     */
    private void returnZipStream(String zipFile, HttpServletResponse response) throws Exception {
        File f = new File(zipFile);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
        ServletOutputStream op = response.getOutputStream();
        FileInputStream in = new FileInputStream(f);
        int length = 0;
        byte[] buf = new byte[4096];
        while ((in != null) && ((length = in.read(buf)) != -1)) {
            System.out.println("Bytes read in: " + Integer.toString(length));
            op.write(buf, 0, length);
        }
        in.close();
    }

    /**
     * @param request
     * @param directoryPath
     * @return @throws
     *         Exception
     */
    private String zipDirectory(HttpServletRequest request, String directoryPath) throws Exception {
        String tempDirPath = getServletContext().getRealPath(IConstants.CONFIG.SYSTEM_TEMP_DIR);
        File f = new File(tempDirPath);
        if (!f.exists()) f.mkdir();
        String outputZipFile = tempDirPath + "/" + System.currentTimeMillis() + ".zip";
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipFile));
        File layoutDir = new File(directoryPath);
        if (!layoutDir.exists() || !layoutDir.isDirectory()) throw new Exception("Error reading directory: " + directoryPath);
        zipDirIntern(directoryPath, directoryPath, zos);
        zos.close();
        return outputZipFile;
    }

    /**
     * @param dir2zip
     * @param zos
     * @throws IOException
     */
    private void zipDirIntern(String layoutRootDir, String dir2zip, ZipOutputStream zos) throws IOException {
        File zipDir = new File(dir2zip);
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                String filePath = f.getPath();
                zipDirIntern(layoutRootDir, filePath, zos);
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            String _path = StringUtils.replace(f.getPath(), layoutRootDir, "");
            if (_path.startsWith("/") || _path.startsWith("\\")) _path = _path.substring(1);
            ZipEntry anEntry = new ZipEntry(_path);
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        }
    }
}
