package be.lassi.ui.library;

import static be.lassi.ui.library.AttributesTableModel.COLUMN_CHANNELS;
import static be.lassi.ui.library.AttributesTableModel.COLUMN_NAME;
import static be.lassi.ui.library.AttributesTableModel.COLUMN_NUMBER;
import static be.lassi.ui.library.AttributesTableModel.COLUMN_PRESET;
import static org.testng.Assert.assertEquals;
import javax.swing.table.TableModel;
import org.testng.annotations.Test;
import be.lassi.base.Holder;
import be.lassi.domain.FixtureDefinition;
import be.lassi.domain.PresetDefinition;

/**
 * Tests class <code>AttributesTableModel</code>.
 */
public class AttributesTableModelTestCase {

    private final Holder<FixtureDefinition> fixtureHolder = new Holder<FixtureDefinition>();

    private final Holder<PresetDefinition> presetHolder = new Holder<PresetDefinition>();

    private final TableModel model = new AttributesTableModel(fixtureHolder, presetHolder);

    @Test
    public void testNoDefinition() {
        fixtureHolder.setValue(null);
        presetHolder.setValue(null);
        assertEquals(model.getRowCount(), 0);
    }

    @Test
    public void testNoAttributes() {
        FixtureDefinition definition = new FixtureDefinition();
        fixtureHolder.setValue(definition);
        assertEquals(model.getRowCount(), 0);
    }

    @Test
    public void testGetValue() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("name", "1");
        fixtureHolder.setValue(definition);
        assertEquals(model.getRowCount(), 1);
        assertEquals(model.getValueAt(0, COLUMN_NUMBER), 1);
        assertEquals(model.getValueAt(0, COLUMN_NAME), "name");
        assertEquals(model.getValueAt(0, COLUMN_CHANNELS), "1");
        assertEquals(model.getValueAt(0, COLUMN_PRESET), "");
    }

    @Test
    public void testSetValue() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("name", "1");
        fixtureHolder.setValue(definition);
        model.setValueAt("name2", 0, AttributesTableModel.COLUMN_NAME);
        assertEquals(definition.getAttribute(0).getName(), "name2");
        model.setValueAt("2", 0, AttributesTableModel.COLUMN_CHANNELS);
        assertEquals(definition.getAttribute(0).getChannels(), "2");
    }
}
