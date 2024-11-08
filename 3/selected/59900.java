package navigators.smart.tom.demo.keyvalue;

import navigators.smart.tom.server.defaultservices.DefaultApplicationState;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import navigators.smart.statemanagment.ApplicationState;
import navigators.smart.tom.MessageContext;
import navigators.smart.tom.ReplicaContext;
import navigators.smart.tom.ServiceReplica;
import navigators.smart.tom.server.Recoverable;
import navigators.smart.tom.server.SingleExecutable;
import navigators.smart.tom.server.defaultservices.StateLog;
import navigators.smart.tom.server.defaultservices.DefaultRecoverable;
import navigators.smart.tom.util.Logger;

/**
 *
 * @author sweta
 * 
 * This class will create a ServiceReplica and will initialize
 * it with a implementation of Executable and Recoverable interfaces. 
 */
public class BFTMapImpl implements SingleExecutable, Recoverable {

    BFTTableMap tableMap = new BFTTableMap();

    ServiceReplica replica;

    private ReplicaContext replicaContext;

    private MessageDigest md;

    private StateLog log;

    private int checkpointPeriod;

    private ReentrantLock logLock = new ReentrantLock();

    private ReentrantLock hashLock = new ReentrantLock();

    private ReentrantLock stateLock = new ReentrantLock();

    public BFTMapImpl(int id) {
        super();
        replica = new ServiceReplica(id, this, this);
        checkpointPeriod = replicaContext.getStaticConfiguration().getCheckpointPeriod();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            java.util.logging.Logger.getLogger(DefaultRecoverable.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] initialState = getSnapshot();
        log = new StateLog(checkpointPeriod, initialState, computeHash(initialState));
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Use: java BFTMapImpl <processId>");
            System.exit(-1);
        }
        new BFTMapImpl(Integer.parseInt(args[0]));
    }

    public void setReplicaContext(ReplicaContext replicaContext) {
        this.replicaContext = replicaContext;
    }

    public final byte[] computeHash(byte[] data) {
        byte[] ret = null;
        hashLock.lock();
        ret = md.digest(data);
        hashLock.unlock();
        return ret;
    }

    @Override
    @SuppressWarnings("static-access")
    public byte[] executeOrdered(byte[] command, MessageContext msgCtx) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataInputStream dis = new DataInputStream(in);
            DataOutputStream dos = new DataOutputStream(out);
            byte[] reply = null;
            int cmd = new DataInputStream(in).readInt();
            switch(cmd) {
                case KVRequestType.PUT:
                    String tableName = new DataInputStream(in).readUTF();
                    String key = dis.readUTF();
                    int valueSize = dis.readInt();
                    byte[] valueBytes = new byte[valueSize];
                    dis.read(valueBytes, 0, valueSize);
                    byte[] ret = tableMap.addData(tableName, key, valueBytes);
                    if (ret == null) {
                        ret = new byte[0];
                    }
                    reply = valueBytes;
                    break;
                case KVRequestType.REMOVE:
                    tableName = dis.readUTF();
                    key = dis.readUTF();
                    valueBytes = tableMap.removeEntry(tableName, key);
                    reply = valueBytes;
                    break;
                case KVRequestType.TAB_CREATE:
                    tableName = new DataInputStream(in).readUTF();
                    ObjectInputStream objIn = new ObjectInputStream(in);
                    Map table = null;
                    try {
                        table = (Map<String, byte[]>) objIn.readObject();
                    } catch (ClassNotFoundException ex) {
                        java.util.logging.Logger.getLogger(BFTMapImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Map<String, byte[]> tableCreated = tableMap.addTable(tableName, table);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream objOut = new ObjectOutputStream(bos);
                    objOut.writeObject(tableCreated);
                    objOut.close();
                    in.close();
                    reply = bos.toByteArray();
                    break;
                case KVRequestType.TAB_REMOVE:
                    tableName = new DataInputStream(in).readUTF();
                    table = tableMap.removeTable(tableName);
                    bos = new ByteArrayOutputStream();
                    objOut = new ObjectOutputStream(bos);
                    objOut.writeObject(table);
                    objOut.close();
                    objOut.close();
                    reply = bos.toByteArray();
                    break;
                case KVRequestType.SIZE_TABLE:
                    int size1 = tableMap.getSizeofTable();
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeInt(size1);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.GET:
                    tableName = new DataInputStream(in).readUTF();
                    key = new DataInputStream(in).readUTF();
                    valueBytes = tableMap.getEntry(tableName, key);
                    String value = new String(valueBytes);
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeBytes(value);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.SIZE:
                    String tableName2 = new DataInputStream(in).readUTF();
                    int size = tableMap.getSize(tableName2);
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeInt(size);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.CHECK:
                    tableName = new DataInputStream(in).readUTF();
                    key = new DataInputStream(in).readUTF();
                    valueBytes = tableMap.getEntry(tableName, key);
                    boolean entryExists = valueBytes != null;
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeBoolean(entryExists);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.TAB_CREATE_CHECK:
                    tableName = new DataInputStream(in).readUTF();
                    table = tableMap.getName(tableName);
                    boolean tableExists = (table != null);
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeBoolean(tableExists);
                    reply = out.toByteArray();
                    break;
            }
            if (msgCtx != null) {
                int eid = msgCtx.getConsensusId();
                if (eid > 0 && eid % checkpointPeriod == 0) {
                    Logger.println("(BFTMapImpl.executeOrdered) Performing checkpoint for consensus " + eid);
                    stateLock.lock();
                    byte[] snapshot = getSnapshot();
                    stateLock.unlock();
                    saveState(snapshot, eid, 0, 0);
                } else {
                    Logger.println("(BFTMapImpl.executeOrdered) Storing message batch in the state log for consensus " + eid);
                    saveCommand(command, eid, 0, 0);
                }
            }
            return reply;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BFTMapImpl.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void saveCommand(byte[] command, int lastEid, int decisionRound, int leader) {
        logLock.lock();
        Logger.println("(TOMLayer.saveBatch) Saving batch of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
        byte[][] commands = new byte[1][command.length];
        commands[0] = command;
        log.addMessageBatch(commands, decisionRound, leader);
        log.setLastEid(lastEid);
        logLock.unlock();
        Logger.println("(TOMLayer.saveBatch) Finished saving batch of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
    }

    private byte[] getSnapshot() {
        try {
            long initMillis = System.currentTimeMillis();
            Map<String, Map<String, byte[]>> tables = tableMap.getTables();
            Collection<String> tableNames = tables.keySet();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
            DataOutputStream dos = new DataOutputStream(baos);
            for (String tableName : tableNames) {
                dos.writeUTF(tableName);
                Map<String, byte[]> tableTmp = tables.get(tableName);
                dos.writeInt(tableTmp.size());
                for (String key : tableTmp.keySet()) {
                    dos.writeUTF(key);
                    dos.flush();
                    byte[] value = tableTmp.get(key);
                    dos.writeInt(value.length);
                    dos.write(value);
                    dos.flush();
                }
                System.out.print("---- Count of rows: " + tableTmp.size());
                dos.flush();
            }
            long timeSpent = System.currentTimeMillis() - initMillis;
            byte[] state = baos.toByteArray();
            System.out.print(", Current byte array size: " + state.length);
            System.out.println(", Time to write the byte array: " + timeSpent + " milliseconds");
            return state;
        } catch (IOException ex) {
            Logger.println(BFTMapImpl.class.getName() + ".getSnapshot() " + ex.getMessage());
            return new byte[0];
        }
    }

    private void installSnapshot(byte[] state) {
        try {
            tableMap = new BFTTableMap();
            ByteArrayInputStream bais = new ByteArrayInputStream(state);
            DataInputStream dis = new DataInputStream(bais);
            while (dis.available() > 0) {
                Map<String, byte[]> table = new TreeMap<String, byte[]>();
                String tableName = dis.readUTF();
                tableMap.addTable(tableName, table);
                int tableSize = dis.readInt();
                for (int i = 0; i < tableSize; i++) {
                    String key = dis.readUTF();
                    int valueSize = dis.readInt();
                    byte[] value = new byte[valueSize];
                    dis.read(value, 0, valueSize);
                    tableMap.addData(tableName, key, value);
                }
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BFTMapImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveState(byte[] snapshot, int lastEid, int decisionRound, int leader) {
        StateLog thisLog = log;
        logLock.lock();
        Logger.println("(TOMLayer.saveState) Saving state of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
        thisLog.newCheckpoint(snapshot, computeHash(snapshot));
        thisLog.setLastEid(-1);
        thisLog.setLastCheckpointEid(lastEid);
        thisLog.setLastCheckpointRound(decisionRound);
        thisLog.setLastCheckpointLeader(leader);
        logLock.unlock();
        Logger.println("(TOMLayer.saveState) Finished saving state of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
    }

    @SuppressWarnings("static-access")
    public byte[] executeUnordered(byte[] command, MessageContext msgCtx) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ByteArrayOutputStream out = null;
            byte[] reply = null;
            int cmd = new DataInputStream(in).readInt();
            switch(cmd) {
                case KVRequestType.SIZE_TABLE:
                    int size1 = tableMap.getSizeofTable();
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeInt(size1);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.GET:
                    String tableName = new DataInputStream(in).readUTF();
                    String key = new DataInputStream(in).readUTF();
                    byte[] valueBytes = tableMap.getEntry(tableName, key);
                    String value = new String(valueBytes);
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeBytes(value);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.SIZE:
                    String tableName2 = new DataInputStream(in).readUTF();
                    int size = tableMap.getSize(tableName2);
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeInt(size);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.CHECK:
                    tableName = new DataInputStream(in).readUTF();
                    key = new DataInputStream(in).readUTF();
                    valueBytes = tableMap.getEntry(tableName, key);
                    boolean entryExists = valueBytes != null;
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeBoolean(entryExists);
                    reply = out.toByteArray();
                    break;
                case KVRequestType.TAB_CREATE_CHECK:
                    tableName = new DataInputStream(in).readUTF();
                    Map<String, byte[]> table = tableMap.getName(tableName);
                    boolean tableExists = (table != null);
                    out = new ByteArrayOutputStream();
                    new DataOutputStream(out).writeBoolean(tableExists);
                    reply = out.toByteArray();
                    break;
            }
            return reply;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BFTMapImpl.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public ApplicationState getState(int eid, boolean sendState) {
        logLock.lock();
        ApplicationState ret = (eid > -1 ? log.getApplicationState(eid, sendState) : new DefaultApplicationState());
        logLock.unlock();
        return ret;
    }

    @Override
    public int setState(ApplicationState recvState) {
        int lastEid = -1;
        if (recvState instanceof DefaultApplicationState) {
            DefaultApplicationState state = (DefaultApplicationState) recvState;
            Logger.println("(DefaultRecoverable.setState) last eid in state: " + state.getLastEid());
            log.update(state);
            int lastCheckpointEid = state.getLastCheckpointEid();
            lastEid = state.getLastEid();
            Logger.println("(DefaultRecoverable.setState) I'm going to update myself from EID " + lastCheckpointEid + " to EID " + lastEid);
            stateLock.lock();
            installSnapshot(state.getState());
            for (int eid = lastCheckpointEid + 1; eid <= lastEid; eid++) {
                try {
                    navigators.smart.tom.util.Logger.println("(DefaultRecoverable.setState) interpreting and verifying batched requests for eid " + eid);
                    System.out.println("(DefaultRecoverable.setState) interpreting and verifying batched requests for eid " + eid);
                    if (state.getMessageBatch(eid) == null) System.out.println("(DefaultRecoverable.setState) " + eid + " NULO!!!");
                    byte[][] commands = state.getMessageBatch(eid).commands;
                    for (byte[] command : commands) {
                        executeOrdered(command, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    if (e instanceof ArrayIndexOutOfBoundsException) {
                        System.out.println("Eid do ultimo checkpoint: " + state.getLastCheckpointEid());
                        System.out.println("Eid do ultimo consenso: " + state.getLastEid());
                        System.out.println("numero de mensagens supostamente no batch: " + (state.getLastEid() - state.getLastCheckpointEid() + 1));
                        System.out.println("numero de mensagens realmente no batch: " + state.getMessageBatches().length);
                    }
                }
            }
            stateLock.unlock();
        }
        return lastEid;
    }
}
