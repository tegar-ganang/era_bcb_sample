package org.nomadpim.core.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.eclipse.core.runtime.Platform;
import org.nomadpim.core.ICoreConstants;
import org.nomadpim.core.internal.plugin.PlatformLocationFilenameResolver;

public class RemoveEscapeCharactersUpdater implements IUpdater {

    private static final String ENCODING = "UTF-8";

    private final String filename;

    public RemoveEscapeCharactersUpdater(final String filename) {
        this.filename = filename;
    }

    private File getFile(String filename) {
        return PlatformLocationFilenameResolver.getWorkspaceFile(filename);
    }

    private String read(File file) throws UpdateException {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), ENCODING);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4 * 1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, bytesRead);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new UpdateException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    ICoreConstants.LOG.error(e);
                }
            }
        }
    }

    public void update() throws UpdateException {
        File file = getFile(filename);
        if (!file.exists()) {
            return;
        }
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            write(file, read(file).replaceAll("&#xD;", "\n"));
        } else {
            write(file, read(file).replaceAll("&#xD;", ""));
        }
    }

    private void write(File file, String newContent) throws UpdateException {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), ENCODING);
            writer.write(newContent.toCharArray());
        } catch (Exception e) {
            throw new UpdateException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    ICoreConstants.LOG.error(e);
                }
            }
        }
    }
}
