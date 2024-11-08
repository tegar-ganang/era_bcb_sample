package uk.ac.ncl.neresc.dynasoar.codestore;

import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.MessageElement;
import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import org.apache.log4j.Logger;
import org.exolab.castor.xml.Marshaller;
import org.w3c.dom.Document;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import uk.ac.ncl.neresc.dynasoar.messages.*;
import uk.ac.ncl.neresc.dynasoar.messages.types.Status;
import uk.ac.ncl.neresc.dynasoar.constants.DynasoarConstants;
import uk.ac.ncl.neresc.dynasoar.utils.DOMUtils;
import uk.ac.ncl.neresc.dynasoar.vm.VirtualMachineDescription;
import uk.ac.ncl.neresc.dynasoar.vm.VmServiceList;

public class CodeStoreUploader extends HttpServlet {

    private static Logger mLog = Logger.getLogger(CodeStoreUploader.class.getName());

    private static boolean init = false;

    private static boolean WAR = false;

    private static boolean VMWARE = false;

    private static boolean moreFiles = false;

    private static Vector fileList = new Vector();

    private static String serviceName = null;

    private static String metadata = null;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("DynasoarUploader", request.getLocale());
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            String title = "Uploader";
            out.println("<title>" + title + "</title>");
            out.println("</head>");
            out.println("<body bgcolor=\"white\">");
            out.println("<h1> Upload your web service code or virtual machines here </h1>");
            out.println("<p>");
            if (!init) {
                out.println("Please check the radio button corresponding to the file type");
                out.println("<p>");
                out.println("<form action=\"CodeStoreUploader\" method=POST>");
                out.println("<input type='radio' name='filetype' value=\"WAR\" checked='checked'/>" + " WAR (Web Archive) ");
                out.println("<br/>");
                out.println("<input type='radio' name='filetype' value=\"VMWare\"/>" + " VMWare (Virtual Machine) ");
                out.println("<hr />");
                init = true;
                out.println("<br/>");
                out.println("<input type=submit value=Continue>");
            } else {
                if (moreFiles) {
                    init = false;
                    mLog.debug("Continuation of previous session");
                    out.println("Please provide the VM name and select the files you wish to upload and then click the Upload button");
                    out.println("<br/>");
                    mLog.debug("Continuing Virtual Machine upload...");
                    out.println("<b>");
                    out.println("Upload the remaining files here...");
                    out.println("<br/>");
                    out.println("The total request size should not increase 2GB due to restrictions in the Java API. For larger files, please use multiple requests.");
                    out.println("</b>");
                    out.println("<br/>");
                    out.println("<form enctype=\"multipart/form-data\" action=\"CodeStoreUploader\" method=POST>");
                    out.println("<input name=virtualharddisk1 type=file size=100");
                    out.println("<br/>");
                    out.println("<input name=virtualharddisk2 type=file size=100");
                    out.println("<br/>");
                    out.println("<input name=virtualharddisk3 type=file size=100");
                    out.println("<br/>");
                    out.println("Please check below if you have more files to upload for this service");
                    out.println("<br/>");
                    out.println("<input type='radio' name='morefiles' value=\"Yes\" checked='checked'/>" + " Yes ");
                    out.println("<br/>");
                    out.println("<input type='radio' name='morefiles' value=\"No\"/>" + " No ");
                    out.println("<hr />");
                    out.println("<input type=submit value=Upload>");
                } else {
                    init = false;
                    String filetype = request.getParameter("filetype");
                    mLog.debug("filetype: " + filetype);
                    if (filetype.equals("WAR")) {
                        mLog.debug("About to upload a WAR file");
                        WAR = true;
                        out.println("Please provide the service name and select the file you wish to upload and then click the Upload button");
                        out.println("<br/>");
                        out.println("<form enctype=\"multipart/form-data\" action=\"CodeStoreUploader\" method=POST>");
                        out.println("<b>");
                        out.println("Service Name ");
                        out.println("</b>");
                        out.println("<input name=servicename type=text size=30");
                        out.println("<br/>");
                        out.println("<input name=filename type=file size=100");
                        out.println("<br/>");
                        out.println("<input type=submit value=Upload>");
                    } else if (filetype.equals("VMWare")) {
                        out.println("Please provide the VM name and select the files you wish to upload and then click the Upload button");
                        out.println("<br/>");
                        mLog.debug("About to upload VMWare Virtual Machine");
                        VMWARE = true;
                        out.println("<b>");
                        out.println("Please make sure that the 1st entry is the config file, the 2nd entry is the XML description and the other entires are for the hard disk(s)");
                        out.println("<br/>");
                        out.println("The total request size should not increase 2GB due to restrictions in the Java API. For larger files, please use multiple requests.");
                        out.println("</b>");
                        out.println("<br/>");
                        out.println("<form enctype=\"multipart/form-data\" action=\"CodeStoreUploader\" method=POST>");
                        out.println("<b>");
                        out.println("VM Name ");
                        out.println("</b>");
                        out.println("<input name=vmname type=text size=30");
                        out.println("<br/>");
                        out.println("<input name=configfile type=file size=100");
                        out.println("<br/>");
                        out.println("<input name=descfile type=file size=100");
                        out.println("<br/>");
                        out.println("<input name=virtualharddisk1 type=file size=100");
                        out.println("<br/>");
                        out.println("Please check below if you have more files to upload for this service");
                        out.println("<br/>");
                        out.println("<input type='radio' name='morefiles' value=\"Yes\" checked='checked'/>" + " Yes ");
                        out.println("<br/>");
                        out.println("<input type='radio' name='morefiles' value=\"No\"/>" + " No ");
                        out.println("<hr />");
                        out.println("<input type=submit value=Upload>");
                    }
                }
            }
            out.println("</form>");
            out.println("</body>");
            out.println("</html>");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            if (!init) {
                mLog.debug("Handling file input...");
                mLog.debug("Request Content Length: " + request.getContentLength());
                handleFile(request, response);
            } else {
                mLog.debug("Handling option input...");
                mLog.debug("Request Content Length: " + request.getContentLength());
                handleRadioOption(request, response);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String parseFileName(String name, String separator) {
        int slashIndex = name.lastIndexOf(separator);
        if (slashIndex > -1) {
            return name.substring(slashIndex + 1);
        } else {
            return name;
        }
    }

    private void errorHandler() {
        System.out.println("There is some error in the file handling...");
    }

    private void handleRadioOption(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    private void handleFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            mLog.debug("processing upload request...");
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>" + "File Received" + "</title>");
            out.println("</head>");
            out.println("<body bgcolor=\"white\">");
            ResourceBundle rb = ResourceBundle.getBundle("DynasoarUploader", request.getLocale());
            String filepath = System.getenv("CATALINA_HOME") + "/" + rb.getString("filesave.location");
            Enumeration e = request.getAttributeNames();
            boolean multipart = FileUpload.isMultipartContent(request);
            FileItem item = null;
            String storedFileName = null;
            FileUpload upload = new FileUpload(new DefaultFileItemFactory());
            List items = upload.parseRequest(request);
            mLog.debug("items.size() = " + items.size());
            Iterator it = items.iterator();
            String writeFilePath = null;
            String parseFilePath = null;
            String serviceURI = rb.getString("URI.endpoint");
            while (it.hasNext()) {
                item = (FileItem) it.next();
                if (!(item.isFormField())) {
                    try {
                        mLog.debug("item name = " + item.getName());
                        String fieldName = item.getFieldName();
                        if (item.getName().equals("")) {
                            continue;
                        }
                        if (fieldName.equals("descfile")) {
                            mLog.debug("this is the metadata file");
                            byte[] fileData = item.get();
                            metadata = new String(fileData);
                            mLog.debug("Metadata: " + metadata + "\n");
                        } else {
                            InputStream in = item.getInputStream();
                            parseFilePath = parseFileName(item.getName(), "/");
                            mLog.debug("Parsed file name after first pass =" + parseFilePath);
                            parseFilePath = parseFileName(item.getName(), "\\");
                            mLog.debug("Parsed file name after second pass =" + parseFilePath);
                            writeFilePath = filepath + File.separator + parseFilePath;
                            mLog.debug("Writing to =" + writeFilePath);
                            fileList.add(serviceURI + parseFilePath);
                            File tempFile = new File(writeFilePath);
                            if (!tempFile.exists()) {
                                tempFile.createNewFile();
                            }
                            FileOutputStream outStream = new FileOutputStream(tempFile);
                            ReadableByteChannel channelIn = Channels.newChannel(in);
                            FileChannel channelOut = outStream.getChannel();
                            channelOut.transferFrom(channelIn, 0, item.getSize());
                            channelIn.close();
                            channelOut.close();
                            outStream.flush();
                            outStream.close();
                        }
                    } catch (IOException ix) {
                        errorHandler();
                    }
                } else {
                    String name = item.getFieldName();
                    String value = item.getString();
                    if (name.equals("vmname") || name.equals("servicename")) {
                        mLog.debug("Service/VM Name: " + value);
                        serviceName = value;
                    } else if (name.equals("morefiles")) {
                        mLog.debug("Adding more files: " + value);
                        if (value.equals("Yes")) {
                            mLog.debug("More files will be uploaded corresponding to this VM");
                            moreFiles = true;
                        } else if (value.equals("No")) {
                            mLog.debug("No more files will be uploaded corresponding to this VM");
                            moreFiles = false;
                        }
                    }
                }
            }
            if (moreFiles) {
                init = true;
                WAR = false;
                VMWARE = true;
                doGet(request, response);
            } else {
                registerCode(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerCode(HttpServletRequest request, HttpServletResponse response) {
        try {
            mLog.debug("Number of files uploaded: " + fileList.size());
            PrintWriter out = response.getWriter();
            ResourceBundle rb = ResourceBundle.getBundle("DynasoarUploader", request.getLocale());
            String token = null;
            CodeStoreRequest req = new CodeStoreRequest();
            if (WAR) {
                mLog.debug("setting token to WAR");
                token = "WAR";
                AddServiceRequest addServ = new AddServiceRequest();
                addServ.setServiceID(serviceName);
                String codeAvailabilityURI = (String) fileList.get(0);
                addServ.setServiceURI(codeAvailabilityURI);
                req.setAddServiceRequest(addServ);
            } else if (VMWARE) {
                mLog.debug("setting token to VMWARE");
                token = "VMWARE";
                AddVMServiceRequest addVMServ = new AddVMServiceRequest();
                addVMServ.setVmName(serviceName);
                for (int i = 0; i < fileList.size(); i++) {
                    String fileItem = (String) fileList.get(i);
                    mLog.debug("File item: " + fileItem);
                    addVMServ.addFileAccessURI(fileItem);
                }
                if (metadata != null) {
                    VirtualMachineDescription vmDesc = (VirtualMachineDescription) VirtualMachineDescription.unmarshal(new StringReader(metadata));
                    addVMServ.setVmPort(vmDesc.getTomcatPort());
                    VmServiceList[] sList = vmDesc.getVmServiceList();
                    if (sList != null) {
                        int sCount = sList.length;
                        mLog.debug(serviceName + "(VM) contains " + sCount + " services.");
                        for (int i = 0; i < sCount; i++) {
                            String vServiceName = sList[i].getServiceName();
                            String vServiceURI = sList[i].getServiceURI();
                            VMWrappedServicesList sItem = new VMWrappedServicesList();
                            sItem.setServiceName(vServiceName);
                            sItem.setServiceURI(vServiceURI);
                            addVMServ.addVMWrappedServicesList(sItem);
                        }
                    }
                }
                req.setAddVMServiceRequest(addVMServ);
            }
            StringWriter writer = new StringWriter();
            Marshaller.marshal(req, writer);
            String writerOutput = writer.toString();
            System.out.println("Marshalled Output:\n" + writerOutput);
            String s1 = writerOutput.substring(DynasoarConstants.XML_HEADER.length() + 1);
            String newString = s1.replaceAll(DynasoarConstants.CSREQ_TO_REPLACE, DynasoarConstants.CS_REQUEST_NAME);
            System.out.println("Final output: " + newString);
            Document outputDocument = XMLUtils.newDocument(new ByteArrayInputStream(newString.getBytes()));
            SOAPEnvelope envelope = new SOAPEnvelope();
            SOAPBody bdy = (SOAPBody) envelope.getBody();
            SOAPBodyElement csReq = (SOAPBodyElement) bdy.addBodyElement(envelope.createName(DynasoarConstants.DYNASOAR_SERVICE_REQUEST));
            SOAPElement elem1 = DOMUtils.convertDOMToSOAPElement(envelope, outputDocument.getDocumentElement());
            csReq.addChildElement(elem1);
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(rb.getString("codestore.URL")));
            call.setTimeout(Integer.MAX_VALUE);
            SOAPEnvelope csResp = call.invoke(envelope);
            SOAPBody respB = (SOAPBody) csResp.getBody();
            Iterator itr = respB.getChildElements();
            boolean uploaded = false;
            if (itr.hasNext()) {
                while (itr.hasNext()) {
                    SOAPBodyElement el1 = (SOAPBodyElement) itr.next();
                    Iterator itr2 = el1.getChildElements(csResp.createName(DynasoarConstants.CS_RESPONSE_NAME));
                    if (!itr2.hasNext()) {
                        mLog.debug("Unable to process request, unknown message");
                        throw new SOAPException("Unable to process request, unknown message");
                    } else {
                        SOAPElement el2 = (SOAPElement) itr2.next();
                        Document doc = ((MessageElement) el2).getAsDocument();
                        String input = XMLUtils.DocumentToString(doc);
                        mLog.debug("Unmarshalling request document");
                        CodeStoreResponse csR = (CodeStoreResponse) CodeStoreResponse.unmarshal(new StringReader(input));
                        if (csR.getAddServiceResponse() != null) {
                            AddServiceResponse aR = csR.getAddServiceResponse();
                            if (aR.getStatus().getType() == Status.SUCCESS_TYPE) {
                                uploaded = true;
                            }
                        }
                    }
                }
            }
            if (uploaded) {
                mLog.debug("file successfully uploaded");
                out.println("Your web service has been received. If you would like to submit another web service, please click the button below.");
            } else {
                mLog.debug("file upload failed");
                out.println("Uploading failed. If you would like to resubmit the web service, please click the button below.");
            }
            out.println("<p>");
            init = false;
            WAR = false;
            VMWARE = false;
            fileList.clear();
            moreFiles = false;
            out.println("<form enctype=\"multipart/form-data\" action=\"CodeStoreUploader\" method=GET>");
            out.println("<input type=submit value=\"Submit more files\">");
            out.println("</form>");
            out.println("</body>");
            out.println("</html>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
