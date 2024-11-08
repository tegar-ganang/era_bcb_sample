package utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Query;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.acl.AclEntry;
import com.google.gdata.data.acl.AclFeed;
import com.google.gdata.data.acl.AclRole;
import com.google.gdata.data.acl.AclScope;
import com.google.gdata.data.docs.DocumentEntry;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.docs.FolderEntry;
import com.google.gdata.data.docs.PresentationEntry;
import com.google.gdata.data.docs.RevisionFeed;
import com.google.gdata.data.docs.SpreadsheetEntry;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class GoogleDocService {

    public DocsService service;

    public GoogleService spreadsheetsService;

    public static final String DEFAULT_AUTH_PROTOCOL = "https";

    public static final String DEFAULT_AUTH_HOST = "docs.google.com";

    public static final String DEFAULT_PROTOCOL = "http";

    public static final String DEFAULT_HOST = "docs.google.com";

    public static final String SPREADSHEETS_SERVICE_NAME = "wise";

    public static final String SPREADSHEETS_HOST = "spreadsheets.google.com";

    private final String URL_FEED = "/feeds";

    private final String URL_DOWNLOAD = "/download";

    private final String URL_DOCLIST_FEED = "/private/full";

    private final String URL_DEFAULT = "/default";

    private final String URL_FOLDERS = "/contents";

    private final String URL_ACL = "/acl";

    private final String URL_REVISIONS = "/revisions";

    private final String URL_CATEGORY_DOCUMENT = "/-/document";

    private final String URL_CATEGORY_SPREADSHEET = "/-/spreadsheet";

    private final String URL_CATEGORY_PDF = "/-/pdf";

    private final String URL_CATEGORY_PRESENTATION = "/-/presentation";

    private final String URL_CATEGORY_STARRED = "/-/starred";

    private final String URL_CATEGORY_TRASHED = "/-/trashed";

    private final String URL_CATEGORY_FOLDER = "/-/folder";

    private final String URL_CATEGORY_EXPORT = "/Export";

    private final String PARAMETER_SHOW_FOLDERS = "showfolders=true";

    private String applicationName;

    private String authProtocol;

    private String authHost;

    private String protocol;

    private String host;

    private String username;

    private String password;

    private String authSubToken;

    private final Map<String, String> DOWNLOAD_DOCUMENT_FORMATS;

    {
        DOWNLOAD_DOCUMENT_FORMATS = new HashMap<String, String>();
        DOWNLOAD_DOCUMENT_FORMATS.put("doc", "doc");
        DOWNLOAD_DOCUMENT_FORMATS.put("txt", "txt");
        DOWNLOAD_DOCUMENT_FORMATS.put("odt", "odt");
        DOWNLOAD_DOCUMENT_FORMATS.put("pdf", "pdf");
        DOWNLOAD_DOCUMENT_FORMATS.put("png", "png");
        DOWNLOAD_DOCUMENT_FORMATS.put("rtf", "rtf");
        DOWNLOAD_DOCUMENT_FORMATS.put("html", "html");
        DOWNLOAD_DOCUMENT_FORMATS.put("zip", "zip");
    }

    private final Map<String, String> DOWNLOAD_PRESENTATION_FORMATS;

    {
        DOWNLOAD_PRESENTATION_FORMATS = new HashMap<String, String>();
        DOWNLOAD_PRESENTATION_FORMATS.put("pdf", "pdf");
        DOWNLOAD_PRESENTATION_FORMATS.put("png", "png");
        DOWNLOAD_PRESENTATION_FORMATS.put("ppt", "ppt");
        DOWNLOAD_PRESENTATION_FORMATS.put("swf", "swf");
        DOWNLOAD_PRESENTATION_FORMATS.put("txt", "txt");
    }

    private final Map<String, String> DOWNLOAD_SPREADSHEET_FORMATS;

    {
        DOWNLOAD_SPREADSHEET_FORMATS = new HashMap<String, String>();
        DOWNLOAD_SPREADSHEET_FORMATS.put("xls", "xls");
        DOWNLOAD_SPREADSHEET_FORMATS.put("ods", "ods");
        DOWNLOAD_SPREADSHEET_FORMATS.put("pdf", "pdf");
        DOWNLOAD_SPREADSHEET_FORMATS.put("csv", "csv");
        DOWNLOAD_SPREADSHEET_FORMATS.put("tsv", "tsv");
        DOWNLOAD_SPREADSHEET_FORMATS.put("html", "html");
    }

    /**
	 * Constructor.
	 * 
	 * @param applicationName name of the application.
	 * 
	 * @throws RuntimeException
	 */
    public GoogleDocService(String applicationName) throws Exception {
        this(applicationName, DEFAULT_AUTH_PROTOCOL, DEFAULT_AUTH_HOST, DEFAULT_PROTOCOL, DEFAULT_HOST);
    }

    /**
	 * Constructor
	 * 
	 * @param applicationName name of the application
	 * @param authProtocol the protocol to use for authentication
	 * @param authHost the host to use for authentication
	 * @param protocol the protocol to use for the http calls.
	 * @param host the host that contains the feeds
	 * 
	 * @throws RuntimeException
	 */
    public GoogleDocService(String applicationName, String authProtocol, String authHost, String protocol, String host) throws RuntimeException {
        if (authProtocol == null || authHost == null || protocol == null || host == null) {
            throw new RuntimeException("null passed in required parameters");
        }
        service = new DocsService(applicationName);
        spreadsheetsService = new GoogleService(SPREADSHEETS_SERVICE_NAME, applicationName);
        this.applicationName = applicationName;
        this.authProtocol = authProtocol;
        this.authHost = authHost;
        this.protocol = protocol;
        this.host = host;
    }

    /**
	 * Set user credentials based on a username and password.
	 * 
	 * @param user username to log in with.
	 * @param pass password for the user logging in.
	 * 
	 * @throws AuthenticationException
	 * @throws RuntimeException
	 */
    public void login(String user, String pass) throws AuthenticationException, RuntimeException {
        if (user == null || pass == null) {
            throw new RuntimeException("null login credentials");
        }
        this.username = user;
        this.password = pass;
        this.authSubToken = "";
        service.setUserCredentials(user, pass);
        spreadsheetsService.setUserCredentials(user, pass);
    }

    /**
	 * Allow a user to login using an AuthSub token.
	 * 
	 * @param token the token to be used when logging in.
	 * 
	 * @throws AuthenticationException
	 * @throws RuntimeException
	 */
    public void loginWithAuthSubToken(String token) throws AuthenticationException, RuntimeException {
        if (token == null) {
            throw new RuntimeException("null login credentials");
        }
        this.authSubToken = token;
        this.username = "";
        this.password = "";
        service.setAuthSubToken(token);
        spreadsheetsService.setAuthSubToken(token);
    }

    /**
	 * Create a new item in the DocList.
	 * 
	 * @param title the title of the document to be created.
	 * @param type the type of the document to be created. One of "spreadsheet", "presentation", or "document".
	 * 
	 * @throws RuntimeException
	 * @throws ServiceException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    public DocumentListEntry createNew(String title, String type) throws Exception {
        if (title == null || type == null) {
            throw new RuntimeException("null title or type");
        }
        DocumentListEntry newEntry = null;
        if (type.equals("document")) {
            newEntry = new DocumentEntry();
        } else if (type.equals("presentation")) {
            newEntry = new PresentationEntry();
        } else if (type.equals("spreadsheet")) {
            newEntry = new SpreadsheetEntry();
        } else if (type.equals("folder")) {
            newEntry = new FolderEntry();
        }
        newEntry.setTitle(new PlainTextConstruct(title));
        return service.insert(buildUrl(URL_DEFAULT + URL_DOCLIST_FEED), newEntry);
    }

    /**
	 * Gets a feed containing the documents.
	 * 
	 * @param category what types of documents to list: "all": lists all the doc objects (documents, spreadsheets, presentations) "folders": lists all doc objects including folders. "documents": lists
	 * only documents. "spreadsheets": lists only spreadsheets. "pdfs": lists only pdfs. "presentations": lists only presentations. "starred": lists only starred objects. "trashed": lists trashed
	 * objects.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public DocumentListFeed getDocsListFeed(String category) throws Exception {
        if (category == null) {
            throw new RuntimeException("null category");
        }
        URL url;
        if (category.equals("all")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED);
        } else if (category.equals("folders")) {
            String[] parameters = { PARAMETER_SHOW_FOLDERS };
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_FOLDER, parameters);
        } else if (category.equals("documents")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_DOCUMENT);
        } else if (category.equals("spreadsheets")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_SPREADSHEET);
        } else if (category.equals("pdfs")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_PDF);
        } else if (category.equals("presentations")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_PRESENTATION);
        } else if (category.equals("starred")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_STARRED);
        } else if (category.equals("trashed")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_TRASHED);
        } else {
            return null;
        }
        return service.getFeed(url, DocumentListFeed.class);
    }

    /**
	 * Gets the entry for the provided object id.
	 * 
	 * @param resourceId the resource id of the object to fetch an entry for.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public DocumentListEntry getDocsListEntry(String resourceId) throws Exception {
        if (resourceId == null) {
            throw new RuntimeException("null resourceId");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId);
        return service.getEntry(url, DocumentListEntry.class);
    }

    /**
	 * Gets the feed for all the objects contained in a folder.
	 * 
	 * @param folderResourceId the resource id of the folder to return the feed for the contents.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public DocumentListFeed getFolderDocsListFeed(String folderResourceId) throws Exception {
        if (folderResourceId == null) {
            throw new RuntimeException("null folderResourceId");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderResourceId + URL_FOLDERS);
        return service.getFeed(url, DocumentListFeed.class);
    }

    /**
	 * Gets a feed containing the documents.
	 * 
	 * @param resourceId the resource id of the object to fetch revisions for.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public RevisionFeed getRevisionsFeed(String resourceId) throws Exception {
        if (resourceId == null) {
            throw new RuntimeException("null resourceId");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_REVISIONS);
        return service.getFeed(url, RevisionFeed.class);
    }

    /**
	 * Search the documents, and return a feed of docs that match.
	 * 
	 * @param searchParameters parameters to be used in searching criteria.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public DocumentListFeed search(Map<String, String> searchParameters) throws Exception {
        return search(searchParameters, null);
    }

    /**
	 * Search the documents, and return a feed of docs that match.
	 * 
	 * @param searchParameters parameters to be used in searching criteria. accepted parameters are: "q": Typical search query "alt": "author": "updated-min": Lower bound on the last time a document'
	 * content was changed. "updated-max": Upper bound on the last time a document' content was changed. "edited-min": Lower bound on the last time a document was edited by the current user. This
	 * value corresponds to the app:edited value in the Atom entry, which represents changes to the document's content or metadata. "edited-max": Upper bound on the last time a document was edited by
	 * the current user. This value corresponds to the app:edited value in the Atom entry, which represents changes to the document's content or metadata. "title": Specifies the search terms for the
	 * title of a document. This parameter used without title-exact will only submit partial queries, not exact queries. "title-exact": Specifies whether the title query should be taken as an exact
	 * string. Meaningless without title. Possible values are true and false. "opened-min": Bounds on the last time a document was opened by the current user. Use the RFC 3339 timestamp format. For
	 * example: 2005-08-09T10:57:00-08:00 "opened-max": Bounds on the last time a document was opened by the current user. Use the RFC 3339 timestamp format. For example: 2005-08-09T10:57:00-08:00
	 * "owner": Searches for documents with a specific owner. Use the email address of the owner. "writer": Searches for documents which can be written to by specific users. Use a single email address
	 * or a comma separated list of email addresses. "reader": Searches for documents which can be read by specific users. Use a single email address or a comma separated list of email addresses.
	 * "showfolders": Specifies whether the query should return folders as well as documents. Possible values are true and false.
	 * @param category define the category to search. (documents, spreadsheets, presentations, starred, trashed, folders)
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public DocumentListFeed search(Map<String, String> searchParameters, String category) throws Exception {
        if (searchParameters == null) {
            throw new RuntimeException("searchParameters null");
        }
        URL url;
        if (category == null || category.equals("")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED);
        } else if (category.equals("documents")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_DOCUMENT);
        } else if (category.equals("spreadsheets")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_SPREADSHEET);
        } else if (category.equals("presentations")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_PRESENTATION);
        } else if (category.equals("starred")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_STARRED);
        } else if (category.equals("trashed")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_TRASHED);
        } else if (category.equals("folders")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_FOLDER);
        } else {
            throw new RuntimeException("invaild category");
        }
        Query qry = new Query(url);
        for (String key : searchParameters.keySet()) {
            qry.setStringCustomParameter(key, searchParameters.get(key));
        }
        return service.query(qry, DocumentListFeed.class);
    }

    public DocumentListEntry upload(InputStream input, String mediaType, String title) throws Exception, ServiceException, RuntimeException {
        if (input == null || title == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        DocumentEntry newDocument = new DocumentEntry();
        MediaSource source = new MediaStreamSource(input, mediaType);
        newDocument.setMediaSource(source);
        newDocument.setTitle(new PlainTextConstruct(title));
        return service.insert(buildUrl(URL_DEFAULT + URL_DOCLIST_FEED), newDocument);
    }

    /**
	 * Trash an object.
	 * 
	 * @param resourceId the resource id of object to be trashed.
	 * @param delete true to delete the permanently, false to move it to the trash.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public void trashObject(String resourceId, boolean delete) throws Exception {
        if (resourceId == null) {
            throw new RuntimeException("null resourceId");
        }
        String feedUrl = URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId;
        if (delete) {
            feedUrl += "?delete=true";
        }
        service.delete(buildUrl(feedUrl), getDocsListEntry(resourceId).getEtag());
    }

    /**
	 * Remove an object from a folder.
	 * 
	 * @param resourceId the resource id of an object to be removed from the folder.
	 * @param folderResourceId the resource id of the folder to remove the object from.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public void removeFromFolder(String resourceId, String folderResourceId) throws Exception {
        if (resourceId == null || folderResourceId == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderResourceId + URL_FOLDERS + "/" + resourceId);
        service.delete(url, getDocsListEntry(resourceId).getEtag());
    }

    public void write(URL exportUrl, OutputStream output) throws Exception {
        if (exportUrl == null || output == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        MediaContent mc = new MediaContent();
        mc.setUri(exportUrl.toString());
        MediaSource ms = service.getMedia(mc);
        InputStream input = ms.getInputStream();
        IOUtils.copy(input, output);
    }

    public void downloadPresentation(String resourceId, OutputStream outputStream, String format) throws Exception {
        if (resourceId == null || outputStream == null || format == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        String[] parameters = { "docID=" + resourceId, "exportFormat=" + format };
        URL url = buildUrl(URL_DOWNLOAD + "/presentations" + URL_CATEGORY_EXPORT, parameters);
        write(url, outputStream);
    }

    /**
	 * Moves a object to a folder.
	 * 
	 * @param resourceId the resource id of the object to be moved to the folder.
	 * @param folderId the id of the folder to move the object to.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public DocumentListEntry moveObjectToFolder(String resourceId, String folderId) throws Exception {
        if (resourceId == null || folderId == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        DocumentListEntry doc = new DocumentListEntry();
        doc.setId(buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId).toString());
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderId + URL_FOLDERS);
        return service.insert(url, doc);
    }

    /**
	 * Gets the access control list for a object.
	 * 
	 * @param resourceId the resource id of the object to retrieve the ACL for.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public AclFeed getAclFeed(String resourceId) throws Exception {
        if (resourceId == null) {
            throw new RuntimeException("null resourceId");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL);
        return service.getFeed(url, AclFeed.class);
    }

    /**
	 * Add an ACL role to an object.
	 * 
	 * @param role the role of the ACL to be added to the object.
	 * @param scope the scope for the ACL.
	 * @param resourceId the resource id of the object to set the ACL for.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public AclEntry addAclRole(AclRole role, AclScope scope, String resourceId) throws Exception {
        if (role == null || scope == null || resourceId == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        AclEntry entry = new AclEntry();
        entry.setRole(role);
        entry.setScope(scope);
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL);
        return service.insert(url, entry);
    }

    /**
	 * Change the ACL role of a file.
	 * 
	 * @param role the new role of the ACL to be updated.
	 * @param scope the new scope for the ACL.
	 * @param resourceId the resource id of the object to be updated.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public AclEntry changeAclRole(AclRole role, AclScope scope, String resourceId) throws Exception {
        if (role == null || scope == null || resourceId == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL);
        return service.update(url, scope, role);
    }

    /**
	 * Remove an ACL role from a object.
	 * 
	 * @param scope scope of the ACL to be removed.
	 * @param email email address to remove the role of.
	 * @param resourceId the resource id of the object to remove the role from.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ServiceException
	 * @throws RuntimeException
	 */
    public void removeAclRole(String scope, String email, String resourceId) throws Exception {
        if (scope == null || email == null || resourceId == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL + "/" + scope + "%3A" + email);
        service.delete(url);
    }

    /**
	 * Returns the format code based on a file extension, and object id.
	 * 
	 * @param resourceId the resource id of the object you want the format for.
	 * @param ext extension of the file you want the format for.
	 * 
	 * @throws RuntimeException
	 */
    public String getDownloadFormat(String resourceId, String ext) throws RuntimeException {
        if (resourceId == null || ext == null) {
            throw new RuntimeException("null passed in for required parameters");
        }
        if (resourceId.indexOf("document") == 0) {
            if (DOWNLOAD_DOCUMENT_FORMATS.containsKey(ext)) {
                return DOWNLOAD_DOCUMENT_FORMATS.get(ext);
            }
        } else if (resourceId.indexOf("presentation") == 0) {
            if (DOWNLOAD_PRESENTATION_FORMATS.containsKey(ext)) {
                return DOWNLOAD_PRESENTATION_FORMATS.get(ext);
            }
        } else if (resourceId.indexOf("spreadsheet") == 0) {
            if (DOWNLOAD_SPREADSHEET_FORMATS.containsKey(ext)) {
                return DOWNLOAD_SPREADSHEET_FORMATS.get(ext);
            }
        }
        throw new RuntimeException("invalid document type");
    }

    /**
	 * Gets the suffix of the resourceId. If the resourceId is "document:dh3bw3j_0f7xmjhd8", "dh3bw3j_0f7xmjhd8" will be returned.
	 * 
	 * @param resourceId the resource id to extract the suffix from.
	 * 
	 * @throws RuntimeException
	 */
    public String getResourceIdSuffix(String resourceId) throws RuntimeException {
        if (resourceId == null) {
            throw new RuntimeException("null resourceId");
        }
        if (resourceId.indexOf("%3A") != -1) {
            return resourceId.substring(resourceId.lastIndexOf("%3A") + 3);
        } else if (resourceId.indexOf(":") != -1) {
            return resourceId.substring(resourceId.lastIndexOf(":") + 1);
        }
        throw new RuntimeException("Bad resourceId");
    }

    /**
	 * Gets the prefix of the resourceId. If the resourceId is "document:dh3bw3j_0f7xmjhd8", "document" will be returned.
	 * 
	 * @param resourceId the resource id to extract the suffix from.
	 * 
	 * @throws RuntimeException
	 */
    public String getResourceIdPrefix(String resourceId) throws RuntimeException {
        if (resourceId == null) {
            throw new RuntimeException("null resourceId");
        }
        if (resourceId.indexOf("%3A") != -1) {
            return resourceId.substring(0, resourceId.indexOf("%3A"));
        } else if (resourceId.indexOf(":") != -1) {
            return resourceId.substring(0, resourceId.indexOf(":"));
        } else {
            throw new RuntimeException("Bad resourceId");
        }
    }

    /**
	 * Builds a URL from a patch.
	 * 
	 * @param path the path to add to the protocol/host
	 * 
	 * @throws MalformedURLException
	 * @throws RuntimeException
	 */
    private URL buildUrl(String path) throws MalformedURLException, RuntimeException {
        if (path == null) {
            throw new RuntimeException("null path");
        }
        return buildUrl(path, null);
    }

    /**
	 * Builds a URL with parameters.
	 * 
	 * @param path the path to add to the protocol/host
	 * @param parameters parameters to be added to the URL.
	 * 
	 * @throws MalformedURLException
	 * @throws RuntimeException
	 */
    private URL buildUrl(String path, String[] parameters) throws MalformedURLException, RuntimeException {
        if (path == null) {
            throw new RuntimeException("null path");
        }
        return buildUrl(host, path, parameters);
    }

    /**
	 * Builds a URL with parameters.
	 * 
	 * @param domain the domain of the server
	 * @param path the path to add to the protocol/host
	 * @param parameters parameters to be added to the URL.
	 * 
	 * @throws MalformedURLException
	 * @throws RuntimeException
	 */
    private URL buildUrl(String domain, String path, String[] parameters) throws MalformedURLException, RuntimeException {
        if (path == null) {
            throw new RuntimeException("null path");
        }
        StringBuffer url = new StringBuffer();
        url.append(protocol + "://" + domain + URL_FEED + path);
        if (parameters != null && parameters.length > 0) {
            url.append("?");
            for (int i = 0; i < parameters.length; i++) {
                url.append(parameters[i]);
                if (i != (parameters.length - 1)) {
                    url.append("&");
                }
            }
        }
        return new URL(url.toString());
    }

    /**
	 * Builds a URL with parameters.
	 * 
	 * @param domain the domain of the server
	 * @param path the path to add to the protocol/host
	 * @param parameters parameters to be added to the URL as key value pairs.
	 * 
	 * @throws MalformedURLException
	 * @throws RuntimeException
	 */
    private URL buildUrl(String domain, String path, Map<String, String> parameters) throws MalformedURLException, RuntimeException {
        if (path == null) {
            throw new RuntimeException("null path");
        }
        StringBuffer url = new StringBuffer();
        url.append(protocol + "://" + domain + URL_FEED + path);
        if (parameters != null && parameters.size() > 0) {
            Set<Map.Entry<String, String>> params = parameters.entrySet();
            Iterator<Map.Entry<String, String>> itr = params.iterator();
            url.append("?");
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                url.append(entry.getKey() + "=" + entry.getValue());
                if (itr.hasNext()) {
                    url.append("&");
                }
            }
        }
        return new URL(url.toString());
    }

    public static void turnOnLogging() {
        Logger httpLogger = Logger.getLogger("com.google.gdata.client.http.HttpGDataRequest");
        httpLogger.setLevel(Level.ALL);
        Logger xmlLogger = Logger.getLogger("com.google.gdata.util.XmlParser");
        xmlLogger.setLevel(Level.ALL);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.ALL);
        httpLogger.addHandler(logHandler);
        xmlLogger.addHandler(logHandler);
    }
}
