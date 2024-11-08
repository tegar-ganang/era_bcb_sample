package br.gov.demoiselle.eclipse.main.core.editapp;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Element;
import br.gov.demoiselle.eclipse.main.IPluginFacade;
import br.gov.demoiselle.eclipse.util.editapp.NavigationCaseHelper;
import br.gov.demoiselle.eclipse.util.editapp.NavigationRuleHelper;
import br.gov.demoiselle.eclipse.util.utility.CoreConstants;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassRepresentation;
import br.gov.demoiselle.eclipse.util.utility.classwriter.FieldHelper;
import br.gov.demoiselle.eclipse.util.utility.plugin.Configurator;
import br.gov.demoiselle.eclipse.util.utility.xml.reader.XMLReader;

/**
 * Implements read/write Navigation Rules information
 * Read and write informations from/to facesconfig xml file 
 * @author CETEC/CTJEE
 * see IPluginFacade
 */
public class NavigationFacade implements IPluginFacade<NavigationRuleHelper>, CoreConstants {

    private String path = null;

    private String xml = null;

    private String packageName = null;

    private boolean insert = false;

    /**
	 * Returns all navigation rules of the project, it is in XML at the parameter path
	 * @return returns object of type NavigationRuleHelper
	 */
    @SuppressWarnings("unchecked")
    public List<NavigationRuleHelper> read() {
        List<Element> elements = XMLReader.readXML(path, null, NODE_NAV_RULE);
        ArrayList<NavigationRuleHelper> rules = new ArrayList<NavigationRuleHelper>();
        if (elements != null) {
            for (Element element : elements) {
                NavigationRuleHelper rule = new NavigationRuleHelper();
                List<Content> listaComent = element.getContent();
                Iterator<Content> itComment = listaComent.iterator();
                while (itComment.hasNext()) {
                    Content content = itComment.next();
                    if (content instanceof Comment) {
                        Comment comment = (Comment) content;
                        rule.setId(comment.getText().trim());
                        break;
                    }
                }
                List<Element> lista = element.getChildren(NODE_NAV_CASE, element.getNamespace());
                Iterator<Element> itCase = lista.iterator();
                while (itCase.hasNext()) {
                    Element elementCase = itCase.next();
                    NavigationCaseHelper navCase = new NavigationCaseHelper();
                    navCase.setFromAction(elementCase.getChildTextTrim("from-action", elementCase.getNamespace()));
                    navCase.setFromOutcome(elementCase.getChildTextTrim("from-outcome", elementCase.getNamespace()));
                    navCase.setToViewId(elementCase.getChildTextTrim("to-view-id", elementCase.getNamespace()));
                    rule.addCase(navCase);
                }
                rules.add(rule);
            }
        }
        Configurator reader = new Configurator();
        this.packageName = reader.readNavRulePackage(xml);
        return rules;
    }

    public void write(NavigationRuleHelper navRule) throws Exception {
        List<NavigationRuleHelper> rules = new ArrayList<NavigationRuleHelper>();
        rules.add(navRule);
        write(rules);
    }

    public void write(List<NavigationRuleHelper> navRules) throws Exception {
        List<Element> list = new ArrayList<Element>();
        ArrayList<String> listAlias = new ArrayList<String>();
        if (navRules != null) {
            try {
                this.setInsert(hasNavigation());
                for (Iterator<?> iterator = navRules.iterator(); iterator.hasNext(); ) {
                    NavigationRuleHelper navigationRuleHelper = (NavigationRuleHelper) iterator.next();
                    Element newNavRule = new Element(NODE_NAV_RULE);
                    Comment comment = new Comment(navigationRuleHelper.getId());
                    newNavRule.addContent(comment);
                    list.add(newNavRule);
                    for (NavigationCaseHelper navigationCaseHelper : navigationRuleHelper.getNavCases()) {
                        Element newNavCase = new Element(NODE_NAV_CASE, newNavRule.getNamespace());
                        newNavRule.addContent(newNavCase);
                        if (navigationCaseHelper.getFromAction() != null) {
                            if (listAlias.indexOf(navigationCaseHelper.getFromAction()) < 0) {
                                listAlias.add(navigationCaseHelper.getFromAction());
                            }
                            Element newFromAction = new Element(NODE_FROM_ACTION, newNavRule.getNamespace());
                            newFromAction.setText(navigationCaseHelper.getFromAction());
                            newNavCase.addContent(newFromAction);
                        }
                        if (navigationCaseHelper.getFromOutcome() != null) {
                            if (listAlias.indexOf(navigationCaseHelper.getFromOutcome()) < 0) {
                                listAlias.add(navigationCaseHelper.getFromOutcome());
                            }
                            Element newFromOutCome = new Element(NODE_FROM_OUTCOME, newNavRule.getNamespace());
                            newFromOutCome.setText(navigationCaseHelper.getFromOutcome());
                            newNavCase.addContent(newFromOutCome);
                        }
                        if (navigationCaseHelper.getToViewId() != null) {
                            Element newToViewId = new Element(NODE_TO_VIEW, newNavRule.getNamespace());
                            newToViewId.setText(navigationCaseHelper.getToViewId());
                            newNavCase.addContent(newToViewId);
                        }
                    }
                }
                XMLReader.writeXML(BASIC_XML_FACES, path, null, NODE_NAV_RULE, list, insert);
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    public void writeConstantes(String path, List<NavigationRuleHelper> navRules) throws Exception {
        ClassHelper clazzAlias = new ClassHelper();
        clazzAlias.setName(ALIAS_CLASS_NAV);
        clazzAlias.setPackageName(packageName);
        List<FieldHelper> fields = new ArrayList<FieldHelper>();
        if (navRules != null) {
            try {
                for (NavigationRuleHelper navigationRuleHelper : navRules) {
                    for (NavigationCaseHelper navigationCaseHelper : navigationRuleHelper.getNavCases()) {
                        if (navigationCaseHelper.getFromOutcome() != null) {
                            String name = ALIAS_PREFIX + navigationCaseHelper.getFromOutcome().toUpperCase();
                            if (!findField(fields, name)) {
                                FieldHelper field = new FieldHelper();
                                field.setName(name);
                                field.setValue("\"" + navigationCaseHelper.getFromOutcome() + "\"");
                                field.setModifier(Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL);
                                field.setType(new ClassRepresentation(String.class.getName()));
                                field.setHasGetMethod(false);
                                field.setHasSetMethod(false);
                                fields.add(field);
                            }
                        }
                    }
                }
                clazzAlias.setFields(fields);
                FileUtil.writeClassFile(path, clazzAlias, false, false);
                Configurator reader = new Configurator();
                reader.writeNavRulePackage(packageName, xml, false);
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    private boolean findField(List<FieldHelper> fields, String name) {
        for (FieldHelper field : fields) {
            if (field.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getPackageName() {
        if (packageName == null || packageName.equals("")) {
            Configurator reader = new Configurator();
            this.packageName = reader.readNavRulePackage(xml);
        }
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
	 * 
	 * @return  if has tag for Navigation (NODE_NAVRULEPACKAGE) elements in xml file 
	 * @throws Exception 
	*/
    public boolean hasNavigation() {
        try {
            if (XMLReader.hasElement(xml, NODE_NAVRULEPACKAGE)) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error while search NODE_NAVRULEPACKAGE..., then will create this. ");
            return false;
        }
        return false;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    /**
	 * Write the Package Name on Demoiselle's File configuration
	 * @throws CoreException
	 */
    public void writePackage() {
        Configurator reader = new Configurator();
        reader.writeNavRulePackage(packageName, xml, false);
    }
}
