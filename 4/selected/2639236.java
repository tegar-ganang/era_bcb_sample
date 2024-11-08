package be.lassi.ui.library;

import javax.swing.table.AbstractTableModel;
import be.lassi.base.Holder;
import be.lassi.base.Listener;
import be.lassi.domain.AttributeDefinition;
import be.lassi.domain.FixtureDefinition;
import be.lassi.domain.PresetDefinition;

public class AttributesTableModel extends AbstractTableModel {

    public static final int COLUMN_NUMBER = 0;

    public static final int COLUMN_NAME = 1;

    public static final int COLUMN_CHANNELS = 2;

    public static final int COLUMN_PRESET = 3;

    public static final int COLUMN_HAS_VALUES = 4;

    private final Holder<FixtureDefinition> fixtureHolder;

    private final Holder<PresetDefinition> presetHolder;

    public AttributesTableModel(final Holder<FixtureDefinition> fixtureHolder, final Holder<PresetDefinition> presetHolder) {
        this.fixtureHolder = fixtureHolder;
        this.presetHolder = presetHolder;
        presetHolder.add(new PresetListener());
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int col) {
        String name;
        switch(col) {
            case COLUMN_NUMBER:
                name = "Nr";
                break;
            case COLUMN_NAME:
                name = "Name";
                break;
            case COLUMN_CHANNELS:
                name = "Channel(s)";
                break;
            case COLUMN_PRESET:
                name = "Preset";
                break;
            case COLUMN_HAS_VALUES:
                name = "Values";
                break;
            default:
                name = "?";
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        int rowCount = 0;
        FixtureDefinition fixtureDefinition = fixtureHolder.getValue();
        if (fixtureDefinition != null) {
            rowCount = fixtureDefinition.getAttributeCount();
        }
        return rowCount;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        AttributeDefinition attribute = fixtureHolder.getValue().getAttribute(row);
        String hasValues = attribute.getValueCount() > 0 ? "*" : "";
        String presetValue = "";
        PresetDefinition preset = presetHolder.getValue();
        if (preset != null) {
            presetValue = preset.getValue(attribute.getName());
        }
        Object value;
        switch(col) {
            case COLUMN_NUMBER:
                value = row + 1;
                break;
            case COLUMN_NAME:
                value = attribute.getName();
                break;
            case COLUMN_CHANNELS:
                value = attribute.getChannels();
                break;
            case COLUMN_PRESET:
                value = presetValue;
                break;
            case COLUMN_HAS_VALUES:
                value = hasValues;
                break;
            default:
                value = "?";
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int col) {
        return col == COLUMN_NAME || col == COLUMN_CHANNELS || (col == COLUMN_PRESET && presetHolder.getValue() != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object value, final int row, final int col) {
        AttributeDefinition attributeDefinition = fixtureHolder.getValue().getAttribute(row);
        if (col == COLUMN_NAME) {
            attributeDefinition.setName((String) value);
        }
        if (col == COLUMN_CHANNELS) {
            attributeDefinition.setChannels((String) value);
        }
        if (col == COLUMN_PRESET) {
            PresetDefinition preset = presetHolder.getValue();
            if (preset != null) {
                preset.set(attributeDefinition, (String) value);
            }
        }
    }

    private class PresetListener implements Listener {

        public void changed() {
            fireTableDataChanged();
        }
    }
}
