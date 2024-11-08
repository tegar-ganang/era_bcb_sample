package au.edu.diasb.emmet.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import net.tanesha.recaptcha.ReCaptcha;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import au.edu.diasb.emmet.EmmetUserDetailsService;
import au.edu.diasb.emmet.access.AccessRule;
import au.edu.diasb.emmet.access.AccessRuleParser;

/**
 * The profile schema specifies what properties may be get and set, by whom, 
 * what they mean and what values they may contain.
 * 
 * @author scrawley
 */
public class EmmetProfileSchema implements InitializingBean {

    /**
     * This class implements an immutable property descriptor for a single
     * profile property.
     * 
     * @author scrawley
     */
    public static class PropertyDescriptor {

        private final String propName;

        private final String readableName;

        private final String description;

        private final AccessRule readRule;

        private final AccessRule writeRule;

        public PropertyDescriptor(String propName, String readableName, String description, AccessRule readRule, AccessRule writeRule) {
            super();
            this.propName = propName;
            this.readableName = readableName;
            this.description = description;
            this.readRule = readRule;
            this.writeRule = writeRule;
        }

        /**
         * Return the name of the property.
         * 
         * @return the name of the property.
         */
        public String getPropName() {
            return propName;
        }

        /**
         * Return a human readable name for the property.
         * 
         * @return a human readable name for the property.
         */
        public String getReadableName() {
            return readableName;
        }

        /**
         * Return a human readable description of the property.
         * 
         * @return a human readable description of the property.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Return the AccessRule for getting this property. 
         * 
         * @return a set of access classes
         */
        public AccessRule getReadRule() {
            return readRule;
        }

        /**
         * Return the AccessRule for setting this property. 
         * 
         * @return a set of access classes
         */
        public AccessRule getWriteRule() {
            return writeRule;
        }
    }

    private Map<String, PropertyDescriptor> descriptors = new TreeMap<String, PropertyDescriptor>();

    private EmmetAuthoritiesRegistry authoritiesRegistry;

    private EmmetUserDetailsService ds;

    private ReCaptcha reCaptcha;

    private Map<String, Properties> schema;

    public EmmetProfileSchema() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(schema, "'schema' not set");
        Assert.notNull(authoritiesRegistry, "'authoritiesRegistry' not set");
        descriptors.clear();
        for (Map.Entry<String, Properties> entry : schema.entrySet()) {
            String propName = entry.getKey();
            Properties propProperties = entry.getValue();
            String readableName = propProperties.getProperty("name", propName);
            String description = propProperties.getProperty("description", "");
            AccessRule readRule = buildAccessRule(propProperties, "read", "access");
            AccessRule writeRule = buildAccessRule(propProperties, "write", "access");
            PropertyDescriptor desc = new PropertyDescriptor(propName, readableName, description, readRule, writeRule);
            descriptors.put(propName, desc);
        }
        Assert.notEmpty(descriptors, "'schema' map is empty");
    }

    /**
     * An authorities registry is required to translate the authority names
     * in the schema's property access rules into the corresponding 
     * EmmetAuthority values.
     * 
     * @param authoritiesRegistry
     */
    public final void setAuthoritiesRegistry(EmmetAuthoritiesRegistry authoritiesRegistry) {
        this.authoritiesRegistry = authoritiesRegistry;
    }

    /**
     * A user details service object is required if the property access rules 
     * make use of a 'HAS_PASSWORD' rule.  Otherwise, it is not necessary.
     * 
     * @param ds
     */
    public final void setUserDetailsService(EmmetUserDetailsService ds) {
        this.ds = ds;
    }

    public final ReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    /**
     * The policy object uses a ReCaptcha object to check
     * challenges / responses for the 'HAS_CAPTCHA' access rule
     * 
     * @param reCaptcha
     */
    public final void setReCaptcha(ReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    /**
     * Setting this property configures the schema from a map.  The
     * map is keyed by property name, and the corresponding values
     * are Properties objects giving the property accesses and human
     * readable names and descriptions.
     * 
     * @param schema
     */
    public void setSchema(Map<String, Properties> schema) {
        this.schema = schema;
    }

    /**
     * Get the property names defined by the schema.
     * 
     * @return a set of names.
     */
    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(descriptors.keySet());
    }

    /**
     * Get the property descriptors for the schema.
     * 
     * @return a collection of descriptors.
     */
    public Collection<PropertyDescriptor> getDescriptors() {
        return Collections.unmodifiableCollection((Collection<? extends PropertyDescriptor>) descriptors.values());
    }

    /**
     * Get the descriptor corresponding to a property name.
     * @param propName
     * @return the property's descriptor, or {@literal null} if the
     *     property name is unknown.
     */
    public PropertyDescriptor lookupProperty(String propName) {
        return descriptors.get(propName);
    }

    private AccessRule buildAccessRule(Properties propProperties, String propName, String altPropName) {
        String ruleSet = propProperties.getProperty(propName);
        if (ruleSet == null) {
            ruleSet = propProperties.getProperty(altPropName);
        }
        if (ruleSet == null) {
            throw new IllegalArgumentException("No access rules for property '" + propName + "'");
        }
        AccessRuleParser parser = new AccessRuleParser(authoritiesRegistry, ds, reCaptcha);
        return parser.parse(ruleSet);
    }
}
