package org.kalypso.nofdpidss.analysis.navigation.isar;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.kalypso.commons.java.util.StringUtilities;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.nofdp.idss.schema.schemata.gml.GmlConstants;
import org.kalypso.nofdpidss.analysis.i18n.Messages;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMaps;
import org.kalypso.nofdpidss.core.common.utils.modules.IImportShapeConfig;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.geodata.store.AbstractMapExport;
import org.kalypso.nofdpidss.geodata.store.IDelegateMapExportConfig;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypsodeegree.model.feature.Feature;

/**
 * @author Dirk Kuch
 */
public class IsarAppMapExportConfig extends AbstractMapExport {

    private final IProject m_project;

    public IsarAppMapExportConfig(final IProject project, final Feature fGeoDataSet) {
        m_project = project;
        setImportedFeature(fGeoDataSet);
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#doSpecificStuff(java.lang.Object,
   *      java.lang.String)
   */
    public void doSpecificStuff(final Object object, final String key) {
        try {
            if (key.equals(IDelegateMapExportConfig.UPDATE_NEW_CREATED_LAYERS) && object instanceof List) updateNewCreatedLayers((List) object);
        } catch (final IOException e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
        }
    }

    private void duplicateStyle(final Style style) throws IOException {
        final IProject project = getProject();
        final IFolder mapFolder = project.getFolder(IMaps.FOLDER);
        final String href = style.getHref();
        final IFile fStyle = mapFolder.getFile(href);
        if (!fStyle.exists()) throw new IllegalStateException(Messages.IsarAppMapExportConfig_0 + fStyle.getLocation().toOSString());
        final IFolder styleFolder = (IFolder) fStyle.getParent();
        String styleName = getImportedFeatureFileName() + "_" + fStyle.getName();
        styleName = BaseGeoUtils.getFileName(styleFolder, styleName);
        final File fSrc = fStyle.getLocation().toFile();
        final File fDest = styleFolder.getFile(styleName).getLocation().toFile();
        FileUtils.copyFile(fSrc, fDest);
        final String[] parts = href.split("/");
        parts[parts.length - 1] = styleName;
        String newRef = "";
        for (final String string : parts) newRef += string + "/";
        newRef = StringUtilities.chomp(newRef);
        style.setHref(newRef);
        WorkspaceSync.sync(styleFolder, IResource.DEPTH_INFINITE);
        WorkspaceSync.sync(mapFolder, IResource.DEPTH_INFINITE);
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#getImportShapeConfig()
   */
    public IImportShapeConfig getImportShapeConfig() {
        throw new IllegalStateException();
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#getProject()
   */
    public IProject getProject() {
        return m_project;
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.delegates.IDelegateMapExportConfig#getStyleName()
   */
    public String getStyleName() {
        throw new IllegalStateException(Messages.IsarAppMapExportConfig_5);
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#showShapeShowExportWizard()
   */
    public boolean showExportWizard() {
        return false;
    }

    private void updateNewCreatedLayers(final List layers) throws IOException {
        for (final Object obj : layers) {
            if (obj == null || !(obj instanceof StyledLayerType)) continue;
            final StyledLayerType layer = (StyledLayerType) obj;
            final List<Style> styles = layer.getStyle();
            for (final Style style : styles) {
                final String styleName = style.getStyle();
                if (styleName == null) continue;
                if (GmlConstants.P_GEODATA_PHYSICAL_RIVER_STRUCTURE_MY_DEFICIT.equals(styleName)) duplicateStyle(style); else if (styleName.contains(GmlConstants.P_GEODATA_PHYSICAL_RIVER_STRUCTURE_MY_MEASURE)) duplicateStyle(style);
            }
        }
    }
}
