package panda.rmi.server.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import panda.query.tree.QueryTreeNode;
import panda.rmi.server.ServerResultSet;
import panda.rmi.server.ServerStatement;
import panda.server.Panda;
import panda.transaction.Transaction;

/**
 * 
 * @author Tian Yuan
 *
 */
public class PandaServerStatement extends UnicastRemoteObject implements ServerStatement {

    PandaServerConnection conn;

    protected PandaServerStatement(PandaServerConnection conn) throws RemoteException {
        super();
        this.conn = conn;
    }

    @Override
    public ServerResultSet executeQuery(String stm) throws RemoteException {
        try {
            Transaction tx = conn.getTransaction();
            QueryTreeNode n = Panda.getPlanner().executeQuery(stm, tx);
            return new PandaServerResultSet(n, conn);
        } catch (RuntimeException r) {
            conn.rollback();
            throw r;
        }
    }

    @Override
    public int executeUpdate(String stm) throws RemoteException {
        try {
            Transaction tx = conn.getTransaction();
            int res = Panda.getPlanner().executeUpdate(stm, tx);
            conn.commit();
            return res;
        } catch (RuntimeException r) {
            conn.rollback();
            throw r;
        }
    }
}
