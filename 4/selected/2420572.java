package org.kalypso.nofdpidss.core.common.utils.modules;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.gmlschema.property.relation.IRelationType;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.project.info.IDocumentMember;
import org.kalypso.nofdpidss.core.base.gml.model.project.info.IProjectInfoModel;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolProjectInfo;
import org.kalypso.nofdpidss.core.common.utils.various.CloneRenaming;
import org.kalypso.ogc.gml.FeatureUtils;
import org.kalypso.ogc.gml.command.DeleteFeatureCommand;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree_impl.model.feature.FeatureHelper;

/**
 * @author Dirk Kuch
 */
public class DocumentUtils {

    public static void cloneDocument(final MyBasePool pool, final Feature fDocument) throws Exception {
        if (fDocument == null) return;
        final Object objFileName = fDocument.getProperty(IDocumentMember.QN_FILE_NAME);
        if (!(objFileName instanceof String) || ((String) objFileName).equals("")) return;
        String sFileName = (String) objFileName;
        final IProject project = NofdpCorePlugin.getProjectManager().getProjectToEdit();
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
        final IFolder documentFolder = project.getFolder(IProjectInfoModel.DOCUMENT_FOLDER);
        final File srcFile = new File(documentFolder.getFile(sFileName).getLocation().toOSString());
        final Feature fParent = fDocument.getParent();
        final IRelationType relation = (IRelationType) fParent.getFeatureType().getProperty(IProjectInfoModel.QN_DOCUMENTS);
        final Feature fClone = FeatureHelper.cloneFeature(fParent, relation, fDocument);
        final String newName = CloneRenaming.getNewName(sFileName);
        FeatureUtils.updateProperty(pool.getWorkspace(), fClone, IDocumentMember.QN_NAME, newName);
        sFileName = "Copy of " + sFileName;
        File destFile = new File(documentFolder.getFile(sFileName).getLocation().toOSString());
        if (destFile.exists()) {
            int count = 1;
            while (destFile.exists()) {
                destFile = new File(documentFolder.getFile(count + " - " + sFileName).getLocation().toOSString());
                count++;
            }
            FeatureUtils.updateProperty(pool.getWorkspace(), fClone, IDocumentMember.QN_FILE_NAME, --count + " - " + sFileName);
        } else FeatureUtils.updateProperty(pool.getWorkspace(), fClone, IDocumentMember.QN_FILE_NAME, sFileName);
        FileUtils.copyFile(srcFile, destFile);
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
    }

    public static void deleteDocument(final PoolProjectInfo pool, final Feature fDocument) throws Exception {
        if (fDocument == null) return;
        final Object objFileName = fDocument.getProperty(IDocumentMember.QN_FILE_NAME);
        final DeleteFeatureCommand delCmd = new DeleteFeatureCommand(fDocument);
        pool.postCommand(delCmd, null);
        if (!(objFileName instanceof String) || ((String) objFileName).equals("")) return;
        final String sFileName = (String) objFileName;
        final IProject project = NofdpCorePlugin.getProjectManager().getProjectToEdit();
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
        final IFolder documentFolder = project.getFolder(IProjectInfoModel.DOCUMENT_FOLDER);
        final File file = new File(documentFolder.getFile(sFileName).getLocation().toOSString());
        if (file.exists()) FileUtils.forceDelete(file);
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
    }

    public static void openDocument(final Feature fDocument) {
        if (fDocument == null) return;
        final Object objFileName = fDocument.getProperty(IDocumentMember.QN_FILE_NAME);
        if (!(objFileName instanceof String) || ((String) objFileName).equals("")) return;
        final String sFileName = (String) objFileName;
        final IProject project = NofdpCorePlugin.getProjectManager().getProjectToEdit();
        final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final FileEditorInput input = new FileEditorInput(project.getFile(IProjectInfoModel.DOCUMENT_FOLDER + "/" + sFileName));
        try {
            IDE.openEditor(activePage, input, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
        } catch (final PartInitException e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
        }
    }
}
