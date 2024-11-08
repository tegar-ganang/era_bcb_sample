package simpleterminal.builtins;

import java.io.*;
import simpleterminal.util.Platform;

/**
 *
 * @author sahaqiel
 */
public class CpBuiltin implements ShellBuiltin {

    private String input;

    @Override
    public String getCommandName() {
        return "cp";
    }

    @Override
    public int getMinParameterCount() {
        return 2;
    }

    @Override
    public String getOuput() {
        String[] args = input.split(" ");
        if (args.length == 2) {
            File file1 = new File(args[0]);
            File file2 = new File(args[1]);
            boolean ok = false;
            if (file1.exists()) {
                if (file2.isDirectory()) {
                    file2 = new File(file2.getAbsolutePath(), file1.getName());
                }
                ok = copyFile(file1, file2);
            }
            if (!ok) {
                return "Copy failed";
            }
        }
        return "";
    }

    @Override
    public void setInput(String input) {
        if (input != null) {
            String in = Platform.fixPath(input);
            if (in.startsWith(getCommandName())) {
                this.input = in.replaceFirst(getCommandName(), "").trim();
            } else {
                this.input = in.trim();
            }
        }
    }

    private boolean copyFile(File inFile, File outFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(inFile));
            writer = new BufferedWriter(new FileWriter(outFile));
            while (reader.ready()) {
                writer.write(reader.read());
            }
            writer.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    return false;
                }
            }
        }
        return true;
    }
}
