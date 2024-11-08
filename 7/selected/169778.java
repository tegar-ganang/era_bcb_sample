package combinacion;

import java.util.Vector;
import carga.AdDatos;
import carga.Grupo;
import carga.Horario;
import carga.Maestro;
import carga.Materia;

public abstract class ModeloCombinatorio extends Thread {

    private Vector eventos;

    protected long total;

    protected long progreso;

    protected int maxHuecos;

    protected int maxHorarios;

    protected int maxHuecosInt;

    protected boolean conCupo;

    protected boolean abortar;

    protected Grupo materias[][];

    protected Horario horarioUsuario;

    public AdDatos datos;

    public Object[] arrSol;

    protected Vector hashHuecos[];

    protected int solCount;

    public long getTotal() {
        return total;
    }

    public long getProgreso() {
        return progreso;
    }

    private void ordenaPorDemanda(boolean inverso) {
        if (!inverso) {
            for (int z = 0; z < materias.length; z++) {
                for (int x = materias[z].length - 2; x >= 0; x--) for (int y = x; y <= materias[z].length - 2; y++) {
                    if (materias[z][y].getDis() < materias[z][y + 1].getDis()) {
                        Grupo tmp = materias[z][y];
                        materias[z][y] = materias[z][y + 1];
                        materias[z][y + 1] = tmp;
                    } else break;
                }
            }
        } else {
            for (int z = 0; z < materias.length; z++) {
                for (int x = materias[z].length - 2; x >= 0; x--) for (int y = x; y <= materias[z].length - 2; y++) {
                    if (materias[z][y].getDis() > materias[z][y + 1].getDis()) {
                        Grupo tmp = materias[z][y];
                        materias[z][y] = materias[z][y + 1];
                        materias[z][y + 1] = tmp;
                    } else break;
                }
            }
        }
    }

    private void ordenaPorTemprano(boolean inverso) {
        if (inverso) {
            for (int z = 0; z < materias.length; z++) {
                for (int x = materias[z].length - 2; x >= 0; x--) for (int y = x; y <= materias[z].length - 2; y++) {
                    if (materias[z][y].horario.getHorarioTotal() < materias[z][y + 1].horario.getHorarioTotal()) {
                        Grupo tmp = materias[z][y];
                        materias[z][y] = materias[z][y + 1];
                        materias[z][y + 1] = tmp;
                    } else break;
                }
            }
        } else {
            for (int z = 0; z < materias.length; z++) {
                for (int x = materias[z].length - 2; x >= 0; x--) for (int y = x; y <= materias[z].length - 2; y++) {
                    if (materias[z][y].horario.getHorarioTotal() > materias[z][y + 1].horario.getHorarioTotal()) {
                        Grupo tmp = materias[z][y];
                        materias[z][y] = materias[z][y + 1];
                        materias[z][y + 1] = tmp;
                    } else break;
                }
            }
        }
    }

    private void ordenaPorNdGrupos(boolean inverso) {
        if (inverso) {
            for (int x = materias.length - 2; x >= 0; x--) for (int y = x; y <= materias.length - 2; y++) {
                if (materias[y].length < materias[y + 1].length) {
                    Grupo tmp[] = materias[y];
                    materias[y] = materias[y + 1];
                    materias[y + 1] = tmp;
                } else break;
            }
        } else {
            for (int x = materias.length - 2; x >= 0; x--) for (int y = x; y <= materias.length - 2; y++) {
                if (materias[y].length > materias[y + 1].length) {
                    Grupo tmp[] = materias[y];
                    materias[y] = materias[y + 1];
                    materias[y + 1] = tmp;
                } else break;
            }
        }
    }

    public ModeloCombinatorio(AdDatos datos) {
        super();
        this.datos = datos;
        eventos = new Vector();
        hashHuecos = new Vector[90];
        solCount = 0;
        abortar = false;
        maxHorarios = datos.maxHorarios;
        maxHuecos = datos.maxHuecos;
        maxHuecosInt = datos.maxHuecosInt;
        conCupo = datos.conCupo;
        horarioUsuario = (Horario) datos.horarioUsuario.clone();
        materias = new Grupo[datos.materias.size()][];
        for (int x = 0; x < datos.materias.size(); x++) {
            int idx = 0;
            int grps = 0;
            Materia mat = (Materia) datos.materias.get(x);
            for (int y = 0; y < mat.maestros.size(); y++) {
                Maestro maestro = (Maestro) mat.maestros.get(y);
                if (maestro.getMarca()) {
                    for (int z = 0; z < maestro.grupos.size(); z++) {
                        Grupo grp = (Grupo) maestro.grupos.get(z);
                        if ((grp.getDis() > 0 || conCupo == false) && grp.subHorario(horarioUsuario)) grps++;
                    }
                }
            }
            materias[x] = new Grupo[grps];
            for (int y = 0; y < mat.maestros.size(); y++) {
                Maestro maestro = (Maestro) mat.maestros.get(y);
                if (maestro.getMarca()) {
                    for (int z = 0; z < maestro.grupos.size(); z++) {
                        Grupo grp = (Grupo) maestro.grupos.get(z);
                        if ((grp.getDis() > 0 || conCupo == false) && grp.subHorario(horarioUsuario)) materias[x][idx++] = (Grupo) maestro.grupos.get(z);
                    }
                }
            }
        }
        ordenaPorNdGrupos(false);
        if (datos.prDemanda > -1) {
            if (datos.prHora > -1) {
                if (datos.prDemanda < datos.prHora) {
                    ordenaPorTemprano(false);
                    ordenaPorDemanda(false);
                } else {
                    ordenaPorDemanda(false);
                    ordenaPorTemprano(false);
                }
            } else {
                if (datos.prDemanda < (datos.prHora * -1)) {
                    ordenaPorTemprano(true);
                    ordenaPorDemanda(false);
                } else {
                    ordenaPorDemanda(false);
                    ordenaPorTemprano(true);
                }
            }
        } else {
            if (datos.prHora > -1) {
                if ((-1 * datos.prDemanda) < datos.prHora) {
                    ordenaPorTemprano(false);
                    ordenaPorDemanda(true);
                } else {
                    ordenaPorDemanda(true);
                    ordenaPorTemprano(false);
                }
            } else {
                if (datos.prDemanda > datos.prHora) {
                    ordenaPorTemprano(true);
                    ordenaPorDemanda(true);
                } else {
                    ordenaPorDemanda(true);
                    ordenaPorTemprano(true);
                }
            }
        }
    }

    public void abortar() {
        abortar = true;
    }

    public void addModCombListener(ModCombListener ml) {
        eventos.add(ml);
    }

    public void terminar() {
        eventos.clear();
        arrSol = null;
        hashHuecos = null;
        solCount = 0;
    }

    protected void fireProgreso(String estado, int porcentaje) {
        for (int x = 0; x < eventos.size(); x++) {
            ((ModCombListener) eventos.get(x)).progreso(estado, porcentaje);
        }
    }

    protected void fireNuevaSolucion(Solucion s) {
        for (int x = 0; x < eventos.size(); x++) {
            ((ModCombListener) eventos.get(x)).nuevaSolucion(s);
        }
    }

    public void run() {
        try {
            combinar();
            if (solCount > 0) {
                arrSol = new Object[solCount];
                int indice = 0;
                for (int x = 0; x < 90; x++) {
                    if (hashHuecos[x] != null) for (int i = 0; i < hashHuecos[x].size(); i++) arrSol[indice++] = hashHuecos[x].get(i);
                }
                fireProgreso("Proceso terminado al", -3);
            } else fireProgreso("No se encontro ningun horario. Trata cambiando las Preferencias", -3);
        } catch (OutOfMemoryError e) {
            arrSol = null;
            hashHuecos = null;
            solCount = 0;
            System.gc();
            fireProgreso("Falta Memoria [Cierra ventanas y limita el n�mero de horarios a generar]", -3);
        }
    }

    public int getSolCount() {
        return solCount;
    }

    public Solucion getSol(int indice) {
        return (Solucion) arrSol[indice];
    }

    protected abstract void combinar();
}
