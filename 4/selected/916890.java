package org.kalypso.nofdpidss.inundation.frequency.worker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.kalypso.commons.xml.XmlTypes;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.gml.processes.raster2vector.Raster2Lines;
import org.kalypso.gml.processes.raster2vector.Raster2LinesWalkingStrategy;
import org.kalypso.gml.processes.raster2vector.collector.CollectorDataProvider;
import org.kalypso.gml.processes.raster2vector.collector.PolygonCollector;
import org.kalypso.gml.processes.raster2vector.collector.SegmentCollector;
import org.kalypso.gmlschema.GMLSchemaFactory;
import org.kalypso.gmlschema.feature.IFeatureType;
import org.kalypso.gmlschema.property.IPropertyType;
import org.kalypso.gmlschema.property.IValuePropertyType;
import org.kalypso.gmlschema.property.relation.IRelationType;
import org.kalypso.gmlschema.types.IMarshallingTypeHandler;
import org.kalypso.gmlschema.types.ITypeRegistry;
import org.kalypso.gmlschema.types.MarshallingTypeRegistrySingleton;
import org.kalypso.grid.AscGridExporter;
import org.kalypso.grid.FlattenToCategoryGrid;
import org.kalypso.grid.GeoGridUtilities;
import org.kalypso.grid.GridCategoryWrapper;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IAllowedString;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategories;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IStringAllowedContentMember;
import org.kalypso.nofdpidss.core.base.gml.model.inundation.IInundationModel;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool.POOL_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.IStyleReplacements;
import org.kalypso.nofdpidss.inundation.frequency.save.StyleReplacerInundationFrequency;
import org.kalypso.nofdpidss.inundation.i18n.Messages;
import org.kalypso.ogc.gml.serialize.ShapeSerializer;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree.model.feature.GMLWorkspace;
import org.kalypsodeegree.model.geometry.GM_Surface;
import org.kalypsodeegree.model.geometry.GM_SurfacePatch;
import org.kalypsodeegree_impl.io.shpapi.ShapeConst;
import org.kalypsodeegree_impl.model.feature.FeatureFactory;
import org.kalypsodeegree_impl.tools.GeometryUtilities;
import com.vividsolutions.jts.geom.GeometryFactory;

public class IDFExporter implements ICoreRunnableWithProgress {

    private static final GeometryFactory GF = new GeometryFactory();

    private final IFolder m_workingFolder;

    private final Map<Integer, MyGridCategoryWrapper> m_wrappers;

    private final Map<String, Integer> m_mapping;

    private final IInundationModel m_inundationModel;

    private final IGeodataModel m_geodataModel;

    public IDFExporter(final IInundationModel inundationModel, final Map<String, Integer> mapping, final IFolder workingFolder, final Map<Integer, MyGridCategoryWrapper> wrappers) {
        m_inundationModel = inundationModel;
        m_mapping = mapping;
        m_workingFolder = workingFolder;
        m_wrappers = wrappers;
        final PoolGeoData pool = (PoolGeoData) NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eGeodata);
        m_geodataModel = pool.getModel();
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        FlattenToCategoryGrid flatten = null;
        try {
            final GridCategoryWrapper[] categories = m_wrappers.values().toArray(new GridCategoryWrapper[] {});
            flatten = GeoGridUtilities.getFlattedGrid(categories, true);
            final AscGridExporter gridExporter = new AscGridExporter(-9999, 3);
            gridExporter.export(flatten, new File(m_workingFolder.getLocation() + "/frequencies.asc"), monitor);
            monitor.worked(1);
            final double[] grenzen = new double[] { 0, 1, 2, 3, 4, 5, 6, 20 };
            final SegmentCollector collector = new PolygonCollector(GF, grenzen, false);
            final Raster2Lines r2l = new Raster2Lines(collector, grenzen);
            new Raster2LinesWalkingStrategy().walk(flatten, r2l, null, monitor);
            final CollectorDataProvider[] data = collector.getData();
            writePolygonShape(data);
        } catch (final Exception e) {
            return StatusUtilities.createErrorStatus(e.getMessage());
        } finally {
            if (flatten != null) {
                flatten.dispose();
            }
        }
        return Status.OK_STATUS;
    }

    private void writePolygonShape(final CollectorDataProvider[] data) throws Exception {
        final ITypeRegistry<IMarshallingTypeHandler> typeRegistry = MarshallingTypeRegistrySingleton.getTypeRegistry();
        final IMarshallingTypeHandler doubleTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_DOUBLE);
        final IMarshallingTypeHandler stringTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_STRING);
        final IMarshallingTypeHandler polygonTypeHandler = typeRegistry.getTypeHandlerForTypeName(GeometryUtilities.QN_POLYGON);
        final QName shapeTypeQName = new QName("anyNS", "shapeType");
        final IValuePropertyType doubleTypeId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "id"), doubleTypeHandler, 1, 1, false);
        final IValuePropertyType stringTypeRange = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "range"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType frequency = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "freq"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType frequencyDescription = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "freq_des"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType polygonType = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "geometry"), polygonTypeHandler, 1, 1, false);
        final IPropertyType[] properties = new IPropertyType[] { polygonType, doubleTypeId, stringTypeRange, frequency, frequencyDescription };
        final IFeatureType shapeFT = GMLSchemaFactory.createFeatureType(shapeTypeQName, properties);
        final Feature shapeRootFeature = ShapeSerializer.createWorkspaceRootFeature(shapeFT, ShapeConst.SHAPE_TYPE_POLYGONZ);
        final GMLWorkspace shapeWorkspace = shapeRootFeature.getWorkspace();
        final IRelationType shapeParentRelation = (IRelationType) shapeRootFeature.getFeatureType().getProperty(ShapeSerializer.PROPERTY_FEATURE_MEMBER);
        for (int i = 0; i < data.length; i++) {
            final GM_Surface<GM_SurfacePatch> line = (GM_Surface<GM_SurfacePatch>) data[i].getGeometry();
            final Double id = data[i].getId();
            final Double[] borders = data[i].getBorders();
            final String[] valueClass = data[i].getName();
            final Set<Entry<String, Integer>> entrySet = m_mapping.entrySet();
            for (final Entry<String, Integer> entry : entrySet) {
                final int value = entry.getValue();
                if (Double.valueOf(borders[0]).intValue() == value) {
                    final String stringId = entry.getKey();
                    final IAllowedString string = getAllowedString(stringId);
                    final Object[] shapeData = new Object[] { line, id, valueClass[0], string.getMainTerm(), string.getDescription() };
                    final Feature feature = FeatureFactory.createFeature(shapeRootFeature, shapeParentRelation, "FeatureID" + i, shapeFT, shapeData);
                    shapeWorkspace.addFeatureAsComposition(shapeRootFeature, shapeParentRelation, -1, feature);
                    break;
                }
            }
        }
        final String shapeBase = m_workingFolder.getLocation() + "/frequencies";
        ShapeSerializer.serialize(shapeWorkspace, shapeBase, null);
        WorkspaceSync.sync(m_workingFolder, IResource.DEPTH_ONE);
        final IFile iFile = m_workingFolder.getFile("/frequencies.shp");
        if (!iFile.exists()) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.IDFWorker_26);
        generateStyle(m_workingFolder);
    }

    private IAllowedString getAllowedString(final String stringId) {
        final IStringAllowedContentMember frequencies = m_inundationModel.getFrequencyMember().getFrequencies(m_geodataModel);
        return frequencies.getAllowedStrings().getString(stringId);
    }

    private void generateStyle(final IFolder importDataFolder) throws IOException, CoreException {
        final IProject base = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFile baseFile = base.getFile(".styles/template_inundationfrequency.sld");
        final IFile iSld = importDataFolder.getFile("frequencies.sld");
        FileUtils.copyFile(baseFile.getLocation().toFile(), iSld.getLocation().toFile());
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
        final PoolGeoData pool = (PoolGeoData) NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eGeodata);
        final IGeodataModel model = pool.getModel();
        final IGeodataCategory[] categories = model.getCategories().getCategories(new QName[] { IGeodataCategories.QN_SUBCATEGORY_INUNDATION_FREQ });
        if (categories.length != 1) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.IDFWorker_29);
        final IStyleReplacements replacer = new StyleReplacerInundationFrequency(categories[0], iSld.getLocation().toFile(), m_wrappers.values().toArray(new MyGridCategoryWrapper[] {}));
        replacer.replace();
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
    }
}
