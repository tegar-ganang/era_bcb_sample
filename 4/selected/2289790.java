package org.kalypso.nofdpidss.geodata.wizard.gds.add.commmon;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.kalypso.commons.resources.SetContentHelper;
import org.kalypso.contribs.eclipse.core.resources.ResourceUtilities;
import org.kalypso.core.KalypsoCorePlugin;
import org.kalypso.gmlschema.IGMLSchema;
import org.kalypso.gmlschema.feature.IFeatureType;
import org.kalypso.gmlschema.property.IPropertyType;
import org.kalypso.gmlschema.property.relation.IRelationType;
import org.kalypso.grid.ConvertAscii2Binary;
import org.kalypso.grid.GeoGridException;
import org.kalypso.grid.IGridMetaReader;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.commands.AtomarAddFeatureCommand;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSetTypes;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.CATEGORY_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.DATA_TYPE;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.geodata.i18n.Messages;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.raster.PageSelectRasterSettings;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.raster.RasterPages;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.ShapePages;
import org.kalypso.ogc.gml.selection.IFeatureSelectionManager;
import org.kalypso.ogc.gml.serialize.GmlSerializer;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree_impl.gml.binding.commons.CoverageCollection;
import org.kalypsodeegree_impl.gml.binding.commons.RectifiedGridDomain;

/**
 * @author Dirk Kuch
 */
public class AbstractRasterData {

    private final IProject m_project;

    private final IGeodataModel m_model;

    private final Feature m_selection;

    private final IGeodataPageProvider m_pages;

    private IFolder m_rasterDataFolder;

    public AbstractRasterData(final IProject project, final IGeodataModel model, final IGeodataPageProvider pages, final Feature selection) {
        m_project = project;
        m_model = model;
        m_pages = pages;
        m_selection = selection;
    }

    /**
   * copy rulez from category to geodataset (these rulez are needed for verification)
   * 
   * @throws Exception
   */
    protected void addAbstractStructureMember(final Feature feature, final DATA_TYPE type) throws Exception {
        if (feature == null) throw new IllegalStateException();
        final QName name = type.getQName();
        final IRelationType rel = (IRelationType) feature.getFeatureType().getProperty(IGeodataSet.QN_ABSTRACT_GEODATA_MEMBER);
        final IGMLSchema schema = feature.getFeatureType().getGMLSchema();
        final IFeatureType targetType = schema.getFeatureType(name);
        final IFeatureSelectionManager selectionManager = KalypsoCorePlugin.getDefault().getSelectionManager();
        final AtomarAddFeatureCommand command = new AtomarAddFeatureCommand(m_model.getWorkspace(), targetType, feature, rel, -1, null, selectionManager);
        m_model.getWorkspace().postCommand(command);
    }

    /**
   * copy a bunch of files, like test.tif -> take test.tfw (worldfile), etc too and copy those files into the geodata
   * project structure
   * 
   * @param geoDataFile
   *            for import selected geoDataFile
   */
    private String copyFiles(final File geoDataFile) throws IOException, CoreException, GeoGridException {
        final String fileName = geoDataFile.getName();
        if (!fileName.contains(".")) throw new IllegalStateException(Messages.AbstractRasterData_1);
        final String[] parts = fileName.split("\\.");
        final File dirDest = m_rasterDataFolder.getLocation().toFile();
        final String geoDataSetName = BaseGeoUtils.getFileName(dirDest, parts[0]);
        final File[] files = BaseGeoUtils.getFiles(geoDataFile.getParentFile(), parts[0]);
        for (final File srcFile : files) {
            final String srcParts[] = srcFile.getName().split("\\.");
            if (srcParts.length != 2) continue;
            if ("asc".equals(srcParts[1].toLowerCase()) || "dat".equals(srcParts[1].toLowerCase())) {
                final File destFile = new File(dirDest, geoDataSetName + ".asc.bin");
                final PageCommonGeoDataSettings pCommonDetails = (PageCommonGeoDataSettings) m_pages.getPage(ShapePages.COMMON_DETAILS);
                final String crs = pCommonDetails.getSelectedCRS();
                final ConvertAscii2Binary converter = new ConvertAscii2Binary(srcFile.toURL(), destFile, 2, crs);
                converter.doConvert(new NullProgressMonitor());
            }
            final File destFile = new File(dirDest, geoDataSetName + "." + srcParts[1]);
            FileUtils.copyFile(srcFile, destFile);
            WorkspaceSync.sync(m_project, IResource.DEPTH_INFINITE);
        }
        return geoDataSetName;
    }

    /**
   * creates a user geodataset entry
   * 
   * @throws Exception
   */
    protected Feature createGmlEntry(final String geoDataSetName, final DATA_TYPE type) throws Exception {
        final IRelationType rel = (IRelationType) m_selection.getFeatureType().getProperty(IGeodataCategory.QN_SUBCATEGORY_MEMBER);
        final IGMLSchema schema = m_selection.getFeatureType().getGMLSchema();
        final IFeatureType targetType;
        final CATEGORY_TYPE categoryType = m_pages.getCategoryType();
        if (CATEGORY_TYPE.eInundationDepthGridSubCat.equals(categoryType)) {
            targetType = schema.getFeatureType(IGeodataSetTypes.QN_INUNDATION_DEPTH_GRID_TYPE);
        } else {
            targetType = schema.getFeatureType(IGeodataSetTypes.QN_GEODATA_SET);
        }
        if (targetType == null) throw new IllegalStateException();
        final IFeatureSelectionManager selectionManager = KalypsoCorePlugin.getDefault().getSelectionManager();
        final Map<IPropertyType, Object> values = PerformFinishStuff.getCommonValues(m_pages, targetType, geoDataSetName);
        final AtomarAddFeatureCommand command = new AtomarAddFeatureCommand(m_model.getWorkspace(), targetType, m_selection, rel, -1, values, selectionManager);
        m_model.getWorkspace().postCommand(command);
        final Feature feature = command.getNewFeature();
        addAbstractStructureMember(feature, type);
        return feature;
    }

    /**
   * creates a nofdp conform geodataset entry
   * 
   * @throws Exception
   */
    protected Feature createNofdpConformGmlEntry(final String geoDataSetName, final DATA_TYPE type) throws Exception {
        final IRelationType rel = (IRelationType) m_selection.getFeatureType().getProperty(IGeodataCategory.QN_SUBCATEGORY_MEMBER);
        final IGMLSchema schema = m_selection.getWorkspace().getGMLSchema();
        final IFeatureType targetType;
        final CATEGORY_TYPE categoryType = m_pages.getCategoryType();
        if (CATEGORY_TYPE.eInundationDepthGridSubCat.equals(categoryType)) {
            targetType = schema.getFeatureType(IGeodataSetTypes.QN_INUNDATION_DEPTH);
        } else {
            targetType = schema.getFeatureType(IGeodataSetTypes.QN_GEODATA_SET);
        }
        if (targetType == null) throw new IllegalStateException();
        final IFeatureSelectionManager selectionManager = KalypsoCorePlugin.getDefault().getSelectionManager();
        final Map<IPropertyType, Object> values = PerformFinishStuff.getCommonValues(m_pages, targetType, geoDataSetName);
        values.put(targetType.getProperty(IGeodataSet.QN_IS_NOFDP_TYPE), true);
        final AtomarAddFeatureCommand command = new AtomarAddFeatureCommand(m_model.getWorkspace(), targetType, m_selection, rel, -1, values, selectionManager);
        m_model.getWorkspace().postCommand(command);
        final Feature feature = command.getNewFeature();
        addAbstractStructureMember(feature, type);
        return feature;
    }

    private String getExtension(final File selectedGeoDataFile) {
        final String name = selectedGeoDataFile.getName();
        final String[] split = name.split("\\.");
        final String ext = split[split.length - 1];
        if ("asc".equals(ext.toLowerCase())) return "asc.bin"; else if ("dat".equals(ext.toLowerCase())) return "asc.bin";
        return ext;
    }

    protected IGeodataPageProvider getPages() {
        return m_pages;
    }

    protected IGeodataModel getModel() {
        return m_model;
    }

    protected IProject getProject() {
        return m_project;
    }

    protected IFolder getRasterDataFolder() {
        return m_rasterDataFolder;
    }

    protected Feature getSelection() {
        return m_selection;
    }

    /**
   * import raster data files to geodata project structure and create an gml file, which wraps those files
   */
    protected String importImageFile(final boolean isAsciiGrid) throws Exception {
        final PageSelectGeodataFile pGeoFile = (PageSelectGeodataFile) m_pages.getPage(ShapePages.COMMON_FILE_SELECTION);
        final PageCommonGeoDataSettings pGeoSettings = (PageCommonGeoDataSettings) m_pages.getPage(ShapePages.COMMON_DETAILS);
        final PageSelectRasterSettings pRaster = (PageSelectRasterSettings) m_pages.getPage(RasterPages.COMMON_RASTER_SETTING_PAGE);
        final File selectedGeoDataFile = pGeoFile.getSelectedFile();
        if (selectedGeoDataFile == null) throw new IllegalStateException();
        m_rasterDataFolder = BaseGeoUtils.getSubDirLocation(m_project, m_selection);
        if (m_rasterDataFolder == null) throw new IllegalStateException();
        final String geoDataSetName = copyFiles(selectedGeoDataFile);
        final String cs = pGeoSettings.getSelectedCRS();
        final IGridMetaReader reader = pRaster.getRasterReader();
        final RectifiedGridDomain coverage = reader.getCoverage(pRaster.getOffsetX(), pRaster.getOffsetY(), pRaster.getOriginCorner(), cs);
        final IFile fRaster = m_rasterDataFolder.getFile(geoDataSetName + "." + getExtension(selectedGeoDataFile));
        final IFile fGml = m_rasterDataFolder.getFile(geoDataSetName + ".gml");
        final CoverageCollection coverages = new CoverageCollection(ResourceUtilities.createURL(m_rasterDataFolder), null);
        final String mimeType = isAsciiGrid ? "image/bin" : "image/" + fRaster.getFileExtension();
        CoverageCollection.addCoverage(coverages, coverage, fRaster.getName(), mimeType);
        final SetContentHelper contentHelper = new SetContentHelper() {

            @Override
            protected void write(final OutputStreamWriter writer) throws Throwable {
                GmlSerializer.serializeWorkspace(writer, coverages.getFeature().getWorkspace());
            }
        };
        contentHelper.setFileContents(fGml, false, true, new NullProgressMonitor());
        return geoDataSetName + ".gml";
    }
}
