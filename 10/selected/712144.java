package com.store;

import com.jedi.BaseObj;
import com.jedi.KeyGen;
import com.tss.util.DbConn;
import com.tss.util.DbRs;
import com.tss.util.TSSDate;

/**
 * @author dai
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Apply extends BaseObj {

    public Apply() {
    }

    public Apply(String id) {
        this.id = id;
    }

    public void reback(String sn) {
        clearErr();
        DbConn conn = new DbConn();
        try {
            if (getId().trim().equals("")) {
                setErr("û��Ҫ���µļ�¼ID��");
                return;
            }
            conn.setAutoCommit(false);
            String sql = "";
            if (sn == null || sn.trim().equals("")) {
                sql = "update t_storage_info set status = ? where list_id = ?";
                conn.prepare(sql);
                conn.setInt(1, 0);
                conn.setString(2, getStorageId());
                conn.executeUpdate();
            } else {
                sql = "update t_storage_info set SN = ?,status = ? where list_id = ?";
                conn.prepare(sql);
                conn.setString(1, sn);
                conn.setInt(2, 0);
                conn.setString(3, getStorageId());
                conn.executeUpdate();
            }
            sql = "update t_storage_apply set" + " return_date = ? ,status = ?" + " where list_id = ?";
            conn.prepare(sql);
            conn.setString(1, TSSDate.shortDate());
            conn.setInt(2, 0);
            conn.setString(3, getId());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            setId(KeyGen.nextID(""));
            conn.setAutoCommit(false);
            sql = "insert into t_storage_apply (" + " list_id,storage_id,contract_id,task_id,apply_date," + " return_date,customer_name,addr,engineer_id,engineer_name," + " apply_type,fix_date,fixed_date,upkeep,origin," + " status" + " ) values ( " + " ?,?,?,?,?,?,?,?,?,?," + " ?,?,?,?,?,?)";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.setString(2, getStorageId());
            conn.setString(3, getContractId());
            conn.setString(4, getTaskId());
            conn.setString(5, getApplyDate());
            conn.setString(6, getReturnDate());
            conn.setString(7, getCustomerName());
            conn.setString(8, getAddr());
            conn.setString(9, getEngineerId());
            conn.setString(10, getEngineerName());
            conn.setInt(11, getApplyType());
            conn.setString(12, getFixDate());
            conn.setString(13, getFixedDate());
            conn.setLong(14, getUpkeep());
            conn.setString(15, getOrigin());
            conn.setInt(16, getStatus());
            conn.executeUpdate();
            sql = "update t_storage_info set status = ? where list_id = ?";
            conn.prepare(sql);
            conn.setInt(1, getStatus());
            conn.setString(2, getStorageId());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void update() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            if (getId().trim().equals("")) {
                setErr("û��Ҫ���µļ�¼ID��");
                return;
            }
            conn.setAutoCommit(false);
            String sql = "";
            String updateContractId = "";
            String updateStorageId = "";
            long updateUpkeep = 0;
            sql = "select * from t_storage_apply where list_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            DbRs rs = conn.executeQuery();
            if (rs == null || rs.size() == 0) {
                setErr("û��Ҫ���µļ�¼!");
                return;
            }
            updateContractId = get(rs, 0, "contract_id");
            updateStorageId = get(rs, 0, "storage_id");
            updateUpkeep = getLong(rs, 0, "upkeep");
            sql = "update t_storage_apply set" + " storage_id = ? , contract_id = ? , task_id = ? , apply_date = ? , return_date = ? ," + " customer_name = ? , addr = ? , engineer_id = ? , engineer_name = ? , apply_type = ? ," + " fix_date = ? , fixed_date = ? , upkeep = ? , origin = ? , status = ?" + " where list_id = ?";
            conn.prepare(sql);
            conn.setString(1, getStorageId());
            conn.setString(2, getContractId());
            conn.setString(3, getTaskId());
            conn.setString(4, getApplyDate());
            conn.setString(5, getReturnDate());
            conn.setString(6, getCustomerName());
            conn.setString(7, getAddr());
            conn.setString(8, getEngineerId());
            conn.setString(9, getEngineerName());
            conn.setInt(10, getApplyType());
            conn.setString(11, getFixDate());
            conn.setString(12, getFixedDate());
            conn.setLong(13, getUpkeep());
            conn.setString(14, getOrigin());
            conn.setInt(15, getStatus());
            conn.setString(16, getId());
            conn.executeUpdate();
            if (getApplyType() > 0) {
                sql = "update t_contract_info set fact_cost_money = fact_cost_money + " + getUpkeep() + " where contract_id = ?";
                conn.prepare(sql);
                conn.setString(1, getContractId());
                conn.executeUpdate();
                sql = "update t_contract_info set fact_cost_money = fact_cost_money - " + updateUpkeep + " where contract_id = ?";
                conn.prepare(sql);
                conn.setString(1, updateContractId);
                conn.executeUpdate();
            }
            sql = "update t_storage_info set status = ? where list_id = ?";
            conn.prepare(sql);
            conn.setInt(1, getStatus());
            conn.setString(2, updateStorageId);
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void delete() {
        clearErr();
        DbConn conn = new DbConn();
        DbRs rs = null;
        try {
            String sql = "";
            sql = "select * from t_storage_apply where list_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            rs = conn.executeQuery();
            if (rs == null || rs.size() == 0) {
                setErr("û��Ҫɾ��ļ�¼!");
                return;
            }
            String updateContractId = get(rs, 0, "contract_id");
            long updateUpkeep = getLong(rs, 0, "upkeep");
            conn.setAutoCommit(false);
            sql = "delete from t_storage_apply where list_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
            sql = "update t_contract_info set fact_cost_money = fact_cost_money - " + updateUpkeep + " where contract_id = ?";
            conn.prepare(sql);
            conn.setString(1, updateContractId);
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public String getId() {
        return id;
    }

    public String getStorageId() {
        return storageId;
    }

    public String getContractId() {
        return contractId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getApplyDate() {
        return applyDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getAddr() {
        return addr;
    }

    public String getEngineerId() {
        return engineerId;
    }

    public String getEngineerName() {
        return engineerName;
    }

    public int getApplyType() {
        return applyType;
    }

    public String getFixDate() {
        return fixDate;
    }

    public String getFixedDate() {
        return fixedDate;
    }

    public long getUpkeep() {
        return upkeep;
    }

    public String getOrigin() {
        return origin;
    }

    public int getStatus() {
        return status;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setApplyDate(String applyDate) {
        this.applyDate = applyDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setEngineerId(String engineerId) {
        this.engineerId = engineerId;
    }

    public void setEngineerName(String engineerName) {
        this.engineerName = engineerName;
    }

    public void setApplyType(int applyType) {
        this.applyType = applyType;
    }

    public void setFixDate(String fixDate) {
        this.fixDate = fixDate;
    }

    public void setFixedDate(String fixedDate) {
        this.fixedDate = fixedDate;
    }

    public void setUpkeep(long upkeep) {
        this.upkeep = upkeep;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    private String id = "";

    private String storageId = "";

    private String contractId = "";

    private String serviceId = "";

    private String taskId = "";

    private String applyDate = "";

    private String returnDate = "";

    private String customerName = "";

    private String addr = "";

    private String engineerId = "";

    private String engineerName = "";

    private int applyType = 0;

    private String fixDate = "";

    private String fixedDate = "";

    private long upkeep = 0;

    private String origin = "";

    private int status = 0;
}
