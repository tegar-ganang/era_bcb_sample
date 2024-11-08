package org.photovault.dcraw;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.FileUtils;
import org.photovault.imginfo.VolumeBase;

/**
 * This class describes a color profile used by Photovault raw conversion.
 * The color profiles are stored in Photovault volumes, and a profile described
 * by an object of this class can be present in many volumes. Each instance is 
 * described by an object of class {@link ColorProfileInstance}.
 
 * @author Harri Kaimio
 * @since 0.4
 */
public class ColorProfileDesc {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ColorProfileDesc.class.getName());

    /**
     * Creates a new instance of ColorProfileDesc
     */
    public ColorProfileDesc() {
    }

    /**
     * Name of this ICC profile
     */
    private String name = null;

    /**
     * Free-text description if  this profile
     */
    private String description = null;

    /**
     * Cource color space of this profile
     */
    private int srcCS = 0;

    /**
     * Target color space of this profile
     */
    private int targetCS = 0;

    /**
     * MD5 hash of this profile
     */
    private byte[] hash = null;

    Vector instances = null;

    int id;

    UUID uuid;

    /**
     * Get the name
     * @return Name of this profile
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this profile
     * @param name New name of the profile
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description
     * @return Description of this profile
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this profile
     * @param description New description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public int getSrcCS() {
        return srcCS;
    }

    public void setSrcCS(int srcCS) {
        this.srcCS = srcCS;
    }

    public int getTargetCS() {
        return targetCS;
    }

    public void setTargetCS(int targetCS) {
        this.targetCS = targetCS;
    }

    /**
     * Get the hash of this profile
     * @return MD hash of the ICC profile hash
     */
    public byte[] getHash() {
        return (hash != null) ? hash.clone() : null;
    }

    /**
     * Set the hash
     * @param hash New MD5 hash
     */
    private void setHash(byte[] hash) {
        this.hash = hash;
    }

    /**
     * Get an existing color profile with a given ID
     * @param id ID of hte color profile to search
     * @return The color profile with a given ID or <CODE>null</CODE> if no such profile 
     * is found.
     */
    public static ColorProfileDesc getProfileById(int id) {
        log.debug("Fetching ColorProfileDesc with ID " + id);
        String oql = "select colorProfiles from " + ColorProfileDesc.class.getName() + " where id=" + id;
        List profiles = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create(oql);
            profiles = (List) query.execute();
            txw.commit();
        } catch (Exception e) {
            log.warn("Error fetching record: " + e.getMessage());
            txw.abort();
            return null;
        }
        if (profiles.size() == 0) {
            return null;
        }
        ColorProfileDesc p = (ColorProfileDesc) profiles.get(0);
        return p;
    }

    /**
     * Get all known ICC prodiles
     * @return Collection containing all known profiles
     */
    public static Collection getAllProfiles() {
        log.debug("Fetching all color profiles");
        String oql = "select colorProfiles from " + ColorProfileDesc.class.getName() + " where id > 0";
        List profiles = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create(oql);
            txw.flush();
            profiles = (List) query.execute();
            txw.commit();
        } catch (Exception e) {
            log.warn("Error fetching records: " + e.getMessage());
            e.printStackTrace();
            txw.abort();
            return null;
        }
        return profiles;
    }

    private void addInstance(ColorProfileInstance i) {
        if (instances == null) {
            instances = new Vector();
        }
        instances.add(i);
    }

    /**
     Get an ICC file with this color profile
     @return An accessible file with the correct ICC profile of <code>null</code>
     if no such file is available.
     */
    public File getInstanceFile() {
        File ret = null;
        Iterator iter = instances.iterator();
        while (iter.hasNext()) {
            ColorProfileInstance i = (ColorProfileInstance) iter.next();
            File cand = i.getProfileFile();
            if (cand.exists()) {
                ret = cand;
                break;
            }
        }
        return ret;
    }

    /**
     * An action used for creating a new ICC profile. Profiles should be created 
     * with this action since it is anticipated that after distributed database
     * support is added this will be the only allowed method.
     */
    public static class CreateProfile {

        File profileFile;

        String name;

        String description;

        /**
         * Create a new CreateProfile action
         * @param f File containing the ICC profile.
         * @param name Name of the profile.
         * @param desc Description of the profile.
         */
        public CreateProfile(File f, String name, String desc) {
            profileFile = f;
            this.name = name;
            this.description = desc;
        }

        /**
         * Creates the profile
         * @return The created profile
         */
        public ColorProfileDesc execute() {
            log.debug("CreateProfile#execute: " + name);
            ODMGXAWrapper txw = new ODMGXAWrapper();
            ColorProfileDesc p = new ColorProfileDesc();
            txw.lock(p, Transaction.WRITE);
            p.setName(name);
            p.setDescription(description);
            VolumeBase defvol = VolumeBase.getDefaultVolume();
            File f = defvol.getFilingFname(profileFile);
            log.debug("Copying to default volume: " + f.getAbsolutePath());
            try {
                FileUtils.copyFile(profileFile, f);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            byte[] hash = FileUtils.calcHash(f);
            p.setHash(hash);
            txw.flush();
            ColorProfileInstance i = new ColorProfileInstance();
            i.fname = defvol.mapFileToVolumeRelativeName(f);
            i.volumeId = defvol.getName();
            i.profileId = p.id;
            p.addInstance(i);
            txw.lock(i, Transaction.WRITE);
            txw.commit();
            return p;
        }
    }

    /**
     * Action to change a color profile
     */
    public static class ChangeProfile {

        ColorProfileDesc p;

        String newName = null;

        String newDesc = null;

        /**
         * Create a new ChangeProfile action.
         * @param p Profile that will be changed
         */
        public ChangeProfile(ColorProfileDesc p) {
            this.p = p;
        }

        /**
         * Set new name for the profile
         * @param name New name for the profile
         */
        public void setName(String name) {
            newName = name;
        }

        /**
         * Set new description for the profile
         * @param desc The new description
         */
        public void setDesc(String desc) {
            newDesc = desc;
        }

        /**
         * Do the changes to profile.
         */
        public void execute() {
            ODMGXAWrapper txw = new ODMGXAWrapper();
            txw.lock(p, Transaction.WRITE);
            if (newName != null) {
                p.setName(newName);
            }
            if (newDesc != null) {
                p.setDescription(newDesc);
            }
        }
    }
}
