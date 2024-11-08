package nhap;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.EventObject;
import javax.swing.text.DateFormatter;
import nhap.ai.Reactor;
import nhap.rep.Player;
import nhap.rep.manager.RepresentationManager;
import nhap.screen.DefaultScreenMode;
import nhap.swing.NethackState;
import nhap.utils.ErrorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.taHjaj.wo.gui.ComponentTraceListener;
import org.xml.sax.SAXException;

/**
 * Description of the Class
 * 
 * @author Administrator
 * @created July 9, 2004
 */
public class NhaPlayer {

    private static final Logger logger = Logger.getLogger(NhaPlayer.class);

    private RepresentationManager representationManager;

    private NhapParams nhapParams;

    public NhaPlayer(final NhapParams nhapParams) throws IOException {
        this.nhapParams = nhapParams;
        representationManager = new RepresentationManager(nhapParams);
    }

    public boolean playGame(final NethackState nethackState, final DefaultScreenMode screenParser) {
        boolean fResult = true;
        try {
            final String playerName = nhapParams.getPlayerName();
            final File representationDirectory = nhapParams.getRepresentationDirectory();
            playTheGame(nethackState, screenParser);
            final Player player = representationManager.getPlayer();
            save(screenParser);
            saveRepresentation(representationDirectory, playerName, player.getTime());
            if (nhapParams.getNethackDirectory() != null) {
                backup(nhapParams);
            }
            quit(screenParser);
        } catch (final IOException exception) {
            logger.error(exception);
            fResult = false;
        } catch (final SAXException exception) {
            logger.error(exception);
            fResult = false;
        } catch (final IntrospectionException introspectionException) {
            logger.error(introspectionException);
            fResult = false;
        }
        return fResult;
    }

    private void backup(final NhapParams nhapParams) throws IOException {
        final Player player = representationManager.getPlayer();
        final File nethackDirectory = nhapParams.getNethackDirectory();
        final File savedFile = new File(nethackDirectory, nhapParams.getEnduser() + "-" + player.getName() + ".NetHack-saved-game");
        final File backupFile = new File(nethackDirectory, nhapParams.getEnduser() + "-" + player.getName() + ".NetHack-saved-game." + nhapParams.getTime());
        FileUtils.copyFile(savedFile, backupFile);
    }

    private void playTheGame(final NethackState nethackState, final DefaultScreenMode screenParser) {
        if (screenParser.containsString("Shall I pick")) {
            screenParser.putString("y\r");
            logger.info("Dumping ...");
            screenParser.dump();
            logger.info("Dumped.");
        } else if (screenParser.containsString("in progress under your name")) {
            screenParser.putString("y\r");
        }
        while (screenParser.containsString("--More--")) {
            screenParser.putString("\r");
        }
        if (screenParser.containsString("Couldn't recover old game.")) {
            throw new RuntimeException("Unrecoverable old game");
        }
        screenParser.putString("@");
        logger.info("Running Reactor");
        representationManager.update();
        Reactor reactor = new Reactor(representationManager.getRepresentation());
        final DateFormatter dateFormatter = new DateFormatter();
        dateFormatter.setFormat(DateFormat.getDateTimeInstance());
        while (nethackState.isPlaying()) {
            try {
                representationManager.update();
                representationToXML(dateFormatter);
                reactor.run();
            } catch (Exception e) {
                logger.error("Exception catched while running: " + e.getMessage(), e);
                screenParser.dump();
                logger.error("Exception catched while running: " + e.getMessage(), e);
            }
            while (screenParser.containsString("--More--")) {
                screenParser.putString("\r");
            }
            while (screenParser.getRow(0).indexOf("? [") != -1) {
                screenParser.putString("\r");
            }
        }
    }

    private void representationToXML(final DateFormatter dateFormatter) {
        File file;
        Date now = new Date();
        try {
            file = new File(SystemUtils.getJavaIoTmpDir(), representationManager.getPlayer().getName() + '-' + StringUtils.replaceChars(dateFormatter.valueToString(now), ':', '-') + ".zip");
            logger.debug("logging to " + file.getAbsolutePath());
            representationManager.toXML(file);
        } catch (final ParseException parseException) {
            ErrorUtils.logAndThrowRuntimeException("appearently the Date value '" + now.toString() + "'could not be parsed", parseException);
        }
    }

    private void quit(final DefaultScreenMode screenParser) {
        DefaultScreenMode.getInstance().getComponentTraceListenerHelper().changed(new EventObject(ComponentTraceListener.EVENT_OBJECT_PREFIX + "Exit"));
        screenParser.dump();
        screenParser.putChar('#');
        screenParser.putChar('q');
        screenParser.putChar('\r');
        screenParser.putChar('y');
        screenParser.putChar('n');
        screenParser.putChar('n');
        screenParser.putChar('n');
        screenParser.putChar(' ');
        screenParser.putChar('\n');
        screenParser.dump();
    }

    private void save(final DefaultScreenMode screenParser) {
        screenParser.getComponentTraceListenerHelper().changed(new EventObject(ComponentTraceListener.EVENT_OBJECT_PREFIX + "Exit"));
        screenParser.dump();
        screenParser.putChar('S');
        screenParser.putChar('y');
    }

    private void saveRepresentation(final File representationDirectory, final String playerName, final int time) throws IOException, SAXException, IntrospectionException {
        final File file = getRepresentationLocation(representationDirectory, playerName, time);
        representationManager.toXML(file);
    }

    private void loadRepresentation(final File representationDirectory, final String playerName, final int time) throws IOException, SAXException {
        final File file = getRepresentationLocation(representationDirectory, playerName, time);
        representationManager.toXML(file);
    }

    private File getRepresentationLocation(File representationDirectory, String playerName, int time) {
        return new File(representationDirectory, playerName + '-' + time + ".xml");
    }

    public static NhapParams getNhapParams(final String playerName) {
        final String[] telnetLogonSequence = { "Michiel\r", "Michiel\r", "c:\r" };
        final String[] startSequence = { "cd \\games\\nethack\r", "nethack\r", playerName + '\r' };
        final NhapParams nhapParams = new NhapParams(telnetLogonSequence, "localhost", 23, startSequence, playerName, new File("/"));
        nhapParams.setNethackDirectory(new File("C:/games/nethack/"));
        nhapParams.setEnduser("Michiel");
        nhapParams.setRunLength(3);
        return nhapParams;
    }
}
