package chatter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ChatPanel extends JPanel implements Observer, Observable, ItemListener {

    private static final long serialVersionUID = -6020280291286012906L;

    private JTextField readField;

    private JTextField writeField;

    private JComboBox combo;

    private JButton apply;

    private HashMap<String, Chatter> map;

    private Chatter current;

    private Observer frame;

    public ChatPanel(Chatter[] chatters) {
        current = null;
        frame = null;
        readField = new JTextField("       read only     ");
        readField.setEditable(false);
        writeField = new JTextField("      write/read     ");
        apply = new JButton("Apply");
        apply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (current != null) current.sendMessage(writeField.getText());
            }
        });
        String[] tmp = new String[chatters.length];
        map = new HashMap<String, Chatter>();
        for (int i = 0; i < chatters.length; i++) {
            tmp[i] = chatters[i].getName();
            map.put(chatters[i].getName(), chatters[i]);
        }
        combo = new JComboBox(tmp);
        combo.addItemListener(this);
        current = map.get((String) combo.getItemAt(0));
        readField.setText(current.getLastMessage());
        add(readField);
        add(writeField);
        add(combo);
        add(apply);
    }

    public void notifyEvent(Event e) {
        if (current != null) readField.setText(current.getLastMessage());
    }

    public void itemStateChanged(ItemEvent e) {
        if (current != null) current.setObserver(null);
        String tmp = (String) e.getItem();
        current = map.get(tmp);
        current.setObserver(this);
        readField.setText(current.getLastMessage());
        frame.notifyEvent(new Event("Current chatter: " + tmp));
    }

    @Override
    public void setObserver(Observer obs) {
        frame = obs;
    }
}
