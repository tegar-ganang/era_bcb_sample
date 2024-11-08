package net.sf.openforge.verilog.testbench;

import java.util.*;
import java.io.*;
import net.sf.openforge.app.*;
import net.sf.openforge.backend.hdl.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.util.IndentWriter;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.pattern.BusWire;
import net.sf.openforge.verilog.pattern.GenericModule;
import net.sf.openforge.verilog.pattern.PortWire;

/**
 * This class generates a self-verifying testbench for the given
 * {@link Design} where input and output vectors are supplied in files
 * named <i>actorName</i>_<i>portName</i>.vec.  The testbench uses PLI
 * calls that are available through the PLI code in
 * konaAtest/bin/autoTestPli.c 
 * 
 * @author imiller
 * @version $Id: GenericTestbenchWriter.java 284 2006-08-15 15:43:34Z imiller $
 */
public class GenericTestbenchWriter {

    private String actorName;

    private Design design;

    public GenericTestbenchWriter(Design design) {
        final GenericJob gj = EngineThread.getGenericJob();
        this.actorName = gj.getOutputBaseName();
        this.design = design;
    }

    private String getActorName() {
        return this.actorName;
    }

    private Design getDesign() {
        return this.design;
    }

    private String getInstanceName() {
        return "dut";
    }

    public void genTestbench() {
        final GenericJob gj = EngineThread.getGenericJob();
        final ForgeFileHandler fileHandler = EngineThread.getGenericJob().getFileHandler();
        int hangTimerExpire = 1500;
        try {
            hangTimerExpire = Integer.parseInt(gj.getOption(OptionRegistry.HANG_TIMER).getValue(CodeLabel.UNSCOPED).toString(), 10);
        } catch (Exception e) {
            hangTimerExpire = 1500;
        }
        final String designVerilogIdentifier = ID.toVerilogIdentifier(ID.showLogical(getDesign()));
        final IndentWriter pw;
        try {
            pw = new IndentWriter(new PrintWriter(new FileWriter(fileHandler.getFile(TestBenchEngine.ATB))));
        } catch (IOException ioe) {
            gj.warn(" Could not create atb file " + ioe);
            return;
        }
        List<TBIOHandler> handlers = new ArrayList();
        for (Iterator iter = getDesign().getFifoInterfaces().iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            if (o instanceof FifoInput) handlers.add(new InputHandler((FifoInput) o)); else if (o instanceof FifoOutput) handlers.add(new OutputHandler((FifoOutput) o)); else throw new IllegalArgumentException("Unknown type of fifo interface");
        }
        StateHandler stateh = new StateHandler(getDesign());
        pw.println("`timescale 1ns/1ps");
        pw.println("`define legacy_model // Some simulators cannot handle the syntax of the new memory models.  This define uses a simpler syntax for the memory models in the unisims library");
        final String simFile = fileHandler.getFile(VerilogTranslateEngine.SIMINCL).getAbsolutePath();
        pw.println("`include \"" + simFile + "\"");
        pw.println("`timescale 1ns/1ps");
        pw.println("");
        pw.println("module fixture();");
        pw.inc();
        pw.println("reg            clk;");
        pw.println("reg            LGSR;");
        pw.println("reg            startSimulation;");
        pw.println("reg            run;");
        pw.println("wire           fire;");
        pw.println("wire           fireDone;");
        pw.println("reg [31:0]     hangTimer;");
        pw.println("reg [31:0]     clockCount;");
        pw.println("reg [31:0]     actionFiringCount;");
        pw.println("integer        resultFile;");
        pw.println("integer        stateDumpFile;");
        for (TBIOHandler ioh : handlers) {
            ioh.writeDeclarations(pw);
        }
        pw.println("always #25 clk = ~clk;");
        pw.println("//initial begin  $dumpfile(\"waves.vcd\");  $dumpvars;end");
        pw.println("assign glbl.GSR=LGSR;");
        pw.println(designVerilogIdentifier);
        pw.inc();
        pw.print(getInstanceName() + " (");
        pw.inc();
        for (TBIOHandler ioh : handlers) {
            ioh.writeInstantiation(pw);
        }
        for (Iterator domainIter = design.getAllocatedClockDomains().iterator(); domainIter.hasNext(); ) {
            Design.ClockDomain domain = (Design.ClockDomain) domainIter.next();
            pw.print("." + domain.getClockPin().getName() + "(clk)");
            if (domain.getResetPin() != null) pw.print(", ." + domain.getResetPin().getName() + "(1'b0)");
            if (domainIter.hasNext()) pw.print(",");
        }
        pw.dec();
        pw.dec();
        pw.println(");");
        pw.println("initial begin");
        pw.inc();
        pw.println("clk <= 0;");
        pw.println("hangTimer <= 0;");
        pw.println("startSimulation <= 0;");
        pw.println("run <= 0;");
        final String simResults = fileHandler.getFile(TestBenchEngine.RESULTS).getAbsolutePath();
        final String simState = fileHandler.getFile(TestBenchEngine.GenericTBEngine.STATE).getAbsolutePath();
        pw.println("resultFile <= $fopen(\"" + simResults + "\");");
        pw.println("stateDumpFile <= $fopen(\"" + simState + "\");");
        pw.println("clockCount <= 0;");
        pw.println("actionFiringCount <= 0;");
        for (TBIOHandler ioh : handlers) {
            ioh.writeInitial(pw);
        }
        pw.println("LGSR <= 1;");
        pw.println("#1 LGSR <= 0;");
        pw.println("#500 startSimulation <= 1;");
        pw.dec();
        pw.println("end");
        pw.println("assign fire = " + getActionFiringString() + ";");
        pw.println("assign fireDone = " + getActionFiringDoneString() + ";");
        pw.println("always @(negedge clk) begin");
        pw.inc();
        pw.println("if (fireDone) begin");
        pw.inc();
        for (String capture : stateh.getCaptureStrings()) pw.println(capture);
        pw.println("$fwrite(stateDumpFile, \"MARK\\n\");");
        pw.dec();
        pw.println("end");
        pw.dec();
        pw.println("end");
        pw.println();
        pw.println("always @(posedge clk) begin");
        pw.inc();
        pw.println("run <= startSimulation; // ensure that we start handling on a rising edge");
        pw.println("if (run) begin");
        pw.inc();
        for (TBIOHandler ioh : handlers) {
            ioh.writeTest(pw);
        }
        pw.dec();
        pw.println("end // else");
        pw.dec();
        pw.println("end // always");
        pw.println();
        pw.println("always @(posedge clk) begin");
        pw.inc();
        pw.println("if (!$actionFiringsRemain()) begin");
        pw.inc();
        pw.println("$fwrite(resultFile, \"PASSED in %d action firings (%d cycles)\\n\", actionFiringCount, clockCount);");
        pw.println("$display(\"PASSED\");");
        pw.println("$finish;");
        pw.dec();
        pw.println("end");
        pw.dec();
        pw.println("end");
        pw.println();
        pw.println("// negedge so that we know the action update happens before the queue update");
        pw.println("always @(negedge clk) begin");
        pw.inc();
        pw.println("if (fire) begin");
        pw.inc();
        pw.println("$markActionFiring();");
        pw.println("actionFiringCount <= actionFiringCount + 1;");
        pw.println("if (actionFiringCount[4:0] === 0) begin $display(\"Fire %d at cycle %d\", actionFiringCount, clockCount); end");
        pw.dec();
        pw.println("end");
        pw.dec();
        pw.println("end");
        pw.println();
        pw.println("always @(posedge clk) begin");
        pw.inc();
        pw.println("clockCount <= clockCount + 1;");
        pw.print("if (");
        for (Iterator iter = handlers.iterator(); iter.hasNext(); ) {
            TBIOHandler ioh = (TBIOHandler) iter.next();
            pw.print(ioh.getActivityName());
            if (iter.hasNext()) pw.print(" || ");
        }
        pw.println(") hangTimer <= 0;");
        pw.println("else begin");
        pw.inc();
        pw.println("hangTimer <= hangTimer + 1;");
        pw.println("if (hangTimer > " + hangTimerExpire + " ) begin");
        pw.inc();
        pw.println("$fwrite (resultFile, \"FAIL: Hang Timer expired after %d action firings (%d - " + hangTimerExpire + "cycles)\\n\", actionFiringCount, clockCount);");
        pw.println("$fwrite (resultFile, \"\\tPortName : TokenCount\\n\");");
        for (TBIOHandler ioh : handlers) {
            pw.println("$fwrite (resultFile, \"\\t" + ioh.getName() + " : %d\\n\", " + ioh.getCountName() + ");");
        }
        pw.println("$finish;");
        pw.dec();
        pw.println("end");
        pw.dec();
        pw.println("end");
        pw.dec();
        pw.println("end");
        pw.dec();
        pw.println("endmodule // fixture");
    }

    /**
     * This is a really kludgy way to find the GO signal for each
     * internally fired task.
     */
    private String getActionFiringString() {
        final List terms = new ArrayList();
        for (Iterator iter = design.getDesignModule().getComponents().iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            if (o instanceof Call) {
                Bus goBus = ((Call) o).getGoPort().getBus();
                if (goBus == null || !(goBus.getOwner().getOwner() instanceof Kicker)) {
                    Port goPort = ((Call) o).getGoPort();
                    if (goPort.getValue().isConstant()) continue;
                    PortWire cport = new PortWire(goPort);
                    terms.add(cport.lexicalify().toString());
                }
            }
        }
        String result = "";
        for (Iterator iter = terms.iterator(); iter.hasNext(); ) {
            result += getInstanceName() + "." + iter.next();
            if (iter.hasNext()) result += " || ";
        }
        return result;
    }

    private String getActionFiringDoneString() {
        final List terms = new ArrayList();
        for (Iterator iter = design.getDesignModule().getComponents().iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            if (o instanceof Call) {
                Bus doneBus = ((Call) o).getExit(Exit.DONE).getDoneBus();
                if (doneBus != null) {
                    BusWire bwire = new BusWire(doneBus);
                    terms.add(bwire.lexicalify().toString());
                }
            }
        }
        String result = "";
        for (Iterator iter = terms.iterator(); iter.hasNext(); ) {
            result += getInstanceName() + "." + iter.next();
            if (iter.hasNext()) result += " || ";
        }
        return result;
    }

    private abstract class TBIOHandler {

        private String name;

        protected TBIOHandler(String name) {
            this.name = name;
            if (this.name.indexOf("_") > 0) this.name = this.name.substring(0, this.name.lastIndexOf("_"));
        }

        protected String getName() {
            return this.name;
        }

        public String getDataName() {
            return this.name + "_din";
        }

        public String getExistsName() {
            return this.name + "_exists";
        }

        public String getCountName() {
            return getDataName() + "_count";
        }

        public String getHandleName() {
            return this.name + "_id";
        }

        public String getVecFileName() {
            return GenericTestbenchWriter.this.getActorName() + "_" + getName() + ".vec";
        }

        public abstract String getActivityName();

        public abstract void writeDeclarations(IndentWriter pw);

        public abstract void writeInstantiation(IndentWriter pw);

        public abstract void writeInitial(IndentWriter pw);

        public abstract void writeStartSim(IndentWriter pw);

        public abstract void writeTest(IndentWriter pw);

        public void writeTermCondition(IndentWriter pw) {
        }
    }

    private class OutputHandler extends TBIOHandler {

        private FifoOutput output;

        public OutputHandler(FifoOutput out) {
            super(out.getDataPin().getName());
            this.output = out;
        }

        public int getDataWidth() {
            return this.output.getDataPin().getWidth();
        }

        public String getDataName() {
            return getName() + "_dout";
        }

        public String getExpectedName() {
            return this.getDataName() + "_expected";
        }

        public String getAckName() {
            return getName() + "_ack";
        }

        private String getSendName() {
            return getName() + "_send";
        }

        public String getActivityName() {
            return getSendName();
        }

        public void writeDeclarations(IndentWriter pw) {
            pw.println("wire [" + (getDataWidth() - 1) + ":0] " + getDataName() + ";");
            pw.println("wire        " + getSendName() + ";");
            pw.println("reg         " + getAckName() + ";");
            pw.println("reg         " + getExistsName() + ";");
            pw.println("reg [" + (getDataWidth() - 1) + ":0]  " + getExpectedName() + ";");
            pw.println("reg [31:0]  " + getCountName() + ";");
            pw.println("integer     " + getHandleName() + ";");
        }

        public void writeInstantiation(IndentWriter pw) {
            Set<SimplePin> pins = new HashSet(this.output.getPins());
            pins.remove(this.output.getDataPin());
            pins.remove(this.output.getSendPin());
            pins.remove(this.output.getAckPin());
            pw.print("." + this.output.getDataPin().getName() + "(" + getDataName() + "), ");
            pw.print("." + this.output.getSendPin().getName() + "(" + getSendName() + "), ");
            pw.print("." + this.output.getAckPin().getName() + "(" + getAckName() + "), ");
            if (this.output.getReadyPin() != null) {
                pw.print("." + this.output.getReadyPin().getName() + "(1'b1), ");
                pins.remove(this.output.getReadyPin());
            }
            for (SimplePin pin : pins) {
                if (pin.getName().toUpperCase().contains("CLK")) pw.print("." + pin.getName() + "(clk), ");
                if (pin.getName().toUpperCase().contains("RESET")) pw.print("." + pin.getName() + "(1'b0), "); else if (pin.getName().toUpperCase().contains("CONTROL")) pw.print("." + pin.getName() + "(), "); else pw.print("/* ." + pin.getName() + "(),*/ ");
            }
        }

        public void writeInitial(IndentWriter pw) {
            pw.println("" + getCountName() + " <= 0;");
            pw.println("" + getHandleName() + " <= $registerVectorFile(\"" + getVecFileName() + "\", " + getExpectedName() + ", " + getExistsName() + ");");
            pw.println("" + getExistsName() + " <= 0;");
            pw.println("" + getExpectedName() + " <= 0;");
            pw.println("" + getAckName() + " <= 1; // For now say we are always acking");
        }

        public void writeStartSim(IndentWriter pw) {
        }

        public void writeTest(IndentWriter pw) {
            pw.println("if (" + getSendName() + ") begin");
            pw.inc();
            pw.println("if (!" + getExistsName() + ") begin");
            pw.inc();
            pw.println("$fwrite(resultFile, \"FAIL: Token output from port " + getName() + " when no output was expected.  Output token %x at count %d\\n\", " + getDataName() + ", " + getCountName() + ");");
            pw.println("#100 $finish;");
            pw.dec();
            pw.println("end");
            pw.println("else if (" + getDataName() + " !== " + getExpectedName() + ") begin");
            pw.inc();
            pw.println("$fwrite(resultFile, \"FAIL: Incorrect result on port " + getName() + ".  output token count %d expected %d found %d\\n\", " + getCountName() + ", " + getExpectedName() + ", " + getDataName() + ");");
            pw.println("#100 $finish;");
            pw.dec();
            pw.println("end");
            pw.println("" + getCountName() + " <= " + getCountName() + " + 1;");
            pw.println("$vectorPop(" + getHandleName() + "); // May be invalid if beyond the end of the queue");
            pw.dec();
            pw.println("end // if (" + getSendName() + ")");
            pw.println("else begin");
            pw.inc();
            pw.println("$vectorPeek(" + getHandleName() + ");");
            pw.dec();
            pw.println("end");
        }
    }

    private class InputHandler extends TBIOHandler {

        private FifoInput input;

        public InputHandler(FifoInput in) {
            super(in.getDataPin().getName());
            this.input = in;
        }

        public int getDataWidth() {
            return this.input.getDataPin().getWidth();
        }

        public String getAckName() {
            return getName() + "_read";
        }

        public String getActivityName() {
            return getAckName();
        }

        public void writeDeclarations(IndentWriter pw) {
            pw.println("reg\t[" + (getDataWidth() - 1) + ":0] " + getDataName() + ";");
            pw.println("wire\t\t" + getAckName() + ";");
            pw.println("reg\t\t" + getExistsName() + ";");
            pw.println("reg\t[31:0] " + getCountName() + ";");
            pw.println("integer\t\t " + getHandleName() + ";");
        }

        public void writeInstantiation(IndentWriter pw) {
            Set<SimplePin> pins = new HashSet(this.input.getPins());
            pins.remove(this.input.getDataPin());
            pins.remove(this.input.getAckPin());
            pins.remove(this.input.getSendPin());
            pw.print("." + this.input.getDataPin().getName() + "(" + getDataName() + "), ");
            pw.print("." + this.input.getAckPin().getName() + "(" + getAckName() + "), ");
            pw.print("." + this.input.getSendPin().getName() + "(" + getExistsName() + "), ");
            for (SimplePin pin : pins) {
                if (pin.getName().toUpperCase().contains("CLK")) pw.print("." + pin.getName() + "(clk), ");
                if (pin.getName().toUpperCase().contains("RESET")) pw.print("." + pin.getName() + "(1'b0), "); else if (pin.getName().toUpperCase().contains("CONTROL")) pw.print("." + pin.getName() + "(1'b0), "); else pw.print("/* ." + pin.getName() + "(),*/");
            }
        }

        public void writeInitial(IndentWriter pw) {
            pw.println(getCountName() + " <= 0;");
            pw.println(getHandleName() + " <= $registerVectorFile(\"" + getVecFileName() + "\", " + getDataName() + ", " + getExistsName() + ");");
            pw.println(getExistsName() + " <= 0;");
            pw.println(getDataName() + " <= 0;");
        }

        public void writeStartSim(IndentWriter pw) {
        }

        public void writeTest(IndentWriter pw) {
            pw.println("if (" + getAckName() + ") begin");
            pw.inc();
            pw.println("if (!" + getExistsName() + ") begin");
            pw.inc();
            pw.println("$fwrite(resultFile, \"FAIL: Illegal read from empty queue, port " + getName() + " on %d token\\n\", " + getCountName() + ");");
            pw.println("#100 $finish;");
            pw.dec();
            pw.println("end");
            pw.println("" + getCountName() + " <= " + getCountName() + " + 1;");
            pw.println("$vectorPop(" + getHandleName() + ");");
            pw.dec();
            pw.println("end else begin");
            pw.inc();
            pw.println("$vectorPeek(" + getHandleName() + ");");
            pw.dec();
            pw.println("end");
        }
    }

    private class StateHandler {

        Map names = new HashMap();

        private StateHandler(Design design) {
            for (Register reg : design.getRegisters()) {
                Register.Physical phys = (Register.Physical) reg.getPhysicalComponent();
                Bus output = phys.getRegisterOutput();
                BusWire bwire = new BusWire(output);
                GenericModule gmod = new GenericModule(phys);
                String instance = gmod.makeInstance().getIdentifier().toString();
                String netName = "fixture.dut." + instance + "." + bwire.lexicalify().toString();
                if (instance.startsWith("register_")) instance = instance.substring(instance.indexOf("_", 9) + 1);
                if (instance.endsWith("_1")) instance = instance.substring(0, instance.length() - 2);
                names.put(instance, netName);
            }
        }

        public List<String> getCaptureStrings() {
            List<String> keys = new ArrayList(names.keySet());
            if (keys.isEmpty()) return Collections.EMPTY_LIST;
            List<String> alpha = new LinkedList();
            alpha.add(keys.remove(0));
            for (String str : keys) {
                boolean inserted = false;
                for (int i = 0; i < alpha.size(); i++) {
                    if (str.compareTo((String) alpha.get(i)) < 0) {
                        alpha.add(i, str);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) alpha.add(str);
            }
            List<String> netDisplay = new ArrayList();
            for (String key : alpha) {
                String netName = (String) this.names.get(key);
                netDisplay.add("$fwrite(stateDumpFile, \"" + key + " : %d\\n\", " + netName + ");");
            }
            return netDisplay;
        }
    }
}
