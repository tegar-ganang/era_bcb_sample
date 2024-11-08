package lif.core.service.impl;

import lif.core.dao.*;
import lif.core.domain.ClientUser;
import lif.core.domain.DbProfile;
import lif.core.service.ProfileService;
import lif.core.service.UserDoesNotExistException;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProfileServiceImpl implements ProfileService {

    private Logger logger = Logger.getLogger(ProfileServiceImpl.class);

    private boolean isDebugEnabled = logger.isDebugEnabled();

    private String profileSource;

    private DbProfileDao dbProfileDao;

    private ClientUserDao clientUserDao;

    private PlatformTransactionManager transactionManager;

    public boolean validateProfile(String profileName) {
        return false;
    }

    public void profileSave(String sourcePath, MultipartFile file) {
        String path = sourcePath + File.separator + file.getOriginalFilename();
        debug("the path of the profile is [" + path + "]");
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(new File(path)));
            bos.write(file.getBytes());
            bos.flush();
            debug("profile [" + file.getOriginalFilename() + "] has been saved sucsessfully");
        } catch (FileNotFoundException e) {
            debug("File not found: profile upload fail");
        } catch (IOException e) {
            debug("IOException [" + e + "]");
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                debug("IOException [" + e + "]");
            }
        }
    }

    public void addProfile(String profileName, String profileOwner) throws UserDoesNotExistException, DataAccessException {
        DbProfile profile = new DbProfile(profileName, false, new Date());
        ClientUser user = clientUserDao.getUser(profileOwner);
        profile.addProfileOwner(user);
        dbProfileDao.addDbProfile(profile);
    }

    public void addProfileToUser(final String profileName, final String profileOwner) throws DuplicateDataObjectException, DataObjectNotFoundException, DataAccessException, UserDoesNotExistException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    DbProfile profile = dbProfileDao.getDbProfile(profileName);
                    ClientUser user = clientUserDao.getUser(profileOwner);
                    profile.addProfileOwner(user);
                    dbProfileDao.updateProfile(profile);
                } catch (DataAccessException e) {
                    debug("DataAccessException occured at ProfileServiceImpl " + e.toString());
                } catch (DuplicateDataObjectException e) {
                    debug("DuplicateDataObjectException occured at ProfileServiceImpl " + e.toString());
                } catch (DataObjectNotFoundException e) {
                    debug("DataObjectNotFoundException occured at ProfileServiceImpl " + e.toString());
                } catch (UserDoesNotExistException e) {
                    debug("UserDoesNotExistException occured at ProfileServiceImpl " + e.toString());
                }
            }
        });
    }

    public DbProfile getDbProfile(String profileName) throws DuplicateDataObjectException, DataObjectNotFoundException, DataAccessException {
        return dbProfileDao.getDbProfile(profileName);
    }

    public boolean isProfileExist(String profileName) throws DuplicateDataObjectException, DataAccessException {
        return dbProfileDao.isProfileExist(profileName);
    }

    public boolean isProfileDirExist(String profileName) {
        String profilePath = profileSource + File.separator + profileName;
        File file = new File(profilePath);
        return file.isDirectory();
    }

    public void extractProfile(String parentDir, String fileName, String profileName) {
        try {
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            if (createProfileDirectory(profileName, parentDir)) {
                debug("the profile directory created .starting the profile extraction");
                String profilePath = parentDir + File.separator + fileName;
                zipinputstream = new ZipInputStream(new FileInputStream(profilePath));
                zipentry = zipinputstream.getNextEntry();
                while (zipentry != null) {
                    String entryName = zipentry.getName();
                    int n;
                    FileOutputStream fileoutputstream;
                    File newFile = new File(entryName);
                    String directory = newFile.getParent();
                    if (directory == null) {
                        if (newFile.isDirectory()) break;
                    }
                    fileoutputstream = new FileOutputStream(parentDir + File.separator + profileName + File.separator + newFile.getName());
                    while ((n = zipinputstream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
                    fileoutputstream.close();
                    zipinputstream.closeEntry();
                    zipentry = zipinputstream.getNextEntry();
                }
                zipinputstream.close();
                debug("deleting the profile.zip file");
                File newFile = new File(profilePath);
                if (newFile.delete()) {
                    debug("the " + "[" + profilePath + "]" + " deleted successfully");
                } else {
                    debug("profile" + "[" + profilePath + "]" + "deletion fail");
                    throw new IllegalArgumentException("Error: deletion error!");
                }
            } else {
                debug("error creating the profile directory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean createProfileDirectory(String profileName, String parentDir) throws IllegalArgumentException {
        String proifleDirecory = parentDir + File.separator + profileName;
        File f = new File(proifleDirecory);
        if (!f.exists()) {
            return f.mkdir();
        } else {
            boolean success = deleteProfileDir(f);
            if (!success) {
                debug(proifleDirecory + " directory deletion error");
                throw new IllegalArgumentException(proifleDirecory + " directory deletion error");
            } else {
                if (!f.mkdir()) {
                    debug("creating " + profileName + " directory fails");
                    throw new IllegalArgumentException("creating " + profileName + " directory fails");
                } else {
                    return true;
                }
            }
        }
    }

    public boolean deleteProfileDir(String profileName) {
        String profilePath = profileSource + File.separator + profileName;
        debug("Starting to delete profile [" + profilePath + "]");
        File file = new File(profilePath);
        if (deleteProfileDir(file)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean deleteProfile(final String profileName) {
        debug("At the deleteProfile ");
        boolean isProfileDirDelete = deleteProfileDir(profileName);
        if (isProfileDirDelete) {
            TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {

                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    try {
                        DbProfile profile = dbProfileDao.getDbProfile(profileName);
                        dbProfileDao.deleteProfile(profile);
                    } catch (DataAccessException e) {
                        debug("DataAccessException occured at ProfileServiceImpl " + e.toString());
                    } catch (DuplicateDataObjectException e) {
                        debug("DuplicateDataObjectException occour at ProfileServiceImpl" + e.toString());
                    } catch (DataObjectNotFoundException e) {
                        debug("DataObjectNotFoundException occour at ProfileServiceImpl" + e.toString());
                    }
                }
            });
            return true;
        }
        return false;
    }

    public List<DbProfile> getAllDbProfiles() throws DataAccessException {
        return dbProfileDao.getAllDbProfile();
    }

    public void changeState(final String profileName) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    DbProfile profile = dbProfileDao.getDbProfile(profileName);
                    boolean status = !profile.isStatus();
                    profile.setStatus(status);
                    dbProfileDao.updateProfile(profile);
                } catch (DataAccessException e) {
                    debug("DataAccessException occured at ProfileServiceImpl " + e.toString());
                } catch (DuplicateDataObjectException e) {
                    debug("DuplicateDataObjectException occour at ProfileServiceImpl" + e.toString());
                } catch (DataObjectNotFoundException e) {
                    debug("DataObjectNotFoundException occour at ProfileServiceImpl" + e.toString());
                }
            }
        });
    }

    private boolean deleteProfileDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteProfileDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void setProfileSource(String profileSource) {
        this.profileSource = profileSource;
    }

    public void setDbProfileDao(DbProfileDao dbProfileDao) {
        this.dbProfileDao = dbProfileDao;
    }

    public void setClientUserDao(ClientUserDao clientUserDao) {
        this.clientUserDao = clientUserDao;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    private void debug(String msg) {
        if (isDebugEnabled) {
            logger.debug(msg);
        }
    }
}
