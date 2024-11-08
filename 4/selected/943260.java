package com.lewisshell.helpyourself.psa;

import java.io.*;
import java.util.*;
import com.lewisshell.helpyourself.*;
import junit.framework.*;

public class HitCounterPSATest extends TestCase {

    private Image image1;

    private Image image2;

    private HitCounterPSA hitCounter;

    @Override
    protected void setUp() throws Exception {
        this.image1 = new Image(1, "image1", null, null, null, 0, null, 1, 1, new Date(1), new Date(1));
        this.image2 = new Image(2, "image2", null, null, null, 0, null, 1, 1, new Date(1), new Date(1));
        this.hitCounter = new HitCounterPSA();
    }

    public void testHitNull() {
        new HitCounterPSA().hitImage(null);
    }

    public void testHitInfoForNull() {
        assertNull(this.hitCounter.hitInfoForImage(null));
    }

    public void testHitInfoStartsWith0() {
        HitCounter.Info image1HitInfo = this.hitCounter.hitInfoForImage(image1);
        assertNotNull(image1HitInfo);
        assertEquals(0, image1HitInfo.getHits());
        assertEquals(0, image1HitInfo.getDownloads());
    }

    public void testHitInfoReused() {
        assertSame(this.hitCounter.hitInfoForImage(image1), this.hitCounter.hitInfoForImage(image1));
    }

    public void testHitImage1() {
        this.hitCounter.hitImage(image1);
        HitCounter.Info image1HitInfo = this.hitCounter.hitInfoForImage(image1);
        assertEquals(1, image1HitInfo.getHits());
    }

    public void testDownloadImage1() {
        this.hitCounter.downloadImage(image1);
        HitCounter.Info image1HitInfo = this.hitCounter.hitInfoForImage(image1);
        assertEquals(1, image1HitInfo.getDownloads());
    }

    public void testHitInfoComparesHits() {
        assertEquals(-1, new HitCounterPSA.HitInfo(0, 0).compareTo(new HitCounterPSA.HitInfo(1, 0)));
    }

    public void testHitInfoComparesDownloads() {
        assertEquals(-1, new HitCounterPSA.HitInfo(0, 0).compareTo(new HitCounterPSA.HitInfo(0, 1)));
    }

    public void testHitInfoComparesHitsOverDownloads() {
        assertEquals(-1, new HitCounterPSA.HitInfo(0, 1).compareTo(new HitCounterPSA.HitInfo(1, 0)));
    }

    public void testHitInfoComparesNull() {
        assertEquals(1, new HitCounterPSA.HitInfo(0, 0).compareTo(null));
    }

    public void testIsDirty() {
        assertFalse(this.hitCounter.isDirty());
    }

    public void testIsDirtyAfterHit() {
        this.hitCounter.hitImage(this.image1);
        assertTrue(this.hitCounter.isDirty());
    }

    public void testIsDirtyAfterDownload() {
        this.hitCounter.downloadImage(this.image1);
        assertTrue(this.hitCounter.isDirty());
    }

    public void testNotDirtyAfterSave() throws Exception {
        this.hitCounter.downloadImage(this.image1);
        File saveFile = File.createTempFile("hit-counter-test", ".hits");
        saveFile.deleteOnExit();
        this.hitCounter.save(saveFile.getAbsolutePath());
        assertFalse(this.hitCounter.isDirty());
        saveFile.delete();
    }

    private void setUpForLoadSave() {
        this.hitCounter.hitImage(image1);
        this.hitCounter.hitImage(image2);
        this.hitCounter.downloadImage(image2);
    }

    private void testLoaded(HitCounterPSA loadedHitCounter) {
        assertEquals(1, loadedHitCounter.hitInfoForImage(image1).getHits());
        assertEquals(0, loadedHitCounter.hitInfoForImage(image1).getDownloads());
        assertEquals(1, loadedHitCounter.hitInfoForImage(image2).getHits());
        assertEquals(1, loadedHitCounter.hitInfoForImage(image2).getDownloads());
    }

    public void testSaveAndLoadToFile() throws Exception {
        this.setUpForLoadSave();
        File hitCounterFile = File.createTempFile("hit-counter-test", ".hits");
        hitCounterFile.deleteOnExit();
        this.hitCounter.save(hitCounterFile.getAbsolutePath());
        HitCounterPSA loadedHitCounter = HitCounterPSA.load(hitCounterFile.getAbsolutePath());
        this.testLoaded(loadedHitCounter);
        hitCounterFile.delete();
    }

    public void testSaveAndLoad() throws Exception {
        this.setUpForLoadSave();
        StringWriter writer = new StringWriter();
        this.hitCounter.save(writer);
        StringReader reader = new StringReader(writer.toString());
        HitCounterPSA loadedHitCounter = HitCounterPSA.load(reader);
        this.testLoaded(loadedHitCounter);
    }

    public void testTrimEmptyHits() {
        this.hitCounter.hitImage(image1);
        HitCounter.Info image2Info = this.hitCounter.hitInfoForImage(this.image2);
        this.hitCounter.trimEmptyHits();
        assertEquals(1, this.hitCounter.hitInfoForImage(image1).getHits());
        assertEquals(0, this.hitCounter.hitInfoForImage(image2).getHits());
        assertNotSame(image2Info, this.hitCounter.hitInfoForImage(this.image2));
    }

    public void testSaveTrimsEmptyHits() {
        this.hitCounter.hitInfoForImage(this.image1);
        this.hitCounter.hitImage(this.image2);
        StringWriter writer = new StringWriter();
        this.hitCounter.save(writer);
        assertTrue(writer.toString().indexOf("<hits>0</hits>") < 0);
    }

    public void testMerge() {
        this.hitCounter.hitImage(this.image1);
        this.hitCounter.hitImage(this.image2);
        this.hitCounter.downloadImage(this.image1);
        HitCounterPSA hitCounter2 = new HitCounterPSA();
        hitCounter2.hitImage(this.image1);
        hitCounter2.downloadImage(this.image2);
        this.hitCounter.merge(hitCounter2);
        assertEquals(2, this.hitCounter.hitInfoForImage(this.image1).getHits());
        assertEquals(1, this.hitCounter.hitInfoForImage(this.image2).getHits());
        assertEquals(1, this.hitCounter.hitInfoForImage(this.image1).getDownloads());
        assertEquals(1, this.hitCounter.hitInfoForImage(this.image2).getDownloads());
    }
}
