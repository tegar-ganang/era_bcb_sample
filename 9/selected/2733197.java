package org.jminer.editor;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.CreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jminer.JMiner;
import org.jminer.JMinerNodeInfo;
import org.jminer.JMinerPlugin;
import org.jminer.model.Diagram;
import org.jminer.model.impl.DiagramImpl;
import org.jminer.parts.JMinerEditPartFactory;

public class JMinerEditor extends GraphicalEditorWithFlyoutPalette {

    /** Preference ID used to persist the palette location. */
    private static final String PALETTE_DOCK_LOCATION = "JMinerEditorPaletteFactory.Location";

    /** Preference ID used to persist the palette size. */
    private static final String PALETTE_SIZE = "JMinerEditorPaletteFactory.Size";

    /** Preference ID used to persist the flyout palette's state. */
    private static final String PALETTE_STATE = "JMinerEditorPaletteFactory.State";

    private static PaletteRoot palette;

    public JMinerEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    protected FlyoutPreferences getPalettePreferences() {
        return new FlyoutPreferences() {

            private IPreferenceStore getPreferenceStore() {
                return JMinerPlugin.getDefault().getPreferenceStore();
            }

            public int getDockLocation() {
                return getPreferenceStore().getInt(PALETTE_DOCK_LOCATION);
            }

            public int getPaletteState() {
                return getPreferenceStore().getInt(PALETTE_STATE);
            }

            public int getPaletteWidth() {
                return getPreferenceStore().getInt(PALETTE_SIZE);
            }

            public void setDockLocation(int location) {
                getPreferenceStore().setValue(PALETTE_DOCK_LOCATION, location);
            }

            public void setPaletteState(int state) {
                getPreferenceStore().setValue(PALETTE_STATE, state);
            }

            public void setPaletteWidth(int width) {
                getPreferenceStore().setValue(PALETTE_SIZE, width);
            }
        };
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
	 */
    protected PaletteRoot getPaletteRoot() {
        if (palette == null) {
            JMiner jminer;
            jminer = JMinerPlugin.getJMiner();
            palette = new PaletteRoot();
            PaletteGroup group = new PaletteGroup("Tools");
            palette.add(group);
            ToolEntry tool = new PanningSelectionToolEntry();
            group.add(tool);
            palette.setDefaultEntry(tool);
            tool = new MarqueeToolEntry();
            group.add(tool);
            List<JMinerNodeInfo> allNodes = jminer.getJMinerNodeInfo();
            for (JMinerNodeInfo node : allNodes) {
                createPaletteEntry(group, node);
            }
        }
        return palette;
    }

    private void createPaletteEntry(PaletteGroup group, JMinerNodeInfo node) {
        PaletteEntry entry = new CreationToolEntry(node.getPaletteLabel(), node.getPaletteDescription(), new SimpleFactory(node.getModelClass()), node.getSmallPaletteIcon(), node.getLargePaletteIcon());
        group.add(entry);
    }

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    protected void initializeGraphicalViewer() {
        EditPartViewer viewer = getGraphicalViewer();
        viewer.setContents(new DiagramImpl());
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new JMinerEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
    }
}
