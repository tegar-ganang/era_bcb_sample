package de.jmda.mview;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.log4j.Logger;
import de.jmda.mview.proj.Project;
import de.jmda.mview.proj.ProjectModel;
import de.jmda.mview.proj.ProjectModelRepository;
import de.jmda.mview.proj.ProjectModelRepositoryStore;
import de.jmda.mview.proj.ProjectModelSelectionDialog;

/**                                                                                    
 * This class was initially generated by {@link
 * de.jmda.gui.swing.mvc.ControllerSupportEnabledGenerator}.    
 * <p>                                                                                 
 * <code>ControllerSupportEnabledGenerator</code> will not regenerate this
 * class, hence modifications will not get lost due to subsequent generation by
 * <code>ControllerSupportEnabledGenerator</code>.                                         
 */
class MViewController extends de.jmda.gui.swing.mvc.AbstractController<MViewModel> {

    private static final Logger LOGGER = Logger.getLogger(MViewController.class);

    private MViewControllerContext context;

    private MViewModel model = new MViewModel(ProjectModelRepository.createDefaultProjectModelRepository());

    /**
	 * Constructor                                                                        
	 */
    MViewController(de.jmda.gui.swing.mvc.ControllerContextProvider controllerContextProvider) {
        super(controllerContextProvider);
        context = (MViewControllerContext) controllerContextProvider.getControllerContext();
    }

    @Override
    public void refreshControllerContext() {
        context = (MViewControllerContext) controllerContextProvider.getControllerContext();
    }

    @Override
    public void setModel(MViewModel model) {
        this.model = model;
    }

    public MViewModel getModel() {
        return model;
    }

    /**
	 * Delegates to {@link MViewModel#updateProjectModelRepository(File)} if
	 * <code>projectRepositoryFileName</code> is not <code>null</code> and the
	 * corresponding file exists. If the update of the model's project repository
	 * file succeeds {@link #updateView()} will also be called. If a project is
	 * selected it will be displayed.
	 * <p>
	 * If the file corresponding to <code>projectRepositoryFileName</code> does
	 * not exist, a message will be displayed.
	 * <p>
	 * If <code>projectRepositoryFileName</code> is null {@link
	 * #createProjectModelRepository()} will be called.
	 *
	 * @param projectRepositoryFileName
	 */
    public void updateProjectRepository(String projectRepositoryFileName) {
        if (projectRepositoryFileName != null) {
            File projectRepositoryFile = new File(projectRepositoryFileName);
            if (projectRepositoryFile.exists()) {
                if (model.updateProjectModelRepository(projectRepositoryFile)) {
                    updateView();
                }
            } else {
                JOptionPane.showMessageDialog(context.getMView(), "Project repository file [" + projectRepositoryFile.getAbsolutePath() + "] does not exist!");
            }
        } else {
            createProjectModelRepository();
        }
    }

    public void saveRepositoryActionPerformed(ActionEvent evt) {
        ProjectModelRepositoryStore.store(model.getProjectModelRepository());
    }

    /**
	 * If <code>projectModelRepository</code> contains exactly one {@link
	 * ProjectModel} it will be used to create and return a new <code>Project
	 * </code> instance.
	 * <p>
	 * If <code>projectModelRepository</code> contains more than one <code>
	 * ProjectModel</code>s, {@link ProjectModelSelectionDialog} will be
	 * displayed. The selected project model will be used to create and return a
	 * new <code>Project</code> instance.
	 * <p>
	 * If <code>projectModelRepository</code> contains no <code>ProjectModel
	 * </code> instances or the user does not select one <code>null</code> will be
	 * returned.
	 *
	 * @param projectModelRepository
	 * @return
	 */
    private Project selectProject(ProjectModelRepository projectModelRepository) {
        Project result = null;
        Set<ProjectModel> projectModels = projectModelRepository.getProjectModels();
        if (projectModels.size() > 0) {
            if (projectModels.size() == 1) {
                result = new Project(projectModels.iterator().next());
            } else {
                ProjectModelSelectionDialog dialog = new ProjectModelSelectionDialog(projectModels);
                dialog.setVisible(true);
                ProjectModel selectedProjectModel = dialog.getSelectedProjectModel();
                if (selectedProjectModel != null) {
                    result = new Project(selectedProjectModel);
                }
            }
        }
        return result;
    }

    private void createProjectModelRepository() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "*" + ProjectModelRepositoryStore.FILE_EXTENSION + " (mview project repositories)";
            }

            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(ProjectModelRepositoryStore.FILE_EXTENSION);
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int state = fileChooser.showDialog(context.getMView(), "new project repository");
        if (state == JFileChooser.APPROVE_OPTION) {
            File projectModelRepositoryFile = fileChooser.getSelectedFile();
            if (projectModelRepositoryFile.exists()) {
                Object[] options = new String[] { "overwrite", "load", "cancel" };
                int n = JOptionPane.showOptionDialog(context.getMView(), "Load or overwrite file [" + projectModelRepositoryFile.getAbsolutePath() + "]", "file [" + projectModelRepositoryFile.getAbsolutePath() + "] " + "already exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                if (n == 2) {
                    return;
                }
                if (n == 0) {
                    ProjectModelRepository projectModelRepository = ProjectModelRepository.createDefaultProjectModelRepository();
                    projectModelRepository.setFile(projectModelRepositoryFile);
                    ProjectModelRepositoryStore.store(projectModelRepository);
                }
                if (model.updateProjectModelRepository(projectModelRepositoryFile)) {
                    updateView();
                } else {
                    LOGGER.error("failure updating project model repository");
                }
            } else {
                if (false == projectModelRepositoryFile.getName().endsWith(ProjectModelRepositoryStore.FILE_EXTENSION)) {
                    projectModelRepositoryFile = new File(projectModelRepositoryFile.getAbsolutePath() + ProjectModelRepositoryStore.FILE_EXTENSION);
                }
                ProjectModelRepository projectModelRepository = ProjectModelRepository.createDefaultProjectModelRepository();
                projectModelRepository.setFile(projectModelRepositoryFile);
                ProjectModelRepositoryStore.store(projectModelRepository);
                if (model.updateProjectModelRepository(projectModelRepositoryFile)) {
                    updateView();
                } else {
                    LOGGER.error("failure updating project model repository");
                }
            }
        }
    }

    private void updateView() {
        Container contentPane = context.getMView().getContentPane();
        contentPane.removeAll();
        Project project = selectProject(model.getProjectModelRepository());
        if (project == null) {
            project = new Project();
            model.getProjectModelRepository().add(project.getModel());
        }
        contentPane.add(project);
        contentPane.revalidate();
    }
}
