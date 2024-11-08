package com.threerings.s3.client;

import com.threerings.s3.client.acl.AccessControlList;
import com.threerings.s3.client.acl.AccessControlList.StandardPolicy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.*;
import static org.junit.Assert.*;

public class S3ConnectionTest {

    @Before
    public void setUp() throws Exception {
        FileOutputStream fileOutput;
        _conn = S3TestConfig.createConnection();
        _testBucketName = S3TestConfig.generateTestBucketName();
        _testFile = File.createTempFile("S3FileObjectTest", null);
        fileOutput = new FileOutputStream(_testFile);
        fileOutput.write(TEST_DATA.getBytes("utf8"));
        fileOutput.close();
        _fileObj = new S3FileObject("aKey", _testFile);
        _conn.createBucket(_testBucketName);
    }

    @After
    public void tearDown() throws Exception {
        S3TestConfig.deleteBucket(_conn, _testBucketName);
        _testFile.delete();
    }

    @Test
    public void testCreateBucket() throws Exception {
        _conn.createBucket(_testBucketName + "testCreateBucket");
        _conn.deleteBucket(_testBucketName + "testCreateBucket");
    }

    /**
     * Test listBucket.
     * Detailed testing of the response parsing, using static test data, is
     * also done in the S3ObjectListingHandler unit tests.
     */
    @Test
    public void testListBucket() throws Exception {
        S3ObjectListing listing;
        List<S3ObjectEntry> entries;
        S3ObjectEntry entry;
        S3Owner owner;
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);
        listing = _conn.listObjects(_testBucketName);
        entries = listing.getEntries();
        assertEquals(_testBucketName, listing.getBucketName());
        assertEquals(1, entries.size());
        entry = entries.get(0);
        assertEquals(_fileObj.getKey(), entry.getKey());
        assertTrue("Object's last modified date is not within the last 5 minutes: " + entry.getLastModified(), (new Date().getTime() - entry.getLastModified().getTime()) < 5 * 60 * 1000);
        String md5 = new String(Hex.encodeHex(_fileObj.getMD5()));
        assertEquals(md5, entry.getETag());
        assertEquals(_testFile.length(), entry.getSize());
        assertEquals(STORAGE_CLASS, entry.getStorageClass());
        owner = entry.getOwner();
        assertNotNull(owner.getId());
        assertNotNull(owner.getDisplayName());
    }

    /**
     * Test bucket listing with a prefix
     */
    @Test
    public void testListBucketPrefix() throws Exception {
        S3ObjectListing listing;
        S3ByteArrayObject obj1;
        S3ByteArrayObject obj2;
        List<S3ObjectEntry> entries;
        S3ObjectEntry entry;
        obj1 = new S3ByteArrayObject("test.obj1", new byte[0]);
        obj2 = new S3ByteArrayObject("ignore.obj2", new byte[0]);
        _conn.putObject(_testBucketName, obj1, AccessControlList.StandardPolicy.PRIVATE);
        _conn.putObject(_testBucketName, obj2, AccessControlList.StandardPolicy.PRIVATE);
        listing = _conn.listObjects(_testBucketName, "test", null, 0, null);
        assertTrue("Listing is truncated", !listing.truncated());
        assertEquals("test", listing.getPrefix());
        entries = listing.getEntries();
        assertEquals(1, entries.size());
        entry = entries.get(0);
        assertEquals("test.obj1", entry.getKey());
    }

    /**
     * Test bucket listing with max keys and marker.
     */
    @Test
    public void testListBucketMaxMarker() throws Exception {
        S3ObjectListing listing;
        S3ByteArrayObject obj1;
        S3ByteArrayObject obj2;
        obj1 = new S3ByteArrayObject("A", new byte[0]);
        obj2 = new S3ByteArrayObject("B", new byte[0]);
        _conn.putObject(_testBucketName, obj1, AccessControlList.StandardPolicy.PRIVATE);
        _conn.putObject(_testBucketName, obj2, AccessControlList.StandardPolicy.PRIVATE);
        listing = _conn.listObjects(_testBucketName, null, 1);
        assertEquals(1, listing.getEntries().size());
        assertEquals(1, listing.getMaxKeys());
        assertTrue("Listing is not truncated", listing.truncated());
        assertEquals("A", listing.getEntries().get(0).getKey());
        listing = _conn.listObjects(_testBucketName, listing.getNextMarker(), 1);
        assertEquals(1, listing.getEntries().size());
        assertEquals(1, listing.getMaxKeys());
        assertEquals("B", listing.getEntries().get(0).getKey());
        assertTrue("Listing is truncated", !listing.truncated());
        assertEquals("A", listing.getMarker());
        assertNull(listing.getNextMarker());
    }

    /**
     * Test bucket listing with a prefix and delimiter
     */
    @Test
    public void testListPrefixDelimiter() throws Exception {
        S3ObjectListing listing;
        S3ByteArrayObject obj1;
        S3ByteArrayObject obj2;
        S3ByteArrayObject obj3;
        obj1 = new S3ByteArrayObject("prefix.item0.0", new byte[0]);
        obj2 = new S3ByteArrayObject("prefix.item0.1", new byte[0]);
        obj3 = new S3ByteArrayObject("prefix.item1.0", new byte[0]);
        _conn.putObject(_testBucketName, obj1, AccessControlList.StandardPolicy.PRIVATE);
        _conn.putObject(_testBucketName, obj2, AccessControlList.StandardPolicy.PRIVATE);
        _conn.putObject(_testBucketName, obj3, AccessControlList.StandardPolicy.PRIVATE);
        listing = _conn.listObjects(_testBucketName, "prefix.", null, 0, ".");
        assertEquals("prefix.item0.", listing.getCommonPrefixes().get(0));
        listing = _conn.listObjects(_testBucketName, "prefix.item0.", null, 0, ".");
        assertEquals("prefix.item0.0", listing.getEntries().get(0).getKey());
        assertEquals("prefix.item0.1", listing.getEntries().get(1).getKey());
    }

    @Test
    public void testPutObject() throws Exception {
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);
    }

    @Test
    public void testCopyObject() throws Exception {
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("meta", "data");
        _fileObj.setMetadata(meta);
        _conn.putObject(_testBucketName, _fileObj, StandardPolicy.PRIVATE);
        _conn.copyObject(_fileObj.getKey(), "copyKey", _testBucketName);
        S3Metadata copiedMeta = _conn.getObjectMetadata(_testBucketName, "copyKey");
        assertArrayEquals(_fileObj.getMD5(), copiedMeta.getMD5());
        assertEquals(meta, copiedMeta.getMetadata());
    }

    @Test
    public void testCopyObjectWithNewMetadata() throws Exception {
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("meta", "data");
        meta.put("notincopy", "notcopied");
        _fileObj.setMetadata(meta);
        _conn.putObject(_testBucketName, _fileObj, StandardPolicy.PRIVATE);
        meta.clear();
        meta.put("meta", "replaceddata");
        meta.put("metameta", "newdata");
        _conn.copyObject(_fileObj.getKey(), "copyKey", _testBucketName, _testBucketName, StandardPolicy.PRIVATE, meta);
        S3Metadata copiedMeta = _conn.getObjectMetadata(_testBucketName, "copyKey");
        assertArrayEquals(_conn.getObjectMetadata(_testBucketName, _fileObj.getKey()).getMD5(), copiedMeta.getMD5());
        assertEquals(meta, copiedMeta.getMetadata());
    }

    @Test
    public void testMediaType() throws Exception {
        MediaType mediaType = new MediaType("mime", "encoding");
        S3Object obj = new S3ByteArrayObject("obj", new byte[5], mediaType);
        _conn.putObject(_testBucketName, obj);
        obj = _conn.getObject(_testBucketName, "obj");
        assertEquals(mediaType, obj.getMediaType());
    }

    @Test
    public void testPutObjectHeaders() throws Exception {
        String name = "Expires", date = "Sun, 17 Jan 2038 19:14:07 GMT";
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(name, date);
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PUBLIC_READ, headers);
        S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());
        S3ObjectTest.testEquals(_fileObj, obj);
        String url = "http://" + S3Utils.DEFAULT_HOST + "/" + _testBucketName + "/" + _fileObj.getKey();
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(url);
        client.executeMethod(method);
        Header header = method.getResponseHeader(name);
        String headerValue = header.toExternalForm().trim();
        assertTrue(headerValue.startsWith(name));
        assertTrue(headerValue.endsWith(date));
    }

    @Test
    public void testGetObject() throws Exception {
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);
        S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());
        S3ObjectTest.testEquals(_fileObj, obj);
        assertTrue(String.format("S3Object modified date: %d Original modified date: %d --- Is your clock correct?", obj.lastModified(), _fileObj.lastModified()), obj.lastModified() >= (_fileObj.lastModified() - (10 * 60 * 1000)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream input = obj.getInputStream();
        byte[] data = new byte[1024];
        int nread;
        while ((nread = input.read(data)) > 0) {
            output.write(data, 0, nread);
            if (output.size() > 2048) {
                break;
            }
        }
        input.close();
        assertEquals(TEST_DATA, output.toString("utf8"));
    }

    @Test
    public void testGetObjectMetadata() throws Exception {
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);
        S3Metadata obj = _conn.getObjectMetadata(_testBucketName, _fileObj.getKey());
        S3ObjectTest.testEquals(_fileObj, obj);
    }

    @Test
    public void testObjectMetadata() throws Exception {
        HashMap<String, String> metadata;
        metadata = new HashMap<String, String>();
        metadata.put("meta", "value whitespace");
        _fileObj.setMetadata(metadata);
        _conn.putObject(_testBucketName, _fileObj, AccessControlList.StandardPolicy.PRIVATE);
        S3Object obj = _conn.getObject(_testBucketName, _fileObj.getKey());
        S3ObjectTest.testEquals(_fileObj, obj);
        S3Metadata meta = _conn.getObjectMetadata(_testBucketName, _fileObj.getKey());
        assertEquals(metadata, meta.getMetadata());
    }

    @Test(expected = S3ServerException.SignatureDoesNotMatchException.class)
    public void testErrorHandling() throws Exception {
        S3Connection badConn = new S3Connection(S3TestConfig.getId(), "bad key");
        badConn.createBucket(_testBucketName);
    }

    @Test(expected = S3ServerException.NoSuchKeyException.class)
    public void testMissingObjectGet() throws Exception {
        _conn.getObject(_testBucketName, _fileObj.getKey());
    }

    @Test(expected = S3ServerException.S3Server404Exception.class)
    public void testMissingObjectHead() throws Exception {
        _conn.getObjectMetadata(_testBucketName, _fileObj.getKey());
    }

    /** Amazon S3 Authenticated Connection */
    private S3Connection _conn;

    /** Test bucket */
    protected String _testBucketName;

    /** Test file. */
    protected File _testFile;

    /** Test object. */
    protected S3FileObject _fileObj;

    /** Test data. */
    protected static final String TEST_DATA = "Hello, World!";

    /** Standard S3 storage class. */
    protected static final String STORAGE_CLASS = "STANDARD";
}
