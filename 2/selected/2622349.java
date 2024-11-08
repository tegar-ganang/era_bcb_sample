package eu.planets_project.ifr.core.storage.impl.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager;
import eu.planets_project.ifr.core.storage.api.query.Query;
import eu.planets_project.ifr.core.storage.api.query.QueryString;
import eu.planets_project.ifr.core.storage.api.query.QueryValidationException;
import eu.planets_project.services.datatypes.DigitalObject;

/**
 * @author <a href="mailto:Andrew.Jackson@bl.uk">Andy Jackson</a>
 *
 */
public class GoogleDigitalObjectManager implements DigitalObjectManager {

    /** The following API key was created by Andy Jackson, and based on the domain http://testbed.planets-project.eu/ */
    private static final String GOOGLE_API_KEY = "ABQIAAAAebl1J8mSr980OAfetjy7WxSBULiB7hKs_Ki5pG5Go7xRjst3uBRQ7D0P9eJH2rt9PwxcTkTVJLdCcQ";

    /** The registered referrer for this API key. */
    private static final String GOOGLE_QUERY_REFERER = "http://testbed.planets-project.eu/";

    /**
     * {@inheritDoc}
     * @see eu.planets_project.ifr.core.storage.api.DigitalObjectManager#getQueryTypes()
     */
    public List<Class<? extends Query>> getQueryTypes() {
        List<Class<? extends Query>> qmodes = new ArrayList<Class<? extends Query>>();
        qmodes.add(QueryString.class);
        return qmodes;
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.ifr.core.storage.api.DigitalObjectManager#isWritable(java.net.URI)
     */
    public boolean isWritable(URI pdURI) {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.ifr.core.storage.api.DigitalObjectManager#list(java.net.URI, eu.planets_project.ifr.core.storage.api.query.Query)
     */
    public List<URI> list(URI pdURI, Query q) throws QueryValidationException {
        if (q instanceof QueryString) {
            QueryString qs = (QueryString) q;
            try {
                return this.executeQuery(pdURI, qs.getQuery());
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * This performs the actual query, and patches things up.
     * 
     * @param pdURI
     * @param qs
     * @return
     * @throws IOException
     */
    private List<URI> executeQuery(URI pdURI, String qs) throws IOException {
        URL url = new URL("http://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=" + qs + "&key=" + GOOGLE_API_KEY);
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Referer", GOOGLE_QUERY_REFERER);
        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        try {
            JSONObject json = new JSONObject(builder.toString());
            System.out.println("GOT: " + json.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.ifr.core.storage.api.DigitalObjectManager#list(java.net.URI)
     */
    public List<URI> list(URI pdURI) {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.ifr.core.storage.api.DigitalObjectManager#retrieve(java.net.URI)
     */
    public DigitalObject retrieve(URI pdURI) throws DigitalObjectNotFoundException {
        return null;
    }

    public URI storeAsNew(URI pdURI, DigitalObject digitalObject) throws eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException {
        throw new DigitalObjectNotStoredException("Storing not supported by this implementation.");
    }

    public URI storeAsNew(DigitalObject digitalObject) throws eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException {
        throw new DigitalObjectNotStoredException("Storing not supported by this implementation.");
    }

    public URI updateExisting(URI pdURI, DigitalObject digitalObject) throws eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException, eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotFoundException {
        throw new DigitalObjectNotStoredException("Storing not supported by this implementation.");
    }
}
