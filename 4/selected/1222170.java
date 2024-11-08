package com.pallas.unicore.extensions;

import java.util.Vector;
import org.unicore.ajo.AbstractAction;
import org.unicore.ajo.ActionGroup;
import org.unicore.ajo.ChangePermissions;
import org.unicore.ajo.CopyFile;
import org.unicore.ajo.CreateDirectory;
import org.unicore.ajo.DeleteFile;
import org.unicore.ajo.RenameFile;
import org.unicore.idiomatic.CopyXtoX;
import org.unicore.resources.PathedStorage;
import org.unicore.resources.Storage;
import org.unicore.resources.USpace;
import org.unicore.sets.ResourceSet;
import com.pallas.unicore.container.TaskContainer;
import com.pallas.unicore.resourcemanager.ResourceManager;

/**
 * Class builds AJO ActionGroup from an input Array of FileSystemOperations
 * 
 * @author Ralf Ratering
 * @version $Id: FileSystemOperationFactory.java,v 1.2 2003/06/30 06:58:20 rrate
 *          Exp $
 */
public class FileSystemOperationFactory extends FileOperationFactory {

    public FileSystemOperationFactory(FileSystemOperation[] fileOpsArray, ActionGroup actionGroup, TaskContainer container) {
        super(fileOpsArray, actionGroup, container);
    }

    public void build() {
        Vector copies = new Vector();
        Vector deletes = new Vector();
        Vector renames = new Vector();
        Vector makedirs = new Vector();
        Vector chmods = new Vector();
        for (int i = 0; i < fileOps.size(); i++) {
            FileSystemOperation fileOperation = (FileSystemOperation) fileOps.get(i);
            if (fileOperation.getOperation().equals(FileSystemOperation.COPY)) {
                copies.add(fileOperation);
            } else if (fileOperation.getOperation().equals(FileSystemOperation.DELETE)) {
                deletes.add(fileOperation);
            } else if (fileOperation.getOperation().equals(FileSystemOperation.RENAME)) {
                renames.add(fileOperation);
            } else if (fileOperation.getOperation().equals(FileSystemOperation.MAKEDIR)) {
                makedirs.add(fileOperation);
            } else if (fileOperation.getOperation().equals(FileSystemOperation.CHMOD)) {
                chmods.add(fileOperation);
            }
        }
        buildCopyTask(copies);
        buildDeleteTask(deletes);
        buildRenameTask(renames);
        buildMakeDirTask(makedirs);
        buildChmodTask(chmods);
    }

    private void buildChmodTask(Vector chmods) {
        for (int i = 0; i < chmods.size(); i++) {
            FileSystemOperation fileOp = (FileSystemOperation) chmods.elementAt(i);
            String permissions = fileOp.getDestinationName();
            boolean read = permissions.indexOf("r") >= 0;
            boolean write = permissions.indexOf("w") >= 0;
            boolean execute = permissions.indexOf("x") >= 0;
            ChangePermissions chmod = new ChangePermissions(container.getName() + "_CHANGE_PERMISSIONS_" + fileOp + ResourceManager.getNextObjectIdentifier(), new ResourceSet((Storage) fileOp.getStorageItem()), fileOp.getSourceName(), read, write, execute);
            actionGroup.add(chmod);
            fileOp.setAbstractAction(chmod);
        }
    }

    private void buildCopyTask(Vector copies) {
        for (int i = 0; i < copies.size(); i++) {
            FileSystemOperation fileOp = (FileSystemOperation) copies.elementAt(i);
            AbstractAction copyFile;
            USpace uspaceHome = ResourceManager.getResourceSet(container.getVsite()).getUspace();
            if (!(uspaceHome.equals((Storage) fileOp.getStorageItem()))) {
                copyFile = new CopyXtoX(container.getName() + "_COPY_X_TO_X_EXEC_" + fileOp + ResourceManager.getNextObjectIdentifier(), null, fileOp.getSourceName(), (PathedStorage) fileOp.getStorageItem(), fileOp.getDestinationName(), (PathedStorage) fileOp.getStorageItem(), true);
            } else {
                copyFile = new CopyFile(container.getName() + "_COPY_FILE_EXEC_" + fileOp + ResourceManager.getNextObjectIdentifier(), null, new ResourceSet((Storage) fileOp.getStorageItem()), fileOp.getSourceName(), fileOp.getDestinationName(), true, true);
            }
            actionGroup.add(copyFile);
            fileOp.setAbstractAction(copyFile);
        }
    }

    private void buildDeleteTask(Vector deletes) {
        for (int i = 0; i < deletes.size(); i++) {
            FileSystemOperation fileOp = (FileSystemOperation) deletes.elementAt(i);
            DeleteFile deleteFile = new DeleteFile(container.getName() + "_DELETE_FILE_EXEC_" + fileOp + ResourceManager.getNextObjectIdentifier(), null, new ResourceSet((Storage) fileOp.getStorageItem()), fileOp.getSourceName());
            actionGroup.add(deleteFile);
            fileOp.setAbstractAction(deleteFile);
        }
    }

    private void buildMakeDirTask(Vector makedirs) {
        for (int i = 0; i < makedirs.size(); i++) {
            FileSystemOperation fileOp = (FileSystemOperation) makedirs.elementAt(i);
            CreateDirectory makeDir = new CreateDirectory(container.getName() + "_CREATE_DIR_EXEC_" + fileOp + ResourceManager.getNextObjectIdentifier(), null, new ResourceSet((Storage) fileOp.getStorageItem()), fileOp.getSourceName());
            actionGroup.add(makeDir);
            fileOp.setAbstractAction(makeDir);
        }
    }

    private void buildRenameTask(Vector renames) {
        for (int i = 0; i < renames.size(); i++) {
            FileSystemOperation fileOp = (FileSystemOperation) renames.elementAt(i);
            RenameFile renameFile = new RenameFile(container.getName() + "_RENAME_FILE_EXEC_" + fileOp + ResourceManager.getNextObjectIdentifier(), null, new ResourceSet((Storage) fileOp.getStorageItem()), fileOp.getSourceName(), fileOp.getDestinationName(), true);
            actionGroup.add(renameFile);
            fileOp.setAbstractAction(renameFile);
        }
    }
}
