package gov.lanl.RAD;

import gov.lanl.Utility.ConfigProperties;
import gov.lanl.Utility.NameService;
import gov.lanl.Utility.ORBThread;
import gov.lanl.Utility.ResourceNameTranslate;
import gov.lanl.SSLTools.ServiceInterface;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.DfResourceAccessDecision.*;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.ArrayList;

/**
 * Container for  PolicyEvaluators and DecisionCombinator for both PolicyEvaluatorLocatorNameAdmin and
 *  PolicyEvaluatorLocatorBasicAdmin
 *   They are stored in a HashTable
 *  This class is a singleton because the same object serves both parent objects.
 * @author $Author: dwforslund $
 * @version $Id: PolicyMgr.java 3499 2007-04-25 14:41:42Z dwforslund $
 */
public class PolicyMgr {

    private static org.omg.CORBA.ORB orb = null;

    private static Hashtable<String, Hashtable<String, PolicyDecisionEvaluators>> domains = new Hashtable<String, Hashtable<String, PolicyDecisionEvaluators>>();

    private static ConfigProperties props = new ConfigProperties();

    private static String configFile;

    private static ServiceInterface service;

    private static NameService ns;

    private static org.apache.log4j.Logger cat = org.apache.log4j.Logger.getLogger(PolicyMgr.class.getName());

    private static PolicyMgr policyMgr = null;

    private static AccessDecisionAdmin ada;

    /**
     * Instantiates a PolicyMgr object.  Creates a new Hashtable that stores
     * PolicyDecisionEvaluators for each resource name
     *  The init() functions builds PolicyMgr from a file or it can be externally populated.
     *  This is the class that would handle persistence of policies.
     */
    private PolicyMgr(org.omg.CORBA.ORB orb, String configFile) {
        setOrb(orb);
        setProperties(configFile);
        init();
    }

    /**
     *  set the Orb
     * @param theOrb
     */
    public static void setOrb(org.omg.CORBA.ORB theOrb) {
        orb = theOrb;
    }

    /**
     * set the Properties
     * @param file to read the properties from
     */
    public static void setProperties(String file) {
        configFile = file;
        if (configFile == null) configFile = "/policy";
        cat.debug("setProperties: " + configFile);
        props.readProperties(configFile);
    }

    public static void setNameService(NameService inNs) {
        ns = inNs;
    }

    /**
     *  PolicyMgr is a singleton that supports  PolicyEvaluatorLocatorNameAdmin,
     *  PolicyEvaluatorLocatorBasicAdmin, and PolicyEvaluatorLocatorPatternAdmin
     */
    public static PolicyMgr getInstance(org.omg.CORBA.ORB orb, String configFile) {
        setOrb(orb);
        setProperties(configFile);
        return current();
    }

    public static PolicyMgr getInstance(ServiceInterface inService, String configFile) {
        service = inService;
        setOrb(service.getORB());
        setProperties(configFile);
        return current();
    }

    /**
     *   return the current PolicyMgr and create it if it doesn't exist.
     */
    public static PolicyMgr current() {
        if (policyMgr == null) policyMgr = new PolicyMgr(orb, configFile);
        return policyMgr;
    }

    /**
     *  Initialize the data structures
     */
    private void init() {
        CreateResource ResObject = CreateResource.getInstance(orb, configFile);
        ArrayList<ResourceList_> resourceListList = ResObject.getResourceListList();
        if (resourceListList.size() == 0) {
            cat.error("Error in CreateResource;  Returned empty vector");
        }
        cat.debug("Number of Resources: " + resourceListList.size());
        for (ResourceList_ resourceList : resourceListList) {
            try {
                String domain = resourceList.getDomain();
                PolicyDecisionEvaluators pde = resourceList.getPolicyDecisionEvaluators();
                String resourceName = resourceList.getResourceName();
                Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
                if (evaluators == null) evaluators = new Hashtable<String, PolicyDecisionEvaluators>();
                evaluators.put(resourceName, pde);
                domains.put(domain, evaluators);
            } catch (Exception ex) {
                cat.error("init failed " + ex);
            }
        }
    }

    /**
     *  Add resource Name without evaluators
     *  @param resourceName
     *  @param domain
     *  @return boolean true if it succeeds in adding the resourceName or false if it exists already
     */
    public boolean addResourceName(String resourceName, String domain) {
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        PolicyDecisionEvaluators pde = null;
        if (evaluators == null) evaluators = new Hashtable(); else {
            pde = evaluators.get(resourceName);
            if (pde != null) return false;
        }
        evaluators.put(resourceName, pde);
        domains.put(domain, evaluators);
        return true;
    }

    /**
     * remove unused resource Name
     * @param resourceName  to be removed
     * @param domain
     * @return boolean true if successfully removed.
     */
    public boolean delResourceName(String resourceName, String domain) {
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        if (evaluators == null) {
            domains.remove(domain);
            return true;
        } else {
            Object o = evaluators.remove(resourceName);
            if (o != null) return true;
        }
        return false;
    }

    /**
     * set the PolicyEvaluators attached to a resourceName in a domain
     * @param policy_evaluator_list the default list of PolicyEvaluators
     * @param resourceName the ResourceName
     */
    public void setEvaluators(NamedPolicyEvaluator[] policy_evaluator_list, String resourceName, String domain) {
        cat.debug("Executing set_evaluators for " + "ResourceName: " + resourceName);
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        PolicyDecisionEvaluators pde = null;
        if (evaluators != null) {
            pde = evaluators.get(resourceName);
            if (pde != null) pde.policy_evaluator_list = policy_evaluator_list;
        } else evaluators = new Hashtable();
        if (pde == null) pde = new PolicyDecisionEvaluators(policy_evaluator_list, null);
        evaluators.put(resourceName, pde);
        domains.put(domain, evaluators);
    }

    /**
     *  Add policyevaluators
     * @param policy_evaluator_list is list of NamedPolicyEvaluators
     * @return boolean (true successfully added, false if not (duplicates))
     */
    public boolean addEvaluators(NamedPolicyEvaluator[] policy_evaluator_list, String resourceName, String domain) throws DuplicateEvaluatorName {
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        ArrayList<NamedPolicyEvaluator> evalVector = new ArrayList<NamedPolicyEvaluator>();
        PolicyDecisionEvaluators pde = null;
        if (evaluators != null) {
            pde = evaluators.get(resourceName);
            if (pde != null) {
                boolean dups = checkDuplicate(pde.policy_evaluator_list, policy_evaluator_list);
                if (!dups) return dups;
                for (int i = 0; i < pde.policy_evaluator_list.length; i++) {
                    evalVector.add(pde.policy_evaluator_list[i]);
                }
                for (int i = 0; i < policy_evaluator_list.length; i++) {
                    evalVector.add(policy_evaluator_list[i]);
                }
                pde.policy_evaluator_list = new NamedPolicyEvaluator[evalVector.size()];
                pde.policy_evaluator_list = evalVector.toArray(pde.policy_evaluator_list);
            }
        } else evaluators = new Hashtable<String, PolicyDecisionEvaluators>();
        if (pde == null) pde = new PolicyDecisionEvaluators(policy_evaluator_list, null);
        evaluators.put(resourceName, pde);
        domains.put(domain, evaluators);
        return true;
    }

    /**
     * Returns references to PolicyEvaluators that were stored persistently(?)
     * Actually retrieves names from the database and gets corresponding
     * references by calling resolve on the rootContext
     * @param resourceName     String representation of Resource Name Component List
     * @param domain          contains resource_naming_authority
     * @return returns a list of references to PolicyEvaluators
     */
    public NamedPolicyEvaluator[] getEvaluators(String resourceName, String domain) {
        cat.debug("Executing getEvaluators  for " + "ResourceName: " + resourceName);
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        PolicyDecisionEvaluators pde = null;
        if (evaluators != null) pde = evaluators.get(resourceName);
        if (pde != null) return pde.policy_evaluator_list; else return null;
    }

    /**
     * Check for Duplicate Named Policy Evaluators by comparing only the evaluator_name of each
     *
     * @param orig_npe
     * @param policy_evaluator_list
     *
     * @return boolean
     *
     * @see
     */
    public boolean checkDuplicate(NamedPolicyEvaluator[] orig_npe, NamedPolicyEvaluator[] policy_evaluator_list) {
        for (int i = 0; i < policy_evaluator_list.length; i++) {
            for (int j = 0; j < orig_npe.length; i++) {
                if (orig_npe[j].evaluator_name.equals(policy_evaluator_list[i].evaluator_name)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * delete the evaluators from the resource
     * @param policy_evaluator_list of evaluators to be deleted
     * @param resourceName containing the evaluators
     * @param domain which the resourceName is defined
     */
    public void delEvaluators(NamedPolicyEvaluator[] policy_evaluator_list, String resourceName, String domain) {
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        if (evaluators != null) {
            PolicyDecisionEvaluators pde = evaluators.get(resourceName);
            if (pde != null) {
                pde.policy_evaluator_list = deleteEvals(pde.policy_evaluator_list, policy_evaluator_list);
                evaluators.put(resourceName, pde);
            }
            domains.put(domain, evaluators);
        }
    }

    /**
     * Delete the NamedPolicyEvalutors and return the new list
     *
     * @param orig_npe
     * @param policy_evaluator_list
     *
     * @return  NamedPolicyEvaluator[] containing new list
     *
     * @see
     */
    public NamedPolicyEvaluator[] deleteEvals(NamedPolicyEvaluator[] orig_npe, NamedPolicyEvaluator[] policy_evaluator_list) {
        boolean present;
        int k, curr_length = orig_npe.length;
        for (int i = 0; i < policy_evaluator_list.length; i++) {
            present = false;
            for (int j = 0; j < curr_length; i++) {
                if (orig_npe[j].evaluator_name.equals(policy_evaluator_list[i].evaluator_name)) {
                    for (k = j; k < curr_length - 1; k++) {
                        orig_npe[k] = orig_npe[k + 1];
                    }
                    curr_length--;
                    present = true;
                }
            }
            if (!present) {
                return null;
            }
        }
        NamedPolicyEvaluator[] npe = new NamedPolicyEvaluator[curr_length];
        for (int i = 0; i < curr_length; i++) {
            npe[i] = orig_npe[i];
        }
        return npe;
    }

    /**
     * Used to get reference to default DecisionCombinator.  It retrieves the
     * name stored in the hashtable and then gets corresponding reference by
     * invoking resolve on rootContext
     * @param resourceName
     * @param domain
     * @return DecisionCombinator for that resourceName and domain
     */
    public DecisionCombinator getCombinator(String resourceName, String domain) {
        cat.debug("Executing getCombinator with " + resourceName + " " + domain);
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        PolicyDecisionEvaluators pde = null;
        if (evaluators != null) pde = evaluators.get(resourceName);
        if (pde != null) {
            return pde.decision_combinator;
        } else return null;
    }

    /**
     * Stores reference to default DecisionCombinator persistently.  Actually,
     * it stores the corresponding name in Hashtable.
     * @param decision_combinator the default DecisionCombinator
     * @param resourceName
     * @param domain
     */
    public void setCombinator(DecisionCombinator decision_combinator, String resourceName, String domain) {
        cat.debug("calling set_combinator for " + resourceName);
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        PolicyDecisionEvaluators pde = null;
        if (evaluators != null) pde = evaluators.get(resourceName);
        if (pde != null) {
            pde.decision_combinator = decision_combinator;
        } else {
            pde = new PolicyDecisionEvaluators(null, decision_combinator);
        }
        evaluators.put(resourceName, pde);
        domains.put(domain, evaluators);
        return;
    }

    /**
     * Get list of ResourceNames stored within a given domain
     * @param domain
     * @return String[] of ResourceNames
     */
    public String[] getResourceNames(String domain) {
        Hashtable<String, PolicyDecisionEvaluators> evaluators = domains.get(domain);
        String[] s = new String[0];
        Vector<String> v = new Vector<String>();
        if (evaluators != null) {
            for (Enumeration<String> en = evaluators.keys(); en.hasMoreElements(); ) v.addElement(en.nextElement());
            s = new String[v.size()];
            v.copyInto(s);
        }
        return s;
    }

    /**
     * Get a list of the domains
     * @return String[] list of domains.
     */
    public String[] getDomains() {
        String[] s;
        Vector<String> v = new Vector<String>();
        for (Enumeration<String> en = domains.keys(); en.hasMoreElements(); ) {
            v.addElement(en.nextElement());
        }
        s = new String[v.size()];
        v.copyInto(s);
        return s;
    }

    /**
     * Populate the remote PolicyEvaluatorLocatorPatternAdmin object with the contents of this PolicyMgr
     * This is useful to populate a remote RAD implementation with policy information
     */
    public void populate() {
        PolicyEvaluatorLocatorPatternAdmin pelpa = ada.get_policy_evaluator_locator().pattern_admin();
        String[] doms = getDomains();
        for (int i = 0; i < doms.length; i++) {
            String[] resources = getResourceNames(doms[i]);
            ResourceNameComponent[] rnc = new ResourceNameComponent[resources.length];
            DecisionCombinator dc;
            for (int j = 0; j < resources.length; j++) {
                cat.debug("populate: " + doms[i] + "," + resources[j]);
                try {
                    rnc = ResourceNameTranslate.toName(resources[j]);
                    ResourceName rn = new ResourceName(doms[i], rnc);
                    NamedPolicyEvaluator[] npe = getEvaluators(resources[j], doms[i]);
                    if (npe != null) {
                        pelpa.add_evaluators_by_pattern(npe, rn);
                        dc = getCombinator(resources[j], doms[i]);
                        pelpa.set_combinator_by_pattern(dc, rn);
                    } else cat.error("No evaluators obtained");
                } catch (InvalidResourceName e) {
                    cat.error("populate " + e);
                } catch (InvalidResourceNamePattern ie) {
                    cat.error("populate " + ie);
                } catch (PatternNotRegistered pe) {
                    cat.error("populate " + pe);
                } catch (InvalidPolicyEvaluatorList ipe) {
                    cat.error("populate " + ipe);
                } catch (DuplicateEvaluatorName de) {
                    cat.error("populate " + de);
                } catch (Exception e) {
                    cat.error("populate: ", e);
                }
            }
        }
    }

    /**
     *  compare to ResourceName objects using regexp with in the template expression
     * this implements the algorithm specified in the Resource Access Decision Service specification
     * Regexp is applied only the values, the names require an exact match
     * @param resourceInput   resource to be compared to template
     * @param resourceTemplate (contains the regexp)
     * @return boolean result of the comparison
     *
     */
    public static boolean match(ResourceName resourceInput, ResourceName resourceTemplate) {
        if (!(resourceInput.resource_naming_authority.equals(resourceTemplate.resource_naming_authority))) return false;
        ResourceNameComponent[] rncTemplate = resourceTemplate.resource_name_component_list;
        ResourceNameComponent[] rnc = resourceInput.resource_name_component_list;
        return match(rnc, rncTemplate);
    }

    /**
     *  compare to ResourceNameComponent[] objects using regexp with in the template expression
     * this implements the algorithm specified in the Resource Access Decision Service specification
     * Regexp is applied only the values, the names require an exact match
     * @param rnc   resource to be compared to template
     * @param rncTemplate (contains the regexp)
     * @return boolean result of the comparison
     *
     */
    public static boolean match(ResourceNameComponent[] rnc, ResourceNameComponent[] rncTemplate) {
        boolean match = false;
        for (int i = 0; i < Math.min(rncTemplate.length, rnc.length); i++) {
            String name = rncTemplate[i].name_string;
            String value = rncTemplate[i].value_string.trim();
            if ((name.equals("*")) && (value.equals("*"))) return true;
            if (!name.equals(rnc[i].name_string)) return false;
            try {
                gnu.regexp.RE re = new gnu.regexp.RE(value);
                match = re.isMatch(rnc[i].value_string);
                if (!match) return false;
            } catch (gnu.regexp.REException ex) {
                cat.error("matchResource: failed with resource " + rnc[i].name_string + " " + ex);
                break;
            }
        }
        return match;
    }

    /**
     *  Main class to enable creation of various policy objects.  Also, if specified, it
     *  will populate a remote PolicyEvaluatorLocatorPatternAdmin object.
     */
    public static void main(String argv[]) {
        ConfigProperties theProps = new ConfigProperties();
        theProps.setProperties("policy.cfg", argv);
        org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(argv, theProps);
        try {
            org.omg.CORBA.Object objPoa = orb.resolve_initial_references("RootPOA");
            org.omg.PortableServer.POA rootPOA = org.omg.PortableServer.POAHelper.narrow(objPoa);
            rootPOA.the_POAManager().activate();
            PolicyMgr policyMgr = PolicyMgr.getInstance(orb, theProps.getProperty("PolicyMgr", "policy"));
            String AD_Admin = theProps.getProperty("AD_Admin");
            ns = new NameService(orb, "");
            cat.debug("PolicyMgr object created");
            ORBThread orb_thread = new ORBThread(orb);
            cat.debug("starting orb thread");
            orb_thread.start();
            if (AD_Admin != null && !AD_Admin.equals("")) {
                org.omg.CORBA.Object obj = ns.connect(AD_Admin);
                ada = AccessDecisionAdminHelper.narrow(obj);
                cat.debug("connected to " + AD_Admin);
                if (theProps.getProperty("populate") != null) policyMgr.populate();
            }
        } catch (InvalidName name) {
            cat.error("main: failed " + name);
        } catch (AdapterInactive inactive) {
            cat.error("main failed because the Adapter was inactive: " + inactive);
        }
    }
}
