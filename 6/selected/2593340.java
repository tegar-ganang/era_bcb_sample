package control.top;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import view.MainWindow;
import view.MessageWindow;
import com.vaadin.Application;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import control.base.xmpp.XMPPConnectionService;

@SuppressWarnings("serial")
public class Messenger extends Application {

    private XMPPConnectionService connectionService;

    private MessageService service;

    private MessageWindow message;

    @Override
    public void init() {
        final MainWindow window = new MainWindow("Luiz");
        message = window.getMessageWindow();
        message.startChat("flavio.franca.ufrj@gmail.com");
        connectionService = new XMPPConnectionService();
        try {
            connectionService.connect("talk.google.com", 5222);
            connectionService.login("falsoparafes@gmail.com", "euaindasouumcorreiodeteste");
            service = connectionService.chat("gust.has@gmail.com");
            MessageListener m = new MessageListener() {

                @Override
                public void processMessage(Chat chat, Message message) {
                    System.out.println(message.getBody());
                    window.getMessageWindow().setOutputText(message.getBody());
                }
            };
            service.createChat(m);
            message.setListenerOutput(m);
        } catch (Exception e) {
        }
        ClickListener c = new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    service.send(message.getInputText());
                } catch (Exception e) {
                }
                message.esvaziaInputText();
            }
        };
        message.setListenerButton(c);
        setMainWindow(window);
    }
}
