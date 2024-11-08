package arche;

import java.io.*;
import physis.core.PHYSIS;
import physis.core.event.ProliferationEvent;
import physis.core.event.InteractionEvent;
import physis.core.virtualmachine.CodeTape;
import physis.core.virtualmachine.GeneticCodeTape;
import physis.core.virtualmachine.PhysisVirtualMachine;
import physis.core.virtualmachine.InstructionSet;
import physis.core.genotype.Genome;
import physis.log.Log;

/**
 * This universal processor is designed to increase the evolvability in Tierra-like systems.
 * The main goal is to enable the evolution of the decoding system, here the processor structure
 * and the instruction set. <BR>
 * The universal processor is something like the Universal Turing Machine. On the codetape the description
 * of the processor is stored: <BR>
 * description_of_processor_P#self_replicating_code_for_P
 * <BR><BR>
 * WARNING FOR DEVELOPERS: The inner structure of this class is rather fragile! e.g. If you
 * change one of the instructions, then you should the number_of_ops table! This class is meant to be ETERNAL! ;)
 */
public class UP extends PhysisVirtualMachine {

    /** The maximum number of usable registers for building the processor's internal structure. (A general purpose internal memory-array in a hardware implementation.) 16 seems to be reasonable but there's no special reason for that.*/
    public static final int STORAGE_ARRAY_SIZE = 16;

    private static InstructionSet is = InstructionSet.getInstance();

    /** The size of the universal processor's instructionset. For building the specific instructionset. */
    private static short up_is_size = (short) is.getSize();

    /** Constant for parsing used for creating a finite state automaton. */
    private static final int BEGIN = 0;

    private static final int INSTRUCTION = 1;

    /**
     * The structural elements. (SEs) This is basically a memory array. Its cells
     * can be used by different data structures called structural elements (registers, queues, stacks).
     */
    private Storage[] ses;

    /** The number of structural elements. */
    private int ssize;

    /** The instruction lookup-table. */
    private short[][] instruction_table;

    /** The position of the SEPARATOR label. This label separates the descriptive and the executable paart of the genome. (at least in human-written  organisms) */
    private int separator_position;

    /** The actual direction (the communication partner). */
    private int direction;

    /** FOR OPTIMIZATION: the divide operation is like a transaction so the VMs can share the ProliferationEvent instance. */
    private static ProliferationEvent prolevnt = new ProliferationEvent(null, null);

    /** FOR OPTIMIZATION: the I/O operation are like transactions so the VMs can share the InteractionEvent instance. */
    private static InteractionEvent interactevnt = new InteractionEvent(null);

    /**
     * When vivifying an organism its virtualmachine is just reset (not created).
     */
    public void reset() {
        if (!tape.contains(SEPARATOR)) {
            bearer.kill();
            return;
        }
        buildProcessor();
        if (instruction_table.length == 0) {
            bearer.kill();
        }
        restart();
        gestation_time = GESTATION_TIME_INVALID;
    }

    /**
     * After successfull replication the ancestor is restarted. It is done by setting
     * the instructionpointer right after the SEPARATOR.
     */
    public void restart() {
        setIP((separator_position + 1) % tape.getSize());
        counter = 0;
    }

    /** Returns the current state of the processor as a formatted String. */
    public String getState() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ses.length; i++) {
            sb.append(i + ". " + ses[i] + "\n");
        }
        sb.append("Next instruction: ");
        int nextinst = Math.abs(tape.read(IP.value) % instruction_table.length);
        sb.append(instruction2String(nextinst));
        sb.append("\n");
        return sb.toString();
    }

    /** Returns the structural information and the instructionset of the processor as a formatted string. */
    public String getProcessorInformation() {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("ARCHITECTURE:\n");
        for (int i = 0; i < ses.length; i++) {
            sb.append(ses[i].getStructure() + " ");
        }
        sb.append("@\n");
        sb.append("INSTRUCTION_SET_LENGTH: " + instruction_table.length + " #\n");
        sb.append("\nINSTRUCTION SET:\n");
        for (int i = 0; i < instruction_table.length; i++) {
            sb.append(i + ".instruction:");
            sb.append(instruction2String(i) + "\n");
        }
        return sb.toString();
    }

    /** The main loop. */
    public void execute() {
        try {
            short[] insts = instruction_table[Math.abs(tape.fetchInst(IP.value) % instruction_table.length)];
            int i = 0;
            while (i < insts.length) {
                switch(insts[i]) {
                    case IN:
                        {
                            i = fillOperands(1, insts, i + 1);
                            in(operands[0]);
                        }
                        break;
                    case OUT:
                        {
                            i = fillOperands(1, insts, i + 1);
                            out(operands[0]);
                        }
                        break;
                    case LOAD:
                        {
                            i = fillOperands(2, insts, i + 1);
                            load(operands[0], operands[1]);
                        }
                        break;
                    case STORE:
                        {
                            i = fillOperands(2, insts, i + 1);
                            store(operands[0], operands[1]);
                        }
                        break;
                    case MOVE:
                        {
                            i = fillOperands(2, insts, i + 1);
                            move(operands[0], operands[1]);
                        }
                        break;
                    case ALLOCATE:
                        {
                            i = fillOperands(1, insts, i + 1);
                            allocate(operands[0]);
                        }
                        break;
                    case COMPARE:
                        {
                            i = fillOperands(3, insts, i + 1);
                            compare(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case IFZERO:
                        {
                            i = fillOperands(1, insts, i + 1);
                            ifzero(operands[0]);
                        }
                        break;
                    case JUMP:
                        {
                            i = fillOperands(1, insts, i + 1);
                            jump(operands[0]);
                        }
                        break;
                    case DEC:
                        {
                            i = fillOperands(1, insts, i + 1);
                            dec(operands[0]);
                        }
                        break;
                    case INC:
                        {
                            i = fillOperands(1, insts, i + 1);
                            inc(operands[0]);
                        }
                        break;
                    case DIVIDE:
                        {
                            i++;
                            divide();
                            return;
                        }
                    case SDIR:
                        {
                            i = fillOperands(1, insts, i + 1);
                            sdir(operands[0]);
                        }
                        break;
                    case GDIR:
                        {
                            i = fillOperands(1, insts, i + 1);
                            gdir(operands[0]);
                        }
                        break;
                    case SEND:
                        {
                            i = fillOperands(1, insts, i + 1);
                            send(operands[0]);
                        }
                        break;
                    case RECIEVE:
                        {
                            i = fillOperands(1, insts, i + 1);
                            receive(operands[0]);
                        }
                        break;
                    case ADD:
                        {
                            i = fillOperands(3, insts, i + 1);
                            add(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case SUB:
                        {
                            i = fillOperands(3, insts, i + 1);
                            sub(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case MUL:
                        {
                            i = fillOperands(3, insts, i + 1);
                            mul(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case DIV:
                        {
                            i = fillOperands(3, insts, i + 1);
                            div(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case MOD:
                        {
                            i = fillOperands(3, insts, i + 1);
                            mod(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case AND:
                        {
                            i = fillOperands(3, insts, i + 1);
                            and(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case OR:
                        {
                            i = fillOperands(3, insts, i + 1);
                            or(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case XOR:
                        {
                            i = fillOperands(3, insts, i + 1);
                            xor(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case NEG:
                        {
                            i = fillOperands(2, insts, i + 1);
                            neg(operands[0], operands[1]);
                        }
                        break;
                    case NOT:
                        {
                            i = fillOperands(2, insts, i + 1);
                            not(operands[0], operands[1]);
                        }
                        break;
                    case SHIFT_L:
                        {
                            i = fillOperands(2, insts, i + 1);
                            shift_l(operands[0], operands[1]);
                        }
                        break;
                    case SHIFT_R:
                        {
                            i = fillOperands(2, insts, i + 1);
                            shift_r(operands[0], operands[1]);
                        }
                        break;
                    case FORK_TH:
                        {
                            i = fillOperands(1, insts, i + 1);
                            fork_th(operands[0]);
                        }
                        break;
                    case KILL_TH:
                        {
                            i++;
                            kill_th();
                        }
                        break;
                    case CLEAR:
                        {
                            i = fillOperands(1, insts, i + 1);
                            clear(operands[0]);
                        }
                        break;
                    case CINC:
                        {
                            i = fillOperands(1, insts, i + 1);
                            cinc(operands[0]);
                        }
                        break;
                    case CDEC:
                        {
                            i = fillOperands(1, insts, i + 1);
                            cdec(operands[0]);
                        }
                        break;
                    case IS_SEP:
                        {
                            i = fillOperands(2, insts, i + 1);
                            is_sep(operands[0], operands[1]);
                        }
                        break;
                    case REL_LOAD:
                        {
                            i = fillOperands(3, insts, i + 1);
                            rel_load(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case REL_STORE:
                        {
                            i = fillOperands(3, insts, i + 1);
                            rel_store(operands[0], operands[1], operands[2]);
                        }
                        break;
                    case IFNOTZERO:
                        {
                            i = fillOperands(1, insts, i + 1);
                            ifnotzero(operands[0]);
                        }
                        break;
                    default:
                        {
                            i++;
                        }
                }
                counter++;
            }
            incrementIP(1);
        } catch (Exception e) {
            try {
                System.out.println("UC:" + PHYSIS.getInstance().getUpdateCount());
                e.printStackTrace();
                System.out.println("ip:" + IP);
                FileWriter fw = new FileWriter("bugos");
                fw.write(bearer.toString());
                fw.close();
                System.exit(-1);
            } catch (Exception ex) {
                System.out.println("EXCEPTON IN EXCEPTION!!!");
                ex.printStackTrace();
                System.out.println("Tape:" + tape + "\n\nbearer:" + bearer);
            }
            System.exit(-1);
        }
    }

    /** puffer for operands - 3 is the maximum number of operands */
    private short[] operands = new short[3];

    /** Fetching the operands: if there aren't enough operands in the instruction itself then we have to fetch operands from the tape. Returns the position in the instruction after reading the operands.*/
    private int fillOperands(int numops, short[] insts, int from) {
        int newpos = 0;
        if (numops <= (insts.length - from)) {
            for (int i = 0; i < numops; i++) {
                operands[i] = insts[from + i];
            }
            newpos = from + numops;
        } else {
            int i = 0;
            for (; (from + i) < insts.length; i++) {
                operands[i] = insts[from + i];
            }
            for (; i < numops; i++) {
                operands[i] = tape.read(IP.value);
                incrementIP(1);
            }
            newpos = insts.length;
        }
        for (int i = 0; i < numops; i++) operands[i] = (short) (((operands[i] < 0) ? -operands[i] : operands[i]) % Short.MAX_VALUE);
        return newpos;
    }

    public void execute(int numberofinstructions) {
        for (int i = 0; i < numberofinstructions; i++) {
            execute();
        }
    }

    public Genome getGenome() {
        return tape.getGenome();
    }

    public int getGenomeSize() {
        return tape.getSize();
    }

    /** The main for method for building the processor. */
    void buildProcessor() {
        StorageArray sta = new StorageArray(STORAGE_ARRAY_SIZE);
        buildStructure(sta);
        ses = sta.getStructuralElements();
        IP = (InstructionPointer) ses[0];
        sta = null;
        ssize = ses.length;
        buildInstructionSet();
    }

    /** Builds the structural elements. Scans the symbols before the separator. The structural
     *	symbols are not necessary connected.
     *  SIDE EFFECT! - sets the separator position variable 
     */
    void buildStructure(StorageArray sta) {
        short i = 0;
        short c = tape.read(i);
        short size = 0;
        while (c != SEPARATOR) {
            switch(c) {
                case S:
                    {
                        size = 1;
                        i++;
                        while (true) {
                            while (!isCodeForStructuralElement(c = tape.read(i))) {
                                i++;
                            }
                            if (c == S) {
                                size++;
                                i++;
                                c = tape.read(i);
                            } else {
                                sta.addStorage(StorageFactory.createStack(size));
                                break;
                            }
                        }
                        break;
                    }
                case Q:
                    {
                        size = 1;
                        i++;
                        while (true) {
                            while (!isCodeForStructuralElement(c = tape.read(i))) {
                                i++;
                            }
                            if (c == Q) {
                                size++;
                                i++;
                                c = tape.read(i);
                            } else {
                                sta.addStorage(StorageFactory.createQueue(size));
                                break;
                            }
                        }
                        break;
                    }
                case R:
                    {
                        sta.addStorage(StorageFactory.createRegister());
                        i++;
                        c = tape.read(i);
                        break;
                    }
                default:
                    {
                        i++;
                        c = tape.read(i);
                        break;
                    }
            }
        }
        separator_position = i;
    }

    /**
     * Builds the instrucionset from the tape. Scans the symbols before the separator.
     */
    void buildInstructionSet() {
        if (!tape.contains(I)) {
            bearer.kill();
        }
        int i = 0;
        short c = 0;
        int number_of_insts = 0;
        while ((c = tape.read(i)) != SEPARATOR) {
            if (c == I) {
                number_of_insts++;
            }
            i++;
        }
        instruction_table = new short[number_of_insts][];
        i = 0;
        int state = BEGIN;
        int start = 0;
        int stop = 0;
        int n = 0;
        while (n < number_of_insts) {
            c = tape.read(i);
            switch(state) {
                case BEGIN:
                    {
                        if (c == I) {
                            start = i;
                            state = INSTRUCTION;
                        }
                    }
                    ;
                    break;
                case INSTRUCTION:
                    {
                        if ((c == I) || (c == SEPARATOR)) {
                            stop = i;
                            createInstruction(n, start + 1, stop);
                            n++;
                            state = BEGIN;
                            i--;
                        }
                    }
                    ;
                    break;
            }
            if (c == SEPARATOR) {
                break;
            }
            i++;
        }
    }

    /** Creates the NUMBERth instruction which is on the tape from start to stop. */
    void createInstruction(int number, int start, int stop) {
        int size = stop - start;
        instruction_table[number] = new short[size];
        for (int i = 0; i < size; i++) {
            instruction_table[number][i] = tape.read(start + i);
        }
        int i = 0;
        while (i < size) {
            instruction_table[number][i] = (short) (Math.abs(instruction_table[number][i]) % up_is_size);
            i += is.getInstructionByCode(instruction_table[number][i]).getNumberOfOperands() + 1;
        }
    }

    private InstructionPointer IP;

    /**
     *  The instruction pointer-register is incremented. The value can be negative as well.
     *  Circularity isn't ensured by this method. Digital organisms should take care of proper IP-handling.
     *  @param value the amount of incrementation
     */
    void incrementIP(int value) {
        IP.value += value;
    }

    /** Directly sets the instruction pointer. */
    void setIP(int value) {
        IP.value = (short) value;
    }

    public static final short NOP = 0;

    public static final short IN = 1;

    /**
     * Reads one piece of data (an int) from the outside world (environment).
     * @param destination Index for the structural element where to be loaded.
     * It's counted modulo number_of_elements.
     */
    public void in(int destination) {
        if (!bearer.getMetabolism().isInputFull()) {
            ses[destination % ssize].write((short) (PHYSIS.environment.getInputData()));
            bearer.getMetabolism().putInputValue(ses[destination % ssize].read());
            interactevnt.reFill(bearer);
            PHYSIS.environment.interactionOccured(interactevnt);
        }
    }

    public static final short OUT = 2;

    /**
     * Writes one piece of data (an int) to the outside world (environment).
     * @param source Index for the structural element of the source.
     * It's counted modulo number_of_elements.
     */
    public void out(int source) {
        if (!bearer.getMetabolism().isOutputFull()) {
            bearer.getMetabolism().putOutputValue(ses[source % ssize].read());
            interactevnt.reFill(bearer);
            PHYSIS.environment.interactionOccured(interactevnt);
        } else {
            bearer.getMetabolism().clear();
        }
    }

    public static final short LOAD = 3;

    /**
     * Loads data from memory pointed by the source structural element.
     * Only this and the store instruction can access the memory. (RISC style)
     * @param source The index of the SE which contains the memory address.
     * @param destination The index of the SE which is the destination.
     */
    public void load(int source, int destination) {
        ses[destination % ssize].write(tape.read(ses[source % ssize].read()));
    }

    public static final short STORE = 4;

    /**
     * Writes data to memory from the source structural element.
     * Only this and the store instruction can access the memory. (RISC style)
     * @param source The index of the SE which contains the memory address.
     * @param destination The index of the SE which is the destination.
     */
    public void store(int destination, int data) {
        tape.write(ses[destination % ssize].read(), ses[data % ssize].read());
    }

    public static final short MOVE = 5;

    /**
     * Moves data between the SEs.
     */
    public void move(int source, int destination) {
        ses[destination % ssize].write(ses[source % ssize].read());
    }

    public static final short ALLOCATE = 6;

    /**
     * Allocates memory with the specified size. (The location is the end of the
     * the organism by default in the Alife system.)
     * @param size_addr is the structural elements' index.
     */
    public void allocate(int size_addr) {
        int ncells = ses[size_addr % ssize].read();
        if (tape.isAllocationPossible(ncells)) {
            tape.allocate(ncells);
        }
    }

    public static final short COMPARE = 7;

    /**
     * Compares the content of the op1 and op2 and the result is stored
     * in the result SE element.
     * RESULT: negative if op1 < op2, zero if op1 = op2, positive if op1 > op2.
     */
    public void compare(int op1, int op2, int result) {
        short res = 0;
        short o1 = ses[op1 % ssize].read();
        short o2 = ses[op2 % ssize].read();
        if (o1 < o2) {
            res = -1;
        } else if (o1 == o2) {
            res = 0;
        } else res = 1;
        ses[result % ssize].write(res);
    }

    public static final short IFZERO = 8;

    /** Executes the next instruction if the content of the structural element
     *  pointed by strel is zero. This is done by changing the IP. If not zero it just skips the next instruction.
     */
    public void ifzero(int strel) {
        if (ses[strel % ssize].read() == 0) {
        } else {
            incrementIP(1);
        }
    }

    public static final short JUMP = 9;

    /**
     * Jumps to the address (mod codetape size). Address is in a SE.
     */
    public void jump(int address) {
        setIP(ses[address % ssize].read());
    }

    public static final short DEC = 10;

    /**
     * Decrements the content of a structural element.
     */
    public void dec(int what) {
        ses[what % ssize].write((short) (ses[what % ssize].read() - 1));
    }

    public static final short INC = 11;

    /**
     * Increments the content of a structural element.
     */
    public void inc(int what) {
        ses[what % ssize].write((short) (ses[what % ssize].read() + 1));
    }

    public static final short DIVIDE = 12;

    /**
     * The only one biological operation for cell division.
     */
    public void divide() {
        if (!tape.isProliferationPossible()) {
            incrementIP(1);
            return;
        }
        gestation_time = counter;
        prolevnt.reFill(tape.divide(), bearer);
        PHYSIS.environment.proliferationPerformed(prolevnt);
    }

    public static final short SDIR = 13;

    /**
     * Th processor can communicate with other processors in its neighbourhood but only with
     * one in a moment. (Later this can be paralleled).
     * @param whereFrom The SE where the new direction is stored.
     */
    public void sdir(int whereFrom) {
    }

    public static final short GDIR = 14;

    public void gdir(int whereTo) {
    }

    public static final short SEND = 15;

    /**
     * Sends a piece of data to an other processor pointed by the direction.
     */
    public void send(int src) {
    }

    public static final short RECIEVE = 16;

    /**
     * Receives a piece of data from an other processor.
     */
    public void receive(int dst) {
    }

    public static final short ADD = 17;

    public void add(int op1, int op2, int result) {
        ses[result % ssize].write((short) (ses[op1 % ssize].read() + ses[op2 % ssize].read()));
    }

    public static final short SUB = 18;

    public void sub(int op1, int op2, int result) {
        ses[result % ssize].write((short) (ses[op1 % ssize].read() - ses[op2 % ssize].read()));
    }

    public static final short MUL = 19;

    public void mul(int op1, int op2, int result) {
        ses[result % ssize].write((short) (ses[op1 % ssize].read() * ses[op2 % ssize].read()));
    }

    public static final short DIV = 20;

    public void div(int op1, int op2, int result) {
        short divider = ses[op2 % ssize].read();
        if (divider == 0) return;
        ses[result % ssize].write((short) (ses[op1 % ssize].read() / divider));
    }

    public static final short MOD = 21;

    public void mod(int op1, int op2, int result) {
        short divider = ses[op2 % ssize].read();
        if (divider == 0) return;
        ses[result % ssize].write((short) (ses[op1 % ssize].read() % divider));
    }

    public static final short AND = 22;

    public void and(int op1, int op2, int result) {
        ses[result % ssize].write((short) (ses[op1 % ssize].read() & ses[op2 % ssize].read()));
    }

    public static final short OR = 23;

    public void or(int op1, int op2, int result) {
        ses[result % ssize].write((short) (ses[op1 % ssize].read() | ses[op2 % ssize].read()));
    }

    public static final short XOR = 24;

    public void xor(int op1, int op2, int result) {
        ses[result % ssize].write((short) (ses[op1 % ssize].read() ^ ses[op2 % ssize].read()));
    }

    public static final short NEG = 25;

    public void neg(int op1, int result) {
        ses[result % ssize].write((short) (-(ses[op1 % ssize].read())));
    }

    public static final short NOT = 26;

    public void not(int op1, int result) {
        ses[result % ssize].write((short) (~(ses[op1 % ssize].read())));
    }

    public static final short SHIFT_L = 27;

    public void shift_l(int op1, int result) {
        ses[result % ssize].write((short) ((ses[op1 % ssize].read()) << 1));
    }

    public static final short SHIFT_R = 28;

    public void shift_r(int op1, int result) {
        ses[result % ssize].write((short) ((ses[op1 % ssize].read()) >> 1));
    }

    public static final short FORK_TH = 29;

    public void fork_th(int pos) {
    }

    public static final short KILL_TH = 30;

    public void kill_th() {
    }

    /** Register. */
    public static final short R = 31;

    /** Stack */
    public static final short S = 32;

    /** Queue */
    public static final short Q = 33;

    /** Instruction */
    public static final short I = 34;

    /** Blank (for separating different stacks for example). */
    public static final short B = 35;

    /** Separator */
    public static final short SEPARATOR = 36;

    public static final short CLEAR = 37;

    public void clear(int n) {
        ses[n % ssize].clear();
    }

    public static final short CINC = 38;

    public void cinc(int n) {
        ses[n % ssize].write((short) ((ses[n % ssize].read() + 1) % tape.getSize()));
    }

    public static final short CDEC = 39;

    public void cdec(int n) {
        short i = ses[n % ssize].read();
        i--;
        if (i < 0) {
            i = (short) (tape.getSize() - 1);
        }
        ses[n % ssize].write(i);
    }

    public static final short IS_SEP = 40;

    public void is_sep(int src, int dst) {
        if (ses[src % ssize].read() == SEPARATOR) {
            ses[dst % ssize].write((short) 1);
        } else {
            ses[dst % ssize].write((short) 0);
        }
    }

    public static final short REL_LOAD = 41;

    /**
	 */
    public void rel_load(int base, int offset, int destination) {
        ses[destination % ssize].write(tape.read(ses[base % ssize].read() + ses[offset % ssize].read()));
    }

    public static final short REL_STORE = 42;

    /**
	 */
    public void rel_store(int base, int offset, int from) {
        tape.write(ses[base % ssize].read() + ses[offset % ssize].read(), ses[from % ssize].read());
    }

    public static final short IFNOTZERO = 43;

    /** Executes the next instruction if the content of thestructural element
	 *  pointed by strel is zero.
	 */
    public void ifnotzero(int strel) {
        if (ses[strel % ssize].read() == 0) {
            incrementIP(1);
        } else {
        }
    }

    private boolean isCodeForStructuralElement(short c) {
        return c == R || c == S || c == Q || c == B || c == SEPARATOR;
    }

    private String instruction2String(int icode) {
        StringBuffer sb = new StringBuffer(32);
        sb.append("[");
        int nextinst = 0;
        int j = 0;
        for (j = 0; j < instruction_table[icode].length; j++) {
            if (nextinst == j) {
                sb.append(is.getInstructionMnemonic(instruction_table[icode][j]) + ",");
                nextinst += is.getInstructionByCode(instruction_table[icode][j]).getNumberOfOperands() + 1;
            } else {
                sb.append("" + ((instruction_table[icode][j]) % ses.length) + ",");
            }
        }
        sb.append("\b]");
        if (j != nextinst) {
            sb.append(" +" + (nextinst - j) + " operands");
        }
        return sb.toString();
    }
}
