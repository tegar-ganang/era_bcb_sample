package br.gov.demoiselle.eclipse.main.core.editapp;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jdom.Element;
import br.gov.demoiselle.eclipse.main.IPluginFacade;
import br.gov.demoiselle.eclipse.util.editapp.ManagedBeanHelper;
import br.gov.demoiselle.eclipse.util.utility.CommonsUtil;
import br.gov.demoiselle.eclipse.util.utility.CoreConstants;
import br.gov.demoiselle.eclipse.util.utility.EclipseUtil;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;
import br.gov.demoiselle.eclipse.util.utility.FrameworkUtil;
import br.gov.demoiselle.eclipse.util.utility.ReflectionUtil;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassRepresentation;
import br.gov.demoiselle.eclipse.util.utility.classwriter.FieldHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.MethodHelper;
import br.gov.demoiselle.eclipse.util.utility.plugin.Configurator;
import br.gov.demoiselle.eclipse.util.utility.xml.reader.XMLReader;

/**
 * Implements read/write Managed Bean classes information
 * Reads Beans from a list in the wizard xml file, locates in selected project
 * Writes Beans configuration and java file
 * see IPluginFacade 
 * @author CETEC/CTJEE
 */
public class ManagedBeanFacade implements IPluginFacade<ManagedBeanHelper>, CoreConstants {

    private String facesXml;

    private String xml;

    private ClassRepresentation constantClass = null;

    private boolean insert = false;

    public List<ManagedBeanHelper> read() {
        Configurator reader = new Configurator();
        List<ManagedBeanHelper> mbList = reader.readBeans(xml);
        if (mbList != null && mbList.size() > 0) {
            for (ManagedBeanHelper bean : mbList) {
                bean.setReadOnly(true);
            }
        }
        return mbList;
    }

    /**
	 * Write a BC of the parameter
	 * 
	 * @param BusinessControllerHelper BC to be written
	 * @throws Exception 
	 */
    public void write(ManagedBeanHelper bean) throws Exception {
        List<ManagedBeanHelper> beans = new ArrayList<ManagedBeanHelper>();
        beans.add(bean);
        write(beans);
    }

    /**
	 * Method that write managed beans classes and update the facesconfig XML
	 * @param beans - List of managed bean to be writing
	 * @param insert - Parameter that indicates how to write the elements of xml 
	 * 				   (true  = Insert all elements preserving the exists of XML; 
	 * 					false = Remove all elements from XML and insert all elements of the list)
	 * @throws Exception 
	 */
    public void write(List<ManagedBeanHelper> beans) throws Exception {
        List<Element> list = new ArrayList<Element>();
        if (beans != null) {
            try {
                this.setInsert(!this.hasMB());
                for (Iterator<?> iterator = beans.iterator(); iterator.hasNext(); ) {
                    ManagedBeanHelper bean = (ManagedBeanHelper) iterator.next();
                    if (!bean.isReadOnly()) {
                        ClassHelper clazz = extractManagedBeanClass(bean);
                        FileUtil.writeClassFile(bean.getAbsolutePath(), clazz, true, false);
                    }
                    Element novoMB = new Element(NODE_MANAGED_BEAN);
                    Element mbName = new Element(NODE_MANAGED_BEAN_NAME, novoMB.getNamespace());
                    mbName.setText(bean.getVarName());
                    Element mbClass = new Element(NODE_MANAGED_BEAN_CLASS, novoMB.getNamespace());
                    mbClass.setText(bean.getPackageName().concat(".").concat(CommonsUtil.toUpperCaseFirstLetter(bean.getName())));
                    Element mbScope = new Element(NODE_MANAGED_BEAN_SCOPE, novoMB.getNamespace());
                    mbScope.setText(bean.getScope());
                    novoMB.addContent(0, mbName);
                    novoMB.addContent(1, mbClass);
                    novoMB.addContent(2, mbScope);
                    list.add(novoMB);
                }
                XMLReader.writeXML(BASIC_XML_FACES, facesXml, null, NODE_MANAGED_BEAN, list, insert);
                Configurator reader = new Configurator();
                reader.writeBeans(beans, xml, insert);
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
	 * This method extract a object of type ClassHelper from a ManagedBeanHelper
	 * @param bean - The managed bean
	 * @return - Returns the class that will generate tha java code
	 */
    public ClassHelper extractManagedBeanClass(ManagedBeanHelper bean) {
        ClassHelper clazz = new ClassHelper();
        clazz.setExtendsClass(FrameworkUtil.getAbstractManagedBean());
        clazz.setName(CommonsUtil.toUpperCaseFirstLetter(bean.getName()));
        clazz.setPackageName(bean.getPackageName());
        ArrayList<ClassRepresentation> imports = new ArrayList<ClassRepresentation>();
        imports.add(FrameworkUtil.getAbstractManagedBean());
        imports.add(new ClassRepresentation(List.class.getName()));
        imports.add(new ClassRepresentation(ArrayList.class.getName()));
        imports.add(FrameworkUtil.getInjection());
        if (constantClass != null) {
            imports.add(constantClass);
        }
        clazz.setImports(imports);
        clazz.addImports(bean.getPojos());
        MethodHelper methodConstructor = new MethodHelper();
        methodConstructor.setName(clazz.getName());
        methodConstructor.setConstructor(true);
        methodConstructor.setModifier(Modifier.PUBLIC);
        methodConstructor.setBody("");
        java.util.List<FieldHelper> fields = new ArrayList<FieldHelper>();
        if (bean.getBusinessController() != null) {
            clazz.getImports().add(bean.getBusinessController());
            FieldHelper field = new FieldHelper();
            field.setModifier(Modifier.PRIVATE);
            field.setAnnotation(FrameworkUtil.getInjectionAnnotation());
            field.setType(bean.getBusinessController());
            field.setHasGetMethod(false);
            field.setHasSetMethod(false);
            field.setName(FrameworkUtil.getIVarName(bean.getBusinessController()));
            fields.add(field);
        }
        for (ClassRepresentation pojo : bean.getPojos()) {
            FieldHelper field = new FieldHelper();
            field.setModifier(Modifier.PRIVATE);
            String pojoName = pojo.getName();
            pojoName = CommonsUtil.toLowerCaseFirstLetter(pojoName);
            field.setName(pojoName);
            field.setValue("new " + pojo.getName() + "()");
            field.setType(pojo);
            fields.add(field);
            FieldHelper fieldList = new FieldHelper();
            fieldList.setModifier(Modifier.PRIVATE);
            ClassRepresentation listClass = new ClassRepresentation(List.class.getName());
            listClass.setGeneric(pojo.getName());
            fieldList.setType(listClass);
            fieldList.setName("list" + pojo.getName());
            fieldList.setValue("new ArrayList<" + pojo.getName() + ">()");
            fields.add(fieldList);
        }
        clazz.setFields(fields);
        clazz.addMethods(bean.getActions());
        clazz.getMethods().add(methodConstructor);
        return clazz;
    }

    public List<String> readConstantes() throws MalformedURLException, Exception {
        String packageName = getConstantesPackage();
        List<String> constantes = new ArrayList<String>();
        if (!packageName.equals("")) {
            Class<?> clazz = EclipseUtil.getClass(packageName + "." + ALIAS_CLASS_NAV);
            if (clazz != null) {
                constantes = ReflectionUtil.extractConstants(clazz);
                for (int i = 0; i < constantes.size(); i++) {
                    constantes.set(i, ALIAS_CLASS_NAV + "." + constantes.get(i));
                }
            }
        }
        return constantes;
    }

    public String getConstantesPackage() {
        Configurator reader = new Configurator();
        return reader.readNavRulePackage(xml);
    }

    public String getFacesXml() {
        return facesXml;
    }

    public void setFacesXml(String facesXml) {
        this.facesXml = facesXml;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public ClassRepresentation getConstantClass() {
        return constantClass;
    }

    public void setConstantClass(ClassRepresentation constantClass) {
        this.constantClass = constantClass;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    /**
	 * 
	 * @return  if has tag for Managed Beans (NODE_BEANS) elements in xml file 
	 * @throws Exception 
	*/
    public boolean hasMB() throws Exception {
        if (XMLReader.hasElement(xml, NODE_BEANS)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void searchMBsOnProject() throws MalformedURLException, Exception {
        List<String> resultBeans = null;
        List<String> resultBCs = null;
        String packageName = null;
        String varPackage = null;
        String varMB = null;
        List<Element> elements = null;
        List<ManagedBeanHelper> mbHelperList = null;
        elements = XMLReader.readXML(facesXml, null, null);
        if (elements != null && elements.size() > 0) {
            packageName = EclipseUtil.getPackageFullName("managedbean");
            varPackage = EclipseUtil.getPackageFullName(".bean");
            resultBeans = FileUtil.findJavaFiles(varPackage);
            varPackage = EclipseUtil.getPackageFullName("business");
            resultBCs = FileUtil.findJavaFiles(varPackage);
            if (mbHelperList == null) {
                mbHelperList = new ArrayList<ManagedBeanHelper>();
            }
            for (Element elem : elements) {
                if (elem.getName().compareTo("managed-bean") == 0) {
                    ManagedBeanHelper mb = new ManagedBeanHelper();
                    ;
                    mb.setAbsolutePath(EclipseUtil.getSourceLocation() + "/" + packageName.replace(".", "/") + "/");
                    mb.setPackageName(packageName);
                    List<Element> children = elem.getChildren();
                    for (Element child : children) {
                        String elName = child.getName();
                        String elText = child.getText();
                        if (elName.compareTo("managed-bean-name") == 0) {
                            mb.setVarName(elText);
                            mb.setName(elText.replaceFirst(elText.substring(0, 1), elText.substring(0, 1).toUpperCase()));
                            varMB = elText.substring(0, elText.lastIndexOf("MB"));
                            varMB = varMB.replaceFirst(varMB.substring(0, 1), varMB.substring(0, 1).toUpperCase());
                            for (String bean : resultBeans) {
                                mb.setPojos(new ArrayList<ClassRepresentation>());
                                if (bean.contains(varMB + ".java")) {
                                    bean = bean.replace("/", ".");
                                    bean = bean.substring(0, bean.lastIndexOf(".java"));
                                    mb.getPojos().add(new ClassRepresentation(bean));
                                    break;
                                }
                            }
                            for (String bc : resultBCs) {
                                if (bc.contains(varMB + "BC.java")) {
                                    bc = bc.replace("/", ".");
                                    bc = bc.substring(0, bc.lastIndexOf(".java"));
                                    mb.setBusinessController(new ClassRepresentation(bc));
                                    break;
                                }
                            }
                        }
                        if (elName.compareTo("managed-bean-scope") == 0) {
                            mb.setScope(elText);
                        }
                        if (elName.compareTo("managed-bean-class") == 0) {
                            try {
                                Method[] methodList = EclipseUtil.getDeclaredMethodsFromClass(elText);
                                mb.setActions(new ArrayList<MethodHelper>());
                                for (Method method : methodList) {
                                    MethodHelper action = new MethodHelper();
                                    action.setName(method.getName());
                                    action.setReturnType(new ClassRepresentation(elText));
                                    action.setBody("return TODO;");
                                    action.setReturnContent("TODO");
                                    mb.getActions().add(action);
                                }
                            } catch (Exception e) {
                                System.err.println(e.getMessage());
                                return;
                            }
                        }
                    }
                    mb.setReadOnly(false);
                    mbHelperList.add(mb);
                }
            }
        }
        if (mbHelperList != null && !mbHelperList.isEmpty()) {
            Configurator reader = new Configurator();
            reader.writeBeans(mbHelperList, getXml(), false);
        }
    }
}
