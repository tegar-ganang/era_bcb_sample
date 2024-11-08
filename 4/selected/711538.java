package gov.sns.apps.istuner;

import java.awt.Color;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.PutException;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SetChannelSpinner extends JSpinner {

    private String ch;

    private boolean caputFlag;

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public SetChannelSpinner(String chan, String title, double ref, double min, double max, double step, boolean putflag) {
        super();
        ch = chan;
        caputFlag = putflag;
        setRef(ref, min, max, step);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(this, "###.00");
        editor.setOpaque(true);
        editor.setBackground(Color.WHITE);
        editor.getTextField().setEditable(false);
        editor.getTextField().setOpaque(true);
        editor.getTextField().setBackground(Color.WHITE);
        this.setEditor(editor);
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED);
        TitledBorder tborder = new TitledBorder(border, title, TitledBorder.CENTER, TitledBorder.LEFT);
        this.setBorder(tborder);
        this.setOpaque(true);
        this.setBackground(Color.WHITE);
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                try {
                    if (caputFlag) {
                        caput();
                    } else {
                        System.out.println("test mode, no caput");
                    }
                } catch (ConnectionException e1) {
                    e1.printStackTrace();
                } catch (PutException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    public void setRef(double ref, double min, double max, double step) {
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(ref, min, max, step);
        this.setModel(spinnerModel);
    }

    public void caput() throws ConnectionException, PutException {
        Channel channel = ChannelFactory.defaultFactory().getChannel(ch);
        channel.connectAndWait(1000);
        Double newVal = (Double) ((SpinnerNumberModel) this.getModel()).getValue();
        channel.putVal(newVal.doubleValue());
    }
}
