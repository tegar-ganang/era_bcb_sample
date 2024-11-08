package ftraq.util;

/**
 * Klasse zur einheitlichen Ausgabe von Debugging-Infos und Warnung auf System.err	<p>
 *
 * Das Prinzip dieser Klasse wurde bei "j4log" vom Apache-Projekt abgeschaut. Ich bin mir nicht sicher, ob wir die gesamte Bibliothek von denen benutzen sollen und habe deswegen diese Klasse mit den n�tigsten Operationen aus "j4log.Category" erstellt, so da� ich nicht viel umschmei�en mu�, wenn wir die Bibliothek benutzen. Vorteil der Bibliothek w�ren z.B.das man auch einfach irgendwohin anders als auf die Konsole loggen kann und die Logging-Optionen ohne neukompilieren �ndern k�nnte.
 *
 * In jeder Klasse sollte man ein statisches Logger-Objekt erzeugen:
 *
 * <pre>
 * private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(DieseKlasse.class);
 * </pre>
 * Danach kann man dann von �berall innerhalb der Klasse einfach Meldungen auf die Konsole ausgeben
 *
 * <pre>
 * logger.debug("nur zum Debuggen interessanter Text");
 * logger.info("text der zum Verst�ndnis des Programmablaufs beitr�gt ");
 * logger.warn("Warnung �ber einen au�ergew�nlichen Zustand");
 * logger.error("Fehlermeldung");
 * logger.fatal("Alles ging schief");
 * </pre>
 *
 * Nun kann man innerhalb der Logger-Klasse festlegen, von welchen Klassen und Paketen wieviel ausgegeben soll. Dies geschieht durch hinzuf�gen eines Elements zur der HashMap packageLoggingPriorities innerhalb des static-Blocks dieser Klasse
 *
 * <pre>
 * packageLoggingPriorities.put("ftraq.", LoggerPriority.WARN);
 * </pre>
 *
 * bedeutet, da� von allen Klassen dessen Name mit "ftraq." beginnt, nur Warnungen und Fehlermeldungen ausgegeben werden.
 *
 * <pre>
 * packageLoggingPriorities.put("ftraq.fs.ftp.FTPConnection", LoggerPriority.DEBUG);
 * </pre>
 * wuerde die Ausgabe von allen Logging-Ausgaben aus der Klasse FTPConnection aktivieren.
 * @version 1.0
 * @author fali
 * @see ftraq.util.LoggerPriority
 */
public class Logger {

    /**
     * Comparator for sorting a list of strings by length (long strings first)
     */
    private static java.util.Comparator stringLengthComparator = new java.util.Comparator() {

        public int compare(Object o1, Object o2) {
            try {
                String s1 = (String) o1;
                String s2 = (String) o2;
                return -(s1.length() - s2.length());
            } catch (Throwable t) {
                return -1;
            }
        }

        ;
    };

    /**
     * the class whose messages this class-specific instance of Logger logs
     */
    private Class loggedClass;

    /**
     * a map containing all class-specific instances of Logger as values, with the complete class name as key
     */
    private static java.util.HashMap allLoggersMap = new java.util.HashMap();

    /**
     * only messages with a priority >= this level will be logged by the class-specific instance of Logger
     */
    private int currentLogLevel;

    /**
     * the default priority for all classes that are not listed in the packageLoggingPriorities map
     */
    private static LoggerPriority defaultPriority = LoggerPriority.INFO;

    /**
     * a sorted map containing the rules for class/package-specific enabling or disabling of logging. To change the logging behaviour for a class or package, put a new element in this map with the class/package name as key and one of the predifned LoggerPriority objects as value.
     */
    private static java.util.SortedMap packageLoggingPriorities = new java.util.TreeMap(stringLengthComparator);

    static {
        packageLoggingPriorities.put("ftraq.", LoggerPriority.WARN);
        packageLoggingPriorities.put("ftraq.LgApplication", LoggerPriority.WARN);
        packageLoggingPriorities.put("ftraq.fs.ftp.service", LoggerPriority.WARN);
        packageLoggingPriorities.put("ftraq.transferqueue", LoggerPriority.WARN);
        packageLoggingPriorities.put("ftraq.transferqueue.LgTransferQueueItemImpl.DirectoryStructureAnalyzer", LoggerPriority.WARN);
        packageLoggingPriorities.put("ftraq.tests.", LoggerPriority.INFO);
    }

    /**
     * this constructor is private, so that you have to use the getInstance() method to create a Logger-Object. This was done to make sure that there aren't two Logger objects for the same class
     */
    private Logger(Class cls) {
        this.loggedClass = cls;
        this.currentLogLevel = this.getLogPriorityForClass(cls.getName()).toInt();
    }

    /**
     * gets the LoggerPriority that should be used for the Class i_className. If this method doesn't find a specific-rule in the packageLoggingPriorities-Map, it returns the defaultPriority.
     * @see LoggerPriority
     */
    private static LoggerPriority getLogPriorityForClass(String i_className) {
        java.util.Iterator it = Logger.packageLoggingPriorities.keySet().iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            if (i_className.startsWith(s)) {
                return (LoggerPriority) Logger.packageLoggingPriorities.get(s);
            }
        }
        return Logger.defaultPriority;
    }

    /**
     * use this method to check if debugging output is enabled for this class-specific instance of Logger.
     */
    private boolean isDebugEnabled() {
        return this.currentLogLevel <= LoggerPriority.DEBUG_INT;
    }

    /**
     * use this method to check if info output is enabled for this class-specific instance of Logger.
     */
    private boolean isInfoEnabled() {
        return this.currentLogLevel <= LoggerPriority.INFO_INT;
    }

    /**
     * creates a class-specific instance of Logger or returns the exsisting instance if it already exists
     */
    public static Logger getInstance(Class cls) {
        if (Logger.allLoggersMap.containsKey(cls)) {
            return (Logger) Logger.allLoggersMap.get(cls);
        }
        Logger logger = new Logger(cls);
        Logger.allLoggersMap.put(cls, logger);
        return logger;
    }

    /**
     * prints i_stringToBeLogged to System.err.
     */
    private static synchronized void writeToLog(String stringToBeLogged) {
        System.err.println(stringToBeLogged);
    }

    /**
     * returns the class name (without the package name) of the class which messages this class-specific instance of Logger logs
     */
    private String getLoggedClassName() {
        return this.loggedClass.getName().substring(this.loggedClass.getName().lastIndexOf('.') + 1);
    }

    /**
     * changes the logging priority of a class-specific instance
     */
    public void setPriority(LoggerPriority i_priority) {
        this.currentLogLevel = i_priority.toInt();
    }

    /**
     * prints a warning message to this logger if warning is enabled
     */
    public void warn(String i_stringToBeLogged) {
        if (this.currentLogLevel > LoggerPriority.WARN_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + i_stringToBeLogged);
    }

    /**
     * prints a message and the message of i_throwable to this logger if warning is enabled
     */
    public void warn(String i_stringToBeLogged, Throwable i_throwable) {
        if (this.currentLogLevel > LoggerPriority.WARN_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + i_stringToBeLogged + "\n" + i_throwable.getClass().getName() + ": " + i_throwable.getMessage());
    }

    /**
     * prints the fatal error message
     */
    public void fatal(String i_stringToBeLogged) {
        if (this.currentLogLevel > LoggerPriority.FATAL_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + i_stringToBeLogged);
    }

    /**
     * prints a message and the message of i_throwable to this logger if error logging isn't disabled
     */
    public void error(String i_stringToBeLogged, Throwable i_throwable) {
        if (this.currentLogLevel > LoggerPriority.ERROR_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + ": " + i_stringToBeLogged + "\n" + i_throwable.getClass().getName() + ": " + i_throwable.getMessage());
        i_throwable.printStackTrace();
    }

    /**
     * prints a message to this logger if error logging isn't disabled
     */
    public void error(String i_stringToBeLogged) {
        if (this.currentLogLevel > LoggerPriority.ERROR_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + i_stringToBeLogged);
    }

    /**
     * prints a message to this logger if info logging is enabled
     */
    public void info(String i_stringToBeLogged) {
        if (this.currentLogLevel > LoggerPriority.INFO_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + i_stringToBeLogged);
    }

    /**
     * prints a message to this logger if debug logging is enabled
     */
    public void debug(String i_stringToBeLogged) {
        if (this.currentLogLevel > LoggerPriority.DEBUG_INT) return;
        this.writeToLog(Thread.currentThread().getName() + ": " + this.getLoggedClassName() + ": " + i_stringToBeLogged);
    }
}
