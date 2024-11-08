package ingenias.module;

import ingenias.editor.ProjectProperty;
import ingenias.editor.actions.LoadFileSwingTask;
import ingenias.exception.NullEntity;
import ingenias.generator.browser.*;
import ingenias.genproject.ProjectGenerator;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.jgraph.JGraph;
import sun.awt.VerticalBagLayout;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

/**
 *  This class generates a project folder
 *@author     Jorge Gomez
 *@created    13 of October of 2009
 */
public class IAFProjectCreator extends ingenias.editor.extension.BasicToolImp {

    private JDialog dialog = null;

    private JRadioButton rad1;

    private JRadioButton rad2;

    private JRadioButton rad3;

    private JRadioButton rad4;

    /**
	 *  Initialises the class with a file containing a INGENIAS specification
	 *
	 *@param  file           Path to file containing INGENIAS specification
	 *@exception  Exception  Error accessing any file or malformed XML exception
	 */
    public IAFProjectCreator(String file) throws Exception {
        super(file);
    }

    /**
	 *  Initialises the class giving access to diagrams in run-time
	 **/
    public IAFProjectCreator(Browser browser) throws Exception {
        super(browser);
    }

    @Override
    public String getVersion() {
        return "@iaf.ver@";
    }

    /**
	 *  Gets the description of this module
	 *
	 *@return    The description
	 */
    public String getDescription() {
        return "It creates a full project";
    }

    /**
	 *  Gets the name of this module
	 *
	 *@return    The name
	 */
    public String getName() {
        return "IAFProjectCreator";
    }

    /**
	 *  It opens the different files generated under the ingenias/jade/components folder looking
	 *  for specific tags. These tags mark the beginning and the end of the modification
	 */
    public void run() {
        final JFileChooser chooser = new JFileChooser();
        if (!this.getIds().prefs.getWorkspacePath().equals("")) {
            chooser.setCurrentDirectory(new File(this.getIds().prefs.getWorkspacePath()));
        }
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dialog = new JDialog(getResources().getMainFrame());
        dialog.setTitle("Project creation wizard");
        final JTextField projectName = new JTextField(20);
        final JLabel finalProjectPath = new JLabel("To be created at: ?");
        projectName.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
                    createProjectInLocation(projectName);
                }
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
                finalProjectPath.setText("To be created at: " + getIds().prefs.getWorkspacePath() + "/" + projectName.getText() + arg0.getKeyChar());
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                dialog.setVisible(false);
                dialog = null;
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup exampleSelGroup = new ButtonGroup();
        JPanel exampleSelection = new JPanel(new GridLayout(5, 1));
        GridBagConstraints gbc = new GridBagConstraints();
        JLabel exampleSelectionLabel = new JLabel("Choose a template for the specification:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        exampleSelection.add(exampleSelectionLabel);
        JPanel option1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        rad1 = new javax.swing.JRadioButton();
        exampleSelGroup.add(rad1);
        option1.add(rad1);
        option1.add(new javax.swing.JLabel("Empty specification"));
        exampleSelection.add(option1);
        JPanel option2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        rad2 = new javax.swing.JRadioButton();
        exampleSelGroup.add(rad2);
        option2.add(rad2);
        option2.add(new javax.swing.JLabel("Hello world"));
        exampleSelection.add(option2);
        JPanel option3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        rad3 = new javax.swing.JRadioButton();
        exampleSelGroup.add(rad3);
        option3.add(rad3);
        option3.add(new javax.swing.JLabel("Example GUI agent"));
        exampleSelection.add(option3);
        JPanel option4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        rad4 = new javax.swing.JRadioButton();
        exampleSelGroup.add(rad4);
        option4.add(rad4);
        option4.add(new javax.swing.JLabel("Example Interaction"));
        exampleSelection.add(option4);
        rad1.setSelected(true);
        buttonPanel.add(cancel);
        JButton create = new JButton("Create project in selected location");
        create.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                createProjectInLocation(projectName);
            }
        });
        Box mainPanel = Box.createVerticalBox();
        JPanel folderSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel directoryLabel = new JLabel("Project name:");
        folderSelectionPanel.add(directoryLabel);
        folderSelectionPanel.add(projectName);
        mainPanel.add(folderSelectionPanel);
        JPanel moreInfoOnProjectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        moreInfoOnProjectPanel.add(finalProjectPath);
        mainPanel.add(moreInfoOnProjectPanel);
        moreInfoOnProjectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        moreInfoOnProjectPanel.add(new JLabel("To change the project path, use preferences ->switch workspace"));
        mainPanel.add(moreInfoOnProjectPanel);
        buttonPanel.add(create);
        buttonPanel.add(cancel);
        mainPanel.add(exampleSelection);
        mainPanel.add(buttonPanel);
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocation(ingenias.editor.utils.DialogWindows.getCenter(dialog.getSize(), getResources().getMainFrame()));
        dialog.setVisible(true);
    }

    private void createProjectInLocation(final JTextField projectName) {
        if (projectName.getText() != null && !projectName.getText().equals("")) {
            File newFolder = new File(getIds().prefs.getWorkspacePath() + "/" + projectName.getText());
            if (newFolder.exists()) {
                if (!newFolder.isDirectory()) {
                    JOptionPane.showMessageDialog(dialog, "A directory has to be chosen and a project name " + projectName.getText() + " conflicts with an existing file which is not a directory", "No directory selected", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    if (JOptionPane.showConfirmDialog(dialog, "There is already a folder matching the name of the project at " + newFolder.getAbsolutePath() + ". Do you want to reuse it?", "Confirm overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
            } else {
                newFolder.mkdirs();
            }
            dialog.setVisible(false);
            SpecificationTemplateKind stk = SpecificationTemplateKind.NoTemplate;
            if (rad2.isSelected()) stk = SpecificationTemplateKind.HelloWorld;
            if (rad3.isSelected()) stk = SpecificationTemplateKind.GUIAgent;
            if (rad4.isSelected()) stk = SpecificationTemplateKind.Interaction;
            new IAFProjectCreatorSwingTask(projectName.getText(), stk, getIdeUpdater(), getIds(), getResources()).execute();
        } else {
            JOptionPane.showMessageDialog(dialog, "No directory has been chosen. Please, press on the browse button to select one", "No directory selected", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getContent(File task) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(task);
            int read = 0;
            byte[] bytesRead = new byte[100];
            StringBuffer sb = new StringBuffer();
            while (read > -1) {
                try {
                    read = fis.read(bytesRead);
                    for (int k = 0; k < read; k++) {
                        sb.append((char) bytesRead[k]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    /**
	 *  This module defines no properties
	 *
	 *@return    Empty properties
	 */
    public Vector<ProjectProperty> defaultProperties() {
        Vector<ProjectProperty> result = new Vector<ProjectProperty>();
        return result;
    }

    /**
	 *  Generates an stats report from a INGENIAS specification file (1st param)
	 *
	 *@param  args           Arguments typed in the command line. Only first one is attended
	 *@exception  Exception  Sth went wrong
	 */
    public static void main(String args[]) throws Exception {
        ingenias.editor.Log.initInstance(new java.io.PrintWriter(System.err));
        System.exit(0);
    }
}
