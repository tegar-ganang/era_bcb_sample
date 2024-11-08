package com.googlecode.kanzaki.ui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SelectChannelDialog extends JDialog {

    /**
	 * 
	 */
    private static final long serialVersionUID = -1564660632974634755L;

    private JTextField channelField;

    public SelectChannelDialog(JFrame owner) {
        super(owner, true);
        Container contentPane;
        JButton okButton;
        channelField = new JTextField(8);
        okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
            }
        });
        contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());
        contentPane.add(new JLabel("�����"));
        contentPane.add(channelField);
        contentPane.add(okButton);
        setTitle("�������� �����");
        setSize(300, 70);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public String getChannelName() {
        return channelField.getText();
    }
}
