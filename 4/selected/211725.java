package org.jd3lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import junit.framework.TestCase;
import org.jd3lib.util.AudioFileFilter;
import org.jd3lib.util.UnitTestHelper;

/**
 * @author Andreas Grunewald
 * 
 * LATER 1.0 Write Documentation
 */
public class Id3v2TagTest extends TestCase {

    public void testCreateNewFrameInNewTag() {
        Id3v2Tag ttag = new Id3v2Tag();
        String[] type = new String[] { "APIC", "COMM", "GEOB", "PCNT", "TRCK", "WXXX" };
        for (int i = 0; i < type.length; i++) {
            assertNull(ttag.getFrame(type[i]));
            ttag.addFrame(type[i]);
            assertEquals(ttag.getFrame(type[i]).getClass().getName(), Id3v2Tag.class.getPackage().getName() + ".Id3Frame" + type[i]);
        }
    }

    public void testCreateNewId3v2Tag() {
        File testfile = new File("data/JUnit_Id3v2TagTest.id3");
        try {
            testfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Couldn't create File");
        }
        try {
            new Id3v2Tag(new FileInputStream(testfile).getChannel());
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            fail("FileNotFound");
        } catch (InstantiationException e) {
            e.printStackTrace();
            fail("No ID3 Tag found");
        }
    }

    public void testGetTagData() {
        FileInputStream theStream = null;
        try {
            theStream = new FileInputStream("test_data/testID3_01_Winamp5.04Tag.id3");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("input file Not found");
        }
        Id3v2Tag test;
        try {
            test = new Id3v2Tag(theStream.getChannel());
            System.out.println(test.getFrame("TIT2"));
            FileOutputStream theFile = null;
            try {
                theFile = new FileOutputStream("test_data/test_01_JunitID3.id3");
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
                fail("output file Not found");
            }
            try {
                test.getTagData().writeTo(theFile);
                theStream = new FileInputStream("test_data/testID3_01_Winamp5.04Tag_withoutpadding.id3");
                byte[] shouldByte = new byte[theStream.available()], isByte = test.getTagData().toByteArray();
                theStream.read(shouldByte);
                UnitTestHelper.compareByteArray(shouldByte, isByte);
            } catch (IOException e) {
                e.printStackTrace();
                fail("IO Exception when wirting to file");
            }
        } catch (InstantiationException e2) {
            e2.printStackTrace();
        }
    }

    public void testId3v2Tag() {
        System.out.println("The Jee greets you");
        File directory = new File("test_data");
        System.out.println("directory:" + directory.getAbsolutePath());
        File[] files = directory.listFiles(new AudioFileFilter(false));
        System.out.println("Found:" + files.length);
        for (int i = 0; i < files.length; i++) {
            FileInputStream theStream = null;
            String[] excludes = { "07 Consuming Fire.mp3", "01 Here We Go.mp3", "01 I Turn to You (Louie Devito Mix).mp3", "Irene.mp3", "03 Sadie Hawkins Dance.mp3", "01 The Final Slowdance.mp3", "08 The Reason.mp3", "TestMP3_17_Musicmatch9AllTags_includingrealAPIC.mp3" };
            try {
                System.out.print("Name:" + files[i].getAbsolutePath());
                int j = 0;
                boolean con = false;
                while (j < excludes.length) {
                    if (excludes[j++].equals(files[i].getName())) {
                        con = true;
                        System.out.println("...skipped");
                        break;
                    }
                }
                if (con) continue;
                theStream = new FileInputStream(files[i]);
                System.out.println(" done");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (theStream == null) System.out.println("ERRRRRORRRR");
            Id3v2Tag test;
            try {
                test = new Id3v2Tag(theStream.getChannel());
                assertEquals(false, Id3v2Tag.faulty);
                System.out.println(test.header);
                String getFrame = "TDRC";
                if (test != null) {
                    System.out.println("Frame available:" + test.getFrameAvailability(getFrame));
                    if (test.getFrameAvailability(getFrame)) System.out.println(test.getFrame(getFrame));
                }
            } catch (InstantiationException e1) {
                e1.printStackTrace();
            }
        }
    }
}
