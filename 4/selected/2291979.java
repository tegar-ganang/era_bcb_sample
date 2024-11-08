package com.leemba.monitor.server.servlet;

import com.leemba.monitor.server.dao.Dao;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 *
 * @author mrjohnson
 */
public class CombineJs extends HttpServlet {

    private static final transient Logger log = Logger.getLogger(CombineJs.class);

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/resources/combined.real.js").forward(request, response);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        updater.setDaemon(true);
        updater.start();
    }

    @Override
    public void destroy() {
        super.destroy();
        updater.interrupt();
        try {
            updater.join(5000);
        } catch (InterruptedException ex) {
        }
    }

    private static final Thread updater = new Updater();

    private static class Updater extends Thread {

        private static final transient Logger log = Logger.getLogger(CombineJs.Updater.class);

        private final File jsdir = new File(Dao.getServerConfig().getWorkingDir() + "/resources/js");

        private final File combined = new File(Dao.getServerConfig().getWorkingDir() + "/resources/combined.real.js");

        @Override
        public void run() {
            try {
                update();
            } catch (Throwable t) {
                log.error("first run error", t);
            }
            while (true) {
                try {
                    if (check()) {
                        log.info("Updating combined.js");
                        update();
                    }
                    sleep(500);
                } catch (InterruptedException e) {
                    log.trace("Interrupted");
                    return;
                } catch (Throwable t) {
                    log.error("Error in update thread", t);
                }
            }
        }

        private boolean check() {
            long modified = combined.lastModified();
            File[] files = listJavascript();
            for (File js : files) {
                if (js.lastModified() > modified) return true;
            }
            return jsdir.lastModified() > modified;
        }

        private File[] listJavascript() {
            return jsdir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".js") && !name.equals("jquery.min.js") && !name.equals("leemba.js");
                }
            });
        }

        private void update() throws IOException {
            FileOutputStream out = new FileOutputStream(combined);
            try {
                File[] _files = listJavascript();
                List<File> files = new ArrayList<File>(Arrays.asList(_files));
                files.add(0, new File(jsdir.getAbsolutePath() + "/leemba.js"));
                files.add(0, new File(jsdir.getAbsolutePath() + "/jquery.min.js"));
                for (File js : files) {
                    FileInputStream fin = null;
                    try {
                        int count = 0;
                        byte buf[] = new byte[16384];
                        fin = new FileInputStream(js);
                        while ((count = fin.read(buf)) > 0) out.write(buf, 0, count);
                    } catch (Throwable t) {
                        log.error("Failed to read file: " + js.getAbsolutePath(), t);
                    } finally {
                        if (fin != null) fin.close();
                    }
                }
            } finally {
                out.close();
            }
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
