package be.lassi.ui.show;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.support.Launcher;

/**
 * Tests class <code>ShowParametersDialog</code>.
 */
public class ShowParametersDialogTestCase {

    private ShowParameters parameters;

    private ShowParametersDialog target;

    @BeforeMethod
    public void init() {
        parameters = new StartParameters();
        target = new ShowParametersDialog(null, parameters);
    }

    @Test
    public void testValidCreateShowParameters() {
        parameters.setChannelCount("120");
        parameters.setSubmasterCount("12");
        parameters.setCueCount("5");
        ShowParametersDialogOperator dialog = openDialog();
        assertTrue(dialog.buttonOk.isEnabled());
        assertTrue(dialog.buttonCancel.isEnabled());
        assertTrue(dialog.channelCount.isEnabled());
        assertTrue(dialog.submasterCount.isEnabled());
        assertEquals(dialog.channelCount.getText(), "120");
        assertEquals(dialog.submasterCount.getText(), "12");
        dialog.channelCount.setText("555");
        assertFalse(dialog.buttonOk.isEnabled());
        dialog.channelCount.setText("60");
        dialog.submasterCount.setText("6");
        assertTrue(dialog.buttonOk.isEnabled());
        dialog.buttonOk.push();
        dialog.window.waitClosed();
        assertFalse(target.isCancelled());
        assertEquals(parameters.getChannelCount(), "60");
        assertEquals(parameters.getSubmasterCount(), "6");
    }

    @Test
    public void testInvalidParameters() {
        parameters.setChannelCount("");
        ShowParametersDialogOperator dialog = openDialog();
        assertFalse(dialog.buttonOk.isEnabled());
        assertTrue(dialog.buttonCancel.isEnabled());
        dialog.buttonCancel.push();
        dialog.window.waitClosed();
        assertTrue(target.isCancelled());
    }

    private ShowParametersDialogOperator openDialog() {
        Launcher.run(new Runnable() {

            public void run() {
                target.show();
            }
        });
        return new ShowParametersDialogOperator();
    }
}
