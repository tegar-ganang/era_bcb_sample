package net.sf.mzmine.modules.projectmethods.projectsave;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.FileNameParameter;
import net.sf.mzmine.util.ExitCode;

public class ProjectSaveParameters extends SimpleParameterSet {

    private static final FileFilter filters[] = new FileFilter[] { new FileNameExtensionFilter("MZmine projects", "mzmine") };

    public static final FileNameParameter projectFile = new FileNameParameter("Project file", "File name of project to be saved");

    public ProjectSaveParameters() {
        super(new Parameter[] { projectFile });
    }

    public ExitCode showSetupDialog() {
        File currentProjectFile = MZmineCore.getCurrentProject().getProjectFile();
        if ((currentProjectFile != null) && (currentProjectFile.canWrite())) {
            getParameter(projectFile).setValue(currentProjectFile);
            return ExitCode.OK;
        }
        JFileChooser chooser = new JFileChooser();
        for (FileFilter filter : filters) chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(false);
        int returnVal = chooser.showSaveDialog(MZmineCore.getDesktop().getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) return ExitCode.CANCEL;
        File selectedFile = chooser.getSelectedFile();
        if (!selectedFile.getName().endsWith(".mzmine")) {
            selectedFile = new File(selectedFile.getPath() + ".mzmine");
        }
        if (selectedFile.exists()) {
            int selectedValue = JOptionPane.showConfirmDialog(MZmineCore.getDesktop().getMainFrame(), selectedFile.getName() + " already exists, overwrite ?", "Question...", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (selectedValue != JOptionPane.YES_OPTION) return ExitCode.CANCEL;
        }
        getParameter(projectFile).setValue(selectedFile);
        return ExitCode.OK;
    }
}
