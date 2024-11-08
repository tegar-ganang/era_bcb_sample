package photospace.meta;

import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.logging.*;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.hp.hpl.jena.rdf.model.*;
import junit.framework.*;
import photospace.meta.rdf.*;
import photospace.vfs.FileSystem;
import photospace.vfs.*;

public class PersisterTest extends TestCase {

    private static final Log log = LogFactory.getLog(PersisterTest.class);

    public void testGetMetadataFromJpeg() throws Exception {
        PersisterImpl persister = new PersisterImpl();
        persister.setStoringInJpeg(false);
        Translator translator = new Translator();
        persister.setTranslator(translator);
        FileSystem filesystem = new FileSystemImpl();
        filesystem.setRoot(new File(System.getProperty("project.root"), "build/test/"));
        persister.setFilesystem(filesystem);
        File jpeg = new File(System.getProperty("project.root"), "build/test/exif-nordf.jpg").getCanonicalFile();
        Metadata exif = persister.getExif(jpeg);
        Model existing = persister.getRdf(jpeg);
        if (existing != null) {
            persister.removeRdf(jpeg);
            exif = persister.getExif(jpeg);
        }
        assertNull(persister.getRdf(jpeg));
        PhotoMeta photo = translator.fromExif(exif);
        Model rdf = translator.toRdf(photo);
        Statement rdfMake = rdf.getProperty(null, TechVocab.CAMERA);
        String exifMake = exif.getDirectory(ExifDirectory.class).getString(ExifDirectory.TAG_MODEL);
        assertNotNull(rdfMake);
        assertNotNull(exifMake);
        PhotoMeta meta = (PhotoMeta) persister.getMeta(jpeg);
        assertEquals("/exif-nordf.jpg", meta.getPath());
        assertEquals(photo.getDevice(), rdfMake.getString());
        assertEquals(photo.getDevice(), exifMake);
        assertEquals(photo.getDevice(), meta.getDevice());
    }

    public void testSaveMeta() throws Exception {
        PersisterImpl persister = new PersisterImpl();
        persister.setStoringInJpeg(false);
        Translator translator = new Translator();
        persister.setTranslator(translator);
        FileSystem filesystem = new FileSystemImpl();
        filesystem.setRoot(new File(System.getProperty("project.root"), "build/test/"));
        persister.setFilesystem(filesystem);
        Date before = new Date();
        File exifJpeg = new File(System.getProperty("project.root"), "build/test/exif-nordf.jpg");
        File rdfJpeg = new File(System.getProperty("project.root"), "build/test/exif-rdf.jpg");
        File destJpeg = new File(System.getProperty("java.io.tmpdir"), "PersisterTest.jpg");
        FileUtils.copyFile(rdfJpeg, destJpeg);
        Metadata exif = persister.getExif(exifJpeg);
        exif = persister.getExif(exifJpeg);
        PhotoMeta exifPhoto = translator.fromExif(exif);
        persister.saveMeta(destJpeg, exifPhoto);
        persister.saveMeta(destJpeg, exifPhoto);
        Model rdf = persister.getRdf(destJpeg);
        PhotoMeta rdfPhoto = (PhotoMeta) translator.fromRdf(rdf);
        assertEquals(exifPhoto.getUpdated(), rdfPhoto.getUpdated());
        assertEquals(exifPhoto, rdfPhoto);
        assertTrue(before.before(exifPhoto.getUpdated()));
        assertTrue(before.before(rdfPhoto.getUpdated()));
    }
}
