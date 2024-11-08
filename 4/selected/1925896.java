package com.gargoylesoftware.base.trace;

import com.gargoylesoftware.base.util.StringUtil;
import java.io.PrintStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A trace writer specifically designed for the bash shell.  The
 * reason it requires "bash" is that is colour codes the tracing
 * using bash specific escape codes.
 *
 * @version $Revision: 1.3 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public class BashTraceWriter implements TraceWriter {

    private static String lastPrefix_ = "";

    private static final Format TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm");

    /**
     * Create a new writer.
     */
    public BashTraceWriter() {
    }

    /**
     * Write the specified item.
     *
     * @param item The TraceItem to be written.
     */
    public void write(final TraceItem item) {
        final PrintStream outStream = Trace.getController().getRealSystemOut();
        final StringBuffer prefixBuffer = new StringBuffer();
        prefixBuffer.append("\033[0;34m");
        final Date timestamp = item.getTime();
        if (timestamp != null) {
            prefixBuffer.append("Time=[");
            prefixBuffer.append(TIMESTAMP_FORMAT.format(timestamp));
            prefixBuffer.append("] ");
        }
        final String threadName = item.getThreadName();
        if (threadName != null) {
            prefixBuffer.append("Thread=[");
            prefixBuffer.append(threadName);
            prefixBuffer.append("] ");
        }
        prefixBuffer.append("Channel=[");
        prefixBuffer.append(item.getChannel().getName());
        prefixBuffer.append("]");
        prefixBuffer.append("\033[0m");
        final String prefix = prefixBuffer.toString();
        if (prefix.equals(lastPrefix_) == false) {
            lastPrefix_ = prefix;
            outStream.println("");
            outStream.println(prefix);
        }
        final String message = item.getMessage();
        if (message != null) {
            outStream.print("\033[0;34m>> \033[0m");
            outStream.print(StringUtil.expandTabs(message, 3));
        }
        final Throwable throwable = item.getThrowable();
        if (throwable != null) {
            int i;
            final String strings[] = Trace.throwableToStringArray(throwable);
            outStream.print("\033[0;34m>> \033[0m");
            outStream.println(strings[0]);
            for (i = 1; i < strings.length; i++) {
                outStream.println(StringUtil.expandTabs(strings[i], 3));
            }
        }
    }
}
