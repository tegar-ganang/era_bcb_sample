package org.etu;

import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

/**
 * Différentes fonctions servant à générer un sudoku complet, ou masqué, et à l'afficher.
 * @author David Barouh
 * 
 */
public class Sudoku {

    /**
	 * Constante de bonne insertion
	 */
    public static final int BON = 0;

    /**
	 * Constante de mauvaise insertion dans le carré
	 */
    public static final int CARRE = 1;

    /**
	 * Constante de mauvaise insertion dans la ligne
	 */
    public static final int LIGNE = 2;

    /**
	 * Constante de mauvaise insertion dans la colonne
	 */
    public static final int COLONNE = 3;

    /**
	 * Tableau du sudoku complet
	 */
    static int[][] sudoSoluce;

    /**
	 * Variable d'itération
	 */
    private final int NBITER = 100;

    /**
	 * Fonction main
	 * @param args
	 */
    public static void main(String[] args) {
        Sudoku s = new Sudoku();
        int[][] complet = new int[9][9];
        complet = s.genererGlobal();
        s.output(9, 9, complet);
    }

    /**
	 * Constructeur
	 *
	 */
    public Sudoku() {
        sudoSoluce = new int[9][9];
    }

    /**
	* Fonction génératrice du sudoku
	* @return int[][] tableau complet et évidé du sudoku (0 == " ")
	*/
    public int[][] genererGlobal() {
        int[][] base = new int[3][3];
        base = createBase();
        int[][] sudoku = new int[9][9];
        sudoku = decaleBlocs(base);
        sudoku = swapAleatoire(sudoku);
        sudoSoluce = sudoku;
        int[][] mask = createMask(sudoku);
        int[][] complet = sudokuMasquer(sudoku, mask);
        return complet;
    }

    /**
	 * Fonction createBase, pour créer le tableau 3x3 générant le reste.
	 * @return int[9][9]
	 */
    private int[][] createBase() {
        int[][] base = new int[3][3];
        Vector<Integer> v = new Vector<Integer>();
        Random r = new Random();
        while (v.size() != 9) {
            int i = r.nextInt(9) + 1;
            if (!v.contains(new Integer(i))) v.add(new Integer(i));
        }
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                base[x][y] = ((Integer) v.firstElement()).intValue();
                v.remove(new Integer(base[x][y]));
            }
        }
        return base;
    }

    /**
	 * Prend en paramètre le tableau 3x3 générateur, et renvoie la grille 9x9
	 * @deprecated La nouvelle version est mieux
	 * @param base
	 * @return int[9][9]
	 */
    @Deprecated
    private int[][] decaleBlocs2(int[][] base) {
        if (base.length != 3) System.exit(1);
        for (int i = 0; i < 3; i++) if (base[i].length != 3) System.exit(2);
        int[][] sudoku = new int[9][9];
        int[] gen = new int[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) gen[3 * i + j] = base[i][j];
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 9; k++) sudoku[3 * i + j][k] = gen[k];
                decaleListe(gen);
            }
            decaleMiniListe(gen);
        }
        return sudoku;
    }

    /**
	 * Prend en paramètre le tableau 3x3 générateur, et renvoie la grille 9x9 (2e version)
	 * @param base
	 * @return int[9][9]
	 */
    private int[][] decaleBlocs(int[][] base) {
        if (base.length != 3) System.exit(1);
        for (int i = 0; i < 3; i++) if (base[i].length != 3) System.exit(2);
        int[][] sudoku = new int[9][9];
        int[] gen = new int[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) gen[3 * i + j] = base[i][j];
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 9; k++) sudoku[3 * i + j][k] = gen[k];
                decaleListe(gen);
            }
            decaleMiniListe(gen);
        }
        return sudoku;
    }

    /**
	 * Echange des lignes et des colonnes aléatoirement
	 * @param sudoku
	 * @return sudoku mélangé
	 */
    private int[][] swapAleatoire(int[][] sudoku) {
        Random r = new Random();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < r.nextInt(3); k++) {
                    sudoku = swapMiniColonnes(sudoku, r.nextInt(3) * 3 + j, r.nextInt(3) * 3 + j, i);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            int j = r.nextInt(3) + 2;
            for (int k = 0; k < j; k++) {
                sudoku = swapLignes(sudoku, 3 * i + r.nextInt(3), 3 * i + r.nextInt(3));
            }
        }
        for (int i = 0; i < 3; i++) {
            int j = r.nextInt(3) + 2;
            for (int k = 0; k < j; k++) {
                sudoku = swapColonnes(sudoku, 3 * i + r.nextInt(3), 3 * i + r.nextInt(3));
            }
        }
        return sudoku;
    }

    /**
	 * Prend en parametre une grille solvable et retourne un booléen, si c'est solvable.
	 * @param solvable
	 * @return Valeur de solvabilité
	 */
    private boolean solver(Object[][][] solvable) {
        boolean change;
        do {
            change = false;
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (((Integer) solvable[i][j][0]).intValue() == 0) {
                        if (((Vector) solvable[i][j][1]).size() == 1) {
                            solvable[i][j][0] = new Integer(1);
                            solvable[i][j][1] = (Integer) ((Vector) solvable[i][j][1]).firstElement();
                            change = true;
                        } else {
                            for (Enumeration vect = ((Vector) solvable[i][j][1]).elements(); vect.hasMoreElements(); ) {
                                Integer temp = (Integer) vect.nextElement();
                                if (contient(solvable, i, j, temp.intValue())) {
                                    ((Vector) solvable[i][j][1]).remove((Object) temp);
                                    change = true;
                                }
                            }
                        }
                    }
                }
            }
            int somme = 0;
            for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) somme += ((Integer) solvable[i][j][0]).intValue();
            if (somme == 81) return true;
        } while (change);
        return false;
    }

    /**
	 * Prend en parametre un sudoku complet et renvoi la grille de masquage
	 * @param sudoku
	 * @return grille de masquage
	 */
    private int[][] createMask(int[][] sudoku) {
        int[][] masque = new int[9][9];
        Stack<Stack<Object>> parcours = new Stack<Stack<Object>>();
        Random r = new Random();
        int nombre = 0;
        Stack<Object> temp = new Stack<Object>();
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) masque[i][j] = 1;
        parcours.push(temp);
        while (nombre < NBITER) {
            int x = r.nextInt(9);
            int y = r.nextInt(9);
            int[] coordonnees = { x, y };
            if (!((Vector) parcours.peek()).contains(coordonnees)) {
                masque[x][y] = 0;
                parcours.peek().push(coordonnees);
                Object[][][] solvable = createSolvableFromComplete(sudoku, masque);
                boolean solve = solver(solvable);
                if (solve) {
                    Stack<Object> temp2 = new Stack<Object>();
                    parcours.push(temp2);
                    nombre++;
                } else {
                    masque[x][y] = 1;
                    if (((Vector) parcours.peek()).size() == 81) {
                        parcours.pop();
                        nombre--;
                    }
                }
            }
        }
        return masque;
    }

    /**
	 * Prend en paramètre une liste de longueur 9 et renvoi la liste décalé de 3
	 * @param liste
	 * @return liste décalé
	 */
    private int[] decaleListe(int[] liste) {
        if (liste.length != 9) System.exit(3);
        int[] miniliste = new int[3];
        for (int i = 0; i < 3; i++) miniliste[i] = liste[i];
        for (int i = 0; i < 6; i++) liste[i] = liste[i + 3];
        for (int i = 0; i < 3; i++) liste[i + 6] = miniliste[i];
        return liste;
    }

    /**
	 * Prend une liste de longueur 9 et renvoi une liste décalé légèrement dans les sous liste de 3
	 * @deprecated La nouvelle version est mieux
	 * @param liste
	 * @return liste un peu décalé
	 */
    @Deprecated
    private int[] decaleMiniListe2(int[] liste) {
        if (liste.length != 9) System.exit(4);
        int temp;
        for (int i = 0; i < 3; i++) {
            temp = liste[3 * i];
            liste[3 * i] = liste[3 * i + 1];
            liste[3 * i + 1] = liste[3 * i + 2];
            liste[3 * i + 2] = temp;
        }
        return liste;
    }

    /**
	 * Prend une liste de longueur 9 et renvoi une liste décalé légèrement dans les sous liste de 3 (pour la deuxi�me version du decaleBloc)
	 * @param liste
	 * @return liste un peu décalé
	 */
    private int[] decaleMiniListe(int[] liste) {
        if (liste.length != 9) System.exit(45);
        int temp;
        for (int i = 0; i < 8; i++) {
            temp = liste[i];
            liste[i] = liste[i + 1];
            liste[i + 1] = temp;
        }
        return liste;
    }

    /**
	 * Prend en parametre le sudoku et les lignes à échanger, et retourne le sudoku échangé
	 * @param sudoku
	 * @param m
	 * @param n
	 * @return sudoku
	 */
    private int[][] swapLignes(int[][] sudoku, int m, int n) {
        if (Math.abs(n - m) > 3) System.exit(5);
        int[] temp = new int[9];
        temp = sudoku[m];
        sudoku[m] = sudoku[n];
        sudoku[n] = temp;
        return sudoku;
    }

    /**
	 * Prend en parametre le sudoku et les colonnes à échanger, et retourne le sudoku
	 * @param sudoku
	 * @param m
	 * @param n
	 * @return sudoku
	 */
    private int[][] swapColonnes(int[][] sudoku, int m, int n) {
        if (Math.abs(n - m) > 3) System.exit(6);
        int[] temp = new int[9];
        for (int i = 0; i < 9; i++) temp[i] = sudoku[i][m];
        for (int i = 0; i < 9; i++) sudoku[i][m] = sudoku[i][n];
        for (int i = 0; i < 9; i++) sudoku[i][n] = temp[i];
        return sudoku;
    }

    /**
	 * Prend en parametre un sudoku, les deux colonnes à échanger, et le niveau de la mini-colonnes
	 * @param sudoku
	 * @param m
	 * @param n
	 * @param k
	 * @return sudoku mélangé en minicolonnes
	 */
    private int[][] swapMiniColonnes(int[][] sudoku, int m, int n, int k) {
        int[] temp = new int[3];
        for (int i = 0; i < 3; i++) {
            temp[i] = sudoku[(3 * k) + i][m];
        }
        for (int i = 0; i < 3; i++) {
            sudoku[(3 * k) + i][m] = sudoku[(3 * k) + i][n];
        }
        for (int i = 0; i < 3; i++) {
            sudoku[(3 * k) + i][n] = temp[i];
        }
        return sudoku;
    }

    /**
	 * Permet à partir d'une grille de sudoku complète et d'une grille de masquage d'obtenir
	 * une grille compréhensible par le solver
	 * @param sudoku
	 * @param masque
	 * @return Un tableau d'Objet en trois dimensions : les deux premières pour la position dans
	 * la grille, la troisième pour les infos d'affichage et les possibilités restantes.
	 */
    public Object[][][] createSolvableFromComplete(int[][] sudoku, int[][] masque) {
        Object[][][] solvable = new Object[9][9][2];
        for (int i = 0; i < sudoku.length; i++) {
            for (int j = 0; j < sudoku[i].length; j++) if (masque[i][j] == 0) {
                solvable[i][j][0] = new Integer(0);
                solvable[i][j][1] = new Vector();
                for (int k = 0; k < 9; k++) ((Vector) solvable[i][j][1]).add(new Integer(k + 1));
            } else {
                solvable[i][j][0] = new Integer(1);
                solvable[i][j][1] = new Integer(sudoku[i][j]);
            }
        }
        return solvable;
    }

    /**
	 * Prend en parametre une grille de sudoku solvable, et vérifie si placer n à la position (i,j) est absurde
	 * @param solvable
	 * @param i
	 * @param j
	 * @param n
	 * @return boolean : est absurde ?
	 */
    public boolean contient(Object[][][] solvable, int i, int j, int n) {
        for (int k = 0; k < solvable[i].length; k++) {
            if ((((Integer) solvable[i][k][0]).intValue() == 1) && (((Integer) solvable[i][k][1]).intValue() == n)) return true;
        }
        for (int k = 0; k < solvable.length; k++) {
            if ((((Integer) solvable[k][j][0]).intValue() == 1) && (((Integer) solvable[k][j][1]).intValue() == n)) return true;
        }
        int a = (3 * (i / 3));
        int b = (3 * (j / 3));
        for (int k = 0; k < 3; k++) {
            for (int l = 0; l < 3; l++) {
                if ((((Integer) solvable[k + a][l + b][0]).intValue() == 1) && (((Integer) solvable[k + a][l + b][1]).intValue() == n)) return true;
            }
        }
        return false;
    }

    /**
	 * Prend en parametre une grille de sudoku, et vérifie si placer n à la position (i,j) est absurde
	 * @param sudoku
	 * @param i
	 * @param j
	 * @param n
	 * @return boolean : est absurde ?
	 */
    public static int contientFromSudoku(int[][] sudoku, int i, int j, int n) {
        int a = (3 * (i / 3));
        int b = (3 * (j / 3));
        for (int k = 0; k < 3; k++) {
            for (int l = 0; l < 3; l++) {
                if (sudoku[a + k][b + l] == n) return CARRE;
            }
        }
        for (int k = 0; k < sudoku[i].length; k++) {
            if (sudoku[i][k] == n) return LIGNE;
        }
        for (int k = 0; k < sudoku.length; k++) {
            if (sudoku[k][j] == n) return COLONNE;
        }
        return BON;
    }

    /**
	 * Prend un sudoku et un masque en parametre et renvoie un sudoku à completer
	 * @param sudoku
	 * @param masque
	 * @return sudoku masqué
	 */
    private int[][] sudokuMasquer(int[][] sudoku, int[][] masque) {
        int[][] complet = new int[9][9];
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) complet[i][j] = sudoku[i][j] * masque[i][j];
        return complet;
    }

    /**
	 * Retourne le tableau de sudoku complet
	 * @return sudoku
	 */
    public int[][] getSudoComplet() {
        return sudoSoluce;
    }

    /**
	 * Prend en paramètre la taille du tableau de int à afficher et ce tableau
	 * @param i
	 * @param j
	 * @param affiche
	 */
    private void output(int i, int j, int[][] affiche) {
        for (int x = 0; x < i; x++) {
            System.out.print("[");
            for (int y = 0; y < j; y++) {
                if (affiche[x][y] == 0) {
                    System.out.print("[ ]");
                } else {
                    System.out.print("[" + affiche[x][y] + "]");
                }
            }
            System.out.println("]");
        }
    }
}
