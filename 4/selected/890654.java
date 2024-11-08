package org.jpos.ee;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;

/** @author Hibernate CodeGenerator */
public class SysConfig implements Serializable {

    /** identifier field */
    private String id;

    /** nullable persistent field */
    private String value;

    /** nullable persistent field */
    private String readPerm;

    /** nullable persistent field */
    private String writePerm;

    /** full constructor */
    public SysConfig(String id, String value, String readPerm, String writePerm) {
        this.id = id;
        this.value = value;
        this.readPerm = readPerm;
        this.writePerm = writePerm;
    }

    /** default constructor */
    public SysConfig() {
    }

    /** minimal constructor */
    public SysConfig(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReadPerm() {
        return this.readPerm;
    }

    public void setReadPerm(String readPerm) {
        this.readPerm = readPerm;
    }

    public String getWritePerm() {
        return this.writePerm;
    }

    public void setWritePerm(String writePerm) {
        this.writePerm = writePerm;
    }

    public String toString() {
        return new ToStringBuilder(this).append("id", getId()).toString();
    }
}
