package org.eoti.io.console.shell.cmd;

import org.eoti.io.console.shell.ShellException;
import org.eoti.io.console.shell.ShellFileExtension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class ShellCat extends ShellFileExtension {

    public ShellCat() {
        super();
    }

    public void execute(List<String> arguments) throws ShellException {
        if (arguments.size() != 1) error("Incorrect format. Expected 'cat FILENAME'.");
        String path = arguments.get(0);
        File file = getFile(path);
        if (file == null) error("Unable to locate to '%s'.", path);
        if (file.isDirectory()) error("Unable to cat a directory.");
        if (!file.exists() || file.lastModified() == 0) error("File '%s' does not exist.", path);
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = "";
            int lineNumber = 1;
            while ((line = in.readLine()) != null) shell.writer().format("%d:  %s\n", lineNumber++, line);
            in.close();
        } catch (Exception e) {
            error(e, "Error reading file '%s'.", file.getName());
        }
    }

    public String getName() {
        return "cat";
    }

    public String getUsage() {
        return "'cat FILENAME' to display a file.";
    }
}
