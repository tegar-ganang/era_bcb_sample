package jdos.shell;

import jdos.Dosbox;
import jdos.ints.Bios_keyboard;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.*;
import jdos.dos.drives.Drive_local;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.Msg;
import jdos.misc.Program;
import jdos.misc.setup.CommandLine;
import jdos.misc.setup.Config;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.*;
import java.util.Vector;

public class Dos_shell extends Program {

    public interface handler {

        public void call(String arg);
    }

    private Vector l_history = new Vector();

    private Vector l_completion = new Vector();

    int input_handle;

    BatchFile bf;

    boolean echo;

    boolean exit;

    boolean call;

    String completion_start;

    int completion_index;

    public Dos_shell() {
        input_handle = Dos_files.STDIN;
        echo = true;
        exit = false;
        bf = null;
        call = false;
        completion_start = null;
    }

    public void Run() {
        String line;
        if ((line = cmd.FindStringRemain("/C")) != null) {
            int pos = line.indexOf('\n');
            if (pos >= 0) line = line.substring(0, pos);
            pos = line.indexOf('\r');
            if (pos >= 0) line = line.substring(0, pos);
            Dos_shell temp = new Dos_shell();
            temp.echo = echo;
            temp.ParseLine(line);
            temp.RunInternal();
            return;
        }
        WriteOut(Msg.get("SHELL_STARTUP_BEGIN"), new Object[] { Config.VERSION });
        if (Config.C_DEBUG) WriteOut(Msg.get("SHELL_STARTUP_DEBUG"));
        if (Dosbox.machine == MachineType.MCH_CGA) WriteOut(Msg.get("SHELL_STARTUP_CGA"));
        if (Dosbox.machine == MachineType.MCH_HERC) WriteOut(Msg.get("SHELL_STARTUP_HERC"));
        WriteOut(Msg.get("SHELL_STARTUP_END"));
        if ((line = cmd.FindString("/INIT", true)) != null) {
            ParseLine(line);
        }
        do {
            if (bf != null) {
                String input_line;
                if ((input_line = bf.ReadLine()) != null) {
                    if (echo) {
                        if (input_line.length() > 0 && input_line.charAt(0) != '@') {
                            ShowPrompt();
                            WriteOut_NoParsing(input_line);
                            WriteOut_NoParsing("\n");
                        }
                    }
                    ParseLine(input_line);
                    if (echo) WriteOut("\n");
                }
            } else {
                if (echo) ShowPrompt();
                String input_line = InputCommand();
                if (input_line == null) input_line = "";
                ParseLine(input_line);
                if (echo && bf == null) WriteOut_NoParsing("\n");
            }
        } while (!exit);
    }

    void RunInternal() {
        String input_line;
        while (bf != null && (input_line = bf.ReadLine()) != null) {
            if (echo) {
                if (input_line.charAt(0) != '@') {
                    ShowPrompt();
                    WriteOut_NoParsing(input_line);
                    WriteOut_NoParsing("\n");
                }
            }
            ParseLine(input_line);
        }
    }

    void ParseLine(String line) {
        Log.log(LogTypes.LOG_EXEC, LogSeverities.LOG_ERROR, "Parsing command line: " + line);
        if (line.startsWith("@")) line = line.substring(1);
        line = line.trim();
        StringRef in = new StringRef(null);
        StringRef out = new StringRef(null);
        IntRef dummy = new IntRef(0), dummy2 = new IntRef(0);
        LongRef bigdummy = new LongRef(0);
        int num = 0;
        BooleanRef append = new BooleanRef();
        boolean normalstdin = false;
        boolean normalstdout = false;
        StringRef s = new StringRef(line);
        num = GetRedirection(s, in, out, append);
        line = s.value;
        if (num > 1) Log.log_msg("SHELL:Multiple command on 1 line not supported");
        if (in.value != null || out != null) {
            normalstdin = (psp.GetFileHandle(0) != 0xff);
            normalstdout = (psp.GetFileHandle(1) != 0xff);
        }
        if (in.value != null) {
            if (Dos_files.DOS_OpenFile(in.value, Dos_files.OPEN_READ, dummy)) {
                Dos_files.DOS_CloseFile(dummy.value);
                Log.log_msg("SHELL:Redirect input from " + in);
                if (normalstdin) Dos_files.DOS_CloseFile(0);
                Dos_files.DOS_OpenFile(in.value, Dos_files.OPEN_READ, dummy);
            }
        }
        if (out.value != null) {
            Log.log_msg("SHELL:Redirect output to " + out.value);
            if (normalstdout) Dos_files.DOS_CloseFile(1);
            if (!normalstdin && in.value == null) Dos_files.DOS_OpenFile("con", Dos_files.OPEN_READWRITE, dummy);
            boolean status = true;
            if (append.value) {
                if ((status = Dos_files.DOS_OpenFile(out.value, Dos_files.OPEN_READWRITE, dummy))) {
                    Dos_files.DOS_SeekFile(1, bigdummy, Dos_files.DOS_SEEK_END);
                } else {
                    status = Dos_files.DOS_CreateFile(out.value, Dos_system.DOS_ATTR_ARCHIVE, dummy);
                }
            } else {
                status = Dos_files.DOS_OpenFileExtended(out.value, Dos_files.OPEN_READWRITE, Dos_system.DOS_ATTR_ARCHIVE, 0x12, dummy, dummy2);
            }
            if (!status && normalstdout) Dos_files.DOS_OpenFile("con", Dos_files.OPEN_READWRITE, dummy);
            if (!normalstdin && in.value == null) Dos_files.DOS_CloseFile(0);
        }
        DoCommand(line);
        if (in.value != null) {
            Dos_files.DOS_CloseFile(0);
            if (normalstdin) Dos_files.DOS_OpenFile("con", Dos_files.OPEN_READWRITE, dummy);
        }
        if (out.value != null) {
            Dos_files.DOS_CloseFile(1);
            if (!normalstdin) Dos_files.DOS_OpenFile("con", Dos_files.OPEN_READWRITE, dummy);
            if (normalstdout) Dos_files.DOS_OpenFile("con", Dos_files.OPEN_READWRITE, dummy);
            if (!normalstdin) Dos_files.DOS_CloseFile(0);
        }
    }

    int GetRedirection(StringRef s, StringRef ifn, StringRef ofn, BooleanRef append) {
        String lr = s.value;
        StringBuffer lw = new StringBuffer();
        char ch;
        int num = 0;
        boolean quote = false;
        while (lr.length() > 0) {
            ch = lr.charAt(0);
            lr = lr.substring(1);
            if (quote && ch != '"') {
                lw.append(ch);
                continue;
            }
            switch(ch) {
                case '"':
                    quote = !quote;
                    break;
                case '>':
                    append.value = (lr.length() > 0 && lr.charAt(0) == '>');
                    if (append.value) lr = lr.substring(1);
                    lr = lr.trim();
                    ofn.value = lr;
                    while (lr.length() > 0 && lr.charAt(0) != ' ' && lr.charAt(0) != '<' && lr.charAt(0) != '|') lr = lr.substring(1);
                    if ((ofn.value.length() != lr.length()) && ofn.value.endsWith(":")) ofn.value = ofn.value.substring(0, ofn.value.length() - 1);
                    ofn.value = ofn.value.substring(0, ofn.value.length() - lr.length());
                    continue;
                case '<':
                    lr = lr.trim();
                    ifn.value = lr;
                    while (lr.length() > 0 && lr.charAt(0) != ' ' && lr.charAt(0) != '<' && lr.charAt(0) != '|') lr = lr.substring(1);
                    if ((ifn.value.length() != lr.length()) && ifn.value.endsWith(":")) ifn.value = ifn.value.substring(0, ifn.value.length() - 1);
                    ifn.value = ifn.value.substring(0, ifn.value.length() - lr.length());
                    continue;
                case '|':
                    ch = 0;
                    num++;
            }
            lw.append(ch);
        }
        s.value = lw.toString();
        return num;
    }

    static void outc(int b) {
        byte[] c = new byte[1];
        c[0] = (byte) b;
        IntRef n = new IntRef(1);
        Dos_files.DOS_WriteFile(Dos_files.STDOUT, c, n);
    }

    String InputCommand() {
        int size = Shell.CMD_MAXLINE - 2;
        byte[] c = new byte[1];
        IntRef n = new IntRef(1);
        int str_len = 0;
        int str_index = 0;
        IntRef len = new IntRef(0);
        boolean current_hist = false;
        byte[] line = new byte[Shell.CMD_MAXLINE];
        int it_history = 0;
        int it_completion = 0;
        while (size != 0) {
            Dos.dos.echo = false;
            while (!Dos_files.DOS_ReadFile(input_handle, c, n)) {
                IntRef dummy = new IntRef(0);
                Dos_files.DOS_CloseFile(input_handle);
                Dos_files.DOS_OpenFile("con", 2, dummy);
                Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "Reopening the input handle.This is a bug!");
            }
            if (n.value == 0) {
                size = 0;
                continue;
            }
            switch(c[0]) {
                case 0x00:
                    {
                        Dos_files.DOS_ReadFile(input_handle, c, n);
                        switch(c[0]) {
                            case 0x3d:
                                if (l_history.size() == 0) break;
                                it_history = 0;
                                if (l_history.size() > 0 && ((String) l_history.firstElement()).length() > str_len) {
                                    String reader = ((String) l_history.firstElement()).substring(str_len);
                                    for (int i = 0; i < reader.length(); i++) {
                                        c[0] = (byte) reader.charAt(0);
                                        line[str_index++] = (byte) reader.charAt(0);
                                        Dos_files.DOS_WriteFile(Dos_files.STDOUT, c, n);
                                    }
                                    str_len = str_index = ((String) l_history.firstElement()).length();
                                    size = Shell.CMD_MAXLINE - str_index - 2;
                                    line[str_len] = 0;
                                }
                                break;
                            case 0x4B:
                                if (str_index != 0) {
                                    outc(8);
                                    str_index--;
                                }
                                break;
                            case 0x4D:
                                if (str_index < str_len) {
                                    outc(line[str_index++]);
                                }
                                break;
                            case 0x47:
                                while (str_index != 0) {
                                    outc(8);
                                    str_index--;
                                }
                                break;
                            case 0x4F:
                                while (str_index < str_len) {
                                    outc(line[str_index++]);
                                }
                                break;
                            case 0x48:
                                if (l_history.size() == 0 || it_history == l_history.size()) break;
                                if (it_history == 0 && !current_hist) {
                                    current_hist = true;
                                    l_history.insertElementAt(new String(line, 0, str_len), 0);
                                    it_history++;
                                }
                                for (; str_index > 0; str_index--) {
                                    outc(8);
                                    outc(' ');
                                    outc(8);
                                }
                                StringHelper.strcpy(line, (String) l_history.elementAt(it_history));
                                len.value = ((String) l_history.elementAt(it_history)).length();
                                str_len = str_index = len.value;
                                size = Shell.CMD_MAXLINE - str_index - 2;
                                Dos_files.DOS_WriteFile(Dos_files.STDOUT, line, len);
                                it_history++;
                                break;
                            case 0x50:
                                if (l_history.size() == 0 || it_history == 0) break;
                                it_history--;
                                if (it_history == 0) {
                                    it_history++;
                                    if (current_hist) {
                                        current_hist = false;
                                        l_history.removeElementAt(0);
                                    }
                                    break;
                                } else it_history--;
                                for (; str_index > 0; str_index--) {
                                    outc(8);
                                    outc(' ');
                                    outc(8);
                                }
                                StringHelper.strcpy(line, (String) l_history.elementAt(it_history));
                                len.value = ((String) l_history.elementAt(it_history)).length();
                                str_len = str_index = len.value;
                                size = Shell.CMD_MAXLINE - str_index - 2;
                                Dos_files.DOS_WriteFile(Dos_files.STDOUT, line, len);
                                it_history++;
                                break;
                            case 0x53:
                                {
                                    if (str_index >= str_len) break;
                                    IntRef a = new IntRef(str_len - str_index - 1);
                                    byte[] text = new byte[a.value];
                                    System.arraycopy(line, str_index + 1, text, 0, a.value);
                                    Dos_files.DOS_WriteFile(Dos_files.STDOUT, text, a);
                                    outc(' ');
                                    outc(8);
                                    for (int i = str_index; i < str_len - 1; i++) {
                                        line[i] = line[i + 1];
                                        outc(8);
                                    }
                                    line[--str_len] = 0;
                                    size++;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                case 0x08:
                    if (str_index != 0) {
                        outc(8);
                        int str_remain = str_len - str_index;
                        size++;
                        if (str_remain != 0) {
                            for (int i = 0; i < str_remain; i++) line[str_index - 1 + i] = line[str_index + i];
                            line[--str_len] = 0;
                            str_index--;
                            for (int i = str_index; i < str_len; i++) outc(line[i]);
                        } else {
                            line[--str_index] = '\0';
                            str_len--;
                        }
                        outc(' ');
                        outc(8);
                        while (str_remain-- != 0) outc(8);
                    }
                    if (l_completion.size() != 0) l_completion.clear();
                    break;
                case 0x0a:
                    break;
                case 0x0d:
                    outc('\n');
                    size = 0;
                    break;
                case '\t':
                    {
                        if (l_completion.size() != 0) {
                            it_completion++;
                            if (it_completion == l_completion.size()) it_completion = 0;
                        } else {
                            boolean dir_only = StringHelper.toString(line).toUpperCase().startsWith("CD ");
                            String sLine = StringHelper.toString(line);
                            int p_completion_start = sLine.lastIndexOf(' ');
                            if (p_completion_start >= 0) {
                                p_completion_start++;
                                completion_index = p_completion_start;
                            } else {
                                p_completion_start = 0;
                                completion_index = 0;
                            }
                            int path;
                            if ((path = sLine.substring(completion_index).lastIndexOf('\\')) >= 0) completion_index += path + 1;
                            if ((path = sLine.substring(completion_index).lastIndexOf('/')) >= 0) completion_index += path + 1;
                            String mask;
                            if (p_completion_start >= 0) {
                                mask = sLine.substring(p_completion_start);
                                int dot_pos = mask.lastIndexOf('.');
                                int bs_pos = mask.lastIndexOf('\\');
                                int fs_pos = mask.lastIndexOf('/');
                                int cl_pos = mask.lastIndexOf(':');
                                if ((dot_pos - bs_pos > 0) && (dot_pos - fs_pos > 0) && (dot_pos - cl_pos > 0)) mask += "*"; else mask += "*.*";
                            } else {
                                mask = "*.*";
                            }
                            int save_dta = Dos.dos.dta();
                            Dos.dos.dta((int) Dos.dos.tables.tempdta);
                            boolean res = Dos_files.DOS_FindFirst(mask, 0xffff & ~Dos_system.DOS_ATTR_VOLUME);
                            if (!res) {
                                Dos.dos.dta((int) save_dta);
                                break;
                            }
                            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());
                            StringRef name = new StringRef();
                            LongRef sz = new LongRef(0);
                            IntRef date = new IntRef(0);
                            IntRef time = new IntRef(0);
                            ShortRef att = new ShortRef(0);
                            int extIndex = 0;
                            while (res) {
                                dta.GetResult(name, sz, date, time, att);
                                if (!name.value.equals(".") && !name.value.equals("..")) {
                                    if (dir_only) {
                                        if ((att.value & Dos_system.DOS_ATTR_DIRECTORY) != 0) l_completion.add(name.value);
                                    } else {
                                        int pos = name.value.lastIndexOf('.');
                                        String ext = null;
                                        if (pos >= 0) ext = name.value.substring(pos + 1);
                                        if (ext != null && (ext.equalsIgnoreCase("BAT") || ext.equalsIgnoreCase("COM") || ext.equalsIgnoreCase("EXE"))) l_completion.insertElementAt(name.value, extIndex++); else l_completion.add(name.value);
                                    }
                                }
                                res = Dos_files.DOS_FindNext();
                            }
                            it_completion = 0;
                            Dos.dos.dta((int) save_dta);
                        }
                        if (l_completion.size() != 0 && ((String) l_completion.elementAt(it_completion)).length() != 0) {
                            for (; str_index > completion_index; str_index--) {
                                outc(8);
                                outc(' ');
                                outc(8);
                            }
                            StringHelper.strcpy(line, completion_index, (String) l_completion.elementAt(it_completion));
                            len.value = ((String) l_completion.elementAt(it_completion)).length();
                            str_len = str_index = completion_index + len.value;
                            size = Shell.CMD_MAXLINE - str_index - 2;
                            Dos_files.DOS_WriteFile(Dos_files.STDOUT, ((String) l_completion.elementAt(it_completion)).getBytes(), len);
                        }
                    }
                    break;
                case 0x1b:
                    outc('\\');
                    outc('\n');
                    line[0] = 0;
                    if (l_completion.size() != 0) l_completion.clear();
                    StringHelper.strcpy(line, InputCommand());
                    size = 0;
                    str_len = 0;
                    break;
                default:
                    if (l_completion.size() != 0) l_completion.clear();
                    if (str_index < str_len && true) {
                        outc(' ');
                        IntRef a = new IntRef(str_len - str_index);
                        byte[] text = new byte[a.value];
                        System.arraycopy(line, str_index, text, 0, a.value);
                        Dos_files.DOS_WriteFile(Dos_files.STDOUT, text, a);
                        outc(8);
                        for (int i = str_len; i > str_index; i--) {
                            line[i] = line[i - 1];
                            outc(8);
                        }
                        line[++str_len] = 0;
                        size--;
                    }
                    line[str_index] = c[0];
                    str_index++;
                    if (str_index > str_len) {
                        line[str_index] = '\0';
                        str_len++;
                        size--;
                    }
                    Dos_files.DOS_WriteFile(Dos_files.STDOUT, c, n);
                    break;
            }
        }
        if (str_len == 0) return null;
        str_len++;
        if (current_hist) {
            current_hist = false;
            l_history.removeElementAt(0);
        }
        String sLine = StringHelper.toString(line);
        l_history.insertElementAt(sLine, 0);
        it_history = 0;
        if (l_completion.size() != 0) l_completion.clear();
        return sLine;
    }

    void ShowPrompt() {
        char drive = (char) (Dos_files.DOS_GetDefaultDrive() + 'A');
        StringRef dir = new StringRef();
        Dos_files.DOS_GetCurrentDir((short) 0, dir);
        WriteOut(String.valueOf((char) drive) + ":\\" + dir.value + ">");
    }

    void DoCommand(String line) {
        line = line.trim();
        StringBuffer cmd_buffer = new StringBuffer();
        while (line.length() > 0) {
            if (line.charAt(0) == 32) break;
            if (line.charAt(0) == '/') break;
            if (line.charAt(0) == '\t') break;
            if (line.charAt(0) == '=') break;
            if ((line.charAt(0) == '.') || (line.charAt(0) == '\\')) {
                int cmd_index = 0;
                while (cmd_list[cmd_index].name != null) {
                    if (cmd_buffer.toString().equalsIgnoreCase(cmd_list[cmd_index].name)) {
                        cmd_list[cmd_index].handler.call(line);
                        return;
                    }
                    cmd_index++;
                }
            }
            cmd_buffer.append(line.charAt(0));
            line = line.substring(1);
        }
        if (cmd_buffer.length() == 0) return;
        int cmd_index = 0;
        while (cmd_list[cmd_index].name != null) {
            if (cmd_buffer.toString().equalsIgnoreCase(cmd_list[cmd_index].name)) {
                cmd_list[cmd_index].handler.call(line);
                return;
            }
            cmd_index++;
        }
        if (Execute(cmd_buffer.toString(), line)) return;
        if (CheckConfig(cmd_buffer.toString(), line)) return;
        WriteOut(Msg.get("SHELL_EXECUTE_ILLEGAL_COMMAND"), new Object[] { cmd_buffer.toString() });
    }

    public static String full_arguments;

    public boolean Execute(String name, String args) {
        String fullname;
        String p_fullname;
        String line = args;
        if (args.length() != 0) {
            if (args.charAt(0) != ' ') {
                line = ' ' + line;
            }
        }
        if ((name.substring(1).equals(":") || name.substring(1).equals(":\\")) && StringHelper.isalpha(name.charAt(0))) {
            if (!Dos_files.DOS_SetDrive((short) (name.toUpperCase().charAt(0) - 'A'))) {
                WriteOut(Msg.get("SHELL_EXECUTE_DRIVE_NOT_FOUND"), new Object[] { new Character(name.toUpperCase().charAt(0)) });
            }
            return true;
        }
        p_fullname = Which(name);
        if (p_fullname == null) return false;
        fullname = p_fullname;
        int extension = fullname.lastIndexOf('.');
        String sExtension = "";
        if (extension >= 0) {
            sExtension = fullname.substring(extension).toLowerCase();
        } else {
            String temp_name = fullname + ".COM";
            String temp_fullname = Which(temp_name);
            if (temp_fullname != null) {
                sExtension = ".com";
                fullname = temp_fullname;
            } else {
                temp_name = fullname + ".EXE";
                temp_fullname = Which(temp_name);
                temp_fullname = Which(temp_name);
                if (temp_fullname != null) {
                    sExtension = ".exe";
                    fullname = temp_fullname;
                } else {
                    temp_name = fullname + ".BAT";
                    temp_fullname = Which(temp_name);
                    temp_fullname = Which(temp_name);
                    if (temp_fullname != null) {
                        sExtension = ".bat";
                        fullname = temp_fullname;
                    } else {
                        return false;
                    }
                }
            }
        }
        if (sExtension.equalsIgnoreCase(".bat")) {
            boolean temp_echo = echo;
            if (bf != null && !call) bf.close();
            bf = new BatchFile(this, fullname, name, line);
            echo = temp_echo;
        } else {
            if (!sExtension.equalsIgnoreCase(".com") && !sExtension.equalsIgnoreCase(".exe")) return false;
            CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word() - 0x200);
            Dos_ParamBlock block = new Dos_ParamBlock(CPU.Segs_SSphys + CPU_Regs.reg_esp.word());
            block.Clear();
            int file_name = CPU_Regs.RealMakeSegSS(CPU_Regs.reg_esp.word() + 0x20);
            Memory.MEM_BlockWrite(Memory.Real2Phys(file_name), fullname, fullname.length() + 1);
            full_arguments = line;
            byte[] cmdtail = new byte[128];
            cmdtail[0] = (byte) line.length();
            StringHelper.strcpy(cmdtail, 1, line);
            cmdtail[line.length() + 1] = 0xd;
            Memory.MEM_BlockWrite(CPU.Segs_SSphys + CPU_Regs.reg_esp.word() + 0x100, cmdtail, 128);
            ShortRef add = new ShortRef(0);
            String tailBuffer = StringHelper.toString(cmdtail, 1, cmdtail.length - 1);
            Dos_files.FCB_Parsename(Dos.dos.psp(), 0x5C, (short) 0x00, tailBuffer, add);
            Dos_files.FCB_Parsename(Dos.dos.psp(), 0x6C, (short) 0x00, tailBuffer.substring(add.value), add);
            block.exec.fcb1 = Memory.RealMake(Dos.dos.psp(), 0x5C);
            block.exec.fcb2 = Memory.RealMake(Dos.dos.psp(), 0x6C);
            block.exec.cmdtail = CPU_Regs.RealMakeSegSS(CPU_Regs.reg_esp.word() + 0x100);
            block.SaveData();
            CPU_Regs.reg_eax.word(0x4b00);
            CPU_Regs.SegSet16DS((int) CPU.Segs_SSval);
            CPU_Regs.reg_edx.word(Memory.RealOff(file_name));
            CPU_Regs.SegSet16ES((int) CPU.Segs_SSval);
            CPU_Regs.reg_ebx.word(CPU_Regs.reg_esp.word());
            CPU_Regs.SETFLAGBIT(CPU_Regs.IF, false);
            Callback.CALLBACK_RunRealInt(0x21);
            CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word() + 0x200);
        }
        return true;
    }

    boolean CheckConfig(String cmd_in, String line) {
        Section test = Dosbox.control.GetSectionFromProperty(cmd_in);
        if (test == null) return false;
        if (line != null && line.length() == 0) {
            String val = test.GetPropValue(cmd_in);
            if (!val.equals(Section.NO_SUCH_PROPERTY)) WriteOut(val + "\n");
            return true;
        }
        String newcom = "z:\\config " + test.GetName() + " " + cmd_in + line;
        DoCommand(newcom);
        return true;
    }

    String Which(String name) {
        if (Dos_files.DOS_FileExists(name)) return name;
        if (Dos_files.DOS_FileExists(name + ".COM")) return name + ".COM";
        if (Dos_files.DOS_FileExists(name + ".EXE")) return name + ".EXE";
        if (Dos_files.DOS_FileExists(name + ".BAT")) return name + ".BAT";
        StringRef temp = new StringRef();
        if (!GetEnvStr("PATH", temp)) return null;
        if (temp.value.length() == 0) return null;
        int pos = temp.value.indexOf('=');
        if (pos < 0) return null;
        String pathenv = temp.value.substring(pos + 1);
        while (pathenv.length() > 0) {
            while (pathenv.length() > 0 && pathenv.charAt(0) == ';') pathenv = pathenv.substring(1);
            StringBuffer path = new StringBuffer();
            while (pathenv.length() > 0 && pathenv.charAt(0) != ';') {
                path.append(pathenv.charAt(0));
                pathenv = pathenv.substring(1);
            }
            if (path.length() > 0) {
                if (path.charAt(path.length() - 1) != '\\') path.append("\\");
                path.append(name);
                String p = path.toString();
                if (Dos_files.DOS_FileExists(p)) return p;
                if (Dos_files.DOS_FileExists(p + ".COM")) return p + ".COM";
                if (Dos_files.DOS_FileExists(p + ".EXE")) return p + ".EXE";
                if (Dos_files.DOS_FileExists(p + ".BAT")) return p + ".BAT";
            }
        }
        return null;
    }

    private boolean HELP(StringRef args, String command) {
        if (ScanCMDBool(args, "?")) {
            WriteOut(Msg.get("SHELL_CMD_" + command + "_HELP"));
            String long_m = Msg.get("SHELL_CMD_" + command + "_HELP_LONG");
            WriteOut("\n");
            if (!long_m.equals("Message not Found!\n")) WriteOut(long_m); else WriteOut(command + "\n");
            return true;
        }
        return false;
    }

    private static String StripSpaces(String args) {
        while (args.length() > 0 && StringHelper.isspace(args.charAt(0))) {
            args = args.substring(1);
        }
        return args;
    }

    private static String StripSpaces(String args, char also) {
        while (args.length() > 0 && (StringHelper.isspace(args.charAt(0)) || args.charAt(0) == also)) {
            args = args.substring(1);
        }
        return args;
    }

    private static String StripWord(StringRef line) {
        String scan = line.value;
        scan = scan.trim();
        if (scan.startsWith("\"")) {
            int end_quote = scan.indexOf('"', 1);
            if (end_quote >= 0) {
                line.value = scan.substring(end_quote + 1).trim();
                return scan.substring(1, end_quote);
            }
        }
        for (int i = 0; i < scan.length(); i++) {
            if (StringHelper.isspace(scan.charAt(i))) {
                line.value = scan.substring(i).trim();
                return scan.substring(0, i);
            }
        }
        line.value = "";
        return scan;
    }

    static String FormatNumber(long num) {
        long numm, numk, numb, numg;
        numb = num % 1000;
        num /= 1000;
        numk = num % 1000;
        num /= 1000;
        numm = num % 1000;
        num /= 1000;
        numg = num;
        if (numg != 0) {
            return StringHelper.sprintf("%d,%03d,%03d,%03d", new Object[] { new Long(numg), new Long(numm), new Long(numk), new Long(numb) });
        }
        if (numm != 0) {
            return StringHelper.sprintf("%d,%03d,%03d", new Object[] { new Long(numm), new Long(numk), new Long(numb) });
        }
        if (numk != 0) {
            return StringHelper.sprintf("%d,%03d", new Object[] { new Long(numk), new Long(numb) });
        }
        return String.valueOf(numb);
    }

    private static String ExpandDot(StringRef args) {
        if (args.value.startsWith(".")) {
            if (args.value.length() == 1) {
                return "*.*";
            }
            if (args.value.charAt(1) != '.' && args.value.charAt(1) != '\\') {
                return "*" + args.value;
            }
        }
        return args.value;
    }

    private static boolean ScanCMDBool(StringRef cmd, String check) {
        int pos = 0;
        check = "/" + check;
        while ((pos = cmd.value.toUpperCase().indexOf(check, pos)) >= 0) {
            int start = pos;
            pos += check.length();
            if (cmd.value.length() == pos || cmd.value.charAt(pos) == ' ' || cmd.value.charAt(pos) == '\t' || cmd.value.charAt(pos) == '/') {
                cmd.value = cmd.value.substring(0, start) + cmd.value.substring(pos).trim();
                return true;
            }
        }
        return false;
    }

    private static String ScanCMDRemain(StringRef cmd) {
        int pos = cmd.value.indexOf('/');
        if (pos >= 0) {
            String scan = cmd.value.substring(pos + 1);
            StringBuffer found = new StringBuffer();
            while (scan.length() > 0 && !StringHelper.isspace(scan.charAt(0))) {
                found.append(scan.charAt(0));
                scan = scan.substring(1);
            }
            return found.toString();
        } else return null;
    }

    handler CMD_HELP = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "HELP")) return;
            boolean optall = ScanCMDBool(args, "ALL");
            if (!optall) WriteOut(Msg.get("SHELL_CMD_HELP"));
            int cmd_index = 0, write_count = 0;
            while (cmd_list[cmd_index].name != null) {
                if (optall || cmd_list[cmd_index].flags == 0) {
                    WriteOut("<\033[34;1m" + StringHelper.leftJustify(cmd_list[cmd_index].name, 8) + "\033[0m> " + Msg.get(cmd_list[cmd_index].help));
                    if ((++write_count % 22) == 0) CMD_PAUSE.call("");
                }
                cmd_index++;
            }
        }
    };

    handler CMD_CLS = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CLS")) return;
            CPU_Regs.reg_eax.word(0x0003);
            Callback.CALLBACK_RunRealInt(0x10);
        }
    };

    private static class copysource {

        String filename = "";

        boolean concat = false;

        copysource(String filein, boolean concatin) {
            filename = filein;
            concat = concatin;
        }

        copysource() {
        }
    }

    handler CMD_COPY = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "COPY")) return;
            final String defaulttarget = ".";
            args.value = StripSpaces(args.value);
            int save_dta = Dos.dos.dta();
            Dos.dos.dta((int) Dos.dos.tables.tempdta);
            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());
            LongRef size = new LongRef(0);
            IntRef date = new IntRef(0);
            IntRef time = new IntRef(0);
            ShortRef attr = new ShortRef();
            StringRef name = new StringRef();
            Vector sources = new Vector();
            while (ScanCMDBool(args, "B")) ;
            while (ScanCMDBool(args, "T")) ;
            while (ScanCMDBool(args, "A")) ;
            ScanCMDBool(args, "Y");
            ScanCMDBool(args, "-Y");
            String rem = ScanCMDRemain(args);
            if (rem != null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"), new Object[] { rem });
                Dos.dos.dta(save_dta);
                return;
            }
            String source_p;
            while ((source_p = StripWord(args)) != null && source_p.length() > 0) {
                do {
                    int plus = source_p.indexOf('+');
                    String source_x;
                    if (plus >= 0) {
                        source_x = source_p.substring(0, plus);
                    } else {
                        source_x = source_p;
                    }
                    boolean has_drive_spec = false;
                    int source_x_len = source_x.length();
                    if (source_x_len > 0) {
                        if (source_x.charAt(source_x_len - 1) == ':') has_drive_spec = true;
                    }
                    if (!has_drive_spec) {
                        if (Dos_files.DOS_FindFirst(source_x, 0xffff & ~Dos_system.DOS_ATTR_VOLUME)) {
                            dta.GetResult(name, size, date, time, attr);
                            if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY) != 0 && source_p.indexOf("*.*") < 0) source_x += "\\*.*";
                        }
                    }
                    sources.add(new copysource(source_x, (plus >= 0) ? true : false));
                    if (plus >= 0) {
                        source_p = source_p.substring(plus + 1);
                    } else {
                        source_p = "";
                    }
                } while (source_p.length() > 0);
            }
            if (sources.size() == 0 || ((copysource) sources.elementAt(0)).filename.length() == 0) {
                WriteOut(Msg.get("SHELL_MISSING_PARAMETER"));
                Dos.dos.dta(save_dta);
                return;
            }
            copysource target = new copysource();
            if (sources.size() > 1 && !((copysource) sources.elementAt(sources.size() - 2)).concat) {
                target = (copysource) sources.lastElement();
                sources.removeElementAt(sources.size() - 1);
            }
            if (target.filename.length() == 0) target = new copysource(defaulttarget, true);
            copysource oldsource = new copysource();
            copysource source = new copysource();
            int count = 0;
            while (sources.size() > 0) {
                oldsource = source;
                source = (copysource) sources.firstElement();
                sources.remove(0);
                if (!oldsource.concat && source.concat && target.concat) {
                    target = source;
                    continue;
                }
                StringRef pathSource = new StringRef();
                StringRef pathTarget = new StringRef();
                if (!Dos_files.DOS_Canonicalize(source.filename, pathSource)) {
                    WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                    Dos.dos.dta(save_dta);
                    return;
                }
                int pos = pathSource.value.lastIndexOf('\\');
                if (pos >= 0) pathSource.value = pathSource.value.substring(0, pos + 1);
                if (!Dos_files.DOS_Canonicalize(target.filename, pathTarget)) {
                    WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                    Dos.dos.dta(save_dta);
                    return;
                }
                int temp = pathTarget.value.indexOf("*.*");
                if (temp >= 0) pathTarget.value = pathTarget.value.substring(0, temp);
                if (pathTarget.value.charAt(pathTarget.value.length() - 1) != '\\') {
                    if (Dos_files.DOS_FindFirst(pathTarget.value, 0xffff & ~Dos_system.DOS_ATTR_VOLUME)) {
                        dta.GetResult(name, size, date, time, attr);
                        if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY) != 0) pathTarget.value += "\\";
                    }
                }
                boolean ret = Dos_files.DOS_FindFirst(source.filename, 0xffff & ~Dos_system.DOS_ATTR_VOLUME);
                if (!ret) {
                    WriteOut(Msg.get("SHELL_CMD_FILE_NOT_FOUND"), new Object[] { source.filename });
                    Dos.dos.dta(save_dta);
                    return;
                }
                String nameTarget;
                String nameSource;
                while (ret) {
                    dta.GetResult(name, size, date, time, attr);
                    if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY) == 0) {
                        nameSource = pathSource.value;
                        nameSource += name.value;
                        IntRef sourceHandle = new IntRef(0), targetHandle = new IntRef(0);
                        if (Dos_files.DOS_OpenFile(nameSource, 0, sourceHandle)) {
                            nameTarget = pathTarget.value;
                            if (nameTarget.charAt(nameTarget.length() - 1) == '\\') nameTarget += name.value;
                            if (oldsource.concat || Dos_files.DOS_CreateFile(nameTarget, 0, targetHandle)) {
                                LongRef dummy = new LongRef(0);
                                if (!oldsource.concat || (Dos_files.DOS_OpenFile(nameTarget, Dos_files.OPEN_READWRITE, targetHandle) && Dos_files.DOS_SeekFile(targetHandle.value, dummy, Dos_files.DOS_SEEK_END))) {
                                    final byte[] buffer = new byte[0x8000];
                                    boolean failed = false;
                                    IntRef toread = new IntRef(0x8000);
                                    do {
                                        failed |= Dos_files.DOS_ReadFile(sourceHandle.value, buffer, toread);
                                        failed |= Dos_files.DOS_WriteFile(targetHandle.value, buffer, toread);
                                    } while (toread.value == 0x8000);
                                    failed |= Dos_files.DOS_CloseFile(sourceHandle.value);
                                    failed |= Dos_files.DOS_CloseFile(targetHandle.value);
                                    WriteOut(" " + name.value + "\n");
                                    if (!source.concat) count++;
                                } else {
                                    Dos_files.DOS_CloseFile(sourceHandle.value);
                                    WriteOut(Msg.get("SHELL_CMD_COPY_FAILURE"), new Object[] { target.filename });
                                }
                            } else {
                                Dos_files.DOS_CloseFile(sourceHandle.value);
                                if (targetHandle.value != 0) Dos_files.DOS_CloseFile(targetHandle.value);
                                WriteOut(Msg.get("SHELL_CMD_COPY_FAILURE"), new Object[] { target.filename });
                            }
                        } else WriteOut(Msg.get("SHELL_CMD_COPY_FAILURE"), new Object[] { source.filename });
                    }
                    ret = Dos_files.DOS_FindNext();
                }
            }
            WriteOut(Msg.get("SHELL_CMD_COPY_SUCCESS"), new Object[] { new Integer(count) });
            Dos.dos.dta(save_dta);
        }
    };

    handler CMD_DIR = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "DIR")) return;
            StringRef line = new StringRef();
            if (GetEnvStr("DIRCMD", line)) {
                int idx = line.value.indexOf('=');
                if (idx >= 0) {
                    args.value += " " + line.value.substring(idx + 1);
                }
            }
            boolean optW = ScanCMDBool(args, "W");
            ScanCMDBool(args, "S");
            boolean optP = ScanCMDBool(args, "P");
            if (ScanCMDBool(args, "WP") || ScanCMDBool(args, "PW")) {
                optW = optP = true;
            }
            boolean optB = ScanCMDBool(args, "B");
            boolean optAD = ScanCMDBool(args, "AD");
            String rem = ScanCMDRemain(args);
            if (rem != null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"), new Object[] { rem });
                return;
            }
            long byte_count, file_count, dir_count;
            int w_count = 0;
            int p_count = 0;
            int w_size = optW ? 5 : 1;
            byte_count = file_count = dir_count = 0;
            args.value = args.value.trim();
            if (args.value.length() == 0) {
                args.value = "*.*";
            } else {
                if (args.value.endsWith("\\") || args.value.endsWith(":")) {
                    args.value += "*.*";
                }
            }
            args.value = ExpandDot(args);
            if (args.value.indexOf("*") < 0 && args.value.indexOf("?") < 0) {
                IntRef attribute = new IntRef(0);
                if (Dos_files.DOS_GetFileAttr(args.value, attribute) && (attribute.value & Dos_system.DOS_ATTR_DIRECTORY) != 0) {
                    args.value += "\\*.*";
                }
            }
            if (args.value.indexOf('.') < 0) {
                args.value += ".*";
            }
            StringRef path = new StringRef();
            if (!Dos_files.DOS_Canonicalize(args.value, path)) {
                WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                return;
            }
            path.value = path.value.substring(0, path.value.lastIndexOf('\\') + 1);
            if (!optB) WriteOut(Msg.get("SHELL_CMD_DIR_INTRO"), new Object[] { path.value });
            int save_dta = Dos.dos.dta();
            Dos.dos.dta((int) Dos.dos.tables.tempdta);
            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());
            boolean ret = Dos_files.DOS_FindFirst(args.value, 0xffff & ~Dos_system.DOS_ATTR_VOLUME);
            if (!ret) {
                if (!optB) WriteOut(Msg.get("SHELL_CMD_FILE_NOT_FOUND"), new Object[] { args.value });
                Dos.dos.dta(save_dta);
                return;
            }
            do {
                StringRef name = new StringRef();
                LongRef size = new LongRef(0);
                IntRef date = new IntRef(0);
                IntRef time = new IntRef(0);
                ShortRef attr = new ShortRef(0);
                dta.GetResult(name, size, date, time, attr);
                if (optAD && (attr.value & Dos_system.DOS_ATTR_DIRECTORY) == 0) continue;
                if (optB) {
                    if (!name.equals(".") && !name.equals("..")) {
                        WriteOut(name + "\n");
                    }
                } else {
                    String ext = "";
                    if (!optW && !name.value.startsWith(".")) {
                        int pos = name.value.lastIndexOf('.');
                        if (pos >= 0) {
                            ext = name.value.substring(pos + 1);
                            name.value = name.value.substring(0, pos);
                        }
                    }
                    short day = (short) (date.value & 0x001f);
                    short month = (short) ((date.value >> 5) & 0x000f);
                    int year = (int) ((date.value >> 9) + 1980);
                    short hour = (short) ((time.value >> 5) >> 6);
                    short minute = (short) ((time.value >> 5) & 0x003f);
                    if ((attr.value & Dos_system.DOS_ATTR_DIRECTORY) != 0) {
                        if (optW) {
                            WriteOut("[" + name.value + "]");
                            int namelen = name.value.length();
                            if (namelen <= 14) {
                                for (int i = 14 - namelen; i > 0; i--) WriteOut(" ");
                            }
                        } else {
                            WriteOut("%-8s %-3s   %-16s %02d-%02d-%04d %2d:%02d\n", new Object[] { name.value, ext, "<DIR>", new Integer(day), new Integer(month), new Integer(year), new Integer(hour), new Integer(minute) });
                        }
                        dir_count++;
                    } else {
                        if (optW) {
                            WriteOut("%-16s", new Object[] { name.value });
                        } else {
                            String numformat = FormatNumber(size.value);
                            WriteOut("%-8s %-3s   %16s %02d-%02d-%04d %2d:%02d\n", new Object[] { name.value, ext, numformat, new Integer(day), new Integer(month), new Integer(year), new Integer(hour), new Integer(minute) });
                        }
                        file_count++;
                        byte_count += size.value;
                    }
                    if (optW) {
                        w_count++;
                    }
                }
                if (optP && (++p_count % (22 * w_size)) == 0) {
                    CMD_PAUSE.call("");
                }
            } while ((ret = Dos_files.DOS_FindNext()));
            if (optW) {
                if ((w_count % 5) != 0) WriteOut("\n");
            }
            if (!optB) {
                String numformat = FormatNumber(byte_count);
                WriteOut(Msg.get("SHELL_CMD_DIR_BYTES_USED"), new Object[] { new Long(file_count), numformat });
                short drive = dta.GetSearchDrive();
                int free_space = 1024 * 1024 * 100;
                if (Dos_files.Drives[drive] != null) {
                    IntRef bytes_sector = new IntRef(0);
                    ShortRef sectors_cluster = new ShortRef();
                    IntRef total_clusters = new IntRef(0);
                    IntRef free_clusters = new IntRef(0);
                    Dos_files.Drives[drive].AllocationInfo(bytes_sector, sectors_cluster, total_clusters, free_clusters);
                    free_space = bytes_sector.value * sectors_cluster.value * free_clusters.value;
                }
                numformat = FormatNumber(free_space);
                WriteOut(Msg.get("SHELL_CMD_DIR_BYTES_FREE"), new Object[] { new Long(dir_count), numformat });
            }
            Dos.dos.dta(save_dta);
        }
    };

    handler CMD_DELETE = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "DELETE")) return;
            int save_dta = Dos.dos.dta();
            Dos.dos.dta((int) Dos.dos.tables.tempdta);
            String rem = ScanCMDRemain(args);
            if (rem != null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"), new Object[] { rem });
                return;
            }
            StringRef full = new StringRef();
            args.value = ExpandDot(args);
            args.value = StripSpaces(args.value);
            if (!Dos_files.DOS_Canonicalize(args.value, full)) {
                WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                return;
            }
            boolean res = Dos_files.DOS_FindFirst(args.value, 0xffff & ~Dos_system.DOS_ATTR_VOLUME);
            if (!res) {
                WriteOut(Msg.get("SHELL_CMD_DEL_ERROR"), new Object[] { args });
                Dos.dos.dta(save_dta);
                return;
            }
            String path = full.value.substring(0, full.value.lastIndexOf("\\"));
            StringRef name = new StringRef();
            LongRef size = new LongRef(0);
            IntRef time = new IntRef(0), date = new IntRef(0);
            ShortRef attr = new ShortRef(0);
            Dos_DTA dta = new Dos_DTA(Dos.dos.dta());
            while (res) {
                dta.GetResult(name, size, date, time, attr);
                if ((attr.value & (Dos_system.DOS_ATTR_DIRECTORY | Dos_system.DOS_ATTR_READ_ONLY)) == 0) {
                    if (!Dos_files.DOS_UnlinkFile(path + name.value)) WriteOut(Msg.get("SHELL_CMD_DEL_ERROR"), new Object[] { full.value });
                }
                res = Dos_files.DOS_FindNext();
            }
            Dos.dos.dta(save_dta);
        }
    };

    handler CMD_ECHO = new handler() {

        public void call(String args) {
            if (args.length() == 0) {
                if (echo) {
                    WriteOut(Msg.get("SHELL_CMD_ECHO_ON"));
                } else {
                    WriteOut(Msg.get("SHELL_CMD_ECHO_OFF"));
                }
                return;
            }
            String cmd = StripSpaces(args);
            if (cmd.equalsIgnoreCase("OFF")) {
                echo = false;
                return;
            }
            if (cmd.equalsIgnoreCase("ON")) {
                echo = true;
                return;
            }
            StringRef a = new StringRef(cmd);
            if (cmd.equalsIgnoreCase("/?")) {
                if (HELP(a, "ECHO")) return;
            }
            args = args.substring(1);
            if (args.endsWith("\r")) {
                Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_WARN, "Hu ? carriage return allready present. Is this possible?");
                WriteOut(args + "\n");
            } else WriteOut(args + "\r\n");
        }
    };

    handler CMD_EXIT = new handler() {

        public void call(String args) {
            StringRef a = new StringRef(args);
            if (HELP(a, "EXIT")) return;
            exit = true;
        }
    };

    handler CMD_MKDIR = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "MKDIR")) return;
            args.value = StripSpaces(args.value);
            String rem = ScanCMDRemain(args);
            if (rem != null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"), new Object[] { rem });
                return;
            }
            if (!Dos_files.DOS_MakeDir(args.value)) {
                WriteOut(Msg.get("SHELL_CMD_MKDIR_ERROR"), new Object[] { args });
            }
        }
    };

    handler CMD_CHDIR = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CHDIR")) return;
            args.value = StripSpaces(args.value);
            if (args.value.length() == 0) {
                char drive = (char) (Dos_files.DOS_GetDefaultDrive() + 'A');
                StringRef dir = new StringRef();
                Dos_files.DOS_GetCurrentDir((short) 0, dir);
                WriteOut(String.valueOf(drive) + ":\\" + dir + "\n");
            } else if (args.value.length() == 2 && args.value.charAt(1) == ':') {
                WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT"), new Object[] { args.value.toUpperCase() });
            } else if (!Dos_files.DOS_ChangeDir(args.value)) {
                String temps = args.value.toUpperCase();
                temps = StringHelper.replace(temps, "/", "\\");
                String[] slash = StringHelper.split(temps, "\\");
                StringBuffer shortversion = new StringBuffer();
                boolean space = false;
                boolean toolong = false;
                for (int i = 0; i < slash.length; i++) {
                    if (slash[i].indexOf(' ') >= 0) space = true;
                    if (slash[i].length() > 8) toolong = true;
                    String s = slash[i];
                    s = StringHelper.replace(s, " ", "");
                    s = StringHelper.replace(s, ".", "");
                    s = StringHelper.replace(s, "\"", "");
                    if (s.length() > 6) s = s.substring(0, 6) + "~1";
                    if (i > 0) shortversion.append("\\");
                    shortversion.append(s);
                }
                if (space) {
                    WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT_2"), new Object[] { shortversion.toString() });
                } else if (toolong) {
                    WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT_2"), new Object[] { shortversion.toString() });
                } else {
                    char drive = (char) (Dos_files.DOS_GetDefaultDrive() + 'A');
                    if (drive == 'Z') {
                        WriteOut(Msg.get("SHELL_CMD_CHDIR_HINT_3"));
                    } else {
                        WriteOut(Msg.get("SHELL_CMD_CHDIR_ERROR"), new Object[] { args.value });
                    }
                }
            }
        }
    };

    handler CMD_RMDIR = new handler() {

        public void call(String ar) {
            StringRef args = new StringRef(ar);
            if (HELP(args, "RMDIR")) return;
            args.value = StripSpaces(args.value);
            String rem = ScanCMDRemain(args);
            if (rem != null) {
                WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"), new Object[] { rem });
                return;
            }
            if (!Dos_files.DOS_RemoveDir(args.value)) {
                WriteOut(Msg.get("SHELL_CMD_RMDIR_ERROR"), new Object[] { args });
            }
        }
    };

    handler CMD_SET = new handler() {

        public void call(String ar) {
            StringRef args = new StringRef(ar);
            if (HELP(args, "SET")) return;
            args.value = StripSpaces(args.value);
            StringRef line = new StringRef();
            if (args.value.length() == 0) {
                int count = GetEnvCount();
                for (int a = 0; a < count; a++) {
                    if (GetEnvNum(a, line)) WriteOut(line.value + "\n");
                }
                return;
            }
            int p = args.value.indexOf("=");
            if (p < 0) {
                if (!GetEnvStr(args.value, line)) WriteOut(Msg.get("SHELL_CMD_SET_NOT_SET"), new Object[] { args.value });
                WriteOut(line.value + "\n");
            } else {
                String key = args.value.substring(0, p);
                p++;
                StringBuffer parsed = new StringBuffer();
                while (p < args.value.length()) {
                    char c = args.value.charAt(p);
                    if (c != '%') {
                        parsed.append(c);
                        p++;
                    } else if (p + 1 < args.value.length() && args.value.charAt(p + 1) == '%') {
                        parsed.append('%');
                        p += 2;
                    } else {
                        int second = args.value.indexOf('%', p + 1);
                        if (second < 0) continue;
                        StringRef temp = new StringRef();
                        if (GetEnvStr(args.value.substring(p + 1, second), temp)) {
                            int pos = temp.value.indexOf('=');
                            if (pos < 0) continue;
                            parsed.append(temp.value.substring(pos + 1));
                        }
                        p = second + 1;
                    }
                }
                if (!SetEnv(key, parsed.toString())) {
                    WriteOut(Msg.get("SHELL_CMD_SET_OUT_OF_SPACE"));
                }
            }
        }
    };

    handler CMD_IF = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "IF")) return;
            args.value = StripSpaces(args.value, '=');
            boolean has_not = false;
            while (args.value.toUpperCase().startsWith("NOT") && args.value.length() > 3) {
                if (!StringHelper.isspace(args.value.charAt(3)) && args.value.charAt(3) != '=') break;
                args.value = args.value.substring(3);
                args.value = StripSpaces(args.value, '=');
                has_not = !has_not;
            }
            if (args.value.toUpperCase().startsWith("ERRORLEVEL")) {
                args.value = args.value.substring(10);
                args.value = StripSpaces(args.value, '=');
                String word = StripWord(args);
                if (!StringHelper.isdigit(word.charAt(0))) {
                    WriteOut(Msg.get("SHELL_CMD_IF_ERRORLEVEL_MISSING_NUMBER"));
                    return;
                }
                int n = 0;
                do {
                    n = n * 10 + (word.charAt(0) - '0');
                    word = word.substring(1);
                } while (word.length() > 0 && StringHelper.isdigit(word.charAt(0)));
                if (word.length() > 0 && !StringHelper.isspace(word.charAt(0))) {
                    WriteOut(Msg.get("SHELL_CMD_IF_ERRORLEVEL_INVALID_NUMBER"));
                    return;
                }
                if ((Dos.dos.return_code >= n) == (!has_not)) DoCommand(args.value);
                return;
            }
            if (args.value.toUpperCase().startsWith("EXIST ")) {
                args.value = args.value.substring(6);
                args.value = StripSpaces(args.value);
                String word = StripWord(args);
                if (word.length() == 0) {
                    WriteOut(Msg.get("SHELL_CMD_IF_EXIST_MISSING_FILENAME"));
                    return;
                }
                {
                    int save_dta = Dos.dos.dta();
                    Dos.dos.dta((int) Dos.dos.tables.tempdta);
                    boolean ret = Dos_files.DOS_FindFirst(word, 0xffff & ~Dos_system.DOS_ATTR_VOLUME);
                    Dos.dos.dta(save_dta);
                    if (ret == (!has_not)) DoCommand(args.value);
                }
                return;
            }
            String word = "";
            while (args.value.length() > 0 && !StringHelper.isspace(args.value.charAt(0)) && args.value.charAt(0) != '=') {
                word += args.value.substring(0, 1);
                args.value = args.value.substring(1);
            }
            while (args.value.length() > 0 && args.value.charAt(0) != '=') {
                args.value = args.value.substring(1);
            }
            if (args.value.length() < 2 || args.value.charAt(1) != '=') {
                SyntaxError();
                return;
            }
            args.value = args.value.substring(2);
            args.value = StripSpaces(args.value, '=');
            String word2 = "";
            while (args.value.length() > 0 && !StringHelper.isspace(args.value.charAt(0)) && args.value.charAt(0) != '=') {
                word2 += args.value.substring(0, 1);
                args.value = args.value.substring(1);
            }
            if (args.value.length() > 0) {
                args.value = StripSpaces(args.value, '=');
                if (word.equals(word2) == (!has_not)) DoCommand(args.value);
            }
        }
    };

    handler CMD_GOTO = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "GOTO")) return;
            args.value = StripSpaces(args.value);
            if (bf == null) return;
            if (args.value.length() > 0 && (args.value.charAt(0) == ':')) args.value = args.value.substring(1);
            for (int i = 0; i < args.value.length(); i++) {
                if (args.value.charAt(0) == ' ' || args.value.charAt(0) == '\t') {
                    args.value = args.value.substring(0, i);
                    break;
                }
            }
            if (args.value.length() == 0) {
                WriteOut(Msg.get("SHELL_CMD_GOTO_MISSING_LABEL"));
                return;
            }
            if (!bf.Goto(args.value)) {
                WriteOut(Msg.get("SHELL_CMD_GOTO_LABEL_NOT_FOUND"), new Object[] { args });
                return;
            }
        }
    };

    handler CMD_TYPE = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "TYPE")) return;
            args.value = StripSpaces(args.value);
            if (args.value.length() == 0) {
                WriteOut(Msg.get("SHELL_SYNTAXERROR"));
                return;
            }
            IntRef handle = new IntRef(0);
            while (true) {
                String word = StripWord(args);
                if (!Dos_files.DOS_OpenFile(word, 0, handle)) {
                    WriteOut(Msg.get("SHELL_CMD_FILE_NOT_FOUND"), new Object[] { word });
                    return;
                }
                IntRef n = new IntRef(0);
                byte[] c = new byte[1];
                do {
                    n.value = 1;
                    Dos_files.DOS_ReadFile(handle.value, c, n);
                    Dos_files.DOS_WriteFile(Dos_files.STDOUT, c, n);
                } while (n.value > 0);
                Dos_files.DOS_CloseFile(handle.value);
                if (args.value.length() == 0) break;
            }
        }
    };

    handler CMD_REM = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            HELP(args, "REM");
        }
    };

    handler CMD_RENAME = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "RENAME")) return;
            args.value = StripSpaces(args.value);
            if (args.value.length() == 0) {
                SyntaxError();
                return;
            }
            if (args.value.indexOf('*') >= 0 || args.value.indexOf('?') >= 0) {
                WriteOut(Msg.get("SHELL_CMD_NO_WILD"));
                return;
            }
            String arg1 = StripWord(args);
            int slash = arg1.lastIndexOf('\\');
            if (slash >= 0) {
                String dir_source = arg1.substring(0, slash);
                slash++;
                if (dir_source.length() == 2 && dir_source.charAt(1) == ':') dir_source += "\\";
                StringRef dir_current = new StringRef();
                Dos_files.DOS_GetCurrentDir((short) 0, dir_current);
                if (!Dos_files.DOS_ChangeDir(dir_source)) {
                    WriteOut(Msg.get("SHELL_ILLEGAL_PATH"));
                    return;
                }
                Dos_files.DOS_Rename(arg1.substring(slash), args.value);
                Dos_files.DOS_ChangeDir(dir_current.value);
            } else {
                Dos_files.DOS_Rename(arg1, args.value);
            }
        }
    };

    handler CMD_CALL = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CALL")) return;
            call = true;
            ParseLine(args.value);
            call = false;
        }
    };

    void SyntaxError() {
        WriteOut(Msg.get("SHELL_SYNTAXERROR"));
    }

    handler CMD_PAUSE = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "PAUSE")) return;
            WriteOut(Msg.get("SHELL_CMD_PAUSE"));
            byte[] c = new byte[1];
            IntRef n = new IntRef(1);
            Dos_files.DOS_ReadFile(Dos_files.STDIN, c, n);
        }
    };

    handler CMD_SUBST = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "SUBST")) return;
            String mountstring;
            mountstring = "MOUNT ";
            args.value = StripSpaces(args.value);
            String arg;
            CommandLine command = new CommandLine(null, args.value);
            if (command.GetCount() != 2) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            arg = command.FindCommand(1);
            if ((arg.length() > 1) && arg.charAt(1) != ':') {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            arg = command.FindCommand(2);
            String temp_str = args.value.substring(0, 1).toUpperCase();
            if (arg.toUpperCase().equals("/D")) {
                if (Dos_files.Drives[temp_str.charAt(0) - 'A'] == null) {
                    WriteOut(Msg.get("SHELL_CMD_SUBST_NO_REMOVE"));
                    return;
                }
                mountstring += "-u ";
                mountstring += temp_str;
                ParseLine(mountstring);
                return;
            }
            if (Dos_files.Drives[temp_str.charAt(0) - 'A'] != null) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            mountstring += temp_str;
            mountstring += " ";
            ShortRef drive = new ShortRef(0);
            StringRef fulldir = new StringRef();
            if (!Dos_files.DOS_MakeName(arg, fulldir, drive)) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            if (!(Dos_files.Drives[drive.value] instanceof Drive_local)) {
                WriteOut(Msg.get("SHELL_CMD_SUBST_FAILURE"));
                return;
            }
            Drive_local ldp = (Drive_local) Dos_files.Drives[drive.value];
            StringRef newname = new StringRef(ldp.basedir);
            newname.value += fulldir.value;
            ldp.dirCache.ExpandName(newname);
            mountstring += "\"";
            mountstring += newname.value;
            mountstring += "\"";
            ParseLine(mountstring);
        }
    };

    handler CMD_LOADHIGH = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "LOADHIGH")) return;
            int umb_start = Dos.dos_infoblock.GetStartOfUMBChain();
            short umb_flag = Dos.dos_infoblock.GetUMBChainState();
            short old_memstrat = (short) (Dos_memory.DOS_GetMemAllocStrategy() & 0xff);
            if (umb_start == 0x9fff) {
                if ((umb_flag & 1) == 0) Dos_memory.DOS_LinkUMBsToMemChain(1);
                Dos_memory.DOS_SetMemAllocStrategy(0x80);
                ParseLine(args.value);
                short current_umb_flag = Dos.dos_infoblock.GetUMBChainState();
                if ((current_umb_flag & 1) != (umb_flag & 1)) Dos_memory.DOS_LinkUMBsToMemChain(umb_flag);
                Dos_memory.DOS_SetMemAllocStrategy(old_memstrat);
            } else ParseLine(args.value);
        }
    };

    private static class DefaultChoice extends Thread {

        int timeout = 0;

        byte[] choice;

        Object mutex = new Object();

        public void run() {
            synchronized (mutex) {
                try {
                    mutex.wait(timeout * 1000);
                    Bios_keyboard.BIOS_AddKeyToBuffer(choice[0]);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    handler CMD_CHOICE = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "CHOICE")) return;
            final String defchoice = "yn";
            String rem = null;
            boolean optN = ScanCMDBool(args, "N");
            boolean optS = ScanCMDBool(args, "S");
            boolean timeout = false;
            String timeoutChoice = "";
            int timeoutTime = -1;
            if (args.value.indexOf("/T") >= 0) {
                int pos1 = args.value.indexOf("/T");
                int pos2 = args.value.indexOf(" ", pos1);
                String command = args.value.substring(pos1 + 2, pos2);
                args.value = args.value.substring(0, pos1) + args.value.substring(pos2 + 1);
                if (command.startsWith(":")) {
                    command = command.substring(1);
                }
                int pos3 = command.indexOf(",");
                if (pos3 >= 0) {
                    timeoutChoice = command.substring(0, pos3);
                    try {
                        timeoutTime = Integer.parseInt(command.substring(pos3 + 1));
                        timeout = true;
                    } catch (Exception e) {
                    }
                }
            }
            if (args.value.length() > 0) {
                args.value = StripSpaces(args.value);
                rem = ScanCMDRemain(args);
                if (rem != null && rem.toLowerCase().charAt(0) != 'c') {
                    WriteOut(Msg.get("SHELL_ILLEGAL_SWITCH"), new Object[] { rem });
                    return;
                }
                if (rem != null && args.value.substring(1).startsWith(rem)) args.value = args.value.substring(rem.length() + 1);
                if (rem != null) rem = rem.substring(2);
                if (rem != null && rem.charAt(0) == ':') rem = rem.substring(1);
            }
            if (rem == null || rem.length() == 0) rem = defchoice;
            if (!optS) rem = rem.toUpperCase();
            if (args.value.length() > 0) {
                args.value = StripSpaces(args.value);
                int argslen = args.value.length();
                if (argslen > 1 && args.value.charAt(0) == '"' && args.value.charAt(argslen - 1) == '"') {
                    args.value = args.value.substring(1, argslen - 1);
                }
                WriteOut(args.value);
            }
            if (!optN) {
                if (args.value.length() > 0) WriteOut(" ");
                WriteOut("[");
                int len = rem.length();
                for (int t = 1; t < len; t++) {
                    WriteOut(String.valueOf(rem.charAt(t - 1)) + ",");
                }
                WriteOut(String.valueOf(rem.charAt(len - 1)) + "]?");
            }
            IntRef n = new IntRef(1);
            byte[] c = new byte[1];
            int pos;
            do {
                DefaultChoice defaultChoice = null;
                if (timeout) {
                    defaultChoice = new DefaultChoice();
                    defaultChoice.choice = timeoutChoice.getBytes();
                    defaultChoice.timeout = timeoutTime;
                    defaultChoice.start();
                }
                Dos_files.DOS_ReadFile(Dos_files.STDIN, c, n);
                if (defaultChoice != null) {
                    defaultChoice.interrupt();
                    try {
                        defaultChoice.join(1000);
                    } catch (Exception e) {
                    }
                }
                if (optS) pos = rem.indexOf((char) c[0]); else pos = rem.indexOf(new String(c).toUpperCase());
            } while (pos < 0);
            c = optS ? c : new String(c).toUpperCase().getBytes();
            Dos_files.DOS_WriteFile(Dos_files.STDOUT, c, n);
            Dos.dos.return_code = (short) (pos + 1);
        }
    };

    handler CMD_ATTRIB = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "ATTRIB")) return;
        }
    };

    handler CMD_PATH = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "PATH")) return;
            if (args.value.length() > 0) {
                String pathstring = "set PATH=";
                while (args.value.length() > 0 && (args.value.charAt(0) == '=' || args.value.charAt(0) == ' ')) args.value = args.value.substring(1);
                pathstring += args.value;
                ParseLine(pathstring);
                return;
            } else {
                StringRef line = new StringRef();
                if (GetEnvStr("PATH", line)) {
                    WriteOut(line.value);
                } else {
                    WriteOut("PATH=(null)");
                }
            }
        }
    };

    handler CMD_SHIFT = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "SHIFT")) return;
            if (bf != null) bf.Shift();
        }
    };

    handler CMD_VER = new handler() {

        public void call(String a) {
            StringRef args = new StringRef(a);
            if (HELP(args, "VER")) return;
            if (args.value.length() > 0) {
                String word = StripWord(args);
                if (!word.equalsIgnoreCase("set")) return;
                word = StripWord(args);
                try {
                    Dos.dos.version.major = (byte) Integer.parseInt(word);
                    Dos.dos.version.minor = (byte) Integer.parseInt(args.value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else WriteOut(Msg.get("SHELL_CMD_VER_VER"), new Object[] { Config.VERSION, new Integer(Dos.dos.version.major), new Integer(Dos.dos.version.minor) });
        }
    };

    SHELL_Cmd[] cmd_list = { new SHELL_Cmd("DIR", 0, CMD_DIR, "SHELL_CMD_DIR_HELP"), new SHELL_Cmd("CHDIR", 1, CMD_CHDIR, "SHELL_CMD_CHDIR_HELP"), new SHELL_Cmd("ATTRIB", 1, CMD_ATTRIB, "SHELL_CMD_ATTRIB_HELP"), new SHELL_Cmd("CALL", 1, CMD_CALL, "SHELL_CMD_CALL_HELP"), new SHELL_Cmd("CD", 0, CMD_CHDIR, "SHELL_CMD_CHDIR_HELP"), new SHELL_Cmd("CHOICE", 1, CMD_CHOICE, "SHELL_CMD_CHOICE_HELP"), new SHELL_Cmd("CLS", 0, CMD_CLS, "SHELL_CMD_CLS_HELP"), new SHELL_Cmd("COPY", 0, CMD_COPY, "SHELL_CMD_COPY_HELP"), new SHELL_Cmd("DEL", 0, CMD_DELETE, "SHELL_CMD_DELETE_HELP"), new SHELL_Cmd("DELETE", 1, CMD_DELETE, "SHELL_CMD_DELETE_HELP"), new SHELL_Cmd("ERASE", 1, CMD_DELETE, "SHELL_CMD_DELETE_HELP"), new SHELL_Cmd("ECHO", 1, CMD_ECHO, "SHELL_CMD_ECHO_HELP"), new SHELL_Cmd("EXIT", 0, CMD_EXIT, "SHELL_CMD_EXIT_HELP"), new SHELL_Cmd("GOTO", 1, CMD_GOTO, "SHELL_CMD_GOTO_HELP"), new SHELL_Cmd("HELP", 1, CMD_HELP, "SHELL_CMD_HELP_HELP"), new SHELL_Cmd("IF", 1, CMD_IF, "SHELL_CMD_IF_HELP"), new SHELL_Cmd("LOADHIGH", 1, CMD_LOADHIGH, "SHELL_CMD_LOADHIGH_HELP"), new SHELL_Cmd("LH", 1, CMD_LOADHIGH, "SHELL_CMD_LOADHIGH_HELP"), new SHELL_Cmd("MKDIR", 1, CMD_MKDIR, "SHELL_CMD_MKDIR_HELP"), new SHELL_Cmd("MD", 0, CMD_MKDIR, "SHELL_CMD_MKDIR_HELP"), new SHELL_Cmd("PATH", 1, CMD_PATH, "SHELL_CMD_PATH_HELP"), new SHELL_Cmd("PAUSE", 1, CMD_PAUSE, "SHELL_CMD_PAUSE_HELP"), new SHELL_Cmd("RMDIR", 1, CMD_RMDIR, "SHELL_CMD_RMDIR_HELP"), new SHELL_Cmd("RD", 0, CMD_RMDIR, "SHELL_CMD_RMDIR_HELP"), new SHELL_Cmd("REM", 1, CMD_REM, "SHELL_CMD_REM_HELP"), new SHELL_Cmd("RENAME", 1, CMD_RENAME, "SHELL_CMD_RENAME_HELP"), new SHELL_Cmd("REN", 0, CMD_RENAME, "SHELL_CMD_RENAME_HELP"), new SHELL_Cmd("SET", 1, CMD_SET, "SHELL_CMD_SET_HELP"), new SHELL_Cmd("SHIFT", 1, CMD_SHIFT, "SHELL_CMD_SHIFT_HELP"), new SHELL_Cmd("SUBST", 1, CMD_SUBST, "SHELL_CMD_SUBST_HELP"), new SHELL_Cmd("TYPE", 0, CMD_TYPE, "SHELL_CMD_TYPE_HELP"), new SHELL_Cmd("VER", 0, CMD_VER, "SHELL_CMD_VER_HELP"), new SHELL_Cmd(null, 0, null, null) };
}
