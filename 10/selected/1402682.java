package control;

import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import view.TelaCEA;
import view.TelaCadastro;
import view.TelaNovoAutor;
import model.Autor;
import model.Capitulo;
import model.Descricao;
import model.Licao;
import model.Midia;
import model.Tutorial;

/**
 * A classe 'AcessaDados' implementa todos os m�todos que processam consultas os atualiza��es a base de dados e tamb�m os m�todos que 
 * est�o relacionados com os dados inseridos nas telas da aplica��o.
 * @author Samuel Henrique N. da Silva
 *
 */
public final class AcessaDados {

    /**
	 * Construtor padr�o da classse <i>AcessaDados</i>
	 */
    public AcessaDados() {
    }

    public static boolean adicionaCapituloEmTutCap(final int codTut, final Vector<Integer> vCodCap, final Connection con) {
        try {
            Statement smt = con.createStatement();
            for (int i = 0; i < vCodCap.size(); i++) {
                smt.executeUpdate("INSERT INTO Tut_Cap VALUES(" + codTut + ", " + vCodCap.get(i) + ")");
            }
            return true;
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return false;
        }
    }

    public static boolean adicionaLicaoEmCapLic(final int codCap, final Vector<Integer> vCodLic, final Connection con) {
        try {
            Statement smt = con.createStatement();
            for (int i = 0; i < vCodLic.size(); i++) {
                smt.executeUpdate("INSERT INTO Cap_Lic VALUES(" + codCap + ", " + vCodLic.get(i) + ")");
            }
            return true;
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return false;
        }
    }

    public static boolean adicionaMidiaEmLicMid(final int codLic, final Vector<Integer> vCodMid, final int numSeq, final Connection con) {
        try {
            Statement smt = con.createStatement();
            for (int i = 0; i < vCodMid.size(); i++) {
                smt.executeUpdate("INSERT INTO Lic_Mid VALUES(" + codLic + ", " + vCodMid.get(i) + ", " + numSeq + ")");
            }
            return true;
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return false;
        }
    }

    /**
	 * Este m�todo realiza a altera��o em um registro do recurso do tipo Cap�tulo. A altera��o � definida pelo usu�rio
	 * na interface gr�fica, e passadas para a base de dados de acordo com o c�digo previamente pesquisado.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do recurso a ser alterado
	 * @return Verdadeiro ou falso, de acordo com o sucesso ou n�o da altera��o
	 */
    public static boolean alterarCapitulo(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM capitulo WHERE codCapitulo = " + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("UPDATE descricao SET texto='" + TelaCEA.getDescricaoTA().getText() + "' WHERE codDescricao=" + codDesc);
            smt.executeUpdate("UPDATE capitulo SET titulo='" + TelaCEA.getTituloTF().getText() + "' WHERE codCapitulo=" + cod);
            smt.executeUpdate("UPDATE capitulo SET comentario='" + TelaCEA.getComentarioTA().getText() + "' WHERE codCapitulo=" + cod);
            con.commit();
            return (true);
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    /**
	 * Este m�todo realiza a altera��o em um registro do recurso do tipo M�dia. A altera��o � definida pelo usu�rio
	 * na interface gr�fica, e passadas para a base de dados de acordo com o c�digo previamente pesquisado.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do recurso a ser alterado
	 * @return Verdadeiro ou falso, de acordo com o sucesso ou n�o da altera��o
	 */
    @SuppressWarnings("deprecation")
    public static boolean alterarMidia(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM midia WHERE codMidia = " + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("UPDATE descricao SET texto='" + TelaCEA.getDescricaoTA().getText() + "' WHERE codDescricao=" + codDesc);
            smt.executeUpdate("UPDATE midia SET titulo='" + TelaCEA.getTituloTF().getText() + "' WHERE codMidia=" + cod);
            smt.executeUpdate("UPDATE midia SET comentario='" + TelaCEA.getComentarioTA().getText() + "' WHERE codMidia=" + cod);
            smt.executeUpdate("UPDATE midia SET url='" + URLDecoder.decode(TelaCEA.getUrlTF().getText()) + "' WHERE codMidia=" + cod);
            if (TelaCEA.getVideoRB().isSelected()) {
                smt.executeUpdate("UPDATE midia SET tipo='video' WHERE codMidia=" + cod);
            } else {
                smt.executeUpdate("UPDATE midia SET tipo='imagem' WHERE codMidia=" + cod);
            }
            con.commit();
            return (true);
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    /**
	 * Este m�todo realiza a altera��o em um registro do recurso do tipo Li��o. A altera��o � definida pelo usu�rio
	 * na interface gr�fica, e passadas para a base de dados de acordo com o c�digo previamente pesquisado.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do recurso a ser alterado
	 * @return Verdadeiro ou falso, de acordo com o sucesso ou n�o da altera��o
	 */
    public static boolean alterarLicao(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM licao WHERE codLicao = " + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("UPDATE descricao SET texto='" + TelaCEA.getDescricaoTA().getText() + "' WHERE codDescricao=" + codDesc);
            smt.executeUpdate("UPDATE licao SET titulo='" + TelaCEA.getTituloTF().getText() + "' WHERE codLicao=" + cod);
            smt.executeUpdate("UPDATE licao SET comentario='" + TelaCEA.getComentarioTA().getText() + "' WHERE codLicao=" + cod);
            con.commit();
            return (true);
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    /**
	 * O m�todo alterarRecurso, realiza altera��es definidas pelo usu�rio no registro de um determinado recurso, consultado
	 * previamente. As altera��es s�o realizadas em um tipo de recurso (Tutorial, Cap�tulo, Li��o ou M�dia) de acordo com o c�digo 
	 * consultado para o respectivo recurso. Para realizar tais opera��es este m�todo chama um dos m�todos respons�veis para determinado
	 * recurso.
	 * @param con Conex�o com a base de dados
	 * @param tipo Tipo do recurso a ser alterado
	 * @param cod C�digo do registro de recurso a ser alterado
	 * @return Verdadeiro ou falso, se a altera��o foi realizado com sucesso ou n�o.
	 */
    public static boolean alteraRecurso(final Connection con, final int tipo, final int cod) {
        switch(tipo) {
            case 1:
                if (alterarTutorial(con, cod)) {
                    return true;
                } else {
                    return false;
                }
            case 2:
                if (alterarCapitulo(con, cod)) {
                    return true;
                } else {
                    return false;
                }
            case 3:
                if (alterarLicao(con, cod)) {
                    return true;
                } else {
                    return false;
                }
            case 4:
                if (alterarMidia(con, cod)) {
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    /**
	 * Este m�todo realiza a altera��o em um registro do recurso do tipo Tutorial. A altera��o � definida pelo usu�rio
	 * na interface gr�fica, e passadas para a base de dados de acordo com o c�digo previamente pesquisado.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do recurso a ser alterado
	 * @return Verdadeiro ou falso, de acordo com o sucesso ou n�o da altera��o
	 */
    public static boolean alterarTutorial(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM tutorial WHERE codTutorial = " + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("UPDATE descricao SET texto='" + TelaCEA.getDescricaoTA().getText() + "' WHERE codDescricao=" + codDesc);
            smt.executeUpdate("UPDATE tutorial SET titulo='" + TelaCEA.getTituloTF().getText() + "' WHERE codTutorial=" + cod);
            smt.executeUpdate("UPDATE tutorial SET comentario='" + TelaCEA.getComentarioTA().getText() + "' WHERE codTutorial=" + cod);
            con.commit();
            return (true);
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    /**
	 * O m�todo <b>retornaAutor(Connection con)<b> efetua uma consulta a base de dados, mais especificamente � tabela 'Autor' e retorna, de acordo
	 * com os dados escolhidos pelo usu�rio na <i>combo box</i>, as informa��es  necess�rias para efetuar a rela��o entre autor e um
	 * recurso a ser cadastrado.
	 * @param con Objeto da classe <i>Connection</i> que guarda a conex�o com a base de dados.
	 * @return Autor Objeto da classe autor.
	 */
    public static Autor retornaAutor(Connection con) {
        Autor res = new Autor();
        String codStr = "";
        int codInt = 0;
        if (!TelaCadastro.getAutorCB().getSelectedItem().toString().trim().equals("") && !TelaCadastro.getAutorCB().getSelectedItem().toString().trim().equals("[Novo Autor]")) {
            codStr = TelaCadastro.getAutorCB().getSelectedItem().toString();
            codInt = Integer.parseInt(codStr.substring(0, codStr.indexOf(' ')));
            try {
                Statement smt = con.createStatement();
                ResultSet rs = smt.executeQuery("SELECT * FROM autor WHERE codAutor = " + codInt);
                rs.next();
                res.setCodAutor(rs.getInt("codAutor"));
                res.setNome(rs.getString("nome"));
                res.setEmail(rs.getString("email"));
            } catch (SQLException e) {
                System.err.print(e.getMessage());
            }
        }
        return res;
    }

    /**
	 * O m�todo <b>recuperaNomeAutores(final Connection con)</b> efetua uma consulta a base de dados, mais espec�ficamente � tabela 'Autores', e retorna os c�digos e nomes
	 * dos respectivos autores cadastrados. Com essas informa��es � realizada a constru��o de um vetor contendo strings que concatem os c�digos e nomes
	 * dos autores cadastrados, para serem mostrados na <i>combo box</i> na Tela de Cadastro.
	 * @param con Objeto da classe <i>Connection</i> que guarda a conex�o com a base de dados.
	 * @return Vector Vetor de String com c�digo e n�mero dos autores cadastrados na base de dados. 
	 */
    public static Vector<String> recuperaNomeAutores(final Connection con) {
        Vector<Integer> v = new Vector<Integer>();
        Vector<String> v2 = new Vector<String>();
        Vector<String> v3 = new Vector<String>();
        String s = "";
        try {
            Statement smt = con.createStatement();
            ResultSet rs = smt.executeQuery("SELECT codAutor, nome FROM autor");
            while (rs.next()) {
                v.addElement(rs.getInt("codAutor"));
                v2.addElement(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int i = 0;
        v3.addElement("");
        v3.addElement("[Novo Autor]");
        while (i < v.size()) {
            s = v.get(i).toString();
            s = s + " - ";
            s = s + v2.get(i).toString();
            v3.addElement(s);
            i++;
        }
        return v3;
    }

    /**
	 * o m�todo <b>cadastraObjeto(final int tipo, final Connection con)</b> � respons�vel por compor as inst�ncias das classes apropriadas utilizando
	 * os dados digitados pelo usu�rio na Tela de Cadastro, e chamar os m�todos que as inserem na base de dados.
	 * @param tipo inteiro indicando que tipo de Recurso(Tutorial, Cap�tulo, Li��o ou M�dia)
	 * @param con Objeto da classe <i>Connection</i> que guarda a conex�o com a base de dados.
	 */
    public static boolean cadastraObjeto(final int tipo, final Connection con) {
        boolean stt = false;
        Autor aut = new Autor();
        Descricao desc = new Descricao();
        aut = retornaAutor(con);
        desc.setTexto(TelaCadastro.getDescricaoTA().getText());
        switch(tipo) {
            case 1:
                Tutorial tut = new Tutorial();
                tut.setTitulo(TelaCadastro.getTituloTF().getText());
                tut.setComentario(TelaCadastro.getComentarioTA().getText());
                if (insereTutorial(con, tut, aut, desc)) {
                    stt = true;
                } else {
                    stt = false;
                }
                break;
            case 2:
                Capitulo cap = new Capitulo();
                cap.setTitulo(TelaCadastro.getTituloTF().getText());
                aut.setNome(TelaCadastro.getAutorTF().getText());
                aut.setEmail("autor.cap@mail.com");
                cap.setComentario(TelaCadastro.getComentarioTA().getText());
                if (insereCapitulo(con, cap, aut, desc)) {
                    stt = true;
                } else {
                    stt = false;
                }
                break;
            case 3:
                Licao lic = new Licao();
                lic.setTitulo(TelaCadastro.getTituloTF().getText());
                lic.setComentario(TelaCadastro.getComentarioTA().getText());
                if (insereLicao(con, lic, aut, desc)) {
                    stt = true;
                } else {
                    stt = false;
                }
                break;
            case 4:
                Midia mid = new Midia();
                mid.setTitulo(TelaCadastro.getTituloTF().getText());
                mid.setComentario(TelaCadastro.getComentarioTA().getText());
                mid.setUrl(TelaCadastro.getUrlTF().getText());
                mid.setTipo(TelaCadastro.getVideoRB().isSelected() ? "video" : "imagem");
                if (insereMidia(con, mid, aut, desc)) {
                    stt = true;
                } else {
                    stt = false;
                }
                break;
            default:
                break;
        }
        return stt;
    }

    /**
	 * Este m�todo efetua a consulta das informa��es cadastradas para um Cap�tulo. Com a vari�vel do tipo int, 'cod' passada por 
	 * par�metro, o m�todo efetua consultas a base de dados, retornando os dados cadastrados para os elementos da interface respons�veis
	 * por mostr�-los ao usu�rio.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do elemento a ser consultado
	 */
    public static void consultaCapitulo(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblCap = smt.executeQuery("SELECT * FROM capitulo WHERE " + "codCapitulo =" + cod);
            tblCap.next();
            Integer temp = tblCap.getInt(1);
            TelaCEA.getCodigoTF().setText(temp.toString());
            TelaCEA.getTituloTF().setText(tblCap.getString(2));
            TelaCEA.getComentarioTA().setText(tblCap.getString(3));
            int codDesc = tblCap.getInt(4);
            ResultSet tblAut = smt.executeQuery("SELECT nome FROM autor WHERE " + "codAutor = SELECT codAutor FROM cap_aut WHERE " + "codCapitulo = " + cod);
            tblAut.next();
            TelaCEA.getAutorTF().setText(tblAut.getString(1));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            TelaCEA.getDescricaoTA().setText(tblDesc.getString(1));
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Este m�todo efetua a consulta das informa��es cadastradas para uma Li��o. Com a vari�vel do tipo int, 'cod' passada por 
	 * par�metro, o m�todo efetua consultas a base de dados, retornando os dados cadastrados para os elementos da interface respons�veis
	 * por mostr�-los ao usu�rio.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do elemento a ser consultado
	 */
    public static void consultaLicao(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblLic = smt.executeQuery("SELECT * FROM licao WHERE " + "codLicao =" + cod);
            tblLic.next();
            Integer temp = tblLic.getInt(1);
            TelaCEA.getCodigoTF().setText(temp.toString());
            TelaCEA.getTituloTF().setText(tblLic.getString(2));
            TelaCEA.getComentarioTA().setText(tblLic.getString(3));
            int codDesc = tblLic.getInt(4);
            ResultSet tblAut = smt.executeQuery("SELECT nome FROM autor WHERE " + "codAutor = SELECT codAutor FROM lic_aut WHERE " + "codLicao = " + cod);
            tblAut.next();
            TelaCEA.getAutorTF().setText(tblAut.getString(1));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            TelaCEA.getDescricaoTA().setText(tblDesc.getString(1));
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Este m�todo efetua a consulta das informa��es cadastradas para uma M�dia. Com a vari�vel do tipo int, 'cod' passada por 
	 * par�metro, o m�todo efetua consultas a base de dados, retornando os dados cadastrados para os elementos da interface respons�veis
	 * por mostr�-los ao usu�rio.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do elemento a ser consultado
	 */
    public static void consultaMidia(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblMid = smt.executeQuery("SELECT * FROM midia WHERE " + "codmidia =" + cod);
            tblMid.next();
            Integer temp = tblMid.getInt(1);
            TelaCEA.getCodigoTF().setText(temp.toString());
            TelaCEA.getTituloTF().setText(tblMid.getString(2));
            TelaCEA.getComentarioTA().setText(tblMid.getString(3));
            TelaCEA.getUrlTF().setText(tblMid.getString(4));
            if (tblMid.getString(5).equals("video")) {
                TelaCEA.getVideoRB().setSelected(true);
            } else {
                TelaCEA.getImagemRB().setSelected(true);
            }
            int codDesc = tblMid.getInt(6);
            ResultSet tblAut = smt.executeQuery("SELECT nome FROM autor WHERE " + "codAutor = SELECT codAutor FROM mid_aut WHERE " + "codMidia = " + cod);
            tblAut.next();
            TelaCEA.getAutorTF().setText(tblAut.getString(1));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            TelaCEA.getDescricaoTA().setText(tblDesc.getString(1));
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * O m�tdo consultaRecurso recebe um objeto da classe Connection, uma vari�vel de par�metro do tipo int representando o tipo de 
	 * recurso (Tutorial, Cap�tulo, Li��o ou M�dia) a ser consultado e mais uma vari�vel do tipo int representado o c�digo a ser consultado. Ap�s verifiar qual o tipo de recurso
	 * de acordo com a vari�vel 'tipo', este m�todo chama um dos m�todos que efetuam a consulta de acordo com o tipo de recurso.
	 * @param con Connection conex�o com a base de dados
	 * @param tipo int tipo do recurso a ser consultado
	 * @param cod int codigo do recurso a ser consultado
	 */
    public static void consultaRecurso(final Connection con, final int tipo, final int cod) {
        switch(tipo) {
            case 1:
                consultaTutorial(con, cod);
                break;
            case 2:
                consultaCapitulo(con, cod);
                break;
            case 3:
                consultaLicao(con, cod);
                break;
            case 4:
                consultaMidia(con, cod);
                break;
            default:
                break;
        }
    }

    /**
	 * Este m�todo efetua a consulta das informa��es cadastradas para um Tutorial. Com a vari�vel do tipo int, 'cod' passada por 
	 * par�metro, o m�todo efetua consultas a base de dados, retornando os dados cadastrados para os elementos da interface respons�veis
	 * por mostr�-los ao usu�rio.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do elemento a ser consultado
	 */
    public static void consultaTutorial(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblTut = smt.executeQuery("SELECT * FROM tutorial WHERE " + "codTutorial =" + cod);
            tblTut.next();
            Integer temp = tblTut.getInt(1);
            TelaCEA.getCodigoTF().setText(temp.toString());
            TelaCEA.getTituloTF().setText(tblTut.getString(2));
            TelaCEA.getComentarioTA().setText(tblTut.getString(3));
            int codDesc = tblTut.getInt(4);
            ResultSet tblAut = smt.executeQuery("SELECT nome FROM autor WHERE " + "codAutor = SELECT codAutor FROM tut_aut WHERE " + "codTutorial = " + cod);
            tblAut.next();
            TelaCEA.getAutorTF().setText(tblAut.getString(1));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            TelaCEA.getDescricaoTA().setText(tblDesc.getString(1));
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Este m�todo exclu� as informa��es cadastradas para um Cap�tlo na base de dados. De acordo com a vari�vel do tipo int, 'cod', passada 
	 * por par�metro, o m�todo realiza as altera��es relacionadas com o cap�tulo consultado na base de dados.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do registro de um recurso a ser exclu�do
	 */
    public static void excluirCapitulo(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM capitulo WHERE " + "codCapitulo=" + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("DELETE FROM capitulo WHERE codCapitulo=" + cod);
            smt.executeUpdate("DELETE FROM descricao WHERE codDescricao=" + codDesc);
            con.commit();
        } catch (SQLException e) {
            System.err.print(e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    public static boolean excluirCapituloDeTutCap(final int codTut, final Vector<Integer> vCodCap, final Connection con) {
        try {
            Statement smt = con.createStatement();
            for (int i = 0; i < vCodCap.size(); i++) {
                smt.executeUpdate("DELETE FROM Tut_Cap WHERE codTutorial=" + codTut + " AND codCapitulo=" + vCodCap.get(i));
            }
            return true;
        } catch (SQLException e) {
            System.out.print(e.getMessage());
            return false;
        }
    }

    /**
	 * Este m�todo exclu� as informa��es cadastradas para um Li��o na base de dados. De acordo com a vari�vel do tipo int, 'cod', passada 
	 * por par�metro, o m�todo realiza as altera��es relacionadas com a li��o consultada na base de dados.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do registro de um recurso a ser exclu�do
	 */
    public static void excluirLicao(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM licao WHERE " + "codLicao=" + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("DELETE FROM licao WHERE codLicao=" + cod);
            smt.executeUpdate("DELETE FROM descricao WHERE codDescricao=" + codDesc);
            con.commit();
        } catch (SQLException e) {
            System.err.print(e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    public static boolean excluirLicaoDeCapLic(final int codCap, final Vector<Integer> vCodLic, final Connection con) {
        try {
            Statement smt = con.createStatement();
            for (int i = 0; i < vCodLic.size(); i++) {
                smt.executeUpdate("DELETE FROM Cap_Lic WHERE codCapitulo=" + codCap + " AND codLicao=" + vCodLic.get(i));
            }
            return true;
        } catch (SQLException e) {
            System.out.print(e.getMessage());
            return false;
        }
    }

    /**
	 * Este m�todo exclu� as informa��es cadastradas para um M�dia na base de dados. De acordo com a vari�vel do tipo int, 'cod', passada 
	 * por par�metro, o m�todo realiza as altera��es relacionadas com a m�dia consultada na base de dados.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do registro de um recurso a ser exclu�do
	 */
    public static void excluirMidia(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM midia WHERE " + "codMidia=" + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            smt.executeUpdate("DELETE FROM midia WHERE codMidia=" + cod);
            smt.executeUpdate("DELETE FROM descricao WHERE codDescricao=" + codDesc);
            con.commit();
        } catch (SQLException e) {
            System.err.print(e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    public static boolean excluirMidiaDeLicMid(final int codLic, final Vector<Integer> vCodMid, final Connection con) {
        try {
            Statement smt = con.createStatement();
            for (int i = 0; i < vCodMid.size(); i++) {
                smt.executeUpdate("DELETE FROM Lic_Mid WHERE codLicao=" + codLic + " AND codMidia=" + vCodMid.get(i));
            }
            return true;
        } catch (SQLException e) {
            System.out.print(e.getMessage());
            return false;
        }
    }

    /**
	 * O m�todo excluirRecurso(), realiza a exclus�o das informa��es para determinado registro de um recurso(Tutorial, Cap�tulo, Li��o, M�dia)
	 * chamando os m�todos respons�veis pela exlus�o de cada um. Com os argumentos passados para este m�todo, as altera��es s�o realizadas
	 * na base de dados.
	 * @param con Conex�o com a base de dados
	 * @param tipo Tipo de recurso a ser exclu�do
	 * @param cod C�digo do registro de um recurso a ser exclu�do
	 */
    public static void excluirRecurso(final Connection con, final int tipo, final int cod) {
        switch(tipo) {
            case 1:
                excluirTutorial(con, cod);
                break;
            case 2:
                excluirCapitulo(con, cod);
                break;
            case 3:
                excluirLicao(con, cod);
                break;
            case 4:
                excluirMidia(con, cod);
                break;
            default:
                break;
        }
    }

    /**
	 * Este m�todo exclu� as informa��es cadastradas para um Tutorial na base de dados. De acordo com a vari�vel do tipo int, 'cod', passada 
	 * por par�metro, o m�todo realiza as altera��es relacionadas com o tutorial consultado na base de dados.
	 * @param con Conex�o com a base de dados
	 * @param cod C�digo do registro de um recurso a ser exclu�do
	 */
    public static void excluirTutorial(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet desc = smt.executeQuery("SELECT codDescricao FROM tutorial WHERE " + "codTutorial=" + cod);
            desc.next();
            int codDesc = desc.getInt(1);
            System.out.println("Resultado do resulset desc em excluir tutorial=" + codDesc);
            smt.executeUpdate("DELETE FROM tutorial WHERE codTutorial=" + cod);
            smt.executeUpdate("DELETE FROM descricao WHERE codDescricao=" + codDesc);
            con.commit();
        } catch (SQLException e) {
            System.err.print(e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    /**
	 * O m�todo <b>insereAutor(final Connection con)</b> realiza a opera��o de inserir na base de dados as informa��es digitadas pelo usu�rio na Tela Novo Autor. Tais 
	 * informa��es s�o o nome e o email, necess�rios para o cadastramento de um autor. O c�digo do mesmo � gerado automaticamente pelo m�todo
	 * <b>gerarCodAutor()</b> da classe <b>GeraID</b>.
	 * @param con Objeto da classe <i>Connection</i> que guarda a conex�o com a base de dados.
	 * @return boolean indica se a inser��o dos dados do autor foi realizada corretamente.
	 */
    public static boolean insereAutor(final Connection con) {
        Autor aut = new Autor();
        GeraID.gerarCodAutor(con, aut);
        aut.setNome(TelaNovoAutor.getNomeTF().getText());
        aut.setEmail(TelaNovoAutor.getEmailTF().getText());
        String nome = aut.getNome().replaceAll("['\"]", "");
        String email = aut.getEmail().replaceAll("['\"]", "");
        try {
            Statement smt = con.createStatement();
            smt.executeUpdate("INSERT INTO autor VALUES(" + aut.getCodAutor() + ",'" + nome + "','" + email + "')");
            TelaCadastro.getAutorCB().setModel(new DefaultComboBoxModel(recuperaNomeAutores(con)));
            return true;
        } catch (SQLException e) {
            System.err.print(e.getMessage());
            return false;
        }
    }

    /**
	 * Este m�todo realiza a inser��o dos dados referentes a um objeto Midia na base de dados, mais espec�ficamente 
	 * na tabela CAPITULO. Como a entidade CAPITULO possui rlacionamentos com as entidades AUTOR E DESCRI��O, outras inser��es
	 * precisam ser feitas: Uma para AUTOR, outra para DESCRICAO e uma ultima para CAP_AUT(relacionamento NxN AUTOR CAPITULO).
	 * @param cap Inst�ncia de Cap�tulo
	 * @param aut Inst�ncia de Autor
	 * @param desc Inst�ncia de Descricao
	 * @return true(Inser��es ok) false(errona inser��o)
	 */
    public static boolean insereCapitulo(final Connection con, Capitulo cap, Autor aut, Descricao desc) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            if (aut.getCodAutor() == 0) {
                GeraID.gerarCodAutor(con, aut);
                smt.executeUpdate("INSERT INTO autor VALUES(" + aut.getCodAutor() + ",'" + aut.getNome() + "','" + aut.getEmail() + "')");
            }
            GeraID.gerarCodDescricao(con, desc);
            GeraID.gerarCodCapitulo(con, cap);
            String text = desc.getTexto().replaceAll("[']", "\"");
            String titulo = cap.getTitulo().replaceAll("['\"]", "");
            String coment = cap.getComentario().replaceAll("[']", "\"");
            smt.executeUpdate("INSERT INTO descricao VALUES(" + desc.getCodDesc() + ",'" + text + "')");
            smt.executeUpdate("INSERT INTO capitulo VALUES(" + cap.getCodigo() + ",'" + titulo + "','" + coment + "'," + desc.getCodDesc() + ")");
            smt.executeUpdate("INSERT INTO cap_aut VALUES(" + cap.getCodigo() + "," + aut.getCodAutor() + ")");
            con.commit();
            return (true);
        } catch (SQLException e) {
            try {
                JOptionPane.showMessageDialog(null, "Rolling back transaction", "CAPITULO: Database error", JOptionPane.ERROR_MESSAGE);
                con.rollback();
            } catch (SQLException e1) {
                System.err.print(e1.getSQLState());
            }
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getSQLState());
            }
        }
    }

    /**
	 * Este m�todo realiza a inser��o dos dados referentes a um objeto Midia na base de dados, mais espec�ficamente 
	 * na tabela LICAO. Como a entidade LICAO possui rlacionamentos com as entidades AUTOR E DESCRI��O, outras inser��es
	 * precisam ser feitas: Uma para AUTOR, outra para DESCRICAO e uma ultima para LIC_AUT(relacionamento NxN AUTOR LICAO).
	 * @param lic Inst�ncia de Licao
	 * @param aut Inst�ncia de Autor
	 * @param desc Inst�ncia de Descricao
	 * @return true(Inser��es ok) false(errona inser��o)
	 */
    public static boolean insereLicao(final Connection con, Licao lic, Autor aut, Descricao desc) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            if (aut.getCodAutor() == 0) {
                GeraID.gerarCodAutor(con, aut);
                smt.executeUpdate("INSERT INTO autor VALUES(" + aut.getCodAutor() + ",'" + aut.getNome() + "','" + aut.getEmail() + "')");
            }
            GeraID.gerarCodDescricao(con, desc);
            GeraID.gerarCodLicao(con, lic);
            String titulo = lic.getTitulo().replaceAll("['\"]", "");
            String coment = lic.getComentario().replaceAll("[']", "\"");
            String texto = desc.getTexto().replaceAll("[']", "\"");
            smt.executeUpdate("INSERT INTO descricao VALUES(" + desc.getCodDesc() + ",'" + texto + "')");
            smt.executeUpdate("INSERT INTO licao VALUES(" + lic.getCodigo() + ",'" + titulo + "','" + coment + "'," + desc.getCodDesc() + ")");
            smt.executeUpdate("INSERT INTO lic_aut VALUES(" + lic.getCodigo() + "," + aut.getCodAutor() + ")");
            con.commit();
            return (true);
        } catch (SQLException e) {
            try {
                JOptionPane.showMessageDialog(null, "Rolling back transaction", "LICAO: Database error", JOptionPane.ERROR_MESSAGE);
                con.rollback();
            } catch (SQLException e1) {
                System.err.print(e1.getSQLState());
            }
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getSQLState());
            }
        }
    }

    /**
	 * Este m�todo realiza a inser��o dos dados referentes a um objeto Midia na base de dados, mais espec�ficamente 
	 * na tabela MIDIA. Como a entidade MID possui rlacionamentos com as entidades AUTOR E DESCRI��O, outras inser��es
	 * precisam ser feitas: Uma para AUTOR, outra para DESCRICAO e uma ultima para MID_AUT(relacionamento NxN AUTOR MIDIA).
	 * @param mid Inst�ncia de Midia
	 * @param aut Inst�ncia de Autor
	 * @param desc Inst�ncia de Descricao
	 * @return true(Inser��es ok) false(errona inser��o)
	 */
    public static boolean insereMidia(final Connection con, Midia mid, Autor aut, Descricao desc) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            if (aut.getCodAutor() == 0) {
                GeraID.gerarCodAutor(con, aut);
                smt.executeUpdate("INSERT INTO autor VALUES(" + aut.getCodAutor() + ",'" + aut.getNome() + "','" + aut.getEmail() + "')");
            }
            GeraID.gerarCodMidia(con, mid);
            GeraID.gerarCodDescricao(con, desc);
            String titulo = mid.getTitulo().replaceAll("['\"]", "");
            String coment = mid.getComentario().replaceAll("[']", "\"");
            String texto = desc.getTexto().replaceAll("[']", "\"");
            smt.executeUpdate("INSERT INTO descricao VALUES(" + desc.getCodDesc() + ",'" + texto + "')");
            smt.executeUpdate("INSERT INTO midia VALUES(" + mid.getCodigo() + ", '" + titulo + "', '" + coment + "','" + mid.getUrl() + "', '" + mid.getTipo() + "', " + desc.getCodDesc() + ")");
            smt.executeUpdate("INSERT INTO mid_aut VALUES(" + mid.getCodigo() + "," + aut.getCodAutor() + ")");
            con.commit();
            return (true);
        } catch (SQLException e) {
            try {
                JOptionPane.showMessageDialog(null, "Rolling back transaction", "MIDIA: Database error", JOptionPane.ERROR_MESSAGE);
                System.err.print(e.getMessage());
                con.rollback();
            } catch (SQLException e1) {
                System.err.print(e1.getSQLState());
            }
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getMessage());
            }
        }
    }

    /**
	 * Este m�todo realiza a inser��o dos dados referentes a um objeto Midia na base de dados, mais espec�ficamente 
	 * na tabela TUTORIAL. Como a entidade TUTORIAL possui rlacionamentos com as entidades AUTOR E DESCRI��O, outras inser��es
	 * precisam ser feitas: Uma para AUTOR, outra para DESCRICAO e uma ultima para TUT_AUT(relacionamento NxN AUTOR TUTORIAL).
	 * @param tut Inst�cia de Tutorial
	 * @param aut Inst�ncia de Autor
	 * @param desc Inst�ncia de Descricao
	 * @return true(if everything is ok) false(some error ocurred)
	 */
    private static boolean insereTutorial(final Connection con, final Tutorial tut, final Autor aut, final Descricao desc) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            if (aut.getCodAutor() == 0) {
                GeraID.gerarCodAutor(con, aut);
                smt.executeUpdate("INSERT INTO autor VALUES(" + aut.getCodAutor() + ",'" + aut.getNome() + "','" + aut.getEmail() + "')");
            }
            GeraID.gerarCodDescricao(con, desc);
            GeraID.gerarCodTutorial(con, tut);
            String titulo = tut.getTitulo().replaceAll("['\"]", "");
            String coment = tut.getComentario().replaceAll("[']", "\"");
            String texto = desc.getTexto().replaceAll("[']", "\"");
            smt.executeUpdate("INSERT INTO descricao VALUES(" + desc.getCodDesc() + ",'" + texto + "')");
            smt.executeUpdate("INSERT INTO tutorial VALUES(" + tut.getCodigo() + ",'" + titulo + "','" + coment + "'," + desc.getCodDesc() + ")");
            smt.executeUpdate("INSERT INTO tut_aut VALUES(" + tut.getCodigo() + "," + aut.getCodAutor() + ")");
            con.commit();
            return (true);
        } catch (SQLException e) {
            try {
                JOptionPane.showMessageDialog(null, "Rolling back transaction", "TUTORIAL: Database error", JOptionPane.ERROR_MESSAGE);
                System.out.print(e.getMessage());
                con.rollback();
            } catch (SQLException e1) {
                System.err.print(e1.getSQLState());
            }
            return (false);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e2) {
                System.err.print(e2.getSQLState());
            }
        }
    }

    /**
	 * Retorna os dados dos cap�tulos presentes na tabela Tut_Cap
	 * @param codTut
	 * @param con
	 * @return result Vetor contendo os id's dos capitulso constantes na tabela Tut_Cap
	 */
    public static Vector<String> retornaCapitulosDeTutCap(final int codTut, final Connection con) {
        try {
            con.setAutoCommit(false);
            Vector<String> cod = new Vector<String>();
            Vector<String> titulo = new Vector<String>();
            Vector<String> result = new Vector<String>();
            String s = "";
            Statement smt = con.createStatement();
            ResultSet rs1 = smt.executeQuery("SELECT codCapitulo FROM tut_cap " + "WHERE codTutorial = " + codTut);
            Vector<Integer> temp = new Vector<Integer>();
            while (rs1.next()) {
                temp.add(rs1.getInt(1));
            }
            for (int i = 0; i < temp.size(); i++) {
                ResultSet rs2 = smt.executeQuery("SELECT codCapitulo, titulo FROM capitulo " + "WHERE codCapitulo = " + temp.get(i));
                rs2.next();
                cod.add(rs2.getString("codCapitulo"));
                titulo.add(rs2.getString("titulo"));
            }
            for (int i = 0; i < cod.size(); i++) {
                s = cod.get(i).toString();
                s = s + " - ";
                s = s + titulo.get(i).toString();
                result.addElement(s);
            }
            return result;
        } catch (SQLException e) {
            System.out.print(e.getMessage());
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Retorna um vetor contendo strings no formato "cod - titulo" dos cap�tulos cadastrados.
	 * @param ConnectionBD
	 * @return Vector<String>
	 */
    public static Vector<String> retornaCapituloID(final Connection con) {
        Vector<String> v = new Vector<String>();
        Vector<String> v2 = new Vector<String>();
        Vector<String> v3 = new Vector<String>();
        String s = "";
        try {
            Statement smt = con.createStatement();
            ResultSet rs = smt.executeQuery("SELECT codCapitulo, titulo FROM capitulo");
            while (rs.next()) {
                v.addElement(rs.getString("codCapitulo"));
                v2.addElement(rs.getString("titulo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int i = 0;
        while (i < v.size()) {
            s = v.get(i).toString();
            s = s + " - ";
            s = s + v2.get(i).toString();
            v3.addElement(s);
            i++;
        }
        return v3;
    }

    /**
	 * Efetua consultas � base de dados e retorna os dados dos capitulos de acordo com o c�digo
	 * do mesmo passado por tutorial
	 * @return Objeto da classe Capitulo
	 */
    public static Capitulo retornaDadosCapitulo(final int codCap, final Connection con) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblCap = smt.executeQuery("SELECT * FROM capitulo WHERE " + "codCapitulo =" + codCap);
            Capitulo ret = new Capitulo();
            tblCap.next();
            ret.setCodigo(tblCap.getInt(1));
            ret.setTitulo(tblCap.getString(2));
            ret.setComentario(tblCap.getString(3));
            int codDesc = tblCap.getInt(4);
            ResultSet tblAut = smt.executeQuery("SELECT nome,email FROM autor WHERE " + "codAutor = SELECT codAutor FROM cap_aut WHERE " + "codCapitulo = " + codCap);
            tblAut.next();
            ret.setAutor(tblAut.getString(1));
            ret.setEmailAutor(tblAut.getString(2));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            ret.setDescricao(tblDesc.getString(1));
            con.commit();
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Retorna objeto da classe Licao cadastrados na base de dados
	 */
    public static Licao retornaDadosLicao(final int codLic, final Connection con) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblLic = smt.executeQuery("SELECT * FROM licao WHERE " + "codLicao =" + codLic);
            Licao ret = new Licao();
            tblLic.next();
            ret.setCodigo(tblLic.getInt(1));
            ret.setTitulo(tblLic.getString(2));
            ret.setComentario(tblLic.getString(3));
            int codDesc = tblLic.getInt(4);
            ResultSet tblAut = smt.executeQuery("SELECT nome,email FROM autor WHERE " + "codAutor = SELECT codAutor FROM lic_aut WHERE " + "codLicao = " + codLic);
            tblAut.next();
            ret.setAutor(tblAut.getString(1));
            ret.setEmailAutor(tblAut.getString(2));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            ret.setDescricao(tblDesc.getString(1));
            con.commit();
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Retorn objeto da classe Midia cadastrado na base de dados
	 */
    public static Midia retornaDadosMidia(final int codMid, final Connection con) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblMid = smt.executeQuery("SELECT * FROM midia WHERE " + "codmidia =" + codMid);
            Midia ret = new Midia();
            tblMid.next();
            ret.setCodigo(tblMid.getInt(1));
            ret.setTitulo(tblMid.getString(2));
            ret.setComentario(tblMid.getString(3));
            ret.setUrl(tblMid.getString(4));
            ret.setTipo(tblMid.getString(5));
            int codDesc = tblMid.getInt(6);
            ResultSet tblAut = smt.executeQuery("SELECT nome,email FROM autor WHERE " + "codAutor = SELECT codAutor FROM mid_aut WHERE " + "codMidia = " + codMid);
            tblAut.next();
            ret.setAutor(tblAut.getString(1));
            ret.setEmailAutor(tblAut.getString(2));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            ret.setDescricao(tblDesc.getString(1));
            con.commit();
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Efetua um consulta � base de dados de acordo com os parametros passados (c�digo do tutorial)
	 * e retorna os dados existentes na base de dados.
	 * @param con
	 * @param cod
	 * @return
	 */
    public static Tutorial retornaDadosTutorial(final Connection con, final int cod) {
        try {
            con.setAutoCommit(false);
            Statement smt = con.createStatement();
            ResultSet tblTut = smt.executeQuery("SELECT * FROM tutorial WHERE " + "codTutorial =" + cod);
            Tutorial ret = new Tutorial();
            tblTut.next();
            ret.setCodigo(tblTut.getInt(1));
            ret.setTitulo(tblTut.getString(2));
            ret.setComentario(tblTut.getString(3));
            int codDesc = tblTut.getInt(4);
            ResultSet tblAut = smt.executeQuery("SELECT nome,email FROM autor WHERE " + "codAutor = SELECT codAutor FROM tut_aut WHERE " + "codTutorial = " + cod);
            tblAut.next();
            ret.setAutor(tblAut.getString(1));
            ret.setEmailAutor(tblAut.getString(2));
            ResultSet tblDesc = smt.executeQuery("Select texto FROM descricao WHERE " + "codDescricao =" + codDesc);
            tblDesc.next();
            ret.setDescricao(tblDesc.getString(1));
            con.commit();
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Retorna os dados das li��es presentes na tabela Cap_Lic
	 * @param codCap
	 * @param con
	 * @return result Vetor contendo os id's das li��es constantes na tabela Cap_Lic
	 */
    public static Vector<String> retornaLicoesDeCapLic(final int codCap, final Connection con) {
        try {
            con.setAutoCommit(false);
            Vector<String> cod = new Vector<String>();
            Vector<String> titulo = new Vector<String>();
            Vector<String> result = new Vector<String>();
            String s = "";
            Statement smt = con.createStatement();
            ResultSet rs1 = smt.executeQuery("SELECT codLicao FROM cap_lic " + "WHERE codCapitulo = " + codCap);
            Vector<Integer> temp = new Vector<Integer>();
            while (rs1.next()) {
                temp.add(rs1.getInt(1));
            }
            for (int i = 0; i < temp.size(); i++) {
                ResultSet rs2 = smt.executeQuery("SELECT codLicao, titulo FROM licao " + "WHERE codLicao = " + temp.get(i));
                rs2.next();
                cod.add(rs2.getString("codLicao"));
                titulo.add(rs2.getString("titulo"));
            }
            for (int i = 0; i < cod.size(); i++) {
                s = cod.get(i).toString();
                s = s + " - ";
                s = s + titulo.get(i).toString();
                result.addElement(s);
            }
            return result;
        } catch (SQLException e) {
            System.out.print(e.getMessage());
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Retorna um vetor contendo strings no formato "cod - titulo" das li��es cadastradas.
	 * @param con Connection 
	 * @return v Vector<String>
	 */
    public static Vector<String> retornaLicaoID(final Connection con) {
        Vector<String> v = new Vector<String>();
        Vector<String> v2 = new Vector<String>();
        Vector<String> v3 = new Vector<String>();
        String s = "";
        try {
            Statement smt = con.createStatement();
            ResultSet rs = smt.executeQuery("SELECT codLicao, titulo FROM licao");
            while (rs.next()) {
                v.addElement(rs.getString("codLicao"));
                v2.addElement(rs.getString("titulo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int i = 0;
        while (i < v.size()) {
            s = v.get(i).toString();
            s = s + " - ";
            s = s + v2.get(i).toString();
            v3.addElement(s);
            i++;
        }
        return v3;
    }

    /**
	 * Retorna os dados das m�dias presentes na tabela Lic_Mid
	 * @param codLic
	 * @param con
	 * @return result Vetor contendo os id's das m�dias constantes na tabela Lic_Mid
	 */
    public static Vector<String> retornaMidiasDeLicMid(final int codLic, final Connection con) {
        try {
            con.setAutoCommit(false);
            Vector<String> cod = new Vector<String>();
            Vector<String> titulo = new Vector<String>();
            Vector<String> result = new Vector<String>();
            String s = "";
            Statement smt = con.createStatement();
            ResultSet rs1 = smt.executeQuery("SELECT codMidia FROM Lic_Mid " + "WHERE codLicao = " + codLic);
            Vector<Integer> temp = new Vector<Integer>();
            while (rs1.next()) {
                temp.add(rs1.getInt(1));
            }
            for (int i = 0; i < temp.size(); i++) {
                ResultSet rs2 = smt.executeQuery("SELECT codMidia, titulo FROM midia " + "WHERE codMidia = " + temp.get(i));
                rs2.next();
                cod.add(rs2.getString("codMidia"));
                titulo.add(rs2.getString("titulo"));
            }
            for (int i = 0; i < cod.size(); i++) {
                s = cod.get(i).toString();
                s = s + " - ";
                s = s + titulo.get(i).toString();
                result.addElement(s);
            }
            return result;
        } catch (SQLException e) {
            System.out.print(e.getMessage());
            return null;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Retorna um vetor contendo strings no formato "cod - titulo" das m�dias cadastradas.
	 * @param con Connection 
	 * @return v Vector<String>
	 */
    public static Vector<String> retornaMidiaID(final Connection con) {
        Vector<String> v = new Vector<String>();
        Vector<String> v2 = new Vector<String>();
        Vector<String> v3 = new Vector<String>();
        String s = "";
        try {
            Statement smt = con.createStatement();
            ResultSet rs = smt.executeQuery("SELECT codMidia, titulo FROM midia");
            while (rs.next()) {
                v.addElement(rs.getString("codMidia"));
                v2.addElement(rs.getString("titulo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int i = 0;
        while (i < v.size()) {
            s = v.get(i).toString();
            s = s + " - ";
            s = s + v2.get(i).toString();
            v3.addElement(s);
            i++;
        }
        return v3;
    }

    /**
	 * Retorna um vetor contendo strings no formato "cod - titulo" dos tutoriais cadastrados. O vetor retornado pode ser passado para uma
	 * lista.	
	 */
    public static Vector<String> retornaTutorialID(final Connection con) {
        Vector<String> v = new Vector<String>();
        Vector<String> v2 = new Vector<String>();
        Vector<String> v3 = new Vector<String>();
        String s = "";
        try {
            Statement smt = con.createStatement();
            ResultSet rs = smt.executeQuery("SELECT codTutorial, titulo FROM tutorial");
            while (rs.next()) {
                v.addElement(rs.getString("codTutorial"));
                v2.addElement(rs.getString("titulo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int i = 0;
        while (i < v.size()) {
            s = v.get(i).toString();
            s = s + " - ";
            s = s + v2.get(i).toString();
            v3.addElement(s);
            i++;
        }
        return v3;
    }
}
