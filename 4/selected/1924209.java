package leapTools;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.DirectoryScanner;
import java.io.*;

/**
   ANT-task preprocessing JADE sources into JADE-LEAP sources
   for J2SE, PJAVA and MIDP environments
   @author Giovanni Caire - TILAB
 */
public class Preprocessor extends Task {

    private static final String ALL_EXCLUDE_FILE_MARKER = "//#ALL_EXCLUDE_FILE";

    private static final String J2SE_EXCLUDE_FILE_MARKER = "//#J2SE_EXCLUDE_FILE";

    private static final String J2ME_EXCLUDE_FILE_MARKER = "//#J2ME_EXCLUDE_FILE";

    private static final String PJAVA_EXCLUDE_FILE_MARKER = "//#PJAVA_EXCLUDE_FILE";

    private static final String MIDP_EXCLUDE_FILE_MARKER = "//#MIDP_EXCLUDE_FILE";

    private static final String DOTNET_EXCLUDE_FILE_MARKER = "//#DOTNET_EXCLUDE_FILE";

    private static final String ALL_EXCLUDE_BEGIN_MARKER = "//#ALL_EXCLUDE_BEGIN";

    private static final String ALL_EXCLUDE_END_MARKER = "//#ALL_EXCLUDE_END";

    private static final String ALL_INCLUDE_BEGIN_MARKER = "/*#ALL_INCLUDE_BEGIN";

    private static final String ALL_INCLUDE_END_MARKER = "#ALL_INCLUDE_END*/";

    private static final String JAVA_EXCLUDE_BEGIN_MARKER = "//#JAVA_EXCLUDE_BEGIN";

    private static final String JAVA_EXCLUDE_END_MARKER = "//#JAVA_EXCLUDE_END";

    private static final String JAVA_INCLUDE_BEGIN_MARKER = "/*#JAVA_INCLUDE_BEGIN";

    private static final String JAVA_INCLUDE_END_MARKER = "#JAVA_INCLUDE_END*/";

    private static final String J2ME_EXCLUDE_BEGIN_MARKER = "//#J2ME_EXCLUDE_BEGIN";

    private static final String J2ME_EXCLUDE_END_MARKER = "//#J2ME_EXCLUDE_END";

    private static final String J2ME_INCLUDE_BEGIN_MARKER = "/*#J2ME_INCLUDE_BEGIN";

    private static final String J2ME_INCLUDE_END_MARKER = "#J2ME_INCLUDE_END*/";

    private static final String PJAVA_EXCLUDE_BEGIN_MARKER = "//#PJAVA_EXCLUDE_BEGIN";

    private static final String PJAVA_EXCLUDE_END_MARKER = "//#PJAVA_EXCLUDE_END";

    private static final String PJAVA_INCLUDE_BEGIN_MARKER = "/*#PJAVA_INCLUDE_BEGIN";

    private static final String PJAVA_INCLUDE_END_MARKER = "#PJAVA_INCLUDE_END*/";

    private static final String MIDP_EXCLUDE_BEGIN_MARKER = "//#MIDP_EXCLUDE_BEGIN";

    private static final String MIDP_EXCLUDE_END_MARKER = "//#MIDP_EXCLUDE_END";

    private static final String MIDP_INCLUDE_BEGIN_MARKER = "/*#MIDP_INCLUDE_BEGIN";

    private static final String MIDP_INCLUDE_END_MARKER = "#MIDP_INCLUDE_END*/";

    private static final String DOTNET_EXCLUDE_BEGIN_MARKER = "//#DOTNET_EXCLUDE_BEGIN";

    private static final String DOTNET_EXCLUDE_END_MARKER = "//#DOTNET_EXCLUDE_END";

    private static final String DOTNET_INCLUDE_BEGIN_MARKER = "/*#DOTNET_INCLUDE_BEGIN";

    private static final String DOTNET_INCLUDE_END_MARKER = "#DOTNET_INCLUDE_END*/";

    private static final String NODEBUG_EXCLUDE_BEGIN_MARKER = "//#NODEBUG_EXCLUDE_BEGIN";

    private static final String NODEBUG_EXCLUDE_END_MARKER = "//#NODEBUG_EXCLUDE_END";

    private static final String J2SE = "j2se";

    private static final String PJAVA = "pjava";

    private static final String MIDP = "midp";

    private static final String DOTNET = "DotNET";

    private static final int KEEP = 0;

    private static final int OVERWRITE = 1;

    private static final int REMOVE = 2;

    private boolean verbose = false;

    private int removedCnt = 0;

    private int modifiedCnt = 0;

    private String target;

    private String basedir;

    private String type;

    public void setTarget(String target) {
        this.target = target;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void execute() throws BuildException {
        String[] ebms = null;
        String[] eems = null;
        String[] ims = null;
        String[] efms = null;
        if (DOTNET.equalsIgnoreCase(type)) {
            ebms = new String[] { ALL_EXCLUDE_BEGIN_MARKER, J2ME_EXCLUDE_BEGIN_MARKER, PJAVA_EXCLUDE_BEGIN_MARKER, DOTNET_EXCLUDE_BEGIN_MARKER };
            eems = new String[] { ALL_EXCLUDE_END_MARKER, J2ME_EXCLUDE_END_MARKER, PJAVA_EXCLUDE_END_MARKER, DOTNET_EXCLUDE_END_MARKER };
            ims = new String[] { ALL_INCLUDE_BEGIN_MARKER, ALL_INCLUDE_END_MARKER, J2ME_INCLUDE_BEGIN_MARKER, J2ME_INCLUDE_END_MARKER, PJAVA_INCLUDE_BEGIN_MARKER, PJAVA_INCLUDE_END_MARKER, DOTNET_INCLUDE_BEGIN_MARKER, DOTNET_INCLUDE_END_MARKER };
            efms = new String[] { ALL_EXCLUDE_FILE_MARKER, DOTNET_EXCLUDE_FILE_MARKER };
        } else if (MIDP.equalsIgnoreCase(type)) {
            ebms = new String[] { ALL_EXCLUDE_BEGIN_MARKER, J2ME_EXCLUDE_BEGIN_MARKER, MIDP_EXCLUDE_BEGIN_MARKER, JAVA_EXCLUDE_BEGIN_MARKER };
            eems = new String[] { ALL_EXCLUDE_END_MARKER, J2ME_EXCLUDE_END_MARKER, MIDP_EXCLUDE_END_MARKER, JAVA_EXCLUDE_END_MARKER };
            ims = new String[] { ALL_INCLUDE_BEGIN_MARKER, ALL_INCLUDE_END_MARKER, J2ME_INCLUDE_BEGIN_MARKER, J2ME_INCLUDE_END_MARKER, MIDP_INCLUDE_BEGIN_MARKER, MIDP_INCLUDE_END_MARKER };
            efms = new String[] { ALL_EXCLUDE_FILE_MARKER, J2ME_EXCLUDE_FILE_MARKER, MIDP_EXCLUDE_FILE_MARKER };
        } else if (PJAVA.equalsIgnoreCase(type)) {
            ebms = new String[] { ALL_EXCLUDE_BEGIN_MARKER, J2ME_EXCLUDE_BEGIN_MARKER, PJAVA_EXCLUDE_BEGIN_MARKER, JAVA_EXCLUDE_BEGIN_MARKER };
            eems = new String[] { ALL_EXCLUDE_END_MARKER, J2ME_EXCLUDE_END_MARKER, PJAVA_EXCLUDE_END_MARKER, JAVA_EXCLUDE_END_MARKER };
            ims = new String[] { ALL_INCLUDE_BEGIN_MARKER, ALL_INCLUDE_END_MARKER, J2ME_INCLUDE_BEGIN_MARKER, J2ME_INCLUDE_END_MARKER, PJAVA_INCLUDE_BEGIN_MARKER, PJAVA_INCLUDE_END_MARKER };
            efms = new String[] { ALL_EXCLUDE_FILE_MARKER, J2ME_EXCLUDE_FILE_MARKER, PJAVA_EXCLUDE_FILE_MARKER };
        } else if (J2SE.equalsIgnoreCase(type)) {
            ebms = new String[] { ALL_EXCLUDE_BEGIN_MARKER, JAVA_EXCLUDE_BEGIN_MARKER };
            eems = new String[] { ALL_EXCLUDE_END_MARKER, JAVA_EXCLUDE_END_MARKER };
            ims = new String[] { ALL_INCLUDE_BEGIN_MARKER, ALL_INCLUDE_END_MARKER };
            efms = new String[] { ALL_EXCLUDE_FILE_MARKER, J2SE_EXCLUDE_FILE_MARKER };
        } else {
            String upperCaseType = type.toUpperCase();
            ebms = new String[] { "//#" + upperCaseType + "_EXCLUDE_BEGIN" };
            eems = new String[] { "//#" + upperCaseType + "_EXCLUDE_END" };
            ims = new String[] { "/*#" + upperCaseType + "_INCLUDE_BEGIN", "#" + upperCaseType + "_INCLUDE_END*/" };
            efms = new String[] { "//#" + upperCaseType + "_EXCLUDE_FILE" };
        }
        if (basedir != null) {
            File d = new File(basedir);
            if (d.isDirectory()) {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir(d);
                scanner.setIncludes(new String[] { "**/*.java" });
                scanner.scan();
                String[] files = scanner.getIncludedFiles();
                System.out.println("Preprocessing " + files.length + " files.");
                for (int i = 0; i < files.length; ++i) {
                    execute(d.getPath() + "/" + files[i], ebms, eems, ims, efms);
                }
                System.out.println("Modified " + modifiedCnt + " files.");
                System.out.println("Removed  " + removedCnt + " files.");
            } else {
                throw new BuildException("Error: " + basedir + " is not a directory.");
            }
        } else {
            execute(target, ebms, eems, ims, efms);
        }
    }

    /**
	   Preprocess a single file
	 */
    private void execute(String file, String[] ebms, String[] eems, String[] ims, String[] efms) throws BuildException {
        if (verbose) {
            System.out.println("Preprocessing file " + file + " (type: " + type + ")");
        }
        try {
            File targetFile = new File(file);
            FileReader fr = new FileReader(targetFile);
            BufferedReader reader = new BufferedReader(fr);
            File preprocFile = File.createTempFile(targetFile.getName(), "tmp", targetFile.getParentFile());
            FileWriter fw = new FileWriter(preprocFile);
            BufferedWriter writer = new BufferedWriter(fw);
            int result = preprocess(reader, writer, ebms, eems, ims, efms);
            reader.close();
            writer.close();
            switch(result) {
                case OVERWRITE:
                    if (!targetFile.delete()) {
                        System.out.println("Can't overwrite target file with preprocessed file");
                        throw new BuildException("Can't overwrite target file " + target + " with preprocessed file");
                    }
                    preprocFile.renameTo(targetFile);
                    if (verbose) {
                        System.out.println("File " + preprocFile.getName() + " modified.");
                    }
                    modifiedCnt++;
                    break;
                case REMOVE:
                    if (!targetFile.delete()) {
                        System.out.println("Can't delete target file");
                        throw new BuildException("Can't delete target file " + target);
                    }
                    if (!preprocFile.delete()) {
                        System.out.println("Can't delete temporary preprocessed file " + preprocFile.getName());
                        throw new BuildException("Can't delete temporary preprocessed file " + preprocFile.getName());
                    }
                    if (verbose) {
                        System.out.println("File " + preprocFile.getName() + " removed.");
                    }
                    removedCnt++;
                    break;
                case KEEP:
                    if (!preprocFile.delete()) {
                        System.out.println("Can't delete temporary preprocessed file " + preprocFile.getName());
                        throw new BuildException("Can't delete temporary preprocessed file " + preprocFile.getName());
                    }
                    break;
                default:
                    throw new BuildException("Unexpected preprocessing result for file " + preprocFile.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e.getMessage());
        }
    }

    private int preprocess(BufferedReader reader, BufferedWriter writer, String[] excludeBeginMarkers, String[] excludeEndMarkers, String[] includeMarkers, String[] excludeFileMarkers) throws IOException {
        String line = null;
        boolean skip = false;
        int result = KEEP;
        String nextExcludeEndMarker = null;
        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            String trimmedLine = line.trim();
            if (isMarker(trimmedLine, excludeFileMarkers)) {
                return REMOVE;
            }
            if (!skip) {
                if (isMarker(trimmedLine, excludeBeginMarkers)) {
                    result = OVERWRITE;
                    skip = true;
                    nextExcludeEndMarker = getExcludeEndMarker(trimmedLine, excludeEndMarkers);
                } else {
                    if (!isMarker(trimmedLine, includeMarkers)) {
                        writer.write(line);
                        writer.newLine();
                    } else {
                        result = OVERWRITE;
                    }
                }
            } else {
                if (trimmedLine.startsWith(nextExcludeEndMarker)) {
                    skip = false;
                    nextExcludeEndMarker = null;
                }
            }
        }
        return result;
    }

    private boolean isMarker(String s, String[] markers) {
        for (int i = 0; i < markers.length; ++i) {
            if (s.startsWith(markers[i])) {
                return true;
            }
        }
        return false;
    }

    private String getExcludeEndMarker(String s, String[] endMarkers) {
        int rootLength = s.indexOf("BEGIN");
        String root = s.substring(0, rootLength);
        for (int i = 0; i < endMarkers.length; ++i) {
            if (endMarkers[i].startsWith(root)) {
                return endMarkers[i];
            }
        }
        return null;
    }
}
