package up2p.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import up2p.core.DuplicateResourceException;
import up2p.core.NetworkAdapterException;
import up2p.core.ResourceNotFoundException;
import up2p.core.WebAdapter;
import up2p.util.FileUtil;
import up2p.util.PairList;
import up2p.xml.TransformerHelper;

/**
 * Handles a file upload from an HTML web page and submits the file to the
 * {@link up2p.core.WebAdapter#publish publish()}method of the
 * <code>WebAdapter</code> method.
 * 
 * @author Neal Arthorne
 * @version 1.0
 */
public class UploadServlet extends AbstractWebAdapterServlet {

    /** Mode for layout tag. */
    private static final String MODE = "create";

    public static final int MAX_DUPLICATE_NOTIFICATIONS = 5;

    protected static void writeError(HttpServletRequest request, HttpServletResponse response, String errorMsg, PairList paramMap) throws IOException, ServletException {
        String respondWithXml = paramMap.getValue(HttpParams.UP2P_FETCH_XML);
        if (respondWithXml != null && respondWithXml.length() > 0) {
            String xmlEscapedErrorMsg = errorMsg.replace("<br/>", "\n").replaceAll("<.*?>", "");
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<upload success=\"false\">");
            out.println("<errmsg>" + xmlEscapedErrorMsg + "</errmsg>");
            out.println("</upload>");
        } else {
            AbstractWebAdapterServlet.writeError(request, response, "<p><b>Error:</b> " + errorMsg + "</p>", MODE);
        }
    }

    /** File upload handler. */
    private ServletFileUpload uploadHandler;

    /** Constructs the servlet. */
    public UploadServlet() {
        super();
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
        diskFileItemFactory.setSizeThreshold(UPLOAD_THRESHOLD);
        uploadHandler = new ServletFileUpload(diskFileItemFactory);
        uploadHandler.setSizeMax(UPLOAD_MAX_SIZE);
        uploadHandler.setHeaderEncoding("ISO-8859-1");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doUpload(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doUpload(req, resp);
    }

    /**
     * Processes an request to upload a file.
     * 
     * @param request inbound request made to the servlet
     * @param response outbound reponse written to the client
     * @throws ServletException if an error occurs
     * @throws IOException if an error occurs reading or writing to files or
     * streams
     */
    protected void doUpload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession reqSession = request.getSession();
        ArrayList<File> uploadFiles = new ArrayList<File>();
        LOG.info("UploadServlet Upload request received");
        if (ServletFileUpload.isMultipartContent(request)) {
            LOG.debug("UploadServlet Received a multipart request.");
        } else {
            LOG.debug("UploadServlet Received a non-multipart request.");
        }
        String tempDirName = UUID.randomUUID().toString();
        File tempUploadDir = new File(adapter.getRootPath() + File.separator + "temp" + File.separator + tempDirName);
        tempUploadDir.getParentFile().mkdir();
        while (tempUploadDir.exists()) {
            tempDirName = UUID.randomUUID().toString();
            tempUploadDir = new File(adapter.getRootPath() + File.separator + "temp" + File.separator + tempDirName);
        }
        tempUploadDir.mkdir();
        File attachmentDir = (File) (reqSession.getAttribute("up2p:attachdir"));
        if (attachmentDir != null) {
            LOG.info("UploadServlet: Copying provided attachment to upload dir from: " + attachmentDir.getAbsolutePath());
            tempUploadDir.delete();
            attachmentDir.renameTo(tempUploadDir);
            reqSession.removeAttribute("up2p:attachdir");
        }
        LOG.info("UploadServlet: Using temporary directory: " + tempUploadDir.getPath());
        PairList paramMap = null;
        if (ServletFileUpload.isMultipartContent(request)) {
            paramMap = getMultipartParameters(request, uploadHandler, LOG, tempUploadDir.getPath());
            if (paramMap.size() == 0) {
                LOG.debug("UploadServlet Parsed multipart request and " + "found no parameters. Parsing as regular" + " request instead.");
                paramMap = copyParameters(request);
                LOG.debug("UploadServlet Parsed as regular request and found " + paramMap.size() + " parameters.");
            }
        } else {
            paramMap = copyParameters(request);
        }
        String communityId = getCurrentCommunityId(request.getSession());
        String newcommunity = paramMap.getValue(HttpParams.UP2P_COMMUNITY);
        LOG.debug("UploadServlet: Got active community: " + newcommunity);
        if (newcommunity != null) {
            communityId = newcommunity;
            LOG.debug("switching to community" + communityId);
        }
        if (communityId == null || communityId.length() == 0) {
            LOG.warn("UploadServlet Current community ID is missing from" + "the user session.");
            writeError(request, response, "The current community is unknown." + " Please select a community before performing " + "any actions.", paramMap);
            for (File f : tempUploadDir.listFiles()) {
                f.delete();
            }
            tempUploadDir.delete();
            return;
        }
        LOG.info("UploadServlet Uploading to community " + communityId + ".");
        uploadFiles.clear();
        Iterator<String> uploadedFileIter = paramMap.getValues(HttpParams.UP2P_FILENAME);
        String filename = "";
        try {
            if (uploadedFileIter.hasNext()) {
                filename = uploadedFileIter.next();
            } else {
                throw new IOException("UploadServlet: No up2p:filename parameters were found.");
            }
            if (filename.startsWith("file:")) filename = filename.substring(5);
            if (filename.length() == 0) {
                throw new IOException("UploadServlet: An empty up2p:filename parameter was submitted.");
            }
            File resourceFile = null;
            resourceFile = new File(adapter.getStorageDirectory(communityId), filename);
            File tempResFile = new File(tempUploadDir, filename);
            if (!tempResFile.exists() && !resourceFile.exists()) {
                throw new IOException("UploadServlet: The uploaded resource could not be found.");
            }
            if ((tempResFile.exists() && resourceFile.exists()) || (tempResFile.exists() && !resourceFile.exists())) {
                resourceFile = FileUtil.createUniqueFile(resourceFile);
                LOG.info("UploadServlet: Direct upload, copying resource file.\n\tOriginal: " + tempResFile.getPath() + "\n\tNew: " + resourceFile.getPath());
                resourceFile.getParentFile().mkdir();
                FileOutputStream resourceCopyStream = new FileOutputStream(resourceFile);
                FileUtil.writeFileToStream(resourceCopyStream, tempResFile, true);
                filename = resourceFile.getName();
            }
            LOG.info("UploadServlet: resource file name: " + filename);
            uploadFiles.add(resourceFile);
        } catch (IOException e) {
            LOG.error("UploadServlet: " + e.getMessage());
            writeError(request, response, e.getMessage(), paramMap);
            return;
        }
        String editResourceId = paramMap.getValue(HttpParams.UP2P_EDIT_RESOURCE);
        if (editResourceId != null && editResourceId.length() > 0) {
            LOG.debug("UploadServlet: Got edit resource: " + editResourceId);
            File editAttach = new File(adapter.getAttachmentStorageDirectory(communityId, editResourceId));
            if (editAttach.exists() && editAttach.isDirectory()) {
                LOG.debug("UploadServlet: Copying additional attachments from: " + editAttach.getAbsolutePath());
                for (File oldFile : editAttach.listFiles()) {
                    File newFile = new File(tempUploadDir, oldFile.getName());
                    if (!newFile.exists()) {
                        LOG.debug("UploadServlet: Copying attachment: " + oldFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
                        FileOutputStream attachCopyStream = new FileOutputStream(newFile);
                        FileUtil.writeFileToStream(attachCopyStream, oldFile, true);
                    } else {
                        LOG.debug("UploadServlet: Attachment " + newFile.getName() + " explicitly replaced in new upload.");
                    }
                }
            }
        }
        String batchUploadString = paramMap.getValue(HttpParams.UP2P_BATCH);
        boolean batchUpload = batchUploadString != null && batchUploadString.length() > 0;
        if (batchUpload) {
            LOG.info("UploadSerlvet Recieved batch upload request.");
            File batchFile = new File(adapter.getStorageDirectory(communityId), uploadFiles.get(0).getName());
            uploadFiles.clear();
            LOG.debug("UploadServlet resource file stored at: " + batchFile.getPath());
            XMLReader reader = TransformerHelper.getXMLReader();
            reader.setContentHandler(new BatchCopyHandler(communityId, uploadFiles));
            try {
                FileInputStream batchInput = new FileInputStream(batchFile);
                reader.parse(new InputSource(batchInput));
                batchInput.close();
            } catch (Exception e) {
                LOG.error("UploadServlet: Error parsing batch upload file.");
                LOG.error("UploadServlet: " + e.getMessage());
                writeError(request, response, "Uploaded content was not a valid batch resource file.", paramMap);
                return;
            }
            batchFile.delete();
            adapter.addNotification("Batch file succesfully processed into " + uploadFiles.size() + " resources.");
        }
        boolean pushUpload = paramMap.getValue(HttpParams.UP2P_PUSH) != null && paramMap.getValue(HttpParams.UP2P_PUSH).length() > 0;
        String id = "";
        boolean duplicateResource = false;
        int uploadCount = 0;
        for (int i = 0; i < uploadFiles.size(); i++) {
            LOG.info("UploadServlet Publishing resource to WebAdapter: " + uploadFiles.get(i).getName());
            try {
                id = adapter.publish(communityId, new File(uploadFiles.get(i).getName()), tempUploadDir);
                uploadFiles.remove(i);
                i--;
                uploadCount++;
                LOG.info("UploadServlet Resource published with id: " + id);
            } catch (IOException e) {
                LOG.warn("UploadServlet IO Error occured in reading the uploaded file: " + e.getMessage(), e);
                uploadFilesCleanup(uploadFiles, tempUploadDir);
                writeError(request, response, "An error occured in reading the uploaded file: " + e.getMessage(), paramMap);
                return;
            } catch (SAXParseException e) {
                LOG.warn("UploadServlet SAX Parse Error occured in uploaded resource: " + e.getMessage());
                String errMsg = "Invalid XML in the uploaded resource.<br/>" + e.getMessage() + "<br/><br/>File location: " + uploadFiles.get(i).getAbsolutePath() + "<br/>Line: " + e.getLineNumber() + " Column: " + e.getColumnNumber();
                uploadFilesCleanup(uploadFiles, tempUploadDir);
                writeError(request, response, errMsg, paramMap);
                return;
            } catch (SAXException e) {
                LOG.warn("UploadServlet Invalid XML in uploaded resource: " + e.getMessage());
                String errMsg = "Invalid XML in uploaded resource<br/><i>" + e.getMessage() + "</i><br/>" + "File location: " + uploadFiles.get(i).getAbsolutePath();
                uploadFilesCleanup(uploadFiles, tempUploadDir);
                writeError(request, response, errMsg, paramMap);
                return;
            } catch (DuplicateResourceException e) {
                LOG.info("UploadServlet Duplicate Resource: " + e.getResourceId() + " Community: " + e.getCommunityId());
                if (batchUpload) {
                    if (!duplicateResource) {
                        adapter.addNotification("Warning: Batch upload contained previously published " + " resources which have been discarded.");
                        duplicateResource = true;
                    }
                } else {
                    uploadFilesCleanup(uploadFiles, tempUploadDir);
                    String respondWithXml = paramMap.getValue(HttpParams.UP2P_FETCH_XML);
                    if (respondWithXml != null && respondWithXml.length() > 0) {
                        writeError(request, response, "This resource is already shared with resource " + " id: " + e.getResourceId(), paramMap);
                        return;
                    }
                    String redirect = response.encodeURL("/overwrite.jsp?up2p:community=" + e.getCommunityId() + "&up2p:resource=" + e.getResourceId());
                    LOG.info("UploadServlet Redirecting to " + redirect);
                    RequestDispatcher rd = request.getRequestDispatcher(redirect);
                    rd.forward(request, response);
                    return;
                }
            } catch (NetworkAdapterException e) {
                LOG.info("UploadServlet Error in the Network Adapter for" + " community ID " + communityId, e);
                uploadFilesCleanup(uploadFiles, tempUploadDir);
                writeError(request, response, "Error in the Network Adapter for this community. <br/>" + e.getMessage(), paramMap);
                return;
            } catch (ResourceNotFoundException e) {
                LOG.info("UploadServlet Error Resource not found " + e);
                uploadFilesCleanup(uploadFiles, tempUploadDir);
                writeError(request, response, "Error : <br/>" + e.getMessage(), paramMap);
                return;
            }
        }
        uploadFilesCleanup(uploadFiles, tempUploadDir);
        String ajaxRequest = paramMap.getValue(HttpParams.UP2P_XMLHTTP);
        String respondWithXml = paramMap.getValue(HttpParams.UP2P_FETCH_XML);
        if (respondWithXml != null && respondWithXml.length() > 0) {
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<upload success=\"true\" >");
            out.println("<resid>" + id + "</resid>");
            out.println("</upload>");
        } else if (ajaxRequest != null && ajaxRequest.length() > 0) {
            LOG.debug("UploadServlet Recieved xmlHttp request, responding with XML");
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.print("<resource id=\"" + request.getParameter(HttpParams.UP2P_RESOURCE) + "\" ");
            if (request.getParameter(HttpParams.UP2P_PEERID) != null) {
                out.print("peerid=\"" + request.getParameter(HttpParams.UP2P_PEERID) + "\" ");
            }
            out.println("/>");
        } else if (batchUpload) {
            adapter.addNotification(uploadCount + " resources were succesfully published.");
            request.setAttribute("up2p.display.mode", "view");
            String redirect = response.encodeURL("/view.jsp?up2p:community=" + communityId);
            LOG.info("UploadServlet Redirecting to " + redirect);
            RequestDispatcher rd = request.getRequestDispatcher(redirect);
            rd.forward(request, response);
        } else if (!pushUpload) {
            request.setAttribute("up2p.display.mode", "view");
            String redirect = response.encodeURL("/view.jsp?up2p:resource=" + id);
            LOG.info("UploadServlet Redirecting to " + redirect);
            RequestDispatcher rd = request.getRequestDispatcher(redirect);
            rd.forward(request, response);
        }
        return;
    }

    /**
     * Deletes all uploaded files and attachments from the current request, as well
     * as removing any resources that were successfully batch uploaded during the
     * current request. This is used when a resource fails to upload for any reason
     * to prevent attachment and resource files from being orphaned in the community
     * directory.
     */
    private void uploadFilesCleanup(List<File> removeFiles, File tempUploadDir) {
        LOG.info("Removing all unused resources from this upload request.");
        for (File resourceFile : removeFiles) {
            resourceFile.delete();
        }
        removeFiles.clear();
        if (tempUploadDir != null) {
            for (File f : tempUploadDir.listFiles()) {
                f.delete();
            }
            tempUploadDir.delete();
            tempUploadDir = null;
        }
    }

    /**
     * Parses a batch upload xml file, and creates a new file for each individual resource
     * within the batch file. These files are then passed as arguments to the publish
     * method of the WebAdapter.
     * 
     * @author Alexander Craig
     */
    class BatchCopyHandler implements ContentHandler {

        private boolean namespaceBegin = false;

        private String currentNamespace;

        private String currentNamespaceUri;

        private Locator locator;

        private List<File> uploadFiles;

        private File communityDir;

        private File tempFile;

        private BufferedWriter out;

        /** Stores the depth of the XML node currently being processed */
        int depth;

        public BatchCopyHandler(String communityId, List<File> uploadFiles) {
            communityDir = new File(adapter.getStorageDirectory(communityId));
            communityDir.mkdir();
            out = null;
            depth = 0;
            this.uploadFiles = uploadFiles;
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void startDocument() {
            LOG.debug("UploadServlet began parsing batch xml file.");
        }

        public void endDocument() {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error("Upload Servlet could not close file writer.");
                }
                out = null;
            }
        }

        public void startPrefixMapping(String prefix, String uri) {
            namespaceBegin = true;
            currentNamespace = prefix;
            currentNamespaceUri = uri;
        }

        public void endPrefixMapping(String prefix) {
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            depth++;
            if (depth == 2) {
                LOG.debug("UploadServlet generated new file at tag: " + qName);
                try {
                    tempFile = File.createTempFile("batch", ".xml", communityDir);
                    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
                    out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                    uploadFiles.add(tempFile);
                    namespaceBegin = true;
                } catch (IOException err) {
                    LOG.error("UploadServlet Error creating new files for batch upload.");
                }
            }
            if (out != null) {
                try {
                    out.write("<" + qName);
                    if (namespaceBegin && currentNamespace != null && currentNamespaceUri != null) {
                        if (currentNamespace.equals("")) {
                            out.write(" xmlns=\"" + currentNamespaceUri + "\"");
                        } else {
                            out.write(" xmlns:" + currentNamespace + "=\"" + currentNamespaceUri + "\"");
                        }
                        namespaceBegin = false;
                    }
                    for (int i = 0; i < atts.getLength(); i++) {
                        out.write(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");
                    }
                    out.write(">");
                } catch (IOException e) {
                    uploadFiles.remove(tempFile);
                    LOG.error("Upload Servet Error writing to batch upload files (Start Element).");
                }
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) {
            depth--;
            if (out != null) {
                try {
                    out.write("</" + qName + ">");
                } catch (IOException e) {
                    uploadFiles.remove(tempFile);
                    LOG.error("Upload Servet Error writing to batch upload files (End Element).");
                }
            }
            if (depth == 1) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error("Upload Servlet could not close file writer.");
                }
                out = null;
                return;
            }
        }

        public void characters(char[] ch, int start, int length) {
            if (out != null) {
                try {
                    String charString = new String(ch, start, length);
                    charString = charString.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
                    out.write(charString);
                } catch (IOException e) {
                    uploadFiles.remove(tempFile);
                    LOG.error("Upload Servet Error writing to batch upload files (Characters).");
                }
            }
        }

        public void ignorableWhitespace(char[] ch, int start, int length) {
            if (out != null) {
                for (int i = start; i < start + length; i++) {
                    try {
                        out.write(ch[i]);
                    } catch (IOException e) {
                        uploadFiles.remove(tempFile);
                        LOG.error("Upload Servet Error writing to batch upload files (Whitespace).");
                    }
                }
            }
        }

        public void processingInstruction(String target, String data) {
            if (out != null) {
                try {
                    out.write("<?" + target + " " + data + "?>");
                } catch (IOException e) {
                    uploadFiles.remove(tempFile);
                    LOG.error("Upload Servet Error writing to batch upload files (Processing Instruction).");
                }
            }
        }

        public void skippedEntity(String name) {
        }
    }
}
