package sintatico;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import junit.framework.Assert;
import lexico.Lexico;
import lexico.TipoToken;
import lexico.Token;
import org.junit.Test;

public class Teste {

    private boolean teste(String res) throws Exception {
        InputStream in1 = Main.class.getResourceAsStream(res);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int i = -1;
        while ((i = in1.read(buf)) > -1) out.write(buf, 0, i);
        ByteArrayInputStream in2 = new ByteArrayInputStream(out.toByteArray());
        Reader r1 = new InputStreamReader(in2);
        Lexico lex = new Lexico();
        lex.setIgnoreComment(true);
        lex.setIgnoreSpace(true);
        List<Token> tokens = lex.analizar(r1);
        Variavel vE1 = new Variavel("E");
        Variavel vE2 = new Variavel("E'");
        Variavel vM1 = new Variavel("M");
        Variavel vM2 = new Variavel("M'");
        Variavel vP1 = new Variavel("P");
        Terminal plus = new Terminal(TipoToken.OPERATOR_PLUS);
        Terminal star = new Terminal(TipoToken.OPERATOR_STAR);
        Terminal open = new Terminal(TipoToken.OPERATOR_PAREN_OPEN);
        Terminal clse = new Terminal(TipoToken.OPERATOR_PAREN_CLOSE);
        Terminal trmn = new Terminal(TipoToken.LITERAL_NUMBER, TipoToken.LITERAL_STRING, TipoToken.LITERAL_CHAR, TipoToken.IDENTIFIER);
        Terminal term = new Terminal(TipoToken.OPERATOR_SEMICOLON);
        Producao p1 = new Producao(vE1, vM1, vE2);
        Producao p2 = new Producao(vE2, plus, vM1, vE2);
        Producao p3 = new Producao(vE2);
        Producao p4 = new Producao(vM1, vP1, vM2);
        Producao p5 = new Producao(vM2, star, vP1, vM2);
        Producao p6 = new Producao(vM2);
        Producao p7 = new Producao(vP1, open, vE1, clse);
        Producao p8 = new Producao(vP1, trmn);
        HashMap<Variavel, HashMap<Terminal, Producao>> tabela;
        tabela = new HashMap<Variavel, HashMap<Terminal, Producao>>();
        HashMap<Terminal, Producao> rowE1 = new HashMap<Terminal, Producao>();
        rowE1.put(open, p1);
        rowE1.put(trmn, p1);
        tabela.put(vE1, rowE1);
        HashMap<Terminal, Producao> rowE2 = new HashMap<Terminal, Producao>();
        rowE2.put(plus, p2);
        rowE2.put(clse, p3);
        rowE2.put(term, p3);
        tabela.put(vE2, rowE2);
        HashMap<Terminal, Producao> rowM1 = new HashMap<Terminal, Producao>();
        rowM1.put(open, p4);
        rowM1.put(trmn, p4);
        tabela.put(vM1, rowM1);
        HashMap<Terminal, Producao> rowM2 = new HashMap<Terminal, Producao>();
        rowM2.put(plus, p6);
        rowM2.put(star, p5);
        rowM2.put(clse, p6);
        rowM2.put(term, p6);
        tabela.put(vM2, rowM2);
        HashMap<Terminal, Producao> rowP1 = new HashMap<Terminal, Producao>();
        rowP1.put(open, p7);
        rowP1.put(trmn, p8);
        tabela.put(vP1, rowP1);
        Sintatico s = new Sintatico(new Variavel[] { vE1, vE2, vM1, vM2, vP1 }, new Terminal[] { plus, star, open, clse, trmn, term }, new Producao[] { p1, p2, p3, p4, p5, p6, p7, p8 }, vE1, term, tabela);
        return s.analisar(tokens);
    }

    @Test
    public void teste1() throws Exception {
        Assert.assertTrue(teste("expr1.txt"));
    }

    @Test
    public void teste2() throws Exception {
        Assert.assertTrue(teste("expr2.txt"));
    }

    @Test
    public void teste3() throws Exception {
        Assert.assertTrue(teste("expr3.txt"));
    }

    @Test
    public void teste4() throws Exception {
        Assert.assertTrue(teste("expr4.txt"));
    }

    @Test
    public void teste5() throws Exception {
        Assert.assertTrue(teste("expr5.txt"));
    }

    @Test
    public void teste6() throws Exception {
        Assert.assertTrue(teste("expr6.txt"));
    }

    @Test
    public void teste7() throws Exception {
        Assert.assertTrue(teste("expressao.txt"));
    }
}
