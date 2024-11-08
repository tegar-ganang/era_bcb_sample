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
public class Task extends BaseObj {

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "insert into task(area_id,conid,cusname,linkman,phone,addr,status,begindate," + "enddate,engineerid,matterinfo,servicelog,diffmod,taskaddress," + "triptype,normaltime,worktime,tasktime,tripfee,spartfee,outefee,tasklile)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            conn.prepare(sql);
            conn.setString(1, getArea_id());
            conn.setInt(2, getConid());
            conn.setString(3, getCusname());
            conn.setString(4, getLinkman());
            conn.setString(5, getPhone());
            conn.setString(6, getAddr());
            conn.setInt(7, getStatus());
            conn.setDate(8, getBegindate());
            conn.setDate(9, getEnddate());
            conn.setString(10, getEngineerid());
            conn.setString(11, getMatterinfo());
            conn.setString(12, getServicelog());
            conn.setInt(13, getDiffmod());
            conn.setString(14, getTaskaddress());
            conn.setInt(15, getTriptype());
            conn.setInt(16, getNormaltime());
            conn.setInt(17, getWorktime());
            conn.setInt(18, getTasktime());
            conn.setDouble(19, getTripfee());
            conn.setDouble(20, getSpartfee());
            conn.setDouble(21, getOutefee());
            conn.setString(22, getTasklile());
            conn.executeUpdate();
            sql = "update techconeng  set  taskid=lastval() where conid=? and engid=? and taskid=?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.setString(2, getEngineerid());
            conn.setInt(3, 0);
            conn.executeUpdate();
            sql = "update techcontract  set  linkman=?,phone=? where conid=? ";
            conn.prepare(sql);
            conn.setString(1, getLinkman());
            conn.setString(2, getPhone());
            conn.setInt(3, getConid());
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
            conn.setAutoCommit(false);
            String sql = "update task  set" + " area_id=?,cusname=?,linkman=?,phone=?," + " addr=?,begindate=?, enddate=?," + "  matterinfo=?,servicelog=?, diffmod=?,taskaddress=?,triptype=?,normaltime=?," + "  worktime=?,tripfee=?,tasktime=?,spartfee=?,outefee=? ,tasklile=? where taskid=?";
            conn.prepare(sql);
            conn.setString(1, getArea_id());
            conn.setString(2, getCusname());
            conn.setString(3, getLinkman());
            conn.setString(4, getPhone());
            conn.setString(5, getAddr());
            conn.setDate(6, getBegindate());
            conn.setDate(7, getEnddate());
            conn.setString(8, getMatterinfo());
            conn.setString(9, getServicelog());
            conn.setInt(10, getDiffmod());
            conn.setString(11, getTaskaddress());
            conn.setInt(12, getTriptype());
            conn.setInt(13, getNormaltime());
            conn.setInt(14, getWorktime());
            conn.setDouble(15, getTripfee());
            conn.setInt(16, getTasktime());
            conn.setDouble(17, getSpartfee());
            conn.setDouble(18, getOutefee());
            conn.setString(19, getTasklile());
            conn.setString(20, getTaskid());
            conn.executeUpdate();
            sql = "update techcontract  set  linkman=?,phone=? where conid=? ";
            conn.prepare(sql);
            conn.setString(1, getLinkman());
            conn.setString(2, getPhone());
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

    public void scroe() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "update task  set  taskscore=?,scoreman=? ,scoretime=?,status=? where taskid=?";
            conn.prepare(sql);
            conn.setInt(1, getTaskscore());
            conn.setString(2, getScoreman());
            conn.setDate(3, getScoretime());
            conn.setInt(4, getStatus());
            conn.setString(5, getTaskid());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public void modiStatus() {
        clearErr();
        DbConn conn = new DbConn();
        String sql = "";
        try {
            conn.setAutoCommit(false);
            if (getStatus() == 1) {
                sql = "update task  set  abegindate=?, status=? where taskid=? ";
            }
            if (getStatus() == 2) {
                sql = "update task  set  aenddate=?, status=? where taskid=? ";
            }
            conn.prepare(sql);
            if (getStatus() == 1) {
                conn.setDate(1, getAbegindate());
            }
            if (getStatus() == 2) {
                conn.setDate(1, getAenddate());
            }
            conn.setInt(2, getStatus());
            conn.setString(3, getTaskid());
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
            String taskid = getTaskid();
            System.out.println(taskid);
            String sql = "";
            DbRs rs = null;
            sql = "select * from task where taskid = ?";
            conn.prepare(sql);
            conn.setString(1, getTaskid());
            rs = conn.executeQuery();
            if (rs == null || rs.size() < 1) {
                setErr("该信息不存在");
                return;
            }
            sql = "delete from taskdetail where taskid = ?";
            conn.prepare(sql);
            conn.setString(1, getTaskid());
            conn.executeUpdate();
            sql = "delete from techconeng where taskid = ?";
            conn.prepare(sql);
            conn.setString(1, getTaskid());
            conn.executeUpdate();
            sql = "delete from task where taskid = ?";
            conn.prepare(sql);
            conn.setString(1, getTaskid());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    private String taskid = "";

    private int conid = 0;

    private String area_id = "";

    private String cusname = "";

    private String linkman = "";

    private String phone = "";

    private String addr = "";

    private int status = 0;

    private Date begindate = null;

    private Date enddate = null;

    private Date abegindate = null;

    private Date aenddate = null;

    private String engineerid = "";

    private int taskscore = 0;

    private String scoreman = "";

    private Date scoretime = null;

    private String matterinfo = "";

    private String servicelog = "";

    private int diffmod = 1;

    private String taskaddress = "";

    private int triptype = 0;

    private int normaltime = 0;

    private int worktime = 0;

    private int tasktime = 0;

    private double tripfee = 0.00;

    private Date createtime = null;

    private String tasklile = "";

    private String prjname = "";

    private double spartfee = 0.00;

    private double outefee = 0.00;

    private String engineername = "";

    private String salename = "";

    private double totdays = 0.00;

    private String scoreman_name = "";

    private Contract contract;

    String id = "";

    private List listDetail = null;

    public Task() {
    }

    public Task(String id) {
        this.id = id;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getArea_id() {
        return area_id;
    }

    public void setArea_id(String area_id) {
        this.area_id = area_id;
    }

    public Date getBegindate() {
        return begindate;
    }

    public void setBegindate(Date begindate) {
        this.begindate = begindate;
    }

    public int getConid() {
        return conid;
    }

    public void setConid(int conid) {
        this.conid = conid;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public String getCusname() {
        return cusname;
    }

    public void setCusname(String cusname) {
        this.cusname = cusname;
    }

    public int getDiffmod() {
        return diffmod;
    }

    public void setDiffmod(int diffmod) {
        this.diffmod = diffmod;
    }

    public Date getEnddate() {
        return enddate;
    }

    public void setEnddate(Date enddate) {
        this.enddate = enddate;
    }

    public String getEngineerid() {
        return engineerid;
    }

    public void setEngineerid(String engineerid) {
        this.engineerid = engineerid;
    }

    public String getLinkman() {
        return linkman;
    }

    public void setLinkman(String linkman) {
        this.linkman = linkman;
    }

    public String getMatterinfo() {
        return matterinfo;
    }

    public void setMatterinfo(String matterinfo) {
        this.matterinfo = matterinfo;
    }

    public int getNormaltime() {
        return normaltime;
    }

    public void setNormaltime(int normaltime) {
        this.normaltime = normaltime;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getScoreman() {
        return scoreman;
    }

    public void setScoreman(String scoreman) {
        this.scoreman = scoreman;
    }

    public Date getScoretime() {
        return scoretime;
    }

    public void setScoretime(Date scoretime) {
        this.scoretime = scoretime;
    }

    public String getServicelog() {
        return servicelog;
    }

    public void setServicelog(String servicelog) {
        this.servicelog = servicelog;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTaskaddress() {
        return taskaddress;
    }

    public void setTaskaddress(String taskaddress) {
        this.taskaddress = taskaddress;
    }

    public String getTaskid() {
        return taskid;
    }

    public void setTaskid(String taskid) {
        this.taskid = taskid;
    }

    public int getTaskscore() {
        return taskscore;
    }

    public void setTaskscore(int taskscore) {
        this.taskscore = taskscore;
    }

    public int getTasktime() {
        return tasktime;
    }

    public void setTasktime(int tasktime) {
        this.tasktime = tasktime;
    }

    public double getTripfee() {
        return tripfee;
    }

    public void setTripfee(double tripfee) {
        this.tripfee = tripfee;
    }

    public int getTriptype() {
        return triptype;
    }

    public void setTriptype(int triptype) {
        this.triptype = triptype;
    }

    public int getWorktime() {
        return worktime;
    }

    public void setWorktime(int worktime) {
        this.worktime = worktime;
    }

    public Date getAbegindate() {
        return abegindate;
    }

    public void setAbegindate(Date abegindate) {
        this.abegindate = abegindate;
    }

    public Date getAenddate() {
        return aenddate;
    }

    public void setAenddate(Date aenddate) {
        this.aenddate = aenddate;
    }

    public List getListDetail() {
        return listDetail;
    }

    public void setListDetail(List listDetail) {
        this.listDetail = listDetail;
    }

    public String getEngineername() {
        return engineername;
    }

    public void setEngineername(String engineername) {
        this.engineername = engineername;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSalename() {
        return salename;
    }

    public void setSalename(String salename) {
        this.salename = salename;
    }

    public String getPrjname() {
        return prjname;
    }

    public void setPrjname(String prjname) {
        this.prjname = prjname;
    }

    public double getOutefee() {
        return outefee;
    }

    public void setOutefee(double outefee) {
        this.outefee = outefee;
    }

    public double getSpartfee() {
        return spartfee;
    }

    public void setSpartfee(double spartfee) {
        this.spartfee = spartfee;
    }

    public String getScoreman_name() {
        return scoreman_name;
    }

    public void setScoreman_name(String scoreman_name) {
        this.scoreman_name = scoreman_name;
    }

    public String getTasklile() {
        return tasklile;
    }

    public void setTasklile(String tasklile) {
        this.tasklile = tasklile;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public double getTotdays() {
        return totdays;
    }

    public void setTotdays(double totdays) {
        this.totdays = totdays;
    }
}
