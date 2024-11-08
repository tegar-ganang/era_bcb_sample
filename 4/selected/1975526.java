package net.sf.xdc.processing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sf.xdc.util.IOUtils;
import net.sf.xdc.util.Logging;
import net.sf.xdc.util.PathDescriptor;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;

/**
 * The <code>XdcSourceCollector</code> class is used to assemble the possible
 * XML source files specified for processing into XDC documentation.
 *
 * @author Jens Vo�
 * @since 0.5
 * @version 0.5
 */
public class XdcSourceCollector {

    private static final Logger LOG = Logging.getLogger();

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private CommandLine commandLine;

    private String[] sourcePaths;

    private File[] sourceDirs;

    private String[] subpackages;

    private String[] excluded;

    private String[] filePaths;

    private boolean defaultExcludes;

    private SortedSet xdcSources = new TreeSet();

    private SortedMap xdcPackages = new TreeMap();

    private FileAction addAction = new FileAction() {

        public void execute(XdcSource xdcSource) {
            XdcSourceCollector.this.xdcSources.add(xdcSource);
            String packageName = xdcSource.getPackageName();
            XdcPackage xdcPackage = (XdcPackage) xdcPackages.get(packageName);
            if (xdcPackage == null) {
                xdcPackage = new XdcPackage(packageName);
                xdcPackages.put(packageName, xdcPackage);
            }
            xdcPackage.addSource(xdcSource);
        }
    };

    private FileAction removeAction = new FileAction() {

        public void execute(XdcSource xdcSource) {
            XdcSourceCollector.this.xdcSources.remove(xdcSource);
            String packageName = xdcSource.getPackageName();
            XdcPackage xdcPackage = (XdcPackage) xdcPackages.get(packageName);
            xdcPackage.removeSource(xdcSource);
        }
    };

    /**
   * Public constructor.
   * @param commandLine The <code>CommandLine</code> containing all option
   *        values of the XDC invocation
   */
    public XdcSourceCollector(CommandLine commandLine) {
        this.commandLine = commandLine;
        this.sourcePaths = getStringArrayFromOption("sourcepath", false, new String[] { "." }, ";");
        this.subpackages = getStringArrayFromOption("subpackages", true, EMPTY_STRING_ARRAY, ":");
        this.excluded = getStringArrayFromOption("exclude", true, EMPTY_STRING_ARRAY, ":");
        this.filePaths = commandLine.getArgs();
        for (int i = 0; i < filePaths.length; i++) {
            filePaths[i] = filePaths[i].replace('\\', '/');
        }
        defaultExcludes = commandLine.hasOption("defaultexcludes");
        collectSourceDirs();
        collectSubpackages();
        collectArgumentFiles();
    }

    /**
   * This getter method retrieves all assembled XML source files.
   *
   * @return All XML source files specified for processing (in the form of an
   *         array of <code>XdcSource</code> objects)
   */
    public XdcSource[] getXdcSources() {
        return (XdcSource[]) xdcSources.toArray(new XdcSource[xdcSources.size()]);
    }

    /**
   * This method retrieves an {@link XdcPackage} object specified for processing
   * by the XDC tool.
   *
   * @param packageName The name of the package
   * @return The <code>XdcPackage</code> with the specified name
   */
    public XdcPackage getXdcPackage(String packageName) {
        return (XdcPackage) xdcPackages.get(packageName);
    }

    /**
   * This method retrieves all <code>XdcSource</code> objects contained in an
   * <code>XdcPackage</code>.
   *
   * @param packageName The name of the package from which the sources are
   *        returned
   * @return All XML source files (in the form of an array of
   *         <code>XdcSource</code> objects) contained in the package with the
   *         specified name.
   */
    public XdcSource[] getXdcSources(String packageName) {
        XdcPackage xdcPackage = (XdcPackage) xdcPackages.get(packageName);
        return xdcPackage.getXdcSources();
    }

    /**
   * This method retrieves a particular <code>XdcSource</code> by its
   * (fully-qualified) source name.
   *
   * @param sourceName The fully-qualified name of the <code>XdcSource</code>
   *        to be retrieved
   * @return The <code>XdcSource</code> with the given name
   */
    public XdcSource getXdcSource(String sourceName) {
        int pos = sourceName.lastIndexOf('/');
        String packageName = pos >= 0 ? sourceName.substring(0, pos) : "";
        XdcPackage xdcPackage = (XdcPackage) xdcPackages.get(packageName);
        return xdcPackage != null ? xdcPackage.getXdcSource(sourceName.substring(pos + 1)) : null;
    }

    private String[] getStringArrayFromOption(String option, boolean replacePeriod, String[] defaultValue, String delim) {
        if (commandLine.hasOption(option)) {
            String spVal = commandLine.getOptionValue(option);
            StringTokenizer tok = new StringTokenizer(spVal, delim, false);
            Set sps = new HashSet();
            while (tok.hasMoreTokens()) {
                String nextElem = tok.nextToken().replace('\\', '/');
                if (replacePeriod) {
                    nextElem = nextElem.replace('.', '/');
                }
                sps.add(nextElem);
            }
            return (String[]) sps.toArray(new String[sps.size()]);
        } else {
            return defaultValue;
        }
    }

    private void collectSourceDirs() {
        Set dirs = new HashSet(sourcePaths.length);
        for (Iterator iter = Arrays.asList(sourcePaths).iterator(); iter.hasNext(); ) {
            String path = (String) iter.next();
            File dir;
            try {
                dir = new File(path).getCanonicalFile();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                continue;
            }
            if (!dir.exists()) {
                LOG.warn("Source path " + path + " does not exist.");
            } else if (!dir.isDirectory()) {
                LOG.warn("Source path " + path + " is not a directory.");
            } else {
                dirs.add(dir);
            }
        }
        this.sourceDirs = (File[]) dirs.toArray(new File[dirs.size()]);
    }

    private void collectSubpackages() {
        Set extensions = new HashSet();
        if (commandLine.hasOption("extensions")) {
            StringTokenizer tok = new StringTokenizer(commandLine.getOptionValue("extensions"), ",", false);
            while (tok.hasMoreTokens()) {
                extensions.add(tok.nextToken());
            }
        }
        for (int i = 0; i < sourceDirs.length; i++) {
            File sourceDir = sourceDirs[i];
            for (int j = 0; j < subpackages.length; j++) {
                String subpackage = subpackages[j];
                File pkg = new File(sourceDir, subpackage);
                if (pkg.exists() && pkg.isDirectory()) {
                    recurse(new FileSelector(pkg, extensions, defaultExcludes), subpackage, addAction, sourceDir);
                }
            }
        }
        for (int i = 0; i < sourceDirs.length; i++) {
            File sourceDir = sourceDirs[i];
            for (int j = 0; j < excluded.length; j++) {
                String excludedPkg = excluded[j];
                File pkg = new File(sourceDir, excludedPkg);
                if (pkg.exists() && pkg.isDirectory()) {
                    recurse(new FileSelector(pkg, extensions, commandLine.hasOption("defaultexcludes")), excludedPkg, removeAction, sourceDir);
                }
            }
        }
    }

    private void collectArgumentFiles() {
        PathDescriptor[] descriptors = new PathDescriptor[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            String filePath = filePaths[i];
            descriptors[i] = new PathDescriptor(filePath, defaultExcludes);
        }
        for (int i = 0; i < sourceDirs.length; i++) {
            File sourceDir = sourceDirs[i];
            FileSelector selector = new PatternSelector(sourceDir, descriptors);
            recurse(selector, "", addAction, sourceDir);
        }
    }

    private void recurse(FileSelector selector, String packageName, FileAction fileAction, File sourceDir) {
        File[] files = selector.selectFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                DialectHandler handler = DialectHandler.getDialectHandler(file, packageName, commandLine);
                fileAction.execute(new XdcSource(file, sourceDir, packageName, handler));
            } else if (file.isDirectory()) {
                String filename = file.getName();
                String subPackageName = packageName != null && packageName.length() > 0 ? packageName + '/' + filename : filename;
                recurse(selector.moveToSubdir(filename), subPackageName, fileAction, sourceDir);
            }
        }
    }

    /**
   * This method returns the directory specified as the first argument of the
   * <code>-sourcepath</code> option. (This value is used as the root directory
   * of the XDC output if no <code>-d</code> option is specified.)
   *
   * @return The <code>File</code> specified as the first sourcepath
   */
    public File getLeadingSourcePath() {
        return this.sourceDirs[0];
    }

    /**
   * This method returns an array of the names of all packages containing
   * XML source files specified for processing.
   *
   * @return The names of all collected <code>XdcPackages</code>
   */
    public String[] getPackageNames() {
        Set retVal = xdcPackages.keySet();
        return (String[]) retVal.toArray(new String[retVal.size()]);
    }

    /**
   * This method is used to determine the number of frames contained in the
   * main frameset of the generated XDC documentation.
   *
   * @return If sources from just one <code>XdcPackage</code> have been
   *         assembled, the method returns 2; otherwise it returns 3.
   */
    public int getFramesetSize() {
        if (xdcPackages.size() == 1) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
   * This method attempts to extract text from an overview file (which is
   * specified by the <code>-overview</code> option and must be placed in of the
   * root directories specified as arguments to the <code>-sourcepath</code>
   * options).
   *
   * @return All text placed between the opening and the closing &lt;body&gt;
   *         tags of the overview file. Note that this text is returned "raw",
   *         i.e. no XDC tags are processed yet.
   */
    public String getSummaryText() {
        if (!commandLine.hasOption("overview")) {
            return "";
        }
        String overview = commandLine.getOptionValue("overview");
        for (int i = 0; i < sourceDirs.length; i++) {
            File packageFile = new File(sourceDirs[i], overview);
            if (packageFile.exists()) {
                String retVal = getSummaryText(packageFile);
                if (retVal != null) {
                    return retVal;
                }
            }
        }
        File packageFile = new File(overview);
        if (packageFile.exists()) {
            String retVal = getSummaryText(packageFile);
            if (retVal != null) {
                return retVal;
            }
        }
        return "";
    }

    private static String getSummaryText(File packageFile) {
        String retVal = null;
        Reader in = null;
        try {
            in = new FileReader(packageFile);
            StringWriter out = new StringWriter();
            IOUtils.copy(in, out);
            StringBuffer buf = out.getBuffer();
            int pos1 = buf.indexOf("<body>");
            int pos2 = buf.lastIndexOf("</body>");
            if (pos1 >= 0 && pos1 < pos2) {
                retVal = buf.substring(pos1 + 6, pos2);
            } else {
                retVal = "";
            }
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return retVal;
    }
}
