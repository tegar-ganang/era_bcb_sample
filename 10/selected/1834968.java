package net.sourceforge.dashbov.persistencia.banco_de_dados;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.xml.rpc.ServiceException;
import net.sourceforge.dashbov.entidades.Constantes;
import net.sourceforge.dashbov.entidades.Constantes.SubabaConsultaGraficos;
import net.sourceforge.dashbov.entidades.Constantes.TipoDeEventoSocietario;
import net.sourceforge.dashbov.entidades.Tipos.Andamento;
import net.sourceforge.dashbov.entidades.Tipos.CampoMalDelimitadoEmRegistroDoArquivoImportado;
import net.sourceforge.dashbov.entidades.Tipos.Cotacao;
import net.sourceforge.dashbov.entidades.Tipos.CotacaoDetalhada;
import net.sourceforge.dashbov.entidades.Tipos.CotacaoResumida;
import net.sourceforge.dashbov.entidades.Tipos.EventoSocietario;
import net.sourceforge.dashbov.entidades.Tipos.FiltroDaConsultaDeGrafico;
import net.sourceforge.dashbov.entidades.Tipos.FiltroDaConsultaDeRelatorio;
import net.sourceforge.dashbov.entidades.Tipos.ItemSelecionavel;
import net.sourceforge.dashbov.entidades.Tipos.ItemSelecionavelImpl;
import net.sourceforge.dashbov.entidades.Tipos.MesEAno;
import net.sourceforge.dashbov.entidades.Tipos.MyBoolean;
import net.sourceforge.dashbov.entidades.Tipos.PIB;
import net.sourceforge.dashbov.entidades.Tipos.ParametrosParaConexaoComBancoDeDados;
import net.sourceforge.dashbov.entidades.Tipos.Pregao;
import net.sourceforge.dashbov.entidades.Tipos.ProblemaNaImportacaoDeArquivo;
import net.sourceforge.dashbov.entidades.Tipos.ProcessamentoAnteriorAindaNaoEncerrado;
import net.sourceforge.dashbov.entidades.Tipos.RelacaoBolsaPorPIB;
import net.sourceforge.dashbov.persistencia.webservice.ManipuladorDeWebservice;
import net.sourceforge.dashbov.persistencia.webservice.bacen.WSValorSerieVO;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

/**
 *
 * @author Everton B. Gomes
 */
public class ManipuladorDeBancoDeDados {

    private Connection conDestino;

    private ParametrosParaConexaoComBancoDeDados parametrizacaoDaConexao;

    public ManipuladorDeBancoDeDados(ParametrosParaConexaoComBancoDeDados pParametros) {
        try {
            OracleDataSource ds = new oracle.jdbc.pool.OracleConnectionPoolDataSource();
            ds.setDriverType("thin");
            ds.setServerName(pParametros.nomeDoServidor);
            ds.setPortNumber(pParametros.numeroDaPorta);
            ds.setDatabaseName(pParametros.siglaDaInstancia);
            ds.setUser(pParametros.nomeDoUsuario);
            ds.setPassword(pParametros.senha);
            ds.setDescription("DashBo↗v database connection");
            this.conDestino = ds.getConnection();
            this.conDestino.setReadOnly(pParametros.somenteLeitura);
            this.conDestino.setAutoCommit(false);
            this.parametrizacaoDaConexao = pParametros;
        } catch (SQLException ex) {
            throw new IllegalArgumentException("Banco de dados não localizado!", ex);
        }
    }

    @Override
    public ManipuladorDeBancoDeDados clone() {
        ManipuladorDeBancoDeDados clone = new ManipuladorDeBancoDeDados(this.parametrizacaoDaConexao);
        return clone;
    }

    /**
     * Realiza carga cumulativa de dados de arquivo *.csv para tabela do banco.
     */
    public void criarBancoDeDados(Reader pScriptSQL, String pDelimitador, boolean pComandosAgrupadosEmBlocos) throws FileNotFoundException, SQLException {
        ScriptRunner runner = new ScriptRunner(this.conDestino);
        runner.setDelimiter(pDelimitador);
        runner.setFullLineDelimiter(pComandosAgrupadosEmBlocos);
        runner.setStopOnError(true);
        runner.runScript(pScriptSQL);
        this.conDestino.commit();
    }

    final int COMANDOS_POR_LOTE = 50;

    /**
     * Realiza carga cumulativa de dados de arquivo *.txt para tabela do banco.
     * Formato: http://www.bmfbovespa.com.br/pt-br/download/SeriesHistoricas_Layout.pdf
     */
    public void importarHistoricoDeCotacoesDosPapeis(File[] pArquivosTXT, boolean pApagarDadosImportadosAnteriormente, Andamento pAndamento) throws FileNotFoundException, SQLException {
        if (pApagarDadosImportadosAnteriormente) {
            Statement stmtLimpezaInicialDestino = conDestino.createStatement();
            String sql = "TRUNCATE TABLE TMP_TB_COTACAO_AVISTA_LOTE_PDR";
            stmtLimpezaInicialDestino.executeUpdate(sql);
            sql = "TRUNCATE TABLE TMP_TB_COTACAO_OUTROS_MERCADOS";
            stmtLimpezaInicialDestino.executeUpdate(sql);
        }
        final int TAMANHO_DO_REGISTRO = 245;
        long TAMANHO_DOS_METADADOS_DO_ARQUIVO = 2 * TAMANHO_DO_REGISTRO;
        long tamanhoDosArquivos = 0;
        for (File arquivoTXT : pArquivosTXT) {
            long tamanhoDoArquivo = arquivoTXT.length();
            tamanhoDosArquivos += tamanhoDoArquivo;
        }
        int quantidadeEstimadaDeRegistros = (int) ((tamanhoDosArquivos - (pArquivosTXT.length * TAMANHO_DOS_METADADOS_DO_ARQUIVO)) / TAMANHO_DO_REGISTRO);
        String sqlMercadoAVistaLotePadrao = "INSERT INTO TMP_TB_COTACAO_AVISTA_LOTE_PDR(DATA_PREGAO, CODBDI, CODNEG, TPMERC, NOMRES, ESPECI, PRAZOT, MODREF, PREABE, PREMAX, PREMIN, PREMED, PREULT, PREOFC, PREOFV, TOTNEG, QUATOT, VOLTOT, PREEXE, INDOPC, DATVEN, FATCOT, PTOEXE, CODISI, DISMES) VALUES(:DATA_PREGAO, :CODBDI, :CODNEG, :TPMERC, :NOMRES, :ESPECI, :PRAZOT, :MODREF, :PREABE, :PREMAX, :PREMIN, :PREMED, :PREULT, :PREOFC, :PREOFV, :TOTNEG, :QUATOT, :VOLTOT, :PREEXE, :INDOPC, :DATVEN, :FATCOT, :PTOEXE, :CODISI, :DISMES)";
        OraclePreparedStatement stmtDestinoMercadoAVistaLotePadrao = (OraclePreparedStatement) conDestino.prepareStatement(sqlMercadoAVistaLotePadrao);
        stmtDestinoMercadoAVistaLotePadrao.setExecuteBatch(COMANDOS_POR_LOTE);
        String sqlOutrosMercados = "INSERT INTO TMP_TB_COTACAO_OUTROS_MERCADOS(DATA_PREGAO, CODBDI, CODNEG, TPMERC, NOMRES, ESPECI, PRAZOT, MODREF, PREABE, PREMAX, PREMIN, PREMED, PREULT, PREOFC, PREOFV, TOTNEG, QUATOT, VOLTOT, PREEXE, INDOPC, DATVEN, FATCOT, PTOEXE, CODISI, DISMES) VALUES(:DATA_PREGAO, :CODBDI, :CODNEG, :TPMERC, :NOMRES, :ESPECI, :PRAZOT, :MODREF, :PREABE, :PREMAX, :PREMIN, :PREMED, :PREULT, :PREOFC, :PREOFV, :TOTNEG, :QUATOT, :VOLTOT, :PREEXE, :INDOPC, :DATVEN, :FATCOT, :PTOEXE, :CODISI, :DISMES)";
        OraclePreparedStatement stmtDestinoOutrosMercados = (OraclePreparedStatement) conDestino.prepareStatement(sqlOutrosMercados);
        stmtDestinoOutrosMercados.setExecuteBatch(COMANDOS_POR_LOTE);
        int quantidadeDeRegistrosImportadosDosArquivos = 0;
        Scanner in = null;
        int numeroDoRegistro = -1;
        try {
            for (File arquivoTXT : pArquivosTXT) {
                int quantidadeDeRegistrosImportadosDoArquivoAtual = 0;
                int vDATA_PREGAO;
                try {
                    in = new Scanner(new FileInputStream(arquivoTXT), Constantes.CONJUNTO_DE_CARACTERES_DOS_ARQUIVOS_TEXTO_DA_BOVESPA.name());
                    String registro;
                    numeroDoRegistro = 0;
                    while (in.hasNextLine()) {
                        ++numeroDoRegistro;
                        registro = in.nextLine();
                        if (registro.length() != TAMANHO_DO_REGISTRO) throw new ProblemaNaImportacaoDeArquivo();
                        if (registro.startsWith("01")) {
                            stmtDestinoMercadoAVistaLotePadrao.clearParameters();
                            stmtDestinoOutrosMercados.clearParameters();
                            vDATA_PREGAO = Integer.parseInt(registro.substring(2, 10).trim());
                            int vCODBDI = Integer.parseInt(registro.substring(10, 12).trim());
                            String vCODNEG = registro.substring(12, 24).trim();
                            int vTPMERC = Integer.parseInt(registro.substring(24, 27).trim());
                            String vNOMRES = registro.substring(27, 39).trim();
                            String vESPECI = registro.substring(39, 49).trim();
                            String vPRAZOT = registro.substring(49, 52).trim();
                            String vMODREF = registro.substring(52, 56).trim();
                            BigDecimal vPREABE = obterBigDecimal(registro.substring(56, 69).trim(), 13, 2);
                            BigDecimal vPREMAX = obterBigDecimal(registro.substring(69, 82).trim(), 13, 2);
                            BigDecimal vPREMIN = obterBigDecimal(registro.substring(82, 95).trim(), 13, 2);
                            BigDecimal vPREMED = obterBigDecimal(registro.substring(95, 108).trim(), 13, 2);
                            BigDecimal vPREULT = obterBigDecimal(registro.substring(108, 121).trim(), 13, 2);
                            BigDecimal vPREOFC = obterBigDecimal(registro.substring(121, 134).trim(), 13, 2);
                            BigDecimal vPREOFV = obterBigDecimal(registro.substring(134, 147).trim(), 13, 2);
                            int vTOTNEG = Integer.parseInt(registro.substring(147, 152).trim());
                            BigDecimal vQUATOT = new BigDecimal(registro.substring(152, 170).trim());
                            BigDecimal vVOLTOT = obterBigDecimal(registro.substring(170, 188).trim(), 18, 2);
                            BigDecimal vPREEXE = obterBigDecimal(registro.substring(188, 201).trim(), 13, 2);
                            int vINDOPC = Integer.parseInt(registro.substring(201, 202).trim());
                            int vDATVEN = Integer.parseInt(registro.substring(202, 210).trim());
                            int vFATCOT = Integer.parseInt(registro.substring(210, 217).trim());
                            BigDecimal vPTOEXE = obterBigDecimal(registro.substring(217, 230).trim(), 13, 6);
                            String vCODISI = registro.substring(230, 242).trim();
                            int vDISMES = Integer.parseInt(registro.substring(242, 245).trim());
                            boolean mercadoAVistaLotePadrao = (vTPMERC == 10 && vCODBDI == 2);
                            OraclePreparedStatement stmtDestino;
                            if (mercadoAVistaLotePadrao) {
                                stmtDestino = stmtDestinoMercadoAVistaLotePadrao;
                            } else {
                                stmtDestino = stmtDestinoOutrosMercados;
                            }
                            stmtDestino.setIntAtName("DATA_PREGAO", vDATA_PREGAO);
                            stmtDestino.setIntAtName("CODBDI", vCODBDI);
                            stmtDestino.setStringAtName("CODNEG", vCODNEG);
                            stmtDestino.setIntAtName("TPMERC", vTPMERC);
                            stmtDestino.setStringAtName("NOMRES", vNOMRES);
                            stmtDestino.setStringAtName("ESPECI", vESPECI);
                            stmtDestino.setStringAtName("PRAZOT", vPRAZOT);
                            stmtDestino.setStringAtName("MODREF", vMODREF);
                            stmtDestino.setBigDecimalAtName("PREABE", vPREABE);
                            stmtDestino.setBigDecimalAtName("PREMAX", vPREMAX);
                            stmtDestino.setBigDecimalAtName("PREMIN", vPREMIN);
                            stmtDestino.setBigDecimalAtName("PREMED", vPREMED);
                            stmtDestino.setBigDecimalAtName("PREULT", vPREULT);
                            stmtDestino.setBigDecimalAtName("PREOFC", vPREOFC);
                            stmtDestino.setBigDecimalAtName("PREOFV", vPREOFV);
                            stmtDestino.setIntAtName("TOTNEG", vTOTNEG);
                            stmtDestino.setBigDecimalAtName("QUATOT", vQUATOT);
                            stmtDestino.setBigDecimalAtName("VOLTOT", vVOLTOT);
                            stmtDestino.setBigDecimalAtName("PREEXE", vPREEXE);
                            stmtDestino.setIntAtName("INDOPC", vINDOPC);
                            stmtDestino.setIntAtName("DATVEN", vDATVEN);
                            stmtDestino.setIntAtName("FATCOT", vFATCOT);
                            stmtDestino.setBigDecimalAtName("PTOEXE", vPTOEXE);
                            stmtDestino.setStringAtName("CODISI", vCODISI);
                            stmtDestino.setIntAtName("DISMES", vDISMES);
                            int contagemDasInsercoes = stmtDestino.executeUpdate();
                            quantidadeDeRegistrosImportadosDoArquivoAtual++;
                            quantidadeDeRegistrosImportadosDosArquivos++;
                        } else if (registro.startsWith("99")) {
                            BigDecimal totalDeRegistros = obterBigDecimal(registro.substring(31, 42).trim(), 11, 0);
                            assert (totalDeRegistros.intValue() - 2) == quantidadeDeRegistrosImportadosDoArquivoAtual : "Quantidade de registros divergente";
                            break;
                        }
                        double percentualCompleto = (double) quantidadeDeRegistrosImportadosDosArquivos / quantidadeEstimadaDeRegistros * 100;
                        pAndamento.setPercentualCompleto((int) percentualCompleto);
                    }
                    conDestino.commit();
                } catch (Exception ex) {
                    conDestino.rollback();
                    ProblemaNaImportacaoDeArquivo problemaDetalhado = new ProblemaNaImportacaoDeArquivo();
                    problemaDetalhado.nomeDoArquivo = arquivoTXT.getName();
                    problemaDetalhado.linhaProblematicaDoArquivo = numeroDoRegistro;
                    problemaDetalhado.detalhesSobreOProblema = ex;
                    throw problemaDetalhado;
                } finally {
                    in.close();
                }
            }
        } finally {
            pAndamento.setPercentualCompleto(100);
            stmtDestinoMercadoAVistaLotePadrao.close();
            stmtDestinoOutrosMercados.close();
        }
    }

    private enum CampoDaPlanilhaDosProventosEmDinheiro {

        NOME_DE_PREGAO, TIPO_DA_ACAO, DATA_DA_APROVACAO, VALOR_DO_PROVENTO, PROVENTO_POR_1_OU_1000_ACOES, TIPO_DO_PROVENTO, ULTIMO_DIA_COM, DATA_DO_ULTIMO_PRECO_COM, ULTIMO_PRECO_COM, PRECO_POR_1_OU_1000_ACOES, PROVENTO_POR_PRECO
    }

    ;

    public void importarHistoricoDeProventos(File pArquivoXLS, boolean pFiltrarPelaDataDeCorteDoCabecalho, Andamento pAndamento) throws IOException, SQLException, InvalidFormatException {
        int iLinha = -1;
        String nomeDaColuna = "";
        Statement stmtLimpezaInicialDestino = null;
        OraclePreparedStatement stmtDestino = null;
        try {
            Workbook arquivo = WorkbookFactory.create(new FileInputStream(pArquivoXLS));
            Sheet plan1 = arquivo.getSheetAt(0);
            int QUANTIDADE_DE_REGISTROS_DE_METADADOS = 2;
            int quantidadeDeRegistrosEstimada = plan1.getPhysicalNumberOfRows() - QUANTIDADE_DE_REGISTROS_DE_METADADOS;
            String vNomeDePregao, vTipoDaAcao, vDataDaAprovacao, vTipoDoProvento, vDataDoUltimoPrecoCom;
            BigDecimal vValorDoProvento, vUltimoPrecoCom, vProventoPorPreco;
            int vProventoPor1Ou1000Acoes, vPrecoPor1Ou1000Acoes;
            java.sql.Date vUltimoDiaCom;
            DateFormat formatadorData = new SimpleDateFormat("yyyyMMdd");
            DateFormat formatadorPadraoData = DateFormat.getDateInstance();
            Row registro;
            Cell celula;
            java.util.Date dataLimite = plan1.getRow(0).getCell(CampoDaPlanilhaDosProventosEmDinheiro.NOME_DE_PREGAO.ordinal()).getDateCellValue();
            Cell celulaUltimoDiaCom;
            java.util.Date tmpUltimoDiaCom;
            stmtLimpezaInicialDestino = conDestino.createStatement();
            String sql = "TRUNCATE TABLE TMP_TB_PROVENTO_EM_DINHEIRO";
            stmtLimpezaInicialDestino.executeUpdate(sql);
            sql = "INSERT INTO TMP_TB_PROVENTO_EM_DINHEIRO(NOME_DE_PREGAO, TIPO_DA_ACAO, DATA_DA_APROVACAO, VALOR_DO_PROVENTO, PROVENTO_POR_1_OU_1000_ACOES, TIPO_DO_PROVENTO, ULTIMO_DIA_COM, DATA_DO_ULTIMO_PRECO_COM, ULTIMO_PRECO_COM, PRECO_POR_1_OU_1000_ACOES, PERC_PROVENTO_POR_PRECO) VALUES(:NOME_DE_PREGAO, :TIPO_DA_ACAO, :DATA_DA_APROVACAO, :VALOR_DO_PROVENTO, :PROVENTO_POR_1_OU_1000_ACOES, :TIPO_DO_PROVENTO, :ULTIMO_DIA_COM, :DATA_DO_ULTIMO_PRECO_COM, :ULTIMO_PRECO_COM, :PRECO_POR_1_OU_1000_ACOES, :PERC_PROVENTO_POR_PRECO)";
            stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
            stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
            int quantidadeDeRegistrosImportados = 0;
            final int NUMERO_DA_LINHA_INICIAL = 1;
            for (iLinha = NUMERO_DA_LINHA_INICIAL; true; iLinha++) {
                registro = plan1.getRow(iLinha);
                if (registro != null) {
                    nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.ULTIMO_DIA_COM.toString();
                    celulaUltimoDiaCom = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.ULTIMO_DIA_COM.ordinal());
                    if (celulaUltimoDiaCom != null) {
                        if (celulaUltimoDiaCom.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            tmpUltimoDiaCom = celulaUltimoDiaCom.getDateCellValue();
                            if (tmpUltimoDiaCom.compareTo(dataLimite) <= 0 || !pFiltrarPelaDataDeCorteDoCabecalho) {
                                vUltimoDiaCom = new java.sql.Date(celulaUltimoDiaCom.getDateCellValue().getTime());
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.NOME_DE_PREGAO.toString();
                                vNomeDePregao = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.NOME_DE_PREGAO.ordinal()).getStringCellValue().trim();
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.TIPO_DA_ACAO.toString();
                                vTipoDaAcao = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.TIPO_DA_ACAO.ordinal()).getStringCellValue().trim();
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.DATA_DA_APROVACAO.toString();
                                celula = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.DATA_DA_APROVACAO.ordinal());
                                try {
                                    java.util.Date tmpDataDaAprovacao;
                                    if (celula.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                        tmpDataDaAprovacao = celula.getDateCellValue();
                                    } else {
                                        tmpDataDaAprovacao = formatadorPadraoData.parse(celula.getStringCellValue());
                                    }
                                    vDataDaAprovacao = formatadorData.format(tmpDataDaAprovacao);
                                } catch (ParseException ex) {
                                    vDataDaAprovacao = celula.getStringCellValue();
                                }
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.VALOR_DO_PROVENTO.toString();
                                vValorDoProvento = new BigDecimal(String.valueOf(registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.VALOR_DO_PROVENTO.ordinal()).getNumericCellValue()));
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.PROVENTO_POR_1_OU_1000_ACOES.toString();
                                vProventoPor1Ou1000Acoes = (int) registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.PROVENTO_POR_1_OU_1000_ACOES.ordinal()).getNumericCellValue();
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.TIPO_DO_PROVENTO.toString();
                                vTipoDoProvento = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.TIPO_DO_PROVENTO.ordinal()).getStringCellValue().trim();
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.DATA_DO_ULTIMO_PRECO_COM.toString();
                                celula = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.DATA_DO_ULTIMO_PRECO_COM.ordinal());
                                if (celula != null) {
                                    try {
                                        java.util.Date tmpDataDoUltimoPrecoCom;
                                        if (celula.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                            tmpDataDoUltimoPrecoCom = celula.getDateCellValue();
                                        } else {
                                            tmpDataDoUltimoPrecoCom = formatadorPadraoData.parse(celula.getStringCellValue());
                                        }
                                        vDataDoUltimoPrecoCom = formatadorData.format(tmpDataDoUltimoPrecoCom);
                                    } catch (ParseException ex) {
                                        vDataDoUltimoPrecoCom = celula.getStringCellValue().trim();
                                    }
                                } else {
                                    vDataDoUltimoPrecoCom = "";
                                }
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.ULTIMO_PRECO_COM.toString();
                                vUltimoPrecoCom = new BigDecimal(String.valueOf(registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.ULTIMO_PRECO_COM.ordinal()).getNumericCellValue()));
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.PRECO_POR_1_OU_1000_ACOES.toString();
                                vPrecoPor1Ou1000Acoes = (int) registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.PRECO_POR_1_OU_1000_ACOES.ordinal()).getNumericCellValue();
                                nomeDaColuna = CampoDaPlanilhaDosProventosEmDinheiro.PROVENTO_POR_PRECO.toString();
                                celula = registro.getCell(CampoDaPlanilhaDosProventosEmDinheiro.PROVENTO_POR_PRECO.ordinal());
                                if (celula != null && celula.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                    vProventoPorPreco = new BigDecimal(String.valueOf(celula.getNumericCellValue()));
                                } else {
                                    vProventoPorPreco = null;
                                }
                                stmtDestino.clearParameters();
                                stmtDestino.setStringAtName("NOME_DE_PREGAO", vNomeDePregao);
                                stmtDestino.setStringAtName("TIPO_DA_ACAO", vTipoDaAcao);
                                stmtDestino.setStringAtName("DATA_DA_APROVACAO", vDataDaAprovacao);
                                stmtDestino.setBigDecimalAtName("VALOR_DO_PROVENTO", vValorDoProvento);
                                stmtDestino.setIntAtName("PROVENTO_POR_1_OU_1000_ACOES", vProventoPor1Ou1000Acoes);
                                stmtDestino.setStringAtName("TIPO_DO_PROVENTO", vTipoDoProvento);
                                stmtDestino.setDateAtName("ULTIMO_DIA_COM", vUltimoDiaCom);
                                stmtDestino.setStringAtName("DATA_DO_ULTIMO_PRECO_COM", vDataDoUltimoPrecoCom);
                                stmtDestino.setBigDecimalAtName("ULTIMO_PRECO_COM", vUltimoPrecoCom);
                                stmtDestino.setIntAtName("PRECO_POR_1_OU_1000_ACOES", vPrecoPor1Ou1000Acoes);
                                stmtDestino.setBigDecimalAtName("PERC_PROVENTO_POR_PRECO", vProventoPorPreco);
                                int contagemDasInsercoes = stmtDestino.executeUpdate();
                                quantidadeDeRegistrosImportados++;
                            }
                        }
                    } else {
                        break;
                    }
                    double percentualCompleto = (double) quantidadeDeRegistrosImportados / quantidadeDeRegistrosEstimada * 100;
                    pAndamento.setPercentualCompleto((int) percentualCompleto);
                } else {
                    break;
                }
            }
            conDestino.commit();
        } catch (Exception ex) {
            conDestino.rollback();
            ProblemaNaImportacaoDeArquivo problemaDetalhado = new ProblemaNaImportacaoDeArquivo();
            problemaDetalhado.nomeDoArquivo = pArquivoXLS.getName();
            problemaDetalhado.linhaProblematicaDoArquivo = iLinha + 1;
            problemaDetalhado.colunaProblematicaDoArquivo = nomeDaColuna;
            problemaDetalhado.detalhesSobreOProblema = ex;
            throw problemaDetalhado;
        } finally {
            pAndamento.setPercentualCompleto(100);
            if (stmtLimpezaInicialDestino != null && (!stmtLimpezaInicialDestino.isClosed())) {
                stmtLimpezaInicialDestino.close();
            }
            if (stmtDestino != null && (!stmtDestino.isClosed())) {
                stmtDestino.close();
            }
        }
    }

    public void importarBancoDeDadosDARI(File pArquivoXLS, Andamento pAndamento) throws IOException, SQLException, InvalidFormatException {
        final String ABA_VALOR_DE_MERCADO = "Valor de Mercado";
        final int COLUNA_DATA = 1, COLUNA_ANO = 6, COLUNA_VALOR_DE_MERCADO_DIARIO_EM_BILHOES_DE_REAIS = 2, COLUNA_VALOR_DE_MERCADO_DIARIO_EM_BILHOES_DE_DOLARES = 3, COLUNA_VALOR_DE_MERCADO_ANUAL_EM_BILHOES_DE_REAIS = 7, COLUNA_VALOR_DE_MERCADO_ANUAL_EM_BILHOES_DE_DOLARES = 8;
        final BigDecimal BILHAO = new BigDecimal("1000000000");
        int iLinha = -1;
        Statement stmtLimpezaInicialDestino = null;
        OraclePreparedStatement stmtDestino = null;
        try {
            Workbook arquivo = WorkbookFactory.create(new FileInputStream(pArquivoXLS));
            Sheet planilhaValorDeMercado = arquivo.getSheet(ABA_VALOR_DE_MERCADO);
            int QUANTIDADE_DE_REGISTROS_DE_METADADOS = 7;
            final Calendar DATA_INICIAL = Calendar.getInstance();
            DATA_INICIAL.setTime(planilhaValorDeMercado.getRow(QUANTIDADE_DE_REGISTROS_DE_METADADOS).getCell(COLUNA_DATA).getDateCellValue());
            final int ANO_DA_DATA_INICIAL = DATA_INICIAL.get(Calendar.YEAR);
            final int ANO_INICIAL = Integer.parseInt(planilhaValorDeMercado.getRow(QUANTIDADE_DE_REGISTROS_DE_METADADOS).getCell(COLUNA_ANO).getStringCellValue());
            final int ANO_FINAL = Calendar.getInstance().get(Calendar.YEAR);
            Row registro;
            int quantidadeDeRegistrosAnuaisEstimada = (ANO_FINAL - ANO_INICIAL + 1), quantidadeDeRegistrosDiariosEstimada = (planilhaValorDeMercado.getPhysicalNumberOfRows() - QUANTIDADE_DE_REGISTROS_DE_METADADOS);
            final int quantidadeDeRegistrosEstimada = quantidadeDeRegistrosAnuaisEstimada + quantidadeDeRegistrosDiariosEstimada;
            int vAno;
            BigDecimal vValorDeMercadoEmReais, vValorDeMercadoEmDolares;
            Cell celulaDoAno, celulaDoValorDeMercadoEmReais, celulaDoValorDeMercadoEmDolares;
            stmtLimpezaInicialDestino = conDestino.createStatement();
            String sql = "TRUNCATE TABLE TMP_TB_VALOR_MERCADO_BOLSA";
            stmtLimpezaInicialDestino.executeUpdate(sql);
            sql = "INSERT INTO TMP_TB_VALOR_MERCADO_BOLSA(DATA, VALOR_DE_MERCADO_REAL, VALOR_DE_MERCADO_DOLAR) VALUES(:DATA, :VALOR_DE_MERCADO_REAL, :VALOR_DE_MERCADO_DOLAR)";
            stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
            stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
            int quantidadeDeRegistrosImportados = 0;
            Calendar calendario = Calendar.getInstance();
            calendario.clear();
            calendario.set(Calendar.MONTH, Calendar.DECEMBER);
            calendario.set(Calendar.DAY_OF_MONTH, 31);
            for (iLinha = QUANTIDADE_DE_REGISTROS_DE_METADADOS; true; iLinha++) {
                registro = planilhaValorDeMercado.getRow(iLinha);
                celulaDoAno = registro.getCell(COLUNA_ANO);
                String anoTmp = celulaDoAno.getStringCellValue();
                if (anoTmp != null && anoTmp.length() > 0) {
                    vAno = Integer.parseInt(anoTmp);
                    if (vAno < ANO_DA_DATA_INICIAL) {
                        celulaDoValorDeMercadoEmReais = registro.getCell(COLUNA_VALOR_DE_MERCADO_ANUAL_EM_BILHOES_DE_REAIS);
                        celulaDoValorDeMercadoEmDolares = registro.getCell(COLUNA_VALOR_DE_MERCADO_ANUAL_EM_BILHOES_DE_DOLARES);
                    } else {
                        break;
                    }
                    calendario.set(Calendar.YEAR, vAno);
                    java.sql.Date vUltimoDiaDoAno = new java.sql.Date(calendario.getTimeInMillis());
                    vValorDeMercadoEmReais = new BigDecimal(celulaDoValorDeMercadoEmReais.getNumericCellValue()).multiply(BILHAO).setScale(0, RoundingMode.DOWN);
                    vValorDeMercadoEmDolares = new BigDecimal(celulaDoValorDeMercadoEmDolares.getNumericCellValue()).multiply(BILHAO).setScale(0, RoundingMode.DOWN);
                    stmtDestino.clearParameters();
                    stmtDestino.setDateAtName("DATA", vUltimoDiaDoAno);
                    stmtDestino.setBigDecimalAtName("VALOR_DE_MERCADO_REAL", vValorDeMercadoEmReais);
                    stmtDestino.setBigDecimalAtName("VALOR_DE_MERCADO_DOLAR", vValorDeMercadoEmDolares);
                    int contagemDasInsercoes = stmtDestino.executeUpdate();
                    quantidadeDeRegistrosImportados++;
                } else {
                    break;
                }
                double percentualCompleto = (double) quantidadeDeRegistrosImportados / quantidadeDeRegistrosEstimada * 100;
                pAndamento.setPercentualCompleto((int) percentualCompleto);
            }
            java.util.Date dataAnterior = null;
            String dataTmp;
            final DateFormat formatadorDeData_ddMMyyyy = new SimpleDateFormat("dd/MM/yyyy", Constantes.IDIOMA_PORTUGUES_BRASILEIRO);
            final DateFormat formatadorDeData_ddMMMyyyy = new SimpleDateFormat("dd/MMM/yyyy", Constantes.IDIOMA_PORTUGUES_BRASILEIRO);
            Cell celulaDaData;
            for (iLinha = QUANTIDADE_DE_REGISTROS_DE_METADADOS; true; iLinha++) {
                registro = planilhaValorDeMercado.getRow(iLinha);
                if (registro != null) {
                    celulaDaData = registro.getCell(COLUNA_DATA);
                    java.util.Date data;
                    if (celulaDaData.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        data = celulaDaData.getDateCellValue();
                    } else {
                        dataTmp = celulaDaData.getStringCellValue();
                        try {
                            data = formatadorDeData_ddMMyyyy.parse(dataTmp);
                        } catch (ParseException ex) {
                            data = formatadorDeData_ddMMMyyyy.parse(dataTmp);
                        }
                    }
                    if (dataAnterior == null || data.after(dataAnterior)) {
                        celulaDoValorDeMercadoEmReais = registro.getCell(COLUNA_VALOR_DE_MERCADO_DIARIO_EM_BILHOES_DE_REAIS);
                        celulaDoValorDeMercadoEmDolares = registro.getCell(COLUNA_VALOR_DE_MERCADO_DIARIO_EM_BILHOES_DE_DOLARES);
                        java.sql.Date vData = new java.sql.Date(data.getTime());
                        vValorDeMercadoEmReais = new BigDecimal(celulaDoValorDeMercadoEmReais.getNumericCellValue()).multiply(BILHAO).setScale(0, RoundingMode.DOWN);
                        vValorDeMercadoEmDolares = new BigDecimal(celulaDoValorDeMercadoEmDolares.getNumericCellValue()).multiply(BILHAO).setScale(0, RoundingMode.DOWN);
                        stmtDestino.clearParameters();
                        stmtDestino.setDateAtName("DATA", vData);
                        stmtDestino.setBigDecimalAtName("VALOR_DE_MERCADO_REAL", vValorDeMercadoEmReais);
                        stmtDestino.setBigDecimalAtName("VALOR_DE_MERCADO_DOLAR", vValorDeMercadoEmDolares);
                        int contagemDasInsercoes = stmtDestino.executeUpdate();
                        quantidadeDeRegistrosImportados++;
                        double percentualCompleto = (double) quantidadeDeRegistrosImportados / quantidadeDeRegistrosEstimada * 100;
                        pAndamento.setPercentualCompleto((int) percentualCompleto);
                    }
                    dataAnterior = data;
                } else {
                    break;
                }
            }
            conDestino.commit();
        } catch (Exception ex) {
            conDestino.rollback();
            ProblemaNaImportacaoDeArquivo problemaDetalhado = new ProblemaNaImportacaoDeArquivo();
            problemaDetalhado.nomeDoArquivo = pArquivoXLS.getName();
            problemaDetalhado.linhaProblematicaDoArquivo = iLinha;
            problemaDetalhado.detalhesSobreOProblema = ex;
            throw problemaDetalhado;
        } finally {
            pAndamento.setPercentualCompleto(100);
            if (stmtLimpezaInicialDestino != null && (!stmtLimpezaInicialDestino.isClosed())) {
                stmtLimpezaInicialDestino.close();
            }
            if (stmtDestino != null && (!stmtDestino.isClosed())) {
                stmtDestino.close();
            }
        }
    }

    private class Periodo {

        public java.util.Date dataInicial;

        public java.util.Date dataFinal;
    }

    private WSValorSerieVO[] obterCotacoesPendentesDoDolar(Andamento pAndamento) throws SQLException, ServiceException, IOException {
        WSValorSerieVO[] cotacoesPendentesDoDolar = null;
        Pregao[] pregoesSemTaxaDeCambio = obterPregoesSemTaxaDeCambio();
        if (pregoesSemTaxaDeCambio != null && pregoesSemTaxaDeCambio.length > 0) {
            Pregao pregaoAnterior = null;
            List<Periodo> periodos = new ArrayList<Periodo>();
            Periodo intervaloAtual = new Periodo();
            intervaloAtual.dataInicial = pregoesSemTaxaDeCambio[0].data;
            int tamanhoDoLoteAtual = 0;
            final int TAMANHO_MAXIMO_POR_LOTE = 100;
            for (int idxPregao = 0; idxPregao < pregoesSemTaxaDeCambio.length; idxPregao++) {
                Pregao pregaoAtual = pregoesSemTaxaDeCambio[idxPregao];
                if (idxPregao > 0) {
                    pregaoAnterior = pregoesSemTaxaDeCambio[idxPregao - 1];
                }
                boolean ultimoPregaoSemTaxaDeCambio = (idxPregao == (pregoesSemTaxaDeCambio.length - 1));
                tamanhoDoLoteAtual++;
                if ((pregaoAnterior == null || pregaoAtual.codigo == pregaoAnterior.codigo + 1) && tamanhoDoLoteAtual < TAMANHO_MAXIMO_POR_LOTE && (!ultimoPregaoSemTaxaDeCambio)) {
                    continue;
                }
                intervaloAtual.dataFinal = (ultimoPregaoSemTaxaDeCambio ? pregaoAtual.data : pregaoAnterior.data);
                periodos.add(intervaloAtual);
                intervaloAtual = new Periodo();
                intervaloAtual.dataInicial = pregaoAtual.data;
                tamanhoDoLoteAtual = 0;
            }
            List<WSValorSerieVO> acumuladorDasCotacoesPendentesDoDolar = new ArrayList<WSValorSerieVO>();
            String dataInicial_ddmmaaaa, dataFinal_ddmmaaaa;
            SimpleDateFormat formatadorDeData = new SimpleDateFormat("dd/MM/yyyy");
            int totalDePeriodos = periodos.size(), periodosObtidos;
            for (int idxPeriodo = 0; idxPeriodo < totalDePeriodos; idxPeriodo++) {
                Periodo intervalo = periodos.get(idxPeriodo);
                dataInicial_ddmmaaaa = formatadorDeData.format(intervalo.dataInicial);
                dataFinal_ddmmaaaa = formatadorDeData.format(intervalo.dataFinal);
                List<WSValorSerieVO> cotacoesPendentesDoDolarTmp = ManipuladorDeWebservice.obterTaxaDeCambioDeRealParaDolar(dataInicial_ddmmaaaa, dataFinal_ddmmaaaa);
                if (cotacoesPendentesDoDolarTmp != null && cotacoesPendentesDoDolarTmp.size() > 0) {
                    acumuladorDasCotacoesPendentesDoDolar.addAll(cotacoesPendentesDoDolarTmp);
                }
                periodosObtidos = idxPeriodo + 1;
                pAndamento.setPercentualCompleto((int) (((double) periodosObtidos / totalDePeriodos) * 100));
            }
            cotacoesPendentesDoDolar = acumuladorDasCotacoesPendentesDoDolar.toArray(new WSValorSerieVO[acumuladorDasCotacoesPendentesDoDolar.size()]);
        }
        pAndamento.setPercentualCompleto(100);
        return cotacoesPendentesDoDolar;
    }

    /**
     * Realiza carga cumulativa de dados de arquivo *.csv para tabela do banco.
     */
    public void importarHistoricoDeCotacoesDoDolar(Andamento pAndamento) throws FileNotFoundException, SQLException, Exception {
        pAndamento.delimitarIntervaloDeVariacao(0, 49);
        WSValorSerieVO[] cotacoesPendentesDoDolar = obterCotacoesPendentesDoDolar(pAndamento);
        pAndamento.delimitarIntervaloDeVariacao(50, 100);
        if (cotacoesPendentesDoDolar != null && cotacoesPendentesDoDolar.length > 0) {
            String sql = "INSERT INTO tmp_TB_COTACAO_DOLAR(DATA, PRECO) VALUES(:DATA, :PRECO)";
            OraclePreparedStatement stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
            stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
            int quantidadeDeRegistrosASeremImportados = cotacoesPendentesDoDolar.length;
            try {
                int quantidadeDeRegistrosImportados = 0;
                int numeroDoRegistro = 0;
                for (WSValorSerieVO cotacaoPendenteDoDolar : cotacoesPendentesDoDolar) {
                    ++numeroDoRegistro;
                    stmtDestino.clearParameters();
                    int ano = cotacaoPendenteDoDolar.getAno(), mes = cotacaoPendenteDoDolar.getMes() - 1, dia = cotacaoPendenteDoDolar.getDia();
                    Calendar calendario = Calendar.getInstance();
                    calendario.clear();
                    calendario.set(ano, mes, dia);
                    java.sql.Date vDATA = new java.sql.Date(calendario.getTimeInMillis());
                    BigDecimal vPRECO = cotacaoPendenteDoDolar.getValor();
                    stmtDestino.setDateAtName("DATA", vDATA);
                    stmtDestino.setBigDecimalAtName("PRECO", vPRECO);
                    int contagemDasInsercoes = stmtDestino.executeUpdate();
                    quantidadeDeRegistrosImportados++;
                    double percentualCompleto = (double) quantidadeDeRegistrosImportados / quantidadeDeRegistrosASeremImportados * 100;
                    pAndamento.setPercentualCompleto((int) percentualCompleto);
                }
                conDestino.commit();
            } catch (Exception ex) {
                conDestino.rollback();
                throw ex;
            } finally {
                if (stmtDestino != null && (!stmtDestino.isClosed())) {
                    stmtDestino.close();
                }
            }
        }
        pAndamento.setPercentualCompleto(100);
    }

    private PIB[] obterValoresPendentesDoPIB(Andamento pAndamento) throws SQLException, ServiceException, IOException {
        PIB[] valoresPendentesDoPIB = null;
        MesEAno[] mesesSemPIB = obterMesesSemPIB();
        if (mesesSemPIB != null && mesesSemPIB.length > 0) {
            MesEAno mesEAnoAnterior = null;
            List<Periodo> periodos = new ArrayList<Periodo>();
            Periodo intervaloAtual = new Periodo();
            int diaInicial = 1;
            Calendar calendario = Calendar.getInstance();
            calendario.clear();
            calendario.set(mesesSemPIB[0].ano, mesesSemPIB[0].mes - 1, diaInicial);
            intervaloAtual.dataInicial = calendario.getTime();
            int tamanhoDoLoteAtual = 0;
            final int TAMANHO_MAXIMO_POR_LOTE = 100;
            final java.util.Date HOJE = Calendar.getInstance().getTime();
            for (int idxMesEAno = 0; idxMesEAno < mesesSemPIB.length; idxMesEAno++) {
                MesEAno mesEAnoAtual = mesesSemPIB[idxMesEAno];
                if (idxMesEAno > 0) {
                    mesEAnoAnterior = mesesSemPIB[idxMesEAno - 1];
                }
                boolean ultimoMesSemPIB = (idxMesEAno == (mesesSemPIB.length - 1));
                tamanhoDoLoteAtual++;
                if ((mesEAnoAnterior == null || mesEAnoAtual.codigo == mesEAnoAnterior.codigo + 1) && tamanhoDoLoteAtual < TAMANHO_MAXIMO_POR_LOTE && (!ultimoMesSemPIB)) {
                    continue;
                }
                intervaloAtual.dataFinal = obterUltimoDiaDoMes(ultimoMesSemPIB ? mesEAnoAtual : mesEAnoAnterior);
                if (intervaloAtual.dataFinal.after(HOJE)) {
                    intervaloAtual.dataFinal = HOJE;
                }
                periodos.add(intervaloAtual);
                intervaloAtual = new Periodo();
                calendario = Calendar.getInstance();
                calendario.clear();
                calendario.set(mesEAnoAtual.ano, mesEAnoAtual.mes - 1, diaInicial);
                intervaloAtual.dataInicial = calendario.getTime();
                tamanhoDoLoteAtual = 0;
            }
            List<PIB> acumuladorDosValoresPendentesDoPIB = new ArrayList<PIB>();
            String dataInicial_ddmmaaaa, dataFinal_ddmmaaaa;
            SimpleDateFormat formatadorDeData = new SimpleDateFormat("dd/MM/yyyy");
            int totalDePeriodos = periodos.size(), periodosObtidos;
            for (int idxPeriodo = 0; idxPeriodo < totalDePeriodos; idxPeriodo++) {
                Periodo intervalo = periodos.get(idxPeriodo);
                dataInicial_ddmmaaaa = formatadorDeData.format(intervalo.dataInicial);
                dataFinal_ddmmaaaa = formatadorDeData.format(intervalo.dataFinal);
                List<PIB> valoresPendentesDoPIBTmp = ManipuladorDeWebservice.obterProdutoInternoBrutoBrasileiro(dataInicial_ddmmaaaa, dataFinal_ddmmaaaa);
                if (valoresPendentesDoPIBTmp != null && valoresPendentesDoPIBTmp.size() > 0) {
                    acumuladorDosValoresPendentesDoPIB.addAll(valoresPendentesDoPIBTmp);
                }
                periodosObtidos = idxPeriodo + 1;
                pAndamento.setPercentualCompleto((int) (((double) periodosObtidos / totalDePeriodos) * 100));
            }
            valoresPendentesDoPIB = acumuladorDosValoresPendentesDoPIB.toArray(new PIB[acumuladorDosValoresPendentesDoPIB.size()]);
        }
        pAndamento.setPercentualCompleto(100);
        return valoresPendentesDoPIB;
    }

    /**
     * Realiza carga cumulativa de dados de arquivo *.csv para tabela do banco.
     */
    public void importarHistoricoDoPIB(Andamento pAndamento) throws FileNotFoundException, SQLException, Exception {
        pAndamento.delimitarIntervaloDeVariacao(0, 49);
        PIB[] valoresPendentesDoPIB = obterValoresPendentesDoPIB(pAndamento);
        pAndamento.delimitarIntervaloDeVariacao(50, 100);
        if (valoresPendentesDoPIB != null && valoresPendentesDoPIB.length > 0) {
            String sql = "INSERT INTO tmp_TB_PIB(ULTIMO_DIA_DO_MES, PIB_ACUM_12MESES_REAL, PIB_ACUM_12MESES_DOLAR) VALUES(:ULTIMO_DIA_DO_MES, :PIB_ACUM_12MESES_REAL, :PIB_ACUM_12MESES_DOLAR)";
            OraclePreparedStatement stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
            stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
            int quantidadeDeRegistrosASeremImportados = valoresPendentesDoPIB.length;
            try {
                int quantidadeDeRegistrosImportados = 0;
                int numeroDoRegistro = 0;
                final BigDecimal MILHAO = new BigDecimal("1000000");
                for (PIB valorPendenteDoPIB : valoresPendentesDoPIB) {
                    ++numeroDoRegistro;
                    stmtDestino.clearParameters();
                    java.sql.Date vULTIMO_DIA_DO_MES = new java.sql.Date(obterUltimoDiaDoMes(valorPendenteDoPIB.mesEAno).getTime());
                    BigDecimal vPIB_ACUM_12MESES_REAL = valorPendenteDoPIB.valorDoPIBEmReais.multiply(MILHAO).setScale(0, RoundingMode.DOWN);
                    BigDecimal vPIB_ACUM_12MESES_DOLAR = valorPendenteDoPIB.valorDoPIBEmDolares.multiply(MILHAO).setScale(0, RoundingMode.DOWN);
                    stmtDestino.setDateAtName("ULTIMO_DIA_DO_MES", vULTIMO_DIA_DO_MES);
                    stmtDestino.setBigDecimalAtName("PIB_ACUM_12MESES_REAL", vPIB_ACUM_12MESES_REAL);
                    stmtDestino.setBigDecimalAtName("PIB_ACUM_12MESES_DOLAR", vPIB_ACUM_12MESES_DOLAR);
                    int contagemDasInsercoes = stmtDestino.executeUpdate();
                    quantidadeDeRegistrosImportados++;
                    double percentualCompleto = (double) quantidadeDeRegistrosImportados / quantidadeDeRegistrosASeremImportados * 100;
                    pAndamento.setPercentualCompleto((int) percentualCompleto);
                }
                conDestino.commit();
            } catch (Exception ex) {
                conDestino.rollback();
                throw ex;
            } finally {
                if (stmtDestino != null && (!stmtDestino.isClosed())) {
                    stmtDestino.close();
                }
            }
        }
        pAndamento.setPercentualCompleto(100);
    }

    private enum CampoDaPlanilhaDosSetores {

        NOME_DO_SETOR, NOME_DO_SUBSETOR, NOME_DO_SEGMENTO_OU_DA_EMPRESA, SIGLA_DA_EMPRESA
    }

    ;

    private class LinhaDaPlanilhaDosSetores {

        final String nomeDoSetor, nomeDoSubsetor, nomeDoSegmentoOuDaEmpresa, siglaDaEmpresa;

        LinhaDaPlanilhaDosSetores(Row registro) {
            Cell celulaDoNomeDoSetor, celulaDoNomeDoSubsetor, celulaDoNomeDoSegmentoOuDaEmpresa, celulaDaSiglaDaEmpresa;
            celulaDoNomeDoSetor = registro.getCell(CampoDaPlanilhaDosSetores.NOME_DO_SETOR.ordinal());
            celulaDoNomeDoSubsetor = registro.getCell(CampoDaPlanilhaDosSetores.NOME_DO_SUBSETOR.ordinal());
            celulaDoNomeDoSegmentoOuDaEmpresa = registro.getCell(CampoDaPlanilhaDosSetores.NOME_DO_SEGMENTO_OU_DA_EMPRESA.ordinal());
            celulaDaSiglaDaEmpresa = registro.getCell(CampoDaPlanilhaDosSetores.SIGLA_DA_EMPRESA.ordinal());
            String tmpNomeDoSetor = "", tmpNomeDoSubsetor = "", tmpNomeDoSegmentoOuDaEmpresa = "", tmpSiglaDaEmpresa = "";
            if (celulaDoNomeDoSetor != null) {
                tmpNomeDoSetor = celulaDoNomeDoSetor.getStringCellValue();
                if (tmpNomeDoSetor != null) {
                    tmpNomeDoSetor = tmpNomeDoSetor.trim();
                }
            }
            if (celulaDoNomeDoSubsetor != null) {
                tmpNomeDoSubsetor = celulaDoNomeDoSubsetor.getStringCellValue();
                if (tmpNomeDoSubsetor != null) {
                    tmpNomeDoSubsetor = tmpNomeDoSubsetor.trim();
                }
            }
            if (celulaDoNomeDoSegmentoOuDaEmpresa != null) {
                tmpNomeDoSegmentoOuDaEmpresa = celulaDoNomeDoSegmentoOuDaEmpresa.getStringCellValue();
                if (tmpNomeDoSegmentoOuDaEmpresa != null) {
                    tmpNomeDoSegmentoOuDaEmpresa = tmpNomeDoSegmentoOuDaEmpresa.trim();
                }
            }
            if (celulaDaSiglaDaEmpresa != null) {
                tmpSiglaDaEmpresa = celulaDaSiglaDaEmpresa.getStringCellValue();
                if (tmpSiglaDaEmpresa != null) {
                    tmpSiglaDaEmpresa = tmpSiglaDaEmpresa.trim();
                }
            }
            this.nomeDoSetor = tmpNomeDoSetor;
            this.nomeDoSubsetor = tmpNomeDoSubsetor;
            this.nomeDoSegmentoOuDaEmpresa = tmpNomeDoSegmentoOuDaEmpresa;
            this.siglaDaEmpresa = tmpSiglaDaEmpresa;
        }
    }

    public void importarSetor(File pArquivoXLS, String pCabecalhoSetor, Andamento pAndamento) throws FileNotFoundException, IOException, SQLException, InvalidFormatException {
        int iLinha = -1;
        Statement stmtLimpezaInicialDestino = null;
        OraclePreparedStatement stmtDestino = null;
        try {
            Workbook arquivo = WorkbookFactory.create(new FileInputStream(pArquivoXLS));
            Sheet plan1 = arquivo.getSheetAt(0);
            int QUANTIDADE_DE_REGISTROS_DE_CABECALHO = 7;
            int QUANTIDADE_DE_REGISTROS_DE_RODAPE = 14;
            int QUANTIDADE_DE_REGISTROS_DE_METADADOS = QUANTIDADE_DE_REGISTROS_DE_CABECALHO + QUANTIDADE_DE_REGISTROS_DE_RODAPE;
            int quantidadeDeRegistrosEstimada = plan1.getPhysicalNumberOfRows() - QUANTIDADE_DE_REGISTROS_DE_METADADOS;
            String vSetor = "", vSubsetor = "", vSegmento = "";
            LinhaDaPlanilhaDosSetores registroAtual;
            int vPapeisPorSegmento = 0;
            stmtLimpezaInicialDestino = conDestino.createStatement();
            String sql = "TRUNCATE TABLE TMP_TB_SETOR_SUBSETOR_SEGMENTO";
            stmtLimpezaInicialDestino.executeUpdate(sql);
            sql = "INSERT INTO TMP_TB_SETOR_SUBSETOR_SEGMENTO(SIGLA_EMPRESA, NOME_SETOR, NOME_SUBSETOR, NOME_SEGMENTO) VALUES(:SIGLA_EMPRESA, :NOME_SETOR, :NOME_SUBSETOR, :NOME_SEGMENTO)";
            stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
            stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
            int quantidadeDeRegistrosImportados = 0;
            iLinha = 8;
            while (true) {
                registroAtual = new LinhaDaPlanilhaDosSetores(plan1.getRow(iLinha));
                if (registroAtual.nomeDoSetor.length() > 0 && !registroAtual.nomeDoSetor.equalsIgnoreCase(pCabecalhoSetor)) {
                    if (registroAtual.nomeDoSubsetor.equalsIgnoreCase("")) {
                        break;
                    } else {
                        vSetor = registroAtual.nomeDoSetor;
                        vSubsetor = null;
                        vSegmento = null;
                    }
                }
                if (registroAtual.nomeDoSubsetor.length() > 0 && !registroAtual.nomeDoSetor.equalsIgnoreCase(pCabecalhoSetor)) {
                    vSubsetor = registroAtual.nomeDoSubsetor;
                    vSegmento = null;
                }
                String nomeDoSegmento = registroAtual.nomeDoSegmentoOuDaEmpresa;
                if (nomeDoSegmento.length() > 0 && !registroAtual.nomeDoSetor.equalsIgnoreCase(pCabecalhoSetor) && registroAtual.siglaDaEmpresa.equals("")) {
                    if (vSegmento != null && vPapeisPorSegmento == 0) {
                        vSegmento = vSegmento + " " + nomeDoSegmento;
                    } else {
                        vSegmento = nomeDoSegmento;
                    }
                    vPapeisPorSegmento = 0;
                }
                String nomeDaEmpresa = registroAtual.nomeDoSegmentoOuDaEmpresa;
                if (registroAtual.siglaDaEmpresa.length() == 4 && !registroAtual.nomeDoSetor.equalsIgnoreCase(pCabecalhoSetor) && !nomeDaEmpresa.equals("")) {
                    String vCodneg = registroAtual.siglaDaEmpresa;
                    stmtDestino.clearParameters();
                    stmtDestino.setStringAtName("SIGLA_EMPRESA", vCodneg);
                    stmtDestino.setStringAtName("NOME_SETOR", vSetor);
                    stmtDestino.setStringAtName("NOME_SUBSETOR", vSubsetor);
                    stmtDestino.setStringAtName("NOME_SEGMENTO", vSegmento);
                    int contagemDasInsercoes = stmtDestino.executeUpdate();
                    quantidadeDeRegistrosImportados++;
                    vPapeisPorSegmento++;
                }
                iLinha++;
                double percentualCompleto = (double) quantidadeDeRegistrosImportados / quantidadeDeRegistrosEstimada * 100;
                pAndamento.setPercentualCompleto((int) percentualCompleto);
            }
            conDestino.commit();
        } catch (Exception ex) {
            conDestino.rollback();
            ProblemaNaImportacaoDeArquivo problemaDetalhado = new ProblemaNaImportacaoDeArquivo();
            problemaDetalhado.nomeDoArquivo = pArquivoXLS.getName();
            problemaDetalhado.linhaProblematicaDoArquivo = iLinha;
            problemaDetalhado.detalhesSobreOProblema = ex;
            throw problemaDetalhado;
        } finally {
            pAndamento.setPercentualCompleto(100);
            if (stmtLimpezaInicialDestino != null && (!stmtLimpezaInicialDestino.isClosed())) {
                stmtLimpezaInicialDestino.close();
            }
            if (stmtDestino != null && (!stmtDestino.isClosed())) {
                stmtDestino.close();
            }
        }
    }

    private enum CampoDoArquivoDasEmpresasAbertas {

        CODIGO_CVM, DENOMINACAO_SOCIAL, DENOMINACAO_COMERCIAL, LOGRADOURO, COMPLEMENTO, BAIRRO, CEP, MUNICIPIO, UF, DDD, TELEFONE, FAX, DENOMINACAO_ANTERIOR, SETOR_ATIVIDADE, CNPJ, DRI, AUDITOR, QUANT_DE_ACOES_ORDINARIAS, QUANT_DE_ACOES_PREF, SITUACAO, DATA_DA_SITUACAO, TIPO_PAPEL1, TIPO_PAPEL2, TIPO_PAPEL3, TIPO_PAPEL4, TIPO_PAPEL5, TIPO_PAPEL6, CONTROLE_ACIONARIO, DATA_DE_REGISTRO, DATA_DO_CANCELAMENTO, MERCADO, BOLSA1, BOLSA2, BOLSA3, BOLSA4, BOLSA5, BOLSA6, BOLSA7, BOLSA8, BOLSA9, MOTIVO_DO_CANCELAMENTO, PATRIMONIO_LIQUIDO, DATA_DO_PATRIMONIO, E_MAIL, NOME_SETOR_ATIVIDADE, DATA_DA_ACAO, TIPO_NEGOCIO1, TIPO_NEGOCIO2, TIPO_NEGOCIO3, TIPO_NEGOCIO4, TIPO_NEGOCIO5, TIPO_NEGOCIO6, TIPO_MERCADO1, TIPO_MERCADO2, TIPO_MERCADO3, TIPO_MERCADO4, TIPO_MERCADO5, TIPO_MERCADO6
    }

    /**
     * Realiza carga não-cumulativa de dados de arquivo *.TXT para tabela do banco.
     */
    public void importarEmpresasAbertas(File pArquivoTXT, Andamento pAndamento) throws FileNotFoundException, SQLException {
        int numeroDoRegistro = -1;
        Scanner in = null;
        Statement stmtLimpezaInicialDestino = conDestino.createStatement();
        String sql = "TRUNCATE TABLE TMP_TB_CIA_ABERTA";
        stmtLimpezaInicialDestino.executeUpdate(sql);
        sql = "INSERT INTO TMP_TB_CIA_ABERTA(CODIGO_CVM, DENOMINACAO_SOCIAL, DENOMINACAO_COMERCIAL, LOGRADOURO, COMPLEMENTO, BAIRRO, CEP, MUNICIPIO, UF, DDD, TELEFONE, FAX, DENOMINACAO_ANTERIOR, SETOR_ATIVIDADE, CNPJ, DRI, AUDITOR, QUANT_DE_ACOES_ORDINARIAS, QUANT_DE_ACOES_PREF, SITUACAO, DATA_DA_SITUACAO, TIPO_PAPEL1, TIPO_PAPEL2, TIPO_PAPEL3, TIPO_PAPEL4, TIPO_PAPEL5, TIPO_PAPEL6, CONTROLE_ACIONARIO, DATA_DE_REGISTRO, DATA_DO_CANCELAMENTO, MERCADO, BOLSA1, BOLSA2, BOLSA3, BOLSA4, BOLSA5, BOLSA6, BOLSA7, BOLSA8, BOLSA9, MOTIVO_DO_CANCELAMENTO, PATRIMONIO_LIQUIDO, DATA_DO_PATRIMONIO, E_MAIL, NOME_SETOR_ATIVIDADE, DATA_DA_ACAO, TIPO_NEGOCIO1, TIPO_NEGOCIO2, TIPO_NEGOCIO3, TIPO_NEGOCIO4, TIPO_NEGOCIO5, TIPO_NEGOCIO6, TIPO_MERCADO1, TIPO_MERCADO2, TIPO_MERCADO3, TIPO_MERCADO4, TIPO_MERCADO5, TIPO_MERCADO6) VALUES(:CODIGO_CVM, :DENOMINACAO_SOCIAL, :DENOMINACAO_COMERCIAL, :LOGRADOURO, :COMPLEMENTO, :BAIRRO, :CEP, :MUNICIPIO, :UF, :DDD, :TELEFONE, :FAX, :DENOMINACAO_ANTERIOR, :SETOR_ATIVIDADE, :CNPJ, :DRI, :AUDITOR, :QUANT_DE_ACOES_ORDINARIAS, :QUANT_DE_ACOES_PREF, :SITUACAO, :DATA_DA_SITUACAO, :TIPO_PAPEL1, :TIPO_PAPEL2, :TIPO_PAPEL3, :TIPO_PAPEL4, :TIPO_PAPEL5, :TIPO_PAPEL6, :CONTROLE_ACIONARIO, :DATA_DE_REGISTRO, :DATA_DO_CANCELAMENTO, :MERCADO, :BOLSA1, :BOLSA2, :BOLSA3, :BOLSA4, :BOLSA5, :BOLSA6, :BOLSA7, :BOLSA8, :BOLSA9, :MOTIVO_DO_CANCELAMENTO, :PATRIMONIO_LIQUIDO, :DATA_DO_PATRIMONIO, :E_MAIL, :NOME_SETOR_ATIVIDADE, :DATA_DA_ACAO, :TIPO_NEGOCIO1, :TIPO_NEGOCIO2, :TIPO_NEGOCIO3, :TIPO_NEGOCIO4, :TIPO_NEGOCIO5, :TIPO_NEGOCIO6, :TIPO_MERCADO1, :TIPO_MERCADO2, :TIPO_MERCADO3, :TIPO_MERCADO4, :TIPO_MERCADO5, :TIPO_MERCADO6)";
        OraclePreparedStatement stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
        stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
        final int TAMANHO_DO_CABECALHO_DO_ARQUIVO = 707;
        final int TAMANHO_DO_RODAPE_DO_ARQUIVO = 0;
        final int TAMANHO_DOS_METADADOS_DO_ARQUIVO = TAMANHO_DO_CABECALHO_DO_ARQUIVO + TAMANHO_DO_RODAPE_DO_ARQUIVO;
        final int TAMANHO_MEDIO_POR_REGISTRO = 659;
        long tamanhoDosArquivos = pArquivoTXT.length();
        int quantidadeDeRegistrosEstimada = (int) (tamanhoDosArquivos - TAMANHO_DOS_METADADOS_DO_ARQUIVO) / TAMANHO_MEDIO_POR_REGISTRO;
        try {
            in = new Scanner(new FileInputStream(pArquivoTXT), Constantes.CONJUNTO_DE_CARACTERES_DO_ARQUIVO_TEXTO_DA_CVM.name());
            int quantidadeDeRegistrosImportada = 0;
            String registro;
            String[] campos;
            in.nextLine();
            numeroDoRegistro = 0;
            int vCODIGO_CVM;
            String vDENOMINACAO_SOCIAL, vDENOMINACAO_COMERCIAL, vLOGRADOURO, vCOMPLEMENTO, vBAIRRO;
            BigDecimal vCEP;
            String vMUNICIPIO, vUF;
            BigDecimal vDDD, vTELEFONE, vFAX;
            String vDENOMINACAO_ANTERIOR, vSETOR_ATIVIDADE;
            BigDecimal vCNPJ;
            String vDRI, vAUDITOR;
            BigDecimal vQUANT_DE_ACOES_ORDINARIAS, vQUANT_DE_ACOES_PREF;
            String vSITUACAO;
            java.sql.Date vDATA_DA_SITUACAO;
            String vTIPO_PAPEL1, vTIPO_PAPEL2, vTIPO_PAPEL3, vTIPO_PAPEL4, vTIPO_PAPEL5, vTIPO_PAPEL6, vCONTROLE_ACIONARIO;
            java.sql.Date vDATA_DE_REGISTRO, vDATA_DO_CANCELAMENTO;
            String vMERCADO, vBOLSA1, vBOLSA2, vBOLSA3, vBOLSA4, vBOLSA5, vBOLSA6, vBOLSA7, vBOLSA8, vBOLSA9, vMOTIVO_DO_CANCELAMENTO;
            BigDecimal vPATRIMONIO_LIQUIDO;
            java.sql.Date vDATA_DO_PATRIMONIO;
            String vE_MAIL, vNOME_SETOR_ATIVIDADE;
            java.sql.Date vDATA_DA_ACAO;
            String vTIPO_NEGOCIO1, vTIPO_NEGOCIO2, vTIPO_NEGOCIO3, vTIPO_NEGOCIO4, vTIPO_NEGOCIO5, vTIPO_NEGOCIO6, vTIPO_MERCADO1, vTIPO_MERCADO2, vTIPO_MERCADO3, vTIPO_MERCADO4, vTIPO_MERCADO5, vTIPO_MERCADO6;
            final int QTDE_CAMPOS = CampoDoArquivoDasEmpresasAbertas.values().length;
            final String SEPARADOR_DE_CAMPOS_DO_REGISTRO = ";";
            while (in.hasNextLine()) {
                ++numeroDoRegistro;
                registro = in.nextLine();
                stmtDestino.clearParameters();
                ArrayList<String> camposTmp = new ArrayList<String>(QTDE_CAMPOS);
                StringBuilder campoTmp = new StringBuilder();
                char[] registroTmp = registro.toCharArray();
                char c;
                boolean houveMesclagemDeCampos = false;
                boolean campoIniciaComEspacoEmBranco, campoPossuiConteudo, registroComExcessoDeDelimitadores;
                int quantidadeDeDelimitadoresEncontrados = (registro.length() - registro.replace(SEPARADOR_DE_CAMPOS_DO_REGISTRO, "").length());
                registroComExcessoDeDelimitadores = (quantidadeDeDelimitadoresEncontrados > (QTDE_CAMPOS - 1));
                for (int idxCaractere = 0; idxCaractere < registroTmp.length; idxCaractere++) {
                    c = registroTmp[idxCaractere];
                    if (c == SEPARADOR_DE_CAMPOS_DO_REGISTRO.charAt(0)) {
                        campoPossuiConteudo = (campoTmp.length() > 0 && campoTmp.toString().trim().length() > 0);
                        if (campoPossuiConteudo) {
                            String campoAnterior = null;
                            if (camposTmp.size() > 0) {
                                campoAnterior = camposTmp.get(camposTmp.size() - 1);
                            }
                            campoIniciaComEspacoEmBranco = campoTmp.toString().startsWith(" ");
                            if (campoAnterior != null && campoIniciaComEspacoEmBranco && registroComExcessoDeDelimitadores) {
                                camposTmp.set(camposTmp.size() - 1, (campoAnterior + campoTmp.toString()).trim());
                                houveMesclagemDeCampos = true;
                            } else {
                                camposTmp.add(campoTmp.toString().trim());
                            }
                        } else {
                            camposTmp.add(null);
                        }
                        campoTmp.setLength(0);
                    } else {
                        campoTmp.append(c);
                    }
                }
                if (registro.endsWith(SEPARADOR_DE_CAMPOS_DO_REGISTRO)) {
                    camposTmp.add(null);
                }
                if (houveMesclagemDeCampos && camposTmp.size() < QTDE_CAMPOS) {
                    camposTmp.add(CampoDoArquivoDasEmpresasAbertas.COMPLEMENTO.ordinal(), null);
                }
                campos = camposTmp.toArray(new String[camposTmp.size()]);
                int quantidadeDeCamposEncontradosIncluindoOsVazios = campos.length;
                if (quantidadeDeCamposEncontradosIncluindoOsVazios != QTDE_CAMPOS) {
                    throw new CampoMalDelimitadoEmRegistroDoArquivoImportado(registro);
                }
                vCODIGO_CVM = Integer.parseInt(campos[CampoDoArquivoDasEmpresasAbertas.CODIGO_CVM.ordinal()]);
                vDENOMINACAO_SOCIAL = campos[CampoDoArquivoDasEmpresasAbertas.DENOMINACAO_SOCIAL.ordinal()];
                vDENOMINACAO_COMERCIAL = campos[CampoDoArquivoDasEmpresasAbertas.DENOMINACAO_COMERCIAL.ordinal()];
                vLOGRADOURO = campos[CampoDoArquivoDasEmpresasAbertas.LOGRADOURO.ordinal()];
                vCOMPLEMENTO = campos[CampoDoArquivoDasEmpresasAbertas.COMPLEMENTO.ordinal()];
                vBAIRRO = campos[CampoDoArquivoDasEmpresasAbertas.BAIRRO.ordinal()];
                String cepTmp = campos[CampoDoArquivoDasEmpresasAbertas.CEP.ordinal()];
                if (cepTmp != null && cepTmp.trim().length() > 0) {
                    vCEP = new BigDecimal(cepTmp);
                } else {
                    vCEP = null;
                }
                vMUNICIPIO = campos[CampoDoArquivoDasEmpresasAbertas.MUNICIPIO.ordinal()];
                vUF = campos[CampoDoArquivoDasEmpresasAbertas.UF.ordinal()];
                String dddTmp = campos[CampoDoArquivoDasEmpresasAbertas.DDD.ordinal()], foneTmp = campos[CampoDoArquivoDasEmpresasAbertas.TELEFONE.ordinal()], dddFone = "";
                if (dddTmp != null && dddTmp.trim().length() > 0) {
                    dddFone = dddFone + dddTmp;
                }
                if (foneTmp != null && foneTmp.trim().length() > 0) {
                    dddFone = dddFone + foneTmp;
                }
                if (dddFone != null && dddFone.trim().length() > 0) {
                    dddFone = new BigDecimal(dddFone).toString();
                    if (dddFone.length() > 10 && dddFone.endsWith("0")) {
                        dddFone = dddFone.substring(0, 10);
                    }
                    vDDD = new BigDecimal(dddFone.substring(0, 2));
                    vTELEFONE = new BigDecimal(dddFone.substring(2));
                } else {
                    vDDD = null;
                    vTELEFONE = null;
                }
                String faxTmp = campos[CampoDoArquivoDasEmpresasAbertas.FAX.ordinal()];
                if (faxTmp != null && faxTmp.trim().length() > 0) {
                    vFAX = new BigDecimal(faxTmp);
                } else {
                    vFAX = null;
                }
                vDENOMINACAO_ANTERIOR = campos[CampoDoArquivoDasEmpresasAbertas.DENOMINACAO_ANTERIOR.ordinal()];
                vSETOR_ATIVIDADE = campos[CampoDoArquivoDasEmpresasAbertas.SETOR_ATIVIDADE.ordinal()];
                String cnpjTmp = campos[CampoDoArquivoDasEmpresasAbertas.CNPJ.ordinal()];
                if (cnpjTmp != null && cnpjTmp.trim().length() > 0) {
                    vCNPJ = new BigDecimal(cnpjTmp);
                } else {
                    vCNPJ = null;
                }
                vDRI = campos[CampoDoArquivoDasEmpresasAbertas.DRI.ordinal()];
                vAUDITOR = campos[CampoDoArquivoDasEmpresasAbertas.AUDITOR.ordinal()];
                String qtdeAcoesON = campos[CampoDoArquivoDasEmpresasAbertas.QUANT_DE_ACOES_ORDINARIAS.ordinal()];
                if (qtdeAcoesON != null && qtdeAcoesON.trim().length() > 0) {
                    vQUANT_DE_ACOES_ORDINARIAS = new BigDecimal(qtdeAcoesON);
                } else {
                    vQUANT_DE_ACOES_ORDINARIAS = null;
                }
                String qtdeAcoesPN = campos[CampoDoArquivoDasEmpresasAbertas.QUANT_DE_ACOES_PREF.ordinal()];
                if (qtdeAcoesPN != null && qtdeAcoesPN.trim().length() > 0) {
                    vQUANT_DE_ACOES_PREF = new BigDecimal(qtdeAcoesPN);
                } else {
                    vQUANT_DE_ACOES_PREF = null;
                }
                vSITUACAO = campos[CampoDoArquivoDasEmpresasAbertas.SITUACAO.ordinal()];
                String dataDaSituacaoTmp = campos[CampoDoArquivoDasEmpresasAbertas.DATA_DA_SITUACAO.ordinal()];
                String[] partesDaData = dataDaSituacaoTmp.trim().split("/");
                int dia = Integer.parseInt(partesDaData[0]), mes = Integer.parseInt(partesDaData[1]) - 1, ano = Integer.parseInt(partesDaData[2]);
                Calendar calendario = Calendar.getInstance();
                calendario.clear();
                calendario.set(ano, mes, dia);
                vDATA_DA_SITUACAO = new java.sql.Date(calendario.getTimeInMillis());
                vTIPO_PAPEL1 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_PAPEL1.ordinal()];
                vTIPO_PAPEL2 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_PAPEL2.ordinal()];
                vTIPO_PAPEL3 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_PAPEL3.ordinal()];
                vTIPO_PAPEL4 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_PAPEL4.ordinal()];
                vTIPO_PAPEL5 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_PAPEL5.ordinal()];
                vTIPO_PAPEL6 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_PAPEL6.ordinal()];
                vCONTROLE_ACIONARIO = campos[CampoDoArquivoDasEmpresasAbertas.CONTROLE_ACIONARIO.ordinal()];
                String dataDeRegistroTmp = campos[CampoDoArquivoDasEmpresasAbertas.DATA_DE_REGISTRO.ordinal()];
                partesDaData = dataDeRegistroTmp.trim().split("/");
                dia = Integer.parseInt(partesDaData[0]);
                mes = Integer.parseInt(partesDaData[1]) - 1;
                ano = Integer.parseInt(partesDaData[2]);
                calendario = Calendar.getInstance();
                calendario.clear();
                calendario.set(ano, mes, dia);
                vDATA_DE_REGISTRO = new java.sql.Date(calendario.getTimeInMillis());
                String dataDoCancelamentoTmp = campos[CampoDoArquivoDasEmpresasAbertas.DATA_DO_CANCELAMENTO.ordinal()];
                if (dataDoCancelamentoTmp != null && dataDoCancelamentoTmp.trim().length() > 0) {
                    partesDaData = dataDoCancelamentoTmp.trim().split("/");
                    dia = Integer.parseInt(partesDaData[0]);
                    mes = Integer.parseInt(partesDaData[1]) - 1;
                    ano = Integer.parseInt(partesDaData[2]);
                    calendario = Calendar.getInstance();
                    calendario.clear();
                    calendario.set(ano, mes, dia);
                    vDATA_DO_CANCELAMENTO = new java.sql.Date(calendario.getTimeInMillis());
                } else {
                    vDATA_DO_CANCELAMENTO = null;
                }
                vMERCADO = campos[CampoDoArquivoDasEmpresasAbertas.MERCADO.ordinal()];
                vBOLSA1 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA1.ordinal()];
                vBOLSA2 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA2.ordinal()];
                vBOLSA3 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA3.ordinal()];
                vBOLSA4 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA4.ordinal()];
                vBOLSA5 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA5.ordinal()];
                vBOLSA6 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA6.ordinal()];
                vBOLSA7 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA7.ordinal()];
                vBOLSA8 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA8.ordinal()];
                vBOLSA9 = campos[CampoDoArquivoDasEmpresasAbertas.BOLSA9.ordinal()];
                vMOTIVO_DO_CANCELAMENTO = campos[CampoDoArquivoDasEmpresasAbertas.MOTIVO_DO_CANCELAMENTO.ordinal()];
                String patrimonioLiquidoTmp = campos[CampoDoArquivoDasEmpresasAbertas.PATRIMONIO_LIQUIDO.ordinal()];
                if (patrimonioLiquidoTmp != null && patrimonioLiquidoTmp.trim().length() > 0) {
                    vPATRIMONIO_LIQUIDO = new BigDecimal(patrimonioLiquidoTmp);
                } else {
                    vPATRIMONIO_LIQUIDO = null;
                }
                String dataDoPatrimonioTmp = campos[CampoDoArquivoDasEmpresasAbertas.DATA_DO_PATRIMONIO.ordinal()];
                if (dataDoPatrimonioTmp != null && dataDoPatrimonioTmp.trim().length() > 0) {
                    partesDaData = dataDoPatrimonioTmp.trim().split("/");
                    dia = Integer.parseInt(partesDaData[0]);
                    mes = Integer.parseInt(partesDaData[1]) - 1;
                    ano = Integer.parseInt(partesDaData[2]);
                    calendario = Calendar.getInstance();
                    calendario.clear();
                    calendario.set(ano, mes, dia);
                    vDATA_DO_PATRIMONIO = new java.sql.Date(calendario.getTimeInMillis());
                } else {
                    vDATA_DO_PATRIMONIO = null;
                }
                vE_MAIL = campos[CampoDoArquivoDasEmpresasAbertas.E_MAIL.ordinal()];
                vNOME_SETOR_ATIVIDADE = campos[CampoDoArquivoDasEmpresasAbertas.NOME_SETOR_ATIVIDADE.ordinal()];
                String dataDaAcaoTmp = campos[CampoDoArquivoDasEmpresasAbertas.DATA_DA_ACAO.ordinal()];
                if (dataDaAcaoTmp != null && dataDaAcaoTmp.trim().length() > 0) {
                    partesDaData = dataDaAcaoTmp.trim().split("/");
                    dia = Integer.parseInt(partesDaData[0]);
                    mes = Integer.parseInt(partesDaData[1]) - 1;
                    ano = Integer.parseInt(partesDaData[2]);
                    calendario = Calendar.getInstance();
                    calendario.clear();
                    calendario.set(ano, mes, dia);
                    vDATA_DA_ACAO = new java.sql.Date(calendario.getTimeInMillis());
                } else {
                    vDATA_DA_ACAO = null;
                }
                vTIPO_NEGOCIO1 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_NEGOCIO1.ordinal()];
                vTIPO_NEGOCIO2 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_NEGOCIO2.ordinal()];
                vTIPO_NEGOCIO3 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_NEGOCIO3.ordinal()];
                vTIPO_NEGOCIO4 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_NEGOCIO4.ordinal()];
                vTIPO_NEGOCIO5 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_NEGOCIO5.ordinal()];
                vTIPO_NEGOCIO6 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_NEGOCIO6.ordinal()];
                vTIPO_MERCADO1 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_MERCADO1.ordinal()];
                vTIPO_MERCADO2 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_MERCADO2.ordinal()];
                vTIPO_MERCADO3 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_MERCADO3.ordinal()];
                vTIPO_MERCADO4 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_MERCADO4.ordinal()];
                vTIPO_MERCADO5 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_MERCADO5.ordinal()];
                vTIPO_MERCADO6 = campos[CampoDoArquivoDasEmpresasAbertas.TIPO_MERCADO6.ordinal()];
                stmtDestino.setIntAtName("CODIGO_CVM", vCODIGO_CVM);
                stmtDestino.setStringAtName("DENOMINACAO_SOCIAL", vDENOMINACAO_SOCIAL);
                stmtDestino.setStringAtName("DENOMINACAO_COMERCIAL", vDENOMINACAO_COMERCIAL);
                stmtDestino.setStringAtName("LOGRADOURO", vLOGRADOURO);
                stmtDestino.setStringAtName("COMPLEMENTO", vCOMPLEMENTO);
                stmtDestino.setStringAtName("BAIRRO", vBAIRRO);
                stmtDestino.setBigDecimalAtName("CEP", vCEP);
                stmtDestino.setStringAtName("MUNICIPIO", vMUNICIPIO);
                stmtDestino.setStringAtName("UF", vUF);
                stmtDestino.setBigDecimalAtName("DDD", vDDD);
                stmtDestino.setBigDecimalAtName("TELEFONE", vTELEFONE);
                stmtDestino.setBigDecimalAtName("FAX", vFAX);
                stmtDestino.setStringAtName("DENOMINACAO_ANTERIOR", vDENOMINACAO_ANTERIOR);
                stmtDestino.setStringAtName("SETOR_ATIVIDADE", vSETOR_ATIVIDADE);
                stmtDestino.setBigDecimalAtName("CNPJ", vCNPJ);
                stmtDestino.setStringAtName("DRI", vDRI);
                stmtDestino.setStringAtName("AUDITOR", vAUDITOR);
                stmtDestino.setBigDecimalAtName("QUANT_DE_ACOES_ORDINARIAS", vQUANT_DE_ACOES_ORDINARIAS);
                stmtDestino.setBigDecimalAtName("QUANT_DE_ACOES_PREF", vQUANT_DE_ACOES_PREF);
                stmtDestino.setStringAtName("SITUACAO", vSITUACAO);
                stmtDestino.setDateAtName("DATA_DA_SITUACAO", vDATA_DA_SITUACAO);
                stmtDestino.setStringAtName("TIPO_PAPEL1", vTIPO_PAPEL1);
                stmtDestino.setStringAtName("TIPO_PAPEL2", vTIPO_PAPEL2);
                stmtDestino.setStringAtName("TIPO_PAPEL3", vTIPO_PAPEL3);
                stmtDestino.setStringAtName("TIPO_PAPEL4", vTIPO_PAPEL4);
                stmtDestino.setStringAtName("TIPO_PAPEL5", vTIPO_PAPEL5);
                stmtDestino.setStringAtName("TIPO_PAPEL6", vTIPO_PAPEL6);
                stmtDestino.setStringAtName("CONTROLE_ACIONARIO", vCONTROLE_ACIONARIO);
                stmtDestino.setDateAtName("DATA_DE_REGISTRO", vDATA_DE_REGISTRO);
                stmtDestino.setDateAtName("DATA_DO_CANCELAMENTO", vDATA_DO_CANCELAMENTO);
                stmtDestino.setStringAtName("MERCADO", vMERCADO);
                stmtDestino.setStringAtName("BOLSA1", vBOLSA1);
                stmtDestino.setStringAtName("BOLSA2", vBOLSA2);
                stmtDestino.setStringAtName("BOLSA3", vBOLSA3);
                stmtDestino.setStringAtName("BOLSA4", vBOLSA4);
                stmtDestino.setStringAtName("BOLSA5", vBOLSA5);
                stmtDestino.setStringAtName("BOLSA6", vBOLSA6);
                stmtDestino.setStringAtName("BOLSA7", vBOLSA7);
                stmtDestino.setStringAtName("BOLSA8", vBOLSA8);
                stmtDestino.setStringAtName("BOLSA9", vBOLSA9);
                stmtDestino.setStringAtName("MOTIVO_DO_CANCELAMENTO", vMOTIVO_DO_CANCELAMENTO);
                stmtDestino.setBigDecimalAtName("PATRIMONIO_LIQUIDO", vPATRIMONIO_LIQUIDO);
                stmtDestino.setDateAtName("DATA_DO_PATRIMONIO", vDATA_DO_PATRIMONIO);
                stmtDestino.setStringAtName("E_MAIL", vE_MAIL);
                stmtDestino.setStringAtName("NOME_SETOR_ATIVIDADE", vNOME_SETOR_ATIVIDADE);
                stmtDestino.setDateAtName("DATA_DA_ACAO", vDATA_DA_ACAO);
                stmtDestino.setStringAtName("TIPO_NEGOCIO1", vTIPO_NEGOCIO1);
                stmtDestino.setStringAtName("TIPO_NEGOCIO2", vTIPO_NEGOCIO2);
                stmtDestino.setStringAtName("TIPO_NEGOCIO3", vTIPO_NEGOCIO3);
                stmtDestino.setStringAtName("TIPO_NEGOCIO4", vTIPO_NEGOCIO4);
                stmtDestino.setStringAtName("TIPO_NEGOCIO5", vTIPO_NEGOCIO5);
                stmtDestino.setStringAtName("TIPO_NEGOCIO6", vTIPO_NEGOCIO6);
                stmtDestino.setStringAtName("TIPO_MERCADO1", vTIPO_MERCADO1);
                stmtDestino.setStringAtName("TIPO_MERCADO2", vTIPO_MERCADO2);
                stmtDestino.setStringAtName("TIPO_MERCADO3", vTIPO_MERCADO3);
                stmtDestino.setStringAtName("TIPO_MERCADO4", vTIPO_MERCADO4);
                stmtDestino.setStringAtName("TIPO_MERCADO5", vTIPO_MERCADO5);
                stmtDestino.setStringAtName("TIPO_MERCADO6", vTIPO_MERCADO6);
                int contagemDasInsercoes = stmtDestino.executeUpdate();
                quantidadeDeRegistrosImportada++;
                double percentualCompleto = (double) quantidadeDeRegistrosImportada / quantidadeDeRegistrosEstimada * 100;
                pAndamento.setPercentualCompleto((int) percentualCompleto);
            }
            conDestino.commit();
        } catch (Exception ex) {
            conDestino.rollback();
            ProblemaNaImportacaoDeArquivo problemaDetalhado = new ProblemaNaImportacaoDeArquivo();
            problemaDetalhado.nomeDoArquivo = pArquivoTXT.getName();
            problemaDetalhado.linhaProblematicaDoArquivo = numeroDoRegistro;
            problemaDetalhado.detalhesSobreOProblema = ex;
            throw problemaDetalhado;
        } finally {
            pAndamento.setPercentualCompleto(100);
            in.close();
            if (stmtLimpezaInicialDestino != null && (!stmtLimpezaInicialDestino.isClosed())) {
                stmtLimpezaInicialDestino.close();
            }
            if (stmtDestino != null && (!stmtDestino.isClosed())) {
                stmtDestino.close();
            }
        }
    }

    private enum CampoDoArquivoDosEmissoresDeTitulosFinanceiros {

        SIGLA, NOME, CNPJ, DATA_CRIACAO
    }

    /**
     * Realiza carga não-cumulativa de dados de arquivo *.TXT para tabela do banco.
     */
    public void importarEmissoresDosTitulosFinanceiros(File pArquivoTXT, Andamento pAndamento) throws FileNotFoundException, SQLException {
        int numeroDoRegistro = -1;
        Scanner in = null;
        Statement stmtLimpezaInicialDestino = conDestino.createStatement();
        String sql = "TRUNCATE TABLE TMP_TB_EMISSOR_TITULO";
        stmtLimpezaInicialDestino.executeUpdate(sql);
        sql = "INSERT INTO TMP_TB_EMISSOR_TITULO(SIGLA, NOME, CNPJ, DATA_CRIACAO) VALUES(:SIGLA, :NOME, :CNPJ, :DATA_CRIACAO)";
        OraclePreparedStatement stmtDestino = (OraclePreparedStatement) conDestino.prepareStatement(sql);
        stmtDestino.setExecuteBatch(COMANDOS_POR_LOTE);
        final int TAMANHO_DO_CABECALHO_DO_ARQUIVO = 0;
        final int TAMANHO_DO_RODAPE_DO_ARQUIVO = 0;
        final int TAMANHO_DOS_METADADOS_DO_ARQUIVO = TAMANHO_DO_CABECALHO_DO_ARQUIVO + TAMANHO_DO_RODAPE_DO_ARQUIVO;
        final int TAMANHO_MEDIO_POR_REGISTRO = 81;
        long tamanhoDosArquivos = pArquivoTXT.length();
        int quantidadeDeRegistrosEstimada = (int) (tamanhoDosArquivos - TAMANHO_DOS_METADADOS_DO_ARQUIVO) / TAMANHO_MEDIO_POR_REGISTRO;
        String registro;
        String[] campos;
        try {
            in = new Scanner(new FileInputStream(pArquivoTXT), Constantes.CONJUNTO_DE_CARACTERES_DOS_ARQUIVOS_TEXTO_DA_BOVESPA.name());
            int quantidadeDeRegistrosImportada = 0;
            numeroDoRegistro = 0;
            String vSIGLA, vNOME;
            BigDecimal vCNPJ;
            java.sql.Date vDATA_CRIACAO;
            final int QTDE_CAMPOS = CampoDoArquivoDosEmissoresDeTitulosFinanceiros.values().length;
            final String SEPARADOR_DE_CAMPOS_DO_REGISTRO = ",";
            final String DELIMITADOR_DE_CAMPOS_DO_REGISTRO = "\"";
            while (in.hasNextLine()) {
                ++numeroDoRegistro;
                registro = in.nextLine();
                stmtDestino.clearParameters();
                registro = registro.substring(1, registro.length() - 1);
                if (registro.endsWith(DELIMITADOR_DE_CAMPOS_DO_REGISTRO)) {
                    registro = registro + " ";
                }
                campos = registro.split(DELIMITADOR_DE_CAMPOS_DO_REGISTRO + SEPARADOR_DE_CAMPOS_DO_REGISTRO + DELIMITADOR_DE_CAMPOS_DO_REGISTRO);
                int quantidadeDeCamposEncontradosIncluindoOsVazios = campos.length;
                if (quantidadeDeCamposEncontradosIncluindoOsVazios != QTDE_CAMPOS) {
                    throw new CampoMalDelimitadoEmRegistroDoArquivoImportado(registro);
                }
                vSIGLA = campos[CampoDoArquivoDosEmissoresDeTitulosFinanceiros.SIGLA.ordinal()];
                vNOME = campos[CampoDoArquivoDosEmissoresDeTitulosFinanceiros.NOME.ordinal()];
                String cnpjTmp = campos[CampoDoArquivoDosEmissoresDeTitulosFinanceiros.CNPJ.ordinal()];
                if (cnpjTmp != null && cnpjTmp.trim().length() > 0) {
                    vCNPJ = new BigDecimal(cnpjTmp);
                } else {
                    vCNPJ = null;
                }
                String dataDaCriacaoTmp = campos[CampoDoArquivoDosEmissoresDeTitulosFinanceiros.DATA_CRIACAO.ordinal()];
                if (dataDaCriacaoTmp != null && dataDaCriacaoTmp.trim().length() > 0) {
                    int dia = Integer.parseInt(dataDaCriacaoTmp.substring(6, 8)), mes = Integer.parseInt(dataDaCriacaoTmp.substring(4, 6)) - 1, ano = Integer.parseInt(dataDaCriacaoTmp.substring(0, 4));
                    Calendar calendario = Calendar.getInstance();
                    calendario.clear();
                    calendario.set(ano, mes, dia);
                    vDATA_CRIACAO = new java.sql.Date(calendario.getTimeInMillis());
                } else {
                    vDATA_CRIACAO = null;
                }
                stmtDestino.setStringAtName("SIGLA", vSIGLA);
                stmtDestino.setStringAtName("NOME", vNOME);
                stmtDestino.setBigDecimalAtName("CNPJ", vCNPJ);
                stmtDestino.setDateAtName("DATA_CRIACAO", vDATA_CRIACAO);
                int contagemDasInsercoes = stmtDestino.executeUpdate();
                quantidadeDeRegistrosImportada++;
                double percentualCompleto = (double) quantidadeDeRegistrosImportada / quantidadeDeRegistrosEstimada * 100;
                pAndamento.setPercentualCompleto((int) percentualCompleto);
            }
            conDestino.commit();
        } catch (Exception ex) {
            conDestino.rollback();
            ProblemaNaImportacaoDeArquivo problemaDetalhado = new ProblemaNaImportacaoDeArquivo();
            problemaDetalhado.nomeDoArquivo = pArquivoTXT.getName();
            problemaDetalhado.linhaProblematicaDoArquivo = numeroDoRegistro;
            problemaDetalhado.detalhesSobreOProblema = ex;
            throw problemaDetalhado;
        } finally {
            pAndamento.setPercentualCompleto(100);
            in.close();
            if (stmtLimpezaInicialDestino != null && (!stmtLimpezaInicialDestino.isClosed())) {
                stmtLimpezaInicialDestino.close();
            }
            if (stmtDestino != null && (!stmtDestino.isClosed())) {
                stmtDestino.close();
            }
        }
    }

    public List<ScheduledFuture> tarefasParalelas = new ArrayList<ScheduledFuture>();

    public void processarDadosImportados(final Andamento pAndamento) throws Exception {
        ScheduledFuture acompanhamentoDoTemporizador = null;
        try {
            reiniciarPercentualConcluidoDoProcessamento();
            Callable<Boolean> carga = new Callable<Boolean>() {

                @Override
                public Boolean call() throws SQLException, InterruptedException {
                    ManipuladorDeBancoDeDados processador = ManipuladorDeBancoDeDados.this.clone();
                    String sql = "{ call PC_CARGA.PR_RODAR_CARGA_COMPLETA() }";
                    CallableStatement stmtDestino = processador.conDestino.prepareCall(sql);
                    stmtDestino.execute();
                    stmtDestino.close();
                    processador.conDestino.commit();
                    processador.finalizar();
                    return Boolean.TRUE;
                }
            };
            int atraso = 0;
            final ScheduledFuture<Boolean> acompanhamentoDaCarga = Executors.newSingleThreadScheduledExecutor().schedule(carga, atraso, TimeUnit.SECONDS);
            Runnable temporizador = new Runnable() {

                @Override
                public void run() {
                    if (!acompanhamentoDaCarga.isDone()) {
                        try {
                            int percentualConcluido = obterPercentualConcluidoDoProcessamento();
                            pAndamento.setPercentualCompleto(percentualConcluido);
                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                        }
                    }
                }

                private int obterPercentualConcluidoDoProcessamento() throws Exception {
                    int progresso = 0;
                    CallableStatement stmtDestino = null;
                    try {
                        String sql = "{ call ? := PC_CARGA.FN_OBTER_PROGRESSO_DA_CARGA() }";
                        stmtDestino = ManipuladorDeBancoDeDados.this.conDestino.prepareCall(sql);
                        stmtDestino.registerOutParameter(1, OracleTypes.INTEGER);
                        stmtDestino.execute();
                        Integer progressoTmp = (Integer) stmtDestino.getObject(1);
                        if (progressoTmp != null) {
                            progresso = progressoTmp.intValue();
                        }
                    } catch (Exception ex) {
                        progresso = 0;
                        throw ex;
                    } finally {
                        if (stmtDestino != null && (!stmtDestino.isClosed())) {
                            stmtDestino.close();
                        }
                    }
                    return progresso;
                }
            };
            int atrasoInicial = 0;
            int atrasosPosteriores = 1;
            acompanhamentoDoTemporizador = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(temporizador, atrasoInicial, atrasosPosteriores, TimeUnit.SECONDS);
            tarefasParalelas.add(acompanhamentoDaCarga);
            tarefasParalelas.add(acompanhamentoDoTemporizador);
            acompanhamentoDaCarga.get();
        } catch (Exception ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SQLException) {
                SQLException causa = (SQLException) ex.getCause();
                if (causa.getErrorCode() == 54) {
                    ex = new ProcessamentoAnteriorAindaNaoEncerrado(causa);
                }
            }
            throw ex;
        } finally {
            pAndamento.setPercentualCompleto(100);
            acompanhamentoDoTemporizador.cancel(true);
            tarefasParalelas.clear();
        }
    }

    private void reiniciarPercentualConcluidoDoProcessamento() throws SQLException {
        Statement stmtDestino = conDestino.createStatement();
        String sql = "TRUNCATE TABLE TB_PROGRESSO_PROCESSAMENTO";
        stmtDestino.executeUpdate(sql);
        conDestino.commit();
    }

    private BigDecimal obterBigDecimal(String valor, int precisao, int escala) {
        return new BigDecimal(new BigInteger(valor), escala);
    }

    public RelacaoBolsaPorPIB[] obterCoordenadasDoGraficoDaRelacaoBolsaPorPIB(FiltroDaConsultaDeGrafico pFiltro) throws SQLException {
        String sql = "{ call ? := PC_CONSULTAS.FN_DASHBOARD(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
        CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
        stmtDestino.registerOutParameter(1, OracleTypes.CURSOR);
        stmtDestino.setString(2, pFiltro.periodo.toString());
        stmtDestino.setString(3, pFiltro.unidadeDeMedida.toString());
        stmtDestino.setString(4, (pFiltro.empresasNovatas ? "X" : ""));
        stmtDestino.setString(5, (pFiltro.empresasVeteranas ? "X" : ""));
        stmtDestino.setString(6, (pFiltro.empresasComDividendos ? "X" : ""));
        stmtDestino.setString(7, (pFiltro.empresasComBonificacoes ? "X" : ""));
        stmtDestino.setString(8, (pFiltro.empresasSemProventos ? "X" : ""));
        stmtDestino.setInt(9, pFiltro.valorSuavizacao);
        stmtDestino.setDate(10, pFiltro.diaLimite);
        stmtDestino.setString(11, pFiltro.moeda.toString());
        stmtDestino.setString(12, (pFiltro.evolucaoRelativaAoMercado ? "X" : ""));
        stmtDestino.setString(13, pFiltro.aba.toString());
        stmtDestino.setString(14, (pFiltro.visaoGeralDoMercado ? "X" : ""));
        stmtDestino.setString(15, (pFiltro.forcasDoMercado ? "X" : ""));
        stmtDestino.setString(16, (pFiltro.divergenciaDoMercado ? "X" : ""));
        stmtDestino.setString(17, (pFiltro.setoresComApenasUmSubsetor ? "X" : ""));
        stmtDestino.setString(18, (pFiltro.setoresComVariosSubsetores ? "X" : ""));
        stmtDestino.setInt(19, (pFiltro.setor != null ? pFiltro.setor.obterCodigo() : null));
        stmtDestino.setString(20, (pFiltro.subsetoresComApenasUmSegmento ? "X" : ""));
        stmtDestino.setString(21, (pFiltro.subsetoresComVariosSegmentos ? "X" : ""));
        stmtDestino.setInt(22, (pFiltro.subsetor != null ? pFiltro.subsetor.obterCodigo() : null));
        stmtDestino.setString(23, (pFiltro.segmentosComApenasUmaEmpresa ? "X" : ""));
        stmtDestino.setString(24, (pFiltro.segmentosComVariasEmpresas ? "X" : ""));
        stmtDestino.setInt(25, (pFiltro.segmento != null ? pFiltro.segmento.obterCodigo() : null));
        stmtDestino.setString(26, (pFiltro.empresasComApenasUmPapel ? "X" : ""));
        stmtDestino.setString(27, (pFiltro.empresasComVariosPapeis ? "X" : ""));
        stmtDestino.setInt(28, (pFiltro.empresa != null ? pFiltro.empresa.obterCodigo() : null));
        stmtDestino.setInt(29, (pFiltro.papel != null ? pFiltro.papel.obterCodigo() : null));
        stmtDestino.execute();
        ResultSet rs = (ResultSet) stmtDestino.getObject(1);
        ArrayList<RelacaoBolsaPorPIB> cotacoes = new ArrayList<RelacaoBolsaPorPIB>();
        RelacaoBolsaPorPIB cotacaoAtual;
        final BigDecimal BILHAO = new BigDecimal("1000000000");
        while (rs.next()) {
            cotacaoAtual = new RelacaoBolsaPorPIB();
            cotacaoAtual.ultimoDia = rs.getDate("ULTIMO_DIA");
            cotacaoAtual.valorDeMercadoDaBolsaEmBilhoes = rs.getBigDecimal("VALOR_BOLSA").divide(BILHAO).doubleValue();
            cotacaoAtual.valorDoPIBEmBilhoes = rs.getBigDecimal("VALOR_PIB").divide(BILHAO).doubleValue();
            cotacaoAtual.percentualBolsaPorPIB = rs.getBigDecimal("PERC_BOLSA_POR_PIB").doubleValue();
            cotacoes.add(cotacaoAtual);
        }
        return cotacoes.toArray(new RelacaoBolsaPorPIB[cotacoes.size()]);
    }

    public Cotacao[] obterCoordenadasDoGraficoDasCotacoes(FiltroDaConsultaDeGrafico pFiltro) throws SQLException {
        String sql = "{ call ? := PC_CONSULTAS.FN_DASHBOARD(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
        CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
        stmtDestino.registerOutParameter(1, OracleTypes.CURSOR);
        stmtDestino.setString(2, pFiltro.periodo.toString());
        stmtDestino.setString(3, pFiltro.unidadeDeMedida.toString());
        stmtDestino.setString(4, (pFiltro.empresasNovatas ? "X" : ""));
        stmtDestino.setString(5, (pFiltro.empresasVeteranas ? "X" : ""));
        stmtDestino.setString(6, (pFiltro.empresasComDividendos ? "X" : ""));
        stmtDestino.setString(7, (pFiltro.empresasComBonificacoes ? "X" : ""));
        stmtDestino.setString(8, (pFiltro.empresasSemProventos ? "X" : ""));
        stmtDestino.setInt(9, pFiltro.valorSuavizacao);
        stmtDestino.setDate(10, pFiltro.diaLimite);
        stmtDestino.setString(11, pFiltro.moeda.toString());
        stmtDestino.setString(12, (pFiltro.evolucaoRelativaAoMercado ? "X" : ""));
        stmtDestino.setString(13, pFiltro.aba.toString());
        stmtDestino.setString(14, (pFiltro.visaoGeralDoMercado ? "X" : ""));
        stmtDestino.setString(15, (pFiltro.forcasDoMercado ? "X" : ""));
        stmtDestino.setString(16, (pFiltro.divergenciaDoMercado ? "X" : ""));
        stmtDestino.setString(17, (pFiltro.setoresComApenasUmSubsetor ? "X" : ""));
        stmtDestino.setString(18, (pFiltro.setoresComVariosSubsetores ? "X" : ""));
        stmtDestino.setInt(19, (pFiltro.setor != null ? pFiltro.setor.obterCodigo() : null));
        stmtDestino.setString(20, (pFiltro.subsetoresComApenasUmSegmento ? "X" : ""));
        stmtDestino.setString(21, (pFiltro.subsetoresComVariosSegmentos ? "X" : ""));
        stmtDestino.setInt(22, (pFiltro.subsetor != null ? pFiltro.subsetor.obterCodigo() : null));
        stmtDestino.setString(23, (pFiltro.segmentosComApenasUmaEmpresa ? "X" : ""));
        stmtDestino.setString(24, (pFiltro.segmentosComVariasEmpresas ? "X" : ""));
        stmtDestino.setInt(25, (pFiltro.segmento != null ? pFiltro.segmento.obterCodigo() : null));
        stmtDestino.setString(26, (pFiltro.empresasComApenasUmPapel ? "X" : ""));
        stmtDestino.setString(27, (pFiltro.empresasComVariosPapeis ? "X" : ""));
        stmtDestino.setInt(28, (pFiltro.empresa != null ? pFiltro.empresa.obterCodigo() : null));
        stmtDestino.setInt(29, (pFiltro.papel != null ? pFiltro.papel.obterCodigo() : null));
        stmtDestino.execute();
        ResultSet rs = (ResultSet) stmtDestino.getObject(1);
        List<Cotacao> cotacoes = new ArrayList<Cotacao>();
        Cotacao cotacaoAtual;
        java.util.Date dataDoPrimeiroPregaoDaSerieAtual = null;
        boolean mudouDeSerie;
        String codigoDaSerieAtual, codigoDaSerieAnterior = null;
        while (rs.next()) {
            codigoDaSerieAtual = rs.getString("CODIGO");
            mudouDeSerie = (!codigoDaSerieAtual.equals(codigoDaSerieAnterior));
            if (mudouDeSerie) {
                dataDoPrimeiroPregaoDaSerieAtual = rs.getDate("DIA");
            }
            java.util.Date dataDoPregao = rs.getDate("DIA");
            Double preult, preabe, premax, premin;
            Double mediaMovelCurtoPrazoPreult, mediaMovelMedioPrazoPreult, mediaMovelLongoPrazoPreult, mediaMovelCurtoPrazoVolumeRelativo;
            MyBoolean periodoValido = new MyBoolean();
            if (pFiltro.aba == SubabaConsultaGraficos.PAPEL) {
                preult = rs.getDouble("VALOR_PRECO_FECHAMENTO");
                preabe = rs.getDouble("VALOR_PRECO_ABERTURA");
                premax = rs.getDouble("VALOR_PRECO_MAXIMO");
                premin = rs.getDouble("VALOR_PRECO_MINIMO");
                mediaMovelCurtoPrazoPreult = rs.getDouble("valor_mm_curta_preco");
                mediaMovelMedioPrazoPreult = rs.getDouble("valor_mm_media_preco");
                mediaMovelLongoPrazoPreult = rs.getDouble("valor_mm_longa_preco");
                mediaMovelCurtoPrazoVolumeRelativo = rs.getDouble("valor_mm_volume");
                cotacaoAtual = (Cotacao) new CotacaoDetalhada(rs.getInt("CODIGO"), null, rs.getString("DESCRICAO"), dataDoPregao, preabe, premax, premin, preult, mediaMovelCurtoPrazoPreult, mediaMovelMedioPrazoPreult, mediaMovelLongoPrazoPreult, mediaMovelCurtoPrazoVolumeRelativo, null, pFiltro.periodo, pFiltro.unidadeDeMedida, dataDoPrimeiroPregaoDaSerieAtual, false, true, periodoValido);
            } else {
                preult = rs.getDouble("VALOR_PRECO");
                cotacaoAtual = (Cotacao) new CotacaoResumida(rs.getInt("CODIGO"), null, rs.getString("DESCRICAO"), dataDoPregao, preult, null, pFiltro.periodo, pFiltro.unidadeDeMedida, dataDoPrimeiroPregaoDaSerieAtual, false, true, periodoValido);
            }
            if (!periodoValido.valorBinario) {
                continue;
            }
            cotacaoAtual.alterarPlotarVolume("X".equals(rs.getString("PREGAO_VALIDO_VOLUME")));
            if (pFiltro.aba == SubabaConsultaGraficos.PAPEL) {
                cotacaoAtual.alterarCodigoDaEmpresaNaCVM(rs.getInt("COD_EMPRESA_CVM"));
                cotacaoAtual.alterarDataExDireitoAEvento("X".equals(rs.getString("EX_PROVENTO")));
                if (cotacaoAtual.obterDataExDireitoAEvento()) {
                    EventoSocietario evento = new EventoSocietario();
                    evento.pregao = cotacaoAtual.obterDataDoPregao();
                    if ("X".equals(rs.getString("EX_PROVENTO_DINHEIRO"))) {
                        evento.tipo = TipoDeEventoSocietario.PROVENTO_EM_DINHEIRO;
                        evento.valorRetirado = rs.getBigDecimal("VALOR_RETIRADO");
                    } else if ("X".equals(rs.getString("EX_BONIFICACAO"))) {
                        evento.tipo = TipoDeEventoSocietario.AGRUPAMENTO_OU_DESDOBRAMENTO;
                    } else if ("X".equals(rs.getString("RENOMEADO"))) {
                        evento.tipo = TipoDeEventoSocietario.RENOMEACAO;
                    }
                    evento.descricao = rs.getString("RESUMO_DA_ALTERACAO_SOFRIDA");
                    cotacaoAtual.alterarEvento(evento);
                }
            }
            cotacaoAtual.alterarVolume(rs.getBigDecimal("VALOR_VOLUME"));
            cotacoes.add(cotacaoAtual);
            codigoDaSerieAnterior = codigoDaSerieAtual;
        }
        return cotacoes.toArray(new Cotacao[cotacoes.size()]);
    }

    private ItemSelecionavel[] obterOpcoesDeFiltro(String pNomeDaProcedure, String pNomeDaColunaChave, String pNomeDaColunaDaSigla, String pNomeDaColunaDescritiva) throws SQLException {
        String sql = "{ call ? := PC_CONSULTAS." + pNomeDaProcedure + "() }";
        CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
        stmtDestino.registerOutParameter(1, OracleTypes.CURSOR);
        stmtDestino.execute();
        ResultSet rs = (ResultSet) stmtDestino.getObject(1);
        ArrayList<ItemSelecionavel> opcoes = new ArrayList<ItemSelecionavel>();
        int chave;
        String sigla, descricao;
        while (rs.next()) {
            chave = rs.getInt(pNomeDaColunaChave);
            if (pNomeDaColunaDaSigla == null) {
                sigla = null;
            } else {
                sigla = rs.getString(pNomeDaColunaDaSigla);
            }
            descricao = rs.getString(pNomeDaColunaDescritiva);
            opcoes.add(new ItemSelecionavelImpl(chave, sigla, descricao));
        }
        return opcoes.toArray(new ItemSelecionavel[] {});
    }

    public ItemSelecionavel[] obterSetores() throws SQLException {
        return obterOpcoesDeFiltro("FN_LISTAR_SETORES", "CODIGO", null, "DESCRICAO");
    }

    public ItemSelecionavel[] obterSubsetores() throws SQLException {
        return obterOpcoesDeFiltro("FN_LISTAR_SUBSETORES", "CODIGO", null, "DESCRICAO");
    }

    public ItemSelecionavel[] obterSegmentos() throws SQLException {
        return obterOpcoesDeFiltro("FN_LISTAR_SEGMENTOS", "CODIGO", null, "DESCRICAO");
    }

    public ItemSelecionavel[] obterEmpresas() throws SQLException {
        return obterOpcoesDeFiltro("FN_LISTAR_EMPRESAS", "CODIGO", "SIGLA", "DESCRICAO");
    }

    public ItemSelecionavel[] obterPapeis() throws SQLException {
        return obterOpcoesDeFiltro("FN_LISTAR_PAPEIS", "CODIGO", "SIGLA", "SIGLA");
    }

    public ItemSelecionavel[] obterRelatorios() throws SQLException {
        return obterOpcoesDeFiltro("FN_LISTAR_RELATORIOS", "CODIGO", "SIGLA", "DESCRICAO");
    }

    public void obterDadosDoRelatorio(FiltroDaConsultaDeRelatorio pFiltro, ArrayList<String> pColunas, ArrayList<Class> tipoDasColunas, ArrayList<ArrayList<Comparable>> pLinhas) throws SQLException {
        String nomeDoRelatorio = pFiltro.relatorio.obterSigla();
        String sql = "{ call ? := PC_CONSULTAS." + nomeDoRelatorio + "(?,?,?,?,?,?,?,?) }";
        CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
        stmtDestino.registerOutParameter(1, OracleTypes.CURSOR);
        stmtDestino.setString(2, pFiltro.periodo.toString());
        stmtDestino.setString(3, (pFiltro.empresasNovatas ? "X" : ""));
        stmtDestino.setString(4, (pFiltro.empresasVeteranas ? "X" : ""));
        stmtDestino.setString(5, (pFiltro.empresasComDividendos ? "X" : ""));
        stmtDestino.setString(6, (pFiltro.empresasComBonificacoes ? "X" : ""));
        stmtDestino.setString(7, (pFiltro.empresasSemProventos ? "X" : ""));
        stmtDestino.setInt(8, pFiltro.valorSuavizacao);
        stmtDestino.setDate(9, pFiltro.diaLimite);
        stmtDestino.execute();
        ResultSet rs = (ResultSet) stmtDestino.getObject(1);
        ResultSetMetaData rsmd = rs.getMetaData();
        for (int iColuna = 1; iColuna <= rsmd.getColumnCount(); iColuna++) {
            pColunas.add(rsmd.getColumnLabel(iColuna));
            tipoDasColunas.add(String.class);
        }
        Comparable[] linhaAtual;
        String nomeColuna;
        ArrayList<Comparable[]> linhas = new ArrayList<Comparable[]>();
        Comparable valorColuna;
        while (rs.next()) {
            linhaAtual = new Comparable[rsmd.getColumnCount()];
            for (int iColuna = 1; iColuna <= rsmd.getColumnCount(); iColuna++) {
                nomeColuna = rsmd.getColumnName(iColuna);
                if (rsmd.getColumnType(iColuna) == java.sql.Types.NUMERIC) {
                    BigDecimal tmpDecimal = rs.getBigDecimal(nomeColuna);
                    if (tmpDecimal == null) {
                        valorColuna = null;
                    } else if (tmpDecimal.stripTrailingZeros().scale() > 0) {
                        valorColuna = new Double(rs.getDouble(nomeColuna));
                        if (tipoDasColunas.get(iColuna - 1) == String.class || tipoDasColunas.get(iColuna - 1) == Integer.class) {
                            tipoDasColunas.set(iColuna - 1, Double.class);
                        }
                    } else {
                        valorColuna = new Integer(rs.getInt(nomeColuna));
                        if (tipoDasColunas.get(iColuna - 1) == String.class) {
                            tipoDasColunas.set(iColuna - 1, Integer.class);
                        }
                    }
                    linhaAtual[iColuna - 1] = valorColuna;
                } else if (rsmd.getColumnType(iColuna) == java.sql.Types.TIMESTAMP) {
                    linhaAtual[iColuna - 1] = rs.getDate(nomeColuna);
                    if (tipoDasColunas.get(iColuna - 1) == String.class) {
                        tipoDasColunas.set(iColuna - 1, java.sql.Date.class);
                    }
                } else {
                    linhaAtual[iColuna - 1] = rs.getString(nomeColuna);
                }
            }
            linhas.add(linhaAtual);
        }
        ArrayList<Comparable> linhaTmp;
        Comparable coluna;
        for (Comparable[] linha : linhas) {
            linhaTmp = new ArrayList<Comparable>();
            for (int iColuna = 0; iColuna < linha.length; iColuna++) {
                if (linha[iColuna] instanceof Integer && tipoDasColunas.get(iColuna) == Double.class) {
                    coluna = new Double(linha[iColuna].toString());
                } else {
                    coluna = linha[iColuna];
                }
                linhaTmp.add(coluna);
            }
            pLinhas.add(linhaTmp);
        }
    }

    public Pregao[] obterPregoesSemTaxaDeCambio() throws SQLException {
        Pregao[] pregoesSemTaxaDeCambio = null;
        String sql = "{ call ? := PC_CONSULTAS.FN_LISTAR_DATAS_SEM_DOLAR() }";
        CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
        stmtDestino.registerOutParameter(1, OracleTypes.CURSOR);
        stmtDestino.execute();
        ResultSet rs = (ResultSet) stmtDestino.getObject(1);
        List<Pregao> pregoesSemCambioTmp = new ArrayList<Pregao>();
        while (rs.next()) {
            Pregao pregaoSemTaxaDeCambio = new Pregao();
            pregaoSemTaxaDeCambio.codigo = rs.getInt("COD_PREGAO");
            pregaoSemTaxaDeCambio.data = rs.getDate("DATA");
            pregoesSemCambioTmp.add(pregaoSemTaxaDeCambio);
        }
        if (pregoesSemCambioTmp.size() > 0) {
            pregoesSemTaxaDeCambio = pregoesSemCambioTmp.toArray(new Pregao[pregoesSemCambioTmp.size()]);
        }
        return pregoesSemTaxaDeCambio;
    }

    public MesEAno[] obterMesesSemPIB() throws SQLException {
        MesEAno[] mesesSemPIB = null;
        String sql = "SELECT COUNT(1) QTD FROM TMP_TB_PIB";
        Statement stmtCargaInicial = this.conDestino.createStatement();
        ResultSet rs = stmtCargaInicial.executeQuery(sql);
        rs.next();
        int quantidadeDeRegistros = rs.getInt("QTD");
        List<MesEAno> mesesSemPIBTmp = new ArrayList<MesEAno>();
        if (quantidadeDeRegistros == 0) {
            Calendar calendario = Calendar.getInstance();
            Calendar calendarioFinal = Calendar.getInstance();
            int anoInicial = 1990, mesInicial = 0, diaInicial = 1;
            int anoFinal = calendarioFinal.get(Calendar.YEAR), mesFinal = calendarioFinal.get(Calendar.MONTH), diaFinal = 2;
            calendario.clear();
            calendario.set(anoInicial, mesInicial, diaInicial);
            calendarioFinal.clear();
            calendarioFinal.set(anoFinal, mesFinal, diaFinal);
            int codigoMesAno = 1, mes, ano;
            while (calendario.before(calendarioFinal)) {
                ano = calendario.get(Calendar.YEAR);
                mes = calendario.get(Calendar.MONTH) + 1;
                MesEAno mesSemPIB = new MesEAno();
                mesSemPIB.codigo = codigoMesAno;
                mesSemPIB.mes = mes;
                mesSemPIB.ano = ano;
                mesesSemPIBTmp.add(mesSemPIB);
                calendario.add(Calendar.MONTH, 1);
                codigoMesAno++;
            }
        } else {
            sql = "{ call ? := PC_CONSULTAS.FN_LISTAR_MESES_SEM_PIB() }";
            CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
            stmtDestino.registerOutParameter(1, OracleTypes.CURSOR);
            stmtDestino.execute();
            rs = (ResultSet) stmtDestino.getObject(1);
            while (rs.next()) {
                MesEAno mesSemPIB = new MesEAno();
                mesSemPIB.codigo = rs.getInt("COD_MES_ANO");
                mesSemPIB.mes = rs.getInt("MES");
                mesSemPIB.ano = rs.getInt("ANO");
                mesesSemPIBTmp.add(mesSemPIB);
            }
        }
        if (mesesSemPIBTmp.size() > 0) {
            mesesSemPIB = mesesSemPIBTmp.toArray(new MesEAno[mesesSemPIBTmp.size()]);
        }
        return mesesSemPIB;
    }

    private static java.util.Date obterUltimoDiaDoMes(MesEAno pMes) {
        int ano = pMes.ano, mes = pMes.mes;
        Calendar calendario = Calendar.getInstance();
        calendario.clear();
        calendario.set(Calendar.YEAR, ano);
        calendario.set(Calendar.MONTH, mes - 1);
        calendario.set(Calendar.DAY_OF_MONTH, 1);
        calendario.add(Calendar.MONTH, 1);
        calendario.add(Calendar.DAY_OF_MONTH, -1);
        java.util.Date ultimoDiaDoMes = calendario.getTime();
        return ultimoDiaDoMes;
    }

    public void alterarCarteira(int pCodPapel) throws SQLException {
        String sql = "{ call PC_CARGA.PR_ALTERAR_CARTEIRA(?) }";
        CallableStatement stmtDestino = this.conDestino.prepareCall(sql);
        stmtDestino.setInt(1, pCodPapel);
        stmtDestino.execute();
        stmtDestino.close();
        conDestino.commit();
    }

    public void finalizar() throws SQLException {
        for (ScheduledFuture tarefaEmParalelo : this.tarefasParalelas) {
            tarefaEmParalelo.cancel(true);
        }
        this.conDestino.rollback();
        this.conDestino.close();
    }
}
