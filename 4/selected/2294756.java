package au.edu.diasb.emmet.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import junit.framework.TestCase;
import au.edu.diasb.chico.config.PropertiesHelper;
import au.edu.diasb.emmet.model.EmmetAuthoritiesRegistry;
import au.edu.diasb.emmet.model.EmmetProfileSchema;
import au.edu.diasb.emmet.model.EmmetUserDetails;
import au.edu.diasb.emmet.model.EmmetUserDetailsBuilder;
import au.edu.diasb.emmet.protocol.EmmetAction;
import au.edu.diasb.emmet.util.EmmetProperties;

/**
 * Various helper methods for building the state used in various unit tests.
 * 
 * @author scrawley
 */
public class EmmetTestHelper {

    private EmmetTestHelper() {
    }

    ;

    public static Properties buildEmmetControllerProps() {
        return PropertiesHelper.initProperties(EmmetProperties.CONTAINER_PROP + "=/emmet" + "\n" + EmmetProperties.SITE_CONTAINER_PROP + "=/site" + "\n" + EmmetProperties.DEFAULT_ACCOUNT_EXPIRY_PROP + "=7 days" + "\n" + EmmetProperties.MAX_ACCOUNT_EXPIRY_PROP + "=never" + "\n" + EmmetProperties.DEFAULT_PASSWORD_EXPIRY_PROP + "=3 days" + "\n" + EmmetProperties.MAX_PASSWORD_EXPIRY_PROP + "=100 days" + "\n" + EmmetProperties.SERVICE_URL_PROP + "=http://localhost/emmet.svc" + "\n" + EmmetProperties.USER_URI_BASE_PROP + "=http://localhost/emmet/users/" + "\n" + EmmetProperties.RESET_TIMEOUT_PROP + "=30 minutes\n" + EmmetProperties.ACTIVATION_TIMEOUT_PROP + "=30 minutes\n" + EmmetProperties.USERNAME_PATTERN_PROP + "=[a-zA-Z][a-zA-Z0-9]+\n" + EmmetProperties.FORCE_HTTPS_PROP + "=false\n" + EmmetProperties.ADMIN_EMAIL_PROP + "=boss@example.com\n" + EmmetProperties.REGISTRATION_AGREE_PROP + "=false\n" + EmmetProperties.ACTIVATION_AGREE_PROP + "=false");
    }

    private static EmmetAuthoritiesRegistry authoritiesRegistry;

    public static synchronized EmmetAuthoritiesRegistry buildAuthoritiesRegistry() {
        if (authoritiesRegistry == null) {
            authoritiesRegistry = new EmmetAuthoritiesRegistry();
            authoritiesRegistry.setProperties(PropertiesHelper.initProperties(EmmetProperties.ROLES_PREFIX_PROP + " = ROLE_\n" + EmmetProperties.ADMIN_ROLE_PROP + " = ROLE_ADMIN\n" + EmmetProperties.DEFAULT_ROLES_PROP + " = ROLE_USER\n" + EmmetProperties.ALL_ROLES_PROP + " = ROLE_USER,ROLE_ADMIN,ROLE_SERVICE,ROLE_ANNOTATOR,ROLE_OAI"));
            try {
                authoritiesRegistry.afterPropertiesSet();
            } catch (Exception ex) {
                TestCase.fail("Failure while creating the test authoritiesRegistry: " + ex);
            }
        }
        return authoritiesRegistry;
    }

    public static List<String> buildAccessList() {
        return Arrays.asList(new String[] { EmmetAction.activateUser + " = ROLE_ADMIN+IS_TARGET_USER+IS_CONFIRMED(Do you really want to deactivate your own account?),ROLE_ADMIN", EmmetAction.addIdentity + " = ROLE_ADMIN+IS_TARGET_USER+HAS_PASSWORD,ROLE_ADMIN,ROLE_USER+IS_TARGET_USER+HAS_PASSWORD", EmmetAction.addGroupMember + " = ROLE_ADMIN", EmmetAction.addUser + " = ROLE_ADMIN", EmmetAction.changeAuthorities + " = ROLE_ADMIN+IS_TARGET_USER+HAS_PASSWORD,ROLE_ADMIN", EmmetAction.changePassword + " = ROLE_ADMIN+IS_TARGET_USER+HAS_PASSWORD,ROLE_ADMIN,ROLE_USER+IS_TARGET_USER+HAS_PASSWORD", EmmetAction.changeGroupOwner + " = ROLE_ADMIN", EmmetAction.changeGroupPermissions + " = ROLE_ADMIN", EmmetAction.changeUserDetails + " = ROLE_ADMIN,ROLE_USER+IS_TARGET_USER", EmmetAction.createGroup + " = ROLE_ADMIN", EmmetAction.doActivate + " = ", EmmetAction.doActivate2 + " = ", EmmetAction.doResetPassword + " = ", EmmetAction.doResetPassword2 + " = ", EmmetAction.fetchAuthentication + " = ", EmmetAction.fetchEmmetUrls + " = ", EmmetAction.getProperty + " = ROLE_ADMIN,ROLE_SERVICE,ROLE_USER", EmmetAction.initiateLogin + " = ", EmmetAction.initiateLogout + " = ", EmmetAction.initiateMyPasswordReset + " = ", EmmetAction.initiatePasswordReset + " = ROLE_ADMIN", EmmetAction.listGroups + " = ", EmmetAction.listUserDetails + " = ROLE_ADMIN", EmmetAction.listUserNames + " = ROLE_ADMIN", EmmetAction.lockUser + " = ROLE_ADMIN+IS_TARGET_USER+IS_CONFIRMED(Do you really want to lock your own account?),ROLE_ADMIN", EmmetAction.removeGroup + " = ROLE_ADMIN", EmmetAction.removeGroupMember + " = ROLE_ADMIN", EmmetAction.removeIdentity + " = ROLE_ADMIN+IS_TARGET_USER+HAS_PASSWORD,ROLE_ADMIN,ROLE_SERVICE,ROLE_USER+IS_TARGET_USER+HAS_PASSWORD", EmmetAction.removeUser + " = ROLE_ADMIN+IS_TARGET_USER+IS_CONFIRMED(Do you really want to remove your own account?),ROLE_ADMIN", EmmetAction.selfRegister + " = ", EmmetAction.setProperty + " = ROLE_ADMIN,ROLE_SERVICE,ROLE_USER+IS_TARGET_USER", EmmetAction.showGroupMembers + " = ROLE_ADMIN", EmmetAction.showGroupPage + " = ", EmmetAction.showGroupPermissions + " = ROLE_ADMIN", EmmetAction.showUserDetails + " = ROLE_ADMIN,ROLE_USER+IS_TARGET_USER" });
    }

    public static List<Properties> buildUserDetailsProps() {
        String[] rawDetails = new String[] { "userName = test\n" + "email = boss@example.com\n" + "password.password = secret\n" + "identity.uri = http://localhost/danno/users/test\n" + "authorities = ROLE_USER,ROLE_ADMIN,ROLE_ANNOTATOR,ROLE_OAI\n", "userName = steve\n" + "firstName = Stephen\n" + "lastName = Smith\n" + "password.password = secret\n" + "identity.uri = http://localhost/danno/users/steve\n" + "authorities = ROLE_USER,ROLE_ADMIN,ROLE_ANNOTATOR\n" + "profile.age = 1\n" + "profile.reputation = 2\n" + "profile.naughtiness = 3\n" + "profile.secret = 4\n", "userName = ron\n" + "firstName = Ron\n" + "lastName = McDonald\n" + "password.password = secret\n" + "identity.uri = http://localhost/danno/users/ron\n" + "authorities = ROLE_USER,ROLE_ADMIN\n", "userName = abdul\n" + "password.1.password = secret\n" + "password.2.password = old\n" + "password.2.status = INACTIVE\n" + "password.2.expiry = 2020-01-01 00:00:00\n" + "password.2.created = 2010-01-01 00:00:00\n" + "password.3.password = ancient\n" + "password.3.status = INACTIVE\n" + "password.3.expiry = 2020-01-01 00:00:00\n" + "password.3.created = 2000-01-01 00:00:00\n" + "identity.uri = http://localhost/danno/users/abdul\n" + "authorities = ROLE_USER\n", "userName = sleepy\n" + "firstName = Sleepy\n" + "password.password = activate-me\n" + "password.status = ACTIVATION\n" + "password.expiry = 2020-01-01 00:00:00\n" + "identity.uri = http://localhost/danno/users/sleepy\n" + "email = sleepy@disneyland.ca.us\n" + "authorities = ROLE_USER\n" + "activated = false\n", "userName = grumpy\n" + "firstName = Grumpy\n" + "password.password = secret\n" + "identity.uri = http://localhost/danno/users/grumpy\n" + "email = grumpy@disneyland.ca.us\n" + "authorities = ROLE_USER\n" + "locked = true\n", "userName = linus\n" + "firstName = Linus\n" + "password.password = \n" + "identity.uri = http://localhost/danno/users/linus\n" + "authorities = ROLE_USER\n", "userName = elvis\n" + "password.password = \n" + "identity.uri = http://localhost/danno/users/elvis\n" + "email = elvis@graveland.memphis.tn.us\n" + "authorities = ROLE_USER\n" + "expiry = 1977-08-16 15:30:00\n", "userName = oai\n" + "password.password = secret\n" + "identity.uri = http://localhost/danno/users/oai\n" + "authorities = ROLE_USER,ROLE_OAI\n", "userName = alice@example.org\n" + "firstName = Alice\n" + "lastName = Cooper\n" + "identity.uri = https://localhost/danno/users/alice\n" + "identity.domain = primary\n" + "identity.2.uri = https://g709-0157.itee.uq.edu.au/idp/shibboleth!https://gs710-9849.itee.uq.edu.au/shibboleth!nxha3QYmL5uW4l96ymBai+9OBY8=\n" + "identity.2.domain = shibboleth:test\n" + "authorities = ROLE_USER,ROLE_ANNOTATOR\n" + "profile.naughtiness = very\n" };
        Properties[] props = new Properties[rawDetails.length];
        for (int i = 0; i < rawDetails.length; i++) {
            props[i] = PropertiesHelper.initProperties(rawDetails[i]);
        }
        return Arrays.asList(props);
    }

    public static Set<EmmetUserDetails> buildUserDetailsSet() {
        EmmetUserDetailsBuilder builder = new EmmetUserDetailsBuilder(buildAuthoritiesRegistry());
        Set<EmmetUserDetails> res = new HashSet<EmmetUserDetails>();
        for (Properties props : buildUserDetailsProps()) {
            res.add(builder.build(props));
        }
        return res;
    }

    public static EmmetUserDetails buildUserDetails(String rawDetails) {
        return new EmmetUserDetailsBuilder(buildAuthoritiesRegistry()).build(PropertiesHelper.initProperties(rawDetails));
    }

    public static EmmetProfileSchema buildEmmetProfileSchema(EmmetAuthoritiesRegistry authoritiesRegistry) {
        EmmetProfileSchema res = new EmmetProfileSchema();
        res.setAuthoritiesRegistry(authoritiesRegistry);
        Map<String, Properties> schema = new HashMap<String, Properties>();
        String[] rawProperties = new String[] { "propName=age\nname=Age\nThe user's age in years\n" + "read=ROLE_ADMIN,ROLE_USER,ROLE_SERVICE\n" + "write=ROLE_ADMIN,ROLE_USER+IS_TARGET_USER,ROLE_SERVICE\n", "propName=reputation\nname=Reputation\nThe user's reputation\n" + "read=ROLE_ADMIN,ROLE_USER,ROLE_SERVICE\n" + "write=ROLE_ADMIN,ROLE_SERVICE\n", "propName=naughtiness\nname=Naughtiness\nThe user's naughtiness\n" + "read=ROLE_ADMIN\n" + "write=ROLE_ADMIN\n", "propName=secret\nname=Secret\nA user secret\n" + "read=ROLE_USER+IS_TARGET_USER\n" + "write=ROLE_USER+IS_TARGET_USER\n" };
        for (String str : rawProperties) {
            Properties props = PropertiesHelper.initProperties(str);
            schema.put(props.getProperty("propName"), props);
        }
        res.setSchema(schema);
        res.afterPropertiesSet();
        return res;
    }
}
