package game.report;

import game.utils.FilePathLocator;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OOOReportRenderer extends ReportRenderer {

    private StringBuffer buf = new StringBuffer();

    private StringBuffer metaBuf = new StringBuffer();

    private static StringBuffer manifestBuf = new StringBuffer();

    public static StringBuffer namedExpressions = new StringBuffer();

    private final File reportFile;

    private final File reportDir;

    private final String reportFileName;

    private final File configDir;

    public static ArrayList<String> imagesList = new ArrayList<String>();

    private static int reportRow = 0;

    public OOOReportRenderer(File reportFile, String reportFileName, File configDir) {
        this.reportFile = reportFile;
        this.reportDir = reportFile.getParentFile();
        this.reportFileName = reportFileName;
        this.configDir = configDir;
    }

    @Override
    protected SRRenderer createRenderer(ISubreport subreport, Collection<ISubreport> allSubreports) {
        return new SROOORenderer(buf, subreport, allSubreports);
    }

    @Override
    public void render() {
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" " + "xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\" xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\" " + "xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\" xmlns:dr3d=\"urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0\" xmlns:math=\"http://www.w3.org/1998/Math/MathML\" " + "xmlns:form=\"urn:oasis:names:tc:opendocument:xmlns:form:1.0\" xmlns:script=\"urn:oasis:names:tc:opendocument:xmlns:script:1.0\" xmlns:ooo=\"http://openoffice.org/2004/office\" xmlns:ooow=\"http://openoffice.org/2004/writer\" xmlns:oooc=\"http://openoffice.org/2004/calc\" " + "xmlns:dom=\"http://www.w3.org/2001/xml-events\" xmlns:xforms=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:rpt=\"http://openoffice.org/2005/report\" " + "xmlns:of=\"urn:oasis:names:tc:opendocument:xmlns:of:1.2\" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:grddl=\"http://www.w3.org/2003/g/data-view#\" xmlns:field=\"urn:openoffice:names:experimental:ooo-ms-interop:xmlns:field:1.0\" office:version=\"1.2\" " + "grddl:transformation=\"http://docs.oasis-open.org/office/1.2/xslt/odf2rdf.xsl\"> \n\n");
        renderStyles();
        buf.append("<office:body>\n");
        buf.append("<office:spreadsheet>\n");
        buf.append("<table:table table:name=\"Sheet1\" table:style-name=\"ta1\" table:print=\"false\">\n");
        renderManifestBegin();
        super.render();
        renderManifestEnd();
        buf.append("</table:table>");
        buf.append("<table:named-expressions>");
        buf.append(namedExpressions);
        buf.append("</table:named-expressions>");
        buf.append("</office:spreadsheet>");
        buf.append("</office:body>\n");
        buf.append("</office:document-content>\n");
        renderMeta();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile, true));
            bw.append(buf);
            bw.close();
            bw = new BufferedWriter(new FileWriter(reportFile + "_meta", true));
            bw.append(metaBuf);
            bw.close();
            bw = new BufferedWriter(new FileWriter(reportFile + "_manifest", true));
            bw.append(manifestBuf);
            bw.close();
            File zipFileName = new File(reportDir.toString() + File.separator + reportFile.getName().substring(0, reportFile.getName().length() - 4));
            System.out.println(zipFileName);
            boolean success = zipFileName.mkdir();
            if (success == true) {
                copyDirectory(new File(FilePathLocator.getInstance().findFile("report-ooo")), zipFileName);
                System.out.println("'report-ooo' copy done");
                File metaDir = new File(zipFileName + File.separator + "META-INF");
                System.out.println("created directory:" + metaDir);
                metaDir.mkdir();
                File picturesDir = new File(zipFileName + File.separator + "Pictures");
                System.out.println("created directory:" + picturesDir);
                picturesDir.mkdir();
                File thumbnailsDir = new File(zipFileName + File.separator + "Thumbnails");
                System.out.println("created directory:" + thumbnailsDir);
                thumbnailsDir.mkdir();
                File oldFile = new File(reportFile.toString());
                File newFile = new File(zipFileName + File.separator + "content.xml");
                success = oldFile.renameTo(newFile);
                if (success == false) {
                    System.err.println("Can't rename exported file to 'content.xml', '.ods' file will be corrupted.");
                }
                System.out.println("zipFilename:" + zipFileName);
                oldFile = new File(reportFile.toString() + "_meta");
                newFile = new File(zipFileName + File.separator + "meta.xml");
                success = oldFile.renameTo(newFile);
                if (success == false) {
                    System.err.println("Can't rename exported file to 'meta.xml', '.ods' file will be corrupted.");
                }
                oldFile = new File(reportFile.toString() + "_manifest");
                System.out.println("reportFile:" + reportFile.toString());
                newFile = new File(zipFileName + File.separator + "META-INF" + File.separator + "manifest.xml");
                success = oldFile.renameTo(newFile);
                if (success == false) {
                    System.err.println("Can't rename exported file to 'META-INF/manifest.xml', '.ods' file will be corrupted.");
                }
                Iterator it = imagesList.iterator();
                while (it.hasNext()) {
                    String str = (String) it.next();
                    oldFile = new File(reportDir + File.separator + str);
                    newFile = new File(zipFileName + File.separator + "Pictures" + File.separator + str);
                    System.out.println("copy:" + zipFileName + File.separator + "Pictures" + File.separator + str);
                    success = oldFile.renameTo(newFile);
                    if (success == false) {
                        System.err.println("Can't rename(copy) image file to '/Pictures/', '.ods' file will be corrupted.");
                    }
                }
                try {
                    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName.toString() + ".ods"));
                    zos.setMethod(ZipOutputStream.DEFLATED);
                    zipDir(zipFileName.toString(), zos, zipFileName.toString());
                    zos.close();
                } catch (Exception e) {
                    System.err.println("Error during generating report.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error during generating report.");
            e.printStackTrace();
        }
    }

    private void renderMeta() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat calFormat1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat calFormat2 = new SimpleDateFormat("HH:mm:ss");
        String time = calFormat1.format(calendar.getTime()) + "T" + calFormat2.format(calendar.getTime()) + ".00";
        metaBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<office:document-meta xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" " + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " + "xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" " + "xmlns:ooo=\"http://openoffice.org/2004/office\" " + "xmlns:grddl=\"http://www.w3.org/2003/g/data-view#\" " + "office:version=\"1.2\" " + "grddl:transformation=\"http://docs.oasis-open.org/office/1.2/xslt/odf2rdf.xsl\">" + "<office:meta><meta:initial-creator>FAKE GAME</meta:initial-creator>" + "<meta:creation-date>" + time + "</meta:creation-date>" + "<dc:date>" + time + "</dc:date>" + "<dc:creator>" + "FAKE GAME" + "</dc:creator>" + "<meta:editing-duration>0</meta:editing-duration>" + "<meta:editing-cycles>1</meta:editing-cycles>" + "<meta:document-statistic meta:table-count=\"0\" meta:cell-count=\"0\" meta:object-count=\"0\"/>" + "<meta:generator>" + "FAKE GAME" + "</meta:generator>" + "</office:meta>" + "</office:document-meta>");
    }

    private void renderManifestBegin() {
        manifestBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\">\n" + " <manifest:file-entry manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\" manifest:version=\"1.2\" manifest:full-path=\"/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/statusbar/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/accelerator/current.xml\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/accelerator/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/floater/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/popupmenu/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/progressbar/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/menubar/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/toolbar/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/images/Bitmaps/\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Configurations2/images/\"/>\n" + " <manifest:file-entry manifest:media-type=\"application/vnd.sun.xml.ui.configuration\" manifest:full-path=\"Configurations2/\"/>");
    }

    private void renderManifestEnd() {
        manifestBuf.append(" <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Pictures/\"/>\n" + " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"content.xml\"/>\n" + " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"styles.xml\"/>\n" + " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"meta.xml\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Thumbnails/thumbnail.png\"/>\n" + " <manifest:file-entry manifest:media-type=\"\" manifest:full-path=\"Thumbnails/\"/>\n" + " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"settings.xml\"/>\n" + "</manifest:manifest>");
    }

    private void renderStyles() {
        buf.append("\"><office:scripts/><office:font-face-decls><style:font-face style:name=\"Arial\" svg:font-family=\"Arial\" style:font-family-generic=\"swiss\" " + "style:font-pitch=\"variable\"/><style:font-face style:name=\"Arial Unicode MS\" svg:font-family=\"&apos;Arial Unicode MS&apos;\" style:font-family-generic=\"system\" " + "style:font-pitch=\"variable\"/><style:font-face style:name=\"Tahoma\" svg:font-family=\"Tahoma\" style:font-family-generic=\"system\" style:font-pitch=\"variable\"/></office:font-face-decls><office:automatic-styles><style:style style:name=\"co1\" " + "style:family=\"table-column\"><style:table-column-properties fo:break-before=\"auto\" style:column-width=\"4.0cm\"/></style:style><style:style style:name=\"ro1\" " + "style:family=\"table-row\"><style:table-row-properties style:row-height=\"0.453cm\" fo:break-before=\"auto\" style:use-optimal-row-height=\"true\"/></style:style><style:style " + "style:name=\"ta1\" style:family=\"table\" style:master-page-name=\"Default\"><style:table-properties table:display=\"true\" style:writing-mode=\"lr-tb\"/></style:style> " + "<style:style style:name=\"ce1\" style:family=\"table-cell\" style:parent-style-name=\"Default\">\n" + "  <style:table-cell-properties fo:background-color=\"#ffcc99\" fo:border=\"none\" /> \n" + "  <style:text-properties fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\" /> \n" + "  </style:style>" + "<style:style style:name=\"ce2\" style:family=\"table-cell\" style:parent-style-name=\"Default\"><style:text-properties fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" " + "style:font-weight-complex=\"bold\"/></style:style><style:style style:name=\"ta_extref\" style:family=\"table\"><style:table-properties table:display=\"false\"/></style:style>" + "<style:style style:name=\"ce3\" style:family=\"table-cell\" style:parent-style-name=\"Default\">\n" + "  <style:text-properties fo:font-style=\"italic\" style:font-style-asian=\"italic\" style:font-style-complex=\"italic\" /> \n" + "  </style:style>" + "<style:style style:name=\"ce4\" style:family=\"table-cell\" style:parent-style-name=\"Default\">\n" + "  <style:table-cell-properties fo:background-color=\"#ffffcc\" fo:border=\"none\" /> \n" + "  <style:text-properties fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\" /> \n" + "  </style:style>" + "<style:style style:name=\"gr1\" style:family=\"graphic\">\n" + "  <style:graphic-properties draw:stroke=\"none\" draw:fill=\"none\" draw:textarea-horizontal-align=\"center\" draw:textarea-vertical-align=\"middle\" draw:color-mode=\"standard\" draw:luminance=\"0%\" draw:contrast=\"0%\" draw:gamma=\"100%\" draw:red=\"0%\" draw:green=\"0%\" draw:blue=\"0%\" fo:clip=\"rect(0cm, 0cm, 0cm, 0cm)\" draw:image-opacity=\"100%\" style:mirror=\"none\" /> \n" + "  </style:style>" + "</office:automatic-styles>\n\n");
    }

    public static int getReportRow() {
        return reportRow;
    }

    public static void addReportRow(int inc) {
        reportRow += inc;
    }

    public static void appendManifestBuf(String str) {
        manifestBuf.append(str);
    }

    public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    static boolean manifest = false;

    /** used from: http://www.devx.com/tips/Tip/14049 **/
    public void zipDir(String dir2zip, ZipOutputStream zos, String callDir) {
        try {
            File zipDir = new File(dir2zip);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            if (manifest == false) {
                File f = new File(zipDir, "META-INF/manifest.xml");
                FileInputStream fis = new FileInputStream(f);
                String n = "META-INF/manifest.xml";
                ZipEntry anEntry = new ZipEntry(n);
                System.out.println("Archiving:" + n);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                manifest = true;
            }
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos, callDir);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String relativePath = f.getPath();
                relativePath = relativePath.replace(callDir + File.separator, "");
                relativePath = relativePath.replace("\\", "/");
                if (relativePath.contains("manifest") == true) {
                    continue;
                }
                ZipEntry anEntry = new ZipEntry(relativePath);
                zos.putNextEntry(anEntry);
                System.out.println("Archiving:" + relativePath);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
            System.err.println("Error during archiving.");
        }
    }

    public static String toOOOText(String text) {
        text = text.replace("<", "&lt;");
        text.replace(">", "&gt;");
        return text;
    }
}
