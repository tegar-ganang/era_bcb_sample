package org.kalypso.nofdpidss.analysis.conflict.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.nofdpidss.analysis.i18n.Messages;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMaps;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool.POOL_TYPE;
import org.kalypso.nofdpidss.core.common.utils.modules.IByPassImportData;
import org.kalypso.nofdpidss.core.common.utils.modules.IImportShapeConfig;
import org.kalypso.nofdpidss.geodata.store.AbstractMapExport;
import org.kalypso.nofdpidss.geodata.store.IDelegateMapExportConfig;
import org.kalypso.ogc.gml.map.IMapPanel;
import org.kalypso.template.types.StyledLayerType;
import org.kalypso.template.types.StyledLayerType.Style;
import org.kalypsodeegree.model.feature.Feature;

/**
 * @author Dirk Kuch
 */
public class CDDelegateMapExportConfig extends AbstractMapExport {

    protected final IGeodataCategory m_category;

    protected final IByPassImportData m_bypass;

    public CDDelegateMapExportConfig(IGeodataCategory category, final IByPassImportData bypass, final IMapPanel mapPanel) {
        m_category = category;
        m_bypass = bypass;
        setMapPanel(mapPanel);
    }

    private void doFooStuff(final Style style) throws IOException {
        style.setStyle("conflict.shp");
        final IProject project = getProject();
        final IFolder mapFolder = project.getFolder(IMaps.FOLDER);
        final String href = style.getHref();
        final IFile iDest = mapFolder.getFile(href);
        final IFolder geoDataFolder = (IFolder) iDest.getParent().getParent();
        final String nameStyle = iDest.getName();
        final IFile iSrc = geoDataFolder.getFile(nameStyle);
        if (!iSrc.exists()) throw new IllegalStateException(Messages.CDDelegateMapExportConfig_1 + iSrc.getLocation().toOSString());
        final File fSrc = iSrc.getLocation().toFile();
        final File fDest = iDest.getLocation().toFile();
        FileUtils.copyFile(fSrc, fDest);
        WorkspaceSync.sync(geoDataFolder, IResource.DEPTH_INFINITE);
        WorkspaceSync.sync(mapFolder, IResource.DEPTH_INFINITE);
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

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#getImportShapeConfig()
   */
    public IImportShapeConfig getImportShapeConfig() {
        return new IImportShapeConfig() {

            public IByPassImportData getByPass() {
                return m_bypass;
            }

            public Feature getCategory() {
                return m_category;
            }

            public PoolGeoData getPool() {
                return (PoolGeoData) NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eGeodata);
            }

            public IProject getProject() {
                return NofdpCorePlugin.getProjectManager().getActiveProject();
            }
        };
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#getProject()
   */
    public IProject getProject() {
        return NofdpCorePlugin.getProjectManager().getActiveProject();
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.delegates.IDelegateMapExportConfig#getStyleName()
   */
    public String getStyleName() {
        return getImportedFeatureFileName();
    }

    /**
   * @see org.kalypso.nofdpidss.ui.application.Delegates.IDelegateMapExportConfig#showExportWizard()
   */
    public boolean showExportWizard() {
        if (getImportedFeature() == null) return true;
        return false;
    }

    private void updateNewCreatedLayers(final List layers) throws IOException {
        for (final Object obj : layers) {
            if (obj == null || !(obj instanceof StyledLayerType)) continue;
            final StyledLayerType layer = (StyledLayerType) obj;
            final List<Style> styles = layer.getStyle();
            for (final Style style : styles) {
                final String styleName = style.getStyle();
                if ("conflict".equals(styleName)) doFooStuff(style);
            }
        }
    }
}
