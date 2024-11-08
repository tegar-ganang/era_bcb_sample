package ren.gui.seqEdit;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import ren.gui.LabelledView;
import ren.gui.ParameterMap;
import ren.gui.components.NumberTextField;
import ren.util.GB;
import ren.util.PO;

public class PartEditor extends JPanel {

    private Part part;

    private NumberTextField inst, chan;

    private JTextField titleField;

    private ParameterMap scope, quantise, shuffle;

    private Box scopeBox = new Box(0);

    private Box quantBox = new Box(0);

    private Box shuBox = new Box(0);

    private final String sctt = "the viewable portion of the pattern";

    private final String qutt = "the level of quantisation of rhythm";

    private final String shutt = "the amount of swing or shuffle in the rythm";

    private final Dimension scdim = new Dimension(60, 30);

    private final Dimension qudim = new Dimension(40, 30);

    private final Dimension shudim = new Dimension(30, 30);

    private ParamNotePanel pnpanel;

    private ParamNTGC converter;

    public PartEditor() {
        this(new ParamNTGC((new ParameterMap()).construct(0, 6, new double[] { 2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0 }, 4.0, "scope")));
    }

    public PartEditor(Part p) {
        this(new ParamNTGC((new ParameterMap()).construct(0, 6, new double[] { 2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0 }, 4.0, "scope")), p);
    }

    public PartEditor(ParamNTGC converter) {
        this(converter, new Part(new Phrase(new Note(60, 1))));
    }

    public PartEditor(ParamNTGC converter, Part p) {
        super();
        this.part = p;
        this.setLayout(new GridBagLayout());
        this.converter = converter;
        converter.setNumberOfBeatsViewed(8);
        this.scope = converter.getScope();
        this.quantise = converter.getQuantiseParam();
        this.shuffle = converter.getShuffleParam();
        pnpanel = new ParamNotePanel();
        pnpanel.construct(converter);
        initialise();
    }

    public ParamNTGC getParamNTGC() {
        return converter;
    }

    public void setParamNTGC(ParamNTGC cv) {
        this.converter = cv;
    }

    /**
     * note - no need to call this, as it is called automatically fro the 
     * constructor
      */
    protected void initialise() {
        this.removeAll();
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == inst) {
                    part.setInstrument(((NumberTextField) e.getSource()).getValue());
                } else if (e.getSource() == chan) {
                    part.setChannel(((NumberTextField) e.getSource()).getValue());
                } else if (e.getSource() == titleField) part.setTitle(titleField.getText());
            }
        };
        inst = new NumberTextField(0, 1000, part.getInstrument());
        inst.addActionListener(al);
        chan = new NumberTextField(1, 96, part.getChannel());
        chan.addActionListener(al);
        titleField = new JTextField(part.getTitle());
        JLabel ilab = new JLabel(" inst");
        JLabel clab = new JLabel(" chan");
        GB.add(this, 1, 0, ilab, 1, 1);
        GB.add(this, 2, 0, inst, 1, 1);
        GB.add(this, 3, 0, clab, 1, 1);
        GB.add(this, 4, 0, chan, 1, 1);
        makeLabView(scope, scopeBox, this.sctt, this.scdim);
        GB.add(this, 5, 0, scopeBox, 3, 1);
        makeLabView(quantise, quantBox, this.qutt, this.qudim);
        GB.add(this, 8, 0, quantBox, 2, 1);
        makeLabView(shuffle, this.shuBox, this.shutt, this.shudim);
        GB.add(this, 10, 0, shuBox, 1, 1);
        pnpanel.setPart(part);
        GB.add(this, 0, 1, pnpanel, 15, 10);
    }

    private void makeLabView(ParameterMap pm, Box b, String toolText, Dimension dim) {
        LabelledView lv = (new LabelledView()).construct(pm, true, true, toolText, 0);
        lv.getView().setPreferredSize(dim);
        b.add(lv);
    }

    public ParameterMap getScope() {
        return scope;
    }

    public void setScope(ParameterMap nscope) {
        this.scope = nscope;
        pnpanel.setScope(nscope);
        scopeBox.remove(0);
        this.makeLabView(scope, scopeBox, sctt, this.scdim);
        scopeBox.repaint();
    }

    public void setQuantise(ParameterMap nquant) {
        this.quantise = nquant;
        pnpanel.setQuantise(nquant);
        quantBox.remove(0);
        this.makeLabView(quantise, quantBox, qutt, this.qudim);
        quantBox.repaint();
    }

    public void setShuffle(ParameterMap shu) {
        this.shuffle = shu;
        this.shuBox.remove(0);
        this.makeLabView(shuffle, shuBox, shutt, shudim);
        this.shuBox.repaint();
    }

    public void setPart(Part p) {
        pnpanel.setPart(p);
        this.part = p;
        inst.setValue(p.getInstrument());
        chan.setValue(p.getChannel());
    }

    public NotePanel getNotePanel() {
        return this.pnpanel;
    }
}
