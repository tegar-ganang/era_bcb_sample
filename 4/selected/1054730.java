package com.pn.be;

public class DanhMucOptionBean {

    private String tenDanhMuc;

    private String read;

    private String write;

    private Long id;

    public DanhMucOptionBean(Long id, String tenDanhMuc, String read, String write) {
        this.id = id;
        this.tenDanhMuc = tenDanhMuc;
        this.read = read;
        this.write = write;
    }

    public void setTenDanhMuc(String tenDanhMuc) {
        this.tenDanhMuc = tenDanhMuc;
    }

    public String getTenDanhMuc() {
        return tenDanhMuc;
    }

    public void setRead(String read) {
        this.read = read;
    }

    public String getRead() {
        return read;
    }

    public void setWrite(String write) {
        this.write = write;
    }

    public String getWrite() {
        return write;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
