package examples.webservices.attachment.statelessSession;

import java.io.Serializable;

/**
 * This class reflects the results of a Attachment transaction.
 *
 * @author Copyright (c) 1999-2004 by BEA Systems, Inc. All Rights Reserved.
 */
public final class AttachmentResult implements Serializable {

    private long retCode;

    private String retCodeDescription;

    private String action;

    private String mime_type;

    private String object_id;

    private String object_id_on_storage;

    private String storage_system_id;

    private long bytes_write;

    private long bytes_read;

    private String[] meta_on_db;

    private String[] meta_on_storage;

    public AttachmentResult() {
    }

    public AttachmentResult(long retCode, String retCodeDescription, String action, String mime_type, String object_id, String object_id_on_storage, String storage_system_id, long bytes_write, long bytes_read, String[] meta_on_db, String[] meta_on_storage) {
        this.retCode = retCode;
        this.retCodeDescription = retCodeDescription;
        this.action = action;
        this.mime_type = mime_type;
        this.object_id = object_id;
        this.object_id_on_storage = object_id_on_storage;
        this.storage_system_id = storage_system_id;
        this.bytes_write = bytes_write;
        this.bytes_read = bytes_read;
        this.meta_on_db = meta_on_db;
        this.meta_on_storage = meta_on_storage;
    }

    public long Get_retCode() {
        this.retCode = retCode;
    }

    public String Get_retCodeDescription() {
        this.retCodeDescription = retCodeDescription;
    }

    public String Get_action() {
        this.action = action;
    }

    public String Get_mime_type() {
        this.mime_type = mime_type;
    }

    public String Get_object_id() {
        this.object_id = object_id;
    }

    public String Get_object_id_on_storage() {
        this.object_id_on_storage = object_id_on_storage;
    }

    public String Get_storage_system_id() {
        this.storage_system_id = storage_system_id;
    }

    public long Get_bytes_write() {
        this.bytes_write = bytes_write;
    }

    public long Get_bytes_read() {
        this.bytes_read = bytes_read;
    }

    public String[] Get_meta_on_db() {
        this.meta_on_db = meta_on_db;
    }

    public String[] Get_meta_on_storage() {
        this.meta_on_storage = meta_on_storage;
    }

    public String toString() {
        String msg = "";
        msg = msg + "retCode=" + this.retCode + "=, ";
        msg = msg + "this.retCodeDescription=" + this.retCodeDescription + "=, ";
        msg = msg + "this.action=" + this.action + "=, ";
        msg = msg + "this.mime_type=" + this.mime_type + "=, ";
        msg = msg + "this.object_id=" + this.object_id + "=, ";
        msg = msg + "this.object_id_on_storage=" + this.object_id_on_storage + "=, ";
        msg = msg + "this.storage_system_id=" + this.storage_system_id + "=, ";
        msg = msg + "this.bytes_write=" + this.bytes_write + "=, ";
        msg = msg + "this.bytes_read=" + this.bytes_read + "=, ";
        msg = msg + "this.meta_on_db=" + this.meta_on_db + "=, ";
        msg = msg + "this.meta_on_storage=" + this.meta_on_storage + "=";
        return (msg);
    }
}
