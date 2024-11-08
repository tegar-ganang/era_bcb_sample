package com.redoddity.faml.tests.daos;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import junit.framework.TestCase;
import com.redoddity.faml.model.Image;
import com.redoddity.faml.model.Picture;
import com.redoddity.faml.model.daos.PictureDAO;
import com.redoddity.faml.model.people.Photographer;

public class PictureDAOTest extends TestCase {

    private PictureDAO p = new PictureDAO();

    protected void setUp() throws Exception {
        super.setUp();
        Picture pic1 = new Picture();
        Picture pic2 = new Picture();
        ArrayList<URL> relatedArticles = new ArrayList<URL>();
        relatedArticles.add(new URL("http://link1"));
        relatedArticles.add(new URL("http://link2"));
        ArrayList<String> comments = new ArrayList<String>();
        comments.add("comment1");
        comments.add("comment2");
        ArrayList<String> tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        pic1.setImg(new Image(new URI("file:///etc/mtab")));
        pic1.setId(1L);
        pic1.setRating(54);
        pic1.setTitle("Pic1");
        pic1.setRelatedArticles(relatedArticles);
        pic1.setComments(comments);
        pic1.setPhotographer(new Photographer(2L, new Image(), "Foobar", "Baz", null, 1));
        pic1.setTags(tags);
        pic1.setHeight(1024);
        pic1.setWidth(768);
        pic1.setSize(64);
        pic1.setNumberOfDownloads(2);
        pic2.setImg(new Image(new URI("img.jpg")));
        pic2.setId(2L);
        pic2.setRating(43);
        pic2.setTitle("Pic2");
        pic2.setRelatedArticles(relatedArticles);
        pic2.setComments(comments);
        pic2.setPhotographer(new Photographer(2L, new Image(), "Foobar", "Baz", null, 567));
        pic2.setTags(tags);
        pic2.setHeight(3024);
        pic2.setWidth(768);
        pic2.setSize(5642);
        pic2.setNumberOfDownloads(214);
        p.addPic(pic1);
        p.addPic(pic2);
    }

    public void testAddPic() {
        try {
            Picture pic3 = new Picture();
            ArrayList<URL> relatedArticles = new ArrayList<URL>();
            relatedArticles.add(new URL("http://link1"));
            relatedArticles.add(new URL("http://link2"));
            ArrayList<String> comments = new ArrayList<String>();
            comments.add("comment1");
            comments.add("comment2");
            ArrayList<String> tags = new ArrayList<String>();
            tags.add("tag1");
            tags.add("tag2");
            pic3.setImg(new Image(new URI("img.jpg")));
            pic3.setId(3L);
            pic3.setRating(6352);
            pic3.setTitle("Pic3");
            pic3.setRelatedArticles(relatedArticles);
            pic3.setComments(comments);
            pic3.setPhotographer(new Photographer(3L, new Image(), "Pippo", "Pippi", null, 3));
            pic3.setTags(tags);
            pic3.setHeight(30);
            pic3.setWidth(7);
            pic3.setSize(5);
            pic3.setNumberOfDownloads(21465241);
            assertEquals(p.addPic(pic3), true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void testSearchMultimediaFile() throws Exception {
        try {
            Picture pic4 = new Picture();
            ArrayList<URL> relatedArticles = new ArrayList<URL>();
            relatedArticles.add(new URL("http://link1"));
            relatedArticles.add(new URL("http://link2"));
            ArrayList<String> comments = new ArrayList<String>();
            comments.add("comment1");
            comments.add("comment2");
            ArrayList<String> tags = new ArrayList<String>();
            tags.add("tag1");
            tags.add("tag2");
            pic4.setImg(new Image(new URI("img.jpg")));
            pic4.setId(4L);
            pic4.setRating(6452);
            pic4.setTitle("Pic4");
            pic4.setRelatedArticles(relatedArticles);
            pic4.setComments(comments);
            pic4.setPhotographer(new Photographer(3L, new Image(), "Pippo", "Pippi", null, 6));
            pic4.setTags(tags);
            pic4.setHeight(40);
            pic4.setWidth(7);
            pic4.setSize(5);
            pic4.setNumberOfDownloads(241);
            p.addPic(pic4);
            assertEquals(p.searchPic("Pic4"), pic4);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void testDeleteMultimediaFile() throws Exception {
        p.deletePic(p.searchPic("Pic2").getId());
        assertEquals(p.searchPic("Pic2"), null);
    }

    public void testDeleteAllMultimediaFiles() throws Exception {
        assertEquals(p.deleteAllPic(), true);
        assertEquals(p.searchPic("Pic1"), null);
        assertEquals(p.searchPic("Pic2"), null);
        assertEquals(p.searchPic("Pic3"), null);
        assertEquals(p.searchPic("Pic4"), null);
    }

    public void testUpdatePic() throws Exception {
        Picture pic1 = new Picture();
        ArrayList<URL> relatedArticles = new ArrayList<URL>();
        relatedArticles.add(new URL("http://link1"));
        relatedArticles.add(new URL("http://link2"));
        ArrayList<String> comments = new ArrayList<String>();
        comments.add("comment1");
        comments.add("comment2");
        ArrayList<String> tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");
        pic1.setImg(new Image(new URI("img.jpg")));
        pic1.setId(1L);
        pic1.setRating(6112);
        pic1.setTitle("Picz1");
        pic1.setRelatedArticles(relatedArticles);
        pic1.setComments(comments);
        pic1.setPhotographer(new Photographer(2L, new Image(), "Foobar", "Baz", null, 67));
        pic1.setTags(tags);
        pic1.setHeight(645);
        pic1.setWidth(12521);
        pic1.setSize(15);
        pic1.setNumberOfDownloads(211112412);
        assertEquals(p.updatePic(pic1), true);
        assertEquals(p.searchPic("Picz1"), pic1);
    }

    public void testImageStream() {
        Image image = p.searchPic("Pic1").getImg();
        File file = new File(p.searchPic("Pic1").getImg().getUri());
        File destination = new File("Pic.jpg");
        if (file.equals(destination)) return;
        boolean buffered = true;
        try {
            InputStream fis = image.getImage(buffered);
            OutputStream fos = new FileOutputStream(destination);
            fos = new BufferedOutputStream(new FileOutputStream(destination));
            try {
                long start = System.currentTimeMillis();
                if (buffered) {
                    final int size = 8192;
                    byte[] buffer = new byte[size];
                    int read;
                    while ((read = fis.read(buffer, 0, size)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                } else {
                    int read;
                    while ((read = fis.read()) != -1) {
                        fos.write(read);
                    }
                }
                System.out.println("copy duration: " + (System.currentTimeMillis() - start));
            } catch (IOException ioe) {
            } finally {
                try {
                    if (fos != null) fos.close();
                    if (fis != null) fis.close();
                } catch (IOException e) {
                }
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
    }
}
