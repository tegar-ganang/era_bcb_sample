package es.randres.net;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XmlSerializer;

public class CatastroRetriever extends Thread {

    public static final String ISO_8859_1 = "ISO-8859-1";

    public static final String TITLE_SEARCH = "titulo";

    public static final String NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION";

    public static final String SUCCESS = "FINISHED";

    public static final String FAIL = "FAIL";

    public static final String AUTHOR_SEARCH = "nombre";

    public static final String NOT_FOUND = "NOT_FOUND";

    private static String LOGIN_WEB_PAGE = "http://www.sedecatastro.gob.es/";

    private static String INIT_WEB_PAGE = "https://www1.sedecatastro.gob.es/CYCBienInmueble/OVCBusqueda.aspx";

    private static String SEARCH_WEB_PAGE = "https://www1.sedecatastro.gob.es/CYCBienInmueble/OVCConCiud.aspx?UrbRus=R&RefC=42283A001054420000FR&del=42&mun=283";

    private static String PDF_WEB_PAGE = "https://www1.sedecatastro.gob.es/Cartografia/ImprimirCroquisyDatos.aspx?del=42&mun=283&cimp=-1&referer=https://www1.sedecatastro.gob.es/CYCBienInmueble/OVCBusqueda.aspx&refcat=";

    private HttpClient m_client;

    private HtmlCleaner m_cleaner = new HtmlCleaner();

    protected final PropertyChangeSupport m_propertyChangeSupport;

    private XmlSerializer serializer;

    private List<SearchParams> m_params;

    private String[] m_credentials;

    public CatastroRetriever(List<SearchParams> params) {
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        m_client = new HttpClient();
        m_params = params;
        serializer = new PrettyXmlSerializer(m_cleaner.getProperties());
    }

    @Override
    public void run() {
        try {
            init();
            for (SearchParams search : m_params) {
                searchSynopsis(search);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        GetMethod login = new GetMethod(LOGIN_WEB_PAGE);
        m_client.executeMethod(login);
        GetMethod getValidation = new GetMethod(INIT_WEB_PAGE);
        m_client.executeMethod(getValidation);
        m_credentials = getCredentials(getValidation.getResponseBodyAsStream());
    }

    private void searchSynopsis(SearchParams search) throws Exception {
        String targetReqId = "[ Retriever ]" + search.toString();
        m_propertyChangeSupport.firePropertyChange("TARGET_REQUEST", "", targetReqId);
        PostMethod getReference = new PostMethod(INIT_WEB_PAGE);
        NameValuePair[] data = { new NameValuePair("rdb_Tipo", "rdbLocalizacion"), new NameValuePair("rdb_UrbRus", "rbLRusticos"), new NameValuePair("txtPar", search.getParcela()), new NameValuePair("slcMunicipios", "TALVEILA"), new NameValuePair("slcProvincias", "42"), new NameValuePair("tipoBusqueda", "Alfa"), new NameValuePair("txtPol", search.getPoligono()), new NameValuePair("__EVENTVALIDATION", m_credentials[1]), new NameValuePair("__VIEWSTATE", m_credentials[0]) };
        getReference.setRequestBody(data);
        m_client.executeMethod(getReference);
        String refLocation = getReference.getResponseHeader("Location").getValue();
        String refCat = parseQuery(refLocation).get("RefC");
        System.out.println(refCat);
        PostMethod pdf = new PostMethod(PDF_WEB_PAGE + refCat);
        m_client.executeMethod(pdf);
        InputStream imageStream = pdf.getResponseBodyAsStream();
        BufferedInputStream bufferedInput = new BufferedInputStream(imageStream);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(new FileOutputStream(refCat + ".pdf"));
        byte[] buffer = new byte[1024 * 16];
        int read = 0;
        while ((read = bufferedInput.read(buffer)) != -1) {
            bufferedOutput.write(buffer, 0, read);
        }
        bufferedOutput.close();
    }

    public TreeMap<String, String> parseQuery(String query) {
        TreeMap<String, String> parameters = new TreeMap<String, String>();
        int index = query.indexOf("?");
        if (index != -1) {
            String qs = query.substring(index + 1);
            String pairs[] = qs.split("&");
            for (String pair : pairs) {
                String name;
                String value;
                int pos = pair.indexOf('=');
                if (pos == -1) {
                    name = pair;
                    value = null;
                } else {
                    try {
                        name = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
                        value = URLDecoder.decode(pair.substring(pos + 1, pair.length()), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException("No UTF-8");
                    }
                }
                parameters.put(name, value);
            }
        }
        return parameters;
    }

    private void showHeaders(PostMethod loginRequest) {
        for (Header h : loginRequest.getRequestHeaders()) {
            System.out.println(h.getName() + "= " + h.getValue());
        }
        System.out.println("========================================");
        for (Header h : loginRequest.getResponseHeaders()) {
            System.out.println(h.getName() + "= " + h.getValue());
        }
        for (NameValuePair pair : loginRequest.getParameters()) {
            System.out.println(pair.getName() + "= " + pair.getValue());
        }
        System.out.println(loginRequest.getStatusCode());
    }

    private String[] getCredentials(InputStream in) throws Exception {
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode node = cleaner.clean(in);
        String viewState = null;
        String eventValidation = null;
        String attName = "id";
        String attValue = "__VIEWSTATE";
        String eventAttValue = "__EVENTVALIDATION";
        boolean isRecursive = true;
        boolean isCaseSensitive = false;
        TagNode viewStateNode = node.findElementByAttValue(attName, attValue, isRecursive, isCaseSensitive);
        if (viewStateNode != null) {
            viewState = viewStateNode.getAttributeByName("value");
        }
        TagNode eventValidationNode = node.findElementByAttValue(attName, eventAttValue, isRecursive, isCaseSensitive);
        if (eventValidationNode != null) {
            eventValidation = eventValidationNode.getAttributeByName("value");
        }
        return new String[] { viewState, eventValidation };
    }

    private NameValuePair[] setSearchParameters(String type, String value) {
        NameValuePair[] data = { new NameValuePair("txtRC", type), new NameValuePair("rdb_Tipo", "rbRC"), new NameValuePair("rdb_Tipo", "rbRC") };
        return data;
    }

    private void extractData(InputStream in) throws Exception {
        boolean valid = true;
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode node = cleaner.clean(in);
        String attName = "id";
        String attValue = "TblPrincipal";
        boolean isRecursive = true;
        boolean isCaseSensitive = false;
        TagNode eventView = node.findElementByAttValue(attName, attValue, isRecursive, isCaseSensitive);
        if (eventView != null) {
            System.out.println(getCell(eventView, 3, 2));
            System.out.println(serializer.getAsString(eventView));
        }
    }

    private String getCell(TagNode table, int row, int col) {
        List rows = table.getElementListByName("tr", true);
        TagNode rowNode = (TagNode) rows.get(row);
        List cells = table.getElementListByName("td", true);
        TagNode cellNode = (TagNode) cells.get(col);
        return cellNode.getText().toString();
    }

    private PostMethod generateRequest(NameValuePair[] params) {
        PostMethod postRequest = new PostMethod(LOGIN_WEB_PAGE);
        postRequest.setRequestBody(params);
        return postRequest;
    }

    /**Add a property change listener for a specific property.
	  @param propertyName The name of the property to listen on.
	  @param listener The <code>PropertyChangeListener</code>
	      to be added.
	 */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static void main(String[] args) {
        long tstart = System.currentTimeMillis();
        int numberOfRetrievers = 5;
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
            SSLContext.setDefault(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<SearchParams> params = new Vector<SearchParams>();
        params.add(new SearchParams("1", "5542"));
        params.add(new SearchParams("1", "5514"));
        params.add(new SearchParams("1", "5706"));
        params.add(new SearchParams("1", "5733"));
        params.add(new SearchParams("1", "5800"));
        params.add(new SearchParams("1", "5853"));
        params.add(new SearchParams("1", "5914"));
        params.add(new SearchParams("1", "5956"));
        params.add(new SearchParams("1", "6004"));
        params.add(new SearchParams("1", "6014"));
        params.add(new SearchParams("1", "6022"));
        params.add(new SearchParams("1", "6039"));
        params.add(new SearchParams("1", "6070"));
        params.add(new SearchParams("2", "5216"));
        params.add(new SearchParams("1", "9"));
        params.add(new SearchParams("2", "37"));
        params.add(new SearchParams("2", "137"));
        params.add(new SearchParams("2", "229"));
        params.add(new SearchParams("3", "188"));
        params.add(new SearchParams("1", "9"));
        CatastroRetriever sr = new CatastroRetriever(params);
        sr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
            }
        });
        sr.start();
    }
}
