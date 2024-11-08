package android.bluebox.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.util.Log;

public class DatabaseBox {

    private static final String DATABASE_NAME = "bluebox.db";

    private static final int DATABASE_VERSION = 56;

    private static final String WORKSPACE = "workspace";

    private static final String WS_ID = "workspaceID";

    private static final String WS_NAME = "workspaceName";

    private static final String WS_IP = "workspaceIP";

    private static final String WS_GPS_LONG = "workspaceGPSLong";

    private static final String WS_GPS_LAT = "workspaceGPSLatt";

    private static final String LAST_VISITED = "lastVisited";

    private static final String TAG = "tag";

    private static final String TAG_ID = "tagID";

    private static final String TAG_NAME = "tagName";

    private static final String ATTRIBUTE = "attribute";

    private static final String ATTR_ID = "attributeID";

    private static final String ATTR_NAME = "attributeName";

    private static final String AVALUE = "avalue";

    private static final String AVALUE_ID = "avalueID";

    private static final String AVALUE_NAME = "avalueName";

    private static final String SYNONYMS = "synonyms";

    private static final String SYN_NAME = "synonymsName";

    private static final String WS_AVALUE = "workspace_avalue";

    private static final String TAG_AVALUE = "tag_avalue";

    private static final String COUNT = "count";

    private static final String LIST_IP = "list_ip";

    private static final String IP = "ip";

    private static final String SYSTEM = "system";

    private static final String S_ID = "id";

    private static final String S_REMIND = "remind";

    private static final String S_HASH = "hash";

    private static final String S_KEY = "key";

    private Context context;

    private SQLiteDatabase db;

    private SqliteHelper openHelper;

    private String[] workspaceColumns = { WS_ID, WS_NAME, WS_IP, WS_GPS_LAT, WS_GPS_LONG, LAST_VISITED };

    private String[] tagColumns = { TAG_ID, TAG_NAME };

    private String[] attributeColumns = { ATTR_ID, ATTR_NAME };

    private String[] avalueColumns = { AVALUE_ID, ATTR_ID, AVALUE_NAME };

    private String[] tagAValueColumns = { TAG_ID, AVALUE_ID, COUNT, LAST_VISITED };

    private String[] workspaceAValueColumns = { WS_ID, AVALUE_ID, COUNT, LAST_VISITED };

    private String[] systemColums = { S_ID, S_REMIND, S_HASH, S_KEY };

    private String[] synonymsColums = { ATTR_ID, SYN_NAME };

    public DatabaseBox(Context context) {
        this.context = context;
        openHelper = new SqliteHelper(this.context);
    }

    public void setContext(Context context) {
        this.context = context;
        openHelper = new SqliteHelper(this.context);
    }

    public void insertWorkspace(Workspace ws) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WS_NAME, ws.getName());
        cv.put(WS_IP, ws.getHostIP());
        cv.put(WS_GPS_LONG, ws.getLongitude());
        cv.put(WS_GPS_LAT, ws.getLatitude());
        cv.put(LAST_VISITED, ws.getLastVisited());
        this.db.insert(WORKSPACE, null, cv);
        db.close();
    }

    public void updateWorkspace(Workspace ws) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WS_NAME, ws.getName());
        cv.put(WS_IP, ws.getHostIP());
        cv.put(WS_GPS_LONG, ws.getLongitude());
        cv.put(WS_GPS_LAT, ws.getLatitude());
        cv.put(LAST_VISITED, ws.getLastVisited());
        this.db.update(WORKSPACE, cv, WS_ID + "=?", new String[] { String.valueOf(ws.getId()) });
        db.close();
    }

    public void deleteWorkspace(int id) {
        this.db = openHelper.getWritableDatabase();
        this.db.delete(WORKSPACE, WS_ID + "=?", new String[] { String.valueOf(id) });
        this.db.close();
    }

    public long getNumberOfWorkspaces() {
        this.db = openHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + WORKSPACE;
        SQLiteStatement stmt = db.compileStatement(sql);
        return stmt.simpleQueryForLong();
    }

    public Workspace getWorkspace(int id) {
        this.db = openHelper.getReadableDatabase();
        Cursor cursor = this.db.query(WORKSPACE, workspaceColumns, WS_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if (cursor.moveToFirst()) {
            Workspace ws = new Workspace();
            ws.setId(Integer.parseInt(cursor.getString(0)));
            ws.setName(cursor.getString(1));
            ws.setHostIP(cursor.getString(2));
            if (cursor.getFloat(3) > 0 && cursor.getFloat(4) > 0) ws.setLocation(cursor.getFloat(3), cursor.getFloat(4));
            ws.setLastVisited(cursor.getString(5));
            cursor.close();
            db.close();
            return ws;
        } else {
            db.close();
            return null;
        }
    }

    public Workspace getWorkspace(String name) {
        this.db = openHelper.getReadableDatabase();
        Cursor cursor = this.db.query(WORKSPACE, workspaceColumns, WS_NAME + "=?", new String[] { name }, null, null, null);
        if (cursor.moveToFirst()) {
            Workspace ws = new Workspace();
            ws.setId(Integer.parseInt(cursor.getString(0)));
            ws.setName(cursor.getString(1));
            ws.setHostIP(cursor.getString(2));
            if (cursor.getFloat(3) > 0 && cursor.getFloat(4) > 0) ws.setLocation(cursor.getFloat(3), cursor.getFloat(4));
            ws.setLastVisited(cursor.getString(5));
            cursor.close();
            db.close();
            return ws;
        } else {
            db.close();
            return null;
        }
    }

    public ArrayList<Workspace> getAllWorkspaces() {
        ArrayList<Workspace> list = new ArrayList<Workspace>();
        this.db = openHelper.getReadableDatabase();
        Cursor cursor = this.db.query(WORKSPACE, workspaceColumns, null, null, null, null, WS_NAME + " desc");
        if (cursor.moveToFirst()) {
            do {
                Workspace ws = new Workspace();
                ws.setId(Integer.parseInt(cursor.getString(0)));
                ws.setName(cursor.getString(1));
                ws.setHostIP(cursor.getString(2));
                if (cursor.getFloat(3) > 0 && cursor.getFloat(4) > 0) ws.setLocation(cursor.getFloat(3), cursor.getFloat(4));
                ws.setLastVisited(cursor.getString(5));
                list.add(ws);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return list;
    }

    public void insertTag(Tag tag) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TAG_NAME, tag.getName());
        this.db.insert(TAG, null, cv);
        db.close();
    }

    public void updateTag(Tag tag) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TAG_NAME, tag.getName());
        this.db.update(TAG, cv, TAG_ID + "=?", new String[] { String.valueOf(tag.getId()) });
        db.close();
    }

    public void deleteTag(int id) {
        this.db = openHelper.getWritableDatabase();
        this.db.delete(TAG, TAG_ID + "=?", new String[] { String.valueOf(id) });
        this.db.close();
    }

    public long getNumberOfTags() {
        db = openHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + TAG;
        SQLiteStatement stmt = db.compileStatement(sql);
        return stmt.simpleQueryForLong();
    }

    public Tag getTag(int id) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(TAG, tagColumns, TAG_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if (cursor.moveToFirst()) {
            Tag tag = new Tag();
            tag.setId(id);
            tag.setName(cursor.getString(1));
            cursor.close();
            db.close();
            return tag;
        } else {
            db.close();
            return null;
        }
    }

    public Tag getTag(String name) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(TAG, tagColumns, TAG_NAME + "=?", new String[] { String.valueOf(name) }, null, null, null);
        if (cursor.moveToFirst()) {
            Tag tag = new Tag();
            tag.setId(Integer.parseInt(cursor.getString(0)));
            tag.setName(name);
            cursor.close();
            db.close();
            return tag;
        } else {
            db.close();
            return null;
        }
    }

    public ArrayList<Tag> getAllTags() {
        ArrayList<Tag> list = new ArrayList<Tag>();
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(TAG, tagColumns, null, null, null, null, TAG_NAME + " desc");
        if (cursor.moveToFirst()) {
            do {
                Tag tag = new Tag();
                tag.setId(Integer.parseInt(cursor.getString(0)));
                tag.setName(cursor.getString(1));
                list.add(tag);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return list;
    }

    public void insertAttribute(Attribute attribute) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(ATTR_NAME, attribute.getName());
        this.db.insert(ATTRIBUTE, null, cv);
        db.close();
        updateSynonyms(getAttribute(attribute.getName()).getId(), attribute.getName());
    }

    public void updateAttribute(Attribute attribute) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(ATTR_NAME, attribute.getName());
        this.db.update(ATTRIBUTE, cv, ATTR_ID + "=?", new String[] { String.valueOf(attribute.getId()) });
        db.close();
    }

    public void deleteAttribute(int id) {
        this.db = openHelper.getWritableDatabase();
        this.db.delete(ATTRIBUTE, ATTR_ID + "=?", new String[] { String.valueOf(id) });
        this.db.close();
    }

    public long getNumberOfAttributes() {
        db = openHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + ATTRIBUTE;
        SQLiteStatement stmt = db.compileStatement(sql);
        return stmt.simpleQueryForLong();
    }

    public Attribute getAttribute(int id) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(ATTRIBUTE, attributeColumns, ATTR_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if (cursor.moveToFirst()) {
            Attribute attribute = new Attribute();
            attribute.setId(id);
            attribute.setName(cursor.getString(1));
            cursor.close();
            db.close();
            return attribute;
        } else {
            db.close();
            return null;
        }
    }

    public Attribute getAttribute(String name) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(ATTRIBUTE, attributeColumns, ATTR_NAME + "=?", new String[] { String.valueOf(name) }, null, null, null);
        if (cursor.moveToFirst()) {
            Attribute attribute = new Attribute();
            attribute.setId(Integer.parseInt(cursor.getString(0)));
            attribute.setName(name);
            cursor.close();
            db.close();
            return attribute;
        } else {
            db.close();
            return null;
        }
    }

    public ArrayList<Attribute> getAllAttributes() {
        ArrayList<Attribute> list = new ArrayList<Attribute>();
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(ATTRIBUTE, attributeColumns, null, null, null, null, ATTR_NAME + " desc");
        if (cursor.moveToFirst()) {
            do {
                Attribute attribute = new Attribute();
                attribute.setId(Integer.parseInt(cursor.getString(0)));
                attribute.setName(cursor.getString(1));
                list.add(attribute);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return list;
    }

    public void insertAValue(AValue aValue) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AVALUE_NAME, aValue.getName());
        cv.put(ATTR_ID, aValue.getAttrId());
        this.db.insert(AVALUE, null, cv);
        insertTagAValue(aValue);
    }

    public void updateAValue(AValue aValue) {
        this.db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(AVALUE_NAME, aValue.getName());
        cv.put(ATTR_ID, aValue.getAttrId());
        this.db.update(AVALUE, cv, AVALUE_ID + "=?", new String[] { String.valueOf(aValue.getId()) });
    }

    public void deleteAValue(int id) {
        this.db = openHelper.getWritableDatabase();
        this.db.delete(AVALUE, AVALUE_ID + "=?", new String[] { String.valueOf(id) });
        this.db.delete(TAG_AVALUE, AVALUE_ID + "=?", new String[] { String.valueOf(id) });
        this.db.delete(WS_AVALUE, AVALUE_ID + "=?", new String[] { String.valueOf(id) });
        this.db.close();
    }

    public long getNumberOfAValues() {
        db = openHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + AVALUE;
        SQLiteStatement stmt = db.compileStatement(sql);
        return stmt.simpleQueryForLong();
    }

    public AValue getAValue(int id) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(AVALUE, avalueColumns, AVALUE_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if (cursor.moveToFirst()) {
            AValue aValue = new AValue();
            aValue.setId(id);
            aValue.setAttrId(Integer.parseInt(cursor.getString(1)));
            aValue.setName(cursor.getString(2));
            cursor.close();
            db.close();
            aValue.setTagIdList(getTagIdList(id));
            return aValue;
        } else {
            db.close();
            return null;
        }
    }

    public AValue getAValue(String name) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(AVALUE, avalueColumns, AVALUE_NAME + "=?", new String[] { String.valueOf(name) }, null, null, null);
        if (cursor.moveToFirst()) {
            AValue aValue = new AValue();
            int id = Integer.parseInt(cursor.getString(0));
            aValue.setId(id);
            aValue.setAttrId(Integer.parseInt(cursor.getString(1)));
            aValue.setName(name);
            cursor.close();
            aValue.setTagIdList(getTagIdList(id));
            db.close();
            return aValue;
        } else {
            db.close();
            return null;
        }
    }

    public ArrayList<AValue> getAllAValues(int attrId) {
        ArrayList<AValue> list = new ArrayList<AValue>();
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(AVALUE, avalueColumns, ATTR_ID + "=?", new String[] { String.valueOf(attrId) }, null, null, AVALUE_NAME + " desc");
        if (cursor.moveToFirst()) {
            do {
                AValue aValue = new AValue();
                int id = Integer.parseInt(cursor.getString(0));
                aValue.setId(id);
                aValue.setAttrId(attrId);
                aValue.setName(cursor.getString(2));
                aValue.setTagIdList(getTagIdList(id));
                list.add(aValue);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return list;
    }

    public ArrayList<AValue> getAllAValues() {
        ArrayList<AValue> list = new ArrayList<AValue>();
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(AVALUE, avalueColumns, null, null, null, null, AVALUE_NAME + " desc");
        if (cursor.moveToFirst()) {
            do {
                AValue aValue = new AValue();
                int id = cursor.getInt(0);
                aValue.setId(id);
                aValue.setAttrId(cursor.getInt(1));
                aValue.setName(cursor.getString(2));
                list.add(aValue);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return list;
    }

    public ArrayList<AValue> getAllAValuesExceptPassword() {
        ArrayList<AValue> list = new ArrayList<AValue>();
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(AVALUE, avalueColumns, null, null, null, null, AVALUE_NAME + " desc");
        if (cursor.moveToFirst()) {
            do {
                int attrId = cursor.getInt(1);
                if (getAttribute(attrId) != null) {
                    String attrName = getAttribute(attrId).getName();
                    if (!attrName.equals("password")) {
                        AValue aValue = new AValue();
                        int id = cursor.getInt(0);
                        aValue.setId(id);
                        aValue.setAttrId(cursor.getInt(1));
                        aValue.setName(cursor.getString(2));
                        aValue.setTagIdList(getTagIdList(id));
                        list.add(aValue);
                    }
                }
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return list;
    }

    public String getSynonyms(int attrId) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(SYNONYMS, synonymsColums, ATTR_ID + "=?", new String[] { String.valueOf(attrId) }, null, null, null);
        String synonyms = "";
        if (cursor.moveToFirst()) {
            synonyms = cursor.getString(1);
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return synonyms;
    }

    public void updateSynonyms(int attrId, String synonyms) {
        db = openHelper.getWritableDatabase();
        db.delete(SYNONYMS, ATTR_ID + "=?", new String[] { String.valueOf(attrId) });
        ContentValues cv = new ContentValues();
        cv.put(ATTR_ID, attrId);
        cv.put(SYN_NAME, synonyms);
        db.insert(SYNONYMS, null, cv);
        db.close();
    }

    public void updateSynonyms(String attrName, String synonyms) {
        Attribute attr = getAttribute(attrName);
        if (attr == null) return;
        int attrId = attr.getId();
        updateSynonyms(attrId, synonyms);
    }

    public int getSynonyms(String synonyms) {
        synonyms = synonyms.toLowerCase();
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(SYNONYMS, synonymsColums, SYN_NAME + " LIKE '%" + synonyms + " %'", null, null, null, null);
        int attrId = -1;
        if (cursor.moveToFirst()) {
            attrId = cursor.getInt(0);
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        db.close();
        return attrId;
    }

    public void insertTagAValue(AValue aValue) {
        db = openHelper.getWritableDatabase();
        if (aValue.getTagIdList() != null) for (int id : aValue.getTagIdList()) {
            Tag tag = getTag(id);
            if (tag != null) {
                ContentValues cv = new ContentValues();
                cv.put(TAG_ID, id);
                cv.put(AVALUE_ID, aValue.getId());
                cv.put(COUNT, -1);
                cv.put(LAST_VISITED, "never logged");
                this.db.insert(TAG_AVALUE, null, cv);
            }
        }
        db.close();
    }

    public void insertTagAValue(int tagId, int aValueId) {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TAG_ID, tagId);
        cv.put(AVALUE_ID, aValueId);
        cv.put(COUNT, -1);
        db.insert(TAG_AVALUE, null, cv);
        db.close();
    }

    public void deleteTagAValue(int tagId, int aValueId) {
        db = openHelper.getWritableDatabase();
        db.delete(TAG_AVALUE, TAG_ID + "=? AND " + AVALUE_ID + "=?", new String[] { String.valueOf(tagId), String.valueOf(aValueId) });
        db.close();
    }

    public boolean checkExistanceOfTagAValue(int tagId, int aValueId) {
        db = openHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + ATTRIBUTE + " WHERE " + TAG_ID + "=" + tagId + " AND " + AVALUE_ID + "=" + aValueId;
        SQLiteStatement stmt = db.compileStatement(sql);
        return stmt.simpleQueryForLong() > 0;
    }

    public ArrayList<Integer> getTagIdList(int aValueId) {
        db = openHelper.getReadableDatabase();
        ArrayList<Integer> list = new ArrayList<Integer>();
        Cursor cursor = db.query(TAG_AVALUE, tagAValueColumns, AVALUE_ID + "=?", new String[] { String.valueOf(aValueId) }, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int tagId = Integer.parseInt(cursor.getString(0));
                list.add(tagId);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public ArrayList<Tag> getAValueTagList(int id) {
        ArrayList<Tag> list = new ArrayList<Tag>();
        ArrayList<Integer> idList = getTagIdList(id);
        for (int tid : idList) {
            Tag tag = getTag(tid);
            list.add(tag);
        }
        return list;
    }

    public ArrayList<AValue> filterAValueByTag(int tagId) {
        db = openHelper.getReadableDatabase();
        ArrayList<AValue> list = new ArrayList<AValue>();
        Cursor cursor = db.query(TAG_AVALUE, tagAValueColumns, TAG_ID + "=?", new String[] { String.valueOf(tagId) }, null, null, COUNT + " desc");
        if (cursor.moveToFirst()) {
            do {
                int aValueId = Integer.parseInt(cursor.getString(1));
                list.add(getAValue(aValueId));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public ArrayList<AValue> filterAValueByTagExceptPassword(int tagId) {
        db = openHelper.getReadableDatabase();
        ArrayList<AValue> list = new ArrayList<AValue>();
        Cursor cursor = db.query(TAG_AVALUE, tagAValueColumns, TAG_ID + "=?", new String[] { String.valueOf(tagId) }, null, null, COUNT + " desc");
        if (cursor.moveToFirst()) {
            do {
                int aValueId = cursor.getInt(1);
                AValue avalue = getAValue(aValueId);
                if (getAttribute(avalue.getAttrId()).getName() == "password") {
                    continue;
                }
                list.add(getAValue(aValueId));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public ArrayList<AValue> filterAValueByTag(String tagName) {
        if (getTag(tagName) != null) {
            int tagId = getTag(tagName).getId();
            return filterAValueByTag(tagId);
        }
        return null;
    }

    public ArrayList<AValue> filterAValueByWorkspace(int wsId) {
        db = openHelper.getReadableDatabase();
        ArrayList<AValue> list = new ArrayList<AValue>();
        Cursor cursor = db.query(WS_AVALUE, workspaceAValueColumns, WS_ID + "=?", new String[] { String.valueOf(wsId) }, null, null, COUNT + " desc");
        if (cursor.moveToFirst()) {
            do {
                int aValueId = Integer.parseInt(cursor.getString(1));
                list.add(getAValue(aValueId));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public void insertWorkspaceAValue(int wsId, int aValueId) {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WS_ID, wsId);
        cv.put(AVALUE_ID, aValueId);
        cv.put(COUNT, 0);
        db.insert(WS_AVALUE, null, cv);
        db.close();
    }

    public void deleteWorkspaceAValue(int wsId, int aValueId) {
        db = openHelper.getWritableDatabase();
        db.delete(WS_AVALUE, WS_ID + "=? AND " + AVALUE_ID + "=?", new String[] { String.valueOf(wsId), String.valueOf(aValueId) });
        db.close();
    }

    public ArrayList<Integer> getWorkspaceIdList(int id) {
        db = openHelper.getReadableDatabase();
        ArrayList<Integer> list = new ArrayList<Integer>();
        Cursor cursor = db.query(WS_AVALUE, workspaceAValueColumns, AVALUE_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int wsId = Integer.parseInt(cursor.getString(0));
                list.add(wsId);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public ArrayList<Workspace> getAValueWorkspaceList(int id) {
        ArrayList<Workspace> list = new ArrayList<Workspace>();
        ArrayList<Integer> idList = getWorkspaceIdList(id);
        for (int wid : idList) {
            Workspace ws = getWorkspace(wid);
            list.add(ws);
        }
        return list;
    }

    public void updateWorkspaceAValue(int aValueId) {
        int wsId = StaticBox.currentWorkspaceId;
        int count = getCountWorkspaceAValue(aValueId);
        if (wsId < 0) return;
        if (count < 0) {
            insertWorkspaceAValue(wsId, aValueId);
        } else {
            db = openHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COUNT, count + 1);
            cv.put(LAST_VISITED, StaticBox.getCurrentDate());
            this.db.update(WS_AVALUE, cv, WS_ID + "=? AND " + AVALUE_ID + "=?", new String[] { String.valueOf(wsId), String.valueOf(aValueId) });
            db.close();
        }
    }

    public int getCountWorkspaceAValue(int wsId, int aValueId) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(WS_AVALUE, workspaceAValueColumns, WS_ID + "=? AND " + AVALUE_ID + "=?", new String[] { String.valueOf(wsId), String.valueOf(aValueId) }, null, null, null);
        int count = -1;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(2);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return count;
    }

    public int getCountWorkspaceAValue(int aValueId) {
        return getCountWorkspaceAValue(StaticBox.currentWorkspaceId, aValueId);
    }

    public String getLastVisitedWorkspaceAValue(int aValueId) {
        return getLastVisitedWorkspaceAValue(StaticBox.currentWorkspaceId, aValueId);
    }

    public String getLastVisitedWorkspaceAValue(int wsId, int aValueId) {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(WS_AVALUE, workspaceAValueColumns, WS_ID + "=? AND " + AVALUE_ID + "=?", new String[] { String.valueOf(wsId), String.valueOf(aValueId) }, null, null, null);
        String lastVisited = "never logged";
        if (cursor.moveToFirst()) {
            lastVisited = cursor.getString(3);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return lastVisited;
    }

    public void insertIpIntoList(String ip) {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(IP, ip);
        db.insert(LIST_IP, null, cv);
        db.close();
    }

    public CharSequence[] loadListIp() {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + LIST_IP, null);
        CharSequence[] list = new CharSequence[cursor.getCount()];
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                list[i++] = cursor.getString(0);
            } while (cursor.moveToNext());
        }
        return list;
    }

    public void createSystemEmptyRecord() {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(S_ID, "1");
        db.insert(SYSTEM, null, cv);
        db.close();
    }

    public String getRemind() {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(SYSTEM, systemColums, S_ID + " = 1", null, null, null, null);
        String strRemind = null;
        if (cursor.moveToFirst()) strRemind = cursor.getString(1);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return strRemind;
    }

    public void updateSystemRemind(String remind) {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(S_REMIND, remind);
        db.update(SYSTEM, cv, S_ID + " = 1", null);
        db.close();
    }

    public void updateSystemHash(String hash) {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(S_HASH, hash);
        db.update(SYSTEM, cv, S_ID + "=?", new String[] { "1" });
        db.close();
    }

    public String getSystemHash() {
        db = openHelper.getReadableDatabase();
        Cursor cursor = db.query(SYSTEM, systemColums, S_ID + " = 1", null, null, null, null);
        String strHash = null;
        if (cursor.moveToFirst()) strHash = cursor.getString(2);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return strHash;
    }

    public void updateSystemKey(String key) {
        db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(S_KEY, key);
        db.update(SYSTEM, cv, S_ID + "=?", new String[] { "1" });
        db.close();
    }

    public String getSystemKey() {
        db = openHelper.getReadableDatabase();
        String sql = "SELECT " + S_KEY + " FROM " + SYSTEM + " WHERE " + S_ID + " = 1";
        SQLiteStatement stmt = db.compileStatement(sql);
        return stmt.simpleQueryForString();
    }

    public boolean backup() {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            if (sd.canWrite()) {
                String currentDBPath = "/data/android.bluebox/databases/bluebox.db";
                String backupDBPath = "/Android/bluebox.bak";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean restore() {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            if (sd.canWrite()) {
                String currentDBPath = "/Android/bluebox.bak";
                String backupDBPath = "/data/android.bluebox/databases/bluebox.db";
                File currentDB = new File(sd, currentDBPath);
                File backupDB = new File(data, backupDBPath);
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class SqliteHelper extends SQLiteOpenHelper {

        public SqliteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + WORKSPACE + " (" + WS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + WS_NAME + " TEXT, " + WS_IP + " TEXT, " + WS_GPS_LAT + " FLOAT, " + WS_GPS_LONG + " FLOAT, " + LAST_VISITED + " TEXT);");
            db.execSQL("CREATE TABLE " + TAG + " (" + TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TAG_NAME + " TEXT);");
            db.execSQL("CREATE TABLE " + ATTRIBUTE + " (" + ATTR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ATTR_NAME + " TEXT);");
            createTableAValue(db);
            db.execSQL("CREATE TABLE " + SYNONYMS + " (" + ATTR_ID + " INTEGER PRIMARY KEY, " + SYN_NAME + " TEXT);");
            createTableWorkspaceAValue(db);
            createTableTagAValue(db);
            createTableListIp(db);
            db.execSQL("CREATE TABLE " + SYSTEM + " (" + S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + S_REMIND + " TEXT, " + S_HASH + " TEXT, " + S_KEY + " TEXT);");
        }

        public void createTableAValue(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + AVALUE + " (" + AVALUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ATTR_ID + " INTEGER, " + AVALUE_NAME + " TEXT);");
        }

        public void createTableWorkspaceAValue(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + WS_AVALUE + " (" + WS_ID + " INTEGER, " + AVALUE_ID + " INTEGER, " + COUNT + " INTEGER, " + LAST_VISITED + " INTEGER, " + "PRIMARY KEY (" + WS_ID + ", " + AVALUE_ID + "));");
        }

        public void createTableTagAValue(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TAG_AVALUE + " (" + TAG_ID + " INTEGER, " + AVALUE_ID + " INTEGER, " + COUNT + " INTEGER, " + LAST_VISITED + " INTEGER, " + "PRIMARY KEY (" + TAG_ID + ", " + AVALUE_ID + "));");
        }

        public void createTableListIp(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + LIST_IP + " (" + IP + " TEXT PRIMARY KEY);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("blueBox", "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS " + WORKSPACE);
            db.execSQL("DROP TABLE IF EXISTS " + TAG);
            db.execSQL("DROP TABLE IF EXISTS " + ATTRIBUTE);
            db.execSQL("DROP TABLE IF EXISTS " + AVALUE);
            db.execSQL("DROP TABLE IF EXISTS " + SYNONYMS);
            db.execSQL("DROP TABLE IF EXISTS " + WS_AVALUE);
            db.execSQL("DROP TABLE IF EXISTS " + TAG_AVALUE);
            db.execSQL("DROP TABLE IF EXISTS " + SYSTEM);
            db.execSQL("DROP TABLE IF EXISTS " + LIST_IP);
            onCreate(db);
        }
    }
}
