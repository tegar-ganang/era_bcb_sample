package com.luzan.tool.nfs;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import com.luzan.common.nfs.XFile;
import com.sun.xfile.XFileOutputStream;
import com.sun.xfile.XFileInputStream;
import java.io.IOException;

/**
 * Command
 *
 * @author Alexander Bondar
 */
public class Command {

    private static void usage() {
        System.out.print("Commands:\n");
        System.out.print("\tdelete <filepath>\n");
        System.out.print("\tcopy <path from> <path to> [mime]\n");
        System.exit(1);
    }

    private static String getParameter(String[] args, int i, String defaultValue) {
        if (args.length < i) return defaultValue;
        return args[i];
    }

    private static String getParameter(String[] args, int i) {
        if (args.length < i) usage();
        return args[i];
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.protocol.xfile", "com.luzan.common.nfs");
        if (args.length < 1) usage();
        final String cmd = args[0];
        if ("delete".equalsIgnoreCase(cmd)) {
            final String path = getParameter(args, 1);
            XFile xfile = new XFile(path);
            if (!xfile.exists()) {
                System.out.print("File doean't exist.\n");
                System.exit(1);
            }
            xfile.delete();
        } else if ("copy".equalsIgnoreCase(cmd)) {
            final String pathFrom = getParameter(args, 1);
            final String pathTo = getParameter(args, 2);
            final XFile xfileFrom = new XFile(pathFrom);
            final XFile xfileTo = new XFile(pathTo);
            if (!xfileFrom.exists()) {
                System.out.print("File doesn't exist.\n");
                System.exit(1);
            }
            final String mime = getParameter(args, 3, null);
            final XFileInputStream in = new XFileInputStream(xfileFrom);
            final XFileOutputStream xout = new XFileOutputStream(xfileTo);
            if (!StringUtils.isEmpty(mime)) {
                final com.luzan.common.nfs.s3.XFileExtensionAccessor xfa = ((com.luzan.common.nfs.s3.XFileExtensionAccessor) xfileTo.getExtensionAccessor());
                if (xfa != null) {
                    xfa.setMimeType(mime);
                    xfa.setContentLength(xfileFrom.length());
                }
            }
            IOUtils.copy(in, xout);
            xout.flush();
            xout.close();
            in.close();
        }
    }
}
