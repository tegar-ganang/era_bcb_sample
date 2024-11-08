package br.gov.demoiselle.eclipse.main.core.editapp;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import br.gov.demoiselle.eclipse.main.IPluginFacade;
import br.gov.demoiselle.eclipse.util.editapp.BusinessControllerHelper;
import br.gov.demoiselle.eclipse.util.utility.EclipseUtil;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;
import br.gov.demoiselle.eclipse.util.utility.FrameworkUtil;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassRepresentation;
import br.gov.demoiselle.eclipse.util.utility.classwriter.FieldHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.MethodHelper;
import br.gov.demoiselle.eclipse.util.utility.plugin.Configurator;
import br.gov.demoiselle.eclipse.util.utility.xml.reader.XMLReader;

/**
 * Implements read/write Business classes information
 * Reads BCs from a list in the wizard xml file, locates in selected project
 * Writes BCs configuration and java file 
 * @author CETEC/CTJEE
 */
public class BusinessControllerFacade implements IPluginFacade<BusinessControllerHelper> {

    private String xml = null;

    private boolean insert = false;

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
	 * Read all BCs of project
	 * 
	 * @return List of BCs 
	 */
    public List<BusinessControllerHelper> read() {
        Configurator reader = new Configurator();
        List<BusinessControllerHelper> bcList = reader.readBcs(xml);
        if (bcList != null && bcList.size() > 0) {
            for (BusinessControllerHelper bc : bcList) {
                bc.setReadOnly(true);
            }
        }
        return bcList;
    }

    /**
	 * Write a BC of the parameter
	 * 
	 * @param BusinessControllerHelper BC to be written
	 * @throws Exception 
	 */
    public void write(BusinessControllerHelper bc) throws Exception {
        List<BusinessControllerHelper> bcs = new ArrayList<BusinessControllerHelper>();
        bcs.add(bc);
        write(bcs);
    }

    /**
	 * Write all Business Controllers of the parameter List that is not marked with readonly attribute
	 * 
	 * @param List<BusinessControllerHelper> BC list to be written
	 * @throws Exception 
	 */
    public void write(List<BusinessControllerHelper> bcs) throws Exception {
        if (bcs != null) {
            try {
                this.setInsert(!this.hasBC());
                for (Iterator<?> iterator = bcs.iterator(); iterator.hasNext(); ) {
                    BusinessControllerHelper bc = (BusinessControllerHelper) iterator.next();
                    if (!bc.isReadOnly()) {
                        ClassHelper clazzInterface = generateInterface(bc);
                        FileUtil.writeClassFile(bc.getAbsolutePath(), clazzInterface, false, true);
                        bc.setBcInterface(new ClassRepresentation(bc.getInterfaceFullName()));
                        File dir = new File(bc.getAbsolutePathImpl());
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        ClassHelper clazzImpl = generateImplementation(bc);
                        FileUtil.writeClassFile(bc.getAbsolutePathImpl(), clazzImpl, true, false);
                    }
                }
                Configurator reader = new Configurator();
                reader.writeBcs(bcs, xml, insert);
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
	 * Generate a Interface ClassHelper object with informations of the bc passed by parameter
	 * This object will be used to create the .java file of the BC Interface
	 * 
	 * @param bc - BusinessControllerHelper
	 * @return ClassHelper 
	 */
    private static ClassHelper generateInterface(BusinessControllerHelper bc) {
        ClassHelper clazzInterface = new ClassHelper();
        clazzInterface.setModifier(Modifier.PUBLIC | Modifier.INTERFACE);
        clazzInterface.setName(bc.getInterfaceName());
        clazzInterface.setPackageName(bc.getPackageName());
        clazzInterface.setExtendsClass(FrameworkUtil.getIBusinessController());
        if (bc.getMethodList() != null && bc.getMethodList().size() > 0) {
            List<MethodHelper> methods = new ArrayList<MethodHelper>();
            for (MethodHelper method : bc.getMethodList()) {
                MethodHelper newMethod = new MethodHelper();
                newMethod.setModifier(method.getModifier());
                newMethod.setName(method.getName());
                newMethod.setParameters(method.getParameters());
                newMethod.setReturnType(method.getReturnType());
                newMethod.setBody(null);
                methods.add(newMethod);
            }
            clazzInterface.setMethods(methods);
        }
        return clazzInterface;
    }

    /**
	 * Generate a Class ClassHelper object with informations of the bc passed by parameter
	 * This object will be used to create the .java file of the BC Implementation Class
	 * 
	 * @param bc - BusinessControllerHelper
	 * @return ClassHelper 
	 */
    private static ClassHelper generateImplementation(BusinessControllerHelper bc) {
        ClassHelper clazzImpl = new ClassHelper();
        clazzImpl.setName(bc.getName());
        clazzImpl.setPackageName(bc.getImplementationPackageName());
        ArrayList<ClassRepresentation> imports = new ArrayList<ClassRepresentation>();
        imports.add(new ClassRepresentation(List.class.getName()));
        imports.add(FrameworkUtil.getInjection());
        clazzImpl.setImports(imports);
        clazzImpl.setInterfaces(new ArrayList<ClassRepresentation>());
        clazzImpl.getInterfaces().add(bc.getBcInterface());
        List<FieldHelper> fields = new ArrayList<FieldHelper>();
        if (bc.getDaoClass() != null) {
            FieldHelper iDao = new FieldHelper();
            iDao.setAnnotation(FrameworkUtil.getInjectionAnnotation());
            iDao.setType(bc.getDaoClass());
            iDao.setHasGetMethod(false);
            iDao.setHasSetMethod(false);
            iDao.setName(FrameworkUtil.getIVarName(bc.getDaoClass()));
            fields.add(iDao);
        }
        clazzImpl.setFields(fields);
        clazzImpl.setMethods(bc.getMethodList());
        return clazzImpl;
    }

    /**
	 * 
	 * @return if has tag to Business Controller (NODE_BCS) elements in xml file
	 * @throws Exception 
	 */
    public boolean hasBC() throws Exception {
        if (XMLReader.hasElement(xml, NODE_BCS)) {
            return true;
        }
        return false;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    /**
	 * Search for BCs artifacts, implemented on selected project, and write it on Demoiselle's configuration file.	
	 */
    public void searchBCsOnProject() {
        List<String> resultBCs = null;
        List<String> resultDAOs = null;
        String varPackage = null;
        String packageName = null;
        String varImpl = null;
        List<BusinessControllerHelper> bcHelperList = null;
        varImpl = ".implementation";
        packageName = EclipseUtil.getPackageFullName("business");
        resultBCs = FileUtil.findJavaFiles(packageName + varImpl);
        varPackage = EclipseUtil.getPackageFullName("dao");
        resultDAOs = FileUtil.findJavaFiles(varPackage + varImpl);
        if (resultBCs != null) {
            for (String bc : resultBCs) {
                if (!bc.contains("package-info")) {
                    bc = bc.replace("/", ".");
                    bc = bc.substring(bc.indexOf(varImpl) + varImpl.length() + 1, bc.lastIndexOf("BC.java"));
                    for (String dao : resultDAOs) {
                        if (dao.contains(bc + "DAO.java")) {
                            dao = dao.replace("/", ".");
                            dao = dao.substring(0, dao.lastIndexOf(".java"));
                            if (bcHelperList == null) {
                                bcHelperList = new ArrayList<BusinessControllerHelper>();
                            }
                            BusinessControllerHelper bcH;
                            bcH = new BusinessControllerHelper();
                            bcH.setName(bc);
                            bcH.setDaoClass(new ClassRepresentation(dao));
                            bcH.setAbsolutePath(EclipseUtil.getSourceLocation() + "/" + varPackage.replace(".", "/") + "/");
                            bcH.setPackageName(packageName);
                            bcHelperList.add(bcH);
                        }
                    }
                }
            }
        }
        if (bcHelperList != null && !bcHelperList.isEmpty()) {
            Configurator reader = new Configurator();
            reader.writeBcs(bcHelperList, getXml(), false);
        }
    }
}
