package com.paullindorff.gwt.jaxrs.rebind;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.jboss.resteasy.annotations.Form;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.paullindorff.gwt.jaxrs.client.jso.BaseJSO;
import com.paullindorff.gwt.jaxrs.client.proxy.RESTResourceProxy;
import com.paullindorff.gwt.jaxrs.client.proxy.ResponseReader;
import com.paullindorff.gwt.jaxrs.client.proxy.VoidResponseReader;

/**
 * Creates a client-side proxy for a WebServiceResource, implementing the associated REST interface
 * @author plindorff
 *
 */
public class RESTProxyCreator {

    private static final String REST_SUFFIX = "REST";

    private static final String JSO_SUFFIX = "JSO";

    private static final String PROXY_SUFFIX = "Proxy";

    private static final String PROXY_SUFFIX_DELIM = "_";

    private static final char PATH_PARAM_START_DELIM = '{';

    private static final char PATH_PARAM_END_DELIM = '}';

    private JClassType resourceInterface;

    private JClassType responseReaderInterface;

    private JPackage restInterfacePackage;

    private JPackage jsoPackage;

    public RESTProxyCreator(JClassType resourceInterface, JClassType responseReaderInterface, JPackage restInterfacePackage, JPackage jsoPackage) {
        this.resourceInterface = resourceInterface;
        this.responseReaderInterface = responseReaderInterface;
        this.restInterfacePackage = restInterfacePackage;
        this.jsoPackage = jsoPackage;
    }

    /**
	 * Entry point for proxy creation.  After the proxy class has been written, the fully-qualified class name of the proxy is returned.
	 * @param logger
	 * @param context
	 * @return
	 * @throws UnableToCompleteException
	 */
    public String create(TreeLogger logger, GeneratorContext context) throws UnableToCompleteException {
        TypeOracle typeOracle = context.getTypeOracle();
        JClassType restInterface = typeOracle.findType(restInterfacePackage.getName() + "." + resourceInterface.getSimpleSourceName() + REST_SUFFIX);
        if (restInterface == null) {
            logger.log(TreeLogger.ERROR, "could not find associated REST interface for " + resourceInterface.getQualifiedSourceName() + " in package " + restInterfacePackage.getName());
            throw new UnableToCompleteException();
        }
        String rootPath = "";
        if (resourceInterface.isAnnotationPresent(Path.class)) {
            rootPath = resourceInterface.getAnnotation(Path.class).value();
            logger.log(TreeLogger.DEBUG, "found annotation indicating root path: " + rootPath);
        }
        RESTInterfaceMapper mapper = new RESTInterfaceMapper(logger, typeOracle);
        Map<JMethod, JMethod> resourceToRESTMap = mapper.mapResourceMethods(logger, resourceInterface, restInterface);
        SourceWriter restSrcWriter = getRESTProxySourceWriter(logger, context, restInterface);
        if (restSrcWriter == null) {
            logger.log(TreeLogger.DEBUG, "proxy class already exists: " + getRESTProxyQualifiedName(restInterface));
            return getRESTProxyQualifiedName(restInterface);
        }
        generateRESTProxyMethods(restSrcWriter, typeOracle, rootPath, resourceToRESTMap, logger);
        restSrcWriter.commit(logger);
        Set<JType> resourceReturnTypes = new HashSet<JType>();
        for (JMethod method : resourceInterface.getOverridableMethods()) resourceReturnTypes.add(method.getReturnType());
        logger.log(TreeLogger.DEBUG, "resourceReturnTypes: " + resourceReturnTypes);
        generateResponseReaderProxies(logger, context, typeOracle, resourceReturnTypes);
        String restProxyFQClassName = getRESTProxyQualifiedName(restInterface);
        logger.log(TreeLogger.DEBUG, "rest proxy successfully generated: " + restProxyFQClassName);
        return restProxyFQClassName;
    }

    /**
	 * Generates a proxy method with the signature specified by the REST interface for each method in the WebServiceResource interface
	 * @param writer
	 * @param typeOracle
	 * @param rootPath
	 * @param resourceToRESTMap
	 */
    private void generateRESTProxyMethods(SourceWriter writer, TypeOracle typeOracle, String rootPath, Map<JMethod, JMethod> resourceToRESTMap, TreeLogger logger) {
        JMethod[] resourceMethods = resourceInterface.getOverridableMethods();
        for (JMethod resourceMethod : resourceMethods) {
            JMethod restMethod = resourceToRESTMap.get(resourceMethod);
            if (restMethod != null) {
                resourceMethod = GWTSourceUtils.getGenericVersion(resourceMethod);
                generateRESTProxyMethod(writer, typeOracle, rootPath, resourceMethod, restMethod, logger);
            }
        }
    }

    /**
	 * Generates a single proxy method, defined by the restMethod, using annotations in the resourceMethod as guidance
	 * @param writer
	 * @param typeOracle
	 * @param rootPath
	 * @param resourceMethod
	 * @param restMethod
	 */
    private void generateRESTProxyMethod(SourceWriter writer, TypeOracle typeOracle, String rootPath, JMethod resourceMethod, JMethod restMethod, TreeLogger logger) {
        writer.println();
        JType restReturnType = restMethod.getReturnType().getErasedType();
        writer.print("public ");
        writer.print(restReturnType.getQualifiedSourceName());
        writer.print(" ");
        writer.print(restMethod.getName() + "(");
        boolean needsComma = false;
        NameFactory nameFactory = new NameFactory();
        JParameter[] asyncParams = restMethod.getParameters();
        for (int i = 0; i < asyncParams.length; ++i) {
            JParameter param = asyncParams[i];
            if (needsComma) writer.print(", "); else needsComma = true;
            JType paramType = param.getType().getErasedType();
            writer.print(paramType.getQualifiedSourceName());
            writer.print(" ");
            String paramName = param.getName();
            nameFactory.addName(paramName);
            writer.print(paramName);
        }
        writer.println(")");
        writer.println("{");
        writer.indent();
        String httpMethod = getHTTPMethodFromAnnotation(resourceMethod);
        writer.println("String httpMethod = \"" + httpMethod + "\";");
        String methodPath = "/";
        if (resourceMethod.isAnnotationPresent(Path.class)) methodPath = resourceMethod.getAnnotation(Path.class).value();
        String path = rootPath + methodPath;
        path = stripRegexFromPath(path);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        writer.println("String path = \"" + path + "\";");
        writer.println("GWT.log(\"path=\" + path);");
        writer.println("Object requestBody = null;");
        writer.println("StringBuilder query = new StringBuilder();");
        boolean needsAmpersand = false;
        boolean requestBodyPresent = false;
        boolean queryPresent = false;
        JType requestBodyType = null;
        for (JParameter param : resourceMethod.getParameters()) {
            TreeLogger paramLogger = logger.branch(TreeLogger.DEBUG, "processing param: " + param.getName());
            if (param.isAnnotationPresent(PathParam.class)) {
                paramLogger.log(TreeLogger.DEBUG, "will be used as a path element");
                writePathItem(param, null, writer);
            } else if (param.isAnnotationPresent(QueryParam.class)) {
                paramLogger.log(TreeLogger.DEBUG, "will be used as a query parameter");
                queryPresent = true;
                writeQueryItem(param, null, writer, needsAmpersand);
                if (!needsAmpersand) needsAmpersand = true;
            } else if (param.isAnnotationPresent(Context.class)) {
                paramLogger.log(TreeLogger.DEBUG, "skipping @Context parameter");
            } else if (param.isAnnotationPresent(Form.class)) {
                TreeLogger formLogger = paramLogger.branch(TreeLogger.DEBUG, "@Form annotation found, unwrapping value object");
                for (JField field : param.getType().isClass().getFields()) {
                    formLogger.log(TreeLogger.DEBUG, "processing field: " + field.getName());
                    if (field.isAnnotationPresent(PathParam.class)) {
                        formLogger.log(TreeLogger.DEBUG, "will be used as a path element");
                        writePathItem(field, param, writer);
                    } else if (field.isAnnotationPresent(QueryParam.class)) {
                        formLogger.log(TreeLogger.DEBUG, "will be used as a query parameter");
                        queryPresent = true;
                        writeQueryItem(field, param, writer, needsAmpersand);
                        if (!needsAmpersand) needsAmpersand = true;
                    } else if (field.isAnnotationPresent(Context.class)) {
                        formLogger.log(TreeLogger.DEBUG, "skipping @Context field");
                    }
                }
            } else {
                paramLogger.log(TreeLogger.DEBUG, "will be used as request body, type is " + param.getType());
                requestBodyPresent = true;
                requestBodyType = param.getType();
                writer.println("requestBody = " + param.getName() + ";");
            }
        }
        if (queryPresent) writer.println("String fullPath = path + \"?\" + query.toString();"); else writer.println("String fullPath = path;");
        JType resourceReturnType = resourceMethod.getReturnType();
        if (isResponseReaderVoid(resourceReturnType)) writer.println("ResponseReader responseReader = new VoidResponseReader();"); else writer.println("ResponseReader responseReader = new " + getResponseReaderProxySimpleName(resourceReturnType) + "();");
        JParameter callbackParam = asyncParams[asyncParams.length - 1];
        String callbackParamName = callbackParam.getName();
        JParameter targetParam = asyncParams[asyncParams.length - 2];
        String targetParamName = targetParam.getName();
        JParameter authParam = asyncParams[asyncParams.length - 3];
        String authParamName = authParam.getName();
        if (requestBodyPresent) {
            writer.println("try {");
            writer.indent();
            writer.println(requestBodyType.getSimpleSourceName() + "JSO jso = (" + requestBodyType.getSimpleSourceName() + "JSO)requestBody;");
            writer.println("String requestBodyString = jso.toJSON();");
            writer.println("performRESTRequest(httpMethod, fullPath, requestBodyString, responseReader, " + authParamName + ", " + targetParamName + ", " + callbackParamName + ");");
            writer.outdent();
            writer.println("} catch (Exception e) {");
            writer.indent();
            writer.println("callback.onFailure(e);");
            writer.outdent();
            writer.println("}");
        } else {
            writer.println("performRESTRequest(httpMethod, fullPath, responseReader, " + authParamName + ", " + targetParamName + ", " + callbackParamName + ");");
        }
        writer.outdent();
        writer.println("}");
    }

    /**
	 * Generates a ResponseReader proxy for each eligible type listed, dispatching to the corresponding JSO implementation
	 * @param logger
	 * @param context
	 * @param typeOracle
	 * @param types
	 */
    private void generateResponseReaderProxies(TreeLogger logger, GeneratorContext context, TypeOracle typeOracle, Set<JType> types) throws UnableToCompleteException {
        for (JType type : types) {
            if (isResponseReaderVoid(type)) continue;
            TreeLogger branch = logger.branch(TreeLogger.INFO, "generating response reader proxy for " + type.getSimpleSourceName());
            String jsoName = getJSOQualifiedName(type.isInterface());
            if (jsoName == null) {
                branch.log(TreeLogger.ERROR, "invalid type specified (" + type.getQualifiedSourceName() + ").  JSOs must implement an interface.");
                throw new UnableToCompleteException();
            }
            JClassType jsoType = typeOracle.findType(jsoName);
            if (jsoType == null) {
                branch.log(TreeLogger.ERROR, "unable to find corresponding JSO class for interface " + type.getQualifiedSourceName());
                throw new UnableToCompleteException();
            }
            SourceWriter responseReaderSrcWriter = getResponseReaderProxySourceWriter(logger, context, type, jsoType);
            if (responseReaderSrcWriter == null) {
                branch.log(TreeLogger.INFO, "reader class already exists: " + getResponseReaderProxyQualifiedName(type.isInterface()));
            } else {
                generateResponseReaderMethod(responseReaderSrcWriter, typeOracle, type, jsoType);
                responseReaderSrcWriter.commit(logger);
                branch.log(TreeLogger.INFO, "ResponseReader proxy successfully generated: " + getResponseReaderProxyQualifiedName(type.isInterface()));
            }
        }
    }

    /**
	 * Generates the "read" method for a ResponseReader JSO proxy, delegating to the JSO type to create itself from the given JSON string
	 * @param writer
	 * @param typeOracle
	 * @param returnType
	 * @param jsoClass
	 */
    private void generateResponseReaderMethod(SourceWriter writer, TypeOracle typeOracle, JType returnType, JClassType jsoClass) {
        writer.println("public " + returnType.getSimpleSourceName() + " read(String response) {");
        writer.indent();
        writer.println("return " + jsoClass.getSimpleSourceName() + ".create(response);");
        writer.outdent();
        writer.println("}");
    }

    /**
	 * Creates a SourceWriter for writing a new class definition
	 * This method returns null if a class with the same fully-qualified name already exists.
	 * @param logger
	 * @param ctx
	 * @param targetInterface
	 * @return
	 */
    private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext context, String packageName, String classSimpleName, String superClass, String[] implementedInterfaces, String[] imports) {
        PrintWriter printWriter = context.tryCreate(logger, packageName, classSimpleName);
        if (printWriter == null) {
            return null;
        }
        ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, classSimpleName);
        if (imports != null) {
            for (String typeName : imports) composerFactory.addImport(typeName);
        }
        if (superClass != null) composerFactory.setSuperclass(superClass);
        if (implementedInterfaces != null) {
            for (String intfName : implementedInterfaces) composerFactory.addImplementedInterface(intfName);
        }
        return composerFactory.createSourceWriter(context, printWriter);
    }

    /**
	 * Creates a SourceWriter for a generated implementation of the given REST interface
	 * @param logger
	 * @param context
	 * @param targetInterface
	 * @return
	 */
    private SourceWriter getRESTProxySourceWriter(TreeLogger logger, GeneratorContext context, JClassType targetInterface) {
        JPackage targetPackage = targetInterface.getPackage();
        String packageName = targetPackage == null ? "" : targetPackage.getName();
        String classSimpleName = getRESTProxySimpleName(targetInterface);
        String[] imports = { RESTResourceProxy.class.getCanonicalName(), ResponseReader.class.getCanonicalName(), VoidResponseReader.class.getCanonicalName(), SerializationException.class.getCanonicalName(), Logger.class.getCanonicalName(), jsoPackage.getName() + ".*", ResponseReader.class.getPackage().getName() + ".*" };
        String superClass = RESTResourceProxy.class.getSimpleName();
        String[] implementedInterfaces = { targetInterface.getErasedType().getQualifiedSourceName() };
        return getSourceWriter(logger, context, packageName, classSimpleName, superClass, implementedInterfaces, imports);
    }

    /**
	 * Creates a SourceWriter for a generated implementation of ResponseReader for the given JSO type
	 * @param logger
	 * @param context
	 * @param returnType
	 * @param jsoType
	 * @return
	 */
    private SourceWriter getResponseReaderProxySourceWriter(TreeLogger logger, GeneratorContext context, JType returnType, JClassType jsoType) {
        JPackage targetPackage = responseReaderInterface.getPackage();
        String packageName = targetPackage == null ? "" : targetPackage.getName();
        String classSimpleName = getResponseReaderProxySimpleName(returnType);
        String[] imports = { responseReaderInterface.getQualifiedSourceName(), BaseJSO.class.getCanonicalName(), returnType.getQualifiedSourceName(), jsoType.getQualifiedSourceName() };
        String[] implementedInterfaces = { responseReaderInterface.getErasedType().getQualifiedSourceName() };
        return getSourceWriter(logger, context, packageName, classSimpleName, null, implementedInterfaces, imports);
    }

    /**
	 * Constructs the fully-qualified class name for the REST proxy class
	 * @param c
	 * @return
	 */
    private String getRESTProxyQualifiedName(JClassType c) {
        String[] name = GWTSourceUtils.synthesizeTopLevelClassName(c, PROXY_SUFFIX_DELIM + PROXY_SUFFIX);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    /**
	 * Constructs the "simple" (name only) class name for the REST proxy class
	 * @param c
	 * @return
	 */
    private String getRESTProxySimpleName(JClassType c) {
        String[] name = GWTSourceUtils.synthesizeTopLevelClassName(c, PROXY_SUFFIX_DELIM + PROXY_SUFFIX);
        return name[1];
    }

    /**
	 * Constructs the fully-qualified class name for the ResponseReader proxy class
	 * @param c
	 * @return
	 */
    private String getResponseReaderProxyQualifiedName(JType subType) {
        String[] name = GWTSourceUtils.synthesizeTopLevelClassName(responseReaderInterface, PROXY_SUFFIX_DELIM + subType.getSimpleSourceName() + PROXY_SUFFIX_DELIM + PROXY_SUFFIX);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    /**
	 * Constructs the "simple" (name only) class name for the ResponseReader proxy class
	 * @param c
	 * @return
	 */
    private String getResponseReaderProxySimpleName(JType subType) {
        String[] name = GWTSourceUtils.synthesizeTopLevelClassName(responseReaderInterface, PROXY_SUFFIX_DELIM + subType.getSimpleSourceName() + PROXY_SUFFIX_DELIM + PROXY_SUFFIX);
        return name[1];
    }

    /**
	 * Constructs the fully-qualified class name for the JSO implementation of the specified interface
	 * @param c
	 * @return
	 */
    private String getJSOQualifiedName(JClassType c) {
        return jsoPackage.getName() + "." + getJSOSimpleName(c);
    }

    /**
	 * Constructs the "simple" (name only) class name for the JSO implementation of the specified interface
	 * @param c
	 * @return
	 */
    private String getJSOSimpleName(JClassType c) {
        return c.getSimpleSourceName() + JSO_SUFFIX;
    }

    /**
	 * RESTEasy allows for validation of path parameters with regular expressions, as in the following:<br>
	 * 		<code>
	 * 			@PathParam("/{eventID: [0-9]+}")
	 * 		</code>
	 * While this is very helpful on the server side, we don't need to validate in our client proxy -- we want just the parameter name.
	 * This method distills the above down to: "/{eventID}", which is ready for token substitution.
	 * @param path
	 * @return
	 */
    private String stripRegexFromPath(String path) {
        if (path.contains(":")) {
            StringBuilder sb = new StringBuilder();
            String[] segments = path.split(":");
            for (String segment : segments) {
                if (segment.contains("}")) {
                    if (segment.endsWith("}")) {
                        sb.append("}");
                    } else {
                        sb.append(segment.substring(segment.indexOf("}/")));
                    }
                } else {
                    sb.append(segment);
                }
            }
            return sb.toString();
        } else {
            return path;
        }
    }

    /**
	 * Returns a String representation of the HTTPMethod based on the existence of one of the appropriate JAX-RS annotations
	 * @param method
	 * @return
	 */
    private String getHTTPMethodFromAnnotation(JMethod method) {
        String httpMethod;
        if (method.isAnnotationPresent(GET.class)) httpMethod = "GET"; else if (method.isAnnotationPresent(POST.class)) httpMethod = "POST"; else if (method.isAnnotationPresent(PUT.class)) httpMethod = "PUT"; else if (method.isAnnotationPresent(DELETE.class)) httpMethod = "DELETE"; else if (method.isAnnotationPresent(HEAD.class)) httpMethod = "HEAD"; else httpMethod = "GET";
        return httpMethod;
    }

    private boolean isResponseReaderVoid(JType type) {
        return ((type.getQualifiedSourceName().equals(javax.ws.rs.core.Response.class.getCanonicalName())) || (type.isPrimitive() != null) || (type.isArray() != null));
    }

    private boolean isBodyParameter(JParameter parameter) {
        return (!(parameter.isAnnotationPresent(PathParam.class) || parameter.isAnnotationPresent(QueryParam.class) || parameter.isAnnotationPresent(Context.class)));
    }

    /**
	 * Writes code that will swap a path placeholder with the item's value
	 * @param item
	 * @param writer
	 * @param parent
	 */
    private void writePathItem(HasAnnotations item, HasAnnotations parent, SourceWriter writer) {
        String itemName = parent == null ? getName(item) : constructGetterCall(getName(parent), getName(item));
        String pathToken = PATH_PARAM_START_DELIM + item.getAnnotation(PathParam.class).value() + PATH_PARAM_END_DELIM;
        writer.print("path = path.replace(\"" + pathToken + "\", ");
        boolean itemIsPrimitive = (getType(item).isPrimitive() != null);
        if (itemIsPrimitive) writer.print("String.valueOf(" + itemName + ")"); else writer.print("getStringValue(" + itemName + ", false)");
        writer.println(");");
    }

    /**
	 * Writes code that will append the given item to the proxy object's query string
	 * @param item
	 * @param writer
	 * @param needsAmpersand
	 * @param parent
	 */
    private void writeQueryItem(HasAnnotations item, HasAnnotations parent, SourceWriter writer, boolean needsAmpersand) {
        String itemName = parent == null ? getName(item) : constructGetterCall(getName(parent), getName(item));
        String queryParamName = item.getAnnotation(QueryParam.class).value();
        boolean itemIsPrimitive = (getType(item).isPrimitive() != null);
        if (!itemIsPrimitive) {
            writer.println("if (" + itemName + " != null) {");
            writer.indent();
        }
        writer.print("query.append(");
        if (needsAmpersand) writer.print("\"&\" + ");
        writer.print("\"" + queryParamName + "=\" + ");
        if (itemIsPrimitive) writer.print("String.valueOf(" + itemName + ")"); else writer.print("getStringValue(" + itemName + ", true)");
        writer.println(");");
        if (!itemIsPrimitive) {
            writer.outdent();
            writer.println("}");
        }
    }

    /**
	 * Extracts the name from JParameter and JField meta-classes, allowing us to use them in a polymorphic manner
	 * @param item
	 * @return
	 */
    private String getName(Object item) {
        if (item instanceof JParameter) return ((JParameter) item).getName();
        if (item instanceof JField) return ((JField) item).getName();
        return null;
    }

    /**
	 * Extracts the type from JParameter and JField meta-classes, allowing us to use them in a polymorphic manner
	 * @param item
	 * @return
	 */
    private JType getType(Object item) {
        if (item instanceof JParameter) return ((JParameter) item).getType();
        if (item instanceof JField) return ((JField) item).getType();
        return null;
    }

    /**
	 * Constructs a call to the getter for the given property on the given object
	 * @param instanceName
	 * @param propertyName
	 * @return
	 */
    private String constructGetterCall(String objectName, String propertyName) {
        return objectName + ".get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1) + "()";
    }
}
