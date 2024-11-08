package ces.sf.oa.util;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.*;
import java.io.*;
import ces.coffice.common.affix.dao.imp.AffixDaoImp;
import ces.coffice.common.affix.dao.AffixResource;
import ces.platform.system.common.IdGenerator;
import ces.coffice.common.affix.dao.imp.AffixUploadDao;
import ces.coffice.common.base.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

public class OAConfig {

    private static final Map map = new HashMap();

    static {
        String path = ces.coral.file.CesGlobals.getCesHome() + "/orgmgr.properties";
        if (new File(path).exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(path);
                java.util.Properties p = new java.util.Properties();
                p.load(fin);
                Enumeration e = p.keys();
                while (e.hasMoreElements()) {
                    String id = (String) e.nextElement();
                    String key = p.getProperty(id);
                    if (key != null) {
                        map.put(id, key.split(","));
                    }
                }
                fin.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public OAConfig() {
    }

    /**
     * ���configĿ¼��·��
     * @return String
     */
    public static String getConfigFilePath() {
        return ces.coral.file.CesGlobals.getCesHome();
    }

    /**
     * huode
     * @return String
     */
    public static String getContentFilePath() {
        return ces.coral.file.CesGlobals.getCesHome() + "/../docs/";
    }

    /**
     * ��������
     * @param id1 long
     * @param id2 long
     * @param type String
     */
    public static int copyAffix(long id1, long id2, String type) {
        AffixDaoImp impl = new AffixDaoImp();
        String touploadPath = AffixUploadDao.DATA_BASE_PATH + AffixUploadDao.DOCRECEIVE_DP;
        String fromuploadPath = AffixUploadDao.DATA_BASE_PATH + AffixUploadDao.JOBLOG_DP;
        if (!new File(touploadPath).exists()) {
            new java.io.File(touploadPath).mkdirs();
        }
        try {
            AffixResource[] rs = impl.getResourcesByCondition("ORG_ID=" + id1);
            for (int i = 0; i < rs.length; i++) {
                rs[i].setOrgType(type);
                rs[i].setResType("1");
                rs[i].setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_COFFICE_RES));
                rs[i].setOrgId((int) id2);
                impl.add(rs[i]);
                try {
                    String file1 = fromuploadPath + File.separator + Function.getNYofDate(rs[i].getCreateDate()) + File.separator + rs[i].getUri();
                    String file2 = touploadPath + File.separator + Function.getNYofDate(rs[i].getCreateDate()) + File.separator + rs[i].getUri();
                    copyFile(file1, file2);
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }
            }
            return rs.length;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    /**
     * ��������
     * @param id1 long
     * @param id2 long
     */
    public static void copyContent(long id1, long id2) {
        String file1 = getContentFilePath() + "doc_" + id1;
        String file2 = getContentFilePath() + "doc_" + id2;
        copyFile(file1, file2);
    }

    /**
     * ��������2
     * @param id1 String
     * @param id2 String
     */
    public static void copyContent(String id1, String id2) {
        String file1 = getContentFilePath() + "doc_" + id1;
        String file2 = "v:/" + "doc_" + id2;
        copyFile(file1, file2);
    }

    /**
     * ����֪ͨ�������HTML
     * @param id1
     * @param id2
     */
    public static void copyHtml(String id1, String id2) {
        String file1 = "d://wjh-file//sfxmn//oa//html//oatzgg/" + id1;
        String file2 = "k:/" + id2;
        copyFile(file1, file2);
    }

    public static void copyFile(String file1, String file2) {
        File filedata1 = new java.io.File(file1);
        if (filedata1.exists()) {
            try {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file2));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file1));
                try {
                    int read;
                    while ((read = in.read()) != -1) {
                        out.write(read);
                    }
                    out.flush();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    out.close();
                    in.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String[] getChartDepartmentIds(String userId) {
        if (map.containsKey(userId)) {
            return (String[]) map.get(userId);
        }
        return new String[0];
    }
}
