package lexico;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class Teste {

    private void teste(String res) throws Exception {
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
        List<Token> tokens1 = lex.analizar(r1);
        for (Token to : tokens1) System.out.println(to);
    }

    @Test
    public void teste1() throws Exception {
        teste("exLexErro1.txt");
    }

    @Test
    public void teste2() throws Exception {
        teste("exLexErro2.txt");
    }

    @Test
    public void teste3() throws Exception {
        teste("exLexOk.txt");
    }

    @Test
    public void teste4() throws Exception {
        teste("exemplocomentario.txt");
    }

    @Test
    public void teste5() throws Exception {
        teste("exemplostringlit.txt");
    }

    @Test
    public void teste6() throws Exception {
        teste("exemplointmain.txt");
    }

    @Test
    public void teste7() throws Exception {
        teste("exemplointmaincomplex.txt");
    }

    @Test
    public void teste8() throws Exception {
        teste("exoperadores.txt");
    }

    @Test
    public void teste9() throws Exception {
        teste("exidentif.txt");
    }

    @Test
    public void teste10() throws Exception {
        String s = "/* aaaa aa aaaaaaaaaa \naaaa \naaa \naaaaaa aaaaa aaaaaaa" + "\taa aaaaaa\r\n aaa aaaaa\naaaaaaaaaaaaaaaa\naaa\naaaa aaa" + "\n aaaaaaaaaaaaaaaa aaaaaaaaaaaaaaaaaaaaa aaaaaaa aaaaaaaa" + "\r\n aaaa aaaaaãaaa aaaaa aaaaaa aa a aaa aaaa aaaaaaa */";
        String re = "/\\*[\\s\\S]*?";
        Assert.assertTrue(s.matches(re));
    }
}
