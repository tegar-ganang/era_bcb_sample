package tw.qing.lwdba.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import tw.qing.lwdba.DBFacade;
import tw.qing.lwdba.DBRow;
import tw.qing.lwdba.TransSQLExecutor;
import tw.qing.lwdba.CachedQueryFacade;

public class LWDBASample extends DBFacade {

    private static LWDBASample instance;

    public static synchronized LWDBASample getInstance() {
        if (instance == null) {
            try {
                instance = new LWDBASample();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    private LWDBASample() throws SQLException, ClassNotFoundException, FileNotFoundException {
        super();
    }

    public void newCustomer(String name, String phone, String address) throws SQLException {
        DBRow dr = new DBRow("Customer", "seqNo");
        dr.setColumn("name", name);
        dr.setColumn("phone", phone);
        dr.setColumn("address", address);
        sqlUpdate(dr.toInsertString());
    }

    public ArrayList listAllActiveCustomer() throws SQLException {
        String sql = sqlManager.getSQL("Customer.listAllActiveCustomer");
        return sqlQueryRows(sql);
    }

    public void demoOracleDBRow(String name, String phone, String address) throws SQLException {
        DBRow dr = new DBRow("Customer", "seqNo", "oracle");
        dr.setColumn("name", name);
        dr.setColumn("phone", phone);
        dr.setColumn("address", address);
        dr.setColumn("createdTime", new Date());
        System.out.println(dr.toInsertString());
    }

    public void demoTransaction() throws SQLException, ClassNotFoundException, FileNotFoundException {
        TransSQLExecutor tse = new TransSQLExecutor();
        DBRow dr = new DBRow("Customer", "seqNo");
        dr.setColumn("name", "Winwell");
        dr.setColumn("phone", "0988168168");
        dr.setColumn("address", "Hsinchu City, Taiwan");
        tse.executeUpdate(dr.toInsertString());
        tse.rollback();
        dr = new DBRow("Customer", "seqNo");
        dr.setColumn("name", "FFEH");
        dr.setColumn("phone", "0968168168");
        dr.setColumn("address", "Hsinchu City, Taiwan");
        tse.executeUpdate(dr.toInsertString());
        tse.commit();
        tse.close();
    }

    public void demoQueryCache() throws SQLException, ClassNotFoundException {
        CachedQueryFacade cqf = CachedQueryFacade.getInstance();
        String sql = sqlManager.getSQL("Customer.listAllActiveCustomer");
        HashMap hm[] = cqf.sqlQueryCached(sql, 180);
        for (int i = 0; i < hm.length; i++) System.out.println(hm[i]);
        System.out.println("====================================");
        hm = cqf.sqlQueryCached(sql, 180);
        for (int i = 0; i < hm.length; i++) System.out.println(hm[i]);
    }

    public static void main(String argv[]) {
        try {
            LWDBASample ls = LWDBASample.getInstance();
            ls.newCustomer("Qing", "0988168168", "Hsinchu City, Taiwan");
            ls.demoOracleDBRow("Qing", "0988168168", "Hsinchu City, Taiwan");
            ls.demoTransaction();
            ArrayList al = ls.listAllActiveCustomer();
            System.out.println(al);
            ls.demoQueryCache();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
