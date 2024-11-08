package com.gnu.acarrascal.RNA;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class Patrones {

    public enum TipoNormalizacion {

        NINGUNA, CERO_UNO, MENOS_UNO_UNO
    }

    ;

    public int NumDatosPorPatron() {
        return m_numDatosPorPatron;
    }

    ;

    public int NumPatrones() {
        return m_numPatrones;
    }

    ;

    public float[] Patron(int indice) {
        return m_patrones[indice];
    }

    public float[] GetValoresMayores() {
        return m_valoresMayores;
    }

    public float[] GetValoresMenores() {
        return m_valoresMenores;
    }

    int m_numPatrones;

    int m_numDatosPorPatron;

    float m_valorMayor;

    float m_valorMenor;

    float[] m_valoresMayores;

    float[] m_valoresMenores;

    TipoNormalizacion m_tipoNormalizacion;

    float[][] m_patrones;

    public Patrones() {
        m_tipoNormalizacion = TipoNormalizacion.NINGUNA;
        m_numPatrones = 0;
        m_patrones = null;
        m_valoresMayores = null;
        m_valoresMenores = null;
    }

    public void NormalizacionGlobal(TipoNormalizacion tipoNormalizacion) {
        int i, j;
        for (i = 0; i < m_numPatrones; i++) {
            for (j = 0; j < m_numDatosPorPatron; j++) {
                switch(tipoNormalizacion) {
                    case CERO_UNO:
                        m_patrones[i][j] = (m_patrones[i][j] - m_valorMenor) / (m_valorMayor - m_valorMenor);
                        break;
                    case MENOS_UNO_UNO:
                        m_patrones[i][j] = (2 * (m_patrones[i][j] - m_valorMenor) / (m_valorMayor - m_valorMenor)) - 1;
                        break;
                }
            }
        }
        m_tipoNormalizacion = tipoNormalizacion;
    }

    public void NormalizacionDiferenciada(TipoNormalizacion tipoNormalizacion) {
        int i, j;
        for (i = 0; i < m_numPatrones; i++) {
            for (j = 0; j < m_numDatosPorPatron; j++) {
                if (m_valoresMayores[j] == m_valoresMenores[j]) continue;
                switch(tipoNormalizacion) {
                    case CERO_UNO:
                        m_patrones[i][j] = (m_patrones[i][j] - m_valoresMenores[j]) / (m_valoresMayores[j] - m_valoresMenores[j]);
                        break;
                    case MENOS_UNO_UNO:
                        m_patrones[i][j] = (2 * (m_patrones[i][j] - m_valoresMenores[j]) / (m_valoresMayores[j] - m_valoresMenores[j])) - 1;
                        break;
                }
            }
        }
        m_tipoNormalizacion = tipoNormalizacion;
    }

    public void NormalizacionDiferenciada(float[] valoresMayores, float[] valoresMenores, TipoNormalizacion tipoNormalizacion) {
        int i;
        for (i = 0; i < m_numDatosPorPatron; i++) {
            m_valoresMayores[i] = valoresMayores[i];
            m_valoresMenores[i] = valoresMenores[i];
        }
        NormalizacionDiferenciada(tipoNormalizacion);
    }

    /**Carga los patrones desde un fichero guardando los valores
	 * mayor y menor, as� como el numero de patrones.
	 * Formato
	 * NumPatrones NumDatosPorPatron
	 * Patrones
	 */
    public boolean CargarPatrones(String nombreFichero) {
        int i, j, k;
        FileReader fichero;
        try {
            fichero = new FileReader(nombreFichero);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        BufferedReader bf = new BufferedReader(fichero);
        String line = null;
        i = 0;
        boolean firstTime = true;
        try {
            while ((line = bf.readLine()) != null) {
                line = line.trim();
                if (line.length() < 1) continue;
                StringTokenizer st = new StringTokenizer(line);
                if (!st.hasMoreElements()) continue;
                if (firstTime) {
                    m_numPatrones = Integer.parseInt(st.nextToken());
                    m_numDatosPorPatron = Integer.parseInt(st.nextToken());
                    m_patrones = new float[m_numPatrones][];
                    m_valoresMayores = new float[m_numDatosPorPatron];
                    m_valoresMenores = new float[m_numDatosPorPatron];
                    firstTime = false;
                    continue;
                }
                m_patrones[i] = new float[m_numDatosPorPatron];
                j = 0;
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    if (token.length() < 1) continue;
                    m_patrones[i][j] = Float.parseFloat(token);
                    j++;
                }
                i++;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            return false;
        }
        try {
            bf.close();
            fichero.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        for (i = 0; i < m_numPatrones; i++) {
            for (j = 0; j < m_numDatosPorPatron; j++) {
                if ((i == 0) && (j == 0)) {
                    m_valorMayor = m_valorMenor = m_patrones[i][j];
                    for (k = 0; k < m_numDatosPorPatron; k++) m_valoresMayores[k] = m_valoresMenores[k] = m_patrones[i][k];
                } else {
                    if (m_patrones[i][j] > m_valorMayor) m_valorMayor = m_patrones[i][j]; else if (m_patrones[i][j] < m_valorMenor) m_valorMenor = m_patrones[i][j];
                    if (m_patrones[i][j] > m_valoresMayores[j]) m_valoresMayores[j] = m_patrones[i][j]; else if (m_patrones[i][j] < m_valoresMenores[j]) m_valoresMenores[j] = m_patrones[i][j];
                }
            }
        }
        return true;
    }

    public boolean FiltrarPatrones(int numEntradas, float errorPermitido) {
        int i, j, k;
        if ((m_numPatrones == 0) || (numEntradas < 0) || (numEntradas > m_numDatosPorPatron - 1)) return false;
        float error;
        for (i = 0; i < m_numPatrones - 1; i++) {
            for (j = i + 1; j < m_numPatrones; j++) {
                boolean eliminarPatron = true;
                for (k = 0; k < numEntradas; k++) {
                    error = m_patrones[i][k] - m_patrones[j][k];
                    if (error < 0) error = -error;
                    if (error > errorPermitido) {
                        eliminarPatron = false;
                        break;
                    }
                }
                if (eliminarPatron) {
                    eliminarPatron = false;
                    for (k = numEntradas; k < m_numDatosPorPatron; k++) {
                        error = m_patrones[i][k] - m_patrones[j][k];
                        if (error < 0) error = -error;
                        if (error > errorPermitido) {
                            eliminarPatron = true;
                            break;
                        }
                    }
                }
                if (eliminarPatron) {
                    EliminarPatron(j);
                    j--;
                }
            }
        }
        return true;
    }

    public boolean EliminarPatron(int indice) {
        int i;
        if ((m_numPatrones == 0) || (indice < 0) || (indice > m_numPatrones - 1)) return false;
        float[][] aux = new float[m_numPatrones - 1][];
        for (i = 0; i < indice; i++) aux[i] = m_patrones[i];
        for (i = indice; i < m_numPatrones; i++) aux[i] = m_patrones[i + 1];
        m_patrones = aux;
        m_numPatrones--;
        return true;
    }
}

;
