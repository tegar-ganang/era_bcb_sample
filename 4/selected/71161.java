package org.kalypso.nofdpidss.geodata.wizard.gds.add.raster;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.DATA_TYPE;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoGmlUtils;
import org.kalypso.nofdpidss.core.view.map.MapTool;
import org.kalypso.nofdpidss.geodata.i18n.Messages;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.commmon.AbstractRasterData;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.commmon.PerformFinishStuff;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypsodeegree.model.feature.Feature;
import org.xml.sax.SAXException;

/**
 * @author Dirk Kuch
 */
public class RasterPerformFinishWorker extends AbstractRasterData implements ICoreRunnableWithProgress {

    public RasterPerformFinishWorker(final IProject project, final IGeodataModel model, final IGeodataPageProvider pages, final Feature selection) {
        super(project, model, pages, selection);
    }

    private void appendMapLayer(final Feature fGeoDataSet) throws IOException, CoreException, JAXBException, SAXException, ParserConfigurationException {
        final IFile[] maps = PerformFinishStuff.getSelectedMaps(getProject(), getPages());
        final Style style = MapTool.generateStyle(fGeoDataSet, getRasterDataFolder());
        final StyledLayerType mapTag = MapTool.getMapTag(getProject(), getSelection(), fGeoDataSet, style, getPages().getUserSelectedDataType());
        MapTool.addMapLayer(maps, mapTag);
        WorkspaceSync.sync(getProject(), IResource.DEPTH_INFINITE);
    }

    /**
   * @see org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress#execute(org.eclipse.core.runtime.IProgressMonitor)
   */
    public IStatus execute(final IProgressMonitor monitor) {
        try {
            final String geoDataSetName = importImageFile(true);
            if (geoDataSetName == null || "".equals(geoDataSetName)) return Status.CANCEL_STATUS;
            final Feature feature;
            if (BaseGeoGmlUtils.isDefaultCat(getSelection())) {
                final DATA_TYPE categoryDataType = getPages().getCategoryDataType();
                final DATA_TYPE user_type = getPages().getUserSelectedDataType();
                if (categoryDataType != null && user_type != null && categoryDataType.equals(user_type)) {
                    feature = createNofdpConformGmlEntry(geoDataSetName, DATA_TYPE.eRaster);
                    generateStyle(feature, geoDataSetName);
                } else feature = createGmlEntry(geoDataSetName, DATA_TYPE.eRaster);
            } else feature = createGmlEntry(geoDataSetName, DATA_TYPE.eRaster);
            if (feature == null) return Status.CANCEL_STATUS;
            appendMapLayer(feature);
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    private IFile generateStyle(final Feature geoDataSet, final String rasterFile) {
        try {
            final String[] parts = rasterFile.split("\\.");
            String geoDataSetName = "";
            for (int i = 0; i <= parts.length - 2; i++) geoDataSetName += parts[i] + ".";
            geoDataSetName += "sld";
            final IFile iRaster = getRasterDataFolder().getFile(rasterFile);
            if (!iRaster.exists()) throw new IllegalStateException(Messages.RasterPerformFinishWorker_5 + iRaster.getLocation().toOSString());
            final IProject global = NofdpCorePlugin.getProjectManager().getBaseProject();
            final IFolder templateFolder = global.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GLOBAL_STYLES_FOLDER);
            final IFile iTemplate = templateFolder.getFile("templateRaster.sld");
            final File fTemplateSld = iTemplate.getLocation().toFile();
            final IFolder dataFolder = getRasterDataFolder();
            final IFolder styleFolder = dataFolder.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER);
            if (!styleFolder.exists()) styleFolder.create(true, true, new NullProgressMonitor());
            final IFile iWorkingSld = styleFolder.getFile(geoDataSetName);
            final File fWorkingSld = iWorkingSld.getLocation().toFile();
            FileUtils.copyFile(fTemplateSld, fWorkingSld);
            WorkspaceSync.sync(getProject(), IResource.DEPTH_INFINITE);
            final IGeodataPageProvider pages = getPages();
            final PageSelectRasterSettings rasterSettings = (PageSelectRasterSettings) pages.getPage(RasterPages.COMMON_RASTER_SETTING_PAGE);
            final StyleReplacerRaster replacerRaster = new StyleReplacerRaster(rasterSettings, geoDataSet, iRaster, fWorkingSld);
            replacerRaster.replace();
            if (!iWorkingSld.exists()) throw new IllegalStateException();
            return iWorkingSld;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
