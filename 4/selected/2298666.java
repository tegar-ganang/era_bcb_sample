package it.javalinux.wise.jaxCore;

import it.javalinux.wise.exceptions.WiseConnectionException;
import it.javalinux.wise.exceptions.WiseException;
import it.javalinux.wise.exceptions.WiseRuntimeException;
import it.javalinux.wise.jaxCore.utils.IDGenerator;
import it.javalinux.wise.jaxCore.utils.IOUtils;
import it.javalinux.wise.jaxCore.utils.JavaUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.tools.WSContractConsumer;
import sun.misc.BASE64Encoder;

/**
 * This is the Wise core, i.e. the JAX-WS client that handles wsdl retrieval &
 * parsing, invocations, etc.
 * 
 * @author Stefano Maestri, stefano.maestri@javalinux.it
 * @author Alessio Soldano, alessio.soldano@javalinux.it
 * 
 * @since
 */
public class WSDynamicClient implements Serializable {

    private static final long serialVersionUID = -7185945063107035243L;

    public static final String WISE_TARGET_PACKAGE = "it.javalinux.wise";

    @SuppressWarnings("unchecked")
    private Class serviceClass;

    private Object service;

    private Endpoint endpoint;

    private URLClassLoader classLoader;

    private Map<String, Endpoint> endpoints;

    private Map<String, Method> webMethods;

    private String tmpDeployDir;

    private String wsdlDir;

    public void init(String wsdlURL, String cid, String userName, String password) throws WiseConnectionException {
        this.init(wsdlURL, cid, userName, password, getCurrentTmpDeployDir());
    }

    /**
     * Init method: - retrieves the wsdl - parses it using WSContractConsume -
     * loads the generated classes from the temp area - locates the service
     * class
     * 
     * @throws WiseConnectionException
     *                 If a connection issue prevents the wsdl from being
     *                 downloaded
     */
    public void init(String wsdlURL, String cid, String userName, String password, String tmpDir) throws WiseConnectionException {
        try {
            tmpDeployDir = tmpDir;
            WSContractConsumer wsImporter = WSContractConsumer.newInstance(Thread.currentThread().getContextClassLoader());
            wsImporter.setGenerateSource(true);
            wsImporter.setMessageStream(System.out);
            List<String> cp = defineAdditionalCompilerClassPath();
            wsImporter.setAdditionalCompilerClassPath(cp);
            wsdlDir = tmpDeployDir + "/WSDLS/" + cid;
            File outputDir = new File(wsdlDir);
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
            wsImporter.setOutputDirectory(outputDir);
            wsImporter.setSourceDirectory(outputDir);
            wsImporter.setTargetPackage(WISE_TARGET_PACKAGE);
            wsImporter.consume(getUsableWSDL(wsdlURL, userName, password));
            String[] className = getClassNames(outputDir);
            classLoader = new URLClassLoader(new URL[] { outputDir.toURL() }, Thread.currentThread().getContextClassLoader());
            ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                JavaUtils.loadJavaType("com.sun.xml.ws.spi.ProviderImpl", classLoader);
                for (int i = 0; i < className.length; i++) {
                    serviceClass = JavaUtils.loadJavaType(WISE_TARGET_PACKAGE + "." + className[i], classLoader);
                    if (Service.class.isAssignableFrom(serviceClass)) {
                        break;
                    }
                }
                service = serviceClass.newInstance();
            } finally {
                Thread.currentThread().setContextClassLoader(oldLoader);
            }
        } catch (WiseConnectionException wce) {
            throw wce;
        } catch (Exception e) {
            WiseRuntimeException.rethrow("Error occurred while consuming wsdl: " + wsdlURL, e);
        }
    }

    /**
     * Gets a WSDL given its url and userName/password if needed.
     * 
     * @param wsdlURL
     *                The wsdl url
     * @param userName
     *                The username; empty string and null mean no username
     * @param password
     *                The password; empty string and null mean no password
     * @return The path to the temp file containing the requested wsdl
     * @throws WiseConnectionException
     *                 If an error occurs while downloading the wsdl
     */
    private String getUsableWSDL(String wsdlURL, String userName, String password) throws WiseConnectionException {
        if (StringUtils.trimToNull(userName) == null || StringUtils.trimToNull(password) == null) {
            return this.transferWSDL(wsdlURL, null);
        } else {
            return this.transferWSDL(wsdlURL, new StringBuffer(userName).append(":").append(password).toString());
        }
    }

    /**
     * Downloads the wsdl.
     * 
     * @throws WiseConnectionException
     *                 If the wsdl cannot be retrieved
     */
    private String transferWSDL(String wsdlURL, String userPassword) throws WiseConnectionException {
        String filePath = null;
        try {
            URL endpoint = new URL(wsdlURL);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            conn.setRequestProperty("Connection", "close");
            if (userPassword != null) {
                conn.setRequestProperty("Authorization", "Basic " + (new BASE64Encoder()).encode(userPassword.getBytes()));
            }
            InputStream is = null;
            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
                InputStreamReader isr = new InputStreamReader(is);
                StringWriter sw = new StringWriter();
                char[] buf = new char[200];
                int read = 0;
                while (read != -1) {
                    read = isr.read(buf);
                    sw.write(buf);
                }
                throw new WiseConnectionException("Remote server's response is an error: " + sw.toString());
            }
            File file = new File(tmpDeployDir, new StringBuffer("Wise").append(IDGenerator.nextVal()).append(".xml").toString());
            OutputStream fos = new BufferedOutputStream(new FileOutputStream(file));
            IOUtils.copyStream(fos, is);
            fos.close();
            is.close();
            filePath = file.getPath();
        } catch (WiseConnectionException wce) {
            throw wce;
        } catch (Exception e) {
            throw new WiseConnectionException("Wsdl download failed!", e);
        }
        return filePath;
    }

    /**
     * Gets an array containing the generated class names
     * 
     * @param outputDir
     * @return
     */
    private String[] getClassNames(File outputDir) {
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".class");
            }
        };
        File scanDir = new File(outputDir.getAbsolutePath() + "/" + WISE_TARGET_PACKAGE.replaceAll("\\.", "/") + "/");
        System.out.println(scanDir);
        String[] children = scanDir.list(filter);
        for (int i = 0; i < children.length; i++) {
            children[i] = children[i].substring(0, children[i].length() - 6);
        }
        return children;
    }

    /**
     * This is used load libraries required by tests and usually not available
     * when running out of container.
     * 
     * @return A list of paths
     */
    private List<String> defineAdditionalCompilerClassPath() {
        List<String> cp = new LinkedList<String>();
        cp.add(Thread.currentThread().getContextClassLoader().getResource("jboss-jaxws.jar").getPath());
        cp.add(Thread.currentThread().getContextClassLoader().getResource("jaxb-api.jar").getPath());
        cp.add(Thread.currentThread().getContextClassLoader().getResource("jaxb-impl.jar").getPath());
        cp.add(tmpDeployDir + "jaxb-xjc.jar");
        return cp;
    }

    /**
     * 
     * @return The path of the dir to be used for deploys
     */
    private String getCurrentTmpDeployDir() {
        String tmpDepDir = Thread.currentThread().getContextClassLoader().getResource("Wise.ejb3").getPath().substring(5);
        tmpDepDir = tmpDepDir.substring(0, tmpDepDir.lastIndexOf("!")) + "-contents/";
        return tmpDepDir;
    }

    /**
     * Create the endpoints' map and gives the their list back.
     * 
     * @return The list of WebEndpoint names
     */
    public List<Endpoint> processEndpoints() {
        endpoints = new HashMap<String, Endpoint>();
        List<Endpoint> result = new LinkedList<Endpoint>();
        for (Method method : serviceClass.getMethods()) {
            WebEndpoint annotation = method.getAnnotation(WebEndpoint.class);
            if (annotation != null) {
                Endpoint ep = this.getWiseEndpoint(annotation, method);
                result.add(ep);
                endpoints.put(ep.getName(), ep);
            }
        }
        return result;
    }

    private Endpoint getWiseEndpoint(WebEndpoint annotation, Method method) {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        Endpoint ep = new Endpoint();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ep.setName(annotation.name());
            ep.setMethodName(method.getName());
            ep.setInstance(serviceClass.getMethod(method.getName(), (Class[]) method.getParameterTypes()).invoke(service, (Object[]) null));
            System.out.println(ep.getName() + " endpoint address: " + ep.getUrl());
            ep.setClazz(serviceClass.getMethod(method.getName(), (Class[]) method.getParameterTypes()).getReturnType());
        } catch (Exception e) {
            WiseRuntimeException.rethrow("Error while reading an endpoint!", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
        return ep;
    }

    /**
     * Set the data of the endpoint to be used for invocations.
     * 
     * @param endpointName
     * @param customUrl
     *                The endpoint url; use empty or null for default url
     * @param userName
     * @param password
     */
    public void selectEndpoint(String endpointName, String customUrl, String userName, String password) {
        endpoint = endpoints.get(endpointName);
        if (StringUtils.trimToNull(userName) != null && StringUtils.trimToNull(password) != null) {
            endpoint.setUsername(userName);
            endpoint.setPassword(password);
        }
        if (customUrl != null && !"".equalsIgnoreCase(customUrl)) {
            endpoint.setUrl(customUrl);
        }
        System.out.println("endpoint address: " + endpoint.getUrl());
    }

    /**
     * Create the webmethods' map and gives the list of their names back.
     * 
     * @return The list of WebMethod names
     */
    public Collection<String> getWebMethod() {
        webMethods = new HashMap<String, Method>();
        for (Method method : endpoint.getClazz().getMethods()) {
            WebMethod annotation = method.getAnnotation(WebMethod.class);
            if (annotation != null) {
                webMethods.put(method.getName(), method);
            }
        }
        return new TreeSet<String>(webMethods.keySet());
    }

    /**
     * Invokes the given method with the provided arguments
     * 
     * @param methodName
     * @param args
     * @return
     * @throws WiseException
     *                 If an unknown exception is received
     */
    public InvocationResult invokeOperation(String methodName, Object[] args) throws WiseException {
        Method methodToInvoke = webMethods.get(methodName);
        Method methodPointer = null;
        InvocationResult result = new InvocationResult();
        try {
            Object epInstance = endpoint.getInstance();
            methodPointer = epInstance.getClass().getMethod(methodToInvoke.getName(), (Class[]) methodToInvoke.getParameterTypes());
            System.out.print(" invoking:" + methodName);
            result.setValue(methodPointer.invoke(epInstance, args));
            if (!isOneWay(methodName)) {
                result.setName("result");
                result.setType(methodToInvoke.getGenericReturnType());
            }
        } catch (InvocationTargetException ite) {
            System.out.print("error invoking:" + methodName);
            System.out.print("error invoking:" + args);
            for (int i = 0; i < methodPointer.getExceptionTypes().length; i++) {
                if (ite.getCause().getClass().isAssignableFrom(methodPointer.getExceptionTypes()[i])) {
                    result.setName("exception");
                    result.setValue(ite.getCause());
                    result.setType(ite.getCause().getClass());
                    return result;
                }
            }
            WiseException.rethrow("Unknown exception received: " + ite.getMessage(), ite);
        } catch (Exception e) {
            WiseRuntimeException.rethrow("Error during method invocation!", e);
        }
        return result;
    }

    /**
     * Gets the list of WebParameters for a selected method
     * 
     * @param methodName
     * @return
     */
    public List<WebParameter> getWebParams(String methodName) {
        System.out.println("methodName-> " + methodName);
        LinkedList<WebParameter> parameters = new LinkedList<WebParameter>();
        Method method = webMethods.get(methodName);
        Annotation[][] annotations = method.getParameterAnnotations();
        Type[] methodparameterTypes = method.getGenericParameterTypes();
        for (int i = 0; i < annotations.length; i++) {
            for (int j = 0; j < annotations[i].length; j++) {
                if (annotations[i][j] instanceof WebParam) {
                    System.out.println("name -> " + ((WebParam) annotations[i][j]).name());
                    System.out.println("type -> " + methodparameterTypes[i]);
                    parameters.add(new WebParameter(methodparameterTypes[i], ((WebParam) annotations[i][j]).name()));
                    break;
                }
            }
        }
        return parameters;
    }

    public boolean isOneWay(String methodName) {
        Method method = webMethods.get(methodName);
        return method.getAnnotation(Oneway.class) != null;
    }

    @SuppressWarnings("unchecked")
    public Object instanceXmlElementDecl(String name, Class scope, String namespace, Object value) {
        try {
            Class objectFatoryClass = null;
            ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                JavaUtils.loadJavaType("com.sun.xml.ws.spi.ProviderImpl", classLoader);
                objectFatoryClass = JavaUtils.loadJavaType("it.javalinux.wise.ObjectFactory", classLoader);
            } finally {
                Thread.currentThread().setContextClassLoader(oldLoader);
            }
            Method[] methods = objectFatoryClass.getMethods();
            Method methodToUse = null;
            for (int i = 0; i < methods.length; i++) {
                XmlElementDecl annotation = methods[i].getAnnotation(XmlElementDecl.class);
                if (annotation != null && name.equals(annotation.name()) && (annotation.namespace() == null || annotation.namespace().equals(namespace)) && (annotation.scope() == null || annotation.scope().equals(scope))) {
                    methodToUse = methods[i];
                    break;
                }
            }
            Object obj = objectFatoryClass.newInstance();
            if (methodToUse != null) {
                Logger.getLogger(this.getClass()).debug(methodToUse + " with value=" + value);
                return methodToUse.invoke(obj, new Object[] { value });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Class getServiceClass() {
        return serviceClass;
    }

    @SuppressWarnings("unchecked")
    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }
}
