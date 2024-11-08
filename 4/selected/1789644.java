package org.personalsmartspace.spm.access.test;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.personalsmartspace.cm.db.impl.StubCtxDBManager;
import org.personalsmartspace.cm.model.api.pss3p.ICtxIdentifier;
import org.personalsmartspace.spm.access.api.platform.CtxPermission;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecision;
import org.personalsmartspace.spm.access.impl.AccessControlDecision;
import org.personalsmartspace.spm.access.test.util.FooPermission;
import org.personalsmartspace.spm.identity.api.platform.DigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;

/**
 * @author <a href="mailto:nliampotis@users.sourceforge.net">Nicolas Liampotis</a> (ICCS)
 * @since 0.3.0
 */
public class AccessControlDecisionTest {

    private static final String requestorDpiStr = "BOB";

    private static final String ctxAttrIdStr = "pss://myPSS@myDev/ENTITY/Person/3/ATTRIBUTE/Name/6";

    private static IAccessControlDecision decision;

    private static IDigitalPersonalIdentifier requestor;

    private static CtxPermission readCtxPerm;

    private static CtxPermission readWriteCtxPerm;

    private static FooPermission fooPerm;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        requestor = new DigitalPersonalIdentifier(requestorDpiStr);
        StubCtxDBManager dbMgr = new StubCtxDBManager();
        ICtxIdentifier ctxAttrId = dbMgr.createIdentifier(ctxAttrIdStr);
        dbMgr = null;
        readCtxPerm = new CtxPermission(ctxAttrId, "read");
        readWriteCtxPerm = new CtxPermission(ctxAttrId, "read,write");
        fooPerm = new FooPermission("foo", "bar");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        requestor = null;
        readCtxPerm = null;
        readWriteCtxPerm = null;
        fooPerm = null;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        decision = new AccessControlDecision(requestor);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        decision = null;
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#getRequestor()}.
     */
    @Test
    public void testGetRequestor() {
        assertEquals(requestor, decision.getRequestor());
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#add(org.personalsmartspace.spm.access.api.platform.Permission)}.
     */
    @Test
    public void testAdd() {
        assertTrue(decision.add(readCtxPerm));
        assertTrue(decision.add(fooPerm));
        assertFalse(decision.add(readCtxPerm));
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#remove(org.personalsmartspace.spm.access.api.platform.Permission)}.
     */
    @Test
    public void testRemove() {
        assertFalse(decision.remove(fooPerm));
        decision.add(fooPerm);
        assertTrue(decision.remove(fooPerm));
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#implies(org.personalsmartspace.spm.access.api.platform.Permission)}.
     */
    @Test
    public void testImplies() {
        decision.add(fooPerm);
        assertFalse(decision.implies(readCtxPerm));
        decision.add(readWriteCtxPerm);
        assertTrue(decision.implies(readWriteCtxPerm));
        assertTrue(decision.implies(readCtxPerm));
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#contains(org.personalsmartspace.spm.access.api.platform.Permission)}.
     */
    @Test
    public void testContains() {
        assertFalse(decision.contains(fooPerm));
        decision.add(fooPerm);
        assertTrue(decision.contains(fooPerm));
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#clear()}.
     */
    @Test
    public void testClear() {
        decision.add(readCtxPerm);
        decision.add(fooPerm);
        decision.clear();
        assertFalse(decision.contains(readCtxPerm));
        assertFalse(decision.contains(fooPerm));
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.AccessControlDecision#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals() {
        IAccessControlDecision copyOfDecision = new AccessControlDecision(requestor);
        assertTrue(decision.equals(copyOfDecision));
        decision.add(fooPerm);
        assertFalse(decision.equals(copyOfDecision));
        copyOfDecision.add(fooPerm);
        assertTrue(decision.equals(copyOfDecision));
    }
}
