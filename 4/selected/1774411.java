package org.netbeans;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/** Class that can enhance bytecode with information about alternative
 * superclass and access modifiers. It can also extract this information 
 * later when the class is about to be loaded into the VM.
 * <P>
 * The additional information is added to attributes of the classfile (global attributes 
 * and also member attributes) and as such the class remain compatible and 
 * understandable for any VM. But if loaded by classloader that before defining 
 * the class invokes:
 * <pre>
 *  byte[] arr = ...;
 *  arr = PatchByteCode.patch (arr); 
 * </pre>
 * The class is altered in its superclass and/or access modifiers.
 * <P>
 * The patching mechanism uses two attributes. ATTR_SUPERCLASS can be just 
 * in global attributes pool (and only once), is of length 2 and contains index
 * into constant pool that contains definition of a Class that should become 
 * the alternate superclass. Attribute ATTR_MEMBER can appear in global
 * attribute set and also in set of each member (field or method). It is of 
 * length 2 and contains alternate value for access flags of the class or of
 * the field.
 * <P>
 * For purposes for speed, each patched class file has to end with bytes "nb". 
 * This is achieved by finishing the patching process by adding third attribute
 * "org.netbeans.enhanced" with value "nb". As such the <code>PatchByteCode.patch</code>
 * can quickly check the byte array and process just those that need processing.
 *
 * @author  Jaroslav Tulach
 */
public final class PatchByteCode {

    private static final String ATTR_SUPERCLASS = "org.netbeans.superclass";

    private static final byte[] BYTE_SUPERCLASS;

    static {
        try {
            BYTE_SUPERCLASS = ATTR_SUPERCLASS.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static final String ATTR_INTERFACES = "org.netbeans.interfaces";

    private static final byte[] BYTE_INTERFACES;

    static {
        try {
            BYTE_INTERFACES = ATTR_INTERFACES.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static final String ATTR_MEMBER = "org.netbeans.member";

    private static final byte[] BYTE_MEMBER;

    static {
        try {
            BYTE_MEMBER = ATTR_MEMBER.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static final String ATTR_NAME = "org.netbeans.name";

    private static final byte[] BYTE_NAME;

    static {
        try {
            BYTE_NAME = ATTR_NAME.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static final String ATTR_INIT = "<init>";

    private static final byte[] BYTE_INIT;

    static {
        try {
            BYTE_INIT = ATTR_INIT.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static final String ATTR_INIT_TYPE = "()V";

    private static final byte[] BYTE_INIT_TYPE;

    static {
        try {
            BYTE_INIT_TYPE = ATTR_INIT_TYPE.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private byte[] arr;

    private int cpCount;

    private int cpEnd;

    private int atCount;

    private int atEnd;

    /** the index of a string that matches the searched attribute */
    private int superClassNameIndex;

    /** the possiton of found attribute */
    private int superClassNameAttr;

    /** the index of a string that matches the searched attribute */
    private int interfacesNameIndex;

    /** the possiton of found attribute */
    private int interfacesNameAttr;

    /** the index of a string to patch members of a field */
    private int memberNameIndex = -1;

    /** position of attribute the change the access rights of the class */
    private int memberClassAttr = -1;

    /** index of <init> UTF8 in constant pool*/
    private int initIndex = -1;

    /** index of ()V UTF8 in constant pool */
    private int initIndexType = -1;

    /** index of CONSTANT_NameAndType index for <init> and ()V in pool */
    private int initNameTypeIndex = -1;

    /** position of the <init> method */
    private int initAttr = -1;

    /** index of string that identifies the rename of a member */
    private int renameNameIndex = -1;

    /** map that maps names of fields to their position in constant pool (String, int[1]) */
    private HashMap nameIndexes;

    /** Creates a new instance of PatchByteCode 
     *
     * @param nameIndexes hashmap from (String -> int[1]) 
     */
    private PatchByteCode(byte[] arr, HashMap nameIndexes) {
        this.arr = arr;
        this.nameIndexes = nameIndexes;
        scan();
        scan();
    }

    /** Generates patch attribute into the classfile to
     * allow method <code>patch</code> to modify the superclass of this 
     * class.
     *
     * @param arr the bytecode to change
     * @param args map with arguments. 
     * @return new version of the bytecode if changed, otherwise null to signal that
     * no change has been made
     */
    public static byte[] enhanceClass(byte[] arr, java.util.Map args) {
        if (isPatched(arr)) {
            return null;
        }
        String superClass = (String) args.get("netbeans.superclass");
        String interfaces = (String) args.get("netbeans.interfaces");
        List methods = (List) args.get("netbeans.public");
        List rename = (List) args.get("netbeans.rename");
        HashMap m;
        if (methods != null || rename != null) {
            m = new HashMap();
            if (methods != null) {
                Iterator it = methods.iterator();
                while (it.hasNext()) {
                    m.put((String) it.next(), new int[1]);
                }
            }
            if (rename != null) {
                Iterator it = rename.iterator();
                while (it.hasNext()) {
                    m.put((String) it.next(), new int[1]);
                }
            }
        } else {
            m = null;
        }
        PatchByteCode pc = new PatchByteCode(arr, m);
        boolean patched = false;
        if (superClass != null) {
            int x = pc.addClass(superClass);
            byte[] sup = new byte[2];
            writeU2(sup, 0, x);
            pc.addAttribute(ATTR_SUPERCLASS, sup);
            patched = true;
        }
        if (interfaces != null) {
            java.util.ArrayList tokens = new java.util.ArrayList();
            java.util.StringTokenizer tok = new java.util.StringTokenizer(interfaces, ",");
            while (tok.hasMoreElements()) {
                tokens.add(tok.nextElement());
            }
            String[] ifaces = (String[]) tokens.toArray(new String[0]);
            byte[] sup = new byte[2 + ifaces.length * 2];
            writeU2(sup, 0, ifaces.length);
            for (int i = 0; i < ifaces.length; i++) {
                int x = pc.addClass(ifaces[i]);
                writeU2(sup, 2 + i * 2, x);
            }
            pc.addAttribute(ATTR_INTERFACES, sup);
            patched = true;
        }
        if (!pc.isPublic()) {
            pc.markPublic();
            patched = true;
        }
        if (methods != null) {
            Iterator it = methods.iterator();
            while (it.hasNext()) {
                patched |= pc.markMemberPublic((String) it.next());
            }
        }
        if (rename != null) {
            Iterator it = rename.iterator();
            while (it.hasNext()) {
                patched |= pc.renameMember((String) it.next(), (String) it.next());
            }
        }
        if (patched) {
            byte[] patch = { 'n', 'b' };
            pc.addAttribute("org.netbeans.enhanced", patch);
        } else {
            return null;
        }
        return pc.getClassFile();
    }

    /** Checks if the class has previously been enhanced by the 
     * change of superclass attribute and if so, changes the bytecode
     * to reflect the change.
     * 
     * @param arr the bytecode
     * @param name the class name
     * @return the enhanced bytecode
     */
    public static byte[] patch(byte[] arr, String name) {
        if (!isPatched(arr)) return arr;
        PatchByteCode pc = new PatchByteCode(arr, null);
        if (pc.superClassNameAttr > 0) {
            int classindex = pc.readU2(pc.superClassNameAttr + 6);
            writeU2(pc.getClassFile(), pc.cpEnd + 4, classindex);
            if (pc.initAttr != -1) {
                writeU2(pc.getClassFile(), pc.initAttr + 1, classindex);
            }
        }
        if (pc.memberClassAttr > 0) {
            if (pc.readU4(pc.memberClassAttr + 2) != 2) {
                throw new IllegalArgumentException("Size of a attribute " + ATTR_MEMBER + " should be 2");
            }
            int access = pc.readU2(pc.memberClassAttr + 6);
            int now = pc.readU2(pc.cpEnd);
            writeU2(pc.getClassFile(), pc.cpEnd, access);
        }
        if (pc.memberNameIndex > 0 || pc.renameNameIndex > 0) {
            pc.applyMemberAccessAndNameChanges();
        }
        byte[] result = pc.getClassFile();
        if (pc.interfacesNameAttr > 0) {
            int numberOfIfaces = pc.readU2(pc.interfacesNameAttr + 6);
            int currentIfaces = pc.readU2(pc.cpEnd + 6);
            byte[] insert = new byte[result.length + numberOfIfaces * 2];
            System.arraycopy(result, 0, insert, 0, pc.cpEnd + 6);
            System.arraycopy(result, pc.interfacesNameAttr + 8, insert, pc.cpEnd + 8, numberOfIfaces * 2);
            System.arraycopy(result, pc.cpEnd + 8, insert, pc.cpEnd + 8 + numberOfIfaces * 2, result.length - pc.cpEnd - 8);
            writeU2(insert, pc.cpEnd + 6, numberOfIfaces + currentIfaces);
            result = insert;
        }
        return result;
    }

    /** Check if the byte code is patched.
     * @param arr the bytecode
     * @return true if patched
     */
    private static boolean isPatched(byte[] arr) {
        if (arr == null || arr.length < 2) return false;
        int base = arr.length - 2;
        if (arr[base + 1] != 'b') return false;
        if (arr[base + 0] != 'n') return false;
        return true;
    }

    /** Gets the current byte array of the actual class file.
     * @return bytes of the class file
     */
    private byte[] getClassFile() {
        return arr;
    }

    /** Creates new contant pool entry representing given class.
     * @param c name of the class
     * @return index of the entry
     */
    private int addClass(String s) {
        int x = addConstant(s);
        byte[] t = { 7, 0, 0 };
        writeU2(t, 1, x);
        return addPool(t);
    }

    /** Adds a new string constant to the constant pool.
     * @param s the string to add
     * @return index of the constant
     */
    private int addConstant(String s) {
        byte[] t;
        try {
            t = s.getBytes("utf-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 shall be always supported");
        }
        byte[] add = new byte[t.length + 3];
        System.arraycopy(t, 0, add, 3, t.length);
        add[0] = 1;
        writeU2(add, 1, t.length);
        return addPool(add);
    }

    /** Adds this array of bytes as another entry into the constant pool */
    private int addPool(byte[] add) {
        byte[] res = new byte[arr.length + add.length];
        System.arraycopy(arr, 0, res, 0, cpEnd);
        int index = readU2(cpCount);
        writeU2(res, cpCount, index + 1);
        System.arraycopy(add, 0, res, cpEnd, add.length);
        System.arraycopy(arr, cpEnd, res, cpEnd + add.length, arr.length - cpEnd);
        arr = res;
        cpEnd += add.length;
        atCount += add.length;
        atEnd += add.length;
        return index;
    }

    /** Checks whether the code is public.
     */
    private boolean isPublic() {
        int x = readU2(cpEnd);
        if ((x & 0x0001) != 0) {
            return true;
        } else {
            return false;
        }
    }

    /** Ensures that the class is public 
     * @return true if really patched, false if not
     */
    private boolean markPublic() {
        if (isPublic()) {
            return false;
        }
        if (memberNameIndex == -1) {
            memberNameIndex = addConstant(ATTR_MEMBER);
        }
        int x = readU2(cpEnd) | 0x0001;
        byte[] sup = new byte[2];
        writeU2(sup, 0, x);
        addAttribute(ATTR_MEMBER, sup);
        return true;
    }

    /** Makes method of field public and non final.
     * @param name name of the method to make public
     * @return true if really changed, false if it already was public
     */
    private boolean markMemberPublic(String name) {
        int constantPoolIndex = ((int[]) nameIndexes.get(name))[0];
        int patchCount = 0;
        boolean modified = false;
        if (memberNameIndex == -1) {
            memberNameIndex = addConstant(ATTR_MEMBER);
        }
        int pos = cpEnd;
        pos += 6;
        pos += 2 * readU2(pos);
        pos += 2;
        for (int fieldsAndMethods = 0; fieldsAndMethods < 2; fieldsAndMethods++) {
            int fieldsOrMethods = readU2(pos);
            pos += 2;
            while (fieldsOrMethods-- > 0) {
                int nameIndex = readU2(pos + 2);
                if (nameIndex == constantPoolIndex) {
                    int access = readU2(pos);
                    if ((access & 0x0001) == 0 || (access & 0x0010) != 0) {
                        access = (access | 0x0001) & ~(0x0010 | 0x0002 | 0x0004);
                        int cnt = readU2(pos + 6) + 1;
                        byte[] res = new byte[arr.length + 2 + 6];
                        System.arraycopy(arr, 0, res, 0, pos + 6);
                        writeU2(res, pos + 6, cnt);
                        writeU2(res, pos + 8, memberNameIndex);
                        writeU4(res, pos + 10, 2);
                        writeU2(res, pos + 14, access);
                        System.arraycopy(arr, pos + 8, res, pos + 8 + 6 + 2, arr.length - pos - 8);
                        atEnd += 2 + 6;
                        atCount += 2 + 6;
                        arr = res;
                        modified = true;
                    }
                    patchCount++;
                }
                pos += memberSize(pos, null);
            }
        }
        if (patchCount == 0) {
            throw new IllegalArgumentException("Member " + name + " not found!");
        }
        return modified;
    }

    /** Marks a field or method as one that should be renamed.
     * @param name name of the member
     * @param rename new name of the member
     * @return true if really changed, false if it already was renamed
     */
    private boolean renameMember(String name, String rename) {
        int constantPoolIndex = ((int[]) nameIndexes.get(name))[0];
        int newPoolIndex;
        {
            int[] arr = (int[]) nameIndexes.get(rename);
            if (arr != null && arr[0] > 0) {
                newPoolIndex = arr[0];
            } else {
                newPoolIndex = addConstant(rename);
                nameIndexes.put(rename, new int[] { newPoolIndex });
            }
        }
        int patchCount = 0;
        boolean modified = false;
        if (renameNameIndex == -1) {
            renameNameIndex = addConstant(ATTR_NAME);
        }
        int pos = cpEnd;
        pos += 6;
        pos += 2 * readU2(pos);
        pos += 2;
        for (int fieldsAndMethods = 0; fieldsAndMethods < 2; fieldsAndMethods++) {
            int fieldsOrMethods = readU2(pos);
            pos += 2;
            while (fieldsOrMethods-- > 0) {
                int nameIndex = readU2(pos + 2);
                if (nameIndex == constantPoolIndex) {
                    int[] attributes = { -1, -1 };
                    memberSize(pos, attributes);
                    if (attributes[1] == -1) {
                        int cnt = readU2(pos + 6) + 1;
                        byte[] res = new byte[arr.length + 2 + 6];
                        System.arraycopy(arr, 0, res, 0, pos + 6);
                        writeU2(res, pos + 6, cnt);
                        writeU2(res, pos + 8, renameNameIndex);
                        writeU4(res, pos + 10, 2);
                        writeU2(res, pos + 14, newPoolIndex);
                        System.arraycopy(arr, pos + 8, res, pos + 8 + 6 + 2, arr.length - pos - 8);
                        atEnd += 2 + 6;
                        atCount += 2 + 6;
                        arr = res;
                        modified = true;
                    }
                    patchCount++;
                }
                pos += memberSize(pos, null);
            }
        }
        if (patchCount == 0) {
            throw new IllegalArgumentException("Member " + name + " not found!");
        }
        return modified;
    }

    /** Checks all members of the class to find out whether they need patching
     * of access rights. If so, patches them.
     */
    private void applyMemberAccessAndNameChanges() {
        int[] result = new int[2];
        int pos = cpEnd;
        pos += 6;
        pos += 2 * readU2(pos);
        pos += 2;
        for (int fieldsAndMethods = 0; fieldsAndMethods < 2; fieldsAndMethods++) {
            int fieldsOrMethods = readU2(pos);
            pos += 2;
            while (fieldsOrMethods-- > 0) {
                result[0] = -1;
                result[1] = -1;
                int size = memberSize(pos, result);
                if (result[0] != -1) {
                    if (readU4(result[0] + 2) != 2) {
                        throw new IllegalArgumentException("Size of a attribute " + ATTR_MEMBER + " should be 2");
                    }
                    int access = readU2(result[0] + 6);
                    writeU2(arr, pos, access);
                }
                if (result[1] != -1) {
                    if (readU4(result[1] + 2) != 2) {
                        throw new IllegalArgumentException("Size of a attribute " + ATTR_NAME + " should be 2");
                    }
                    int newName = readU2(result[1] + 6);
                    writeU2(arr, pos + 2, newName);
                }
                pos += size;
            }
        }
    }

    /** Adds an attribute to the class file.
     * @param name name of the attribute to add
     * @param b the bytes representing the attribute
     */
    private void addAttribute(String name, byte[] b) {
        int index = -1;
        if (ATTR_SUPERCLASS.equals(name) && superClassNameIndex > 0) {
            index = superClassNameIndex;
        }
        if (ATTR_MEMBER.equals(name) && memberNameIndex > 0) {
            index = memberNameIndex;
        }
        if (ATTR_INTERFACES.equals(name) && interfacesNameIndex > 0) {
            index = interfacesNameIndex;
        }
        if (index == -1) {
            index = addConstant(name);
        }
        byte[] res = new byte[arr.length + b.length + 6];
        System.arraycopy(arr, 0, res, 0, arr.length);
        writeU2(res, arr.length, index);
        writeU4(res, arr.length + 2, b.length);
        int begin = arr.length + 6;
        System.arraycopy(b, 0, res, begin, b.length);
        atEnd += b.length + 6;
        writeU2(res, atCount, readU2(atCount) + 1);
        arr = res;
    }

    /** Gets i-th element from the array.
     */
    private int get(int pos) {
        if (pos >= arr.length) {
            throw new ArrayIndexOutOfBoundsException("Size: " + arr.length + " index: " + pos);
        }
        int x = arr[pos];
        return x >= 0 ? x : 256 + x;
    }

    /** Scans the file to find out important possitions
     * @return size of the bytecode
     */
    private void scan() {
        if (get(0) != 0xCA && get(1) != 0xFE && get(2) != 0xBA && get(3) != 0xBE) {
            throw new IllegalStateException("Not a class file!");
        }
        int pos = 10;
        cpCount = 8;
        int cp = readU2(8);
        for (int[] i = { 1 }; i[0] < cp; i[0]++) {
            int len = constantPoolSize(pos, i);
            pos += len;
        }
        cpEnd = pos;
        pos += 6;
        pos += 2 * readU2(pos);
        pos += 2;
        int fields = readU2(pos);
        pos += 2;
        while (fields-- > 0) {
            pos += memberSize(pos, null);
        }
        int methods = readU2(pos);
        pos += 2;
        while (methods-- > 0) {
            pos += memberSize(pos, null);
        }
        int[] memberAccess = { -1, -1 };
        atCount = pos;
        int attrs = readU2(pos);
        pos += 2;
        while (attrs-- > 0) {
            pos += attributeSize(pos, memberAccess);
        }
        if (memberAccess[0] != -1) {
            memberClassAttr = memberAccess[0];
        }
        atEnd = pos;
    }

    private int readU2(int pos) {
        int b1 = get(pos);
        int b2 = get(pos + 1);
        return b1 * 256 + b2;
    }

    private int readU4(int pos) {
        return readU2(pos) * 256 * 256 + readU2(pos + 2);
    }

    private static void writeU2(byte[] arr, int pos, int value) {
        int v1 = (value & 0xff00) >> 8;
        int v2 = value & 0xff;
        if (v1 < 0) v1 += 256;
        if (v2 < 0) v2 += 256;
        arr[pos] = (byte) v1;
        arr[pos + 1] = (byte) v2;
    }

    private static void writeU4(byte[] arr, int pos, int value) {
        writeU2(arr, pos, (value & 0xff00) >> 16);
        writeU2(arr, pos + 2, value & 0x00ff);
    }

    /** @param pos position to read from
     * @param cnt[0] index in the pool that we are now reading
     */
    private int constantPoolSize(int pos, int[] cnt) {
        switch(get(pos)) {
            case 7:
            case 8:
                return 3;
            case 12:
                int nameIndex = readU2(pos + 1);
                if (nameIndex == initIndex) {
                    int descriptorIndex = readU2(pos + 3);
                    if (descriptorIndex == initIndexType) {
                        if (initNameTypeIndex > 0 && initNameTypeIndex != cnt[0]) {
                            throw new IllegalArgumentException("Second initialization of name type index");
                        }
                        initNameTypeIndex = cnt[0];
                    }
                }
                return 5;
            case 10:
                int classname = readU2(pos + 1);
                int nameAndType = readU2(pos + 3);
                if (nameAndType == initNameTypeIndex) {
                    int superclass = readU2(cpEnd + 4);
                    if (superclass == classname) {
                        if (initAttr > 0 && initAttr != pos) {
                            throw new IllegalStateException("Second initialization of position of <init> invocation");
                        }
                        initAttr = pos;
                    }
                }
                return 5;
            case 9:
            case 11:
            case 3:
            case 4:
                return 5;
            case 5:
            case 6:
                cnt[0]++;
                return 9;
            case 1:
                int len = readU2(pos + 1);
                if (compareUtfEntry(BYTE_INIT, pos)) {
                    if (initIndex > 0 && initIndex != cnt[0]) {
                        throw new IllegalArgumentException("Second initialization of " + ATTR_INIT);
                    }
                    initIndex = cnt[0];
                }
                if (compareUtfEntry(BYTE_INIT_TYPE, pos)) {
                    if (initIndexType > 0 && initIndexType != cnt[0]) {
                        throw new IllegalArgumentException("Second initialization of " + ATTR_INIT_TYPE);
                    }
                    initIndexType = cnt[0];
                }
                if (compareUtfEntry(BYTE_SUPERCLASS, pos)) {
                    if (superClassNameIndex > 0 && superClassNameIndex != cnt[0]) {
                        throw new IllegalStateException(ATTR_SUPERCLASS + " registered for the second time!");
                    }
                    superClassNameIndex = cnt[0];
                }
                if (compareUtfEntry(BYTE_INTERFACES, pos)) {
                    if (interfacesNameIndex > 0 && interfacesNameIndex != cnt[0]) {
                        throw new IllegalStateException(ATTR_INTERFACES + " registered for the second time!");
                    }
                    interfacesNameIndex = cnt[0];
                }
                if (compareUtfEntry(BYTE_MEMBER, pos)) {
                    if (memberNameIndex > 0 && memberNameIndex != cnt[0]) {
                        throw new IllegalStateException(ATTR_MEMBER + " registered for the second time!");
                    }
                    memberNameIndex = cnt[0];
                }
                if (compareUtfEntry(BYTE_NAME, pos)) {
                    if (renameNameIndex > 0 && renameNameIndex != cnt[0]) {
                        throw new IllegalStateException(ATTR_NAME + " registered for the second time!");
                    }
                    renameNameIndex = cnt[0];
                }
                if (nameIndexes != null) {
                    String s;
                    try {
                        s = new String(arr, pos + 3, len, "utf-8");
                    } catch (UnsupportedEncodingException ex) {
                        throw new IllegalStateException("utf-8 is always supported");
                    }
                    int[] index = (int[]) nameIndexes.get(s);
                    if (index != null) {
                        index[0] = cnt[0];
                    }
                }
                return len + 3;
            default:
                throw new IllegalStateException("Unknown pool type: " + get(pos));
        }
    }

    private int memberSize(int pos, int[] containsPatchAttributeAndRename) {
        int s = 8;
        int name = readU2(pos + 2);
        int cnt = readU2(pos + 6);
        while (cnt-- > 0) {
            s += attributeSize(pos + s, containsPatchAttributeAndRename);
        }
        return s;
    }

    /** Into the containsPatchAttribute (if not null) it adds the
     * index of structure of attribute ATTR_MEMBER.
     */
    private int attributeSize(int pos, int[] containsPatchAttributeAndRename) {
        int name = readU2(pos);
        if (name == superClassNameIndex) {
            if (superClassNameAttr > 0 && superClassNameAttr != pos) {
                throw new IllegalStateException("Two attributes with name " + ATTR_SUPERCLASS);
            }
            superClassNameAttr = pos;
        }
        if (name == interfacesNameIndex) {
            if (interfacesNameAttr > 0 && interfacesNameAttr != pos) {
                throw new IllegalStateException("Two attributes with name " + ATTR_INTERFACES);
            }
            interfacesNameAttr = pos;
        }
        if (name == memberNameIndex && containsPatchAttributeAndRename != null) {
            if (containsPatchAttributeAndRename[0] != -1) {
                throw new IllegalStateException("Second attribute " + ATTR_MEMBER);
            }
            containsPatchAttributeAndRename[0] = pos;
        }
        if (name == renameNameIndex && containsPatchAttributeAndRename != null) {
            if (containsPatchAttributeAndRename[1] != -1) {
                throw new IllegalStateException("Second attribute " + ATTR_NAME);
            }
            containsPatchAttributeAndRename[1] = pos;
        }
        int len = readU4(pos + 2);
        return len + 6;
    }

    /** Compares arrays.
     */
    private boolean compareUtfEntry(byte[] pattern, int pos) {
        int len = readU2(pos + 1);
        if (pattern.length != len) {
            return false;
        }
        int base = pos + 3;
        for (int i = 0; i < len; i++) {
            if (pattern[i] != arr[base + i]) {
                return false;
            }
        }
        return true;
    }
}
