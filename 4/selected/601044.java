package org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.worker;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.core.KalypsoCorePlugin;
import org.kalypso.gmlschema.IGMLSchema;
import org.kalypso.gmlschema.feature.IFeatureType;
import org.kalypso.gmlschema.property.IPropertyType;
import org.kalypso.gmlschema.property.relation.IRelationType;
import org.kalypso.nofdp.idss.schema.schemata.gml.GmlConstants;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.commands.AtomarAddFeatureCommand;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IDataStructureMember;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSetTypes;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMappingMember;
import org.kalypso.nofdpidss.core.common.utils.modules.IByPassImportData;
import org.kalypso.nofdpidss.core.common.utils.modules.IImportedGeoData;
import org.kalypso.nofdpidss.core.common.utils.modules.LanguageUtils;
import org.kalypso.nofdpidss.core.common.utils.modules.ShapeData;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.CATEGORY_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.IGeodataPageProvider.DATA_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.ShapeData.SHAPE_DATATYPE;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.core.view.map.MapTool;
import org.kalypso.nofdpidss.geodata.i18n.Messages;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.commmon.PageSelectGeodataFile;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.commmon.PerformFinishStuff;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.IHandleShapeMappingErrors;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.ShapeErrorsPerformFinish;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.ShapePages;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.VerifyMappedShapeData;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.IHandleShapeMappingErrors.PERFORM_ACTION;
import org.kalypso.nofdpidss.geodata.wizard.gds.add.shape.mapping.PageShapeMapping;
import org.kalypso.ogc.gml.FeatureUtils;
import org.kalypso.ogc.gml.selection.IFeatureSelectionManager;
import org.kalypso.ogc.gml.serialize.GmlSerializeException;
import org.kalypso.template.types.StyledLayerType;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree_impl.model.feature.FeatureHelper;

/**
 * @author Dirk Kuch
 */
public abstract class AbstractShapePerformFinishWorker implements ICoreRunnableWithProgress {

    public static PERFORM_ACTION inspectShapeFile(final Feature feature, final ShapeData data, final File shapeFile, final IHandleShapeMappingErrors handler) throws GmlSerializeException {
        if (feature == null) return PERFORM_ACTION.eError;
        final Object objAllowedContent = feature.getProperty(IMappingMember.QN_ALLOWED_CONTENTS);
        if (objAllowedContent == null || !(objAllowedContent instanceof Feature)) return PERFORM_ACTION.eOk;
        final Feature fAllowedContent = (Feature) objAllowedContent;
        final Set<String> myRules = new HashSet<String>();
        final Map<String, String> map = LanguageUtils.getAllowedStringMembers(fAllowedContent, NofdpCorePlugin.getDefault().getLanguage());
        final Collection<String> values = map.keySet();
        for (final String str : values) myRules.add(str.toUpperCase());
        final VerifyMappedShapeData v = new VerifyMappedShapeData(shapeFile);
        final Set<String> badEntries = v.verify(data, myRules);
        if (badEntries != null && badEntries.size() > 0) handler.execute(badEntries, data);
        return handler.getAction();
    }

    protected final ShapePages m_pages;

    protected final Feature m_selection;

    protected final IGeodataModel m_model;

    protected final IProject m_project;

    protected String m_geoDataSetName;

    private final IByPassImportData m_byPass;

    private final IImportedGeoData m_importData;

    public AbstractShapePerformFinishWorker(final IProject project, final IGeodataModel model, final ShapePages pages, final Feature selection) {
        this(project, model, pages, selection, null, null);
    }

    public AbstractShapePerformFinishWorker(final IProject project, final IGeodataModel model, final ShapePages pages, final Feature selection, final IByPassImportData byPass, final IImportedGeoData importData) {
        m_project = project;
        m_model = model;
        m_pages = pages;
        m_selection = selection;
        m_byPass = byPass;
        m_importData = importData;
    }

    protected void addAbstractStructureMember(final Feature feature) throws Exception {
        if (feature == null) throw new IllegalStateException();
        if (!DATA_TYPE.eShape.equals(m_pages.getDataType())) throw new IllegalStateException(Messages.AbstractShapePerformFinishWorker_0);
        final QName name = m_pages.getDataType().getQName();
        final SHAPE_DATATYPE shapeGeometry = m_pages.getShapeDetails().getGeometryType();
        final String geometry = shapeGeometry.getTypeMemberFromType();
        final IRelationType rel = (IRelationType) feature.getFeatureType().getProperty(IGeodataSet.QN_ABSTRACT_GEODATA_MEMBER);
        final IGMLSchema schema = feature.getFeatureType().getGMLSchema();
        final IFeatureType targetType = schema.getFeatureType(name);
        final IFeatureSelectionManager selectionManager = KalypsoCorePlugin.getDefault().getSelectionManager();
        final Map<IPropertyType, Object> values = new HashMap<IPropertyType, Object>();
        values.put(targetType.getProperty(IDataStructureMember.QN_SHAPE_GEOMETRY_TYPE), geometry);
        final AtomarAddFeatureCommand command = new AtomarAddFeatureCommand(m_model.getWorkspace(), targetType, feature, rel, -1, values, selectionManager);
        m_model.getWorkspace().postCommand(command);
    }

    private void addCategoryRulez(final Feature parent) throws Exception {
        if (parent == null) throw new IllegalStateException();
        final Object objDataMember = m_selection.getProperty(IGeodataSet.QN_ABSTRACT_GEODATA_MEMBER);
        if (objDataMember == null || !(objDataMember instanceof Feature)) return;
        final Feature fDataMember = (Feature) objDataMember;
        final IRelationType relation = (IRelationType) parent.getFeatureType().getProperty(IGeodataSet.QN_ABSTRACT_GEODATA_MEMBER);
        final Feature feature = FeatureHelper.cloneFeature(parent, relation, fDataMember);
        final PageShapeMapping mapping = (PageShapeMapping) m_pages.getPage(ShapePages.MAPPING_PAGE);
        final Set<Feature> mappedFeatures = mapping.getMappedFeatures();
        final List lstAttrMembers = (List) feature.getProperty(IDataStructureMember.QN_MAPPING_MEMBER);
        for (final Object object : lstAttrMembers) {
            if (!(object instanceof Feature)) continue;
            final Feature fAttr = (Feature) object;
            Object objIntAttr = fAttr.getProperty(IMappingMember.QN_INTERNAL_NAME);
            if (objIntAttr == null || !(objIntAttr instanceof Feature)) continue;
            Feature fIntAttr = (Feature) objIntAttr;
            final Object objIntAttrName = fIntAttr.getProperty(GmlConstants.QN_GEODATA_SET_INTERNAL_ATTR_NAME);
            if (!(objIntAttrName instanceof String)) continue;
            final String intAttrName = (String) objIntAttrName;
            for (final Object objMapped : mappedFeatures) {
                if (!(objMapped instanceof Feature)) continue;
                final Feature fMapped = (Feature) objMapped;
                objIntAttr = fMapped.getProperty(IMappingMember.QN_INTERNAL_NAME);
                if (!(objIntAttr instanceof Feature)) continue;
                fIntAttr = (Feature) objIntAttr;
                final Object objCompare = fIntAttr.getProperty(GmlConstants.QN_GEODATA_SET_INTERNAL_ATTR_NAME);
                if (intAttrName.equals(objCompare)) {
                    final StructuredSelection mappingSelection = mapping.getMappingSelection(fMapped);
                    final Object element = mappingSelection.getFirstElement();
                    if (element instanceof ShapeData) {
                        final ShapeData data = (ShapeData) element;
                        final String extName = data.getName();
                        FeatureUtils.updateProperty(m_model.getWorkspace(), fAttr, IMappingMember.QN_EXTERNAL_NAME, extName);
                    }
                }
            }
        }
    }

    private boolean createGeoDataSet(final QName qSetType, final boolean isNofdpCategory) throws Exception {
        final IGMLSchema schema = m_selection.getFeatureType().getGMLSchema();
        final IFeatureType targetType = schema.getFeatureType(qSetType);
        final IRelationType rel = (IRelationType) m_selection.getFeatureType().getProperty(IGeodataCategory.QN_SUBCATEGORY_MEMBER);
        final IFeatureSelectionManager selectionManager = KalypsoCorePlugin.getDefault().getSelectionManager();
        final Map<IPropertyType, Object> values = PerformFinishStuff.getCommonValues(m_pages, targetType, m_geoDataSetName);
        if (isNofdpCategory) values.put(targetType.getProperty(IGeodataSet.QN_IS_NOFDP_TYPE), true);
        final AtomarAddFeatureCommand command = new AtomarAddFeatureCommand(m_model.getWorkspace(), targetType, m_selection, rel, -1, values, selectionManager);
        m_model.getWorkspace().postCommand(command);
        final Feature feature = command.getNewFeature();
        if (m_importData != null) {
            m_importData.setImportedFeature(feature);
            m_importData.setImportedFeatureFileName(m_geoDataSetName);
        }
        final StyledLayerType layer;
        if (isNofdpCategory) {
            addCategoryRulez(feature);
            layer = MapTool.generateLayer(m_project, m_pages, command.getNewFeature(), m_geoDataSetName, DATA_TYPE.eShape, false);
        } else {
            addAbstractStructureMember(feature);
            layer = MapTool.generateLayer(m_project, m_pages, command.getNewFeature(), m_geoDataSetName, DATA_TYPE.eShape, true);
        }
        final IFile[] maps = PerformFinishStuff.getSelectedMaps(m_project, m_pages);
        MapTool.addMapLayer(maps, layer);
        return true;
    }

    protected boolean createNormalGeoDataSet() throws Exception {
        return createGeoDataSet(IGeodataSetTypes.QN_GEODATA_SET, false);
    }

    protected boolean createSpecificGeoDataSet(final CATEGORY_TYPE category) throws Exception {
        if (category == null) return false;
        final QName qSetType = category.getGeoDataSetName();
        if (qSetType == null) throw new IllegalStateException(Messages.AbstractShapePerformFinishWorker_1);
        return createGeoDataSet(qSetType, true);
    }

    protected IByPassImportData getByPass() {
        return m_byPass;
    }

    protected IFolder getDestinationFolder() throws CoreException {
        return BaseGeoUtils.getSubDirLocation(m_project, m_selection);
    }

    protected String getGeoDataSetName() {
        return m_geoDataSetName;
    }

    protected boolean importShapeFileToProject() throws IOException, CoreException {
        final WizardPage pFile = m_pages.getPage(ShapePages.COMMON_FILE_SELECTION);
        if (pFile == null || !(pFile instanceof PageSelectGeodataFile)) return false;
        final PageSelectGeodataFile geoFilePage = (PageSelectGeodataFile) pFile;
        final File file = geoFilePage.getSelectedFile();
        if (file == null) throw new IllegalStateException(Messages.AbstractShapePerformFinishWorker_2);
        final IFolder fDest = getDestinationFolder();
        if (fDest == null) throw new IllegalStateException(Messages.AbstractShapePerformFinishWorker_3);
        final String fileName = file.getName();
        if (!fileName.contains(".")) return false;
        final String[] parts = fileName.split("\\.");
        final File dirDest = fDest.getLocation().toFile();
        m_geoDataSetName = BaseGeoUtils.getFileName(dirDest, parts[0]);
        final File[] files = BaseGeoUtils.getFiles(file.getParentFile(), parts[0]);
        for (final File srcFile : files) {
            final String srcParts[] = srcFile.getName().split("\\.");
            if (srcParts.length != 2) continue;
            final File destFile = new File(dirDest, m_geoDataSetName + "." + srcParts[1]);
            FileUtils.copyFile(srcFile, destFile);
            WorkspaceSync.sync(m_project, IResource.DEPTH_INFINITE);
        }
        return true;
    }

    protected PERFORM_ACTION performShapeInspection() throws GmlSerializeException {
        final ShapeErrorsPerformFinish handler = new ShapeErrorsPerformFinish();
        final WizardPage pMapping = m_pages.getPage(ShapePages.MAPPING_PAGE);
        if (!(pMapping instanceof PageShapeMapping)) return PERFORM_ACTION.eImportAsGeoDataSet;
        final PageShapeMapping pageMapping = (PageShapeMapping) pMapping;
        final Set<Feature> mappedFeatures = pageMapping.getMappedFeatures();
        final PageSelectGeodataFile geoFilePage = (PageSelectGeodataFile) m_pages.getPage(ShapePages.COMMON_FILE_SELECTION);
        final File shapeFile = geoFilePage.getSelectedFile();
        boolean importAsGeoDataSet = false;
        for (final Feature feature : mappedFeatures) {
            final StructuredSelection selection = pageMapping.getMappingSelection(feature);
            final Object element = selection.getFirstElement();
            if (element instanceof ShapeData) {
                AbstractShapePerformFinishWorker.inspectShapeFile(feature, (ShapeData) element, shapeFile, handler);
                final PERFORM_ACTION action = handler.getAction();
                if (PERFORM_ACTION.eCancel.equals(action) || PERFORM_ACTION.eError.equals(action)) return action; else if (PERFORM_ACTION.eImportAsGeoDataSet.equals(action)) importAsGeoDataSet = true;
            }
        }
        if (importAsGeoDataSet) return PERFORM_ACTION.eImportAsGeoDataSet;
        return PERFORM_ACTION.eOk;
    }
}
