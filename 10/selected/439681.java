package com.wangyu001.util.main;

import static org.sysolar.util.Constants.LS;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.sysolar.util.file.FileIO;
import com.wangyu001.entity.CoreUrl;
import com.wangyu001.entity.Domain;
import com.wangyu001.entity.UserUrl;
import com.wangyu001.helper.UserUrlHelper;

public class DataRepair {

    public static void main(String[] args) {
        repairUserUrlHref();
    }

    public static void repairUserUrlHref() {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append("select USER_URL_ID ").append(LS);
        buffer.append("      ,URL_HREF    ").append(LS);
        buffer.append("  from USER_URL    ").append(LS);
        buffer.append(" where CORE_URL_ID is null").append(LS);
        buffer.append(" order by URL_HREF desc").append(LS);
        List<UserUrl> userUrlList = new ArrayList<UserUrl>(200);
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConnHelper.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(buffer.toString());
            while (rs.next()) {
                userUrlList.add(new UserUrl().setUrlHref(rs.getString("URL_HREF")).setUserUrlId(rs.getLong("USER_URL_ID")));
            }
            ConnHelper.close(null, stmt, rs);
            String sql = "select CORE_URL_ID, DOMAIN_ID from CORE_URL where CORE_URL_HREF = ?";
            pstmt = conn.prepareStatement(sql);
            Long domainId;
            for (UserUrl userUrl : userUrlList) {
                userUrl = UserUrlHelper.check(userUrl);
                domainId = userUrl.getDomainId();
                if (null == userUrl || (null != domainId && domainId == Domain.NATIVE_DOMAIN_ID)) {
                    continue;
                }
                pstmt.setString(1, userUrl.getCoreUrl().getCoreUrlHref());
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    userUrl.setCoreUrlId(rs.getLong("CORE_URL_ID")).setDomainId(rs.getLong("DOMAIN_ID"));
                }
                ConnHelper.close(null, null, rs);
            }
            sql = "update USER_URL set CORE_URL_ID = ?, DOMAIN_ID = ? where USER_URL_ID = ?";
            pstmt = conn.prepareStatement(sql);
            for (UserUrl userUrl : userUrlList) {
                if (null == userUrl.getCoreUrlId() || null == userUrl.getDomainId()) {
                    System.out.println(userUrl.getUserUrlId() + " " + userUrl.getUrlHref());
                    continue;
                }
                pstmt.setLong(1, userUrl.getCoreUrlId());
                pstmt.setLong(2, userUrl.getDomainId());
                pstmt.setLong(3, userUrl.getUserUrlId());
                pstmt.executeUpdate();
            }
            System.out.println(userUrlList.size());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, pstmt, rs);
        }
    }

    /**
     * 修复网站标题，只需标题，不包含网站介绍。
     */
    private static void repairCoreUrlName() {
        String sql = "select CORE_URL_ID, CORE_URL_NAME from CORE_URL";
        List<CoreUrl> coreUrlList = new ArrayList<CoreUrl>(20000);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConnHelper.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                coreUrlList.add(new CoreUrl().setCoreUrlId(rs.getLong("CORE_URL_ID")).setCoreUrlName(rs.getString("CORE_URL_NAME")));
            }
            String[] arr;
            String coreUrlName;
            for (CoreUrl coreUrl : coreUrlList) {
                coreUrlName = coreUrl.getCoreUrlName();
                if (null == coreUrlName) {
                    continue;
                }
                coreUrlName = coreUrlName.replaceAll("欢迎访问", "").replaceAll("网站", "");
                arr = coreUrlName.split("[－\\-]");
                if (arr.length > 1) {
                    if (arr[0].length() < 3) {
                        coreUrlName = arr[0].trim() + " - " + arr[1].trim();
                    } else {
                        coreUrlName = arr[0].trim();
                    }
                }
                coreUrl.setCoreUrlName(coreUrlName);
            }
            sql = "update CORE_URL set CORE_URL_NAME = ? where CORE_URL_ID = ?";
            pstmt = conn.prepareStatement(sql);
            for (CoreUrl coreUrl : coreUrlList) {
                pstmt.setString(1, coreUrl.getCoreUrlName());
                pstmt.setLong(2, coreUrl.getCoreUrlId());
                pstmt.executeUpdate();
                System.out.println(coreUrl.getCoreUrlName());
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, pstmt, rs);
        }
    }

    private static void removeInvalidPicture() {
        String dir = "E:/Business/5i56/bak/web/img4url/mid";
        StringBuilder buffer = new StringBuilder(256);
        List<File> picList = FileIO.listFilesInDirSubdirs(dir);
        for (File pic : picList) {
            if (pic.length() == 5041 || pic.length() == 6121) {
                pic.delete();
                continue;
            }
            buffer.append("update CORE_URL set CORE_URL_IMG_TYPE = 1 where CORE_URL_ID = ").append(pic.getName().substring(0, pic.getName().indexOf('.'))).append(";").append(LS);
        }
        File f = new File("E:/Business/5i56/bak/sql/", "UPDATE_CORE_URL_IMG_TYPE.sql");
        FileIO.writeToFile(f, buffer.toString(), false, "UTF-8");
    }

    /**
     *  启动10个线程，循环往 DOMAIN 表执行插入、查询操作。
     */
    public static void testAutoIncrement() {
        final int count = 3;
        final Object lock = new Object();
        for (int i = 0; i < count; i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        StringBuilder buffer = new StringBuilder(128);
                        buffer.append("insert into DOMAIN (                         ").append(LS);
                        buffer.append("    DOMAIN_ID, TOP_DOMAIN_ID, DOMAIN_HREF,   ").append(LS);
                        buffer.append("    DOMAIN_RANK, DOMAIN_TYPE, DOMAIN_STATUS, ").append(LS);
                        buffer.append("    DOMAIN_ICO_CREATED, DOMAIN_CDATE         ").append(LS);
                        buffer.append(") values (                   ").append(LS);
                        buffer.append("    null ,null, ?,").append(LS);
                        buffer.append("    1, 2, 1,                 ").append(LS);
                        buffer.append("    0, now()                 ").append(LS);
                        buffer.append(")                            ").append(LS);
                        String sqlInsert = buffer.toString();
                        boolean isAutoCommit = false;
                        int i = 0;
                        Connection conn = null;
                        PreparedStatement pstmt = null;
                        ResultSet rs = null;
                        try {
                            conn = ConnHelper.getConnection();
                            conn.setAutoCommit(isAutoCommit);
                            pstmt = conn.prepareStatement(sqlInsert);
                            for (i = 0; i < 10; i++) {
                                String lock = "" + ((int) (Math.random() * 100000000)) % 100;
                                pstmt.setString(1, lock);
                                pstmt.executeUpdate();
                            }
                            if (!isAutoCommit) conn.commit();
                            rs = pstmt.executeQuery("select max(DOMAIN_ID) from DOMAIN");
                            if (rs.next()) {
                                String str = System.currentTimeMillis() + " " + rs.getLong(1);
                            }
                        } catch (Exception e) {
                            try {
                                if (!isAutoCommit) conn.rollback();
                            } catch (SQLException ex) {
                                ex.printStackTrace(System.out);
                            }
                            String msg = System.currentTimeMillis() + " " + Thread.currentThread().getName() + " - " + i + " " + e.getMessage() + LS;
                            FileIO.writeToFile("D:/DEAD_LOCK.txt", msg, true, "GBK");
                        } finally {
                            ConnHelper.close(conn, pstmt, rs);
                        }
                    }
                }
            }).start();
        }
    }

    private static final class Sql {

        private static StringBuilder buffer = new StringBuilder(128);

        public static final String GET_USER_URL;

        static {
            buffer.append("select USER_URL_ID ").append(LS);
            buffer.append("      ,URL_HREF    ").append(LS);
            buffer.append("  from USER_URL    ").append(LS);
            buffer.append(" where CORE_URL_ID is null").append(LS);
            buffer.append(" order by URL_HREF desc").append(LS);
            GET_USER_URL = buffer.toString();
        }
    }
}
