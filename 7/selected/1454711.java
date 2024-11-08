package br.usp.semantico;

import br.usp.estrutura.Pilha;
import br.usp.lexico.Simbolo;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 * @author Bruno Grisi
 */
public class Semantico {

    Memoria memoria;

    Hashtable tabelaSimbolos;

    private Pilha pilhaIDAtual;

    private Pilha pilhaConstantes;

    private Pilha pilhaVariaveis;

    private Pilha pilhaOperandos;

    private Pilha pilhaOperadores;

    private StringWriter saidaASM;

    private int erroSintatico;

    public Semantico(Hashtable tabelaSimbolos) {
        this.memoria = new Memoria();
        this.pilhaIDAtual = new Pilha();
        this.pilhaConstantes = new Pilha();
        this.pilhaVariaveis = new Pilha();
        this.pilhaOperadores = new Pilha();
        this.pilhaOperandos = new Pilha();
        this.tabelaSimbolos = tabelaSimbolos;
        this.saidaASM = new StringWriter();
        this.erroSintatico = 0;
    }

    public void Inicializar() {
        this.saidaASM.write(" " + "@" + " " + "=0 ; area do programa ");
        this.saidaASM.write("\n");
    }

    public boolean DeclaracaoID(Simbolo token) {
        if (token.getDeclarado()) return false;
        token.setDeclarado();
        token.setCategoria(Simbolo.VARIAVEL);
        this.pilhaVariaveis.empilha(token);
        return true;
    }

    public boolean AtribuicaoID(Simbolo token) {
        if (this.tabelaSimbolos.containsKey(token.getNome())) {
            Simbolo simbolo = (Simbolo) this.tabelaSimbolos.get(token.getNome());
            if (!simbolo.getDeclarado()) {
                System.out.printf("Erro no Semantico: Identificador " + token.getNome() + " nao foi declarado! \n");
                return false;
            }
            if (!simbolo.getCategoria().equals(Simbolo.VARIAVEL)) {
                System.out.printf("Erro no Semantico: Identificador " + token.getNome() + " nao eh uma variável! \n");
                return false;
            }
        }
        this.pilhaIDAtual.empilha(token.getNome());
        return true;
    }

    public boolean AtribuicaoGeraCodigo() {
        String identificadorNome = (String) this.pilhaIDAtual.desempilha();
        Simbolo identificador = (Simbolo) this.tabelaSimbolos.get(identificadorNome);
        String nome = identificador.getNome() + identificador.getCodigo();
        int valor = 0000;
        try {
            valor = Integer.parseInt(((Simbolo) pilhaOperandos.desempilha()).getNome());
        } catch (Exception e) {
            System.out.printf("Erro no Semantico: expressao nao calculada corretamente! \n");
        }
        this.saidaASM.write(" " + "LD" + " " + "=" + valor + " ;");
        this.saidaASM.write("\n");
        this.saidaASM.write(" " + "MM" + " " + nome + " ;");
        this.saidaASM.write("\n");
        return true;
    }

    public boolean ExpressaoEmpilhaOperandos(Simbolo token) {
        this.pilhaOperandos.empilha(token);
        return true;
    }

    public boolean ExpressaoEmpilhaOperadores(Simbolo token) {
        this.pilhaOperadores.empilha(token);
        return true;
    }

    public boolean ExpressaoX5() {
        if (((Simbolo) this.pilhaOperadores.getTopo()).getNome().equals(Token.ABRE_PARENTESES)) {
            this.pilhaOperadores.desempilha();
        } else {
            this.ExpressaoGeraCodigo();
        }
        return true;
    }

    public boolean ExpressaoX6() {
        String simboloNome = ((Simbolo) this.pilhaOperadores.getTopo()).getNome();
        if (simboloNome.equals(Token.MAIS) || simboloNome.equals(Token.MENOS) || simboloNome.equals(Token.ASTERISCO) || simboloNome.equals(Token.BARRA)) {
            this.ExpressaoGeraCodigo();
        } else {
            this.pilhaOperadores.empilha(Token.MAIS);
        }
        return true;
    }

    public boolean ExpressaoX7() {
        String simboloNome = ((Simbolo) this.pilhaOperadores.getTopo()).getNome();
        if (simboloNome.equals(Token.MAIS) || simboloNome.equals(Token.MENOS) || simboloNome.equals(Token.ASTERISCO) || simboloNome.equals(Token.BARRA)) {
            this.ExpressaoGeraCodigo();
        } else {
            this.pilhaOperadores.empilha(Token.MENOS);
        }
        return true;
    }

    public boolean ExpressaoX8() {
        String simboloNome = ((Simbolo) this.pilhaOperadores.getTopo()).getNome();
        if (simboloNome.equals(Token.ASTERISCO) || simboloNome.equals(Token.BARRA)) {
            this.ExpressaoGeraCodigo();
        } else {
            this.pilhaOperadores.empilha(Token.ASTERISCO);
        }
        return true;
    }

    public boolean ExpressaoX9() {
        String simboloNome = ((Simbolo) this.pilhaOperadores.getTopo()).getNome();
        if (simboloNome.equals(Token.ASTERISCO) || simboloNome.equals(Token.BARRA)) {
            this.ExpressaoGeraCodigo();
        } else {
            this.pilhaOperadores.empilha(Token.BARRA);
        }
        return true;
    }

    public boolean ExpressaoX10() {
        String simboloNome = ((Simbolo) this.pilhaOperadores.getTopo()).getNome();
        if (!simboloNome.equals("")) {
            this.ExpressaoGeraCodigo();
        }
        return true;
    }

    public boolean SaidaExpressao() {
        String identificadorNome = (String) this.pilhaIDAtual.desempilha();
        Simbolo output = (Simbolo) this.tabelaSimbolos.get(identificadorNome);
        this.saidaASM.write(" " + "LD" + " " + output.getNome() + " ;");
        this.saidaASM.write("\n");
        this.saidaASM.write(" " + "PD" + " " + "=1 ;");
        this.saidaASM.write("\n");
        return true;
    }

    public void Encerrar() {
        this.saidaASM.write(" " + "HM" + " " + "/0 ;");
        this.saidaASM.write("\n");
        this.saidaASM.write(" " + "#" + " " + "/0 ;");
        this.saidaASM.write("\n");
        this.saidaASM.write(" " + "@" + " " + "=2000 ; area de dados");
        this.saidaASM.write("\n");
        incluirConstantesPadrao();
        int i = 0;
        while (!pilhaConstantes.pilhaVazia()) {
            int valor;
            valor = ((Integer) pilhaConstantes.desempilha());
            this.saidaASM.write("C" + i + " " + "K" + " " + "=" + valor + " ;");
            this.saidaASM.write("\n");
            i++;
        }
        while (!pilhaVariaveis.pilhaVazia()) {
            Simbolo token;
            String nome;
            int valor = 0000;
            token = (Simbolo) pilhaVariaveis.desempilha();
            nome = token.getNome() + token.getCodigo();
            if (token.getCategoria().equals(Simbolo.VETOR)) {
                this.saidaASM.write("P" + nome + " " + "K" + " " + nome + " ;");
                this.saidaASM.write("\n");
            }
            this.saidaASM.write(nome + " " + "K" + " " + "=" + valor + " ;");
            this.saidaASM.write("\n");
        }
    }

    public void ExpressaoGeraCodigo() {
        String operador, operando1, operando2;
        operador = operando1 = operando2 = "";
        operador = ((Simbolo) this.pilhaOperadores.desempilha()).getNome();
        operando1 = ((Simbolo) this.pilhaOperandos.desempilha()).getNome();
        operando2 = ((Simbolo) this.pilhaOperandos.desempilha()).getNome();
        this.saidaASM.write(" " + "LD" + " " + operando2 + " ;");
        this.saidaASM.write("\n");
        if (operador.equals(Token.MAIS)) {
            this.saidaASM.write(" " + "+ " + " " + operando1 + " ;");
            this.saidaASM.write("\n");
        } else if (operador.equals(Token.MENOS)) {
            this.saidaASM.write(" " + "- " + " " + operando1 + " ;");
            this.saidaASM.write("\n");
        } else if (operador.equals(Token.ASTERISCO)) {
            this.saidaASM.write(" " + "* " + " " + operando1 + " ;");
            this.saidaASM.write("\n");
        } else if (operador.equals(Token.BARRA)) {
            this.saidaASM.write(" " + "/ " + " " + operando1 + " ;");
            this.saidaASM.write("\n");
        }
    }

    public void incluirConstantesPadrao() {
        this.saidaASM.write("true" + " " + "K" + " " + "=0001" + " ;");
        this.saidaASM.write("\n");
        this.saidaASM.write("false" + " " + "K" + " " + "=0000" + " ;");
        this.saidaASM.write("\n");
    }

    public Pilha getPilhaConstantes() {
        return pilhaConstantes;
    }

    public void setPilhaConstantes(Pilha pilhaConstantes) {
        this.pilhaConstantes = pilhaConstantes;
    }

    public Pilha getPilhaVariaveis() {
        return pilhaVariaveis;
    }

    public void setPilhaVariaveis(Pilha pilhaVariaveis) {
        this.pilhaVariaveis = pilhaVariaveis;
    }

    public Pilha getPilhaOperandos() {
        return pilhaOperandos;
    }

    public void setPilhaOperandos(Pilha pilhaOperandos) {
        this.pilhaOperandos = pilhaOperandos;
    }

    public Pilha getPilhaOperadores() {
        return pilhaOperadores;
    }

    public void setPilhaOperadores(Pilha pilhaOperadores) {
        this.pilhaOperadores = pilhaOperadores;
    }

    public StringWriter getSaidaASM() {
        return saidaASM;
    }

    public void setSaidaASM(StringWriter saidaASM) {
        this.saidaASM = saidaASM;
    }

    public int getErroSintatico() {
        return erroSintatico;
    }

    public void setErroSintatico(int erroSintatico) {
        this.erroSintatico = erroSintatico;
    }

    private void incluirConstantes() {
        this.reservarEspacoMemoria("false");
        this.reservarEspacoMemoria("true");
        this.constanteParaAcumulador(0);
        this.acumuladorParaMemoria("false");
        this.constanteParaAcumulador(1);
        this.acumuladorParaMemoria("true");
    }

    private void resolverEnderecos() {
        Iterator it = memoria.iterator();
        int enderecoAtual = 0;
        while (it.hasNext()) {
            String enderecoAtualHexa = Conversor.int2Hex(enderecoAtual);
            ((PosicaoMemoria) it.next()).setEndereco(enderecoAtualHexa);
            enderecoAtual += 2;
        }
        it = memoria.iterator();
        while (it.hasNext()) {
            PosicaoMemoria posicaoAtual = (PosicaoMemoria) it.next();
            String rotuloOperando = posicaoAtual.getRotuloOperando();
            String operando;
            if (rotuloOperando == null) {
                operando = Conversor.int2Hex(Conversor.hex2Int(posicaoAtual.getEndereco()) + 2);
            } else {
                operando = memoria.getEnderecoDeRotulo(rotuloOperando);
            }
            if (operando == null) {
                operando = rotuloOperando;
            }
            posicaoAtual.setOperando(operando);
        }
        memoria.imprimirMVNes();
        memoria.imprimir();
    }

    private void reservarEspacoMemoria(String rotulo) {
        if (memoria.getEnderecoDeRotulo(rotulo) == null) memoria.inserir(new PosicaoMemoria(rotulo, "0", null));
    }

    private void desvioIncondicional(String rotuloDesvio) {
        memoria.inserir(new PosicaoMemoria("desvioIncondicional", MVN.desvioIncondicional(), rotuloDesvio));
    }

    private void chamadaSubRotina(String rotuloSubRotina) {
        memoria.inserir(new PosicaoMemoria("chamadaSubRotina", MVN.desvioParaSubprograma(), rotuloSubRotina));
    }

    private void retornoSubRotina(String rotuloSubRotina) {
        memoria.inserir(new PosicaoMemoria("retornoSubRotina", MVN.retornoDeSubPrograma(), rotuloSubRotina));
    }

    private void encerrarPrograma() {
        memoria.inserir(new PosicaoMemoria("fimDoPrograma", MVN.parada(), "fimDoPrograma"));
    }

    private void constanteParaAcumulador(int valorConstante) {
        memoria.inserir(new PosicaoMemoria("constanteParaACC", MVN.constanteParaAcumulador(), Conversor.int2Hex(valorConstante)));
    }

    private void memoriaParaAcumulador(String rotuloMemoria) {
        memoria.inserir(new PosicaoMemoria("memParaACC", MVN.memoriaParaAcumulador(), rotuloMemoria));
    }

    private void acumuladorParaMemoria(String rotuloMemoria) {
        memoria.inserir(new PosicaoMemoria("ACCparaMem", MVN.acumuladorParaMemoria(), rotuloMemoria));
    }

    private void soma(String rotuloParcela) {
        memoria.inserir(new PosicaoMemoria("soma", MVN.soma(), rotuloParcela));
    }

    private void subtracao(String rotuloSubtraendo) {
        memoria.inserir(new PosicaoMemoria("subtracao", MVN.subtracao(), rotuloSubtraendo));
    }

    private void divisao(String rotuloDivisor) {
        memoria.inserir(new PosicaoMemoria("divisao", MVN.divisao(), rotuloDivisor));
    }

    private void multiplicacao(String rotuloMultiplicador) {
        memoria.inserir(new PosicaoMemoria("mult", MVN.multiplicacao(), rotuloMultiplicador));
    }

    private void desvioACCZero(String rotuloDesvio) {
        memoria.inserir(new PosicaoMemoria("desvioACCZero", MVN.desvioAcumuladorZero(), rotuloDesvio));
    }

    private void saida() {
        memoria.inserir(new PosicaoMemoria("devmonitor0", MVN.saida(), "E100"));
    }

    private void entrada() {
        memoria.inserir(new PosicaoMemoria("devteclado0", MVN.entrada(), "D000"));
    }

    private void comparacaoIgual(String rotulo, String rotuloVarA, String rotuloVarB) {
        rotulo = rotulo + "-" + "compIgual" + rotuloVarA + "-" + rotuloVarB;
        memoria.inserir(new PosicaoMemoria(rotulo, MVN.memoriaParaAcumulador(), rotuloVarA));
        memoria.inserir(new PosicaoMemoria(rotulo + "2", MVN.subtracao(), rotuloVarB));
        memoria.inserir(new PosicaoMemoria(rotulo + "4", MVN.desvioAcumuladorZero(), rotulo + "10"));
        memoria.inserir(new PosicaoMemoria(rotulo + "6", MVN.constanteParaAcumulador(), "0"));
        memoria.inserir(new PosicaoMemoria(rotulo + "8", MVN.desvioIncondicional(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "10", MVN.constanteParaAcumulador(), "1"));
        memoria.inserir(new PosicaoMemoria(rotulo + "12", "0", null));
    }

    private void comparacaoDiferente(String rotulo, String rotuloVarA, String rotuloVarB) {
        rotulo = rotulo + "-" + "compDif" + rotuloVarA + "-" + rotuloVarB;
        memoria.inserir(new PosicaoMemoria(rotulo, MVN.memoriaParaAcumulador(), rotuloVarA));
        memoria.inserir(new PosicaoMemoria(rotulo + "2", MVN.subtracao(), rotuloVarB));
        memoria.inserir(new PosicaoMemoria(rotulo + "4", MVN.desvioAcumuladorZero(), rotulo + "10"));
        memoria.inserir(new PosicaoMemoria(rotulo + "6", MVN.constanteParaAcumulador(), "1"));
        memoria.inserir(new PosicaoMemoria(rotulo + "8", MVN.desvioIncondicional(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "10", MVN.constanteParaAcumulador(), "0"));
        memoria.inserir(new PosicaoMemoria(rotulo + "12", "0", null));
    }

    private void comparacaoMaior(String rotulo, String rotuloVarA, String rotuloVarB) {
        rotulo = rotulo + "-" + "compMaior" + rotuloVarA + "-" + rotuloVarB;
        memoria.inserir(new PosicaoMemoria(rotulo, MVN.memoriaParaAcumulador(), rotuloVarB));
        memoria.inserir(new PosicaoMemoria(rotulo + "2", MVN.subtracao(), rotuloVarA));
        memoria.inserir(new PosicaoMemoria(rotulo + "4", MVN.desvioAcumuladorNegativo(), rotulo + "10"));
        memoria.inserir(new PosicaoMemoria(rotulo + "6", MVN.constanteParaAcumulador(), "0"));
        memoria.inserir(new PosicaoMemoria(rotulo + "8", MVN.desvioIncondicional(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "10", MVN.constanteParaAcumulador(), "1"));
        memoria.inserir(new PosicaoMemoria(rotulo + "12", "0", null));
    }

    private void comparacaoMenor(String rotulo, String rotuloVarA, String rotuloVarB) {
        rotulo = rotulo + "-" + "compMenor" + rotuloVarA + "-" + rotuloVarB;
        memoria.inserir(new PosicaoMemoria(rotulo, MVN.memoriaParaAcumulador(), rotuloVarA));
        memoria.inserir(new PosicaoMemoria(rotulo + "2", MVN.subtracao(), rotuloVarB));
        memoria.inserir(new PosicaoMemoria(rotulo + "4", MVN.desvioAcumuladorNegativo(), rotulo + "10"));
        memoria.inserir(new PosicaoMemoria(rotulo + "6", MVN.constanteParaAcumulador(), "0"));
        memoria.inserir(new PosicaoMemoria(rotulo + "8", MVN.desvioIncondicional(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "10", MVN.constanteParaAcumulador(), "1"));
        memoria.inserir(new PosicaoMemoria(rotulo + "12", "0", null));
    }

    private void comparacaoMaiorOuIgual(String rotulo, String rotuloVarA, String rotuloVarB) {
        rotulo = rotulo + "-" + "compMaiorIgual" + rotuloVarA + "-" + rotuloVarB;
        memoria.inserir(new PosicaoMemoria(rotulo, MVN.memoriaParaAcumulador(), rotuloVarB));
        memoria.inserir(new PosicaoMemoria(rotulo + "2", MVN.subtracao(), rotuloVarA));
        memoria.inserir(new PosicaoMemoria(rotulo + "4", MVN.desvioAcumuladorNegativo(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "6", MVN.desvioAcumuladorZero(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "8", MVN.constanteParaAcumulador(), "0"));
        memoria.inserir(new PosicaoMemoria(rotulo + "10", MVN.desvioIncondicional(), rotulo + "14"));
        memoria.inserir(new PosicaoMemoria(rotulo + "12", MVN.constanteParaAcumulador(), "1"));
        memoria.inserir(new PosicaoMemoria(rotulo + "14", "0", null));
    }

    private void comparacaoMenorOuIgual(String rotulo, String rotuloVarA, String rotuloVarB) {
        rotulo = rotulo + "-" + "compMenorIgual" + rotuloVarA + "-" + rotuloVarB;
        memoria.inserir(new PosicaoMemoria(rotulo, MVN.memoriaParaAcumulador(), rotuloVarA));
        memoria.inserir(new PosicaoMemoria(rotulo + "2", MVN.subtracao(), rotuloVarB));
        memoria.inserir(new PosicaoMemoria(rotulo + "4", MVN.desvioAcumuladorNegativo(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "6", MVN.desvioAcumuladorZero(), rotulo + "12"));
        memoria.inserir(new PosicaoMemoria(rotulo + "8", MVN.constanteParaAcumulador(), "0"));
        memoria.inserir(new PosicaoMemoria(rotulo + "10", MVN.desvioIncondicional(), rotulo + "14"));
        memoria.inserir(new PosicaoMemoria(rotulo + "12", MVN.constanteParaAcumulador(), "1"));
        memoria.inserir(new PosicaoMemoria(rotulo + "14", "0", null));
    }

    public void declaracao(String sentenca[]) {
        for (int i = 0; i > sentenca.length; i++) System.out.println(">>>>>>>>>>>>>>>>> " + sentenca[i]);
        String rotulo = sentenca[0];
        this.reservarEspacoMemoria(rotulo);
    }

    public void declaracaoVetor(String sentenca[]) {
        for (int i = 0; i > sentenca.length; i++) System.out.println(">>>>>>>>>>>>>>>>> " + sentenca[i]);
        String rotulo = sentenca[0];
        int tamanho = Integer.parseInt(sentenca[1]);
        for (int i = 0; i < tamanho; i++) {
            this.reservarEspacoMemoria(rotulo + "-" + i);
        }
    }

    public void atribuicaoAritmetica(String sentenca[]) {
        String rotuloRecebe = sentenca[0];
        String expressao[] = new String[sentenca.length - 1];
        for (int i = 1; i < sentenca.length; i++) {
            expressao[i - 1] = new String(sentenca[i]);
        }
        expressaoAritmetica(expressao);
        this.acumuladorParaMemoria(rotuloRecebe);
    }

    public void atribuicaoAritmeticaVetor(String sentenca[]) {
        String rotuloVetorRecebe = sentenca[0];
        String posicaoVetor = sentenca[1];
        String expressao[] = new String[sentenca.length - 1];
        expressao[0] = new String(rotuloVetorRecebe + "-" + posicaoVetor);
        for (int i = 1; i < expressao.length; i++) {
            expressao[i] = sentenca[i + 1];
        }
        atribuicaoAritmetica(expressao);
    }

    public void atribuicaoBooleana(String sentenca[]) {
        String rotuloRecebe = sentenca[0];
        String expressao[] = new String[sentenca.length - 1];
        for (int i = 1; i < sentenca.length; i++) {
            expressao[i - 1] = new String(sentenca[i]);
        }
        expressaoBooleana(expressao);
        this.acumuladorParaMemoria(rotuloRecebe);
    }

    public void atribuicaoBooleanaVetor(String sentenca[]) {
        String rotuloVetorRecebe = sentenca[0];
        String posicaoVetor = sentenca[1];
        String expressao[] = new String[sentenca.length - 1];
        expressao[0] = new String(rotuloVetorRecebe + "-" + posicaoVetor);
        for (int i = 1; i < expressao.length; i++) {
            expressao[i] = sentenca[i + 1];
        }
        atribuicaoBooleana(expressao);
    }

    public void expressaoAritmetica(String expressaoNormal[]) {
        String[] expressao = Conversor.notacaoPolonesa(expressaoNormal);
        int pilha = 0;
        this.reservarEspacoMemoria("temp");
        this.reservarEspacoMemoria("temp" + pilha);
        for (int i = 0; i < expressao.length; i++) {
            String atual = expressao[i];
            if (ehNumero(atual)) {
                this.constanteParaAcumulador(Integer.valueOf(atual));
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            } else if (!atual.equals(Token.MAIS) && !atual.equals(Token.MENOS) && !atual.equals(Token.BARRA) && !atual.equals(Token.ASTERISCO)) {
                this.memoriaParaAcumulador(atual);
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            } else {
                pilha--;
                this.memoriaParaAcumulador("temp" + pilha);
                pilha--;
                if (atual.equals(Token.MAIS)) this.soma("temp" + pilha); else if (atual.equals(Token.MENOS)) {
                    this.acumuladorParaMemoria("temp");
                    this.memoriaParaAcumulador("temp" + pilha);
                    this.subtracao("temp");
                } else if (atual.equals(Token.BARRA)) {
                    this.acumuladorParaMemoria("temp");
                    this.memoriaParaAcumulador("temp" + pilha);
                    this.divisao("temp");
                } else if (atual.equals(Token.ASTERISCO)) this.multiplicacao("temp" + pilha);
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            }
        }
    }

    public void expressaoBooleana(String expressaoNormal[]) {
        String[] expressao = Conversor.notacaoPolonesaBooleana(expressaoNormal);
        int pilha = 0;
        this.reservarEspacoMemoria("temp");
        this.reservarEspacoMemoria("temp" + pilha);
        for (int i = 0; i < expressao.length; i++) {
            String atual = expressao[i];
            if (ehNumero(atual)) {
                this.constanteParaAcumulador(Integer.parseInt(atual));
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            } else if (atual.equals("TRUE")) {
                this.constanteParaAcumulador(1);
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            } else if (atual.equals("FALSE")) {
                this.constanteParaAcumulador(0);
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            } else if (!atual.equals(Token.ECOMERCIAL_ECOMERCIAL) && !atual.equals(Token.BVERTICAL_BVERTICAL) && !atual.equals(Token.MAIOR) && !atual.equals(Token.MAIOR_IGUAL) && !atual.equals(Token.MENOR) && !atual.equals(Token.MENOR_IGUAL) && !atual.equals(Token.IGUAL_IGUAL) && !atual.equals(Token.EXCLAMACAO_IGUAL)) {
                this.memoriaParaAcumulador(atual);
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            } else {
                pilha--;
                this.memoriaParaAcumulador("temp" + pilha);
                pilha--;
                if (atual.equals(Token.ECOMERCIAL_ECOMERCIAL)) {
                    this.multiplicacao("temp" + pilha);
                } else if (atual.equals(Token.BVERTICAL_BVERTICAL)) {
                    this.soma("temp" + pilha);
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoDiferente("temp" + pilha, "temp", "false");
                } else if (atual.equals(Token.IGUAL_IGUAL)) {
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoIgual("temp" + pilha, "temp" + pilha, "temp");
                } else if (atual.equals(Token.EXCLAMACAO_IGUAL)) {
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoDiferente("temp" + pilha, "temp" + pilha, "temp");
                } else if (atual.equals(Token.MAIOR)) {
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoMaior("temp" + pilha, "temp" + pilha, "temp");
                } else if (atual.equals(Token.MENOR)) {
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoMenor("temp" + pilha, "temp" + pilha, "temp");
                } else if (atual.equals(Token.MAIOR_IGUAL)) {
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoMaiorOuIgual("temp" + pilha, "temp" + pilha, "temp");
                } else if (atual.equals(Token.MENOR_IGUAL)) {
                    this.acumuladorParaMemoria("temp");
                    this.comparacaoMenorOuIgual("temp" + pilha, "temp" + pilha, "temp");
                }
                this.acumuladorParaMemoria("temp" + pilha);
                pilha++;
                this.reservarEspacoMemoria("temp" + pilha);
            }
        }
    }

    public void comandoWhile(String sentenca[]) {
        String rotuloWhile = sentenca[0];
        String rotuloFim = rotuloWhile + "-fim";
        String rotuloComandos = rotuloWhile + "-comandos";
        this.reservarEspacoMemoria("temp");
        String expressaoBooleana[] = new String[sentenca.length - 1];
        for (int i = 1; i < sentenca.length; i++) {
            expressaoBooleana[i - 1] = sentenca[i];
        }
        this.reservarEspacoMemoria(rotuloWhile);
        this.expressaoBooleana(expressaoBooleana);
        this.acumuladorParaMemoria("temp");
        this.comparacaoIgual(rotuloWhile, "temp", "true");
        this.desvioACCZero(rotuloFim);
        this.reservarEspacoMemoria(rotuloComandos);
        this.desvioIncondicional(rotuloWhile);
        this.reservarEspacoMemoria(rotuloFim);
        memoria.setInserirEmSeguida(rotuloComandos);
    }

    public void comandoWhileFim() {
        memoria.resetInserirEmSeguida();
    }

    public void comandoIf(String sentenca[]) {
        String rotuloIf = sentenca[0];
        String rotuloFim = rotuloIf + "-fim";
        String rotuloElsif = rotuloIf + "-elsif";
        String rotuloElse = rotuloIf + "-else";
        String rotuloComandosIf = rotuloIf + "-comandos";
        this.reservarEspacoMemoria("temp");
        String expressaoBooleana[] = new String[sentenca.length - 1];
        for (int i = 1; i < sentenca.length; i++) {
            expressaoBooleana[i - 1] = sentenca[i];
        }
        this.reservarEspacoMemoria(rotuloIf);
        this.expressaoBooleana(expressaoBooleana);
        this.acumuladorParaMemoria("temp");
        this.comparacaoIgual(rotuloIf, "temp", "true");
        this.desvioACCZero(rotuloElsif);
        this.reservarEspacoMemoria(rotuloComandosIf);
        this.desvioIncondicional(rotuloFim);
        this.reservarEspacoMemoria(rotuloElsif);
        this.reservarEspacoMemoria(rotuloElse);
        this.reservarEspacoMemoria(rotuloFim);
        memoria.setInserirEmSeguida(rotuloComandosIf);
    }

    public void comandoElsif(String sentenca[]) {
        String rotuloIf = sentenca[0];
        String rotuloElsif = sentenca[1];
        String rotuloElse = rotuloIf + "-else";
        String rotuloComandosElsif = rotuloElsif + "-comandos";
        String rotuloFim = rotuloIf + "-fim";
        String expressaoBooleana[] = new String[sentenca.length - 2];
        for (int i = 2; i < sentenca.length; i++) {
            expressaoBooleana[i - 2] = sentenca[i];
        }
        memoria.resetInserirEmSeguida();
        memoria.setInserirEmSeguida(rotuloIf + "-elsif");
        this.reservarEspacoMemoria(rotuloElsif);
        this.expressaoBooleana(expressaoBooleana);
        this.acumuladorParaMemoria("temp");
        this.comparacaoIgual(rotuloElsif, "temp", "true");
        this.desvioACCZero(rotuloElse);
        this.reservarEspacoMemoria(rotuloComandosElsif);
        this.desvioIncondicional(rotuloFim);
        memoria.resetInserirEmSeguida();
        memoria.setInserirEmSeguida(rotuloComandosElsif);
    }

    public void comandoElse(String sentenca[]) {
        String rotuloElse = sentenca[0] + "-else";
        memoria.resetInserirEmSeguida();
        memoria.setInserirEmSeguida(rotuloElse);
    }

    public void comandoIfFim() {
        memoria.resetInserirEmSeguida();
    }

    public void comandoSaida(String variavel[]) {
        this.memoriaParaAcumulador(variavel[0]);
        this.saida();
    }

    public void comandoEntrada(String variavel[]) {
        this.entrada();
        this.acumuladorParaMemoria(variavel[0]);
    }

    public boolean ehNumero(String teste) {
        try {
            Integer.valueOf(teste);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
