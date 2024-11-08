package org.jcompany.commons.io;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import jregex.util.io.PathPattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jcompany.commons.io.filters.IFilterCopy;

/**
 * Fornece m�todos para trabalhar com:
 * <ul>
 * <li>C�pia de diret�rios</li>
 * <li>Mover diret�rios</li>
 * <li>Mover arquivos</li>
 * </ul>
 * 
 * @author Lucas Gon�alves
 * 
 */
public class PlcFileUtils {

    /**
	 * Copia o diret�rio recursivamente.
	 * 
	 * @param origem
	 *            Diret�rio � ser copiado
	 * @param destino
	 *            Diret�rio destino.
	 * @param pararError
	 *            Se true: Caso aconte�a alguma falha ao copiar um arquivo a
	 *            copia dos outros arquivos n�o ser� feita lan�ando um excess�o.
	 *            Se false: Continuar� copiando o diret�rio mesmo que d� erro ao
	 *            copiar um ou mais arquivos.
	 * @param filtros
	 *            lista de filtros. Esses filtros servem para decidir se ser�o
	 *            copiados ou n�o determinados arquivos.
	 * @throws IOException
	 */
    public static void copiarDiretorio(File origem, File destino, boolean pararError, IFilterCopy... filtros) throws IOException {
        String base = null;
        base = origem.getCanonicalPath();
        if (!origem.exists()) return;
        copyFile(new File(base), destino, base, pararError, false, false, filtros);
    }

    /**
	 * Copia o diret�rio recursivamente.
	 * 
	 * @param origem
	 *            Diret�rio � ser copiado
	 * @param destino
	 *            Diret�rio destino.
	 * @param pararError
	 *            Se true: Caso aconte�a alguma falha ao copiar um arquivo a
	 *            copia dos outros arquivos n�o ser� feita lan�ando um excess�o.
	 *            Se false: Continuar� copiando o diret�rio mesmo que d� erro ao
	 *            copiar um ou mais arquivos.
	 * @param debug
	 *            Imprime os arquivos copiados.
	 * @param filtros
	 *            lista de filtros. Esses filtros servem para decidir se ser�o
	 *            copiados ou n�o determinados arquivos.
	 * @throws IOException
	 */
    public static void copiarDiretorio(File origem, File destino, boolean pararError, boolean debug, IFilterCopy... filtros) throws IOException {
        String base = null;
        base = origem.getCanonicalPath();
        if (!origem.exists()) return;
        copyFile(new File(base), destino, base, pararError, false, debug, filtros);
    }

    /**
	 * Move o diret�rio recursivamente.
	 * 
	 * @param origem
	 *            Diret�rio � ser copiado
	 * @param destino
	 *            Diret�rio destino.
	 * @param pararError
	 *            Se true: Caso aconte�a alguma falha ao copiar um arquivo a
	 *            copia dos outros arquivos n�o ser� feita lan�ando um excess�o.
	 *            Se false: Continuar� copiando o diret�rio mesmo que d� erro ao
	 *            copiar um ou mais arquivos.
	 * @param filtros
	 *            lista de filtros. Esses filtros servem para decidir se ser�o
	 *            copiados ou n�o determinados arquivos.
	 * @throws IOException
	 */
    public static void mover(File origem, File destino, boolean pararError, IOFileFilter... filtros) throws IOException {
        String base = "";
        String normal = FilenameUtils.separatorsToUnix(origem.getAbsolutePath());
        String partes[] = normal.split("/");
        for (String parte : partes) {
            if (!parte.contains("*") && !parte.contains("?")) base += parte + "/"; else break;
        }
        base = base.substring(0, base.length() - 1);
        copiar(origem, destino, base, pararError, true, false, filtros);
    }

    private static void copyFile(File origem, File destino, String base, boolean pararError, boolean apagar, boolean debug, IFilterCopy... filtros) throws IOException {
        String relativo = getCaminhoRelativo(origem, base);
        File arquivoDestino = new File(destino, relativo);
        if (origem.isDirectory()) {
            if (filtros != null) for (IFilterCopy filtro : filtros) if (!filtro.copiar(origem, relativo)) return;
            for (File arquivo : origem.listFiles()) {
                copyFile(arquivo, destino, base, pararError, apagar, debug, filtros);
            }
            if (apagar) {
                if (origem.list().length == 0) FileUtils.deleteDirectory(origem);
            }
        } else {
            if (filtros != null) for (IFilterCopy filtro : filtros) if (!filtro.copiar(origem, arquivoDestino, relativo)) return;
            try {
                if (debug) System.out.println("Copiando arquivo: " + origem.getAbsolutePath());
                FileUtils.copyFile(origem, arquivoDestino);
                if (apagar) origem.delete();
            } catch (IOException e) {
                if (pararError) throw e;
                System.out.println("N�o foi poss�vel copiar o arquivo: " + origem.getAbsolutePath());
            }
        }
    }

    /**
	 * Retira a string base do nome do arquivo absoluto origem. Ex: origem
	 * c:\powerlogic\teste.txt base c:\powerlogic returna \teste.txt
	 * 
	 * @param origem
	 *            arquivo qualquer
	 * @param base
	 *            base relativa do arquivo origem
	 * @return endereco do arquivo relativo a base.
	 */
    public static String getCaminhoRelativo(File origem, String base) {
        String diferenca = null;
        String origemNormal = FilenameUtils.separatorsToUnix(origem.getAbsolutePath());
        String baseNormal = FilenameUtils.separatorsToUnix(base);
        if (origemNormal.length() == baseNormal.length()) return "";
        if (baseNormal.endsWith("/")) diferenca = origemNormal.substring(base.length()); else diferenca = origemNormal.substring(base.length() + 1);
        return diferenca;
    }

    public static void copiar(File origem, File destino, String base, boolean pararError, boolean apagar, boolean debug, IOFileFilter... filtros) throws IOException {
        if (!origem.isDirectory() && origem.exists()) {
            File destinoArquivo = new File(destino, getCaminhoRelativo(origem, base).equals("") ? origem.getName() : getCaminhoRelativo(origem, base));
            FileUtils.copyFile(origem, destinoArquivo);
            if (apagar) origem.delete();
            return;
        }
        if (origem.isDirectory() && origem.exists()) origem = new File(origem, "/**/*");
        PathPattern pp = new PathPattern(origem.getAbsolutePath());
        Enumeration e = pp.enumerateFiles();
        while (e.hasMoreElements()) {
            File origemArquivo = (File) e.nextElement();
            if (!accept(origemArquivo, filtros)) continue;
            File destinoArquivo = new File(destino, getCaminhoRelativo(origemArquivo, base));
            try {
                FileUtils.copyFile(origemArquivo, destinoArquivo);
                if (apagar) origemArquivo.delete();
            } catch (IOException e1) {
                if (pararError) throw e1;
            }
        }
    }

    private static boolean accept(File origem, IOFileFilter... filtros) {
        if (filtros != null) for (IOFileFilter filtro : filtros) {
            if (!filtro.accept(origem)) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        String origem = "C:\\Documents and Settings\\lucas.goncalves\\Meus documentos\\WorkSpaces\\Migracao\\jcompany_jcurriculo\\teste\\**\\*";
        PathPattern pp = new PathPattern(origem);
        Enumeration e = pp.enumerateFiles();
        while (e.hasMoreElements()) {
            File origemFile = (File) e.nextElement();
        }
    }
}
