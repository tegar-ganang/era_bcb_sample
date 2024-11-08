package org.jtv.frontend;

import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jtv.common.ConsoleTvControllerObserver;
import org.jtv.common.TvController;
import org.jtv.common.TvControllerEvent;
import org.jtv.common.TvControllerObserver;

public class SwingTvController extends JFrame implements TvControllerObserver {

    private static final long serialVersionUID = 1L;

    private JPanel jContentPane = null;

    private JButton jButton = null;

    private JButton jButton1 = null;

    private JLabel jLabel = null;

    private TvController controller;

    /**
   * This is the default constructor
   */
    public SwingTvController(TvController controller) {
        super();
        this.controller = controller;
        controller.getObservers().addObserver(this);
        initialize();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        this.setSize(321, 143);
        this.setContentPane(getJContentPane());
        this.setTitle("JFrame");
    }

    /**
   * This method initializes jContentPane
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jLabel = new JLabel();
            jLabel.setBounds(new Rectangle(90, 15, 91, 76));
            jLabel.setText(String.valueOf(controller.getChannel(0)));
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(getJButton(), null);
            jContentPane.add(getJButton1(), null);
            jContentPane.add(jLabel, null);
        }
        return jContentPane;
    }

    /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setBounds(new Rectangle(195, 15, 92, 31));
            jButton.setText("ch up");
            jButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int channel = Integer.parseInt(jLabel.getText());
                    controller.changeChannel(0, channel + 1);
                }
            });
        }
        return jButton;
    }

    /**
   * This method initializes jButton1
   * 
   * @return javax.swing.JButton
   */
    private JButton getJButton1() {
        if (jButton1 == null) {
            jButton1 = new JButton();
            jButton1.setBounds(new Rectangle(195, 60, 91, 31));
            jButton1.setText("ch down");
            jButton1.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int channel = Integer.parseInt(jLabel.getText());
                    controller.changeChannel(0, channel - 1);
                }
            });
        }
        return jButton1;
    }

    public void event(TvControllerEvent event) {
        switch(event.getEvent()) {
            case CHANNEL_CHANGED:
                {
                    jLabel.setText(String.valueOf(event.getInt("channel")));
                    break;
                }
        }
    }

    public static void main(String[] args) {
        final TvController remote = new JmxTvController("localhost", 9999);
        remote.getObservers().addObserver(new ConsoleTvControllerObserver("REMOTE", System.out));
        SwingTvController controller = new SwingTvController(remote);
        controller.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                remote.close();
            }
        });
    }
}
