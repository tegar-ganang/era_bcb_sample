package org.kalypso.nofdpidss.analysis.conflict.worker;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.nofdp.idss.schema.schemata.gml.GmlConstants;
import org.kalypso.nofdpidss.analysis.i18n.Messages;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IConflict;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool.POOL_TYPE;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.shape.ShapeUtils;
import org.kalypso.nofdpidss.core.common.utils.modules.CDCombinations;
import org.kalypso.nofdpidss.core.common.utils.modules.CDUtils;
import org.kalypso.nofdpidss.core.common.utils.style.StyleReplacerConflictDetection;
import org.kalypso.nofdpidss.core.common.utils.various.GeneralConfigGmlUtil;
import org.kalypso.ogc.gml.serialize.ShapeSerializer;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Property;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypsodeegree.KalypsoDeegreePlugin;
import org.kalypsodeegree.model.feature.Feature;
import org.kalypsodeegree_impl.io.shpapi.ShapeFile;
import org.kalypsodeegree_impl.io.shpapi.dataprovider.StandardShapeDataProvider;

public class ConflictDetectionShapeFileWorker implements ICoreRunnableWithProgress, IConflictDetectionShapeFileProvider {

    public static final String LAYER_VALUE = "CONFLICT";

    private static final String LAYER_ID = "LAYER_ID";

    private final CDCombinations m_combinations;

    private IFile m_destination;

    private final IConflict m_conflict;

    private StyledLayerType m_generateStyle;

    public ConflictDetectionShapeFileWorker(IConflict conflict, final CDCombinations combinations) {
        m_conflict = conflict;
        m_combinations = combinations;
    }

    public IStatus execute(IProgressMonitor monitor) throws CoreException {
        try {
            m_combinations.updateConflictFeatures();
            final Feature[] conflicts = m_combinations.getConflicts();
            final QName[] properties = ShapeUtils.getShapeProperties(conflicts[0], new QName[] { GmlConstants.QN_GEODATA_CONFLICT_DETECTION_CONFLICT });
            final Map<String, String> mapping = new LinkedHashMap<String, String>();
            for (final QName name : properties) mapping.put(name.getLocalPart(), name.getLocalPart());
            mapping.put(LAYER_VALUE, GmlConstants.QN_GEODATA_CONFLICT_DETECTION_CONFLICT.getLocalPart());
            final IProject project = NofdpCorePlugin.getProjectManager().getActiveProject();
            final IFolder folder = project.getFolder(NofdpIDSSConstants.NOFDP_TMP_FOLDER);
            if (!folder.exists()) folder.create(true, true, new NullProgressMonitor());
            m_destination = folder.getFile("conflict.shp");
            final String location = m_destination.getLocation().toOSString();
            final int index = location.indexOf(".shp");
            final String subString = location.substring(0, index);
            final StandardShapeDataProvider provider = new StandardShapeDataProvider(conflicts);
            ShapeSerializer.serializeFeatures(conflicts, mapping, ShapeFile.GEOM, subString, provider);
            WorkspaceSync.sync(folder, IResource.DEPTH_INFINITE);
            m_generateStyle = generateStyle();
            return Status.OK_STATUS;
        } catch (Exception e) {
            return StatusUtilities.createErrorStatus(Messages.ConflictDetectionShapeFileWorker_1);
        }
    }

    private StyledLayerType generateStyle() throws IOException {
        final MyBasePool pool = NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eGeodata);
        final IGeodataCategory category = GeneralConfigGmlUtil.getConflictAreaCategory((PoolGeoData) pool);
        final IFile template = CDUtils.getStandardTemplateOfCategory(category);
        if (!template.exists()) throw new IllegalStateException(Messages.CDMapGenerator_5 + template.getLocation().toOSString());
        IFolder tmpFolder = (IFolder) m_destination.getParent();
        final IFile workingSld = tmpFolder.getFile("conflict.sld");
        FileUtils.copyFile(template.getLocation().toFile(), workingSld.getLocation().toFile());
        WorkspaceSync.sync(workingSld, IResource.DEPTH_INFINITE);
        if (!workingSld.exists()) throw new IllegalStateException(Messages.CDMapGenerator_8 + workingSld.getLocation().toOSString());
        final StyleReplacerConflictDetection replacer = new StyleReplacerConflictDetection(m_conflict, category, m_destination.getLocation().toFile(), workingSld.getLocation().toFile());
        boolean replaced = replacer.replace();
        if (!replaced) throw new IllegalStateException(Messages.CDMapGenerator_9);
        final Style style = new Style();
        style.setStyle("conflict.shp");
        style.setLinktype("sld");
        style.setType("simple");
        style.setHref("./../.tmp/conflict.sld");
        style.setActuate("onRequest");
        final StyledLayerType layer = new StyledLayerType();
        layer.setVisible(true);
        layer.setName(Messages.CDMapGenerator_2);
        layer.setId("1");
        layer.setFeaturePath("featureMember");
        layer.setType("simple");
        layer.setActuate("onRequest");
        layer.setHref("./../.tmp/conflict#" + KalypsoDeegreePlugin.getDefault().getCoordinateSystem());
        layer.setLinktype("shape");
        layer.getStyle().add(style);
        final List<Property> properties = layer.getProperty();
        final Property property = new StyledLayerType.Property();
        property.setName(LAYER_ID);
        property.setValue(LAYER_VALUE);
        properties.add(property);
        return layer;
    }

    public IFile getShapeFile() {
        return m_destination;
    }

    public StyledLayerType getLayerType() {
        return m_generateStyle;
    }
}
