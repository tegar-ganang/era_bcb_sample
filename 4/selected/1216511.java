package com.lemu.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.Border;
import com.lemu.music.LPart;
import jmms.Sequencer;
import jmms.StartListener;
import ren.gui.components.NumberTextField;
import ren.gui.components.RJCheckBox;
import ren.gui.components.ValueListener;
import ren.util.DimUtil;
import ren.util.GB;
import ren.util.PO;

public class LPButton extends JComponent {

    private JTextField ti = new JTextField();

    private RJCheckBox mu = new RJCheckBox("M", false, 1);

    private RJCheckBox so = new RJCheckBox("S", false, 0);

    private NumberTextField vol = new NumberTextField(1, 128, 100);

    private NumberTextField idc = new NumberTextField(0, 20, 0);

    private LPart lp;

    private PMGEditor pmge;

    private LPButton thislpb;

    private Sequencer seq;

    private boolean selected = false;

    private Border norm = BorderFactory.createLineBorder(Color.BLACK);

    private Border highlight = BorderFactory.createLineBorder(new Color(190, 250, 200), 5);

    private MouseAdapter clickedThis = new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
            if (pmge != null) {
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (pmge != null) {
                pmge.setSelectedLPB(thislpb);
            } else {
                PO.p("null, apparently");
            }
            if (e.getClickCount() == 2 && e.getSource() == ti) {
                ti.setEditable(true);
                ti.selectAll();
            }
        }
    };

    public LPButton(LPart pm, KeyListener kl) {
        super();
        thislpb = this;
        this.setLPart(pm);
        this.setPreferredSize(new Dimension(120, 20));
        this.addMouseListener(clickedThis);
        ti.addMouseListener(clickedThis);
        ti.addFocusListener(new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                ti.setEditable(false);
                lp.getPart().setTitle(ti.getText());
            }
        });
        ti.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == e.VK_ENTER) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                    lp.getPart().setTitle(ti.getText());
                }
            }
        });
        vol.addValueListener(new ValueListener() {

            public void valueGeneratorUpdate(int rv) {
                if (seq != null) {
                    seq.sendVol(lp.getPart().getChannel() - 1, rv);
                    lp.getPart().setVolume(rv);
                }
            }
        });
        mu.setModel(lp.getMuteModel());
        mu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pmge.getPMG().getAutoPlayer().record(lp, "mute", mu.isSelected());
            }
        });
        so.setModel(lp.getSoloModel());
        this.setMuteKeyListener(kl);
        build();
    }

    public void setMuteKeyListener(KeyListener kl) {
        this.addKeyListener(kl);
        ti.addKeyListener(kl);
        mu.addKeyListener(kl);
        so.addKeyListener(kl);
    }

    /**
     * this is only so that you can call setSeectedLPB when it is clicked
     * @param pmge
     */
    public void addPGME(PMGEditor pmge) {
        this.pmge = pmge;
        this.seq = pmge.getLPlayer().getSequencer();
        pmge.getLPlayer().addStartListener(new StartListener() {

            public void started() {
                vol.fireActionPerformed();
            }
        });
    }

    private void build() {
        this.setLayout(new GridBagLayout());
        ti.setText(lp.getPart().getTitle());
        ti.setEditable(false);
        ti.setDisabledTextColor(Color.BLACK);
        vol.setMargin(new Insets(0, 0, 0, 0));
        DimUtil.insets(ti, 0, 0, 0, 0);
        DimUtil.insets(vol, 0, 0, 0, 0);
        GB.add(this, 0, 0, idc, 1, 1, 0, 0, 0, 0, 0.1, 0.0, true, true);
        GB.add(this, 1, 0, ti, 6, 1, 0, 0, 0, 0, 0.5, 0.0, true, true);
        GB.add(this, 7, 0, mu, 1, 1, 0, 0, 0, 0, 0.0, 0.0, false, true);
        GB.add(this, 8, 0, so, 1, 1, 0, 0, 0, 0, 0.0, 0.0, false, true);
        GB.add(this, 9, 0, vol, 1, 1, 0, 0, 0, 0, 0.2, 0.0, true, true);
        this.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setPreferredSize(this.getLayout().preferredLayoutSize(this));
        this.setMaximumSize(this.getPreferredSize());
    }

    public void setLPart(LPart p) {
        this.lp = p;
        this.vol.setValue(p.getPart().getVolume());
        this.ti.setText(lp.getPart().getTitle());
        this.idc.setValue(p.getPart().getIdChannel());
        this.mu.setModel(p.getMuteModel());
        this.so.setModel(p.getSoloModel());
        this.mu.revalidate();
        this.repaint();
    }

    public LPart getLPart() {
        return lp;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            this.setBorder(highlight);
        } else {
            this.setBorder(norm);
        }
        repaint();
        this.selected = selected;
    }

    public void flipMute() {
        this.mu.doClick();
    }
}
