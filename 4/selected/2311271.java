package org.personalsmartspace.rms050;

import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.osgi.util.tracker.ServiceTracker;
import org.personalsmartspace.cm.model.api.pss3p.CtxModelType;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAttribute;
import org.personalsmartspace.cm.model.api.pss3p.ICtxEntity;
import org.personalsmartspace.cm.model.api.pss3p.ICtxEntityIdentifier;
import org.personalsmartspace.cm.model.api.pss3p.ICtxIdentifier;
import org.personalsmartspace.impl.Activator;
import org.personalsmartspace.log.impl.PSSLog;
import org.personalsmartspace.spm.access.api.platform.CtxPermission;
import org.personalsmartspace.spm.access.api.platform.IAccessControl;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecision;
import org.personalsmartspace.spm.access.api.platform.IAccessControlDecisionMgr;
import org.personalsmartspace.spm.access.api.platform.Permission;
import org.personalsmartspace.spm.identity.api.platform.IIdentityManagement;

public class SetupInterCtx extends TestCase {

    static final String REMOTE_ENTITY = "remoteEntity";

    static final String REMOTE_EMPTY_ENTITY = "remoteEmptyEntity";

    static final String REMOTE_NAME_ATTRIBUTE = "remoteName";

    static final String ADD_ATTRIBUTE_3P = "addAttribute3p";

    static final String REMOVE_ATTRIBUTE_3P = "removeAttribute3p";

    static final String UPDATE_ATTRIBUTE_3P = "updateAttribute3p";

    static final String RANGE_ATTRIBUTE_NAME = "rangeAttribute";

    static final int RANGE_ATTRIBUTE_VALUE = 10;

    /**
     * Context Broker service tracker
     */
    private ServiceTracker ctxBrokerPlatformTracker;

    private ServiceTracker ctxBroker3pTracker;

    private ServiceTracker identityManagementTracker;

    private ServiceTracker accessControlTracker;

    private ServiceTracker decisionManagerTracker;

    /**
     * Logger
     */
    private PSSLog log = new PSSLog(this);

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        ctxBrokerPlatformTracker = new ServiceTracker(Activator.bundleContext, org.personalsmartspace.cm.broker.api.platform.ICtxBroker.class.getName(), null);
        ctxBrokerPlatformTracker.open();
        ctxBroker3pTracker = new ServiceTracker(Activator.bundleContext, org.personalsmartspace.cm.broker.api.pss3p.ICtxBroker.class.getName(), null);
        ctxBroker3pTracker.open();
        identityManagementTracker = new ServiceTracker(Activator.bundleContext, IIdentityManagement.class.getName(), null);
        identityManagementTracker.open();
        accessControlTracker = new ServiceTracker(Activator.bundleContext, IAccessControl.class.getName(), null);
        accessControlTracker.open();
        decisionManagerTracker = new ServiceTracker(Activator.bundleContext, IAccessControlDecisionMgr.class.getName(), null);
        decisionManagerTracker.open();
        cleanDb();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        ctxBrokerPlatformTracker.close();
        ctxBroker3pTracker.close();
        identityManagementTracker.close();
        accessControlTracker.close();
        decisionManagerTracker.close();
    }

    /**
     * Tests remote lookup of entity.
     * 
     * @throws Exception
     */
    public void testSetupIntraPssLookup() throws Exception {
        ICtxEntity entity;
        List<ICtxIdentifier> identifiers;
        List<ICtxEntityIdentifier> entities;
        entity = getPlatformCtxBroker().createEntity(REMOTE_EMPTY_ENTITY);
        entity = getPlatformCtxBroker().createEntity(REMOTE_ENTITY);
        final ICtxAttribute remoteNameAttribute = getPlatformCtxBroker().createAttribute(entity.getCtxIdentifier(), REMOTE_NAME_ATTRIBUTE);
        final ICtxAttribute remove3pAttribute = getPlatformCtxBroker().createAttribute(entity.getCtxIdentifier(), REMOVE_ATTRIBUTE_3P);
        final ICtxAttribute update3pAttribute = getPlatformCtxBroker().createAttribute(entity.getCtxIdentifier(), UPDATE_ATTRIBUTE_3P);
        final ICtxAttribute minValueAttribute = getPlatformCtxBroker().createAttribute(entity.getCtxIdentifier(), RANGE_ATTRIBUTE_NAME, RANGE_ATTRIBUTE_VALUE);
        getIdentityManagement().addMappedCtxIdentifier(getIdentityManagement().getPublicDigitalPersonalIdentifier(), remoteNameAttribute.getCtxIdentifier());
        getIdentityManagement().addMappedCtxIdentifier(getIdentityManagement().getPublicDigitalPersonalIdentifier(), remove3pAttribute.getCtxIdentifier());
        getIdentityManagement().addMappedCtxIdentifier(getIdentityManagement().getPublicDigitalPersonalIdentifier(), update3pAttribute.getCtxIdentifier());
        getIdentityManagement().addMappedCtxIdentifier(getIdentityManagement().getPublicDigitalPersonalIdentifier(), minValueAttribute.getCtxIdentifier());
        final Permission addAttributePermission = new CtxPermission(entity.getCtxIdentifier(), "create");
        final Permission remoteEntityPermission = new CtxPermission(entity.getCtxIdentifier(), "read");
        final Permission remoteNamePermission = new CtxPermission(remoteNameAttribute.getCtxIdentifier(), "read");
        final Permission remove3pPermission = new CtxPermission(remove3pAttribute.getCtxIdentifier(), "read,delete");
        final Permission update3pPermission = new CtxPermission(update3pAttribute.getCtxIdentifier(), "read,write");
        IAccessControlDecision decision = getAccessControlDecisionMgr().create(getIdentityManagement().getPublicDigitalPersonalIdentifier());
        decision.add(addAttributePermission);
        decision.add(remoteEntityPermission);
        decision.add(remoteNamePermission);
        decision.add(remove3pPermission);
        decision.add(update3pPermission);
        getAccessControlDecisionMgr().update(decision);
        identifiers = get3pCtxBroker().lookup(getIdentityManagement().getPublicDigitalPersonalIdentifier(), getIdentityManagement().getPublicDigitalPersonalIdentifier(), CtxModelType.ENTITY, REMOTE_ENTITY);
        assertEquals(1, identifiers.size());
        log.info("retrieving: " + identifiers.get(0));
        entity = (ICtxEntity) get3pCtxBroker().retrieve(getIdentityManagement().getPublicDigitalPersonalIdentifier(), identifiers.get(0));
        assertNotNull("Entity not retrieved", entity);
        assertEquals("Wrong identifier type returned", CtxModelType.ENTITY, entity.getModelType());
        assertEquals("Wrong entity type returned", REMOTE_ENTITY, entity.getType());
        assertEquals("Incorrect number of attributes returned - check target dpi mappings", 4, entity.getAttributesSize());
        identifiers = getPlatformCtxBroker().lookup(CtxModelType.ENTITY, REMOTE_EMPTY_ENTITY);
        assertEquals(1, identifiers.size());
        log.info("retrieving: " + identifiers.get(0));
        entity = (ICtxEntity) getPlatformCtxBroker().retrieve(identifiers.get(0));
        assertNotNull("Entity not retrieved", entity);
        assertEquals("Wrong identifier type returned", CtxModelType.ENTITY, entity.getModelType());
        assertEquals("Wrong entity type returned", REMOTE_EMPTY_ENTITY, entity.getType());
        entities = getPlatformCtxBroker().lookupEntities(REMOTE_ENTITY, RANGE_ATTRIBUTE_NAME, RANGE_ATTRIBUTE_VALUE);
        log.info("lookup entities by attribute value returns " + entities.size() + " entities");
        entities = getPlatformCtxBroker().lookupEntities(REMOTE_ENTITY, RANGE_ATTRIBUTE_NAME, RANGE_ATTRIBUTE_VALUE - 1, RANGE_ATTRIBUTE_VALUE + 1);
        log.info("lookup entities in range returns " + entities.size() + " entities");
    }

    /**
     * Gets an instance of an <code>ICtxBroker</code> service.
     * 
     * @return  An instance of <code>ICtxBroker</code>
     * @throws Exception if no context broker platform service is available
     */
    private org.personalsmartspace.cm.broker.api.platform.ICtxBroker getPlatformCtxBroker() throws Exception {
        org.personalsmartspace.cm.broker.api.platform.ICtxBroker platformCtxBroker = null;
        platformCtxBroker = (org.personalsmartspace.cm.broker.api.platform.ICtxBroker) ctxBrokerPlatformTracker.getService();
        log.info("returning " + platformCtxBroker);
        if (null == platformCtxBroker) {
            throw new Exception("platform context broker service not available");
        }
        return platformCtxBroker;
    }

    /**
     * Gets an instance of an <code>ICtxBroker</code> service.
     * 
     * @return  An instance of <code>ICtxBroker</code>
     * @throws Exception if no context broker 3p service is available
     */
    private org.personalsmartspace.cm.broker.api.pss3p.ICtxBroker get3pCtxBroker() throws Exception {
        org.personalsmartspace.cm.broker.api.pss3p.ICtxBroker ctxBroker3p = null;
        ctxBroker3p = (org.personalsmartspace.cm.broker.api.pss3p.ICtxBroker) ctxBroker3pTracker.getService();
        log.info("returning " + ctxBroker3p);
        if (null == ctxBroker3p) {
            throw new Exception("context broker 3p service not available");
        }
        return ctxBroker3p;
    }

    private IIdentityManagement getIdentityManagement() throws Exception {
        final IIdentityManagement identityMgmt = (IIdentityManagement) identityManagementTracker.getService();
        if (null == identityMgmt) {
            throw new Exception("identity management service not available");
        }
        return identityMgmt;
    }

    private IAccessControlDecisionMgr getAccessControlDecisionMgr() throws Exception {
        final IAccessControlDecisionMgr decisionMgr = (IAccessControlDecisionMgr) decisionManagerTracker.getService();
        if (null == decisionMgr) {
            throw new Exception("decision manager service not available");
        }
        return decisionMgr;
    }

    /**
     * Cleans context database.
     * 
     * @throws Exception
     */
    private void cleanDb() throws Exception {
        List<ICtxIdentifier> identifiers;
        identifiers = this.getPlatformCtxBroker().lookup(CtxModelType.ENTITY, REMOTE_ENTITY);
        Iterator<ICtxIdentifier> it = identifiers.iterator();
        while (it.hasNext()) {
            final ICtxIdentifier identifier = it.next();
            if (!identifier.getLocalServiceId().equals(getPlatformCtxBroker().getLocalBrokerId()) || !getIdentityManagement().isMyIdentifier(identifier)) {
                it.remove();
            }
        }
        getPlatformCtxBroker().remove(identifiers);
    }
}
