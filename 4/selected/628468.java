package au.edu.diasb.emmet.test;

import java.util.HashMap;
import java.util.Properties;
import junit.framework.TestCase;
import au.edu.diasb.chico.config.PropertiesHelper;
import au.edu.diasb.emmet.access.OrRule;
import au.edu.diasb.emmet.access.PassRule;
import au.edu.diasb.emmet.model.EmmetProfileSchema;

public class EmmetProfileSchemaTest extends TestCase {

    public void testConstructor() {
        new EmmetProfileSchema();
    }

    public void testNoSchema() {
        try {
            EmmetProfileSchema ps = new EmmetProfileSchema();
            ps.afterPropertiesSet();
            fail("exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("'schema' not set", ex.getMessage());
        }
    }

    public void testNoAuthoritiesRegistry() {
        try {
            EmmetProfileSchema ps = new EmmetProfileSchema();
            ps.setSchema(new HashMap<String, Properties>());
            ps.afterPropertiesSet();
            fail("exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("'authoritiesRegistry' not set", ex.getMessage());
        }
    }

    public void testEmptySchema() {
        try {
            EmmetProfileSchema ps = new EmmetProfileSchema();
            ps.setAuthoritiesRegistry(EmmetTestHelper.buildAuthoritiesRegistry());
            ps.setSchema(new HashMap<String, Properties>());
            ps.afterPropertiesSet();
            fail("exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("'schema' map is empty", ex.getMessage());
        }
    }

    public void testBadAccess() {
        try {
            EmmetProfileSchema ps = new EmmetProfileSchema();
            ps.setAuthoritiesRegistry(EmmetTestHelper.buildAuthoritiesRegistry());
            HashMap<String, Properties> init = new HashMap<String, Properties>();
            init.put("fred", PropertiesHelper.initProperties("read=FISH"));
            ps.setSchema(init);
            ps.afterPropertiesSet();
            fail("exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("Unrecognized <leafRule> (FISH) : " + "expected an authority name or a builtin rule name", ex.getMessage());
        }
    }

    public void testOK1() {
        EmmetProfileSchema ps = new EmmetProfileSchema();
        ps.setAuthoritiesRegistry(EmmetTestHelper.buildAuthoritiesRegistry());
        HashMap<String, Properties> init = new HashMap<String, Properties>();
        init.put("fred", PropertiesHelper.initProperties("access="));
        ps.setSchema(init);
        ps.afterPropertiesSet();
        EmmetProfileSchema.PropertyDescriptor desc = ps.lookupProperty("fred");
        assertNotNull(desc);
        assertEquals("fred", desc.getPropName());
        assertEquals("fred", desc.getReadableName());
        assertEquals("", desc.getDescription());
        assertTrue(desc.getReadRule() instanceof PassRule);
        assertTrue(desc.getWriteRule() instanceof PassRule);
    }

    public void testOK2() {
        EmmetProfileSchema ps = new EmmetProfileSchema();
        ps.setAuthoritiesRegistry(EmmetTestHelper.buildAuthoritiesRegistry());
        HashMap<String, Properties> init = new HashMap<String, Properties>();
        init.put("fred", PropertiesHelper.initProperties("read=ROLE_USER,ROLE_ADMIN\nwrite=\nname=Frederic\ndescription=the great\n"));
        ps.setSchema(init);
        ps.afterPropertiesSet();
        EmmetProfileSchema.PropertyDescriptor desc = ps.lookupProperty("fred");
        assertNotNull(desc);
        assertEquals("fred", desc.getPropName());
        assertEquals("Frederic", desc.getReadableName());
        assertEquals("the great", desc.getDescription());
        assertTrue(desc.getReadRule() instanceof OrRule);
        assertTrue(desc.getWriteRule() instanceof PassRule);
    }
}
