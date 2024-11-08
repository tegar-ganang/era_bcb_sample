package org.wportal.util;

import java.io.*;

/**
 * User: SimonLei
 * Date: 2004-10-10
 * Time: 13:37:06
 * $Id: CopyUtil.java,v 1.1 2004/10/19 01:50:02 echou Exp $
 */
public class CopyUtil {

    public static void copyInput2Output(InputStream in, OutputStream out) throws IOException {
        int read = 0;
        byte buffer[] = new byte[8192];
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    public static void copyInputReader2OutWriter(Reader reader, Writer writer) throws IOException {
        int read = 0;
        char buffer[] = new char[8192];
        while ((read = reader.read(buffer)) > -1) {
            writer.write(buffer, 0, read);
        }
        reader.close();
        writer.close();
    }
}
