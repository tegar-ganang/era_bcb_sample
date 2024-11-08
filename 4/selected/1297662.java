package ch.arpage.collaboweb.services.actions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.ServletRequestUtils;
import ch.arpage.collaboweb.exceptions.ActionException;
import ch.arpage.collaboweb.model.BinaryAttribute;
import ch.arpage.collaboweb.model.Resource;
import ch.arpage.collaboweb.model.User;
import ch.arpage.collaboweb.services.ResourceManager;

/**
 * Returns a ZIP File containing all the files whose resourceId was given
 * as request parameter
 *
 * @author <a href="mailto:patrick@arpage.ch">Patrick Herber</a>
 */
public class FileZipAction implements Action {

    private ResourceManager resourceManager;

    /**
	 * Set the resourceManager.
	 * @param resourceManager the resourceManager to set
	 */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public String execute(HttpServletRequest request, HttpServletResponse response, User user, String parameter) throws Exception {
        long[] files = ServletRequestUtils.getLongParameters(request, "resourceId");
        if (files != null && files.length > 0) {
            try {
                response.addHeader("Accept-Ranges", "bytes");
                response.addHeader("Content-Disposition", "attachment; filename=\"collaboweb.zip\"");
                response.addHeader("Cache-Control", "private");
                response.setContentType("application/octet-stream");
                ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
                zos.setMethod(ZipOutputStream.DEFLATED);
                zos.setLevel(9);
                for (long resourceId : files) {
                    addToZip(zos, resourceManager.get(resourceId, user), "", parameter, user);
                }
                zos.close();
            } catch (ZipException ze) {
                if (ze.toString().indexOf("ZIP file must have at least one entry") != -1) {
                    throw new ActionException("errors.actions.zipFileEmpty", ze);
                } else {
                    throw new ActionException("errors.actions.zipFileError", ze);
                }
            } catch (Exception e) {
                throw new ActionException("errors.actions.zipFileError", e);
            }
        }
        return null;
    }

    protected void addToZip(ZipOutputStream zos, Resource file, String path, String attributeId, User user) throws Exception {
        if (file != null) {
            if (file.getTypeId() == 5) {
                BinaryAttribute ba = resourceManager.readAttribute(file.getResourceId(), attributeId, user);
                addZipEntry(path, file.getName(), file.getUpdateDate().getTime(), zos);
                int read = 0;
                byte[] readBuffer = new byte[2156];
                InputStream is = ba.getInputStream();
                while ((read = is.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, read);
                }
                is.close();
                zos.closeEntry();
            } else if (file.getTypeId() == 8) {
            }
            List<Resource> dir = resourceManager.getList(file.getResourceId(), user);
            path += toUsAscii(file.getName()) + "/";
            for (Resource resource : dir) {
                addToZip(zos, resource, path, attributeId, user);
            }
        }
    }

    private void addZipEntry(String path, String fileName, long time, ZipOutputStream zos) throws IOException {
        int collisionCounter = 0;
        do {
            try {
                ZipEntry entry = new ZipEntry(getZipEntryName(path, fileName, collisionCounter));
                entry.setTime(time);
                zos.putNextEntry(entry);
                break;
            } catch (ZipException ze) {
                ++collisionCounter;
            }
        } while (true);
    }

    private String getZipEntryName(String path, String fileName, int collisionCounter) {
        fileName = toUsAscii(fileName);
        if (collisionCounter == 0) {
            return path + fileName;
        } else {
            int lastPoint = fileName.indexOf('.');
            if (lastPoint == -1) {
                return new StringBuffer(path).append(fileName).append("_").append(collisionCounter).toString();
            } else {
                return new StringBuffer(path).append(fileName.substring(0, lastPoint)).append("_").append(collisionCounter).append(fileName.substring(lastPoint)).toString();
            }
        }
    }

    private String toUsAscii(String value) {
        String[] find = { "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "�", "\\", "/", ":", "*", "?", "\"", "<", ">", "|" };
        String[] replace = { "ae", "ue", "oe", "a", "e", "e", "e", "i", "o", "u", "c", "n", "Ae", "Oe", "Ue", "Ae", "E", "E", "E", "I", "O", "U", "N", "_", "_", "_", "_", "_", "_", "_", "_", "_" };
        for (int i = 0; i < find.length; ++i) {
            value = value.replace(find[i], replace[i]);
        }
        return value;
    }
}
