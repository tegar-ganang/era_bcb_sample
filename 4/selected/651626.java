package com.tresys.slide.linkage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.tresys.slide.linkage.libapoljava.SWIGTYPE_p_FILE;
import com.tresys.slide.linkage.libapoljava.SWIGTYPE_p_int;
import com.tresys.slide.linkage.libapoljava.SWIGTYPE_p_p_char;
import com.tresys.slide.linkage.libapoljava.SWIGTYPE_p_p_classes_perm_map;
import com.tresys.slide.linkage.libapoljava.SWIGTYPE_p_p_int;
import com.tresys.slide.linkage.libapoljava.SWIGTYPE_p_p_policy;
import com.tresys.slide.linkage.libapoljava.apol;
import com.tresys.slide.linkage.libapoljava.policy_t;

/**
 * This class queries base policy for common information such as existing types
 * and file context.
 */
public class BasePolicy {

    private final policy_t Policy;

    /**
	 * This constructor parses a policy.conf file file and creates data
	 * structures to support common base policy functions.
	 * 
	 * @param policyConf
	 *            The compiled policy.conf file.
	 * @throws IOException
	 *             if the policy.conf file fails to open
	 * @throws Exception
	 *             If memory allocation fails
	 */
    public BasePolicy(String flaskPath) throws Exception {
        SWIGTYPE_p_p_policy p_p_pol = apol.new_policy_t_p_p();
        if (!flaskPath.endsWith("/")) flaskPath += "/";
        File tmpPolConf = File.createTempFile("tmpBasePolicy", ".conf");
        BufferedWriter tmpPolFile = new BufferedWriter(new FileWriter(tmpPolConf));
        BufferedReader secClassFile = new BufferedReader(new FileReader(flaskPath + "security_classes"));
        int bufSize = 1024;
        char[] buffer = new char[bufSize];
        int read;
        while ((read = secClassFile.read(buffer)) > 0) {
            tmpPolFile.write(buffer, 0, read);
        }
        secClassFile.close();
        BufferedReader sidsFile = new BufferedReader(new FileReader(flaskPath + "initial_sids"));
        while ((read = sidsFile.read(buffer)) > 0) {
            tmpPolFile.write(buffer, 0, read);
        }
        sidsFile.close();
        BufferedReader axxVecFile = new BufferedReader(new FileReader(flaskPath + "access_vectors"));
        while ((read = axxVecFile.read(buffer)) > 0) {
            tmpPolFile.write(buffer, 0, read);
        }
        axxVecFile.close();
        tmpPolFile.write("attribute ricka; \ntype rick_t; \nrole rick_r types rick_t; \nuser rick_u roles rick_r;\nsid kernel      rick_u:rick_r:rick_t\nfs_use_xattr ext3 rick_u:rick_r:rick_t;\ngenfscon proc /  rick_u:rick_r:rick_t\n");
        tmpPolFile.flush();
        tmpPolFile.close();
        if (apol.open_policy(tmpPolConf.getAbsolutePath(), p_p_pol) == 0) {
            Policy = apol.policy_t_p_p_value(p_p_pol);
            if (Policy == null) {
                throw new Exception("Failed to allocate memory for policy_t struct.");
            }
            tmpPolConf.delete();
        } else {
            throw new IOException("Failed to open/parse base policy file: " + tmpPolConf.getAbsolutePath());
        }
    }

    /**
	 * This constructor parses a policy.conf file and file_context file and
	 * creates data structures to support common base policy functions.
	 * 
	 * @param policyConf
	 *            The compiled policy.conf file.
	 * @param fileContext
	 *            The compiled file_context file.
	 * @throws IOException
	 *             if the policy.conf file fails to open
	 * @throws Exception
	 *             If memory allocation fails
	 */
    public BasePolicy(String flaskPath, String fileContext) throws IOException, Exception {
        this(flaskPath);
    }

    public BasePolicy(String flaskPath, String fileContext, String permMapFile) throws IOException, Exception {
        this(flaskPath, fileContext);
        LoadPermMapFile(permMapFile);
    }

    /**
	 * Check for type's existence in base policy.
	 * 
	 * @param type
	 *            The type name used in lookup.
	 * @return <code>true</code> if type exists in base policy,
	 *         <code>false</code> otherwise.
	 */
    public boolean TypeExists(String type) {
        return (apol.get_type_idx(type, Policy) >= 0);
    }

    public void LoadPermMapFile(String fileName) throws IOException {
        SWIGTYPE_p_p_classes_perm_map mappp = apol.new_classes_perm_map_t_p_p();
        SWIGTYPE_p_FILE f;
        if ((f = apol.fopen(fileName, "r")) != null) {
            apol.load_perm_mappings(mappp, Policy, f);
            System.out.println("Permission Map is loaded.");
        } else {
            throw new IOException("Failed to open/load permission file: " + fileName);
        }
    }

    /**
	 * Check for object class' existence in base policy.
	 * 
	 * @param objClass
	 *            The object class used in lookup.
	 * @return <code>true</code> if objectclass exists in base policy,
	 *         <code>false</code> otherwise.
	 */
    public boolean ObjectClassExists(String objClass) {
        return (apol.get_obj_class_idx(objClass, Policy) >= 0);
    }

    /**
	 * Check for object class permission's existence in base policy.
	 * 
	 * @param objClass
	 *            The object class used in lookup.
	 * @param perm
	 *            The permission used in lookup.
	 * @return <code>true</code> if objectclass exists in base policy,
	 *         <code>false</code> otherwise.
	 */
    public boolean PermissionExists(String objClass, String perm) {
        int objIndex = apol.get_obj_class_idx(objClass, Policy);
        int permIndex = apol.get_perm_idx(perm, Policy);
        return (apol.TRUE == apol.is_valid_perm_for_obj_class(Policy, objIndex, permIndex));
    }

    /**
	 * Get Permission Name string
	 * 
	 * @param index
	 *            The permission index to look up name in Policy.
	 * @return <code>String</code> if permission exists in base policy,
	 *         <code>null</code> otherwise.
	 */
    public String GetPermissionName(int idx) {
        SWIGTYPE_p_p_char name = apol.new_char_p_p();
        if (apol.get_perm_name(idx, name, Policy) < 0) {
            apol.delete_char_p_p(name);
            return null;
        } else {
            String st = apol.char_p_p_value(name);
            apol.delete_char_p_p(name);
            return (st.length() > 0) ? st : null;
        }
    }

    /**
	 * Get ObjectClass Name
	 * 
	 * @param index
	 *            The Class index to look up name in Policy.
	 * @return <code>String</code> if Object class exists in base policy,
	 *         <code>null</code> otherwise.
	 */
    public String GetObjectClassName(int idx) {
        SWIGTYPE_p_p_char name = apol.new_char_p_p();
        if (apol.get_obj_class_name(idx, name, Policy) < 0) {
            apol.delete_char_p_p(name);
            return null;
        } else {
            String st = apol.char_p_p_value(name);
            apol.delete_char_p_p(name);
            return (st.length() > 0) ? st : null;
        }
    }

    public String[] GetObjectClasses() {
        int num_obj_classes = Policy.getNum_obj_classes();
        String[] classes = new String[num_obj_classes];
        for (int i = 0; i < num_obj_classes; i++) {
            SWIGTYPE_p_p_char name = apol.new_char_p_p();
            apol.get_obj_class_name(i, name, Policy);
            classes[i] = apol.char_p_p_value(name);
            apol.delete_char_p_p(name);
        }
        return classes;
    }

    public String[] GetObjectClassPerms(String objectClass) {
        int obj_class_idx = apol.get_obj_class_idx(objectClass, Policy);
        SWIGTYPE_p_int numPermsPtr = apol.new_int_p();
        SWIGTYPE_p_p_int permArrayPtr = apol.new_int_p_p();
        apol.get_obj_class_perms(obj_class_idx, numPermsPtr, permArrayPtr, Policy);
        int numPerms = apol.int_p_value(numPermsPtr);
        SWIGTYPE_p_int permArray = apol.int_p_p_value(permArrayPtr);
        String[] perms = new String[numPerms];
        for (int i = 0; i < numPerms; i++) {
            int idx = apol.intArray_getitem(permArray, i);
            SWIGTYPE_p_p_char name = apol.new_char_p_p();
            apol.get_perm_name(idx, name, Policy);
            perms[i] = apol.char_p_p_value(name);
            apol.delete_char_p_p(name);
        }
        apol.delete_int_p(numPermsPtr);
        apol.delete_intArray(permArray);
        apol.delete_int_p_p(permArrayPtr);
        return perms;
    }

    protected void finalize() {
        if (Policy != null) {
            apol.close_policy(Policy);
        }
    }
}
