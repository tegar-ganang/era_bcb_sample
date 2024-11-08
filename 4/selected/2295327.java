package org.kalypso.nofdpidss.inundation.duration.hydrograph.wizard.worker.duration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.xml.datatype.Duration;
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
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IAllowedString;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategories;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.model.inundation.IDHEntireDefinitionMember;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool.POOL_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.IStyleReplacements;
import org.kalypso.nofdpidss.inundation.duration.IDurationConstant;
import org.kalypso.nofdpidss.inundation.frequency.worker.MyGridCategoryWrapper;
import org.kalypso.nofdpidss.inundation.i18n.Messages;
import org.kalypso.ogc.gml.serialize.GmlSerializeException;
import org.kalypso.ogc.gml.serialize.ShapeSerializer;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree.model.feature.GMLWorkspace;
import org.kalypsodeegree.model.geometry.GM_Surface;
import org.kalypsodeegree_impl.io.shpapi.ShapeConst;
import org.kalypsodeegree_impl.model.feature.FeatureFactory;
import org.kalypsodeegree_impl.tools.GeometryUtilities;
import com.vividsolutions.jts.geom.GeometryFactory;

public class IDHExportWorker implements ICoreRunnableWithProgress {

    private static final GeometryFactory GF = new GeometryFactory();

    private final MyGridCategoryWrapper[] m_wrappers;

    private final IDHEntireDefinitionMember m_entire;

    public IDHExportWorker(final IDHEntireDefinitionMember entire, final MyGridCategoryWrapper[] wrappers) {
        m_entire = entire;
        m_wrappers = wrappers;
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        try {
            final AscGridExporter gridExporter = new AscGridExporter(-9999, 3);
            final FlattenToCategoryGrid grid = GeoGridUtilities.getFlattedGrid(m_wrappers, true);
            final IFolder baseFolder = m_entire.getFolder(false);
            gridExporter.export(grid, new File(baseFolder.getLocation() + "/hydrographBasedDuration.asc"), monitor);
            vectorize(grid, monitor);
            grid.dispose();
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            return StatusUtilities.createErrorStatus(e.getMessage());
        }
        return Status.OK_STATUS;
    }

    private void vectorize(final FlattenToCategoryGrid grid, final IProgressMonitor monitor) throws GmlSerializeException, Exception {
        final double[] grenzen = new double[] { -0.5, 0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5 };
        final SegmentCollector collector = new PolygonCollector(GF, grenzen, false);
        final Raster2Lines r2l = new Raster2Lines(collector, grenzen);
        new Raster2LinesWalkingStrategy().walk(grid, r2l, null, monitor);
        final CollectorDataProvider[] data = collector.getData();
        writePolygonShape(data);
    }

    private void writePolygonShape(final CollectorDataProvider[] data) throws Exception, GmlSerializeException {
        final ITypeRegistry<IMarshallingTypeHandler> typeRegistry = MarshallingTypeRegistrySingleton.getTypeRegistry();
        final IMarshallingTypeHandler doubleTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_DOUBLE);
        final IMarshallingTypeHandler stringTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_STRING);
        final IMarshallingTypeHandler polygonTypeHandler = typeRegistry.getTypeHandlerForTypeName(GeometryUtilities.QN_POLYGON);
        final IValuePropertyType doubleTypeId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "id"), doubleTypeHandler, 1, 1, false);
        final IValuePropertyType stringTypeRange = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "range"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType duration = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "duration"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType borderId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "border"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType polygonType = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "geometry"), polygonTypeHandler, 1, 1, false);
        final QName shapeTypeQName = new QName("anyNS", "shapeType");
        final IPropertyType[] properties = new IPropertyType[] { polygonType, doubleTypeId, stringTypeRange, duration, borderId };
        final IFeatureType shapeFT = GMLSchemaFactory.createFeatureType(shapeTypeQName, properties);
        final Feature shapeRootFeature = ShapeSerializer.createWorkspaceRootFeature(shapeFT, ShapeConst.SHAPE_TYPE_POLYGONZ);
        final GMLWorkspace shapeWorkspace = shapeRootFeature.getWorkspace();
        final IRelationType shapeParentRelation = (IRelationType) shapeRootFeature.getFeatureType().getProperty(ShapeSerializer.PROPERTY_FEATURE_MEMBER);
        for (int i = 0; i < data.length; i++) {
            final GM_Surface<?> line = (GM_Surface<?>) data[i].getGeometry();
            final Double id = data[i].getId();
            final Double[] borders = data[i].getBorders();
            final String[] range = data[i].getName();
            final Double to = borders[1];
            final IAllowedString string = getAllowedString(to.intValue());
            final String border = getBorder(string);
            final Object[] shapeData = new Object[] { line, id, range[0], string.getName(), border };
            final Feature feature = FeatureFactory.createFeature(shapeRootFeature, shapeParentRelation, "FeatureID" + i, shapeFT, shapeData);
            shapeWorkspace.addFeatureAsComposition(shapeRootFeature, shapeParentRelation, -1, feature);
        }
        final IFolder folder = m_entire.getFolder(false);
        final String shapeBase = folder.getLocation() + "/hydrographBasedDuration";
        ShapeSerializer.serialize(shapeWorkspace, shapeBase, null);
        WorkspaceSync.sync(folder, IResource.DEPTH_ONE);
        final IFile iFile = folder.getFile("/hydrographBasedDuration.shp");
        if (!iFile.exists()) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.IDFEventWorker_28);
        generateStyle();
    }

    private String getBorder(final IAllowedString allowedString) {
        for (final MyGridCategoryWrapper wrapper : m_wrappers) {
            final IAllowedString string = wrapper.getString();
            if (!string.equals(allowedString)) {
                continue;
            }
            final Duration duration = wrapper.getDuration();
            if (IDurationConstant.DURATION_7_ALWAYS.equals(string.getId())) return Messages.IDHExportWorker_0; else return String.format(Messages.IDHExportWorker_1, duration.getDays(), duration.getHours(), duration.getMinutes());
        }
        return "-1";
    }

    private IAllowedString getAllowedString(final int value) {
        for (final MyGridCategoryWrapper wrapper : m_wrappers) {
            if (value == wrapper.getValue().intValue()) return wrapper.getString();
        }
        throw new IllegalStateException();
    }

    private void generateStyle() throws IOException, CoreException {
        final IProject base = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFile baseFile = base.getFile(".styles/template_inundationduration.sld");
        final IFile iSld = m_entire.getFolder(false).getFile("hydrographBasedDuration.sld");
        FileUtils.copyFile(baseFile.getLocation().toFile(), iSld.getLocation().toFile());
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
        final PoolGeoData pool = (PoolGeoData) NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eGeodata);
        final IGeodataModel model = pool.getModel();
        final IGeodataCategory[] categories = model.getCategories().getCategories(new QName[] { IGeodataCategories.QN_SUBCATEGORY_INUNDATION_DURATION });
        if (categories.length != 1) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.IDFEventWorker_31);
        final IStyleReplacements replacer = new StyleReplacerHydrographBasedInundationDuration(m_wrappers, categories[0], iSld.getLocation().toFile());
        replacer.replace();
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
    }
}
