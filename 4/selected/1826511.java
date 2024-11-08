package fulmine.model.field.containerdefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fulmine.Domain;
import fulmine.IType;
import fulmine.Type;
import fulmine.context.DefaultPermissionProfile;
import fulmine.context.FulmineContext;
import fulmine.context.IFrameworkContext;
import fulmine.event.IEvent;
import fulmine.model.container.AbstractDynamicContainer;
import fulmine.model.container.IContainer;
import fulmine.model.container.impl.RecordJUnitTest;
import fulmine.model.field.DoubleField;
import fulmine.model.field.IntegerField;
import fulmine.model.field.StringField;
import fulmine.protocol.wire.WireIdentity;
import fulmine.protocol.wire.operation.BasicOperation;

/**
 * Test cases for the {@link ContainerDefinitionField}
 * 
 * @author Ramon Servadei
 */
@SuppressWarnings("all")
public class ContainerDefinitionJUnitTest {

    String NAME = "ContainerDefinitionJUnitTest-" + System.nanoTime();

    ContainerDefinitionField candidate;

    StringField field1;

    DoubleField field2;

    IntegerField field3;

    String FIELD_ID1 = "field1";

    String FIELD_ID2 = "field2";

    String FIELD_ID3 = "field3";

    Mockery mocks = new JUnit4Mockery();

    IFrameworkContext context = new FulmineContext(getClass().getSimpleName());

    final Level level = Logger.getLogger(RecordJUnitTest.FULMINE_CONTEXT_EVENT_MANAGER).getLevel();

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        context.start();
        Logger.getLogger(FulmineContext.class).setLevel(Level.WARN);
        field1 = new StringField(FIELD_ID1);
        field2 = new DoubleField(FIELD_ID2);
        field3 = new IntegerField(FIELD_ID3);
        candidate = new ContainerDefinitionField(new JUnitDynamicContainer(context.getIdentity(), NAME, Type.get(Type.BASE_USER_START), context));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        context.destroy();
        Logger.getLogger(RecordJUnitTest.FULMINE_CONTEXT_EVENT_MANAGER).setLevel(level);
    }

    @Test
    public void testWireCodeStart() {
        assertEquals("Wire code MUST start from...", Type.CONTAINER_DEFINITION.value() + 1, candidate.wireCodeCounter);
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#ContainerDefinition(String, fulmine.model.component.IComponent...)}
     * .
     */
    @Test
    public void testStaticContainerDefinitionCtor() {
        candidate = new ContainerDefinitionField("sdf1", field1, field2);
        assertNull("Should be a noop container for static definitions", candidate.getContainer());
        assertFalse("isDynamic", candidate.isDynamic());
        try {
            candidate.add(field3);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
        try {
            candidate.remove(field2);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#add(fulmine.model.component.IComponent)}
     * .
     */
    @Test
    public void testAddSelf() {
        candidate.add(candidate);
        final String[] componentIdentities = candidate.getComponentIdentities();
        final List<String> ids = Arrays.asList(componentIdentities);
        assertFalse("Should not add self!", ids.contains(candidate.getIdentity()));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#add(fulmine.model.field.IField)}
     * .
     */
    @Test
    public void testAdd() {
        int wireCounter = candidate.wireCodeCounter;
        candidate.add(field1);
        final DescriptorField desc = new DescriptorField(field1.getIdentity());
        desc.setWireCode(wireCounter);
        desc.setDataType(field1.getType().value());
        assertEquals("wire counter not incremented", wireCounter + 1, candidate.wireCodeCounter);
        assertEquals("component not added", desc, candidate.getFields().get((FIELD_ID1)));
        assertEquals("component not added against wire code", desc, candidate.getDescriptorFields().get(wireCounter));
        assertTrue("description not added to changes", candidate.getChanges().contains(desc));
        assertTrue("definition did not signal a change event", ((JUnitDynamicContainer) candidate.getContainer()).getEvents().contains(candidate));
    }

    /**
     * Test method for
     * {@link fulmine.model.container.definition.ContainerDefinition#remove(fulmine.model.field.IField)
     * )}.
     */
    @Test
    public void testRemove() {
        int wireCounter = candidate.wireCodeCounter;
        final DescriptorField desc = new DescriptorField(field1.getIdentity());
        desc.setWireCode(wireCounter);
        desc.setDataType(field1.getType().value());
        candidate.getFields().put(field1.getIdentity(), desc);
        candidate.getDescriptorFields().put(wireCounter, desc);
        candidate.remove(field1);
        desc.setDataType(ContainerDefinitionField.REMOVE_FIELD);
        assertEquals("wire counter incremented", wireCounter, candidate.wireCodeCounter);
        assertEquals("component not updated to signal remove", desc, candidate.getFields().get((FIELD_ID1)));
        assertEquals("component removed too early from wire code", desc, candidate.getDescriptorFields().get(wireCounter));
        assertTrue("description not added to changes", candidate.getChanges().contains(desc));
        assertTrue("definition did not signal a change event", ((JUnitDynamicContainer) candidate.getContainer()).getEvents().contains(candidate));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getSource()}
     * .
     */
    @Test
    public void testGetSource() {
        assertEquals("definition should be the event source", candidate, candidate.getSource());
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getIdentityFor(fulmine.protocol.wire.IWireIdentity)}
     * .
     */
    @Test
    public void testGetIdentityForIWF() {
        final WireIdentity wireIdentity = WireIdentity.get(candidate.wireCodeCounter);
        candidate.add(field1);
        assertEquals(field1.getIdentity(), candidate.getIdentityFor(wireIdentity));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getIdentityFor(fulmine.protocol.wire.IWireIdentity)}
     * .
     */
    @Test
    public void testGetIdentityForSWF() {
        final WireIdentity wireIdentity = WireIdentity.get("invalid");
        candidate.add(field1);
        try {
            candidate.getIdentityFor(wireIdentity);
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getWireIdentityFor(fulmine.model.key.String)}
     * .
     */
    @Test
    public void testGetWireIdentityFor() {
        final WireIdentity wireIdentity = WireIdentity.get(candidate.wireCodeCounter);
        candidate.add(field1);
        assertEquals(wireIdentity, candidate.getWireIdentityFor(field1.getIdentity()));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getIdentityForWireCode(int)}
     * .
     */
    @Test
    public void testGetIdentityForWireCode() {
        final WireIdentity wireIdentity = WireIdentity.get(candidate.wireCodeCounter);
        candidate.add(field1);
        assertEquals(field1.getIdentity(), candidate.getIdentityForWireCode(wireIdentity.getAsInteger()));
    }

    @Test
    public void testGetPermission() {
        final WireIdentity wireIdentity = WireIdentity.get(candidate.wireCodeCounter);
        candidate.add(field1);
        assertEquals(field1.getPermission(), candidate.getPermission(wireIdentity.getAsInteger()));
    }

    @Test
    public void testGetApplication() {
        final WireIdentity wireIdentity = WireIdentity.get(candidate.wireCodeCounter);
        candidate.add(field1);
        assertEquals(field1.getApplication(), candidate.getApplication(wireIdentity.getAsInteger()));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getIdentityForWireCode(int)}
     * .
     */
    @Test
    public void testGetIdentityForWireCodeNotExisting() {
        candidate.add(field1);
        try {
            candidate.getIdentityForWireCode(-987);
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getWireCodeForIdentity(fulmine.model.key.String)}
     * .
     */
    @Test
    public void testGetWireCodeForIdentityForSelf() {
        candidate.add(field1);
        assertEquals((int) Type.CONTAINER_DEFINITION.value(), candidate.getWireCodeForIdentity(candidate.getIdentity()));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getWireCodeForIdentity(fulmine.model.key.String)}
     * .
     */
    @Test
    public void testGetWireCodeForIdentity() {
        final WireIdentity wireIdentity = WireIdentity.get(candidate.wireCodeCounter);
        candidate.add(field1);
        assertEquals(wireIdentity.getAsInteger(), candidate.getWireCodeForIdentity(field1.getIdentity()));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getWireCodeForIdentity(fulmine.model.key.String)}
     * .
     */
    @Test
    public void testGetWireCodeForIdentityNotExisting() {
        try {
            candidate.getWireCodeForIdentity(field1.getIdentity());
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#populate(fulmine.model.container.IContainer)}
     * .
     */
    @Test
    public void testPopulate() {
        final IContainer container = mocks.mock(IContainer.class);
        candidate.add(field1);
        mocks.checking(new Expectations() {

            {
                one(container).add(with(equal(field1)));
            }
        });
        candidate.populate(container);
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getType()}
     * .
     */
    @Test
    public void testGetType() {
        assertEquals(Type.CONTAINER_DEFINITION, candidate.getType());
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#getComponentIdentities()}
     * .
     */
    @Test
    public void testGetComponentIdentities() {
        candidate.add(field1);
        final String[] componentIdentities = candidate.getComponentIdentities();
        final List<String> ids = Arrays.asList(componentIdentities);
        assertTrue("Did not find component identity", ids.contains(field1.getIdentity()));
    }

    /**
     * Test method for
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#doWriteState(fulmine.model.component.operation.IOperationScope, fulmine.protocol.wire.IWireIdentity, byte[][], int[], byte[][], int[], boolean)}
     * and
     * {@link fulmine.model.field.containerdefinition.ContainerDefinitionField#doReadState(fulmine.model.component.operation.IOperationScope, byte[], int, int)}
     * .
     */
    @Test
    public void testWriteStateReadState() {
        candidate.add(field1);
        candidate.add(field2);
        candidate.add(field3);
        JUnitDynamicContainer other = new JUnitDynamicContainer(context.getIdentity(), NAME, Type.get(Type.BASE_USER_START), context);
        ContainerDefinitionField result = (ContainerDefinitionField) other.get(other.getComponentIdentities()[0]);
        doWriteReadTest(result);
        candidate.resetChanges();
        candidate.remove(field2);
        assertTrue("Field 2 not found", result.getFields().containsKey(field2.getIdentity()));
        doWriteReadTest(result);
        assertFalse("Field 2 found: " + result, result.getFields().containsKey(field2.getIdentity()));
    }

    private void doWriteReadTest(ContainerDefinitionField result) {
        byte[][] headerBuffer = new byte[1][1024];
        byte[][] dataBuffer = new byte[1][1024];
        int[] headerBufferPosition = new int[1];
        int[] dataBufferPosition = new int[1];
        headerBufferPosition[0] = 0;
        dataBufferPosition[0] = 0;
        candidate.writeState(new BasicOperation(new DefaultPermissionProfile()), WireIdentity.get("a"), headerBuffer, headerBufferPosition, dataBuffer, dataBufferPosition, false);
        result.readState(new BasicOperation(new DefaultPermissionProfile()), dataBuffer[0], 0, dataBufferPosition[0]);
        assertNotSame("Failed to write/read container definition", candidate, result);
        assertEquals("Failed to write/read container definition", candidate, result);
    }

    /**
     * JUnit version
     * 
     * @author Ramon Servadei
     * 
     */
    private class JUnitDynamicContainer extends AbstractDynamicContainer {

        public JUnitDynamicContainer(String nativeContextIdentity, String identity, IType type, IFrameworkContext hostContext) {
            super(hostContext.getIdentity(), identity, type, Domain.get(2), hostContext, true);
        }

        Collection<IEvent> getEvents() {
            return events;
        }
    }
}
