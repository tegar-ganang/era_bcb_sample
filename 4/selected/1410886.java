package org.kalypso.nofdpidss.analysis.suitabilities.ws;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.gmlschema.property.IPropertyType;
import org.kalypso.nofdp.idss.schema.schemata.gml.GmlConstants;
import org.kalypso.nofdp.idss.schema.utils.MonitorUtils;
import org.kalypso.nofdpidss.analysis.NofdpAnalysisDebug;
import org.kalypso.nofdpidss.analysis.i18n.Messages;
import org.kalypso.nofdpidss.analysis.navigation.ws.MyWsSeasonType;
import org.kalypso.nofdpidss.analysis.suitabilities.ISuitabilityMenuPart;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityHelper;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityShapeDelegate;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityShapeResultDelegate;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityThemeCommand;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMaps;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.ISuitabilities.WS_SEASON;
import org.kalypso.nofdpidss.core.common.shape.IShapeComparatorWorkspaceDelegate;
import org.kalypso.nofdpidss.core.common.shape.ShapeDissolverTools;
import org.kalypso.nofdpidss.core.common.shape.ShapeUtils;
import org.kalypso.nofdpidss.core.common.utils.gml.SuitabilityGmlUtils;
import org.kalypso.nofdpidss.core.common.utils.gml.WSGmlUtils;
import org.kalypso.nofdpidss.core.common.utils.gml.WSGmlUtils.WS_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.CDUtils;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerSuitability;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.core.common.utils.various.GeneralConfigGmlUtil;
import org.kalypso.nofdpidss.core.view.map.MapTool;
import org.kalypso.nofdpidss.core.view.map.MapTool.ADD_MAP_LAYER_AT;
import org.kalypso.nofdpidss.geodata.comparator.ShapeComparator;
import org.kalypso.nofdpidss.geodata.store.DelegateShapeExportConfig;
import org.kalypso.nofdpidss.geodata.store.IDelegateMapExportConfig;
import org.kalypso.observation.result.TupleResult;
import org.kalypso.ogc.gml.IKalypsoLayerModell;
import org.kalypso.ogc.gml.map.IMapPanel;
import org.kalypso.ogc.gml.serialize.GmlSerializeException;
import org.kalypso.ogc.gml.serialize.ShapeSerializer;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Property;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypso.ui.action.AddCascadingThemeCommand;
import org.kalypso.ui.action.IThemeCommand.ADD_THEME_POSITION;
import org.kalypso.ui.views.map.MapView;
import org.kalypsodeegree.KalypsoDeegreePlugin;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree.model.feature.FeatureList;
import org.kalypsodeegree.model.feature.GMLWorkspace;
import org.kalypsodeegree_impl.io.shpapi.ShapeFile;
import org.kalypsodeegree_impl.io.shpapi.dataprovider.StandardShapeDataProvider;
import org.xml.sax.SAXException;

/**
 * @author Dirk Kuch
 */
public class WSGenerator implements ICoreRunnableWithProgress {

    private final QName[] m_suitabilies = new QName[] { GmlConstants.QN_GEODATA_WATER_STORAGE_SUMMER, GmlConstants.QN_GEODATA_WATER_STORAGE_WINTER, GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_SMALL, GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_LARGE };

    public static final String WS_LAYER_ID = "LAYER_ID";

    public static final String WS_LAYER_VALUE = "WATER_STORAGE_SUITABILITY";

    private static final String WATER_STORAGE_FILE_NAME = "waterStorageSuitability";

    private final ISuitabilityMenuPart m_menuPart;

    private final IProject m_project;

    private final PoolGeoData m_pool;

    private final IGeodataSet[] m_datasets;

    private final MyWsSeasonType m_selection;

    /**
   * class responsible for generating water storage suitability results
   */
    public WSGenerator(final ISuitabilityMenuPart part, final IProject project, final PoolGeoData gmlPool, final MyWsSeasonType selection, final IGeodataSet[] selectedDatasets) {
        m_menuPart = part;
        m_project = project;
        m_pool = gmlPool;
        m_selection = selection;
        m_datasets = selectedDatasets;
    }

    private void setTargetValueColumn(final List<Feature> features, final QName wsType) {
        final TupleResult result = SuitabilityGmlUtils.getObsMapping(m_selection.getFeature());
        final Map<String, Map> mapping = getMapping(result);
        final List<Object> history = new ArrayList<Object>();
        for (final Feature feature : features) {
            final IPropertyType[] extNames = getExternalNames(feature);
            Map<String, Map> ptrMapping = mapping;
            for (final IPropertyType pt : extNames) {
                final Object objValue = feature.getProperty(pt);
                history.add(objValue);
                ptrMapping = ptrMapping.get(objValue);
                if (ptrMapping == null) {
                    String msg = Messages.WSGenerator_2;
                    for (final Object obj : history) {
                        if (obj == null) {
                            continue;
                        }
                        msg += obj.toString() + " - ";
                    }
                    throw new IllegalStateException(msg);
                }
            }
            final String[] arrSummer = ptrMapping.keySet().toArray(new String[] {});
            if (arrSummer.length != 1) throw new IllegalStateException(Messages.WSGenerator_3);
            ptrMapping = ptrMapping.get(arrSummer[0]);
            final String[] arrWinter = ptrMapping.keySet().toArray(new String[] {});
            if (arrWinter.length != 1) throw new IllegalStateException(Messages.WSGenerator_4);
            if (GmlConstants.QN_GEODATA_WATER_STORAGE_SUMMER.equals(wsType)) {
                feature.setProperty(wsType, arrSummer[0]);
            } else if (GmlConstants.QN_GEODATA_WATER_STORAGE_WINTER.equals(wsType)) {
                feature.setProperty(wsType, arrWinter[0]);
            } else {
                continue;
            }
            history.clear();
        }
    }

    private IPropertyType[] getExternalNames(final Feature feature) {
        final List<IPropertyType> types = new ArrayList<IPropertyType>();
        final IPropertyType[] properties = feature.getFeatureType().getProperties();
        for (final IPropertyType pt : properties) {
            final QName name = pt.getQName();
            final String localPart = name.getLocalPart();
            if (ShapeFile.GEOM.equals(localPart)) {
                continue;
            } else if ("id".equals(localPart)) {
                continue;
            } else if (ArrayUtils.contains(m_suitabilies, name)) {
                continue;
            }
            types.add(pt);
        }
        return types.toArray(new IPropertyType[] {});
    }

    private IFile generateMap(final IFile shapeFile, final String viewName) throws IOException, JAXBException, CoreException, SAXException, ParserConfigurationException {
        final IFolder mapFolder = m_project.getFolder(IMaps.FOLDER);
        final IFile mapTemplate = mapFolder.getFile(IMaps.ID_MAP_WATER_STORAGE_TEMPLATE);
        final File fTemplate = mapTemplate.getLocation().toFile();
        final IFolder parent = (IFolder) shapeFile.getParent();
        WorkspaceSync.sync(parent, IResource.DEPTH_INFINITE);
        final IFile map = parent.getFile(WSGenerator.WATER_STORAGE_FILE_NAME + ".gmt");
        final File fMap = map.getLocation().toFile();
        FileUtils.copyFile(fTemplate, fMap);
        WorkspaceSync.sync(parent, IResource.DEPTH_INFINITE);
        final StyledLayerType layer = getStyleLayer(viewName);
        MapTool.addMapLayer(map, layer, ADD_MAP_LAYER_AT.eFront);
        WorkspaceSync.sync(m_project, IResource.DEPTH_INFINITE);
        return map;
    }

    private IFile generateShapeFile(final Feature[] features, final QName resultType) throws GmlSerializeException, CoreException {
        final QName[] dontInclude = new QName[] { GmlConstants.QN_GEODATA_CONFLICT_DETECTION_CONFLICT };
        final QName[] properties = ShapeUtils.getShapeProperties(features[0], dontInclude);
        final Map<String, String> mapping = new LinkedHashMap<String, String>();
        for (final QName name : properties) {
            mapping.put(name.getLocalPart(), name.getLocalPart());
        }
        final IFolder folder = m_project.getFolder(NofdpIDSSConstants.NOFDP_TMP_FOLDER);
        if (!folder.exists()) {
            folder.create(true, true, new NullProgressMonitor());
        }
        final IFile file = folder.getFile(WSGenerator.WATER_STORAGE_FILE_NAME + ".shp");
        final String location = file.getLocation().toOSString();
        final int index = location.indexOf(".shp");
        final String subString = location.substring(0, index);
        final StandardShapeDataProvider provider = new StandardShapeDataProvider(features);
        ShapeSerializer.serializeFeatures(features, mapping, ShapeFile.GEOM, subString, provider);
        WorkspaceSync.sync(m_project, IResource.DEPTH_INFINITE);
        return file;
    }

    private Map<String, Map> getMapping(final TupleResult result) {
        return CDUtils.getBaseMap(result, false);
    }

    private StyledLayerType getStyleLayer(final String viewName) {
        final Style style = new Style();
        style.setStyle(viewName);
        style.setLinktype("sld");
        style.setType("simple");
        style.setHref("./" + WSGenerator.WATER_STORAGE_FILE_NAME + ".sld");
        style.setActuate("onRequest");
        final StyledLayerType layer = new StyledLayerType();
        layer.setVisible(true);
        layer.setName(viewName);
        layer.setId("1");
        layer.setFeaturePath("featureMember");
        layer.setType("simple");
        layer.setActuate("onRequest");
        layer.setHref(WSGenerator.WATER_STORAGE_FILE_NAME + "#" + KalypsoDeegreePlugin.getDefault().getCoordinateSystem());
        layer.setLinktype("shape");
        layer.getStyle().add(style);
        final List<Property> properties = layer.getProperty();
        final Property property = new StyledLayerType.Property();
        property.setName(WS_LAYER_ID);
        property.setValue(WS_LAYER_VALUE);
        properties.add(property);
        return layer;
    }

    /**
   * generates the water storage suitability shape file and its map
   */
    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        final boolean debug = NofdpAnalysisDebug.WATER_STORAGE_SUITABILITY_DEBUG.isEnabled();
        final List<GMLWorkspace> workspaces = new LinkedList<GMLWorkspace>();
        for (final IGeodataSet dataset : m_datasets) {
            try {
                final File shape = ShapeUtils.getShapeFileFromGeodataSet(m_project, dataset);
                workspaces.add(ShapeUtils.getShapeWorkspace(shape));
            } catch (final Exception e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
                return ExceptionHelper.getMultiState(this.getClass(), Messages.WSGenerator_1, new IStatus[] { StatusUtilities.createErrorStatus(e.getMessage()) });
            }
            MonitorUtils.step(monitor);
        }
        final WS_SEASON season = m_selection.getSeason();
        final WS_TYPE type = WSGmlUtils.getTypeFromSelection(m_selection.getFeature());
        String[] resultNames = new String[] {};
        final QName wsType = WSGmlUtils.getWSShapeTargetColumnType(season);
        if (WS_TYPE.eGeneral.equals(type)) {
            resultNames = new String[] { "FREQ", "DURATION", "DEPTH", "VSS_GEN" };
        } else if (WS_TYPE.eFloodPlains.equals(type)) {
            resultNames = new String[] { "FREQ", "DURATION", "DEPTH", "VSS_FP" };
        } else throw new IllegalStateException();
        final QName qn1 = SuitabilityGmlUtils.getDatasetDefaultColumn(workspaces.get(0).getGMLSchema().getTargetNamespace(), m_datasets[0]);
        final QName qn2 = SuitabilityGmlUtils.getDatasetDefaultColumn(workspaces.get(1).getGMLSchema().getTargetNamespace(), m_datasets[1]);
        IShapeComparatorWorkspaceDelegate delegate1 = new SuitabilityShapeDelegate(workspaces.get(0), qn1, resultNames[0], m_datasets[0]);
        IShapeComparatorWorkspaceDelegate delegate2 = new SuitabilityShapeDelegate(workspaces.get(1), qn2, resultNames[1], m_datasets[1]);
        SuitabilityShapeResultDelegate result = new SuitabilityShapeResultDelegate(delegate1, delegate2, wsType);
        monitor.subTask(String.format(Messages.WSGenerator_0, workspaces.size() - 1));
        ShapeComparator comparator = new ShapeComparator(delegate1, delegate2, result);
        IStatus status = comparator.execute(monitor);
        if (IStatus.ERROR == status.getSeverity()) return status;
        if (debug) {
            SuitabilityHelper.writeDebug(result);
        }
        GMLWorkspace workspace = result.getWorkspace();
        delegate1.dispose();
        delegate2.dispose();
        if (monitor.isCanceled()) return null;
        MonitorUtils.step(monitor);
        for (int i = 2; i < workspaces.size(); i++) {
            monitor.subTask(String.format(Messages.WSGenerator_20, i, workspaces.size() - 1));
            final QName qni = SuitabilityGmlUtils.getDatasetDefaultColumn(workspaces.get(i).getGMLSchema().getTargetNamespace(), m_datasets[i]);
            delegate1 = new SuitabilityShapeDelegate(workspace, null, null, null);
            delegate2 = new SuitabilityShapeDelegate(workspaces.get(i), qni, resultNames[i], m_datasets[i]);
            result = new SuitabilityShapeResultDelegate(delegate1, delegate2, wsType);
            comparator = new ShapeComparator(delegate1, delegate2, result);
            status = comparator.execute(monitor);
            if (IStatus.ERROR == status.getSeverity()) return status;
            if (debug) {
                SuitabilityHelper.writeDebug(result);
            }
            workspace = result.getWorkspace();
            delegate1.dispose();
            delegate2.dispose();
            if (monitor.isCanceled()) return null;
            MonitorUtils.step(monitor);
        }
        final FeatureList myList = ShapeDissolverTools.getFeatureListFromRoot(workspace.getRootFeature());
        if (myList.size() == 0) throw new CoreException(StatusUtilities.createErrorStatus(Messages.WSGenerator_19));
        setTargetValueColumn(myList, wsType);
        final List<Feature> myFeatures = new ArrayList<Feature>();
        for (final Object object : myList) {
            if (!(object instanceof Feature)) {
                continue;
            }
            final Feature f = (Feature) object;
            myFeatures.add(f);
        }
        try {
            final IFile shapeFile = generateShapeFile(myFeatures.toArray(new Feature[] {}), wsType);
            MonitorUtils.step(monitor);
            final IGeodataCategory category = GeneralConfigGmlUtil.getWaterStorageSuitabilityCategory(m_pool);
            if (category == null) throw new IllegalStateException();
            final WSByPassImportWizard bypass = new WSByPassImportWizard(shapeFile);
            m_menuPart.setAdapterParameter(DelegateShapeExportConfig.class, new DelegateShapeExportConfig(category, shapeFile, bypass));
            final IFile iTemplate = BaseGeoUtils.getStyleTemplateForCategory(category);
            final File fTemplateSld = iTemplate.getLocation().toFile();
            final IFolder tmpFolder = m_project.getFolder(NofdpIDSSConstants.NOFDP_TMP_FOLDER);
            final File fWorkingSld = new File(tmpFolder.getLocation().toFile(), WSGenerator.WATER_STORAGE_FILE_NAME + ".sld");
            FileUtils.copyFile(fTemplateSld, fWorkingSld);
            MonitorUtils.step(monitor);
            final StyleReplacerSuitability styleReplacer = new StyleReplacerSuitability(category, fWorkingSld, wsType.getLocalPart());
            final boolean replace = styleReplacer.replace();
            if (!replace) throw new IllegalStateException();
            MonitorUtils.step(monitor);
            m_menuPart.setAdapterParameter(IDelegateMapExportConfig.class, new WSMapExportConfig(m_pool, category, bypass));
            addMapResult(shapeFile, styleReplacer.getViewName());
            new UIJob("") {

                @Override
                public IStatus runInUIThread(final IProgressMonitor monitor) {
                    SuitabilityHelper.zoomToResultLayer(m_menuPart.getMapView());
                    return Status.OK_STATUS;
                }
            }.schedule(1000);
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            return ExceptionHelper.getMultiState(this.getClass(), Messages.WSGenerator_22, new IStatus[] { StatusUtilities.createErrorStatus(e.getMessage()) });
        }
        return Status.OK_STATUS;
    }

    private void addMapResult(final IFile shapeFile, final String styleName) {
        final MapView mapView = m_menuPart.getMapView();
        final IMapPanel mapPanel = mapView.getMapPanel();
        final IKalypsoLayerModell mapModell = (IKalypsoLayerModell) mapPanel.getMapModell();
        final AddCascadingThemeCommand command = new AddCascadingThemeCommand(mapModell, Messages.WSGenerator_23, ADD_THEME_POSITION.eFront);
        command.addCommand(new SuitabilityThemeCommand(Messages.WSGenerator_24, shapeFile, styleName));
        final Property prLayerId = new StyledLayerType.Property();
        prLayerId.setName(SuitabilityHelper.LAYER_ID);
        prLayerId.setValue(Messages.WSGenerator_25);
        final Property prInput = new StyledLayerType.Property();
        prInput.setName(SuitabilityHelper.IS_RESULT_LAYER);
        prInput.setValue("true");
        final Property prDel = new StyledLayerType.Property();
        prDel.setName("deleteable");
        prDel.setValue("false");
        command.addProperties(new StyledLayerType.Property[] { prLayerId, prInput, prDel });
        mapView.postCommand(command, null);
    }
}
