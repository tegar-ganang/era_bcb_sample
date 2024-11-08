package org.zmpp.instructions;

import org.zmpp.base.MemoryAccess;
import org.zmpp.encoding.ZCharEncoder;
import org.zmpp.sound.SoundSystem;
import org.zmpp.vm.Machine;
import org.zmpp.vm.ScreenModel;
import org.zmpp.vm.TextCursor;
import org.zmpp.vm.ZObject;

/**
 * This class represents instructions of type VARIABLE.
 * 
 * @author Wei-ju Wu
 * @version 1.0
 */
public class VariableInstruction extends AbstractInstruction {

    /**
   * The operand count.
   */
    private OperandCount operandCount;

    /**
   * Constructor.
   * 
   * @param machineState a reference to a MachineState object
   * @param operandCount the operand count
   * @param opcode the instruction's opcode
   */
    public VariableInstruction(Machine machineState, OperandCount operandCount, int opcode) {
        super(machineState, opcode);
        this.operandCount = operandCount;
    }

    /**
   * {@inheritDoc}
   */
    public InstructionForm getInstructionForm() {
        return InstructionForm.VARIABLE;
    }

    /**
   * {@inheritDoc}
   */
    public OperandCount getOperandCount() {
        return operandCount;
    }

    /**
   * {@inheritDoc}
   */
    protected InstructionStaticInfo getStaticInfo() {
        return VariableStaticInfo.getInstance();
    }

    /**
   * Returns the memory access object.
   * 
   * @return the memory access object
   */
    private MemoryAccess getMemoryAccess() {
        return getMachine().getServices().getMemoryAccess();
    }

    /**
   * {@inheritDoc}
   */
    protected InstructionResult doInstruction() {
        switch(getOpcode()) {
            case VariableStaticInfo.OP_CALL:
                call();
                break;
            case VariableStaticInfo.OP_CALL_VS2:
                call();
                break;
            case VariableStaticInfo.OP_STOREW:
                storew();
                break;
            case VariableStaticInfo.OP_STOREB:
                storeb();
                break;
            case VariableStaticInfo.OP_PUT_PROP:
                put_prop();
                break;
            case VariableStaticInfo.OP_SREAD:
                sread();
                break;
            case VariableStaticInfo.OP_PRINT_CHAR:
                print_char();
                break;
            case VariableStaticInfo.OP_PRINT_NUM:
                print_num();
                break;
            case VariableStaticInfo.OP_RANDOM:
                random();
                break;
            case VariableStaticInfo.OP_PUSH:
                push();
                break;
            case VariableStaticInfo.OP_PULL:
                pull();
                break;
            case VariableStaticInfo.OP_SPLIT_WINDOW:
                split_window();
                break;
            case VariableStaticInfo.OP_SET_TEXT_STYLE:
                set_text_style();
                break;
            case VariableStaticInfo.OP_BUFFER_MODE:
                buffer_mode();
                break;
            case VariableStaticInfo.OP_SET_WINDOW:
                set_window();
                break;
            case VariableStaticInfo.OP_OUTPUTSTREAM:
                output_stream();
                break;
            case VariableStaticInfo.OP_INPUTSTREAM:
                input_stream();
                break;
            case VariableStaticInfo.OP_SOUND_EFFECT:
                sound_effect();
                break;
            case VariableStaticInfo.OP_ERASE_WINDOW:
                erase_window();
                break;
            case VariableStaticInfo.OP_ERASE_LINE:
                erase_line();
                break;
            case VariableStaticInfo.OP_SET_CURSOR:
                set_cursor();
                break;
            case VariableStaticInfo.OP_GET_CURSOR:
                get_cursor();
                break;
            case VariableStaticInfo.OP_READ_CHAR:
                read_char();
                break;
            case VariableStaticInfo.OP_SCAN_TABLE:
                scan_table();
                break;
            case VariableStaticInfo.OP_NOT:
                not();
                break;
            case VariableStaticInfo.OP_CALL_VN:
            case VariableStaticInfo.OP_CALL_VN2:
                call();
                break;
            case VariableStaticInfo.OP_TOKENISE:
                tokenise();
                break;
            case VariableStaticInfo.OP_ENCODE_TEXT:
                encode_text();
                break;
            case VariableStaticInfo.OP_COPY_TABLE:
                copy_table();
                break;
            case VariableStaticInfo.OP_PRINT_TABLE:
                print_table();
                break;
            case VariableStaticInfo.OP_CHECK_ARG_COUNT:
                check_arg_count();
                break;
            default:
                throwInvalidOpcode();
        }
        return new InstructionResult(TRUE, false);
    }

    private void call() {
        call(getNumOperands() - 1);
    }

    private void storew() {
        MemoryAccess memaccess = getMemoryAccess();
        int array = getUnsignedValue(0);
        int wordIndex = getUnsignedValue(1);
        short value = getValue(2);
        memaccess.writeShort(array + wordIndex * 2, value);
        nextInstruction();
    }

    private void storeb() {
        MemoryAccess memaccess = getMemoryAccess();
        int array = getUnsignedValue(0);
        int byteIndex = getUnsignedValue(1);
        byte value = (byte) getValue(2);
        memaccess.writeByte(array + byteIndex, value);
        nextInstruction();
    }

    private void put_prop() {
        int obj = getUnsignedValue(0);
        int property = getUnsignedValue(1);
        short value = getValue(2);
        if (obj > 0) {
            ZObject object = getObjectTree().getObject(obj);
            if (object.isPropertyAvailable(property)) {
                if (object.getPropertySize(property) == 1) {
                    object.setPropertyByte(property, 0, (byte) (value & 0xff));
                } else {
                    object.setPropertyByte(property, 0, (byte) ((value >> 8) & 0xff));
                    object.setPropertyByte(property, 1, (byte) (value & 0xff));
                }
                nextInstruction();
            } else {
                getMachine().halt("put_prop: the property [" + property + "] of object [" + obj + "] does not exist");
            }
        } else {
            getMachine().warn("@put_prop illegal access to object " + obj);
            nextInstruction();
        }
    }

    private void print_char() {
        short zchar = getValue(0);
        getMachine().printZsciiChar(zchar, false);
        nextInstruction();
    }

    private void print_num() {
        short number = getValue(0);
        getMachine().printNumber(number);
        nextInstruction();
    }

    private void push() {
        short value = getValue(0);
        getMachine().setVariable(0, value);
        nextInstruction();
    }

    private void pull() {
        int varnum = getUnsignedValue(0);
        short value = getMachine().getVariable(0);
        if (varnum == 0) {
            getMachine().setStackTopElement(value);
        } else {
            getMachine().setVariable(varnum, value);
        }
        nextInstruction();
    }

    private void output_stream() {
        short streamnumber = getValue(0);
        if (streamnumber < 0 && streamnumber >= -3) {
            getMachine().selectOutputStream(-streamnumber, false);
        } else if (streamnumber > 0 && streamnumber <= 3) {
            if (streamnumber == Machine.OUTPUTSTREAM_MEMORY) {
                int tableAddress = this.getUnsignedValue(1);
                getMachine().selectOutputStream3(tableAddress);
            } else {
                getMachine().selectOutputStream(streamnumber, true);
            }
        }
        nextInstruction();
    }

    private void input_stream() {
        getMachine().selectInputStream(getUnsignedValue(0));
        nextInstruction();
    }

    private void random() {
        short range = getValue(0);
        storeResult(getMachine().random(range));
        nextInstruction();
    }

    private void sread() {
        int version = getStoryFileVersion();
        if (version <= 3) {
            getMachine().updateStatusLine();
        }
        int textbuffer = getUnsignedValue(0);
        int parsebuffer = 0;
        int time = 0;
        short packedAddress = 0;
        if (getNumOperands() >= 2) parsebuffer = getUnsignedValue(1);
        if (getNumOperands() >= 3) time = getUnsignedValue(2);
        if (getNumOperands() >= 4) packedAddress = getValue(3);
        short terminal = getMachine().getServices().getInputFunctions().readLine(textbuffer, time, packedAddress);
        if (version < 5 || (version >= 5 && parsebuffer > 0)) {
            getMachine().getServices().getTokenizer().tokenize(textbuffer, parsebuffer, 0, false);
        }
        if (storesResult()) {
            storeResult(terminal);
        }
        nextInstruction();
    }

    /**
   * Implements the sound_effect instruction.
   */
    private void sound_effect() {
        int soundnum = 1;
        int effect = 0;
        int volume = 0;
        int routine = 0;
        if (getNumOperands() >= 1) {
            soundnum = getUnsignedValue(0);
        }
        if (getNumOperands() >= 2) {
            effect = getUnsignedValue(1);
        }
        if (getNumOperands() >= 3) {
            volume = getUnsignedValue(2);
        }
        if (getNumOperands() == 4) {
            routine = getUnsignedValue(3);
        }
        SoundSystem soundSystem = getMachine().getServices().getSoundSystem();
        soundSystem.play(soundnum, effect, volume);
        nextInstruction();
    }

    private void split_window() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.splitWindow(getUnsignedValue(0));
        }
        nextInstruction();
    }

    private void set_window() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.setWindow(getUnsignedValue(0));
        }
        nextInstruction();
    }

    private void set_text_style() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.setTextStyle(getUnsignedValue(0));
        }
        nextInstruction();
    }

    private void buffer_mode() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.setBufferMode(getUnsignedValue(0) > 0);
        }
        nextInstruction();
    }

    private void erase_window() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.eraseWindow(getValue(0));
        }
        nextInstruction();
    }

    private void erase_line() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.eraseLine(getValue(0));
        }
        nextInstruction();
    }

    private void set_cursor() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            screenModel.setTextCursor(getUnsignedValue(0), getUnsignedValue(1));
        }
        nextInstruction();
    }

    private void get_cursor() {
        ScreenModel screenModel = getMachine().getScreen();
        if (screenModel != null) {
            TextCursor cursor = screenModel.getTextCursor();
            MemoryAccess memaccess = getMemoryAccess();
            int arrayAddr = getUnsignedValue(0);
            memaccess.writeShort(arrayAddr, (short) cursor.getLine());
            memaccess.writeShort(arrayAddr + 2, (short) cursor.getColumn());
        }
        nextInstruction();
    }

    private void scan_table() {
        MemoryAccess memaccess = getMemoryAccess();
        short x = getValue(0);
        int table = getUnsignedValue(1);
        int length = getUnsignedValue(2);
        int form = 0x82;
        if (getNumOperands() == 4) {
            form = getUnsignedValue(3);
        }
        int fieldlen = form & 0x7f;
        boolean isWordTable = (form & 0x80) > 0;
        int pointer = table;
        boolean found = false;
        for (int i = 0; i < length; i++) {
            short current = isWordTable ? memaccess.readShort(pointer) : memaccess.readByte(pointer);
            if (current == x) {
                storeResult((short) pointer);
                found = true;
                break;
            }
            pointer += fieldlen;
        }
        if (!found) {
            storeResult((short) 0);
        }
        branchOnTest(found);
    }

    private void read_char() {
        int time = 0;
        int routineAddress = 0;
        if (getNumOperands() >= 2) {
            time = getUnsignedValue(1);
        }
        if (getNumOperands() >= 3) {
            routineAddress = getValue(2);
        }
        storeResult(getMachine().getServices().getInputFunctions().readChar(time, routineAddress));
        nextInstruction();
    }

    /**
   * not instruction. Actually a copy from Short1Instruction, probably we
   * can remove this duplication.
   */
    private void not() {
        int notvalue = ~getUnsignedValue(0);
        storeResult((short) (notvalue & 0xffff));
        nextInstruction();
    }

    private void tokenise() {
        int textbuffer = getUnsignedValue(0);
        int parsebuffer = getUnsignedValue(1);
        int dictionary = 0;
        int flag = 0;
        if (getNumOperands() >= 3) dictionary = getUnsignedValue(2);
        if (getNumOperands() >= 4) flag = getUnsignedValue(3);
        getMachine().getServices().getTokenizer().tokenize(textbuffer, parsebuffer, dictionary, (flag != 0));
        nextInstruction();
    }

    private void check_arg_count() {
        int argumentNumber = getUnsignedValue(0);
        int currentNumArgs = getMachine().getCurrentRoutineContext().getNumArguments();
        branchOnTest(argumentNumber <= currentNumArgs);
    }

    private void copy_table() {
        int first = getUnsignedValue(0);
        int second = getUnsignedValue(1);
        int size = getValue(2);
        MemoryAccess memaccess = getMemoryAccess();
        if (second == 0) {
            size = Math.abs(size);
            for (int i = 0; i < size; i++) {
                memaccess.writeByte(first + i, (byte) 0);
            }
        } else {
            if (size < 0 || first > second) {
                size = Math.abs(size);
                for (int i = 0; i < size; i++) {
                    memaccess.writeByte(second + i, memaccess.readByte(first + i));
                }
            } else {
                size = Math.abs(size);
                for (int i = size - 1; i >= 0; i--) {
                    memaccess.writeByte(second + i, memaccess.readByte(first + i));
                }
            }
        }
        nextInstruction();
    }

    /**
   * Do the print_table instruction. This method takes a text and formats
   * it in a specified format. It requires access to the cursor position
   * in order to be implemented correctly, otherwise horizontal home
   * position would always be set to the left position of the window.
   * Interestingly, the text is not encoded, so the characters should be
   * accessed one by one in ZSCII format.
   */
    private void print_table() {
        int zsciiText = getUnsignedValue(0);
        int width = getUnsignedValue(1);
        int height = 1;
        int skip = 0;
        if (getNumOperands() >= 3) height = getUnsignedValue(2);
        if (getNumOperands() == 4) skip = getUnsignedValue(3);
        short zchar = 0;
        MemoryAccess memaccess = getMemoryAccess();
        TextCursor cursor = getMachine().getScreen().getTextCursor();
        int column = cursor.getColumn();
        int row = cursor.getLine();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int offset = (width * i) + j;
                zchar = memaccess.readUnsignedByte(zsciiText + offset);
                getMachine().printZsciiChar(zchar, false);
            }
            row += skip + 1;
            getMachine().getScreen().setTextCursor(row, column);
        }
        nextInstruction();
    }

    private void encode_text() {
        int zsciiText = getUnsignedValue(0);
        int length = getUnsignedValue(1);
        int from = getUnsignedValue(2);
        int codedText = getUnsignedValue(3);
        ZCharEncoder encoder = getMachine().getServices().getZCharEncoder();
        encoder.encode(getMemoryAccess(), zsciiText + from, length, codedText);
        nextInstruction();
    }
}
