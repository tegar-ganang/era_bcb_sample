package com.luzan.app.map.tool;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.hibernate.criterion.Expression;
import com.luzan.app.map.bean.user.UserMapOriginal;
import com.luzan.app.map.utils.Configuration;
import com.luzan.db.dao.GenericDAO;
import com.luzan.db.dao.DAOFactory;
import com.luzan.db.DBConnectionFactoryImpl;
import com.luzan.db.ReadOnlyTransaction;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;

/**
 * TileCollector
 *
 * @author Alexander Bondar
 */
public class TileCollector {

    private static final Logger logger = Logger.getLogger(TileCollector.class);

    protected String cfg;

    protected String mixMapGuid;

    protected String tileCode;

    protected File outDir;

    public void setTileCode(String tileCode) {
        this.tileCode = tileCode;
    }

    public void setMixMapGuid(String mixMapGuid) {
        this.mixMapGuid = mixMapGuid;
    }

    public void setOutDir(String outDir) {
        this.outDir = new File(outDir);
    }

    public void setCfg(String cfg) {
        this.cfg = cfg;
    }

    private void doIt() throws Throwable {
        GenericDAO<UserMapOriginal> dao = DAOFactory.createDAO(UserMapOriginal.class);
        try {
            ReadOnlyTransaction.beginTransaction();
        } catch (Throwable e) {
            logger.error(e);
            throw e;
        }
        try {
            UserMapOriginal map = dao.findUniqueByCriteria(Expression.eq("guid", mixMapGuid));
            final File srcDir = new File(Configuration.getInstance().getPrivateMapStorage().toString());
            for (UserMapOriginal m : map.getSubmaps()) {
                final File mapDir = new File(srcDir, m.getGuid());
                if (mapDir.exists() && mapDir.isDirectory()) {
                    for (String fileName : mapDir.list()) {
                        File file = new File(mapDir, fileName);
                        if (file.isFile() && file.exists() && file.canRead()) {
                            while (fileName.indexOf('.') > 0) {
                                fileName = fileName.split("\\.")[0];
                            }
                            if (fileName.indexOf('_') > 0) {
                                if (tileCode.equalsIgnoreCase(fileName)) {
                                    FileUtils.copyFile(file, new File(outDir.getAbsolutePath(), m.getGuid() + ".png"));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Error importing", e);
        } finally {
            ReadOnlyTransaction.closeTransaction();
        }
    }

    public static void main(String args[]) {
        TileCollector mixer = new TileCollector();
        String allArgs = StringUtils.join(args, ' ');
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(TileCollector.class, Object.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Pattern p = Pattern.compile("-" + pd.getName() + "\\s*([\\S]*)", Pattern.CASE_INSENSITIVE);
                final Matcher m = p.matcher(allArgs);
                if (m.find()) {
                    pd.getWriteMethod().invoke(mixer, m.group(1));
                }
            }
            Configuration.getInstance().load(mixer.cfg);
            DBConnectionFactoryImpl.configure(mixer.cfg);
            mixer.doIt();
        } catch (Throwable e) {
            logger.error("error", e);
            System.out.println(e.getMessage());
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(TileCollector.class);
                System.out.println("Options:");
                for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                    System.out.println("-" + pd.getName());
                }
            } catch (Throwable t) {
                System.out.print("Internal error");
            }
        }
    }
}
