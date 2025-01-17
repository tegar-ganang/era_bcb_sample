package org.jumpmind.symmetric.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.csv.CsvConstants;
import org.jumpmind.symmetric.ext.NodeGroupTestDataLoaderFilter;
import org.jumpmind.symmetric.ext.TestDataLoaderFilter;
import org.jumpmind.symmetric.load.AbstractDataLoaderTest;
import org.jumpmind.symmetric.load.csv.CsvLoader;
import org.jumpmind.symmetric.model.IncomingBatch;
import org.jumpmind.symmetric.model.IncomingBatchHistory;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.test.TestConstants;
import org.jumpmind.symmetric.transport.internal.InternalIncomingTransport;
import org.junit.Test;
import com.csvreader.CsvWriter;

public class DataLoaderServiceTest extends AbstractDataLoaderTest {

    protected Node client = new Node(TestConstants.TEST_CLIENT_EXTERNAL_ID, null, null);

    ;

    public DataLoaderServiceTest() throws Exception {
        super();
    }

    public DataLoaderServiceTest(String dbType) {
        super(dbType);
    }

    protected Level setLoggingLevelForTest(Level level) {
        Level old = Logger.getLogger(DataLoaderService.class).getLevel();
        Logger.getLogger(DataLoaderService.class).setLevel(level);
        Logger.getLogger(CsvLoader.class).setLevel(level);
        return old;
    }

    @Test
    public void testIncomingBatch() throws Exception {
        String[] insertValues = new String[TEST_COLUMNS.length];
        insertValues[2] = insertValues[4] = "incoming test";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.CHANNEL, "test_channel" });
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        insertValues[0] = getNextId();
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);
        IncomingBatch batch = getIncomingBatchService().findIncomingBatch(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(batch.getStatus(), IncomingBatch.Status.OK, "Wrong status. " + printDatabase());
        assertEquals(batch.getChannelId(), "test_channel", "Wrong channel. " + printDatabase());
    }

    @Test
    public void testStatistics() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] updateValues = new String[TEST_COLUMNS.length + 1];
        updateValues[0] = updateValues[updateValues.length - 1] = getNextId();
        updateValues[2] = updateValues[4] = "required string";
        String[] insertValues = (String[]) ArrayUtils.subarray(updateValues, 0, updateValues.length - 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writer.write(CsvConstants.UPDATE);
        writer.writeRecord(updateValues, true);
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        insertValues[0] = getNextId();
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        writer.writeRecord(new String[] { CsvConstants.DELETE, getNextId() }, true);
        writer.writeRecord(new String[] { CsvConstants.DELETE, getNextId() }, true);
        writer.writeRecord(new String[] { CsvConstants.DELETE, getNextId() }, true);
        insertValues[0] = getNextId();
        insertValues[5] = "i am an invalid date";
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 1, "Wrong number of history. " + printDatabase());
        IncomingBatchHistory history = list.get(0);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status. " + printDatabase());
        assertNotNull(history.getStartTime(), "Start time cannot be null. " + printDatabase());
        assertNotNull(history.getEndTime(), "End time cannot be null. " + printDatabase());
        assertEquals(history.getFailedRowNumber(), 8l, "Wrong failed row number. " + printDatabase());
        assertEquals(history.getByteCount(), 322l, "Wrong byte count. " + printDatabase());
        assertEquals(history.getStatementCount(), 8l, "Wrong statement count. " + printDatabase());
        assertEquals(history.getFallbackInsertCount(), 1l, "Wrong fallback insert count. " + printDatabase());
        assertEquals(history.getFallbackUpdateCount(), 2l, "Wrong fallback update count. " + printDatabase());
        assertEquals(history.getMissingDeleteCount(), 3l, "Wrong missing delete count. " + printDatabase());
        setLoggingLevelForTest(old);
    }

    @Test
    public void testUpdateCollision() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] insertValues = new String[TEST_COLUMNS.length];
        insertValues[0] = getNextId();
        insertValues[2] = insertValues[4] = "inserted row for testUpdateCollision";
        String[] updateValues = new String[TEST_COLUMNS.length];
        updateValues[0] = getId();
        updateValues[TEST_COLUMNS.length - 1] = getNextId();
        updateValues[2] = updateValues[4] = "update will become an insert that violates PK";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        writer.write(CsvConstants.UPDATE);
        writer.writeRecord(updateValues, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 1, "Wrong number of history");
        load(out);
        IncomingBatchHistory history = list.get(0);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 2, "Wrong number of history");
        history = list.get(0);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        history = list.get(1);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        setLoggingLevelForTest(old);
    }

    @Test
    public void testSqlStatistics() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] insertValues = new String[TEST_COLUMNS.length];
        insertValues[2] = insertValues[4] = "sql stat test";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        String firstId = getNextId();
        insertValues[0] = firstId;
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        String secondId = getNextId();
        insertValues[0] = secondId;
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        String thirdId = getNextId();
        insertValues[0] = thirdId;
        insertValues[2] = "This is a very long string that will fail upon insert into the database.";
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(insertValues, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 1, "Wrong number of history");
        IncomingBatchHistory history = list.get(0);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status. " + printDatabase());
        assertNotNull(history.getStartTime(), "Start time cannot be null. " + printDatabase());
        assertNotNull(history.getEndTime(), "End time cannot be null. " + printDatabase());
        assertEquals(history.getFailedRowNumber(), 3l, "Wrong failed row number. " + printDatabase());
        assertEquals(history.getByteCount(), 390l, "Wrong byte count. " + printDatabase());
        assertEquals(history.getStatementCount(), 3l, "Wrong statement count. " + printDatabase());
        assertEquals(history.getFallbackInsertCount(), 0l, "Wrong fallback insert count. " + printDatabase());
        assertEquals(history.getFallbackUpdateCount(), 1l, "Wrong fallback update count. " + printDatabase());
        assertEquals(history.getMissingDeleteCount(), 0l, "Wrong missing delete count. " + printDatabase());
        assertNotNull(history.getSqlState(), "Sql state should not be null. " + printDatabase());
        assertNotNull(history.getSqlMessage(), "Sql message should not be null. " + printDatabase());
        setLoggingLevelForTest(old);
    }

    @Test
    public void testSkippingResentBatch() throws Exception {
        String[] values = { getNextId(), "resend string", "resend string not null", "resend char", "resend char not null", "2007-01-25 00:00:00.0", "2007-01-25 01:01:01.0", "0", "7", "10.10", "0.474" };
        getNextBatchId();
        for (int i = 0; i < 7; i++) {
            batchId--;
            testSimple(CsvConstants.INSERT, values, values);
            IncomingBatchHistory.Status expectedStatus = IncomingBatchHistory.Status.OK;
            long expectedCount = 1;
            if (i > 0) {
                expectedStatus = IncomingBatchHistory.Status.SK;
                expectedCount = 0;
            }
            assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.OK, "Wrong status");
            List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
            assertEquals(list.size(), i + 1, "Wrong number of history");
            IncomingBatchHistory history = list.get(i);
            assertEquals(history.getStatus(), expectedStatus, "Wrong status");
            assertEquals(history.getFailedRowNumber(), 0l, "Wrong failed row number");
            assertEquals(history.getStatementCount(), expectedCount, "Wrong statement count");
            assertEquals(history.getFallbackInsertCount(), 0l, "Wrong fallback insert count");
            assertEquals(history.getFallbackUpdateCount(), 0l, "Wrong fallback update count");
            Thread.sleep(10);
        }
    }

    @Test
    public void testErrorWhileSkip() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "string2", "string not null2", "char2", "char not null2", "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89", "0.474" };
        testSimple(CsvConstants.INSERT, values, values);
        assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.OK, "Wrong status");
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 1, "Wrong number of history");
        IncomingBatchHistory history = list.get(0);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.OK, "Wrong status");
        assertEquals(history.getFailedRowNumber(), 0l, "Wrong failed row number");
        assertEquals(history.getStatementCount(), 1l, "Wrong statement count");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writer.writeRecord(new String[] { CsvConstants.BATCH, getBatchId() });
        writer.write(CsvConstants.KEYS);
        writer.writeRecord(TEST_KEYS);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, getBatchId() });
        writer.close();
        Thread.sleep(10);
        load(out);
        assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.OK, "Wrong status");
        list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 2, "Wrong number of history");
        history = list.get(1);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        assertEquals(history.getFailedRowNumber(), 0l, "Wrong failed row number");
        assertEquals(history.getStatementCount(), 0l, "Wrong statement count");
        setLoggingLevelForTest(old);
    }

    @Test
    public void testDataIntregrityError() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "string3", "string not null3", "char3", "char not null3", "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89", "0.474" };
        IParameterService paramService = (IParameterService) find(Constants.PARAMETER_SERVICE);
        paramService.saveParameter("dataloader.enable.fallback.update", "false");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writer.writeRecord(new String[] { CsvConstants.BATCH, getNextBatchId() });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values, true);
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, getBatchId() });
        writer.close();
        load(out);
        assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.ER, "Wrong status");
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 1, "Wrong number of history");
        IncomingBatchHistory history = list.get(0);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        assertEquals(history.getFailedRowNumber(), 2l, "Wrong failed row number");
        assertEquals(history.getStatementCount(), 2l, "Wrong statement count");
        load(out);
        assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.ER, "Wrong status");
        list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 2, "Wrong number of history");
        history = list.get(1);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status");
        assertEquals(history.getFailedRowNumber(), 2l, "Wrong failed row number");
        assertEquals(history.getStatementCount(), 2l, "Wrong statement count");
        paramService.saveParameter("dataloader.enable.fallback.update", "true");
        setLoggingLevelForTest(old);
    }

    @Test
    public void testErrorWhileParsing() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "should not reach database", "string not null", "char", "char not null", "2007-01-02", "2007-02-03 04:05:06.0", "0", "47", "67.89", "0.474" };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writer.write("UnknownTokenOutsideBatch");
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writer.writeRecord(new String[] { CsvConstants.TABLE, TEST_TABLE });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        writer.close();
        load(out);
        assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), null, "Wrong status");
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), 0, "Wrong number of history");
        setLoggingLevelForTest(old);
    }

    @Test
    public void testErrorThenSuccessBatch() throws Exception {
        Logger.getLogger(DataLoaderServiceTest.class).warn("testErrorThenSuccessBatch");
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "This string is too large and will cause the statement to fail", "string not null2", "char2", "char not null2", "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89", "0.474" };
        getNextBatchId();
        int retries = 3;
        for (int i = 0; i < retries; i++) {
            batchId--;
            testSimple(CsvConstants.INSERT, values, null);
            assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.ER, "Wrong status");
            List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
            assertEquals(list.size(), i + 1, "Wrong number of history. " + printDatabase());
            IncomingBatchHistory history = list.get(i);
            assertEquals(history.getStatus(), IncomingBatchHistory.Status.ER, "Wrong status. " + printDatabase());
            assertEquals(history.getFailedRowNumber(), 1l, "Wrong failed row number. " + printDatabase());
            assertEquals(history.getStatementCount(), 1l, "Wrong statement count. " + printDatabase());
            Thread.sleep(10);
        }
        batchId--;
        values[1] = "A smaller string that will succeed";
        testSimple(CsvConstants.INSERT, values, values);
        assertEquals(findIncomingBatchStatus(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.OK, "Wrong status. " + printDatabase());
        List<IncomingBatchHistory> list = getIncomingBatchService().findIncomingBatchHistory(batchId, TestConstants.TEST_CLIENT_EXTERNAL_ID);
        assertEquals(list.size(), retries + 1, "Wrong number of history. " + printDatabase());
        IncomingBatchHistory history = list.get(retries);
        assertEquals(history.getStatus(), IncomingBatchHistory.Status.OK, "Wrong status. " + printDatabase());
        assertEquals(history.getFailedRowNumber(), 0l, "Wrong failed row number. " + printDatabase());
        assertEquals(history.getStatementCount(), 1l, "Wrong statement count. " + printDatabase());
        setLoggingLevelForTest(old);
    }

    @Test
    public void testMultipleBatch() throws Exception {
        Level old = setLoggingLevelForTest(Level.OFF);
        String[] values = { getNextId(), "string", "string not null2", "char2", "char not null2", "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89", "0.474" };
        String[] values2 = { getNextId(), "This string is too large and will cause the statement to fail", "string not null2", "char2", "char not null2", "2007-01-02 00:00:00.0", "2007-02-03 04:05:06.0", "0", "47", "67.89", "0.474" };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = getWriter(out);
        writer.writeRecord(new String[] { CsvConstants.NODEID, TestConstants.TEST_CLIENT_EXTERNAL_ID });
        writeTable(writer, TEST_TABLE, TEST_KEYS, TEST_COLUMNS);
        String nextBatchId = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId });
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId });
        String nextBatchId2 = getNextBatchId();
        writer.writeRecord(new String[] { CsvConstants.BATCH, nextBatchId2 });
        writer.write(CsvConstants.INSERT);
        writer.writeRecord(values2, true);
        writer.writeRecord(new String[] { CsvConstants.COMMIT, nextBatchId2 });
        writer.close();
        load(out);
        assertTestTableEquals(values[0], values);
        assertTestTableEquals(values2[0], null);
        assertEquals(findIncomingBatchStatus(Integer.parseInt(nextBatchId), TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.OK, "Wrong status. " + printDatabase());
        assertEquals(findIncomingBatchStatus(Integer.parseInt(nextBatchId2), TestConstants.TEST_CLIENT_EXTERNAL_ID), IncomingBatch.Status.ER, "Wrong status. " + printDatabase());
        setLoggingLevelForTest(old);
    }

    protected void load(ByteArrayOutputStream out) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        getTransportManager().setIncomingTransport(new InternalIncomingTransport(in));
        getDataLoaderService().loadData(client, null);
    }

    protected IncomingBatch.Status findIncomingBatchStatus(int batchId, String nodeId) {
        IncomingBatch batch = getIncomingBatchService().findIncomingBatch(batchId, nodeId);
        IncomingBatch.Status status = null;
        if (batch != null) {
            status = batch.getStatus();
        }
        return status;
    }

    @Test
    public void testAutoRegisteredExtensionPoint() {
        TestDataLoaderFilter registeredFilter = (TestDataLoaderFilter) find("registeredDataFilter");
        TestDataLoaderFilter unRegisteredFilter = (TestDataLoaderFilter) find("unRegisteredDataFilter");
        assertTrue(registeredFilter.getNumberOfTimesCalled() > 0);
        assertTrue(unRegisteredFilter.getNumberOfTimesCalled() == 0);
        NodeGroupTestDataLoaderFilter registeredNodeGroupFilter = (NodeGroupTestDataLoaderFilter) find("registeredNodeGroupTestDataFilter");
        NodeGroupTestDataLoaderFilter unRegisteredNodeGroupFilter = (NodeGroupTestDataLoaderFilter) find("unRegisteredNodeGroupTestDataFilter");
        assertTrue(registeredNodeGroupFilter.getNumberOfTimesCalled() > 0);
        assertTrue(unRegisteredNodeGroupFilter.getNumberOfTimesCalled() == 0);
    }
}
