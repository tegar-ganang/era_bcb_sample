package org.afekete.filecompareprocessor.action;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.afekete.filecompareprocessor.comparator.ChecksumAndSizeComparator;
import org.afekete.filemetadatacreator.generated.Metadatacollection;
import org.afekete.filemetadatacreator.generated.Metadatacollection.Metadata;
import org.apache.commons.io.FileUtils;

/**
 * Represents an action class that executes a specific file operation
 * if there is a match in versions.
 * 
 * @author afekete
 *
 */
public class FileAction {

    /**
	 * Type of action to execute on files
	 * @author afekete
	 *
	 */
    public static enum ActionType {

        COPY_SECONDARY_VERSION_ITEM, MOVE_SECONDARY_VERSION_ITEM, DELETE_SECONDARY_VERSION_ITEM
    }

    /**
	 * Array of ActionType objects
	 */
    public static ActionType[] actions = new ActionType[] { ActionType.COPY_SECONDARY_VERSION_ITEM, ActionType.MOVE_SECONDARY_VERSION_ITEM, ActionType.DELETE_SECONDARY_VERSION_ITEM };

    private ActionType action;

    private String path;

    private boolean createStructure;

    private char version;

    private List<Metadata> primaryDataList;

    private List<Metadata> secondaryDataList;

    private String svPath;

    private String pvPath;

    private static ChecksumAndSizeComparator casc = new ChecksumAndSizeComparator();

    /**
	 * Invokes the proper method for file operation execution 
	 * depending on the action attribute.
	 */
    public void executeAction() {
        if (action.equals(ActionType.COPY_SECONDARY_VERSION_ITEM)) executeCopySecondaryVersionItem(); else if (action.equals(ActionType.MOVE_SECONDARY_VERSION_ITEM)) executeMoveSecondaryVersionItem(); else if (action.equals(ActionType.DELETE_SECONDARY_VERSION_ITEM)) executeDeleteSecondaryVersionItem();
    }

    /**
	 * Executes deletion of files on match. 
	 */
    private void executeDeleteSecondaryVersionItem() {
        for (Metadata mds : secondaryDataList) for (Metadata mdp : primaryDataList) if (casc.compare(mds, mdp) == 0) getFile(svPath, mds.getRelativepath(), mds.getName()).delete();
    }

    /**
	 * Executes move of files on match.
	 */
    private void executeMoveSecondaryVersionItem() {
        for (Metadata mds : secondaryDataList) for (Metadata mdp : primaryDataList) {
            if (casc.compare(mds, mdp) == 0) {
                String aRelativePath = createStructure ? (version == 'p' ? mdp.getRelativepath() : mds.getRelativepath()) : "";
                File srcFile = getFile(svPath, mds.getRelativepath(), mds.getName());
                File destFile = getFile(path, aRelativePath, mds.getName());
                try {
                    FileUtils.moveFile(srcFile, destFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * Executes copy of files on match.
	 */
    private void executeCopySecondaryVersionItem() {
        for (Metadatacollection.Metadata mds : secondaryDataList) for (Metadatacollection.Metadata mdp : primaryDataList) {
            if (casc.compare(mds, mdp) == 0) {
                String aRelativePath = createStructure ? (version == 'p' ? mdp.getRelativepath() : mds.getRelativepath()) : "";
                File srcFile = getFile(svPath, mds.getRelativepath(), mds.getName());
                File destFile = getFile(path, aRelativePath, mds.getName());
                try {
                    FileUtils.copyFile(srcFile, destFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File getFile(String aPath, String aRelativePath, String name) {
        return new File(aPath + File.separator + aRelativePath + File.separator + name);
    }

    public void setPrimaryDataList(List<Metadata> primaryDataList) {
        this.primaryDataList = primaryDataList;
    }

    public void setSecondaryDataList(List<Metadata> secondaryDataList) {
        this.secondaryDataList = secondaryDataList;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setCreateStructure(boolean createStructure) {
        this.createStructure = createStructure;
    }

    public void setVersion(char version) {
        this.version = version;
    }

    public void setSvPath(String svPath) {
        this.svPath = svPath;
    }

    public void setPvPath(String pvPath) {
        this.pvPath = pvPath;
    }

    public static void setCasc(ChecksumAndSizeComparator casc) {
        FileAction.casc = casc;
    }
}
