package avrora.monitors;

import avrora.actions.SimAction;
import avrora.arch.avr.AVRProperties;
import avrora.arch.legacy.LegacyState;
import avrora.core.Program;
import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.mcu.Microcontroller;
import avrora.sim.util.MemoryProfiler;
import cck.text.*;
import cck.util.Option;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>MemoryMonitor</code> class implements a monitor that collects information about how the program
 * accesses the data memory over its execution. For each RAM address it keeps an account of the number of
 * reads and the number of writes and reports that information after the program is completed.
 *
 * @author Ben L. Titzer
 */
public class MemoryMonitor extends MonitorFactory {

    public final Option.List LOCATIONS = newOptionList("locations", "", "This option, when set, specifies a list of memory locations to instrument. When " + "this option is not specified, the monitor will instrument all reads and writes to " + "memory.");

    public final Option.Bool LOWER_ADDRESS = newOption("low-addresses", false, "When this option is enabled, the memory monitor will be inserted for lower addresses, " + "recording reads and writes to the general purpose registers on the AVR and also IO registers " + "through direct and indirect memory reads and writes.");

    public class Monitor implements avrora.monitors.Monitor {

        public final Simulator simulator;

        public final Microcontroller microcontroller;

        public final Program program;

        public final int ramsize;

        public final int memstart;

        public final MemoryProfiler memprofile;

        Monitor(Simulator s) {
            simulator = s;
            microcontroller = simulator.getMicrocontroller();
            program = simulator.getProgram();
            AVRProperties p = (AVRProperties) microcontroller.getProperties();
            ramsize = p.sram_size + p.ioreg_size + LegacyState.NUM_REGS;
            if (LOWER_ADDRESS.get()) {
                memstart = 0;
            } else {
                memstart = LegacyState.IOREG_BASE + p.ioreg_size;
            }
            memprofile = new MemoryProfiler(ramsize);
            insertWatches();
        }

        private void insertWatches() {
            if (!LOCATIONS.get().isEmpty()) {
                List l = LOCATIONS.get();
                List loc = SimAction.getLocationList(program, l);
                Iterator i = loc.iterator();
                while (i.hasNext()) {
                    SourceMapping.Location location = (SourceMapping.Location) i.next();
                    simulator.insertWatch(memprofile, location.vma_addr - 0x800000);
                }
            } else {
                for (int cntr = memstart; cntr < ramsize; cntr++) {
                    simulator.insertWatch(memprofile, cntr);
                }
            }
        }

        public void report() {
            TermUtil.printSeparator("Memory profiling results for node " + simulator.getID());
            Terminal.printGreen("   Address     Reads               Writes");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);
            double rtotal = 0;
            long[] rcount = memprofile.rcount;
            double wtotal = 0;
            long[] wcount = memprofile.wcount;
            int imax = rcount.length;
            for (int cntr = 0; cntr < imax; cntr++) {
                rtotal += rcount[cntr];
                wtotal += wcount[cntr];
            }
            int zeroes = 0;
            for (int cntr = memstart; cntr < imax; cntr++) {
                long r = rcount[cntr];
                long w = wcount[cntr];
                if (r == 0 && w == 0) zeroes++; else zeroes = 0;
                if (zeroes == 2) {
                    Terminal.println("                   .                    .");
                    continue;
                } else if (zeroes > 2) continue;
                String addr = StringUtil.addrToString(cntr);
                printLine(addr, r, rtotal, w, wtotal);
            }
            printLine("total ", (long) rtotal, rtotal, (long) wtotal, wtotal);
            Terminal.nextln();
        }

        private void printLine(String addr, long r, double rtotal, long w, double wtotal) {
            String rcnt = StringUtil.rightJustify(r, 8);
            float rpcnt = (float) (100 * r / rtotal);
            String rpercent = StringUtil.rightJustify(StringUtil.toFixedFloat(rpcnt, 4), 8) + " %";
            String wcnt = StringUtil.rightJustify(w, 8);
            float wpcnt = (float) (100 * w / wtotal);
            String wpercent = StringUtil.rightJustify(StringUtil.toFixedFloat(wpcnt, 4), 8) + " %";
            Terminal.printGreen("    " + addr);
            Terminal.print(": ");
            Terminal.printBrightCyan(rcnt);
            Terminal.print(' ' + ("  " + rpercent));
            Terminal.printBrightCyan(wcnt);
            Terminal.println(' ' + ("  " + wpercent));
        }
    }

    public MemoryMonitor() {
        super("The \"memory\" monitor collects information about the " + "memory usage statistics of the program, including the number " + "of reads and writes to every byte of data memory.");
    }

    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new Monitor(s);
    }
}
