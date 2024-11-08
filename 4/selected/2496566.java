package org.personalsmartspace.spm.access.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.personalsmartspace.cm.db.impl.model.StubCtxAttributeIdentifier;
import org.personalsmartspace.cm.db.impl.model.StubCtxEntityIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAttributeIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxEntityIdentifier;
import org.personalsmartspace.spm.access.api.platform.AccessControlException;
import org.personalsmartspace.spm.access.api.platform.CtxPermission;
import org.personalsmartspace.spm.access.api.platform.IAccessControl;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecision;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecisionMgr;
import org.personalsmartspace.spm.access.api.platform.Permission;
import org.personalsmartspace.spm.access.impl.StubAccessControlDecisionMgr;
import org.personalsmartspace.spm.access.impl.StubAccessController;
import org.personalsmartspace.spm.identity.api.platform.DigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;

/**
 * Tests stub implementation of AccessControl component.
 * 
 * @author <a href="mailto:nliampotis@users.sourceforge.net">Nicolas
 *         Liampotis</a> (ICCS)
 * @since 0.4.0
 */
public class TestStubAccessController {

    private static IAccessControl accessController;

    private static IAccessControlDecisionMgr decisionMgr;

    private static IDigitalPersonalIdentifier bob;

    private static IDigitalPersonalIdentifier alice;

    private static Permission readLocation;

    private static Permission writeLocation;

    private static Permission readWriteLocation;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        bob = new DigitalPersonalIdentifier("Bob");
        alice = new DigitalPersonalIdentifier("Alice");
        ICtxEntityIdentifier personId = new StubCtxEntityIdentifier("operatorId", "localServiceId", "person", 37L);
        ICtxAttributeIdentifier locationId = new StubCtxAttributeIdentifier(personId, "location", 666L);
        readLocation = new CtxPermission(locationId, "read");
        writeLocation = new CtxPermission(locationId, "write");
        readWriteLocation = new CtxPermission(locationId, "read,write");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        readLocation = null;
        writeLocation = null;
        readWriteLocation = null;
        alice = null;
        bob = null;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        decisionMgr = new StubAccessControlDecisionMgr();
        accessController = new StubAccessController(decisionMgr, null);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        accessController = null;
        decisionMgr = null;
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.StubAccessController#checkPermission(org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier, org.personalsmartspace.spm.access.api.platform.Permission)}.
     * @throws AccessControlException 
     */
    @Test
    public void testGrantedPerm() throws AccessControlException {
        IAccessControlDecision decision = decisionMgr.create(alice);
        decision.add(readLocation);
        decisionMgr.update(decision);
        accessController.checkPermission(alice, readLocation);
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.StubAccessController#checkPermission(org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier, org.personalsmartspace.spm.access.api.platform.Permission)}.
     * @throws AccessControlException 
     */
    @Test
    public void testImpliedPerm() throws AccessControlException {
        IAccessControlDecision decision = decisionMgr.create(alice);
        decision.add(readWriteLocation);
        decisionMgr.update(decision);
        accessController.checkPermission(alice, readLocation);
        accessController.checkPermission(alice, writeLocation);
    }

    /**
     * Test method for {@link org.personalsmartspace.spm.access.impl.StubAccessController#checkPermission(org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier, org.personalsmartspace.spm.access.api.platform.Permission)}.
     * @throws AccessControlException 
     */
    @Test(expected = AccessControlException.class)
    public void testDeniedPerm() throws AccessControlException {
        IAccessControlDecision decision = decisionMgr.create(bob);
        decision.add(readLocation);
        decisionMgr.update(decision);
        accessController.checkPermission(bob, writeLocation);
    }
}
