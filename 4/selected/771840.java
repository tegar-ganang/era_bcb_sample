package umlc.codeGeneration.jmatter;

import com.l2fprod.common.propertysheet.DefaultProperty;
import java.io.*;
import java.nio.channels.FileChannel;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;
import tools.Debug;
import java.util.*;
import org.apache.commons.beanutils.ConvertUtils;
import umlc.codeGeneration.*;
import umlc.parseTree.*;

/**
 *
 * @author ryan
 */
public class JmatterGeneratorImpl implements GeneratorIF {

    private String output_dir;

    /** Creates a new instance of JmatterGeneratorImpl */
    public JmatterGeneratorImpl() {
    }

    public void initGenerator() {
        ConvertUtils.register(new ColorConverter(), java.awt.Color.class);
        ConvertUtils.register(new UmlcStringConverter(), String.class);
    }

    public void generateCode(java.util.Hashtable packages, String _output_dir) {
        output_dir = _output_dir;
        try {
            Properties p = new Properties();
            p.setProperty("resource.loader", "class");
            p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            Velocity.init(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap classList = new HashMap();
        Vector requiredList = new Vector();
        for (java.util.Enumeration e = packages.elements(); e.hasMoreElements(); ) {
            UmlPackage cur_package = (UmlPackage) e.nextElement();
            File currentDir = createOrRetrivePackageDir(cur_package);
            for (int i = 0; i < cur_package.UmlClasses.size(); i++) {
                UmlClass cur_class = (UmlClass) cur_package.UmlClasses.elementAt(i);
                Debug.println(5, "Class: " + cur_class.getName());
                generateAbstractEObject(cur_package, cur_class, currentDir);
                classList.put(cur_package.getName() + "." + cur_class.getName(), new Boolean(cur_class.hasAnnotation("no_class_list")));
                for (int j = 0; j < cur_class.attributes.size(); j++) {
                    UmlAttribute att = (UmlAttribute) cur_class.attributes.elementAt(j);
                    if (att.hasAnnotation("required")) {
                        requiredList.add(cur_class.getName() + "." + att.name);
                    }
                }
                copyIconFiles(cur_class);
            }
        }
        generateAppConfig(classList);
        generateClassList(classList);
        generateRequired(requiredList);
    }

    private File createOrRetrivePackageDir(UmlPackage currentPackage) {
        String fullname = currentPackage.getName();
        String path = fullname.replace(".", "/");
        File outputDir = new File(output_dir + "/" + path);
        if (!outputDir.exists()) outputDir.mkdirs();
        return outputDir;
    }

    private void copyIconFiles(UmlClass clazz) {
        if (clazz.hasAnnotation("icon16")) {
            String i16 = clazz.annotationValue("icon16");
            String fileType = ".png";
            if (i16.endsWith(".jpg")) fileType = ".jpg";
            if (i16.endsWith(".gif")) fileType = ".gif";
            String desti16 = output_dir + "/../resources/images/" + clazz.getName() + "16" + fileType;
            try {
                FileChannel src = new FileInputStream(i16).getChannel();
                FileChannel dst = new FileOutputStream(desti16).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (clazz.hasAnnotation("icon32")) {
            String i32 = clazz.annotationValue("icon32");
            String fileType = ".png";
            if (i32.endsWith(".jpg")) fileType = ".jpg";
            if (i32.endsWith(".gif")) fileType = ".gif";
            String desti32 = output_dir + "/../resources/images/" + clazz.getName() + "32" + fileType;
            try {
                FileChannel src = new FileInputStream(i32).getChannel();
                FileChannel dst = new FileOutputStream(desti32).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateRequired(Vector p) {
        VelocityContext context = new VelocityContext();
        context.put("requiredList", p);
        Template template = null;
        try {
            File dir = new File(output_dir + "/../resources");
            dir.mkdirs();
            File file = new File(output_dir + "/../resources/model-metadata.properties");
            FileWriter w = new FileWriter(file);
            template = Velocity.getTemplate("umlc/codeGeneration/jmatter/model-metadata.properties.vm");
            template.merge(context, w);
            w.close();
        } catch (Exception rnfe) {
            rnfe.printStackTrace();
        }
    }

    private void generateClassList(HashMap p) {
        VelocityContext context = new VelocityContext();
        context.put("classes", p);
        Template template = null;
        try {
            File dir = new File(output_dir + "/com/u2d");
            dir.mkdirs();
            File file = new File(output_dir + "/com/u2d/class-list.xml");
            FileWriter w = new FileWriter(file);
            template = Velocity.getTemplate("umlc/codeGeneration/jmatter/class-list.xml.vm");
            template.merge(context, w);
            w.close();
        } catch (Exception rnfe) {
            rnfe.printStackTrace();
        }
    }

    private void generateAppConfig(HashMap p) {
        VelocityContext context = new VelocityContext();
        context.put("classes", p.keySet());
        Template template = null;
        try {
            File dir = new File(output_dir + "/com/u2d");
            dir.mkdirs();
            File file = new File(output_dir + "/com/u2d/app-config.xml");
            FileWriter w = new FileWriter(file);
            template = Velocity.getTemplate("umlc/codeGeneration/jmatter/app-config.xml.vm");
            template.merge(context, w);
            w.close();
        } catch (Exception rnfe) {
            rnfe.printStackTrace();
        }
    }

    private void generateAbstractEObject(UmlPackage p, UmlClass c, File outputDir) {
        VelocityContext context = new VelocityContext();
        context.put("date", new java.util.Date());
        context.put("package", p);
        context.put("class", c);
        context.put("allAttributes", c.attributes);
        context.put("associationEnds", c.associations);
        context.put("formatter", new Formatter());
        int iconCount = 0;
        String iconField = null;
        for (Iterator i = c.attributes.iterator(); i.hasNext(); ) {
            UmlAttribute u = (UmlAttribute) i.next();
            if (u.hasAnnotation("icon")) {
                iconCount++;
                iconField = u.name;
            }
        }
        if (iconCount > 1) System.out.println("Warning: There appears to be more than one icon field.");
        context.put("icon", iconField);
        if (c.hasAnnotation("search")) {
            String value = c.annotationValue("search");
            String[] fields = value.split("\\.");
            StringBuffer fieldString = new StringBuffer();
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) fieldString.append(".");
                fieldString.append("field(\"" + fields[i] + "\")");
            }
            context.put("defaultSearch", fieldString.toString());
        }
        Template template = null;
        try {
            File file = new File(outputDir.getAbsoluteFile() + "/" + c.getName() + ".java");
            FileWriter w = new FileWriter(file);
            template = Velocity.getTemplate("umlc/codeGeneration/jmatter/AbstractComplexEObject.vm");
            template.merge(context, w);
            w.close();
        } catch (Exception rnfe) {
            rnfe.printStackTrace();
        }
    }

    public Set<String> classAnnotationNames() {
        Set<String> valid = new HashSet<String>();
        valid.add("title");
        valid.add("icon16");
        valid.add("icon32");
        valid.add("plural");
        valid.add("color");
        valid.add("search");
        valid.add("no_class_list");
        valid.add("no_app_config");
        return valid;
    }

    public DefaultProperty getClassAnnotationByName(String name) {
        DefaultProperty prop = new DefaultProperty();
        prop.setName(name);
        prop.setDisplayName(name);
        prop.setEditable(true);
        if ("title".equals(name)) {
            prop.setType(String.class);
            prop.setDisplayName("a. Title");
            prop.setCategory("1. Main");
            prop.setShortDescription("Every JMatter class you define should specify a title and how titles for instances should be formatted.");
        }
        if ("plural".equals(name)) {
            prop.setType(String.class);
            prop.setDisplayName("b. Plural Name");
            prop.setCategory("1. Main");
            prop.setShortDescription("Icon (small) that represents the entity. Must be a jpg,gif,png");
        }
        if ("search".equals(name)) {
            prop.setType(String.class);
            prop.setDisplayName("c. Search Field");
            prop.setCategory("1. Main");
            prop.setShortDescription("The default field used to search on. Use property names (eg name.subname)");
        }
        if ("no_class_list".equals(name)) {
            prop.setType(Boolean.class);
            prop.setDisplayName("d. No Class List");
            prop.setCategory("1. Main");
            prop.setShortDescription("Set to true if you dont want the class to show in the left sidebar.");
        }
        if ("no_app_config".equals(name)) {
            prop.setType(Boolean.class);
            prop.setDisplayName("e. No Persistance");
            prop.setCategory("1. Main");
            prop.setShortDescription("Set to true if you dont want this class to be a base persistant class");
        }
        if ("icon16".equals(name)) {
            prop.setType(java.io.File.class);
            prop.setDisplayName("a. Icon (16x16)");
            prop.setCategory("2. Display");
            prop.setShortDescription("Icon (small) that represents the entity. Must be a jpg,gif,png");
        }
        if ("icon32".equals(name)) {
            prop.setType(java.io.File.class);
            prop.setDisplayName("b. Icon (32x32)");
            prop.setCategory("2. Display");
            prop.setShortDescription("Icon (large) that represents the entity. Must be a jpg,gif,png");
        }
        if ("color".equals(name)) {
            prop.setType(java.awt.Color.class);
            prop.setDisplayName("c. Color");
            prop.setCategory("2. Display");
            prop.setShortDescription("The color of the Title bar for the class.");
        }
        return prop;
    }

    public Set<Object> suggestedClassAnnotationValues(String annotation) {
        return new HashSet();
    }

    public Set<String> suggestedPropertyTypes() {
        Set<String> types = new TreeSet<String>();
        types.add("StringEO");
        types.add("DateEO");
        types.add("TextEO");
        types.add("IntEO");
        types.add("BooleanEO");
        types.add("BusinessContact");
        types.add("Business");
        types.add("CharEO");
        types.add("ChoiceEO");
        types.add("Contact");
        types.add("DateEO");
        types.add("DateTime");
        types.add("DateWithAge");
        types.add("DetailedPerson");
        types.add("Email");
        types.add("EmploymentInfo");
        types.add("FileEO");
        types.add("FileWEO");
        types.add("FloatEO");
        types.add("Folder");
        types.add("ImgEO");
        types.add("IntEO");
        types.add("Logo");
        types.add("LongEO");
        types.add("LoggedEvent");
        types.add("Name");
        types.add("Note");
        types.add("Password");
        types.add("Percent");
        types.add("Person");
        types.add("Photo");
        types.add("SSN");
        types.add("StringEO");
        types.add("TermsEO");
        types.add("TextEO");
        types.add("TimeEO");
        types.add("TimeInterval");
        types.add("TimeSpan");
        types.add("URI");
        types.add("USAddress");
        types.add("USDollar");
        types.add("USPhone");
        types.add("USZipCode");
        return types;
    }

    public Set<String> annotationNamesByPropertyType(String type) {
        Set<String> valid = new HashSet<String>();
        valid.add("label");
        valid.add("displaysize");
        valid.add("colsize");
        valid.add("identity");
        valid.add("required");
        valid.add("flatten");
        valid.add("tab");
        if ("Photo".equals(type) || "Logo".equals(type) || "ImgEO".equals(type)) valid.add("icon");
        return valid;
    }

    public DefaultProperty getPropertyAnnotationByName(String name) {
        DefaultProperty prop = new DefaultProperty();
        prop.setName(name);
        prop.setDisplayName(name);
        prop.setEditable(true);
        if ("label".equals(name)) {
            prop.setCategory("2. Display");
            prop.setDisplayName("a. Label");
            prop.setType(String.class);
            prop.setShortDescription("The label the user will see for this property.");
        }
        if ("displaysize".equals(name)) {
            prop.setCategory("2. Display");
            prop.setDisplayName("b. Label Size");
            prop.setType(Integer.class);
            prop.setShortDescription("The size of the text box for this property.");
        }
        if ("flatten".equals(name)) {
            prop.setCategory("2. Display");
            prop.setDisplayName("c. Flatten");
            prop.setType(Boolean.class);
            prop.setShortDescription("Flatten sub GUIs...more description needed.");
        }
        if ("icon".equals(name)) {
            prop.setCategory("2. Display");
            prop.setDisplayName("d. Icon for Instance of Class");
            prop.setType(Boolean.class);
            prop.setShortDescription("Is this property the icon for this class?");
        }
        if ("tab".equals(name)) {
            prop.setCategory("2. Display");
            prop.setDisplayName("e. Show in Tab");
            prop.setType(Boolean.class);
            prop.setShortDescription("Put display of this attribute in a seperate tab.");
        }
        if ("identity".equals(name)) {
            prop.setCategory("1. Main");
            prop.setDisplayName("a. Identity");
            prop.setType(Boolean.class);
            prop.setShortDescription("Set to true if the values for this property should be unique.");
        }
        if ("required".equals(name)) {
            prop.setCategory("1. Main");
            prop.setDisplayName("b. Required");
            prop.setType(Boolean.class);
            prop.setShortDescription("Set to true if the value for this property is required.");
        }
        if ("colsize".equals(name)) {
            prop.setCategory("1. Main");
            prop.setDisplayName("c. DB Column Size");
            prop.setType(Integer.class);
            prop.setShortDescription("The size of the database column for this property.");
        }
        return prop;
    }

    public Set<Object> suggestedPropertyAnnotationValues(String type, String annotation) {
        return new HashSet();
    }
}
