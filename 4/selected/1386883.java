package src.project;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import src.Constants;
import src.Statistics;
import src.Wiki2xhtmlArgsParser;
import src.project.FallbackFile.FallbackLocation;
import src.project.FallbackFile.NoFileFoundException;
import src.project.file.VirtualWikiFile;
import src.project.file.WikiFile;
import src.project.settings.PageSettings;
import src.project.settings.PageSettingsReader;
import src.project.settings.Settings;
import src.resources.ResProjectSettings.SettingsE;
import src.utilities.FileChangesMap;
import src.utilities.IOUtils;
import src.utilities.StringTools;

/**
 * Bundles multiple WikiFiles and project settings.
 */
public class WikiProject {

    public WikiProject(String projectDirectory) {
        setProjectDirectory(new File(projectDirectory));
        styleDirectory = new File(this.projectDirectory.getAbsolutePath() + File.separator + "style");
        outputDirectory = new File(this.projectDirectory.getAbsolutePath() + File.separator + Constants.Directories.target);
        styleOutputDirectory = "style";
        updateFileChangesMap();
        projectSettings.set_(SettingsE.imagepagesDir, Constants.Directories.imagePages);
        projectSettings.set_(SettingsE.imagepageImgWidth, Constants.Standards.widthImgImagepages);
        projectSettings.set_(SettingsE.thumbWidth, Constants.Standards.widthThumbs);
        projectSettings.set_(SettingsE.imagepageTitle, Constants.Standards.imagepageCaption);
        projectSettings.set_(SettingsE.galleryImagesPerLine, Constants.Standards.galleryImagesPerLine);
    }

    public File projectDirectory() {
        return projectDirectory;
    }

    /** Setting the project directory is only allowed if no files have been added yet. */
    public boolean setProjectDirectory(File f) {
        if (fileCount() == 0) {
            try {
                projectDirectory = f.getCanonicalFile();
            } catch (IOException e) {
                projectDirectory = f;
            }
            updateFileChangesMap();
            return true;
        }
        return false;
    }

    public File outputDirectory() {
        return outputDirectory;
    }

    /** @see #setProjectDirectory(File) */
    public boolean setOutputDirectory(File f) {
        if (fileCount() == 0) {
            try {
                checkOutputDirectoryLocation();
                try {
                    outputDirectory = f.getCanonicalFile();
                } catch (IOException e) {
                    outputDirectory = f;
                }
                updateSitemap();
                return true;
            } catch (InvalidOutputDirectoryLocationException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public File styleDirectory() {
        return styleDirectory;
    }

    public boolean setStyleDirectory(File f) {
        if (fileCount() == 0) {
            try {
                styleDirectory = f.getCanonicalFile();
            } catch (IOException e) {
                styleDirectory = f;
            }
            return true;
        }
        return false;
    }

    public String styleOutputDirectoryName() {
        return styleOutputDirectory;
    }

    public File styleOutputDirectory() {
        return new File(outputDirectory.getAbsolutePath() + File.separator + styleOutputDirectory);
    }

    public void setStyleOutputDirectory(String dirName) {
        styleOutputDirectory = dirName;
    }

    /** Returns the WikiMenu object or <strong><code>null</code></strong>, if no menu is set. */
    public WikiMenu wikiMenu() {
        if (wikiMenu == null && argsParser != null) {
            String menuLocation = (String) argsParser.getOptionValue(argsParser.menuFile, false);
            if (menuLocation != null) {
                try {
                    FallbackFile fMenu = locateDefault(menuLocation, null);
                    String content = fMenu.getContent().toString();
                    wikiMenu = new WikiMenu();
                    wikiMenu.readNewMenu(content);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
        return wikiMenu;
    }

    public WikiFile getFile(int id) {
        return fileMap.get(id);
    }

    public void addFile(WikiFile f) throws InvalidLocationException {
        if (f.validLocation()) {
            fileMap.put(nextID(), f);
        } else {
            throw new InvalidLocationException(String.format("File %s is not at a valid location.", f.projectAbsoluteName()));
        }
    }

    public int fileCount() {
        return fileMap.size();
    }

    public String getProperty(SettingsE property) {
        return projectSettings.get_(property);
    }

    public boolean isPropertySet(SettingsE property) {
        return projectSettings.isSet(property);
    }

    public void checkOutputDirectoryLocation() throws InvalidOutputDirectoryLocationException {
        if (projectDirectory.equals(outputDirectory)) {
            throw new InvalidOutputDirectoryLocationException("Source directory must not be equal to the output directory.");
        }
        try {
            if (projectDirectory.getCanonicalPath().startsWith(outputDirectory.getCanonicalPath())) {
                throw new InvalidOutputDirectoryLocationException("Project directory must not be a subdirectory of the output directory.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new InvalidOutputDirectoryLocationException(e.getMessage());
        }
    }

    public void make() throws IOException, InvalidOutputDirectoryLocationException {
        Statistics.getInstance().sw.timeOverall.start();
        checkOutputDirectoryLocation();
        updateSitemap();
        boolean incremental = (Boolean) argsParser.getOptionValue(argsParser.incremental, Boolean.FALSE, false);
        String pathMenuFile = null;
        String pathCommonFile = null;
        String commonFile = (String) argsParser.getOptionValue(argsParser.commonFile, false);
        if (commonFile != null) {
            FallbackFile ff = locate(commonFile, FallbackFile.projectLocationOnly);
            PageSettingsReader psr = new PageSettingsReader(ff.getContent(), (PageSettings) projectSettings);
            psr.readSettings(false);
            pathCommonFile = IOUtils.trimPath(ff.pathInfo(), projectDirectory().getAbsolutePath());
            if (!fileChangesMap.queryUnchanged(pathCommonFile)) {
                if (incremental) {
                    System.out.println("Common file has changed, re-processing everything.");
                    incremental = false;
                }
            }
        }
        String menuFile = (String) argsParser.getOptionValue(argsParser.menuFile, false);
        if (menuFile != null) {
            pathMenuFile = IOUtils.trimPath(locate(menuFile, FallbackFile.projectLocationOnly).pathInfo(), projectDirectory().getAbsolutePath());
            if (!fileChangesMap.queryUnchanged(pathMenuFile)) {
                if (incremental) {
                    System.out.println("Menu file has changed, re-processing everything.");
                    incremental = false;
                }
            }
            incremental &= fileChangesMap.queryUnchanged(pathMenuFile);
        }
        wikiStyle.copyFiles();
        copyFiles();
        for (WikiFile f : fileMap.values()) {
            if (incremental && fileChangesMap.queryUnchanged(f.projectAbsoluteName())) {
                System.out.printf("Skipping %s (unchanged).\n", f.internalName());
            } else {
                f.parse();
                f.write();
                fileChangesMap.update(f.projectAbsoluteName());
                if (f.includedFiles.size() > 0) {
                    for (String s : f.includedFiles) {
                        fileChangesMap.updateInclude(f.projectAbsoluteName(), s);
                    }
                }
                if (f.sitemap) {
                    sitemap.add(f.internalName());
                }
                System.out.printf("Processed %s (%s).\n", f.internalName(), StringTools.formatTimeMilliseconds(f.timeParsingMillis));
            }
        }
        if (commonFile != null) {
            fileChangesMap.update(pathCommonFile);
        }
        if (menuFile != null) {
            fileChangesMap.update(pathMenuFile);
        }
        fileChangesMap.updateIncludedHashes();
        sitemap.write();
        Statistics.getInstance().sw.timeOverall.stop();
        System.out.printf("Total time taken: %s\n", Statistics.getInstance().sw.timeOverall.getStoppedTimeString());
    }

    /** 
	 * @param requester Can be <code>null</code>. Denotes the file requesting <code>filename</code>. Should be set
	 * if <code>filename</code> is included by <code>requester</code>, i.e. if <code>requester</code> needs to be 
	 * updated if <code>filename</code> changes.
	 * @see {@link FallbackFile} 
	 */
    public final FallbackFile locateDefault(String filename, WikiFile requester) throws NoFileFoundException {
        FallbackFile ff = new FallbackFile(filename, this);
        if (requester != null && ff.file() != null) {
            requester.includedFiles.add(ff.file().getAbsolutePath().substring(requester.project.projectDirectory().getAbsolutePath().length() + 1));
        }
        return ff;
    }

    /** See {@link FallbackFile} */
    public final FallbackFile locate(String filename, Vector<FallbackLocation> fallback) throws NoFileFoundException {
        return new FallbackFile(filename, this, fallback);
    }

    /**
	 * <p>Copies project files with rsync. The list of files to copy is given in the file {@code resources.txt}
	 * in the rsync include file format. Example:</p>
	 * <p><code>+ images/*.jpg<br/>
	 * - resources.txt<br/>
	 * - *~</code></p>
	 * <p>The space after the {@code +/-} is mandatory!</p>
	 */
    public void copyFiles() {
        IOUtils.copyWithRsync(projectDirectory, outputDirectory, String.format("(%s->%s)   ", projectDirectory().getName(), outputDirectory.getName()));
    }

    public static class InvalidLocationException extends Exception {

        private static final long serialVersionUID = 1L;

        public InvalidLocationException(String msg) {
            super(msg);
        }
    }

    public static class InvalidOutputDirectoryLocationException extends Exception {

        private static final long serialVersionUID = 1L;

        public InvalidOutputDirectoryLocationException(String msg) {
            super(msg);
        }
    }

    private File projectDirectory;

    private File outputDirectory;

    private File styleDirectory;

    private String styleOutputDirectory;

    private FileChangesMap fileChangesMap;

    public Sitemap sitemap = null;

    public WikiStyle wikiStyle = new WikiStyle(this);

    public Wiki2xhtmlArgsParser argsParser = null;

    private Settings<SettingsE, String> projectSettings = new PageSettings();

    private WikiMenu wikiMenu = null;

    private int id = 0;

    private int nextID() {
        return id++;
    }

    /** All files in the project */
    private HashMap<Integer, WikiFile> fileMap = new HashMap<Integer, WikiFile>();

    /** If the project directory changes */
    private void updateFileChangesMap() {
        fileChangesMap = new FileChangesMap(projectDirectory(), ".wiki2xhtml-hashes");
    }

    private void updateSitemap() {
        sitemap = null;
        if (argsParser != null) {
            String prefix = (String) argsParser.getOptionValue(argsParser.sitemap, false);
            boolean incremental = (Boolean) argsParser.getOptionValue(argsParser.incremental, Boolean.FALSE, false);
            if (prefix != null) {
                File f = new File(outputDirectory().getAbsolutePath() + File.separator + "sitemap.txt");
                sitemap = new Sitemap(f, prefix, incremental);
            }
        }
        if (sitemap == null) {
            sitemap = new Sitemap(null, "", false);
        }
    }

    public static void main(String[] args) throws InvalidLocationException, IOException, InvalidOutputDirectoryLocationException {
        WikiProject p = new WikiProject(".");
        StringBuffer sb = new StringBuffer();
        sb.append("Hallo. [[link.html]]\n*bla");
        VirtualWikiFile vf = new VirtualWikiFile(p, "myname", false, sb);
        p.addFile(vf);
        p.make();
        System.out.println(vf.getContent());
    }
}
