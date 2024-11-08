package org.kalypso.nofdpidss.inundation.duration.event.worker;

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
import org.kalypso.grid.GeoGridException;
import org.kalypso.grid.GeoGridUtilities;
import org.kalypso.grid.IGeoGrid;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IAllowedString;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IAllowedStrings;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategories;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool.POOL_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.IStyleReplacements;
import org.kalypso.nofdpidss.inundation.duration.IDurationConstant;
import org.kalypso.nofdpidss.inundation.duration.event.save.StyleReplacerEventBasedInundationDuration;
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

public class IDEExportWorker implements ICoreRunnableWithProgress {

    private static final GeometryFactory GF = new GeometryFactory();

    private final MyGridCategoryWrapper[] m_wrappers;

    private final IFolder m_workingFolder;

    private final IAllowedStrings m_allowedStrings;

    public IDEExportWorker(final IAllowedStrings allowedStrings, final IFolder workingFolder, final MyGridCategoryWrapper[] wrappers) {
        m_allowedStrings = allowedStrings;
        m_workingFolder = workingFolder;
        m_wrappers = wrappers;
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        monitor.subTask(Messages.IDFEventWorker_9);
        try {
            final FlattenToCategoryGrid grid = GeoGridUtilities.getFlattedGrid(m_wrappers, true);
            final AscGridExporter gridExporter = new AscGridExporter(-9999, 3);
            gridExporter.export(grid, new File(m_workingFolder.getLocation() + "/eventBasedDuration.asc"), monitor);
            monitor.worked(1);
            monitor.beginTask(Messages.IDFEventWorker_11, 1);
            vectorize(grid, monitor);
            monitor.worked(1);
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            return StatusUtilities.createErrorStatus(e.getMessage());
        }
        return Status.OK_STATUS;
    }

    private void vectorize(final IGeoGrid raster, final IProgressMonitor monitor) throws GeoGridException, Exception, GmlSerializeException {
        final double[] grenzen = new double[] { -0.5, 0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5 };
        final SegmentCollector collector = new PolygonCollector(GF, grenzen, false);
        final Raster2Lines r2l = new Raster2Lines(collector, grenzen);
        new Raster2LinesWalkingStrategy().walk(raster, r2l, null, monitor);
        final CollectorDataProvider[] data = collector.getData();
        writePolygonShape(data);
    }

    private void writePolygonShape(final CollectorDataProvider[] data) throws Exception, GmlSerializeException {
        final ITypeRegistry<IMarshallingTypeHandler> typeRegistry = MarshallingTypeRegistrySingleton.getTypeRegistry();
        final IMarshallingTypeHandler doubleTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_DOUBLE);
        final IMarshallingTypeHandler stringTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_STRING);
        final IMarshallingTypeHandler polygonTypeHandler = typeRegistry.getTypeHandlerForTypeName(GeometryUtilities.QN_POLYGON);
        final QName shapeTypeQName = new QName("anyNS", "shapeType");
        final IValuePropertyType doubleTypeId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "id"), doubleTypeHandler, 1, 1, false);
        final IValuePropertyType stringTypeRange = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "range"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType duration = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "duration"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType borderId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "border"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType polygonType = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "geometry"), polygonTypeHandler, 1, 1, false);
        final IPropertyType[] properties = new IPropertyType[] { polygonType, doubleTypeId, stringTypeRange, duration, borderId };
        final IFeatureType shapeFT = GMLSchemaFactory.createFeatureType(shapeTypeQName, properties);
        final Feature shapeRootFeature = ShapeSerializer.createWorkspaceRootFeature(shapeFT, ShapeConst.SHAPE_TYPE_POLYGONZ);
        final GMLWorkspace shapeWorkspace = shapeRootFeature.getWorkspace();
        final IRelationType shapeParentRelation = (IRelationType) shapeRootFeature.getFeatureType().getProperty(ShapeSerializer.PROPERTY_FEATURE_MEMBER);
        for (int i = 0; i < data.length; i++) {
            final GM_Surface<?> line = (GM_Surface<?>) data[i].getGeometry();
            final Double id = data[i].getId();
            final Double[] borders = data[i].getBorders();
            final String[] name = data[i].getName();
            IAllowedString string;
            if (1 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_1_VERY_SHORT);
            } else if (2 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_2_SHORT);
            } else if (3 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_3_MEDIUM);
            } else if (4 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_4_LONG);
            } else if (5 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_5_VERY_LONG);
            } else if (6 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_6_ALMOST_ALWAYS);
            } else if (7 == borders[0].intValue()) {
                string = m_allowedStrings.getString(IDurationConstant.DURATION_7_ALWAYS);
            } else {
                continue;
            }
            final String border = getBorder(string);
            final Object[] shapeData = new Object[] { line, id, name[0], string.getName(), border };
            final Feature feature = FeatureFactory.createFeature(shapeRootFeature, shapeParentRelation, "FeatureID" + i, shapeFT, shapeData);
            shapeWorkspace.addFeatureAsComposition(shapeRootFeature, shapeParentRelation, -1, feature);
        }
        final String shapeBase = m_workingFolder.getLocation() + "/eventBasedDuration";
        ShapeSerializer.serialize(shapeWorkspace, shapeBase, null);
        WorkspaceSync.sync(m_workingFolder, IResource.DEPTH_ONE);
        final IFile iFile = m_workingFolder.getFile("/eventBasedDuration.shp");
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
            if (IDurationConstant.DURATION_7_ALWAYS.equals(string.getId())) return Messages.IDEExportWorker_0; else return String.format(Messages.IDEExportWorker_1, duration.getDays(), duration.getHours(), duration.getMinutes());
        }
        return "-1";
    }

    private void generateStyle() throws IOException, CoreException {
        final IProject base = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFile baseFile = base.getFile(".styles/template_inundationduration.sld");
        final IFile iSld = m_workingFolder.getFile("eventBasedDuration.sld");
        FileUtils.copyFile(baseFile.getLocation().toFile(), iSld.getLocation().toFile());
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
        final PoolGeoData pool = (PoolGeoData) NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eGeodata);
        final IGeodataModel model = pool.getModel();
        final IGeodataCategory[] categories = model.getCategories().getCategories(new QName[] { IGeodataCategories.QN_SUBCATEGORY_INUNDATION_DURATION });
        if (categories.length != 1) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.IDFEventWorker_31);
        final IStyleReplacements replacer = new StyleReplacerEventBasedInundationDuration(m_wrappers, categories[0], iSld.getLocation().toFile());
        replacer.replace();
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
    }
}
