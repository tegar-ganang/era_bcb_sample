package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.util.NamedXPathAwareChild;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.jdom.Element;
import org.apache.log4j.Logger;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Used to execute an HTTP request
 *
 * @author <a href="jonathan@indiekid.org">Jonathan Gerrish</a>
 */
public class HTTPPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(ExecutePublisher.class);

    private static final Set<String> METHODS = new HashSet<String>();

    static {
        METHODS.add("GET");
        METHODS.add("POST");
        METHODS.add("HEAD");
        METHODS.add("OPTIONS");
        METHODS.add("PUT");
        METHODS.add("DELETE");
        METHODS.add("TRACE");
    }

    private final Collection<NamedXPathAwareChild> parameters = new ArrayList<NamedXPathAwareChild>();

    private String urlString;

    private String requestMethod;

    private URL url;

    /**
     * Publish the results to a URL.
     *
     * @param cruisecontrolLog of the current build
     * @throws CruiseControlException - on any error
     */
    public void publish(final Element cruisecontrolLog) throws CruiseControlException {
        HttpURLConnection conn = null;
        try {
            if ("GET".equals(this.requestMethod)) {
                String dataSet = createDataSet(cruisecontrolLog);
                if (dataSet.length() > 0) {
                    dataSet = "?" + dataSet;
                }
                url = new URL(this.urlString + dataSet);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(this.requestMethod);
            } else {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(this.requestMethod);
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(createDataSet(cruisecontrolLog));
                wr.flush();
            }
            LOG.info("Sending " + this.requestMethod + " to " + this.urlString);
            conn.connect();
            LOG.info("Returned: " + conn.getResponseCode() + " : " + conn.getResponseMessage());
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(this.urlString, "urlString", this.getClass());
        try {
            url = new URL(this.urlString);
        } catch (MalformedURLException e) {
            ValidationHelper.fail("URL: " + this.urlString + " is not a valid URL", e);
        }
        ValidationHelper.assertTrue(METHODS.contains(this.requestMethod), "Request Method: " + this.requestMethod + " is mot a valid HTTP Request method");
        for (final NamedXPathAwareChild parameter : parameters) {
            parameter.validate();
        }
    }

    /**
     * Method to specify the URL used to publish the build result
     * @param url URL to which to publish build result
     */
    public void setUrl(String url) {
        this.urlString = url;
    }

    /**
     * Method to specify the HTTP Request method to use for this request
     * @param requestMethod String representing the HTTP Request method
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * An HTTP request attribute-value parameter pair, represented by a NamedXPathAwareChild to provide
     * for dymanic parameter value resolution from the either the cruisecontrol log or other file.
     * @return NamedXPathAwareChild representing attribute-value HTTP parameter pair.
     * @see NamedXPathAwareChild
     */
    @ManualChildName("parameter")
    public NamedXPathAwareChild createParameter() {
        NamedXPathAwareChild parameter = new NamedXPathAwareChild();
        parameters.add(parameter);
        return parameter;
    }

    /**
     * This method will build up a URL encoded string of all the parameters specified
     * for the HTTP request
     * @param cruisecontrolLog The cruise control log document.
     * @return URL Encoded string of the parameters
     * @throws CruiseControlException On any error
     */
    private String createDataSet(final Element cruisecontrolLog) throws CruiseControlException {
        final StringBuilder data = new StringBuilder();
        final Iterator<NamedXPathAwareChild> it = this.parameters.iterator();
        while (it.hasNext()) {
            final NamedXPathAwareChild parameter = it.next();
            final String name = parameter.getName();
            final String value = parameter.lookupValue(cruisecontrolLog);
            LOG.info("Adding request property: " + name + " = " + value);
            try {
                data.append(URLEncoder.encode(name, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new CruiseControlException("UTF-8 encoding not available", e);
            }
            if (it.hasNext()) {
                data.append("&");
            }
        }
        return data.toString();
    }
}
