package spacewars.principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import spacewars.misc.FileBundle;
import spacewars.misc.LeitorDeFase;
import spacewars.principal.evento.ExplosaoListener;
import spacewars.principal.evento.FaseListener;
import spacewars.principal.evento.LaserListener;
import spacewars.principal.evento.NaveListener;

public class EngineJogo {

    private static List<LaserListener> laserlisteners = new ArrayList<LaserListener>();

    private static List<NaveListener> navelisteners = new ArrayList<NaveListener>();

    private static List<FaseListener> faselisteners = new ArrayList<FaseListener>();

    private static List<ExplosaoListener> explosaolisteners = new ArrayList<ExplosaoListener>();

    public static final File HomeDir;

    public static final File SpaceWarsDir;

    static {
        HomeDir = new File(System.getProperty("user.home"));
        SpaceWarsDir = new File(HomeDir, ".space-wars");
        SpaceWarsDir.mkdirs();
    }

    public static final Properties messageStrings;

    static {
        messageStrings = FileBundle.getProperties("config/mainMessages", ".txt", Locale.getDefault(), EngineJogo.class.getClassLoader());
    }

    public static final String NOME_VERSAO = messageStrings.getProperty("version");

    private static int pausado = 0;

    private static SortedSet<SubEngine> subEngines = new TreeSet<SubEngine>(new Comparator<SubEngine>() {

        public int compare(SubEngine eng1, SubEngine eng2) {
            if (eng1.getInstanteProximaExecucao() < eng2.getInstanteProximaExecucao()) return -1; else if (eng1.getInstanteProximaExecucao() > eng2.getInstanteProximaExecucao()) return 1; else return eng1.toString().compareTo(eng2.toString());
        }

        public boolean equals(Object obj) {
            return false;
        }
    });

    private static void adicionaSubEngine(String NomeSubEngine) throws Exception {
        Class classe = Class.forName(NomeSubEngine);
        try {
            Constructor construtor = classe.getConstructor();
            subEngines.add((SubEngine) construtor.newInstance());
        } catch (Exception e) {
            throw new Exception("Impossivel criar o subEngine " + NomeSubEngine, e);
        }
    }

    private static void adicionaSubEngines() throws Exception {
        BufferedReader listaSubEngines;
        try {
            listaSubEngines = new BufferedReader(new InputStreamReader(EngineJogo.class.getResourceAsStream("/config/subEngines.cfg")));
        } catch (Exception e) {
            throw new Exception("Não foi possivel iniciar o engine, pois a lista de subEngines não está disponivel", e);
        }
        while (listaSubEngines.ready()) {
            try {
                adicionaSubEngine(listaSubEngines.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static boolean acabou = false;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(FileBundle.getFile("config/asciiLogo", ".txt", EngineJogo.class.getClassLoader()), "utf-8");
        while (scanner.hasNextLine()) System.out.println(FileBundle.addKeyValues(scanner.nextLine(), messageStrings));
        System.out.println();
        System.out.println(messageStrings.getProperty("loading"));
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                acabou = true;
            }
        });
        try {
            adicionaSubEngines();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("#################################################################");
        System.out.println("#################### STARTING GAME ##############################");
        System.out.println("#################################################################");
        long now = System.nanoTime();
        while (!acabou) {
            SubEngine engAtual = subEngines.first();
            subEngines.remove(engAtual);
            long delay;
            do {
                delay = engAtual.getInstanteProximaExecucao() - System.nanoTime();
                if (delay > 0) try {
                    Thread.sleep(delay / 1000000000);
                } catch (Exception e) {
                }
            } while (delay > 0);
            now = System.nanoTime();
            long ultimaExecucao = engAtual.getInstanteUltimaExecucao();
            {
                String nome = engAtual.getClass().getName();
                int ind = nome.lastIndexOf(".");
                nome = nome.substring(ind + 1);
            }
            try {
                engAtual.run();
                subEngines.add(engAtual);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            float deltaTmpExecucao = (engAtual.getInstanteUltimaExecucao() - ultimaExecucao);
            float fps = 0;
            float percCPU = 0;
            if (deltaTmpExecucao > 0) {
                fps = 1000000000f / deltaTmpExecucao;
                percCPU = 100 * (System.nanoTime() - now) / (deltaTmpExecucao);
            }
            java.text.DecimalFormat nf = new java.text.DecimalFormat();
            nf.setMaximumFractionDigits(3);
            nf.setMaximumIntegerDigits(2);
            nf.setMinimumIntegerDigits(2);
            if (false) System.out.println(nf.format((System.nanoTime() - now) / 1000000f) + "ms " + nf.format(fps) + " fps) -> " + nf.format(percCPU) + "% CPU");
        }
        try {
            termina();
        } catch (Throwable t) {
        }
    }

    protected static void termina() throws Throwable {
        System.out.println("Closing SpaceWars");
        for (SubEngine se : subEngines) se.destroy();
        System.out.println("-----END-----");
    }

    public static void adicionaExplosao(Explosao exp) {
        for (ExplosaoListener explistener : explosaolisteners) explistener.adicionouExplosao(exp);
    }

    private static void iniciaVida(Peca p) {
        p.setAtributo("vida", p.getTipo().getProperty("Durabilidade", "1"));
        for (Peca sp : p.getSubPecas()) {
            iniciaVida(sp);
        }
    }

    public static void adicionaNave(Nave nave) {
        iniciaVida(nave);
        System.out.println(nave);
        Fase.getNaves().put(nave.getNome(), nave);
        for (NaveListener navelistener : navelisteners) navelistener.adicionouNave(nave);
    }

    public static void removeNave(Nave nave) {
        Fase.getNaves().remove(nave.getNome());
        for (NaveListener navelistener : navelisteners) navelistener.removeuNave(nave);
    }

    public static Nave separaParteNave(Peca pecaDestacada) {
        if (pecaDestacada.getPai() == null) return null;
        Nave navePai = pecaDestacada.getNave();
        Nave novaNave = new Nave(pecaDestacada, false);
        novaNave.setTime(navePai.getTime());
        novaNave.setMatriz(pecaDestacada.getMatrizAbsoluta());
        pecaDestacada.destruida = true;
        pecaDestacada.novaReferencia = novaNave;
        pecaDestacada.subPecas.clear();
        pecaDestacada.getPai().getSubPecas().remove(pecaDestacada);
        Fase.getNaves().put(novaNave.getNome(), novaNave);
        for (NaveListener navelistener : navelisteners) navelistener.separouParteNave(navePai, novaNave, pecaDestacada);
        return novaNave;
    }

    public static void adicionaLaser(Laser laser) {
        Fase.getLasers().add(laser);
        for (LaserListener laserlistener : laserlisteners) laserlistener.adicionouLaser(laser);
    }

    public static void removeLaser(Laser laser) {
        Fase.getLasers().remove(laser);
        for (LaserListener laserlistener : laserlisteners) laserlistener.removeuLaser(laser);
    }

    public static void addLaserListener(LaserListener l) {
        laserlisteners.add(l);
    }

    public static void addNaveListener(NaveListener l) {
        navelisteners.add(l);
    }

    public static void addFaseListener(FaseListener l) {
        faselisteners.add(l);
    }

    public static void addExplosaoListener(ExplosaoListener l) {
        explosaolisteners.add(l);
    }

    public static boolean estaPausado() {
        return pausado > 0;
    }

    public static void bloquear() {
        pausado++;
    }

    public static void desbloquear() {
        pausado--;
    }

    public static void leFase(URL fase) {
        try {
            for (Nave n : new ArrayList<Nave>(Fase.getNaves().values())) EngineJogo.removeNave(n);
            for (Laser l : new ArrayList<Laser>(Fase.getLasers())) EngineJogo.removeLaser(l);
            LeitorDeFase.LeFase(fase);
            for (FaseListener faselistener : faselisteners) faselistener.carregouFase();
            for (Nave n : Fase.getNaves().values()) {
                iniciaVida(n);
                for (NaveListener navelistener : navelisteners) navelistener.adicionouNave(n);
            }
        } catch (Exception e) {
            e.printStackTrace();
            acabou = true;
        }
    }
}
