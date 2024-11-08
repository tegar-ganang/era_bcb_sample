import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class ClassDef {

    public boolean isUnion = false;

    public String name = new String();

    public Vector<MemberDef> members = new Vector<MemberDef>();

    public static Map<String, Integer> messageLengthInfo = new HashMap<String, Integer>();

    public String baseClass = "";

    public String baseMember = "";

    public String lastMember = "";

    public String lastMemberArraySize = "";

    private boolean hasSpan;

    private boolean hasChannel;

    private boolean hasSpanB;

    private boolean hasChannelB;

    public boolean hasAIB;

    public String getMembers() {
        String str = new String("    // ----------------------- Member Declaration -----------------\n");
        Iterator<MemberDef> iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            str += temp.getDeclaration();
        }
        return str;
    }

    public String getOperations() {
        String str = new String("    // ----------------------- Operations -----------------\n");
        Iterator<MemberDef> iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            if (temp.Name.equals("Span") || temp.Name.equals("SpanA")) {
                hasSpan = true;
            }
            if (temp.Name.equals("Channel") || temp.Name.equals("ChannelA")) {
                hasChannel = true;
            }
            if (temp.Name.equals("SpanB")) {
                hasSpanB = true;
            }
            if (temp.Name.equals("ChannelB")) {
                hasChannelB = true;
            }
            if (temp.Name.equals("AddrInfo")) {
                hasAIB = true;
            }
            str += temp.getOperations();
        }
        if (hasAIB) {
            str += "    // Is this message a one channel message\n";
            str += "    public boolean isOneChannelMessage() {\n" + "        return ((m_AddrInfo[0] == 0x00) && (m_AddrInfo[1] == 0x01) &&\n" + "            (m_AddrInfo[2] == 0x0d) && (m_AddrInfo[3] == 0x03));\n" + "    }\n\n";
            str += "    // Is this message a two channel message\n";
            str += "    public boolean isTwoChannelMessage() {\n" + "        return ((m_AddrInfo[0] == 0x00) && (m_AddrInfo[1] == 0x02) &&\n" + "            (m_AddrInfo[2] == 0x0d) && (m_AddrInfo[3] == 0x03) &&\n" + "            (m_AddrInfo[7] == 0x0d) && (m_AddrInfo[8] == 0x03));\n" + "    }\n\n";
            str += "    // Is this a channel related message at all?\n" + "    public boolean isChannelRelatedMessage() {\n" + "        return isOneChannelMessage() || isTwoChannelMessage();\n" + "    }\n\n";
            if (name.equals("XL_PPLEventIndication") || name.equals("XL_PPLEventRequest") || name.equals("XL_DS0StatusChange") || name.equals("XL_PlayFileStart") || name.equals("XL_PlayFileStop") || name.equals("XL_RecordFileStart") || name.equals("XL_RecordFileStop") || name.equals("XL_CallProcessingEvent")) {
                str += "    public int getSpan() {\n" + "        if (isChannelRelatedMessage()) {\n" + "            return (m_AddrInfo[4] << 8) | m_AddrInfo[5];\n" + "        } else {\n" + "            return -1;\n" + "        }\n" + "    };\n";
                str += "    public byte getChannel() {\n" + "        if (isChannelRelatedMessage()) {\n" + "            return m_AddrInfo[6];\n" + "        } else {\n" + "            return -1;\n" + "        }\n" + "    };\n";
                str += "    public int getSpanA() {\n" + "        if (isTwoChannelMessage()) {\n" + "            return (m_AddrInfo[4] << 8) | m_AddrInfo[5];\n" + "        } else {\n" + "            return -1;\n" + "        }\n" + "    };\n";
                str += "    public byte getChannelA() {\n" + "        if (isTwoChannelMessage()) {\n" + "            return m_AddrInfo[6];\n" + "        } else {\n" + "            return -1;\n" + "        }\n" + "    };\n";
                str += "    public int getSpanB() {\n" + "        if (isTwoChannelMessage()) {\n" + "            return (m_AddrInfo[9] << 8) | m_AddrInfo[10];\n" + "        } else {\n" + "            return -1;\n" + "        }\n" + "    };\n";
                str += "    public byte getChannelB() {\n" + "        if (isTwoChannelMessage()) {\n" + "            return m_AddrInfo[11];\n" + "        } else {\n" + "            return -1;\n" + "        }\n" + "    };\n";
                str += "    public void setSpanChannel(int Span, byte Channel) {\n" + "        m_AddrInfo[0] = 0x00; \n" + "        m_AddrInfo[1] = 0x01; \n" + "        m_AddrInfo[2] = 0x0D; \n" + "        m_AddrInfo[3] = 0x03; \n" + "        m_AddrInfo[4] = (byte)((Span >> 8) & 0xFF); \n" + "        m_AddrInfo[5] = (byte)(Span & 0xFF); \n" + "        m_AddrInfo[6] = (byte)(Channel & 0xFF); \n" + "    };\n";
                str += "    public void setSpanChannels(int SpanA, byte ChannelA, int SpanB, byte ChannelB) {\n" + "        m_AddrInfo[0] = 0x00; \n" + "        m_AddrInfo[1] = 0x02; \n" + "        m_AddrInfo[2] = 0x0D; \n" + "        m_AddrInfo[3] = 0x03; \n" + "        m_AddrInfo[4] = (byte)((SpanA >> 8) & 0xFF); \n" + "        m_AddrInfo[5] = (byte)(SpanA & 0xFF); \n" + "        m_AddrInfo[6] = (byte)(ChannelA & 0xFF); \n" + "        m_AddrInfo[7] = 0x0D; \n" + "        m_AddrInfo[8] = 0x03; \n" + "        m_AddrInfo[9] = (byte)((SpanB >> 8) & 0xFF); \n" + "        m_AddrInfo[10] = (byte)(SpanB & 0xFF); \n" + "        m_AddrInfo[11] = (byte)(ChannelB & 0xFF); \n" + "    };\n";
            }
        } else {
            if (hasSpan && hasChannel) {
                str += "    // Is this message a channel related message?\n";
                str += "    public boolean isChannelRelatedMessage() { return true; };\n\n";
            }
            if (hasSpanB && hasChannelB) {
                str += "    // Is this message a two channel message\n";
                str += "    public boolean isTwoChannelMessage() { return true; };\n\n";
                str += "    // Is this message a one channel message\n";
                str += "    public boolean isOneChannelMessage() { return false; };\n";
            } else if (hasSpan && hasChannel) {
                str += "    // Is this message a two channel message\n";
                str += "    public boolean isTwoChannelMessage() { return false; };\n\n";
                str += "    // Is this message a one channel message\n";
                str += "    public boolean isOneChannelMessage() { return true; };\n\n";
            }
        }
        return str;
    }

    public String getJNICodeFromJava() {
        String declaration = "MsgStruct *ConvertToJNI_" + name + "(JNIEnv *env, jobject obj, MsgStruct *_msg) {\n" + "  static int initialized = 0;\n";
        String varDef = "  " + name + " *msg = NULL;\n";
        String body = "";
        boolean hasByteArray = false;
        boolean hasStringVar = false;
        varDef += "  static jclass cid = NULL;\n";
        Iterator<MemberDef> iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            varDef += "  static jfieldID fid_" + ConverterVisitor.mappedName(temp.Name) + ";\n";
        }
        body += "\n  if (!initialized) {\n\n" + "    cid = (*env)->GetObjectClass(env, obj);\n" + "    if ((*env)->ExceptionOccurred(env)) {\n" + "      (*env)->ExceptionDescribe(env);\n" + "      return NULL;\n" + "    }\n" + "    if (!cid) { ThrowException(env, \"Couldn't locate object's class in " + name + " conversion\"); }\n\n";
        iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            body += "    fid_" + ConverterVisitor.mappedName(temp.Name) + " = (*env)->GetFieldID(env, cid, \"m_" + ConverterVisitor.mappedName(temp.Name) + "\",\"" + ((temp.isArray && (temp.Signature != 'R')) ? "[" : "") + ((temp.Signature == 'R') ? "Ljava/lang/String;" : temp.Signature) + "\");\n" + "    if ((*env)->ExceptionOccurred(env)) {\n" + "      (*env)->ExceptionDescribe(env);\n" + "       return NULL;\n" + "    }\n" + "    if (!fid_" + ConverterVisitor.mappedName(temp.Name) + ") { ThrowException(env, \"Could not locate field 'm_" + ConverterVisitor.mappedName(temp.Name) + "' in class " + name + "\"); }\n\n";
        }
        body += "    initialized = 1;\n\n" + "  }\n\n";
        body += "  if (!_msg) {\n" + "    msg = (" + name + " *)malloc(sizeof(" + name + "));\n";
        if (!name.equals("BaseFields") && !name.equals("XL_AcknowledgeMessage")) {
            body += "    sk_initMsg(msg, TAG_" + name.replaceFirst("(SK_)|(XL_)", "") + ");\n";
        }
        body += "  } else {\n" + "    msg = (" + name + " *)_msg;\n" + "  }\n\n";
        if ((null != baseClass) && !(baseClass.length() == 0) && !baseClass.equals("SKJMessage")) {
            body += "  // Call base-class initialization\n" + "  ConvertToJNI_" + baseClass + "(env, obj, (MsgStruct *)msg);\n\n";
        }
        iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            String tempName = ConverterVisitor.mappedName(temp.Name);
            if (tempName.equals("Type") || tempName.equals("Tag") || tempName.equals("SeqNum") || tempName.equals("Size") || tempName.equals("EngineName")) {
            } else if (!temp.isArray) {
                body += "  msg->" + temp.Name + " = (*env)->Get" + MemberDef.javaTypeName(temp.Signature) + "Field(env, obj, fid_" + ConverterVisitor.mappedName(temp.Name) + ");\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (msg) free(msg);\n;" + "     return NULL;\n" + "  }\n";
            } else {
                if (temp.Signature == 'B') {
                    if (!hasByteArray) {
                        hasByteArray = true;
                        varDef += "  jbyteArray byteArray;\n" + "  jbyte     *byteArrayData;\n" + "  jint       byteArrayLen;\n";
                    }
                    body += "  // -- Begin processing array: " + ConverterVisitor.mappedName(temp.Name) + "\n" + "  byteArray = (*env)->GetObjectField(env, obj, fid_" + ConverterVisitor.mappedName(temp.Name) + ");\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (msg) free(msg);\n;" + "     return NULL;\n" + "  }\n" + "  // Make sure there is no overrun of data\n" + "  if ((byteArrayLen = (*env)->GetArrayLength(env, byteArray)) > " + temp.Array + ") {\n";
                    if (temp.Name.equals(lastMember)) {
                        body += "    // We can try to realloc the memory, since this is the last member\n" + "    msg = (" + name + " *)realloc(msg, sizeof(" + name + ") + byteArrayLen - " + temp.Array + " + 10);\n" + "    if (!msg) {\n" + "      ThrowException(env, \"Array size overrun on field " + ConverterVisitor.mappedName(temp.Name) + ".  Max allowed is " + temp.Array + ", and realloc() failed!\");\n" + "    }\n";
                    } else {
                        body += "    if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "    ThrowException(env, \"Array size overrun on field " + ConverterVisitor.mappedName(temp.Name) + ".  Max allowed is " + temp.Array + "\");\n";
                    }
                    body += "  }\n" + "  byteArrayData = (*env)->Get" + MemberDef.javaTypeName(temp.Signature) + "ArrayElements(env, byteArray, 0);\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (msg) free(msg);\n;" + "     return NULL;\n" + "  }\n" + "  memcpy(msg->" + temp.Name + ", byteArrayData, byteArrayLen);\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (msg) free(msg);\n;" + "     return NULL;\n" + "  }\n" + "  (*env)->Release" + MemberDef.javaTypeName(temp.Signature) + "ArrayElements(env, byteArray, byteArrayData, 0);\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (msg) free(msg);\n;" + "     return NULL;\n" + "  }\n\n" + "  // We dont' need this reference any more\n" + "  (*env)->DeleteLocalRef(env, byteArray);\n" + "  // -- End processing array: " + ConverterVisitor.mappedName(temp.Name) + "\n";
                } else if (temp.Signature == 'R') {
                    if (!hasStringVar) {
                        hasStringVar = true;
                        varDef += "  jstring    stringMember;\n";
                    }
                    body += "  stringMember = (*env)->GetObjectField(env, obj, fid_" + temp.Name + ");\n";
                    body += "  if (stringMember) {\n";
                    body += "     const char *" + temp.Name + " = (*env)->GetStringUTFChars(env, stringMember, NULL);\n";
                    body += "     strncpy(msg->" + temp.Name + ", " + temp.Name + ", " + temp.Array + ");\n";
                    body += "     (*env)->ReleaseStringUTFChars(env, stringMember, " + temp.Name + ");\n";
                    body += "     (*env)->DeleteLocalRef(env, stringMember);\n";
                    body += "  }\n";
                } else {
                    System.err.println("**** UNKNOW ARRAY TYPE: FIELD => " + name + "." + temp.Name + ", Type: " + temp.Signature);
                }
            }
        }
        body += "  return (MsgStruct *)msg;\n";
        body += "}\n";
        return declaration + varDef + body;
    }

    public String getJNICodeToJava() {
        boolean hasByteArray = false;
        boolean hasString = false;
        String declaration = "jobject ConvertToJava_" + name + "(JNIEnv *env, MsgStruct *_msg, jobject obj) {\n" + "  // Has this class converter been initialized?\n" + "  static int initialized = 0;\n";
        String body = "";
        String varDef = "  // Class ID of this class\n" + "  static jclass cid = NULL;\n" + "  // Constructor's method id\n" + "  static jmethodID mid_ctor_" + name + ";\n\n" + "  // Type cast our message, so that we can get data out of it\n" + "  " + name + " *msg = (" + name + " *)_msg;\n\n" + "  // Other variables used in this function\n";
        Iterator<MemberDef> iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            varDef += "  static jfieldID fid_" + ConverterVisitor.mappedName(temp.Name) + ";\n";
        }
        body += "\n  if (!initialized) {\n\n" + "    cid = (*env)->FindClass(env, \"com/rubixinfotech/SKJava/Messages/EXS/" + name + "\");\n" + "    if ((*env)->ExceptionOccurred(env)) {\n" + "      (*env)->ExceptionDescribe(env);\n" + "      return NULL;\n" + "    }\n" + "    if (!cid) { ThrowException(env, \"Couldn't locate class " + name + " for conversion\"); }\n\n";
        iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            body += "    fid_" + ConverterVisitor.mappedName(temp.Name) + " = (*env)->GetFieldID(env, cid, \"m_" + ConverterVisitor.mappedName(temp.Name) + "\",\"" + ((temp.isArray && (temp.Signature != 'R')) ? "[" : "") + ((temp.Signature == 'R') ? "Ljava/lang/String;" : temp.Signature) + "\");\n" + "    if ((*env)->ExceptionOccurred(env)) {\n" + "      (*env)->ExceptionDescribe(env);\n" + "       return NULL;\n" + "    }\n" + "     if (!fid_" + ConverterVisitor.mappedName(temp.Name) + ") { ThrowException(env, \"Could not locate field 'm_" + ConverterVisitor.mappedName(temp.Name) + "' in class " + name + "\"); }\n\n";
        }
        body += "    mid_ctor_" + name + " = (*env)->GetMethodID(env, cid, \"<init>\", \"()V\");\n" + "    if ((*env)->ExceptionOccurred(env)) {\n" + "      (*env)->ExceptionDescribe(env);\n" + "       return NULL;\n" + "    }\n" + "     if (!mid_ctor_" + name + ") { ThrowException(env, \"Could not locate constructor for class '" + name + "'\"); }\n\n";
        body += "    initialized = 1;\n\n" + "  }\n\n";
        body += "  if (!obj) {\n" + "    obj = (*env)->NewObject(env, cid, mid_ctor_" + name + ");\n" + "  }\n\n";
        if ((null != baseClass) && !(baseClass.length() == 0) && !baseClass.equals("SKJMessage")) {
            body += "  // Perform base class initialization\n" + "  ConvertToJava_" + baseClass + "(env, (MsgStruct *)&(msg->" + baseMember + "), obj);\n\n";
        }
        iterator = members.iterator();
        while (iterator.hasNext()) {
            MemberDef temp = iterator.next();
            if (!temp.isArray) {
                body += "  (*env)->Set" + MemberDef.javaTypeName(temp.Signature) + "Field(env, obj, fid_" + ConverterVisitor.mappedName(temp.Name) + ", msg->" + temp.Name + ");\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (obj) (*env)->DeleteLocalRef(env, obj);\n" + "     return NULL;\n" + "  }\n\n";
            } else {
                if (temp.Signature == 'B') {
                    if (!hasByteArray) {
                        hasByteArray = true;
                        varDef += "  jbyteArray byteArray;\n" + "  jbyte     *byteArrayData;\n" + "  jint       byteArrayLen;\n";
                    }
                    body += "  // -- Begin processing array: " + ConverterVisitor.mappedName(temp.Name) + "\n";
                    if (temp.Name.equals(lastMember)) {
                        if (messageLengthInfo.containsKey(name)) {
                            body += "  byteArrayLen = msg->Base.Size - " + messageLengthInfo.get(name) + "; // Bytes parsed out already in " + name + "\n";
                        } else {
                            body += "  byteArrayLen = " + temp.Array + ";\n";
                        }
                    } else {
                        body += "  byteArrayLen = " + temp.Array + ";\n";
                    }
                    body += "  byteArray = (*env)->NewByteArray(env, byteArrayLen);\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (obj) (*env)->DeleteLocalRef(env, obj);\n" + "     return NULL;\n" + "  }\n" + "  (*env)->SetByteArrayRegion(env, byteArray, 0, byteArrayLen, (jbyte *)msg->" + temp.Name + ");\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (obj) (*env)->DeleteLocalRef(env, obj);\n" + "     return NULL;\n" + "  }\n" + "  (*env)->SetObjectField(env, obj, fid_" + ConverterVisitor.mappedName(temp.Name) + ", byteArray);\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (byteArray) (*env)->DeleteLocalRef(env, byteArray);\n" + "     if (obj) (*env)->DeleteLocalRef(env, obj);\n" + "     return NULL;\n" + "  }\n" + "  (*env)->DeleteLocalRef(env, byteArray);\n" + "  // -- End processing array: " + ConverterVisitor.mappedName(temp.Name) + "\n";
                } else if (temp.Signature == 'R') {
                    if (!hasString) {
                        hasString = true;
                        varDef += "  jstring   stringVar;\n";
                        varDef += "  int       maxStringLen;\n";
                    }
                    body += "  // -- Begin processing char array " + ConverterVisitor.mappedName(temp.Name) + " as a string\n";
                    if (temp.Name.equals(lastMember)) {
                        if (messageLengthInfo.containsKey(name)) {
                            body += "  maxStringLen = msg->Base.Size - " + messageLengthInfo.get(name) + "; // chars parsed out already in " + name + "\n";
                        } else {
                            body += "  maxStringLen = " + temp.Array + ";\n";
                        }
                    } else {
                        body += "  maxStringLen = " + temp.Array + ";\n";
                    }
                    body += "  stringVar = (*env)->NewStringUTF(env, msg->" + temp.Name + ");\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (obj) (*env)->DeleteLocalRef(env, obj);\n" + "     return NULL;\n" + "  }\n" + "  " + "  (*env)->SetObjectField(env, obj, fid_" + ConverterVisitor.mappedName(temp.Name) + ", stringVar);\n" + "  if ((*env)->ExceptionOccurred(env)) {\n" + "    (*env)->ExceptionDescribe(env);\n" + "     if (stringVar) (*env)->DeleteLocalRef(env, stringVar);\n" + "     if (obj) (*env)->DeleteLocalRef(env, obj);\n" + "     return NULL;\n" + "  }\n" + "  (*env)->DeleteLocalRef(env, stringVar);\n" + "  // -- End processing char array  " + ConverterVisitor.mappedName(temp.Name) + "\n";
                } else {
                    System.err.println("**** UNKNOW ARRAY TYPE: FIELD => " + name + "." + temp.Name + ", Type: " + temp.Signature);
                }
            }
        }
        body += "  return obj;\n";
        body += "}\n";
        return declaration + varDef + body;
    }
}
