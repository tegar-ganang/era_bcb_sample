package br.gov.ba.mam.banco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import br.gov.ba.mam.banco.db4o.GerentePersistencia;
import br.gov.ba.mam.banco.mysql.C3P0Pool;
import br.gov.ba.mam.beans.Artista;
import br.gov.ba.mam.beans.Categoria;
import br.gov.ba.mam.beans.Obra;

public class Migracao {

    private static GerentePersistencia ger;

    public static void main(String[] args) {
        migrarCategoria();
        migrarArtista();
        System.out.println("Migracao.main() Terminou");
    }

    private static void migrarArtista() {
        if (ger == null) ger = new GerentePersistencia();
        List<Artista> listaArtista = ger.getListaArtistas();
        for (Artista artista : listaArtista) {
            try {
                salvarArtista(artista);
                slvarListaObra(artista);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void slvarListaObra(Artista artista) {
        List<Obra> listaObra = artista.getListaObras();
        for (Obra obra : listaObra) {
            try {
                salvarObra(artista, obra);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void salvarObra(Artista artista, Obra obra) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        int categoria;
        System.out.println("Migracao.salvarObra() obra: " + obra.toString2());
        if (obra.getCategoria() != null) {
            categoria = getCategoria(obra.getCategoria().getNome()).getCodigo();
        } else {
            categoria = getCategoria("Sem Categoria").getCodigo();
        }
        try {
            conn = C3P0Pool.getConnection();
            String sql = "insert into obra VALUES (?,?,?,?,?,?)";
            ps = conn.prepareStatement(sql);
            ps.setNull(1, Types.INTEGER);
            ps.setString(2, obra.getTitulo());
            ps.setInt(3, obra.getSelec());
            ps.setInt(4, categoria);
            ps.setInt(5, artista.getNumeroInscricao());
            ps.setInt(6, obra.getCodigo());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            close(conn, ps);
        }
    }

    private static Categoria getCategoria(String nome) {
        Connection conn = null;
        PreparedStatement ps = null;
        Categoria cat = null;
        try {
            conn = C3P0Pool.getConnection();
            String sql = "select * from categoria where nome_categoria like ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    cat = getCategoria(rs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, ps);
        }
        return cat;
    }

    /**
	 * @throws SQLException  */
    private static Categoria getCategoria(ResultSet rs) throws SQLException {
        Categoria retorno = new Categoria();
        retorno.setCodigo(rs.getInt(1));
        retorno.setNome(rs.getString(2));
        return retorno;
    }

    private static void salvarArtista(Artista artista) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = C3P0Pool.getConnection();
            String sql = "insert into artista VALUES (?,?,?,?,?,?,?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, artista.getNumeroInscricao());
            ps.setString(2, artista.getNome());
            ps.setBoolean(3, artista.isSexo());
            ps.setString(4, artista.getEmail());
            ps.setString(5, artista.getObs());
            ps.setString(6, artista.getTelefone());
            ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            salvarEndereco(conn, ps, artista);
            conn.commit();
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            close(conn, ps);
        }
    }

    private static void salvarEndereco(Connection conn, PreparedStatement ps, Artista artista) throws Exception {
        String sql = "insert into endereco VALUES (?,?,?,?,?,?,?,?)";
        ps = conn.prepareStatement(sql);
        ps.setNull(1, Types.INTEGER);
        ps.setString(2, artista.getEndereco().getLogradouro());
        ps.setString(3, artista.getEndereco().getCep());
        ps.setInt(4, artista.getEndereco().getEstado());
        ps.setString(5, artista.getEndereco().getBairro());
        ps.setString(6, artista.getEndereco().getCidade());
        ps.setString(7, artista.getEndereco().getPais());
        ps.setInt(8, artista.getNumeroInscricao());
        ps.executeUpdate();
    }

    private static void migrarCategoria() {
        if (ger == null) ger = new GerentePersistencia();
        List<Categoria> listaCategoria = ger.getListaCategorias();
        HashMap<String, Categoria> hash = new HashMap<String, Categoria>();
        for (Categoria categoria : listaCategoria) {
            Categoria cat = new Categoria();
            cat.setNome(categoria.getNome());
            hash.put(cat.getNome(), cat);
        }
        for (Categoria cat2 : hash.values()) {
            try {
                if (!cat2.getNome().equals("Sem categoria")) ;
                salvarCategoria(cat2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void salvarCategoria(Categoria categoria) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = C3P0Pool.getConnection();
            String sql = "insert into categoria VALUES (?,?)";
            ps = conn.prepareStatement(sql);
            ps.setNull(1, Types.INTEGER);
            ps.setString(2, categoria.getNome());
            ps.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            close(conn, ps);
        }
    }

    private static void close(Connection conn, PreparedStatement ps) {
        try {
            if (ps != null) {
                ps.close();
            }
        } catch (Exception e) {
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
