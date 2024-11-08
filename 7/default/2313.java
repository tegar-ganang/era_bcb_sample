import javax.swing.*;
import java.io.IOException;
import java.util.Vector;
import fr.esrf.tangoatk.widget.util.jdraw.JDrawEditor;
import fr.esrf.tangoatk.widget.util.jdraw.JDLabel;
import fr.esrf.tangoatk.widget.util.jdraw.JDObject;
import fr.esrf.tangoatk.widget.util.jdraw.JDValueListener;
import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;

public class Interactive extends JFrame implements JDValueListener {

    JDrawEditor theGraph;

    JDObject btn1;

    JDObject btn2;

    JDObject checkbox;

    JDLabel textArea;

    String[] lines = { "", "", "" };

    public Interactive() {
        theGraph = new JDrawEditor(JDrawEditor.MODE_PLAY);
        try {
            theGraph.loadFile("interactive.jdw");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        btn1 = getObject("Button1");
        btn2 = getObject("Button2");
        checkbox = getObject("Checkbox");
        textArea = (JDLabel) getObject("textArea");
        addText("");
        btn1.addValueListener(this);
        btn2.addValueListener(this);
        checkbox.addValueListener(this);
        setContentPane(theGraph);
        setTitle("Interactive");
    }

    public void valueChanged(JDObject src) {
        if (src == checkbox) {
            addText("Checkbox value changed: " + src.getValue());
            textArea.refresh();
        }
    }

    public void valueExceedBounds(JDObject src) {
        if (src == btn1) {
            addText("Button1 pressed.");
        } else if (src == btn2) {
            addText("Button2 pressed.");
        }
    }

    /**
   * Returns the JDObject having the given name.
   * @param name Name to search
   */
    public JDObject getObject(String name) {
        Vector objs = theGraph.getObjectsByName(name, false);
        if (objs.size() == 0) {
            System.out.print("Error , no object named '" + name + "' found.");
            System.exit(0);
        } else if (objs.size() > 1) {
            System.out.print("Warning , more than one object having the name : " + name + " found ,getting first...");
        }
        return (JDObject) objs.get(0);
    }

    /**
   * Adds the specified line to the text area.
   * @param s Line to be added
   */
    public void addText(String s) {
        int i;
        for (i = 0; i < lines.length - 1; i++) lines[i] = lines[i + 1];
        lines[i] = s;
        String tmp = "";
        for (i = 0; i < lines.length; i++) tmp += lines[i] + "\n";
        textArea.setText(tmp);
        textArea.refresh();
    }

    public static void main(String[] args) {
        final Interactive f = new Interactive();
        ATKGraphicsUtils.centerFrameOnScreen(f);
        f.setVisible(true);
    }
}
