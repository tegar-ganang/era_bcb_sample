package br.com.petrobras.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;
import br.com.petrobras.facade.BDFacade;
import br.com.petrobras.model.ArquivoVO;
import br.com.petrobras.model.GrupoVO;
import br.com.petrobras.model.TipoLogradouroVO;
import br.com.petrobras.model.UnidadeFederativaVO;

/**
 * Classes com funcoes de manutencao para o Sistema
 *
 * @author CZT7
 *
 */
public class Manutencao {

    private static Log logger = LogFactory.getLog(Manutencao.class);

    /**
   * Limpa os arquivos, cuja ultima modificacao seja menor que determinada data.
   *
   * @param diretorio
   *          Caminho completo do diretorio para apagar os arquivos
   * @param data
   *          Data para comparar com a data de ultima modificacao dos arquivos. <br>
   *          Se data for null, apaga todos os arquivos do diretorio.
   */
    public static void limpaDiretorio(String diretorio, Date data) {
        File dir = new File(diretorio);
        File[] files = dir.listFiles();
        if (dir.exists() && files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) limpaDiretorio(files[i].getPath(), data); else {
                    if (data != null) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(data);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        if (files[i].lastModified() > calendar.getTimeInMillis()) continue;
                    }
                    logger.debug("Apagando arquivo: " + files[i].getName());
                    files[i].delete();
                }
            }
        }
    }

    /**
   * Salva o arquivo de upload
   *
   * @param input
   *          InputStream com os dados do upload
   * @param arquivo
   *          objeto Arquivo
   * @throws FileNotFoundException
   * @throws IOException
   */
    public static void salvaUpload(InputStream input, ArquivoVO arquivo) throws FileNotFoundException, IOException {
        File dir = new File(Diretorio.UPLOAD);
        if (!dir.exists()) dir.mkdir();
        BufferedInputStream in = new BufferedInputStream(input);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(arquivo.getNomeServidorCompleto()));
        for (int b = in.read(); b != -1; out.write(b), b = in.read()) ;
        in.close();
        out.flush();
        out.close();
    }

    public static void atualizaCacheServidor(ServletContext servletContext) {
        BDFacade facade = BDFacade.getInstance();
        MessageResources messages = MessageResources.getMessageResources("ApplicationResources");
        servletContext.setAttribute("messages", messages);
        List<LabelValueBean> meses = new ArrayList<LabelValueBean>();
        meses.add(new LabelValueBean("Janeiro", "1"));
        meses.add(new LabelValueBean("Fevereiro", "2"));
        meses.add(new LabelValueBean("Mar�o", "3"));
        meses.add(new LabelValueBean("Abril", "4"));
        meses.add(new LabelValueBean("Maio", "5"));
        meses.add(new LabelValueBean("Junho", "6"));
        meses.add(new LabelValueBean("Julho", "7"));
        meses.add(new LabelValueBean("Agosto", "8"));
        meses.add(new LabelValueBean("Setembro", "9"));
        meses.add(new LabelValueBean("Outubro", "10"));
        meses.add(new LabelValueBean("Novembro", "11"));
        meses.add(new LabelValueBean("Dezembro", "12"));
        servletContext.setAttribute("meses", meses);
        List<LabelValueBean> anos = new ArrayList<LabelValueBean>();
        int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 2005; i < (anoAtual + 5); i++) anos.add(new LabelValueBean(String.valueOf(i), String.valueOf(i)));
        servletContext.setAttribute("anos", anos);
        List<UnidadeFederativaVO> unidadesFederativas = null;
        List<TipoLogradouroVO> tiposLogradouros = null;
        try {
            unidadesFederativas = facade.consultarUFs(BooleanUtils.toBoolean(servletContext.getInitParameter("PETROVIDA")));
            tiposLogradouros = facade.listarTipoLogradouro();
        } catch (SQLException e) {
            logger.error("Erro de SQL na busca por UFs." + e);
        } catch (Exception e1) {
            logger.error(e1);
        }
        servletContext.setAttribute("uFs", unidadesFederativas);
        servletContext.setAttribute("siglas", tiposLogradouros);
        try {
            List<ArquivoVO> semVinculo = facade.consultarArquivosSemVinculo();
            for (ArquivoVO arquivo : semVinculo) {
                if (arquivo.existeNoFileSystem()) {
                    if (arquivo.excluirDoFileSystem()) {
                        logger.warn(String.format("Arquivo sem vinculo excluido: %s(%s) ", arquivo.getNomeServidor(), arquivo.getNome()));
                    } else {
                        logger.warn(String.format("Falha na exclusao do arquivo sem vinculo: %s(%s)", arquivo.getNomeServidor(), arquivo.getNome()));
                    }
                }
            }
        } catch (SQLException sql) {
            logger.error("Erro ao obter arquivos sem vinculo: " + sql.getMessage());
        } catch (Exception e) {
            logger.error("Erro ao obter arquivos sem vinculo: " + e.getMessage());
        }
        try {
            servletContext.setAttribute("pessoaPrincipal", facade.consultaPessoaPrincipal());
        } catch (SQLException e) {
            logger.error("Erro de SQL ao busca Pessoa Principal." + e);
        } catch (Exception e1) {
            logger.error(e1);
        }
        try {
            GrupoVO grupo = new GrupoVO();
            grupo.setAdministrador(true);
            List<GrupoVO> listaGrupos = facade.consultarGrupos(grupo);
            grupo = (listaGrupos.size() > 0) ? listaGrupos.get(0) : null;
            servletContext.setAttribute("grupoAdmGlobal", grupo);
        } catch (SQLException e) {
            logger.error("Erro de SQL ao busca Pessoa Principal." + e);
        } catch (Exception e1) {
            logger.error(e1);
        }
    }
}
