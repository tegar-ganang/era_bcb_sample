package net.sourceforge.cridmanager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import net.sourceforge.cridmanager.box.BoxManager;
import net.sourceforge.cridmanager.error.CridmanagerException;
import net.sourceforge.cridmanager.services.ServiceProvider;
import org.apache.log4j.Logger;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Diese Klasse speichert die EPG-Info in eine Datei.
 */
public class SaveEPGInfo {

    private static Logger logger = Logger.getLogger("net.sourceforge.cridmanager.SaveEPGInfo");

    private ILocation path;

    private IFile destination;

    private CridController controller;

    private CridInfo info;

    protected String errorTxt;

    private String fileName;

    private BoxManager boxManager;

    private ISettings settings;

    private static final int LEFT = 0;

    private static final int CENTER = 1;

    private static final int RIGHT = 2;

    private static final int JUSTIFIED = 3;

    private static final boolean NO_UNDERLINE = false;

    private static final boolean UNDERLINE = true;

    /**
	 * Erstellt einen Klasse, die die EPG-Infos einer Aufnahme in eine Datei abspeichert. Als
	 * Dateitypen sind TXT, JPG m�glich. Die abzuspeichernde Dateien werden aus <code>Settings</code>
	 * gelesen. *
	 * 
	 * @param info Die Aufnahme
	 * @param controller Der �berwachende Controller
	 * @param destFile Die Ziel-Datei
	 */
    public SaveEPGInfo(CridInfo infoP, CridController controllerP, IFile destFile) {
        this();
        this.destination = destFile;
        this.controller = controllerP;
        this.info = infoP;
        path = destination.getParent();
        fileName = Utils.legalFilename(destination.getName().substring(0, destination.getName().lastIndexOf(".")));
    }

    /**
	 * alternativer Konstruktor
	 * 
	 * @param info Die Aufnahme
	 * @param controller Der �berwachende Controller
	 * @param dest Der Ziel-Pfad
	 */
    public SaveEPGInfo(CridInfo infoP, CridController controllerP, ILocation dest) {
        this();
        this.destination = dest.getFile(infoP.getCridTitel() + ".ts");
        this.controller = controllerP;
        this.info = infoP;
        path = dest;
        fileName = Utils.legalFilename(infoP.getCridTitel());
    }

    /**
	 * alternativer Konstruktor
	 */
    public SaveEPGInfo() {
        settings = ServiceProvider.instance().getSettings();
        this.destination = null;
        this.controller = null;
        this.info = null;
        path = null;
        fileName = "";
    }

    /**
	 * initialisiert die Member der Klasse
	 */
    public void init(CridInfo infoP, CridController controllerP, ILocation dest) {
        if (logger.isDebugEnabled()) {
            logger.debug("init(CridInfo, CridController, ILocation) - start");
        }
        this.destination = dest.getFile(infoP.getCridTitel() + ".ts");
        this.controller = controllerP;
        this.info = infoP;
        path = dest;
        fileName = Utils.legalFilename(infoP.getCridTitel());
        if (logger.isDebugEnabled()) {
            logger.debug("init(CridInfo, CridController, ILocation) - end");
        }
    }

    /**
	 * initialisiert die Member der Klasse
	 */
    public void init(CridInfo infoP, CridController controllerP, IFile destFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("init(CridInfo, CridController, IFile) - start");
        }
        this.destination = destFile;
        this.controller = controllerP;
        this.info = infoP;
        path = destination.getParent();
        fileName = Utils.legalFilename(destination.getName().substring(0, destination.getName().lastIndexOf(".")));
        if (logger.isDebugEnabled()) {
            logger.debug("init(CridInfo, CridController, IFile) - end");
        }
    }

    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("run() - start");
        }
        String lineFeed = System.getProperty("line.separator");
        try {
            if (fileName.length() > 0) {
                if (settings.isSet(ISettings.SAVE_EPGINFO_TXT)) exportAsText();
                if (settings.isSet(ISettings.SAVE_EPGINFO_CSV)) exportAsCSV();
                if (settings.isSet(ISettings.SAVE_EPGINFO_CSV_DB)) exportAsCSV_DB();
                if (settings.isSet(ISettings.SAVE_EPGINFO_JPG)) exportAsJPG();
            }
        } catch (CridmanagerException e) {
            logger.error(e.getMessage(), e);
            controller.showErrorDialog(Messages.getString("SaveEPGInfo.SaveError") + lineFeed + errorTxt);
        } catch (Exception e) {
            logger.fatal(Messages.getString("SaveEPGInfo.SaveFatal"), e);
            controller.showErrorDialog(Messages.getString("SaveEPGInfo.SaveFatal") + lineFeed + errorTxt);
        } finally {
        }
        if (logger.isDebugEnabled()) {
            logger.debug("run() - end");
        }
    }

    /**
	 * liefert die Info, ob mind. ein Ausgabeformat gesetzt ist
	 */
    public boolean actionPossible() {
        if (logger.isDebugEnabled()) {
            logger.debug("actionPossible() - start");
        }
        boolean ret = false;
        if (settings.isSet(ISettings.SAVE_EPGINFO_TXT) || settings.isSet(ISettings.SAVE_EPGINFO_CSV) || settings.isSet(ISettings.SAVE_EPGINFO_JPG)) {
            ret = true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("actionPossible() - end");
        }
        return ret;
    }

    /**
	 * l�scht angelegte Files wieder, falls ein Problem auftrat
	 */
    public boolean delete() {
        if (logger.isDebugEnabled()) {
            logger.debug("delete() - start");
        }
        boolean ret = false;
        try {
            if (fileName.length() > 0) {
                if (settings.isSet(ISettings.SAVE_EPGINFO_TXT)) {
                    IFile destinationTXT = path.getFile(fileName + ".txt");
                    if (destinationTXT.exists()) {
                        destinationTXT.delete();
                        ret = true;
                    }
                }
                if (settings.isSet(ISettings.SAVE_EPGINFO_CSV)) {
                    IFile destinationCSV = path.getFile(fileName + ".csv");
                    if (destinationCSV.exists()) {
                        destinationCSV.delete();
                        ret = true;
                    }
                }
                if (settings.isSet(ISettings.SAVE_EPGINFO_CSV_DB)) {
                }
                if (settings.isSet(ISettings.SAVE_EPGINFO_JPG)) {
                    IFile destinationJPG;
                    boolean fileDeleted;
                    int counter = 0;
                    do {
                        fileDeleted = false;
                        if (counter == 0) destinationJPG = path.getFile(fileName + ".jpg"); else destinationJPG = path.getFile(fileName + "_" + String.valueOf(counter) + ".jpg");
                        if (destinationJPG.exists()) {
                            destinationJPG.delete();
                            fileDeleted = true;
                            ret = true;
                        }
                        counter++;
                    } while (fileDeleted);
                }
            }
        } catch (Exception e) {
            String message = Messages.getString("SaveEPGInfo.DeleteFatal");
            logger.fatal(message, e);
            controller.showErrorDialog(message);
        } finally {
        }
        if (logger.isDebugEnabled()) {
            logger.debug("delete() - end");
        }
        return ret;
    }

    /**
	 * gibt die EPG-Informationen in ein Text-File aus
	 */
    private boolean exportAsText() throws CridmanagerException {
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsText() - start");
        }
        OutputStream out = null;
        String EPGtext = "";
        String channelName = "";
        String lineFeed = System.getProperty("line.separator");
        IFile destinationTXT = null;
        try {
            destinationTXT = path.create(fileName + ".txt");
            out = new BufferedOutputStream(destinationTXT.createOutput());
            if (settings.isSet(ISettings.SAVE_EPGINFO_TITLE)) {
                EPGtext += info.getCridTitel();
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_YEAR)) {
                if (EPGtext.length() != 0) EPGtext += lineFeed + lineFeed;
                EPGtext += info.getCridFilmTypeYear();
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_DESCRIPTION)) {
                if (EPGtext.length() != 0) EPGtext += lineFeed + lineFeed;
                EPGtext += info.getCridDescription();
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_BROADCASTSTATION)) {
                if (EPGtext.length() != 0) EPGtext += lineFeed + lineFeed;
                channelName = getChannelName();
                EPGtext += channelName;
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_DATE)) {
                if (settings.isSet(ISettings.SAVE_EPGINFO_BROADCASTSTATION)) EPGtext += ", "; else {
                    EPGtext += lineFeed + lineFeed;
                }
                EPGtext += date2shortString();
            }
            out.write(EPGtext.getBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("exportAsText()", e);
            if (e instanceof FileNotFoundException) {
                errorTxt = Messages.getString("SaveEPGInfo.CantCreateFile");
            } else {
                errorTxt = e.getMessage();
            }
            String dest = destination == null ? Messages.getString("Global.Unbekannt") : destination.getAbsolutePath();
            String crid = info == null ? Messages.getString("Global.Unbekannt") : info.getFileName();
            throw new CridmanagerException(new StringBuffer(Messages.getString("SaveEPGInfo.SaveErrorMessage") + ", dest= " + dest + ", crid= " + crid + "). "), e);
        } finally {
            if (out != null) destinationTXT.close();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsText() - end");
        }
        return true;
    }

    private BoxManager getBoxManager() {
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxManager() - start");
        }
        if (boxManager == null) {
            boxManager = (BoxManager) ServiceProvider.instance().getService(this.getClass(), BoxManager.class);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxManager() - end");
        }
        return boxManager;
    }

    /**
	 * gibt die EPG-Informationen in ein CSV-File aus
	 */
    private boolean exportAsCSV() throws CridmanagerException {
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsCSV() - start");
        }
        OutputStream out = null;
        String wholeText = "";
        ;
        String quoteSign = "\"";
        String channelName = "";
        IFile destinationCSV = null;
        try {
            destinationCSV = path.create(fileName + ".csv");
            out = new BufferedOutputStream(destinationCSV.createOutput());
            if (settings.isSet(ISettings.SAVE_EPGINFO_TITLE)) {
                wholeText = quoteSign + info.getCridTitel().replace('\"', '\'') + quoteSign;
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_YEAR)) {
                if (wholeText.length() != 0) wholeText += ";";
                wholeText += quoteSign + info.getCridFilmTypeYear().replace('\"', '\'') + quoteSign;
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_DESCRIPTION)) {
                if (wholeText.length() != 0) wholeText += ";";
                wholeText += quoteSign + info.getCridDescription().replace('\"', '\'') + quoteSign;
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_BROADCASTSTATION)) {
                if (wholeText.length() != 0) wholeText += ";";
                channelName = getChannelName();
                wholeText += quoteSign + channelName.replace('\"', '\'') + quoteSign;
            }
            if (settings.isSet(ISettings.SAVE_EPGINFO_DATE)) {
                if (wholeText.length() != 0) wholeText += ";";
                wholeText += quoteSign + date2shortString() + quoteSign;
            }
            out.write(wholeText.getBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("exportAsCSV()", e);
            if (e instanceof FileNotFoundException) {
                errorTxt = Messages.getString("SaveEPGInfo.CantCreateFile");
            } else {
                errorTxt = e.getMessage();
            }
            String dest = destination == null ? Messages.getString("Global.Unbekannt") : destination.getAbsolutePath();
            String crid = info == null ? Messages.getString("Global.Unbekannt") : info.getFileName();
            throw new CridmanagerException(new StringBuffer(Messages.getString("SaveEPGInfo.SaveErrorMessage") + ", dest= " + dest + ", crid= " + crid + "). "), e);
        } finally {
            if (out != null) destinationCSV.close();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsCSV() - end");
        }
        return true;
    }

    /**
	 * gibt die EPG-Informationen in eine CSV-Datenbank aus
	 */
    private boolean exportAsCSV_DB() throws CridmanagerException {
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsCSV_DB() - start");
        }
        String wholeText = "";
        ;
        String quoteSign = "\"";
        String channelName = "";
        String lineFeed = System.getProperty("line.separator");
        String databaseName = Messages.getString("SaveEPGInfo.DatabaseFile");
        OutputStream out = null;
        FsFile destinationCSV_DB = null;
        try {
            destinationCSV_DB = new FsFile(Utils.getUserHome(databaseName));
            if (!destinationCSV_DB.exists()) {
                out = new BufferedOutputStream(destinationCSV_DB.createOutput());
            } else {
                out = new BufferedOutputStream(destinationCSV_DB.getOutput());
            }
            wholeText = quoteSign + info.getCridTitel().replace('\"', '\'') + quoteSign;
            wholeText += ";";
            wholeText += quoteSign + info.getCridFilmTypeYear().replace('\"', '\'') + quoteSign;
            wholeText += ";";
            wholeText += quoteSign + info.getCridDescription().replace('\"', '\'') + quoteSign;
            wholeText += ";";
            channelName = getChannelName();
            wholeText += quoteSign + channelName.replace('\"', '\'') + quoteSign;
            wholeText += ";";
            wholeText += quoteSign + date2shortString() + quoteSign;
            wholeText += lineFeed;
            out.write(wholeText.getBytes());
            out.flush();
        } catch (Exception e) {
            logger.error("exportAsCSV_DB()", e);
            if (e instanceof FileNotFoundException) {
                errorTxt = Messages.getString("SaveEPGInfo.CantCreateFile");
            } else {
                errorTxt = e.getMessage();
            }
            String dest = destination == null ? Messages.getString("Global.Unbekannt") : destination.getAbsolutePath();
            String crid = info == null ? Messages.getString("Global.Unbekannt") : info.getFileName();
            throw new CridmanagerException(new StringBuffer(Messages.getString("SaveEPGInfo.SaveErrorMessage") + ", dest= " + dest + ", crid= " + crid + "). "), e);
        } finally {
            if (out != null) destinationCSV_DB.close();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsCSV_DB() - end");
        }
        return true;
    }

    /**
	 * @return
	 */
    private String getChannelName() {
        if (logger.isDebugEnabled()) {
            logger.debug("getChannelName() - start");
        }
        String channelName;
        channelName = Messages.getString("CridInfo.Channel") + " " + info.getCridServiceID();
        try {
            channelName = getBoxManager().getBox(info.getCridFile()).getChannelManager().getServiceName(info.getCridServiceID());
        } catch (Exception e) {
            logger.error("getChannelName()", e);
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getChannelName() - end");
        }
        return channelName;
    }

    /**
	 * gibt die EPG-Informationen in ein JPG-File aus
	 */
    private boolean exportAsJPG() throws CridmanagerException {
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsJPG() - start");
        }
        OutputStream out = null;
        IFile destinationJPG = null;
        Collection textPagesDesc;
        Collection textPagesType;
        Collection textPagesTitle;
        float drawPosY = 100;
        float drawPosXMargin = 70;
        float drawPosX = 0;
        Point imageSize = new Point(720, 576);
        int spaceTitle_Type = 35;
        int spaceType_Desc = 20;
        BufferedImage image;
        try {
            Font fontTitle = new Font(Messages.getString("SaveEPGInfo.JpegFont"), Font.BOLD, 29);
            Font fontType = new Font(Messages.getString("SaveEPGInfo.JpegFont"), Font.BOLD | Font.ITALIC, 18);
            Font fontDesc = new Font(Messages.getString("SaveEPGInfo.JpegFont"), Font.PLAIN, 18);
            {
                image = new BufferedImage(imageSize.x, imageSize.y, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2D = image.createGraphics();
                FontRenderContext frc = g2D.getFontRenderContext();
                textPagesTitle = splitTextToPages(new StringBuffer(info.getCridTitel()), fontTitle, drawPosY);
                if (settings.isSet(ISettings.SAVE_EPGINFO_TITLE)) {
                    TextLayout layout = new TextLayout("Teststring", fontTitle, frc);
                    drawPosY += (layout.getAscent() + layout.getDescent() + layout.getLeading()) * countLines(textPagesTitle);
                }
                drawPosY += spaceTitle_Type;
                textPagesType = splitTextToPages(new StringBuffer(info.getCridFilmTypeYear()), fontType, drawPosY);
                if (settings.isSet(ISettings.SAVE_EPGINFO_YEAR)) {
                    TextLayout layout = new TextLayout("Teststring", fontType, frc);
                    drawPosY += (layout.getAscent() + layout.getDescent() + layout.getLeading()) * countLines(textPagesType);
                }
                drawPosY += spaceType_Desc;
                textPagesDesc = splitTextToPages(new StringBuffer(info.getCridDescription()), fontDesc, drawPosY);
            }
            Iterator iteratorDesc = textPagesDesc.iterator();
            int counter = 0;
            do {
                if (counter == 0) destinationJPG = path.create(fileName + ".jpg"); else destinationJPG = path.create(fileName + "_" + String.valueOf(counter) + ".jpg");
                out = new BufferedOutputStream(destinationJPG.createOutput());
                image = new BufferedImage(imageSize.x, imageSize.y, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2D = image.createGraphics();
                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2D.setColor(new Color(4, 9, 111));
                g2D.fillRect(0, 0, image.getWidth(), image.getHeight());
                drawPosY = 100;
                if (settings.isSet(ISettings.SAVE_EPGINFO_TITLE)) {
                    if (info.getCridTitel().length() > 0) {
                        g2D.setFont(fontTitle);
                        g2D.setColor(new Color(255, 255, 255));
                        Iterator iteratorTitle = textPagesTitle.iterator();
                        drawPosY = paintLines2Graphic(g2D, iteratorTitle, fontTitle, imageSize, new Point2D.Float(drawPosX, drawPosY), drawPosXMargin, CENTER, UNDERLINE);
                        drawPosY += spaceTitle_Type;
                    }
                }
                if (settings.isSet(ISettings.SAVE_EPGINFO_YEAR)) {
                    if (info.getCridFilmTypeYear().length() > 0) {
                        g2D.setFont(fontType);
                        g2D.setColor(new Color(255, 255, 255));
                        Iterator iteratorType = textPagesType.iterator();
                        drawPosY = paintLines2Graphic(g2D, iteratorType, fontType, imageSize, new Point2D.Float(drawPosX, drawPosY), drawPosXMargin, CENTER, NO_UNDERLINE);
                        drawPosY += spaceType_Desc;
                    }
                }
                if (settings.isSet(ISettings.SAVE_EPGINFO_DESCRIPTION)) {
                    g2D.setFont(fontDesc);
                    g2D.setColor(new Color(200, 200, 200));
                    drawPosY = paintLines2Graphic(g2D, iteratorDesc, fontDesc, imageSize, new Point2D.Float(drawPosX, drawPosY), drawPosXMargin, LEFT, NO_UNDERLINE);
                }
                String channelTime = "";
                if (settings.isSet(ISettings.SAVE_EPGINFO_BROADCASTSTATION)) {
                    channelTime = getChannelName();
                }
                if (settings.isSet(ISettings.SAVE_EPGINFO_DATE)) {
                    if (settings.isSet(ISettings.SAVE_EPGINFO_BROADCASTSTATION)) channelTime += ", ";
                    channelTime += date2shortString();
                }
                if (channelTime != "") {
                    g2D.setFont(fontDesc);
                    g2D.setColor(new Color(200, 200, 200));
                    drawPosY += 1.5 * fontDesc.getSize2D();
                    g2D.drawString(channelTime, drawPosXMargin, drawPosY);
                }
                ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream(0xfff);
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(byteArrayOutput);
                JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(image);
                param.setQuality((float) 0.95, true);
                encoder.encode(image, param);
                out.write(byteArrayOutput.toByteArray());
                out.flush();
                destinationJPG.close();
                counter++;
            } while (iteratorDesc.hasNext());
        } catch (Exception e) {
            logger.error("exportAsJPG()", e);
            if (e instanceof FileNotFoundException) {
                errorTxt = Messages.getString("SaveEPGInfo.CantCreateFile");
            } else {
                errorTxt = e.getMessage();
            }
            String dest = destination == null ? Messages.getString("Global.Unbekannt") : destination.getAbsolutePath();
            String crid = info == null ? Messages.getString("Global.Unbekannt") : info.getFileName();
            throw new CridmanagerException(new StringBuffer(Messages.getString("SaveEPGInfo.SaveErrorMessage") + ", dest= " + dest + ", crid= " + crid + "). "), e);
        } finally {
            if (out != null) destinationJPG.close();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("exportAsJPG() - end");
        }
        return true;
    }

    /**
	 * zeichnet alle Textzeilen einer Seite in eine Grafik
	 */
    private float paintLines2Graphic(Graphics2D g2D, Iterator iteratorPage, Font font, Point imageSize, Point2D.Float drawPos, float drawPosXMargin, int alignment, boolean underline) {
        if (logger.isDebugEnabled()) {
            logger.debug("paintLines2Graphic(Graphics2D, Iterator, Font, Point, Point2D.Float, float, int, boolean) - start");
        }
        FontRenderContext frc = g2D.getFontRenderContext();
        if (iteratorPage.hasNext()) {
            ArrayList page = (ArrayList) iteratorPage.next();
            Iterator iteratorLine = page.iterator();
            while (iteratorLine.hasNext()) {
                String line = (String) iteratorLine.next();
                TextLayout layout = new TextLayout(line, font, frc);
                if (alignment == JUSTIFIED && line.lastIndexOf('\n') == -1) layout = layout.getJustifiedLayout(imageSize.x - drawPosXMargin * 2);
                drawPos.y += layout.getAscent();
                switch(alignment) {
                    case LEFT:
                        drawPos.x = drawPosXMargin;
                        break;
                    case CENTER:
                        drawPos.x = (imageSize.x - (int) layout.getVisibleAdvance()) / 2;
                        break;
                    case RIGHT:
                        drawPos.x = imageSize.x - layout.getVisibleAdvance() - drawPosXMargin;
                        break;
                    case JUSTIFIED:
                        drawPos.x = drawPosXMargin;
                        break;
                }
                layout.draw(g2D, drawPos.x, drawPos.y);
                drawPos.y += layout.getDescent();
                if (underline) g2D.fillRect((int) drawPos.x, (int) drawPos.y, (int) layout.getVisibleAdvance(), 4);
                drawPos.y += layout.getLeading();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("paintLines2Graphic(Graphics2D, Iterator, Font, Point, Point2D.Float, float, int, boolean) - end");
        }
        return drawPos.y;
    }

    /**
	 * z�hlt die Anzahl der Textzeilen einer Collection
	 */
    private int countLines(Collection textPagesTitle) {
        if (logger.isDebugEnabled()) {
            logger.debug("countLines(Collection) - start");
        }
        int lines = 0;
        Iterator iteratorType = textPagesTitle.iterator();
        while (iteratorType.hasNext()) {
            lines += ((ArrayList) iteratorType.next()).size();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("countLines(Collection) - end");
        }
        return lines;
    }

    /**
	 * unterteilt einen Text in Seiten (sofern noetig) und gibt Zeilen & Seiten als Collection of
	 * ArrayList zur�ck
	 */
    private Collection splitTextToPages(StringBuffer text, Font font, float drawPosYStart) {
        if (logger.isDebugEnabled()) {
            logger.debug("splitTextToPages(StringBuffer, Font, float) - start");
        }
        ArrayList lines = new ArrayList();
        Collection pages = new ArrayList();
        String dots = "... ";
        int oldPositionPage = 0;
        int oldPositionLine = 0;
        float drawPosXMargin = 70;
        float drawPosYMargin = 90;
        float drawPosY = drawPosYStart;
        BufferedImage imageT = new BufferedImage(720, 576, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2DT = imageT.createGraphics();
        if (text.length() > 0) {
            AttributedString attributedString = new AttributedString(text.toString());
            attributedString.addAttribute(TextAttribute.FONT, font);
            AttributedCharacterIterator paragraph = attributedString.getIterator();
            float formatWidth = imageT.getWidth() - drawPosXMargin * 2;
            int paragraphStart = paragraph.getBeginIndex();
            int paragraphEnd = paragraph.getEndIndex();
            ArrayList tabLocations = new ArrayList();
            int ch = 0;
            do {
                tabLocations.add(new Integer(ch = text.indexOf("\n", ch + 1)));
            } while (ch != -1);
            tabLocations.set(tabLocations.size() - 1, new Integer(text.length() - 1));
            Iterator iteratorCount = tabLocations.iterator();
            int currentTab = ((Integer) iteratorCount.next()).intValue();
            LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, g2DT.getFontRenderContext());
            lineMeasurer.setPosition(paragraphStart);
            oldPositionPage = lineMeasurer.getPosition();
            while (lineMeasurer.getPosition() < paragraphEnd) {
                oldPositionLine = lineMeasurer.getPosition();
                if (!pages.isEmpty()) {
                    text.insert(oldPositionLine, dots);
                    attributedString = new AttributedString(text.toString());
                    attributedString.addAttribute(TextAttribute.FONT, font);
                    paragraph = attributedString.getIterator();
                    lineMeasurer = new LineBreakMeasurer(paragraph, g2DT.getFontRenderContext());
                    paragraphEnd = paragraph.getEndIndex();
                    lineMeasurer.setPosition(oldPositionPage);
                    for (int i = 0; i < tabLocations.size(); i++) {
                        tabLocations.set(i, new Integer(((Integer) tabLocations.get(i)).intValue() + dots.length()));
                    }
                    currentTab += dots.length();
                }
                while ((lineMeasurer.getPosition() < paragraphEnd) && (drawPosY < 576 - drawPosYMargin)) {
                    TextLayout layout = lineMeasurer.nextLayout(formatWidth, currentTab + 1, false);
                    lines.add(text.substring(oldPositionLine, lineMeasurer.getPosition()));
                    oldPositionLine = lineMeasurer.getPosition();
                    if (lineMeasurer.getPosition() > currentTab && iteratorCount.hasNext()) currentTab = ((Integer) iteratorCount.next()).intValue();
                    drawPosY += layout.getAscent() + layout.getDescent() + layout.getLeading();
                }
                oldPositionPage = lineMeasurer.getPosition();
                int lastLineNumber = lines.size() - 1;
                if (oldPositionPage < paragraphEnd) {
                    String oldLastLine = (String) lines.get(lastLineNumber);
                    String newLastLine = appendDots(oldLastLine, lineMeasurer, font, g2DT, formatWidth);
                    lines.set(lastLineNumber, newLastLine);
                } else {
                    String oldLastLine = (String) lines.get(lastLineNumber);
                    if (oldLastLine.charAt((oldLastLine.length()) - 1) != '\n') lines.set(lastLineNumber, oldLastLine.concat("\n"));
                }
                pages.add(new ArrayList(lines));
                lines.clear();
                drawPosY = drawPosYStart;
                oldPositionPage = lineMeasurer.getPosition() + 1;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("splitTextToPages(StringBuffer, Font, float) - end");
        }
        return pages;
    }

    /**
	 * H�ngt "..." an die Textzeile an. Wenn kein Platz ist, wird Text gek�rzt und der Zeiger im
	 * lineMeasurer entsprechend korrigiert
	 */
    private String appendDots(String line, LineBreakMeasurer lineMeasurer, Font font, Graphics2D g2DT, float formatWidth) {
        if (logger.isDebugEnabled()) {
            logger.debug("appendDots(String, LineBreakMeasurer, Font, Graphics2D, float) - start");
        }
        int index;
        int length = line.length();
        String extendedLine;
        FontRenderContext frc = g2DT.getFontRenderContext();
        TextLayout layout = new TextLayout(line.concat("..."), font, frc);
        if (layout.getAdvance() > (formatWidth)) {
            do {
                index = line.lastIndexOf(' ');
                line = line.substring(0, index);
            } while (index > length - 4);
            lineMeasurer.setPosition(lineMeasurer.getPosition() - (length - index - 1));
            extendedLine = line.concat(" ...");
        } else {
            extendedLine = line.concat("...");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("appendDots(String, LineBreakMeasurer, Font, Graphics2D, float) - end");
        }
        return extendedLine;
    }

    /**
	 * gibt Aufnahmedatum und -zeit als String zur�ck
	 */
    private String date2shortString() {
        if (logger.isDebugEnabled()) {
            logger.debug("date2shortString() - start");
        }
        Date startDate = info.getCridStartDate();
        Date endDate = info.getCridEndDate();
        DateFormat dFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat tFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        String returnString = dFormat.format(startDate) + " " + tFormat.format(startDate) + "-" + tFormat.format(endDate);
        if (logger.isDebugEnabled()) {
            logger.debug("date2shortString() - end");
        }
        return returnString;
    }
}
