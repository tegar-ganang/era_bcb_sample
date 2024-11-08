package org.grobid.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.grobid.core.exceptions.GROBIDServiceException;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.TextUtilities;
import grobid.service.exchange.GROBIDJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grobid.service.GROBIDService;
import org.junit.Before;

public class GROBIDServiceTest {

    private static Log logger = LogFactory.getLog(GROBIDServiceTest.class);

    private static HttpServer server = null;

    private String host = null;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Before
    public void setUp() throws Exception {
        this.readProperties();
        if (this.createHost) {
            try {
                if (server == null) {
                    server = HttpServerFactory.create(this.getHost());
                    server.start();
                    logger.info("server for GROBIDService started on " + this.getHost() + "...");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("run tests on host: " + this.getHost());
    }

    private String propFileName = null;

    private boolean createHost = true;

    private void readProperties() throws FileNotFoundException, IOException {
        File propFile = new File("./src/test/resources/grobidHost_private.properties");
        if (!propFile.exists()) {
            propFile = new File("./src/test/resources/grobidHost.properties");
            if (!propFile.exists()) {
                throw new GrobidException("Cannot run tests for grobid service, because the property file for" + " grobid tests does not exist.");
            }
        }
        propFileName = propFile.getAbsolutePath();
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(propFile);
            props.load(in);
        } finally {
            in.close();
        }
        if ((props.getProperty("grobidHost") != null) && (!props.getProperty("grobidHost").equals(""))) this.setHost(props.getProperty("grobidHost")); else fail("cannot find the host for the grobidService, please check configuration file: " + propFileName);
        if ((props.getProperty("createHost") != null) && (!props.getProperty("createHost").equals(""))) {
            if (props.getProperty("createHost").equalsIgnoreCase("no")) createHost = false;
        }
    }

    private String getGROBIDHost() {
        return (this.getHost() + "grobid/");
    }

    private String resourceDir = "./src/test/resources/";

    private String tmpDir = null;

    public File getResourceDir() {
        File file = new File(resourceDir);
        if (!file.exists()) {
            if (!file.mkdirs()) throw new GROBIDServiceException("Cannot create folder for resources.");
        }
        return (file);
    }

    public File getTMPDir() {
        GrobidProperties.init();
        tmpDir = System.getProperty(GrobidProperties.PROP_TMP_PATH);
        if (tmpDir == null) throw new GrobidException("Cannot start test, because tmp folder is not set.");
        File file = new File(tmpDir);
        if (!file.exists()) {
            if (!file.mkdirs()) throw new GROBIDServiceException("Cannot create temprorary folder.");
        }
        return (file);
    }

    /**
	 * Checks if necessary environment variable is set. If this test fails, the service cannot run correctly. 
	 */
    public void testEnvVariable() {
        logger.debug("testEnvVariable()...");
        assertNotNull(System.getenv(GrobidProperties.ENV_GROBID_HOME));
        assertTrue(!System.getenv(GrobidProperties.ENV_GROBID_HOME).equals(""));
    }

    /**
	 * Checks if the service is alive, if this test fails, all the other will also fail.
	 */
    public void testIsAlive() {
        logger.debug("testIsAlive()...");
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        response = service.path("isAlive").accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
        assertEquals(200, response.getStatus());
        String isAlive = response.getEntity(String.class);
        assertTrue(isAlive.equalsIgnoreCase("true"));
    }

    private GROBIDJob createJob() {
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        response = service.path("jobs").accept(MediaType.APPLICATION_XML).post(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return (response.getEntity(GROBIDJob.class));
    }

    private GROBIDJob getJob(URI jobUri) {
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        service = Client.create().resource(jobUri);
        response = service.accept(MediaType.TEXT_XML).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return (response.getEntity(GROBIDJob.class));
    }

    private void deleteJob(URI jobUri) {
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        service = Client.create().resource(jobUri);
        response = service.delete(ClientResponse.class);
        assertEquals(200, response.getStatus());
    }

    private void uploadPDFDocument(URI uploadURI, File pdfFile) {
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        assertTrue("Cannot run the test, because the sample file '" + pdfFile + "' does not exists.", pdfFile.exists());
        FormDataMultiPart form = new FormDataMultiPart();
        form.field("fileContent", pdfFile, MediaType.MULTIPART_FORM_DATA_TYPE);
        service = Client.create().resource(uploadURI);
        response = service.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML).post(ClientResponse.class, form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    /**
	 * Checks if the creation of a job works well.
	 */
    public void testCreateJob() {
        logger.debug("testCreateJob()...");
        GROBIDJob job = createJob();
        assertNotNull(job);
        assertNotNull(job.getJobId());
        assertNotNull(job.getJobPW());
        assertNotNull(job.getDocumentOriginURI());
    }

    /**
	 * Checks if the creation of a job works well.
	 */
    public void testGetJob() {
        logger.debug("testCreateJob()...");
        GROBIDJob job1 = createJob();
        GROBIDJob job2 = this.getJob(job1.getJobURI());
        assertNotNull(job2);
        assertEquals(job1, job2);
    }

    public void testDeleteJob() {
        GROBIDJob job1 = this.createJob();
        GROBIDJob job2 = this.getJob(job1.getJobURI());
        assertEquals(job1, job2);
        this.deleteJob(job1.getJobURI());
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        service = Client.create().resource(job1.getJobURI());
        response = service.accept(MediaType.TEXT_XML).get(ClientResponse.class);
        assertEquals("the status code 404 shall be returned, but it was '" + response.getStatus() + "'", Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    public void testUploadDownloadOriginDocument() throws IOException {
        GROBIDJob job = this.createJob();
        File pdfFile = new File(this.getResourceDir().getAbsoluteFile() + "/sample1/sample.pdf");
        this.uploadPDFDocument(job.getDocumentOriginURI(), pdfFile);
        {
            Client create = Client.create();
            WebResource service = create.resource(getGROBIDHost());
            ClientResponse response = null;
            service = Client.create().resource(job.getDocumentOriginURI());
            response = service.accept("application/pdf").get(ClientResponse.class);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            File d_pdfFile = new File(this.getTMPDir().getAbsoluteFile() + "/sample1_d.pdf");
            InputStream inputStream = response.getEntity(InputStream.class);
            OutputStream out = null;
            try {
                out = new FileOutputStream(d_pdfFile);
                byte buf[] = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
            } catch (IOException e) {
                throw e;
            } finally {
                try {
                    if (out != null) out.close();
                    inputStream.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            assertTrue(pdfFile.exists());
        }
    }

    private void downloadTEIFile(URI uri, File teiFile) throws Exception {
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        service = Client.create().resource(uri);
        response = service.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
        System.out.println("downloading tei file from '" + uri + "'.");
        System.out.println("status: " + response.getStatus());
        if (Status.ACCEPTED.getStatusCode() == response.getStatus()) {
            int counter = 50;
            while ((counter != 0) && (Status.OK.getStatusCode() != response.getStatus())) {
                response = service.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
                counter--;
                Thread.sleep(1000);
            }
        }
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        InputStream inputStream = response.getEntity(InputStream.class);
        OutputStream out = null;
        try {
            out = new FileOutputStream(teiFile);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (out != null) out.close();
                inputStream.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }

    public void testDownloadTeiFullDocument() throws Exception {
        GROBIDJob job = this.createJob();
        File pdfFile = new File(this.getResourceDir().getAbsoluteFile() + "/sample2/sample.pdf");
        this.uploadPDFDocument(job.getDocumentOriginURI(), pdfFile);
        File d_teiFile = new File(this.getTMPDir().getAbsoluteFile() + "/sample2_d.tei");
        this.downloadTEIFile(job.getDocumentTEIFullURI(), d_teiFile);
        assertTrue(d_teiFile.exists());
        System.out.println("tei fulltext:");
        System.out.println(GROBIDService.xmlFileToString(d_teiFile));
    }

    public void testDownloadTeiHeaderDocument() throws Exception {
        GROBIDJob job = this.createJob();
        File pdfFile = new File(this.getResourceDir().getAbsoluteFile() + "/sample3/sample.pdf");
        this.uploadPDFDocument(job.getDocumentOriginURI(), pdfFile);
        File d_teiFile = new File(this.getTMPDir().getAbsoluteFile() + "/sample3_d.tei");
        this.downloadTEIFile(job.getDocumentTEIAbstractURI(), d_teiFile);
        assertTrue(d_teiFile.exists());
        System.out.println("tei header:");
        System.out.println(GROBIDService.xmlFileToString(d_teiFile));
    }

    public void testUploadDownloadTeiCorrectedFullDocument() throws Exception {
        GROBIDJob job = this.createJob();
        File pdfFile = new File(this.getResourceDir().getAbsoluteFile() + "/sample4/sample.pdf");
        this.uploadPDFDocument(job.getDocumentOriginURI(), pdfFile);
        File d_teiFile = new File(this.getTMPDir().getAbsoluteFile() + "/sample4_d.tei");
        this.downloadTEIFile(job.getDocumentTEIFullURI(), d_teiFile);
        assertTrue(d_teiFile.exists());
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        String correctedXML = "<correctedTei/>\n";
        {
            response = service.uri(job.getDocumentTEICorrectedURI()).type(MediaType.APPLICATION_XML).post(ClientResponse.class, correctedXML);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
        {
            response = service.uri(job.getDocumentTEICorrectedURI()).accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String correctedTeiDocument = response.getEntity(String.class);
            assertEquals(correctedXML, correctedTeiDocument);
        }
    }

    /**
	 * test the synchronous fully state less rest call
	 */
    public void testFullyRestLessHeaderDocument() throws Exception {
        File pdfFile = new File(this.getResourceDir().getAbsoluteFile() + "/sample4/sample.pdf");
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        assertTrue("Cannot run the test, because the sample file '" + pdfFile + "' does not exists.", pdfFile.exists());
        FormDataMultiPart form = new FormDataMultiPart();
        form.field("fileContent", pdfFile, MediaType.MULTIPART_FORM_DATA_TYPE);
        System.out.println("calling " + this.getHost() + "grobid/processHeaderDocument");
        service = Client.create().resource(this.getHost() + "grobid/processHeaderDocument");
        response = service.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML).post(ClientResponse.class, form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        InputStream inputStream = response.getEntity(InputStream.class);
        String tei = TextUtilities.convertStreamToString(inputStream);
        System.out.println(tei);
    }

    /**
	 *  Test the synchronous fully state less rest call
	 */
    public void testFullyRestLessFulltextDocument() throws Exception {
        File pdfFile = new File(this.getResourceDir().getAbsoluteFile() + "/sample4/sample.pdf");
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        assertTrue("Cannot run the test, because the sample file '" + pdfFile + "' does not exists.", pdfFile.exists());
        FormDataMultiPart form = new FormDataMultiPart();
        form.field("fileContent", pdfFile, MediaType.MULTIPART_FORM_DATA_TYPE);
        System.out.println("calling " + this.getHost() + "grobid/processFulltextDocument");
        service = Client.create().resource(this.getHost() + "grobid/processFulltextDocument");
        response = service.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML).post(ClientResponse.class, form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        InputStream inputStream = response.getEntity(InputStream.class);
        String tei = TextUtilities.convertStreamToString(inputStream);
        System.out.println(tei);
    }

    /**
	 *  Test the synchronous state less rest call for dates
	 */
    public void testRestDate() throws Exception {
        String date = "November 14 1999";
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("date", date);
        service = Client.create().resource(this.getHost() + "grobid/processDate");
        response = service.post(ClientResponse.class, formData);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String postResp = response.getEntity(String.class);
        System.out.println(postResp);
    }

    /**
	 *  Test the synchronous state less rest call for author sequences in headers
	 */
    public void testRestNamesHeader() throws Exception {
        String names = "Ahmed Abu-Rayyan *,a, Qutaiba Abu-Salem b, Norbert Kuhn * ,b, Cäcilia Maichle-Mößmer b";
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("names", names);
        service = Client.create().resource(this.getHost() + "grobid/processNamesHeader");
        response = service.post(ClientResponse.class, formData);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String postResp = response.getEntity(String.class);
        System.out.println(postResp);
    }

    /**
	 *  Test the synchronous state less rest call for author sequences in citations
	 */
    public void testRestNamesCitations() throws Exception {
        String names = "Marc Shapiro and Susan Horwitz";
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("names", names);
        service = Client.create().resource(this.getHost() + "grobid/processNamesCitation");
        response = service.post(ClientResponse.class, formData);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String postResp = response.getEntity(String.class);
        System.out.println(postResp);
    }

    /**
	 *  Test the synchronous state less rest call for affiliation + address blocks
	 */
    public void testRestAffiliations() throws Exception {
        String affiliations = "Atomic Physics Division, Department of Atomic Physics and Luminescence, " + "Faculty of Applied Physics and Mathematics, Gdansk University of " + "Technology, Narutowicza 11/12, 80-233 Gdansk, Poland";
        Client create = Client.create();
        WebResource service = create.resource(getGROBIDHost());
        ClientResponse response = null;
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("affiliations", affiliations);
        service = Client.create().resource(this.getHost() + "grobid/processAffiliations");
        response = service.post(ClientResponse.class, formData);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String postResp = response.getEntity(String.class);
        System.out.println(postResp);
    }
}
