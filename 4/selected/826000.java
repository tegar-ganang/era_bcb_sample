package com.google.gwt.user.rebind.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.GenUtil;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteServiceAbsolutePath;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.XDRCallInfo;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.FailedRequest;
import com.google.gwt.user.client.rpc.impl.FailingRequestBuilder;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

class XDRProxyCreator extends ProxyCreator {

    private static final String ENTRY_POINT_TAG = "gwt.defaultEntryPoint";

    private static final Map<JPrimitiveType, ResponseReader> JPRIMITIVETYPE_TO_RESPONSEREADER = new HashMap<JPrimitiveType, ResponseReader>();

    private static final String PROXY_SUFFIX = "_Proxy";

    private JClassType m_serviceIntf;

    {
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.BOOLEAN, ResponseReader.BOOLEAN);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.BYTE, ResponseReader.BYTE);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.CHAR, ResponseReader.CHAR);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.DOUBLE, ResponseReader.DOUBLE);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.FLOAT, ResponseReader.FLOAT);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.INT, ResponseReader.INT);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.LONG, ResponseReader.LONG);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.SHORT, ResponseReader.SHORT);
        JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.VOID, ResponseReader.VOID);
    }

    public XDRProxyCreator(JClassType pServiceIntf) {
        super(pServiceIntf);
        this.m_serviceIntf = pServiceIntf;
    }

    public String create(TreeLogger logger, GeneratorContext context) throws UnableToCompleteException {
        TypeOracle typeOracle = context.getTypeOracle();
        TreeLogger javadocAnnotationDeprecationBranch = null;
        if (GenUtil.warnAboutMetadata()) {
            javadocAnnotationDeprecationBranch = logger.branch(TreeLogger.TRACE, "Scanning this RemoteService for deprecated annotations; " + "Please see " + RemoteServiceRelativePath.class.getName() + " for more information.", null);
        }
        JClassType serviceAsync = typeOracle.findType(m_serviceIntf.getQualifiedSourceName() + "Async");
        if (serviceAsync == null) {
            logger.branch(TreeLogger.ERROR, "Could not find an asynchronous version for the service interface " + m_serviceIntf.getQualifiedSourceName(), null);
            RemoteServiceAsyncValidator.logValidAsyncInterfaceDeclaration(logger, m_serviceIntf);
            throw new UnableToCompleteException();
        }
        SourceWriter srcWriter = getSourceWriter(logger, context, serviceAsync);
        if (srcWriter == null) {
            return getProxyQualifiedName();
        }
        RemoteServiceAsyncValidator rsav = new RemoteServiceAsyncValidator(logger, typeOracle);
        Map<JMethod, JMethod> syncMethToAsyncMethMap = rsav.validate(logger, m_serviceIntf, serviceAsync);
        SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(logger, context.getPropertyOracle(), typeOracle);
        try {
            addRequiredRoots(logger, typeOracle, stob);
            addRemoteServiceRootTypes(logger, typeOracle, stob, m_serviceIntf);
        } catch (NotFoundException e) {
            logger.log(TreeLogger.ERROR, "", e);
            throw new UnableToCompleteException();
        }
        OutputStream pathInfo = context.tryCreateResource(logger, m_serviceIntf.getQualifiedSourceName() + ".rpc.log");
        stob.setLogOutputStream(pathInfo);
        SerializableTypeOracle sto = stob.build(logger);
        if (pathInfo != null) {
            context.commitResource(logger, pathInfo).setPrivate(true);
        }
        TypeSerializerCreator tsc = new TypeSerializerCreator(logger, sto, context, sto.getTypeSerializerQualifiedName(m_serviceIntf));
        tsc.realize(logger);
        String serializationPolicyStrongName = writeSerializationPolicyFile(logger, context, sto);
        generateProxyFields(srcWriter, sto, serializationPolicyStrongName);
        generateProxyContructor(javadocAnnotationDeprecationBranch, srcWriter);
        generateProxyMethods(srcWriter, sto, syncMethToAsyncMethMap);
        srcWriter.commit(logger);
        return getProxyQualifiedName();
    }

    private String getProxyQualifiedName() {
        String[] name = Shared.synthesizeTopLevelClassName(m_serviceIntf, PROXY_SUFFIX);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    private static void addRequiredRoots(TreeLogger logger, TypeOracle typeOracle, SerializableTypeOracleBuilder stob) throws NotFoundException {
        logger = logger.branch(TreeLogger.DEBUG, "Analyzing implicit types");
        JClassType stringType = typeOracle.getType(String.class.getName());
        stob.addRootType(logger, stringType);
        JClassType icseType = typeOracle.getType(IncompatibleRemoteServiceException.class.getName());
        stob.addRootType(logger, icseType);
    }

    private static void addRemoteServiceRootTypes(TreeLogger logger, TypeOracle typeOracle, SerializableTypeOracleBuilder stob, JClassType remoteService) throws NotFoundException {
        logger = logger.branch(TreeLogger.DEBUG, "Analyzing '" + remoteService.getParameterizedQualifiedSourceName() + "' for serializable types", null);
        JMethod[] methods = remoteService.getOverridableMethods();
        JClassType exceptionClass = typeOracle.getType(Exception.class.getName());
        TreeLogger validationLogger = logger.branch(TreeLogger.DEBUG, "Analyzing methods:", null);
        for (JMethod method : methods) {
            TreeLogger methodLogger = validationLogger.branch(TreeLogger.DEBUG, method.toString(), null);
            JType returnType = method.getReturnType();
            if (returnType != JPrimitiveType.VOID) {
                TreeLogger returnTypeLogger = methodLogger.branch(TreeLogger.DEBUG, "Return type: " + returnType.getParameterizedQualifiedSourceName(), null);
                stob.addRootType(returnTypeLogger, returnType);
            }
            JParameter[] params = method.getParameters();
            for (JParameter param : params) {
                TreeLogger paramLogger = methodLogger.branch(TreeLogger.DEBUG, "Parameter: " + param.toString(), null);
                JType paramType = param.getType();
                stob.addRootType(paramLogger, paramType);
            }
            JType[] exs = method.getThrows();
            if (exs.length > 0) {
                TreeLogger throwsLogger = methodLogger.branch(TreeLogger.DEBUG, "Throws:", null);
                for (JType ex : exs) {
                    if (!exceptionClass.isAssignableFrom(ex.isClass())) {
                        throwsLogger = throwsLogger.branch(TreeLogger.WARN, "'" + ex.getQualifiedSourceName() + "' is not a checked exception; only checked exceptions may be used", null);
                    }
                    stob.addRootType(throwsLogger, ex);
                }
            }
        }
    }

    private String writeSerializationPolicyFile(TreeLogger logger, GeneratorContext ctx, SerializableTypeOracle sto) throws UnableToCompleteException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos, SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING);
            PrintWriter pw = new PrintWriter(osw);
            JType[] serializableTypes = sto.getSerializableTypes();
            for (int i = 0; i < serializableTypes.length; ++i) {
                JType serializableType = serializableTypes[i];
                String binaryTypeName = sto.getSerializedTypeName(serializableType);
                boolean maybeInstantiated = sto.maybeInstantiated(serializableType);
                pw.print(binaryTypeName + ", " + Boolean.toString(maybeInstantiated) + '\n');
            }
            pw.close();
            byte[] serializationPolicyFileContents = baos.toByteArray();
            String serializationPolicyName = Util.computeStrongName(serializationPolicyFileContents);
            String serializationPolicyFileName = SerializationPolicyLoader.getSerializationPolicyFileName(serializationPolicyName);
            OutputStream os = ctx.tryCreateResource(logger, serializationPolicyFileName);
            if (os != null) {
                os.write(serializationPolicyFileContents);
                ctx.commitResource(logger, os);
            } else {
                logger.log(TreeLogger.TRACE, "SerializationPolicy file for RemoteService '" + m_serviceIntf.getQualifiedSourceName() + "' already exists; no need to rewrite it.", null);
            }
            return serializationPolicyName;
        } catch (UnsupportedEncodingException e) {
            logger.log(TreeLogger.ERROR, SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING + " is not supported", e);
            throw new UnableToCompleteException();
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, null, e);
            throw new UnableToCompleteException();
        }
    }

    private void generateProxyFields(SourceWriter srcWriter, SerializableTypeOracle serializableTypeOracle, String serializationPolicyStrongName) {
        srcWriter.println("private static final String REMOTE_SERVICE_INTERFACE_NAME = \"" + serializableTypeOracle.getSerializedTypeName(m_serviceIntf) + "\";");
        srcWriter.println("private static final String SERIALIZATION_POLICY =\"" + serializationPolicyStrongName + "\";");
        String typeSerializerName = serializableTypeOracle.getTypeSerializerQualifiedName(m_serviceIntf);
        srcWriter.println("private static final " + typeSerializerName + " SERIALIZER = new " + typeSerializerName + "();");
        srcWriter.println();
    }

    private void generateProxyContructor(TreeLogger javadocAnnotationDeprecationBranch, SourceWriter srcWriter) {
        srcWriter.println("public " + getProxySimpleName() + "() {");
        srcWriter.indent();
        srcWriter.println("super(GWT.getModuleBaseURL(),");
        srcWriter.indent();
        srcWriter.println(getRemoteServiceRelativePath(javadocAnnotationDeprecationBranch) + ", ");
        srcWriter.println(getRemoteServiceAbsolutePathPath(javadocAnnotationDeprecationBranch) + ", ");
        srcWriter.println("SERIALIZATION_POLICY, ");
        srcWriter.println("SERIALIZER);");
        srcWriter.outdent();
        srcWriter.outdent();
        srcWriter.println("}");
    }

    private String getRemoteServiceAbsolutePathPath(TreeLogger javadocAnnotationDeprecationBranch) {
        RemoteServiceAbsolutePath absoluteURL = m_serviceIntf.getAnnotation(RemoteServiceAbsolutePath.class);
        if (absoluteURL != null) {
            return "\"" + absoluteURL.value() + "\"";
        }
        return null;
    }

    private String getRemoteServiceRelativePath(TreeLogger javadocAnnotationDeprecationBranch) {
        String[][] metaData = m_serviceIntf.getMetaData(ENTRY_POINT_TAG);
        if (metaData.length != 0) {
            if (javadocAnnotationDeprecationBranch != null) {
                javadocAnnotationDeprecationBranch.log(TreeLogger.WARN, "Deprecated use of " + ENTRY_POINT_TAG + "; Please use " + RemoteServiceRelativePath.class.getName() + " instead", null);
            }
            return metaData[0][0];
        } else {
            RemoteServiceRelativePath moduleRelativeURL = m_serviceIntf.getAnnotation(RemoteServiceRelativePath.class);
            if (moduleRelativeURL != null) {
                return "\"" + moduleRelativeURL.value() + "\"";
            }
        }
        return null;
    }

    private String getXDRCallType(JMethod syncMethod) {
        String callType = "POST_GET";
        XDRCallInfo act = syncMethod.getAnnotation(XDRCallInfo.class);
        if (act != null) {
            if (XDRCallInfo.Type.GET == act.value()) {
                callType = "GET";
            } else if (XDRCallInfo.Type.POST == act.value()) {
                callType = "POST";
            } else if (XDRCallInfo.Type.POST_GWT == act.value()) {
                callType = "POST_GWT";
            }
        }
        return callType;
    }

    private int getXDRCallTimeout(JMethod syncMethod) {
        int res = 15000;
        XDRCallInfo act = syncMethod.getAnnotation(XDRCallInfo.class);
        if (act != null) {
            res = act.timeout();
            if (res <= 0) {
                res = 15000;
            }
        }
        return res;
    }

    private void generateProxyMethods(SourceWriter w, SerializableTypeOracle serializableTypeOracle, Map<JMethod, JMethod> syncMethToAsyncMethMap) {
        JMethod[] syncMethods = m_serviceIntf.getOverridableMethods();
        for (JMethod syncMethod : syncMethods) {
            JMethod asyncMethod = syncMethToAsyncMethMap.get(syncMethod);
            assert (asyncMethod != null);
            JClassType enclosingType = syncMethod.getEnclosingType();
            JParameterizedType isParameterizedType = enclosingType.isParameterized();
            if (isParameterizedType != null) {
                JMethod[] methods = isParameterizedType.getMethods();
                for (int i = 0; i < methods.length; ++i) {
                    if (methods[i] == syncMethod) {
                        syncMethod = isParameterizedType.getBaseType().getMethods()[i];
                    }
                }
            }
            generateProxyMethod(w, serializableTypeOracle, syncMethod, asyncMethod);
        }
    }

    private void generateProxyMethod(SourceWriter w, SerializableTypeOracle serializableTypeOracle, JMethod syncMethod, JMethod asyncMethod) {
        w.println();
        JType asyncReturnType = asyncMethod.getReturnType().getErasedType();
        w.print("public ");
        w.print(asyncReturnType.getQualifiedSourceName());
        w.print(" ");
        w.print(asyncMethod.getName() + "(");
        boolean needsComma = false;
        boolean needsTryCatchBlock = false;
        NameFactory nameFactory = new NameFactory();
        JParameter[] asyncParams = asyncMethod.getParameters();
        for (int i = 0; i < asyncParams.length; ++i) {
            JParameter param = asyncParams[i];
            if (needsComma) {
                w.print(", ");
            } else {
                needsComma = true;
            }
            JType paramType = param.getType();
            paramType = paramType.getErasedType();
            if (i < asyncParams.length - 1 && paramType.isPrimitive() == null && !paramType.getQualifiedSourceName().equals(String.class.getCanonicalName())) {
                needsTryCatchBlock = true;
            }
            w.print(paramType.getQualifiedSourceName());
            w.print(" ");
            String paramName = param.getName();
            nameFactory.addName(paramName);
            w.print(paramName);
        }
        w.println(") {");
        w.indent();
        String requestIdName = nameFactory.createName("requestId");
        w.println("int " + requestIdName + " = getNextRequestId();");
        String statsMethodExpr = getProxySimpleName() + "." + syncMethod.getName();
        String tossName = nameFactory.createName("toss");
        w.println("boolean " + tossName + " = isStatsAvailable() && stats(" + "timeStat(\"" + statsMethodExpr + "\", getRequestId(), \"begin\"));");
        w.print(ClientSerializationStreamWriter.class.getSimpleName());
        w.print(" ");
        String streamWriterName = nameFactory.createName("streamWriter");
        w.println(streamWriterName + " = createStreamWriter();");
        w.println("// createStreamWriter() prepared the stream");
        w.println(streamWriterName + ".writeString(REMOTE_SERVICE_INTERFACE_NAME);");
        if (needsTryCatchBlock) {
            w.println("try {");
            w.indent();
        }
        w.println(streamWriterName + ".writeString(\"" + syncMethod.getName() + "\");");
        JParameter[] syncParams = syncMethod.getParameters();
        w.println(streamWriterName + ".writeInt(" + syncParams.length + ");");
        for (JParameter param : syncParams) {
            w.println(streamWriterName + ".writeString(\"" + serializableTypeOracle.getSerializedTypeName(param.getType().getErasedType()) + "\");");
        }
        for (int i = 0; i < asyncParams.length - 1; ++i) {
            JParameter asyncParam = asyncParams[i];
            w.print(streamWriterName + ".");
            w.print(Shared.getStreamWriteMethodNameFor(asyncParam.getType()));
            w.println("(" + asyncParam.getName() + ");");
        }
        String payloadName = nameFactory.createName("payload");
        w.println("String " + payloadName + " = " + streamWriterName + ".toString();");
        w.println(tossName + " = isStatsAvailable() && stats(" + "timeStat(\"" + statsMethodExpr + "\", getRequestId(), \"requestSerialized\"));");
        if (asyncReturnType == JPrimitiveType.VOID) {
            w.print("doInvoke(");
        } else if (asyncReturnType.getQualifiedSourceName().equals(RequestBuilder.class.getName())) {
            w.print("return doPrepareRequestBuilder(");
        } else if (asyncReturnType.getQualifiedSourceName().equals(Request.class.getName())) {
            w.print("return doInvoke(");
        } else {
            throw new RuntimeException("Unhandled return type " + asyncReturnType.getQualifiedSourceName());
        }
        JParameter callbackParam = asyncParams[asyncParams.length - 1];
        String callbackName = callbackParam.getName();
        JType returnType = syncMethod.getReturnType();
        w.print("ResponseReader." + getResponseReaderFor(returnType).name());
        w.println(", \"" + getProxySimpleName() + "." + syncMethod.getName() + "\", getRequestId(), " + payloadName + ", " + "\"" + getXDRCallType(syncMethod) + "\", " + getXDRCallTimeout(syncMethod) + ", " + callbackName + ");");
        if (needsTryCatchBlock) {
            w.outdent();
            w.print("} catch (SerializationException ");
            String exceptionName = nameFactory.createName("ex");
            w.println(exceptionName + ") {");
            w.indent();
            if (!asyncReturnType.getQualifiedSourceName().equals(RequestBuilder.class.getName())) {
                w.println(callbackName + ".onFailure(" + exceptionName + ");");
            }
            if (asyncReturnType.getQualifiedSourceName().equals(RequestBuilder.class.getName())) {
                w.println("return new " + FailingRequestBuilder.class.getName() + "(" + exceptionName + ", " + callbackName + ");");
            } else if (asyncReturnType.getQualifiedSourceName().equals(Request.class.getName())) {
                w.println("return new " + FailedRequest.class.getName() + "();");
            } else {
                assert asyncReturnType == JPrimitiveType.VOID;
            }
            w.outdent();
            w.println("}");
        }
        w.outdent();
        w.println("}");
    }

    private ResponseReader getResponseReaderFor(JType returnType) {
        if (returnType.isPrimitive() != null) {
            return JPRIMITIVETYPE_TO_RESPONSEREADER.get(returnType.isPrimitive());
        }
        if (returnType.getQualifiedSourceName().equals(String.class.getCanonicalName())) {
            return ResponseReader.STRING;
        }
        return ResponseReader.OBJECT;
    }

    private String getProxySimpleName() {
        String[] name = Shared.synthesizeTopLevelClassName(m_serviceIntf, PROXY_SUFFIX);
        return name[1];
    }

    private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx, JClassType serviceAsync) {
        JPackage serviceIntfPkg = serviceAsync.getPackage();
        String packageName = serviceIntfPkg == null ? "" : serviceIntfPkg.getName();
        PrintWriter printWriter = ctx.tryCreate(logger, packageName, getProxySimpleName());
        if (printWriter == null) {
            return null;
        }
        ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, getProxySimpleName());
        String[] imports = new String[] { RemoteServiceProxy.class.getCanonicalName(), ClientSerializationStreamWriter.class.getCanonicalName(), GWT.class.getCanonicalName(), ResponseReader.class.getCanonicalName(), SerializationException.class.getCanonicalName() };
        for (String imp : imports) {
            composerFactory.addImport(imp);
        }
        composerFactory.setSuperclass("com.os.rpc.client.XDRRpcServiceProxy");
        composerFactory.addImplementedInterface(serviceAsync.getErasedType().getQualifiedSourceName());
        return composerFactory.createSourceWriter(ctx, printWriter);
    }
}
