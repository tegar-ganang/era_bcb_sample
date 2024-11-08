package net.sf.hdkp.wow;

import java.io.*;
import net.sf.hdkp.messages.MessageReceiver;

public class WindowsGameFinder extends GenericGameFinder {

    private static final String REG_EXEC = "reg query ";

    private static final String REG_KEY = "\"HKLM\\SOFTWARE\\Blizzard Entertainment\\World of Warcraft\"";

    private static final String REGSTR_TOKEN = "REG_SZ";

    private static final String DEFAULT_LAUNCHER = "Launcher.exe";

    private String installPath;

    private String gamePath;

    public WindowsGameFinder(MessageReceiver receiver) {
        super(receiver);
    }

    @Override
    public synchronized String getGamePath() {
        if (this.gamePath == null) {
            this.gamePath = getKey("GamePath");
        }
        return this.gamePath;
    }

    @Override
    public synchronized String getInstallPath() {
        if (this.installPath == null) {
            this.installPath = getKey("InstallPath");
        }
        return this.installPath;
    }

    @Override
    public String getGamePathForInstallPath(String installPath) {
        if (installPath == null) {
            return null;
        }
        final File file = new File(installPath, DEFAULT_LAUNCHER);
        if (file.isFile()) {
            return file.getAbsolutePath();
        }
        return null;
    }

    @Override
    public boolean isValidGamePath(String gamePath) {
        if (gamePath == null) {
            return false;
        }
        return new File(gamePath).isFile();
    }

    private String getKey(String value) {
        try {
            Process process = Runtime.getRuntime().exec(REG_EXEC + REG_KEY + " /v \"" + value + "\"");
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            debug("Registry.getKey({0})=[{1}]", value, stripNewlines(result));
            final int p = result.indexOf(REGSTR_TOKEN);
            if (p == -1) {
                error("Could not parse registry data for [{0}]\n" + "Registry key [{1}].\n" + "Data read: [{2}].", value, REG_KEY, result);
                return null;
            }
            return result.substring(p + REGSTR_TOKEN.length()).trim();
        } catch (Exception e) {
            error("Could not parse registry data for [{0}].\nRegistry key [{1}].", value, REG_KEY, e);
            return null;
        }
    }

    private String stripNewlines(String data) {
        return data == null ? "" : data.replaceAll("\\r", " ").replaceAll("\\n", " ");
    }

    private static class StreamReader extends Thread {

        private final InputStream is;

        private final StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        @Override
        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) sw.write(c);
            } catch (IOException e) {
                ;
            }
        }

        String getResult() {
            return sw.toString();
        }
    }
}
