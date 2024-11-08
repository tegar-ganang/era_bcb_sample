package entagged.tageditor.tools.renaming;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import entagged.listing.ListingProcessor;
import entagged.listing.gui.ListingProgressDialog;
import entagged.tageditor.TagEditorFrame;
import entagged.tageditor.resources.LangageManager;
import entagged.tageditor.tools.renaming.data.DirectoryDescriptor;
import entagged.tageditor.tools.renaming.data.DirectoryPruneStructure;
import entagged.tageditor.tools.renaming.data.FileDescriptor;
import entagged.tageditor.tools.renaming.data.ProcessingResult;
import entagged.tageditor.tools.renaming.data.RenameConfiguration;
import entagged.tageditor.tools.renaming.data.stat.properties.CanNotWriteProperty;
import entagged.tageditor.tools.renaming.data.stat.properties.ContainsWarningsProperty;
import entagged.tageditor.tools.renaming.data.stat.properties.DestinationExistsProperty;
import entagged.tageditor.tools.renaming.data.stat.properties.DuplicateErrorProperty;
import entagged.tageditor.tools.renaming.data.stat.properties.NoChangeProperty;
import entagged.tageditor.tools.renaming.gui.InspectionControl;
import entagged.tageditor.tools.renaming.model.DefaultTreeModel;
import entagged.tageditor.tools.renaming.model.TargetTreeFilter;
import entagged.tageditor.tools.renaming.pattern.DirectoryPattern;
import entagged.tageditor.tools.renaming.pattern.FilePattern;
import entagged.tageditor.tools.renaming.processing.FileProcessor;
import entagged.tageditor.tools.stringtransform.TransformSet;

/**
 * This class is meant for performing file movement or file renaming.<br>
 * The {@link #run()} method will start the scanning of the given selection,
 * present the results, and rename and moves the audiofiles.<br>
 * <b>Consider:</b> This class will show dialogs, its not just processing.
 * 
 * @author Christian Laireiter
 */
public final class FileRenamer implements Runnable {

    /**
	 * This constant represents the placeholder for the album of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String ALBUM_MASK = "<album>";

    /**
	 * This constant represents the placeholder for the artist of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String ARTIST_MASK = "<artist>";

    /**
	 * This constant represents the placeholder for the bitrate of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String BITRATE_MASK = "<bitrate>";

    /**
	 * This constant represents the placeholder for the comment of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String COMMENT_MASK = "<comment>";

    /**
	 * This constant represents the placeholder for the genre of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String GENRE_MASK = "<genre>";

    /**
	 * This field contains a {@link Pattern}object which is compiled to
	 * identify a set of invalid characters for directory specification. <br>
	 * One can easily replace all defined characters of a string by doing the
	 * following: <br>
	 * 
	 * <pre>
	 * String path = &quot;C:\\temp\\Hello%World&quot;;
	 * Matcher matcher = PATTERN_INVALID_DIRECTORY_CHARS.matcher(path);
	 * path = matcher.replaceAll(&quot;-&quot;);
	 * </pre>
	 * 
	 * <br>
	 * No Path will be "C:\temp\Hello-World"; <br>
	 * <br>
	 * Using this will speed up multiple replacements.
	 */
    public static final Pattern PATTERN_INVALID_DIRECTORY_CHARS = Pattern.compile("(\\:|\\?|\\*|\\\"|\\'|\\<|\\>|\\||\\!|\\%|\\Q" + File.separatorChar + "\\E|\\Q" + File.pathSeparatorChar + "\\E)");

    /**
	 * Like {@link #PATTERN_INVALID_DIRECTORY_CHARS}this field defines the same
	 * chars plus path separators ("\" and "/") and
	 * System.getProperty("path.separator") <br>
	 */
    public static final Pattern PATTERN_INVALID_FILE_CHARS = Pattern.compile("(\\:|\\?|\\*|\\\"|\\'|\\<|\\>|\\||\\!|\\%|\\\\|\\/|\\Q" + File.separatorChar + "\\E|\\Q" + File.pathSeparatorChar + "\\E)");

    /**
	 * This constant represents the placeholder for the title of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String TITLE_MASK = "<title>";

    /**
	 * This constant represents the placeholder for the track of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String TRACK_MASK = "<track>";

    /**
	 * This constant represents the placeholder for the year of an audio tag.<br>
	 * It is used to define patterns.
	 */
    public static final String YEAR_MASK = "<year>";

    /**
	 * If this is set to <code>true</code>, the user aborted the operation.
	 */
    private boolean aborted = false;

    /**
	 * This field recieves a parent for the preview window.
	 */
    private JFrame parentFrame = null;

    /**
	 * Stores the result of the processing.<br>
	 */
    private ProcessingResult processingResult;

    /**
	 * This field will be assigned at each call of {@link #run()}. <br>
	 * The methode {@link #renameFile(FileDescriptor)} will fill it with those
	 * directories, which are the source of a handeled file and not a target of
	 * any other (this is easy due to a Flag).
	 */
    private DirectoryPruneStructure pruneStruct;

    /**
	 * This field stores the configuration of the current operation.<br>
	 */
    private RenameConfiguration renameConfig;

    /**
	 * Creates an instance of the file renamin operation.<br>
	 * 
	 * @param selection
	 *            The files or directories which should be processed.
	 * @param directoryPattern
	 *            The directorypattern for the operation. May be
	 *            <code>null</code> to indicate that no movement should be
	 *            performed.<br>
	 * @param filePattern
	 *            The filenamepattern for the operation. May be
	 *            <code>null</code> to indicate that the filename should be
	 *            preserved.<br>
	 * @param transformSet
	 *            The set of transformation which will be used.
	 */
    public FileRenamer(File[] selection, String directoryPattern, String filePattern, TransformSet transformSet) {
        assert selection != null;
        this.renameConfig = new RenameConfiguration(selection, new DirectoryPattern(directoryPattern), new FilePattern(filePattern), transformSet);
        this.renameConfig.getDirectoryPattern().setTransformSet(transformSet);
        this.renameConfig.getFilePattern().setTransformSet(transformSet);
        this.processingResult = new ProcessingResult();
    }

    /**
	 * Creates an instance of the file renamin operation.<br>
	 * 
	 * @param previewParent
	 *            A frame, which becomes parent of the prieview winodw.
	 * @param selection
	 *            The files or directories which should be processed.
	 * @param directoryPattern
	 *            The directorypattern for the operation. May be
	 *            <code>null</code> to indicate that no movement should be
	 *            performed.<br>
	 * @param filePattern
	 *            The filenamepattern for the operation. May be
	 *            <code>null</code> to indicate that the filename should be
	 *            preserved.<br>
	 * @param transformSet
	 *            The set of transformation which will be used.
	 */
    public FileRenamer(JFrame previewParent, File[] selection, String directoryPattern, String filePattern, TransformSet transformSet) {
        this(selection, directoryPattern, filePattern, transformSet);
        this.parentFrame = previewParent;
    }

    /**
	 * This method will copy the <code>file</code> to the given
	 * <code>targetFile</code>
	 * 
	 * @param file
	 *            The file to be copied.
	 * @param targetFile
	 *            The destination.
	 */
    private void copyFile(File file, File targetFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
            byte[] tmp = new byte[8192];
            int read = -1;
            while ((read = bis.read(tmp)) > 0) {
                bos.write(tmp, 0, read);
            }
            bis.close();
            bos.close();
        } catch (Exception e) {
            if (!targetFile.delete()) {
                System.err.println("Ups, created copy cant be deleted (" + targetFile.getAbsolutePath() + ")");
            }
        }
    }

    /**
	 * This method returns the configuration object.
	 * 
	 * @return Configuration of renaming.
	 */
    public RenameConfiguration getConfig() {
        return this.renameConfig;
    }

    /**
	 * @return Returns the parentFrame.
	 */
    public JFrame getParentFrame() {
        return this.parentFrame;
    }

    /**
	 * Returns the result structure.
	 * 
	 * @return Result structure.
	 */
    public ProcessingResult getResult() {
        return this.processingResult;
    }

    /**
	 * This method reads the children of the current parent out of the treemodel
	 * and renames them (if they represent files).
	 * 
	 * @param lpd
	 *            The progress dialog.
	 * @param treemodel
	 *            The treemodel.
	 * @param currentParent
	 *            The current parent object (will be a directory).
	 */
    private void handleChildren(ListingProgressDialog lpd, DefaultTreeModel treemodel, Object currentParent) {
        int childCount = treemodel.getChildCount(currentParent);
        lpd.directoryChanged(childCount, currentParent.toString());
        for (int i = 0; i < childCount && !this.aborted; i++) {
            this.aborted = lpd.abort();
            Object currentChild = treemodel.getChild(currentParent, i);
            if (!treemodel.isLeaf(currentChild)) {
                handleChildren(lpd, treemodel, currentChild);
                lpd.directoryChanged(childCount, currentParent.toString());
            } else {
                lpd.fileChanged(i, currentChild.toString());
                if (currentChild instanceof FileDescriptor) {
                    renameFile((FileDescriptor) currentChild);
                }
            }
        }
    }

    /**
	 * Returns <code>true</code> if the user aborted the operation.
	 * 
	 * @return <code>true</code> if renaming was aborted.
	 */
    public boolean isAborted() {
        return this.aborted;
    }

    /**
	 * Will prune the statistics of the given descriptors.<br>
	 * 
	 * @param fileSystemRoots
	 *            directories to process.
	 */
    private void refreshStatistics(DirectoryDescriptor[] fileSystemRoots) {
        for (int i = 0; i < fileSystemRoots.length; i++) {
            fileSystemRoots[i].refreshStat(DestinationExistsProperty.PROPERTY_NAME);
            fileSystemRoots[i].refreshStat(DuplicateErrorProperty.PROPERTY_NAME);
            fileSystemRoots[i].refreshStat(ContainsWarningsProperty.PROPERTY_NAME);
        }
    }

    /**
	 * Renames the given file.<br>
	 * <b>Consider:</b> {@link #pruneStruct} must not be <code>null</code>.
	 * 
	 * @param descriptor
	 *            The file descriptor, which represents the file to rename.
	 */
    private void renameFile(FileDescriptor descriptor) {
        if (descriptor.getStatistic().getProperty(NoChangeProperty.PROPERTY_NAME) > 0) {
            return;
        }
        DirectoryDescriptor targetDir = descriptor.getTargetDirectory();
        if (targetDir != null) {
            File targetDirFile = new File(targetDir.getPath());
            if (!targetDirFile.exists()) {
                if (!targetDirFile.mkdirs()) {
                    throw new RuntimeException("Can't create " + targetDir.getPath());
                }
            }
            File targetFile = new File(descriptor.getTargetDirectory().getPath() + File.separator + descriptor.getTargetName());
            if (targetFile.exists()) {
                throw new RuntimeException("Target file already exists \n" + targetFile.getAbsolutePath());
            }
            pruneStruct.addDirectory(descriptor.getSourceDirectory().getPath());
            if (descriptor.getStatistic().getProperty(CanNotWriteProperty.PROPERTY_NAME) > 0) {
                if (renameConfig.isCopyUnmodifiableFiles()) {
                    copyFile(new File(descriptor.getPath()), targetFile);
                    pruneStruct.excludeDirectory(descriptor.getPath());
                }
            } else {
                File sourceFile = new File(descriptor.getPath());
                if (!sourceFile.renameTo(targetFile)) {
                    pruneStruct.excludeDirectory(descriptor.getSourceDirectory().getPath());
                    throw new RuntimeException("can't rename (\n" + sourceFile.getAbsolutePath() + "\nto\n" + targetFile.getAbsolutePath() + ")");
                }
            }
        }
    }

    /**
	 * This method traverses the tree structure using the
	 * {@link entagged.tageditor.tools.renaming.model.DefaultTreeModel} in
	 * conjunction with
	 * {@link entagged.tageditor.tools.renaming.model.TargetTreeFilter}.<br>
	 * This is what the user has seen and confirmed.
	 */
    private void renameFiles() {
        DefaultTreeModel treemodel = new DefaultTreeModel(processingResult.getFileSystemRoots(), new TargetTreeFilter(getConfig()), "arbitrary");
        Object currentParent = treemodel.getRoot();
        final ListingProgressDialog lpd = new ListingProgressDialog(parentFrame);
        lpd.setModal(true);
        lpd.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                lpd.setVisible(true);
            }
        });
        try {
            handleChildren(lpd, treemodel, currentParent);
            lpd.appendMessage(LangageManager.getProperty("tagrename.prune"));
            pruneStruct.prune();
        } catch (RuntimeException re) {
            lpd.dispose();
            throw re;
        } finally {
            lpd.processingFinished();
            lpd.dispose();
        }
    }

    /**
	 * (overridden) This method performs the processing, the result display and
	 * the renaming.
	 * 
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        this.pruneStruct = new DirectoryPruneStructure();
        File[] selection = renameConfig.getSelection();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].getParent() != null) pruneStruct.excludeDirectory(selection[i].getParent());
        }
        try {
            ListingProcessor lpc = new ListingProcessor(new FileProcessor(this.processingResult, renameConfig), renameConfig.getSelection());
            ListingProgressDialog lpd = new ListingProgressDialog(getParentFrame());
            lpd.setLocationRelativeTo(null);
            lpc.setListingProgressListener(lpd);
            lpd.setModal(false);
            lpd.setVisible(true);
            lpc.start().join();
            lpd.processingFinished();
            lpd.dispose();
            if (lpc.getLastSeriouseException() != null) {
                throw lpc.getLastSeriouseException();
            }
            if (!lpd.abort()) {
                refreshStatistics(getResult().getFileSystemRoots());
                InspectionControl control = new InspectionControl(this);
                control.displayInspector();
                if (!(aborted = control.isAborted())) {
                    renameFiles();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(getParentFrame(), LangageManager.getProperty("tagrename.errormsg") + "\n" + e.getMessage(), LangageManager.getProperty("common.dialog.error"), JOptionPane.ERROR_MESSAGE);
        } finally {
            if (this.parentFrame != null && this.parentFrame instanceof TagEditorFrame) {
                ((TagEditorFrame) parentFrame).refreshCurrentTableView();
            }
        }
    }

    /**
	 * This method enables or disables the copy of unmoveable files.
	 * 
	 * @param b
	 *            <code>true</code>, if unmoveable files should be copied
	 */
    public void setCopyUnmodifiableFiles(boolean b) {
        if (getConfig().isCopyUnmodifiableFiles() != b) {
            getConfig().setCopyUnmodifiableFiles(b);
            if (getConfig().getDirectoryPattern().containsBitrate()) {
                FileProcessor.processBitrate(getResult().getFileSystemRoots(), getConfig().isCopyUnmodifiableFiles());
                refreshStatistics(getResult().getFileSystemRoots());
            }
        }
    }
}
