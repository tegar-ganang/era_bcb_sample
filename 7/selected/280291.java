package frontend;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.dyno.visual.swing.layouts.Constraints;
import org.dyno.visual.swing.layouts.GroupLayout;
import org.dyno.visual.swing.layouts.Leading;
import concrete.Dice;
import concrete.Player;
import concrete.Token;
import backend.BoardMode;

public class ChooseToken extends JFrame {

    private static final long serialVersionUID = 1L;

    private BoardMode boardMode;

    private int playerNum;

    private boolean diceSumAlloc;

    private int[] diceSum;

    private Player[] players = new Player[8];

    private boolean[] playerPos = new boolean[8];

    private JButton token01;

    private JButton token02;

    private JButton token03;

    private JButton token04;

    private JButton token05;

    private JButton token06;

    private JButton token07;

    private JButton token08;

    private JButton startGameButton;

    private JButton backButton;

    private JLabel backgroundImage;

    private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

    public ChooseToken() {
        this.boardMode = new BoardMode();
        for (int i = 0; i < 8; i++) {
            playerPos[i] = false;
        }
        diceSumAlloc = false;
        playerNum = 0;
        initComponents();
    }

    public boolean getDiceSumAlloc() {
        return this.diceSumAlloc;
    }

    public void setDiceSumAlloc(boolean diceSumAlloc) {
        this.diceSumAlloc = diceSumAlloc;
    }

    private void initComponents() {
        setTitle("Choose a Token");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/icon.gif")));
        setResizable(false);
        setLayout(new GroupLayout());
        add(initToken02(), new Constraints(new Leading(467, 204, 10, 10), new Leading(50, 212, 10, 10)));
        add(initToken01(), new Constraints(new Leading(3, 191, 10, 10), new Leading(50, 195, 6, 6)));
        add(initToken08(), new Constraints(new Leading(157, 181, 10, 10), new Leading(54, 200, 10, 10)));
        add(initToken05(), new Constraints(new Leading(311, 197, 6, 6), new Leading(54, 201, 6, 6)));
        add(initToken03(), new Constraints(new Leading(307, 212, 10, 10), new Leading(223, 197, 10, 10)));
        add(initToken06(), new Constraints(new Leading(136, 222, 6, 6), new Leading(234, 197, 10, 10)));
        add(initToken04(), new Constraints(new Leading(454, 217, 6, 6), new Leading(228, 196, 10, 10)));
        add(initToken07(), new Constraints(new Leading(0, 225, 6, 6), new Leading(231, 197, 6, 6)));
        add(initStartGameButton(), new Constraints(new Leading(349, 10, 10), new Leading(434, 6, 6)));
        add(initBackButton(), new Constraints(new Leading(247, 86, 10, 10), new Leading(434, 6, 6)));
        add(initBackgroundImage(), new Constraints(new Leading(0, 10, 10), new Leading(0, 11, 11)));
        setSize(671, 525);
    }

    private JButton initToken01() {
        if (token01 == null) {
            token01 = new JButton();
            token01.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/01.png")));
            token01.setBorderPainted(false);
            token01.setContentAreaFilled(false);
            token01.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token01.png")));
            token01.setDefaultCapable(false);
            token01.setVerticalAlignment(SwingConstants.TOP);
            token01.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token01ActionActionPerformed(event);
                }
            });
        }
        return token01;
    }

    private JButton initToken02() {
        if (token02 == null) {
            token02 = new JButton();
            token02.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/02.png")));
            token02.setBorderPainted(false);
            token02.setContentAreaFilled(false);
            token02.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token02.png")));
            token02.setDefaultCapable(false);
            token02.setVerticalAlignment(SwingConstants.TOP);
            token02.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token02ActionActionPerformed(event);
                }
            });
        }
        return token02;
    }

    private JButton initToken03() {
        if (token03 == null) {
            token03 = new JButton();
            token03.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/03.png")));
            token03.setBorderPainted(false);
            token03.setContentAreaFilled(false);
            token03.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token03.png")));
            token03.setDefaultCapable(false);
            token03.setVerticalAlignment(SwingConstants.TOP);
            token03.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token03ActionActionPerformed(event);
                }
            });
        }
        return token03;
    }

    private JButton initToken04() {
        if (token04 == null) {
            token04 = new JButton();
            token04.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/04.png")));
            token04.setBorderPainted(false);
            token04.setContentAreaFilled(false);
            token04.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token04.png")));
            token04.setDefaultCapable(false);
            token04.setVerticalAlignment(SwingConstants.TOP);
            token04.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token04ActionActionPerformed(event);
                }
            });
        }
        return token04;
    }

    private JButton initToken05() {
        if (token05 == null) {
            token05 = new JButton();
            token05.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/05.png")));
            token05.setBorderPainted(false);
            token05.setContentAreaFilled(false);
            token05.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token05.png")));
            token05.setDefaultCapable(false);
            token05.setVerticalAlignment(SwingConstants.TOP);
            token05.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token05ActionActionPerformed(event);
                }
            });
        }
        return token05;
    }

    private JButton initToken06() {
        if (token06 == null) {
            token06 = new JButton();
            token06.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/06.png")));
            token06.setBorderPainted(false);
            token06.setContentAreaFilled(false);
            token06.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token06.png")));
            token06.setDefaultCapable(false);
            token06.setVerticalAlignment(SwingConstants.TOP);
            token06.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token06ActionActionPerformed(event);
                }
            });
        }
        return token06;
    }

    private JButton initToken07() {
        if (token07 == null) {
            token07 = new JButton();
            token07.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/07.png")));
            token07.setBorderPainted(false);
            token07.setContentAreaFilled(false);
            token07.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token07.png")));
            token07.setDefaultCapable(false);
            token07.setVerticalAlignment(SwingConstants.TOP);
            token07.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token07ActionActionPerformed(event);
                }
            });
        }
        return token07;
    }

    private JButton initToken08() {
        if (token08 == null) {
            token08 = new JButton();
            token08.setIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/08.png")));
            token08.setBorderPainted(false);
            token08.setContentAreaFilled(false);
            token08.setRolloverIcon(new ImageIcon(getClass().getResource("/images/tokenChoose/token08.png")));
            token08.setDefaultCapable(false);
            token08.setVerticalAlignment(SwingConstants.TOP);
            token08.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    token08ActionActionPerformed(event);
                }
            });
        }
        return token08;
    }

    private JButton initStartGameButton() {
        if (startGameButton == null) {
            startGameButton = new JButton();
            startGameButton.setText("Start Game");
            startGameButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    startGameActionActionPerformed(event);
                }
            });
        }
        return startGameButton;
    }

    private JButton initBackButton() {
        if (backButton == null) {
            backButton = new JButton();
            backButton.setText("Back");
            backButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    backActionActionPerformed(event);
                }
            });
        }
        return backButton;
    }

    private JLabel initBackgroundImage() {
        if (backgroundImage == null) {
            backgroundImage = new JLabel();
            backgroundImage.setIcon(new ImageIcon(getClass().getResource("/images/background/ChooseToken.jpg")));
        }
        return backgroundImage;
    }

    private static void installLnF() {
        try {
            String lnfClassname = PREFERRED_LOOK_AND_FEEL;
            if (lnfClassname == null) lnfClassname = UIManager.getCrossPlatformLookAndFeelClassName();
            UIManager.setLookAndFeel(lnfClassname);
        } catch (Exception e) {
            System.err.println("Cannot install " + PREFERRED_LOOK_AND_FEEL + " on this platform:" + e.getMessage());
        }
    }

    /**
	 * Main entry of the class.
	 * Note: This class is only created so that you can easily preview the result at runtime.
	 * It is not expected to be managed by the designer.
	 * You can modify it as you like.
	 */
    public static void main(String[] args) {
        installLnF();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ChooseToken frame = new ChooseToken();
                Index index = new Index();
                frame.setDefaultCloseOperation(ChooseToken.EXIT_ON_CLOSE);
                frame.setTitle("ChooseToken");
                frame.getContentPane().setPreferredSize(frame.getSize());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                index.dispose();
            }
        });
    }

    public JButton getjButton0() {
        return this.token01;
    }

    public JButton getjButton1() {
        return this.token02;
    }

    public JButton getjButton2() {
        return this.token03;
    }

    public JButton getjButton3() {
        return this.token04;
    }

    public JButton getjButton4() {
        return this.token05;
    }

    public JButton getjButton5() {
        return this.token06;
    }

    public JButton getjButton6() {
        return this.token07;
    }

    public JButton getjButton7() {
        return this.token08;
    }

    public void setPlayer(Player player, int position) {
        this.players[position] = player;
    }

    public void setPlayerPos(int position, boolean playerPos) {
        this.playerPos[position] = playerPos;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public void openSubWindow(String image) {
        CreatePlayer createPlayer = new CreatePlayer(this, this.playerNum, this.players, image);
        createPlayer.setVisible(true);
    }

    public void buttonDisable(String button) {
        if (button == "01.png") this.token01.setEnabled(false);
        if (button == "08.png") this.token08.setEnabled(false);
        if (button == "05.png") this.token05.setEnabled(false);
        if (button == "02.png") this.token02.setEnabled(false);
        if (button == "07.png") this.token07.setEnabled(false);
        if (button == "06.png") this.token06.setEnabled(false);
        if (button == "03.png") this.token03.setEnabled(false);
        if (button == "04.png") this.token04.setEnabled(false);
    }

    public void rollDiceTurn() {
        boolean duplicate;
        diceSum = new int[playerNum];
        Dice dice = new Dice();
        for (int i = 0; i < playerNum; i++) {
            duplicate = true;
            while (duplicate) {
                duplicate = false;
                dice.rollDice();
                this.diceSum[i] = dice.getNumber(1) + dice.getNumber(2);
                if (playerNum > 1) {
                    for (int j = 0; j < i; j++) {
                        if (j != i && diceSum[j] == diceSum[i]) duplicate = true;
                    }
                }
            }
        }
        this.diceSumAlloc = true;
    }

    public void sortPlayersTurn() {
        Token tempT = new Token();
        Player tempP = new Player("test name", tempT);
        int tempN = 0;
        boolean exchangeMade = true;
        for (int i = 0; i < playerNum - 1 && exchangeMade; i++) {
            exchangeMade = false;
            for (int j = 0; j < playerNum - 1 - i; j++) {
                if (diceSum[j] < diceSum[j + 1]) {
                    tempP = players[j];
                    tempN = diceSum[j];
                    players[j] = players[j + 1];
                    diceSum[j] = diceSum[j + 1];
                    players[j + 1] = tempP;
                    diceSum[j + 1] = tempN;
                    exchangeMade = true;
                }
            }
        }
    }

    private void token01ActionActionPerformed(ActionEvent event) {
        if (playerPos[0] == false) openSubWindow("01.png");
    }

    private void token08ActionActionPerformed(ActionEvent event) {
        if (playerPos[7] == false) openSubWindow("08.png");
    }

    private void token05ActionActionPerformed(ActionEvent event) {
        if (playerPos[4] == false) openSubWindow("05.png");
    }

    private void token02ActionActionPerformed(ActionEvent event) {
        if (playerPos[1] == false) openSubWindow("02.png");
    }

    private void token07ActionActionPerformed(ActionEvent event) {
        if (playerPos[6] == false) openSubWindow("07.png");
    }

    private void token06ActionActionPerformed(ActionEvent event) {
        if (playerPos[5] == false) openSubWindow("06.png");
    }

    private void token03ActionActionPerformed(ActionEvent event) {
        if (playerPos[2] == false) openSubWindow("03.png");
    }

    private void token04ActionActionPerformed(ActionEvent event) {
        if (playerPos[3] == false) openSubWindow("04.png");
    }

    private void startGameActionActionPerformed(ActionEvent event) {
        if (this.playerNum > 0) {
            rollDiceTurn();
            sortPlayersTurn();
            BoardFrame boardFrame = new BoardFrame(players, playerNum);
            boardFrame.setupPlayers();
            this.boardMode.setBoardFrame(boardFrame);
            boardFrame.setBoardMode(this.boardMode);
            boardFrame.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(null, "Ooops!!! \nYou have not yet chosen a token to be used. \nPlease select a token and create a player \nbefore starting the game.", "Ooops!!!", JOptionPane.PLAIN_MESSAGE);
        }
    }

    private void backActionActionPerformed(ActionEvent event) {
        Index index = new Index();
        index.setVisible(true);
        this.dispose();
    }
}
