package org.carp.test.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.Date;
import org.carp.CarpDataSetQuery;
import org.carp.CarpSession;
import org.carp.CarpSessionBuilder;
import org.carp.cfg.CarpConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SqlUpdateTest {

    CarpConfig config = null;

    ;

    CarpSessionBuilder builder;

    CarpSession s = null;

    @Before
    public void setUp() throws Exception {
        config = new CarpConfig();
        builder = config.getSessionBuilder();
        s = builder.getSession();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void pk() throws Exception {
        Connection conn = s.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement ps = conn.prepareStatement("insert into t_test(t_name,t_cname,t_data,t_date,t_double) values(?,?,?,?,?)");
        for (int i = 10; i < 20; ++i) {
            ps.setString(1, "name-" + i);
            ps.setString(2, "cname-" + i);
            ps.setBlob(3, null);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setNull(5, java.sql.Types.DOUBLE);
            ps.executeUpdate();
        }
        conn.rollback();
        conn.setAutoCommit(true);
        ps.close();
        conn.close();
    }

    public void meta() throws Exception {
        Connection conn = s.getConnection();
        PreparedStatement ps = conn.prepareStatement("select * from t_test");
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        int count = rsmd.getColumnCount();
        for (int i = 1; i <= count; ++i) {
            System.out.print(rsmd.getColumnName(i) + " -- ");
            System.out.print(rsmd.getColumnClassName(i) + " -- ");
            System.out.print(rsmd.getColumnDisplaySize(i) + " -- ");
            System.out.print(rsmd.getColumnLabel(i) + " -- ");
            System.out.print(rsmd.getColumnType(i) + " -- ");
            System.out.print(rsmd.getColumnTypeName(i) + " -- ");
            System.out.print(rsmd.getPrecision(i) + " -- ");
            System.out.print(rsmd.getScale(i));
            System.out.println();
        }
        rs.close();
        ps.close();
    }

    public void query() throws Exception {
        Connection conn = s.getConnection();
        PreparedStatement ps = conn.prepareStatement("select * from t_test");
        ResultSet rs = ps.executeQuery();
        FileOutputStream fos = new FileOutputStream("d:/bbb.xls");
        int len = -1;
        while (rs.next()) {
            InputStream is = rs.getBlob(6).getBinaryStream();
            while ((len = is.read()) != -1) {
                fos.write(len);
            }
            fos.close();
            is.close();
        }
        rs.close();
        ps.close();
    }

    public void update() throws Exception {
        Connection conn = s.getConnection();
        PreparedStatement ps = conn.prepareStatement("insert into t_test values(?,?,?,?,?,?)");
        ps.setLong(1, 1);
        ps.setString(2, "name-1");
        ps.setString(3, "�������-1");
        ps.setDouble(4, 222.01);
        ps.setTimestamp(5, null);
        File file = new File("d:/aaa.xls");
        FileInputStream fis = new FileInputStream(file);
        ps.setBinaryStream(6, fis, (int) file.length());
        ps.executeUpdate();
        ps.close();
    }

    public void add() throws Exception {
        CarpDataSetQuery q = s.creatDataSetQuery("insert into t_test values(?,?,?,?,?,?)");
        q.setLong(1, 2);
        q.setString(2, "name-2");
        q.setString(3, "�������-2");
        q.setDouble(6, 111.01);
        q.setTimestamp(5, new Date());
        q.setBinaryStream(4, new FileInputStream("e:/pmp.txt"));
        q.executeUpdate();
    }

    public void del() throws Exception {
        FileInputStream fis = new FileInputStream("d:/aaa.xls");
        FileOutputStream fos = new FileOutputStream("d:/bbb.xls");
        int count = 0;
        int len = -1;
        byte[] b = new byte[4096];
        while ((len = fis.read(b, 0, b.length)) != -1) {
            count += len;
        }
        fis.skip(-count);
    }
}
