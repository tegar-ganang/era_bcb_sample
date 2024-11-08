import java.util.Vector;

public class Montador {

    public short[] memoria = new short[1024];

    public String[] instrucao = new String[1024];

    public int pos = 0;

    public Montador() {
    }

    public String adicionar(String operacao, String op1, String imed, String op2) {
        if (operacao.equals("")) return "Escolha a opera��o";
        if (op1.equals("imediato")) {
            if (imed.equals("")) return "Imediato vazio";
            try {
                Short.parseShort(imed);
            } catch (Exception e) {
                return "Imediato inv�lido";
            }
        }
        if (operacao.equals("ADD") || operacao.equals("SUB") || operacao.equals("MOV") || operacao.equals("CMP") || operacao.equals("AND") || operacao.equals("OR")) {
            if (op1.equals("") || op2.equals("")) return operacao + " precisa de 2 operandos";
        } else if (operacao.equals("NOT") || operacao.equals("CLR") || operacao.equals("NEG") || operacao.equals("SHL") || operacao.equals("SHR")) {
            if (op1.equals("")) return operacao + " precisa do primeiro operando"; else if (op1.equals("imediato")) return operacao + " n�o se aplica a um imediato";
            op2 = "";
        } else if (operacao.equals("BRZ") || operacao.equals("BRN") || operacao.equals("BRE") || operacao.equals("BRL") || operacao.equals("BRG") || operacao.equals("BRC") || operacao.equals("JMP")) {
            if (!op1.equals("imediato")) return operacao + " precisa de um imediato";
            op2 = "";
        } else if (operacao.equals("HALT")) {
            op1 = op2 = imed = "";
        }
        guardar(operacao, op1, imed, op2);
        return "Instru��o adicionada";
    }

    private void guardar(String operacao, String op1, String imed, String op2) {
        adicionarLinha(pos);
        if (op2.equals("")) instrucao[pos] = operacao + " " + op1; else if (op1.equals("")) instrucao[pos] = operacao; else instrucao[pos] = operacao + " " + op1 + ", " + op2;
        memoria[pos++] = (short) CODECInstrucao.codificar(operacao, op1, op2);
        if (op1.equals("imediato")) {
            adicionarLinha(pos);
            instrucao[pos] = "";
            memoria[pos++] = Short.parseShort(imed);
        }
    }

    public void adicionarLinha(int row) {
        for (int i = 1022; i >= row; i--) {
            memoria[i + 1] = memoria[i];
            instrucao[i + 1] = instrucao[i];
        }
        memoria[row] = 0;
        instrucao[row] = "";
    }

    public void removerLinha(int row) {
        for (int i = row; i < 1023; i++) {
            memoria[i] = memoria[i + 1];
            instrucao[i] = instrucao[i + 1];
        }
        memoria[1023] = 0;
        instrucao[1023] = "";
    }
}
