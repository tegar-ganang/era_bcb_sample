package raptor.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import raptor.Raptor;
import raptor.connector.Connector;
import raptor.connector.bics.BicsConnector;
import raptor.connector.fics.FicsConnector;
import raptor.pref.PreferenceKeys;
import raptor.service.ThreadService;
import raptor.util.RaptorLogger;
import raptor.util.RaptorRunnable;
import raptor.util.RaptorStringTokenizer;

/**
 * Logs chat messages to a file and provides, and allows a
 * ChatEventParseListener to parse the file.
 * 
 * This is being used to add old tells to a newly created Channel or Person tab.
 * 
 */
public class ChatLogger {

    public static interface ChatEventParseListener {

        /**
		 * Invoked on each chat event encountered in the file. Returns true if
		 * the parse should continue, false if it should cease.
		 */
        public boolean onNewEventParsed(ChatEvent event);

        public void onParseCompleted();
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static final RaptorLogger LOG = RaptorLogger.getLog(ChatLogger.class);

    protected String pathToFile;

    protected Connector connector;

    /**
	 * Constructs a ChatLogger which writes to the specified file. Deletes the
	 * file if it already exists.
	 * 
	 * @param connector
	 *            The connector
	 * @param pathToFile
	 *            The path to the backing file.
	 */
    public ChatLogger(Connector connector, String pathToFile) {
        this(connector, pathToFile, true);
    }

    /**
	 * Constructs a ChatLogger which writes to the specified file.
	 * 
	 * @param connector
	 *            The connector
	 * @param pathToFile
	 *            The path to the backing file.
	 * @param isDeleting
	 *            True if an existing file should be deleted, false otherwise.
	 */
    public ChatLogger(Connector connector, String pathToFile, boolean isDeleting) {
        this.pathToFile = pathToFile;
        if (isDeleting) {
            delete();
        }
        this.connector = connector;
    }

    /**
	 * Creates a read only chat logger.
	 * 
	 * @param pathToFile
	 *            Path to log file.
	 * @param isDeleting
	 *            True if the file should be deleted if it exists, false
	 *            otherwise.
	 */
    public ChatLogger(String pathToFile, boolean isDeleting) {
        this.pathToFile = pathToFile;
        if (isDeleting) {
            delete();
        }
    }

    /**
	 * Deletes the backing file.
	 */
    public void delete() {
        File file = new File(pathToFile);
        file.delete();
    }

    /**
	 * Parses the ChatLogger and invokes the listener on each chat event
	 * encountered.
	 */
    public void parseFile(ChatEventParseListener listener) {
        synchronized (this) {
            @SuppressWarnings("unused") long startTime = System.currentTimeMillis();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(pathToFile));
                String currentLine = reader.readLine();
                while (currentLine != null) {
                    try {
                        ChatEvent event = ChatEventUtils.deserializeChatEvent(currentLine);
                        if (!listener.onNewEventParsed(event)) {
                            break;
                        }
                    } catch (Throwable t) {
                        LOG.warn("Error reading chat event line " + currentLine + " skipping ChatEvent", t);
                    }
                    currentLine = reader.readLine();
                }
                listener.onParseCompleted();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable t) {
                    }
                }
            }
        }
    }

    protected boolean vetoWrite(ChatEvent event) {
        return event.getType() == ChatType.GAMES || event.getType() == ChatType.BUGWHO_ALL || event.getType() == ChatType.BUGWHO_AVAILABLE_TEAMS || event.getType() == ChatType.BUGWHO_UNPARTNERED_BUGGERS || event.getType() == ChatType.QTELL || event.getType() == ChatType.SEEKS;
    }

    /**
	 * Writes a chat even to this chat logger. Also appends the chat event to
	 * the configured loggers in the users preferences.
	 */
    public void write(ChatEvent event) {
        if (vetoWrite(event)) {
            return;
        }
        synchronized (this) {
            writeToLogFiles(event);
            if (event.getMessage().length() < 1500) {
                @SuppressWarnings("unused") long startTime = System.currentTimeMillis();
                FileWriter writer = null;
                try {
                    writer = new FileWriter(pathToFile, true);
                    writer.write(ChatEventUtils.serializeChatEvent(event) + "\n");
                } catch (Throwable t) {
                    LOG.warn("Error occured writihg chat event: ", t);
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Throwable t) {
                        }
                    }
                }
            }
        }
    }

    /**
	 * Returns the connector short name.
	 */
    public String getConnectorType() {
        if (connector == null) {
            return "unknown";
        }
        if (connector instanceof FicsConnector) {
            return "fics";
        } else if (connector instanceof BicsConnector) {
            return "bics";
        } else {
            throw new IllegalStateException("Unknown Connector type: " + connector);
        }
    }

    /**
	 * Writes the chat event to all log files specified in the Preferences.
	 * 
	 * @param event
	 *            The event to log.
	 */
    protected void writeToLogFiles(final ChatEvent event) {
        ThreadService.getInstance().run(new RaptorRunnable() {

            @Override
            public void execute() {
                if (Raptor.getInstance().getPreferences().getBoolean(PreferenceKeys.APP_IS_LOGGING_CONSOLE) && !vetoLogging(event.getSource())) {
                    appendToFile(Raptor.USER_RAPTOR_HOME_PATH + "/logs/console/" + getConnectorType() + "-console.txt", event);
                }
                if (Raptor.getInstance().getPreferences().getBoolean(PreferenceKeys.APP_IS_LOGGING_CHANNEL_TELLS) && event.getType() == ChatType.CHANNEL_TELL) {
                    appendToFile(Raptor.USER_RAPTOR_HOME_PATH + "/logs/console/" + getConnectorType() + "-" + event.getChannel() + ".txt", event);
                }
                if (Raptor.getInstance().getPreferences().getBoolean(PreferenceKeys.APP_IS_LOGGING_PERSON_TELLS) && event.getType() == ChatType.TELL && !vetoLogging(event.getSource())) {
                    appendToFile(Raptor.USER_RAPTOR_HOME_PATH + "/logs/console/" + getConnectorType() + "-" + event.getSource().toLowerCase() + ".txt", event);
                }
                if (Raptor.getInstance().getPreferences().getBoolean(PreferenceKeys.APP_IS_LOGGING_PERSON_TELLS) && event.getType() == ChatType.OUTBOUND) {
                    RaptorStringTokenizer tok = new RaptorStringTokenizer(event.getMessage(), " ", true);
                    String firstWord = tok.nextToken();
                    String secondWord = tok.nextToken();
                    if (firstWord != null && secondWord != null) {
                        if ("tell".startsWith(firstWord.toLowerCase()) && !vetoLogging(secondWord)) {
                            try {
                                Integer.parseInt(secondWord);
                            } catch (NumberFormatException nfe) {
                                appendToFile(Raptor.USER_RAPTOR_HOME_PATH + "/logs/" + getConnectorType() + "-" + secondWord.toLowerCase() + ".txt", event);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
	 * Returns true if logging should be vetoed. Used to filter out bot tells
	 * 
	 * @param userName
	 *            The user.
	 * @return The result.
	 */
    protected boolean vetoLogging(String userName) {
        return userName != null && (userName.equalsIgnoreCase("gamebot") || userName.equalsIgnoreCase("notesbot") || userName.equalsIgnoreCase("problembot") || userName.equalsIgnoreCase("endgamebot"));
    }

    /**
	 * Appends the chat event to the specified file.
	 * 
	 * @param fileName
	 *            The file name.
	 * @param event
	 *            The chat event.
	 */
    protected void appendToFile(String fileName, ChatEvent event) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(fileName, true);
            fileWriter.append("[").append(DATE_FORMAT.format(new Date(event.time))).append("] ").append(event.getMessage()).append("\n");
            fileWriter.flush();
        } catch (IOException ioe) {
            Raptor.getInstance().onError("Error occured writing to file: " + fileName, ioe);
        } finally {
            try {
                fileWriter.close();
            } catch (Throwable t) {
            }
        }
    }
}
