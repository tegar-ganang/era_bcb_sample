package net.sourceforge.strategema.ui;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Interaction through the standard input, output and error streams.
 * 
 * @author Lizzy
 * 
 */
public class ConsoleInteraction extends Interaction {

    /** Logger */
    private static final Logger LOG = Logger.getLogger(ConsoleInteraction.class.getName());

    /** Buffered handle to the standard input stream. */
    private static final BufferedReader STDIN = new BufferedReader(new InputStreamReader(System.in));

    /** Singleton instance */
    public static final Interaction CONSOLE = new ConsoleInteraction();

    /** Private constructor */
    private ConsoleInteraction() {
        super();
    }

    @Override
    public void attention() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
	 * Converts a message type to a string.
	 * @param type The type of message from {@link JOptionPane}.
	 * @return A string representing the message type that can be used as a suitable prefix for the message.
	 */
    private static String prefix(final int type) {
        switch(type) {
            case JOptionPane.ERROR_MESSAGE:
                return "Error: ";
            case JOptionPane.WARNING_MESSAGE:
                return "Warning: ";
            default:
                return "";
        }
    }

    @Override
    public synchronized void alert(final String msg, final int type) {
        final String prefix = prefix(type);
        if (type == JOptionPane.ERROR_MESSAGE || type == JOptionPane.WARNING_MESSAGE) {
            System.err.println(prefix);
            System.err.println(msg);
            if (type == JOptionPane.ERROR_MESSAGE && System.console() != null) {
                System.err.flush();
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            System.out.println(prefix);
            System.out.println(msg);
        }
    }

    @Override
    public synchronized Object choice(final String caption, final String prompt, final int type, final Object defaultOption, final Option... options) {
        String entry = null;
        boolean selected = false;
        boolean allowAny = false;
        Object optionSelected = defaultOption;
        final PrintWriter output;
        final Console console = System.console();
        if (console != null) {
            output = console.writer();
        } else {
            output = new PrintWriter(System.out);
        }
        do {
            output.print(prefix(type));
            output.print(prompt);
            output.print(" ");
            for (int i = 0; i < options.length; i++) {
                if (options[i].caseSensitive) {
                    output.print(options[i].entryText);
                } else {
                    output.print(options[i].entryText.toLowerCase());
                }
                if (i < options.length - 1) output.print("/");
            }
            output.print("? ");
            output.flush();
            try {
                entry = STDIN.readLine();
                output.println();
            } catch (final IOException e) {
                output.println();
                LOG.warning(e.toString());
            }
            if (entry == null) return defaultOption;
            entry = entry.trim();
            for (final Option option : options) {
                if (option == null) {
                    allowAny = true;
                } else if ((option.caseSensitive && entry.equals(option.entryText)) || (!option.caseSensitive && entry.equalsIgnoreCase(option.entryText))) {
                    optionSelected = option.value;
                    selected = true;
                }
            }
            if (allowAny && !entry.equals("")) {
                optionSelected = entry;
                selected = true;
            } else if (defaultOption != null && entry.equals("")) {
                selected = true;
            }
        } while (!selected);
        return optionSelected;
    }

    @Override
    public synchronized File chooseFile(final int operation, final File initialDir, final javax.swing.filechooser.FileFilter... filters) {
        final UserExperience xp = UserExperience.getUserExperience();
        final FileFilter[] adaptedFilters;
        if (filters == null || filters.length == 0) {
            adaptedFilters = new FileFilter[1];
        } else {
            adaptedFilters = new FileFilter[filters.length];
            for (int i = 0; i < filters.length; i++) {
                if (filters[i] == null || filters[i] instanceof FileFilter) {
                    adaptedFilters[i] = (FileFilter) filters[i];
                } else {
                    adaptedFilters[i] = new FileFilterAdapter(filters[i]);
                }
            }
        }
        final PrintWriter output;
        final Console console = System.console();
        if (console != null) {
            output = console.writer();
        } else {
            output = null;
        }
        File selectedFile = null;
        File currentDir = initialDir;
        if (currentDir == null) {
            try {
                currentDir = new File(System.getProperty("user.home"));
            } catch (final SecurityException e) {
                currentDir = new File("");
            }
        } else {
            try {
                while (currentDir != null && !currentDir.isDirectory()) {
                    currentDir = currentDir.getParentFile();
                }
            } catch (final SecurityException e) {
                LOG.info(e.toString());
            }
        }
        final SortedSet<String> filenames = new TreeSet<String>();
        final SortedSet<String> dirnames = new TreeSet<String>();
        boolean gotDirname = true;
        while (selectedFile == null && gotDirname) {
            if (output != null) {
                if (currentDir == null) {
                    output.println("Root locations");
                } else {
                    try {
                        currentDir = currentDir.getCanonicalFile();
                    } catch (final IOException e) {
                        try {
                            currentDir = currentDir.getAbsoluteFile();
                        } catch (final SecurityException e2) {
                            LOG.info(e2.toString());
                        }
                    } catch (final SecurityException e) {
                        LOG.info(e.toString());
                    }
                    if (currentDir != null) {
                        output.println("Directory of " + currentDir.toString());
                    }
                }
                output.println();
            }
            if (currentDir != null) {
                try {
                    for (final FileFilter filter : adaptedFilters) {
                        final File[] files = currentDir.listFiles(filter);
                        if (files != null) {
                            for (final File file : files) {
                                try {
                                    if (!file.isHidden()) {
                                        if (file.isDirectory()) {
                                            dirnames.add(file.getName());
                                        } else if (file.exists()) {
                                            filenames.add(file.getName());
                                        }
                                    }
                                } catch (final SecurityException e) {
                                    dirnames.add(file.getName());
                                }
                            }
                        } else {
                            System.err.println("I/O error.");
                        }
                    }
                } catch (final SecurityException e) {
                    if (output != null) output.println("Access denied.");
                }
                if (currentDir.getParent() != null) {
                    dirnames.add("..");
                } else {
                    final File[] roots = File.listRoots();
                    if (roots != null && roots.length > 1) {
                        dirnames.add("..");
                    }
                }
            } else {
                final File[] roots = File.listRoots();
                if (roots != null) {
                    for (final File root : roots) {
                        try {
                            if (!root.isHidden()) {
                                dirnames.add(root.getPath());
                            }
                        } catch (final SecurityException e) {
                            dirnames.add(root.getPath());
                        }
                    }
                }
            }
            if (output != null) {
                int i = 0;
                final StringBuilder line = new StringBuilder(80);
                for (final String dirname : dirnames) {
                    if (i % 2 == 1 && dirname.length() > 37) {
                        output.println();
                        i++;
                    }
                    line.append("+");
                    line.append(String.format("%1$#-37s", dirname));
                    line.append("  ");
                    i++;
                    if (i % 2 == 0 || dirname.length() > 37) {
                        output.println(line);
                        line.delete(0, line.length());
                        if (i % 2 == 1) i++;
                    }
                }
                for (final String filename : filenames) {
                    if (i % 2 == 1 && filename.length() > 38) {
                        output.println();
                        i++;
                    }
                    line.append(" ");
                    line.append(String.format("%1$#-38s", filename));
                    line.append(" ");
                    i++;
                    if (i % 2 == 0 || filename.length() > 38) {
                        output.println(line);
                        line.delete(0, line.length());
                        if (i % 2 == 1) i++;
                    }
                }
                if (i % 2 == 1) {
                    output.println(line);
                }
                output.println();
                output.flush();
            }
            gotDirname = false;
            do {
                System.err.flush();
                final String filename = this.input("", "Filename:", JOptionPane.PLAIN_MESSAGE);
                if (filename.equals("")) {
                    System.out.println("Cancelled.");
                    return null;
                }
                if (currentDir != null && filename.equals("..")) {
                    currentDir = currentDir.getParentFile();
                    gotDirname = true;
                } else {
                    String adjustedFilename = xp.getFilename(filename, null);
                    File chosenFile = new File(adjustedFilename);
                    if (!chosenFile.isAbsolute()) {
                        chosenFile = new File(currentDir, adjustedFilename);
                    }
                    boolean isExistingDir;
                    try {
                        isExistingDir = chosenFile.isDirectory();
                    } catch (final SecurityException e) {
                        isExistingDir = adjustedFilename.endsWith(File.separator);
                    }
                    if (isExistingDir) {
                        currentDir = chosenFile;
                        gotDirname = true;
                    } else if (adjustedFilename.endsWith(File.separator)) {
                        if (!chosenFile.exists()) {
                            final Confirmation confirm = this.confirm("", chosenFile.getPath() + " does not exist.  Create directory", JOptionPane.WARNING_MESSAGE);
                            if (confirm == Confirmation.YES) {
                                try {
                                    final boolean success = chosenFile.mkdirs();
                                    if (!success) {
                                        System.err.println("Failed.");
                                        try {
                                            do {
                                                chosenFile = chosenFile.getParentFile();
                                            } while (chosenFile != null && !chosenFile.isDirectory());
                                        } catch (final SecurityException e) {
                                            LOG.info(e.toString());
                                        }
                                        gotDirname = true;
                                    } else {
                                        currentDir = chosenFile;
                                        gotDirname = true;
                                    }
                                } catch (final SecurityException e) {
                                    System.err.println("Access denied.");
                                }
                            }
                        } else {
                            System.err.println(chosenFile.getName() + " is a file and not a directory.");
                        }
                    } else {
                        if (filters != null && filters.length > 0) {
                            boolean foundFilter = false;
                            for (final javax.swing.filechooser.FileFilter filter : filters) {
                                if (filter.accept(chosenFile)) {
                                    adjustedFilename = xp.getFilename(filename, filter);
                                    foundFilter = true;
                                    break;
                                }
                            }
                            if (!foundFilter) {
                                adjustedFilename = xp.getFilename(filename, filters[0]);
                            }
                            chosenFile = new File(adjustedFilename);
                            if (!chosenFile.isAbsolute()) {
                                chosenFile = new File(currentDir, adjustedFilename);
                            }
                        }
                        boolean canAccess;
                        if (operation == JFileChooser.OPEN_DIALOG) {
                            try {
                                canAccess = chosenFile.canRead();
                            } catch (final SecurityException e) {
                                canAccess = false;
                            }
                            if (!canAccess) {
                                System.err.println(adjustedFilename + " does not exist or cannot be accessed.");
                            } else {
                                selectedFile = chosenFile;
                            }
                        } else {
                            boolean exists;
                            try {
                                exists = chosenFile.exists();
                            } catch (final SecurityException e) {
                                exists = false;
                            }
                            final File parent = chosenFile.getParentFile();
                            boolean missingParent = false;
                            try {
                                missingParent = parent != null && !parent.isDirectory();
                                canAccess = (!exists && !missingParent) || chosenFile.canWrite();
                            } catch (final SecurityException e) {
                                canAccess = false;
                            }
                            if (canAccess) {
                                if (exists) {
                                    final Confirmation overwrite = this.confirm("", adjustedFilename + " already exists.  Overwrite", JOptionPane.WARNING_MESSAGE);
                                    if (overwrite == Confirmation.YES) {
                                        selectedFile = chosenFile;
                                    }
                                } else {
                                    selectedFile = chosenFile;
                                }
                            } else if (missingParent && parent != null) {
                                System.err.println(parent.toString() + " is not a valid directory.");
                            } else {
                                System.err.println("Access denied.");
                            }
                        }
                    }
                }
            } while (selectedFile == null && !gotDirname && output != null);
            dirnames.clear();
            filenames.clear();
        }
        return selectedFile;
    }

    @Override
    public synchronized void clear() {
        final Console console = System.console();
        if (console != null) {
            final PrintWriter writer = console.writer();
            writer.println(((char) 27) + "[2J");
            for (int i = 0; i <= 25; i++) {
                writer.println();
            }
            console.flush();
        }
    }

    @Override
    public synchronized Confirmation confirm(final String caption, final String prompt, final boolean rememberOption, final int type) {
        final Reader stdInUnbuf = new InputStreamReader(System.in);
        Confirmation result = null;
        int ch;
        char[] chars;
        final PrintWriter output;
        final Console console = System.console();
        if (console != null) {
            output = console.writer();
        } else {
            output = new PrintWriter(System.out);
        }
        while (result == null) {
            output.print(prefix(type));
            output.print(prompt);
            if (rememberOption) {
                output.print(" (y/n/c/A(lways)/N(ever)? ");
            } else {
                output.print(" (y/n/c)? ");
            }
            output.flush();
            try {
                ch = stdInUnbuf.read();
                output.println();
            } catch (final IOException e) {
                output.println();
                LOG.warning(e.toString());
                ch = -1;
            }
            if (ch == -1) return Confirmation.CANCEL;
            chars = Character.toChars(ch);
            if (chars[0] == 'y' || chars[0] == 'Y') {
                result = Confirmation.YES;
            } else if (chars[0] == 'n' || (!rememberOption && chars[0] == 'N')) {
                result = Confirmation.NO;
            } else if (chars[0] == 'c' || chars[0] == 'C') {
                result = Confirmation.CANCEL;
            } else if (rememberOption && chars[0] == 'A') {
                result = Confirmation.ALWAYS;
            } else if (rememberOption && chars[0] == 'N') {
                result = Confirmation.NEVER;
            }
        }
        return result;
    }

    @Override
    public synchronized void error(final String err) {
        System.out.flush();
        System.err.println(err);
    }

    @Override
    public synchronized String input(final String caption, final String prompt, final int type) {
        final PrintWriter output;
        final Console console = System.console();
        if (console != null) {
            output = console.writer();
        } else {
            output = new PrintWriter(System.out);
        }
        output.print(prefix(type));
        output.print(prompt);
        output.print(' ');
        output.flush();
        String result = null;
        try {
            result = STDIN.readLine();
            output.println();
            return result;
        } catch (final IOException e) {
            output.println();
            LOG.warning(e.toString());
            return "";
        }
    }

    @Override
    public boolean isGraphical() {
        return false;
    }

    @Override
    public synchronized void output(final String out) {
        System.out.println(out);
    }

    @Override
    public synchronized char[] secureInput(final String caption, final String prompt, final int type) {
        final Console console = System.console();
        final char[] result;
        if (console == null) {
            System.out.print(prefix(type));
            System.out.println(prompt);
            char[] chars = new char[16];
            int length = 0;
            int ch = -1;
            try {
                do {
                    ch = STDIN.read();
                    if (ch != -1 && ch != 13 && ch != 10) {
                        length++;
                        if (length > chars.length) {
                            final char[] newChars = Arrays.copyOf(chars, chars.length * 2);
                            Arrays.fill(chars, (char) 0);
                            chars = newChars;
                        }
                        chars[length - 1] = (char) ch;
                    }
                } while (ch != -1 && ch != 13 && ch != 10);
            } catch (final IOException e) {
                LOG.warning(e.toString());
            }
            try {
                if (ch == 13 && STDIN.ready()) {
                    STDIN.mark(2);
                    ch = STDIN.read();
                    if (ch != -1 && ch != 10) {
                        STDIN.reset();
                    }
                }
            } catch (final IOException e) {
                LOG.warning(e.toString());
            }
            result = Arrays.copyOf(chars, length);
            Arrays.fill(chars, (char) 0);
            return result;
        } else {
            final PrintWriter writer = console.writer();
            writer.println(((char) 27) + "[2J");
            for (int i = 0; i < 25; i++) {
                writer.println();
            }
            writer.print(prefix(type));
            writer.print(prompt);
            writer.print(" ");
            console.flush();
            result = console.readPassword();
            if (result != null) {
                return result;
            } else {
                return new char[0];
            }
        }
    }
}
