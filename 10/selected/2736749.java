package com.sunstar.sos.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.sunstar.sos.connect.ConnectUtil;
import com.sunstar.sos.constants.Constants;
import com.sunstar.sos.pojo.SysSequences;

/**
 * ��Dao�ӿ�ʵ���࣬�����װ�˶���ݵĴ󲿷ֵĲ������?�����Ҫ�������
 * ��չ������Ҫ����ģ�鼶��������н��У���Ҫ�ڱ���������չ����
 * @author Administrator
 *
 */
public class SequencesDao {

    /**
	 * ȡ����һ����������ֵ,��ṹ��table_name,next_value,step_value
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
    public List<SysSequences> getSeqs() throws Exception {
        List<SysSequences> list = new ArrayList<SysSequences>();
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("update ss_sys_sequences set next_value=next_value+step_value");
            ps.executeUpdate();
            ps.close();
            ps = conn.prepareStatement("select * from ss_sys_sequences");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SysSequences seq = new SysSequences();
                seq.setTableName(rs.getString(1));
                long nextValue = rs.getLong(2);
                long stepValue = rs.getLong(3);
                seq.setNextValue(nextValue - stepValue);
                seq.setStepValue(stepValue);
                list.add(seq);
            }
            rs.close();
            ps.close();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception e) {
            }
            ConnectUtil.closeConn(conn);
        }
        return list;
    }

    /**
	 * ȡ����һ����������ֵ,��ṹ��table_name,next_value,step_value
	 * @param tableName
	 * @return
	 * @throws SQLException 
	 * @throws Exception
	 */
    public SysSequences getSeqs(String tableName) throws SQLException {
        SysSequences seq = new SysSequences();
        if (tableName == null || tableName.trim().equals("")) return null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("update ss_sys_sequences set next_value=next_value+step_value where table_name='" + tableName + "'");
            ps.executeUpdate();
            ps.close();
            ps = conn.prepareStatement("select * from ss_sys_sequences where table_name='" + tableName + "'");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long nextValue = rs.getLong(2);
                long stepValue = rs.getLong(3);
                seq.setTableName(tableName);
                seq.setNextValue(nextValue - stepValue + 1);
                seq.setStepValue(stepValue);
            }
            rs.close();
            ps.close();
            if (seq.getTableName() == null) {
                ps = conn.prepareStatement("insert into ss_sys_sequences values('" + tableName + "'," + (Constants.DEFAULT_CURR_VALUE + Constants.DEFAULT_STEP_VALUE) + "," + Constants.DEFAULT_STEP_VALUE + ")");
                ps.executeUpdate();
                ps.close();
                seq.setTableName(tableName);
                seq.setNextValue(Constants.DEFAULT_CURR_VALUE + 1);
                seq.setStepValue(Constants.DEFAULT_STEP_VALUE);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception e) {
            }
            ConnectUtil.closeConn(conn);
        }
        return seq;
    }
}
