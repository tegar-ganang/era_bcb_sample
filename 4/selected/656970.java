package gdb.breakpoints;

import javax.swing.text.Position;
import gdb.core.CommandManager;
import gdb.core.Debugger;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;

public class Breakpoint {

    String file;

    Position pos = null;

    int line;

    int number;

    View view;

    Buffer buffer;

    BreakpointPainter painter;

    boolean enabled;

    String condition = "";

    int skipCount = 0;

    boolean initialized = false;

    String what;

    String options;

    String when;

    public Breakpoint(String what, boolean read, boolean write) {
        this.file = null;
        this.view = null;
        if (read) {
            if (write) {
                this.options = "-a";
                this.when = "access";
            } else {
                this.options = "-r";
                this.when = "read";
            }
        } else {
            this.options = "";
            this.when = "write";
        }
        this.what = what;
        initialize();
        BreakpointList.getInstance().add(this);
    }

    public Breakpoint(View view, Buffer buffer, int line) {
        this.view = view;
        this.buffer = buffer;
        this.line = line;
        this.file = buffer.getPath();
        this.pos = buffer.createPosition(buffer.getLineStartOffset(line));
        initialize();
        addPainter();
        enabled = true;
        BreakpointList.getInstance().add(this);
    }

    public void reset() {
        initialized = false;
    }

    public void initialize() {
        if (initialized) return;
        CommandManager commandManager = getCommandManager();
        if (commandManager != null) {
            if (file != null) {
                String fileName = file;
                if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") > -1) {
                    fileName = file.replace('\\', '/');
                }
                commandManager.add("-break-insert " + fileName + ":" + getLine(), new BreakpointResultHandler(this));
            } else commandManager.add("-break-watch " + options + " " + what, new BreakpointResultHandler(this));
            initialized = true;
        }
    }

    public boolean isBreakpoint() {
        return (file != null);
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        if (buffer != null && pos != null) return buffer.getLineOfOffset(pos.getOffset());
        return line;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    private void addPainter() {
        painter = new BreakpointPainter(view.getEditPane(), buffer, this);
        view.getTextArea().getGutter().addExtension(painter);
    }

    private void removePainter() {
        if (painter == null) return;
        view.getTextArea().getGutter().removeExtension(painter);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        gdbSetEnabled(false);
    }

    private void gdbSetEnabled(boolean now) {
        CommandManager commandManager = getCommandManager();
        if (commandManager != null) {
            String cmd = null;
            if (enabled) cmd = "-break-enable " + number; else cmd = "-break-disable " + number;
            if (now) commandManager.addNow(cmd); else commandManager.add(cmd);
        }
        if (painter != null) {
            removePainter();
            addPainter();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void remove() {
        BreakpointList.getInstance().remove(this);
        CommandManager commandManager = getCommandManager();
        if (commandManager != null) commandManager.add("-break-delete " + number);
        removePainter();
    }

    public void setNumber(int num) {
        number = num;
        if (condition.length() > 0) gdbSetCondition(true);
        if (skipCount > 0) gdbSetSkipCount(true);
        if (!enabled) {
            gdbSetEnabled(true);
        }
    }

    public int getNumber() {
        return number;
    }

    public void setSkipCount(int count) {
        if (skipCount == count) return;
        skipCount = count;
        gdbSetSkipCount(false);
    }

    private void gdbSetSkipCount(boolean now) {
        CommandManager commandManager = getCommandManager();
        if (commandManager != null) {
            String cmd = "-break-after " + number + " " + skipCount;
            if (now) commandManager.addNow(cmd); else commandManager.add(cmd);
        }
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setCondition(String cond) {
        if (condition.equals(cond)) return;
        condition = cond;
        gdbSetCondition(false);
    }

    private CommandManager getCommandManager() {
        return Debugger.getInstance().getCommandManager();
    }

    private void gdbSetCondition(boolean now) {
        CommandManager commandManager = getCommandManager();
        if (commandManager != null) {
            String cmd = "-break-condition " + number + " " + condition;
            if (now) commandManager.addNow(cmd); else commandManager.add(cmd);
        }
    }

    public String getCondition() {
        return condition;
    }

    public String getWhat() {
        return what;
    }

    public String getWhen() {
        return when;
    }
}
