package com.volantis.mcs.interaction.sample;

import com.volantis.mcs.interaction.BeanProxy;
import com.volantis.mcs.interaction.ListProxy;
import com.volantis.mcs.interaction.Proxy;
import com.volantis.mcs.interaction.ReadWriteState;
import com.volantis.mcs.interaction.event.InteractionEventListenerAdapter;
import com.volantis.mcs.interaction.event.ReadOnlyStateChangedEvent;
import com.volantis.mcs.interaction.operation.Operation;
import com.volantis.mcs.interaction.sample.model.Contacts;
import com.volantis.mcs.interaction.sample.model.Person;
import com.volantis.mcs.interaction.sample.model.Address;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the read-write state functionality.
 */
public class ReadWriteTestCase extends FlintstoneTestAbstract {

    private ListProxy contactsProxy;

    private BeanProxy wilmaProxy;

    private BeanProxy fredProxy;

    /**
     * Set up a simple Flintstones model using the interaction layer.
     *
     * @throws Exception if an error occurs
     */
    protected void setUp() throws Exception {
        super.setUp();
        Contacts contacts = new Contacts();
        BeanProxy contactsBeanProxy = (BeanProxy) createProxy(contacts);
        contactsProxy = (ListProxy) contactsBeanProxy.getPropertyProxy(Contacts.CONTACTS);
        Person wilma = createWilmaFlintstone(createFlintStoneAddress());
        wilmaProxy = (BeanProxy) createProxy(wilma);
        contactsProxy.prepareAddProxyItemOperation(wilmaProxy).execute();
        Person fred = createFredFlintstone(createFlintStoneAddress());
        fredProxy = (BeanProxy) createProxy(fred);
        contactsProxy.prepareAddProxyItemOperation(fredProxy).execute();
    }

    /**
     * Tests that the default states for proxies are set correctly.
     */
    public void testDefaultStates() {
        assertEquals("Default state should be inherit", ReadWriteState.INHERIT, contactsProxy.getReadWriteState());
        assertFalse("Default inherited value should be read/write", contactsProxy.isReadOnly());
    }

    /**
     * Tests that a variety of modification types fail when the proxy is
     * read-only.
     */
    public void testOperationsFailWhenReadOnly() {
        contactsProxy.setReadWriteState(ReadWriteState.READ_ONLY);
        assertOperationFails("Add model item should fail when list is RO", contactsProxy.prepareAddModelItemOperation(new Person()));
        assertOperationFails("Add proxy item should fail when list is RO", contactsProxy.prepareAddProxyItemOperation(createProxy(new Person())));
        assertOperationFails("Create/add proxy item should fail when list is RO", contactsProxy.prepareCreateAndAddProxyItemOperation());
        assertOperationFails("Remove proxy item should fail when list is RO", contactsProxy.prepareRemoveProxyItemOperation(wilmaProxy));
        assertOperationFails("Set model should fail when list is RO", contactsProxy.prepareSetModelObjectOperation(new ArrayList()));
        wilmaProxy.setReadWriteState(ReadWriteState.READ_ONLY);
        assertOperationFails("Set model should fail when bean is RO", wilmaProxy.prepareSetModelObjectOperation(new Person()));
        Proxy opaqueProxy = wilmaProxy.getPropertyProxy(Person.FIRST_NAME);
        opaqueProxy.setReadWriteState(ReadWriteState.READ_ONLY);
        assertOperationFails("Set model should fail when property is RO", opaqueProxy.prepareSetModelObjectOperation("Pebbles"));
    }

    /**
     * Tests that read/write states are inherited correctly.
     */
    public void testReadWriteInheritance() {
        contactsProxy.setReadWriteState(ReadWriteState.READ_ONLY);
        assertTrue("Child should inherit read-only state from parent", wilmaProxy.isReadOnly());
        contactsProxy.setReadWriteState(ReadWriteState.READ_WRITE);
        assertFalse("Child should inherit read-write state from parent", wilmaProxy.isReadOnly());
    }

    /**
     * Tests that events are propagated when the state changes.
     */
    public void testReadOnlyEvents() {
        wilmaProxy.setReadWriteState(ReadWriteState.READ_ONLY);
        BeanProxy wilmaAddress = (BeanProxy) wilmaProxy.getPropertyProxy(Person.ADDRESS);
        wilmaProxy.getPropertyProxy(Person.FIRST_NAME).setReadWriteState(ReadWriteState.READ_WRITE);
        final List expectedSources = new ArrayList();
        expectedSources.add(wilmaProxy);
        expectedSources.add(wilmaProxy.getPropertyProxy(Person.AGE));
        expectedSources.add(wilmaProxy.getPropertyProxy(Person.LAST_NAME));
        expectedSources.add(wilmaAddress);
        ListProxy lines = (ListProxy) wilmaAddress.getPropertyProxy(Address.LINES);
        expectedSources.add(lines);
        for (int i = 0; i < lines.size(); i++) {
            expectedSources.add(lines.getItemProxy(i));
        }
        wilmaProxy.addListener(new InteractionEventListenerAdapter() {

            public void readOnlyStateChanged(ReadOnlyStateChangedEvent event) {
                assertFalse("New read-only value should be false", event.isReadOnly());
                assertTrue("Source of state change should be in expected list", expectedSources.contains(event.getProxy()));
                expectedSources.remove(event.getProxy());
            }
        }, true);
        wilmaProxy.setReadWriteState(ReadWriteState.READ_WRITE);
        assertTrue("All expected sources should have fired events", expectedSources.isEmpty());
    }

    /**
     * Assert that an operation will fail with an illegal state exception.
     *
     * @param message The message to display if the assertion is incorrect
     * @param operation The operation to execute
     */
    private void assertOperationFails(String message, Operation operation) {
        try {
            operation.execute();
            fail(message);
        } catch (IllegalStateException ise) {
        }
    }
}
