package dproxy.application.ui.commands;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import dproxy.application.ui.CommandLineProcessor;
import dproxy.server.cache.bo.CacheBO;
import dproxy.server.cache.to.RequestResponse;

/**
 * List cache entryes.
 */
public class List implements Command {

    /** Lenght for padding. */
    private static final int FIVE_CHARACTERS = 10000;

    /** Lenght for padding. */
    private static final int FOUR_CHARACTERS = 1000;

    /** Lenght for padding. */
    private static final int THREE_CHARACTERS = 100;

    /** Lenght for padding. */
    private static final int TWO_CHARACTERS = 10;

    /** Sub-Command that shows how many entryes exist on cache. */
    private static final String SC_SIZE = "size";

    /** Sub-Command that shows the last N entryes from cache. */
    private static final String SC_LAST_N = "last";

    /** Sub-Command that shows data about threads. */
    private static final String THREADS = "threads";

    /** Date formatter. */
    private SimpleDateFormat sdf = new SimpleDateFormat();

    /** Sub command (command option). */
    private String subCommand;

    /** Number numeric parameter. */
    private int firstNumberParameter;

    /**
     * @see dproxy.application.ui.commands.Command#description()
     */
    public String description() {
        return "   'list'       - Show information about cache contents\n" + "                  - Options:\n" + "                    " + SC_SIZE + "         - show how many entryes exist on cache\n" + "                    " + SC_LAST_N + "         - show last entry from cache\n" + "                    " + SC_LAST_N + " N       - show last N entryes from cache\n" + "                    " + THREADS + "      - show information about running threads";
    }

    /**
     * @see dproxy.application.ui.commands.Command#validate(Collection)
     */
    public void validate(Collection<String> parameters) throws ValidationException {
        Iterator<String> itParameters = parameters.iterator();
        if (itParameters.hasNext()) {
            subCommand = itParameters.next();
            if (SC_SIZE.equals(subCommand)) {
                ValidationUtils.validateNoMoreParameters(itParameters);
            } else if (THREADS.equals(subCommand)) {
                ValidationUtils.validateNoMoreParameters(itParameters);
            } else if (SC_LAST_N.equals(subCommand)) {
                try {
                    firstNumberParameter = ValidationUtils.getNumberParameter(itParameters);
                    ValidationUtils.validateNoMoreParameters(itParameters);
                } catch (NoMoreParametersException nmpe) {
                    firstNumberParameter = 1;
                }
            } else {
                throw new ValidationException();
            }
        } else {
            throw new ValidationException();
        }
    }

    /**
     * @see dproxy.application.ui.commands.Command#confirm()
     */
    public boolean confirm() {
        return true;
    }

    /**
     * @see dproxy.application.ui.commands.Command#execute()
     */
    public void execute() {
        if (SC_SIZE.equals(subCommand)) {
            int cacheSize = CacheBO.getInstance().getCacheSize();
            CommandLineProcessor.getInstance().write("Cache contains " + cacheSize + " entryes");
        } else if (THREADS.equals(subCommand)) {
            CommandLineProcessor.getInstance().write("The following application threads are alive:");
            Map<Thread, StackTraceElement[]> threadMaps = Thread.getAllStackTraces();
            for (Thread thread : threadMaps.keySet()) {
                if (!thread.isDaemon() && thread.isAlive()) {
                    CommandLineProcessor.getInstance().write("   " + thread.getName() + " [" + thread.getState() + "]");
                }
            }
        } else if (SC_LAST_N.equals(subCommand)) {
            listLastCommand();
        }
    }

    /**
     * Refactoring of "List last n" command execution.
     */
    private void listLastCommand() {
        String markBegin = "[";
        String markEnd = "]";
        Collection<RequestResponse> rrs = CacheBO.getInstance().listLast(firstNumberParameter);
        if (rrs.size() == 0) {
            CommandLineProcessor.getInstance().write("Cache contains no entryes");
        } else if (rrs.size() == 1) {
            CommandLineProcessor.getInstance().write("Last entry:");
        } else {
            CommandLineProcessor.getInstance().write("Last " + rrs.size() + " entryes:");
        }
        String header = markBegin + "Index" + markEnd;
        header += "\t";
        header += markBegin + "Date           " + markEnd;
        CommandLineProcessor.getInstance().write(header);
        for (RequestResponse rr : rrs) {
            int index = rr.getIndex();
            String padding;
            if (index < TWO_CHARACTERS) {
                padding = "    ";
            } else if (index < THREE_CHARACTERS) {
                padding = "   ";
            } else if (index < FOUR_CHARACTERS) {
                padding = "  ";
            } else if (index < FIVE_CHARACTERS) {
                padding = " ";
            } else {
                padding = "";
            }
            StringBuffer line = new StringBuffer();
            line.append(markBegin);
            line.append(index);
            line.append(padding);
            line.append(markEnd);
            line.append("\t");
            line.append(markBegin);
            line.append(sdf.format(rr.getDate()));
            line.append(markEnd);
            CommandLineProcessor.getInstance().write(line.toString());
        }
    }
}
