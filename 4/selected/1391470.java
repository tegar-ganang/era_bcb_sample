package org.personalsmartspace.spm.access.test;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.personalsmartspace.cm.db.impl.StubCtxDBManager;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAssociationIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAttributeIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxEntityIdentifier;
import org.personalsmartspace.spm.access.api.platform.CtxPermission;

/**
 * @author <a href="mailto:nliampotis@users.sourceforge.net">Nicolas
 *         Liampotis</a> (ICCS)
 * @since 0.3.0
 */
public class CtxPermissionTest {

    private static String ctxEntityIdStr = "pss://myPSS@myDev/ENTITY/Person/3";

    private static ICtxEntityIdentifier ctxEntityId;

    private static String ctxAttributeIdStr = "pss://myPSS@myDev/ENTITY/Person/3/ATTRIBUTE/Name/6";

    private static ICtxAttributeIdentifier ctxAttributeId;

    private static String ctxAssociationIdStr = "pss://myPSS@myDev/ASSOCIATION/hasFriends/9";

    private static ICtxAssociationIdentifier ctxAssociationId;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        StubCtxDBManager dbMgr = new StubCtxDBManager();
        ctxEntityId = (ICtxEntityIdentifier) dbMgr.createIdentifier(ctxEntityIdStr);
        ctxAttributeId = (ICtxAttributeIdentifier) dbMgr.createIdentifier(ctxAttributeIdStr);
        ctxAssociationId = (ICtxAssociationIdentifier) dbMgr.createIdentifier(ctxAssociationIdStr);
        dbMgr = null;
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ctxEntityId = null;
        ctxAttributeId = null;
        ctxAssociationId = null;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.api.platform.CtxPermission#getResource()}
     * .
     */
    @Test
    public void testGetResource() {
        CtxPermission readPerm = new CtxPermission(ctxAttributeId, "read");
        assertEquals(ctxAttributeId.toString(), readPerm.getName());
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.api.platform.CtxPermission#getActions()}
     * .
     */
    @Test
    public void testGetValidActions() {
        CtxPermission readPerm = new CtxPermission(ctxAttributeId, "read");
        assertEquals("read", readPerm.getActions());
        CtxPermission writePerm = new CtxPermission(ctxAttributeId, "write");
        assertEquals("write", writePerm.getActions());
        CtxPermission createPerm = new CtxPermission(ctxAttributeId, "create");
        assertEquals("create", createPerm.getActions());
        CtxPermission deletePerm = new CtxPermission(ctxAttributeId, "delete");
        assertEquals("delete", deletePerm.getActions());
        CtxPermission readWritePerm = new CtxPermission(ctxAttributeId, "write,read");
        assertEquals("read,write", readWritePerm.getActions());
        CtxPermission noPerm1 = new CtxPermission(ctxAssociationId, null);
        assertEquals("", noPerm1.getActions());
        CtxPermission noPerm2 = new CtxPermission(ctxEntityId, "");
        assertEquals("", noPerm2.getActions());
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.api.platform.CtxPermission#getActions()}
     * .
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetMalformedActions() {
        new CtxPermission(ctxAttributeId, "read,write,eat");
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.api.platform.CtxPermission#implies(org.personalsmartspace.spm.access.api.platform.Permission)}
     * .
     */
    @Test
    public void testImplies() {
        CtxPermission entReadWritePerm = new CtxPermission(ctxEntityId, "read,write");
        CtxPermission entReadPerm = new CtxPermission(ctxEntityId, "read");
        assertTrue(entReadWritePerm.implies(entReadPerm));
        assertFalse(entReadPerm.implies(entReadWritePerm));
        CtxPermission attrReadPerm = new CtxPermission(ctxAttributeId, "read");
        assertTrue(entReadWritePerm.implies(attrReadPerm));
        CtxPermission attrWritePerm = new CtxPermission(ctxAttributeId, "write");
        assertFalse(entReadPerm.implies(attrWritePerm));
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.api.platform.CtxPermission#equals(java.lang.Object)}
     * .
     */
    @Test
    public void testEquals() {
        CtxPermission perm1 = new CtxPermission(ctxAttributeId, "read,write");
        CtxPermission perm2 = new CtxPermission(ctxAttributeId, "write,read");
        assertEquals(perm1.hashCode(), perm2.hashCode());
        assertTrue(perm1.equals(perm2));
        CtxPermission perm3 = new CtxPermission(ctxAttributeId, "read");
        assertFalse(perm1.equals(perm3));
        CtxPermission perm4 = new CtxPermission(ctxEntityId, "read");
        assertFalse(perm3.equals(perm4));
    }
}
