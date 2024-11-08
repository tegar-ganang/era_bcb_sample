package com.prolix.editor.graph.templates.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import com.prolix.editor.dialogs.BasicGLMDialog;
import com.prolix.editor.graph.commands.ChainCommand;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.graph.templates.commands.detailelements.GraphicalTemplateGetGeneratetElement;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;

/**
 * <<class description>>
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public abstract class GraphicalTemplateCommand extends ChainCommand {

    private ModelDiagramMain parent;

    private Rectangle constraints;

    private GraphicalTemplateBasicDialog dialog;

    private boolean enableCommand;

    public GraphicalTemplateCommand(String commandName) {
        super(commandName);
        enableCommand = true;
    }

    public abstract GraphicalTemplateBasicDialog createConfigDialog();

    public BasicGLMDialog getConfigDialog() {
        return dialog;
    }

    private void openDialog() {
        dialog = createConfigDialog();
        if (dialog == null) return;
        enableCommand = dialog.openDialog();
    }

    public void setConstraints(Rectangle constraints) {
        this.constraints = constraints;
    }

    /**
	 * @param parent
	 *           the ModelDiagramMain to set
	 */
    public void setModelDiagramMain(ModelDiagramMain parent) {
        this.parent = parent;
    }

    public Rectangle getConstraints() {
        if (constraints == null) constraints = new Rectangle();
        return constraints;
    }

    public boolean canExecute() {
        return true;
    }

    public final void execute() {
        openDialog();
        if (!enableCommand) return;
        getLearningDesignDataModel().getEditor().getGraphicalViewer().deselectAll();
        createNewRoles();
        createEnvironments();
        createNewActivities();
        createNewPoints();
        createInteractions();
        assignRoles();
        assignEnvironments();
        assignOperations();
        assignConnections();
        assignTextResources();
        super.execute();
    }

    public void redo() {
        if (!enableCommand) return;
        super.redo();
    }

    public void undo() {
        if (!enableCommand) return;
        super.undo();
    }

    protected void createNewActivities() {
    }

    protected void createNewPoints() {
    }

    protected void createNewRoles() {
    }

    protected void assignRoles() {
    }

    protected void assignConnections() {
    }

    protected void createEnvironments() {
    }

    protected void createInteractions() {
    }

    protected void assignEnvironments() {
    }

    protected void assignOperations() {
    }

    protected void assignTextResources() {
    }

    /**
	 * @return the parent
	 */
    public ModelDiagramMain getParent() {
        if (parent == null) throw new IllegalAccessError();
        return parent;
    }

    protected LearningDesignDataModel getLearningDesignDataModel() {
        return (LearningDesignDataModel) getParent().getLearningDesign().getDataModel();
    }

    public GraphicalTemplateGetGeneratetElement addCommandWithGeneratedElement(Command command) {
        super.addCommand(command);
        if (command instanceof GraphicalTemplateGetGeneratetElement) return (GraphicalTemplateGetGeneratetElement) command;
        return null;
    }
}
