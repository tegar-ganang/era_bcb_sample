package org.kalypso.nofdpidss.analysis.suitabilities.vs;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
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
import org.kalypso.nofdpidss.analysis.suitabilities.ISuitabilityMenuPart;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityHelper;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityShapeDelegate;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityShapeResultDelegate;
import org.kalypso.nofdpidss.analysis.suitabilities.SuitabilityThemeCommand;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.shape.ShapeDissolverTools;
import org.kalypso.nofdpidss.core.common.shape.ShapeUtils;
import org.kalypso.nofdpidss.core.common.utils.gml.SuitabilityGmlUtils;
import org.kalypso.nofdpidss.core.common.utils.gml.VSGmlUtils;
import org.kalypso.nofdpidss.core.common.utils.modules.CDUtils;
import org.kalypso.nofdpidss.core.common.utils.modules.VsUtils;
import org.kalypso.nofdpidss.core.common.utils.modules.VsUtils.VS_TYPE;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerSuitability;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.core.common.utils.various.GeneralConfigGmlUtil;
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

/**
 * @author Dirk Kuch
 */
public class VSGenerator implements ICoreRunnableWithProgress {

    private final QName[] m_suitabilies = new QName[] { GmlConstants.QN_GEODATA_WATER_STORAGE_SUMMER, GmlConstants.QN_GEODATA_WATER_STORAGE_WINTER, GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_SMALL, GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_LARGE };

    public static final String VS_LAYER_ID = "LAYER_ID";

    public static final String VS_LAYER_VALUE = "VEGETATION_SUITABILITY";

    public static final String VEG_SMALL = "VSGEN";

    public static final String VEG_LARGE = "VSPLAIN";

    private static final String VEGETATION_SUITABILITY_FILE_NAME = "vegetationSuitability";

    private final IGeodataSet[] m_datasets;

    private final IProject m_project;

    private final Feature m_vegetationSuitability;

    private final PoolGeoData m_pool;

    private final ISuitabilityMenuPart m_menuPart;

    public VSGenerator(final ISuitabilityMenuPart part, final IProject project, final PoolGeoData pool, final Feature vegetationSuitability, final IGeodataSet[] datasets) {
        m_menuPart = part;
        m_project = project;
        m_pool = pool;
        m_vegetationSuitability = vegetationSuitability;
        m_datasets = datasets;
    }

    /** computation of result and adding computed result to shape workspace */
    private void setTargetValueColumn(final List<Feature> features, final QName vsType) {
        final TupleResult result = SuitabilityGmlUtils.getObsMapping(m_vegetationSuitability);
        final List<Object> history = new ArrayList<Object>();
        final Map<String, Map> mapping = getMapping(result);
        for (final Feature feature : features) {
            final IPropertyType[] extNames = getExternalNames(feature);
            Map<String, Map> ptrMapping = mapping;
            for (final IPropertyType pt : extNames) {
                final Object objValue = feature.getProperty(pt);
                history.add(objValue);
                ptrMapping = ptrMapping.get(objValue);
                if (ptrMapping == null) {
                    String msg = Messages.VSGenerator_3;
                    for (final Object obj : history) {
                        if (obj == null) {
                            continue;
                        }
                        msg += obj.toString() + " - ";
                    }
                    throw new IllegalStateException(msg);
                }
            }
            final String[] keys = ptrMapping.keySet().toArray(new String[] {});
            if (keys.length != 1) throw new IllegalStateException(Messages.VSGenerator_4);
            feature.setProperty(vsType, keys[0]);
            history.clear();
        }
    }

    private IFile generateShapeFile(final Feature[] features, final QName resultType) throws GmlSerializeException, CoreException {
        final QName[] properties = ShapeUtils.getShapeProperties(features[0], m_suitabilies);
        final Map<String, String> mapping = new LinkedHashMap<String, String>();
        for (final QName name : properties) {
            mapping.put(name.getLocalPart(), name.getLocalPart());
        }
        if (resultType.equals(GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_SMALL)) {
            mapping.put(VSGenerator.VEG_SMALL, GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_SMALL.getLocalPart());
        } else if (resultType.equals(GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_LARGE)) {
            mapping.put(VSGenerator.VEG_LARGE, GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_LARGE.getLocalPart());
        } else throw new NotImplementedException();
        final IFolder folder = m_project.getFolder(NofdpIDSSConstants.NOFDP_TMP_FOLDER);
        if (!folder.exists()) {
            folder.create(true, true, new NullProgressMonitor());
        }
        final IFile file = folder.getFile(VSGenerator.VEGETATION_SUITABILITY_FILE_NAME + ".shp");
        final String location = file.getLocation().toOSString();
        final int index = location.indexOf(".shp");
        final String subString = location.substring(0, index);
        final StandardShapeDataProvider provider = new StandardShapeDataProvider(features);
        ShapeSerializer.serializeFeatures(features, mapping, ShapeFile.GEOM, subString, provider);
        WorkspaceSync.sync(m_project, IResource.DEPTH_INFINITE);
        return file;
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

    /**
   * mapping of an observation
   */
    private Map<String, Map> getMapping(final TupleResult result) {
        return CDUtils.getBaseMap(result, false);
    }

    private StyledLayerType getStyleLayer(final String viewName) {
        final Style style = new Style();
        style.setStyle(viewName);
        style.setLinktype("sld");
        style.setType("simple");
        style.setHref("./" + VSGenerator.VEGETATION_SUITABILITY_FILE_NAME + ".sld");
        style.setActuate("onRequest");
        final StyledLayerType layer = new StyledLayerType();
        layer.setVisible(true);
        layer.setName(viewName);
        layer.setId("1");
        layer.setFeaturePath("featureMember");
        layer.setType("simple");
        layer.setActuate("onRequest");
        layer.setHref(VSGenerator.VEGETATION_SUITABILITY_FILE_NAME + "#" + KalypsoDeegreePlugin.getDefault().getCoordinateSystem());
        layer.setLinktype("shape");
        layer.getStyle().add(style);
        final List<Property> properties = layer.getProperty();
        final Property property = new StyledLayerType.Property();
        property.setName(VS_LAYER_ID);
        property.setValue(VS_LAYER_VALUE);
        properties.add(property);
        return layer;
    }

    /**
   * @see org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress#execute(org.eclipse.core.runtime.IProgressMonitor)
   */
    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        final boolean debug = NofdpAnalysisDebug.VEGETATION_SUITABILITY_DEBUG.isEnabled();
        final List<GMLWorkspace> workspaces = new LinkedList<GMLWorkspace>();
        for (final IGeodataSet dataset : m_datasets) {
            final File shape = ShapeUtils.getShapeFileFromGeodataSet(m_project, dataset);
            try {
                workspaces.add(ShapeUtils.getShapeWorkspace(dataset, shape));
            } catch (final Exception e) {
                NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
                return ExceptionHelper.getMultiState(this.getClass(), Messages.VSGenerator_2, new IStatus[] { StatusUtilities.createErrorStatus(e.getMessage()) });
            }
            MonitorUtils.step(monitor);
        }
        final QName vsType = VSGmlUtils.getVSShapeTargetColumnType(m_vegetationSuitability);
        String[] resultNames = new String[] {};
        final VS_TYPE type = VsUtils.getSelectedMenuFromFeature(m_vegetationSuitability);
        if (VS_TYPE.eSmall.equals(type)) {
            resultNames = new String[] { "NUTRIENT", "MOISTURE", "VEGETATION" };
        } else if (VS_TYPE.eSmallSalinity.equals(type)) {
            resultNames = new String[] { "NUTRIENT", "MOISTURE", "VEGETATION", "SALINITY" };
        } else if (VS_TYPE.eLarge.equals(type)) {
            resultNames = new String[] { "FLOODPLAIN", "DURATION" };
        } else throw new IllegalStateException();
        final QName qn1 = SuitabilityGmlUtils.getDatasetDefaultColumn(workspaces.get(0).getGMLSchema().getTargetNamespace(), m_datasets[0]);
        final QName qn2 = SuitabilityGmlUtils.getDatasetDefaultColumn(workspaces.get(1).getGMLSchema().getTargetNamespace(), m_datasets[1]);
        SuitabilityShapeDelegate delegate1 = new SuitabilityShapeDelegate(workspaces.get(0), qn1, resultNames[0], m_datasets[0]);
        SuitabilityShapeDelegate delegate2 = new SuitabilityShapeDelegate(workspaces.get(1), qn2, resultNames[1], m_datasets[1]);
        SuitabilityShapeResultDelegate result = new SuitabilityShapeResultDelegate(delegate1, delegate2, vsType);
        monitor.subTask(String.format(Messages.VSGenerator_1, workspaces.size() - 1));
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
        for (int i = 2; i < workspaces.size(); i++) {
            monitor.subTask(String.format(Messages.VSGenerator_0, i, workspaces.size() - 1));
            final QName qni = SuitabilityGmlUtils.getDatasetDefaultColumn(workspaces.get(i).getGMLSchema().getTargetNamespace(), m_datasets[i]);
            delegate1 = new SuitabilityShapeDelegate(workspace, null, null, null);
            delegate2 = new SuitabilityShapeDelegate(workspaces.get(i), qni, resultNames[i], m_datasets[i]);
            result = new SuitabilityShapeResultDelegate(delegate1, delegate2, vsType);
            comparator = new ShapeComparator(delegate1, delegate2, result);
            status = comparator.execute(monitor);
            if (IStatus.ERROR == status.getSeverity()) return status;
            if (debug) {
                SuitabilityHelper.writeDebug(result);
            }
            workspace = result.getWorkspace();
            delegate1.dispose();
            delegate2.dispose();
            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            MonitorUtils.step(monitor);
        }
        final FeatureList myList = ShapeDissolverTools.getFeatureListFromRoot(workspace.getRootFeature());
        if (myList.size() == 0) throw new CoreException(StatusUtilities.createErrorStatus(Messages.VSGenerator_29));
        setTargetValueColumn(myList, vsType);
        final List<Feature> myFeatures = new ArrayList<Feature>();
        for (final Object object : myList) {
            if (!(object instanceof Feature)) {
                continue;
            }
            final Feature f = (Feature) object;
            myFeatures.add(f);
        }
        try {
            final IFile shapeFile = generateShapeFile(myFeatures.toArray(new Feature[] {}), vsType);
            MonitorUtils.step(monitor);
            final IGeodataCategory category = GeneralConfigGmlUtil.getVegetationSuitabilityCategory(m_pool, m_vegetationSuitability.getParentRelation().getQName());
            if (category == null) throw new IllegalStateException();
            final VSByPassImportWizard bypass = new VSByPassImportWizard(shapeFile);
            m_menuPart.setAdapterParameter(DelegateShapeExportConfig.class, new DelegateShapeExportConfig(category, shapeFile, bypass));
            final IFile iTemplate = BaseGeoUtils.getStyleTemplateForCategory(category);
            final File fTemplateSld = iTemplate.getLocation().toFile();
            final IFolder tmpFolder = m_project.getFolder(NofdpIDSSConstants.NOFDP_TMP_FOLDER);
            final File fWorkingSld = new File(tmpFolder.getLocation().toFile(), VSGenerator.VEGETATION_SUITABILITY_FILE_NAME + ".sld");
            FileUtils.copyFile(fTemplateSld, fWorkingSld);
            MonitorUtils.step(monitor);
            final StyleReplacerSuitability styleReplacer;
            if (vsType.equals(GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_SMALL)) {
                styleReplacer = new StyleReplacerSuitability(category, fWorkingSld, VSGenerator.VEG_SMALL);
            } else if (vsType.equals(GmlConstants.QN_GEODATA_VEGETATION_SUITABILITY_LARGE)) {
                styleReplacer = new StyleReplacerSuitability(category, fWorkingSld, VSGenerator.VEG_LARGE);
            } else throw new NotImplementedException();
            final boolean replace = styleReplacer.replace();
            if (!replace) throw new IllegalStateException();
            MonitorUtils.step(monitor);
            m_menuPart.setAdapterParameter(IDelegateMapExportConfig.class, new VSMapExportConfig(m_pool, category, bypass));
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
            return ExceptionHelper.getMultiState(this.getClass(), Messages.VSGenerator_31, new IStatus[] { StatusUtilities.createErrorStatus(e.getMessage()) });
        }
        return Status.OK_STATUS;
    }

    private void addMapResult(final IFile shapeFile, final String styleName) {
        final MapView mapView = m_menuPart.getMapView();
        final IMapPanel mapPanel = mapView.getMapPanel();
        final IKalypsoLayerModell mapModell = (IKalypsoLayerModell) mapPanel.getMapModell();
        final AddCascadingThemeCommand command = new AddCascadingThemeCommand(mapModell, Messages.VSGenerator_32, ADD_THEME_POSITION.eFront);
        command.addCommand(new SuitabilityThemeCommand(Messages.VSGenerator_33, shapeFile, styleName));
        final Property prLayerId = new StyledLayerType.Property();
        prLayerId.setName(SuitabilityHelper.LAYER_ID);
        prLayerId.setValue("VEGETATION_SUITABILITY_RESULT");
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
