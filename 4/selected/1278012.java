package help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import console.IOBuffer;

/**
 *
 * @author Michael Hanns
 *
 */
public class ShellHelp extends Help {

    public ShellHelp(IOBuffer terminal) {
        term = terminal;
        helpRead = false;
        readingText = false;
        textField = "";
        entries = new ArrayList<HelpEntry>();
        String fileName = new File(System.getProperty("user.dir")) + "";
        helpLocation = fileName + "/help/shellhelp.txt";
        readHelpFiles();
    }

    @Override
    public String query(String query) {
        if (helpRead) {
            String helpText = "\nYou searched for:\n".concat(query.concat("\n\n"));
            boolean match = false;
            for (int x = 0; x < entries.size() && !match; x++) {
                if (entries.get(x).matchesSearch(query)) {
                    helpText = helpText.concat(entries.get(x).getEntry());
                    match = true;
                }
            }
            if (!match) {
                return helpText + "Your search returned no results.";
            }
            return helpText;
        } else {
            return "\nERROR: Shell help is not available.";
        }
    }

    @Override
    protected void readHelpFiles() {
        File helpFile = new File(helpLocation);
        if (helpFile.exists()) {
            term.writeTo("\nLoading shell help...");
            try {
                FileReader reader = new FileReader(helpFile);
                BufferedReader in = new BufferedReader(reader);
                String line;
                StringTokenizer lntkns;
                HelpEntry entry = null;
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) {
                        entry = readLine(entry);
                    } else {
                        lntkns = new StringTokenizer(line);
                        if (lntkns.hasMoreTokens()) {
                            entry = readLine(lntkns, entry);
                        }
                    }
                }
                entries.add(entry);
                term.writeTo("\nFinished reading " + entries.size() + " entries.");
                helpRead = true;
                reader.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            term.writeTo("\nERROR: Shell help file not found at " + helpLocation + ".");
        }
    }
}
