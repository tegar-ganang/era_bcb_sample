package net.sf.joafip.service.bug;

import net.sf.joafip.entity.EnumFilePersistenceCloseAction;
import net.sf.joafip.service.FilePersistenceException;

/**
 * test Long I/O in file
 * 
 * @author luc peuvrier
 * 
 */
public class TestLongIO extends AbstractFilePersistenceForTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test1() throws FilePersistenceException {
        session.open();
        final Long value = Long.valueOf("115465874564584225");
        session.setObject("key", value);
        session.close(EnumFilePersistenceCloseAction.SAVE);
        session.open();
        final Long valueReaded = (Long) session.getObject("key");
        assertEquals("readed must be equals to writed", value, valueReaded);
        session.close(EnumFilePersistenceCloseAction.DO_NOT_SAVE);
    }
}
