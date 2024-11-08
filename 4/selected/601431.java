package com.lemu.leco.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import jm.music.data.Part;
import ren.gui.components.NumberTextField;
import ren.util.PO;
import com.lemu.music.LPart;

public class EditPartBlock extends JPanel {

    private LPart part;

    private JComboBox channelSelect;

    private NumberTextField instrumentTextField;

    private JTextField title;

    public EditPartBlock() {
        super();
    }

    public EditPartBlock construct(LPart p) {
        this.removeAll();
        BoxLayout lay = new BoxLayout(this, 1);
        this.setLayout(lay);
        this.getInsets().top = 0;
        this.getInsets().bottom = 0;
        this.part = p;
        if (LeCoFrame.debugGui) {
            this.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 1));
        }
        String labelString = "null part";
        if (p != null) {
            labelString = p.getPart().getTitle();
        } else {
            part = (new LPart()).construct(new Part(labelString));
        }
        title = new JTextField(labelString, 7);
        title.setBackground(title.getForeground());
        title.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                updateTitleView();
            }

            public void insertUpdate(DocumentEvent e) {
                updateTitleView();
            }

            public void removeUpdate(DocumentEvent e) {
                updateTitleView();
            }
        });
        if (LeCoFrame.debugGui) {
            title.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        }
        constructChannelSelect();
        instrumentTextField = new NumberTextField(0, 127, 0);
        Dimension dim = new Dimension(EditPanel.partWid - this.getInsets().left + this.getInsets().right, 25);
        title.setPreferredSize(dim);
        title.setMaximumSize(dim);
        channelSelect.setMaximumSize(dim);
        this.add(title);
        this.add(channelSelect);
        JLabel instLabel = new JLabel(" Inst ");
        Box instb = new Box(0);
        instb.add(instLabel);
        instb.add(instrumentTextField);
        instrumentTextField.setMaximumSize(new Dimension(40, 25));
        instrumentTextField.setPreferredSize(new Dimension(40, 25));
        instrumentTextField.setMinimumSize(new Dimension(40, 25));
        instrumentTextField.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                updateInstrument();
            }

            private void updateInstrument() {
                String txt = instrumentTextField.getText();
                if (txt != null && txt.length() > 0) {
                    int i = Integer.parseInt(txt);
                    if (i > 0 && i < 128) {
                        part.getPart().setInstrument(i);
                    } else {
                        PO.p("WARNING: instrument out of range: " + i);
                    }
                }
            }

            public void insertUpdate(DocumentEvent e) {
                updateInstrument();
            }

            public void changedUpdate(DocumentEvent e) {
                updateInstrument();
            }
        });
        instb.setAlignmentX(RIGHT_ALIGNMENT);
        this.add(instb);
        this.setPreferredSize(new Dimension(EditPanel.partWid, EditPanel.partHei));
        return this;
    }

    public void updateTitleView() {
        part.getPart().setTitle(title.getText());
        part.viewPartBlock.updateTitle();
    }

    private void constructChannelSelect() {
        channelSelect = new JComboBox();
        for (int i = 0; i < 16; i++) {
            channelSelect.addItem("ch " + (i + 1));
        }
        channelSelect.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                part.getPart().setChannel(channelSelect.getSelectedIndex() + 1);
            }
        });
        if (part.getPart().getChannel() > 16 || part.getPart().getChannel() < 1) {
            PO.p("channel out of bounds warning " + part.getPart().getChannel());
        }
        channelSelect.setSelectedIndex(part.getPart().getChannel() - 1);
    }

    public void setPart(Part part) {
        this.part.setPart(part);
    }

    public Part getPart() {
        return part.getPart();
    }
}
