package net.sf.joafip.heapfile.record.entity;

import java.io.File;
import net.sf.joafip.AbstractDeleteFileTestCase;
import net.sf.joafip.TestConstant;
import net.sf.joafip.file.entity.AbstractFileStorable;
import net.sf.joafip.file.service.FileForStorable;
import net.sf.joafip.heapfile.service.HeapException;

public class TestMarshall extends AbstractDeleteFileTestCase {

    private class Marshallizable extends AbstractFileStorable {

        public Marshallizable() {
            super(0, 8 + 4 + 1 + 4);
        }

        public long longValue;

        public int intValue;

        public boolean booleanValue;

        @Override
        protected void marshallImpl() throws HeapException {
            writeLong(longValue);
            writeInteger(intValue);
            writeBoolean(booleanValue);
            writeInteger(crc32);
        }

        @Override
        protected void unmarshallImpl() throws HeapException {
            longValue = readLong();
            intValue = readInteger();
            booleanValue = readBoolean();
            readAndCheckCrc32();
        }
    }

    ;

    private Marshallizable fileStorableForWrite;

    private FileForStorable fileForStorable;

    protected void setUp() throws Exception {
        super.setUp();
        fileForStorable = new FileForStorable(TestConstant.RUNTIME_DIR + File.separator + "test.dat");
    }

    protected void tearDown() throws Exception {
        try {
            if (fileForStorable.isOpened()) {
                fileForStorable.close();
            }
        } catch (Throwable throwable) {
        }
        super.tearDown();
    }

    public void testInOut() throws HeapException {
        fileStorableForWrite = new Marshallizable();
        assertTrue("must be in state just created", fileStorableForWrite.isJustCreated());
        assertFalse("must be in state value not changed", fileStorableForWrite.isValueChanged());
        fileStorableForWrite.longValue = 0;
        fileStorableForWrite.intValue = 0;
        fileStorableForWrite.booleanValue = false;
        fileStorableForWrite.setValueIsChanged();
        assertFalse("must not be in state just created", fileStorableForWrite.isJustCreated());
        assertTrue("must be in state value changed", fileStorableForWrite.isValueChanged());
        check();
        fileStorableForWrite = new Marshallizable();
        fileStorableForWrite.longValue = 0xff;
        fileStorableForWrite.intValue = 0xff;
        fileStorableForWrite.booleanValue = true;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite = new Marshallizable();
        fileStorableForWrite.longValue = 0xff00;
        fileStorableForWrite.intValue = 0xff00;
        fileStorableForWrite.booleanValue = false;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite = new Marshallizable();
        fileStorableForWrite.longValue = 0xff0000;
        fileStorableForWrite.intValue = 0xff0000;
        fileStorableForWrite.booleanValue = true;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite = new Marshallizable();
        fileStorableForWrite.longValue = 0xff000000;
        fileStorableForWrite.intValue = 0xff000000;
        fileStorableForWrite.booleanValue = false;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite = new Marshallizable();
        fileStorableForWrite.longValue = 0xff00000000L;
        fileStorableForWrite.intValue = 0xff00ff00;
        fileStorableForWrite.booleanValue = true;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite.longValue = 0xff0000000000L;
        fileStorableForWrite.intValue = 0x00ff00ff;
        fileStorableForWrite.booleanValue = false;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite.longValue = 0xff000000000000L;
        fileStorableForWrite.intValue = 0xf0f0f0f0;
        fileStorableForWrite.booleanValue = false;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite.longValue = 0xff00000000000000L;
        fileStorableForWrite.intValue = 0x0f0f0f0f;
        fileStorableForWrite.booleanValue = true;
        fileStorableForWrite.setValueIsChanged();
        check();
        fileStorableForWrite.longValue = 0xff00ff00ff00ff00L;
        fileStorableForWrite.intValue = 0xffffffff;
        fileStorableForWrite.booleanValue = false;
        fileStorableForWrite.setValueIsChanged();
        check();
    }

    /**
	 * @throws HeapException
	 * @throws FileCorruptedException
	 * 
	 */
    private void check() throws HeapException {
        final Marshallizable fileStorableForRead;
        fileForStorable.open();
        fileStorableForWrite.writeToFile(fileForStorable);
        assertFalse("must not be in state value changed since just writed", fileStorableForWrite.isValueChanged());
        fileStorableForRead = new Marshallizable();
        fileStorableForRead.readFromFile(fileForStorable);
        assertFalse("must be in state just created", fileStorableForRead.isJustCreated());
        assertFalse("must not be in state value changed", fileStorableForRead.isValueChanged());
        fileForStorable.close();
        assertEquals("writed and readed long value must be equals", fileStorableForWrite.longValue, fileStorableForRead.longValue);
        assertEquals("writed and readed int value must be equals", fileStorableForWrite.intValue, fileStorableForRead.intValue);
        assertEquals("writed and readed boolean value must be equals", fileStorableForWrite.booleanValue, fileStorableForRead.booleanValue);
    }
}
