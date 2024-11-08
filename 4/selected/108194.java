package org.lwjgl.test;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.lwjgl.LWJGLException;
import org.lwjgl.util.WaveData;

/**
 * <br>
 * Test class WaveDataTest
 *
 * @author Brian Matzon <brian@matzon.dk>
 */
public class WaveDataTest {

    String filePath = "Footsteps.wav";

    /**
	 * Creates a new DisplayTest
	 */
    public WaveDataTest() {
    }

    /**
	 * Runs the tests
	 */
    public void executeTest() throws LWJGLException {
        executeCreationTest();
        executeBrokenCreationTest();
        executeMidStreamCreationTest();
    }

    private void executeCreationTest() {
        WaveData wd = WaveData.create(filePath);
        if (wd != null) {
            System.out.println("executeCreationTest::success");
        }
    }

    private void executeBrokenCreationTest() {
        WaveData wd = WaveData.create("");
        if (wd == null) {
            System.out.println("executeBrokenCreationTest::success");
        }
    }

    private void executeStreamCreationTest() {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filePath));
            WaveData wd = WaveData.create(ais);
            if (wd == null) {
                System.out.println("executeMidStreamCreationTest::success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeMidStreamCreationTest() {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(WaveDataTest.class.getClassLoader().getResource(filePath));
            int totalSize = ais.getFormat().getChannels() * (int) ais.getFrameLength() * ais.getFormat().getSampleSizeInBits() / 8;
            int skip = totalSize / 4;
            long skipped = ais.skip(skip);
            WaveData wd = WaveData.create(ais);
            if (wd == null) {
                System.out.println("executeMidStreamCreationTest::success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	   * Pause current thread for a specified time
	   * 
	   * @param time milliseconds to sleep
	   */
    private void pause(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException inte) {
        }
    }

    /**
	 * Tests the Sys class, and serves as basic usage test
	 * 
	 * @param args ignored
	 */
    public static void main(String[] args) throws LWJGLException {
        new WaveDataTest().executeTest();
        System.exit(0);
    }
}
