package com.mentorgen.tools.profile.output;

import java.io.PrintWriter;
import com.mentorgen.tools.profile.Controller;
import com.mentorgen.tools.profile.runtime.Frame;

/**
 * 
 * @author Andrew Wilcox
 * @see com.mentorgen.tools.profile.output.ProfileTextDump
 */
final class FrameDump {

    private static final String NEW_LINE = System.getProperty("line.separator");

    static void dump(PrintWriter writer, Frame frame, int interaction) {
        if (Controller._compactThreadDepth) {
            if (!frame.hasChildren()) {
                return;
            }
            boolean childAboveThreshold = false;
            for (Frame child : frame.childIterator()) {
                if (!belowThreshold(child) || child.hasChildren()) {
                    childAboveThreshold = true;
                    break;
                }
            }
            if (!childAboveThreshold) {
                return;
            }
        }
        writer.print("+------------------------------");
        writer.print(NEW_LINE);
        writer.print("| Thread: ");
        writer.print(frame.getThreadId());
        if (interaction > 1) {
            writer.print(" (interaction #");
            writer.print(interaction);
            writer.print(")");
        }
        writer.print(NEW_LINE);
        writer.print("+------------------------------");
        writer.print(NEW_LINE);
        writer.print("              Time            Percent    ");
        writer.print(NEW_LINE);
        writer.print("       ----------------- ---------------");
        writer.print(NEW_LINE);
        writer.print(" Count    Total      Net   Total     Net  Location");
        writer.print(NEW_LINE);
        writer.print(" =====    =====      ===   =====     ===  =========");
        writer.print(NEW_LINE);
        long threadTotalTime = frame._metrics.getTotalTime();
        dump(writer, 0, frame, (double) threadTotalTime);
    }

    private static void dump(PrintWriter writer, int depth, Frame parent, double threadTotalTime) {
        long total = parent._metrics.getTotalTime();
        long net = parent.netTime();
        writer.printf("%6d ", parent._metrics.getCount());
        writer.printf("%8.1f ", Math.nanoToMilli(total));
        writer.printf("%8.1f ", Math.nanoToMilli(net));
        if (total > 0) {
            double percent = Math.toPercent(total, threadTotalTime);
            writer.printf("%7.1f ", percent);
        } else {
            writer.print("        ");
        }
        if (net > 0 && threadTotalTime > 0.000) {
            double percent = Math.toPercent(net, threadTotalTime);
            if (percent > 0.1) {
                writer.printf("%7.1f ", percent);
            } else {
                writer.print("        ");
            }
        } else {
            writer.print("        ");
        }
        writer.print(" ");
        for (int i = 0; i < depth; i++) {
            writer.print("| ");
        }
        writer.print("+--");
        writer.print(parent.getInvertedName());
        writer.print(NEW_LINE);
        if (Controller._threadDepth != Controller.UNLIMITED && depth == Controller._threadDepth - 1) {
            return;
        }
        for (Frame child : parent.childIterator()) {
            if (belowThreshold(child)) {
                continue;
            }
            dump(writer, depth + 1, child, threadTotalTime);
        }
    }

    private static boolean belowThreshold(Frame f) {
        return (Controller._compactThreadDepth && f._metrics.getTotalTime() < Controller._compactThreadThreshold * 1000000);
    }
}
