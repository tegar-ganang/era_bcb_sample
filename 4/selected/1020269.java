package pshell.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Classe principal do Pleno Shell (pshell). Possui a estrutura b�sica de m�todos
 * a serem utilizados pelas classes que ser�o chamadas via pshell.
 * @author <B>Lourival Almeida</B>
 * @version 1.3
 */
public class ShellBaseClass {

    /** Instancia em uso da classe*/
    private static Object instancia = null;

    private BufferedReader br = null;

    private HashMap atalhos;

    private CommandManager commandManager;

    private StringBuffer helpBuffer;

    /**
     * Construtor da classe.
     */
    public ShellBaseClass() {
        instancia = this;
        this.br = new BufferedReader(new InputStreamReader(System.in));
        atalhos = new HashMap();
        helpBuffer = new StringBuffer();
        helpBuffer.append("\n--- pshell.base.ShellBaseClass ---\n");
        helpBuffer.append("help\n");
        helpBuffer.append("sair\n");
        helpBuffer.append("set\n");
        helpBuffer.append("set <link> <comando>\n");
        helpBuffer.append("use <classe>\n");
        helpBuffer.append("versao\n");
    }

    /**
     * Retorna o buffer que conter� as linhas de comandos informadas pelo usu�rio.
     * Este buffer estar� diretamente ligado ao System.in
     * @return Apontador para linha de comando.
     */
    public BufferedReader getBuffer() {
        return br;
    }

    /**
     * Retorna qual a classe que est� sendo utilizada no shell em um determinado momento.
     * @return Classe em utiliza��o no shell.
     */
    public Object getInstancia() {
        return instancia;
    }

    /**
     * Abre uma classe para execu��o de comandos no pshell.
     * @param classe nome completo da classe a ser utilizada. Ex.: pshell.base.ShellDBUtils
     * @throws java.lang.ClassNotFoundException 
     * @throws java.lang.InstantiationException 
     * @throws java.lang.IllegalAccessException 
     */
    public final void use(String classe) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        try {
            instancia = Class.forName(classe).newInstance();
            return;
        } catch (Exception e) {
        }
        try {
            instancia = Class.forName(getConfigFile().getString("cmd.use." + classe)).newInstance();
            return;
        } catch (Exception e) {
            System.out.println("Classe informada como par�metro n�o foi encontrada.");
        }
    }

    public final void use() {
        System.out.println("Atalho 1: " + getConfigFile().getString("cmd.use.1"));
        System.out.println("Atalho 2: " + getConfigFile().getString("cmd.use.2"));
        System.out.println("Atalho 3: " + getConfigFile().getString("cmd.use.3"));
        System.out.println("Atalho 4: " + getConfigFile().getString("cmd.use.4"));
        System.out.println("Atalho 5: " + getConfigFile().getString("cmd.use.5"));
        System.out.println("Atalho 6: " + getConfigFile().getString("cmd.use.6"));
        System.out.println("Atalho 7: " + getConfigFile().getString("cmd.use.7"));
        System.out.println("Atalho 8: " + getConfigFile().getString("cmd.use.8"));
        System.out.println("Atalho 9: " + getConfigFile().getString("cmd.use.9"));
    }

    /**
     * Abre no pshell uma entrada de dados para o usu�rio.
     * @param mensagem mensagem que ser� exibida para o usu�rio antes da entrada de dados.
     * @throws java.io.IOException 
     * @return 
     */
    public String lerTeclado(String mensagem) throws IOException {
        System.out.print(mensagem);
        String teste = getBuffer().readLine();
        return teste;
    }

    /**
     * Encerra o pshell.
     */
    public void sair() {
        System.out.println(ResourceBundle.getBundle("pshell/base/Mensagens").getString("pshell.sair"));
        System.exit(0);
    }

    /**
     * Exibe todos os m�todo dispon�veis em uma classe para que o usu�rio
     * possa utilizar no pshell.
     */
    public void helpAll() {
        System.out.println(ResourceBundle.getBundle("pshell/base/Mensagens").getString("saida.lista_comandos"));
        Method m[] = this.getClass().getMethods();
        int i = 0, x = 0;
        for (i = 0; i < m.length; i++) {
            Class[] tipoParametros = m[i].getParameterTypes();
            for (x = 0; x < tipoParametros.length; x++) {
                if (!tipoParametros[x].getName().equals("java.lang.String")) break;
            }
            if (x == tipoParametros.length) {
                System.out.println(m[i].getName());
            }
        }
    }

    /**
     * Exibe o help dispon�vel na classe que est� em utiliza��o.
     */
    public void help() {
        System.out.println(helpBuffer.toString());
    }

    /**
     * Exibe a versão em utilização do pshell.
     */
    public final void versao() {
        System.out.println(ResourceBundle.getBundle("pshell/base/Mensagens").getString("pshell.versao"));
    }

    /**
     * Seta um atalho (link) para um comando.
     * Com este m�todo o usu�rio pode criar um "apelido" para um comando muito utilizado.
     * Ex.:<br>
     * <B> Comando:</B> set load [use pshell.base.ShellDBBaseClass]<br>
     * <B> Resultado:</B> Cria um atalho chamado "load" para o comando "use pshell.base.ShellDBBaseClass".<br>
     * O atalho criado poder� ser chamado no pshell como se fosse um m�todo existente da classe.
     * Tamb�m � poss�vel utilizar par�metros nos atalhos criados. Ex.:<br>
     * <B> Comando:</B> set con [conectar ?]<br>
     * <B> Resultado:</B> Cria um atalho que possui um par�metro. Sempre que o atalho for acionado dever�
     * ser passado mais este par�metro para o pshell. Neste caso, por exemplo, poderia ser chamado
     * "con mysql" que seria transformado em "conectar mysql".
     * @param link nome do atalho a ser criado.
     * @param comando comando a ser associado ao atalho. Este comando deve estar entre "[" e "]".
     */
    public void set(String link, String comando) {
        atalhos.put(link, comando);
        commandManager.registrarAtalho(link, comando);
    }

    /**
     * Exibe todos os atalhos existentes para o pshell em um dado momento.
     */
    public void set() {
        System.out.println(atalhos.toString());
    }

    /**
     * Retorna o arquivo de configura��o onde est�o mapeadas as propriedades 
     * das bases de dados que poderao ser utilizadas.
     */
    protected ResourceBundle getConfigFile() {
        File arquivo = new File("pshell.properties");
        if (!arquivo.exists()) {
            try {
                InputStream inputStream = getClass().getResourceAsStream("/pshell/base/default.properties");
                FileWriter outputStream = new FileWriter("pshell.properties");
                int c;
                while ((c = inputStream.read()) != -1) outputStream.write(c);
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println("Arquivo pshell.properties criado no diretorio " + System.getProperty("user.dir"));
        }
        try {
            ResourceBundle.clearCache();
            return ResourceBundle.getBundle("pshell");
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Seta o Gerenciador de Comando associado a esta classe.
     * @param cmd Objeto que representa o Gerenciador de Comando.
     */
    public void setCommandManager(CommandManager cmd) {
        this.commandManager = cmd;
    }

    protected StringBuffer getHelpBuffer() {
        return helpBuffer;
    }

    public void charsetDisponiveis() throws IOException {
        Map map = Charset.availableCharsets();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String charsetName = (String) it.next();
            System.out.println(charsetName);
        }
        System.out.println("Charset em utiliza��o pela JVM: " + System.getProperty("file.encoding"));
    }

    public String converte(String valor, String charset) throws UnsupportedEncodingException {
        if (valor == null) return null;
        byte[] saida = valor.getBytes(charset);
        return new String(saida, 0, saida.length);
    }
}
