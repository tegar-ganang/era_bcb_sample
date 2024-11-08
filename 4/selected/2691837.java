package de.dgrid.bisgrid.common.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;
import x0PolicySchemaOs.oasisNamesTcXacml2.PolicyDocument;
import x0PolicySchemaOs.oasisNamesTcXacml2.PolicyType;
import x0PolicySchemaOs.oasisNamesTcXacml2.ResourceMatchDocument;
import x0PolicySchemaOs.oasisNamesTcXacml2.ResourceMatchType;
import x0PolicySchemaOs.oasisNamesTcXacml2.ResourceType;
import x0PolicySchemaOs.oasisNamesTcXacml2.ResourcesType;
import de.dgrid.bisgrid.common.BISGridConstants;
import de.dgrid.bisgrid.common.BISGridProperties;
import de.dgrid.bisgrid.common.exceptions.PolicyConfigProcessingException;
import de.dgrid.bisgrid.common.exceptions.PolicyDestructionFailedException;
import de.dgrid.bisgrid.common.exceptions.PolicyInstatiationFailedException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.security.IUASSecurityProperties;

/**
 * Helper class for managing XACML files and XACML Configuration file. The fact
 * that the PDP monitors the xacml.config file and rereads the XACML Policy
 * files every time when the config has changed.
 * 
 * @author ahoeing
 * 
 */
public class XACMLConfigurationHelper {

    private static Logger log = Logger.getLogger(XACMLConfigurationHelper.class);

    private static String policysuffix = "_security_policy.xml";

    /**
	 * Deploys a Security Policy for the process with name processName
	 * 
	 * The PDP monitors the xacml.config file and rereads the XACML Policy files
	 * every time when the config has changed. We use this to deploy a new
	 * security policy for workflows:
	 * 
	 * <ul>
	 * <li>store xacml policy on disk</li>
	 * <li>add entry in config file</li>
	 * </ul>
	 * 
	 * @param processName
	 *            the name of the process (this is used to create a file where
	 *            the XACML Policy is stored in)
	 * @param policyType
	 *            The Policy description
	 * @throws PolicyInstatiationFailedException
	 * @return filename where policy is stored
	 */
    @SuppressWarnings("unchecked")
    public static String deploySecurityPolicy(String processName, PolicyType policyType) throws PolicyInstatiationFailedException {
        log.debug("trying to deploy security policy:" + policyType);
        if (!checkResourceLimitations(processName, policyType)) {
            try {
                throw new PolicyInstatiationFailedException("The policy is not valid because it must be restricted to the deployed service. Insert or corrent ResoruceMatch for policy like " + example(processName));
            } catch (XmlException e) {
                throw new PolicyInstatiationFailedException("The policy is not valid because it must be restricted to the deployed service.");
            }
        }
        String securityPoliciesFolderString = BISGridProperties.getInstance().getProperty(BISGridProperties.BISGRID_XACML_POLICIES_DIRECTORY, "xacml");
        String securityFolicyFileString = securityPoliciesFolderString + File.separator + processName + policysuffix;
        String accessControlConfig = UAS.getSecurityProperties().getProperty(IUASSecurityProperties.UAS_CHECKACCESS_PDPCONFIG);
        log.info("trying to store policy document at: " + securityFolicyFileString);
        File securityPoliciesFile = new File(securityFolicyFileString);
        try {
            PolicyDocument policy = PolicyDocument.Factory.newInstance();
            policy.setPolicy(policyType);
            log.debug("saving PolicySetDocument: " + policy);
            policy.save(securityPoliciesFile);
        } catch (IOException e) {
            throw new PolicyInstatiationFailedException("Failed to write policy in file " + securityPoliciesFile.getAbsolutePath());
        }
        log.info("adding entry in xacml config: " + accessControlConfig);
        Element list = null;
        try {
            list = getListTagInXACMLConfig(accessControlConfig);
            boolean exists = false;
            List<Element> stringChildren = (List<Element>) list.getChildren("string", Namespace.getNamespace("http://sunxacml.sourceforge.net/schema/config-0.3"));
            if (stringChildren.isEmpty()) {
                throw new PolicyInstatiationFailedException("there is no <string> in list tag. It seems that no policy is instatiated");
            } else {
                for (Element cur : stringChildren) {
                    if ("string".equals(cur.getName())) {
                        if (securityFolicyFileString.equals(cur.getText())) {
                            exists = true;
                            break;
                        }
                    }
                }
            }
            if (!exists) {
                Element newPolicy = new Element("string", Namespace.getNamespace("http://sunxacml.sourceforge.net/schema/config-0.3"));
                newPolicy.addContent(securityFolicyFileString);
                list.addContent(newPolicy);
            } else {
                log.debug("entry for this workflow already exists. write config to force reread");
            }
            writeXMLDocInFile(accessControlConfig, list);
        } catch (PolicyConfigProcessingException e1) {
            securityPoliciesFile.delete();
            throw new PolicyInstatiationFailedException(e1.getMessage());
        }
        return securityFolicyFileString;
    }

    /**
	 * undeploys an XACML policy from the system 
	 *  
	 *   <ul>
	 *   <li>remove policy from xacml config file</li>
	 *   <li>remove xacml file from hd</li>
	 *   </ul>
	 *   
	 * @param policyFileName the filename where the policy is stored
	 * @throws PolicyDestructionFailedException
	 */
    @SuppressWarnings("unchecked")
    public static void undeploySecuirityPolicy(String policyFileName) throws PolicyDestructionFailedException {
        String accessControlConfig = UAS.getSecurityProperties().getProperty(IUASSecurityProperties.UAS_CHECKACCESS_PDPCONFIG);
        Element list = null;
        try {
            list = getListTagInXACMLConfig(accessControlConfig);
        } catch (PolicyConfigProcessingException e) {
            throw new PolicyDestructionFailedException(e.getMessage());
        }
        List<Element> stringChildren = (List<Element>) list.getChildren("string", Namespace.getNamespace("http://sunxacml.sourceforge.net/schema/config-0.3"));
        if (stringChildren.isEmpty()) {
            throw new PolicyDestructionFailedException("there is no <string> in list tag. It seems that no policy is instatiated");
        } else {
            for (Element cur : stringChildren) {
                if ("string".equals(cur.getName())) {
                    if (policyFileName.equals(cur.getText())) {
                        cur.getParent().removeContent(cur);
                        break;
                    }
                }
            }
        }
        try {
            writeXMLDocInFile(accessControlConfig, list);
        } catch (PolicyConfigProcessingException e) {
            throw new PolicyDestructionFailedException("cannot write xacml config file: " + accessControlConfig + ")");
        }
        File xacmlFile = new File(policyFileName);
        xacmlFile.delete();
    }

    private static void writeXMLDocInFile(String accessControlConfig, Element list) throws PolicyConfigProcessingException {
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        try {
            FileOutputStream out = new FileOutputStream(accessControlConfig);
            outputter.output(list.getDocument(), out);
            out.close();
        } catch (Exception e) {
            throw new PolicyConfigProcessingException("cannot wirte back xacml.config in file (" + accessControlConfig + ")");
        }
    }

    /**
	 * 
	 * @param accessControlConfig
	 * @return
	 * @throws PolicyConfigProcessingException
	 */
    private static Element getListTagInXACMLConfig(String accessControlConfig) throws PolicyConfigProcessingException {
        SAXBuilder sxbuild = new SAXBuilder();
        InputSource is = new InputSource(accessControlConfig);
        Document xacmlConfig;
        try {
            xacmlConfig = sxbuild.build(is);
        } catch (Exception e) {
            throw new PolicyConfigProcessingException("cant read or parse original xacml.config file of the unicore installation (file: " + accessControlConfig + ")");
        }
        Filter filter = new ElementFilter("policyFinderModule");
        Iterator<?> descendants = xacmlConfig.getDescendants(filter);
        while (descendants.hasNext()) {
            Element content = (Element) descendants.next();
            if ("com.sun.xacml.finder.impl.FilePolicyModule".equals(content.getAttribute("class").getValue())) {
                Element list = content.getChild("list", Namespace.getNamespace("http://sunxacml.sourceforge.net/schema/config-0.3"));
                if (list == null) throw new PolicyConfigProcessingException("connot find <list> tag as child of policyFinderModule[class=\"com.sun.xacml.finder.impl.FolePolicyModule\"]. Please contact system Administrator"); else return list;
            }
        }
        throw new PolicyConfigProcessingException("instatiation of policy failed because cannot find the correct tags in xacml.config configuration");
    }

    /**
	 * It is not alloweded that this policies are top-level policies. Furthermore the policies must be restricted to the newly created resoruces
	 * 
	 * The following example shows an example resource restriction
	 * <code>  
	 * <Resources>
     *   <Resource>
     *     <ResourceMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:anyURI-equal">
     *       <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#anyURI">WorkflowManagementServiceFactory</AttributeValue>
     *      <ResourceAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:resource:resource-id" DataType="http://www.w3.org/2001/XMLSchema#anyURI" MustBePresent="true" />
     *    </ResourceMatch>
     *   </Resource>
     *   <Resource>
     *     <ResourceMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:anyURI-equal">
     *       <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#anyURI">WorkflowManagementService</AttributeValue>
     *       <ResourceAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:resource:resource-id" DataType="http://www.w3.org/2001/XMLSchema#anyURI" MustBePresent="true" />
     *     </ResourceMatch>
     *   </Resource>
     *</Resources>
     *</code>
	 * 
	 * @param serviceName The service name (WorkflowService or Factory will be added to this name in the method)
	 * @param policy The policy
	 * @return is the policy ok for bisgrid?
	 */
    public static boolean checkResourceLimitations(String serviceName, PolicyType policy) {
        ResourcesType resources = policy.getTarget().getResources();
        for (ResourceType resource : resources.getResourceArray()) {
            for (ResourceMatchType match : resource.getResourceMatchArray()) {
                if (!"urn:oasis:names:tc:xacml:1.0:function:anyURI-equal".equals(match.getMatchId())) return false;
                if (!(match.getAttributeValue().toString().contains(serviceName + BISGridConstants.processNameExtensionFactory) || match.getAttributeValue().toString().contains(serviceName + BISGridConstants.processNameExtensionWorkflow))) return false;
            }
        }
        return true;
    }

    public static String example(String serviceName) throws XmlException {
        ResourceMatchDocument example;
        example = ResourceMatchDocument.Factory.parse("<ResourceMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:anyURI-equal\" xmlns=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\"><AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">" + serviceName + BISGridConstants.processNameExtensionFactory + "</AttributeValue><ResourceAttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\"/></ResourceMatch>");
        return example.toString();
    }
}
