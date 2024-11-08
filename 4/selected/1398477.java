package be.lassi.ui.main;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.base.Dirty;
import be.lassi.context.ShowContext;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.support.Launcher;
import be.lassi.ui.show.NewShowParametersDialogOperator;
import be.lassi.ui.show.ShowParametersDialogOperator;
import be.lassi.ui.show.StartParameters;
import be.lassi.util.ShowContextFactory;
import be.lassi.util.Util;

/**
 * Tests class <code>MainPresentationModel</code>.
 */
public class MainPresentationModelTestCase {

    private final ShowContext context = ShowContextFactory.getContext();

    private final MainPresentationModel model = new MainPresentationModel(null, context, null);

    @BeforeMethod
    public void before() {
        model.setExitEnabled(false);
    }

    @AfterClass
    public void closeContext() {
        context.close();
    }

    @Test
    public void testActionExitConfirmNo() {
        actionExit();
        ExitDialogOperator dialog = new ExitDialogOperator();
        dialog.no.push();
        dialog.window.waitClosed();
        assertFalse(model.isExit());
    }

    @Test
    public void testActionExitConfirmYes() {
        context.getDirtyShow().clear();
        actionExit();
        ExitDialogOperator dialog = new ExitDialogOperator();
        JLabelOperator message = new JLabelOperator(dialog.window, "OK to quit ?");
        assertFalse(message == null);
        dialog.yes.push();
        dialog.window.waitClosed();
        Util.sleep(100);
        assertTrue(model.isExit());
    }

    @Test
    public void testActionExitConfirmDirtyYes() {
        context.getDirtyShow().mark();
        actionExit();
        ExitDialogOperator dialog = new ExitDialogOperator();
        JLabelOperator message = new JLabelOperator(dialog.window, "OK to quit without saving show changes ?");
        assertFalse(message == null);
        dialog.yes.push();
        dialog.window.waitClosed();
        assertTrue(model.isExit());
    }

    @Test
    public void testActionNewCancel() {
        StartParameters parameters = context.getPreferences().getStartParameters();
        parameters.setChannelCount("5");
        parameters.setSubmasterCount("3");
        parameters.setCueCount("10");
        Show before = context.getShow();
        actionNew();
        NewShowParametersDialogOperator dialog = new NewShowParametersDialogOperator();
        dialog.buttonCancel.push();
        dialog.window.waitClosed();
        assertEquals(parameters.getChannelCount(), "5");
        assertEquals(parameters.getSubmasterCount(), "3");
        assertEquals(parameters.getCueCount(), "10");
        Show after = context.getShow();
        assertEquals(after.getNumberOfChannels(), before.getNumberOfChannels());
        assertEquals(after.getNumberOfSubmasters(), before.getNumberOfSubmasters());
        assertEquals(after.getCues().size(), before.getCues().size());
    }

    @Test
    public void testActionNew() {
        StartParameters parameters = context.getPreferences().getStartParameters();
        parameters.setChannelCount("5");
        parameters.setSubmasterCount("3");
        parameters.setCueCount("10");
        actionNew();
        NewShowParametersDialogOperator dialog = new NewShowParametersDialogOperator();
        dialog.channelCount.setText("10");
        dialog.submasterCount.setText("6");
        dialog.cueCount.setText("20");
        dialog.buttonOk.push();
        dialog.window.waitClosed();
        Util.sleep(100);
        assertEquals(parameters.getChannelCount(), "10");
        assertEquals(parameters.getSubmasterCount(), "6");
        assertEquals(parameters.getCueCount(), "20");
        Show show = context.getShow();
        assertEquals(show.getNumberOfChannels(), 10);
        assertEquals(show.getNumberOfSubmasters(), 6);
        assertEquals(show.getCues().size(), 20);
    }

    @Test
    public void testActionSetup() {
        Dirty dirty = context.getDirtyShow();
        Show before = ShowBuilder.build(dirty, 10, 5, 10, "");
        context.setShow(before);
        actionSetup();
        ShowParametersDialogOperator dialog = new ShowParametersDialogOperator();
        assertEquals(dialog.channelCount.getText(), "10");
        assertEquals(dialog.submasterCount.getText(), "5");
        dialog.channelCount.setText("12");
        dialog.submasterCount.setText("7");
        dialog.buttonOk.push();
        dialog.window.waitClosed();
        StartParameters parameters = context.getPreferences().getStartParameters();
        assertEquals(parameters.getChannelCount(), "12");
        assertEquals(parameters.getSubmasterCount(), "7");
        Show after = context.getShow();
        assertEquals(after.getNumberOfChannels(), 12);
        assertEquals(after.getNumberOfSubmasters(), 7);
    }

    private void actionExit() {
        Launcher.run(new Runnable() {

            public void run() {
                model.getActions().getActionExit().action();
            }
        });
    }

    private void actionNew() {
        Launcher.run(new Runnable() {

            public void run() {
                model.getActions().getActionNew().action();
            }
        });
    }

    private void actionSetup() {
        Launcher.run(new Runnable() {

            public void run() {
                model.getActions().getActionSetup().action();
            }
        });
    }

    private class ExitDialogOperator {

        JDialogOperator window = new JDialogOperator("Lassi quit");

        JButtonOperator yes = new JButtonOperator(window, "Yes");

        JButtonOperator no = new JButtonOperator(window, "No");

        JLabelOperator label = new JLabelOperator(window);

        public ExitDialogOperator() {
        }
    }
}
