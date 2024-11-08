package net.sf.joafip.service.bug.primitive;

import net.sf.joafip.AbstractDeleteFileTestCase;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.StorableAccess;
import net.sf.joafip.TestException;
import net.sf.joafip.entity.EnumFilePersistenceCloseAction;
import net.sf.joafip.service.FilePersistenceBuilder;
import net.sf.joafip.service.FilePersistenceClassNotFoundException;
import net.sf.joafip.service.FilePersistenceDataCorruptedException;
import net.sf.joafip.service.FilePersistenceException;
import net.sf.joafip.service.FilePersistenceInvalidClassException;
import net.sf.joafip.service.FilePersistenceNotSerializableException;
import net.sf.joafip.service.FilePersistenceTooBigForSerializationException;
import net.sf.joafip.service.IDataAccessSession;
import net.sf.joafip.service.IFilePersistence;
import net.sf.joafip.store.service.proxy.ProxyException;
import net.sf.joafip.store.service.proxy.ProxyManager2;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
@StorableAccess
public class TestHoldPrimitive extends AbstractDeleteFileTestCase {

    private static final String MUST_NOT_BE_SAME = "must not be same";

    private static final String MUST_BE_A_PROXY = "must be a proxy";

    private static final String BAD_VALUE = "bad value";

    public TestHoldPrimitive() throws TestException {
        super();
    }

    public TestHoldPrimitive(final String name) throws TestException {
        super(name);
    }

    public void test() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, ProxyException, FilePersistenceTooBigForSerializationException {
        IFilePersistence filePersistence = createFilePersistence();
        IDataAccessSession dataAccessSession = filePersistence.createDataAccessSession();
        dataAccessSession.open();
        final Holder writeByteHolder = new Holder();
        final Holder writeShortHolder = new Holder();
        final Holder writeIntegerHolder = new Holder();
        final Holder writeLongHolder = new Holder();
        final Holder writeFloatHolder = new Holder();
        final Holder writeDoubleHolder = new Holder();
        final Holder writeBooleanHolder = new Holder();
        final Holder writeCharacterHolder = new Holder();
        writeByteHolder.setObject1(new Byte((byte) 0));
        writeByteHolder.setObject2(new Byte((byte) 0));
        writeShortHolder.setObject1(new Short((short) 0));
        writeShortHolder.setObject2(new Short((short) 0));
        writeIntegerHolder.setObject1(new Integer(0));
        writeIntegerHolder.setObject2(new Integer(0));
        writeLongHolder.setObject1(new Long((short) 0));
        writeLongHolder.setObject2(new Long((short) 0));
        writeFloatHolder.setObject1(new Float(0.0));
        writeFloatHolder.setObject2(new Float(0.0));
        writeDoubleHolder.setObject1(new Double(0.0));
        writeDoubleHolder.setObject2(new Double(0.0));
        writeBooleanHolder.setObject1(new Boolean(false));
        writeBooleanHolder.setObject2(new Boolean(false));
        writeCharacterHolder.setObject1(new Character('a'));
        writeCharacterHolder.setObject2(new Character('a'));
        dataAccessSession.setObject("byte", writeByteHolder);
        dataAccessSession.setObject("short", writeShortHolder);
        dataAccessSession.setObject("integer", writeIntegerHolder);
        dataAccessSession.setObject("long", writeLongHolder);
        dataAccessSession.setObject("float", writeFloatHolder);
        dataAccessSession.setObject("double", writeDoubleHolder);
        dataAccessSession.setObject("boolean", writeBooleanHolder);
        dataAccessSession.setObject("character", writeCharacterHolder);
        dataAccessSession.closeAndWait(EnumFilePersistenceCloseAction.SAVE);
        filePersistence.close();
        filePersistence = createFilePersistence();
        dataAccessSession = filePersistence.createDataAccessSession();
        dataAccessSession.open();
        final Holder readByteHolder = (Holder) dataAccessSession.getObject("byte");
        final Holder readShortHolder = (Holder) dataAccessSession.getObject("short");
        final Holder readIntegerHolder = (Holder) dataAccessSession.getObject("integer");
        final Holder readLongHolder = (Holder) dataAccessSession.getObject("long");
        final Holder readFloatHolder = (Holder) dataAccessSession.getObject("float");
        final Holder readDoubleHolder = (Holder) dataAccessSession.getObject("double");
        final Holder readBooleanHolder = (Holder) dataAccessSession.getObject("boolean");
        final Holder readCharacterHolder = (Holder) dataAccessSession.getObject("character");
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readByteHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeByteHolder, readByteHolder);
        Object object1 = readByteHolder.getObject1();
        assertEquals(BAD_VALUE, Byte.valueOf((byte) 0), object1);
        Object object2 = readByteHolder.getObject2();
        assertEquals(BAD_VALUE, Byte.valueOf((byte) 0), object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readShortHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeShortHolder, readShortHolder);
        object1 = readShortHolder.getObject1();
        assertEquals(BAD_VALUE, Short.valueOf((short) 0), object1);
        object2 = readShortHolder.getObject2();
        assertEquals(BAD_VALUE, Short.valueOf((short) 0), object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readIntegerHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeIntegerHolder, readIntegerHolder);
        object1 = readIntegerHolder.getObject1();
        assertEquals(BAD_VALUE, Integer.valueOf(0), object1);
        object2 = readIntegerHolder.getObject2();
        assertEquals(BAD_VALUE, Integer.valueOf(0), object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readLongHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeLongHolder, readLongHolder);
        object1 = readLongHolder.getObject1();
        assertEquals(BAD_VALUE, Long.valueOf(0), object1);
        object2 = readLongHolder.getObject2();
        assertEquals(BAD_VALUE, Long.valueOf(0), object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readFloatHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeFloatHolder, readFloatHolder);
        object1 = readFloatHolder.getObject1();
        assertEquals(BAD_VALUE, Float.valueOf((float) 0.0), object1);
        object2 = readFloatHolder.getObject2();
        assertEquals(BAD_VALUE, Float.valueOf((float) 0.0), object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readDoubleHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeDoubleHolder, readDoubleHolder);
        object1 = readDoubleHolder.getObject1();
        assertEquals(BAD_VALUE, Double.valueOf(0.0), object1);
        object2 = readDoubleHolder.getObject2();
        assertEquals(BAD_VALUE, Double.valueOf(0.0), object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readBooleanHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeBooleanHolder, readBooleanHolder);
        object1 = readBooleanHolder.getObject1();
        assertEquals(BAD_VALUE, Boolean.FALSE, object1);
        object2 = readBooleanHolder.getObject2();
        assertEquals(BAD_VALUE, Boolean.FALSE, object2);
        assertTrue(MUST_BE_A_PROXY, ProxyManager2.isProxyOrEnhanced(readCharacterHolder));
        assertNotSame(MUST_NOT_BE_SAME, writeCharacterHolder, readCharacterHolder);
        object1 = readCharacterHolder.getObject1();
        assertEquals(BAD_VALUE, Character.valueOf('a'), object1);
        object2 = readCharacterHolder.getObject2();
        assertEquals(BAD_VALUE, Character.valueOf('a'), object2);
        dataAccessSession.closeAndWait(EnumFilePersistenceCloseAction.SAVE);
        filePersistence.close();
    }

    private IFilePersistence createFilePersistence() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException {
        final boolean removeFiles = false;
        final int taillePageCacheDisque = 1024;
        final int nombreDePageDuCacheDisque = 100;
        final boolean crashSafeMode = false;
        final String xpath = path.getAbsolutePath();
        final FilePersistenceBuilder builder = new FilePersistenceBuilder();
        builder.setPathName(xpath);
        builder.setRemoveFiles(removeFiles);
        builder.setFileCache(taillePageCacheDisque, nombreDePageDuCacheDisque);
        builder.setGarbageManagement(false);
        builder.setCrashSafeMode(crashSafeMode);
        return builder.build();
    }
}