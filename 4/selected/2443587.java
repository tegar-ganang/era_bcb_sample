package net.sf.xwav.soundrenderer.test;

import static org.junit.Assert.*;
import java.io.IOException;
import net.sf.xwav.soundrenderer.BadParameterException;
import net.sf.xwav.soundrenderer.SelfFillSoundBuffer;
import net.sf.xwav.soundrenderer.SoundBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSoundBuffer {

    SoundBuffer mono;

    SoundBuffer stereo;

    @Before
    public void setUp() throws Exception {
        mono = new SoundBuffer(1, 100);
        for (int i = 0; i < 100; i++) mono.getChannelData(0)[i] = i;
        stereo = new SoundBuffer(2, 150);
        for (int i = 0; i < 150; i++) {
            stereo.getChannelData(0)[i] = 2 * i;
            stereo.getData()[1][i] = 3 * i;
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetData() {
        for (int i = 0; i < 100; i++) assertEquals(mono.getData()[0][i], i, 0.001);
        for (int i = 0; i < 150; i++) {
            assertEquals(stereo.getData()[0][i], 2 * i, 0.001);
            assertEquals(stereo.getData()[1][i], 3 * i, 0.001);
        }
    }

    @Test
    public void testGetChannelData() throws IOException, BadParameterException {
        for (int i = 0; i < 100; i++) assertEquals(mono.getChannelData(0)[i], i, 0.001);
        for (int i = 0; i < 150; i++) {
            assertEquals(stereo.getChannelData(0)[i], 2 * i, 0.001);
            assertEquals(stereo.getChannelData(1)[i], 3 * i, 0.001);
        }
    }

    @Test
    public void testFillAndCompareChannelData() throws IOException, BadParameterException {
        SelfFillSoundBuffer monoTest = new SelfFillSoundBuffer(1, 100);
        monoTest.fillChannelData(0, mono0TestData);
        assertTrue(monoTest.compareChannelData(0, mono0TestData, 0.001f, 0));
        monoTest.getData()[0][17] = 1;
        assertFalse(monoTest.compareChannelData(0, mono0TestData, 0.001f, 0));
        SelfFillSoundBuffer stereoTest = new SelfFillSoundBuffer(2, 150);
        stereoTest.fillChannelData(0, stereo0TestData);
        stereoTest.fillChannelData(1, stereo1TestData);
        assertTrue(stereoTest.compareChannelData(0, stereo0TestData, 0.001f, 0));
        stereoTest.getData()[0][17] = 1;
        assertFalse(stereoTest.compareChannelData(0, stereo0TestData, 0.001f, 0));
        assertTrue(stereoTest.compareChannelData(1, stereo1TestData, 0.001f, 0));
        stereoTest.getData()[1][17] = 1;
        assertFalse(stereoTest.compareChannelData(1, stereo1TestData, 0.001f, 0));
    }

    @Test
    public void testGetNumberOfChannels() {
        assertEquals(mono.getNumberOfChannels(), 1);
        assertEquals(stereo.getNumberOfChannels(), 2);
    }

    @Test
    public void testGetDataLength() {
        assertEquals(mono.getDataLength(), 100);
        assertEquals(stereo.getDataLength(), 150);
    }

    @Test(expected = BadParameterException.class)
    public void testOutOfRangeChannel() throws BadParameterException {
        @SuppressWarnings("unused") float f = mono.getChannelData(1)[0];
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOutOfRangeData() throws BadParameterException {
        @SuppressWarnings("unused") float f = mono.getChannelData(0)[100];
    }

    float mono0TestData[] = { 0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f, 21.0f, 22.0f, 23.0f, 24.0f, 25.0f, 26.0f, 27.0f, 28.0f, 29.0f, 30.0f, 31.0f, 32.0f, 33.0f, 34.0f, 35.0f, 36.0f, 37.0f, 38.0f, 39.0f, 40.0f, 41.0f, 42.0f, 43.0f, 44.0f, 45.0f, 46.0f, 47.0f, 48.0f, 49.0f, 50.0f, 51.0f, 52.0f, 53.0f, 54.0f, 55.0f, 56.0f, 57.0f, 58.0f, 59.0f, 60.0f, 61.0f, 62.0f, 63.0f, 64.0f, 65.0f, 66.0f, 67.0f, 68.0f, 69.0f, 70.0f, 71.0f, 72.0f, 73.0f, 74.0f, 75.0f, 76.0f, 77.0f, 78.0f, 79.0f, 80.0f, 81.0f, 82.0f, 83.0f, 84.0f, 85.0f, 86.0f, 87.0f, 88.0f, 89.0f, 90.0f, 91.0f, 92.0f, 93.0f, 94.0f, 95.0f, 96.0f, 97.0f, 98.0f, 99.0f };

    float stereo0TestData[] = { 0.0f, 2.0f, 4.0f, 6.0f, 8.0f, 10.0f, 12.0f, 14.0f, 16.0f, 18.0f, 20.0f, 22.0f, 24.0f, 26.0f, 28.0f, 30.0f, 32.0f, 34.0f, 36.0f, 38.0f, 40.0f, 42.0f, 44.0f, 46.0f, 48.0f, 50.0f, 52.0f, 54.0f, 56.0f, 58.0f, 60.0f, 62.0f, 64.0f, 66.0f, 68.0f, 70.0f, 72.0f, 74.0f, 76.0f, 78.0f, 80.0f, 82.0f, 84.0f, 86.0f, 88.0f, 90.0f, 92.0f, 94.0f, 96.0f, 98.0f, 100.0f, 102.0f, 104.0f, 106.0f, 108.0f, 110.0f, 112.0f, 114.0f, 116.0f, 118.0f, 120.0f, 122.0f, 124.0f, 126.0f, 128.0f, 130.0f, 132.0f, 134.0f, 136.0f, 138.0f, 140.0f, 142.0f, 144.0f, 146.0f, 148.0f, 150.0f, 152.0f, 154.0f, 156.0f, 158.0f, 160.0f, 162.0f, 164.0f, 166.0f, 168.0f, 170.0f, 172.0f, 174.0f, 176.0f, 178.0f, 180.0f, 182.0f, 184.0f, 186.0f, 188.0f, 190.0f, 192.0f, 194.0f, 196.0f, 198.0f, 200.0f, 202.0f, 204.0f, 206.0f, 208.0f, 210.0f, 212.0f, 214.0f, 216.0f, 218.0f, 220.0f, 222.0f, 224.0f, 226.0f, 228.0f, 230.0f, 232.0f, 234.0f, 236.0f, 238.0f, 240.0f, 242.0f, 244.0f, 246.0f, 248.0f, 250.0f, 252.0f, 254.0f, 256.0f, 258.0f, 260.0f, 262.0f, 264.0f, 266.0f, 268.0f, 270.0f, 272.0f, 274.0f, 276.0f, 278.0f, 280.0f, 282.0f, 284.0f, 286.0f, 288.0f, 290.0f, 292.0f, 294.0f, 296.0f, 298.0f };

    float stereo1TestData[] = { 0.0f, 3.0f, 6.0f, 9.0f, 12.0f, 15.0f, 18.0f, 21.0f, 24.0f, 27.0f, 30.0f, 33.0f, 36.0f, 39.0f, 42.0f, 45.0f, 48.0f, 51.0f, 54.0f, 57.0f, 60.0f, 63.0f, 66.0f, 69.0f, 72.0f, 75.0f, 78.0f, 81.0f, 84.0f, 87.0f, 90.0f, 93.0f, 96.0f, 99.0f, 102.0f, 105.0f, 108.0f, 111.0f, 114.0f, 117.0f, 120.0f, 123.0f, 126.0f, 129.0f, 132.0f, 135.0f, 138.0f, 141.0f, 144.0f, 147.0f, 150.0f, 153.0f, 156.0f, 159.0f, 162.0f, 165.0f, 168.0f, 171.0f, 174.0f, 177.0f, 180.0f, 183.0f, 186.0f, 189.0f, 192.0f, 195.0f, 198.0f, 201.0f, 204.0f, 207.0f, 210.0f, 213.0f, 216.0f, 219.0f, 222.0f, 225.0f, 228.0f, 231.0f, 234.0f, 237.0f, 240.0f, 243.0f, 246.0f, 249.0f, 252.0f, 255.0f, 258.0f, 261.0f, 264.0f, 267.0f, 270.0f, 273.0f, 276.0f, 279.0f, 282.0f, 285.0f, 288.0f, 291.0f, 294.0f, 297.0f, 300.0f, 303.0f, 306.0f, 309.0f, 312.0f, 315.0f, 318.0f, 321.0f, 324.0f, 327.0f, 330.0f, 333.0f, 336.0f, 339.0f, 342.0f, 345.0f, 348.0f, 351.0f, 354.0f, 357.0f, 360.0f, 363.0f, 366.0f, 369.0f, 372.0f, 375.0f, 378.0f, 381.0f, 384.0f, 387.0f, 390.0f, 393.0f, 396.0f, 399.0f, 402.0f, 405.0f, 408.0f, 411.0f, 414.0f, 417.0f, 420.0f, 423.0f, 426.0f, 429.0f, 432.0f, 435.0f, 438.0f, 441.0f, 444.0f, 447.0f };
}
