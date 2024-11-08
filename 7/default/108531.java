import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

class card {

    public char m_type;

    public char m_color;

    public int m_quantity;
}

public class Deck {

    private card[] m_data;

    private int m_size;

    private int m_length;

    private int m_numOfCards;

    private int m_lastCard;

    public Deck() {
        m_data = null;
        m_size = 0;
        m_length = 0;
        m_numOfCards = 0;
    }

    public Deck(String a_filename) throws IOException {
        m_data = null;
        m_size = 0;
        m_length = 0;
        m_numOfCards = 0;
        FileReader f = new FileReader(a_filename);
        load(f);
    }

    public Deck(Deck other) {
        m_length = other.m_length;
        m_data = new card[m_length];
        for (int r = 0; r < m_length; ++r) {
            m_data[r] = new card();
            m_data[r].m_type = '0';
            m_data[r].m_color = '0';
            m_data[r].m_quantity = 0;
        }
        m_size = m_numOfCards = m_lastCard = 0;
    }

    public boolean load(String a_filename) {
        try {
            FileReader f = new FileReader(a_filename);
            load(f);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void loadMcNasty() {
        m_length = m_size = 54;
        m_data = new card[m_length];
        for (int i = 0; i < m_length; ++i) {
            m_data[i] = new card();
            if (i == 0 || i == 13 || i == 26 || i == 39) {
                m_data[i].m_type = '0';
                m_data[i].m_quantity = 1;
            } else if (i == 1 || i == 14 || i == 27 || i == 40) {
                m_data[i].m_type = '1';
                m_data[i].m_quantity = 2;
            } else if (i == 2 || i == 15 || i == 28 || i == 41) {
                m_data[i].m_type = '2';
                m_data[i].m_quantity = 2;
            } else if (i == 3 || i == 16 || i == 29 || i == 42) {
                m_data[i].m_type = '3';
                m_data[i].m_quantity = 2;
            } else if (i == 4 || i == 17 || i == 30 || i == 43) {
                m_data[i].m_type = '4';
                m_data[i].m_quantity = 2;
            } else if (i == 5 || i == 18 || i == 31 || i == 44) {
                m_data[i].m_type = '5';
                m_data[i].m_quantity = 2;
            } else if (i == 6 || i == 19 || i == 32 || i == 45) {
                m_data[i].m_type = '6';
                m_data[i].m_quantity = 2;
            } else if (i == 7 || i == 20 || i == 33 || i == 46) {
                m_data[i].m_type = '7';
                m_data[i].m_quantity = 2;
            } else if (i == 8 || i == 21 || i == 34 || i == 47) {
                m_data[i].m_type = '8';
                m_data[i].m_quantity = 2;
            } else if (i == 9 || i == 22 || i == 35 || i == 48) {
                m_data[i].m_type = '9';
                m_data[i].m_quantity = 2;
            } else if (i == 10 || i == 23 || i == 36 || i == 49) {
                m_data[i].m_type = 'D';
                m_data[i].m_quantity = 2;
            } else if (i == 11 || i == 24 || i == 37 || i == 50) {
                m_data[i].m_type = 'R';
                m_data[i].m_quantity = 2;
            } else if (i == 12 || i == 25 || i == 38 || i == 51) {
                m_data[i].m_type = 'S';
                m_data[i].m_quantity = 2;
            } else if (i == 52) {
                m_data[i].m_type = '0';
                m_data[i].m_quantity = 4;
            } else if (i == 53) {
                m_data[i].m_type = '4';
                m_data[i].m_quantity = 4;
            }
            if (i < 13) {
                m_data[i].m_color = 'B';
            } else if (i < 26) {
                m_data[i].m_color = 'G';
            } else if (i < 39) {
                m_data[i].m_color = 'R';
            } else if (i < 52) {
                m_data[i].m_color = 'Y';
            } else {
                m_data[i].m_color = 'W';
            }
            if (i > 10) {
            }
            m_numOfCards += m_data[i].m_quantity;
        }
    }

    public void load(InputStreamReader f) throws IOException {
        m_length = readNextInt(f);
        m_size = m_length;
        m_data = new card[m_length];
        for (int r = 0; r < m_length; ++r) {
            m_data[r] = new card();
            m_data[r].m_type = (char) ignoreEndline(f);
            m_data[r].m_color = (char) ignoreEndline(f);
            m_data[r].m_quantity = readNextInt(f);
            m_numOfCards += m_data[r].m_quantity;
        }
    }

    private static int ignoreEndline(InputStreamReader f) throws IOException {
        int c;
        boolean whitespaceRead = true;
        do {
            c = f.read();
            switch(c) {
                case '\n':
                case '\r':
                    whitespaceRead = true;
                    break;
                default:
                    whitespaceRead = false;
            }
        } while (whitespaceRead);
        return c;
    }

    private static int readNextInt(InputStreamReader f) throws IOException {
        String s = readNextToken(f).toString();
        return Integer.parseInt(s);
    }

    private static StringBuffer readNextToken(InputStreamReader f) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        boolean whitespaceRead = false;
        do {
            c = f.read();
            switch(c) {
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                case '\b':
                    whitespaceRead = true;
                    break;
                default:
                    sb.append((char) c);
            }
        } while (sb.length() == 0 || !whitespaceRead);
        return sb;
    }

    public void load(Deck other) {
        m_length = other.m_length;
        m_data = new card[m_length];
        for (int r = 0; r < m_length; ++r) {
            m_data[r].m_type = '0';
            m_data[r].m_color = '0';
            m_data[r].m_quantity = 0;
        }
        m_size = m_numOfCards = m_lastCard = 0;
    }

    public void calcNumOfCards() {
        int total = 0;
        for (int r = 0; r < m_length; ++r) {
            total += m_data[r].m_quantity;
        }
        m_numOfCards = total;
    }

    public void calcSize() {
        int size = 0;
        for (int r = 0; r < m_length; ++r) {
            if (m_data[r].m_quantity > 0) {
                ++size;
            }
        }
        m_size = size;
    }

    public int getLength() {
        return m_length;
    }

    public int getSize() {
        calcSize();
        return m_size;
    }

    public int getNumOfCards() {
        calcNumOfCards();
        return m_numOfCards;
    }

    public char getTypeAt(int row) {
        return m_data[row].m_type;
    }

    public char getColorAt(int row) {
        return m_data[row].m_color;
    }

    public int getQuantityAt(int row) {
        return m_data[row].m_quantity;
    }

    public boolean drawCard(Deck other) {
        if (getNumOfCards() > 0) {
            Random rand = new Random();
            int card = rand.nextInt(getSize());
            int check = other.getNumOfCards();
            addCardToOther(card, other);
            if (other.getNumOfCards() > check + 1) {
                System.out.println("------------FAIL!!!!!!-------------");
            }
            if (m_data[card].m_quantity == 0) {
                moveToEnd(card);
            }
            return true;
        }
        return false;
    }

    public void addCardToOther(int card, Deck other) {
        boolean isAdded = false;
        other.calcSize();
        for (int r = 0; r < other.m_size; ++r) {
            if (other.m_data[r].m_type == m_data[card].m_type && other.m_data[r].m_color == m_data[card].m_color) {
                other.m_data[r].m_quantity++;
                m_data[card].m_quantity--;
                isAdded = true;
                other.m_lastCard = r;
            }
        }
        if (!isAdded) {
            other.m_data[other.m_size].m_type = m_data[card].m_type;
            other.m_data[other.m_size].m_color = m_data[card].m_color;
            other.m_data[other.m_size].m_quantity = 1;
            m_data[card].m_quantity--;
            other.m_lastCard = other.m_size;
        }
    }

    public void moveToEnd(int loc) {
        card temp = new card();
        temp.m_type = m_data[loc].m_type;
        temp.m_color = m_data[loc].m_color;
        for (int r = loc; r < m_length - 1; ++r) {
            m_data[r] = m_data[r + 1];
        }
        m_data[m_data.length - 1].m_type = temp.m_type;
        m_data[m_data.length - 1].m_color = temp.m_color;
        m_data[m_data.length - 1].m_quantity = 0;
    }

    public void shuffle(Deck other) {
        other.calcSize();
        for (int r = 0; r < other.m_size; ++r) {
            while (other.getQuantityAt(r) > 0) {
                addOtherToThis(r, other);
            }
            other.debug();
        }
    }

    public void addOtherToThis(int card, Deck other) {
        boolean isAdded = false;
        calcSize();
        for (int r = 0; r < getSize(); ++r) {
            if (m_data[r].m_type == other.m_data[card].m_type && m_data[r].m_color == other.m_data[card].m_color) {
                m_data[r].m_quantity++;
                other.m_data[card].m_quantity--;
                isAdded = true;
            }
        }
        if (!isAdded) {
            m_data[m_size].m_type = other.m_data[card].m_type;
            m_data[m_size].m_color = other.m_data[card].m_color;
            m_data[m_size].m_quantity = 1;
            other.m_data[card].m_quantity--;
        }
    }

    public int getLastCard() {
        return m_lastCard;
    }

    public void addFromHandToPile(int card, Deck other) {
        addCardToOther(card, other);
        if (getQuantityAt(card) == 0) {
            moveToEnd(card);
        }
    }

    public void shuffleMinusOne(int card, Deck other) {
        other.calcSize();
        for (int r = 0; r < other.m_size; ++r) {
            if (r != card) {
                while (other.getQuantityAt(r) > 0) {
                    addOtherToThis(r, other);
                }
            } else {
                while (other.getQuantityAt(r) > 1) {
                    addOtherToThis(r, other);
                }
            }
        }
        other.m_data[0].m_type = other.m_data[card].m_type;
        other.m_data[0].m_color = other.m_data[card].m_color;
        other.m_data[0].m_quantity = 1;
        other.m_data[card].m_quantity = 0;
        other.m_lastCard = 0;
    }

    public void debug() {
        System.out.println("DEBUG: \nSize: " + getSize() + "  Card#: " + getNumOfCards() + "\n");
        for (int r = 0; r < m_size; ++r) {
            System.out.println("Type: " + m_data[r].m_type + " Color: " + m_data[r].m_color + "Quantity: " + m_data[r].m_quantity + "\n");
        }
    }

    public void add(int loc, char a_type, char a_color) {
        m_data[loc].m_type = a_type;
        m_data[loc].m_color = a_color;
    }

    public int getLocation(char a_type, char a_color) {
        int val = -1;
        for (int n = 0; n < m_length; ++n) {
            if (m_data[n].m_type == a_type && m_data[n].m_color == a_color) {
                val = n;
            }
        }
        return val;
    }

    public void clearQuantity() {
        for (int r = 0; r < m_length; ++r) {
            m_data[r].m_quantity = 0;
        }
    }

    public void clearAll() {
        m_size = 0;
        m_numOfCards = 0;
        for (int r = 0; r < m_length; ++r) {
            m_data[r].m_type = '0';
            m_data[r].m_color = '0';
            m_data[r].m_quantity = 0;
        }
    }

    public Deck storeCopy() {
        Deck temp = new Deck(this);
        for (int i = 0; i < m_length; ++i) {
            temp.m_data[i].m_color = m_data[i].m_color;
            temp.m_data[i].m_type = m_data[i].m_type;
            temp.m_data[i].m_quantity = m_data[i].m_quantity;
        }
        return temp;
    }

    public void addCopy(Deck other) {
        for (int i = 0; i < m_length; ++i) {
            m_data[i].m_color = other.m_data[i].m_color;
            m_data[i].m_type = other.m_data[i].m_type;
            m_data[i].m_quantity = other.m_data[i].m_quantity;
        }
    }
}
