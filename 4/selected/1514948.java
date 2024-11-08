package be.lassi.web;

import static be.lassi.util.Util.newArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 *
 *
 */
public abstract class DocBuilder extends PageBuilder {

    /**
     * The state of building a web page.
     */
    private enum State {

        IDLE, COPYING
    }

    private State state = State.IDLE;

    /**
     * The directory that will contain the Javadoc html pages including
     * the LASSI navigation.
     */
    private final String targetDir;

    private final List<String> excludes = newArrayList();

    /**
     * Constructs a new instance.
     *
     * @param sourceDir the directory with the html pages to be changed
     * @param targetDir the directory with the html pages after change
     */
    protected DocBuilder(final String sourceDir, final String targetDir) {
        super(sourceDir);
        this.targetDir = targetDir;
    }

    /**
     * Adds a file to the list of files that should not get the logo and
     * the navigation bar at the top.
     *
     * @param filename the name of the file to be added to the list
     */
    protected void exclude(final String filename) {
        excludes.add(filename);
    }

    /**
     * Processes the files.
     *
     * @throws IOException if problem with reading or writing the files
     */
    public void process() throws IOException {
        File root = new File(getSourceDir());
        processDir(root);
    }

    /**
     * Recursively processes given directory and all its subdirectories.
     *
     * @param dir the directory to be processed
     * @throws IOException if problem with reading or writing the files
     */
    private void processDir(final File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
            if (!file.getName().contains(".svn")) {
                if (file.isDirectory()) {
                    new File(targetFilename(file)).mkdir();
                    processDir(file);
                } else if (shouldProcess(file)) {
                    processHtml(file);
                } else {
                    copy(file);
                }
            }
        }
    }

    /**
     * Copies file without change.
     *
     * @param file the file to be copied
     * @throws IOException if problem with reading or writing the files
     */
    private void copy(final File file) throws IOException {
        String targetFilename = targetFilename(file);
        FileInputStream fis = new FileInputStream(file);
        try {
            FileChannel source = fis.getChannel();
            try {
                FileOutputStream fos = new FileOutputStream(targetFilename);
                try {
                    FileChannel target = fos.getChannel();
                    try {
                        target.transferFrom(source, 0, source.size());
                    } finally {
                        target.close();
                    }
                } finally {
                    fos.close();
                }
            } finally {
                source.close();
            }
        } finally {
            fis.close();
        }
    }

    /**
     * Processes given html file.
     *
     * @param file
     *            the html file to be processed
     * @throws IOException
     *             if problem with reading or writing the files
     */
    private void processHtml(final File file) throws IOException {
        state = State.IDLE;
        String filename = targetFilename(file);
        LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(file.getAbsolutePath())));
        try {
            PrintWriter out = new PrintWriter(filename);
            try {
                Page page = new Page(out);
                int depth = depth(file);
                String path = path(depth);
                if (filename.endsWith("404.html")) {
                    path = "/";
                }
                page.setPathToRoot(path);
                page.printHeader();
                String string = null;
                do {
                    string = lnr.readLine();
                    if (string != null) {
                        processLine1(page, string);
                    }
                } while (string != null);
                page.printFooter();
            } finally {
                out.close();
            }
        } finally {
            lnr.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    private void processLine1(final Page page, final String string) {
        String lowerCase = string.toLowerCase();
        if (state == State.IDLE) {
            if (lowerCase.startsWith("<body")) {
                state = State.COPYING;
            }
        } else {
            if (lowerCase.startsWith("</body>")) {
                state = State.IDLE;
            } else {
                processLine(page, string);
            }
        }
    }

    /**
     * Processes html file line, insert LASSI navigation bar when
     * correct location found.
     *
     * @param out output file
     * @param file source file
     * @param string the line to be processed
     */
    protected void processLine(final Page page, final String string) {
        page.println(string);
    }

    /**
     * Gets the target file name.
     *
     * @param file the file that is processed
     * @return the target file name
     */
    private String targetFilename(final File file) {
        String path = sourceFilename(file);
        return targetDir + File.separator + path;
    }

    /**
     * Indicates whether given file should get the
     * logo and the navigation bar at the top.
     *
     * @param file the file to be checked
     * @return true if file needs to be processed
     */
    private boolean shouldProcess(final File file) {
        String name = file.getName();
        boolean shouldProcess = name.endsWith(".html");
        for (int i = 0; shouldProcess && i < excludes.size(); i++) {
            shouldProcess = !name.endsWith(excludes.get(i));
        }
        return shouldProcess;
    }
}
