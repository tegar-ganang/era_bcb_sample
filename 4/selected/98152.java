package org.apache.harmony.unpack200;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.harmony.pack200.Codec;
import org.apache.harmony.pack200.Pack200Exception;
import org.apache.harmony.unpack200.bytecode.Attribute;
import org.apache.harmony.unpack200.bytecode.BCIRenumberedAttribute;
import org.apache.harmony.unpack200.bytecode.ByteCode;
import org.apache.harmony.unpack200.bytecode.CPClass;
import org.apache.harmony.unpack200.bytecode.CodeAttribute;
import org.apache.harmony.unpack200.bytecode.ExceptionTableEntry;
import org.apache.harmony.unpack200.bytecode.NewAttribute;
import org.apache.harmony.unpack200.bytecode.OperandManager;

/**
 * Bytecode bands
 */
public class BcBands extends BandSet {

    private byte[][][] methodByteCodePacked;

    private int[] bcCaseCount;

    private int[] bcCaseValue;

    private int[] bcByte;

    private int[] bcLocal;

    private int[] bcShort;

    private int[] bcLabel;

    private int[] bcIntRef;

    private int[] bcFloatRef;

    private int[] bcLongRef;

    private int[] bcDoubleRef;

    private int[] bcStringRef;

    private int[] bcClassRef;

    private int[] bcFieldRef;

    private int[] bcMethodRef;

    private int[] bcIMethodRef;

    private int[] bcThisField;

    private int[] bcSuperField;

    private int[] bcThisMethod;

    private int[] bcSuperMethod;

    private int[] bcInitRef;

    private int[] bcEscRef;

    private int[] bcEscRefSize;

    private int[] bcEscSize;

    private int[][] bcEscByte;

    private List wideByteCodes;

    /**
     * @param segment
     */
    public BcBands(Segment segment) {
        super(segment);
    }

    public void read(InputStream in) throws IOException, Pack200Exception {
        AttributeLayoutMap attributeDefinitionMap = segment.getAttrDefinitionBands().getAttributeDefinitionMap();
        int classCount = header.getClassCount();
        long[][] methodFlags = segment.getClassBands().getMethodFlags();
        int bcCaseCountCount = 0;
        int bcByteCount = 0;
        int bcShortCount = 0;
        int bcLocalCount = 0;
        int bcLabelCount = 0;
        int bcIntRefCount = 0;
        int bcFloatRefCount = 0;
        int bcLongRefCount = 0;
        int bcDoubleRefCount = 0;
        int bcStringRefCount = 0;
        int bcClassRefCount = 0;
        int bcFieldRefCount = 0;
        int bcMethodRefCount = 0;
        int bcIMethodRefCount = 0;
        int bcThisFieldCount = 0;
        int bcSuperFieldCount = 0;
        int bcThisMethodCount = 0;
        int bcSuperMethodCount = 0;
        int bcInitRefCount = 0;
        int bcEscCount = 0;
        int bcEscRefCount = 0;
        AttributeLayout abstractModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_ABSTRACT, AttributeLayout.CONTEXT_METHOD);
        AttributeLayout nativeModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_NATIVE, AttributeLayout.CONTEXT_METHOD);
        methodByteCodePacked = new byte[classCount][][];
        int bcParsed = 0;
        List switchIsTableSwitch = new ArrayList();
        wideByteCodes = new ArrayList();
        for (int c = 0; c < classCount; c++) {
            int numberOfMethods = methodFlags[c].length;
            methodByteCodePacked[c] = new byte[numberOfMethods][];
            for (int m = 0; m < numberOfMethods; m++) {
                long methodFlag = methodFlags[c][m];
                if (!abstractModifier.matches(methodFlag) && !nativeModifier.matches(methodFlag)) {
                    ByteArrayOutputStream codeBytes = new ByteArrayOutputStream();
                    byte code;
                    while ((code = (byte) (0xff & in.read())) != -1) codeBytes.write(code);
                    methodByteCodePacked[c][m] = codeBytes.toByteArray();
                    bcParsed += methodByteCodePacked[c][m].length;
                    int[] codes = new int[methodByteCodePacked[c][m].length];
                    for (int i = 0; i < codes.length; i++) {
                        codes[i] = methodByteCodePacked[c][m][i] & 0xff;
                    }
                    for (int i = 0; i < methodByteCodePacked[c][m].length; i++) {
                        int codePacked = 0xff & methodByteCodePacked[c][m][i];
                        switch(codePacked) {
                            case 16:
                            case 188:
                                bcByteCount++;
                                break;
                            case 17:
                                bcShortCount++;
                                break;
                            case 18:
                            case 19:
                                bcStringRefCount++;
                                break;
                            case 234:
                            case 237:
                                bcIntRefCount++;
                                break;
                            case 235:
                            case 238:
                                bcFloatRefCount++;
                                break;
                            case 197:
                                bcByteCount++;
                            case 233:
                            case 236:
                            case 187:
                            case 189:
                            case 192:
                            case 193:
                                bcClassRefCount++;
                                break;
                            case 20:
                                bcLongRefCount++;
                                break;
                            case 239:
                                bcDoubleRefCount++;
                                break;
                            case 169:
                                bcLocalCount++;
                                break;
                            case 167:
                            case 168:
                            case 200:
                            case 201:
                                bcLabelCount++;
                                break;
                            case 170:
                                switchIsTableSwitch.add(new Boolean(true));
                                bcCaseCountCount++;
                                bcLabelCount++;
                                break;
                            case 171:
                                switchIsTableSwitch.add(new Boolean(false));
                                bcCaseCountCount++;
                                bcLabelCount++;
                                break;
                            case 178:
                            case 179:
                            case 180:
                            case 181:
                                bcFieldRefCount++;
                                break;
                            case 182:
                            case 183:
                            case 184:
                                bcMethodRefCount++;
                                break;
                            case 185:
                                bcIMethodRefCount++;
                                break;
                            case 202:
                            case 203:
                            case 204:
                            case 205:
                            case 209:
                            case 210:
                            case 211:
                            case 212:
                                bcThisFieldCount++;
                                break;
                            case 206:
                            case 207:
                            case 208:
                            case 213:
                            case 214:
                            case 215:
                                bcThisMethodCount++;
                                break;
                            case 216:
                            case 217:
                            case 218:
                            case 219:
                            case 223:
                            case 224:
                            case 225:
                            case 226:
                                bcSuperFieldCount++;
                                break;
                            case 220:
                            case 221:
                            case 222:
                            case 227:
                            case 228:
                            case 229:
                                bcSuperMethodCount++;
                                break;
                            case 132:
                                bcLocalCount++;
                                bcByteCount++;
                                break;
                            case 196:
                                int nextInstruction = 0xff & methodByteCodePacked[c][m][i + 1];
                                wideByteCodes.add(new Integer(nextInstruction));
                                if (nextInstruction == 132) {
                                    bcLocalCount++;
                                    bcShortCount++;
                                } else if (endsWithLoad(nextInstruction) || endsWithStore(nextInstruction) || nextInstruction == 169) {
                                    bcLocalCount++;
                                } else {
                                    segment.log(Segment.LOG_LEVEL_VERBOSE, "Found unhandled " + ByteCode.getByteCode(nextInstruction));
                                }
                                i++;
                                break;
                            case 230:
                            case 231:
                            case 232:
                                bcInitRefCount++;
                                break;
                            case 253:
                                bcEscRefCount++;
                                break;
                            case 254:
                                bcEscCount++;
                                break;
                            default:
                                if (endsWithLoad(codePacked) || endsWithStore(codePacked)) {
                                    bcLocalCount++;
                                } else if (startsWithIf(codePacked)) {
                                    bcLabelCount++;
                                }
                        }
                    }
                }
            }
        }
        bcCaseCount = decodeBandInt("bc_case_count", in, Codec.UNSIGNED5, bcCaseCountCount);
        int bcCaseValueCount = 0;
        for (int i = 0; i < bcCaseCount.length; i++) {
            boolean isTableSwitch = ((Boolean) switchIsTableSwitch.get(i)).booleanValue();
            if (isTableSwitch) {
                bcCaseValueCount += 1;
            } else {
                bcCaseValueCount += bcCaseCount[i];
            }
        }
        bcCaseValue = decodeBandInt("bc_case_value", in, Codec.DELTA5, bcCaseValueCount);
        for (int index = 0; index < bcCaseCountCount; index++) {
            bcLabelCount += bcCaseCount[index];
        }
        bcByte = decodeBandInt("bc_byte", in, Codec.BYTE1, bcByteCount);
        bcShort = decodeBandInt("bc_short", in, Codec.DELTA5, bcShortCount);
        bcLocal = decodeBandInt("bc_local", in, Codec.UNSIGNED5, bcLocalCount);
        bcLabel = decodeBandInt("bc_label", in, Codec.BRANCH5, bcLabelCount);
        bcIntRef = decodeBandInt("bc_intref", in, Codec.DELTA5, bcIntRefCount);
        bcFloatRef = decodeBandInt("bc_floatref", in, Codec.DELTA5, bcFloatRefCount);
        bcLongRef = decodeBandInt("bc_longref", in, Codec.DELTA5, bcLongRefCount);
        bcDoubleRef = decodeBandInt("bc_doubleref", in, Codec.DELTA5, bcDoubleRefCount);
        bcStringRef = decodeBandInt("bc_stringref", in, Codec.DELTA5, bcStringRefCount);
        bcClassRef = decodeBandInt("bc_classref", in, Codec.UNSIGNED5, bcClassRefCount);
        bcFieldRef = decodeBandInt("bc_fieldref", in, Codec.DELTA5, bcFieldRefCount);
        bcMethodRef = decodeBandInt("bc_methodref", in, Codec.UNSIGNED5, bcMethodRefCount);
        bcIMethodRef = decodeBandInt("bc_imethodref", in, Codec.DELTA5, bcIMethodRefCount);
        bcThisField = decodeBandInt("bc_thisfield", in, Codec.UNSIGNED5, bcThisFieldCount);
        bcSuperField = decodeBandInt("bc_superfield", in, Codec.UNSIGNED5, bcSuperFieldCount);
        bcThisMethod = decodeBandInt("bc_thismethod", in, Codec.UNSIGNED5, bcThisMethodCount);
        bcSuperMethod = decodeBandInt("bc_supermethod", in, Codec.UNSIGNED5, bcSuperMethodCount);
        bcInitRef = decodeBandInt("bc_initref", in, Codec.UNSIGNED5, bcInitRefCount);
        bcEscRef = decodeBandInt("bc_escref", in, Codec.UNSIGNED5, bcEscRefCount);
        bcEscRefSize = decodeBandInt("bc_escrefsize", in, Codec.UNSIGNED5, bcEscRefCount);
        bcEscSize = decodeBandInt("bc_escsize", in, Codec.UNSIGNED5, bcEscCount);
        bcEscByte = decodeBandInt("bc_escbyte", in, Codec.BYTE1, bcEscSize);
    }

    public void unpack() throws Pack200Exception {
        int classCount = header.getClassCount();
        long[][] methodFlags = segment.getClassBands().getMethodFlags();
        int[] codeMaxNALocals = segment.getClassBands().getCodeMaxNALocals();
        int[] codeMaxStack = segment.getClassBands().getCodeMaxStack();
        ArrayList[][] methodAttributes = segment.getClassBands().getMethodAttributes();
        String[][] methodDescr = segment.getClassBands().getMethodDescr();
        AttributeLayoutMap attributeDefinitionMap = segment.getAttrDefinitionBands().getAttributeDefinitionMap();
        AttributeLayout abstractModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_ABSTRACT, AttributeLayout.CONTEXT_METHOD);
        AttributeLayout nativeModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_NATIVE, AttributeLayout.CONTEXT_METHOD);
        AttributeLayout staticModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_STATIC, AttributeLayout.CONTEXT_METHOD);
        int[] wideByteCodeArray = new int[wideByteCodes.size()];
        for (int index = 0; index < wideByteCodeArray.length; index++) {
            wideByteCodeArray[index] = ((Integer) wideByteCodes.get(index)).intValue();
        }
        OperandManager operandManager = new OperandManager(bcCaseCount, bcCaseValue, bcByte, bcShort, bcLocal, bcLabel, bcIntRef, bcFloatRef, bcLongRef, bcDoubleRef, bcStringRef, bcClassRef, bcFieldRef, bcMethodRef, bcIMethodRef, bcThisField, bcSuperField, bcThisMethod, bcSuperMethod, bcInitRef, wideByteCodeArray);
        operandManager.setSegment(segment);
        int i = 0;
        ArrayList orderedCodeAttributes = segment.getClassBands().getOrderedCodeAttributes();
        int codeAttributeIndex = 0;
        int[] handlerCount = segment.getClassBands().getCodeHandlerCount();
        int[][] handlerStartPCs = segment.getClassBands().getCodeHandlerStartP();
        int[][] handlerEndPCs = segment.getClassBands().getCodeHandlerEndPO();
        int[][] handlerCatchPCs = segment.getClassBands().getCodeHandlerCatchPO();
        int[][] handlerClassTypes = segment.getClassBands().getCodeHandlerClassRCN();
        boolean allCodeHasFlags = segment.getSegmentHeader().getOptions().hasAllCodeFlags();
        boolean[] codeHasFlags = segment.getClassBands().getCodeHasAttributes();
        for (int c = 0; c < classCount; c++) {
            int numberOfMethods = methodFlags[c].length;
            for (int m = 0; m < numberOfMethods; m++) {
                long methodFlag = methodFlags[c][m];
                if (!abstractModifier.matches(methodFlag) && !nativeModifier.matches(methodFlag)) {
                    int maxStack = codeMaxStack[i];
                    int maxLocal = codeMaxNALocals[i];
                    if (!staticModifier.matches(methodFlag)) maxLocal++;
                    maxLocal += SegmentUtils.countInvokeInterfaceArgs(methodDescr[c][m]);
                    String[] cpClass = segment.getCpBands().getCpClass();
                    operandManager.setCurrentClass(cpClass[segment.getClassBands().getClassThisInts()[c]]);
                    operandManager.setSuperClass(cpClass[segment.getClassBands().getClassSuperInts()[c]]);
                    List exceptionTable = new ArrayList();
                    if (handlerCount != null) {
                        for (int j = 0; j < handlerCount[i]; j++) {
                            int handlerClass = handlerClassTypes[i][j] - 1;
                            CPClass cpHandlerClass = null;
                            if (handlerClass != -1) {
                                cpHandlerClass = segment.getCpBands().cpClassValue(handlerClass);
                            }
                            ExceptionTableEntry entry = new ExceptionTableEntry(handlerStartPCs[i][j], handlerEndPCs[i][j], handlerCatchPCs[i][j], cpHandlerClass);
                            exceptionTable.add(entry);
                        }
                    }
                    CodeAttribute codeAttr = new CodeAttribute(maxStack, maxLocal, methodByteCodePacked[c][m], segment, operandManager, exceptionTable);
                    ArrayList methodAttributesList = methodAttributes[c][m];
                    int indexForCodeAttr = 0;
                    for (int index = 0; index < methodAttributesList.size(); index++) {
                        Attribute attribute = (Attribute) methodAttributesList.get(index);
                        if ((attribute instanceof NewAttribute && ((NewAttribute) attribute).getLayoutIndex() < 15)) {
                            indexForCodeAttr++;
                        } else {
                            break;
                        }
                    }
                    methodAttributesList.add(indexForCodeAttr, codeAttr);
                    codeAttr.renumber(codeAttr.byteCodeOffsets);
                    List currentAttributes;
                    if (allCodeHasFlags) {
                        currentAttributes = (List) orderedCodeAttributes.get(i);
                    } else {
                        if (codeHasFlags[i]) {
                            currentAttributes = (List) orderedCodeAttributes.get(codeAttributeIndex);
                            codeAttributeIndex++;
                        } else {
                            currentAttributes = Collections.EMPTY_LIST;
                        }
                    }
                    for (int index = 0; index < currentAttributes.size(); index++) {
                        Attribute currentAttribute = (Attribute) currentAttributes.get(index);
                        codeAttr.addAttribute(currentAttribute);
                        if (currentAttribute.hasBCIRenumbering()) {
                            ((BCIRenumberedAttribute) currentAttribute).renumber(codeAttr.byteCodeOffsets);
                        }
                    }
                    i++;
                }
            }
        }
    }

    private boolean startsWithIf(int codePacked) {
        return (codePacked >= 153 && codePacked <= 166) || (codePacked == 198) || (codePacked == 199);
    }

    private boolean endsWithLoad(int codePacked) {
        return (codePacked >= 21 && codePacked <= 25);
    }

    private boolean endsWithStore(int codePacked) {
        return (codePacked >= 54 && codePacked <= 58);
    }

    public byte[][][] getMethodByteCodePacked() {
        return methodByteCodePacked;
    }

    public int[] getBcCaseCount() {
        return bcCaseCount;
    }

    public int[] getBcCaseValue() {
        return bcCaseValue;
    }

    public int[] getBcByte() {
        return bcByte;
    }

    public int[] getBcClassRef() {
        return bcClassRef;
    }

    public int[] getBcDoubleRef() {
        return bcDoubleRef;
    }

    public int[] getBcFieldRef() {
        return bcFieldRef;
    }

    public int[] getBcFloatRef() {
        return bcFloatRef;
    }

    public int[] getBcIMethodRef() {
        return bcIMethodRef;
    }

    public int[] getBcInitRef() {
        return bcInitRef;
    }

    public int[] getBcIntRef() {
        return bcIntRef;
    }

    public int[] getBcLabel() {
        return bcLabel;
    }

    public int[] getBcLocal() {
        return bcLocal;
    }

    public int[] getBcLongRef() {
        return bcLongRef;
    }

    public int[] getBcMethodRef() {
        return bcMethodRef;
    }

    public int[] getBcShort() {
        return bcShort;
    }

    public int[] getBcStringRef() {
        return bcStringRef;
    }

    public int[] getBcSuperField() {
        return bcSuperField;
    }

    public int[] getBcSuperMethod() {
        return bcSuperMethod;
    }

    public int[] getBcThisField() {
        return bcThisField;
    }

    public int[] getBcThisMethod() {
        return bcThisMethod;
    }

    public int[] getBcEscRef() {
        return bcEscRef;
    }

    public int[] getBcEscRefSize() {
        return bcEscRefSize;
    }

    public int[] getBcEscSize() {
        return bcEscSize;
    }

    public int[][] getBcEscByte() {
        return bcEscByte;
    }
}
