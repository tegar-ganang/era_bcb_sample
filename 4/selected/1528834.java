package mou.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import mou.Main;
import mou.Subsystem;
import mou.core.colony.Colony;
import mou.core.res.ResourceMenge;
import mou.gui.colonyscreen.ColonyDialog;
import mou.net.battle.SpaceBattleResult;
import mou.storage.ser.ID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import burlov.swing.MessagePanel.Message;

/**
 * Enth�lt Methoden die f�e alle Klassen der GUI-Package n�tzlich sein k�nnten
 */
public class GUI extends Subsystem {

    private static final String STRING_UNBEKANNT = "Unbekannt";

    public static final int MSG_PRIORITY_POPUP = 0;

    public static final int MSG_PRIORITY_NORMAL = 1;

    public static final int MSG_PRIORITY_URGENT = 2;

    private static final DecimalFormat LONG_FORMAT;

    private static final DecimalFormat DOUBLE_FORMAT;

    private static final DecimalFormat SMART_FORMAT_1;

    private static final DecimalFormat SMART_FORMAT_2;

    private static final DecimalFormat SMART_FORMAT_3;

    private static final DecimalFormat DATE_FORMAT;

    private static MainFrame mainFrame;

    private ColonyDialog colonyScreen;

    private Point startStarmapPosition;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(':');
        DATE_FORMAT = new DecimalFormat("000000000,000", symbols);
        symbols = new DecimalFormatSymbols(Locale.GERMANY);
        LONG_FORMAT = new DecimalFormat("###,###", symbols);
        DOUBLE_FORMAT = new DecimalFormat("###,##0.000", symbols);
        SMART_FORMAT_1 = new DecimalFormat("###,##0.0", symbols);
        SMART_FORMAT_2 = new DecimalFormat("###,##0.00", symbols);
        SMART_FORMAT_3 = new DecimalFormat("###,##0.000", symbols);
    }

    public GUI(Subsystem parent) {
        super(parent);
        mainFrame = new MainFrame();
    }

    public void updateGUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.updateComponentTreeUI(Main.instance().getGUI().getMainFrame());
    }

    /**
	 * Nachricht wird im NAchrichtenfester f�r den Benutzer sichtbar gemacht
	 * 
	 * @param msg
	 *            die Nachricht selbst
	 * @param prioritet
	 *            Eine der MSG_PRIORITY Konstanten
	 */
    public void promtMessage(final String titel, final String msg, final int prioritet) {
        promtMessage(titel, msg, prioritet, null);
    }

    public void promtMessage(final String titel, final String msg, final int prioritet, final Runnable run) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    promtMessage(titel, msg, prioritet, run);
                }
            });
            return;
        }
        String text = "<html><font color=green>" + formatDate(Main.instance().getTime()) + "</font> " + "<font color=blue>[" + titel + "] </font>" + msg + "</html>";
        Message message = new Message(run, text, false);
        switch(prioritet) {
            case MSG_PRIORITY_POPUP:
                message.setUrgent(true);
                if (mainFrame == null) return;
                mainFrame.getMessagesPanel().appendMessage(message);
                return;
            case MSG_PRIORITY_URGENT:
                message.setUrgent(true);
            case MSG_PRIORITY_NORMAL:
                if (mainFrame == null) return;
                mainFrame.getMessagesPanel().appendMessage(message);
        }
    }

    public void promtBattleResult(final Point pos, SpaceBattleResult res, ID gegner) {
        if (res == null) return;
        String msg = "<br><font color=red>Eigene Verluste (Schiffe): Zerst�rt " + GUI.formatLong(res.getDestroyedGood()) + " Besch�digt " + GUI.formatLong(res.getDamagedGood()) + ".</font>" + "<font color=green> Feindliche Verluste (Schiffe): Zerst�rt " + GUI.formatLong(res.getDestroyedEvil()) + " Besch�digt " + GUI.formatLong(res.getDamagedEvil()) + ".</font>";
        if (res.isColonyCaptured()) {
            msg += " Eine feindliche Kolonie wurde erobert!";
        }
        promtMessage("Raumkampf bei Koordinaten " + GUI.formatPoint(pos) + " gegen " + Main.instance().getMOUDB().getCivilizationDB().getCivName(gegner), msg, GUI.MSG_PRIORITY_URGENT, new Runnable() {

            public void run() {
                centreStarmaponPosition(pos);
            }
        });
    }

    public void centreStarmaponPosition(Point pos) {
        getMainFrame().getStarmapScreen().centerPosition(pos);
        getMainFrame().selectScreen(MainFrame.SCREEN_STARMAP);
    }

    public Font loadFont(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            Font ret = Font.createFont(Font.TRUETYPE_FONT, in);
            in.close();
            return ret;
        } catch (Exception e1) {
            logThrowable("Fehler bei Font laden.", e1);
        }
        return GUIConstants.FONT_DEFAULT;
    }

    public static Image loadImage(String path) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = mainFrame.getClass().getResourceAsStream(path);
        if (in == null) throw new RuntimeException("Ressource " + path + " not found");
        try {
            IOUtils.copy(in, out);
            in.close();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            new RuntimeException("Error reading ressource " + path, e);
        }
        return Toolkit.getDefaultToolkit().createImage(out.toByteArray());
    }

    /**
	 * Um den Konstruktor zu entlasten werden alles Zweitrangiges hier gestartet
	 */
    public void startModulIntern() throws Exception {
        try {
        } catch (Throwable e) {
            logThrowable("Fehler bei Look-and-Feel initialisierung", e);
        }
        mainFrame.initFrame();
        mainFrame.restoreSettings(getPreferences());
        if (startStarmapPosition != null) mainFrame.getStarmapScreen().centerPosition(startStarmapPosition);
        mainFrame.setVisible(true);
        colonyScreen = new ColonyDialog(mainFrame);
        ToolTipManager man = ToolTipManager.sharedInstance();
        man.setDismissDelay(1000 * 60);
        man.setInitialDelay(0);
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public ColonyDialog getColonyDialog() {
        return colonyScreen;
    }

    /**
	 * Preferences File wird in dem Benutzerverzeichnis abgelegt.
	 */
    public File getPreferencesFile() {
        File ret = new File(Main.APPLICATION_USER_DATA_DIR, "gui.cfg");
        if (Main.isDebugMode()) System.out.println(ret);
        return ret;
    }

    public void shutdownIntern() {
        mainFrame.shutdown(getPreferences());
    }

    public String getModulName() {
        return "GUI";
    }

    public Level getDefaultLoggerLevel() {
        return Level.ALL;
    }

    /**
	 * Zentriert ein Window relativ zu dem Parent-Window
	 * 
	 * @param parent
	 *            Parent-Window, wenn null, dann wird relativ zu dem Bildschirm zentriert
	 * @param child
	 *            Window das zentrirt werden soll.
	 */
    public static void centreWindow(Window parent, Window child) {
        if (child == null) return;
        Point parentLocation = null;
        Dimension parentSize = null;
        if (parent == null) {
            parentLocation = new Point(0, 0);
            parentSize = child.getToolkit().getScreenSize();
        } else {
            parentLocation = parent.getLocationOnScreen();
            parentSize = parent.getSize();
        }
        Dimension childSize = child.getSize();
        child.setLocation((int) (parentLocation.getX() + parentSize.getWidth() / 2 - childSize.getWidth() / 2), (int) (parentLocation.getY() + parentSize.getHeight() / 2 - childSize.getHeight() / 2));
    }

    public static synchronized String formatLong(long val) {
        return LONG_FORMAT.format(val);
    }

    public static synchronized String formatLong(Number val) {
        if (val == null) return STRING_UNBEKANNT;
        return LONG_FORMAT.format(val);
    }

    /**
	 * Formatiert Zahlen immer mit 3 Wertstellen
	 * 
	 * @param val
	 * @return
	 */
    public static synchronized String formatDouble(double val) {
        return DOUBLE_FORMAT.format(val);
    }

    /**
	 * Formatiert Zahlen immer mit 3 Wertstellen
	 * 
	 * @param val
	 * @return
	 */
    public static synchronized String formatDouble(Number val) {
        if (val == null) return STRING_UNBEKANNT;
        return DOUBLE_FORMAT.format(val);
    }

    public static synchronized String formatDouble(double val, int place) {
        switch(place) {
            case 1:
                return SMART_FORMAT_1.format(val);
            case 2:
                return SMART_FORMAT_2.format(val);
            case 3:
                return SMART_FORMAT_3.format(val);
        }
        return DOUBLE_FORMAT.format(val);
    }

    /**
	 * Formatiert Zahlen immer mit mindestens 3 Wertstellen. Beispiele: Zahl < 1: drei
	 * Nachkommastellen 1 < Zahl < 10: zwei Nachkommastellen 10 < Zahl < 100: eine Nachkommastelle
	 * 100 < Zahl: keine Nachkommastellen
	 * 
	 * @param val
	 * @return
	 */
    public static synchronized String formatSmartDouble(Number val) {
        return formatSmartDouble(val.doubleValue());
    }

    /**
	 * Formatiert Zahlen immer mit mindestens 3 Wertstellen. Beispiele: Zahl < 1: drei
	 * Nachkommastellen 1 < Zahl < 10: zwei Nachkommastellen 10 < Zahl < 100: eine Nachkommastelle
	 * 100 < Zahl: keine Nachkommastellen
	 * 
	 * @param val
	 * @return
	 */
    public static synchronized String formatSmartDouble(double val) {
        double absVal = Math.abs(val);
        if (absVal < 0.001) return "0";
        if (absVal < 1) return SMART_FORMAT_3.format(val);
        if (absVal < 10) return SMART_FORMAT_2.format(val);
        if (absVal < 100) return SMART_FORMAT_1.format(val);
        return LONG_FORMAT.format(val);
    }

    public static synchronized String formatPoint(Point p) {
        if (p == null) return STRING_UNBEKANNT;
        return "[" + p.x + ":" + p.y + "]";
    }

    public static String formatPoints(Collection<Point> points) {
        StringBuffer buf = new StringBuffer("{");
        for (Point pos : points) buf.append(formatPoint(pos));
        buf.append("}");
        return buf.toString();
    }

    /**
	 * Formatiert mit Prozentformat und ein + oder -
	 * 
	 * @param val
	 * @return
	 */
    public static synchronized String formatProzentSigned(double val) {
        String ret = formatSmartDouble(val);
        if (val >= 0) ret = "+" + ret;
        return ret + "%";
    }

    /**
	 * Formatiert mit Prozentformat ohne Pluszeichen
	 * 
	 * @param val
	 * @return
	 */
    public static synchronized String formatProzent(double val) {
        String ret = formatSmartDouble(val);
        return ret + "%";
    }

    public static synchronized String formatDate(long date) {
        return DATE_FORMAT.format(date);
    }

    public static synchronized String formatDate(Long date) {
        if (date == null) return STRING_UNBEKANNT;
        return DATE_FORMAT.format(date);
    }

    /**
	 * Darstelle Liste mit ResourceMenge Objekten in HTML-Format, ohne anf�hrenden und
	 * abschlie�enden html-Tags
	 * 
	 * @param resources
	 * @return
	 */
    public static String htmlFormatRessourceMenge(Collection<ResourceMenge> resources) {
        StringBuffer buf = new StringBuffer();
        for (ResourceMenge res : resources) {
            buf.append("<b>");
            buf.append(res.getRessource().getName());
            buf.append(": </b>");
            buf.append(formatSmartDouble(res.getMenge()));
            buf.append("<br>");
        }
        return buf.toString();
    }

    public void showColony(Colony col) {
        colonyScreen.showKolonie(col);
        colonyScreen.pack();
        centreWindow(mainFrame, colonyScreen);
        colonyScreen.setVisible(true);
    }

    /**
	 * Bestimmt Position auf der Sternenkarte die nach dem Start gezeigt wird
	 */
    public void setStartPosition(Point pos) {
        startStarmapPosition = pos;
    }

    /**
	 * Zeigt ein Auswahldialog mit bestehenden Kolonien
	 * 
	 * @param msg
	 * @return Kolonie oder null
	 */
    public Colony selectKolony(String msg) {
        Map<ID, Colony> cols = Main.instance().getMOUDB().getKolonieDB().getAlleKolonien();
        if (cols.isEmpty()) return null;
        Object[] array = cols.values().toArray();
        Colony col = (Colony) JOptionPane.showInputDialog(getMainFrame(), msg, "Kolonie ausw�hlen", JOptionPane.QUESTION_MESSAGE, null, array, null);
        return col;
    }

    public void selectDiplomacyScreen() {
        getMainFrame().selectScreen(MainFrame.SCREEN_DIPLOMACY);
    }
}
