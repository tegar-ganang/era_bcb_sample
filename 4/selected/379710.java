package com.ohua.tests.travelers;

import static junit.framework.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import com.ohua.checkpoint.framework.operatorcheckpoints.AbstractCheckPoint;
import com.ohua.checkpoint.framework.operatorcheckpoints.GeneratorOperatorCheckpoint;
import com.ohua.checkpoint.framework.serialization.OperatorCheckpointSerializer;
import com.ohua.engine.AbstractProcessManager;
import com.ohua.engine.flowgraph.elements.operator.OperatorID;
import com.ohua.engine.operators.GeneratorOperator;
import com.ohua.tests.AbstractIOTestCase;

public class NDMergeReplayTestHarness extends DatabaseEndpointReplayTestHarness {

    private int _committedFromLeftGenerator = 0;

    private int _committedFromRightGenerator = 0;

    protected NDMergeReplayTestHarness(AbstractIOTestCase testProbe) {
        super(testProbe);
    }

    @Override
    protected void recoverTransactionalEndpointsState() throws Throwable {
        AbstractProcessManager manager = _testProbe.loadProcess(_testProbe.getTestClassInputDirectory() + "Det-Replay-flow.xml");
        ((GeneratorOperator) manager.getProcess().getGraph().getOperator("Left-DataGenerator").getOperatorAlgorithm()).getProperties().setAmountToGenerate(_committedFromLeftGenerator);
        ((GeneratorOperator) manager.getProcess().getGraph().getOperator("Right-DataGenerator").getOperatorAlgorithm()).getProperties().setAmountToGenerate(_committedFromRightGenerator);
        _testProbe.runFlowNoAssert(manager);
        AbstractIOTestCase.tableRegressionCheck("det_restart", _committedFromLeftGenerator + _committedFromRightGenerator);
    }

    /**
   * The goal here is to understand how many packets have been committed to the database from
   * each of the data generators.
   */
    @Override
    protected void analyseCheckpointsAndPrepareRestart(int checkpointID) throws Throwable {
        super.analyseCheckpointsAndPrepareRestart(checkpointID);
        OperatorID operatorID = new OperatorID(3);
        long miniCheckpointPointer = extractMiniCheckpointPointerFromCheckpoint(checkpointID, operatorID);
        validateNDMergeMiniCheckpointPointer(operatorID, miniCheckpointPointer);
        _committedFromLeftGenerator = getAlreadySentFromGeneratorCheckpoint(checkpointID, 1);
        _committedFromRightGenerator = getAlreadySentFromGeneratorCheckpoint(checkpointID, 2);
        int committedWithRestartCheckpoint = _committedFromLeftGenerator + _committedFromRightGenerator;
        assertTrue("_packetsCommittedAtRestartCP: " + _packetsCommittedAtRestartCP + " committedWithRestartCheckpoint:" + committedWithRestartCheckpoint, _packetsCommittedAtRestartCP == committedWithRestartCheckpoint);
        assertTrue("committedWithRestartCheckpoint: " + committedWithRestartCheckpoint + " _committedRecords: " + _committedRecords, committedWithRestartCheckpoint <= _committedRecords);
        if (committedWithRestartCheckpoint < _committedRecords) {
            Map<String, Integer> inputPortsReplay = prepareNDMiniForRestart(miniCheckpointPointer, operatorID, _committedRecords - committedWithRestartCheckpoint);
            _committedFromLeftGenerator = _committedFromLeftGenerator + inputPortsReplay.get("input_1");
            _committedFromRightGenerator = _committedFromRightGenerator + inputPortsReplay.get("input_2");
        }
    }

    /**
   * Clearly all the mini checkpoints stored in the mini checkpoint file for the ND merge
   * accumulated up to the pointer stored in the checkpoint we restart from must be the same
   * amount as seen in the database mini.
   * @param operatorID
   * @param miniCheckpointPointer
   * @throws IOException
   */
    private void validateNDMergeMiniCheckpointPointer(OperatorID operatorID, long miniCheckpointPointer) throws IOException {
        String relativePathToMinis = "operators/operator_" + operatorID.getIDInt() + "/checkpoints/minis/mini_checkpoints";
        RandomAccessFile reader = new RandomAccessFile(_testProbe.getTestMethodOutputDirectory() + "run/" + relativePathToMinis, "r");
        FileChannel channel = reader.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(5);
        int seenPackets = 0;
        while (seenPackets < _packetsCommittedAtRestartCP) {
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
            buffer.get();
            seenPackets = seenPackets + buffer.getInt();
        }
        assertTrue("seenPackets: " + seenPackets + " packetsCommittedAtRestartCP: " + _packetsCommittedAtRestartCP, seenPackets == _packetsCommittedAtRestartCP);
        assertTrue("channel.position(): " + channel.position() + " miniCheckpointPointer:" + miniCheckpointPointer, channel.position() == miniCheckpointPointer);
        channel.close();
        reader.close();
    }

    private Map<String, Integer> prepareNDMiniForRestart(long miniCheckpointPointer, OperatorID operatorID, int excessPackets) throws Throwable {
        Map<String, Integer> excessPacketMap = new HashMap<String, Integer>();
        excessPacketMap.put("input_1", new Integer(0));
        excessPacketMap.put("input_2", new Integer(0));
        String relativePathToMinis = "operators/operator_" + operatorID.getIDInt() + "/checkpoints/minis/mini_checkpoints";
        RandomAccessFile reader = new RandomAccessFile(_testProbe.getTestMethodOutputDirectory() + "run/" + relativePathToMinis, "r");
        String minisFilePath = _testProbe.getTestMethodOutputDirectory() + "restart/" + relativePathToMinis;
        File miniCPs = new File(minisFilePath);
        miniCPs.getParentFile().mkdirs();
        miniCPs.createNewFile();
        RandomAccessFile writer = new RandomAccessFile(miniCPs, "rw");
        reader.getChannel().transferTo(0, miniCheckpointPointer, writer.getChannel());
        FileChannel channel = reader.getChannel();
        channel.position(miniCheckpointPointer);
        ByteBuffer buffer = ByteBuffer.allocateDirect(5);
        int index = 0;
        int excess = excessPackets;
        while (true) {
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
            byte port = buffer.get();
            int xcessPackets = buffer.getInt();
            index += 5;
            excess = excess - xcessPackets;
            String portName = port == 0x0001 ? "input_1" : "input_2";
            if (excess <= 0) {
                xcessPackets = xcessPackets + excess;
                excessPacketMap.put(portName, excessPacketMap.get(portName) + xcessPackets);
                break;
            } else {
                excessPacketMap.put(portName, excessPacketMap.get(portName) + xcessPackets);
            }
        }
        reader.getChannel().transferTo(miniCheckpointPointer, index, writer.getChannel());
        reader.close();
        writer.close();
        return excessPacketMap;
    }

    private int getAlreadySentFromGeneratorCheckpoint(int checkpointID, int generatorID) {
        OperatorID operatorID = new OperatorID(generatorID);
        OperatorCheckpointSerializer serializer = new OperatorCheckpointSerializer(operatorID);
        serializer.setCheckpointID(checkpointID);
        serializer.initialize();
        AbstractCheckPoint cp = serializer.deserialize();
        return ((GeneratorOperatorCheckpoint) cp).getSent();
    }
}
