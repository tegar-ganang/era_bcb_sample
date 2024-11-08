package mw.client.dialogs.jframe;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mw.client.constants.Constants;
import mw.client.managers.ConnectionManager;
import mw.client.managers.ProfileManager;
import mw.client.managers.SettingsManager;
import mw.client.utils.CardImageUtils;
import mw.client.utils.cache.SaveObjectUtil;
import mw.mtgforge.Constant;
import mw.server.list.CardBeanList;
import mw.server.model.CardUrl;
import mw.server.model.MagicWarsModel.CardSuperType;
import mw.server.model.bean.CardBean;
import mw.server.rmi.MessagingInterface;
import mw.utils.CacheObjectUtil;
import org.apache.log4j.Logger;

public class DownloadPictures extends DefaultBoundedRangeModel implements Runnable {

    private int type;

    private JTextField addr, port;

    private JProgressBar bar;

    private JOptionPane dlg;

    private boolean cancel;

    private JButton close;

    private int cardIndex;

    private ArrayList<CardUrl> cards;

    private ArrayList<CardUrl> cardsInGame;

    private JComboBox jComboBox1;

    private JLabel jLabel1;

    private static boolean offlineMode = false;

    private JCheckBox checkBox;

    public static final Proxy.Type[] types = Proxy.Type.values();

    public static void main(String[] args) {
        startDownload(null);
    }

    public static void startDownload(JFrame frame) {
        ArrayList<CardUrl> cards = getNeededCards();
        if (cards == null || cards.size() == 0) {
            JOptionPane.showMessageDialog(null, "All card pictures have been downloaded.");
            return;
        }
        DownloadPictures download = new DownloadPictures(cards);
        JDialog dlg = download.getDlg(frame);
        dlg.setVisible(true);
        dlg.dispose();
        download.setCancel(true);
    }

    public JDialog getDlg(JFrame frame) {
        String title = "Downloading";
        if (offlineMode) {
            title += " (using local card db)";
        }
        final JDialog dlg = this.dlg.createDialog(frame, title);
        close.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });
        return dlg;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public DownloadPictures(ArrayList<CardUrl> cards) {
        this.cards = cards;
        this.cardsInGame = new ArrayList<CardUrl>();
        for (CardUrl url : cards) {
            if (url.isExistsInTheGame()) cardsInGame.add(url);
        }
        addr = new JTextField("Proxy Address");
        port = new JTextField("Proxy Port");
        bar = new JProgressBar(this);
        JPanel p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));
        ButtonGroup bg = new ButtonGroup();
        String[] labels = { "No Proxy", "HTTP Proxy", "SOCKS Proxy" };
        for (int i = 0; i < types.length; i++) {
            JRadioButton rb = new JRadioButton(labels[i]);
            rb.addChangeListener(new ProxyHandler(i));
            bg.add(rb);
            p0.add(rb);
            if (i == 0) rb.setSelected(true);
        }
        p0.add(addr);
        p0.add(port);
        p0.add(Box.createVerticalStrut(5));
        jLabel1 = new JLabel();
        jLabel1.setText("Please select server:");
        jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        p0.add(jLabel1);
        p0.add(Box.createVerticalStrut(5));
        ComboBoxModel jComboBox1Model = new DefaultComboBoxModel(new String[] { "magiccards.info" });
        jComboBox1 = new JComboBox();
        jComboBox1.setModel(jComboBox1Model);
        jComboBox1.setAlignmentX(Component.LEFT_ALIGNMENT);
        p0.add(jComboBox1);
        p0.add(Box.createVerticalStrut(5));
        final JButton b = new JButton("Start download");
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new Thread(DownloadPictures.this).start();
                b.setEnabled(false);
                checkBox.setEnabled(false);
            }
        });
        p0.add(Box.createVerticalStrut(5));
        p0.add(bar);
        bar.setStringPainted(true);
        int count = cards.size();
        float mb = (count * 70.0f) / 1024;
        bar.setString(String.format(cardIndex == cards.size() ? "%d of %d cards finished! Please close!" : "%d of %d cards finished! Please wait! [%.1f Mb]", 0, cards.size(), mb));
        Dimension d = bar.getPreferredSize();
        d.width = 300;
        bar.setPreferredSize(d);
        p0.add(Box.createVerticalStrut(5));
        checkBox = new JCheckBox("Download for current game only.");
        p0.add(checkBox);
        p0.add(Box.createVerticalStrut(5));
        checkBox.setEnabled(!offlineMode);
        checkBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (checkBox.isSelected()) {
                    int count = DownloadPictures.this.cardsInGame.size();
                    float mb = (count * 70.0f) / 1024;
                    bar.setString(String.format(count == 0 ? "No images to download!" : "%d of %d cards finished! Please wait! [%.1f Mb]", 0, DownloadPictures.this.cardsInGame.size(), mb));
                } else {
                    int count = DownloadPictures.this.cards.size();
                    float mb = (count * 70.0f) / 1024;
                    bar.setString(String.format(cardIndex == count ? "%d of %d cards finished! Please close!" : "%d of %d cards finished! Please wait! [%.1f Mb]", 0, count, mb));
                }
            }
        });
        Object[] options = { b, close = new JButton("Cancel") };
        dlg = new JOptionPane(p0, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
    }

    private static ArrayList<CardUrl> getNeededCards() {
        ArrayList<CardUrl> cardsToDownload = new ArrayList<CardUrl>();
        ArrayList<CardUrl> allcards = new ArrayList<CardUrl>();
        try {
            MessagingInterface conn = ConnectionManager.getRMIConnection();
            if (conn != null) {
                allcards = conn.getCardUrls(ProfileManager.getMyId());
                offlineMode = false;
            } else {
                offlineMode = true;
                Object object = CacheObjectUtil.load(Constants.DEFAULT_CACHE_DIR, Constants.CARDS_CACHE_FILENAME);
                if (object != null && object instanceof CardBeanList) {
                    CardBeanList allCards = (CardBeanList) object;
                    for (CardBean card : allCards) {
                        if (card.getType().contains(CardSuperType.Rule)) continue;
                        if (card.getCollectorID() > 0 && !card.getSetName().isEmpty()) {
                            CardUrl url = new CardUrl(card.getName(), card.getSetName(), card.getCollectorID(), false);
                            allcards.add(url);
                        } else {
                            if (card.getCollectorID() < 1) {
                                System.err.println("There was a critical error!");
                                log.error("Card has no collector ID and won't be sent to client: " + card);
                            } else if (card.getSetName().isEmpty()) {
                                System.err.println("There was a critical error!");
                                log.error("Card has no set name and won't be sent to client:" + card);
                            }
                        }
                    }
                }
                log.info("Card urls generated in offline mode.");
            }
            SaveObjectUtil.saveObject(allcards, ProfileManager.getMyId(), "card_urls");
        } catch (Exception e) {
            log.error(e);
        }
        File file;
        for (CardUrl card : allcards) {
            boolean withCollectorId = false;
            if (card.name.equals("Forest") || card.name.equals("Mountain") || card.name.equals("Swamp") || card.name.equals("Island") || card.name.equals("Plains")) {
                withCollectorId = true;
            }
            file = new File(CardImageUtils.getImagePath(card, withCollectorId));
            if (!file.exists()) {
                cardsToDownload.add(card);
            }
        }
        for (CardUrl card : cardsToDownload) {
            if (card.token) {
                log.info("Card to download: " + card.name + " (Token) " + card.url);
            } else {
                try {
                    log.info("Card to download: " + card.name + " (" + card.set + ") " + CardImageUtils.generateURL(card.collector, card.set));
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
        return cardsToDownload;
    }

    static class Card {

        public final String name;

        public final String url;

        Card(String cardName, String cardURL) {
            name = cardName;
            url = cardURL;
        }
    }

    private class ProxyHandler implements ChangeListener {

        private int type;

        public ProxyHandler(int type) {
            this.type = type;
        }

        public void stateChanged(ChangeEvent e) {
            if (((AbstractButton) e.getSource()).isSelected()) {
                DownloadPictures.this.type = type;
                addr.setEnabled(type != 0);
                port.setEnabled(type != 0);
            }
        }
    }

    public void run() {
        BufferedInputStream in;
        BufferedOutputStream out;
        File base = new File(Constant.IO.imageBaseDir);
        if (!base.exists()) {
            base.mkdir();
        }
        Proxy p = null;
        if (type == 0) p = Proxy.NO_PROXY; else try {
            p = new Proxy(types[type], new InetSocketAddress(addr.getText(), Integer.parseInt(port.getText())));
        } catch (Exception ex) {
            throw new RuntimeException("Gui_DownloadPictures : error 1 - " + ex);
        }
        if (p != null) {
            byte[] buf = new byte[1024];
            int len;
            HashSet<String> ignoreUrls = SettingsManager.getManager().getIgnoreUrls();
            for (update(0); (checkBox.isSelected() ? cardIndex < cardsInGame.size() : cardIndex < cards.size()) && !cancel; update(cardIndex + 1)) {
                try {
                    CardUrl card = checkBox.isSelected() ? cardsInGame.get(cardIndex) : cards.get(cardIndex);
                    log.info("Downloading card: " + card.name + " (" + card.set + ")");
                    URL url = new URL(CardImageUtils.generateURL(card.collector, card.set));
                    if (ignoreUrls.contains(card.set) || card.token) {
                        if (card.collector != 0) {
                            continue;
                        }
                        url = new URL(card.url);
                    }
                    in = new BufferedInputStream(url.openConnection(p).getInputStream());
                    createDirForCard(card);
                    boolean withCollectorId = false;
                    if (card.name.equals("Forest") || card.name.equals("Mountain") || card.name.equals("Swamp") || card.name.equals("Island") || card.name.equals("Plains")) {
                        withCollectorId = true;
                    }
                    File fileOut = new File(CardImageUtils.getImagePath(card, withCollectorId));
                    out = new BufferedOutputStream(new FileOutputStream(fileOut));
                    while ((len = in.read(buf)) != -1) {
                        if (cancel) {
                            in.close();
                            out.flush();
                            out.close();
                            fileOut.delete();
                            return;
                        }
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.flush();
                    out.close();
                } catch (Exception ex) {
                    log.error(ex, ex);
                }
            }
        }
        close.setText("Close");
    }

    private static File createDirForCard(CardUrl card) throws Exception {
        File setDir = new File(CardImageUtils.getImageDir(card));
        if (!setDir.exists()) {
            setDir.mkdirs();
        }
        return setDir;
    }

    private void update(int card) {
        this.cardIndex = card;
        final class Worker implements Runnable {

            private int card;

            Worker(int card) {
                this.card = card;
            }

            public void run() {
                fireStateChanged();
                if (checkBox.isSelected()) {
                    int count = DownloadPictures.this.cardsInGame.size();
                    int countLeft = count - card;
                    float mb = (countLeft * 70.0f) / 1024;
                    bar.setString(String.format(this.card == count ? "%d of %d cards finished! Please close!" : "%d of %d cards finished! Please wait!  [%.1f Mb]", this.card, count, mb));
                } else {
                    int count = DownloadPictures.this.cards.size();
                    int countLeft = count - card;
                    float mb = (countLeft * 70.0f) / 1024;
                    bar.setString(String.format(cardIndex == count ? "%d of %d cards finished! Please close!" : "%d of %d cards finished! Please wait! [%.1f Mb]", this.card, count, mb));
                }
            }
        }
        EventQueue.invokeLater(new Worker(card));
    }

    private static final Logger log = Logger.getLogger(DownloadPictures.class);

    private static final long serialVersionUID = 1L;
}
