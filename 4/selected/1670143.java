package anima.info.java;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import anima.dcc.DCCObject;
import anima.dcc.InterfaceType;
import anima.dcc.PropertyType;
import anima.info.InfoDataNode;
import anima.info.InfoIOException;
import anima.info.InfoNode;
import anima.info.InfoNodeBranch;
import anima.info.InfoUnitFormated;
import anima.info.InfoUnitIOException;
import anima.info.Namespaces;
import anima.info.xml.InfoUnitXMLData;
import anima.util.BeanUtil;

public class InfoUnitJava extends InfoUnitFormated {

    public static final String STRUCTURE_JAVA_ANIMA = InfoUnitJava.class.getResource("info-java-anima.xml").toString();

    public static final String defaultImportDCCType = "http://purl.org/net/dcc/DCCTaxonomy.owl#ProcessDCC";

    protected DCCObject animaObj;

    protected boolean extended;

    protected static String propertyVisibility;

    protected static InfoDataNode strj;

    protected boolean onlyChangeableProperty = true;

    public InfoUnitJava() {
        super();
    }

    public InfoUnitJava(String sourcePath, String sourceFile, boolean extended, boolean simplified) throws InfoIOException {
        super();
        parse(sourcePath, sourceFile, extended, simplified);
    }

    public void setOnlyChangeableProperty(boolean onlyChangeableProperty) {
        this.onlyChangeableProperty = onlyChangeableProperty;
    }

    public boolean isOnlyChangeableProperty() {
        return onlyChangeableProperty;
    }

    public InfoNodeBranch getInfoRoot() {
        if (animaObj == null) return null; else {
            infoRoot = animaObj.toInfoNode(extended);
            return infoRoot;
        }
    }

    public void parse(URL source, boolean extended, boolean simplified) throws InfoIOException {
        String complete = source.toString();
        int div = Math.max(complete.lastIndexOf('\\'), complete.lastIndexOf('/'));
        String fPath = complete.substring(0, div + 1), fName = complete.substring(div + 1);
        parse(fPath, fName, extended, simplified);
    }

    public void parse(String path, String fileName, boolean extended, boolean simplified) throws InfoIOException {
        this.extended = extended;
        InfoUnitXMLData jud = null;
        try {
            jud = new InfoUnitXMLData(new URL(STRUCTURE_JAVA_ANIMA));
        } catch (MalformedURLException error) {
            throw new InfoUnitIOException(error.getMessage());
        }
        strj = jud.load("structure");
        propertyVisibility = strj.ft("basic/visibility/public");
        Object theBean = null;
        Class classBean = null;
        try {
            theBean = BeanUtil.instantiateBean(path, fileName);
            classBean = theBean.getClass();
            String pack = (classBean.getPackage() != null) ? classBean.getPackage().getName() : null;
            String className = classBean.getName();
            if (pack != null && className.startsWith(pack)) className = className.substring(pack.length() + 1);
            animaObj = new DCCObject(className, "1.0", defaultImportDCCType);
            BeanInfo bInfo = Introspector.getBeanInfo(classBean);
            PropertyDescriptor lpd[] = bInfo.getPropertyDescriptors();
            InterfaceType propertyInterface = new InterfaceType("Properties", null);
            PropertyType properties[] = new PropertyType[lpd.length];
            for (int i = 0; i < lpd.length; i++) properties[i] = parseProperty(theBean, classBean, lpd[i]);
            propertyInterface.setProperty(properties);
            animaObj.setDefaultPropertyInterface(propertyInterface);
            loadNamespaces();
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    protected PropertyType parseProperty(Object theBean, Class classBean, PropertyDescriptor pd) {
        Class pt = pd.getPropertyType();
        if (pt == null || (pd.getName().equals("class") && pt.equals(java.lang.Class.class)) || (onlyChangeableProperty && pd.getWriteMethod() == null)) return null; else {
            String propertyType = strj.ft("basic/constraint/" + pt.getName());
            if (propertyType == null) propertyType = pd.getPropertyType().getName();
            Method readMethod = pd.getReadMethod(), writeMethod = pd.getWriteMethod();
            String readName = null;
            StringBuffer bName = new StringBuffer(pd.getName());
            char c = bName.charAt(0);
            if (c >= 'a' && c <= 'z') bName.setCharAt(0, Character.toUpperCase(c));
            try {
                if (readMethod != null) readName = readMethod.getName(); else {
                    readName = "get" + bName;
                    readMethod = classBean.getMethod(readName, null);
                }
            } catch (NoSuchMethodException error) {
            }
            Object initial = null;
            if (readMethod != null) {
                try {
                    initial = readMethod.invoke(theBean, null);
                } catch (Exception error) {
                }
            }
            PropertyType propType = new PropertyType(pd.getName(), false, propertyType, (initial == null) ? null : initial.toString());
            propType.setVisibility(propertyVisibility);
            propType.setChangeable(writeMethod != null);
            return propType;
        }
    }

    protected void loadNamespaces() throws InfoUnitIOException {
        InfoUnitXMLData in = new InfoUnitXMLData(NAMESPACES_INFO);
        InfoDataNode idn = in.load("namespaces");
        Set<InfoNode> names = (idn != null) ? null : idn.getChildren();
        if (names != null) {
            String ref[] = new String[names.size()];
            int n = 0;
            for (InfoNode nms : names) {
                ref[n] = nms.getLabel();
                n++;
            }
            infoNamespaces = new Namespaces(ref);
        }
    }
}
