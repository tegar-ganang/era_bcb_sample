package com.etc.bin.beans;

import com.etc.bin.base.Constant;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author MaGicBank
 */
public class UserLoginBeans implements Serializable {

    private static final long serialVersionUID = 2L;

    private int id;

    private String code;

    private String name;

    private int permission;

    private int locale;

    private String read;

    private String write;

    private String execute;

    private String store;

    private String progroup;

    public UserLoginBeans() {
    }

    public UserLoginBeans(int id, String code, String name, int permission, int locale, String read, String write, String execute, String store, String progroup) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.permission = permission;
        this.locale = locale;
        this.read = read;
        this.write = write;
        this.execute = execute;
        this.store = store;
        this.progroup = progroup;
    }

    private boolean getAccess(String page, int attribute) {
        page = ":" + page + ":";
        if (this.getPermission() >= Constant.ADMINISTRATOR) {
            return true;
        } else if (this.getPermission() <= Constant.OFFICER) {
            return false;
        } else {
            if (attribute == Constant.ACCESS_READ) {
                return this.getRead().contains(page);
            } else if (attribute == Constant.ACCESS_WRITE) {
                return this.getWrite().contains(page);
            } else if (attribute == Constant.ACCESS_EXECUTE) {
                return this.getExecute().contains(page);
            } else {
                return false;
            }
        }
    }

    public List<String> getStoreAccess() {
        List<String> list = new ArrayList<String>();
        String[] data = StringUtils.removeStartIgnoreCase(StringUtils.removeEndIgnoreCase(getStore(), ":"), ":").split(":");
        for (int i = 0; i < data.length; i++) {
            if (data[i] != null) {
                list.add(new String(data[i]));
            }
        }
        return list;
    }

    public Set<String> getProductGroupAccess() {
        Set<String> list = new HashSet<String>();
        String[] data = StringUtils.removeStartIgnoreCase(StringUtils.removeEndIgnoreCase(getProgroup(), ":"), ":").split(":");
        for (int i = 0; i < data.length; i++) {
            list.add(new String(data[i]));
        }
        return list;
    }

    public boolean r(String page) {
        return this.getAccess(page, Constant.ACCESS_READ);
    }

    public boolean w(String page) {
        return this.getAccess(page, Constant.ACCESS_WRITE);
    }

    public boolean x(String page) {
        return this.getAccess(page, Constant.ACCESS_EXECUTE);
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the permission
     */
    public int getPermission() {
        return permission;
    }

    /**
     * @param permission the permission to set
     */
    public void setPermission(int permission) {
        this.permission = permission;
    }

    /**
     * @return the locale
     */
    public int getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(int locale) {
        this.locale = locale;
    }

    /**
     * @return the read
     */
    public String getRead() {
        return read;
    }

    /**
     * @param read the read to set
     */
    public void setRead(String read) {
        this.read = read;
    }

    /**
     * @return the write
     */
    public String getWrite() {
        return write;
    }

    /**
     * @param write the write to set
     */
    public void setWrite(String write) {
        this.write = write;
    }

    /**
     * @return the execute
     */
    public String getExecute() {
        return execute;
    }

    /**
     * @param execute the execute to set
     */
    public void setExecute(String execute) {
        this.execute = execute;
    }

    /**
     * @return the store
     */
    public String getStore() {
        return store;
    }

    /**
     * @param store the store to set
     */
    public void setStore(String store) {
        this.store = store;
    }

    /**
     * @return the progroup
     */
    public String getProgroup() {
        return progroup;
    }

    /**
     * @param progroup the progroup to set
     */
    public void setProgroup(String progroup) {
        this.progroup = progroup;
    }
}
