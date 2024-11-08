package be.lassi.ui.sheet.cells;

import java.awt.Component;
import javax.swing.JTable;
import be.lassi.cues.CueChannelLevel;

/**
 *
 *
 *
 */
public class CueChannelLevelEditor extends AbstractLevelEditor {

    public CueChannelLevelEditor() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        CueChannelLevel cueLevel = (CueChannelLevel) value;
        int intValue = cueLevel.getChannelIntValue();
        level(cueLevel.isActive(), intValue);
        return editorComponent;
    }
}
