package com.baldwin.www.common;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import com.baldwin.www.datahandler.*;

/**
 * ����������ݲ�����
 */
public class PropertyDAO extends BaseDAO {

    public PropertyDAO(DatabaseHandler dp) {
        super(dp);
    }

    public ArrayList<PropertyInfo> getList(long id, Map<PropertyInfo, ArrayList<PropertyInfo>> treeMap) {
        for (Iterator i = treeMap.keySet().iterator(); i.hasNext(); ) {
            PropertyInfo info = (PropertyInfo) i.next();
            if (info.getId() == id) return treeMap.get(info);
        }
        return null;
    }

    public ArrayList<PropertyInfo> getLevel2ProductList(String name, Map<PropertyInfo, ArrayList<PropertyInfo>> treeMap) {
        for (Iterator i = treeMap.keySet().iterator(); i.hasNext(); ) {
            PropertyInfo info = (PropertyInfo) i.next();
            if (name.startsWith(info.getName())) return treeMap.get(info);
        }
        return null;
    }

    public ArrayList<PropertyInfo> getLevel2MenuList(String name, Map<PropertyInfo, ArrayList<PropertyInfo>> treeMap) {
        for (Iterator i = treeMap.keySet().iterator(); i.hasNext(); ) {
            PropertyInfo info = (PropertyInfo) i.next();
            if (name.startsWith(info.getName())) return treeMap.get(info);
        }
        return null;
    }

    public ArrayList<PropertyInfo> getChannelList() {
        String sql = String.format("select DISTINCT s.ID as Id, s.sectionname as Name From Section s, SpotLight t where t.sectionid = s.ID and t.leadflag=1");
        ResultSet rs = dbh.executeQuery(sql);
        ArrayList<PropertyInfo> list = new ArrayList<PropertyInfo>();
        if (rs != null) {
            try {
                while (rs.next()) {
                    PropertyInfo Info = new PropertyInfo();
                    Info.setId(rs.getLong("Id"));
                    Info.setName(transform(rs.getString("Name")));
                    list.add(Info);
                    Info = null;
                }
                rs.close();
                return list;
            } catch (Exception e) {
                printStackTrace(e);
                return list;
            }
        }
        return list;
    }
}
