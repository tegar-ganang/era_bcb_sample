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
import org.personalsmartspace.spm.access.api.repo.AccessControlRepoException;
import org.personalsmartspace.spm.access.api.repo.IDecisionRepo;
import org.personalsmartspace.spm.access.impl.AccessControlDecision;
import org.personalsmartspace.spm.access.impl.repo.FileDecisionRepo;
import org.personalsmartspace.spm.access.test.util.FooPermission;
import org.personalsmartspace.spm.identity.api.platform.DigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;

/**
 * @author <a href="mailto:nliampotis@users.sourceforge.net">Nicolas
 *         Liampotis</a> (ICCS)
 * @since 0.3.0
 */
public class FileDecisionRepoTest {

    private static final String aRequestorDpiStr = "BOB";

    private static final String anotherRequestorDpiStr = "ALICE";

    private static final String ctxAttrIdStr = "pss://myPSS@myDev/ENTITY/Person/3/ATTRIBUTE/Name/6";

    private static IDecisionRepo repoMgr;

    private static IAccessControlDecision aDecision;

    private static IAccessControlDecision anotherDecision;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        IDigitalPersonalIdentifier aRequestor = new DigitalPersonalIdentifier(aRequestorDpiStr);
        IDigitalPersonalIdentifier anotherRequestor = new DigitalPersonalIdentifier(anotherRequestorDpiStr);
        StubCtxDBManager dbMgr = new StubCtxDBManager();
        ICtxIdentifier ctxAttrId = dbMgr.createIdentifier(ctxAttrIdStr);
        dbMgr = null;
        aDecision = new AccessControlDecision(aRequestor);
        aDecision.add(new CtxPermission(ctxAttrId, "read"));
        anotherDecision = new AccessControlDecision(anotherRequestor);
        anotherDecision.add(new CtxPermission(ctxAttrId, "read,write"));
        anotherDecision.add(new FooPermission("foo", "bar"));
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aDecision = null;
        anotherDecision = null;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        repoMgr = new FileDecisionRepo();
        repoMgr.clear();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        repoMgr = null;
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.impl.repo.FileDecisionRepo#retrieve(org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier)}
     * .
     * 
     * @throws AccessControlRepoException
     */
    @Test
    public void testRetrieve() throws AccessControlRepoException {
        assertNull(repoMgr.retrieve(aDecision.getRequestor()));
        repoMgr.store(aDecision);
        IAccessControlDecision result = repoMgr.retrieve(aDecision.getRequestor());
        assertEquals(aDecision, result);
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.impl.repo.FileDecisionRepo#store(org.personalsmartspace.spm.access.api.platform.IAccessControlDecision)}
     * .
     * 
     * @throws AccessControlRepoException
     */
    @Test
    public void testStore() throws AccessControlRepoException {
        IAccessControlDecision decision = new AccessControlDecision(new DigitalPersonalIdentifier(aRequestorDpiStr));
        FooPermission fooPerm = new FooPermission("foo", "bar");
        decision.add(fooPerm);
        assertTrue(decision.contains(fooPerm));
        repoMgr.store(decision);
        IAccessControlDecision result = repoMgr.retrieve(decision.getRequestor());
        assertEquals(decision, result);
        decision.remove(fooPerm);
        assertFalse(decision.contains(fooPerm));
        repoMgr.store(decision);
        result = repoMgr.retrieve(decision.getRequestor());
        assertFalse(result.contains(fooPerm));
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.impl.repo.FileDecisionRepo#remove(IDigitalPersonalIdentifier)}
     * .
     * 
     * @throws AccessControlRepoException
     */
    @Test
    public void testRemove() throws AccessControlRepoException {
        boolean result = repoMgr.remove(aDecision.getRequestor());
        assertFalse(result);
        repoMgr.store(aDecision);
        IAccessControlDecision result2 = repoMgr.retrieve(aDecision.getRequestor());
        assertEquals(aDecision, result2);
        boolean result3 = repoMgr.remove(aDecision.getRequestor());
        assertTrue(result3);
    }

    /**
     * Test method for
     * {@link org.personalsmartspace.spm.access.impl.repo.FileDecisionRepo#clear()}
     * .
     * 
     * @throws AccessControlRepoException
     */
    @Test
    public void testClear() throws AccessControlRepoException {
        repoMgr.store(aDecision);
        IAccessControlDecision result = repoMgr.retrieve(aDecision.getRequestor());
        assertNotNull(result);
        repoMgr.clear();
        result = repoMgr.retrieve(aDecision.getRequestor());
        assertNull(result);
    }
}
