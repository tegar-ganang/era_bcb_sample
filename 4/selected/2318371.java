package net.sf.webphotos.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import net.sf.webphotos.Album;
import net.sf.webphotos.BancoImagem;
import net.sf.webphotos.Photo;
import net.sf.webphotos.tools.Thumbnail;
import net.sf.webphotos.gui.PainelWebFotos;
import net.sf.webphotos.util.legacy.CacheFTP;
import net.sf.webphotos.gui.util.TableModelFoto;
import net.sf.webphotos.gui.util.TableModelAlbum;
import net.sf.webphotos.util.Util;
import javax.sql.RowSet;

/**
 * Altera ou cria alb�ns. Possui um construtor que recebe bot�es e tabelas de
 * alb�ns e fotos, um m�todo que idenfica a a��o obtida pelo evento e outro
 * m�todo que executa uma s�rie de passos para implementar as altera��es.
 */
public class AcaoAlterarAlbum extends AbstractAction {

    /**
     *
     */
    private static final long serialVersionUID = 7297664420604720262L;

    JButton btAlterar, btNovo;

    JTable tbAlbuns, tbFotos;

    private RowSet rowSet = BancoImagem.getRSet();

    private boolean sucesso;

    /**
     * Contrutor da classe. Recebe como par�metro dois bot�es, um para altera��o
     * e o outro para implementa��o nova. Seta os valores dos bot�es da classe a
     * partir dos recebidos e seta as tabelas de alb�ns e fotos a partir de
     * m�todos get da classe {@link net.sf.webphotos.gui.PainelWebFotos PainelWebFotos}.
     *
     * @param botaoNovo
     *            Bot�o para identificar a a��o de implementa��o novo.
     * @param botaoAlterar
     *            Bot�o para identificar a a��o de altera��o.
     */
    public AcaoAlterarAlbum(JButton botaoNovo, JButton botaoAlterar) {
        btAlterar = botaoAlterar;
        btNovo = botaoNovo;
        tbAlbuns = PainelWebFotos.getTbAlbuns();
        tbFotos = PainelWebFotos.getTbFotos();
    }

    /**
     * Identica qual a a��o que ocorreu. Recebe como par�metro um evento e
     * verifica qual tipo de a��o ocorreu por
     * {@link java.util.EventObject#getSource() getSource} e pelo
     * {@link javax.swing.AbstractButton#getActionCommand() getActionCommand}.
     * Se o usu�rio clicou em <I>novo</I> cria um novo �lbum, caso o texto do
     * bot�o seja <I>cancelar</I>, ent�o o usu�rio estar� cancelando a cria��o
     * de um novo alb�m e por �ltimo, caso seja <I>alterar</I>, efetuar� a
     * atualiza��o do �lbum, coletando os valores dos controles GUI, validando
     * os dados e atualizando o objeto.
     *
     * @param ev
     *            Evento de a��o.
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == btNovo && btNovo.getActionCommand().equals(PainelWebFotos.ACAO_NOVO)) {
            PainelWebFotos.botaoNovo();
        } else if (btAlterar.getActionCommand().equals(PainelWebFotos.ACAO_CANCELAR) && ev.getSource() == btAlterar) {
            PainelWebFotos.botaoCancelar();
        } else if ((btAlterar.getActionCommand().equals(PainelWebFotos.ACAO_ALTERAR) && ev.getSource() == btAlterar) || (btNovo.getActionCommand().equals(PainelWebFotos.ACAO_FINALIZAR) && ev.getSource() == btNovo)) {
            if (!PainelWebFotos.atualizaAlbum()) {
                return;
            }
            executaAlteracoes();
            if (ev.getSource() == btNovo) {
                PainelWebFotos.botaoFinalizar();
            }
        }
    }

    /**
     * M�todo respons�vel pelas altera��es ou cria��o de um novo alb�m. Primeiro
     * faz o registro do alb�m no banco de dados, checa se � necess�rio a
     * cria��o de um novo alb�m, criando um ID e atualizando o banco. Logo ap�s
     * registra as fotos no banco, todas as fotos s�o registradas novamente.
     * Fotos novas recebem um ID. Faz um INSERT no banco e atualiza novamente.
     * Move e renomeia os arquivos para o diret�rio do alb�m. Faz os Thumbs para
     * ajustar a dimens�o das fotos e adciona no FTP. Limpa a flag
     * CaminhoArquivo e apresenta as altera��es. E por �ltimo, executar o
     * sistema de envio por FTP.
     */
    public void executaAlteracoes() {
        Album album = Album.getAlbum();
        Photo[] fotos = album.getFotos();
        Photo f;
        int ultimoFotoID = -1;
        int albumID = album.getAlbumID();
        sucesso = true;
        PainelWebFotos.setCursorWait(true);
        albumID = recordAlbumData(album, albumID);
        sucesso = recordFotoData(fotos, ultimoFotoID, albumID);
        String caminhoAlbum = Util.getFolder("albunsRoot").getPath() + File.separator + albumID;
        File diretorioAlbum = new File(caminhoAlbum);
        if (!diretorioAlbum.isDirectory()) {
            if (!diretorioAlbum.mkdir()) {
                Util.log("[AcaoAlterarAlbum.executaAlteracoes.7]/ERRO: diretorio " + caminhoAlbum + " n�o pode ser criado. abortando");
                return;
            }
        }
        for (int i = 0; i < fotos.length; i++) {
            f = fotos[i];
            if (f.getCaminhoArquivo().length() > 0) {
                try {
                    FileChannel canalOrigem = new FileInputStream(f.getCaminhoArquivo()).getChannel();
                    FileChannel canalDestino = new FileOutputStream(caminhoAlbum + File.separator + f.getFotoID() + ".jpg").getChannel();
                    canalDestino.transferFrom(canalOrigem, 0, canalOrigem.size());
                    canalOrigem = null;
                    canalDestino = null;
                } catch (Exception e) {
                    Util.log("[AcaoAlterarAlbum.executaAlteracoes.8]/ERRO: " + e);
                    sucesso = false;
                }
            }
        }
        prepareThumbsAndFTP(fotos, albumID, caminhoAlbum);
        prepareExtraFiles(album, caminhoAlbum);
        fireChangesToGUI(fotos);
        dispatchAlbum();
        PainelWebFotos.setCursorWait(false);
    }

    /**
     * PASSO 4 - Fazer Thumbs e Adicionar em FTP
     * @param fotos
     * @param albumID
     * @param caminhoAlbum
     */
    private void prepareThumbsAndFTP(Photo[] fotos, int albumID, String caminhoAlbum) {
        Photo f;
        for (int i = 0; i < fotos.length; i++) {
            f = fotos[i];
            String caminhoArquivo;
            if (f.getCaminhoArquivo().length() > 0) {
                caminhoArquivo = caminhoAlbum + File.separator + f.getFotoID() + ".jpg";
                Thumbnail.makeThumbs(caminhoArquivo);
                CacheFTP.getCache().addCommand(CacheFTP.UPLOAD, albumID, f.getFotoID());
            }
        }
    }

    /**
     * PASSO 6 - Limpar a flag CaminhoArquivo e apresentar as altera��es
     *
     * @param fotos
     */
    private void fireChangesToGUI(Photo[] fotos) {
        Photo f;
        for (int i = 0; i < fotos.length; i++) {
            f = fotos[i];
            f.resetCaminhoArquivo();
        }
        try {
            TableModelFoto.getModel().update();
            TableModelFoto.getModel().fireTableDataChanged();
            TableModelAlbum.getModel().update();
        } catch (Exception ex) {
            ex.printStackTrace(Util.err);
        }
        TableModelAlbum.getModel().fireTableDataChanged();
        ((javax.swing.table.AbstractTableModel) tbAlbuns.getModel()).fireTableDataChanged();
        ((javax.swing.table.AbstractTableModel) tbFotos.getModel()).fireTableDataChanged();
        PainelWebFotos.alteracaoFinalizada();
        if (sucesso) {
            Util.log("Opera��o finalizada com sucesso.");
        } else {
            Util.log("Opera��o finalizada com erros.");
        }
    }

    /**
     * PASSO 5 - Preparar os arquivos adicionais
     *
     * @param album
     * @param caminhoAlbum
     */
    private void prepareExtraFiles(Album album, String caminhoAlbum) {
        try {
            FileWriter out = new FileWriter(caminhoAlbum + File.separator + album.getAlbumID() + ".js");
            out.write(album.toJavaScript());
            out.flush();
            out.close();
            out = new FileWriter(caminhoAlbum + File.separator + album.getAlbumID() + ".xml");
            out.write(album.toXML());
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace(Util.err);
        }
    }

    /**
     * PASSO 7 - Executar o sistema de envio por FTP
     */
    private void dispatchAlbum() {
        boolean autoTransferir = Util.getConfig().getBoolean("autoTransferir");
        int retorno = 1;
        if (!autoTransferir) {
            retorno = JOptionPane.showConfirmDialog(PainelWebFotos.getInstance(), "Deseja publicar agora as fotos na internet?\n\nSE CLICAR EM \"N�O\" SER� NECESS�RIO ATIVAR A PUBLICA��O MANUALMENTE!", "Publicar fotos?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }
        if (retorno == 0 || autoTransferir) {
            Thread t = new Thread(new net.sf.webphotos.gui.util.FtpClient());
            t.start();
        }
    }

    /**
     * PASSO 2 - Registrar fotos no banco de dados todas as fotos s�o
     * registradas novamente. Fotos novas recebem um ID.
     *
     * @param fotos
     * @param ultimoFotoID
     * @param albumID
     * @return
     */
    public boolean recordFotoData(Photo[] fotos, int ultimoFotoID, int albumID) {
        Photo f;
        String nomeArquivo;
        String legenda;
        int fotoID;
        int creditoID;
        int altura;
        int largura;
        String sql;
        sucesso = false;
        for (int i = 0; i < fotos.length; i++) {
            f = fotos[i];
            fotoID = f.getFotoID();
            creditoID = f.getCreditoID();
            legenda = f.getLegenda();
            altura = f.getAltura();
            largura = f.getLargura();
            nomeArquivo = f.getCaminhoArquivo();
            if (nomeArquivo.length() > 0) {
                if (ultimoFotoID < 0) {
                    try {
                        sql = "SELECT max(fotoID) FROM fotos";
                        rowSet.setCommand(sql);
                        rowSet.execute();
                        rowSet.first();
                        ultimoFotoID = rowSet.getInt(1);
                        sql = "SELECT fotoID FROM fotos ORDER BY albumID DESC LIMIT 1";
                        rowSet.setCommand(sql);
                        rowSet.execute();
                        fotoID = ++ultimoFotoID;
                        f.setFotoID(fotoID);
                        rowSet.moveToInsertRow();
                        rowSet.updateInt("fotoID", fotoID);
                        rowSet.insertRow();
                    } catch (Exception e) {
                        Util.log("[AcaoAlterarAlbum.recordFotoData]/ERRO: " + e);
                        e.printStackTrace(Util.err);
                        PainelWebFotos.setCursorWait(false);
                        sucesso = false;
                        return sucesso;
                    }
                }
            }
            try {
                sql = "SELECT fotoID,albumID,creditoID,legenda,altura,largura FROM fotos WHERE fotoID=" + fotoID;
                rowSet.setCommand(sql);
                rowSet.execute();
                rowSet.first();
                fotoID = rowSet.getInt(1);
                rowSet.updateInt("albumID", albumID);
                rowSet.updateInt("creditoID", creditoID);
                rowSet.updateInt("altura", altura);
                rowSet.updateInt("largura", largura);
                rowSet.updateString("legenda", legenda);
                rowSet.updateRow();
            } catch (Exception e) {
                Util.log("[AcaoAlterarAlbum.recordFotoData]/ERRO: " + e);
                e.printStackTrace(Util.err);
                PainelWebFotos.setCursorWait(false);
                sucesso = false;
                return sucesso;
            }
        }
        try {
            Util.log("finalizando altera��es em banco de dados");
            rowSet.refreshRow();
        } catch (Exception e) {
            Util.log("[AcaoAlterarAlbum.recordFotoData]/ERRO: " + e);
            e.printStackTrace(Util.err);
            sucesso = false;
        }
        return sucesso;
    }

    /**
     * PASSO 1 - Registrar o �lbum no banco de dados
     *
     * @param album
     * @param albumID
     * @return
     */
    public int recordAlbumData(Album album, int albumID) {
        String sql;
        String dtAnsi = "0000-00-00";
        try {
            SimpleDateFormat dataBR = new SimpleDateFormat("dd/MM/yy");
            SimpleDateFormat dataAnsi = new SimpleDateFormat("yyyy-MM-dd");
            Date d = dataBR.parse(album.getDtInsercao());
            dtAnsi = dataAnsi.format(d);
        } catch (Exception e) {
            Util.out.println("[AcaoAlterarAlbum.recordAlbumData]/ERRO: " + e);
            PainelWebFotos.setCursorWait(false);
            return 0;
        }
        if (albumID == 0) {
            try {
                sql = "SELECT MAX(albumID) FROM albuns";
                rowSet.setCommand(sql);
                rowSet.execute();
                rowSet.first();
                albumID = rowSet.getInt(1);
                sql = "SELECT albumID, DtInsercao FROM albuns ORDER BY albumID DESC LIMIT 1";
                rowSet.setCommand(sql);
                rowSet.execute();
                album.setAlbumID(++albumID);
                rowSet.moveToInsertRow();
                rowSet.updateInt("albumID", albumID);
                rowSet.updateDate("DtInsercao", new java.sql.Date((new Date()).getTime()));
                rowSet.insertRow();
            } catch (Exception e) {
                Util.log("[AcaoAlterarAlbum.recordAlbumData]/ERRO: " + e);
                e.printStackTrace(Util.err);
                sucesso = false;
                PainelWebFotos.setCursorWait(false);
                return 0;
            }
        }
        try {
            sql = "SELECT usuarioID,categoriaID,NmAlbum,Descricao,DtInsercao,albumID FROM albuns WHERE albumID=" + albumID;
            rowSet.setCommand(sql);
            rowSet.execute();
            rowSet.first();
            albumID = rowSet.getInt("albumID");
            rowSet.updateInt("categoriaID", album.getCategoriaID());
            rowSet.updateInt("usuarioID", album.getUsuarioID());
            rowSet.updateString("NmAlbum", album.getNmAlbum());
            rowSet.updateString("Descricao", album.getDescricao());
            rowSet.updateString("DtInsercao", dtAnsi);
            rowSet.updateRow();
        } catch (Exception e) {
            Util.log("[AcaoAlterarAlbum.recordAlbumData]/ERRO: " + e);
            e.printStackTrace(Util.err);
            PainelWebFotos.setCursorWait(false);
            return 0;
        }
        return albumID;
    }
}
