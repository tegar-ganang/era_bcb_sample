package com.art.anette.client.main;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import javax.net.ssl.SSLException;
import javax.swing.*;
import com.art.anette.client.controller.BasicController;
import com.art.anette.client.controller.ClientConfiguration;
import com.art.anette.client.controller.Logic;
import com.art.anette.client.network.NetworkControl;
import com.art.anette.client.ui.CheckThreadViolationRepaintManager;
import com.art.anette.client.ui.EventDispatchThreadHangMonitor;
import com.art.anette.client.ui.forms.MainWindow;
import com.art.anette.client.ui.forms.WelcomeDialog;
import com.art.anette.common.SharedGlobal;
import com.art.anette.common.logging.LogController;
import com.art.anette.common.logging.Logger;
import com.art.anette.common.network.LoginRequest;
import com.art.anette.exceptions.LogicInitFailedException;
import com.art.anette.exceptions.LoginFailedException;
import com.art.anette.exceptions.NetworkException;

/**
 * Startet den Client und zeigt die GUI an.
 *
 * @author Markus Groß
 */
public class Main {

    /**
     * Die Sprach-Resource, enthält die Übersetzungen der Strings.
     */
    private static ResourceBundle lang;

    private static Logger logger;

    /**
     * Startet das Programm und entscheidet, ob der Registrierungs-, Login-
     * oder Hauptfenster-Dialog angezeigt wird.
     *
     * @param args Die Kommando-Zeilen Parameter. Es werden keine ausgewertet,
     *             also können diese immer leer sein.
     */
    public static void main(final String[] args) throws InterruptedException, InvocationTargetException {
        ExceptionCatcher exceptionCatcher = new ExceptionCatcher();
        Thread.setDefaultUncaughtExceptionHandler(exceptionCatcher);
        System.setProperty("sun.awt.exception.handler", ExceptionCatcher.class.getName());
        setupHome();
        if (!setupLocking(logger)) {
            System.exit(1);
        }
        logger = LogController.getLogger(Main.class);
        final ClientConfiguration config = ClientConfiguration.getInstance();
        setDefaultLocale(config);
        lang = ResourceBundle.getBundle("com/art/anette/client/ui/resources/lang");
        if (!SystemTray.isSupported()) {
            ProgressUtils.errorMessage(lang.getString("NoTray"));
            System.exit(1);
        }
        if (!TimeZone.getDefault().useDaylightTime()) {
            showTimezoneInfo(logger);
            ProgressUtils.infoMessage(lang.getString("NoDaylightTime"));
        }
        if ("OpenJDK 64-Bit Server VM".equals(System.getProperty("java.vm.name"))) {
            ProgressUtils.errorMessage(lang.getString("BadVM1"));
            System.exit(1);
        }
        if ("true".equals(System.getProperty("anetteCheckEDT", "true"))) {
            RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
        } else {
            logger.info("Don't install EDT check by user request");
        }
        if ("true".equals(System.getProperty("anetteCheckSlowEvents", "true"))) {
            EventDispatchThreadHangMonitor.initMonitoring();
        } else {
            logger.info("Don't install check for slow event processing by user request");
        }
        adjustTimezone(logger);
        try {
            NetworkControl.getInstance().initSSL();
        } catch (SSLException ex) {
            logger.severe("SSL Failure", ex);
            ProgressUtils.errorMessage("SSLFailed");
            System.exit(1);
        }
        final BasicController basicControl = BasicController.getInstance();
        if ("".equals(config.getProperty("user.email")) || "".equals(config.getProperty("user.password"))) {
            showWelcomeDialog(config, basicControl, "", "");
        } else {
            try {
                final ProgressUtils.Monitor monitor = ProgressUtils.createProgress(lang.getString("ProgressLoggingIn"));
                basicControl.login(new LoginRequest(config.getProperty("user.email"), config.getProperty("user.password")));
                monitor.finish();
            } catch (NetworkException ex) {
                logger.warning("Connection Failure", ex);
                ProgressUtils.errorMessage(lang.getString("ConnectionFailed"), ex.getMessage());
                showWelcomeDialog(config, basicControl, config.getProperty("user.email"), config.getProperty("user.password"));
            } catch (LoginFailedException ex) {
                ProgressUtils.errorMessage(String.format(lang.getString("LoginFailed"), ex.getMessage()));
                logger.info("Login Failure", ex);
                showWelcomeDialog(config, basicControl, config.getProperty("user.email"), config.getProperty("user.password"));
            }
        }
        if (basicControl.isLoggedIn()) {
            try {
                final ProgressUtils.Monitor progress = ProgressUtils.createProgress(lang.getString("ProgressLogicDatabase"));
                final Logic logic = Logic.getInstance(config, basicControl.getLoggedInEmployee());
                NetworkControl.getInstance().setLoginData(basicControl.getLoginData(), logic.getDBControl());
                NetworkControl.getInstance().start();
                ClientConfiguration.getInstance().setProperty("db.dirty", "true");
                ClientConfiguration.getInstance().writeConfiguration();
                progress.finish();
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        new MainWindow(logic);
                    }
                });
            } catch (LogicInitFailedException ex) {
                logger.severe("Failed to initialize the logic!", ex);
                ProgressUtils.errorMessage(lang.getString("NoValidLogic"));
            }
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    static void adjustTimezone(final Logger logger) {
        final String wanted = ClientConfiguration.getInstance().getProperty("timezone", "Europe/Berlin");
        final String[] ids = TimeZone.getAvailableIDs();
        boolean valid = false;
        for (String id : ids) {
            if (id.equals(wanted)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            logger.severe("The timezone requested ('" + wanted + "') isn't available. All known timezones: " + Arrays.toString(ids));
            ProgressUtils.errorMessage(lang.getString("BadTimezone"));
            System.exit(1);
        }
        final TimeZone timeZone = TimeZone.getTimeZone(wanted);
        if (!timeZone.equals(TimeZone.getDefault())) {
            logger.warning("The requested timezone ('" + timeZone.getID() + "') differs from the default timezone of the JVM ('" + TimeZone.getDefault().getID() + "'). Will change the timezone according to your request.");
            TimeZone.setDefault(timeZone);
        }
    }

    private static void showWelcomeDialog(final ClientConfiguration config, final BasicController basicControl, final String email, final String password) throws InterruptedException, InvocationTargetException {
        GuiUtils.invokeAndWait(new Runnable() {

            public void run() {
                WelcomeDialog wd = new WelcomeDialog(basicControl, config);
                wd.setLoginData(email, password);
                wd.setVisible(true);
            }
        });
    }

    static void setDefaultLocale(ClientConfiguration config) {
        final String language = config.getProperty("app.language", Locale.getDefault().getLanguage());
        Locale.setDefault(new Locale(language));
    }

    static void showTimezoneInfo(final Logger logger) {
        logger.info("Timezone: java properties:");
        logger.info("  user.timezone=" + System.getProperty("user.timezone"));
        logger.info("  user.country=" + System.getProperty("user.country"));
        logger.info("Timezone: default");
        final TimeZone dtz = TimeZone.getDefault();
        logger.info("  toString=" + dtz);
        logger.info("  id=" + dtz.getID());
        logger.info("  name=" + dtz.getDisplayName());
        logger.info("  useDaylightTime=" + dtz.useDaylightTime());
    }

    @SuppressWarnings({ "UseOfSystemOutOrSystemErr" })
    static void setupHome() {
        File homeDir = new File(SharedGlobal.APP_HOME_DIR);
        if (!homeDir.exists()) {
            homeDir.mkdirs();
        }
        boolean b1 = homeDir.setReadable(false, false);
        boolean b2 = homeDir.setWritable(false, false);
        boolean b3 = homeDir.setExecutable(false, false);
        boolean b4 = homeDir.setReadable(true, true);
        boolean b5 = homeDir.setWritable(true, true);
        boolean b6 = homeDir.setExecutable(true, true);
        if (!b1 || !b2 || !b3 || !b4 || !b5 || !b6) {
            System.err.println("Warning: Can't secure the directory " + homeDir + ". Results= " + b1 + ' ' + b2 + ' ' + b3 + ' ' + b4 + ' ' + b5 + ' ' + b6);
        }
        if (!homeDir.canRead() || !homeDir.canWrite() || !homeDir.canExecute()) {
            System.err.println("Warning: Can't access the directory " + homeDir + " fully. This may result in other error messages. read=" + homeDir.canRead() + " write=" + homeDir.canWrite() + " exec=" + homeDir.canExecute());
        }
        File logDir = new File(Global.LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    /**
     * Erstellt die Locking-Datei und lockt sie. Falls die Datei bereits gelockt
     * ist, läuft bereits ein anderer Client. From http://jimlife.wordpress.com/2008/07/21/java-application-make-sure-only-singleone-instance-running-with-file-lock-ampampampampamp-shutdownhook/.
     *
     * @return True, falls kein anderer Client läuft.
     */
    static boolean setupLocking(final Logger logger) {
        try {
            final File lockFile = new File(Global.LOCK_FILE);
            LogController.setUpClientLogging(Global.LOG_DIR);
            final FileChannel lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            final FileLock lock = lockChannel.tryLock();
            if (lock == null) {
                lockChannel.close();
                final String message = "Another client is running on your computer.\n" + "You have to quit the other client.\n" + "This client will quit itself now.";
                if (GraphicsEnvironment.isHeadless()) {
                    System.err.println(message.replace("\n", " "));
                } else {
                    JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
                return false;
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        try {
                            if (lock != null) {
                                lock.release();
                                lockChannel.close();
                                lockFile.delete();
                            }
                            logger.info("Client shuting down");
                        } catch (IOException ex) {
                            logger.severe(null, ex);
                        }
                    }
                });
                return true;
            }
        } catch (IOException ex) {
            logger.severe(null, ex);
            return true;
        }
    }

    private Main() {
    }
}
