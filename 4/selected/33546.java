package net.sourceforge.ondex.server.doclet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;

/**
 * Doclet for generating web service documentation in html.
 * 
 * @author David Withers, Christian Brenninkmeijer
 */
public class WebserviceDoclet {

    static boolean success = true;

    public static boolean start(RootDoc root) {
        try {
            writeDoc("index.html", "net.sourceforge.ondex.server.ONDEXServiceWS", null, root);
            writeDoc("export.html", "net.sourceforge.ondex.server.plugins.export.ExportAuto", "Export", root);
            writeDoc("exportUsingJob.html", "net.sourceforge.ondex.server.plugins.export.ExportUsingJobAuto", "Export", root);
            writeDoc("filter.html", "net.sourceforge.ondex.server.plugins.filter.FilterAuto", "Filter", root);
            writeDoc("filterUsingJob.html", "net.sourceforge.ondex.server.plugins.filter.FilterUsingJobAuto", "Filter", root);
            writeDoc("parser.html", "net.sourceforge.ondex.server.plugins.parser.ParserAuto", "Parser", root);
            writeDoc("parserUsingJob.html", "net.sourceforge.ondex.server.plugins.parser.ParserUsingJobAuto", "Parser", root);
            writeDoc("mapping.html", "net.sourceforge.ondex.server.plugins.mapping.MappingAuto", "Mapping", root);
            writeDoc("mappingUsingJob.html", "net.sourceforge.ondex.server.plugins.mapping.MappingUsingJobAuto", "Mapping", root);
            writeDoc("transformer.html", "net.sourceforge.ondex.server.plugins.transformer.TransformerAuto", "Transformer", root);
            writeDoc("transformerUsingJob.html", "net.sourceforge.ondex.server.plugins.transformer.TransformerUsingJobAuto", "Transformer", root);
            return success;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private static void writeDoc(String htmlPage, String classPath, String pluginType, RootDoc root) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(htmlPage);
        ClassDoc classdoc = root.classNamed(classPath);
        writeDoc(root, out, classdoc, pluginType);
        out.flush();
        out.close();
    }

    private static void writeDoc(RootDoc root, PrintWriter out, ClassDoc classDoc, String pluginName) {
        List<ClassDoc> typeClasses = new ArrayList<ClassDoc>();
        Set<String> subClasses = getSubClassSet(classDoc.name());
        for (ClassDoc subClassDoc : root.classes()) {
            if (subClasses.contains(subClassDoc.name())) {
                typeClasses.add(subClassDoc);
            }
        }
        System.out.println("Writing " + classDoc.name());
        writeHeader(out, classDoc.name());
        out.print("<p>");
        out.print(classDoc.commentText());
        out.println("</p>");
        MethodDoc[] methods = classDoc.methods();
        ClassDoc parent = classDoc.superclass();
        if (parent.name().equals("JobBase")) {
            MethodDoc[] parentMethods = parent.methods();
            MethodDoc[] temp = new MethodDoc[methods.length + parentMethods.length];
            System.arraycopy(methods, 0, temp, 0, methods.length);
            System.arraycopy(parentMethods, 0, temp, methods.length, parentMethods.length);
            System.out.println(methods.length + " " + temp.length);
            methods = temp;
        }
        if (pluginName == null) {
            sortMethods(methods);
        } else {
            sortMethods(methods, pluginName);
        }
        writeOperationsTable(out, methods);
        writeTypesTable(out, typeClasses);
        writeOperationDetails(out, methods);
        writeTypeDetails(out, typeClasses);
        writeFooter(out);
        if (!success) {
            System.err.println("Just Generated webpage for " + classDoc);
        }
    }

    /**
	 * 
	 * 
	 * @param out
	 * @param methods
	 */
    private static void writeOperationsTable(PrintWriter out, MethodDoc[] methods) {
        out.println("<h2>Operations</h2>");
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Operation</th>");
        out.println("<th>Description</th>");
        out.println("</tr>");
        boolean odd = true;
        for (MethodDoc method : methods) {
            if (webMethodExcludeFalse(method)) {
                out.println("<tr class=\"" + (odd ? "odd" : "even") + "\">");
                out.print("<td>");
                out.print("<a href=\"#");
                out.print(method.name());
                out.print("\">");
                out.print(method.name());
                out.print("</a>");
                out.println("</td>");
                out.print("<td>");
                writeTags(out, method.firstSentenceTags());
                out.println("</td>");
                out.println("</tr>");
                odd = !odd;
            }
        }
        out.println("</table>");
    }

    /**
	 *
	 *
	 * @param out
	 * @param tags
	 */
    private static void writeTags(PrintWriter out, Tag[] tags) {
        for (Tag tag : tags) {
            if (tag.kind().equals("@see")) {
                SeeTag seeTag = (SeeTag) tag;
                if (seeTag.referencedClass() == null) {
                    System.err.println("seeTag: " + seeTag + " referencedClass not found. " + "Is it correct and listed in the imports");
                    success = false;
                    out.print(tag.text());
                } else if (seeTag.referencedMember() != null) {
                    String fieldName = seeTag.referencedMemberName();
                    if (seeTag.referencedMember().isField()) {
                        ClassDoc classDoc = seeTag.referencedClass();
                        FieldDoc[] fieldDocs = classDoc.fields();
                        for (FieldDoc fieldDoc : fieldDocs) {
                            if (fieldDoc.name().equals(fieldName)) {
                                Object object = fieldDoc.constantValue();
                                if (object != null) {
                                    out.print(object);
                                    return;
                                } else {
                                    System.err.println("seeTag: " + seeTag + " referencedMember " + fieldName + " is not a constant.");
                                    out.print(tag.text());
                                    success = false;
                                    return;
                                }
                            }
                        }
                        System.err.println("seeTag: " + seeTag + " referencedMember " + fieldName + " not found in the class.");
                        out.print(tag.text());
                        success = false;
                        return;
                    } else {
                        System.err.println("seeTag: " + seeTag + " referencedMember " + fieldName + " if not a field.");
                        out.print(tag.text());
                        success = false;
                        return;
                    }
                } else {
                    out.print(getTypeName(seeTag.referencedClass()));
                }
            } else {
                String text = tag.text();
                text = text.replace("&", "&amp;");
                text = text.replace("<", "&lt;");
                text = text.replace(">", "&gt;");
                out.print(text);
            }
        }
    }

    /**
	 *
	 *
	 * @param out
	 * @param typeClasses
	 */
    private static void writeTypesTable(PrintWriter out, List<ClassDoc> typeClasses) {
        out.println("<h2>Types</h2>");
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Type</th>");
        out.println("<th>Description</th>");
        out.println("</tr>");
        boolean odd = true;
        for (ClassDoc classDoc : typeClasses) {
            String name = classDoc.name().substring(2);
            out.println("<tr class=\"" + (odd ? "odd" : "even") + "\">");
            out.print("<td>");
            out.print("<a href=\"#");
            out.print(name);
            out.print("\">");
            out.print(name);
            out.print("</a>");
            out.println("</td>");
            out.print("<td>");
            writeTags(out, classDoc.firstSentenceTags());
            out.println("</td>");
            out.println("</tr>");
            odd = !odd;
        }
        out.println("</table>");
    }

    private static boolean webMethodExcludeFalse(MethodDoc method) {
        boolean excludeFalse = true;
        boolean excludeFound = false;
        boolean webResult = false;
        if (!method.isPublic()) {
            System.out.println("Ignoring non public method " + method.name());
            return false;
        }
        AnnotationDesc[] annotations = method.annotations();
        for (AnnotationDesc annotation : annotations) {
            if (annotation.annotationType().toString().contains("WebMethod")) {
                ElementValuePair[] pairs = annotation.elementValues();
                for (ElementValuePair pair : pairs) {
                    excludeFalse = (pair.toString().contains("false"));
                    excludeFound = true;
                }
            }
            if (annotation.annotationType().toString().contains("WebResult")) {
                webResult = true;
            }
        }
        if (excludeFalse && !webResult) {
            System.err.println("*** Method " + method + " does not have an @WebResult tag");
            success = false;
        }
        if (!excludeFound) {
            System.err.println("*** Method " + method + " does not have an @WebMethod tag");
            success = false;
        }
        return excludeFalse;
    }

    private static void writeOperationDetails(PrintWriter out, MethodDoc[] methods) {
        out.println("<h2>Operation Details</h2>");
        boolean first = true;
        for (MethodDoc method : methods) {
            if (webMethodExcludeFalse(method)) {
                if (first) {
                    first = false;
                } else {
                    out.print("<hr/>");
                }
                out.print("<h3><a name=\"");
                out.print(method.name());
                out.print("\">");
                out.print(method.name());
                out.println("</a></h3>");
                out.print("<p>");
                writeTags(out, method.inlineTags());
                out.println("</p>");
                out.println("<h4>Inputs</h4>");
                if (method.parameters().length > 0) {
                    writeParametersTable(out, method);
                } else {
                    out.println("None");
                }
                out.println("<h4>Output</h4>");
                if (method.returnType().toString().equals("void")) {
                    out.println("None");
                } else {
                    writeOutputTable(out, method);
                }
            }
        }
    }

    /**
	 *
	 *
	 * @param out
	 * @param typeClasses
	 */
    private static void writeTypeDetails(PrintWriter out, List<ClassDoc> typeClasses) {
        out.println("<h2>Type Details</h2>");
        boolean first = true;
        for (ClassDoc classDoc : typeClasses) {
            String name = classDoc.name().substring(2);
            if (first) {
                first = false;
            } else {
                out.print("<hr/>");
            }
            out.print("<h3><a name=\"");
            out.print(name);
            out.print("\">");
            out.print(name);
            out.println("</a></h3>");
            out.print("<p>");
            out.print(classDoc.commentText());
            out.println("</p>");
            out.println("<h4>Properties</h4>");
            ClassDoc supertype = classDoc.superclass();
            if (classDoc.fields().length > 0 || supertype.fields().length > 0) {
                writePropertiesTable(out, classDoc);
            } else {
                out.println("None");
            }
        }
    }

    /**
	 *
	 *
	 * @param out
	 * @param classDoc
	 */
    private static void writePropertiesTable(PrintWriter out, ClassDoc classDoc) {
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Name</th>");
        out.println("<th>Type</th>");
        out.println("<th>Description</th>");
        out.println("</tr>");
        ClassDoc supertype = classDoc.superclass();
        boolean odd = true;
        if (supertype != null) {
            odd = writePropertiesRows(out, supertype, odd);
        }
        writePropertiesRows(out, classDoc, odd);
        out.println("</table>");
    }

    private static boolean writePropertiesRows(PrintWriter out, ClassDoc classDoc, boolean odd) {
        for (FieldDoc field : classDoc.fields()) {
            out.println("<tr class=\"" + (odd ? "odd" : "even") + "\">");
            out.print("<td>");
            out.print(field.name());
            out.println("</td>");
            out.print("<td>");
            out.print(getTypeName(field.type()));
            out.println("</td>");
            out.print("<td>");
            writeTags(out, field.inlineTags());
            out.println("</td>");
            out.println("</tr>");
            odd = !odd;
        }
        return odd;
    }

    /**
	 * 
	 * 
	 * @param out
	 * @param method
	 */
    private static void writeOutputTable(PrintWriter out, MethodDoc method) {
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Name</th>");
        out.println("<th>Type</th>");
        out.println("<th>Description</th>");
        out.println("</tr>");
        Tag returnTag = null;
        for (Tag tag : method.tags("@return")) {
            returnTag = tag;
            break;
        }
        String returnName = "";
        for (AnnotationDesc annotation : method.annotations()) {
            if (annotation.annotationType().name().equals("WebResult")) {
                for (ElementValuePair element : annotation.elementValues()) {
                    if (element.element().name().equals("name")) {
                        returnName = element.value().value().toString();
                    }
                }
            }
        }
        out.println("<tr class=\"odd\"\">");
        out.print("<td>");
        out.print(returnName);
        out.println("</td>");
        out.print("<td>");
        out.print(getTypeName(method.returnType()));
        out.println("</td>");
        out.print("<td>");
        if (returnTag == null) {
            out.println("Return tag has no info");
            System.err.println("*** Return tag has no info in method " + method);
            success = false;
        } else {
            writeTags(out, returnTag.inlineTags());
        }
        out.println("</td>");
        out.println("</tr>");
        out.println("</table>");
    }

    private static String getTypeName(Type type) {
        StringBuilder sb = new StringBuilder();
        String typeName = type.typeName();
        if (typeName.startsWith("WS")) {
            sb.append("<a href=\"#");
            sb.append(typeName.substring(2));
            sb.append("\">");
            sb.append(typeName.substring(2));
            sb.append("</a>");
        } else {
            ParameterizedType parameterizedType = type.asParameterizedType();
            if (parameterizedType != null) {
                sb.append(parameterizedType.typeName());
                sb.append(" of ");
                boolean first = true;
                for (Type typeArguament : parameterizedType.typeArguments()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(getTypeName(typeArguament));
                }
            } else {
                sb.append(typeName);
            }
        }
        return sb.toString();
    }

    /**
	 * 
	 * 
	 * @param out
	 * @param method
	 */
    private static void writeParametersTable(PrintWriter out, MethodDoc method) {
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Name</th>");
        out.println("<th>Type</th>");
        out.println("<th>Description</th>");
        out.println("</tr>");
        boolean odd = true;
        for (Parameter parameter : method.parameters()) {
            String name = parameter.name();
            ParamTag parameterTag = findParameterTag(method, parameter);
            AnnotationDesc[] annotations = parameter.annotations();
            for (AnnotationDesc annotation : annotations) {
                if (annotation.annotationType().toString().contains("WebParam")) {
                    ElementValuePair[] pairs = annotation.elementValues();
                    for (ElementValuePair pair : pairs) {
                        name = (String) pair.value().value();
                    }
                }
            }
            out.println("<tr class=\"" + (odd ? "odd" : "even") + "\">");
            out.print("<td>");
            out.print(name);
            out.println("</td>");
            out.print("<td>");
            out.print(getTypeName(parameter.type()));
            out.println("</td>");
            out.print("<td>");
            if (parameterTag != null) {
                writeTags(out, parameterTag.inlineTags());
            } else {
                out.print("ParameterTag missing in ");
                System.err.println("ParameterTag missing in " + method + " for " + parameter.name());
                success = false;
            }
            out.println("</td>");
            out.println("</tr>");
            odd = !odd;
        }
        out.println("</table>");
    }

    /**
	 * 
	 * 
	 * @param method
	 * @return
	 */
    private static ParamTag findParameterTag(MethodDoc method, Parameter parameter) {
        for (ParamTag parameterTag : method.paramTags()) {
            String pName = parameterTag.parameterName();
            if (pName.startsWith("@see")) {
                pName = pName.substring(4, pName.length());
            }
            if (parameterTag.parameterName().equals(parameter.name())) {
                return parameterTag;
            }
        }
        return null;
    }

    private static String classNameToServiceName(String className) {
        if (className.equalsIgnoreCase("ONDEXServiceWS")) {
            return "ondex-graph";
        }
        if (className.equalsIgnoreCase("ExportAuto")) {
            return "ondex-export";
        }
        if (className.equalsIgnoreCase("ExportUsingJobAuto")) {
            return "ondex-exportUsingJob";
        }
        if (className.equalsIgnoreCase("FilterAuto")) {
            return "ondex-filter";
        }
        if (className.equalsIgnoreCase("FilterUsingJobAuto")) {
            return "ondex-filterUsingJob";
        }
        if (className.equalsIgnoreCase("ParserAuto")) {
            return "ondex-parser";
        }
        if (className.equalsIgnoreCase("ParserUsingJobAuto")) {
            return "ondex-parserUsingJob";
        }
        if (className.equalsIgnoreCase("MappingAuto")) {
            return "ondex-mapping";
        }
        if (className.equalsIgnoreCase("MappingUsingJobAuto")) {
            return "ondex-mappingUsingJob";
        }
        if (className.equalsIgnoreCase("TransformerAuto")) {
            return "ondex-transformer";
        }
        if (className.equalsIgnoreCase("TransformerUsingJobAuto")) {
            return "ondex-transformerUsingJob";
        }
        return className;
    }

    private static Set<String> getSubClassSet(String className) {
        Set<String> subClasses = new HashSet<String>();
        subClasses.add("WSJob");
        if (className.equalsIgnoreCase("ONDEXServiceWS")) {
            subClasses.add("WSAttributeName");
            subClasses.add("WSCV");
            subClasses.add("WSConcept");
            subClasses.add("WSConceptAccession");
            subClasses.add("WSConceptClass");
            subClasses.add("WSConceptGDS");
            subClasses.add("WSConceptName");
            subClasses.add("WSEvidenceType");
            subClasses.add("WSGDS");
            subClasses.add("WSGraph");
            subClasses.add("WSGraphMetaData");
            subClasses.add("WSRelationType");
            subClasses.add("WSRelationKey");
            subClasses.add("WSUnit");
        }
        if (className.equalsIgnoreCase("ExportUsingJobAuto")) {
            subClasses.add(" WSExportJobResult");
        }
        if (className.equalsIgnoreCase("FilterAuto")) {
            subClasses.add("WSFilterResult");
        }
        if (className.equalsIgnoreCase("ParserAuto")) {
        }
        if (className.equalsIgnoreCase("MappingAuto")) {
        }
        if (className.equalsIgnoreCase("StatisticsAuto")) {
        }
        if (className.equalsIgnoreCase("TransformerAuto")) {
        }
        return subClasses;
    }

    /**
	 * Writes the html document header.
	 * 
	 * @param out
	 * @param classType 
	 */
    private static void writeHeader(PrintWriter out, String classType) {
        String serviceName = classNameToServiceName(classType);
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        out.println("<title>ONDEX web service</title>");
        out.println("<style type=\"text/css\" media=\"all\">");
        out.println("@import url(\"./stylesheet.css\");");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>ONDEX web service</h1>");
        out.println("<h2>" + serviceName + "</h2>");
        out.println("<div id=\"menu\">");
        out.println("<ul id=\"nav\">");
        out.println("<li><a href=\"index.html\">Graph Service</a></li>");
        out.println("<li><a href=\"parser.html\">Parsers</a></li>");
        out.println("<li><a href=\"parserUsingJob.html\">Parsers_Using_Job</a></li>");
        out.println("<li><a href=\"filter.html\">Filters</a></li>");
        out.println("<li><a href=\"filterUsingJob.html\">Filters_Using_Job</a></li>");
        out.println("<li><a href=\"mapping.html\">Mapping</a></li>");
        out.println("<li><a href=\"mappingUsingJob.html\">Mapping_Using_Job</a></li>");
        out.println("<li><a href=\"transformer.html\">Transformer</a></li>");
        out.println("<li><a href=\"transformerUsingJob.html\">Transformer_Using_Job</a></li>");
        out.println("<li><a href=\"export.html\">Export</a></li>");
        out.println("<li><a href=\"exportUsingJob.html\">Export_Using_Job</a></li>");
        out.println("</ul>");
        out.println("</div>");
        out.println("<br>");
        out.println("<a href=\"services\">Overview page</a>");
        out.println("<br>");
        out.println("<p>WSDL: <a href=\"services/" + serviceName + "?wsdl\">services/" + serviceName + "?wsdl</a></p>");
    }

    /**
	 * Writes the html footer.
	 * 
	 * @param out
	 */
    private static void writeFooter(PrintWriter out) {
        out.println("</body>");
        out.println("</html>");
    }

    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    private static void copy(String sourceName, String destName) throws IOException {
        File source = new File(sourceName);
        File dest = new File(destName);
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    private static boolean greater(MethodDoc one, MethodDoc two, String pluginName) {
        String oneName = one.name();
        String twoName = two.name();
        if (oneName.endsWith(pluginName)) {
            if (!twoName.endsWith(pluginName)) {
                return false;
            }
        } else {
            if (twoName.endsWith(pluginName)) {
                return true;
            }
        }
        return (oneName.compareTo(twoName) > 0);
    }

    private static boolean greater(MethodDoc one, MethodDoc two) {
        return (one.name().compareTo(two.name()) > 0);
    }

    private static void sortMethods(MethodDoc[] methods) {
        MethodDoc temp;
        for (int i = 0; i < methods.length - 1; i++) {
            for (int j = i + 1; j < methods.length; j++) {
                if (greater(methods[i], methods[j])) {
                    temp = methods[i];
                    methods[i] = methods[j];
                    methods[j] = temp;
                }
            }
        }
    }

    private static void sortMethods(MethodDoc[] methods, String pluginName) {
        MethodDoc temp;
        for (int i = 0; i < methods.length - 1; i++) {
            for (int j = i + 1; j < methods.length; j++) {
                if (greater(methods[i], methods[j], pluginName)) {
                    temp = methods[i];
                    methods[i] = methods[j];
                    methods[j] = temp;
                }
            }
        }
    }
}
