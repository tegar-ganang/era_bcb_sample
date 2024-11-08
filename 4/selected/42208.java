package net.ar.guia.helpers;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.*;
import net.ar.guia.*;
import net.ar.guia.managers.adapters.*;
import net.ar.guia.managers.contributors.*;
import net.ar.guia.managers.pages.*;
import net.ar.guia.managers.registry.*;
import net.ar.guia.managers.skins.*;
import net.ar.guia.own.adapters.data.*;
import net.ar.guia.own.implementation.*;
import net.ar.guia.own.interfaces.*;
import net.ar.guia.own.layouters.*;
import net.ar.guia.plugins.*;
import net.ar.guia.render.templates.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;
import com.thoughtworks.xstream.alias.*;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.converters.collections.*;
import com.thoughtworks.xstream.converters.reflection.*;
import com.thoughtworks.xstream.io.*;
import com.thoughtworks.xstream.mapper.*;

public class GuiaHelper {

    protected static MyXStream xstream;

    protected static Registry allInterfaces = new CachedRegistry(new DefaultRegistry());

    protected static class TemplateForComponentChooser implements LayouterVisitor {

        private String componentName;

        private VisualComponent component;

        private Template template;

        public TemplateForComponentChooser(String aComponentName, VisualComponent aComponent) {
            componentName = aComponentName;
            component = aComponent;
            if (aComponent != null && aComponent.getLayouter() != null) aComponent.getLayouter().accept(this); else getTemplateDirectly();
        }

        public void visitTemplateLayouter(TemplateLayouter aLayouter) {
            template = aLayouter.getClonedTemplate();
        }

        public void visitLayouter(Layouter aLayouter) {
            getTemplateDirectly();
        }

        protected void getTemplateDirectly() {
            template = GuiaFramework.getInstance().getTemplateManager().getTemplateForName(componentName);
        }
    }

    public static void copyStreams(final InputStream input, final OutputStream output, final int bufferSize) throws IOException {
        int n = 0;
        final byte[] buffer = new byte[bufferSize];
        while (-1 != (n = input.read(buffer))) output.write(buffer, 0, n);
    }

    public static String getFileNameFromResource(String aResource) {
        URL theFile = GuiaHelper.class.getResource(aResource);
        if (theFile == null) throw new GuiaException("Cannot find the resource '" + aResource + "'"); else return theFile.getFile();
    }

    public static Object restoreObjectFromXml(String anXmlResource) {
        InputStream resource = GuiaHelper.class.getResourceAsStream(anXmlResource);
        if (resource == null) throw new GuiaException("Cannot find this resource: " + anXmlResource);
        return getXStream().fromXML(new InputStreamReader(resource));
    }

    public static MyXStream getXStream() {
        if (xstream == null) {
            xstream = new MyXStream(new PureJavaReflectionProvider());
            xstream.registerConverter(new ConfigMapConverter(xstream.getClassMapper()));
            xstream.alias("guia-framework", GuiaFramework.class);
            xstream.alias("adapter-manager", AdapterManager.class);
            xstream.alias("page-manager", HtmlPageManager.class);
            xstream.alias("web-page", HtmlPageManagerEntry.class);
            xstream.alias("adapter", AdapterManager.ComponentAdapterRelationship.class);
            xstream.alias("template", DefaultGuiaTemplateManagerEntry.class);
            xstream.alias("contributor", ContributorManager.ComponentContributorRelationship.class);
            xstream.alias("contributor-manager", ContributorManager.class);
            xstream.alias("skin", Skin.class);
            xstream.alias("skin-manager", SimpleSkinManager.class);
            xstream.alias("path", String.class);
        }
        return xstream;
    }

    protected static void addAdapterManagerAliases() {
        xstream.aliasField("default-component-adapter-class", HtmlPageManagerEntry.class, "defaultComponentAdapterClass");
        xstream.aliasField("adapters-mappings", HtmlPageManagerEntry.class, "adaptersMappings");
    }

    protected static void addPageManagerAliases() {
        xstream.aliasField("page-class", HtmlPageManagerEntry.class, "pageClass");
        xstream.aliasField("window-class", HtmlPageManagerEntry.class, "windowClass");
        xstream.aliasField("web-path", HtmlPageManagerEntry.class, "webPath");
        xstream.aliasField("window-place", HtmlPageManagerEntry.class, "windowPlace");
    }

    public static void persistObjectToXml(Object anObject, String anXmlResource) {
        try {
            getXStream().toXML(anObject, new OutputStreamWriter(getOutputStreamFromResourceName(anXmlResource)));
        } catch (Exception e) {
            LogFactory.getLog(GuiaFramework.class).error("Cannot persist the object", e);
        }
    }

    public static FileOutputStream getOutputStreamFromResourceName(String anXmlResource) throws FileNotFoundException {
        return new FileOutputStream(GuiaHelper.class.getResource(anXmlResource).getFile());
    }

    public static Object getObjectFromFile(InputStream aFile) {
        try {
            ObjectInputStream oos = new ObjectInputStream(aFile);
            return oos.readObject();
        } catch (Exception e) {
            throw new GuiaException(e);
        }
    }

    public static void serializeObjectToFile(Object anObject, String aResource) {
        try {
            FileOutputStream out = new FileOutputStream(GuiaHelper.getFileNameFromResource(aResource));
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(anObject);
            oos.flush();
        } catch (Exception e) {
            LogFactory.getLog(GuiaFramework.class).error("Cannot serialize", e);
        }
    }

    public static Map convertRequestParametersToMap(HttpServletRequest aRequest) {
        Map theResult = new HashMap();
        Enumeration theParametersNames = aRequest.getParameterNames();
        while (theParametersNames.hasMoreElements()) {
            String theComponentName = theParametersNames.nextElement().toString();
            String[] theValues = aRequest.getParameterValues(theComponentName);
            theResult.put(theComponentName, theValues);
        }
        return theResult;
    }

    public static InputStream getResourceAsStream(String aResource) {
        return GuiaHelper.class.getResourceAsStream(aResource);
    }

    public static StringBuffer getResourceAsStringBuffer(String aResourceName) {
        InputStream theInputStream = GuiaHelper.class.getResourceAsStream(aResourceName);
        return getInputStreamAsStringBuffer(theInputStream);
    }

    public static StringBuffer getInputStreamAsStringBuffer(InputStream anInputStream) {
        BufferedReader resourceReader = new BufferedReader(new InputStreamReader(anInputStream));
        StringBuffer resourceBuffer = new StringBuffer();
        int i, charsRead = 0, size = 16384;
        char[] charArray = new char[size];
        try {
            while ((charsRead = resourceReader.read(charArray, 0, size)) != -1) resourceBuffer.append(charArray, 0, charsRead);
            while ((i = resourceReader.read()) != -1) resourceBuffer.append((char) i);
            resourceReader.close();
            return resourceBuffer;
        } catch (Exception e) {
            throw new GuiaException("Cannot load the resource: " + anInputStream);
        }
    }

    public static String getNoPackageClassName(Object anObject) {
        return getNoPackageClassName(anObject.getClass());
    }

    public static String getNoPackageClassName(Class aClass) {
        String t = aClass.getName();
        return t.substring(t.lastIndexOf(".") + 1);
    }

    public static String exceptionToHtml(Exception e) {
        StringBuffer theResult = new StringBuffer();
        if (System.getProperty("java.version").charAt(2) > '3') theResult = new StringBuffer(exceptionToHtmlJDK14(e)); else {
            theResult.append("<table style='font-family:verdana;font-size:13px;'>\n<tr>\n<td>\n");
            ByteArrayOutputStream theOutput = new ByteArrayOutputStream(10000);
            e.printStackTrace(new PrintStream(theOutput));
            String theStackText = StringUtils.replace(theOutput.toString(), "\n", "<br>");
            theResult.append("<br><span style='color:#FFFFFF;background-color:#EE0000'>Exception</span>:&nbsp;&nbsp;<span style='color: #BB0000'>" + escapeToWeb(theStackText) + "</span>\n");
            theResult.append("</td>\n</tr>\n</table>\n");
        }
        return theResult.toString();
    }

    public static String exceptionToHtmlJDK14(Throwable e) {
        StringBuffer theResult = new StringBuffer();
        theResult.append("<table style='font-family:verdana;font-size:13px;'>\n<tr>\n<td>\n");
        StringBuffer theStackResult = new StringBuffer();
        do {
            StackTraceElement[] theStackElements = e.getStackTrace();
            theStackResult.append("<br><span style='color:#FFFFFF;background-color:#EE0000'>Exception</span>:&nbsp;&nbsp;<span style='color: #BB0000'>" + escapeToWeb(e.toString()) + "</span>\n");
            theStackResult.append("<table border='0' style='font-family:verdana;font-size:13px;'>\n");
            for (int i = 0; i < theStackElements.length; i++) {
                theStackResult.append("<tr>\n");
                theStackResult.append("<td>\n");
                theStackResult.append("&nbsp;&nbsp;&nbsp;<span style='color: #005500'>" + theStackElements[i].getClassName() + "</span>.<span style='color: #00AA00;'>" + theStackElements[i].getMethodName() + "</span>");
                theStackResult.append("</td>\n");
                theStackResult.append("<td>\n");
                theStackResult.append("&nbsp;(in <span style='color:#0000BB'>" + theStackElements[i].getFileName() + "</span>&nbsp;line&nbsp;<span style='color:#0000FF'>" + theStackElements[i].getLineNumber() + "</span>)");
                theStackResult.append("</td>\n");
                theStackResult.append("</tr>\n");
            }
            theStackResult.append("</table>\n");
        } while ((e = e.getCause()) != null);
        theResult.append(theStackResult);
        theResult.append("</td>\n</tr>\n</table>\n");
        return theResult.toString();
    }

    public static String escapeToWeb(String aString) {
        return StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(aString, "<", "&lt;"), ">", "&gt;"), "\n", "<br>"), "\t", "&nbsp;&nbsp;&nbsp;");
    }

    public static Object createClassInstance(String aClassName) {
        try {
            if (aClassName.length() > 0) return Class.forName(aClassName).newInstance(); else return null;
        } catch (ClassNotFoundException e) {
            throw new GuiaException("Cannot create an instance of '" + aClassName + "', class not found", e);
        } catch (InstantiationException e) {
            throw new GuiaException("Cannot create an instance of '" + aClassName + "', trying to instantiate an interface	or an object that does not have a default constructor ", e);
        } catch (IllegalAccessException e) {
            throw new GuiaException("Cannot create an instance of '" + aClassName + "', illegal access", e);
        }
    }

    public static Template getTemplateForComponent(final String theComponentName, VisualComponent aComponent) {
        return new TemplateForComponentChooser(theComponentName, aComponent).template;
    }

    public static String getHTMLColorString(GColor aColor) {
        StringBuffer theResult = new StringBuffer();
        theResult.append(StringUtils.leftPad(Integer.toHexString(aColor.getRed()), 2, "0"));
        theResult.append(StringUtils.leftPad(Integer.toHexString(aColor.getGreen()), 2, "0"));
        theResult.append(StringUtils.leftPad(Integer.toHexString(aColor.getBlue()), 2, "0"));
        return theResult.toString();
    }

    public static void setPropertyValue(final Object anObject, String aPropertyName, Object aValue) {
        try {
            Method setter = getSetter(anObject, aPropertyName);
            setter.invoke(anObject, new Object[] { aValue });
        } catch (Exception e) {
            throw new GuiaException(e);
        }
    }

    public static Method getSetter(final Object anObject, String aPropertyName) {
        try {
            Method getter = getGetter(anObject, aPropertyName);
            return getter != null ? anObject.getClass().getMethod("set" + StringUtils.capitalise(aPropertyName), new Class[] { getter.getReturnType() }) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Method getGetter(final Object anObject, String aPropertyName) {
        return getGetter(anObject.getClass(), aPropertyName);
    }

    public static Method getGetter(Class clazz, String aPropertyName) {
        try {
            return clazz.getMethod("get" + StringUtils.capitalise(aPropertyName), null);
        } catch (Exception e) {
            try {
                return clazz.getMethod("is" + StringUtils.capitalise(aPropertyName), null);
            } catch (Exception e1) {
                return null;
            }
        }
    }

    public static Object getPropertyValue(final Object anObject, String aPropertyName) {
        try {
            return getGetter(anObject, aPropertyName).invoke(anObject, null);
        } catch (Exception e) {
            throw new GuiaException(e);
        }
    }

    public static List getAllFields(Object anObject) {
        List fields = new Vector();
        Class aClass = anObject.getClass();
        do {
            Field[] declaredFields = aClass.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) fields.add(declaredFields[i]);
        } while ((aClass = aClass.getSuperclass()) != null);
        return fields;
    }

    public static List getAllGetters(Object anObject) {
        List getters = new Vector();
        Class aClass = anObject.getClass();
        do {
            Method[] declaredMethods = aClass.getDeclaredMethods();
            for (int i = 0; i < declaredMethods.length; i++) {
                Method method = declaredMethods[i];
                if (method.getParameterTypes().length == 0 && ((isGetter(method) && getSetter(anObject, method.getName().substring(3)) != null) || (method.getName().startsWith("is") && getSetter(anObject, method.getName().substring(2)) != null))) getters.add(method);
            }
        } while ((aClass = aClass.getSuperclass()) != null);
        return getters;
    }

    public static List getAllMethods(Object anObject) {
        return getAllMethods(anObject.getClass());
    }

    public static List getAllMethods(Class aClass) {
        List methods = new Vector();
        do {
            Method[] declaredMethods = aClass.getDeclaredMethods();
            for (int i = 0; i < declaredMethods.length && declaredMethods[i].getDeclaringClass().toString().indexOf("java.lang.reflect.Proxy") == -1; i++) methods.add(declaredMethods[i]);
        } while ((aClass = aClass.getSuperclass()) != null);
        return methods;
    }

    public static List getProperties(Object anObject) {
        List result = new Vector();
        if (anObject != null) {
            for (Iterator i = getAllGetters(anObject).iterator(); i.hasNext(); ) {
                Method getter = (Method) i.next();
                String propertyName = getter.getName().startsWith("get") ? getter.getName().substring(3) : getter.getName().substring(2);
                result.add(propertyName);
            }
        }
        return result;
    }

    public static List getReadOnlyProperties(Object anObject) {
        List result = new Vector();
        if (anObject != null) {
            for (Iterator i = getAllMethods(anObject).iterator(); i.hasNext(); ) {
                Method getter = (Method) i.next();
                String propertyName = null;
                if (getter.getName().startsWith("get")) propertyName = getter.getName().substring(3); else if (getter.getName().startsWith("is")) propertyName = getter.getName().substring(2);
                if (propertyName != null) result.add(propertyName);
            }
        }
        return result;
    }

    public static class ConfigFieldAliasingMapper extends FieldAliasingMapper {

        public ConfigFieldAliasingMapper(ClassMapper wrapped) {
            super(wrapped);
        }

        public String serializedMember(Class type, String memberName) {
            String serialized = super.serializedMember(type, memberName);
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < serialized.length(); i++) {
                char character = serialized.charAt(i);
                if (Character.isUpperCase(character)) result.append("-");
                result.append(Character.toLowerCase(character));
            }
            return result.toString();
        }

        public String realMember(Class type, String serialized) {
            String real = super.realMember(type, serialized);
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < real.length(); i++) {
                char character = real.charAt(i);
                if (character == '-') character = Character.toUpperCase(real.charAt(++i));
                result.append(character);
            }
            return result.toString();
        }
    }

    public static class ConfigMapConverter extends MapConverter {

        public ConfigMapConverter(ClassMapper arg0) {
            super(arg0);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Map map = (Map) source;
            for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key == value) writeItem(value, context, writer); else {
                    writer.startNode("entry");
                    writeItem(key, context, writer);
                    writeItem(value, context, writer);
                    writer.endNode();
                }
            }
        }

        protected void populateMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map) {
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if (reader.getNodeName().equals("entry")) {
                    reader.moveDown();
                    Object key = readItem(reader, context, map);
                    reader.moveUp();
                    reader.moveDown();
                    Object value = readItem(reader, context, map);
                    reader.moveUp();
                    map.put(key, value);
                } else {
                    Object value = readItem(reader, context, map);
                    map.put(value, value);
                }
                reader.moveUp();
            }
        }
    }

    public static Set getAllInterfaces(Class aClass) {
        Class currentClass = aClass;
        Set result = (Set) allInterfaces.getValue(currentClass);
        if (result == null) {
            result = new LinkedHashSet();
            while (currentClass != null) {
                result.add(currentClass);
                List interfaces = Arrays.asList(currentClass.getInterfaces());
                result.addAll(interfaces);
                for (Iterator i = interfaces.iterator(); i.hasNext(); ) result.addAll(getAllInterfaces((Class) i.next()));
                currentClass = currentClass.getSuperclass();
            }
            allInterfaces.registerValue(aClass, result);
        }
        result.add(Object.class);
        return result;
    }

    public static VisualComponent getTopParent(VisualComponent aComponent) {
        VisualComponent component = aComponent;
        while (component.getParent() != null) component = component.getParent();
        return component;
    }

    public static Set newHashSet(Object anObject) {
        Set result = new HashSet();
        result.add(anObject);
        return result;
    }

    public static List getValueHoldersTypes(List classes) {
        List result = new Vector();
        for (Iterator i = classes.iterator(); i.hasNext(); ) result.add(((ValueHolder) i.next()).getValueType());
        return result;
    }

    public static List getNoParamsGetterMethodInvokers(List instances) {
        List result = new Vector();
        for (Iterator i = instances.iterator(); i.hasNext(); ) {
            ValueHolder object = (ValueHolder) i.next();
            for (Iterator i2 = getAllGetters(object.getValue()).iterator(); i2.hasNext(); ) {
                Method method = (Method) i2.next();
                if (method.getParameterTypes().length == 0) result.add(new MethodInvoker(object, method));
            }
        }
        return result;
    }

    public static Object createClassInstance(Class aClass) {
        return createClassInstance(aClass.getName());
    }

    public static Object getPropertyName(Method aMethod) {
        if (isGetter(aMethod) || isSetter(aMethod)) return aMethod.getName().substring(aMethod.getReturnType().equals(boolean.class) ? 2 : 3); else return null;
    }

    public static boolean isGetter(Method aMethod) {
        return aMethod.getParameterTypes().length == 0 && (aMethod.getName().startsWith("get") || aMethod.getName().startsWith("is"));
    }

    public static boolean isSetter(Method aMethod) {
        return aMethod.getParameterTypes().length == 0 && aMethod.getName().startsWith("set");
    }

    public static boolean listsContainsSameReferences(List aList1, List aList) {
        boolean isEqual = aList != null && aList1 != null && aList.size() == aList1.size();
        for (int i = 0; isEqual && i < aList.size(); i++) isEqual = aList.get(i) == aList1.get(i);
        return isEqual;
    }

    public static List convertIntArrayToList(int[] anArray) {
        List result = new Vector();
        ;
        for (int i = 0; i < anArray.length; i++) result.add(new Integer(anArray[i]));
        return result;
    }

    public static Layouter getTemplateLayouterFromType(Class aTemplateType) {
        Template template = GuiaFramework.getCurrentTemplateManager().getTemplateType(aTemplateType);
        return new DefaultTemplateLayouter(template);
    }

    public static void fillList(int x, List aList) {
        if (aList.size() < x + 1) aList.addAll(Arrays.asList(new Object[aList.size() + 1 - x]));
    }
}
