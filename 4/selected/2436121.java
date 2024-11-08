package org.shestkoff.common.helper;

import org.slf4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.shestkoff.common.log.Logging;

/**
 * Project: Maxifier
 * User: Shestkov Vasily
 * Date: 12.01.2009 13:33:21
 * <p/>
 * Copyright (c) 1999-2006 Magenta Corporation Ltd. All Rights Reserved.
 * Magenta Technology proprietary and confidential.
 * Use is subject to license terms.
 */
public final class StreamHelper {

    private static final Logger logger = Logging.getLogger(StreamHelper.class);

    public static String toString(InputStream stream) {
        StringBuilder res = new StringBuilder(2048);
        byte[] buffer = new byte[2048];
        try {
            int readed = 0;
            while ((readed = stream.read(buffer, 0, 2048)) > 0) {
                res.append(new String(buffer, 0, readed));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res.toString();
    }

    public static void copy(@NotNull final InputStream source, @NotNull final OutputStream destination) {
        try {
            byte[] buffer = new byte[2048];
            int readed = 0;
            while ((readed = source.read(buffer, 0, buffer.length)) > 0) {
                destination.write(buffer, 0, readed);
            }
        } catch (IOException e) {
            logger.error("IOException", e);
        } finally {
            try {
                source.close();
            } catch (IOException e) {
            }
            try {
                destination.close();
            } catch (IOException e) {
            }
        }
    }
}
