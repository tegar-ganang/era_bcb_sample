package org.osmius.service.impl;

import org.osmius.dao.*;
import org.osmius.dao.jdbc.UtilsDaoJDBC;
import org.osmius.model.*;
import org.osmius.service.OsmInterfaceManager;
import org.osmius.service.exceptions.OsmTypeventExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class OsmInterfaceManagerImpl extends BaseManager implements OsmInterfaceManager {

    private OsmInterfaceDao dao;

    private OsmTypeventDao osmTypeventDao;

    private OsmTypeventsTemplateDao osmTypeventsTemplateDao;

    private OsmInterfaceUsereventsDao osmInterfaceUsereventsDao;

    private OsmInstanceEventDao osmInstanceEventDao;

    private OsmUserscriptsDao osmUserscriptsDao;

    private OsmInterfaceTypinstDao osmInterfaceTypinstDao;

    private OsmInstanceDao osmInstanceDao;

    private OsmTaskDao osmTaskDao;

    private UtilsDaoJDBC utilsDaoJDBC;

    private ResourceBundleMessageSource messageSource;

    public void setUtilsDaoJDBC(UtilsDaoJDBC utilsDaoJDBC) {
        this.utilsDaoJDBC = utilsDaoJDBC;
    }

    public void setOsmInstanceDao(OsmInstanceDao osmInstanceDao) {
        this.osmInstanceDao = osmInstanceDao;
    }

    public void setOsmTaskDao(OsmTaskDao osmTaskDao) {
        this.osmTaskDao = osmTaskDao;
    }

    public void setOsmUserscriptsDao(OsmUserscriptsDao osmUserscriptsDao) {
        this.osmUserscriptsDao = osmUserscriptsDao;
    }

    public void setOsmInterfaceTypinstDao(OsmInterfaceTypinstDao osmInterfaceTypinstDao) {
        this.osmInterfaceTypinstDao = osmInterfaceTypinstDao;
    }

    public void setOsmTypeventsTemplateDao(OsmTypeventsTemplateDao osmTypeventsTemplateDao) {
        this.osmTypeventsTemplateDao = osmTypeventsTemplateDao;
    }

    public void setOsmInterfaceUsereventsDao(OsmInterfaceUsereventsDao osmInterfaceUsereventsDao) {
        this.osmInterfaceUsereventsDao = osmInterfaceUsereventsDao;
    }

    public void setOsmInstanceEventDao(OsmInstanceEventDao osmInstanceEventDao) {
        this.osmInstanceEventDao = osmInstanceEventDao;
    }

    public void setOsmTypeventDao(OsmTypeventDao osmTypeventDao) {
        this.osmTypeventDao = osmTypeventDao;
    }

    public void setOsmInterfaceDao(OsmInterfaceDao dao) {
        this.dao = dao;
    }

    public List getOsmInterfaces(OsmInterface osmInterface) {
        return dao.getOsmInterfaces(osmInterface);
    }

    public OsmInterface getOsmInterface(String idnInterface) {
        return dao.getOsmInterface(idnInterface);
    }

    public void saveInterfacesEventsData(OsmTypevent[] arrOsmTypevent, OsmTypeventsTemplate[] arrOsmTypeventsTemplate, OsmInterfaceUserevents[] arrOsmInterfaceUserevents, OsmInstanceEvent[] arrDelIns, OsmInstanceEvent[] arrAddIns, String script, String pathFile) throws OsmTypeventExistsException {
        if (arrOsmTypevent != null) {
            for (int i = 0; i < arrOsmTypevent.length; i++) {
                OsmTypevent osmTypevent = arrOsmTypevent[i];
                try {
                    osmTypeventDao.saveOsmTypevent(osmTypevent);
                } catch (DataIntegrityViolationException e) {
                    throw new OsmTypeventExistsException(osmTypevent.getId().getTypEvent());
                }
            }
        }
        if (arrOsmTypeventsTemplate != null) {
            for (int i = 0; i < arrOsmTypeventsTemplate.length; i++) {
                OsmTypeventsTemplate osmTypeventsTemplate = arrOsmTypeventsTemplate[i];
                osmTypeventsTemplateDao.saveOrUpdateOsmTypeventsTemplate(osmTypeventsTemplate);
            }
        }
        if (arrOsmInterfaceUserevents != null) {
            for (int i = 0; i < arrOsmInterfaceUserevents.length; i++) {
                OsmInterfaceUserevents osmInterfaceUserevent = arrOsmInterfaceUserevents[i];
                if (osmInterfaceUserevent.getId().getIdnInterface().equals("T")) {
                    osmInterfaceTypinstDao.saveOrUpdate(osmInterfaceUserevent.getId().getTypInstance(), osmInterfaceUserevent.getId().getIdnInterface());
                }
                osmInterfaceUsereventsDao.saveOrUpdateOsmInterfaceUserevents(osmInterfaceUserevent);
            }
        }
        if (arrDelIns != null) {
            for (int i = 0; i < arrDelIns.length; i++) {
                OsmInstanceEvent delIns = arrDelIns[i];
                osmInstanceEventDao.removeOsmInstanceEvent(osmInstanceEventDao.getOsmInstanceEvent(delIns.getId().getTypInstance(), delIns.getId().getIdnInstance(), delIns.getId().getTypEvent()));
            }
        }
        if (arrAddIns != null) {
            for (int i = 0; i < arrAddIns.length; i++) {
                OsmInstanceEvent addIns = arrAddIns[i];
                osmInstanceEventDao.saveOrUpdateOsmInstanceEvent(addIns);
            }
        }
        if (arrOsmInterfaceUserevents != null) {
            if ("P".equals(arrOsmInterfaceUserevents[0].getId().getIdnInterface())) {
                OsmUserscripts osmUserscripts = osmUserscriptsDao.getOsmUserscriptById(script);
                byte[] binScript = osmUserscripts.getBinScript();
                try {
                    long time = new Date().getTime();
                    String path = pathFile;
                    new File(path + File.separator + time).mkdirs();
                    FileOutputStream fos = new FileOutputStream(path + File.separator + time + File.separator + script + ".zip");
                    fos.write(binScript);
                    fos.flush();
                    fos.close();
                    unzip(new File(path + File.separator + time + File.separator + script + ".zip"));
                    File oldFile = new File(path + File.separator + time + File.separator + "user" + File.separator + "scripts" + File.separator + script);
                    File newFile = new File(path + File.separator + time + File.separator + "user" + File.separator + "scripts" + File.separator + (((OsmTypevent) arrOsmTypevent[0]).getId().getTypEvent() + (script.indexOf(".") != -1 ? script.substring(script.indexOf(".")) : "")));
                    oldFile.renameTo(newFile);
                    new File(path + File.separator + time + File.separator + script + ".zip").delete();
                    long time2 = time + 1;
                    new File(path + File.separator + time2).mkdirs();
                    zipDirectory(new File(path + File.separator + time), new File(path + File.separator + time2 + File.separator + (((OsmTypevent) arrOsmTypevent[0]).getId().getTypEvent()) + ".zip"));
                    byte[] file = getBytesFromFile(new File(path + File.separator + time2 + File.separator + (((OsmTypevent) arrOsmTypevent[0]).getId().getTypEvent()) + ".zip"));
                    OsmUserscripts oldScript = osmUserscriptsDao.getOsmUserscript(script);
                    OsmUserscripts finalScript = new OsmUserscripts();
                    finalScript.setTxtScript((((OsmTypevent) arrOsmTypevent[0]).getId().getTypEvent() + (script.indexOf(".") != -1 ? script.substring(script.indexOf(".")) : "")));
                    finalScript.setDesScript(oldScript.getDesScript());
                    finalScript.setTxtCommand(oldScript.getTxtCommand().replace(script, (((OsmTypevent) arrOsmTypevent[0]).getId().getTypEvent() + (script.indexOf(".") != -1 ? script.substring(script.indexOf(".")) : ""))));
                    finalScript.setBinScript(file);
                    osmUserscriptsDao.renameOsmUserscripts(oldScript, finalScript);
                    deleteDir(new File(path + File.separator + time));
                    deleteDir(new File(path + File.separator + time2));
                } catch (FileNotFoundException ex) {
                    System.out.println(ex);
                } catch (IOException ioe) {
                    System.out.println(ioe);
                }
            }
        }
    }

    public void saveInterfacesEventsData(OsmTypevent[] arrOsmTypevent, OsmTypeventsTemplate[] arrOsmTypeventsTemplate, OsmInterfaceUserevents[] arrOsmInterfaceUserevents, OsmInstanceEvent[] arrDelIns, OsmInstanceEvent[] arrAddIns, String script, String path, String[] instancesToApply) throws OsmTypeventExistsException {
        saveInterfacesEventsData(arrOsmTypevent, arrOsmTypeventsTemplate, arrOsmInterfaceUserevents, arrDelIns, arrAddIns, script, path);
        for (int i = 0; i < instancesToApply.length; i++) {
            String instance = instancesToApply[i];
            OsmInstance osmInstance = osmInstanceDao.getOsmInstance(instance);
            Set agentInstances = osmInstance.getOsmAgentInstances();
            if (agentInstances.size() > 0) {
                for (Iterator iterator = agentInstances.iterator(); iterator.hasNext(); ) {
                    OsmAgentInstance osmAgentInstance = (OsmAgentInstance) iterator.next();
                    OsmTask task = new OsmTask();
                    task.setOsmMasteragent(osmAgentInstance.getOsmAgent().getOsmMasteragent());
                    task.setOsmTypagent(osmAgentInstance.getOsmAgent().getOsmTypagent());
                    task.setIndState(new Integer(0));
                    task.setDtiExecute(new Date(utilsDaoJDBC.getActualTimestamp().getTime()));
                    task.setNumRetries(new Integer(0));
                    OsmTyptask osmTyptask = new OsmTyptask();
                    osmTyptask.setTypTask(new Integer(1));
                    task.setOsmTyptask(osmTyptask);
                    task.setOsmInstance(osmInstance);
                    osmTaskDao.saveTask(task);
                }
            }
        }
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

    private void zipDir(String zipFileName, String dir) {
        File dirObj = new File(dir);
        if (!dirObj.isDirectory()) {
            System.err.println(dir + " is not a directory");
        }
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
            addDir(dirObj, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void zipFiles(String uploadDir, String dir, String file, String zipfile) {
        new File("." + File.separator + dir + File.separator + "user" + File.separator + "scripts").mkdirs();
        NioCopier nioCopier = new NioCopier();
        try {
            nioCopier.copy(new File(uploadDir + File.separator + file), new File("." + File.separator + dir + File.separator + "user" + File.separator + "scripts" + File.separator + file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        zipDir(zipfile, dir);
    }

    private void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDir(files[i], out);
                continue;
            }
            String dir = dirObj.toString();
            dir = dir.substring(dir.indexOf(File.separator) + 1);
            FileInputStream in = new FileInputStream(dirObj + File.separator + files[i].getName());
            out.putNextEntry(new ZipEntry(dir + File.separator + files[i].getName()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
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

    private void zipDirectory(File directory, File zip) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
        zip(directory, directory, zos);
        zos.close();
    }

    private void zip(File directory, File base, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                zip(files[i], base, zos);
            } else {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))) {
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
    }
}
