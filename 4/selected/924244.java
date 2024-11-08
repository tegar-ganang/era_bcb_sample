package org.jxpl.primitives.service.soap;

import org.w3c.dom.*;
import org.jxpl.*;
import org.jxpl.bindings.*;
import org.jxpl.exception.*;
import java.util.*;
import java.util.jar.*;
import java.math.*;
import javax.xml.rpc.handler.Handler;
import javax.xml.rpc.handler.GenericHandler;
import javax.xml.rpc.handler.HandlerInfo;
import javax.xml.rpc.handler.HandlerRegistry;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.soap.SOAPMessage;
import javax.xml.rpc.JAXRPCException;
import javax.xml.rpc.ServiceException;
import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceFactory;
import javax.xml.namespace.QName;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.parsers.*;
import javax.xml.soap.*;
import org.apache.commons.logging.*;
import org.jxpl.primitives.service.*;
import org.jxpl.primitives.service.jxpl2Interface.*;
import org.apache.axis2.description.*;
import org.apache.axis2.AxisFault;

/**
 *  This primitive is designed to produce and package web services at design time.
 *  at runtime it is used to evaluate the proper logic.  This switching of mode is determined
 *  by a flag set in the properties of the primitive.
 *
 *  @author Eric Harris
 *  @version 0.1
 */
public class SOAPService implements org.jxpl.primitives.service.Service {

    private Processor environment;

    private String REPOSITORY = System.getProperty("java.io.tmpdir") + File.separator + "repository";

    private File pathToWar = null;

    private String namespace = "http://jxpl.org/jxpl/services/";

    private String serviceName;

    private List<SOAPOperation> operations;

    private JxplElement script;

    private String path;

    private static Log recorder = LogFactory.getLog(SOAPService.class.getName());

    private boolean packageForDeployment = false;

    public void setProcessor(Processor env) {
        environment = env;
    }

    /**
    *  Produce the script for creating and binding the service
    */
    public JxplElement evaluate(JxplElement input) throws JxplException {
        List<JxplElement> list = ((JxplList) input).getElements();
        JxplPrimitive prim = (JxplPrimitive) list.get(0);
        boolean generateService = false;
        List propList = prim.getProperties();
        Hashtable props = PropertyHelper.hash(propList, environment);
        if (props.containsKey("name")) {
            setServiceName((String) props.get("name"));
            path = REPOSITORY + File.separator + getServiceName() + File.separator;
        }
        if (props.containsKey("namepace")) {
            setNamespace((String) props.get("namespace"));
        }
        if (props.containsKey("package")) {
            Object pack = props.get("package");
            if (pack instanceof String) {
                if (((String) pack).equalsIgnoreCase("true")) packageForDeployment = true;
            } else if (pack instanceof JxplSymbol) {
                if (((JxplSymbol) pack).getQName().toString().equalsIgnoreCase("true")) packageForDeployment = true;
            }
        }
        if (props.containsKey("generate")) {
            Object generate = props.get("generate");
            if (generate instanceof String) {
                if (((String) generate).equalsIgnoreCase("true")) generateService = true;
            } else if (generate instanceof JxplSymbol) {
                if (((JxplSymbol) generate).getQName().toString().equalsIgnoreCase("true")) generateService = true;
            }
            if (props.containsKey("outputFile")) {
                String loc = (String) props.get("outputFile");
                pathToWar = new File(loc);
            }
        }
        List jxplOperations = ((JxplList) list.get(1)).getElements();
        if (jxplOperations.size() < 1) throw new JxplMalformedException("Missing Operation descriptions");
        operations = new LinkedList();
        for (int i = 1; i < jxplOperations.size(); i++) {
            addOperation(parseOperation((JxplElement) jxplOperations.get(i)));
        }
        recorder.info("Service has been parsed");
        JxplElement output;
        if (generateService) {
            setScript(input);
            output = generateSOAPService() ? Processor.TRUE : Processor.FALSE;
        } else {
            output = evaluateCall(((JxplList) list.get(2)).getElements());
        }
        return output;
    }

    /**
    *   This method facilitates the invocation of the function if the service is already generated
    *   @param params   set of inputs to be passed into the service including the operation name
    */
    private JxplElement evaluateCall(List<JxplElement> params) throws JxplException {
        JxplElement elem = null;
        String methodName = null;
        SOAPOperation myOperation = null;
        elem = environment.evaluate(params.get(0));
        if (elem instanceof JxplString) methodName = ((JxplString) elem).getValue(); else throw new JxplMalformedException("No method defined to invoke");
        for (int i = 0; i < operations.size(); i++) {
            SOAPOperation temp = operations.get(i);
            if (methodName.equalsIgnoreCase(temp.getName())) {
                myOperation = temp;
                break;
            }
        }
        if (myOperation == null) throw new JxplException("Cannot find operation");
        if (environment.getVariable(methodName) == null) {
            myOperation.cacheOperation();
        }
        JxplList completeCall = new JxplList();
        completeCall.addElement(new JxplPrimitive(methodName));
        for (int i = 1; i < params.size(); i++) completeCall.addElement(params.get(i));
        return environment.evaluate(completeCall);
    }

    /**
    *   Parse and create the description information required for building the wrapper
    *   @param elem Element containing the description information
    */
    protected SOAPOperation parseOperation(JxplElement elem) throws JxplException {
        SOAPOperation operation = null;
        if (elem instanceof JxplSymbol) {
            elem = environment.evaluate(elem);
        }
        if (elem instanceof JxplList) {
            JxplElement head = ((JxplList) elem).first();
            recorder.debug("First Element in the list was " + head);
            if (head instanceof JxplPrimitive) {
                JxplPrimitive prim = (JxplPrimitive) head;
                if (prim.getName().equalsIgnoreCase(".service.soap.SOAPOperation")) {
                    operation = SOAPOperation.buildOperation(elem, environment);
                    recorder.debug("Operation Parsed " + operation);
                } else throw new JxplMalformedException("Expected SOAPOperation but got " + prim.getName());
            } else parseOperation(head);
        } else throw new JxplMalformedException("List was expected");
        return operation;
    }

    /**
    *  Adds an operation to the service
    *  @param Operation to be added to service 
    */
    public void addOperation(Operation oper) {
        if (oper instanceof SOAPOperation) operations.add((SOAPOperation) oper);
    }

    /**
    *   Set the service name for this service
    *   @param servicename Name of the service
    */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
    *   Get the serviceName
    */
    public String getServiceName() {
        return serviceName;
    }

    /**
    *   Set the namespace for this service instance
    *   @param namespace set the namespace for the respective service
    */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
    *   Retuns the fully qualified servicename with namespace included.
    *   @return QName
    */
    public QName getFullServiceName() {
        return new QName(namespace, serviceName);
    }

    /**
    *   Get the namespace of the service
    */
    public String getNamespace() {
        return namespace;
    }

    /**
    *   Return a list of operations attached to this service
    */
    public List<SOAPOperation> getOperations() {
        return operations;
    }

    public void setScript(JxplElement script) {
        try {
            this.script = convertScript(script);
        } catch (JxplException e) {
        }
    }

    public JxplElement getScript() {
        return script;
    }

    /**
    *   Convert the primitive such that it will evaluate a method call upon evaluation
    *   instead of generating a service.  This is accomplished by removing the generate flag.
    *   @param script   Script to be converted
    */
    private JxplElement convertScript(JxplElement script) throws JxplException {
        List<JxplElement> list = ((JxplList) script).getElements();
        JxplPrimitive prim = ((JxplPrimitive) list.get(0));
        Vector<JxplElement> props = prim.getProperties();
        for (int i = 0; i < props.size(); i++) {
            JxplProperty jxplProp = (JxplProperty) environment.evaluate(props.get(i));
            if (jxplProp.getName().equalsIgnoreCase("generate")) {
                props.remove(i);
                break;
            }
        }
        prim.setProperties(props);
        recorder.debug("Transformed: \r\n" + script);
        return script;
    }

    /**
    *   Will generate service from the JxplDescription
    */
    protected boolean generateSOAPService() throws JxplException {
        try {
            new File(path + File.separator + "jxplService").mkdirs();
            new File(path + "META-INF").mkdir();
            generateWrapper(path + File.separator + "jxplService");
            recorder.debug("Proxy Class Created");
            generateConfigFile(path);
            recorder.debug("Service Config File Created");
            if (packageForDeployment) {
                generateAAR(path);
                cleanup();
            } else startServer();
        } catch (Exception e) {
            recorder.error(e);
            throw new JxplException("Error generating soap service..." + e.getMessage());
        }
        return true;
    }

    /**
    *   Include any neccessary code to cleanup temporary files goes here
    */
    private void cleanup() {
        new File(path).delete();
    }

    /**
    *   Generate service.xml document for use in configuring the service
    */
    protected String generateConfigFile(String path) throws Exception {
        String config = "<service name=\"" + getServiceName() + "\" scope=\"application\">\r\n";
        config += "<description> Published Jxpl Workflow </description>\r\n";
        config += "<messageReceivers>\r\n";
        config += "<messageReceiver mep=\"http://www.w3.org/2004/08/wsdl/in-only\"" + " class=\"org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver\"/>\r\n";
        config += "<messageReceiver mep=\"http://www.w3.org/2004/08/wsdl/in-out\"" + " class=\"org.apache.axis2.rpc.receivers.RPCMessageReceiver\"/>\r\n";
        config += "</messageReceivers>\r\n";
        config += "<parameter name=\"ServiceClass\" locked=\"false\">jxplService." + getServiceName() + "</parameter>\r\n";
        config += "</service>";
        PrintWriter configFile = new PrintWriter(new File(path + "META-INF/services.xml"));
        configFile.print(config);
        configFile.close();
        return config;
    }

    /**
    *  Create a class that will generate a proxy class to map rpc calls to wsdl via Axis2's wsdl generator
    */
    protected boolean generateWrapper(String path) throws Exception {
        File source = new File(path + File.separator + getServiceName() + ".java");
        System.out.println("Files are " + source);
        if (source.exists()) source.delete();
        File classFile = new File(path + File.separator + getServiceName() + ".class");
        if (classFile.exists()) classFile.delete();
        String proxyCode = JxplInterface.generateInterface(this);
        PrintWriter os = new PrintWriter(source);
        os.print(proxyCode);
        os.close();
        PrintWriter byteout = new PrintWriter(classFile);
        String[] args = new String[3];
        args[0] = "-cp";
        args[1] = System.getProperty("java.class.path");
        args[2] = source.getAbsolutePath();
        if (com.sun.tools.javac.Main.compile(args, byteout) > 0) {
            recorder.error("Unable to compile wrapper class, check parameters to ensure that the parameter names are non-numeric");
            throw new JxplMalformedException("Unable to compile wrapper class, check paramters to ensure that the parameter names are non-numeric");
        }
        byteout.close();
        recorder.debug("Proxy Class Compiled");
        return true;
    }

    /**
    *   Launch Stand Alone Axis2 Server
    *   are there any better ways to launch this...
    */
    private void startServer() throws Exception {
        recorder.info("Starting Server");
        String[] args = { "-repo", REPOSITORY, "-conf", "configs/axis2.xml" };
    }

    /**
    *   Package everything in a jar file that can be used later for deployment
    *   within any J2EE - Axis2 enabled container
    *   @param directory directory containing all package contents
    *   @param aarFile  File in which to generate the package AAR file
    */
    protected boolean generateAAR(String directory, File aarFile) throws IOException {
        FileOutputStream fout = new FileOutputStream(aarFile);
        FileInputStream fin;
        JarOutputStream jarOut = new JarOutputStream(new BufferedOutputStream(fout));
        jarDirectory(jarOut, "", directory);
        jarOut.close();
        return true;
    }

    /**
    *   Package everything in a jar file that can be used later for deployment
    *   within any J2EE - Axis2 enabled container
    *   @param directory directory containing all package contents
    */
    protected boolean generateAAR(String directory) throws IOException {
        if (pathToWar != null) generateAAR(directory, pathToWar); else {
            generateAAR(directory, new File(getServiceName() + ".aar"));
        }
        return true;
    }

    /**
    *   Recursively write files from the given directory structure to a jar archive file
    *   @param jarOut   JarOutputStream provided for the jar file
    *   @param pathInjar    How the directory structure will appear in the jar file
    *   @param actualPath   The actual path of the directory on the file system.
    */
    protected void jarDirectory(JarOutputStream jarOut, String pathInJar, String actualPath) throws IOException {
        FileInputStream fin;
        byte[] buffer = new byte[4096];
        String[] files = new File(actualPath).list();
        if (pathInJar.length() > 0) {
            pathInJar += "/";
            jarOut.putNextEntry(new JarEntry(pathInJar));
            jarOut.closeEntry();
        }
        for (int i = 0; i < files.length; i++) {
            if (new File(actualPath + "/" + files[i]).isDirectory()) {
                jarDirectory(jarOut, pathInJar + files[i], actualPath + "/" + files[i]);
            } else {
                fin = new FileInputStream(actualPath + "/" + files[i]);
                jarOut.putNextEntry(new JarEntry(pathInJar + files[i]));
                int length;
                while ((length = fin.read(buffer)) > 0) jarOut.write(buffer, 0, length);
                jarOut.closeEntry();
                fin.close();
            }
        }
    }
}
