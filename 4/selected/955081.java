package net.sf.jvibes.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import net.sf.jvibes.JVibes;
import net.sf.jvibes.io.Save;
import net.sf.jvibes.kernel.Calculator;
import net.sf.jvibes.kernel.elements.Model;
import net.sf.jvibes.ui.StatusBar;
import net.sf.jvibes.ui.Task;
import net.sf.jvibes.ui.TaskManager;
import net.sf.jvibes.ui.Modeller;

public class StartSimulationAction extends AbstractAction {

    private static final ImageIcon LARGE_ICON = new ImageIcon(StartSimulationAction.class.getResource("/net/sf/jvibes/ui/actions/StartSimulationActionL.png"));

    private static final ImageIcon SMALL_ICON = new ImageIcon(StartSimulationAction.class.getResource("/net/sf/jvibes/ui/actions/StartSimulationActionS.png"));

    public StartSimulationAction() {
        putValue(Action.LARGE_ICON_KEY, LARGE_ICON);
        putValue(Action.SMALL_ICON, SMALL_ICON);
        putValue(NAME, "Run");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Modeller ws = JVibes.getWorkspace();
        final Model model = ws.getSelectedModel();
        File f = (File) model.getPropertyValue(Model.PROPERTY_FILE);
        if (f == null) f = Save.saveAs(model);
        if (f == null) return;
        File outFile = (File) model.getPropertyValue(Model.PROPERTY_RESULTS_FILE);
        if (outFile == null) {
            String path = f.getAbsolutePath();
            int idx = path.lastIndexOf(".");
            if (idx >= 0) {
                path = path.substring(0, idx);
            }
            path += ".jvr";
            outFile = new File(path);
            if (outFile.exists() && !outFile.canWrite()) return;
        }
        FileWriter writer;
        try {
            writer = new FileWriter(outFile);
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        Calculator calc = new Calculator(model, 10.0, writer.getPipe());
        StatusBar.getInstance().setTask(calc);
        TaskManager.getInstance().addTask(writer);
        TaskManager.getInstance().addTask(calc);
        calc.start();
        writer.start();
    }

    private static final class FileWriter extends Task {

        private Pipe _pipe;

        private ByteBuffer _buffer;

        private FileChannel _out;

        private Pipe.SourceChannel _source;

        public FileWriter(File f) throws IOException {
            _pipe = Pipe.open();
            _out = new FileOutputStream(f).getChannel();
            _source = _pipe.source();
        }

        public Pipe getPipe() {
            return _pipe;
        }

        @Override
        protected boolean init() {
            _buffer = ByteBuffer.allocate(8 * 1024);
            return super.init();
        }

        @Override
        protected boolean loop() {
            if (!_pipe.sink().isOpen()) return false;
            _buffer.clear();
            int bytesRead;
            try {
                bytesRead = _source.read(_buffer);
                if (bytesRead < 0) return false;
                _buffer.flip();
                _out.write(_buffer);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return bytesRead < 0;
        }

        @Override
        protected void done() {
            _buffer.clear();
            try {
                while (_source.read(_buffer) > 0) {
                    _buffer.flip();
                    _out.write(_buffer);
                    _buffer.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.done();
        }
    }
}
