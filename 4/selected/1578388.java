package org.kalypso.nofdpidss.core.view.map;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.core.KalypsoCorePlugin;
import org.kalypso.core.jaxb.TemplateUtilitites;
import org.kalypso.nofdp.idss.schema.schemata.gml.GmlConstants;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IDataStructureMember;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategories;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMap;
import org.kalypso.nofdpidss.core.base.gml.model.project.info.IProjectInfoModel;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolProjectInfo;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.utils.modules.DeleteFileData;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider;
import org.kalypso.nofdpidss.core.common.utils.modules.IStyleReplacements;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.DATA_TYPE;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerCommon;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.core.i18n.Messages;
import org.kalypso.ogc.gml.FeatureUtils;
import org.kalypso.ogc.gml.GisTemplateHelper;
import org.kalypso.ogc.gml.GisTemplateMapModell;
import org.kalypso.ogc.gml.IKalypsoTheme;
import org.kalypso.ogc.gml.map.IMapPanel;
import org.kalypso.ogc.gml.selection.IFeatureSelectionManager;
import org.kalypso.template.gismapview.CascadingLayer;
import org.kalypso.template.gismapview.Gismapview;
import org.kalypso.template.gismapview.ObjectFactory;
import org.kalypso.template.gismapview.Gismapview.Layers;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Property;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree.model.geometry.GM_Envelope;
import org.xml.sax.SAXException;

/**
 * @author Dirk Kuch
 */
public class MapTool {

    public enum ADD_MAP_LAYER_AT {

        eBack, eFront
    }

    /**
   * TODO: map is saved each time a layer is added; is that intended?<br/>
   * add a theme layer to an .gmt map
   */
    public static void addMapLayer(final IFile map, final StyledLayerType layer, final ADD_MAP_LAYER_AT pos) throws JAXBException, CoreException, SAXException, ParserConfigurationException, IOException {
        if (map == null || layer == null) throw new IllegalStateException();
        final Gismapview mapview = GisTemplateHelper.loadGisMapView(map);
        final Layers layers = mapview.getLayers();
        final JAXBElement<? extends StyledLayerType> layerType;
        if (layer instanceof CascadingLayer) layerType = TemplateUtilitites.OF_GISMAPVIEW.createCascadingLayer((CascadingLayer) layer); else layerType = TemplateUtilitites.OF_GISMAPVIEW.createLayer(layer);
        if (ADD_MAP_LAYER_AT.eFront.equals(pos)) layers.getLayer().add(0, layerType); else if (ADD_MAP_LAYER_AT.eBack.equals(pos)) layers.getLayer().add(layerType); else throw new IllegalStateException();
        GisTemplateHelper.saveGisMapView(mapview, map, "UTF-8");
    }

    public static void addMapLayer(final IFile[] maps, final StyledLayerType layer) throws JAXBException, CoreException, SAXException, ParserConfigurationException, IOException {
        for (final IFile map : maps) {
            MapTool.addMapLayer(map, layer, ADD_MAP_LAYER_AT.eBack);
            WorkspaceSync.sync(map, IResource.DEPTH_INFINITE);
        }
    }

    private static void cleanCascadingLayer(final CascadingLayer cascading, final DeleteFileData data) {
        final List<JAXBElement<? extends StyledLayerType>> layers = cascading.getLayer();
        final Object[] array = layers.toArray();
        for (final Object object : array) {
            final JAXBElement<? extends StyledLayerType> layer = (JAXBElement<? extends StyledLayerType>) object;
            final StyledLayerType slt = layer.getValue();
            if (slt instanceof CascadingLayer) {
                final CascadingLayer c = (CascadingLayer) slt;
                cleanCascadingLayer(c, data);
            } else {
                if (removeLayer(slt, data)) layers.remove(layer);
            }
        }
    }

    /**
   * creation of an new empty map gmt
   */
    public static void createMapFile(final IFile mapFile) throws JAXBException, IOException, CoreException {
        final ObjectFactory factory = new ObjectFactory();
        final Gismapview gismapview = factory.createGismapview();
        gismapview.setLayers(new Layers());
        final StyledLayerType scaleLayer = getScaleLayer();
        gismapview.getLayers().getLayer().add(TemplateUtilitites.OF_GISMAPVIEW.createLayer(scaleLayer));
        GisTemplateHelper.saveGisMapView(gismapview, mapFile, "UTF-8");
    }

    public static StyledLayerType getScaleLayer() {
        final StyledLayerType slt = new StyledLayerType();
        slt.setVisible(true);
        slt.setName("Scale");
        slt.setId("SCALE");
        slt.setLinktype("scale");
        final List<Property> properties = slt.getProperty();
        final Property property = new StyledLayerType.Property();
        property.setName("deleteable");
        property.setValue("false");
        properties.add(property);
        return slt;
    }

    /**
   * common style generator, template file is needed (replaces placeholders in those template files)
   */
    public static Style generateCommonStyleLayerDescriptor(final IProject project, final Feature fCategory, final Feature fData, final String dataSetName, final boolean defaultStyle) throws CoreException, IOException {
        if (project == null || fCategory == null || fData == null) throw new IllegalStateException();
        boolean bDefaultStyle = false;
        File templateSld = null;
        File workingSld = null;
        final Object objDataDescr = fCategory.getProperty(IGeodataSet.QN_ABSTRACT_GEODATA_MEMBER);
        if (objDataDescr instanceof Feature) {
            final Feature fDataDescr = (Feature) objDataDescr;
            final Object objSld = fDataDescr.getProperty(IDataStructureMember.QN_MAP_SYMBOLIZATION);
            if (!(objSld instanceof String) || ((String) objSld).equals("")) bDefaultStyle = true; else {
                final IFolder folder = NofdpCorePlugin.getProjectManager().getBaseProject().getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GLOBAL_STYLES_FOLDER);
                final IFile file = folder.getFile((String) objSld);
                if (file.exists()) templateSld = file.getLocation().toFile(); else bDefaultStyle = true;
            }
        } else bDefaultStyle = true;
        if (defaultStyle) bDefaultStyle = true;
        if (!bDefaultStyle) {
            final IFolder folderCategory = BaseGeoUtils.getSubDirLocation(project, fCategory);
            final IFolder folderStyles = folderCategory.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER);
            if (!folderStyles.exists()) folderStyles.create(true, true, new NullProgressMonitor());
            workingSld = new File(folderStyles.getLocation().toFile(), dataSetName + ".sld");
            FileUtils.copyFile(templateSld, workingSld);
            final IStyleReplacements replacer = new StyleReplacerCommon(fData, workingSld);
            final boolean bReplaced = replacer.replace();
            if (bReplaced) {
                final IFolder dir = BaseGeoUtils.getSubDirLocation(project, fCategory);
                final Style style = MapTool.generateStyle(fData, dir);
                return style;
            }
        }
        return null;
    }

    /**
   * @param styleName
   *          set styleName = null if it is an user data set - no style will be added -> default symbolization
   */
    public static StyledLayerType generateLayer(final IProject project, final Feature fGeoDataSet, final String styleName) throws CoreException {
        final IFolder fGeoData = BaseGeoUtils.getSubDirLocation(project, fGeoDataSet);
        final IFolder fStyles = fGeoData.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER);
        final String geoDataSetFileName = FeatureUtils.getProperty(fGeoDataSet, IGeodataSet.QN_FILE_NAME);
        final IFile file = fStyles.getFile(geoDataSetFileName + ".sld");
        final String pathStyles = "../" + file.getProjectRelativePath().toString();
        final String pathGeodDataSet = "../" + fGeoData.getProjectRelativePath().toString() + "/" + geoDataSetFileName;
        final String coord = (String) fGeoDataSet.getProperty(IGeodataSet.QN_COORDINATE_SYSTEM);
        final String legend = FeatureUtils.getFeatureName(GmlConstants.NS_GEODATA, fGeoDataSet);
        final Style style = new Style();
        style.setStyle(styleName);
        style.setLinktype("sld");
        style.setType("simple");
        style.setHref(pathStyles);
        style.setActuate("onRequest");
        final StyledLayerType layer = new StyledLayerType();
        layer.setVisible(true);
        layer.setName(legend);
        layer.setId(new Integer(fGeoDataSet.hashCode()).toString());
        layer.setFeaturePath("featureMember");
        layer.setType("simple");
        layer.setActuate("onRequest");
        layer.setHref(pathGeodDataSet + "#" + coord);
        layer.setLinktype("shape");
        if (styleName != null) layer.getStyle().add(style);
        return layer;
    }

    public static StyledLayerType generateLayer(final IProject project, final IGeodataPageProvider pages, final Feature fData, final String dataSetName, final DATA_TYPE dataType, final boolean defaultStyle) throws IOException, CoreException {
        final Feature fCategory = fData.getParent();
        final QName qName = fCategory.getFeatureType().getSubstitutionGroupFT().getQName();
        if (!(IGeodataCategories.QN_ABSTRACT_CATEGORY.equals(qName) || IGeodataCategories.QN_CATEGORY_TYPE.equals(qName) || IGeodataCategories.QN_ABSTRACT_SUB_CATEGORY.equals(qName) || IGeodataCategories.QN_SUB_CATEGORY.equals(qName))) throw new IllegalStateException();
        final StyledLayerType layer;
        if (DATA_TYPE.eImage.equals(dataType)) {
            final Style style = MapTool.generateCommonStyleLayerDescriptor(project, fCategory, fData, dataSetName, defaultStyle);
            layer = MapTool.getMapTag(project, fCategory, fData, style, pages.getUserSelectedDataType());
        } else if (DATA_TYPE.eShape.equals(dataType)) {
            final Style style = MapTool.generateCommonStyleLayerDescriptor(project, fCategory, fData, dataSetName, defaultStyle);
            layer = MapTool.getMapTag(project, fCategory, fData, style, pages.getUserSelectedDataType());
        } else throw new NotImplementedException();
        return layer;
    }

    public static Style generateStyle(final Feature fData, final IFolder dir) {
        final String name = FeatureUtils.getProperty(fData, IGeodataSet.QN_FILE_NAME);
        final String geoDataSetName = FeatureUtils.chopGeoDataSetName(name);
        if (geoDataSetName == null || "".equals(geoDataSetName)) throw new IllegalStateException();
        final String sld = "../" + dir.getProjectRelativePath().toString() + "/.styles/" + geoDataSetName + ".sld";
        final Style style = new Style();
        style.setStyle(geoDataSetName);
        style.setLinktype("sld");
        style.setType("simple");
        style.setHref(sld);
        style.setActuate("onRequest");
        return style;
    }

    private static String getExtension(final String string) {
        if (string.contains("#")) {
            final String[] arr = string.split("\\#");
            return arr[0];
        }
        return string;
    }

    public static StyledLayerType getMapTag(final IProject project, final Feature category, final Feature data, final Style slt, final DATA_TYPE dataType) throws CoreException {
        if (project == null || category == null || data == null) throw new IllegalStateException();
        final IFolder dir = BaseGeoUtils.getSubDirLocation(project, category);
        final Object objDataSetName = data.getProperty(IGeodataSet.QN_FILE_NAME);
        if (!(objDataSetName instanceof String) || ((String) objDataSetName).equals("")) throw new IllegalStateException();
        final String labelName = FeatureUtils.getFeatureName(GmlConstants.NS_GEODATA, data);
        if (labelName == null) throw new IllegalStateException();
        final String dataset = dir.getProjectRelativePath().toString() + "/" + (String) objDataSetName;
        final Object objCoord = data.getProperty(IGeodataSet.QN_COORDINATE_SYSTEM);
        if (!(objCoord instanceof String) || "".equals(objCoord)) throw new IllegalStateException();
        final StyledLayerType type = new StyledLayerType();
        type.setVisible(true);
        type.setName(labelName);
        type.setId(data.getId());
        type.setFeaturePath("featureMember");
        type.setType("simple");
        type.setActuate("onRequest");
        switch(dataType) {
            case eImage:
                type.setLinktype(MapTool.getExtension("gmlpic"));
                type.setHref("../" + dataset);
                break;
            case eRaster:
                type.setLinktype("gml");
                type.setHref("../" + dataset);
                type.setFeaturePath("coverageMember");
                break;
            case eShape:
                type.setLinktype("shape");
                type.setHref("../" + dataset + "#" + (String) objCoord);
                break;
            default:
                throw new IllegalStateException();
        }
        if (slt != null) type.getStyle().add(slt);
        return type;
    }

    public static Feature getProjectBoundary(final PoolProjectInfo pool) {
        final Feature root = pool.getWorkspace().getRootFeature();
        final Feature metaInfo = (Feature) root.getProperty(IProjectInfoModel.QN_META_INFO_MEMBER);
        return metaInfo;
    }

    public static void removeAllLayers(final IFile map) throws JAXBException, CoreException, SAXException, ParserConfigurationException, IOException {
        final Gismapview gismapview = GisTemplateHelper.loadGisMapView(map);
        if (gismapview == null) throw new IllegalStateException();
        final Layers layers = gismapview.getLayers();
        layers.getLayer().clear();
        GisTemplateHelper.saveGisMapView(gismapview, map, "UTF-8");
    }

    private static boolean removeLayer(final StyledLayerType slt, final DeleteFileData data) {
        final String href = slt.getHref();
        if (href == null) return false;
        final String[] folderParts = data.folder.getProjectRelativePath().toString().toLowerCase().split("/");
        final String[] layerParts = href.toLowerCase().split("/");
        if (folderParts.length < 1 || layerParts.length < 2) return false;
        if (!folderParts[folderParts.length - 1].equals(layerParts[layerParts.length - 2])) return false;
        final String[] resourceParts = layerParts[layerParts.length - 1].split("#");
        if (resourceParts[0].toLowerCase().equals(data.fileName.toLowerCase())) return true;
        return false;
    }

    /**
   * remove geodata set from map
   * 
   * @param data
   *          gds to remove
   */
    public static void removeMapLayers(final IMap map, final DeleteFileData data) throws CoreException {
        final Gismapview gis = map.getGismapView();
        final List<JAXBElement<? extends StyledLayerType>> layers = gis.getLayers().getLayer();
        final Object[] myLayers = layers.toArray();
        for (final Object object : myLayers) {
            final JAXBElement<? extends StyledLayerType> layer = (JAXBElement<? extends StyledLayerType>) object;
            final StyledLayerType slt = layer.getValue();
            if (slt instanceof CascadingLayer) {
                final CascadingLayer cascading = (CascadingLayer) slt;
                cleanCascadingLayer(cascading, data);
            } else {
                if (removeLayer(slt, data)) layers.remove(layer);
            }
        }
        try {
            GisTemplateHelper.saveGisMapView(gis, map.getIFile(), "UTF-8");
        } catch (final Exception e) {
            throw ExceptionHelper.getCoreException(IStatus.ERROR, MapTool.class, e.getMessage());
        }
    }

    /**
   * remove (cascading)map layers from name
   * 
   * @param s
   *          names of map layers to remove
   */
    public static void removeMapLayers(final IMap map, final String[] s) throws JAXBException, CoreException, SAXException, ParserConfigurationException, IOException {
        final Gismapview gismapview = GisTemplateHelper.loadGisMapView(map.getIFile());
        if (gismapview == null) throw new IllegalStateException(Messages.MapTool_0);
        final Layers layers = gismapview.getLayers();
        if (layers == null) throw new IllegalStateException(Messages.MapTool_1);
        final Layers cleandLayers = new Layers();
        final List<JAXBElement<? extends StyledLayerType>> lCleaned = cleandLayers.getLayer();
        final List<JAXBElement<? extends StyledLayerType>> lStylesLayerType = layers.getLayer();
        for (final JAXBElement<? extends StyledLayerType> layer : lStylesLayerType) {
            if (ArrayUtils.contains(s, layer.getValue().getName())) continue;
            lCleaned.add(layer);
        }
        gismapview.setLayers(cleandLayers);
        GisTemplateHelper.saveGisMapView(gismapview, map.getIFile(), "UTF-8");
    }

    public static void saveMap(final IMapPanel panel, final IFile newMap) throws MalformedURLException, CoreException {
        final URL context = newMap.getLocationURI().toURL();
        final IProject project = newMap.getProject();
        final IFeatureSelectionManager sm = KalypsoCorePlugin.getDefault().getSelectionManager();
        final GisTemplateMapModell modell = new GisTemplateMapModell(context, panel.getMapModell().getCoordinatesSystem(), project, sm);
        final IKalypsoTheme[] themes = panel.getMapModell().getAllThemes();
        for (final IKalypsoTheme theme : themes) modell.addTheme(theme);
        final GM_Envelope boundingBox = panel.getBoundingBox();
        final String srsName = panel.getMapModell().getCoordinatesSystem();
        modell.saveGismapTemplate(boundingBox, srsName, new NullProgressMonitor(), newMap);
    }
}
