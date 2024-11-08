package com.techstar.dmis.dto.transfer;

import java.io.Serializable;

/**
 * Domain classe for 自动化月报
 * This classe is based on ValueObject Pattern
 */
public class TransZdhAutomonthreportDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public TransZdhAutomonthreportDto() {
    }

    private int reportmonth;

    private int reportyear;

    private String fstatus;

    private String isvalid;

    private String scheduleunit;

    private int rtuarate;

    private int rtusrate;

    private int computerrate;

    private int rscanum;

    private int rseanum;

    private int eddreportrate;

    private int gpafrate;

    private int upafrate;

    private int gridpcfrate;

    private int exchangeecfrate;

    private int agcrundate;

    private int agcrunrate;

    private int agcctrlstandtime;

    private int agcctrlstandrate;

    private int agcctrlcps1;

    private int agcctrlcps2;

    private int rtunum;

    private int sfaultrepairhours;

    private int rtuspinterrupthours;

    private int channelinterrupthours;

    private int linepowercuthours;

    private int sfreoairhours;

    private int otheroutrunhours;

    private int rtuoutrunallhours;

    private int psaveragefhour;

    private int sfhours;

    private int fsaveragefhour;

    private int hceqpfhours;

    private int powerinthours;

    private int maintainhours;

    private int otherfaulthours;

    private int computeroutrunhours;

    private int semurate;

    private int ermrate;

    private int tidestandardrate;

    private int lprrate;

    private int lpmarate;

    private int averagelpmarate;

    private String remarks;

    private String phonenumber;

    private String approver;

    private String sys_fille;

    private String sys_filldept;

    private java.sql.Timestamp sys_filltime;

    private int sys_isvalid;

    private String sys_dataowner;

    private String reportid;

    private int version;

    /**
     * getters and setters
     */
    public void setReportmonth(int reportmonth) {
        this.reportmonth = reportmonth;
    }

    public int getReportmonth() {
        return reportmonth;
    }

    public void setReportyear(int reportyear) {
        this.reportyear = reportyear;
    }

    public int getReportyear() {
        return reportyear;
    }

    public void setFstatus(String fstatus) {
        this.fstatus = fstatus;
    }

    public String getFstatus() {
        return fstatus;
    }

    public void setIsvalid(String isvalid) {
        this.isvalid = isvalid;
    }

    public String getIsvalid() {
        return isvalid;
    }

    public void setScheduleunit(String scheduleunit) {
        this.scheduleunit = scheduleunit;
    }

    public String getScheduleunit() {
        return scheduleunit;
    }

    public void setRtuarate(int rtuarate) {
        this.rtuarate = rtuarate;
    }

    public int getRtuarate() {
        return rtuarate;
    }

    public void setRtusrate(int rtusrate) {
        this.rtusrate = rtusrate;
    }

    public int getRtusrate() {
        return rtusrate;
    }

    public void setComputerrate(int computerrate) {
        this.computerrate = computerrate;
    }

    public int getComputerrate() {
        return computerrate;
    }

    public void setRscanum(int rscanum) {
        this.rscanum = rscanum;
    }

    public int getRscanum() {
        return rscanum;
    }

    public void setRseanum(int rseanum) {
        this.rseanum = rseanum;
    }

    public int getRseanum() {
        return rseanum;
    }

    public void setEddreportrate(int eddreportrate) {
        this.eddreportrate = eddreportrate;
    }

    public int getEddreportrate() {
        return eddreportrate;
    }

    public void setGpafrate(int gpafrate) {
        this.gpafrate = gpafrate;
    }

    public int getGpafrate() {
        return gpafrate;
    }

    public void setUpafrate(int upafrate) {
        this.upafrate = upafrate;
    }

    public int getUpafrate() {
        return upafrate;
    }

    public void setGridpcfrate(int gridpcfrate) {
        this.gridpcfrate = gridpcfrate;
    }

    public int getGridpcfrate() {
        return gridpcfrate;
    }

    public void setExchangeecfrate(int exchangeecfrate) {
        this.exchangeecfrate = exchangeecfrate;
    }

    public int getExchangeecfrate() {
        return exchangeecfrate;
    }

    public void setAgcrundate(int agcrundate) {
        this.agcrundate = agcrundate;
    }

    public int getAgcrundate() {
        return agcrundate;
    }

    public void setAgcrunrate(int agcrunrate) {
        this.agcrunrate = agcrunrate;
    }

    public int getAgcrunrate() {
        return agcrunrate;
    }

    public void setAgcctrlstandtime(int agcctrlstandtime) {
        this.agcctrlstandtime = agcctrlstandtime;
    }

    public int getAgcctrlstandtime() {
        return agcctrlstandtime;
    }

    public void setAgcctrlstandrate(int agcctrlstandrate) {
        this.agcctrlstandrate = agcctrlstandrate;
    }

    public int getAgcctrlstandrate() {
        return agcctrlstandrate;
    }

    public void setAgcctrlcps1(int agcctrlcps1) {
        this.agcctrlcps1 = agcctrlcps1;
    }

    public int getAgcctrlcps1() {
        return agcctrlcps1;
    }

    public void setAgcctrlcps2(int agcctrlcps2) {
        this.agcctrlcps2 = agcctrlcps2;
    }

    public int getAgcctrlcps2() {
        return agcctrlcps2;
    }

    public void setRtunum(int rtunum) {
        this.rtunum = rtunum;
    }

    public int getRtunum() {
        return rtunum;
    }

    public void setSfaultrepairhours(int sfaultrepairhours) {
        this.sfaultrepairhours = sfaultrepairhours;
    }

    public int getSfaultrepairhours() {
        return sfaultrepairhours;
    }

    public void setRtuspinterrupthours(int rtuspinterrupthours) {
        this.rtuspinterrupthours = rtuspinterrupthours;
    }

    public int getRtuspinterrupthours() {
        return rtuspinterrupthours;
    }

    public void setChannelinterrupthours(int channelinterrupthours) {
        this.channelinterrupthours = channelinterrupthours;
    }

    public int getChannelinterrupthours() {
        return channelinterrupthours;
    }

    public void setLinepowercuthours(int linepowercuthours) {
        this.linepowercuthours = linepowercuthours;
    }

    public int getLinepowercuthours() {
        return linepowercuthours;
    }

    public void setSfreoairhours(int sfreoairhours) {
        this.sfreoairhours = sfreoairhours;
    }

    public int getSfreoairhours() {
        return sfreoairhours;
    }

    public void setOtheroutrunhours(int otheroutrunhours) {
        this.otheroutrunhours = otheroutrunhours;
    }

    public int getOtheroutrunhours() {
        return otheroutrunhours;
    }

    public void setRtuoutrunallhours(int rtuoutrunallhours) {
        this.rtuoutrunallhours = rtuoutrunallhours;
    }

    public int getRtuoutrunallhours() {
        return rtuoutrunallhours;
    }

    public void setPsaveragefhour(int psaveragefhour) {
        this.psaveragefhour = psaveragefhour;
    }

    public int getPsaveragefhour() {
        return psaveragefhour;
    }

    public void setSfhours(int sfhours) {
        this.sfhours = sfhours;
    }

    public int getSfhours() {
        return sfhours;
    }

    public void setFsaveragefhour(int fsaveragefhour) {
        this.fsaveragefhour = fsaveragefhour;
    }

    public int getFsaveragefhour() {
        return fsaveragefhour;
    }

    public void setHceqpfhours(int hceqpfhours) {
        this.hceqpfhours = hceqpfhours;
    }

    public int getHceqpfhours() {
        return hceqpfhours;
    }

    public void setPowerinthours(int powerinthours) {
        this.powerinthours = powerinthours;
    }

    public int getPowerinthours() {
        return powerinthours;
    }

    public void setMaintainhours(int maintainhours) {
        this.maintainhours = maintainhours;
    }

    public int getMaintainhours() {
        return maintainhours;
    }

    public void setOtherfaulthours(int otherfaulthours) {
        this.otherfaulthours = otherfaulthours;
    }

    public int getOtherfaulthours() {
        return otherfaulthours;
    }

    public void setComputeroutrunhours(int computeroutrunhours) {
        this.computeroutrunhours = computeroutrunhours;
    }

    public int getComputeroutrunhours() {
        return computeroutrunhours;
    }

    public void setSemurate(int semurate) {
        this.semurate = semurate;
    }

    public int getSemurate() {
        return semurate;
    }

    public void setErmrate(int ermrate) {
        this.ermrate = ermrate;
    }

    public int getErmrate() {
        return ermrate;
    }

    public void setTidestandardrate(int tidestandardrate) {
        this.tidestandardrate = tidestandardrate;
    }

    public int getTidestandardrate() {
        return tidestandardrate;
    }

    public void setLprrate(int lprrate) {
        this.lprrate = lprrate;
    }

    public int getLprrate() {
        return lprrate;
    }

    public void setLpmarate(int lpmarate) {
        this.lpmarate = lpmarate;
    }

    public int getLpmarate() {
        return lpmarate;
    }

    public void setAveragelpmarate(int averagelpmarate) {
        this.averagelpmarate = averagelpmarate;
    }

    public int getAveragelpmarate() {
        return averagelpmarate;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getApprover() {
        return approver;
    }

    public void setSys_fille(String sys_fille) {
        this.sys_fille = sys_fille;
    }

    public String getSys_fille() {
        return sys_fille;
    }

    public void setSys_filldept(String sys_filldept) {
        this.sys_filldept = sys_filldept;
    }

    public String getSys_filldept() {
        return sys_filldept;
    }

    public void setSys_filltime(java.sql.Timestamp sys_filltime) {
        this.sys_filltime = sys_filltime;
    }

    public java.sql.Timestamp getSys_filltime() {
        return sys_filltime;
    }

    public void setSys_isvalid(int sys_isvalid) {
        this.sys_isvalid = sys_isvalid;
    }

    public int getSys_isvalid() {
        return sys_isvalid;
    }

    public void setSys_dataowner(String sys_dataowner) {
        this.sys_dataowner = sys_dataowner;
    }

    public String getSys_dataowner() {
        return sys_dataowner;
    }

    public void setReportid(String reportid) {
        this.reportid = reportid;
    }

    public String getReportid() {
        return reportid;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }
}
