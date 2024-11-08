package fulmine.protocol.specification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.Random;
import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fulmine.Domain;
import fulmine.IDomain;
import fulmine.IType;
import fulmine.Type;
import fulmine.context.DefaultPermissionProfile;
import fulmine.context.FulmineContext;
import fulmine.context.IFrameworkContext;
import fulmine.model.component.AbstractComponent;
import fulmine.model.component.IComponent;
import fulmine.model.container.ContainerJUnitTest;
import fulmine.model.container.IContainer;
import fulmine.model.container.IContainerFactory;
import fulmine.model.container.impl.BasicContainer;
import fulmine.model.field.DoubleField;
import fulmine.model.field.IField;
import fulmine.model.field.IntegerField;
import fulmine.model.field.StringField;
import fulmine.model.field.containerdefinition.IContainerDefinitionField;
import fulmine.protocol.specification.FieldReader.IFieldReaderTask;
import fulmine.protocol.wire.IWireIdentity;
import fulmine.protocol.wire.IWireIdentityRegistry;
import fulmine.protocol.wire.SWFWireIdentityRegistry;
import fulmine.protocol.wire.WireIdentity;
import fulmine.protocol.wire.operation.BasicOperation;
import fulmine.protocol.wire.operation.IOperationScope;
import fulmine.util.collection.CollectionFactory;

/**
 * Test cases for the {@link FrameReader} and {@link FrameWriter}
 * 
 * @author Ramon Servadei
 */
@SuppressWarnings("all")
public class FrameReaderAndFrameWriterJUnitTest {

    private static final String FIELD3 = "field3";

    private static final String FIELD2 = "field2";

    private static final String FIELD1 = "field1";

    private static final IDomain DOMAIN = Domain.get(1);

    Collection<IComponent> components = CollectionFactory.newList();

    StringField field1;

    DoubleField field2;

    IntegerField field3;

    IContainer result;

    IFrameworkContext context = new FulmineContext(getClass().getSimpleName());

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        context.start();
        field1 = new StringField(FIELD1);
        field1.set("a");
        field2 = new DoubleField(FIELD2);
        field2.set(20d);
        field3 = new IntegerField(FIELD3);
        field3.set(12);
        components.add(field1);
        components.add(field2);
        components.add(field3);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        context.destroy();
    }

    /**
     * Test method for
     * {@link FrameWriter#write(fulmine.model.container.IContainer)} and
     * {@link fulmine.protocol.specification.FrameReader#read(byte[])}.
     * <p>
     * Only tests a static definition.
     * 
     * @throws InterruptedException
     */
    @Test
    public void testWriteThenRead() throws InterruptedException {
        Random rnd = new Random();
        final IType code = Type.get(120);
        context.getContainerFactory().registerBuilder(code, new JUnitContainerBuilder());
        IContainer expected = context.getContainerFactory().createContainer(context.getIdentity(), "junit-test-", code, DOMAIN, context, true);
        doWriteReadTest(rnd, expected, false, false);
    }

    /**
     * Test method for {@link FrameWriter#writeComplete(IContainer)}.
     * 
     * @throws InterruptedException
     */
    @Test
    public void testWriteComplete() throws InterruptedException {
        Random rnd = new Random();
        final IType code = Type.get(120);
        context.getContainerFactory().registerBuilder(code, new JUnitContainerBuilder());
        IContainer expected = context.getContainerFactory().createContainer(context.getIdentity(), "junit-test-", code, DOMAIN, context, true);
        doWriteReadTest(rnd, expected, false, false);
        doWriteReadTest(rnd, expected, true, true);
        doWriteReadTest(rnd, expected, true, false);
    }

    private void doWriteReadTest(Random rnd, IContainer expected, boolean completeState, boolean remove) throws InterruptedException {
        if (!completeState) {
            ((StringField) expected.get((FIELD1))).set("lasers-" + rnd.nextLong());
            ((DoubleField) expected.get((FIELD2))).set(rnd.nextDouble());
            ((IntegerField) expected.get((FIELD3))).set(rnd.nextInt());
        }
        if (remove) {
            assertTrue("remote reference not removed", context.removeContainer((IContainer) result));
            assertFalse("remote reference was removed again", context.removeContainer((IContainer) result));
        }
        final byte[] frame = completeState ? new FrameWriter().writeComplete(expected) : ContainerJUnitTest.getFrame(context, expected, false, true);
        result = null;
        Runnable task = new Runnable() {

            public void run() {
                result = new FrameReader().read(frame, "remoteContextIdentity", context);
            }
        };
        Thread reader = new Thread(task);
        reader.start();
        reader.join();
        assertNotSame("Did not write/read correctly", expected, result);
        assertEquals("Did not write/read correctly", expected, result);
    }

    /**
     * Test method for
     * {@link fulmine.protocol.specification.FrameReader#findHeaderAndDataBufferPositions(byte[], int, int[], int[], int[], int[])}
     * .
     */
    @Test
    public void testFindHeaderAndDataBufferPositions() {
        int start = 4;
        byte[][] frame = new byte[1][1024];
        int idSize = ByteWriter.writeString(FIELD1, frame, start + IFrameConstants.ID_SIZE_LENGTH);
        ByteWriter.writeIntegerAsBytes(idSize, frame, start, IFrameConstants.ID_SIZE_LENGTH);
        int dynamicSectionSize = idSize + IFrameConstants.ID_SIZE_LENGTH;
        int expectedHeaderStart = start + dynamicSectionSize + IFrameConstants.PREAMBLE_STATIC_SECTION_LENGTH;
        int expectedHeaderSize = 24;
        int expectedDataStart = expectedHeaderStart + expectedHeaderSize;
        int expectedDataSize = 345;
        ByteWriter.writeIntegerAsBytes(expectedHeaderSize, frame, start + dynamicSectionSize + IFrameConstants.HEADER_SIZE_START, IFrameConstants.HEADER_SIZE_LENGTH);
        ByteWriter.writeIntegerAsBytes(expectedDataSize, frame, start + dynamicSectionSize + IFrameConstants.DATA_SIZE_START, IFrameConstants.DATA_SIZE_LENGTH);
        int[] headerStart = new int[1];
        int[] headerLength = new int[1];
        int[] dataStart = new int[1];
        int[] dataLength = new int[1];
        FrameReader.findHeaderAndDataBufferPositions(frame[0], start, headerStart, headerLength, dataStart, dataLength);
        assertEquals("headerLength", expectedHeaderSize, headerLength[0]);
        assertEquals("dataLength", expectedDataSize, dataLength[0]);
        assertEquals("dataStart", expectedDataStart, dataStart[0]);
        assertEquals("headerStart", expectedHeaderStart, headerStart[0]);
    }

    /**
     * Test method for
     * {@link FrameWriter#writeNested(java.util.Collection, fulmine.protocol.wire.IWireIdentityRegistry, fulmine.protocol.wire.operation.IOperationScope)}
     * and
     * {@link FrameReader#readNestedSWF(IOperationScope, byte[], int, int, IFieldReaderTask)}
     * .
     */
    @Test
    public void testWriteThenReadNested() {
        IWireIdentityRegistry registry = new SWFWireIdentityRegistry();
        byte[] frame = FrameWriter.writeNested(components, registry, new BasicOperation(new DefaultPermissionProfile()), false);
        byte[] chaff = new byte[] { 21, 34, 45, 12 };
        frame = ArrayUtils.addAll(chaff, frame);
        int start = chaff.length;
        JUnitReader reader = new JUnitReader();
        FrameReader.readNestedSWF(new BasicOperation(new DefaultPermissionProfile()), frame, start, frame.length, reader);
        assertNotSame("Did not write/read correctly", this.components, reader.components);
        assertEquals("Did not write/read correctly", this.components, reader.components);
    }

    /**
     * JUnit version
     * 
     * @author Ramon Servadei
     * 
     */
    private class JUnitReader implements IFieldReaderTask {

        Collection<IComponent> components = CollectionFactory.newList();

        public void read(IOperationScope scope, int fieldId, byte[] dataBuffer, int dataStart, int dataLen) {
            throw new IllegalStateException("Not SWF compatible!!!");
        }

        public void read(IOperationScope scope, String fieldId, byte[] dataBuffer, int dataStart, int dataLen) {
            if (FIELD1.equals(fieldId)) {
                StringField field = new StringField(fieldId);
                field.set(ByteReader.readString(dataBuffer, dataStart, dataLen));
                this.components.add(field);
            } else {
                if (FIELD2.equals(fieldId)) {
                    DoubleField field = new DoubleField(fieldId);
                    field.set(ByteReader.readDouble(dataBuffer, dataStart, dataLen));
                    this.components.add(field);
                } else {
                    if (FIELD3.equals(fieldId)) {
                        IntegerField field = new IntegerField(fieldId);
                        field.set(ByteReader.readInteger(dataBuffer, dataStart, dataLen));
                        this.components.add(field);
                    } else {
                        throw new IllegalArgumentException("What is this component doing here? " + fieldId);
                    }
                }
            }
        }
    }

    /**
     * JUnit version
     * 
     * @author Ramon Servadei
     * 
     */
    private class JUnitContainerBuilder implements IContainerFactory.IContainerBuilder {

        public IContainer createContainer(String nativeContextIdentity, String identity, IType type, IDomain domain, IFrameworkContext runtime, boolean local) {
            return new BasicContainer(nativeContextIdentity, identity, type, DOMAIN, runtime, local);
        }

        public IContainerDefinitionField createContainerDefinition() {
            return new JUnitContainerDefinition();
        }
    }

    /**
     * JUnit version
     * 
     * @author Ramon Servadei
     * 
     */
    private class JUnitContainerDefinition extends AbstractComponent implements IContainerDefinitionField {

        protected JUnitContainerDefinition() {
            super("Junit-definition", Type.CONTAINER_DEFINITION, DOMAIN);
        }

        public String getIdentityForWireCode(int wireCode) {
            switch(wireCode) {
                case 1:
                    return (FIELD1);
                case 2:
                    return (FIELD2);
                case 3:
                    return (FIELD3);
                default:
                    throw new IllegalArgumentException("What is this code doing here? " + wireCode);
            }
        }

        public int getWireCodeForIdentity(String identity) {
            if ((FIELD1).equals(identity)) {
                return 1;
            }
            if ((FIELD2).equals(identity)) {
                return 2;
            }
            if ((FIELD3).equals(identity)) {
                return 3;
            }
            throw new IllegalArgumentException("What is this component? " + identity);
        }

        public void populate(IContainer container) {
            container.add(new StringField(FIELD1));
            container.add(new DoubleField(FIELD2));
            container.add(new IntegerField(FIELD3));
        }

        public String getIdentityFor(IWireIdentity wireId) {
            return getIdentityForWireCode(wireId.getAsInteger());
        }

        public IWireIdentity getWireIdentityFor(String identity) {
            return WireIdentity.get(getWireCodeForIdentity(identity));
        }

        public void add(IComponent component) {
        }

        public void remove(IComponent component) {
        }

        public boolean isDynamic() {
            return false;
        }

        @Override
        protected boolean doReadState(IOperationScope scope, byte[] buffer, int start, int numberOfBytes) throws Exception {
            return false;
        }

        @Override
        protected boolean doWriteState(IOperationScope scope, IWireIdentity wireId, byte[][] headerBuffer, int[] headerBufferPosition, byte[][] dataBuffer, int[] dataBufferPosition, boolean completeState) throws Exception {
            return false;
        }

        public void add(IField field) {
        }

        public void remove(IField field) {
        }

        public void addedToContainer(IContainer container) {
        }

        public IContainer getContainer() {
            return null;
        }

        public void removedFromContainer(IContainer container) {
        }

        public void resetChanges() {
        }

        public String getValueAsString() {
            return null;
        }

        public boolean containsDefinition(int wireCode) {
            return false;
        }

        public IField createField(int wireCode) {
            return null;
        }

        public Object getValue() {
            return null;
        }

        public byte getApplication(int wireCode) {
            return 0;
        }

        public short getPermission(int wireCode) {
            return 0;
        }

        public byte getApplication() {
            return 0;
        }

        public short getPermission() {
            return 0;
        }

        public boolean setValueFromString(String value) {
            return false;
        }
    }
}
