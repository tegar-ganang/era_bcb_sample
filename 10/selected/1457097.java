package com.store;

import java.sql.Date;
import java.util.List;
import com.jedi.BaseObj;
import com.tss.util.DbConn;
import com.tss.util.DbRs;

/**
 * @author wevjoso
 * 
 */
public class Contract extends BaseObj {

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            conn.setAutoCommit(false);
            sql = "insert into techcontract (" + "serviceid,area,customer,linkman,busid,phone,saleid,status," + "begindate,enddate,servicelevel,contractdesc,contractlile, " + "techbegindate,techenddate,sevbegindate,sevenddate,analyreport," + "techsupport,comid,techmanager,engineer,assigntime,contype, " + " fifee,mfee,myear,spartfee,outefee,prjname,createman,signfee) values ( " + " ?,?,?,?,?,?,?,?,?,?," + " ?,?,?,?,?,?,?,?,?,?," + " ?,?,?,?,?,?,?,?,?,?,?,? )";
            conn.prepare(sql);
            conn.setString(1, getServiceid());
            conn.setString(2, getArea());
            conn.setString(3, getCustomer());
            conn.setString(4, getLinkman());
            conn.setInt(5, getBusid());
            conn.setString(6, getPhone());
            conn.setString(7, getSaleid());
            conn.setInt(8, getStatus());
            conn.setDate(9, getBegindate());
            conn.setDate(10, getEnddate());
            conn.setInt(11, getServicelevel());
            conn.setString(12, getContractdesc());
            conn.setString(13, getContractlile());
            conn.setDate(14, getTechbegindate());
            conn.setDate(15, getTechenddate());
            conn.setDate(16, getSevbegindate());
            conn.setDate(17, getSevenddate());
            conn.setString(18, getAnalyreport());
            conn.setString(19, getTechsupport());
            conn.setInt(20, getComid());
            conn.setString(21, getTechmanager());
            conn.setString(22, getEngineer());
            conn.setDate(23, null);
            conn.setInt(24, getContype());
            conn.setDouble(25, getFifee());
            conn.setDouble(26, getMfee());
            conn.setDouble(27, getMyear());
            conn.setDouble(28, getSpartfee());
            conn.setDouble(29, getOutefee());
            conn.setString(30, getPrjname());
            conn.setString(31, getCreateman());
            conn.setDouble(32, getSignfee());
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
        double signfee = getSignfee();
        try {
            conn.setAutoCommit(false);
            String sql = "update techcontract  set" + " serviceid = ? , area = ? , customer = ? ,  linkman= ?," + " phone = ? ,saleid = ? , begindate=?," + " enddate = ? ,servicelevel=?,contractdesc = ?,contractlile=?," + "fifee=?,mfee=? ,myear=?,spartfee=?,outefee=?,busid=? ," + "techbegindate=?,techenddate=?,sevbegindate=?,sevenddate=? ," + "  analyreport=?,techsupport=?,comid=?,techmanager=?,assigntime=? ,contype=?,status=?,prjname=?,signfee=? " + "where conid=?";
            conn.prepare(sql);
            conn.setString(1, getServiceid());
            conn.setString(2, getArea());
            conn.setString(3, getCustomer());
            conn.setString(4, getLinkman());
            conn.setString(5, getPhone());
            conn.setString(6, getSaleid());
            conn.setDate(7, getBegindate());
            conn.setDate(8, getEnddate());
            conn.setInt(9, getServicelevel());
            conn.setString(10, getContractdesc());
            conn.setString(11, getContractlile());
            conn.setDouble(12, getFifee());
            conn.setDouble(13, getMfee());
            conn.setDouble(14, getMyear());
            conn.setDouble(15, getSpartfee());
            conn.setDouble(16, getOutefee());
            conn.setInt(17, getBusid());
            conn.setDate(18, getTechbegindate());
            conn.setDate(19, getTechenddate());
            conn.setDate(20, getSevbegindate());
            conn.setDate(21, getSevenddate());
            conn.setString(22, getAnalyreport());
            conn.setString(23, getTechsupport());
            conn.setInt(24, getComid());
            conn.setString(25, getTechmanager());
            conn.setDate(26, getAssigntime());
            conn.setInt(27, getContype());
            conn.setInt(28, getStatus());
            conn.setString(29, getPrjname());
            conn.setDouble(30, getSignfee());
            conn.setInt(31, getConid());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public void delete() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            DbRs rs = null;
            sql = "select * from techcontract where conid = ?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            rs = conn.executeQuery();
            if (!(rs == null || rs.size() > 0)) {
                setErr("该信息不存在");
                return;
            }
            sql = "select * from task where conid = ?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            rs = conn.executeQuery();
            if (rs.size() > 0) {
                setErr("该合同已经定制工单,不能删除！");
                return;
            }
            sql = "delete from pala where conid = ?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.executeUpdate();
            sql = "delete from techconeng where conid = ?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.executeUpdate();
            sql = "delete from techcontract where conid = ?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    /**
	 * 分配工程师
	 *
	 */
    public void insert_engs() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            List list = getListEngs();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    DbRs rs = null;
                    Techconeng item = (Techconeng) list.get(i);
                    String sql = "select * from techconeng where conid=? and engid=? and taskid=? ";
                    conn.prepare(sql);
                    conn.setInt(1, item.getConid());
                    conn.setString(2, item.getEngid());
                    conn.setInt(3, item.getTaskid());
                    rs = conn.executeQuery();
                    if (rs.size() > 0) {
                        setErr("请不要在相同合同中添加同一个没有创建合同的工程师");
                        return;
                    }
                    insert(conn, item);
                }
            }
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

    public void insert(DbConn conn, Techconeng eng) throws Exception {
        clearErr();
        String sql = "";
        sql = "insert into techconeng (conid,engid,taskid) values (?,?,? )";
        conn.prepare(sql);
        conn.setInt(1, eng.getConid());
        conn.setString(2, eng.getEngid());
        conn.setInt(3, eng.getTaskid());
        conn.executeUpdate();
    }

    public void distribute() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "update techcontract  set" + "  engineer=?,assigntime=? where conid=? ";
            conn.prepare(sql);
            conn.setString(1, getEngineer());
            conn.setDate(2, getAssigntime());
            conn.setInt(3, getConid());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public Contract() {
    }

    public Contract(String id) {
        this.id = id;
    }

    private String id = "";

    private int conid = 0;

    private String serviceid = "";

    private String area = "";

    private String customer = "";

    private String linkman = "";

    private int busid = 0;

    private String phone = "";

    private String saleid = "";

    private int status = 0;

    private Date begindate = null;

    private Date enddate = null;

    private int servicelevel = 0;

    private String contractdesc = "";

    private String contractlile = "";

    private Date techbegindate = null;

    private Date techenddate = null;

    private Date sevbegindate = null;

    private Date sevenddate = null;

    private Date createtime = null;

    private String analyreport = "";

    private String techsupport = "";

    private int comid = 0;

    private String techmanager = "";

    private String engineer = "";

    private Date assigntime = null;

    private int contype = 0;

    private double fifee = 0.00;

    private double mfee = 0.00;

    private int myear = 0;

    private double spartfee = 0.00;

    private double outefee = 0.00;

    private double signfee = 0.00;

    private String prjname = "";

    private String createman = "";

    private String createman_name = "";

    private int preNum = 0;

    private int delcount = 0;

    private double totFee = 0.00;

    private int order = 0;

    private List listEngs = null;

    /**
	 *虚拟字段
	 *
	 */
    private String bus_name = "";

    private String sale_name = "";

    private String status_name = "";

    private String contype_name = "";

    private String engineer_name = "";

    private String techmanager_name = "";

    private String com_name = "";

    private Sevgrade sevgrade;

    public Sevgrade getSevgrade() {
        return sevgrade;
    }

    public void setSevgrade(Sevgrade sevgrade) {
        this.sevgrade = sevgrade;
    }

    public String getAnalyreport() {
        return analyreport;
    }

    public void setAnalyreport(String analyreport) {
        this.analyreport = analyreport;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Date getAssigntime() {
        return assigntime;
    }

    public void setAssigntime(Date assigntime) {
        this.assigntime = assigntime;
    }

    public Date getBegindate() {
        return begindate;
    }

    public void setBegindate(Date begindate) {
        this.begindate = begindate;
    }

    public String getBus_name() {
        return bus_name;
    }

    public void setBus_name(String bus_name) {
        this.bus_name = bus_name;
    }

    public int getBusid() {
        return busid;
    }

    public void setBusid(int busid) {
        this.busid = busid;
    }

    public String getCom_name() {
        return com_name;
    }

    public void setCom_name(String com_name) {
        this.com_name = com_name;
    }

    public int getComid() {
        return comid;
    }

    public void setComid(int comid) {
        this.comid = comid;
    }

    public int getConid() {
        return conid;
    }

    public void setConid(int conid) {
        this.conid = conid;
    }

    public String getContractdesc() {
        return contractdesc;
    }

    public void setContractdesc(String contractdesc) {
        this.contractdesc = contractdesc;
    }

    public String getContractlile() {
        return contractlile;
    }

    public void setContractlile(String contractlile) {
        this.contractlile = contractlile;
    }

    public int getContype() {
        return contype;
    }

    public void setContype(int contype) {
        this.contype = contype;
    }

    public String getContype_name() {
        return contype_name;
    }

    public void setContype_name(String contype_name) {
        this.contype_name = contype_name;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public Date getEnddate() {
        return enddate;
    }

    public void setEnddate(Date enddate) {
        this.enddate = enddate;
    }

    public String getEngineer() {
        return engineer;
    }

    public void setEngineer(String engineer) {
        this.engineer = engineer;
    }

    public String getEngineer_name() {
        return engineer_name;
    }

    public void setEngineer_name(String engineer_name) {
        this.engineer_name = engineer_name;
    }

    public double getFifee() {
        return fifee;
    }

    public void setFifee(double fifee) {
        this.fifee = fifee;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLinkman() {
        return linkman;
    }

    public void setLinkman(String linkman) {
        this.linkman = linkman;
    }

    public double getMfee() {
        return mfee;
    }

    public void setMfee(double mfee) {
        this.mfee = mfee;
    }

    public int getMyear() {
        return myear;
    }

    public void setMyear(int myear) {
        this.myear = myear;
    }

    public double getOutefee() {
        return outefee;
    }

    public void setOutefee(double outefee) {
        this.outefee = outefee;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSale_name() {
        return sale_name;
    }

    public void setSale_name(String sale_name) {
        this.sale_name = sale_name;
    }

    public String getSaleid() {
        return saleid;
    }

    public void setSaleid(String saleid) {
        this.saleid = saleid;
    }

    public String getServiceid() {
        return serviceid;
    }

    public void setServiceid(String serviceid) {
        this.serviceid = serviceid;
    }

    public int getServicelevel() {
        return servicelevel;
    }

    public void setServicelevel(int servicelevel) {
        this.servicelevel = servicelevel;
    }

    public Date getSevbegindate() {
        return sevbegindate;
    }

    public void setSevbegindate(Date sevbegindate) {
        this.sevbegindate = sevbegindate;
    }

    public Date getSevenddate() {
        return sevenddate;
    }

    public void setSevenddate(Date sevenddate) {
        this.sevenddate = sevenddate;
    }

    public double getSpartfee() {
        return spartfee;
    }

    public void setSpartfee(double spartfee) {
        this.spartfee = spartfee;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatus_name() {
        return status_name;
    }

    public void setStatus_name(String status_name) {
        this.status_name = status_name;
    }

    public Date getTechbegindate() {
        return techbegindate;
    }

    public void setTechbegindate(Date techbegindate) {
        this.techbegindate = techbegindate;
    }

    public Date getTechenddate() {
        return techenddate;
    }

    public void setTechenddate(Date techenddate) {
        this.techenddate = techenddate;
    }

    public String getTechmanager() {
        return techmanager;
    }

    public void setTechmanager(String techmanager) {
        this.techmanager = techmanager;
    }

    public String getTechmanager_name() {
        return techmanager_name;
    }

    public void setTechmanager_name(String techmanager_name) {
        this.techmanager_name = techmanager_name;
    }

    public String getTechsupport() {
        return techsupport;
    }

    public void setTechsupport(String techsupport) {
        this.techsupport = techsupport;
    }

    public String getPrjname() {
        return prjname;
    }

    public void setPrjname(String prjname) {
        this.prjname = prjname;
    }

    public String getCreateman() {
        return createman;
    }

    public void setCreateman(String createman) {
        this.createman = createman;
    }

    public String getCreateman_name() {
        return createman_name;
    }

    public void setCreateman_name(String createman_name) {
        this.createman_name = createman_name;
    }

    public double getSignfee() {
        return signfee;
    }

    public void setSignfee(double signfee) {
        this.signfee = signfee;
    }

    public int getDelcount() {
        return delcount;
    }

    public void setDelcount(int delcount) {
        this.delcount = delcount;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getPreNum() {
        return preNum;
    }

    public void setPreNum(int preNum) {
        this.preNum = preNum;
    }

    public double getTotFee() {
        return totFee;
    }

    public void setTotFee(double totFee) {
        this.totFee = totFee;
    }

    public List getListEngs() {
        return listEngs;
    }

    public void setListEngs(List listEngs) {
        this.listEngs = listEngs;
    }
}
