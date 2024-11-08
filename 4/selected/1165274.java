package org.kalypso.nofdpidss.export.worker;

import java.awt.geom.IllegalPathStateException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.grid.AscGridExporter;
import org.kalypso.grid.GeoGridUtilities;
import org.kalypso.grid.IGeoGrid;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.DATA_TYPE;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.export.data.ExportManagerData;
import org.kalypso.nofdpidss.export.i18n.Messages;
import org.kalypso.ogc.gml.serialize.GmlSerializer;
import org.kalypsodeegree.model.feature.GMLWorkspace;
import org.kalypsodeegree_impl.gml.binding.commons.ICoverage;
import org.kalypsodeegree_impl.gml.binding.commons.ICoverageCollection;

public class GeodataExportWorker implements ICoreRunnableWithProgress {

    private final String DIR_GEODATA = "geodata";

    private final IProject m_project;

    private final ExportManagerData m_data;

    private final IFolder m_tmpDir;

    public GeodataExportWorker(final IProject project, final ExportManagerData data, final IFolder tmpDir) {
        m_project = project;
        m_data = data;
        m_tmpDir = tmpDir;
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        final IFolder dirGeoData = m_tmpDir.getFolder(DIR_GEODATA);
        final IGeodataSet[] geoDataSets = m_data.getGeoData();
        for (final IGeodataSet gds : geoDataSets) {
            try {
                final DATA_TYPE type = gds.getType();
                final IFile iGds = gds.getIFile();
                final IFolder iParent = (IFolder) iGds.getParent();
                final File parent = iParent.getLocation().toFile();
                final String name = iGds.getName();
                final int lastIndexOf = name.lastIndexOf(".");
                final String fileName = name.substring(0, lastIndexOf + 1);
                final File[] files = BaseGeoUtils.getFiles(parent, fileName);
                final IFolder iDestFolder = createFolderStructure(dirGeoData, iParent);
                final File destFolder = iDestFolder.getLocation().toFile();
                for (File file : files) {
                    try {
                        if (DATA_TYPE.eRaster.equals(type)) {
                            final String fn = file.getName().toLowerCase();
                            if (fn.endsWith(".bin")) {
                                file = convert2Asc(file, monitor);
                            } else if (fn.endsWith(".gml")) continue;
                        }
                        final File dest = new File(destFolder, file.getName());
                        FileUtils.copyFile(file, dest);
                    } catch (final Exception e) {
                        NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
                    }
                }
            } catch (final IOException e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
                throw ExceptionHelper.getCoreException(IStatus.ERROR, getClass(), e.getMessage());
            }
            WorkspaceSync.sync(m_tmpDir, IResource.DEPTH_INFINITE);
        }
        return Status.OK_STATUS;
    }

    private File convert2Asc(final File raster, final IProgressMonitor monitor) throws Exception {
        final File parent = raster.getParentFile();
        final String[] parts = raster.getName().split("\\.");
        final File target = new File(parent, parts[0] + ".asc");
        if (target.exists()) return target;
        final File fileGml = new File(parent, parts[0] + ".gml");
        if (!fileGml.exists()) throw ExceptionHelper.getCoreException(IStatus.ERROR, getClass(), Messages.GeodataExportWorker_5);
        final URL urlRaster = fileGml.toURL();
        final GMLWorkspace workspace = GmlSerializer.createGMLWorkspace(urlRaster, null);
        final ICoverageCollection coverages = (ICoverageCollection) workspace.getRootFeature().getAdapter(ICoverageCollection.class);
        if (coverages == null || coverages.size() == 0) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.GeodataExportWorker_6);
        for (final ICoverage coverage : coverages) {
            final IGeoGrid grid = GeoGridUtilities.toGrid(coverage);
            final AscGridExporter gridExporter = new AscGridExporter(-9999, 3);
            gridExporter.export(grid, target, monitor);
            return target;
        }
        throw new IllegalStateException();
    }

    /**
   * @return destination folder in tmpDir structure
   */
    private IFolder createFolderStructure(final IFolder tmpDir, final IFolder getDataFolder) throws IOException {
        final IPath fullPath = getDataFolder.getFullPath();
        final String path = fullPath.toString().toLowerCase();
        final String[] mypath = path.split("geodata/");
        if (mypath.length != 2) throw new IllegalPathStateException(Messages.FileExporter_4 + path);
        final IFolder folder = tmpDir.getFolder(mypath[1]);
        if (!folder.exists()) FileUtils.forceMkdir(folder.getLocation().toFile());
        WorkspaceSync.sync(tmpDir, IResource.DEPTH_INFINITE);
        return folder;
    }
}
