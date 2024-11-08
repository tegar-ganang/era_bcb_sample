package com.ibm.celldt.alf.ui.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.ui.ManagedBuilderUIPlugin;
import com.ibm.celldt.alf.ui.Messages;
import com.ibm.celldt.alf.ui.wizard.ALFWizard;

/**
 * This class implements the back-end actions that are to be performed after the user has input all of the necessary parameters
 * 
 * @author Sean Curry
 * @since 3.0.0
 */
public class ALFWizardCreationAction implements IRunnableWithProgress {

    private static String PATH_SEPERATOR = "/";

    private static String SPACE = " ";

    private ALFWizard wizard;

    private IPath projectLocation;

    private String projectName;

    private int stackSize;

    private int expAccelNum;

    private int partitionMethod;

    private ArrayList buffers;

    private boolean is64bit;

    public ALFWizardCreationAction(ALFWizard wizard, IPath projectLocation, String projectName, int stackSize, int expAccelNum, int partitionMethod, ArrayList buffers, boolean is64bit) {
        this.wizard = wizard;
        this.projectLocation = projectLocation;
        this.projectName = projectName;
        this.stackSize = stackSize;
        this.expAccelNum = expAccelNum;
        this.partitionMethod = partitionMethod;
        this.buffers = buffers;
        this.is64bit = is64bit;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        boolean wasCanceled = false;
        File tmpDir = null;
        File xmlParameterFile = null;
        IProject ppuProject = null;
        IProject spuProject = null;
        IProject libraryProject = null;
        try {
            String date = getDate();
            monitor.beginTask(Messages.ALFWizardCreationAction_mainTask, 9);
            monitor.setTaskName(Messages.ALFWizardCreationAction_taskName);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask1);
            String tmpDirPath = Messages.ALFWizardCreationAction_tmpDirLocation + date;
            tmpDir = new File(tmpDirPath);
            if (!tmpDir.exists()) tmpDir.mkdir();
            Document xmlParameterFileDocument = createXMLParameterDocument(tmpDir.getAbsolutePath(), date);
            xmlParameterFile = createXMLParameterFile(xmlParameterFileDocument, tmpDir);
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.worked(1);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask2);
            String codeGenLocation = Messages.ALFWizardCreationAction_codeGeneratorCommand;
            File codeGenFile = new File(Messages.ALFWizardCreationAction_codeGeneratorCommand);
            if (!codeGenFile.exists()) {
                codeGenLocation = wizard.queryPathLocation(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_queryCodeGenPath, Messages.ALFWizardCreationAction_codeGeneratorCommand);
            }
            if (codeGenLocation == null) throw new InvocationTargetException(new Exception(), Messages.ALFWizardCreationAction_errorCallingCodeGenMsg);
            Process codeGen = Runtime.getRuntime().exec(codeGenLocation + SPACE + Messages.ALFWizardCreationAction_codeGeneratorOptions + SPACE + xmlParameterFile.getAbsolutePath());
            codeGen.waitFor();
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.worked(1);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask3);
            ppuProject = createProject("ppu_" + projectName, "celldt.managedbuild.target.cell.ppu.exe");
            monitor.worked(1);
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask4);
            libraryProject = createProject("lib" + projectName, "celldt.managedbuild.target.cell.ppu.so");
            monitor.worked(1);
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask5);
            spuProject = createProject("spu_" + projectName, "celldt.managedbuild.target.cell.spu.exe");
            monitor.worked(1);
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask6);
            importFiles(monitor, tmpDirPath, ppuProject, spuProject);
            monitor.worked(1);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask7);
            configureSPUProject(spuProject, ppuProject.getName());
            try {
                spuProject.refreshLocal(2, null);
            } catch (CoreException e) {
            }
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.worked(1);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask8);
            try {
                IProjectDescription projectDescription = libraryProject.getDescription();
                projectDescription.setReferencedProjects(new IProject[] { spuProject });
                libraryProject.setDescription(projectDescription, null);
            } catch (Exception e) {
            }
            configureSharedLibraryProject32bit(libraryProject, spuProject.getName());
            configureSharedLibraryProject64bit(libraryProject, spuProject.getName());
            try {
                libraryProject.refreshLocal(2, null);
            } catch (CoreException e) {
            }
            monitor.worked(1);
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            monitor.subTask(Messages.ALFWizardCreationAction_subTask9);
            try {
                IProjectDescription projectDescription = ppuProject.getDescription();
                projectDescription.setReferencedProjects(new IProject[] { libraryProject });
                ppuProject.setDescription(projectDescription, null);
            } catch (Exception e) {
            }
            configurePPUProject32bit(ppuProject, libraryProject.getName(), spuProject.getName());
            configurePPUProject64bit(ppuProject, libraryProject.getName(), spuProject.getName());
            try {
                ppuProject.refreshLocal(2, null);
            } catch (CoreException e) {
            }
            monitor.worked(1);
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
        } catch (FactoryConfigurationError e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingParamFileMsg);
        } catch (ParserConfigurationException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingParamFileMsg);
        } catch (TransformerException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingParamFileMsg);
        } catch (IOException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCallingCodeGenMsg);
        } catch (InterruptedException e) {
            wasCanceled = true;
            throw e;
        } finally {
            if (xmlParameterFile != null) xmlParameterFile.delete();
            if (tmpDir != null) deleteDir(tmpDir);
            if (wasCanceled) {
                if (ppuProject != null) {
                    try {
                        ppuProject.delete(true, true, null);
                    } catch (CoreException e) {
                    }
                }
                if (spuProject != null) {
                    try {
                        spuProject.delete(true, true, null);
                    } catch (CoreException e) {
                    }
                }
                if (libraryProject != null) {
                    try {
                        libraryProject.delete(true, true, null);
                    } catch (CoreException e) {
                    }
                }
            }
        }
    }

    /**
	 * Configures the build properties of the host (PPU) project's 32bit GNU configuration to include the necessary libraries, look-up paths, and embed-SPU input.
	 * 
	 * @param ppuProject the PPU project to configure
	 * @param spuProjectName the name of the SPU project, so the embed SPU input can be correctly specified
	 * @return true if the options were successfully saved in the configuration, else false
	 */
    private boolean configurePPUProject32bit(IProject ppuProject, String libProjectName, String spuProjectName) {
        boolean saveBuildStatus = false;
        try {
            IConfiguration[] configs = ManagedBuildManager.getBuildInfo(ppuProject).getManagedProject().getConfigurations();
            IConfiguration configuration = null;
            for (int i = 0; i < configs.length; i++) {
                if (configs[i].getId().startsWith("celldt.managedbuild.config.cell.ppu.gnu32.exe.debug")) {
                    configuration = configs[i];
                    break;
                }
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_OPEN, true);
            IToolChain toolChain = configuration.getToolChain();
            ITool linkerTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.ppu.gnu32.c.linker")[0];
            IOption libraryOption = linkerTool.getOptionBySuperClassId("cell.ppu.gnu.c.linker.option.libs");
            IOption librarySearchPathOption = linkerTool.getOptionBySuperClassId("gnu.c.link.option.paths");
            ManagedBuildManager.setOption(configuration, linkerTool, libraryOption, new String[] { "dl", "pthread", "spe2", "alf" });
            ManagedBuildManager.setOption(configuration, linkerTool, librarySearchPathOption, new String[] { "/opt/cell/sysroot/usr/lib" });
            ITool compilerTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.ppu.gnu32.c.compiler.debug")[0];
            IOption includePathOption = compilerTool.getOptionBySuperClassId("gnu.c.compiler.option.include.paths");
            ManagedBuildManager.setOption(configuration, compilerTool, includePathOption, new String[] { "/opt/cell/sysroot/usr/include", "/opt/cell/sysroot/opt/cell/sdk/usr/include" });
            saveBuildStatus = ManagedBuildManager.saveBuildInfo(ppuProject, true);
            IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(ppuProject);
            if (bi != null & bi instanceof ManagedBuildInfo) ((ManagedBuildInfo) bi).initializePathEntries();
            if (!is64bit) {
                ManagedBuildManager.setDefaultConfiguration(ppuProject, configuration);
                ManagedBuildManager.setSelectedConfiguration(ppuProject, configuration);
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_APPLY, true);
            if (!saveBuildStatus) throw new Exception();
        } catch (Exception e) {
            wizard.openWarningMessage(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_errorConfiguringPpuProject);
        }
        return saveBuildStatus;
    }

    /**
	 * Configures the PPU projects 64bit GNU configuration. 
	 * 
	 * @param ppuProject the PPU project to configure
	 * @param spuProjectName the name of the SPU project
	 * @return true if the options were successfully saved in the configuration, else false
	 */
    private boolean configurePPUProject64bit(IProject ppuProject, String libProjectName, String spuProjectName) {
        boolean saveBuildStatus = false;
        try {
            IConfiguration[] configs = ManagedBuildManager.getBuildInfo(ppuProject).getManagedProject().getConfigurations();
            IConfiguration configuration = null;
            for (int i = 0; i < configs.length; i++) {
                if (configs[i].getId().startsWith("celldt.managedbuild.config.cell.ppu.gnu64.exe.debug")) {
                    configuration = configs[i];
                    break;
                }
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_OPEN, true);
            IToolChain toolChain = configuration.getToolChain();
            ITool linkerTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.ppu.gnu64.c.linker")[0];
            IOption libraryOption = linkerTool.getOptionBySuperClassId("cell.ppu.gnu.c.linker.option.libs");
            IOption librarySearchPathOption = linkerTool.getOptionBySuperClassId("gnu.c.link.option.paths");
            ManagedBuildManager.setOption(configuration, linkerTool, libraryOption, new String[] { "dl", "pthread", "spe2", "alf" });
            ManagedBuildManager.setOption(configuration, linkerTool, librarySearchPathOption, new String[] { "/opt/cell/sysroot/usr/lib64" });
            ITool compilerTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.ppu.gnu64.c.compiler.debug")[0];
            IOption includePathOption = compilerTool.getOptionBySuperClassId("gnu.c.compiler.option.include.paths");
            ManagedBuildManager.setOption(configuration, compilerTool, includePathOption, new String[] { "/opt/cell/sysroot/usr/include", "/opt/cell/sysroot/opt/cell/sdk/usr/include" });
            IOption definedSymbolOption = compilerTool.getOptionBySuperClassId("gnu.c.compiler.option.preprocessor.def.symbols");
            ManagedBuildManager.setOption(configuration, compilerTool, definedSymbolOption, new String[] { "__64BIT__" });
            saveBuildStatus = ManagedBuildManager.saveBuildInfo(ppuProject, true);
            IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(ppuProject);
            if (bi != null & bi instanceof ManagedBuildInfo) ((ManagedBuildInfo) bi).initializePathEntries();
            if (is64bit) {
                ManagedBuildManager.setDefaultConfiguration(ppuProject, configuration);
                ManagedBuildManager.setSelectedConfiguration(ppuProject, configuration);
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_APPLY, true);
            if (!saveBuildStatus) throw new Exception();
        } catch (Exception e) {
            wizard.openWarningMessage(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_errorConfiguringPpuProject);
        }
        return saveBuildStatus;
    }

    private boolean configureSharedLibraryProject32bit(IProject libProject, String spuProjectName) {
        boolean saveBuildStatus = false;
        try {
            IConfiguration[] configs = ManagedBuildManager.getBuildInfo(libProject).getManagedProject().getConfigurations();
            IConfiguration configuration = null;
            for (int i = 0; i < configs.length; i++) {
                if (configs[i].getId().startsWith("celldt.managedbuild.config.cell.ppu.gnu32.so.debug")) {
                    configuration = configs[i];
                    break;
                }
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_OPEN, true);
            IToolChain toolChain = configuration.getToolChain();
            ITool embedSpuTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.ppu.gnu32.embedspu")[0];
            IOption embedSpuInputOption = embedSpuTool.getOptionBySuperClassId("cell.ppu.gnu.embedspu.option.inputs");
            ManagedBuildManager.setOption(configuration, embedSpuTool, embedSpuInputOption, new String[] { "\"${workspace_loc:/" + spuProjectName + "/spu-gnu-debug/" + spuProjectName + "}\"" });
            saveBuildStatus = ManagedBuildManager.saveBuildInfo(libProject, true);
            IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(libProject);
            if (bi != null & bi instanceof ManagedBuildInfo) ((ManagedBuildInfo) bi).initializePathEntries();
            if (!is64bit) {
                ManagedBuildManager.setDefaultConfiguration(libProject, configuration);
                ManagedBuildManager.setSelectedConfiguration(libProject, configuration);
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_APPLY, true);
            if (!saveBuildStatus) throw new Exception();
        } catch (Exception e) {
            wizard.openWarningMessage(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_errorConfiguringSharedLibraryProject);
        }
        return saveBuildStatus;
    }

    private boolean configureSharedLibraryProject64bit(IProject libProject, String spuProjectName) {
        boolean saveBuildStatus = false;
        try {
            IConfiguration[] configs = ManagedBuildManager.getBuildInfo(libProject).getManagedProject().getConfigurations();
            IConfiguration configuration = null;
            for (int i = 0; i < configs.length; i++) {
                if (configs[i].getId().startsWith("celldt.managedbuild.config.cell.ppu.gnu64.so.debug")) {
                    configuration = configs[i];
                    break;
                }
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_OPEN, true);
            IToolChain toolChain = configuration.getToolChain();
            ITool embedSpuTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.ppu.gnu64.embedspu")[0];
            IOption embedSpuInputOption = embedSpuTool.getOptionBySuperClassId("cell.ppu.gnu.embedspu.option.inputs");
            ManagedBuildManager.setOption(configuration, embedSpuTool, embedSpuInputOption, new String[] { "\"${workspace_loc:/" + spuProjectName + "/spu-gnu-debug/" + spuProjectName + "}\"" });
            saveBuildStatus = ManagedBuildManager.saveBuildInfo(libProject, true);
            IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(libProject);
            if (bi != null & bi instanceof ManagedBuildInfo) ((ManagedBuildInfo) bi).initializePathEntries();
            if (is64bit) {
                ManagedBuildManager.setDefaultConfiguration(libProject, configuration);
                ManagedBuildManager.setSelectedConfiguration(libProject, configuration);
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_APPLY, true);
            if (!saveBuildStatus) throw new Exception();
        } catch (Exception e) {
            wizard.openWarningMessage(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_errorConfiguringSharedLibraryProject);
        }
        return saveBuildStatus;
    }

    /**
	 * Configures the build properties of the accelerator (SPU) project to include the alf libraries and library look-up paths.
	 * @param spuProject the SPU project to be configured
	 * @param ppuProjectName the name of the PPU project
	 */
    private boolean configureSPUProject(IProject spuProject, String ppuProjectName) {
        boolean saveBuildStatus = false;
        try {
            IConfiguration[] configs = ManagedBuildManager.getBuildInfo(spuProject).getManagedProject().getConfigurations();
            IConfiguration configuration = null;
            for (int i = 0; i < configs.length; i++) {
                if (configs[i].getId().startsWith("celldt.managedbuild.config.cell.spu.gnu.exe.debug")) {
                    configuration = configs[i];
                    break;
                }
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_OPEN, true);
            IToolChain toolChain = configuration.getToolChain();
            ITool linkerTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.spu.gnu.c.linker")[0];
            IOption libraryOption = linkerTool.getOptionBySuperClassId("cell.gnu.c.linker.option.libs");
            IOption librarySearchPathOption = linkerTool.getOptionBySuperClassId("gnu.c.link.option.paths");
            ManagedBuildManager.setOption(configuration, linkerTool, libraryOption, new String[] { "alf" });
            ManagedBuildManager.setOption(configuration, linkerTool, librarySearchPathOption, new String[] { "/opt/cell/sysroot/usr/spu/lib" });
            ITool compilerTool = toolChain.getToolsBySuperClassId("celldt.managedbuild.tool.cell.spu.gnu.c.compiler.debug")[0];
            IOption includePathOption = compilerTool.getOptionBySuperClassId("gnu.c.compiler.option.include.paths");
            ManagedBuildManager.setOption(configuration, compilerTool, includePathOption, new String[] { "/opt/cell/sysroot/usr/spu/include", "\"${workspace_loc:/" + ppuProjectName + "}\"" });
            saveBuildStatus = ManagedBuildManager.saveBuildInfo(spuProject, true);
            IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(spuProject);
            if (bi != null & bi instanceof ManagedBuildInfo) {
                ((ManagedBuildInfo) bi).initializePathEntries();
            }
            ManagedBuildManager.performValueHandlerEvent(configuration, IManagedOptionValueHandler.EVENT_APPLY, true);
            if (!saveBuildStatus) throw new Exception();
        } catch (Exception e) {
            wizard.openWarningMessage(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_errorConfiguringSpuProject);
        }
        return saveBuildStatus;
    }

    private void cloneProjectConfigurations(IProject project) {
        IConfiguration[] configs = ManagedBuildManager.getBuildInfo(project).getManagedProject().getConfigurations();
        for (int i = 0; i < configs.length; i++) {
            ManagedBuildManager.performValueHandlerEvent(configs[i], IManagedOptionValueHandler.EVENT_OPEN, true);
            IToolChain tempToolChain = configs[i].getToolChain();
            cloneTools(tempToolChain, configs[i]);
            ManagedBuildManager.performValueHandlerEvent(configs[i], IManagedOptionValueHandler.EVENT_APPLY, true);
        }
    }

    private void cloneTools(IToolChain toolChain, IConfiguration configuration) {
        ITool[] tools = toolChain.getTools();
        for (int i = 0; i < tools.length; i++) {
            ITool newTool = toolChain.createTool(tools[i], ManagedBuildManager.calculateChildId(tools[i].getId(), null), tools[i].getName(), false);
            IOption[] options = tools[i].getOptions();
            for (int j = 0; j < options.length; j++) {
                newTool.createOption(options[j], ManagedBuildManager.calculateChildId(options[j].getId(), null), options[j].getName(), false);
            }
        }
        toolChain.setDirty(true);
    }

    private IProject createProject(String name, String projectTypeID) throws InvocationTargetException {
        IProject newProject = null;
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IProject newProjectHandle = workspace.getRoot().getProject(name);
            IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
            if (Platform.getLocation().equals(projectLocation)) description.setLocation(null); else description.setLocation(projectLocation);
            newProject = CCorePlugin.getDefault().createCProject(description, newProjectHandle, null, ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);
            ManagedCProjectNature.addManagedNature(newProject, null);
            ManagedCProjectNature.addManagedBuilder(newProject, null);
            IManagedProject newManagedProject = null;
            IManagedBuildInfo info = null;
            info = ManagedBuildManager.createBuildInfo(newProject);
            IProjectType parent = ManagedBuildManager.getExtensionProjectType(projectTypeID);
            newManagedProject = ManagedBuildManager.createManagedProject(newProject, parent);
            if (newManagedProject != null) {
                IConfiguration[] selectedConfigs = parent.getConfigurations();
                for (int i = 0; i < selectedConfigs.length; i++) {
                    IConfiguration config = selectedConfigs[i];
                    int id = ManagedBuildManager.getRandomNumber();
                    IConfiguration newConfig = newManagedProject.createConfigurationClone(config, config.getId() + "." + id);
                    newConfig.setArtifactName(newManagedProject.getDefaultArtifactName());
                }
                IConfiguration defaultCfg = null;
                IConfiguration[] newConfigs = newManagedProject.getConfigurations();
                for (int i = 0; i < newConfigs.length; i++) {
                    if (newConfigs[i].isSupported()) {
                        defaultCfg = newConfigs[i];
                        break;
                    }
                }
                if (defaultCfg == null && newConfigs.length > 0) defaultCfg = newConfigs[0];
                if (defaultCfg != null) {
                    ManagedBuildManager.setDefaultConfiguration(newProject, defaultCfg);
                    ManagedBuildManager.setSelectedConfiguration(newProject, defaultCfg);
                }
                ManagedBuildManager.setNewProjectVersion(newProject);
                ICDescriptor desc = null;
                try {
                    desc = CCorePlugin.getDefault().getCProjectDescription(newProject, true);
                    desc.create(CCorePlugin.BUILD_SCANNER_INFO_UNIQ_ID, ManagedBuildManager.INTERFACE_IDENTITY);
                } catch (CoreException e) {
                    ManagedBuilderUIPlugin.log(e);
                }
                if (info != null) {
                    info.setValid(true);
                    ManagedBuildManager.saveBuildInfo(newProject, true);
                }
            }
            IStatus initResult = ManagedBuildManager.initBuildInfoContainer(newProject);
            if (initResult.getCode() != IStatus.OK) {
                ManagedBuilderUIPlugin.log(initResult);
            }
            cloneProjectConfigurations(newProject);
        } catch (OperationCanceledException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingProject);
        } catch (CoreException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingProject);
        } catch (BuildException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingProject);
        }
        return newProject;
    }

    private Document createXMLParameterDocument(String outputDir, String date) throws InvocationTargetException, ParserConfigurationException {
        String tempString;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.newDocument();
        Element alfRoot = doc.createElement(Messages.ALFWizardCreationAction_tag_alf);
        Element global = doc.createElement(Messages.ALFWizardCreationAction_tag_global);
        Element alfVersion = doc.createElement(Messages.ALFWizardCreationAction_tag_ALF_VERSION);
        Text alfVersionText = doc.createTextNode(Messages.ALFWizardCreationAction_alfVersion);
        alfVersion.appendChild(alfVersionText);
        Element dateTime = doc.createElement(Messages.ALFWizardCreationAction_tag_DATE_TIME);
        Text dateTimeText = doc.createTextNode(date);
        dateTime.appendChild(dateTimeText);
        Element templateDir = doc.createElement(Messages.ALFWizardCreationAction_tag_TEMPLATE_DIR);
        String tDirPath = Messages.ALFWizardCreationAction_templateDir;
        File tDir = new File(tDirPath);
        if (!tDir.exists()) {
            tDirPath = wizard.queryPathLocation(Messages.ALFWizardCreationAction_queryTitle, Messages.ALFWizardCreationAction_queryTemplateDirMessage, Messages.ALFWizardCreationAction_templateDir);
        }
        if (tDirPath == null) {
            throw new InvocationTargetException(new Exception(), Messages.ALFWizardCreationAction_errorTemplateDir);
        }
        Text templateDirText = doc.createTextNode(tDirPath);
        templateDir.appendChild(templateDirText);
        Element targetDir = doc.createElement(Messages.ALFWizardCreationAction_tag_TARGET_DIR);
        Text targetDirText = doc.createTextNode(outputDir);
        targetDir.appendChild(targetDirText);
        Element projectName = doc.createElement(Messages.ALFWizardCreationAction_tag_PROJECT_NAME);
        Text projectNameText = doc.createTextNode(this.projectName);
        projectName.appendChild(projectNameText);
        Element stackSize = doc.createElement(Messages.ALFWizardCreationAction_tag_STACK_SIZE);
        Text stackSizeText = doc.createTextNode(this.stackSize + "");
        stackSize.appendChild(stackSizeText);
        Element partitionMethod = doc.createElement(Messages.ALFWizardCreationAction_tag_PARTITION_METHOD);
        Text partitionMethodText = doc.createTextNode(this.partitionMethod + "");
        partitionMethod.appendChild(partitionMethodText);
        Element expAccelNum = doc.createElement(Messages.ALFWizardCreationAction_tag_EXP_ACCEL_NUM);
        Text expAccelNumText = doc.createTextNode(this.expAccelNum + "");
        expAccelNum.appendChild(expAccelNumText);
        Element bufferNumber = doc.createElement(Messages.ALFWizardCreationAction_tag_BUFFER_NUMBER);
        Text bufferNumberText = doc.createTextNode(this.buffers.size() + "");
        bufferNumber.appendChild(bufferNumberText);
        doc.appendChild(alfRoot);
        alfRoot.appendChild(global);
        global.appendChild(alfVersion);
        global.appendChild(dateTime);
        global.appendChild(templateDir);
        global.appendChild(targetDir);
        global.appendChild(projectName);
        global.appendChild(stackSize);
        global.appendChild(partitionMethod);
        global.appendChild(expAccelNum);
        global.appendChild(bufferNumber);
        for (int i = 0; i < buffers.size(); i++) {
            ALFBuffer buf = (ALFBuffer) buffers.get(i);
            Element buffer = doc.createElement(Messages.ALFWizardCreationAction_tag_buffer);
            alfRoot.appendChild(buffer);
            Element variableName = doc.createElement(Messages.ALFWizardCreationAction_tag_VARIABLE_NAME);
            Text variableNameText = doc.createTextNode(buf.getName());
            variableName.appendChild(variableNameText);
            Element elementType = doc.createElement(Messages.ALFWizardCreationAction_tag_ELEMENT_TYPE);
            Text elementTypeText = doc.createTextNode(buf.getElementType());
            elementType.appendChild(elementTypeText);
            Element elementUnit = doc.createElement(Messages.ALFWizardCreationAction_tag_ELEMENT_UNIT);
            tempString = null;
            switch(buf.getElementUnit()) {
                case ALFConstants.ALF_DATA_BYTE:
                    tempString = Messages.ALFWizardCreationAction_elementUnitByte;
                    break;
                case ALFConstants.ALF_DATA_INT16:
                    tempString = Messages.ALFWizardCreationAction_elementUnitInt16;
                    break;
                case ALFConstants.ALF_DATA_INT32:
                    tempString = Messages.ALFWizardCreationAction_elementUnitInt32;
                    break;
                case ALFConstants.ALF_DATA_INT64:
                    tempString = Messages.ALFWizardCreationAction_elementUnitInt64;
                    break;
                case ALFConstants.ALF_DATA_FLOAT:
                    tempString = Messages.ALFWizardCreationAction_elementUnitFloat;
                    break;
                case ALFConstants.ALF_DATA_DOUBLE:
                    tempString = Messages.ALFWizardCreationAction_elementUnitDouble;
                    break;
                case ALFConstants.ALF_DATA_ADDR32:
                    tempString = Messages.ALFWizardCreationAction_elementUnitAddr32;
                    break;
                case ALFConstants.ALF_DATA_ADDR64:
                    tempString = Messages.ALFWizardCreationAction_elementUnitAddr64;
                    break;
                case ALFConstants.ALF_DATA_ELEMENT_TYPE:
                    tempString = buf.getElementType();
                    break;
                default:
                    tempString = null;
            }
            Text elementUnitText = doc.createTextNode(tempString);
            elementUnit.appendChild(elementUnitText);
            Element bufferType = doc.createElement(Messages.ALFWizardCreationAction_tag_BUFFER_TYPE);
            tempString = null;
            switch(buf.getBufferType()) {
                case ALFConstants.ALF_BUFFER_INPUT:
                    tempString = Messages.ALFWizardCreationAction_bufferTypeInput;
                    break;
                case ALFConstants.ALF_BUFFER_OUTPUT:
                    tempString = Messages.ALFWizardCreationAction_bufferTypeOutput;
                    break;
                default:
                    tempString = null;
            }
            Text bufferTypeText = doc.createTextNode(tempString);
            bufferType.appendChild(bufferTypeText);
            Element numDimension = doc.createElement(Messages.ALFWizardCreationAction_tag_NUM_DIMENSION);
            int actualNumDim = buf.getNumDimensions() + 1;
            Text numDimensionText = doc.createTextNode(actualNumDim + "");
            numDimension.appendChild(numDimensionText);
            Element dimensionSizeX = doc.createElement(Messages.ALFWizardCreationAction_tag_DIMENSION_SIZE_X);
            Text dimensionSizeXText = doc.createTextNode(buf.getDimensionSizeX() + "");
            dimensionSizeX.appendChild(dimensionSizeXText);
            Element dimensionSizeY = doc.createElement(Messages.ALFWizardCreationAction_tag_DIMENSION_SIZE_Y);
            Text dimensionSizeYText = doc.createTextNode(buf.getDimensionSizeY() + "");
            dimensionSizeY.appendChild(dimensionSizeYText);
            Element dimensionSizeZ = doc.createElement(Messages.ALFWizardCreationAction_tag_DIMENSION_SIZE_Z);
            Text dimensionSizeZText = doc.createTextNode(buf.getDimensionSizeZ() + "");
            dimensionSizeZ.appendChild(dimensionSizeZText);
            Element distributionModelX = doc.createElement(Messages.ALFWizardCreationAction_tag_DISTRIBUTION_MODEL_X);
            tempString = null;
            switch(buf.getDistributionModelX()) {
                case ALFConstants.DIST_MODEL_STAR:
                    tempString = Messages.ALFWizardCreationAction_distributionModelStar;
                    break;
                case ALFConstants.DIST_MODEL_BLOCK:
                    tempString = Messages.ALFWizardCreationAction_distributionModelBlock;
                    break;
                case ALFConstants.DIST_MODEL_CYCLIC:
                    tempString = Messages.ALFWizardCreationAction_distributionModelCyclic;
                    break;
                default:
                    tempString = null;
            }
            Text distributionModelXText = doc.createTextNode(tempString);
            distributionModelX.appendChild(distributionModelXText);
            Element distributionModelY = doc.createElement(Messages.ALFWizardCreationAction_tag_DISTRIBUTION_MODEL_Y);
            tempString = null;
            switch(buf.getDistributionModelY()) {
                case ALFConstants.DIST_MODEL_STAR:
                    tempString = Messages.ALFWizardCreationAction_distributionModelStar;
                    break;
                case ALFConstants.DIST_MODEL_BLOCK:
                    tempString = Messages.ALFWizardCreationAction_distributionModelBlock;
                    break;
                case ALFConstants.DIST_MODEL_CYCLIC:
                    tempString = Messages.ALFWizardCreationAction_distributionModelCyclic;
                    break;
                default:
                    tempString = null;
            }
            Text distributionModelYText = doc.createTextNode(tempString);
            distributionModelY.appendChild(distributionModelYText);
            Element distributionModelZ = doc.createElement(Messages.ALFWizardCreationAction_tag_DISTRIBUTION_MODEL_Z);
            tempString = null;
            switch(buf.getDistributionModelZ()) {
                case ALFConstants.DIST_MODEL_STAR:
                    tempString = Messages.ALFWizardCreationAction_distributionModelStar;
                    break;
                case ALFConstants.DIST_MODEL_BLOCK:
                    tempString = Messages.ALFWizardCreationAction_distributionModelBlock;
                    break;
                case ALFConstants.DIST_MODEL_CYCLIC:
                    tempString = Messages.ALFWizardCreationAction_distributionModelCyclic;
                    break;
                default:
                    tempString = null;
            }
            Text distributionModelZText = doc.createTextNode(tempString);
            distributionModelZ.appendChild(distributionModelZText);
            Element distributionSizeX = doc.createElement(Messages.ALFWizardCreationAction_tag_DISTRIBUTION_SIZE_X);
            Text distributionSizeXText = doc.createTextNode(buf.getDistributionSizeX() + "");
            distributionSizeX.appendChild(distributionSizeXText);
            Element distributionSizeY = doc.createElement(Messages.ALFWizardCreationAction_tag_DISTRIBUTION_SIZE_Y);
            Text distributionSizeYText = doc.createTextNode(buf.getDistributionSizeY() + "");
            distributionSizeY.appendChild(distributionSizeYText);
            Element distributionSizeZ = doc.createElement(Messages.ALFWizardCreationAction_tag_DISTRIBUTION_SIZE_Z);
            Text distributionSizeZText = doc.createTextNode(buf.getDistributionSizeZ() + "");
            distributionSizeZ.appendChild(distributionSizeZText);
            buffer.appendChild(variableName);
            buffer.appendChild(elementType);
            buffer.appendChild(elementUnit);
            buffer.appendChild(bufferType);
            buffer.appendChild(numDimension);
            int numDim = buf.getNumDimensions();
            switch(numDim) {
                case ALFConstants.ONE_DIMENSIONAL:
                    buffer.appendChild(dimensionSizeX);
                    buffer.appendChild(distributionModelX);
                    buffer.appendChild(distributionSizeX);
                    break;
                case ALFConstants.TWO_DIMENSIONAL:
                    buffer.appendChild(dimensionSizeX);
                    buffer.appendChild(dimensionSizeY);
                    buffer.appendChild(distributionModelX);
                    buffer.appendChild(distributionSizeX);
                    buffer.appendChild(distributionModelY);
                    buffer.appendChild(distributionSizeY);
                    break;
                case ALFConstants.THREE_DIMENSIONAL:
                    buffer.appendChild(dimensionSizeX);
                    buffer.appendChild(dimensionSizeY);
                    buffer.appendChild(dimensionSizeZ);
                    buffer.appendChild(distributionModelX);
                    buffer.appendChild(distributionSizeX);
                    buffer.appendChild(distributionModelY);
                    buffer.appendChild(distributionSizeY);
                    buffer.appendChild(distributionModelZ);
                    buffer.appendChild(distributionSizeZ);
                    break;
                default:
                    throw new InvocationTargetException(new Exception(), Messages.ALFWizardCreationAction_errorCreatingParamFileMsg);
            }
        }
        return doc;
    }

    private static void copyFile(String src, String dst) throws InvocationTargetException {
        try {
            FileChannel srcChannel;
            srcChannel = new FileInputStream(src).getChannel();
            FileChannel dstChannel = new FileOutputStream(dst).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (FileNotFoundException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorSourceFilesNotFound);
        } catch (IOException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCopyingFiles);
        }
    }

    private File createXMLParameterFile(Document doc, File tmpDir) throws InvocationTargetException, TransformerException {
        try {
            File xmlFile = new File(tmpDir, Messages.ALFWizardCreationAction_xmlParamFileName);
            if (!xmlFile.exists()) xmlFile.createNewFile();
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();
            Source src = new DOMSource(doc);
            Result dest = new StreamResult(xmlFile.getPath());
            aTransformer.transform(src, dest);
            return xmlFile;
        } catch (IOException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCreatingParamFileMsg);
        }
    }

    private void deleteDir(File file) {
        if (file == null) return;
        if (file.isFile()) file.delete();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) deleteDir(files[i]);
            file.delete();
        }
    }

    /**
	 * Returns the current date in a format which is not obtainable using the Calendar.getTime()
	 * @return string representation of the current date and time, in the formate: YYYY-MM-DD_HH:MM:SS
	 */
    private String getDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        String month = (cal.get(Calendar.MONTH) + 1) + "";
        if (month.length() == 1) {
            month = "0" + month;
        }
        String day = cal.get(Calendar.DAY_OF_MONTH) + "";
        if (day.length() == 1) {
            day = "0" + day;
        }
        String hour = cal.get(Calendar.HOUR_OF_DAY) + "";
        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        String minute = cal.get(Calendar.MINUTE) + "";
        if (minute.length() == 1) {
            minute = "0" + minute;
        }
        String second = cal.get(Calendar.SECOND) + "";
        if (second.length() == 1) {
            second = "0" + second;
        }
        return cal.get(Calendar.YEAR) + "-" + month + "-" + day + "_" + hour + ":" + minute + ":" + second;
    }

    private void importFiles(IProgressMonitor monitor, String tmpDirPath, IProject ppuProject, IProject spuProject) throws InvocationTargetException, InterruptedException {
        try {
            String ppuFilePath = tmpDirPath + PATH_SEPERATOR + "host" + PATH_SEPERATOR + "ppu_" + projectName + ".c";
            File ppuFile = new File(ppuFilePath);
            String spuFilePath = tmpDirPath + PATH_SEPERATOR + "accel" + PATH_SEPERATOR + "spu_" + projectName + ".c";
            File spuFile = new File(spuFilePath);
            if (!ppuFile.exists() || !spuFile.exists()) throw new InvocationTargetException(new Exception(), Messages.ALFWizardCreationAction_errorSourceFilesNotFound);
            File newPPUFile = new File(ppuProject.getLocation().toString() + PATH_SEPERATOR + "ppu_" + projectName + ".c");
            newPPUFile.createNewFile();
            copyFile(ppuFile.getAbsolutePath(), newPPUFile.getAbsolutePath());
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            File newSPUFile = new File(spuProject.getLocation().toString() + PATH_SEPERATOR + "spu_" + projectName + ".c");
            newSPUFile.createNewFile();
            copyFile(spuFile.getAbsolutePath(), newSPUFile.getAbsolutePath());
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            File headerFile = new File(tmpDirPath + PATH_SEPERATOR + "common.h");
            if (!headerFile.exists()) throw new InvocationTargetException(new Exception(), Messages.ALFWizardCreationAction_errorHeaderFileNotFound);
            File newHeaderFile = new File(ppuProject.getLocation().toString() + PATH_SEPERATOR + "common.h");
            newHeaderFile.createNewFile();
            copyFile(headerFile.getAbsolutePath(), newHeaderFile.getAbsolutePath());
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
            File hostHeaderFile = new File(tmpDirPath + PATH_SEPERATOR + "host" + PATH_SEPERATOR + "ppu_" + projectName + ".h");
            if (!hostHeaderFile.exists()) throw new InvocationTargetException(new Exception(), Messages.ALFWizardCreationAction_errorHeaderFileNotFound);
            File newHostHeaderFile = new File(ppuProject.getLocation().toString() + PATH_SEPERATOR + "ppu_" + projectName + ".h");
            newHostHeaderFile.createNewFile();
            copyFile(hostHeaderFile.getAbsolutePath(), newHostHeaderFile.getAbsolutePath());
            if (monitor.isCanceled()) throw new InterruptedException(Messages.ALFWizardCreationAction_canceled);
        } catch (IOException e) {
            throw new InvocationTargetException(e, Messages.ALFWizardCreationAction_errorCopyingFiles);
        }
    }
}
