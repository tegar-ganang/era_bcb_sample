package br.gov.demoiselle.eclipse.main.core.editapp;

import java.util.ArrayList;
import java.util.List;
import br.gov.demoiselle.eclipse.main.IPluginFacade;
import br.gov.demoiselle.eclipse.util.editapp.FacadeHelper;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;
import br.gov.demoiselle.eclipse.util.utility.FrameworkUtil;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassRepresentation;
import br.gov.demoiselle.eclipse.util.utility.classwriter.FieldHelper;
import br.gov.demoiselle.eclipse.util.utility.plugin.Configurator;
import br.gov.demoiselle.eclipse.util.utility.xml.reader.XMLReader;

/**
 * Implements read/write Facade classes information
 * Reads Facades from a list in the wizard xml file, locates in selected project
 * Writes Facades configuration and java file
 * see IPluginFacade 
 * @author CETEC/CTJEE
 */
public class FacadeFacade implements IPluginFacade<FacadeHelper> {

    private String xml = null;

    private boolean insert = false;

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public List<FacadeHelper> read() {
        Configurator reader = new Configurator();
        List<FacadeHelper> facadeList = reader.readFacades(xml);
        if (facadeList != null && facadeList.size() > 0) {
            for (FacadeHelper facade : facadeList) {
                facade.setReadOnly(true);
            }
        }
        return facadeList;
    }

    /**
	 * Write all Facades of the parameter List that is not marked with readonly attribute
	 * 
	 * @param List<FacadeHelper> Facade list to be written
	 * @throws Exception 
	 */
    public void write(List<FacadeHelper> facades) throws Exception {
        if (facades != null) {
            try {
                this.setInsert(!this.hasFacade());
                for (FacadeHelper facade : facades) {
                    if (!facade.isReadOnly()) {
                        ClassHelper clazzImpl = generateImplementation(facade);
                        FileUtil.writeClassFile(facade.getAbsolutePath(), clazzImpl, true, true);
                    }
                }
                Configurator reader = new Configurator();
                reader.writeFacades(facades, xml, insert);
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
	 * Generate a Class ClassHelper object with informations of the facade passed by parameter
	 * This object will be used to create the .java file of the Facade Implementation Class
	 * 
	 * @param facade - FacadeHelper
	 * @return ClassHelper 
	 */
    private static ClassHelper generateImplementation(FacadeHelper facade) {
        ClassHelper clazzImpl = new ClassHelper();
        clazzImpl.setName(facade.getName());
        clazzImpl.setPackageName(facade.getPackageName());
        ArrayList<ClassRepresentation> imports = new ArrayList<ClassRepresentation>();
        imports.add(new ClassRepresentation(List.class.getName()));
        imports.add(FrameworkUtil.getInjection());
        clazzImpl.setImports(imports);
        clazzImpl.setInterfaces(new ArrayList<ClassRepresentation>());
        clazzImpl.getInterfaces().add(FrameworkUtil.getIFacade());
        List<FieldHelper> fields = new ArrayList<FieldHelper>();
        if (facade.getBusinessController() != null) {
            FieldHelper iBc = new FieldHelper();
            iBc.setAnnotation(FrameworkUtil.getInjectionAnnotation());
            iBc.setType(facade.getBusinessController());
            iBc.setHasGetMethod(false);
            iBc.setHasSetMethod(false);
            iBc.setName(FrameworkUtil.getIVarName(facade.getBusinessController()));
            fields.add(iBc);
        }
        clazzImpl.setFields(fields);
        return clazzImpl;
    }

    /**
	 * 
	 * @return if has tag for NODE_FACADES elements in xml file
	 * @throws Exception 
	 */
    public boolean hasFacade() throws Exception {
        if (XMLReader.hasElement(xml, NODE_FACADES)) {
            return true;
        }
        return false;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }
}
