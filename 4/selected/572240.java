package org.jscsi.scsi.lu;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jscsi.scsi.protocol.Command;
import org.jscsi.scsi.protocol.cdb.CDB;
import org.jscsi.scsi.protocol.cdb.Read10;
import org.jscsi.scsi.protocol.cdb.Read12;
import org.jscsi.scsi.protocol.cdb.Read16;
import org.jscsi.scsi.protocol.cdb.Read6;
import org.jscsi.scsi.protocol.cdb.Write10;
import org.jscsi.scsi.protocol.cdb.Write12;
import org.jscsi.scsi.protocol.cdb.Write16;
import org.jscsi.scsi.protocol.cdb.Write6;
import org.jscsi.scsi.protocol.inquiry.InquiryDataRegistry;
import org.jscsi.scsi.protocol.inquiry.StaticInquiryDataRegistry;
import org.jscsi.scsi.protocol.mode.ModePageRegistry;
import org.jscsi.scsi.protocol.mode.StaticModePageRegistry;
import org.jscsi.scsi.tasks.TaskAttribute;
import org.jscsi.scsi.tasks.TaskFactory;
import org.jscsi.scsi.tasks.buffered.BufferedTaskFactory;
import org.jscsi.scsi.tasks.management.DefaultTaskManager;
import org.jscsi.scsi.tasks.management.DefaultTaskSet;
import org.jscsi.scsi.tasks.management.TaskManager;
import org.jscsi.scsi.tasks.management.TaskSet;
import org.jscsi.scsi.transport.TestTargetTransportPort;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultLogicalUnitTest extends AbstractLogicalUnit {

    private static Logger _logger = Logger.getLogger(DefaultLogicalUnitTest.class);

    private static final int NUM_BLOCKS_TRANSFER = 16;

    private static final int TASK_SET_QUEUE_DEPTH = 16;

    private static final int TASK_MGR_NUM_THREADS = 2;

    private static final int STORE_BLOCK_SIZE = 4096;

    private static final int STORE_CAPACITY = 8192;

    private static LogicalUnit lu;

    private static TaskSet taskSet;

    private static TaskManager taskManager;

    private static ModePageRegistry modeRegistry;

    private static InquiryDataRegistry inquiryRegistry;

    private static TaskFactory taskFactory;

    private static ByteBuffer store;

    private long cmdRef = 0;

    private TestTargetTransportPort transport = new TestTargetTransportPort(STORE_CAPACITY, STORE_BLOCK_SIZE);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        _logger.debug("initializing test");
        taskSet = new DefaultTaskSet(TASK_SET_QUEUE_DEPTH);
        taskManager = new DefaultTaskManager(TASK_MGR_NUM_THREADS, taskSet);
        modeRegistry = new StaticModePageRegistry();
        inquiryRegistry = new StaticInquiryDataRegistry();
        store = ByteBuffer.allocate(STORE_BLOCK_SIZE * STORE_CAPACITY);
        taskFactory = new BufferedTaskFactory(store, STORE_BLOCK_SIZE, modeRegistry, inquiryRegistry);
        lu = new DefaultLogicalUnitTest(taskSet, taskManager, taskFactory);
        _logger.debug("created logical unit: " + lu);
        lu.start();
        _logger.debug("logical unit successfully started");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        lu.stop();
        _logger.debug("exiting test");
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void TestReadWriteCompare6() {
        CDB cdb1 = new Write6(false, true, 10, NUM_BLOCKS_TRANSFER);
        Command cmd1 = new Command(this.transport.createNexus(this.cmdRef), cdb1, TaskAttribute.ORDERED, this.cmdRef, 0);
        this.transport.createReadData(NUM_BLOCKS_TRANSFER * STORE_BLOCK_SIZE, this.cmdRef);
        lu.enqueue(this.transport, cmd1);
        this.cmdRef++;
        CDB cdb2 = new Read6(false, true, 10, NUM_BLOCKS_TRANSFER);
        Command cmd2 = new Command(this.transport.createNexus(this.cmdRef), cdb2, TaskAttribute.ORDERED, this.cmdRef, 0);
        lu.enqueue(this.transport, cmd2);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        byte[] readBuf = this.transport.getReadDataMap().get(cmdRef - 1).array();
        byte[] writeBuf = this.transport.getWriteDataMap().get(cmdRef).array();
        Assert.assertTrue("inconsistent read/write comparison", Arrays.equals(readBuf, writeBuf));
    }

    @Test
    public void TestReadWriteCompare10() {
        CDB cdb1 = new Write10(0, false, false, false, false, false, 10, NUM_BLOCKS_TRANSFER);
        Command cmd1 = new Command(this.transport.createNexus(this.cmdRef), cdb1, TaskAttribute.ORDERED, this.cmdRef, 0);
        this.transport.createReadData(NUM_BLOCKS_TRANSFER * STORE_BLOCK_SIZE, this.cmdRef);
        lu.enqueue(this.transport, cmd1);
        this.cmdRef++;
        CDB cdb2 = new Read10(0, false, false, false, false, false, 10, NUM_BLOCKS_TRANSFER);
        Command cmd2 = new Command(this.transport.createNexus(this.cmdRef), cdb2, TaskAttribute.ORDERED, this.cmdRef, 0);
        lu.enqueue(this.transport, cmd2);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        byte[] readBuf = this.transport.getReadDataMap().get(cmdRef - 1).array();
        byte[] writeBuf = this.transport.getWriteDataMap().get(cmdRef).array();
        Assert.assertTrue("inconsistent read/write comparison", Arrays.equals(readBuf, writeBuf));
    }

    @Test
    public void TestReadWriteCompare12() {
        CDB cdb1 = new Write12(0, false, false, false, false, false, 10, NUM_BLOCKS_TRANSFER);
        Command cmd1 = new Command(this.transport.createNexus(this.cmdRef), cdb1, TaskAttribute.ORDERED, this.cmdRef, 0);
        this.transport.createReadData(NUM_BLOCKS_TRANSFER * STORE_BLOCK_SIZE, this.cmdRef);
        lu.enqueue(this.transport, cmd1);
        this.cmdRef++;
        CDB cdb2 = new Read12(0, false, false, false, false, false, 10, NUM_BLOCKS_TRANSFER);
        Command cmd2 = new Command(this.transport.createNexus(this.cmdRef), cdb2, TaskAttribute.ORDERED, this.cmdRef, 0);
        lu.enqueue(this.transport, cmd2);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        byte[] readBuf = this.transport.getReadDataMap().get(cmdRef - 1).array();
        byte[] writeBuf = this.transport.getWriteDataMap().get(cmdRef).array();
        Assert.assertTrue("inconsistent read/write comparison", Arrays.equals(readBuf, writeBuf));
    }

    @Test
    public void TestReadWriteCompare16() {
        CDB cdb1 = new Write16(0, false, false, false, false, false, 10, NUM_BLOCKS_TRANSFER);
        Command cmd1 = new Command(this.transport.createNexus(this.cmdRef), cdb1, TaskAttribute.ORDERED, this.cmdRef, 0);
        this.transport.createReadData(NUM_BLOCKS_TRANSFER * STORE_BLOCK_SIZE, this.cmdRef);
        lu.enqueue(this.transport, cmd1);
        this.cmdRef++;
        CDB cdb2 = new Read16(0, false, false, false, false, false, 10, NUM_BLOCKS_TRANSFER);
        Command cmd2 = new Command(this.transport.createNexus(this.cmdRef), cdb2, TaskAttribute.ORDERED, this.cmdRef, 0);
        lu.enqueue(this.transport, cmd2);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        byte[] readBuf = this.transport.getReadDataMap().get(cmdRef - 1).array();
        byte[] writeBuf = this.transport.getWriteDataMap().get(cmdRef).array();
        Assert.assertTrue("inconsistent read/write comparison", Arrays.equals(readBuf, writeBuf));
    }

    public DefaultLogicalUnitTest() {
    }

    public DefaultLogicalUnitTest(TaskSet taskSet, TaskManager taskManager, TaskFactory taskFactory) {
        super(taskSet, taskManager, taskFactory);
    }
}
