package net.sf.sageplugins.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.sageplugins.sageutils.SageApi;
import net.sf.sageplugins.sageutils.Translate;
import net.sf.sageplugins.sagexmlinfo.SageXmlReader;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

public class XmlImporterServlet extends SageServlet {

    private static final long serialVersionUID = -4410527793026183735L;

    private static final int REDATE_MODE_NONE = 0;

    private static final int REDATE_MODE_MEDIAFILE = 1;

    private static final int REDATE_MODE_AIRING = 1;

    private static final String UNC_REGEX = "^\\\\\\\\[^\\\\].*";

    private static final String MULTIPLE_SLASH_REGEX = "[(\\\\)/]{2,}";

    private static final String TRAILING_SLASH_REGEX = "[(\\\\)/]$";

    protected void doServletGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (SageApi.booleanApi("IsClient", null)) {
            htmlHeaders(resp);
            noCacheHeaders(resp);
            PrintWriter out = resp.getWriter();
            xhtmlHeaders(out);
            out.println("<head>");
            jsCssImport(req, out);
            out.println("<title>Import Sage XML</title></head>");
            out.println("<body>");
            printTitle(out, "Import Sage XML: Error");
            out.println("<div id=\"content\">");
            out.println("<h3>Cannot run in the context of a client</h3>");
            out.println("</div>");
            printMenu(req, out);
            out.println("</body></html>");
            out.close();
            return;
        }
        if (req.getQueryString() == null || !req.getMethod().equalsIgnoreCase("POST") || !req.getQueryString().startsWith("Import")) {
            printForm(req, resp);
        } else {
            importXml(req, resp);
        }
    }

    private void printForm(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        htmlHeaders(resp);
        noCacheHeaders(resp);
        PrintWriter out = resp.getWriter();
        xhtmlHeaders(out);
        out.println("<head>");
        jsCssImport(req, out);
        out.println("<title>Import Sage XML</title></head>");
        out.println("<body>");
        printTitle(out, "Import Sage XML Info");
        out.println("<div id=\"content\">");
        out.println("<h3>Warning: This is an experimental feature. \n" + "<br/>Backup your wiz.bin and use at your own risk</h3>\n" + "For more information on the XML file format, see the <a href=\"http://tools.assembla.com/sageplugins/wiki/SageXmlInfo\">SageXML page on the wiki</a>");
        out.println("<form method='post' enctype='multipart/form-data' action='XmlImporter?Import=yes' name='XmlImportForm'>");
        if (req.getParameter("MediaFileId") != null) {
            Airing mf;
            try {
                mf = new Airing(req);
            } catch (Exception e) {
                out.println("Invalid MediaFileID: " + e);
                out.println("</form>");
                out.println("</div>");
                printMenu(out);
                out.println("</body></html>");
                out.close();
                return;
            }
            if (SageApi.booleanApi("IsDVD", new Object[] { mf.sageAiring })) {
                out.println("cannot import show info for DVDs");
                out.println("</form>");
                out.println("</div>");
                printMenu(out);
                out.println("</body></html>");
                out.close();
                return;
            }
            if (mf.idType == Airing.ID_TYPE_AIRING) {
                out.println("argument is not a Sage Media File");
                out.println("</form>");
                out.println("</div>");
                printMenu(out);
                out.println("</body></html>");
                out.close();
                return;
            }
            out.println("Import Show information for file:");
            File files[] = (File[]) SageApi.Api("GetSegmentFiles", new Object[] { mf.sageAiring });
            if (files != null) {
                out.println("<pre>");
                for (int i = 0; i < files.length; i++) {
                    out.print(Translate.encode(files[i].getAbsolutePath()));
                }
                out.println("</pre>");
            }
            out.println("<input type='hidden' value='on' name='impMediaFiles'/>");
            out.println("<input type='hidden' value='" + mf.id + "' name='MediaFileID'/>");
            out.println("<ul>\n" + "    <li><input type='checkbox' checked='checkbox' name='impShowOverwrite'/>Overwrite Existing Show Information</li>\n" + "    <li><input type='checkbox' name='forceUnviewableChannel'/>Force use of channels that are not currently viewable</li>\n" + "     <li>Filename handling: <br/>\n" + "         &nbsp;&nbsp;<input type=\"radio\" name=\"impMFRename\" value=\"keep\" checked=\"checked\"/>Keep original imported filename<br/>\n" + "     </li>" + "     <li>File timestamp handling<br/>\n" + "         &nbsp;&nbsp;<input type=\"radio\" name=\"impMFRedate\" value=\"keep\" checked=\"checked\"/>Use file's timestamp to determine file startTime<br/>\n" + "     </li>" + "</ul>");
        } else {
            out.println("<ul>");
            out.println("  <li><input type='checkbox' name='impFaves'/>Import Favorites</li>\n" + "  <li>Favorite Options:\n" + "  <ul><li><input type='checkbox' checked='checked' name='impFavesOverwrite'/>Overwrite Existing Favorites</li></ul>\n" + "  <li><input type='checkbox'  name='impAirs'/>Import TV Airings</li>\n" + "  <li><input type='checkbox'  name='impTVFiles'/>Import TV files</li>\n" + "  <li>Airing/MediaFile Options:\n" + "  <ul>\n" + "     <li><input type='checkbox' checked='checkbox' name='impShowOverwrite'/>Overwrite Existing Show Information</li>\n" + "     <li><input type='checkbox' name='forceUnviewableChannel'/>Force use of channels that are not currently viewable</li>\n" + "     <li><input type='checkbox' checked='' name='impMFOverwrite'/>Overwrite Existing Sage TV File Information</li>\n" + "     <li>Filename handling: <br/>\n" + "         <ul><li><input type=\"radio\" name=\"impMFRename\" value=\"keep\" checked=\"checked\"/>Keep original imported filename<br/>\n" + "     </ul></li>\n" + "     <li>File timestamp handling<br/>\n" + "     <ul><li><input type=\"radio\" name=\"impMFRedate\" value=\"keep\" checked=\"checked\"/>Use file's original timestamp to determine file startTime<br/>\n" + "         <li><input type=\"radio\" name=\"impMFRedate\" value=\"redateFromFile\"/>Use imported file data to determine startTime<br/>(use when file timestamp/timeline is incorrect. Will change imported file timestamp)\n" + "         <li><input type=\"radio\" name=\"impMFRedate\" value=\"redateFromAiring\"/>Use imported Airing data to determine startTime<br/>(use when importing edited files. Will change imported file timestamp)\n" + "     </ul>\n" + "     </li>\n" + "  </ul>\n" + "</li>" + "</ul>");
        }
        out.println("<p>Paste any XML to import here:<br/>\n" + "<input type=\"textarea\" name=\"pastedXml\" cols='80' rows='5' value=\"\"/></p>");
        out.println("<p>Directory Path To Import All XML (EX: D:\\PVR_DATA):<br/>\n" + "This path must be accessible to the server.<br/>\n" + "<input type=\"text\" name=\"directoryXML\" size='80' value=\"\"/></p>");
        out.println("<p>Upload any Sage XML files here:<br/>" + "<input type=\"file\" accept=\"text/xml\" size='65' name=\"xmlFile\"/></p>");
        out.println("<p><input type=\"submit\" name=\"Import\"/></p>");
        out.println("</form>");
        out.println("<hr/>\n" + "<p>Note that any airings imported using this function will never be removed from the DB, unless the property\n" + "<tt>wizard/retain_airings_from_completed_recordings</tt> is set to false</p>\n");
        printFooter(req, out);
        out.println("</div>");
        printMenu(out);
        out.println("</body></html>");
        out.close();
    }

    private void importXml(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        SageXmlReader fileReader = null;
        String fileName = null;
        SageXmlReader pastedStringReader = null;
        String mediaFileID = null;
        boolean impFaves = false;
        boolean impFavesOverwrite = false;
        boolean impAirs = false;
        boolean impTVFiles = false;
        boolean impMFOverwrite = false;
        boolean impMFRename = false;
        int impMFRedate = REDATE_MODE_NONE;
        boolean forceUnviewableChannel = false;
        boolean impShowOverwrite = false;
        String directoryXml = null;
        try {
            MultipartParser mp = new MultipartParser(req, Integer.MAX_VALUE, true, true, charset);
            Part part;
            while ((part = mp.readNextPart()) != null) {
                if (part.isParam()) {
                    ParamPart param = (ParamPart) part;
                    if (param.getName().equals("pastedXml")) {
                        String pastedString = param.getStringValue().trim();
                        if (pastedString.length() > 0) {
                            pastedStringReader = new SageXmlReader(null);
                            pastedStringReader.read(pastedString);
                        }
                    } else if (param.getName().equals("directoryXML")) {
                        directoryXml = param.getStringValue();
                        directoryXml = cleanPath(directoryXml);
                    } else if (param.getName().equals("impFaves")) impFaves = true; else if (param.getName().equals("impFavesOverwrite")) impFavesOverwrite = true; else if (param.getName().equals("impShowOverwrite")) impShowOverwrite = true; else if (param.getName().equals("impAirs")) impAirs = true; else if (param.getName().equals("forceUnviewableChannel")) forceUnviewableChannel = true; else if (param.getName().equals("impTVFiles")) impTVFiles = true; else if (param.getName().equals("impMFOverwrite")) impMFOverwrite = true; else if (param.getName().equals("impMFRedate")) {
                        String val = param.getStringValue().trim();
                        if (val.equalsIgnoreCase("redateFromFile")) impMFRedate = REDATE_MODE_MEDIAFILE; else if (val.equalsIgnoreCase("redateFromAiring")) impMFRedate = REDATE_MODE_AIRING; else impMFRedate = REDATE_MODE_NONE;
                    } else if (param.getName().equals("impMFRename")) impMFRename = param.getStringValue().trim().equalsIgnoreCase("rename"); else if (param.getName().equals("MediaFileID")) mediaFileID = param.getStringValue();
                }
                if (part.isFile() && part.getName().equals("xmlFile")) {
                    FilePart filePart = (FilePart) part;
                    if (filePart.getFileName() != null && filePart.getFilePath().trim().length() > 0) {
                        fileName = filePart.getFilePath().trim();
                        fileReader = new SageXmlReader(null);
                        fileReader.read(filePart.getInputStream());
                    }
                }
            }
        } catch (Throwable e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println();
            out.println();
            out.println("<body><pre>");
            out.println("Exception while reading posted data:\n" + e.toString());
            e.printStackTrace(out);
            out.println("</pre>");
            out.close();
            log("Exception while processing servlet", e);
        }
        htmlHeaders(resp);
        PrintWriter out = resp.getWriter();
        try {
            xhtmlHeaders(out);
            out.println("<head>");
            jsCssImport(req, out);
            out.println("<title>Import Sage XML</title></head>");
            out.println("<body>");
            printTitle(out, "Import Sage XML Info Results:");
            out.println("<div id=\"content\">");
            if (mediaFileID != null) {
                Airing mf;
                try {
                    mf = new Airing(Airing.ID_TYPE_MEDIAFILE, Integer.parseInt(mediaFileID));
                    if (SageApi.booleanApi("IsDVD", new Object[] { mf.sageAiring })) {
                        throw new Exception("Cannot import data for DVDs");
                    }
                } catch (Exception e) {
                    out.println("Invalid MediaFileID: " + e);
                    out.println("</div>");
                    printMenu(out);
                    out.println("</body></html>");
                    out.close();
                    return;
                }
                if (pastedStringReader != null && fileReader != null) {
                    out.println("Error: can only use either pasted XML or uploaded file, not both");
                    out.println("</div>");
                    printMenu(out);
                    out.println("</body></html>");
                    out.close();
                    return;
                }
                if ((directoryXml != null) && (directoryXml.trim().length() > 0)) {
                    out.println("Error: directory import feature cannot be used when attaching to a single media file");
                    out.println("</div>");
                    printMenu(out);
                    out.println("</body></html>");
                    out.close();
                    return;
                }
                SageXmlReader reader = fileReader;
                if (pastedStringReader != null) reader = pastedStringReader;
                if (!reader.isReadOk()) {
                    out.println("Failed to read XML :<pre> ");
                    if (reader.getLastError() != null) out.println(Translate.encode(reader.getLastError()));
                    out.println("</pre>");
                    out.println("</div>");
                    printMenu(out);
                    out.println("</body></html>");
                    out.close();
                    return;
                }
                if (reader.getLastError() != null) {
                    out.println("XML imported with errors:<pre>" + Translate.encode(reader.getLastError()) + "</pre>");
                }
                net.sf.sageplugins.sagexmlinfo.Airing impAir;
                if (reader.getAiringsWithMediaFiles().size() == 1) impAir = (net.sf.sageplugins.sagexmlinfo.Airing) reader.getAiringsWithMediaFiles().get(0); else if (reader.getAiringsWithoutMediaFiles().size() == 1) impAir = (net.sf.sageplugins.sagexmlinfo.Airing) reader.getAiringsWithoutMediaFiles().get(0); else {
                    out.println("Error: XML contains " + reader.getAiringsWithMediaFiles().size() + reader.getAiringsWithoutMediaFiles().size() + " airings - it must contain 1 single airing for this function to work.");
                    out.println("</div>");
                    printMenu(out);
                    out.println("</body></html>");
                    out.close();
                    return;
                }
                out.println("<pre>");
                out.print("Import Show information for file: ");
                File files[] = (File[]) SageApi.Api("GetSegmentFiles", new Object[] { mf.sageAiring });
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        out.print(Translate.encode(files[i].getAbsolutePath()));
                    }
                }
                ImportStatus importStatus = new ImportStatus();
                associateFileToImportedAiring(out, impAir, files[0], mf.sageAiring, importStatus, forceUnviewableChannel, impShowOverwrite, impMFRename, impMFRedate, impMFOverwrite);
                out.println("</pre></div>");
                printMenu(out);
                out.println("</body></html>");
                out.close();
                return;
            } else {
                ArrayList<SageXmlReader> listOfXMLFiles = new ArrayList<SageXmlReader>();
                if ((directoryXml != null) && (directoryXml.trim().length() > 0)) {
                    File directory = new File(directoryXml);
                    String[] listing = directory.list();
                    if (listing == null) {
                        out.println(directory.getPath() + " is not a valid directory on the server");
                        out.println("</div>");
                        printMenu(out);
                        out.println("</body></html>");
                        out.close();
                        return;
                    }
                    for (int fileID = 0; fileID < listing.length; fileID++) {
                        if (listing[fileID] != null && listing[fileID].toLowerCase().endsWith(".xml")) {
                            File file = new File(directoryXml, listing[fileID]);
                            SageXmlReader tempFileReader = new SageXmlReader(null);
                            tempFileReader.read(new FileInputStream(file));
                            listOfXMLFiles.add(tempFileReader);
                        }
                    }
                    DecimalFormat wholeNumberDF = new DecimalFormat("#,##0");
                    out.println("<h3>Processing List of " + wholeNumberDF.format(listOfXMLFiles.size()) + " Files:<br>");
                } else if (pastedStringReader != null) {
                    out.println("<h3>Processing pasted XML:</h3>");
                    listOfXMLFiles.add(pastedStringReader);
                } else if (fileReader != null) {
                    out.println("<h3>Processing imported file: " + Translate.encode(fileName) + "</h3>");
                    listOfXMLFiles.add(fileReader);
                } else {
                    out.println("Error: no input data was entered");
                    out.println("</div>");
                    printMenu(out);
                    out.println("</body></html>");
                    out.close();
                    return;
                }
                for (SageXmlReader reader : listOfXMLFiles) {
                    if (reader.isReadOk()) {
                        if (reader.getLastError() != null) {
                            out.println("XML imported with errors:<pre>" + Translate.encode(reader.getLastError()) + "</pre>");
                        }
                        ProcessImportedData(out, reader, impFaves, impFavesOverwrite, impAirs, forceUnviewableChannel, impShowOverwrite, impTVFiles, impMFOverwrite, impMFRename, impMFRedate);
                    } else {
                        out.println("Failed to read XML file:<pre> ");
                        if (reader.getLastError() != null) out.println(Translate.encode(reader.getLastError()));
                        out.println("</pre>");
                    }
                    out.println("<br/><br/>");
                    out.flush();
                }
            }
            out.println("</div>");
            printMenu(out);
            out.println("</body></html>");
            out.flush();
            out.close();
        } catch (Throwable e) {
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/html");
            }
            out.println();
            out.println();
            out.println("<body><pre>");
            out.println("Exception while processing servlet:\n" + e.toString());
            e.printStackTrace(out);
            out.println("</pre>");
            out.close();
            log("Exception while processing servlet", e);
        }
    }

    /**
     * import data from importedData according to options 
     * 
     * @param out
     * @param importedData
     * @param doImpFaves
     * @param doImpFavesOverwrite
     * @param doImpAirs
     * @param forceUnviewableChannel
     * @param doImpMediaFiles
     * @param doImpMFOverwrite
     * @param doImpMFRename
     */
    private void ProcessImportedData(PrintWriter out, SageXmlReader importedData, boolean doImpFaves, boolean doImpFavesOverwrite, boolean doImpAirs, boolean forceUnviewableChannel, boolean impShowOverwrite, boolean doImpTVFiles, boolean doImpMFOverwrite, boolean doImpMFRename, int doImpMFRedate) {
        if (doImpFaves) {
            ImportStatus importStatus = new ImportStatus();
            out.println("<hr/><h4>Importing " + importedData.getFavorites().size() + " favorites:</h4><pre>");
            for (Iterator<?> iter = importedData.getFavorites().values().iterator(); iter.hasNext(); ) {
                net.sf.sageplugins.sagexmlinfo.Favorite impFave = (net.sf.sageplugins.sagexmlinfo.Favorite) iter.next();
                importSingleFavorite(out, impFave, importStatus, doImpFavesOverwrite);
                out.flush();
            }
            out.println("</pre>\n" + "Imported " + importStatus.imported + " favorites");
            if (importStatus.warnings > 0) out.println(" (with " + importStatus.warnings + " warnings)");
            if (importStatus.skipped > 0) out.println("<br/>skipped importing " + importStatus.skipped + " favorites");
            if (importStatus.errors > 0) out.println("<br/>failed importing " + importStatus.errors + " favorites");
            out.flush();
        }
        if (doImpTVFiles) {
            ImportStatus importStatus = new ImportStatus();
            out.println("<hr/><h4>Importing " + importedData.getAiringsWithMediaFiles().size() + " mediaFiles:</h4><pre>");
            for (Iterator<?> iter = importedData.getAiringsWithMediaFiles().iterator(); iter.hasNext(); ) {
                net.sf.sageplugins.sagexmlinfo.Airing impAir = (net.sf.sageplugins.sagexmlinfo.Airing) iter.next();
                importSingleMediaFile(out, impAir, importStatus, forceUnviewableChannel, impShowOverwrite, doImpMFRename, doImpMFRedate, doImpMFOverwrite);
                out.flush();
            }
            out.println("</pre>\n" + "Imported " + importStatus.imported + " mediaFiles");
            if (importStatus.warnings > 0) out.println(" (with " + importStatus.warnings + " warnings)");
            if (importStatus.skipped > 0) out.println("<br/>skipped importing " + importStatus.skipped + " mediaFiles");
            if (importStatus.errors > 0) out.println("<br/>failed importing " + importStatus.errors + " mediaFiles");
            out.flush();
        }
        if (doImpAirs) {
            List<net.sf.sageplugins.sagexmlinfo.Airing> airingsToImport = null;
            if (doImpTVFiles) {
                airingsToImport = importedData.getAiringsWithoutMediaFiles();
            } else {
                airingsToImport = new java.util.LinkedList<net.sf.sageplugins.sagexmlinfo.Airing>(importedData.getAiringsWithoutMediaFiles());
                airingsToImport.addAll(importedData.getAiringsWithMediaFiles());
            }
            out.println("<hr/><h4>Importing " + airingsToImport.size() + " Airings:</h4><pre>");
            ImportStatus importStatus = new ImportStatus();
            for (net.sf.sageplugins.sagexmlinfo.Airing impAir : airingsToImport) {
                importSingleAiring(impAir, importStatus, out, forceUnviewableChannel, impShowOverwrite, true);
                out.flush();
            }
            out.println("</pre>\n" + "Imported " + importStatus.imported + " airings");
            if (importStatus.warnings > 0) out.println(" (with " + importStatus.warnings + " warnings)");
            if (importStatus.skipped > 0) out.println("<br/>skipped importing " + importStatus.skipped + " airings");
            if (importStatus.errors > 0) out.println("<br/>failed importing " + importStatus.errors + " airings");
            out.flush();
        }
        out.println("<hr/>");
    }

    /**
     * Imports a singleAiring and associates a Sage MediaFile to it
     * retports status to out, assumes a pre block
     * 
     * @param out
     * @param reader
     * @param mediaFile
     * @param forceUnviewableChannel
     * @param impMFRename
     * @param impMFRedate
     * @param impMFOverwrite
     * @return true==success
     * @throws InvocationTargetException
     */
    boolean associateFileToImportedAiring(PrintWriter out, net.sf.sageplugins.sagexmlinfo.Airing impAir, File file, Object mediaFile, ImportStatus importStatus, boolean forceUnviewableChannel, boolean impShowOverwrite, boolean impMFRename, int impMFRedate, boolean impMFOverwrite) throws InvocationTargetException {
        if (impAir.getStartTime() <= 0) {
            long startTime = ((Long) SageApi.Api("GetFileStartTime", mediaFile)).longValue();
            impAir.setStartTime(startTime);
        }
        if (impAir.getDuration() <= 0) {
            long duration = ((Long) SageApi.Api("GetFileDuration", mediaFile)).longValue();
            impAir.setDuration(duration);
        }
        if (importSingleAiring(impAir, importStatus, out, forceUnviewableChannel, impShowOverwrite, false)) {
            Object sageAir = impAir.findSageObject(forceUnviewableChannel);
            System.out.println("XML import assiging MF " + mediaFile.toString() + " to Airing " + sageAir);
            try {
                if (SageApi.booleanApi("SetMediaFileAiring", new Object[] { mediaFile, sageAir })) {
                    out.println("  =&gt; successfully assigned imported airing to MediaFile");
                    return true;
                } else {
                    out.println("  =&gt;failed to assign imported airing to MediaFile");
                    importStatus.errors++;
                    return true;
                }
            } finally {
            }
        } else {
            out.println("  =&gt;not updating MediaFile information");
            return false;
        }
    }

    /**
     * Imports a single airing into Sage DB 
     * updates importStatus counters
     * Logs result to out (assumes a pre block)
     * 
     * @param impAir
     * @param importStatus
     * @param out
     * @param forceUnviewableChannel
     * @throws InvocationTargetException
     * @return boolean if success;
     */
    private boolean importSingleAiring(net.sf.sageplugins.sagexmlinfo.Airing impAir, ImportStatus importStatus, PrintWriter out, boolean forceUnviewableChannel, boolean impShowOverwrite, boolean forceTVAiring) {
        String extId = impAir.getShow().getExtId();
        if (extId.startsWith("EP") || extId.startsWith("SH") || extId.startsWith("MV") || extId.startsWith("SP")) {
            try {
                Object sageAir = impAir.findSageObject(forceUnviewableChannel);
                if (sageAir != null) {
                    String descr = net.sf.sageplugins.sageutils.SageApi.StringApi("PrintAiringShort", new Object[] { sageAir });
                    Airing airing = new Airing(sageAir);
                    out.println("Airing already exists: <a href=\"DetailedInfo?" + airing.getIdArg() + "\">" + Translate.encode(descr) + "</a>");
                    importStatus.skipped++;
                    return true;
                } else if (impAir.getStartTime() <= 0) {
                    out.println("Airing for show ID " + extId + " cannot be imported -- no startTime specified");
                    importStatus.errors++;
                    return false;
                } else if (impAir.getDuration() <= 0) {
                    out.println("Airing for show ID " + extId + " cannot be imported -- no duration specified");
                    importStatus.errors++;
                    return false;
                } else {
                    Object sageShow = impAir.getShow().findSageObject();
                    if (sageShow == null || impShowOverwrite) {
                        sageShow = impAir.getShow().createSageObject();
                    } else {
                        importStatus.warnings++;
                        out.println("Show information already exists -- not using imported data\n");
                    }
                    Object[] ret = impAir.createSageObject(forceUnviewableChannel, forceTVAiring);
                    if (ret != null) {
                        importStatus.imported++;
                        String descr = net.sf.sageplugins.sageutils.SageApi.StringApi("PrintAiringShort", new Object[] { ret[0] });
                        Airing airing = new Airing(ret[0]);
                        out.println("Airing <a href=\"DetailedInfo?" + airing.getIdArg() + "\">" + Translate.encode(descr) + "</a> imported");
                        if (ret[1] != null) {
                            importStatus.warnings++;
                            out.println("  with warnings: " + Translate.encode((String) ret[1]));
                        }
                        return true;
                    } else {
                        importStatus.errors++;
                        return false;
                    }
                }
            } catch (Exception e) {
                out.println("Failed to import Airing " + impAir.getId() + " - " + e);
                log("Failed to import Airing ", e);
                importStatus.errors++;
                return false;
            }
        } else {
            importStatus.skipped++;
            out.println("skipping Airing ID " + impAir.getId() + " for showID " + extId + " -- not a TV airing");
            return false;
        }
    }

    /**
     * Imports a single mediaFile into Sage DB 
     * updates importStatus counters
     * Logs result to out (assumes a pre block)
     * 
     * @param out
     * @param impAir
     * @param importStatus
     * @param forceUnviewableChannel
     * @param impMFRename
     * @param impMFRedate
     * @param impMFOverwrite
     * @return
     */
    boolean importSingleMediaFile(PrintWriter out, net.sf.sageplugins.sagexmlinfo.Airing impAir, ImportStatus importStatus, boolean forceUnviewableChannel, boolean impShowOverwrite, boolean impMFRename, int impMFRedate, boolean impMFOverwrite) {
        String filename = ((net.sf.sageplugins.sagexmlinfo.MediaFileSegment) impAir.getMediaFile().getSegments().get(0)).getFilename();
        try {
            File file = new File(filename);
            if (!file.exists()) {
                out.println("Error MediaFile " + filename + " does not exist");
                importStatus.errors++;
                return false;
            }
            if (!file.canRead()) {
                out.println("Error MediaFile " + filename + " is not readable");
                importStatus.errors++;
                return false;
            }
            Object sageMediaFile = impAir.getMediaFile().findSageObject();
            if (sageMediaFile != null) {
                if (impMFOverwrite || !SageApi.booleanApi("IsTVFile", new Object[] { sageMediaFile })) {
                    if (impMFRedate != REDATE_MODE_NONE) {
                        File[] segments = (File[]) SageApi.Api("GetSegmentFiles", sageMediaFile);
                        if (segments.length != 1) {
                            out.println("Warning MediaFile " + filename + " has multiple segments: cannot reset timestamps");
                            importStatus.warnings++;
                        } else {
                            long fileEndTime = 0;
                            if (impMFRedate == REDATE_MODE_AIRING) {
                                if (impAir.getStartTime() > 0 && impAir.getDuration() > 0) fileEndTime = impAir.getStartTime() + impAir.getDuration();
                            } else if (impMFRedate == REDATE_MODE_MEDIAFILE) {
                                net.sf.sageplugins.sagexmlinfo.MediaFile impMf = impAir.getMediaFile();
                                if (impMf.getStartTime() > 0 && impMf.getDuration() > 0) fileEndTime = impMf.getStartTime() + impMf.getDuration();
                            }
                            if (fileEndTime > 0 && fileEndTime != segments[0].lastModified()) {
                                File newFile = new File(segments[0].getAbsolutePath() + ".tmp");
                                if (!segments[0].renameTo(newFile)) {
                                    out.println("Warning MediaFile " + filename + " timestamp could not be modified (file in use?)");
                                    importStatus.warnings++;
                                } else {
                                    try {
                                        for (int i = 0; i < 10; i++) {
                                            System.out.println("Deleteing " + sageMediaFile.toString());
                                            if (!SageApi.booleanApi("DeleteFile", new Object[] { sageMediaFile })) {
                                                out.println("Warning MediaFile " + filename + " timestamp could not be modified (file in use?)");
                                                importStatus.warnings++;
                                                newFile.renameTo(segments[0]);
                                                break;
                                            } else {
                                                if (!SageApi.booleanApi("IsMediaFileObject", new Object[] { sageMediaFile })) break;
                                                System.out.println("File Object not yet deleted");
                                                Thread.sleep(200);
                                            }
                                        }
                                    } finally {
                                        newFile.setLastModified(fileEndTime);
                                        newFile.renameTo(segments[0]);
                                    }
                                    System.out.println("recreating " + filename);
                                    sageMediaFile = SageApi.Api("AddMediaFile", new Object[] { filename, "" });
                                    System.out.println("new MF " + sageMediaFile.toString());
                                    if (sageMediaFile == null) {
                                        out.println("Error MediaFile " + filename + " could not be imported by sage (see logging)");
                                        importStatus.errors++;
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Airing airing = new Airing(sageMediaFile);
                    out.println("Skipping MediaFile " + filename + ":\n   Already exists: <a href=\"DetailedInfo?" + airing.getIdArg() + "\">" + Translate.encode(airing.getAiringShortDescr()) + "</a>");
                    importStatus.skipped++;
                    return false;
                }
            } else {
                if (impAir.getMediaFile().getSegments().size() > 1) {
                    out.println("Warning MediaFile " + filename + " has multiple segments: only importing first segment");
                    importStatus.warnings++;
                }
                if (impMFRedate != REDATE_MODE_NONE) {
                    long fileEndTime = 0;
                    if (impMFRedate == REDATE_MODE_AIRING) {
                        if (impAir.getStartTime() > 0 && impAir.getDuration() > 0) fileEndTime = impAir.getStartTime() + impAir.getDuration();
                    } else if (impMFRedate == REDATE_MODE_MEDIAFILE) {
                        net.sf.sageplugins.sagexmlinfo.MediaFile impMf = impAir.getMediaFile();
                        if (impMf.getStartTime() > 0 && impMf.getDuration() > 0) fileEndTime = impMf.getStartTime() + impMf.getDuration();
                    }
                    if (fileEndTime > 0) {
                        File filePath = new File(filename);
                        if (!filePath.setLastModified(fileEndTime)) {
                            out.println("Warning could not reset timestamp on MediaFile " + filename);
                            importStatus.warnings++;
                        }
                    }
                }
                sageMediaFile = SageApi.Api("AddMediaFile", new Object[] { filename, "" });
                System.out.println("new MF " + sageMediaFile.toString());
                if (sageMediaFile == null) {
                    out.println("Error MediaFile " + filename + " could not be imported by sage (see logging)");
                    importStatus.errors++;
                    return false;
                }
            }
            return associateFileToImportedAiring(out, impAir, file, sageMediaFile, importStatus, forceUnviewableChannel, impShowOverwrite, impMFRename, impMFRedate, impMFOverwrite);
        } catch (Exception e) {
            out.println("Failed to import MediaFile " + filename + " - " + e);
            importStatus.errors++;
            log("Failed to import MediaFile ", e);
            return false;
        }
    }

    /**
     * Imports a single favorite into Sage DB 
     * updates importStatus counters
     * Logs result to out (assumes a pre block)
     * 
     * @param out
     * @param impFave
     * @param importStatus
     * @param doImpFavesOverwrite
     * @return
     */
    boolean importSingleFavorite(PrintWriter out, net.sf.sageplugins.sagexmlinfo.Favorite impFave, ImportStatus importStatus, boolean doImpFavesOverwrite) {
        try {
            Object sageFave = impFave.findSageObject();
            if (sageFave != null && !doImpFavesOverwrite) {
                String descr = net.sf.sageplugins.sageutils.SageApi.StringApi("GetFavoriteDescription", new Object[] { sageFave });
                Favorite fave = new Favorite(sageFave);
                out.println("Favorite already exists: <a href=\"FavoriteDetails?" + fave.getIdArg() + "\">\"" + Translate.encode(descr) + "\"</a>");
                importStatus.skipped++;
                return true;
            } else {
                Object[] ret = impFave.createSageObject();
                if (ret != null) {
                    importStatus.imported++;
                    String descr = net.sf.sageplugins.sageutils.SageApi.StringApi("GetFavoriteDescription", new Object[] { ret[0] });
                    Favorite fave = new Favorite(ret[0]);
                    out.println("Favorite <a href=\"FavoriteDetails?" + fave.getIdArg() + "\">\"" + Translate.encode(descr) + "\"</a> imported ");
                    if (ret[1] != null) {
                        importStatus.warnings++;
                        out.println("  with warnings: " + Translate.encode((String) ret[1]));
                    }
                    return true;
                } else {
                    importStatus.errors++;
                    return false;
                }
            }
        } catch (Exception e) {
            out.println("failed to import favorite " + impFave.getId() + " - " + e);
            importStatus.errors++;
            return false;
        }
    }

    private static String cleanPath(String path) throws IOException {
        String newPath = (path == null) ? "" : path;
        boolean isUNCPath = false;
        isUNCPath = path.matches(UNC_REGEX);
        String replaceWith = (isUNCPath) ? "\\\\" : File.separator;
        if (isUNCPath) {
            newPath = "\\\\" + path.substring(2).replaceAll(MULTIPLE_SLASH_REGEX, replaceWith);
        } else {
            newPath = path.replaceAll(MULTIPLE_SLASH_REGEX, replaceWith);
        }
        newPath = newPath.replaceAll(TRAILING_SLASH_REGEX, "");
        return newPath;
    }

    /**
     * Utitily class to capture import counters
     * @author Niel Markwick
     *
     */
    private class ImportStatus {

        int imported = 0;

        int skipped = 0;

        int errors = 0;

        int warnings = 0;
    }

    public static void main(String[] args) throws IOException {
        String path = "";
        path = "\\\\\\server\\path\\folder\\file";
        cleanPath(path);
        path = "\\\\server\\path\\folder\\file";
        cleanPath(path);
        path = "\\\\server\\path\\";
        cleanPath(path);
        path = "\\\\";
        cleanPath(path);
        path = "a";
        cleanPath(path);
        path = "\\\\server\\path\\\\\\\\";
        cleanPath(path);
        path = "\\\\server\\path\\/\\\\/\\";
        cleanPath(path);
        path = "\\\\server\\/\\\\/\\///path\\/\\\\/\\";
        cleanPath(path);
        path = "\\server\\path\\";
        cleanPath(path);
        path = "C:\\server\\path\\";
        cleanPath(path);
        path = "C:\\server\\path";
        cleanPath(path);
        path = "/home/user/";
        cleanPath(path);
        path = "/home/\\\\\\\\/////\\/user/";
        cleanPath(path);
        path = "/home/user";
        cleanPath(path);
    }
}
