package net.redwarp.actions.hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.redwarp.actions.tools.Tools;
import com.jbbres.lib.actions.elements.ActionExecutionException;
import com.jbbres.lib.actions.elements.Parameters;
import com.jbbres.lib.actions.tools.elements.AbstractAction;
import com.jbbres.lib.actions.tools.elements.AbstractActionService;

public class HashFilesService extends AbstractActionService<File[], String[]> {

    public HashFilesService(AbstractAction parent) {
        super(parent);
    }

    @Override
    public String[] executeAction(File[] files, Parameters params) throws ActionExecutionException {
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                throw new ActionExecutionException(HashFiles.bundle.getString("errorDirectory"));
            }
            if (file.length() > Integer.MAX_VALUE) {
                throw new ActionExecutionException("File " + file.getName() + " is too big");
            }
        }
        String[] output = new String[files.length];
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new ActionExecutionException("MD5 doesn't exist");
        }
        for (int i = 0; i < files.length; i++) {
            InputStream fis;
            try {
                fis = new FileInputStream(files[i]);
            } catch (FileNotFoundException e) {
                throw new ActionExecutionException(HashFiles.bundle.getString("errorFileNotFound"));
            }
            try {
                fis = new DigestInputStream(fis, digest);
                byte[] pointlessBuffer = new byte[1024];
                while (fis.read(pointlessBuffer) != -1) ;
            } catch (Exception e) {
                throw new ActionExecutionException(HashFiles.bundle.getString("errorFileAccess"));
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new ActionExecutionException(HashFiles.bundle.getString("errorIO"));
                }
            }
            byte[] result = digest.digest();
            output[i] = Tools.toHexString(result);
        }
        return output;
    }
}
