package org.osmius.service.impl;

import org.osmius.service.OsmAgentManager;
import org.osmius.service.UtilsManager;
import org.osmius.service.OsmTypplatformTypagentManager;
import org.osmius.model.OsmAgent;
import org.osmius.model.OsmTypplatformTypagent;
import org.osmius.dao.OsmAgentDao;
import org.osmius.dao.OsmTaskDao;
import org.osmius.dao.OsmMasteragentDao;
import org.osmius.dao.jdbc.UtilsDaoJDBC;
import java.util.List;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.*;
import java.nio.channels.FileChannel;
import java.sql.SQLException;

/**
 * @see org.osmius.service.OsmAgentManager
 */
public class OsmAgentManagerImpl extends BaseManager implements OsmAgentManager {

    private OsmAgentDao dao;

    private OsmMasteragentDao osmMasteragentDao;

    private OsmTaskDao osmTaskDao;

    private UtilsDaoJDBC utilsDaoJDBC;

    OsmTypplatformTypagentManager osmTypplatformTypagentManager;

    public void setOsmTypplatformTypagentManager(OsmTypplatformTypagentManager osmTypplatformTypagentManager) {
        this.osmTypplatformTypagentManager = osmTypplatformTypagentManager;
    }

    public void setUtilsDaoJDBC(UtilsDaoJDBC utilsDaoJDBC) {
        this.utilsDaoJDBC = utilsDaoJDBC;
    }

    public void setOsmMasteragentDao(OsmMasteragentDao osmMasteragentDao) {
        this.osmMasteragentDao = osmMasteragentDao;
    }

    public void setOsmTaskDao(OsmTaskDao osmTaskDao) {
        this.osmTaskDao = osmTaskDao;
    }

    /**
    * Sets an agent dao - <a href="http://www.springframework.org">Spring</a> IoC
    */
    public void setOsmAgentDao(OsmAgentDao dao) {
        this.dao = dao;
    }

    /**
    * @see org.osmius.service.OsmAgentManager#getOsmAgent(String,String)
    */
    public OsmAgent getOsmAgent(String idnMaster, String typAgent) {
        return dao.getOsmAgent(idnMaster, typAgent);
    }

    /**
    * @see org.osmius.service.OsmAgentManager#getOsmAgents(org.osmius.model.OsmAgent)
    */
    public List getOsmAgents(OsmAgent osmAgent) {
        return dao.getOsmAgents(osmAgent);
    }

    /**
    * @see org.osmius.service.OsmAgentManager#getOsmAgentsByMaster(String)
    */
    public List getOsmAgentsByMaster(String idnMaster) {
        return dao.getOsmAgentsByMaster(idnMaster);
    }

    /**
    * @see org.osmius.service.OsmAgentManager#saveOsmAgents(org.osmius.model.OsmAgent[])
    */
    public void saveOsmAgents(OsmAgent[] osmAgents) {
        dao.saveOsmAgents(osmAgents);
    }

    public boolean startAG(String masterAgent, String agent) {
        boolean value = true;
        try {
            dao.startAG(masterAgent, agent);
            OsmAgent osmAgent = dao.getOsmAgent(masterAgent, agent);
            if (osmAgent != null) {
                osmTaskDao.startAG(osmAgent);
            }
        } catch (Exception e) {
            value = false;
        }
        return value;
    }

    public boolean stopAG(String masterAgent, String agent) {
        boolean value = true;
        try {
            dao.stopAG(masterAgent, agent);
            OsmAgent osmAgent = dao.getOsmAgent(masterAgent, agent);
            if (osmAgent != null) {
                osmTaskDao.stopAG(osmAgent);
            }
        } catch (Exception e) {
            value = false;
        }
        return value;
    }

    public List getAgentsStateByPlatform() {
        return dao.getAgentsStateByPlatform();
    }

    public String[] deployAgent(String fileName, String uploadDir, String imagesDir) throws SQLException {
        long time = new Date().getTime();
        String[] platforms = null;
        String dir = "tmp" + time;
        new File("." + File.separator + dir + File.separator).mkdirs();
        NioCopier nioCopier = new NioCopier();
        try {
            nioCopier.copy(new File(uploadDir + File.separator + fileName), new File("." + File.separator + dir + File.separator + fileName));
            unzip(new File("." + File.separator + dir + File.separator + fileName));
            try {
                parseSQLfile(new StringBuilder().append(".").append(File.separator).append(dir).append(File.separator).append(fileName.substring(0, fileName.indexOf("."))).append(".sql").toString());
            } catch (SQLException e) {
                deleteDir(new File("." + File.separator + dir));
                throw e;
            }
            String[] gifFiles = filterFiles("." + File.separator + dir, "gif");
            nioCopier.copy(new File("." + File.separator + dir + File.separator + gifFiles[0]), new File(imagesDir + File.separator + gifFiles[0]));
            String[] zipFiles = filterFiles("." + File.separator + dir, "zip");
            Vector vPlatfomrs = new Vector();
            for (int i = 0; i < zipFiles.length; i++) {
                String zipFile = zipFiles[i];
                if (!zipFile.equals(fileName)) {
                    byte[] file = getBytesFromFile(new File("." + File.separator + dir + File.separator + zipFile));
                    String typAgent = zipFile.substring(0, zipFile.indexOf("_"));
                    String typPlatform = zipFile.substring(zipFile.indexOf("_") + 1, zipFile.indexOf("."));
                    OsmTypplatformTypagent osmTypplatformTypagent = osmTypplatformTypagentManager.getOsmTypplatformTypagent(typAgent, typPlatform);
                    osmTypplatformTypagent.setBinAgent(file);
                    osmTypplatformTypagentManager.saveOrUpdateOsmTypplatformTypagent(osmTypplatformTypagent);
                    vPlatfomrs.add(zipFile);
                }
            }
            platforms = new String[vPlatfomrs.size()];
            vPlatfomrs.toArray(platforms);
        } catch (IOException e) {
            e.printStackTrace();
        }
        deleteDir(new File("." + File.separator + dir));
        return platforms;
    }

    private void unzip(File f) throws IOException {
        ZipFile zip;
        zip = new ZipFile(f);
        Enumeration e = zip.entries();
        while (e.hasMoreElements()) {
            ZipEntry zen = (ZipEntry) e.nextElement();
            if (zen.isDirectory()) {
                continue;
            }
            int size = (int) zen.getSize();
            InputStream zis = zip.getInputStream(zen);
            String extractfile = f.getParentFile().getAbsolutePath() + File.separator + zen.getName();
            writeFile(zis, new File(extractfile), size);
            zis.close();
        }
        zip.close();
    }

    private void writeFile(InputStream zis, File file, int size) throws IOException {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            byte[] byteStream = new byte[(int) size];
            int buf = -1;
            int rb = 0;
            while ((((int) size - rb) > 0)) {
                buf = zis.read(byteStream, rb, (int) size - rb);
                if (buf == -1) {
                    break;
                }
                rb += buf;
            }
            fos.write(byteStream);
        } catch (IOException e) {
            throw new IOException("UNZIP_ERROR");
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File fileList[] = dir.listFiles();
            for (int index = 0; index < fileList.length; index++) {
                File file = fileList[index];
                deleteDir(file);
            }
        }
        dir.delete();
    }

    private class NioCopier {

        public void copy(File s, File t) throws IOException {
            FileChannel in = (new FileInputStream(s)).getChannel();
            FileChannel out = (new FileOutputStream(t)).getChannel();
            in.transferTo(0, s.length(), out);
            in.close();
            out.close();
        }
    }

    private class OnlyExt implements FilenameFilter {

        String ext;

        public OnlyExt(String ext) {
            this.ext = "." + ext;
        }

        public boolean accept(File dir, String name) {
            return name.endsWith(ext);
        }
    }

    private void parseSQLfile(String file) throws SQLException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuffer sqlBuf = new StringBuffer();
        String line;
        boolean statementReady = false;
        int count = 0;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("--") || line.length() == 0) {
                    continue;
                } else if (line.endsWith(";")) {
                    sqlBuf.append(' ');
                    sqlBuf.append(line.substring(0, line.length() - 1));
                    statementReady = true;
                } else {
                    sqlBuf.append(' ');
                    sqlBuf.append(line);
                    statementReady = false;
                }
                if (statementReady) {
                    if (sqlBuf.length() == 0) continue;
                    try {
                        utilsDaoJDBC.executeUpdate(sqlBuf.toString());
                    } catch (SQLException e) {
                        throw e;
                    }
                    count++;
                    sqlBuf.setLength(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] filterFiles(String dir, String ext) {
        File f = new File(dir);
        FilenameFilter ff = new OnlyExt(ext);
        return f.list(ff);
    }

    private byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            return null;
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }
}
