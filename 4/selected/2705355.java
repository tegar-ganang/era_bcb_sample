package com.umc.builder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.*;
import org.apache.xmlbeans.XmlOptions;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.umc.ConfigController;
import com.umc.UMCStatistics;
import com.umc.beans.ViewResult;
import com.umc.beans.Warning;
import com.umc.beans.media.Episode;
import com.umc.beans.media.Movie;
import com.umc.beans.media.MovieGroup;
import com.umc.beans.media.Season;
import com.umc.beans.media.SeriesGroup;
import com.umc.collector.Publisher;
import com.umc.dao.DataAccessFactory;
import com.umc.dao.UMCDataAccessInterface;
import com.umc.filescanner.FileNameFilterFactory;
import com.umc.filescanner.FilescannerController;
import com.umc.gui.GuiController;
import com.umc.helper.UMCConstants;
import com.umc.helper.UMCLanguage;
import com.umc.helper.UMCUtils;
import de.umcProject.xmlbeans.SkinDocument;

/**
 * Diese Klasse dient als "Backend" Steuerklasse, die den Grossteil der
 * Programmlogik enthält. Sie wird vom Verwaltungsfrontend angesteuert.
 * 
 * @version 0.1 03 Nov 2008
 * @author 	DonGyros
 * 
 */
public class BuilderController {

    public static Logger log = null;

    /**
	When using log4j or apache logging, use
	if (log.isDebugEnabled()) log.debug("My debug message " + makeOnlineMessagePart());
	instead of
	log.debug("My debug message " + makeOnlineMessagePart());
	The rationale is that the first case doesn’t compose the whole message if the debug is not enabled. 
	The second one does create a possibly expensive message for no purpose even if log4j configuration or 
	underling logging subsystem disables debugging.
	*/
    private static boolean debug = false;

    private static UMCDataAccessInterface dao = null;

    /**Mit dieser Aktion können alle selektierten Frontends für den NMT erstellt werden*/
    public static final String ACTION_BUILD_INTERFACES = "build_interfaces";

    /**Mit dieser Aktion kann das PHP-Interface für den NMT erstellt werden*/
    public static final String ACTION_BUILD_PHP_INTERFACE = "build_php_interface";

    /**Mit dieser Aktion kann das Flash-Interface für den NMT erstellt werden*/
    public static final String ACTION_BUILD_FLASH_INTERFACE = "build_flash_interface";

    /**Mit dieser Aktion kann die Scan-Ergebnis HTML-Seite erstellt werden*/
    public static final String ACTION_BUILD_HTML_SCAN_RESULT = "build_html_scan_result";

    /**Mit dieser Aktion kann die Scan-Ergebnis PDF-Seite erstellt werden*/
    public static final String ACTION_BUILD_PDF_SCAN_RESULT = "build_pdf_scan_result";

    static {
        startLogging();
    }

    public static ViewResult dispatch(String aAction) {
        dao = DataAccessFactory.getUMCDataSourceAccessor(DataAccessFactory.DB_TYPE_SQLITE, Publisher.getInstance().getParamDBDriverconnect() + Publisher.getInstance().getParamDBName(), Publisher.getInstance().getParamDBDriver(), Publisher.getInstance().getParamDBUser(), Publisher.getInstance().getParamDBPwd());
        final String sAction = aAction;
        if (sAction != null) {
            if (sAction.equals(ACTION_BUILD_INTERFACES)) return buildInterfaces();
        }
        final ViewResult vres = new ViewResult("Unbekannter Aufruf");
        return vres;
    }

    /**
	 * Initialisiert das logging
	 */
    private static void startLogging() {
        try {
            synchronized (BuilderController.class) {
                if (log != null) return;
                log = Logger.getLogger("com.umc.file");
                debug = log.isDebugEnabled();
            }
        } catch (Throwable ex) {
            System.out.println("UMC Klasse " + BuilderController.class.getName() + " could not write to any logifle : " + ex);
        }
    }

    private static byte[] resize2byteArray(BufferedImage image, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        img.createGraphics().drawImage(new ImageIcon(image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)).getImage(), 0, 0, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        ImageIO.write(img, "jpeg", baos);
        baos.flush();
        byte[] buff = baos.toByteArray();
        baos.close();
        return buff;
    }

    private static PdfPTable createSeriesTable(SeriesGroup sg, PdfOutline outline, Font font7) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setKeepTogether(true);
        table.setSpacingAfter(6f);
        new PdfOutline(outline, PdfAction.gotoLocalPage("ID" + sg.getUuid(), false), sg.getLanguageData().getTitle(Publisher.getInstance().getParamLanguage()).length() < 25 ? sg.getLanguageData().getTitle(Publisher.getInstance().getParamLanguage()) : sg.getLanguageData().getTitle(Publisher.getInstance().getParamLanguage()).substring(0, 24));
        PdfPCell cell = new PdfPCell(new Phrase(new Chunk(sg.getLanguageData().getTitle(Publisher.getInstance().getParamLanguage())).setLocalDestination("ID" + sg.getUuid())));
        cell.setBackgroundColor(new Color(220, 220, 220));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(160, 160, 160));
        cell.setPaddingBottom(4f);
        table.addCell(cell);
        cell = new PdfPCell(new Phrase(sg.getLanguageData().getPlot(Publisher.getInstance().getParamLanguage()), font7));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(160, 160, 160));
        table.addCell(cell);
        Season season = null;
        return table;
    }

    private static PdfPTable createMovieTable(Movie mf, PdfOutline outline, Font font7) throws Exception {
        float[] columnDefinitionSize = { 20.0F, 45.0F, 35.0F };
        PdfPTable table = new PdfPTable(columnDefinitionSize);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidthPercentage(100);
        table.setKeepTogether(true);
        table.setSpacingAfter(6f);
        return table;
    }

    private static ViewResult buildScanResultPDF(Collection<Object> c) {
        log.info(UMCLanguage.getText("builder.pdf.info1", null));
        final ViewResult vres = new ViewResult();
        Document document = null;
        try {
            Font font4 = FontFactory.getFont(FontFactory.HELVETICA, 4);
            Font font5 = FontFactory.getFont(FontFactory.HELVETICA, 5);
            Font font6 = FontFactory.getFont(FontFactory.HELVETICA, 6);
            Font font7 = FontFactory.getFont(FontFactory.HELVETICA, 7);
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(Publisher.getInstance().getParamMediaCenterLocation() + UMCConstants.fileSeparator + "umc-scan-results.pdf"));
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();
            writer.setViewerPreferences(PdfWriter.PageModeUseOutlines | PdfWriter.FitWindow | PdfWriter.PageLayoutOneColumn);
            document.open();
            float[] columnDefinitionSize = { 20.0F, 45.0F, 35.0F };
            float pos = height / 2;
            PdfPTable table = null;
            PdfPCell cell = null;
            PdfOutline rootOutline = writer.getRootOutline();
            PdfOutline movieOutline = new PdfOutline(rootOutline, new PdfAction(), "Filme");
            PdfOutline groupOutline = new PdfOutline(rootOutline, new PdfAction(), "Gruppen");
            PdfOutline seriesOutline = new PdfOutline(rootOutline, new PdfAction(), "Serien");
            List<PdfPTable> movieList = new LinkedList<PdfPTable>();
            Map<String, List<PdfPTable>> movieGroupList = new LinkedHashMap<String, List<PdfPTable>>();
            List<PdfPTable> seriesList = new LinkedList<PdfPTable>();
            for (Object o : c) {
                if (o instanceof MovieGroup) {
                    MovieGroup mg = (MovieGroup) o;
                    List<PdfPTable> mpl = new LinkedList<PdfPTable>();
                    PdfOutline grpOutline = new PdfOutline(groupOutline, new PdfAction(), mg.getComputedTitel());
                    for (Movie part : mg.getChilds()) {
                        mpl.add(createMovieTable(part, grpOutline, font7));
                    }
                    movieGroupList.put(mg.getComputedTitel(), mpl);
                } else if (o instanceof SeriesGroup) {
                    SeriesGroup sg = (SeriesGroup) o;
                    seriesList.add(createSeriesTable(sg, seriesOutline, font7));
                } else {
                    Movie m = (Movie) o;
                    movieList.add(createMovieTable(m, movieOutline, font7));
                }
            }
            for (PdfPTable t : movieList) {
                document.add(t);
            }
            for (String key : movieGroupList.keySet()) {
                document.newPage();
                for (PdfPTable t : movieGroupList.get(key)) {
                    document.add(t);
                }
            }
            for (PdfPTable s : seriesList) {
                document.newPage();
                document.add(s);
            }
            document.close();
            writer.close();
        } catch (DocumentException exc) {
            log.error("PDF Dokument Fehler", exc);
        } catch (IOException exc) {
            log.error("PDF IO Fehler", exc);
        } catch (Exception exc) {
            log.error(UMCLanguage.getText("builder.pdf.error1", null), exc);
            vres.setErrorMessage(UMCLanguage.getText("builder.pdf.error2", null) + " " + exc.getMessage());
        }
        return vres;
    }

    private static ViewResult buildScanResultPDF2() {
        log.info(UMCLanguage.getText("builder.pdf.info1", null));
        final ViewResult vres = new ViewResult();
        Document document = null;
        try {
            Collection<Object> allMovies = Publisher.getInstance().getSortedMovies(Publisher.getInstance().getParamLanguage(), false);
            Font font4 = FontFactory.getFont(FontFactory.HELVETICA, 4);
            Font font5 = FontFactory.getFont(FontFactory.HELVETICA, 5);
            Font font6 = FontFactory.getFont(FontFactory.HELVETICA, 6);
            Font font7 = FontFactory.getFont(FontFactory.HELVETICA, 7);
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(Publisher.getInstance().getParamMediaCenterLocation() + UMCConstants.fileSeparator + "umc-scan-results.pdf"));
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();
            writer.setViewerPreferences(PdfWriter.PageModeUseOutlines | PdfWriter.FitWindow | PdfWriter.PageLayoutOneColumn);
            document.open();
            float[] columnDefinitionSize = { 20.0F, 45.0F, 35.0F };
            float pos = height / 2;
            PdfPTable table = null;
            PdfPCell cell = null;
            PdfOutline rootOutline = writer.getRootOutline();
            PdfOutline movieOutline = new PdfOutline(rootOutline, new PdfAction(), "Filme");
            PdfOutline groupOutline = new PdfOutline(rootOutline, new PdfAction(), "Gruppen");
            PdfOutline seriesOutline = new PdfOutline(rootOutline, new PdfAction(), "Serien");
            List<PdfPTable> movieList = new LinkedList<PdfPTable>();
            Map<String, List<PdfPTable>> movieGroupList = new LinkedHashMap<String, List<PdfPTable>>();
            List<PdfPTable> seriesList = new LinkedList<PdfPTable>();
            for (Object o : allMovies) {
                if (o instanceof MovieGroup) {
                    MovieGroup mg = (MovieGroup) o;
                    List<PdfPTable> mpl = new LinkedList<PdfPTable>();
                    PdfOutline grpOutline = new PdfOutline(groupOutline, new PdfAction(), mg.getComputedTitel());
                    for (Movie part : mg.getChilds()) {
                        mpl.add(createMovieTable(part, grpOutline, font7));
                    }
                    movieGroupList.put(mg.getComputedTitel(), mpl);
                } else if (o instanceof SeriesGroup) {
                    SeriesGroup sg = (SeriesGroup) o;
                    seriesList.add(createSeriesTable(sg, seriesOutline, font7));
                } else {
                    Movie m = (Movie) o;
                    movieList.add(createMovieTable(m, movieOutline, font7));
                }
            }
            for (PdfPTable t : movieList) {
                document.add(t);
            }
            for (String key : movieGroupList.keySet()) {
                document.newPage();
                for (PdfPTable t : movieGroupList.get(key)) {
                    document.add(t);
                }
            }
            for (PdfPTable s : seriesList) {
                document.newPage();
                document.add(s);
            }
            document.close();
            writer.close();
        } catch (DocumentException exc) {
            log.error("PDF Dokument Fehler", exc);
        } catch (IOException exc) {
            log.error("PDF IO Fehler", exc);
        } catch (Exception exc) {
            log.error(UMCLanguage.getText("builder.pdf.error1", null), exc);
            vres.setErrorMessage(UMCLanguage.getText("builder.pdf.error2", null) + " " + exc.getMessage());
        }
        return vres;
    }

    private static ViewResult buildScanResultHTML2() {
        log.info(UMCLanguage.getText("builder.scanResult.info1", null));
        final ViewResult vres = new ViewResult();
        try {
            String pluginMainLanguage = Publisher.getInstance().getPluginMainLanguage();
            Collection<Object> allMovies = Publisher.getInstance().getSortedMovies(pluginMainLanguage, false);
            String cssClass = "result1";
            StringBuffer plots = new StringBuffer();
            StringBuffer warnings = new StringBuffer();
            StringBuffer sbFM = new StringBuffer();
            sbFM.append("<table>\n");
            StringBuffer sbMG = new StringBuffer();
            sbMG.append("<table>\n");
            StringBuffer sbUG = new StringBuffer();
            sbUG.append("<table>\n");
            StringBuffer sbStatistic = new StringBuffer();
            sbStatistic.append("<table class=\"result1\">\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic1", null) + "</td><td align=\"right\">" + (UMCStatistics.getInstance().getRunningTime() / 1000) + " seconds</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic2", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getScanDirsScanned() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic4", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesFound() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic31", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesSearchedAndFoundByTitle() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic32", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesSearchedAndFoundByOnlineID() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic33", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesSearchedAndFoundByIMDB() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic28", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNotFoundByTitleAndID() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic5", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesInserted() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic14", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMovieInsertErrors() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic15", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMovieUpdateErrors() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic37", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMovieGroupsInserted() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic38", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getGroupInsertErrors() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic39", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getGroupUpdateErrors() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic6", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesLocked() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic7", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesWithoutErrors() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic10", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMovieProcessingErrors() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic11", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesRefused() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic12", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMovieProcessingXMLWarnings() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic13", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesRefusedByIgnorePattern() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic19", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNoCoverFoundLocal() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic20", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNoPosterFoundLocal() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic21", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNoBackdropFoundLocal() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic22", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNoCoverFoundOnline() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic23", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNoPosterFoundOnline() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic24", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMoviesNoBackdropFoundOnline() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic8", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getMGParts() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic9", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getUGParts() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic29", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getPlaylistsNotCreated() + "</td></tr>\n");
            sbStatistic.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic25", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getGroupsNoPosterFoundLocal() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic26", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getGroupsNoPosterFoundLocal() + "</td></tr>\n");
            sbStatistic.append("<tr><td>" + UMCLanguage.getText("umc.statistic27", null) + "</td><td align=\"right\">" + UMCStatistics.getInstance().getGroupsNoBackdropFoundLocal() + "</td></tr>\n");
            sbStatistic.append("</table>\n");
            String adjustedTitle = null;
            String ofdbLink = "";
            String imdbLink = "";
            int count = 0;
            int countFM = 1;
            int countMG = 1;
            int countUG = 1;
            String plot = null;
            StringBuffer sb = new StringBuffer();
            for (Object o : allMovies) {
                if (o instanceof Movie) {
                    Movie m = (Movie) o;
                    String title = m.getLanguageData().getTitle(pluginMainLanguage);
                    if (!m.isMultipartgroup()) {
                        plot = m.getLanguageData().getPlot(pluginMainLanguage);
                        for (Warning warning : m.getWarnings()) {
                            sb.append("\"" + warning.getAdditionalInfo() + "\",");
                        }
                        plots.append("plots[" + count + "] = new Array(\"false\",\"" + plot.replaceAll("[\\r\\n]+", " ").replaceAll("\"", "'") + "\");\n");
                        String warningImage = "green.png";
                        String value = "";
                        if (sb.toString().length() > 0) {
                            warningImage = "red.png";
                            value = sb.toString().substring(0, sb.toString().length() - 1);
                        }
                        warnings.append("warnings[" + count + "] = new Array(\"false\",new Array(" + value + "));\n");
                        if (m.getLanguageData().getID("OFDB") != null && !m.getLanguageData().getID("OFDB").equals("")) ofdbLink = "<a href=\"http://www.ofdb.de/film/" + m.getLanguageData().getID("OFDB") + ",\">OFDB</a>"; else ofdbLink = "<a href=\"http://www.ofdb.de/view.php?SText=" + title.replaceAll(" ", "+") + "&Kat=Titel&page=suchergebnis\"><font color=\"red\">no id</font></a>";
                        if (m.getLanguageData().getID("imdb") != null && !m.getLanguageData().getID("imdb").equals("")) imdbLink = "<a href=\"http://www.imdb.com/title/" + m.getLanguageData().getID("imdb") + "/\">IMDB</a>"; else imdbLink = "<a href=\"http://www.imdb.de/find?s=tt&q=" + title.replaceAll(" ", ".") + "\"><font color=\"red\">no id</font></a>";
                        String infos = "- Running time: " + m.getDurationFormatted() + "<br>- Size: " + m.getFilesize() + "<br>- Year: " + m.getYear() + "<br>- Codec: " + m.getCodec() + "<br>- Resolution: " + m.getWidth() + " x " + m.getHeight() + "<br>- Duration: " + m.getDurationFormatted();
                        sbFM.append("<tr class=\"headline\"><td colspan=\"4\">Movie (FM): " + title + "</td></tr>\n");
                        sbFM.append("<tr class=\"result2\"><td  colspan=\"2\"><a href=\"javascript:showPlot('" + count + "')\"><font id=\"sign" + count + "\">+</font></a>&nbsp|&nbsp;<a href=\"javascript:showWarnings('" + count + "')\"><img src=\"pics/" + warningImage + "\" width=\"10\" height=\"10\"></a>&nbsp;|&nbsp;" + m.getFilename() + m.getFiletype() + "</td><td>" + ofdbLink + "</td><td>" + imdbLink + "</td></tr>\n");
                        sbFM.append("<tr><td colspan=\"4\"><p id=\"dyn" + count + "\"></p></td></tr>");
                        sbFM.append("<tr><td><img src=\"pics/Cover/Movies/" + adjustedTitle + "[Default].png\" height=\"169\" border=\"0\" alt=\"no cover\"></td><td class=\"infos\">" + infos + "</td><td colspan=\"2\"><img src=\"pics/Backdrops/Movies/" + adjustedTitle + ".jpg\" width=\"300\" height=\"169\" border=\"0\" alt=\"no backdrop\"></td></tr>\n");
                        sbFM.append("<tr><td colspan=\"4\">&nbsp;</td></tr>\n");
                        countFM++;
                    } else {
                        sbMG.append("<tr class=\"headline\"><td colspan=\"4\">Group (MG): " + title + "</td></tr>\n");
                        for (Movie mp : m.getChilds()) {
                            if (m.getLanguageData().getID("OFDB") != null && !m.getLanguageData().getID("OFDB").equals("")) ofdbLink = "<a href=\"http://www.ofdb.de/film/" + m.getLanguageData().getID("OFDB") + ",\">OFDB</a>"; else ofdbLink = "<a href=\"http://www.ofdb.de/view.php?SText=" + title.replaceAll(" ", "+") + "&Kat=Titel&page=suchergebnis\"><font color=\"red\">no id</font></a>";
                            if (m.getLanguageData().getID("imdb") != null && !m.getLanguageData().getID("imdb").equals("")) imdbLink = "<a href=\"http://www.imdb.com/title/" + m.getLanguageData().getID("imdb") + "/\">IMDB</a>"; else imdbLink = "<a href=\"http://www.imdb.de/find?s=tt&q=" + title.replaceAll(" ", ".") + "\"><font color=\"red\">no id</font></a>";
                            sbMG.append("<tr class=\"" + cssClass + "\"><td>" + mp.getFilename() + mp.getFiletype() + "</td><td>" + mp.getLanguageData().getTitle(pluginMainLanguage) + "</td><td>" + ofdbLink + "</td><td>" + imdbLink + "</td></tr>\n");
                            if (cssClass.equals("result1")) cssClass = "result2"; else cssClass = "result1";
                        }
                        sbMG.append("<tr><td><img src=\"pics/Cover/Movies/" + adjustedTitle + "[Default].png\" height=\"169\" border=\"0\" alt=\"no cover\"></td><td  colspan=\"3\"><img src=\"pics/Backdrops/Movies/" + adjustedTitle + ".jpg\" width=\"300\" height=\"169\" border=\"0\" alt=\"no backdrop\"></td></tr>\n");
                        sbMG.append("<tr><td colspan=\"3\">&nbsp;</td></tr>\n");
                    }
                } else if (o instanceof MovieGroup) {
                    MovieGroup mg = (MovieGroup) o;
                    sbUG.append("<tr class=\"headline\"><td colspan=\"4\">Group (UG): " + mg.getComputedTitel() + "</td></tr>\n");
                    String childTitle = null;
                    for (Movie mp : mg.getChilds()) {
                        childTitle = mp.getLanguageData().getTitle(pluginMainLanguage);
                        if (mp.getLanguageData().getID("OFDB") != null && !mp.getLanguageData().getID("OFDB").equals("")) ofdbLink = "<a href=\"http://www.ofdb.de/film/" + mp.getLanguageData().getID("OFDB") + ",\">OFDB</a>"; else ofdbLink = "<a href=\"http://www.ofdb.de/view.php?SText=" + childTitle.replaceAll(" ", "+") + "&Kat=Titel&page=suchergebnis\"><font color=\"red\">no id</font></a>";
                        if (mp.getLanguageData().getID("imdb") != null && !mp.getLanguageData().getID("imdb").equals("")) imdbLink = "<a href=\"http://www.imdb.com/title/" + mp.getLanguageData().getID("imdb") + "/\">IMDB</a>"; else imdbLink = "<a href=\"http://www.imdb.de/find?s=tt&q=" + childTitle.replaceAll(" ", ".") + "\"><font color=\"red\">no id</font></a>";
                        if (mp.isMultipartgroup()) {
                            sbUG.append("<tr class=\"headline\"><td colspan=\"2\">Group (MG): " + childTitle + "</td><td>" + ofdbLink + "</td><td>" + imdbLink + "</td></tr>\n");
                            for (Movie part : mp.getChilds()) {
                                sbUG.append("<tr class=\"" + cssClass + "\"><td>" + part.getFilename() + part.getFiletype() + "</td><td colspan=\"2\">" + childTitle + "</td></tr>\n");
                                if (cssClass.equals("result1")) cssClass = "result2"; else cssClass = "result1";
                            }
                            sbUG.append("<tr><td><img src=\"pics/Cover/Movies/" + adjustedTitle + "[Default].png\" height=\"169\" border=\"0\" alt=\"no cover\"></td><td>&nbsp;</td><td  colspan=\"2\"><img src=\"pics/Backdrops/Movies/" + adjustedTitle + ".jpg\" width=\"300\" height=\"169\" border=\"0\" alt=\"no backdrop\"></td></tr>\n");
                            sbUG.append("<tr><td colspan=\"3\">&nbsp;</td></tr>\n");
                        } else {
                            sbUG.append("<tr class=\"" + cssClass + "\"><td>" + mp.getFilename() + mp.getFiletype() + "</td><td>" + childTitle + "</td><td>" + ofdbLink + "</td><td>" + imdbLink + "</td></tr>\n");
                            sbUG.append("<tr><td><img src=\"pics/Cover/Movies/" + adjustedTitle + "[Default].png\" height=\"169\" border=\"0\" alt=\"no cover\"></td><td>&nbsp;</td><td colspan=\"2\"><img src=\"pics/Backdrops/Movies/" + adjustedTitle + ".jpg\" width=\"300\" height=\"169\" border=\"0\" alt=\"no backdrop\"></td></tr>\n");
                        }
                        if (cssClass.equals("result1")) cssClass = "result2"; else cssClass = "result1";
                    }
                    sbUG.append("<tr><td colspan=\"4\">&nbsp;</td></tr>\n");
                }
                count++;
            }
            sbFM.append("</table>\n");
            sbMG.append("</table>\n");
            sbUG.append("</table>\n");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(System.getProperty("user.dir") + File.separator + "resources" + File.separator + "HTML" + File.separator + "umc-scan-result.htm"), "UTF-8"));
            String line = "";
            StringBuffer htmlCode = new StringBuffer();
            while ((line = br.readLine()) != null) {
                htmlCode.append(line + "\n");
            }
            br.close();
            String result = htmlCode.toString();
            String s = plots.toString().replaceAll("\\$", "\\\\\\$");
            result = result.replaceAll("#PLOTS#", s);
            s = warnings.toString().replaceAll("\\$", "\\\\\\$");
            result = result.replaceAll("#WARNINGS#", s);
            if (sbFM.toString() != null) result = result.replaceAll("#FM#", sbFM.toString());
            if (sbMG.toString() != null) result = result.replaceAll("#MG#", sbMG.toString());
            if (sbUG.toString() != null) result = result.replaceAll("#UG#", sbUG.toString());
            if (sbStatistic.toString() != null) result = result.replaceAll("#STATISTIC#", sbStatistic.toString());
            log.info(UMCLanguage.getText("builder.scanResult.info2", null));
            OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(Publisher.getInstance().getParamMediaCenterLocation() + "/umc-scan-results.html"), "UTF-8");
            ow.write(result.toString());
            ow.close();
        } catch (Throwable exc) {
            log.error(UMCLanguage.getText("builder.scanResult.error1", null), exc);
            vres.setErrorMessage(UMCLanguage.getText("builder.scanResult.error2", null) + " " + exc.getMessage());
        }
        return vres;
    }

    /**
	 * Diese Methode stösst das Bauen des HTML-Interface für den PCH an.
	 * 
	 * @param dir Das Verzeichnis in dem das HTML-Interface gespeichert werden soll
	 * @return
	 */
    private static boolean buildPHPSkin(String skinName, String skinPath) {
        log.info(UMCLanguage.getText("builder.PHP.info1", null));
        if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelTop().setActualStep("Adding PHP skin '" + skinName + "' to the frontend...");
        final String skinDir = skinPath;
        final String phpDir = skinDir + UMCConstants.fileSeparator + "php";
        final String dir = Publisher.getInstance().getParamMediaCenterLocation() + UMCConstants.fileSeparator + skinName;
        File sd = new File(dir);
        if (!sd.exists()) sd.mkdir();
        try {
            log.info(UMCLanguage.getText("builder.PHP.info2", null));
            String share = Publisher.getInstance().getParamFrontendShare() + "/" + skinName;
            if (share == null || share.equals("")) share = "HARD_DISK/UMC/" + skinName;
            File inF = null;
            File outF = null;
            File f = null;
            BufferedReader br = null;
            String line = null;
            OutputStreamWriter ow = null;
            f = new File(phpDir + UMCConstants.fileSeparator);
            File[] phpFiles = f.listFiles(FileNameFilterFactory.getInstance().getFileNameFilter(FileNameFilterFactory.FILE_TYPE_PHP));
            for (File file : phpFiles) {
                outF = new File(dir + UMCConstants.fileSeparator + file.getName());
                copyFile(file, outF);
            }
            if (!Publisher.getInstance().getParamFrontendApache()) {
                f = new File(dir + "/index.html");
                br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "index.html"), "UTF-8"));
                line = "";
                StringBuffer htmlCode = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    htmlCode.append(line + "\n");
                }
                br.close();
                String html = htmlCode.toString();
                html = html.replaceAll("#SHARE#", share);
                if (Publisher.getInstance().getParamFrontendArea() != null && !Publisher.getInstance().getParamFrontendArea().equals("")) {
                    String destination = Publisher.getInstance().getParamFrontendArea();
                    if (Publisher.getInstance().getParamFrontendArea().equals("movies")) destination = "movieindex1";
                    html = html.replaceAll("#DESTINATION#", destination);
                } else {
                    html = html.replaceAll("#DESTINATION#", "home");
                }
                ow = new OutputStreamWriter(new FileOutputStream(Publisher.getInstance().getParamMediaCenterLocation() + File.separator + "index_" + skinName.toLowerCase() + ".html"), "UTF-8");
                ow.write(html);
                ow.close();
            }
            String s = "umc.cgi";
            br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "umc.cgi"), "UTF-8"));
            line = "";
            StringBuffer htmlCode = new StringBuffer();
            while ((line = br.readLine()) != null) {
                htmlCode.append(line + "\n");
            }
            br.close();
            String html = htmlCode.toString();
            if (Publisher.getInstance().getParamFrontendApache()) {
                html = html.replaceAll("#PHPINTERPRETOR#", "");
                s = "index.php";
            } else {
                html = html.replaceAll("#PHPINTERPRETOR#", Publisher.getInstance().getParamPHPInterpretor() + "\n");
            }
            ow = new OutputStreamWriter(new FileOutputStream(dir + File.separator + s), "UTF-8");
            ow.write(html);
            ow.close();
            File cgi = new File(phpDir + UMCConstants.fileSeparator + "playlist.cgi");
            if (cgi.exists()) {
                s = "playlist.cgi";
                br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "playlist.cgi"), "UTF-8"));
                line = "";
                htmlCode = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    htmlCode.append(line + "\n");
                }
                br.close();
                html = htmlCode.toString();
                if (Publisher.getInstance().getParamFrontendApache()) {
                    html = html.replaceAll("#PHPINTERPRETOR#", "");
                    s = "playlist.php";
                } else {
                    html = html.replaceAll("#PHPINTERPRETOR#", Publisher.getInstance().getParamPHPInterpretor() + "\n");
                }
                ow = new OutputStreamWriter(new FileOutputStream(dir + File.separator + s), "UTF-8");
                ow.write(html);
                ow.close();
            }
            cgi = new File(phpDir + UMCConstants.fileSeparator + "play.cgi");
            if (cgi.exists()) {
                s = "play.cgi";
                br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "play.cgi"), "UTF-8"));
                line = "";
                htmlCode = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    htmlCode.append(line + "\n");
                }
                br.close();
                html = htmlCode.toString();
                if (Publisher.getInstance().getParamFrontendApache()) {
                    html = html.replaceAll("#PHPINTERPRETOR#", "");
                    s = "play.php";
                } else {
                    html = html.replaceAll("#PHPINTERPRETOR#", Publisher.getInstance().getParamPHPInterpretor() + "\n");
                }
                ow = new OutputStreamWriter(new FileOutputStream(dir + File.separator + s), "UTF-8");
                ow.write(html);
                ow.close();
            }
            f = new File(dir + "/home.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "home.php");
                outF = new File(dir + "/home.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/settings.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "settings.php");
                outF = new File(dir + "/settings.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/movieindex1.php");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "movieindex1.php"), "UTF-8"));
            line = "";
            StringBuffer phpCode = new StringBuffer();
            while ((line = br.readLine()) != null) {
                phpCode.append(line + "\n");
            }
            br.close();
            String php = phpCode.toString();
            StringBuffer result = new StringBuffer();
            if (Publisher.getInstance().getParamQuickFilter().equals("genre")) {
                Collection<String> allGenre = dao.getAssignedGenres(Publisher.getInstance().getParamLanguage());
                result.append("var quickfilter = new Array(" + allGenre.size() + ");\n");
                int i = 0;
                for (String genre : allGenre) {
                    int genreID = dao.getGenreID(Publisher.getInstance().getParamLanguage(), genre);
                    result.append("quickfilter[" + i + "]={\"" + genre + "\",\"" + genreID + "\"};\n");
                    i++;
                }
            } else if (Publisher.getInstance().getParamQuickFilter().equals("resolution")) {
                Collection<int[]> allResolutions = dao.getResolutions();
                result.append("var quickfilter = new Array(" + allResolutions.size() + ");\n");
                int i = 0;
                for (int[] resolution : allResolutions) {
                    result.append("quickfilter[" + i + "]={\"" + (resolution[0] + "x" + resolution[1]) + "\",\"" + ("width=" + resolution[0] + " AND height=" + resolution[1]) + "\"};\n");
                    i++;
                }
            } else if (Publisher.getInstance().getParamQuickFilter().equals("year")) {
                Collection<String> allYears = dao.getYears();
                result.append("var quickfilter = new Array(" + allYears.size() + ");\n");
                int i = 0;
                for (String year : allYears) {
                    result.append("quickfilter[" + i + "]={\"" + year + "\",\"" + year + "\"};\n");
                    i++;
                }
            }
            php = php.replaceAll("#QUICKFILTER#", result.toString());
            if (Publisher.getInstance().getParamFrontendBackdrops()) php = php.replaceAll("#DETAILS#", ""); else php = php.replaceAll("#DETAILS#", "&details=true&tab=1&data=");
            ow = new OutputStreamWriter(new FileOutputStream(dir + UMCConstants.fileSeparator + "movieindex1.php"), "UTF-8");
            ow.write(php);
            ow.close();
            f = new File(dir + "/movieindex2.php");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "movieindex2.php"), "UTF-8"));
            line = "";
            phpCode = new StringBuffer();
            while ((line = br.readLine()) != null) {
                phpCode.append(line + "\n");
            }
            br.close();
            php = phpCode.toString();
            result = new StringBuffer();
            if (Publisher.getInstance().getParamQuickFilter().equals("genre")) {
                Collection<String> allGenre = dao.getAssignedGenres(Publisher.getInstance().getParamLanguage());
                result.append("var quickfilter = new Array(" + allGenre.size() + ");\n");
                int i = 0;
                for (String genre : allGenre) {
                    int genreID = dao.getGenreID(Publisher.getInstance().getParamLanguage(), genre);
                    result.append("quickfilter[" + i + "]={\"" + genre + "\",\"" + genreID + "\"};\n");
                    i++;
                }
            } else if (Publisher.getInstance().getParamQuickFilter().equals("resolution")) {
                Collection<int[]> allResolutions = dao.getResolutions();
                result.append("var quickfilter = new Array(" + allResolutions.size() + ");\n");
                int i = 0;
                for (int[] resolution : allResolutions) {
                    result.append("quickfilter[" + i + "]={\"" + (resolution[0] + "x" + resolution[1]) + "\",\"" + ("width=" + resolution[0] + " AND height=" + resolution[1]) + "\"};\n");
                    i++;
                }
            } else if (Publisher.getInstance().getParamQuickFilter().equals("year")) {
                Collection<String> allYears = dao.getYears();
                result.append("var quickfilter = new Array(" + allYears.size() + ");\n");
                int i = 0;
                for (String year : allYears) {
                    result.append("quickfilter[" + i + "]={\"" + year + "\",\"" + year + "\"};\n");
                    i++;
                }
            }
            php = php.replaceAll("#QUICKFILTER#", result.toString());
            if (Publisher.getInstance().getParamFrontendBackdrops()) php = php.replaceAll("#DETAILS#", ""); else php = php.replaceAll("#DETAILS#", "&details=true&tab=1&data=");
            ow = new OutputStreamWriter(new FileOutputStream(dir + UMCConstants.fileSeparator + "movieindex2.php"), "UTF-8");
            ow.write(php);
            ow.close();
            f = new File(dir + "/movieindex3.php");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "movieindex3.php"), "UTF-8"));
            line = "";
            phpCode = new StringBuffer();
            while ((line = br.readLine()) != null) {
                phpCode.append(line + "\n");
            }
            br.close();
            php = phpCode.toString();
            result = new StringBuffer();
            if (Publisher.getInstance().getParamQuickFilter().equals("genre")) {
                Collection<String> allGenre = dao.getAssignedGenres(Publisher.getInstance().getParamLanguage());
                result.append("var quickfilter = new Array(" + allGenre.size() + ");\n");
                int i = 0;
                for (String genre : allGenre) {
                    int genreID = dao.getGenreID(Publisher.getInstance().getParamLanguage(), genre);
                    result.append("quickfilter[" + i + "]={\"" + genre + "\",\"" + genreID + "\"};\n");
                    i++;
                }
            } else if (Publisher.getInstance().getParamQuickFilter().equals("resolution")) {
                Collection<int[]> allResolutions = dao.getResolutions();
                result.append("var quickfilter = new Array(" + allResolutions.size() + ");\n");
                int i = 0;
                for (int[] resolution : allResolutions) {
                    result.append("quickfilter[" + i + "]={\"" + (resolution[0] + "x" + resolution[1]) + "\",\"" + ("width=" + resolution[0] + " AND height=" + resolution[1]) + "\"};\n");
                    i++;
                }
            } else if (Publisher.getInstance().getParamQuickFilter().equals("year")) {
                Collection<String> allYears = dao.getYears();
                result.append("var quickfilter = new Array(" + allYears.size() + ");\n");
                int i = 0;
                for (String year : allYears) {
                    result.append("quickfilter[" + i + "]={\"" + year + "\",\"" + year + "\"};\n");
                    i++;
                }
            }
            php = php.replaceAll("#QUICKFILTER#", result.toString());
            if (Publisher.getInstance().getParamFrontendBackdrops()) php = php.replaceAll("#DETAILS#", ""); else php = php.replaceAll("#DETAILS#", "&details=true&tab=1&data=");
            ow = new OutputStreamWriter(new FileOutputStream(dir + UMCConstants.fileSeparator + "movieindex3.php"), "UTF-8");
            ow.write(php);
            ow.close();
            f = new File(dir + "/seriesindex.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "seriesindex.php");
                outF = new File(dir + UMCConstants.fileSeparator + "seriesindex.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/episodeindex.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "episodeindex.php");
                outF = new File(dir + UMCConstants.fileSeparator + "episodeindex.php");
                copyFile(inF, outF);
            }
            String groupPHP = "groupindex1.php";
            if (Publisher.getInstance().getParamFrontendGroupStyle().equals("coverflow")) groupPHP = "groupindex2.php";
            f = new File(dir + "/groupindex.php");
            inF = new File(phpDir + UMCConstants.fileSeparator + groupPHP);
            outF = new File(dir + UMCConstants.fileSeparator + "groupindex.php");
            copyFile(inF, outF);
            f = new File(dir + "/moviedetails.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "moviedetails.php");
                outF = new File(dir + UMCConstants.fileSeparator + "moviedetails.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/filter.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "filter.php");
                outF = new File(dir + UMCConstants.fileSeparator + "filter.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/tv.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "tv.php");
                outF = new File(dir + UMCConstants.fileSeparator + "tv.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/functions.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "functions.php");
                outF = new File(dir + UMCConstants.fileSeparator + "functions.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/config.php");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(phpDir + UMCConstants.fileSeparator + "config.php"), "UTF-8"));
            line = "";
            phpCode = new StringBuffer();
            while ((line = br.readLine()) != null) {
                phpCode.append(line + "\n");
            }
            br.close();
            php = phpCode.toString();
            php = php.replaceAll("#LANGUAGE#", Publisher.getInstance().getParamLanguage().toUpperCase());
            if (!Publisher.getInstance().getParamFrontendApache()) {
                php = php.replaceAll("#UMCPATH#", "http://localhost.drives:8883/" + share + "/umc.cgi");
                php = php.replaceAll("#PLAYLISTPATH#", "http://localhost.drives:8883/" + share + "/playlist.cgi");
                php = php.replaceAll("#PLAYPATH#", "http://localhost.drives:8883/" + share + "/play.cgi");
            } else {
                php = php.replaceAll("#UMCPATH#", "index.php");
                php = php.replaceAll("#PLAYLISTPATH#", "playlist.php");
                php = php.replaceAll("#PLAYPATH#", "play.php");
            }
            ow = new OutputStreamWriter(new FileOutputStream(dir + UMCConstants.fileSeparator + "config.php"), "UTF-8");
            ow.write(php);
            ow.close();
            f = new File(dir + "/ums/ums.php");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "ums" + UMCConstants.fileSeparator + "ums.php");
                outF = new File(dir + UMCConstants.fileSeparator + "ums" + UMCConstants.fileSeparator + "ums.php");
                copyFile(inF, outF);
            }
            f = new File(dir + "/ums/ums.css");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "ums" + UMCConstants.fileSeparator + "ums.css");
                outF = new File(dir + UMCConstants.fileSeparator + "ums" + UMCConstants.fileSeparator + "ums.css");
                copyFile(inF, outF);
            }
            f = new File(dir + "/script/functions.js");
            if (!f.exists()) {
                inF = new File(phpDir + UMCConstants.fileSeparator + "script" + UMCConstants.fileSeparator + "functions.js");
                outF = new File(dir + UMCConstants.fileSeparator + "script" + UMCConstants.fileSeparator + "functions.js");
                copyFile(inF, outF);
            }
            String resolution = Publisher.getInstance().getParamFrontendResolution();
            if (resolution == null || resolution.equals("")) resolution = "HD";
            log.info(UMCLanguage.getText("builder.PHP.info3", dir, null));
            f = new File(dir + "/pics");
            if (!f.exists()) f.mkdir();
            f = new File(phpDir + UMCConstants.fileSeparator + "pics");
            if (f.exists()) {
                File[] allMoviedetails = f.listFiles();
                for (File file : allMoviedetails) {
                    if (file.isFile() && !new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + file.getName()).exists()) {
                        inF = new File(file.getAbsolutePath());
                        outF = new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + file.getName());
                        copyFile(inF, outF);
                    }
                }
            }
            f = new File(phpDir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + resolution);
            if (f.exists()) {
                File[] allMoviedetails = f.listFiles();
                for (File file : allMoviedetails) {
                    if (file.isFile() && !new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + file.getName()).exists()) {
                        inF = new File(file.getAbsolutePath());
                        outF = new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + file.getName());
                        copyFile(inF, outF);
                    }
                }
            }
            log.info(UMCLanguage.getText("builder.PHP.info5", dir, null));
            f = new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + "Keyboard");
            if (!f.exists()) f.mkdir();
            f = new File(phpDir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + resolution + UMCConstants.fileSeparator + "Keyboard");
            if (f.exists()) {
                File[] allMoviedetails = f.listFiles();
                for (File file : allMoviedetails) {
                    if (file.isFile() && !new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + "Keyboard" + UMCConstants.fileSeparator + file.getName()).exists()) {
                        inF = new File(file.getAbsolutePath());
                        outF = new File(dir + UMCConstants.fileSeparator + "pics" + UMCConstants.fileSeparator + "Keyboard" + UMCConstants.fileSeparator + file.getName());
                        copyFile(inF, outF);
                    }
                }
            }
        } catch (Throwable exc) {
            log.error(UMCLanguage.getText("builder.PHP.error1", null), exc);
            return false;
        }
        return true;
    }

    /**
	 * Diese Methode stösst das Bauen eines Flash-Skins für den PCH an.
	 * 
	 * @param dir Das Verzeichnis in dem das Flash-Interface gespeichert werden soll
	 * @return
	 */
    private static boolean buildFlashSkin(String skinName, String skinPath) {
        log.info("############################################## Generating frontend ##############################################");
        if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelTop().setActualStep("Adding RobG skin '" + skinName + "' to the frontend...");
        final String skinDir = skinPath + UMCConstants.fileSeparator + "frontend";
        final String destinationDir = Publisher.getInstance().getParamMediaCenterLocation();
        File f = new File(destinationDir);
        if (!f.exists()) f.mkdir();
        try {
            log.info("copying needed files to the frontend folder");
            File outF = null;
            f = null;
            BufferedReader br = null;
            String line = null;
            OutputStreamWriter ow = null;
            Collection<File> allSkinFiles = FilescannerController.getAllFilesForFrontend(skinDir);
            for (File inF : allSkinFiles) {
                String s = inF.getAbsolutePath().replaceAll("\\\\", "/").replaceAll(skinDir.replaceAll("\\\\", "/"), "");
                outF = new File(destinationDir + UMCConstants.fileSeparator + "skins" + UMCConstants.fileSeparator + skinName + UMCConstants.fileSeparator + s);
                if (!outF.getParentFile().exists()) outF.getParentFile().mkdir();
                copyFile(inF, outF);
                if (skinName.equalsIgnoreCase("ReVamp 2.0") && outF.getName().equals("skin.phf")) {
                    outF = new File(destinationDir + UMCConstants.fileSeparator + "umc.phf");
                    copyFile(inF, outF);
                }
            }
        } catch (Throwable exc) {
            log.error("Could not create RobG frontend", exc);
            return false;
        }
        return true;
    }

    /**
	 * Diese Methode stösst das Bauen aller selektierten Frontends/Skins für den NMT an.
	 *
	 * @return
	 */
    private static ViewResult buildInterfaces() {
        final ViewResult vres = new ViewResult();
        boolean frontendCreated = false;
        for (String skinPath : Publisher.getInstance().getParamFrontendSkinPath()) {
            File skinXML = new File(skinPath + UMCConstants.fileSeparator + "skin.xml");
            if (skinXML.exists()) {
                try {
                    SkinDocument sd = SkinDocument.Factory.parse(skinXML);
                    String type = sd.getSkin().getType();
                    if (StringUtils.isNotEmpty(type)) {
                        frontendCreated = false;
                        if (type.toUpperCase().equals("PHF")) frontendCreated = buildFlashSkin(sd.getSkin().getName(), skinPath); else if (type.toUpperCase().equals("PHP")) frontendCreated = buildPHPSkin(sd.getSkin().getName(), skinPath); else log.warn("Could not create frontend because type (PHF or PHP) is not specified in skin.xml");
                        if (frontendCreated) if (UMCConstants.debug) log.debug("Frontend has been created"); else log.error("Frontend could not be created");
                    }
                } catch (Exception exc) {
                    log.error("skin.xml could not be parsed", exc);
                    vres.setErrorMessage("skin.xml could not be parsed: " + exc.getMessage());
                }
            }
        }
        try {
            log.info("copying database to directory " + Publisher.getInstance().getParamMediaCenterLocation());
            File f = new File(Publisher.getInstance().getParamMediaCenterLocation() + UMCConstants.fileSeparator + "umc.db");
            if (f.exists()) f.delete();
            File inF = new File(System.getProperty("user.dir") + UMCConstants.fileSeparator + "database" + UMCConstants.fileSeparator + "umc.db");
            File outF = new File(Publisher.getInstance().getParamMediaCenterLocation() + UMCConstants.fileSeparator + "database" + UMCConstants.fileSeparator + "umc.db");
            copyFile(inF, outF);
        } catch (IOException exc) {
            log.error("Database could not be copied to the frontend directory", exc);
        }
        return vres;
    }

    private static void copyFile(File src, File dest, int bufSize, boolean force) throws IOException {
        if (dest.exists()) {
            if (force) {
                dest.delete();
            } else {
                throw new IOException("Cannot overwrite existing file: " + dest.getName());
            }
        }
        byte[] buffer = new byte[bufSize];
        int read = 0;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            while (true) {
                read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        }
    }

    private static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: destination file is unwriteable: " + toFileName);
            System.out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String response = in.readLine();
            if (!response.equals("Y") && !response.equals("y")) throw new IOException("FileCopy: existing file was not overwritten.");
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
