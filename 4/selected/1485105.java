package net.sourceforge.iwii.db.dev.ui.model.artifacts;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.AbstractTableModel;
import net.sourceforge.iwii.db.dev.bo.project.ProjectBO;
import net.sourceforge.iwii.db.dev.bo.project.ProjectPhaseBO;
import net.sourceforge.iwii.db.dev.bo.project.ProjectSubPhaseBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.ProjectArtifactBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase5.UseCaseBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase5.UseCaseModelArtifactDataBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase5.UseCaseOperationBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase5.UseCaseOperationDataPackageBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase5.UseCaseOperationDataPackagePropertyBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase6.EntityClassBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase6.EntityClassModelArtifactDataBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase6.EntityClassPropertyBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase7.DatabaseOperationBO;
import net.sourceforge.iwii.db.dev.bo.project.artifact.phase7.DatabaseOperationModelArtifactDataBO;
import net.sourceforge.iwii.db.dev.common.enumerations.UseCaseOperationStates;
import net.sourceforge.iwii.db.dev.common.interfaces.IModel;
import net.sourceforge.iwii.db.dev.common.utils.ServiceInjector;
import net.sourceforge.iwii.db.dev.logic.fascade.api.IProjectManagmentFascade;

/**
 * Class represents database operation model artifact data model.
 * 
 * @author Grzegorz 'Gregor736' Wolszczak
 * @version 1.00
 */
public class DatabaseOperationModelArtifactDataModel implements IModel {

    private class DatabaseOperationsTableModel extends AbstractTableModel {

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            return artifactData.getOperations().size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DatabaseOperationBO operation = artifactData.getOperations().get(rowIndex);
            StringBuilder ucOperations = new StringBuilder();
            for (UseCaseOperationBO ucOperation : operation.getOperations()) {
                ucOperations.append("UCO/");
                ucOperations.append(ucOperation.getInProjectId());
                ucOperations.append("<br>");
            }
            StringBuilder readEntities = new StringBuilder();
            for (EntityClassPropertyBO readEntity : operation.getReadEntities()) {
                readEntities.append("ENT/");
                readEntities.append(readEntity.getEntityClass().getInProjectId());
                readEntities.append(": ");
                readEntities.append(readEntity.getEntityClass().getEntityName());
                readEntities.append(" - ");
                readEntities.append(readEntity.getProperty());
                readEntities.append("<br>");
            }
            StringBuilder writeEntities = new StringBuilder();
            for (EntityClassPropertyBO writeEntity : operation.getWriteEntities()) {
                writeEntities.append("ENT/");
                writeEntities.append(writeEntity.getEntityClass().getInProjectId());
                writeEntities.append(": ");
                writeEntities.append(writeEntity.getEntityClass().getEntityName());
                writeEntities.append(" - ");
                writeEntities.append(writeEntity.getProperty());
                writeEntities.append("<br>");
            }
            return new String[] { "DBO/" + operation.getInProjectId(), operation.getName(), ucOperations.toString(), readEntities.toString(), writeEntities.toString() }[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int column) {
            return new String[] { "Id operacji", "Nazwa operacji", "Realizowane operacjie PU", "Odczytywane własności encji", "Zapisywane własności encji" }[column];
        }
    }

    private class UCOperationsListModel extends AbstractListModel {

        @Override
        public Object getElementAt(int index) {
            return selectedOperation.getOperations().get(index);
        }

        @Override
        public int getSize() {
            return selectedOperation.getOperations().size();
        }
    }

    private class ReadEntitiesListModel extends AbstractListModel {

        @Override
        public Object getElementAt(int index) {
            return selectedOperation.getReadEntities().get(index);
        }

        @Override
        public int getSize() {
            return selectedOperation.getReadEntities().size();
        }
    }

    private class WriteEntitiesListModel extends AbstractListModel {

        @Override
        public Object getElementAt(int index) {
            return selectedOperation.getWriteEntities().get(index);
        }

        @Override
        public int getSize() {
            return selectedOperation.getWriteEntities().size();
        }
    }

    private DatabaseOperationModelArtifactDataBO artifactData;

    private DatabaseOperationsTableModel databaseOperationsTableModel = new DatabaseOperationsTableModel();

    private UCOperationsListModel uCOperationsListModel = new UCOperationsListModel();

    private ReadEntitiesListModel readEntitiesListModel = new ReadEntitiesListModel();

    private WriteEntitiesListModel writeEntitiesListModel = new WriteEntitiesListModel();

    private DatabaseOperationBO selectedOperation;

    private boolean selectedOperationEdited;

    public DatabaseOperationModelArtifactDataBO getArtifactData() {
        return artifactData;
    }

    public void setArtifactData(DatabaseOperationModelArtifactDataBO artifactData) {
        this.artifactData = artifactData;
    }

    public DatabaseOperationsTableModel getDatabaseOperationsTableModel() {
        return databaseOperationsTableModel;
    }

    public ReadEntitiesListModel getReadEntitiesListModel() {
        return readEntitiesListModel;
    }

    public UCOperationsListModel getUCOperationsListModel() {
        return uCOperationsListModel;
    }

    public WriteEntitiesListModel getWriteEntitiesListModel() {
        return writeEntitiesListModel;
    }

    public DatabaseOperationBO getSelectedOperation() {
        return selectedOperation;
    }

    public void setSelectedOperation(DatabaseOperationBO selectedOperation) {
        this.selectedOperation = selectedOperation;
    }

    public boolean isSelectedOperationEdited() {
        return selectedOperationEdited;
    }

    public void setSelectedOperationEdited(boolean selectedOperationEdited) {
        this.selectedOperationEdited = selectedOperationEdited;
    }

    private List<UseCaseOperationBO> getUCOperations() {
        ProjectBO project = artifactData.getArtifactVersion().getArtifact().getSubphase().getSuperPhase().getProject();
        ProjectSubPhaseBO useCaseSubPhase = null;
        for (ProjectPhaseBO phase : project.getPhases()) {
            if (phase.getName().equals("Model przypadków użycia")) {
                useCaseSubPhase = phase.getSubPhases().get(0);
                break;
            }
        }
        UseCaseModelArtifactDataBO useCaseModel = null;
        for (ProjectArtifactBO artifact : useCaseSubPhase.getArtifacts()) {
            if (artifact.getVersions().get(0).getData() instanceof UseCaseModelArtifactDataBO) {
                useCaseModel = (UseCaseModelArtifactDataBO) artifact.getVersions().get(0).getData();
                break;
            }
        }
        List<UseCaseOperationBO> result = new LinkedList<UseCaseOperationBO>();
        for (UseCaseBO useCase : useCaseModel.getUseCases()) {
            result.addAll(useCase.getUseCaseOperations());
        }
        return result;
    }

    private List<EntityClassBO> getEntities() {
        ProjectBO project = artifactData.getArtifactVersion().getArtifact().getSubphase().getSuperPhase().getProject();
        ProjectSubPhaseBO entitiesModelSubPhase = null;
        for (ProjectPhaseBO phase : project.getPhases()) {
            if (phase.getName().equals("Model klas encyjnych")) {
                entitiesModelSubPhase = phase.getSubPhases().get(0);
                break;
            }
        }
        EntityClassModelArtifactDataBO entitiesModel = null;
        for (ProjectArtifactBO artifact : entitiesModelSubPhase.getArtifacts()) {
            if (artifact.getVersions().get(0).getData() instanceof EntityClassModelArtifactDataBO) {
                entitiesModel = (EntityClassModelArtifactDataBO) artifact.getVersions().get(0).getData();
                break;
            }
        }
        return entitiesModel.getEntityClasses();
    }

    public List<EntityClassPropertyBO> getEntityProperties() {
        List<EntityClassPropertyBO> properties = new LinkedList<EntityClassPropertyBO>();
        for (EntityClassBO bo : getEntities()) {
            properties.addAll(bo.getProperties());
        }
        return properties;
    }

    private Set<EntityClassPropertyBO> getMappingsForProperty(List<UseCaseOperationDataPackagePropertyBO> properties, List<EntityClassBO> entities) {
        Set<EntityClassPropertyBO> mapped = new HashSet<EntityClassPropertyBO>();
        for (UseCaseOperationDataPackagePropertyBO property : properties) {
            for (EntityClassBO entity : entities) {
                for (EntityClassPropertyBO entityProperty : entity.getProperties()) {
                    if (entityProperty.getPropertyMapping() != null && entityProperty.getPropertyMapping().equals(property)) {
                        mapped.add(entityProperty);
                    }
                }
            }
        }
        return mapped;
    }

    private List<UseCaseOperationDataPackagePropertyBO> getProperties(List<UseCaseOperationDataPackageBO> packages) {
        List<UseCaseOperationDataPackagePropertyBO> properties = new LinkedList<UseCaseOperationDataPackagePropertyBO>();
        for (UseCaseOperationDataPackageBO dataPackage : packages) {
            properties.addAll(dataPackage.getProperties());
        }
        return properties;
    }

    public ComboBoxModel getUCOperationsComboBoxModel() {
        return new DefaultComboBoxModel(this.getUCOperations().toArray());
    }

    public ComboBoxModel getEntitiesComboBoxModel() {
        return new DefaultComboBoxModel(this.getEntityProperties().toArray());
    }

    public void addUCOperation(UseCaseOperationBO operation) {
        this.getSelectedOperation().getOperations().add(operation);
        operation.getTraceToDatabaseOperations().add(this.getSelectedOperation());
        List<UseCaseOperationDataPackagePropertyBO> ucProperties = this.getProperties(operation.getDataPackages());
        Set<EntityClassPropertyBO> properties = this.getMappingsForProperty(ucProperties, this.getEntities());
        if (operation.getState().equals(UseCaseOperationStates.Read)) {
            for (EntityClassPropertyBO bo : properties) {
                if (!this.getSelectedOperation().getReadEntities().contains(bo)) {
                    this.getSelectedOperation().getReadEntities().add(bo);
                }
            }
        } else if (operation.getState().equals(UseCaseOperationStates.Write)) {
            for (EntityClassPropertyBO bo : properties) {
                if (!this.getSelectedOperation().getWriteEntities().contains(bo)) {
                    this.getSelectedOperation().getWriteEntities().add(bo);
                }
            }
        }
    }

    @Override
    public void clear() {
    }

    public void save() {
        ProjectBO project = this.artifactData.getArtifactVersion().getArtifact().getSubphase().getSuperPhase().getProject();
        project.increaseRevision();
        this.artifactData.getArtifactVersion().increaseRevision();
        ServiceInjector.injectService(IProjectManagmentFascade.class).simpleUpdate(project);
    }
}
