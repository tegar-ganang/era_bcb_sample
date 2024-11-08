package net.sourceforge.appgen.job;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.appgen.editor.MappingDataEditor;
import net.sourceforge.appgen.generator.AttachFileFileGenerator;
import net.sourceforge.appgen.generator.AttachFilePersisterFileGenerator;
import net.sourceforge.appgen.generator.AttachFilePropertyEditorFileGenerator;
import net.sourceforge.appgen.generator.BaseCriteriaFileGenerator;
import net.sourceforge.appgen.generator.BaseServiceFileGenerator;
import net.sourceforge.appgen.generator.ControllerFileGenerator;
import net.sourceforge.appgen.generator.CriteriaFileGenerator;
import net.sourceforge.appgen.generator.DaoClassFileGenerator;
import net.sourceforge.appgen.generator.DaoInterfaceFileGenerator;
import net.sourceforge.appgen.generator.DefaultMethodInvocationLoggerFileGenerator;
import net.sourceforge.appgen.generator.DefaultPagingFileGenerator;
import net.sourceforge.appgen.generator.DetailPageFileGenerator;
import net.sourceforge.appgen.generator.EditPageFileGenerator;
import net.sourceforge.appgen.generator.EntityFileGenerator;
import net.sourceforge.appgen.generator.FileGenerator;
import net.sourceforge.appgen.generator.FilenameGeneratorFileGenerator;
import net.sourceforge.appgen.generator.FormControllerFileGenerator;
import net.sourceforge.appgen.generator.IndexFileGenerator;
import net.sourceforge.appgen.generator.JdbcDriverFileGenerator;
import net.sourceforge.appgen.generator.JdbcPropertiesFileGenerator;
import net.sourceforge.appgen.generator.ListPageFileGenerator;
import net.sourceforge.appgen.generator.Log4jDtdFileGenerator;
import net.sourceforge.appgen.generator.Log4jXmlFileGenerator;
import net.sourceforge.appgen.generator.MessageKoPropertiesFileGenerator;
import net.sourceforge.appgen.generator.MessagePropertiesFileGenerator;
import net.sourceforge.appgen.generator.MethodInvocationInfoInterceptorFileGenerator;
import net.sourceforge.appgen.generator.MethodInvocationLoggerFileGenerator;
import net.sourceforge.appgen.generator.MethodInvocationLoggingAdviceFileGenerator;
import net.sourceforge.appgen.generator.PagingFileGenerator;
import net.sourceforge.appgen.generator.PomFileGenerator;
import net.sourceforge.appgen.generator.ResourcePropertiesFileGenerator;
import net.sourceforge.appgen.generator.ServiceClassFileGenerator;
import net.sourceforge.appgen.generator.ServiceInterfaceFileGenerator;
import net.sourceforge.appgen.generator.ServletXmlFileGenerator;
import net.sourceforge.appgen.generator.SqlMapConfigFileGenerator;
import net.sourceforge.appgen.generator.SqlMapFileGenerator;
import net.sourceforge.appgen.generator.StyleFileGenerator;
import net.sourceforge.appgen.generator.TagsFileGenerator;
import net.sourceforge.appgen.generator.UUIDFilenameGeneratorFileGenerator;
import net.sourceforge.appgen.generator.UploadSaveDirectoryGenerator;
import net.sourceforge.appgen.generator.ValidatorFileGenerator;
import net.sourceforge.appgen.generator.WebXmlFileGenerator;
import net.sourceforge.appgen.generator.WritePageFileGenerator;
import net.sourceforge.appgen.model.ConnectionInformation;
import net.sourceforge.appgen.model.Entity;
import net.sourceforge.appgen.model.GenerationInformation;
import net.sourceforge.appgen.model.MappingData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.RefreshAction;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.EditorPart;

/**
 * @author Byeongkil Woo
 */
public class GenerateFileJob extends Job {

    private EditorPart editor;

    private MappingData mappingData;

    public GenerateFileJob(MappingData mappingData, EditorPart editor, String text) {
        super(text);
        this.mappingData = mappingData;
        this.editor = editor;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                runInternal(monitor);
            }
        });
        return Status.OK_STATUS;
    }

    private void runInternal(IProgressMonitor monitor) {
        try {
            List<FileGenerator> fileGeneratorList = new ArrayList<FileGenerator>();
            ConnectionInformation connectionInformation = mappingData.getConnectionInformation();
            GenerationInformation generationInformation = mappingData.getGenerationInformation();
            List<Entity> entityList = mappingData.getEntityList();
            fileGeneratorList.add(new PomFileGenerator(generationInformation));
            fileGeneratorList.add(new WebXmlFileGenerator(generationInformation));
            fileGeneratorList.add(new ServletXmlFileGenerator(generationInformation, entityList));
            fileGeneratorList.add(new SqlMapConfigFileGenerator(generationInformation, entityList));
            fileGeneratorList.add(new Log4jDtdFileGenerator(generationInformation));
            fileGeneratorList.add(new Log4jXmlFileGenerator(generationInformation));
            fileGeneratorList.add(new MessagePropertiesFileGenerator(generationInformation));
            fileGeneratorList.add(new MessageKoPropertiesFileGenerator(generationInformation));
            fileGeneratorList.add(new JdbcPropertiesFileGenerator(generationInformation, connectionInformation));
            fileGeneratorList.add(new ResourcePropertiesFileGenerator(generationInformation));
            fileGeneratorList.add(new JdbcDriverFileGenerator(generationInformation, connectionInformation));
            fileGeneratorList.add(new BaseCriteriaFileGenerator(generationInformation));
            fileGeneratorList.add(new BaseServiceFileGenerator(generationInformation));
            fileGeneratorList.add(new PagingFileGenerator(generationInformation));
            fileGeneratorList.add(new UploadSaveDirectoryGenerator(generationInformation));
            fileGeneratorList.add(new AttachFileFileGenerator(generationInformation));
            fileGeneratorList.add(new AttachFilePersisterFileGenerator(generationInformation));
            fileGeneratorList.add(new AttachFilePropertyEditorFileGenerator(generationInformation));
            fileGeneratorList.add(new FilenameGeneratorFileGenerator(generationInformation));
            fileGeneratorList.add(new UUIDFilenameGeneratorFileGenerator(generationInformation));
            fileGeneratorList.add(new DefaultMethodInvocationLoggerFileGenerator(generationInformation));
            fileGeneratorList.add(new MethodInvocationInfoInterceptorFileGenerator(generationInformation));
            fileGeneratorList.add(new MethodInvocationLoggerFileGenerator(generationInformation));
            fileGeneratorList.add(new MethodInvocationLoggingAdviceFileGenerator(generationInformation));
            fileGeneratorList.add(new EntityFileGenerator(generationInformation));
            fileGeneratorList.add(new CriteriaFileGenerator(generationInformation));
            fileGeneratorList.add(new ValidatorFileGenerator(generationInformation));
            fileGeneratorList.add(new DaoInterfaceFileGenerator(generationInformation));
            fileGeneratorList.add(new DaoClassFileGenerator(generationInformation));
            fileGeneratorList.add(new SqlMapFileGenerator(generationInformation));
            fileGeneratorList.add(new ServiceInterfaceFileGenerator(generationInformation));
            fileGeneratorList.add(new ServiceClassFileGenerator(generationInformation));
            fileGeneratorList.add(new ControllerFileGenerator(generationInformation));
            fileGeneratorList.add(new FormControllerFileGenerator(generationInformation));
            fileGeneratorList.add(new StyleFileGenerator(generationInformation));
            fileGeneratorList.add(new TagsFileGenerator(generationInformation));
            fileGeneratorList.add(new DefaultPagingFileGenerator(generationInformation));
            fileGeneratorList.add(new IndexFileGenerator(generationInformation, entityList));
            fileGeneratorList.add(new ListPageFileGenerator(generationInformation));
            fileGeneratorList.add(new DetailPageFileGenerator(generationInformation));
            fileGeneratorList.add(new WritePageFileGenerator(generationInformation));
            fileGeneratorList.add(new EditPageFileGenerator(generationInformation));
            monitor.beginTask("Generate soure files", (entityList != null && entityList.size() > 0) ? entityList.size() * fileGeneratorList.size() : 0);
            if (entityList != null && entityList.size() > 0) {
                boolean overwriteYesToAll = false;
                boolean overwriteNoToAll = false;
                List<File> generatedFileList = new ArrayList<File>();
                for (FileGenerator fileGenerator : fileGeneratorList) {
                    fileGenerator.generateDirectory();
                    for (Entity entity : entityList) {
                        monitor.subTask(fileGenerator.getFile(entity).getPath());
                        if (entity.isCreate()) {
                            try {
                                if (fileGenerator.existFile(entity)) {
                                    if (overwriteNoToAll) {
                                        continue;
                                    }
                                    if (!overwriteYesToAll) {
                                        boolean overwrite = false;
                                        MessageDialog overwriteDialog = new MessageDialog(editor.getSite().getShell(), "Question", null, "The file '" + fileGenerator.getFile(entity) + "' already exists. Do you want to replace the existing file?", MessageDialog.WARNING, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_TO_ALL_LABEL }, 1);
                                        int value = overwriteDialog.open();
                                        switch(value) {
                                            case 0:
                                                overwrite = true;
                                                break;
                                            case 1:
                                                overwrite = false;
                                                break;
                                            case 2:
                                                overwrite = true;
                                                overwriteYesToAll = true;
                                                break;
                                            case 3:
                                                overwrite = false;
                                                overwriteNoToAll = true;
                                                break;
                                            default:
                                                overwriteNoToAll = true;
                                                break;
                                        }
                                        if (!overwrite) {
                                            continue;
                                        }
                                    }
                                }
                                entity.setPackageName(generationInformation.getPackageName());
                                File file = fileGenerator.generate(entity);
                                if (file != null) {
                                    generatedFileList.add(file);
                                }
                            } catch (Exception e) {
                                MessageDialog.openError(editor.getSite().getShell(), "Error - generate", e.getMessage());
                            }
                        }
                        monitor.worked(1);
                    }
                }
                printGeneratedFileList(generatedFileList);
            }
            RefreshAction refreshAction = new RefreshAction(editor.getEditorSite());
            refreshAction.refreshAll();
            monitor.done();
        } catch (Exception e) {
            MessageDialog.openError(editor.getSite().getShell(), "Generate file error", e.getMessage());
        } finally {
        }
    }

    private void printGeneratedFileList(List<File> generatedFileList) {
        if (generatedFileList == null) {
            return;
        }
        Collections.sort(generatedFileList);
        MappingDataEditor mappingDataEditor = (MappingDataEditor) editor;
        MessageConsole console = mappingDataEditor.getConsole();
        MessageConsoleStream out = console.newMessageStream();
        out.setActivateOnWrite(true);
        out.println("---- AppGen: Generated information ----------------------------------------------------------------");
        out.println(generatedFileList.size() + " files generated.");
        out.println("---- file list ------------------------------------------------------------------------------------");
        for (File file : generatedFileList) {
            out.println(file.toString());
        }
        out.println("---------------------------------------------------------------------------------------------------");
    }
}
