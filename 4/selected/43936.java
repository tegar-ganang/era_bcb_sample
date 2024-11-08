package edu.upenn.law.util;

import edu.upenn.law.Checkio;
import edu.upenn.law.Workflow;
import edu.upenn.law.exception.CheckoutException;
import edu.upenn.law.io.AnnotationFile;
import edu.upenn.law.io.AnnotationFileList;
import edu.upenn.law.io.exception.InvalidAnnotationFileTypeException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hojothum.common.Debug;
import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.text.SimpleDateFormat;

/**
 * @author  Eric Pancoast
 */
public class FileStream extends HttpServlet {

    /** Tells the Manual class to output all debug messages if true.
	 *  If false the class will only output error messages. */
    private static final boolean DEBUG = true;

    /** Tells the Manual class to output the date with all debug messages if true.
	 *  If false the class will not output dated debug messages. */
    private static final boolean DEBUG_DATE = true;

    /** Tells the Manual class what type of debug message redirect to use.
	 *  See Debug class. */
    private static final int DEBUG_TYPE = Debug.TYPE_SYSOUT;

    /** If a type of file redirect is used, this indicates what the output files
	 *  name should be. */
    private static final String DEBUG_FILENAME = "";

    /** Redirects debug output messages using the Debug class. */
    private static void dmsg(String message) {
        if (DEBUG) Debug.dmsg("FileStream: " + message, DEBUG_TYPE, DEBUG_DATE, DEBUG_FILENAME);
    }

    /** Redirects debug error messages using the Debug class. */
    private static void derr(String message) {
        Debug.derr("FileStream: " + message, DEBUG_TYPE, DEBUG_DATE, DEBUG_FILENAME);
    }

    public static final String TEMP_CO = edu.upenn.law.Constants.ROOT_PATH + "/.tmp/co";

    int user = -1;

    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /** Destroys the servlet.
     */
    public void destroy() {
    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int source_file_info_id = -1;
        Date checkout_begin_date = null;
        Date checkout_end_date = null;
        Date checkin_begin_date = null;
        Date checkin_end_date = null;
        boolean order_by_annotator = false;
        boolean order_by_source_file_id = false;
        boolean order_by_checkin = false;
        boolean order_by_checkout = false;
        boolean order_by_outfor = false;
        String wh_ids = "-1";
        String filetype = "completed";
        File file_to_stream = null;
        int type = AnnotationFile.ACTION_OBSERVE;
        String action = (request.getParameter("action") != null) ? request.getParameter("action") : "";
        if (action.compareTo("observe") == 0) {
            source_file_info_id = (request.getParameter("source_file_info_id") != null) ? Integer.parseInt(request.getParameter("source_file_info_id")) : -1;
            checkout_begin_date = (request.getParameter("checkout_begin_date") != null) ? new Date(Long.parseLong(request.getParameter("checkout_begin_date"))) : null;
            checkout_end_date = (request.getParameter("checkout_end_date") != null) ? new Date(Long.parseLong(request.getParameter("checkout_end_date"))) : null;
            checkin_begin_date = (request.getParameter("checkin_begin_date") != null) ? new Date(Long.parseLong(request.getParameter("checkin_begin_date"))) : null;
            checkin_end_date = (request.getParameter("checkin_end_date") != null) ? new Date(Long.parseLong(request.getParameter("checkin_end_date"))) : null;
            order_by_annotator = (request.getParameter("order_by_annotator") != null && request.getParameter("order_by_annotator").compareTo("1") == 0) ? true : false;
            order_by_source_file_id = (request.getParameter("order_by_source_file_id") != null && request.getParameter("order_by_source_file_id").compareTo("1") == 0) ? true : false;
            order_by_checkin = (request.getParameter("order_by_checkin") != null && request.getParameter("order_by_checkin").compareTo("1") == 0) ? true : false;
            order_by_checkout = (request.getParameter("order_by_checkout") != null && request.getParameter("order_by_checkout").compareTo("1") == 0) ? true : false;
            order_by_outfor = (request.getParameter("order_by_outfor") != null && request.getParameter("order_by_outfor").compareTo("1") == 0) ? true : false;
            filetype = (request.getParameter("filetype") != null) ? request.getParameter("filetype") : "completed";
        }
        int user_id = (request.getParameter("user_id") != null) ? Integer.parseInt(request.getParameter("user_id")) : -1;
        user = (request.getParameter("li_user_id") != null) ? Integer.parseInt(request.getParameter("li_user_id")) : -1;
        int workarea_id = (request.getParameter("workarea_id") != null) ? Integer.parseInt(request.getParameter("workarea_id")) : -1;
        int workflow_history_id = (request.getParameter("workflow_history_id") != null) ? Integer.parseInt(request.getParameter("workflow_history_id")) : -1;
        int numFiles = (request.getParameter("numFiles") != null) ? Integer.parseInt(request.getParameter("numFiles")) : 1;
        int numObsFiles = (request.getParameter("numObsFiles") != null) ? Integer.parseInt(request.getParameter("numObsFiles")) : -1;
        if (action.compareTo("observe") == 0) {
            if (request.getParameter("wh_ids2") != null) {
                for (int i = 0; i < request.getParameterValues("wh_ids2").length; i++) {
                    wh_ids += "," + request.getParameterValues("wh_ids2")[i];
                }
            }
        } else {
            if (request.getParameter("wh_ids") != null) {
                for (int i = 0; i < request.getParameterValues("wh_ids").length; i++) {
                    wh_ids += "," + request.getParameterValues("wh_ids")[i];
                }
            }
        }
        File files[] = null;
        String fileNames[] = null;
        String errormsg = "Error in file download.";
        String zipname = "LAW";
        String reloadURL = null;
        AnnotationFileList afList = null;
        if (((action.compareTo("checkout") == 0 || action.compareTo("recheckout") == 0) && ((user_id != -1 && workarea_id != -1) || (user_id != -1 && workflow_history_id != -1))) || (action.compareTo("observe") == 0 && workarea_id != -1)) {
            Date zipdate = new Date();
            SimpleDateFormat dateformat = new SimpleDateFormat("yyMMdd_HHmm_ssSS");
            if (action.compareTo("checkout") == 0) {
                type = AnnotationFile.ACTION_CHECKOUT;
                errormsg = "There are no files available for you to checkout in this workarea.";
                Checkio cio = new Checkio();
                try {
                    afList = cio.coFile(user_id, workarea_id, numFiles);
                } catch (CheckoutException ce) {
                    errormsg = ce.getMessage();
                }
                zipname = "checkout_" + dateformat.format(zipdate) + "_wa" + workarea_id;
                reloadURL = "checkio.jsp";
            } else if (action.compareTo("recheckout") == 0) {
                type = AnnotationFile.ACTION_REDOWNLOAD;
                Checkio cio = new Checkio();
                if (workarea_id != -1) {
                    afList = cio.getCheckedOutFiles(user_id, workarea_id, wh_ids, true);
                    zipname = "redownload_" + dateformat.format(zipdate) + "_wa" + workarea_id;
                    errormsg = "There are no files for you to re-download in this workarea.";
                } else {
                    afList = cio.getCheckedOutFiles(user_id, -1, "" + workflow_history_id, true);
                    zipname = "redownload_S_" + dateformat.format(zipdate) + "_wa" + workarea_id;
                    errormsg = "File: " + workflow_history_id + " is not available for you to re-download in this workarea.";
                }
            } else if (action.compareTo("observe") == 0) {
                type = AnnotationFile.ACTION_OBSERVE;
                if (workarea_id != -1) {
                    Workflow w = new Workflow();
                    afList = w.observe(workarea_id, user_id, source_file_info_id, checkout_begin_date, checkout_end_date, checkin_begin_date, checkin_end_date, numObsFiles, order_by_annotator, order_by_source_file_id, order_by_checkin, order_by_checkout, order_by_outfor, filetype, wh_ids);
                }
                zipname = "observe_" + dateformat.format(zipdate) + "_wa" + workarea_id;
                errormsg = "No files available for observation.<br>(wa:" + workarea_id + ")";
            }
            if (afList != null) {
                zipname += ("_u" + user + ".zip");
                file_to_stream = createZip(afList, type, zipname);
                if (file_to_stream.exists() && file_to_stream.canRead()) {
                    response.setContentType("APPLICATION/OCTET-STREAM");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + zipname + "\"");
                    response.setContentLength((int) file_to_stream.length());
                    try {
                        ServletOutputStream servletout = response.getOutputStream();
                        InputStream filein = new FileInputStream(file_to_stream);
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = filein.read(buf)) > 0) {
                            servletout.write(buf, 0, len);
                        }
                        servletout.flush();
                        filein.close();
                        servletout.close();
                    } catch (java.net.SocketException se) {
                        dmsg("**Client may have canceled D/L:(" + se.getMessage() + ") Continuing normally.. ");
                    }
                    if (file_to_stream.exists()) {
                        file_to_stream.delete();
                    }
                    dmsg("processRequest: user:(" + user + ") Successfully sent " + file_to_stream.getAbsolutePath() + " to client on action=" + action + ".");
                } else {
                    derr("processRequest: Could not find [" + file_to_stream.getAbsolutePath() + "] after creating it. (action=" + action + ")");
                    response.sendRedirect("/error.jsp?msg=" + "There was a problem finding the file you requested on the server." + "<br><br>uid:" + user_id + "<br>waid:" + workarea_id + "<br>zip_file:" + file_to_stream.getAbsolutePath() + "<br><br>Email this error to:<br>Eric Pancoast<br>edp23@linc.cis.upenn.edu");
                }
            } else {
                dmsg("processRequest: user:(" + user + ") No files found to zip up. (action=" + action + ")");
                response.sendRedirect("/error.jsp?msg=" + errormsg);
            }
        }
        dmsg("processRequest: user:(" + user + ") (*) File checkout complete." + ((afList == null) ? "\nERROR:" + errormsg : " (" + afList.size() + " AF files in '" + file_to_stream.getAbsolutePath() + "')"));
    }

    public File createZip(AnnotationFileList afList, int type, String zip_file_name) {
        File temp_zip_dir = new File(TEMP_CO);
        if (!temp_zip_dir.isDirectory()) {
            temp_zip_dir.mkdirs();
        }
        String absolute_zip_file = TEMP_CO + File.separator + zip_file_name;
        byte b[] = new byte[512];
        File zip_file = null;
        FileOutputStream zip_stream = null;
        try {
            zip_file = new File(absolute_zip_file);
            zip_file.createNewFile();
            zip_stream = new FileOutputStream(absolute_zip_file);
        } catch (java.io.FileNotFoundException fnfe) {
            derr(" createZip: '" + absolute_zip_file + "' not found. ");
        } catch (java.io.IOException ioe) {
            derr(" createZip: IOException when creating new file '" + absolute_zip_file + "' ");
        }
        if (zip_stream == null) {
            return null;
        } else {
            try {
                ZipOutputStream zout = new ZipOutputStream(zip_stream);
                zout.setComment(afList.size() + " Files in " + zip_file_name);
                String fl = "";
                int fc = 0;
                derr(" createZip: zipping " + afList.size() + " files... ");
                for (Iterator i = afList.iterator(); i.hasNext(); ) {
                    AnnotationFile af = (AnnotationFile) i.next();
                    File sourceFile = af.getSourceFile();
                    String sourceFileZipName = af.getSourceFileZipName();
                    File annotationFile = null;
                    try {
                        annotationFile = af.getAnnotationFile(type);
                    } catch (InvalidAnnotationFileTypeException iafte) {
                        iafte.printStackTrace();
                        derr(" createZip: " + iafte.getMessage());
                    }
                    String annotationFileZipName = af.getAnnotationFileZipName();
                    InputStream in = null;
                    if (sourceFile != null && sourceFile.exists() && sourceFile.canRead()) {
                        try {
                            in = new FileInputStream(sourceFile);
                            ZipEntry e = new ZipEntry(sourceFileZipName);
                            e.setTime(sourceFile.lastModified());
                            e.setComment("Source File (" + af.getSourceFileID() + ")");
                            zout.putNextEntry(e);
                            int len = 0;
                            while ((len = in.read(b)) != -1) {
                                zout.write(b, 0, len);
                            }
                            zout.closeEntry();
                            fl += e + ((i.hasNext()) ? ", " : " ");
                            fc++;
                        } catch (java.io.FileNotFoundException fnfe) {
                            derr(" createZip: Source file not found: '" + sourceFile.getAbsolutePath() + "'");
                            fnfe.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        derr(" createZip: Problem with source file:" + ((sourceFile != null) ? sourceFile.getAbsolutePath() : sourceFile.toString()));
                        if (sourceFile != null) {
                            derr(" createZip: exists:" + sourceFile.exists() + " canRead:" + sourceFile.canRead());
                        }
                    }
                    if (annotationFile != null && annotationFile.exists() && annotationFile.canRead()) {
                        try {
                            in = new FileInputStream(annotationFile);
                            ZipEntry e = new ZipEntry(annotationFileZipName);
                            e.setTime(sourceFile.lastModified());
                            e.setComment("Annotation File (pwh" + af.getPreviousWorkflowHistoryID() + "/wh" + af.getWorkflowHistoryID() + ")");
                            zout.putNextEntry(e);
                            int len = 0;
                            while ((len = in.read(b)) != -1) {
                                zout.write(b, 0, len);
                            }
                            zout.closeEntry();
                            fl += e + ((i.hasNext()) ? ", " : " ");
                            fc++;
                        } catch (java.io.FileNotFoundException fnfe) {
                            derr(" createZip: Source file not found: '" + annotationFile.getAbsolutePath() + "'");
                            fnfe.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        derr(" createZip: Problem with source file: " + ((annotationFile != null) ? annotationFile.getAbsolutePath() : "[null] type:" + type + " af_history_valid:" + af.validHistory()));
                        if (annotationFile != null) {
                            derr(" createZip: exists:" + annotationFile.exists() + " canRead:" + annotationFile.canRead());
                        }
                    }
                }
                dmsg(" createZip: User:(" + user + ") Zipped " + fc + " files : \n'" + fl + "' .");
                zout.close();
            } catch (java.io.IOException ioe) {
                derr(" createZip: IOException when zipping to '" + absolute_zip_file + "' ");
                ioe.printStackTrace();
            }
        }
        return zip_file;
    }

    public void copyFile(String source_file_path, String destination_file_path) {
        FileWriter fw = null;
        FileReader fr = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        File source = null;
        try {
            fr = new FileReader(source_file_path);
            fw = new FileWriter(destination_file_path);
            br = new BufferedReader(fr);
            bw = new BufferedWriter(fw);
            source = new File(source_file_path);
            int fileLength = (int) source.length();
            char charBuff[] = new char[fileLength];
            while (br.read(charBuff, 0, fileLength) != -1) bw.write(charBuff, 0, fileLength);
        } catch (FileNotFoundException fnfe) {
            System.out.println(source_file_path + " does not exist!");
        } catch (IOException ioe) {
            System.out.println("Error reading/writing files!");
        } finally {
            try {
                if (br != null) br.close();
                if (bw != null) bw.close();
            } catch (IOException ioe) {
            }
        }
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "This servlet streams a file from the filesystem through the webserver that would normally be inaccessible.";
    }
}
