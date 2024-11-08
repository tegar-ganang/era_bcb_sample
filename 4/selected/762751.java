package org.kalypso.nofdpidss.ui.view.wizard.project.document;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.core.KalypsoCorePlugin;
import org.kalypso.gmlschema.IGMLSchema;
import org.kalypso.gmlschema.feature.IFeatureType;
import org.kalypso.gmlschema.property.IPropertyType;
import org.kalypso.gmlschema.property.relation.IRelationType;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.project.info.IDocumentMember;
import org.kalypso.nofdpidss.core.base.gml.model.project.info.IProjectInfoModel;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolProjectInfo;
import org.kalypso.nofdpidss.ui.i18n.Messages;
import org.kalypso.nofdpidss.ui.view.wizard.project.document.NewDocumentSettings.DOCUMENT_TYPE;
import org.kalypso.ogc.gml.mapmodel.CommandableWorkspace;
import org.kalypso.ogc.gml.selection.IFeatureSelectionManager;
import org.kalypso.ui.editor.gmleditor.util.command.AddFeatureCommand;
import org.kalypsodeegree.model.feature.Feature;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * @author Dirk Kuch
 */
public class WizardNewDocument extends Wizard implements IWorkbenchWizard {

    private PageSelectFileWizard m_selectFilePage;

    private PageDocumentSettings m_settingsPage;

    private final NewDocumentSettings m_settings;

    public WizardNewDocument(final PoolProjectInfo pool) {
        final IProject projectToEdit = NofdpCorePlugin.getProjectManager().getProjectToEdit();
        WorkspaceSync.sync(projectToEdit, IResource.DEPTH_INFINITE);
        m_settings = new NewDocumentSettings(projectToEdit);
        m_settings.m_workspace = pool.getWorkspace();
        setHelpAvailable(false);
    }

    /**
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
    @Override
    public void addPages() {
        setWindowTitle(Messages.WizardNewDocument_0);
        m_selectFilePage = new PageSelectFileWizard(m_settings);
        addPage(m_selectFilePage);
        m_settingsPage = new PageDocumentSettings(m_settings);
        addPage(m_settingsPage);
    }

    /**
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
   *      org.eclipse.jface.viewers.IStructuredSelection)
   */
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
    }

    @Override
    public boolean performFinish() {
        try {
            final File srcFile = new File(m_settings.m_documentFilePath.getText());
            final File destFile = m_settings.m_documentFolder.getFile(m_settings.m_tFileName.getText()).getLocation().toFile();
            FileUtils.copyFile(srcFile, destFile);
            WorkspaceSync.sync(m_settings.m_project, IResource.DEPTH_INFINITE);
            final CommandableWorkspace workspace = m_settings.m_workspace;
            final Feature rootFeature = workspace.getRootFeature();
            final IFeatureSelectionManager selectionManager = KalypsoCorePlugin.getDefault().getSelectionManager();
            final IRelationType relation = (IRelationType) rootFeature.getFeatureType().getProperty(IProjectInfoModel.QN_DOCUMENTS);
            IFeatureType targetFeatureType = null;
            final IGMLSchema schema = workspace.getGMLSchema();
            final DOCUMENT_TYPE type = NewDocumentSettings.DOCUMENT_TYPE.getType(m_settings.m_tFileName.getText());
            switch(type) {
                case notDefined:
                    return true;
                case general:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_GENERAL);
                    break;
                case image:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_IMAGE);
                    break;
                case pdf:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_PDF);
                    break;
                case text:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_TEXT);
                    break;
                case spreadsheet:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_SPREADSHEET);
                    break;
                case presentation:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_PRESENTATION);
                    break;
                case archive:
                    targetFeatureType = schema.getFeatureType(IDocumentMember.QN_TYPE_ARCHIVE);
                    break;
                default:
                    return true;
            }
            final Map<IPropertyType, Object> properties = new HashMap<IPropertyType, Object>();
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_NAME), m_settings.m_tName.getText());
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_FILE_NAME), m_settings.m_tFileName.getText());
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_DESCRIPTION), m_settings.m_tDesc.getText());
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_DATA_SOURCE), m_settings.m_documentFilePath.getText());
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_COPYRIGHT), m_settings.m_tCopy.getText());
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_ORIGIN), m_settings.m_tSupply.getText());
            properties.put(targetFeatureType.getProperty(IDocumentMember.QN_IMPORT_DATE), new XMLGregorianCalendarImpl(new GregorianCalendar()));
            final AddFeatureCommand command = new AddFeatureCommand(workspace, targetFeatureType, rootFeature, relation, -1, properties, selectionManager, -1);
            workspace.postCommand(command);
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
        }
        return true;
    }
}
