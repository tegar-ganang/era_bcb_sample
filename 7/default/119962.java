public class BellmanFord {

    private int n = 0;

    private static No[] todosOsNos = new No[4];

    private static int[] nos = new int[4];

    private static int[] distancias = new int[12];

    private static void init() {
        String enlacesconfig = "1 2 3 2 4 10 2 3 3 3 4 2";
        String[] b = enlacesconfig.split(" ");
        for (int i = 0; i < b.length; i++) {
            distancias[i] = Integer.parseInt(b[i]);
        }
        int j = 0;
        for (int i = 0; i < distancias.length; i++) {
            if (i != 2 && i != 5 && i != 8 && i != 11 && !contains(nos, distancias[i])) {
                nos[j] = distancias[i];
                j++;
            }
        }
    }

    private static boolean contains(int[] vetor, int x) {
        for (int i = 0; i < vetor.length; i++) {
            if (vetor[i] == x) return true;
        }
        return false;
    }

    private static int[] getVizinhos(int id) {
        int[] vizinhosTemp = new int[3];
        int j = 0;
        for (int i = 0; i < distancias.length; i += 3) {
            if (id == distancias[i]) {
                vizinhosTemp[j] = distancias[i + 1];
                j++;
            } else if (id == distancias[i + 1]) {
                vizinhosTemp[j] = distancias[i];
                j++;
            }
        }
        int[] vizinhos = new int[j];
        for (int i = 0; i < j; i++) {
            vizinhos[i] = vizinhosTemp[i];
        }
        return vizinhos;
    }

    private static void atualiza(No noAtualizado, int[] vizinhosDoNo) {
        for (int i = 0; i < vizinhosDoNo.length; i++) {
            boolean atualizou = (todosOsNos[vizinhosDoNo[i] - 1]).atualiza(noAtualizado.id, noAtualizado.tabela);
            if (atualizou) {
                atualiza(todosOsNos[vizinhosDoNo[i] - 1], getVizinhos(vizinhosDoNo[i]));
            }
        }
    }

    public static void main(String[] args) {
        init();
        No no1 = new No(1, 1, 4, distancias);
        no1.print();
        todosOsNos[0] = no1;
        System.out.println();
        No no2 = new No(2, 3, 4, distancias);
        no2.print();
        todosOsNos[1] = no2;
        System.out.println();
        No no3 = new No(3, 2, 4, distancias);
        no3.print();
        todosOsNos[2] = no3;
        System.out.println();
        No no4 = new No(4, 2, 4, distancias);
        no4.print();
        todosOsNos[3] = no4;
        atualiza(no2, getVizinhos(no2.id));
        atualiza(no3, getVizinhos(no3.id));
        atualiza(no4, getVizinhos(no4.id));
        atualiza(no1, getVizinhos(no1.id));
        System.out.println();
        no2.print();
    }
}
