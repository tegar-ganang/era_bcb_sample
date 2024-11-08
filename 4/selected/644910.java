package org.kalypso.nofdpidss.hydraulic.computation.processing.worker.calculation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.contribs.eclipse.ui.progress.ConsoleHelper;
import org.kalypso.contribs.java.io.MyPrintStream;
import org.kalypso.gmlschema.feature.IFeatureType;
import org.kalypso.model.wspm.sobek.calculation.job.ISobekCalculationJobConstants;
import org.kalypso.model.wspm.sobek.calculation.job.WspmSobekCalcJob;
import org.kalypso.model.wspm.sobek.core.interfaces.ILastfall;
import org.kalypso.model.wspm.sobek.core.interfaces.ISobekConstants;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.hydraulic.computation.i18n.Messages;
import org.kalypso.nofdpidss.hydraulic.computation.processing.interfaces.IHydraulicCalculationCase;
import org.kalypso.nofdpidss.hydraulic.computation.processing.worker.calculation.utils.ResourcesHandler;
import org.kalypso.nofdpidss.hydraulic.computation.processing.worker.utils.workspace.IWorkspaceProvider;
import org.kalypso.nofdpidss.hydraulic.computation.sobek.INofdpModelMember;
import org.kalypso.nofdpidss.hydraulic.computation.sobek.NofdpModelMember;
import org.kalypso.ogc.gml.mapmodel.CommandableWorkspace;
import org.kalypso.ogc.gml.serialize.GmlSerializer;
import org.kalypso.simulation.core.util.DefaultSimulationDataProvider;
import org.kalypso.simulation.core.util.DefaultSimulationResultEater;
import org.kalypso.simulation.core.util.SimulationUtilitites;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree.model.feature.GMLWorkspace;

public class CalculationWorker implements ICoreRunnableWithProgress {

    private final IHydraulicCalculationCase m_calculationCase;

    private final IWorkspaceProvider m_provider;

    private final MyPrintStream m_nofdpStream;

    private final MyPrintStream m_sobekStream;

    public CalculationWorker(final IHydraulicCalculationCase calculationCase, final IWorkspaceProvider provider, final MyPrintStream nofdpStream, final MyPrintStream sobekStream) {
        m_calculationCase = calculationCase;
        m_provider = provider;
        m_nofdpStream = nofdpStream;
        m_sobekStream = sobekStream;
    }

    private void getModelData(final IFolder folder, final DefaultSimulationDataProvider inputProvider) throws CoreException {
        final ResourcesHandler resource = new ResourcesHandler(folder);
        try {
            inputProvider.put(ISobekCalculationJobConstants.CALC_CASE_PATH, resource.getCalcCaseFolder().getLocation().toFile().toURL());
            inputProvider.put(ISobekCalculationJobConstants.FLOW_NETWORK_PATH, resource.getFlowNetworkFolder().getLocation().toFile().toURL());
        } catch (final MalformedURLException e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
        }
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        final DefaultSimulationResultEater resultEater = new DefaultSimulationResultEater();
        IFolder calculation = null;
        File tmpDir = null;
        try {
            ConsoleHelper.writeLine(m_nofdpStream, String.format(Messages.CalculationWorker_1));
            ConsoleHelper.writeLine(m_nofdpStream, String.format(Messages.CalculationWorker_2));
            final IFolder workingFolder = m_calculationCase.getWorkspaceFolder();
            final IFile iCuttedModelFile = workingFolder.getFile(IWorkspaceProvider.CUTTED_PROFILES_HYDRAUL_MODEL_FILE);
            if (!iCuttedModelFile.exists()) throw ExceptionHelper.getCoreException(IStatus.ERROR, getClass(), String.format(Messages.CalculationWorker_0, iCuttedModelFile));
            final GMLWorkspace workspace = GmlSerializer.createGMLWorkspace(iCuttedModelFile.getLocationURI().toURL(), null);
            final IFeatureType modelFT = workspace.getGMLSchema().getFeatureType(ISobekConstants.QN_SOBEK_MODEL);
            final Feature[] sobekFeature = workspace.getFeatures(modelFT);
            final CommandableWorkspace commandableWorkspace = new CommandableWorkspace(workspace);
            final IFolder folder = m_calculationCase.getFolder(false);
            calculation = folder.getFolder("calculation");
            if (calculation.exists()) calculation.delete(true, monitor);
            calculation.create(true, true, monitor);
            final URL urlTarget = calculation.getLocation().toFile().toURL();
            final ILastfall lastfall = m_calculationCase.getLinkedLastfall(m_provider.getHydraulicModel().getSobekModelMember());
            ConsoleHelper.writeLine(m_nofdpStream, String.format(Messages.CalculationWorker_3));
            final INofdpModelMember nofdpModel = NofdpModelMember.getModel(commandableWorkspace, sobekFeature[0]);
            nofdpModel.convertNofdpStructs2SbkStructs();
            nofdpModel.writePi(urlTarget, new ILastfall[] { lastfall });
            WorkspaceSync.sync(folder, IResource.DEPTH_INFINITE);
            commandableWorkspace.dispose();
            workspace.dispose();
            ConsoleHelper.writeLine(m_nofdpStream, String.format(Messages.CalculationWorker_4));
            tmpDir = SimulationUtilitites.createSimulationTmpDir("" + System.currentTimeMillis());
            final WspmSobekCalcJob job = new WspmSobekCalcJob(m_nofdpStream, m_sobekStream);
            final DefaultSimulationDataProvider inputProvider = new DefaultSimulationDataProvider();
            getModelData(calculation, inputProvider);
            final NullSimulationMonitorExtension simMonitor = new NullSimulationMonitorExtension(monitor);
            job.run(tmpDir, inputProvider, resultEater, simMonitor);
            if (simMonitor.getFinishStatus() != IStatus.OK) throw new CoreException(StatusUtilities.createStatus(simMonitor.getFinishStatus(), simMonitor.getFinishText(), null));
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            if (e instanceof CoreException) throw (CoreException) e; else throw ExceptionHelper.getCoreException(IStatus.ERROR, getClass(), e.getMessage());
        } finally {
            storeResults(resultEater, m_calculationCase.getResultFolder(), monitor);
            SimulationUtilitites.clearTmpDir(tmpDir);
        }
        ConsoleHelper.writeLine(m_nofdpStream, String.format(""));
        return StatusUtilities.createOkStatus(Messages.CalculationWorker_7);
    }

    private void storeResults(final DefaultSimulationResultEater eater, final IFolder resultFolder, final IProgressMonitor monitor) throws CoreException {
        final Object result1 = eater.getResult(ISobekCalculationJobConstants.CALCULATION_RESULT_POINTS);
        final Object result2 = eater.getResult(ISobekCalculationJobConstants.CALCULATION_RESULT_STRUCTURES);
        if (result1 instanceof File) {
            final File file = (File) result1;
            final IFile iFile = resultFolder.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        if (result2 instanceof File) {
            final File file = (File) result2;
            final IFile iFile = resultFolder.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        final IFolder logs = resultFolder.getFolder("logs");
        if (!logs.exists()) logs.create(true, true, monitor);
        final Object log1 = eater.getResult(ISobekCalculationJobConstants.LOG_PI2SOBEK);
        final Object log2 = eater.getResult(ISobekCalculationJobConstants.LOG_OPENMI_CONTROL);
        final Object log3 = eater.getResult(ISobekCalculationJobConstants.LOG_SOBEK);
        final Object log4 = eater.getResult(ISobekCalculationJobConstants.LOG_SOBEK2PI_POINTS);
        final Object log5 = eater.getResult(ISobekCalculationJobConstants.LOG_SOBEK2PI_STRUCTURES);
        if (log1 instanceof File) {
            final File file = (File) log1;
            final IFile iFile = logs.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        if (log2 instanceof File) {
            final File file = (File) log2;
            final IFile iFile = logs.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        if (log3 instanceof File) {
            final File file = (File) log3;
            final IFile iFile = logs.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        if (log4 instanceof File) {
            final File file = (File) log4;
            final IFile iFile = logs.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        if (log5 instanceof File) {
            final File file = (File) log5;
            final IFile iFile = logs.getFile(file.getName());
            try {
                FileUtils.copyFile(file, iFile.getLocation().toFile());
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            }
        }
        WorkspaceSync.sync(logs, IResource.DEPTH_ONE);
        WorkspaceSync.sync(resultFolder, IResource.DEPTH_ONE);
    }
}
