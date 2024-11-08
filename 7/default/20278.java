import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

@SuppressWarnings("serial")
public class JKlotski extends JFrame {

    private JButton[] pieces;

    String startPattern;

    JPanel board;

    JPanel control;

    JTextArea text;

    JButton load_b;

    JButton solve_b;

    JButton next_b;

    JButton previous_b;

    JButton start_b;

    JButton stop_b;

    JLabel label;

    Game g;

    char curr;

    int x;

    int y;

    int pathPos;

    boolean autoShow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new JKlotski("2442\n2442\n2332\n2112\n1001\n").setVisible(true);
            }
        });
    }

    private String parse(String str) {
        final char[] trans2 = "bcdef".toCharArray();
        final char[] trans1 = "ghij".toCharArray();
        int index1 = 0;
        int index2 = 0;
        str = str.replaceAll("[\n\r]", "");
        char[] input = str.toCharArray();
        for (int i = 0; i < 20; i++) {
            switch(input[i]) {
                case '4':
                    input[i] = input[i + 1] = input[i + 4] = input[i + 5] = 'a';
                    break;
                case '1':
                    input[i] = trans1[index1++];
                    break;
                case '2':
                    input[i] = input[i + 4] = trans2[index2++];
                    break;
                case '3':
                    input[i] = input[i + 1] = (char) (trans2[index2++] - 'a' + 'A');
                    break;
                case '0':
                    input[i] = ' ';
                    break;
                default:
            }
        }
        return new String(input);
    }

    private void update() {
        char[] sID = g.getSpecialID().toCharArray();
        for (int i = 0; i < 20; i++) {
            if (sID[i] != '#') {
                switch(sID[i]) {
                    case 'a':
                        sID[i + 1] = sID[i + 4] = sID[i + 5] = '#';
                        ImageIcon a1 = new ImageIcon("images/a.gif");
                        pieces[i].setName("a");
                        pieces[i + 1].setName("a");
                        pieces[i + 4].setName("a");
                        pieces[i + 5].setName("a");
                        pieces[i].setBackground(chooseColor("a"));
                        pieces[i + 1].setBackground(chooseColor("a"));
                        pieces[i + 4].setBackground(chooseColor("a"));
                        pieces[i + 5].setBackground(chooseColor("a"));
                        pieces[i].setIcon(a1);
                        pieces[i + 1].setIcon(a1);
                        pieces[i + 4].setIcon(a1);
                        pieces[i + 5].setIcon(a1);
                        break;
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                        sID[i + 4] = '#';
                        ImageIcon b1 = new ImageIcon("images/b1.gif");
                        ImageIcon b2 = new ImageIcon("images/b2.gif");
                        pieces[i].setName("" + sID[i]);
                        pieces[i].setBackground(chooseColor("" + sID[i]));
                        pieces[i].setIcon(b1);
                        pieces[i + 4].setName("" + sID[i]);
                        pieces[i + 4].setBackground(chooseColor("" + sID[i]));
                        pieces[i + 4].setIcon(b2);
                        break;
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                        sID[i + 1] = '#';
                        ImageIcon e1 = new ImageIcon("images/e1.gif");
                        ImageIcon e2 = new ImageIcon("images/e2.gif");
                        pieces[i].setName("" + sID[i]);
                        pieces[i].setBackground(chooseColor("" + sID[i]));
                        pieces[i].setIcon(e1);
                        pieces[i + 1].setName("" + sID[i]);
                        pieces[i + 1].setBackground(chooseColor("" + sID[i]));
                        pieces[i + 1].setIcon(e2);
                        break;
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                        ImageIcon g = new ImageIcon("images/g.gif");
                        pieces[i].setName("" + sID[i]);
                        pieces[i].setBackground(chooseColor("" + sID[i]));
                        pieces[i].setIcon(g);
                        break;
                    default:
                        pieces[i].setIcon(null);
                        pieces[i].setName(" ");
                        pieces[i].setBackground(chooseColor(" "));
                }
                pieces[i].setName("" + sID[i]);
                pieces[i].setBackground(chooseColor("" + g.getSpecialID().charAt(i)));
            }
        }
    }

    private Color chooseColor(String str) {
        str = str.toLowerCase();
        char c = str.charAt(0);
        switch(c) {
            case 'a':
                return Color.RED;
            case 'b':
                return Color.CYAN;
            case 'c':
                return Color.GREEN;
            case 'd':
                return Color.ORANGE;
            case 'e':
                return Color.BLACK;
            case 'f':
                return Color.MAGENTA;
            case 'g':
                return Color.BLUE;
            case 'h':
                return Color.PINK;
            case 'i':
                return Color.GRAY;
            case 'j':
                return Color.YELLOW;
            default:
                return Color.WHITE;
        }
    }

    public JKlotski(String str) {
        startPattern = parse(str);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(800, 500);
        getContentPane().setLayout(new GridLayout(1, 2));
        board = new JPanel();
        board.setSize(400, 500);
        control = new JPanel();
        control.setSize(400, 500);
        add(board);
        add(control);
        control.setLayout(new GridLayout(5, 4));
        text = new JTextArea();
        text.setText(str);
        load_b = new JButton("Reload");
        solve_b = new JButton("Solve");
        next_b = new JButton("Next");
        previous_b = new JButton("Previous");
        start_b = new JButton("Start");
        stop_b = new JButton("Stop");
        next_b.setEnabled(false);
        previous_b.setEnabled(false);
        start_b.setEnabled(false);
        stop_b.setEnabled(false);
        label = new JLabel();
        control.add(text);
        control.add(load_b);
        control.add(solve_b);
        control.add(label);
        control.add(previous_b);
        control.add(next_b);
        control.add(start_b);
        control.add(stop_b);
        load_b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String str = text.getText().replaceAll("[\n\r]", "");
                if (str.matches("[0-4]*") && str.length() == 20) {
                    pathPos = 0;
                    Game.path.clear();
                    startPattern = parse(str);
                    g = new Game(startPattern);
                    update();
                    label.setText("Loaded!");
                    next_b.setEnabled(false);
                    previous_b.setEnabled(false);
                    start_b.setEnabled(false);
                    stop_b.setEnabled(false);
                } else {
                    label.setText("Wrong Format! Try again!");
                }
            }
        });
        solve_b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread rnb = new Thread() {

                    @Override
                    public void run() {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {

                                public void run() {
                                    solve_b.setEnabled(false);
                                }
                            });
                        } catch (InterruptedException ex) {
                            Logger.getLogger(JKlotski.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(JKlotski.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        boolean solvable = Game.search(g);
                        if (solvable) {
                            pathPos = 0;
                            startPattern = g.toString();
                            next_b.setEnabled(true);
                            previous_b.setEnabled(true);
                            start_b.setEnabled(true);
                            stop_b.setEnabled(true);
                            solve_b.setEnabled(true);
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    Game.path.addFirst(new Game(startPattern).toString());
                                    label.setText("Solved in " + (Game.path.size() - 1) + " steps.");
                                }
                            });
                        } else {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    label.setText("No solution!");
                                }
                            });
                        }
                    }
                };
                rnb.start();
            }
        });
        next_b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (pathPos >= 0 && pathPos < Game.path.size() - 1) {
                    g = new Game(Game.path.get(++pathPos));
                    update();
                }
            }
        });
        previous_b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (pathPos > 0 && pathPos < Game.path.size()) {
                    g = new Game(Game.path.get(--pathPos));
                    update();
                }
            }
        });
        start_b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                autoShow = true;
                Thread show = new Thread() {

                    @Override
                    public void run() {
                        while (autoShow && pathPos < Game.path.size() - 1) {
                            g = new Game(Game.path.get(++pathPos));
                            try {
                                SwingUtilities.invokeLater(new Runnable() {

                                    public void run() {
                                        update();
                                    }
                                });
                                Thread.sleep(200);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                };
                show.start();
            }
        });
        stop_b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                autoShow = false;
            }
        });
        MouseListener mouseListener = new MouseInputAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getButton() == MouseEvent.BUTTON1) {
                    curr = ((JButton) me.getComponent()).getName().charAt(0);
                    x = me.getX();
                    y = me.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                if (me.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int changeX = me.getX() - x;
                int changeY = me.getY() - y;
                D dir = D.E;
                if (changeX == 0 && changeY == 0) {
                    return;
                }
                if (Math.abs(changeX) >= Math.abs(changeY)) {
                    dir = changeX > 0 ? D.E : D.W;
                } else {
                    dir = changeY > 0 ? D.S : D.N;
                }
                Command cmd = new Command(curr, dir);
                if (g.isMovable(cmd)) {
                    g.move(cmd);
                    update();
                }
            }
        };
        g = new Game(startPattern);
        pieces = new JButton[20];
        board.setLayout(new GridLayout(5, 4));
        for (int i = 0; i < 20; i++) {
            pieces[i] = new JButton();
            pieces[i].addMouseListener(mouseListener);
            pieces[i].setEnabled(false);
            pieces[i].setBackground(chooseColor("" + startPattern.charAt(i)));
            pieces[i].setBorderPainted(false);
            pieces[i].setName("" + startPattern.charAt(i));
            board.add(pieces[i]);
        }
        update();
        Thread show = new Thread() {

            @Override
            public void run() {
                while (true) {
                    while (autoShow && pathPos < Game.path.size() - 1) {
                        g = new Game(Game.path.get(++pathPos));
                        try {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    update();
                                }
                            });
                            Thread.sleep(200);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };
        show.start();
    }
}

enum D {

    N, E, S, W
}

class Encoding {

    /**
	 * general encoding, same shape, same code e.g. abba abba aeea agga g##g
	 */
    String general;

    /**
	 * special encoding, same block, same code e.g. abbc abbc deef dghf i##j
	 */
    String special;

    /**
	 * the general encoding of parent. used in backtracking to output the path.
	 * assume that in the visited set, general/special is a one-to-one mapping.
	 * null means root.
	 */
    String parent;

    public Encoding(String g, String s, String p) {
        general = g;
        special = s;
        parent = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Encoding)) return false;
        Encoding ecd = (Encoding) o;
        return (this.general.equals(ecd.general));
    }

    @Override
    public int hashCode() {
        System.out.println(general.hashCode());
        return general.hashCode();
    }
}

class Command {

    char c;

    D d;

    public Command(char c, D d) {
        this.c = c;
        this.d = d;
    }

    public boolean isInverse(Command cmd) {
        if (c != cmd.c) {
            return false;
        }
        return (d == D.E && cmd.d == D.W) || (d == D.S && cmd.d == D.N) || (d == D.W && cmd.d == D.E) || (d == D.N && cmd.d == D.S);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Command) {
            Command cmd = (Command) o;
            return c == cmd.c && d == cmd.d;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }
}

class Game {

    static Map<String, Encoding> visited = new Hashtable<String, Encoding>();

    static LinkedList<Encoding> deque = new LinkedList<Encoding>();

    static LinkedList<String> path = new LinkedList<String>();

    /**
	 * Linear representation of the configuration. Easy for input and interface.
	 * May out of date after move().
	 */
    StringBuffer state;

    /**
	 * Matrix representation. Easy for internal computation. Derived from
	 * 'state'.
	 */
    char[][] board = new char[5][4];

    /**
	 * same as in Encoding, for backtracking. null means root.
	 */
    String parent = null;

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
	 * update the matrix representation using the state StringBuffer
	 */
    private void update() {
        board[0] = state.substring(0, 4).toCharArray();
        board[1] = state.substring(4, 8).toCharArray();
        board[2] = state.substring(8, 12).toCharArray();
        board[3] = state.substring(12, 16).toCharArray();
        board[4] = state.substring(16).toCharArray();
    }

    public Game(String str) {
        state = new StringBuffer(str);
        update();
    }

    /**
	 * Change the current configuration according to 'str'. Used for resuming
	 * the configuration.
	 * 
	 * @param str
	 */
    public void decode(Encoding ecd) {
        state = new StringBuffer(ecd.special);
        parent = ecd.parent;
        update();
    }

    /**
	 * Determines whether the given Command object is applicable to the current
	 * board configuration. This is done by checking whether all the sub-pieces
	 * labeled by cmd.c are movable, assuming they form a rectangle.
	 * 
	 * @param cmd
	 *            Attempting Command object (label name, direction)
	 * @return Return true when the block is movable.
	 */
    public boolean isMovable(Command cmd) {
        char c = cmd.c;
        D d = cmd.d;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 4; x++) {
                if (board[y][x] != c) {
                    continue;
                }
                if (d == D.N && y == 0) return false;
                if (d == D.E && x == 3) return false;
                if (d == D.S && y == 4) return false;
                if (d == D.W && x == 0) return false;
                int xx = x;
                int yy = y;
                switch(d) {
                    case N:
                        yy--;
                        break;
                    case E:
                        xx++;
                        break;
                    case S:
                        yy++;
                        break;
                    case W:
                        xx--;
                        break;
                }
                if (board[yy][xx] != ' ' && board[yy][xx] != c) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * Apply the given Command. Assuming it results a valid configuration. Must
	 * be called after calling isMovable() to verify that. This moves all the
	 * sub-pieces of a block.
	 * 
	 * Implemented this way, isMovable() is useless(), because invalid movement
	 * will result in the same configuration.
	 * 
	 * @param cmd
	 */
    public void move(Command cmd) {
        char c = cmd.c;
        D d = cmd.d;
        String pnt = getGeneralID();
        switch(d) {
            case E:
                for (int i = 0; i < 5; i++) {
                    for (int j = 3; j > 0; j--) {
                        if (board[i][j] == ' ' && board[i][j - 1] == c) {
                            board[i][j] = c;
                            board[i][j - 1] = ' ';
                        }
                    }
                }
                break;
            case S:
                for (int j = 0; j < 4; j++) {
                    for (int i = 4; i > 0; i--) {
                        if (board[i][j] == ' ' && board[i - 1][j] == c) {
                            board[i][j] = c;
                            board[i - 1][j] = ' ';
                        }
                    }
                }
                break;
            case W:
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (board[i][j] == ' ' && board[i][j + 1] == c) {
                            board[i][j] = c;
                            board[i][j + 1] = ' ';
                        }
                    }
                }
                break;
            case N:
                for (int j = 0; j < 4; j++) {
                    for (int i = 0; i < 4; i++) {
                        if (board[i][j] == ' ' && board[i + 1][j] == c) {
                            board[i][j] = c;
                            board[i + 1][j] = ' ';
                        }
                    }
                }
                break;
        }
        parent = pnt;
    }

    public String getSpecialID() {
        char[] s = new char[20];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                s[i * 4 + j] = board[i][j];
            }
        }
        return new String(s);
    }

    public String getGeneralID() {
        char[] s = new char[20];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                char c = board[i][j];
                switch(c) {
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                        c = 'b';
                        break;
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                        c = 'B';
                        break;
                    case 'h':
                    case 'i':
                    case 'j':
                        c = 'g';
                        break;
                    default:
                }
                s[i * 4 + j] = c;
            }
        }
        return new String(s);
    }

    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                str += board[i][j];
            }
        }
        return str;
    }

    private LinkedList<Encoding> getAllMoves() {
        LinkedList<Encoding> oneMove = new LinkedList<Encoding>();
        Encoding current = new Encoding(getGeneralID(), getSpecialID(), getParent());
        for (char c = 'a'; c <= 'j'; c++) {
            for (D d : D.values()) {
                Command cmd = new Command(c, d);
                if (isMovable(cmd)) {
                    move(cmd);
                    oneMove.addLast(new Encoding(getGeneralID(), getSpecialID(), getParent()));
                    decode(current);
                }
            }
        }
        for (char c = 'B'; c <= 'F'; c++) {
            for (D d : D.values()) {
                Command cmd = new Command(c, d);
                if (isMovable(cmd)) {
                    move(cmd);
                    oneMove.addLast(new Encoding(getGeneralID(), getSpecialID(), getParent()));
                    decode(current);
                }
            }
        }
        return oneMove;
    }

    private LinkedList<Encoding> getAllMoves2() {
        LinkedList<Encoding> twoMove = new LinkedList<Encoding>();
        Encoding current = new Encoding(getGeneralID(), getSpecialID(), getParent());
        Encoding afterOneMove = null;
        Encoding afterTwoMoves = null;
        for (char c = 'a'; c <= 'j'; c++) {
            for (D d : D.values()) {
                Command cmd = new Command(c, d);
                if (isMovable(cmd)) {
                    move(cmd);
                    afterOneMove = new Encoding(getGeneralID(), getSpecialID(), getParent());
                    for (D d2 : D.values()) {
                        cmd = new Command(c, d2);
                        if (isMovable(cmd)) {
                            move(cmd);
                            afterTwoMoves = new Encoding(getGeneralID(), getSpecialID(), current.general);
                            if (!current.equals(afterTwoMoves)) {
                                twoMove.addLast(afterTwoMoves);
                            }
                            decode(afterOneMove);
                        }
                    }
                    decode(current);
                }
            }
        }
        for (char c = 'B'; c <= 'F'; c++) {
            for (D d : D.values()) {
                Command cmd = new Command(c, d);
                if (isMovable(cmd)) {
                    move(cmd);
                    afterOneMove = new Encoding(getGeneralID(), getSpecialID(), getParent());
                    for (D d2 : D.values()) {
                        cmd = new Command(c, d2);
                        if (isMovable(cmd)) {
                            move(cmd);
                            afterTwoMoves = new Encoding(getGeneralID(), getSpecialID(), current.general);
                            if (!current.equals(afterTwoMoves)) {
                                twoMove.addLast(afterTwoMoves);
                            }
                            decode(afterOneMove);
                        }
                    }
                    decode(current);
                }
            }
        }
        return twoMove;
    }

    public static boolean search(Game g) {
        g.parent = null;
        deque.clear();
        path.clear();
        visited.clear();
        Encoding ecd = new Encoding(g.getGeneralID(), g.getSpecialID(), g.getParent());
        deque.addLast(ecd);
        visited.put(g.getGeneralID(), ecd);
        while (!deque.isEmpty()) {
            Encoding current = deque.pollFirst();
            g.decode(current);
            if (g.escaped()) {
                while (g.parent != null) {
                    path.addFirst(g.toString());
                    current = visited.get(g.parent);
                    g.decode(current);
                }
                return true;
            }
            LinkedList<Encoding> oneMoves = g.getAllMoves();
            ListIterator<Encoding> itr = oneMoves.listIterator();
            while (itr.hasNext()) {
                Encoding next = itr.next();
                if (!visited.containsKey(next.general)) {
                    deque.addLast(next);
                    visited.put(next.general, next);
                }
            }
            LinkedList<Encoding> twoMoves = g.getAllMoves2();
            ListIterator<Encoding> itr2 = twoMoves.listIterator();
            while (itr2.hasNext()) {
                Encoding next = itr2.next();
                if (!visited.containsKey(next.general)) {
                    deque.addLast(next);
                    visited.put(next.general, next);
                }
            }
        }
        return false;
    }

    public boolean escaped() {
        return (board[4][1] == 'a' && board[4][2] == 'a');
    }
}
