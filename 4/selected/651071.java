package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import ui.models.GuiStatusModel;
import ui.models.LanboxStatusModel;
import communications.LanboxCommunication;
import communications.SerialLanboxCommunication;

/**
 * @author jan
 *  
 */
public class MixingDesk extends JInternalFrame {

    /**
	 * number of sliders
	 */
    private int sliders;

    /**
	 * the sliders
	 */
    private LightChannel[] desk;

    /** StatusModel gives all information about the status of the lanbox */
    private LanboxStatusModel lanboxModel;

    private GuiStatusModel guiModel;

    /**
	 * Communication to the lanbox.
	 * This is basicly for sending messages as reading is done
	 * via the lanbox status model.
	 */
    private LanboxCommunication comm;

    /**
	 * Create a mixing desk with n sliders.
	 * 
	 * @param n
	 *            number of channels
	 * @param channelsPerRow
	 *            number of channels to display in one row
	 */
    public MixingDesk(int n, int channelsPerRow, LanboxStatusModel lanboxModel, GuiStatusModel guiModel) {
        super("Mixing Desk");
        getContentPane().setLayout(new BorderLayout());
        this.lanboxModel = lanboxModel;
        this.guiModel = guiModel;
        lanboxModel.addChangeListener(new LanboxListener());
        guiModel.addChangeListener(new GuiModelListener());
        int half = n / 2;
        JPanel container = new JPanel(new GridLayout(0, channelsPerRow));
        desk = new LightChannel[n];
        for (int i = 0; i < desk.length; i++) {
            desk[i] = new LightChannel("" + (i + 1));
            container.add(desk[i]);
        }
        JScrollPane scroller = new JScrollPane(container);
        getContentPane().add(scroller);
        setPreferredSize(new Dimension((int) getPreferredSize().getWidth() + 20, (int) desk[0].getPreferredSize().getHeight() * 2 + 30));
        setEditable(false);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        pack();
        setVisible(true);
    }

    public void setEditable(boolean edit) {
        for (int i = 0; i < desk.length; i++) desk[i].setEnabled(edit);
    }

    public void setValues(int[] values) {
        for (int i = 0; i < desk.length && i < values.length; i++) desk[i].setValue(values[i]);
    }

    private class LanboxListener implements ChangeListener {

        public void stateChanged(ChangeEvent evt) {
            setValues(lanboxModel.getChannelValues());
        }
    }

    private class GuiModelListener implements ChangeListener {

        public void stateChanged(ChangeEvent evt) {
            setEditable(guiModel.isEdit());
        }
    }
}
