package jdos.misc;

import jdos.cpu.Callback;
import jdos.dos.Dos;
import jdos.dos.Dos_PSP;
import jdos.dos.Dos_files;
import jdos.dos.drives.Drive_virtual;
import jdos.hardware.Memory;
import jdos.misc.setup.CommandLine;
import jdos.misc.setup.Section;
import jdos.shell.Dos_shell;
import jdos.shell.Shell;
import jdos.util.*;
import jdos.Dosbox;
import java.util.Vector;

public abstract class Program {

    private static int call_program;

    private static final byte[] exe_block = { (byte) 0xbc, 0x00, 0x04, (byte) 0xbb, 0x40, 0x00, (byte) 0xb4, 0x4a, (byte) 0xcd, 0x21, (byte) 0xFE, 0x38, 0x00, 0x00, (byte) 0xb8, 0x00, 0x4c, (byte) 0xcd, 0x21 };

    private static final int CB_POS = 12;

    public static interface PROGRAMS_Main {

        public Program call();
    }

    private static Vector internal_progs = new Vector();

    public abstract void Run();

    public static void PROGRAMS_MakeFile(String name, PROGRAMS_Main main) {
        byte[] comdata = new byte[32];
        System.arraycopy(exe_block, 0, comdata, 0, exe_block.length);
        comdata[CB_POS] = (byte) (call_program & 0xff);
        comdata[CB_POS + 1] = (byte) ((call_program >> 8) & 0xff);
        if (internal_progs.size() > 255) Log.exit("PROGRAMS_MakeFile program size too large (" + internal_progs.size() + ")");
        int index = internal_progs.size();
        internal_progs.addElement(main);
        comdata[exe_block.length] = (byte) (index & 0xFF);
        int size = exe_block.length + 1;
        Drive_virtual.VFILE_Register(name, comdata, size);
    }

    private static Callback.Handler PROGRAMS_Handler = new Callback.Handler() {

        public String getName() {
            return "Program.PROGRAMS_Handler";
        }

        public int call() {
            int size = 1;
            int index;
            int reader = Memory.PhysMake(Dos.dos.psp(), 256 + exe_block.length);
            index = Memory.mem_readb(reader++);
            Program new_program;
            if (index > internal_progs.size()) Log.exit("something is messing with the memory");
            PROGRAMS_Main handler = (PROGRAMS_Main) internal_progs.elementAt(index);
            new_program = handler.call();
            new_program.Run();
            return Callback.CBRET_NONE;
        }
    };

    protected String temp_line;

    protected CommandLine cmd;

    protected Dos_PSP psp;

    public Program() {
        psp = new Dos_PSP(Dos.dos.psp());
        int envscan = Memory.PhysMake(psp.GetEnvironment(), 0);
        while (Memory.mem_readb(envscan) != 0) envscan += Memory.mem_strlen(envscan) + 1;
        envscan += 3;
        String tail;
        tail = Memory.MEM_BlockRead(Memory.PhysMake(Dos.dos.psp(), 128), 128);
        if (tail.length() > 0) tail = tail.substring(1, (int) tail.charAt(0) + 1);
        String filename = Memory.MEM_StrCopy(envscan, 256);
        cmd = new CommandLine(filename, tail);
    }

    public void ChangeToLongCmd() {
        if (cmd.Get_arglength() > 100) {
            CommandLine temp = new CommandLine(cmd.GetFileName(), Dos_shell.full_arguments);
            cmd = temp;
        }
        Dos_shell.full_arguments = "";
    }

    protected void WriteOut(String format) {
        WriteOut(format, new Object[0]);
    }

    protected void WriteOut(String format, Object[] args) {
        String buf = StringHelper.sprintf(format, args);
        int size = buf.length();
        for (int i = 0; i < size; i++) {
            byte[] out = new byte[1];
            IntRef s = new IntRef(1);
            if (buf.charAt(i) == 0xA && i > 0 && buf.charAt(i - 1) != 0xD) {
                out[0] = 0xD;
                Dos_files.DOS_WriteFile(Dos_files.STDOUT, out, s);
            }
            out[0] = (byte) buf.charAt(i);
            Dos_files.DOS_WriteFile(Dos_files.STDOUT, out, s);
        }
    }

    protected void WriteOut_NoParsing(String format) {
        int size = format.length();
        for (int i = 0; i < size; i++) {
            byte[] out = new byte[1];
            IntRef s = new IntRef(1);
            if (format.charAt(i) == 0xA && i > 0 && format.charAt(i - 1) != 0xD) {
                out[0] = 0xD;
                Dos_files.DOS_WriteFile(Dos_files.STDOUT, out, s);
            }
            out[0] = (byte) format.charAt(i);
            Dos_files.DOS_WriteFile(Dos_files.STDOUT, out, s);
        }
    }

    public boolean GetEnvStr(String entry, StringRef result) {
        if (entry.equalsIgnoreCase("errorlevel")) {
            result.value = entry + "=" + String.valueOf(Dos.dos.return_code);
            return true;
        }
        int env_read = Memory.PhysMake(psp.GetEnvironment(), 0);
        String env_string;
        result.value = "";
        if (entry.length() == 0) return false;
        do {
            env_string = Memory.MEM_StrCopy(env_read, 1024);
            if (env_string.length() == 0) return false;
            env_read += env_string.length() + 1;
            int pos = env_string.indexOf('=');
            if (pos < 0) continue;
            String key = env_string.substring(0, pos);
            if (key.equalsIgnoreCase(entry)) {
                result.value = env_string;
                return true;
            }
        } while (true);
    }

    public boolean GetEnvNum(int num, StringRef result) {
        String env_string;
        int env_read = Memory.PhysMake(psp.GetEnvironment(), 0);
        do {
            env_string = Memory.MEM_StrCopy(env_read, 1024);
            if (env_string.length() == 0) return false;
            if (num == 0) {
                result.value = env_string;
                return true;
            }
            env_read += env_string.length() + 1;
            num--;
        } while (true);
    }

    public int GetEnvCount() {
        int env_read = Memory.PhysMake(psp.GetEnvironment(), 0);
        int num = 0;
        while (Memory.mem_readb(env_read) != 0) {
            for (; Memory.mem_readb(env_read) != 0; env_read++) {
            }
            ;
            env_read++;
            num++;
        }
        return num;
    }

    public boolean SetEnv(String entry, String new_string) {
        int env_read = Memory.PhysMake(psp.GetEnvironment(), 0);
        int env_write = env_read;
        String env_string;
        do {
            env_string = Memory.MEM_StrCopy(env_read, 1024);
            if (env_string.length() == 0) break;
            env_read += env_string.length() + 1;
            int pos = env_string.indexOf('=');
            if (pos < 0) continue;
            String key = env_string.substring(0, pos);
            if (key.equalsIgnoreCase(entry)) {
                continue;
            }
            Memory.MEM_BlockWrite(env_write, env_string, env_string.length() + 1);
            env_write += env_string.length() + 1;
        } while (true);
        if (new_string.length() > 0) {
            new_string = entry.toUpperCase() + "=" + new_string;
            Memory.MEM_BlockWrite(env_write, new_string, new_string.length() + 1);
            env_write += new_string.length() + 1;
        }
        Memory.mem_writed(env_write, 0);
        return true;
    }

    static class CONFIG extends Program {

        public void Run() {
            if ((temp_line = cmd.FindString("-writeconf", true)) != null || (temp_line = cmd.FindString("-wc", true)) != null) {
                if (Dosbox.control.SecureMode()) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                    return;
                }
                if (!FileIOFactory.canOpen(temp_line, FileIOFactory.MODE_WRITE)) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_FILE_ERROR"), new Object[] { temp_line });
                    return;
                }
                Dosbox.control.PrintConfig(temp_line);
                return;
            }
            if ((temp_line = cmd.FindString("-writelang", true)) != null || (temp_line = cmd.FindString("-wl", true)) != null) {
                if (Dosbox.control.SecureMode()) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                    return;
                }
                if (!FileIOFactory.canOpen(temp_line, FileIOFactory.MODE_WRITE)) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_FILE_ERROR"), new Object[] { temp_line });
                    return;
                }
                Msg.write(temp_line);
                return;
            }
            if (cmd.FindExist("-securemode", true)) {
                Dosbox.control.SwitchToSecureMode();
                WriteOut(Msg.get("PROGRAM_CONFIG_SECURE_ON"));
                return;
            }
            if ((temp_line = cmd.FindString("-get", true)) != null) {
                String temp2 = cmd.GetStringRemain();
                if (temp2 != null && temp2.length() > 0) temp_line = temp_line + " " + temp2;
                int space = temp_line.indexOf(" ");
                if (space < 0) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_GET_SYNTAX"));
                    return;
                }
                String prop = temp_line.substring(space + 1);
                temp_line = temp_line.substring(0, space);
                Section sec = Dosbox.control.GetSection(temp_line);
                if (sec == null) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_SECTION_ERROR"), new Object[] { temp_line });
                    return;
                }
                String val = sec.GetPropValue(prop);
                if (val.equals(Section.NO_SUCH_PROPERTY)) {
                    WriteOut(Msg.get("PROGRAM_CONFIG_NO_PROPERTY"), new Object[] { prop, temp_line });
                    return;
                }
                WriteOut(val);
                Shell.first_shell.SetEnv("CONFIG", val);
                return;
            }
            if ((temp_line = cmd.FindString("-set", true)) != null) {
                String temp2 = cmd.GetStringRemain();
                if (temp2 != null && temp2.length() > 0) temp_line = temp_line + " " + temp2;
            } else if ((temp_line = cmd.GetStringRemain()) == null) {
                WriteOut(Msg.get("PROGRAM_CONFIG_USAGE"));
                return;
            }
            int pos = temp_line.indexOf(' ');
            if (pos < 0) pos = temp_line.indexOf('=');
            String copy;
            if (pos >= 0) {
                copy = temp_line.substring(0, pos);
                temp_line = temp_line.substring(pos + 1);
            } else {
                WriteOut(Msg.get("PROGRAM_CONFIG_USAGE"));
                return;
            }
            int sign = temp_line.indexOf('=');
            if (sign <= 0) {
                sign = temp_line.indexOf(' ');
                if (sign >= 0) {
                    temp_line = temp_line.substring(0, sign) + "=" + temp_line.substring(sign + 1);
                } else {
                    Section sec = Dosbox.control.GetSectionFromProperty(copy);
                    if (sec == null) {
                        if (Dosbox.control.GetSectionFromProperty(temp_line) != null) return;
                        WriteOut(Msg.get("PROGRAM_CONFIG_PROPERTY_ERROR"), new Object[] { copy });
                        return;
                    }
                    temp_line = copy + "=" + temp_line;
                    copy = sec.GetName();
                    sign = temp_line.indexOf(' ');
                    if (sign >= 0) temp_line = temp_line.substring(0, sign) + "=" + temp_line.substring(sign + 1);
                }
            }
            Section sec = Dosbox.control.GetSection(copy);
            if (sec == null) {
                WriteOut(Msg.get("PROGRAM_CONFIG_SECTION_ERROR"), new Object[] { copy });
                return;
            }
            sec.ExecuteDestroy(false);
            sec.HandleInputline(temp_line);
            sec.ExecuteInit(false);
        }
    }

    private static PROGRAMS_Main CONFIG_ProgramStart = new PROGRAMS_Main() {

        public Program call() {
            return new CONFIG();
        }
    };

    public static Section.SectionFunction PROGRAMS_Init = new Section.SectionFunction() {

        public void call(Section section) {
            call_program = Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_program, PROGRAMS_Handler, Callback.CB_RETF, "internal program");
            PROGRAMS_MakeFile("CONFIG.COM", CONFIG_ProgramStart);
            Msg.add("PROGRAM_CONFIG_FILE_ERROR", "Can't open file %s\n");
            Msg.add("PROGRAM_CONFIG_USAGE", "Config tool:\nUse -writeconf filename to write the current config.\nUse -writelang filename to write the current language strings.\n");
            Msg.add("PROGRAM_CONFIG_SECURE_ON", "Switched to secure mode.\n");
            Msg.add("PROGRAM_CONFIG_SECURE_DISALLOW", "This operation is not permitted in secure mode.\n");
            Msg.add("PROGRAM_CONFIG_SECTION_ERROR", "Section %s doesn't exist.\n");
            Msg.add("PROGRAM_CONFIG_PROPERTY_ERROR", "No such section or property.\n");
            Msg.add("PROGRAM_CONFIG_NO_PROPERTY", "There is no property %s in section %s.\n");
            Msg.add("PROGRAM_CONFIG_GET_SYNTAX", "Correct syntax: config -get \"section property\".\n");
        }
    };
}
