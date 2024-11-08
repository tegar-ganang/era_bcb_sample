package fi.pyramus.plugin.maven;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;

public class HttpWagon extends StreamWagon {

    private HttpURLConnection putConnection;

    public static final int MAX_REDIRECTS = 10;

    private boolean useCache;

    private Properties httpHeaders;

    /**
   * Builds a complete URL string from the repository URL and the relative path passed.
   * 
   * @param path
   *          the relative path
   * @return the complete URL
   */
    private String buildUrl(String path) {
        final String repoUrl = getRepository().getUrl();
        path = path.replace(' ', '+');
        if (repoUrl.charAt(repoUrl.length() - 1) != '/') {
            return repoUrl + '/' + path;
        }
        return repoUrl + path;
    }

    public void fillInputData(InputData inputData) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = inputData.getResource();
        String visitingUrl = buildUrl(resource.getName());
        try {
            List<String> visitedUrls = new ArrayList<String>();
            for (int redirectCount = 0; redirectCount < MAX_REDIRECTS; redirectCount++) {
                if (visitedUrls.contains(visitingUrl)) {
                    throw new TransferFailedException("Cyclic http redirect detected. Aborting! " + visitingUrl);
                }
                visitedUrls.add(visitingUrl);
                URL url = new URL(visitingUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept-Encoding", "gzip");
                if (!useCache) {
                    urlConnection.setRequestProperty("Pragma", "no-cache");
                }
                addHeaders(urlConnection);
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new AuthorizationException("Access denied to: " + buildUrl(resource.getName()));
                }
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    visitingUrl = urlConnection.getHeaderField("Location");
                    continue;
                }
                InputStream is = urlConnection.getInputStream();
                String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
                boolean isGZipped = contentEncoding != null && "gzip".equalsIgnoreCase(contentEncoding);
                if (isGZipped) {
                    is = new GZIPInputStream(is);
                }
                inputData.setInputStream(is);
                resource.setLastModified(urlConnection.getLastModified());
                resource.setContentLength(urlConnection.getContentLength());
                break;
            }
        } catch (MalformedURLException e) {
            throw new ResourceDoesNotExistException("Invalid repository URL: " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new ResourceDoesNotExistException("Unable to locate resource in repository", e);
        } catch (IOException e) {
            StringBuilder message = new StringBuilder("Error transferring file: ");
            message.append(e.getMessage());
            message.append(" from " + visitingUrl);
            throw new TransferFailedException(message.toString(), e);
        }
    }

    private void addHeaders(HttpURLConnection urlConnection) {
        if (httpHeaders != null) {
            for (Iterator<?> i = httpHeaders.keySet().iterator(); i.hasNext(); ) {
                String header = (String) i.next();
                urlConnection.setRequestProperty(header, httpHeaders.getProperty(header));
            }
        }
    }

    public void fillOutputData(OutputData outputData) throws TransferFailedException {
        Resource resource = outputData.getResource();
        try {
            URL url = new URL(buildUrl(resource.getName()));
            putConnection = (HttpURLConnection) url.openConnection();
            addHeaders(putConnection);
            putConnection.setRequestMethod("PUT");
            putConnection.setDoOutput(true);
            outputData.setOutputStream(putConnection.getOutputStream());
        } catch (IOException e) {
            throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
        }
    }

    protected void finishPutTransfer(Resource resource, InputStream input, OutputStream output) throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException {
        try {
            int statusCode = putConnection.getResponseCode();
            switch(statusCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_ACCEPTED:
                case HttpURLConnection.HTTP_NO_CONTENT:
                    break;
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AuthorizationException("Access denied to: " + buildUrl(resource.getName()));
                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new ResourceDoesNotExistException("File: " + buildUrl(resource.getName()) + " does not exist");
                default:
                    throw new TransferFailedException("Failed to transfer file: " + buildUrl(resource.getName()) + ". Return code is: " + statusCode);
            }
        } catch (IOException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);
            throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
        }
    }

    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
    }

    public void closeConnection() throws ConnectionException {
        if (putConnection != null) {
            putConnection.disconnect();
        }
    }

    /**
  public List<String> getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
    InputData inputData = new InputData();

    if (destinationDirectory.length() > 0 && !destinationDirectory.endsWith("/")) {
      destinationDirectory += "/";
    }

    String url = buildUrl(destinationDirectory);

    Resource resource = new Resource(destinationDirectory);

    inputData.setResource(resource);

    fillInputData(inputData);

    InputStream is = inputData.getInputStream();

    if (is == null) {
      throw new TransferFailedException(url + " - Could not open input stream for resource: '" + resource + "'");
    }

    return HtmlFileListParser.parseFileList(url, is);
  }
**/
    public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        HttpURLConnection headConnection;
        try {
            URL url = new URL(buildUrl(new Resource(resourceName).getName()));
            headConnection = (HttpURLConnection) url.openConnection();
            addHeaders(headConnection);
            headConnection.setRequestMethod("HEAD");
            headConnection.setDoOutput(true);
            int statusCode = headConnection.getResponseCode();
            switch(statusCode) {
                case HttpURLConnection.HTTP_OK:
                    return true;
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AuthorizationException("Access denied to: " + url);
                case HttpURLConnection.HTTP_NOT_FOUND:
                    return false;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    throw new AuthorizationException("Access denied to: " + url);
                default:
                    throw new TransferFailedException("Failed to look for file: " + buildUrl(resourceName) + ". Return code is: " + statusCode);
            }
        } catch (IOException e) {
            throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
        }
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public Properties getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Properties httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    void setSystemProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.getProperties().remove(key);
        }
    }
}
