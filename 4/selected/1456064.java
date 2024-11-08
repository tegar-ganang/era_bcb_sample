package org.kalypso.nofdpidss.hydraulic.computation.processing.worker.floodzones;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
import org.kalypso.contribs.eclipse.core.resources.ResourceUtilities;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.contribs.eclipse.ui.progress.ConsoleHelper;
import org.kalypso.contribs.java.io.MyPrintStream;
import org.kalypso.gml.processes.raster2vector.Raster2Lines;
import org.kalypso.gml.processes.raster2vector.Raster2LinesWalkingStrategy;
import org.kalypso.gml.processes.raster2vector.collector.CollectorDataProvider;
import org.kalypso.gml.processes.raster2vector.collector.PolygonCollector;
import org.kalypso.gmlschema.GMLSchemaFactory;
import org.kalypso.gmlschema.feature.IFeatureType;
import org.kalypso.gmlschema.property.IPropertyType;
import org.kalypso.gmlschema.property.IValuePropertyType;
import org.kalypso.gmlschema.property.relation.IRelationType;
import org.kalypso.gmlschema.types.IMarshallingTypeHandler;
import org.kalypso.gmlschema.types.ITypeRegistry;
import org.kalypso.gmlschema.types.MarshallingTypeRegistrySingleton;
import org.kalypso.grid.AscGridExporter;
import org.kalypso.grid.GeoGridException;
import org.kalypso.grid.GeoGridUtilities;
import org.kalypso.grid.IGeoGrid;
import org.kalypso.model.wspm.core.gml.IProfileFeature;
import org.kalypso.model.wspm.sobek.core.interfaces.IBranch;
import org.kalypso.model.wspm.sobek.core.interfaces.ICrossSectionNode;
import org.kalypso.model.wspm.sobek.core.interfaces.ISobekModelMember;
import org.kalypso.model.wspm.sobek.result.processing.interfaces.ISobekResultModel;
import org.kalypso.model.wspm.sobek.result.processing.model.IResultTimeSeries;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategories;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.hydraulic.base.IHydraulModel;
import org.kalypso.nofdpidss.core.base.gml.model.project.base.IMeasure;
import org.kalypso.nofdpidss.core.base.gml.model.project.base.IMeasureProtectedArea;
import org.kalypso.nofdpidss.core.base.gml.model.project.base.IProjectModel;
import org.kalypso.nofdpidss.core.base.gml.model.project.base.IVariant;
import org.kalypso.nofdpidss.core.common.utils.modules.IStyleReplacements;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerInundationArea;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerInundationDepth;
import org.kalypso.nofdpidss.hydraulic.computation.i18n.Messages;
import org.kalypso.nofdpidss.hydraulic.computation.processing.interfaces.ICalculationHandler;
import org.kalypso.nofdpidss.hydraulic.computation.processing.interfaces.IHydraulicCalculationCase;
import org.kalypso.nofdpidss.hydraulic.computation.processing.worker.floodzones.utils.FloodDiffGridHydraulic;
import org.kalypso.nofdpidss.hydraulic.computation.processing.worker.floodzones.utils.FloodZoneHelper;
import org.kalypso.nofdpidss.hydraulic.computation.processing.worker.utils.workspace.IWorkspaceProvider;
import org.kalypso.ogc.gml.serialize.GmlSerializeException;
import org.kalypso.ogc.gml.serialize.GmlSerializer;
import org.kalypso.ogc.gml.serialize.ShapeSerializer;
import org.kalypsodeegree.KalypsoDeegreePlugin;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree.model.feature.GMLWorkspace;
import org.kalypsodeegree.model.geometry.GM_Exception;
import org.kalypsodeegree.model.geometry.GM_Surface;
import org.kalypsodeegree.model.geometry.GM_Triangle;
import org.kalypsodeegree.model.geometry.GM_TriangulatedSurface;
import org.kalypsodeegree_impl.gml.binding.commons.ICoverage;
import org.kalypsodeegree_impl.gml.binding.commons.ICoverageCollection;
import org.kalypsodeegree_impl.io.shpapi.ShapeConst;
import org.kalypsodeegree_impl.model.feature.FeatureFactory;
import org.kalypsodeegree_impl.tools.GeometryUtilities;
import com.vividsolutions.jts.geom.GeometryFactory;

public class FloodZoneWorker implements ICoreRunnableWithProgress {

    private final ISobekResultModel m_model;

    private final ICalculationHandler m_calculationHandler;

    private final IWorkspaceProvider m_provider;

    private static final GeometryFactory GF = new GeometryFactory();

    private final MyPrintStream m_outputStream;

    private final IHydraulicCalculationCase m_calculationCase;

    public FloodZoneWorker(final ISobekResultModel model, final IWorkspaceProvider provider, final IHydraulicCalculationCase calculationCase, final ICalculationHandler flood, final MyPrintStream outputStream) {
        m_model = model;
        m_provider = provider;
        m_calculationCase = calculationCase;
        m_calculationHandler = flood;
        m_outputStream = outputStream;
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        try {
            ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_0));
            final IGeodataModel geodataModel = m_provider.getGeodataModel();
            final IHydraulModel hydraulModel = m_provider.getHydraulicModel();
            final IGeodataSet lnkDEM = hydraulModel.getLnkDEM(geodataModel);
            if (lnkDEM == null) return StatusUtilities.createStatus(IStatus.CANCEL, "Digital Elevation Model needed for generating flooding areas.", new GM_Exception(""));
            final IMeasureProtectedArea[] areas = getProjtectedAreaMeasures();
            final ISobekModelMember sobekModel = hydraulModel.getSobekModelMember();
            final IBranch[] branches = sobekModel.getBranchMembers();
            final String crs = KalypsoDeegreePlugin.getDefault().getCoordinateSystem();
            final GM_TriangulatedSurface surface = org.kalypsodeegree_impl.model.geometry.GeometryFactory.createGM_TriangulatedSurface(crs);
            ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_4));
            for (int b = 0; b < branches.length; b++) {
                final IBranch branch = branches[b];
                ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_5, branch.getName(), b + 1, branches.length));
                if (monitor.isCanceled()) return Status.CANCEL_STATUS;
                final ICrossSectionNode[] nodes = branch.getCrossSectionNodes();
                for (int c = 0; c < nodes.length - 1; c++) {
                    if (monitor.isCanceled()) return Status.CANCEL_STATUS;
                    try {
                        final ICrossSectionNode csn1 = nodes[c];
                        final ICrossSectionNode csn2 = nodes[c + 1];
                        ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_6, csn1.getName(), csn2.getName()));
                        final IProfileFeature p1 = csn1.getLinkedProfile();
                        final IProfileFeature p2 = csn2.getLinkedProfile();
                        final IResultTimeSeries r1 = m_model.getNodeTimeSeries(csn1);
                        final IResultTimeSeries r2 = m_model.getNodeTimeSeries(csn2);
                        final Double w1 = m_calculationHandler.getFloodZoneWaterLevel(r1);
                        final Double w2 = m_calculationHandler.getFloodZoneWaterLevel(r2);
                        final GM_Triangle[] triangles = FloodZoneHelper.createSurface(p1, w1, p2, w2);
                        for (final GM_Triangle triangle : triangles) {
                            surface.add(triangle);
                        }
                    } catch (final GM_Exception e) {
                        NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
                    }
                }
            }
            ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_7));
            ConsoleHelper.writeLine(m_outputStream, String.format(""));
            ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_9));
            final FloodDiffGridHydraulic grid = new FloodDiffGridHydraulic(getGeoGrid(lnkDEM), surface, areas);
            final IFolder floodZoneFolder = m_calculationCase.getFloodZoneFolder();
            final AscGridExporter gridExporter = new AscGridExporter(-9999, 3);
            gridExporter.export(grid, new File(floodZoneFolder.getLocation() + "/floodZones.asc"), monitor);
            WorkspaceSync.sync(floodZoneFolder, IResource.DEPTH_ONE);
            ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_10));
            if (m_calculationHandler.createGeodataSets()) {
                final DGMUpdater updater = new DGMUpdater(m_calculationCase, m_provider);
                updater.execute(monitor);
                final InundationDepthGridExporter wlGridExporter = new InundationDepthGridExporter(m_provider, floodZoneFolder.getFile("floodZones.asc"));
                wlGridExporter.execute(monitor);
                exportInundationShapes(floodZoneFolder, grid, monitor);
            }
            grid.dispose();
            ConsoleHelper.writeLine(m_outputStream, String.format(Messages.FloodZoneWorker_12));
            ConsoleHelper.writeLine(m_outputStream, String.format(""));
        } catch (final Exception e) {
            return StatusUtilities.createErrorStatus(e.getMessage());
        }
        return StatusUtilities.createOkStatus(Messages.FloodZoneWorker_14);
    }

    private IMeasureProtectedArea[] getProjtectedAreaMeasures() throws CoreException {
        final IProjectModel projectModel = m_provider.getProjectModel();
        final IVariant variant = m_calculationCase.getVariantResultMember().getLinkedVariant(projectModel);
        final List<IMeasureProtectedArea> myAreas = new ArrayList<IMeasureProtectedArea>();
        final IMeasure[] measures = variant.getMeasures();
        for (final IMeasure measure : measures) {
            if (measure instanceof IMeasureProtectedArea) {
                myAreas.add((IMeasureProtectedArea) measure);
            }
        }
        return myAreas.toArray(new IMeasureProtectedArea[] {});
    }

    private void exportInundationShapes(final IFolder importDataFolder, final IGeoGrid raster, final IProgressMonitor monitor) throws GeoGridException, Exception, GmlSerializeException {
        final boolean bSimple = false;
        double[] grenzen = new double[] { 0.0, 0.5, 1000 };
        PolygonCollector collector = new PolygonCollector(GF, grenzen, bSimple);
        Raster2Lines r2l = new Raster2Lines(collector, grenzen);
        new Raster2LinesWalkingStrategy().walk(raster, r2l, null, monitor);
        CollectorDataProvider[] data = collector.getData();
        writeInundationDepthsAsShape(data, importDataFolder);
        grenzen = new double[] { 0.0, 1000 };
        collector = new PolygonCollector(GF, grenzen, bSimple);
        r2l = new Raster2Lines(collector, grenzen);
        new Raster2LinesWalkingStrategy().walk(raster, r2l, null, monitor);
        data = collector.getData();
        writeInundationAreaAsShape(data, importDataFolder);
    }

    private void writeInundationAreaAsShape(final CollectorDataProvider[] data, final IFolder importDataFolder) throws Exception, GmlSerializeException {
        final ITypeRegistry<IMarshallingTypeHandler> typeRegistry = MarshallingTypeRegistrySingleton.getTypeRegistry();
        final IMarshallingTypeHandler doubleTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_DOUBLE);
        final IMarshallingTypeHandler stringTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_STRING);
        final IMarshallingTypeHandler polygonTypeHandler = typeRegistry.getTypeHandlerForTypeName(GeometryUtilities.QN_POLYGON);
        final QName shapeTypeQName = new QName("anyNS", "shapeType");
        final IValuePropertyType doubleTypeId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "id"), doubleTypeHandler, 1, 1, false);
        final IValuePropertyType stringTypeRange = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "inundation"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType polygonType = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "geometry"), polygonTypeHandler, 1, 1, false);
        final IPropertyType[] properties = new IPropertyType[] { polygonType, doubleTypeId, stringTypeRange };
        final IFeatureType shapeFT = GMLSchemaFactory.createFeatureType(shapeTypeQName, properties);
        final Feature shapeRootFeature = ShapeSerializer.createWorkspaceRootFeature(shapeFT, ShapeConst.SHAPE_TYPE_POLYGONZ);
        final GMLWorkspace shapeWorkspace = shapeRootFeature.getWorkspace();
        final IRelationType shapeParentRelation = (IRelationType) shapeRootFeature.getFeatureType().getProperty(ShapeSerializer.PROPERTY_FEATURE_MEMBER);
        for (int i = 0; i < data.length; i++) {
            final GM_Surface<?> surface = (GM_Surface<?>) data[i].getGeometry();
            final Double id = data[i].getId();
            final Double[] borders = data[i].getBorders();
            final double to = borders[1];
            if (to <= 0) {
                continue;
            }
            final String value = "inundated";
            final Object[] shapeData = new Object[] { surface, id, value };
            final Feature feature = FeatureFactory.createFeature(shapeRootFeature, shapeParentRelation, "FeatureID" + i, shapeFT, shapeData);
            shapeWorkspace.addFeatureAsComposition(shapeRootFeature, shapeParentRelation, -1, feature);
        }
        final String shapeBase = importDataFolder.getLocation() + "/inundationArea";
        ShapeSerializer.serialize(shapeWorkspace, shapeBase, null);
        WorkspaceSync.sync(importDataFolder, IResource.DEPTH_ONE);
        final IFile iFile = importDataFolder.getFile("/inundationArea.shp");
        if (!iFile.exists()) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), String.format(Messages.FloodZoneWorker_1, iFile));
        generateInundationAreaStyle(importDataFolder);
    }

    private void generateInundationAreaStyle(final IFolder importDataFolder) throws IOException, CoreException {
        final IProject base = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFile baseFile = base.getFile(".styles/template_inundationarea.sld");
        final IFile iSld = importDataFolder.getFile("inundationArea.sld");
        FileUtils.copyFile(baseFile.getLocation().toFile(), iSld.getLocation().toFile());
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
        final IGeodataModel model = m_provider.getGeodataModel();
        final IGeodataCategory[] categories = model.getCategories().getCategories(new QName[] { IGeodataCategories.QN_SUBCATEGORY_INUNDATION_AREA });
        if (categories.length != 1) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.FloodZoneWorker_2);
        final IStyleReplacements replacer = new StyleReplacerInundationArea(categories[0], iSld.getLocation().toFile());
        replacer.replace();
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
    }

    private void writeInundationDepthsAsShape(final CollectorDataProvider[] data, final IFolder importDataFolder) throws Exception, GmlSerializeException {
        final ITypeRegistry<IMarshallingTypeHandler> typeRegistry = MarshallingTypeRegistrySingleton.getTypeRegistry();
        final IMarshallingTypeHandler doubleTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_DOUBLE);
        final IMarshallingTypeHandler stringTypeHandler = typeRegistry.getTypeHandlerForTypeName(XmlTypes.XS_STRING);
        final IMarshallingTypeHandler polygonTypeHandler = typeRegistry.getTypeHandlerForTypeName(GeometryUtilities.QN_POLYGON);
        final QName shapeTypeQName = new QName("anyNS", "shapeType");
        final IValuePropertyType doubleTypeId = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "id"), doubleTypeHandler, 1, 1, false);
        final IValuePropertyType stringTypeRange = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "depth"), stringTypeHandler, 1, 1, false);
        final IValuePropertyType polygonType = GMLSchemaFactory.createValuePropertyType(new QName("anyNS", "geometry"), polygonTypeHandler, 1, 1, false);
        final IPropertyType[] properties = new IPropertyType[] { polygonType, doubleTypeId, stringTypeRange };
        final IFeatureType shapeFT = GMLSchemaFactory.createFeatureType(shapeTypeQName, properties);
        final Feature shapeRootFeature = ShapeSerializer.createWorkspaceRootFeature(shapeFT, ShapeConst.SHAPE_TYPE_POLYGONZ);
        final GMLWorkspace shapeWorkspace = shapeRootFeature.getWorkspace();
        final IRelationType shapeParentRelation = (IRelationType) shapeRootFeature.getFeatureType().getProperty(ShapeSerializer.PROPERTY_FEATURE_MEMBER);
        for (int i = 0; i < data.length; i++) {
            final GM_Surface<?> surface = (GM_Surface<?>) data[i].getGeometry();
            final Double id = data[i].getId();
            final Double[] borders = data[i].getBorders();
            final double to = borders[0];
            String value;
            if (to < 0.5 && to >= 0.0) {
                value = "shallow";
            } else if (to >= 0.5) {
                value = "deep";
            } else {
                value = "-9999";
            }
            final Object[] shapeData = new Object[] { surface, id, value };
            final Feature feature = FeatureFactory.createFeature(shapeRootFeature, shapeParentRelation, "FeatureID" + i, shapeFT, shapeData);
            shapeWorkspace.addFeatureAsComposition(shapeRootFeature, shapeParentRelation, -1, feature);
        }
        final String shapeBase = importDataFolder.getLocation() + "/inundationDepth";
        ShapeSerializer.serialize(shapeWorkspace, shapeBase, null);
        WorkspaceSync.sync(importDataFolder, IResource.DEPTH_ONE);
        final IFile iFile = importDataFolder.getFile("/inundationDepth.shp");
        if (!iFile.exists()) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), String.format(Messages.FloodZoneWorker_1, iFile));
        generateInundationDepthStyle(importDataFolder);
    }

    private void generateInundationDepthStyle(final IFolder importDataFolder) throws IOException, CoreException {
        final IProject base = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFile baseFile = base.getFile(".styles/template_inundationdepth.sld");
        final IFile iSld = importDataFolder.getFile("inundationDepth.sld");
        FileUtils.copyFile(baseFile.getLocation().toFile(), iSld.getLocation().toFile());
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
        final IGeodataModel model = m_provider.getGeodataModel();
        final IGeodataCategory[] categories = model.getCategories().getCategories(new QName[] { IGeodataCategories.QN_SUBCATEGORY_INUNDATION_DEPTH });
        if (categories.length != 1) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.FloodZoneWorker_2);
        final IStyleReplacements replacer = new StyleReplacerInundationDepth(categories[0], iSld.getLocation().toFile());
        replacer.replace();
        WorkspaceSync.sync(iSld, IResource.DEPTH_ONE);
    }

    private IGeoGrid getGeoGrid(final IGeodataSet lnkDEM) throws Exception {
        final URL gmlUrl = ResourceUtilities.createURL(lnkDEM.getIFile());
        final GMLWorkspace workspace = GmlSerializer.createGMLWorkspace(gmlUrl, null);
        final ICoverageCollection coverages = (ICoverageCollection) workspace.getRootFeature().getAdapter(ICoverageCollection.class);
        if (coverages.size() != 1) throw ExceptionHelper.getCoreException(IStatus.ERROR, this.getClass(), Messages.FloodZoneWorker_3);
        for (final ICoverage coverage : coverages) return GeoGridUtilities.toGrid(coverage);
        return null;
    }
}
