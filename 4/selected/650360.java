package org.darkimport.omeglespy_z;

import java.io.PrintStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.darkimport.omeglespy$z.ChatHistoryHelper;

final class CLChatHistoryHelper extends ChatHistoryHelper {

    private int longestLabelLength = 0;

    private static final int SCREEN_WIDTH = 80;

    private static final String SEPARATOR = ": ";

    private final PrintStream out = System.out;

    @Override
    protected void doPrintLabelledMessage(final String label, final String message) {
        if (label != null && label.length() > longestLabelLength) {
            longestLabelLength = label.length();
        }
        final String printedLabel = StringUtils.leftPad(label != null ? label : StringUtils.EMPTY, longestLabelLength, ' ');
        final String rawPrintedMessage = WordUtils.wrap(message, SCREEN_WIDTH - SEPARATOR.length() - longestLabelLength, "|", true);
        final String[] printedMessageLines = rawPrintedMessage.split("\\|");
        final StringBuffer firstLine = new StringBuffer(printedLabel).append(SEPARATOR).append(printedMessageLines[0]);
        out.println(firstLine);
        if (printedMessageLines.length > 0) {
            final String leftPadding = StringUtils.leftPad(StringUtils.EMPTY, SEPARATOR.length() + longestLabelLength, ' ');
            for (int i = 1; i < printedMessageLines.length; i++) {
                final StringBuffer nextLine = new StringBuffer(leftPadding).append(printedMessageLines[i]);
                out.println(nextLine);
            }
        }
    }

    @Override
    protected void doPrintStatusMessage(final String message) {
        out.println(WordUtils.wrap(message, SCREEN_WIDTH, null, true));
    }

    /**
	 * @return the out
	 */
    public PrintStream getOut() {
        return out;
    }
}
