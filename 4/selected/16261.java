package edu.psu.citeseerx.updates;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import edu.psu.citeseerx.dao2.logic.CSXDAO;
import edu.psu.citeseerx.domain.Algorithm;
import edu.psu.citeseerx.domain.Document;
import edu.psu.citeseerx.utility.SafeText;

/**
 * Utilities for updating a Solr index to be consistent with the information
 * in the database.
 * @author Sumit Bathia
 * @version $Rev: 1241 $ $Date: 2010-05-07 16:43:23 -0400 (Fri, 07 May 2010) $
 */
public class AlgorithmIndexUpdater {

    private static final String expectedResponse = "<int name=\"status\">0</int>";

    private URL solrAlgorithmUpdateUrl;

    public void setSolrURL(String solrUpdateUrl) throws MalformedURLException {
        this.solrAlgorithmUpdateUrl = new URL(solrUpdateUrl);
    }

    private CSXDAO csxdao;

    public void setCSXDAO(CSXDAO csxdao) {
        this.csxdao = csxdao;
    }

    public void indexAll() {
        Date lastUpdate = csxdao.lastAlgorithmIndexTime();
        List<Algorithm> algorithmList = csxdao.getUpdatedAlgorithms(lastUpdate);
        StringBuffer xmlBuffer = new StringBuffer();
        xmlBuffer.append("<add>");
        for (Algorithm eachAlgorithm : algorithmList) {
            String doi = eachAlgorithm.getPaperIDForAlgorithm();
            Document doc = csxdao.getDocumentFromDB(doi);
            if (doc.isPublic()) {
                xmlBuffer.append("<doc>");
                addField(xmlBuffer, "id", Long.toString(eachAlgorithm.getID()));
                addField(xmlBuffer, "caption", eachAlgorithm.getCaption());
                addField(xmlBuffer, "synopsis", eachAlgorithm.getSynopsis());
                addField(xmlBuffer, "reftext", eachAlgorithm.getAlgorithmReference());
                addField(xmlBuffer, "page", Integer.toString(eachAlgorithm.getAlgorithmOccursInPage()));
                addField(xmlBuffer, "paperid", doi);
                addField(xmlBuffer, "year", doc.getDatum(Document.YEAR_KEY));
                addField(xmlBuffer, "ncites", Long.toString(doc.getNcites()));
                xmlBuffer.append("</doc>");
            }
        }
        xmlBuffer.append("</add>");
        try {
            sendPost(xmlBuffer.toString());
            sendCommit();
            sendOptimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        csxdao.updateAlgorithmIndexTime();
    }

    private void addField(StringBuffer buffer, String fieldName, String value) {
        buffer.append("<field name=\"");
        buffer.append(fieldName);
        buffer.append("\">");
        String newvalue = value;
        try {
            byte[] utf8Bytes = value.getBytes("UTF-8");
            newvalue = new String(utf8Bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buffer.append(SafeText.stripBadChars(newvalue));
        buffer.append("</field>");
    }

    private void sendOptimize() throws IOException {
        sendPost("<optimize/>");
    }

    private void sendPost(String str) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) solrAlgorithmUpdateUrl.openConnection();
        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
        }
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);
        conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        Writer wr = new OutputStreamWriter(conn.getOutputStream());
        try {
            pipe(new StringReader(str), wr);
        } catch (IOException e) {
            throw (e);
        } finally {
            try {
                wr.close();
            } catch (Exception e) {
            }
        }
        Reader reader = new InputStreamReader(conn.getInputStream());
        try {
            StringWriter output = new StringWriter();
            pipe(reader, output);
            checkExpectedResponse(output.toString());
        } catch (IOException e) {
            throw (e);
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    private static void pipe(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[1024];
        int read = 0;
        while ((read = reader.read(buf)) >= 0) {
            writer.write(buf, 0, read);
        }
        writer.flush();
    }

    private static void checkExpectedResponse(String response) throws IOException {
        if (response.indexOf(expectedResponse) < 0) {
            throw new IOException("Unexpected response from solr: " + response);
        }
    }

    private void sendCommit() throws IOException {
        sendPost("<commit waitFlush=\"false\" waitSearcher=\"false\"/>");
    }
}
