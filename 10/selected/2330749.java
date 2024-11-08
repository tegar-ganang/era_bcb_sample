package br.com.fabrica_ti.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import br.com.fabrica_ti.model.Atividade;
import br.com.fabrica_ti.model.IUsuario;
import br.com.fabrica_ti.model.Leilao;
import br.com.fabrica_ti.model.Notificacao;
import br.com.fabrica_ti.model.RecursoHumano;
import br.com.fabrica_ti.model.Requerente;
import br.com.fabrica_ti.model.TipoAtividade;
import br.com.fabrica_ti.util.Utils;

public class AtividadeDAO {

    private ConnectionFactory connectionFactory = ConnectionFactory.getInstance();

    public Atividade getAtividade(Atividade atividade) throws SQLException {
        Connection conn = null;
        Atividade atividadeUpdate;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Atividade where idatividade = " + atividade.getIdAtividade();
            atividade = null;
            ResultSet rs = stmt.executeQuery(sqlSelect);
            LeilaoDAO leilaoDAO = new LeilaoDAO();
            while (rs.next()) {
                atividade = new Atividade();
                atividade.setIdAtividade(rs.getInt("idatividade"));
                Requerente requerente = new Requerente();
                requerente.setIdRequerente(rs.getInt("requerente_idrequerente"));
                atividade.setRequerente(requerente);
                RecursoHumano recursoHumano = new RecursoHumano();
                atividade.setRecursoHumano(recursoHumano);
                atividade.setDataCriacao(rs.getDate("datacriacao"));
                atividade.setDataTermino(rs.getDate("datatermino"));
                atividade.setValor(new BigDecimal(rs.getDouble("valor")));
                TipoAtividade tipoAtividade = new TipoAtividade();
                tipoAtividade.setIdTipoAtividade(rs.getInt("tipoatividade"));
                atividade.setTipoAtividade(tipoAtividade);
                atividade.setDescricao(rs.getString("descricao"));
                atividade.setFaseIdFase(rs.getInt("fase_idfase"));
                atividade.setEstado(rs.getInt("estado"));
                Leilao leilao = checkLeilao(atividade, leilaoDAO);
                atividade.setLeilao(leilao);
                return atividade;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return null;
    }

    private Leilao checkLeilao(Atividade atividade, LeilaoDAO leilaoDAO) throws SQLException {
        Leilao leilao = new Leilao();
        leilao.setAtividade(atividade);
        leilao = leilaoDAO.getLeilaoPorAtividade(leilao);
        return leilao;
    }

    public Atividade insertAtividade(Atividade atividade) throws SQLException {
        Connection conn = null;
        String insert = "insert into Atividade (idatividade, requerente_idrequerente, datacriacao, datatermino, valor, tipoatividade, descricao, fase_idfase, estado) " + "values " + "(nextval('seq_atividade'), " + atividade.getRequerente().getIdRequerente() + ", " + "'" + atividade.getDataCriacao() + "', '" + atividade.getDataTermino() + "', '" + atividade.getValor() + "', '" + atividade.getTipoAtividade().getIdTipoAtividade() + "', '" + atividade.getDescricao() + "', " + atividade.getFaseIdFase() + ", " + atividade.getEstado() + ")";
        try {
            conn = connectionFactory.getConnection(true);
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(insert);
            if (result == 1) {
                String sqlSelect = "select last_value from seq_atividade";
                ResultSet rs = stmt.executeQuery(sqlSelect);
                while (rs.next()) {
                    atividade.setIdAtividade(rs.getInt("last_value"));
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
        return null;
    }

    public void updateAtividade(Atividade atividade) throws SQLException {
        Connection conn = null;
        String update = "update Atividade set " + "datacriacao = '" + atividade.getDataCriacao() + "', " + "datatermino= '" + atividade.getDataTermino() + "', " + "valor = '" + atividade.getValor() + "', " + "tipoatividade = " + atividade.getTipoAtividade().getIdTipoAtividade() + ", " + "descricao = '" + atividade.getDescricao() + "', " + "fase_idfase = " + atividade.getFaseIdFase() + ", " + "ativo = '" + atividade.getAtivo() + "', " + "estado = " + atividade.getEstado() + " " + "where " + "idAtividade=" + atividade.getIdAtividade();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    public void notificarEstado(Notificacao notificacao) throws SQLException {
        Connection conn = null;
        String update = "update Atividade set " + "estado = " + notificacao.getAtividade().getEstado() + " " + "where " + "idAtividade=" + notificacao.getAtividade().getIdAtividade();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
            inserirNotificacao(notificacao);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    private void inserirNotificacao(Notificacao notificacao) throws SQLException {
        Connection conn = null;
        String update = "insert into Notificacao (idnotificacao, atividade_idatividade, conteudo) " + "values " + "(nextval('seq_notificacao'), " + notificacao.getAtividade().getIdAtividade() + ", " + "'" + notificacao.getConteudo() + "')";
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    public List<Atividade> getAtividades() throws SQLException {
        List<Atividade> atividades = new ArrayList<Atividade>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Atividade where fase_idfase <> 6";
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Atividade atividadesList = null;
            LeilaoDAO leilaoDAO = new LeilaoDAO();
            while (rs.next()) {
                atividadesList = new Atividade();
                atividadesList.setIdAtividade(rs.getInt("idatividade"));
                Requerente requerente = new Requerente();
                requerente.setIdRequerente(rs.getInt("requerente_idrequerente"));
                atividadesList.setRequerente(requerente);
                atividadesList.setDataCriacao(Utils.getDateFormat(rs.getString("datacriacao"), "yyyy-MM-dd"));
                atividadesList.setDataTermino(Utils.getDateFormat(rs.getString("datatermino"), "yyyy-MM-dd"));
                atividadesList.setValor(new BigDecimal(rs.getDouble("valor")));
                TipoAtividade tipoAtividade = new TipoAtividade();
                tipoAtividade.setIdTipoAtividade(rs.getInt("tipoatividade"));
                atividadesList.setTipoAtividade(tipoAtividade);
                atividadesList.setDescricao(rs.getString("descricao"));
                atividadesList.setEstado(rs.getInt("estado"));
                Leilao leilao = checkLeilao(atividadesList, leilaoDAO);
                atividadesList.setLeilao(leilao);
                atividades.add(atividadesList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return atividades;
    }

    public List<Atividade> getAtividadesRequerente(Requerente requerente) throws SQLException {
        List<Atividade> atividades = new ArrayList<Atividade>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT a.* FROM Atividade a where " + "a.fase_idfase <> 6 " + "and " + "a.requerente_idrequerente=" + requerente.getIdRequerente();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Atividade atividadesList = null;
            LeilaoDAO leilaoDAO = new LeilaoDAO();
            while (rs.next()) {
                atividadesList = new Atividade();
                atividadesList.setIdAtividade(rs.getInt("idatividade"));
                atividadesList.setRequerente(requerente);
                atividadesList.setDataCriacao(Utils.getDateFormat(rs.getString("datacriacao"), "yyyy-MM-dd"));
                atividadesList.setDataTermino(Utils.getDateFormat(rs.getString("datatermino"), "yyyy-MM-dd"));
                atividadesList.setValor(new BigDecimal(rs.getDouble("valor")));
                TipoAtividade tipoAtividade = new TipoAtividade();
                tipoAtividade.setIdTipoAtividade(rs.getInt("tipoatividade"));
                atividadesList.setTipoAtividade(tipoAtividade);
                atividadesList.setDescricao(rs.getString("descricao"));
                atividadesList.setEstado(rs.getInt("estado"));
                Leilao leilao = checkLeilao(atividadesList, leilaoDAO);
                atividadesList.setLeilao(leilao);
                atividades.add(atividadesList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return atividades;
    }

    public List<Notificacao> getNotificacoesAtividade(Atividade atividade) throws SQLException {
        List<Notificacao> notificacoes = new ArrayList<Notificacao>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Notificacao a where a.atividade_idatividade =" + atividade.getIdAtividade();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Notificacao notificacaoList = null;
            while (rs.next()) {
                notificacaoList = new Notificacao();
                notificacaoList.setConteudo(rs.getString("conteudo"));
                notificacaoList.setIdNotificacao(rs.getInt("idnotificacao"));
                notificacaoList.setAtividade(atividade);
                notificacoes.add(notificacaoList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return notificacoes;
    }

    public List<Atividade> getAtividadesRecursoHumano(RecursoHumano recursoHumano) throws SQLException {
        List<Atividade> atividades = new ArrayList<Atividade>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Atividade a, Atividade_has_recurso_humano rh " + "where " + "a.fase_idfase <> 6 and " + "a.idatividade=rh.atividade_idatividade and " + "rh.ativo = 'TRUE' " + "and rh.usuario_idusuario=" + recursoHumano.getIdUsuario();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Atividade atividadesList = null;
            LeilaoDAO leilaoDAO = new LeilaoDAO();
            while (rs.next()) {
                atividadesList = new Atividade();
                atividadesList.setIdAtividade(rs.getInt("idatividade"));
                Requerente requerente = new Requerente();
                requerente.setIdRequerente(rs.getInt("requerente_idrequerente"));
                atividadesList.setRequerente(requerente);
                atividadesList.setRecursoHumano(recursoHumano);
                atividadesList.setDataCriacao(Utils.getDateFormat(rs.getString("datacriacao"), "yyyy-MM-dd"));
                atividadesList.setDataTermino(Utils.getDateFormat(rs.getString("datatermino"), "yyyy-MM-dd"));
                atividadesList.setValor(new BigDecimal(rs.getDouble("valor")));
                TipoAtividade tipoAtividade = new TipoAtividade();
                tipoAtividade.setIdTipoAtividade(rs.getInt("tipoatividade"));
                atividadesList.setTipoAtividade(tipoAtividade);
                atividadesList.setDescricao(rs.getString("descricao"));
                atividadesList.setAtivo(rs.getBoolean("ativo"));
                atividadesList.setEstado(rs.getInt("estado"));
                Leilao leilao = checkLeilao(atividadesList, leilaoDAO);
                atividadesList.setLeilao(leilao);
                atividades.add(atividadesList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return atividades;
    }

    public Boolean isAtividadeAtiva(Atividade atividade) throws SQLException {
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Atividade_has_recurso_humano rh " + "where " + "rh.ativo = 'TRUE' and " + "rh.atividade_idatividade=" + atividade.getIdAtividade();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            while (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return false;
    }

    public List<Atividade> getCandidatoAtividades(RecursoHumano recursoHumano) throws SQLException {
        List<Atividade> atividades = new ArrayList<Atividade>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT a.*, rh.ativo, rh.usuario_idusuario FROM Atividade a, Atividade_has_recurso_humano rh " + "where " + "a.fase_idfase <> 6 and " + "rh.ativo = 'FALSE' and " + "rh.usuario_idusuario=" + recursoHumano.getIdUsuario() + " and " + "a.idatividade = rh.atividade_idatividade";
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Atividade atividadesList = null;
            LeilaoDAO leilaoDAO = new LeilaoDAO();
            while (rs.next()) {
                atividadesList = new Atividade();
                atividadesList.setIdAtividade(rs.getInt("idatividade"));
                Requerente requerente = new Requerente();
                requerente.setIdRequerente(rs.getInt("requerente_idrequerente"));
                RequerenteDAO requerenteDAO = new RequerenteDAO();
                requerente = requerenteDAO.getRequerenteUsuario(requerente);
                atividadesList.setRequerente(requerente);
                atividadesList.setRecursoHumano(recursoHumano);
                atividadesList.setDataCriacao(Utils.getDateFormat(rs.getString("datacriacao"), "yyyy-MM-dd"));
                atividadesList.setDataTermino(Utils.getDateFormat(rs.getString("datatermino"), "yyyy-MM-dd"));
                atividadesList.setValor(new BigDecimal(rs.getDouble("valor")));
                TipoAtividade tipoAtividade = new TipoAtividade();
                tipoAtividade.setIdTipoAtividade(rs.getInt("tipoatividade"));
                atividadesList.setTipoAtividade(tipoAtividade);
                atividadesList.setDescricao(rs.getString("descricao"));
                atividadesList.setAtivo(rs.getBoolean("ativo"));
                atividadesList.setEstado(rs.getInt("estado"));
                Leilao leilao = checkLeilao(atividadesList, leilaoDAO);
                atividadesList.setLeilao(leilao);
                atividades.add(atividadesList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return atividades;
    }

    public List<Atividade> getCandidatosAtividades(Requerente requerente) throws SQLException {
        List<Atividade> atividades = new ArrayList<Atividade>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT a.*, rh.usuario_idusuario, rh.ativo FROM Atividade a, Atividade_has_recurso_humano rh " + "where " + "a.fase_idfase <> 6 and " + "rh.ativo = 'FALSE' and " + "a.idatividade = rh.atividade_idatividade";
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Atividade atividadesList = null;
            LeilaoDAO leilaoDAO = new LeilaoDAO();
            while (rs.next()) {
                atividadesList = new Atividade();
                atividadesList.setIdAtividade(rs.getInt("idatividade"));
                RecursoHumano recursoHumano = new RecursoHumano();
                recursoHumano.setIdUsuario(rs.getInt("usuario_idusuario"));
                RecursoHumanoDAO recursoHumanoDAO = new RecursoHumanoDAO();
                recursoHumano = recursoHumanoDAO.getRecursoHumanoUsuario(recursoHumano);
                atividadesList.setRequerente(requerente);
                atividadesList.setRecursoHumano(recursoHumano);
                atividadesList.setDataCriacao(Utils.getDateFormat(rs.getString("datacriacao"), "yyyy-MM-dd"));
                atividadesList.setDataTermino(Utils.getDateFormat(rs.getString("datatermino"), "yyyy-MM-dd"));
                atividadesList.setValor(new BigDecimal(rs.getDouble("valor")));
                TipoAtividade tipoAtividade = new TipoAtividade();
                tipoAtividade.setIdTipoAtividade(rs.getInt("tipoatividade"));
                atividadesList.setTipoAtividade(tipoAtividade);
                atividadesList.setDescricao(rs.getString("descricao"));
                atividadesList.setAtivo(rs.getBoolean("ativo"));
                atividadesList.setEstado(rs.getInt("estado"));
                Leilao leilao = checkLeilao(atividadesList, leilaoDAO);
                atividadesList.setLeilao(leilao);
                atividades.add(atividadesList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return atividades;
    }

    public void cancelarAtividade(Atividade atividade) throws SQLException {
        Connection conn = null;
        String update = "update Atividade set fase_idfase=6, datacancelamento='" + atividade.getDataCancelamento() + "'" + "where " + "idAtividade=" + atividade.getIdAtividade();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    public void candidatarAtividade(Atividade atividade) throws SQLException {
        Connection conn = null;
        String insert = "insert into Atividade_has_recurso_humano " + "(atividade_idatividade, usuario_idusuario, ativo) " + "values " + "(" + atividade.getIdAtividade() + ", " + "" + atividade.getRecursoHumano().getIdUsuario() + ", " + "'false')";
        try {
            conn = connectionFactory.getConnection(true);
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(insert);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    public void desistirCandidatura(Atividade atividade) throws SQLException {
        Connection conn = null;
        String insert = "delete from Atividade_has_recurso_humano where atividade_idatividade=" + atividade.getIdAtividade() + " and usuario_idusuario=" + atividade.getRecursoHumano().getIdUsuario();
        try {
            conn = connectionFactory.getConnection(true);
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(insert);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    public void aprovarCandidato(Atividade atividade) throws SQLException {
        Connection conn = null;
        String insert = "update Atividade_has_recurso_humano set ativo='true' " + "where atividade_idatividade=" + atividade.getIdAtividade() + " and " + " usuario_idusuario=" + atividade.getRecursoHumano().getIdUsuario();
        try {
            conn = connectionFactory.getConnection(true);
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(insert);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    public Boolean isCandidatoAtividade(IUsuario usuario, Atividade atividade) throws SQLException {
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Atividade_has_recurso_humano where usuario_idusuario = " + usuario.getIdUsuario();
            usuario = null;
            ResultSet rs = stmt.executeQuery(sqlSelect);
            while (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return false;
    }
}
