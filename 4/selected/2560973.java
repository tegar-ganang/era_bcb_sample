package be.lassi.lanbox.commands.layer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.lanbox.commands.CommandTCA;
import be.lassi.lanbox.domain.FadeType;
import be.lassi.lanbox.domain.LayerStatus;
import be.lassi.lanbox.domain.Time;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;
import be.lassi.util.Chrono;

/**
 * Tests class <code>LayerPause</code>.
 */
public class LayerPauseTCL extends CommandTCA {

    @Test
    public void encodeDecode() {
        LayerPause command1 = new LayerPause(7);
        LayerPause command2 = new LayerPause(command1.getRequest());
        assertEquals(command1, command2);
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                return new LayerClear(1);
            }

            public Object getObject2() {
                return new LayerClear(2);
            }
        };
        ObjectTest.test(b);
    }

    @Test
    public void fading() {
        doTestFading(true);
        doTestFading(false);
    }

    @Test
    public void pauseAndResumeFade() {
        execute(new LayerSetFading(1, false));
        setChannel1(0);
        execute(new LayerResume(1));
        execute(new LayerSetFading(1, true));
        execute(new LayerSetFadeType(1, FadeType.CROSS_FADE));
        execute(new LayerSetFadeTime(1, Time.TIME_2S));
        Chrono chrono = new Chrono();
        setChannel1(255);
        chrono.waitFor(500);
        execute(new LayerPause(1));
        assertEquals(getChannel1(), 255 / 4, 10);
        sleep(1000);
        chrono = new Chrono();
        execute(new LayerResume(1));
        chrono.waitFor(500);
        assertEquals(getChannel1(), 255 / 2, 10);
        chrono.waitFor(1000);
        assertEquals(getChannel1(), 3 * 255 / 4, 10);
        chrono.waitFor(1500);
        assertEquals(getChannel1(), 255, 10);
        sleep(50);
        LayerStatus status = new LayerStatus();
        execute(new LayerGetStatus(1, status));
        assertFalse(status.isPausing());
        assertEquals(status.getFadeTimeRemaining(), 0);
    }

    @Test
    public void pauseFadeAndStop() {
        execute(new LayerSetFading(1, false));
        setChannel1(0);
        execute(new LayerSetFading(1, true));
        execute(new LayerSetFadeType(1, FadeType.CROSS_FADE));
        execute(new LayerSetFadeTime(1, Time.TIME_2S));
        execute(new LayerResume(1));
        Chrono chrono = new Chrono();
        setChannel1(255);
        chrono.waitFor(500);
        execute(new LayerPause(1));
        LayerStatus status = new LayerStatus();
        execute(new LayerGetStatus(1, status));
        assertTrue(status.isPausing());
        assertEquals(status.getFadeTimeRemaining(), 1500, 100);
        assertEquals(getChannel1(), 255 / 4, 10);
        chrono.waitFor(1000);
        assertEquals(getChannel1(), 255 / 4, 10);
        execute(new LayerSetFadeType(1, FadeType.OFF));
        setChannel1(63);
        execute(new LayerResume(1));
        execute(new LayerGetStatus(1, status));
        assertFalse(status.isPausing());
        chrono.waitFor(1500);
        assertEquals(getChannel1(), 63);
    }

    @Test
    public void timing() {
        doTestTiming(Time.TIME_2S, 500);
        doTestTiming(Time.TIME_1S, 100);
        doTestTiming(Time.TIME_1S, 900);
        doTestTiming(Time.TIME_5S, 100);
    }

    private void doTestFading(final boolean enabled) {
        execute(new LayerSetFading(1, enabled));
        LayerStatus status = layerGetStatus(1);
        assertEquals(status.isFading(), enabled);
    }

    private void doTestTiming(final Time time, final int waitTime) {
        execute(new LayerSetFading(1, false));
        setChannel1(0);
        execute(new LayerSetFading(1, true));
        execute(new LayerSetFadeType(1, FadeType.CROSS_FADE));
        execute(new LayerSetFadeTime(1, time));
        execute(new LayerResume(1));
        Chrono chrono = new Chrono();
        setChannel1(255);
        chrono.waitFor(waitTime);
        execute(new LayerPause(1));
        LayerStatus status = new LayerStatus();
        execute(new LayerGetStatus(1, status));
        int millis = status.getFadeTimeRemaining();
        int value = getChannel1();
        int expectedValue = 255 * (time.getMillis() - millis) / time.getMillis();
        assertEquals(value, expectedValue, 1);
    }
}
