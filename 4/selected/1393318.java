package org.kalypso.nofdpidss.core.common.utils.modules;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMap;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMaps;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerCommon;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.core.i18n.Messages;
import org.kalypso.nofdpidss.core.view.map.MapTool;
import org.kalypso.nofdpidss.core.view.map.MapTool.ADD_MAP_LAYER_AT;
import org.kalypso.ogc.gml.FeatureUtils;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Property;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypsodeegree.model.feature.Feature;
import org.xml.sax.SAXException;

/**
 * @author Dirk Kuch
 */
public class IsarUtils {

    private static final String ISAR_APP_MAP_FILE = "isarApp.gmt";

    private static final String NOFDP_MEA5 = "NOFDP_MEA5";

    private static final String NOFDP_MEA4 = "NOFDP_MEA4";

    private static final String NOFDP_MEA3 = "NOFDP_MEA3";

    private static final String NOFDP_MEA2 = "NOFDP_MEA2";

    private static final String NOFDP_MEA1 = "NOFDP_MEA1";

    public static final String NOFDP_DEF = "NOFDP_DEF";

    public static final String LAYER_ID = "isarAppId";

    private static final String NOFDP_DEF_TEMPLATE_NAME = "template_struka_nofdp_def.sld";

    private static final String NOFDP_MEA_TEMPLATE_NAME = "template_struka_nofdp_mea";

    private static final String NOFDP_DEF_STYLE_NAME = "struka_nofdp_def.sld";

    private static final String NOFDP_MEA_STYLE_NAME = "struka_nofdp_mea";

    private static String getExternalName(final String name) {
        if ("struka_nofdp_def.sld".equals(name)) return IsarUtils.NOFDP_DEF; else if ("struka_nofdp_mea1.sld".equals(name)) return IsarUtils.NOFDP_MEA1; else if ("struka_nofdp_mea2.sld".equals(name)) return IsarUtils.NOFDP_MEA2; else if ("struka_nofdp_mea3.sld".equals(name)) return IsarUtils.NOFDP_MEA3; else if ("struka_nofdp_mea4.sld".equals(name)) return IsarUtils.NOFDP_MEA4; else if ("struka_nofdp_mea5.sld".equals(name)) return IsarUtils.NOFDP_MEA5;
        return null;
    }

    private static StyledLayerType getLayer(final IProject project, final Feature fGeoDataSet, final File sld) throws CoreException {
        final IFolder fGeoData = BaseGeoUtils.getSubDirLocation(project, fGeoDataSet);
        final IFolder fStyles = fGeoData.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER);
        final IFile file = fStyles.getFile(sld.getName());
        final String geoDataSetFileName = FeatureUtils.getProperty(fGeoDataSet, IGeodataSet.QN_FILE_NAME);
        final String pathStyles = "../" + file.getProjectRelativePath().toString();
        final String pathGeodDataSet = "../" + fGeoData.getProjectRelativePath().toString() + "/" + geoDataSetFileName;
        String extName = IsarUtils.getExternalName(sld.getName());
        if (extName == null) extName = geoDataSetFileName;
        final String coord = (String) fGeoDataSet.getProperty(IGeodataSet.QN_COORDINATE_SYSTEM);
        final Style style = new Style();
        style.setStyle(extName);
        style.setLinktype("sld");
        style.setType("simple");
        style.setHref(pathStyles);
        style.setActuate("onRequest");
        final StyledLayerType layer = new StyledLayerType();
        layer.setVisible(true);
        layer.setName(IsarUtils.getLegendName(sld.getName()));
        layer.setId(new Integer(sld.hashCode()).toString());
        layer.setFeaturePath("featureMember");
        layer.setType("simple");
        layer.setActuate("onRequest");
        layer.setHref(pathGeodDataSet + "#" + coord);
        layer.setLinktype("shape");
        layer.getStyle().add(style);
        final List<Property> properties = layer.getProperty();
        final StyledLayerType.Property property = new StyledLayerType.Property();
        property.setName(LAYER_ID);
        property.setValue(extName);
        properties.add(property);
        return layer;
    }

    private static String getLegendName(final String name) {
        if ("struka_nofdp_def.sld".equals(name)) return Messages.IsarUtils_29; else if ("struka_nofdp_mea1.sld".equals(name)) return Messages.IsarUtils_31; else if ("struka_nofdp_mea2.sld".equals(name)) return Messages.IsarUtils_33; else if ("struka_nofdp_mea3.sld".equals(name)) return Messages.IsarUtils_35; else if ("struka_nofdp_mea4.sld".equals(name)) return Messages.IsarUtils_37; else if ("struka_nofdp_mea5.sld".equals(name)) return Messages.IsarUtils_39;
        return Messages.IsarUtils_40;
    }

    public static File getMapLoacation(final IProject project, final Feature isarMap) {
        return IsarUtils.getProjectMapLoacation(project, isarMap).getLocation().toFile();
    }

    private static File getPhysicalRiverQualityRiverStyle(final IProject project, final Feature geodataSet) throws CoreException {
        final IFolder shapeFolder = BaseGeoUtils.getSubDirLocation(project, geodataSet);
        final IFolder styleFolder = shapeFolder.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER);
        final String dataSet = FeatureUtils.getProperty(geodataSet, IGeodataSet.QN_FILE_NAME);
        final IFile file = styleFolder.getFile(dataSet + ".sld");
        return file.getLocation().toFile();
    }

    public static IFile getProjectMapLoacation(final IProject project, final Feature isarMap) {
        if (isarMap == null) throw new IllegalStateException();
        final Object objFileName = isarMap.getProperty(IMap.QN_FILE_NAME);
        if (objFileName == null || !(objFileName instanceof String) || "".equals(objFileName)) throw new IllegalStateException();
        final IFolder folder = project.getFolder(IMaps.FOLDER);
        final String fileName = BaseGeoUtils.getFileName(folder, (String) objFileName);
        return folder.getFile(fileName);
    }

    private static String getStyleMeaName(final Integer no) {
        if (no < 1 || no > 5) throw new IllegalStateException(Messages.IsarUtils_43 + no.toString());
        return IsarUtils.NOFDP_MEA_STYLE_NAME + no + ".sld";
    }

    private static IStyleReplacements getStyleReplacer(final IProject project, final Feature fDataSet, final String workingSld, final String templateSld) throws CoreException, IOException {
        final IFile iTemplate = BaseGeoUtils.getStyleTemplateForCategory(templateSld);
        final File fTemplate = new File(iTemplate.getLocation().toOSString());
        final IFolder subDir = BaseGeoUtils.getSubDirLocation(project, fDataSet);
        final IFolder fStyle = subDir.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER);
        if (!fStyle.exists()) fStyle.create(true, false, new NullProgressMonitor());
        final IFile iDest = fStyle.getFile(workingSld);
        final File fDest = new File(iDest.getLocation().toOSString());
        FileUtils.copyFile(fTemplate, fDest);
        return new StyleReplacerCommon(fDataSet, fDest);
    }

    private static String getTemplateStyleMeaName(final Integer no) {
        if (no < 1 || no > 5) throw new IllegalStateException(Messages.IsarUtils_45 + no.toString());
        return IsarUtils.NOFDP_MEA_TEMPLATE_NAME + no + ".sld";
    }

    public static IFile updateIsarMap(final IProject project, final Feature fGeodataSet) throws JAXBException, IOException, CoreException, SAXException, ParserConfigurationException {
        final IFolder mapFolder = project.getFolder(IMaps.FOLDER);
        final IFile mapTemplate = mapFolder.getFile(IsarUtils.ISAR_APP_MAP_FILE);
        final File fTemplate = mapTemplate.getLocation().toFile();
        final IFolder tmpFolder = project.getFolder(NofdpIDSSConstants.NOFDP_TMP_FOLDER);
        if (!tmpFolder.exists()) tmpFolder.create(true, true, new NullProgressMonitor());
        WorkspaceSync.sync(tmpFolder, IResource.DEPTH_INFINITE);
        final IFile map = tmpFolder.getFile(IsarUtils.ISAR_APP_MAP_FILE);
        final File fMap = map.getLocation().toFile();
        FileUtils.copyFile(fTemplate, fMap);
        WorkspaceSync.sync(tmpFolder, IResource.DEPTH_INFINITE);
        final List<File> slds = new LinkedList<File>();
        final File riverSld = IsarUtils.getPhysicalRiverQualityRiverStyle(project, fGeodataSet);
        slds.add(riverSld);
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
        for (int i = 5; i > 0; i--) {
            final IStyleReplacements srm = IsarUtils.getStyleReplacer(project, fGeodataSet, IsarUtils.getStyleMeaName(i), IsarUtils.getTemplateStyleMeaName(i));
            if (!srm.replace()) throw new IllegalStateException(Messages.IsarUtils_48);
            slds.add(srm.getSld());
        }
        final IStyleReplacements srd = IsarUtils.getStyleReplacer(project, fGeodataSet, IsarUtils.NOFDP_DEF_STYLE_NAME, IsarUtils.NOFDP_DEF_TEMPLATE_NAME);
        if (!srd.replace()) throw new IllegalStateException(Messages.IsarUtils_47);
        slds.add(srd.getSld());
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
        for (final File sld : slds) {
            final StyledLayerType layer = IsarUtils.getLayer(project, fGeodataSet, sld);
            WorkspaceSync.sync(map, IResource.DEPTH_INFINITE);
            MapTool.addMapLayer(map, layer, ADD_MAP_LAYER_AT.eFront);
        }
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
        return map;
    }
}
