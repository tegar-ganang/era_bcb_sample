package be.lassi.ui.main;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import be.lassi.context.ShowContext;
import be.lassi.domain.Memory;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.widgets.SmallButton;
import be.lassi.util.NLS;

/**
 *
 *
 *
 */
public class MemoryFrame extends BasicFrame {

    private JTabbedPane panelTabs;

    private MemoriesPanel panelMemories;

    private MemoriesPanel panelSubmasters;

    private JButton buttonFromA;

    private JButton buttonFromB;

    private JButton buttonFromStage;

    private JButton buttonFromSubA;

    private JButton buttonFromSubB;

    private JButton buttonToA;

    private JButton buttonToB;

    private JButton buttonAdd;

    private JButton buttonInsert;

    private JButton buttonRemove;

    public MemoryFrame(final ShowContext context) {
        super(context);
        setName(NLS.get("memories.window.title"));
        init();
        updateButtons();
    }

    private void actionFromA() {
        getSelection().setLevelValues(getShow().getControl().getPresetA());
    }

    private void actionFromB() {
        getSelection().setLevelValues(getShow().getControl().getPresetB());
    }

    private void actionFromStage() {
        getSelection().setLevelValues(getShow().getChannels().asMemory());
    }

    private void actionFromSubA() {
        getSelection().setLevelValues(getShow().getSubmasters().getControl().getPresetA());
    }

    private void actionFromSubB() {
        getSelection().setLevelValues(getShow().getSubmasters().getControl().getPresetB());
    }

    private void actionToA() {
        getShow().getControl().getPresetA().setLevelValues(getSelection());
    }

    private void actionToB() {
        getShow().getControl().getPresetB().setLevelValues(getSelection());
    }

    private JComponent createButtonAdd() {
        buttonAdd = new SmallButton(NLS.get("memories.action.add"));
        buttonAdd.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                panelMemories.actionAdd();
            }
        });
        return buttonAdd;
    }

    private JComponent createButtonFromA() {
        buttonFromA = new SmallButton(NLS.get("memories.action.copyFromPresetA"));
        buttonFromA.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionFromA();
            }
        });
        return buttonFromA;
    }

    private JComponent createButtonFromB() {
        buttonFromB = new SmallButton(NLS.get("memories.action.copyFromPresetB"));
        buttonFromB.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionFromB();
            }
        });
        return buttonFromB;
    }

    private JComponent createButtonFromStage() {
        buttonFromStage = new SmallButton(NLS.get("memories.action.copyFromStage"));
        buttonFromStage.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionFromStage();
            }
        });
        return buttonFromStage;
    }

    private JComponent createButtonFromSubA() {
        buttonFromSubA = new SmallButton(NLS.get("memories.action.copyFromSubmasterA"));
        buttonFromSubA.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionFromSubA();
            }
        });
        return buttonFromSubA;
    }

    private JComponent createButtonFromSubB() {
        buttonFromSubB = new SmallButton(NLS.get("memories.action.copyFromSubmasterB"));
        buttonFromSubB.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionFromSubB();
            }
        });
        return buttonFromSubB;
    }

    private JComponent createButtonInsert() {
        buttonInsert = new SmallButton(NLS.get("memories.action.insert"));
        buttonInsert.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                panelMemories.actionInsert();
            }
        });
        return buttonInsert;
    }

    private JComponent createButtonRemove() {
        buttonRemove = new SmallButton(NLS.get("memories.action.delete"));
        buttonRemove.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                panelMemories.actionRemove();
                updateButtons();
            }
        });
        return buttonRemove;
    }

    private JComponent createButtonToA() {
        buttonToA = new SmallButton(NLS.get("memories.action.copyToPresetA"));
        buttonToA.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionToA();
            }
        });
        return buttonToA;
    }

    private JComponent createButtonToB() {
        buttonToB = new SmallButton(NLS.get("memories.action.copyToPresetB"));
        buttonToB.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionToB();
            }
        });
        return buttonToB;
    }

    protected JComponent createPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(3, 3));
        panel.add(createPanelTabs(), BorderLayout.CENTER);
        panel.add(createPanelButtons(), BorderLayout.SOUTH);
        updateButtons();
        return panel;
    }

    private JComponent createPanelButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 0, 3, 3));
        panel.add(createButtonFromA());
        panel.add(createButtonFromB());
        panel.add(createButtonFromStage());
        panel.add(createButtonFromSubA());
        panel.add(createButtonFromSubB());
        panel.add(createButtonToA());
        panel.add(createButtonToB());
        panel.add(createButtonAdd());
        panel.add(createButtonInsert());
        panel.add(createButtonRemove());
        return panel;
    }

    private JComponent createPanelMemories() {
        panelMemories = new MemoriesPanel(getShow().getMemories());
        panelMemories.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                updateButtons();
            }
        });
        return panelMemories;
    }

    private JComponent createPanelSubmasters() {
        panelSubmasters = new MemoriesPanel(getShow().getSubmasters().getMemories());
        panelSubmasters.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                updateButtons();
            }
        });
        return panelSubmasters;
    }

    private JComponent createPanelTabs() {
        panelTabs = new JTabbedPane();
        panelTabs.addTab(NLS.get("memories.tab.memories"), createPanelMemories());
        panelTabs.addTab(NLS.get("memories.tab.submasters"), createPanelSubmasters());
        panelTabs.setSelectedIndex(0);
        panelTabs.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                updateButtons();
            }
        });
        return panelTabs;
    }

    private Memory getSelection() {
        Memory selection;
        if (panelTabs.getSelectedIndex() == 0) {
            selection = panelMemories.getSelection();
        } else {
            selection = panelSubmasters.getSelection();
        }
        return selection;
    }

    private void updateButtons() {
        boolean memories = panelTabs.getSelectedIndex() == 0;
        boolean selection;
        if (memories) {
            selection = !panelMemories.isSelectionEmpty();
        } else {
            selection = !panelSubmasters.isSelectionEmpty();
        }
        buttonFromA.setEnabled(selection);
        buttonFromB.setEnabled(selection);
        buttonFromStage.setEnabled(selection);
        buttonFromSubA.setEnabled(selection);
        buttonFromSubB.setEnabled(selection);
        buttonToA.setEnabled(selection);
        buttonToB.setEnabled(selection);
        buttonAdd.setEnabled(memories);
        buttonInsert.setEnabled(memories);
        buttonRemove.setEnabled(selection & memories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isObsolete() {
        return true;
    }
}
