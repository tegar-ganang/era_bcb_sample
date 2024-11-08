package be.lassi.ui.show;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.support.Launcher;

/**
 * Tests class <code>StartParametersDialog</code>.
 */
public class StartParametersDialogTestCase {

    private StartParameters parameters;

    private StartParametersDialog target;

    @BeforeMethod
    public void init() {
        parameters = new StartParameters();
        target = new StartParametersDialog(null, parameters);
    }

    @Test
    public void testValidCreateShowParameters() {
        parameters.setChannelCount("120");
        parameters.setSubmasterCount("12");
        parameters.setCueCount("5");
        StartParametersDialogOperator dialog = openDialog();
        assertTrue(dialog.buttonOk.isEnabled());
        assertTrue(dialog.buttonCancel.isEnabled());
        assertFalse(dialog.filename.isEnabled());
        assertTrue(dialog.channelCount.isEnabled());
        assertTrue(dialog.submasterCount.isEnabled());
        assertTrue(dialog.cueCount.isEnabled());
        assertEquals(dialog.channelCount.getText(), "120");
        assertEquals(dialog.submasterCount.getText(), "12");
        assertEquals(dialog.cueCount.getText(), "5");
        dialog.channelCount.setText("555");
        assertFalse(dialog.buttonOk.isEnabled());
        dialog.channelCount.setText("60");
        dialog.submasterCount.setText("6");
        dialog.cueCount.setText("3");
        assertTrue(dialog.buttonOk.isEnabled());
        dialog.buttonOk.push();
        dialog.window.waitClosed();
        assertFalse(target.isCancelled());
        assertEquals(parameters.getChannelCount(), "60");
        assertEquals(parameters.getSubmasterCount(), "6");
        assertEquals(parameters.getCueCount(), "3");
    }

    @Test
    public void testInvalidParameters() {
        parameters.setChannelCount("");
        StartParametersDialogOperator dialog = openDialog();
        assertFalse(dialog.buttonOk.isEnabled());
        assertTrue(dialog.buttonCancel.isEnabled());
        dialog.buttonCancel.push();
        dialog.window.waitClosed();
        assertTrue(target.isCancelled());
    }

    @Test
    public void testValidOpenShowParameters() {
        StartParametersDialogOperator dialog = openDialog();
        dialog.openShowButton.clickMouse();
        assertTrue(parameters.isOpenShow());
        assertFalse(dialog.channelCount.isEnabled());
        assertFalse(dialog.submasterCount.isEnabled());
        assertFalse(dialog.cueCount.isEnabled());
        assertTrue(dialog.filename.isEnabled());
        assertFalse(dialog.buttonOk.isEnabled());
        dialog.filename.setText("example.show");
        assertTrue(dialog.buttonOk.isEnabled());
        dialog.buttonOk.push();
        dialog.window.waitClosed();
        assertFalse(target.isCancelled());
        assertEquals("example.show", parameters.getFilename());
    }

    private StartParametersDialogOperator openDialog() {
        Launcher.run(new Runnable() {

            public void run() {
                target.show();
            }
        });
        return new StartParametersDialogOperator();
    }
}
