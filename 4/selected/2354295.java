package net.sf.refactorit.ui.module.where;

import net.sf.refactorit.query.usage.filters.BinVariableSearchFilter;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Obtains filter options for BinVariable
 *
 * @author Vladislav Vislogubov
 */
public class VariablePanel extends JPanel {

    JButton ok;

    JCheckBox read = new JCheckBox("Read Access", true);

    JCheckBox write = new JCheckBox("Write Access", true);

    public VariablePanel(JButton ok, BinVariableSearchFilter filter) {
        super();
        this.ok = ok;
        if (filter != null) {
            read.setSelected(filter.isReadAccess());
            write.setSelected(filter.isWriteAccess());
        }
        init();
    }

    private void init() {
        setLayout(new GridBagLayout());
        read.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!read.isSelected() && !write.isSelected()) {
                    ok.setEnabled(false);
                } else {
                    ok.setEnabled(true);
                }
            }
        });
        write.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!read.isSelected() && !write.isSelected()) {
                    ok.setEnabled(false);
                } else {
                    ok.setEnabled(true);
                }
            }
        });
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 10, 3, 5);
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.SOUTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        add(read, constraints);
        constraints.insets = new Insets(0, 10, 5, 5);
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        add(write, constraints);
    }

    public boolean isRead() {
        return read.isSelected();
    }

    public boolean isWrite() {
        return write.isSelected();
    }
}
