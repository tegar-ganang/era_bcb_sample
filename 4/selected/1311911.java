package com.bugfree4j.domain;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;

/** @author Hibernate CodeGenerator */
public class SysGeneralright implements Serializable {

    /** identifier field */
    private Integer id;

    /** nullable persistent field */
    private String read;

    /** nullable persistent field */
    private String write;

    /** nullable persistent field */
    private String print;

    /** nullable persistent field */
    private String memo;

    /** persistent field */
    private com.bugfree4j.domain.SysGroup sysGroup;

    /** persistent field */
    private com.bugfree4j.domain.SysModule sysModule;

    /** full constructor */
    public SysGeneralright(Integer id, String read, String write, String print, String memo, com.bugfree4j.domain.SysGroup sysGroup, com.bugfree4j.domain.SysModule sysModule) {
        this.id = id;
        this.read = read;
        this.write = write;
        this.print = print;
        this.memo = memo;
        this.sysGroup = sysGroup;
        this.sysModule = sysModule;
    }

    /** default constructor */
    public SysGeneralright() {
    }

    /** minimal constructor */
    public SysGeneralright(Integer id, com.bugfree4j.domain.SysGroup sysGroup, com.bugfree4j.domain.SysModule sysModule) {
        this.id = id;
        this.sysGroup = sysGroup;
        this.sysModule = sysModule;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRead() {
        return this.read;
    }

    public void setRead(String read) {
        this.read = read;
    }

    public String getWrite() {
        return this.write;
    }

    public void setWrite(String write) {
        this.write = write;
    }

    public String getPrint() {
        return this.print;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public String getMemo() {
        return this.memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public com.bugfree4j.domain.SysGroup getSysGroup() {
        return this.sysGroup;
    }

    public void setSysGroup(com.bugfree4j.domain.SysGroup sysGroup) {
        this.sysGroup = sysGroup;
    }

    public com.bugfree4j.domain.SysModule getSysModule() {
        return this.sysModule;
    }

    public void setSysModule(com.bugfree4j.domain.SysModule sysModule) {
        this.sysModule = sysModule;
    }

    public String toString() {
        return new ToStringBuilder(this).append("id", getId()).toString();
    }
}
