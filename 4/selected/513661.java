package org.apache.shindig.gadgets.rewrite.image;

import org.apache.sanselan.ImageReadException;
import org.apache.shindig.gadgets.http.HttpResponse;
import java.io.IOException;

/**
 * Test JPEG rewiting.
 */
public class JPEGOptimizerTest extends BaseOptimizerTest {

    public void testSmallJPEGToPNG() throws Exception {
        HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/small.jpg", "image/jpeg");
        HttpResponse rewritten = rewrite(resp);
        assertEquals(rewritten.getHeader("Content-Type"), "image/png");
        assertTrue(rewritten.getContentLength() * 100 / resp.getContentLength() < 70);
    }

    public void testLargeJPEG() throws Exception {
        HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/large.jpg", "image/jpeg");
        HttpResponse rewritten = rewrite(resp);
        assertEquals(resp.getHeader("Content-Type"), "image/jpeg");
        assertTrue(rewritten.getContentLength() <= resp.getContentLength());
    }

    public void testBadImage() throws Exception {
        try {
            HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/bad.jpg", "image/jpeg");
            rewrite(resp);
            fail("Should fail to read an invalid JPEG");
        } catch (Throwable t) {
        }
    }

    public void xtestBadICC1() throws Exception {
        HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc.jpg", "image/jpeg");
        rewrite(resp);
    }

    public void testBadICC2() throws Exception {
        try {
            HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc2.jpg", "image/jpeg");
            rewrite(resp);
            fail("Should error with invalid ICC data");
        } catch (Throwable t) {
        }
    }

    public void testBadICC3() throws Exception {
        try {
            HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc3.jpg", "image/jpeg");
            rewrite(resp);
            fail("Should error with invalid ICC data");
        } catch (Throwable t) {
        }
    }

    public void testBadICC4() throws Exception {
        HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc4.jpg", "image/jpeg");
        try {
            rewrite(resp);
            fail("Should have failed with OutOfMemory exception");
        } catch (OutOfMemoryError oome) {
        } catch (NullPointerException npe) {
        }
    }

    public void testBadICC5() throws Exception {
        HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/2ndjpgbad.jpg", "image/jpeg");
        rewrite(resp);
    }

    HttpResponse rewrite(HttpResponse original) throws IOException, ImageReadException {
        return new JPEGOptimizer(new OptimizerConfig(), original).rewrite(JPEGOptimizer.readJpeg(original.getResponse()));
    }
}
