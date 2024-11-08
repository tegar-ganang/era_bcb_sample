package servidor.log.impl;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import servidor.Id;
import servidor.catalog.Catalogo;
import servidor.catalog.Valor;
import servidor.catalog.tipo.Conversor;
import servidor.indice.hash.RegistroIndice;
import servidor.indice.hash.RegistroIndice.ID;
import servidor.lock.FabricaLockManager;
import servidor.log.LSN;
import servidor.log.Log;
import servidor.log.Operacion;
import servidor.log.impl.estructuras.DatoBloqueSucio;
import servidor.log.impl.estructuras.DatoTransaccion;
import servidor.log.impl.eventos.Evento;
import servidor.tabla.Registro;
import servidor.transaccion.Transaccion;

public class LogImpl implements Log {

    private FileOutputStream escritor;

    private static final String LOG_FILENAME = "log";

    private static final String MASTER_RECORD_FILENAME = "master_record";

    public LogImpl() {
        try {
            File logFile = new File(LOG_FILENAME);
            if (!logFile.exists() || logFile.length() == 0) {
                this.escritor = new FileOutputStream(logFile);
                this.escritor.write('L');
                this.escritor.write('O');
                this.escritor.write('G');
                this.escritor.write(':');
                this.escritor.flush();
            } else {
                this.escritor = new FileOutputStream(logFile, true);
                this.escritor.getChannel().position(logFile.length());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Operacion | Longitud | CantidadTx | {idTransaccion | Estado | lastLSN | cantidadUndoNextLSN | {undoNextLSN} } | CantidadPagSucias | {idPagina | recLSN}  
     */
    public LSN escribirEndCheckpoint(Collection<DatoTransaccion> transacciones, Collection<DatoBloqueSucio> paginasSucias) {
        byte[] evento = LogHelper.enteroAByteArray(transacciones.size());
        for (DatoTransaccion transaccion : transacciones) {
            byte[] idTrans = LogHelper.IdTransaccionAByteArray(transaccion.idTransaccion);
            evento = LogHelper.concatenar(evento, idTrans);
            byte[] estado = LogHelper.enteroAByteArray(transaccion.estado.ordinal());
            evento = LogHelper.concatenar(evento, estado);
            byte[] lastLSN = LogHelper.LSNAByteArray(transaccion.lastLSN);
            evento = LogHelper.concatenar(evento, lastLSN);
            SortedSet<LSN> undoNextLSNs = transaccion.undoNextLSN;
            byte[] cantidadUndoNextLSNs = LogHelper.enteroAByteArray(undoNextLSNs.size());
            evento = LogHelper.concatenar(evento, cantidadUndoNextLSNs);
            for (LSN undoNextLSN : undoNextLSNs) {
                byte[] undoNextLSNBytes = LogHelper.LSNAByteArray(undoNextLSN);
                evento = LogHelper.concatenar(evento, undoNextLSNBytes);
            }
            byte[] cantidadLocks = LogHelper.enteroAByteArray(transaccion.registrosBloqueados.size());
            evento = LogHelper.concatenar(evento, cantidadLocks);
            for (Registro.ID idRegistro : transaccion.registrosBloqueados) {
                byte[] registroBytes = LogHelper.IdRegistroAByteArray(idRegistro);
                evento = LogHelper.concatenar(evento, registroBytes);
            }
        }
        byte[] cantidadSucias = LogHelper.enteroAByteArray(paginasSucias.size());
        evento = LogHelper.concatenar(evento, cantidadSucias);
        for (DatoBloqueSucio paginaSucia : paginasSucias) {
            byte[] idPagina = LogHelper.IdPaginaAByteArray(paginaSucia.idPagina);
            evento = LogHelper.concatenar(evento, idPagina);
            byte[] recoveryLSN = LogHelper.LSNAByteArray(paginaSucia.recLSN);
            evento = LogHelper.concatenar(evento, recoveryLSN);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.END_CHECKPOINT);
        evento = LogHelper.concatenar(operacion, evento);
        LSN lsn = this.escribir(evento);
        this.forzarADisco();
        return lsn;
    }

    /**
	 * Operacion 
	 */
    public LSN escribirBeginCheckpoint() {
        byte[] evento = LogHelper.OperacionAByteArray(Operacion.BEGIN_CHECKPOINT);
        LSN lsn = this.escribir(evento);
        return lsn;
    }

    public void escribirMasterRecord(LSN lsn) {
        try {
            FileOutputStream masterRecordFileOutputStream = new FileOutputStream(MASTER_RECORD_FILENAME);
            byte[] lsnBytes = LogHelper.LSNAByteArray(lsn);
            masterRecordFileOutputStream.write(lsnBytes);
            masterRecordFileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | idTransaccionHija | lastLSNHija  
	 */
    public LSN escribirChildCommittedTransaccion(Transaccion transaccion, Transaccion transaccionHija) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSNBytes = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSNBytes);
        byte[] transaccionHijaBytes = LogHelper.IdTransaccionAByteArray(transaccionHija.id());
        evento = LogHelper.concatenar(evento, transaccionHijaBytes);
        LSN ultimoLSNHija = transaccionHija.ultimoLSN();
        byte[] lastLSN_Hija = LogHelper.LSNAByteArray(ultimoLSNHija);
        evento = LogHelper.concatenar(evento, lastLSN_Hija);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.CHILD_COMMITTED);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN  
	 */
    public LSN escribirPrepareTransaccion(Transaccion transaccion) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        Set<Id> locks = FabricaLockManager.dameInstancia().locksExclusivos(transaccion.id());
        Iterator<Id> iterator = locks.iterator();
        while (iterator.hasNext()) {
            Id id = iterator.next();
            if (!(id instanceof Registro.ID)) {
                iterator.remove();
            }
        }
        byte[] cantidadLocks = LogHelper.enteroAByteArray(locks.size());
        evento = LogHelper.concatenar(evento, cantidadLocks);
        for (Id lock : locks) {
            Registro.ID idRegistro = (Registro.ID) lock;
            byte[] registroBytes = LogHelper.IdRegistroAByteArray(idRegistro);
            evento = LogHelper.concatenar(evento, registroBytes);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.PREPARE);
        evento = LogHelper.concatenar(operacion, evento);
        LSN lsn = this.escribir(evento);
        this.forzarADisco();
        return lsn;
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN  
	 */
    public LSN escribirFinTransaccion(Transaccion transaccion) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.END);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | idRegistro | CantidadColumnas | {nroColumna | ContenidoNuevo}
	 */
    public LSN escribirInsert(Transaccion transaccion, Registro.ID idRegistro, Collection<Valor> valoresNuevos) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] registro = LogHelper.IdRegistroAByteArray(idRegistro);
        evento = LogHelper.concatenar(evento, registro);
        byte[] cantidadColumnas = LogHelper.enteroAByteArray(valoresNuevos.size());
        evento = LogHelper.concatenar(evento, cantidadColumnas);
        for (Valor valorNuevo : valoresNuevos) {
            byte[] nroColumna = LogHelper.enteroAByteArray(valorNuevo.posicion());
            Conversor conversor = Conversor.conversorABytes();
            byte[] contenido = (byte[]) conversor.convertir(valorNuevo.campo(), valorNuevo.contenido());
            byte[] columna = LogHelper.concatenar(nroColumna, contenido);
            evento = LogHelper.concatenar(evento, columna);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.INSERT);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | idRegistro | CantidadColumnas | {nroColumna | ContenidoViejo | ContenidoNuevo}
	 */
    public LSN escribirUpdate(Transaccion transaccion, Registro registroViejo, Collection<Valor> valoresNuevos) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] registroBytes = LogHelper.IdRegistroAByteArray(registroViejo.id());
        evento = LogHelper.concatenar(evento, registroBytes);
        byte[] cantidadColumnas = LogHelper.enteroAByteArray(valoresNuevos.size());
        evento = LogHelper.concatenar(evento, cantidadColumnas);
        Conversor conversor = Conversor.conversorABytes();
        for (Valor valorNuevo : valoresNuevos) {
            byte[] columna = LogHelper.enteroAByteArray(valorNuevo.posicion());
            byte[] datoViejo = (byte[]) conversor.convertir(valorNuevo.campo(), registroViejo.valor(valorNuevo.posicion()));
            columna = LogHelper.concatenar(columna, datoViejo);
            byte[] datoNuevo = (byte[]) conversor.convertir(valorNuevo.campo(), valorNuevo.contenido());
            columna = LogHelper.concatenar(columna, datoNuevo);
            evento = LogHelper.concatenar(evento, columna);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.UPDATE);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | idRegistro | CantidadColumnas | {nroColumna | ContenidoViejo}
	 */
    public LSN escribirDelete(Transaccion transaccion, Registro registroViejo) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] registroBytes = LogHelper.IdRegistroAByteArray(registroViejo.id());
        evento = LogHelper.concatenar(evento, registroBytes);
        byte[] cantidadColumnas = LogHelper.enteroAByteArray(registroViejo.aridad());
        evento = LogHelper.concatenar(evento, cantidadColumnas);
        List<Valor> valoresViejos = registroViejo.getValores();
        for (Valor valorViejo : valoresViejos) {
            byte[] nroColumna = LogHelper.enteroAByteArray(valorViejo.posicion());
            Conversor conversor = Conversor.conversorABytes();
            byte[] datoViejo = (byte[]) conversor.convertir(valorViejo.campo(), valorViejo.contenido());
            byte[] columna = LogHelper.concatenar(nroColumna, datoViejo);
            evento = LogHelper.concatenar(evento, columna);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.DELETE);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | UndoNextLSN
	 */
    public LSN escribirDummyCLR(Transaccion transaccion, LSN undoNextLSN) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] lsn = LogHelper.LSNAByteArray(undoNextLSN);
        evento = LogHelper.concatenar(evento, lsn);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.DUMMY_CLR);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | cantidadUndoLSN | {UndoNextLSN} | idRegistro
	 */
    public LSN escribirCLRInsert(Transaccion transaccion, Registro.ID idRegistro, Set<LSN> undoNextLSNs) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] cantidadLSNs = LogHelper.enteroAByteArray(undoNextLSNs.size());
        evento = LogHelper.concatenar(evento, cantidadLSNs);
        for (LSN undoNextLSN : undoNextLSNs) {
            byte[] lsn = LogHelper.LSNAByteArray(undoNextLSN);
            evento = LogHelper.concatenar(evento, lsn);
        }
        byte[] registro = LogHelper.IdRegistroAByteArray(idRegistro);
        evento = LogHelper.concatenar(evento, registro);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.CLR_INSERT);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | cantidadUndoLSN | {UndoNextLSN} | idRegistro | CantidadColumnas | {nroColumna | ContenidoViejo}
	 */
    public LSN escribirCLRUpdate(Transaccion transaccion, Registro.ID idRegistro, Set<LSN> undoNextLSNs, Collection<Valor> valoresViejos) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] cantidadLSNs = LogHelper.enteroAByteArray(undoNextLSNs.size());
        evento = LogHelper.concatenar(evento, cantidadLSNs);
        for (LSN undoNextLSN : undoNextLSNs) {
            byte[] lsn = LogHelper.LSNAByteArray(undoNextLSN);
            evento = LogHelper.concatenar(evento, lsn);
        }
        byte[] registro = LogHelper.IdRegistroAByteArray(idRegistro);
        evento = LogHelper.concatenar(evento, registro);
        byte[] cantidadColumnas = LogHelper.enteroAByteArray(valoresViejos.size());
        evento = LogHelper.concatenar(evento, cantidadColumnas);
        for (Valor valorViejo : valoresViejos) {
            byte[] nroColumna = LogHelper.enteroAByteArray(valorViejo.posicion());
            Conversor conversor = Conversor.conversorABytes();
            byte[] datoViejo = (byte[]) conversor.convertir(valorViejo.campo(), valorViejo.contenido());
            byte[] columna = LogHelper.concatenar(nroColumna, datoViejo);
            evento = LogHelper.concatenar(evento, columna);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.CLR_UPDATE);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | cantidadUndoLSN | {UndoNextLSN} | idRegistro | CantidadColumnas | {nroColumna | ContenidoViejo}
	 */
    public LSN escribirCLRDelete(Transaccion transaccion, Registro.ID idRegistro, Set<LSN> undoNextLSNs, Collection<Valor> valoresViejos) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] cantidadLSNs = LogHelper.enteroAByteArray(undoNextLSNs.size());
        evento = LogHelper.concatenar(evento, cantidadLSNs);
        for (LSN undoNextLSN : undoNextLSNs) {
            byte[] lsn = LogHelper.LSNAByteArray(undoNextLSN);
            evento = LogHelper.concatenar(evento, lsn);
        }
        byte[] registro = LogHelper.IdRegistroAByteArray(idRegistro);
        evento = LogHelper.concatenar(evento, registro);
        byte[] cantidadColumnas = LogHelper.enteroAByteArray(valoresViejos.size());
        evento = LogHelper.concatenar(evento, cantidadColumnas);
        for (Valor valorViejo : valoresViejos) {
            byte[] nroColumna = LogHelper.enteroAByteArray(valorViejo.posicion());
            Conversor conversor = Conversor.conversorABytes();
            byte[] datoViejo = (byte[]) conversor.convertir(valorViejo.campo(), valorViejo.contenido());
            byte[] columna = LogHelper.concatenar(nroColumna, datoViejo);
            evento = LogHelper.concatenar(evento, columna);
        }
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.CLR_DELETE);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    private synchronized LSN escribir(byte[] datos) {
        try {
            LSN lsn = LSN.nuevoLSN(this.escritor.getChannel().position());
            this.escritor.write(datos);
            return lsn;
        } catch (IOException e) {
            throw new RuntimeException("Error when writing in log!", e);
        }
    }

    public void forzarADisco() {
        try {
            this.escritor.getFD().sync();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN
	 */
    public LSN escribirRollbackTransaccion(Transaccion transaccion) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.ROLLBACK);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * @see servidor.log.Log#leerEvento(servidor.log.LSN)
	 */
    public Evento leerEvento(LSN lsn) {
        try {
            DataInput lector = this.dameEntrada(lsn);
            try {
                byte[] operacionBytes = new byte[Catalogo.LONGITUD_INT];
                lector.readFully(operacionBytes);
                Operacion operacion = LogHelper.byteArrayAOperacion(operacionBytes);
                Evento evento = Evento.dameEvento(operacion);
                if (evento == null) {
                    throw new RuntimeException("Event nonrecognized.");
                }
                evento.leerEvento(lector);
                return evento;
            } finally {
                if (lector instanceof Closeable) {
                    ((Closeable) lector).close();
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DataInput dameEntrada(LSN lsn) throws IOException {
        DataInputStream lector = new DataInputStream(new FileInputStream(LOG_FILENAME));
        lector.skip(lsn.lsn());
        return lector;
    }

    /**
	 * @see servidor.log.Log#cerrar()
	 */
    public synchronized void cerrar() {
        try {
            this.escritor.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @see servidor.log.Log#borrarLog()
	 */
    public boolean borrarLog() {
        return new File(LOG_FILENAME).delete();
    }

    public LSN dameLSNMaestro() {
        try {
            FileInputStream lector = new FileInputStream(MASTER_RECORD_FILENAME);
            try {
                byte[] lsnBytes = new byte[Catalogo.LONGITUD_LONG];
                if (lector.read(lsnBytes) == -1) {
                    return LSN.PRIMER_LSN;
                }
                return LogHelper.byteArrayALSN(lsnBytes);
            } finally {
                lector.close();
            }
        } catch (FileNotFoundException e) {
            return LSN.PRIMER_LSN;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | cantidadUndoLSN | {UndoNextLSN} | idRegistroIndice | idRegistroViejo
	 */
    public LSN escribirCLRDeleteIndex(Transaccion transaccion, ID idRegistroIndice, Set<LSN> undoNextLSNs, servidor.tabla.Registro.ID idRegistroViejo) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] cantidadLSNs = LogHelper.enteroAByteArray(undoNextLSNs.size());
        evento = LogHelper.concatenar(evento, cantidadLSNs);
        for (LSN undoNextLSN : undoNextLSNs) {
            byte[] lsn = LogHelper.LSNAByteArray(undoNextLSN);
            evento = LogHelper.concatenar(evento, lsn);
        }
        byte[] registroIndice = LogHelper.IdRegistroIndiceAByteArray(idRegistroIndice);
        evento = LogHelper.concatenar(evento, registroIndice);
        byte[] registroViejo = LogHelper.IdRegistroAByteArray(idRegistroViejo);
        evento = LogHelper.concatenar(evento, registroViejo);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.CLR_DELETE_INDEX);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | cantidadUndoLSN | {UndoNextLSN} | idRegistroIndice
	 */
    public LSN escribirCLRInsertIndex(Transaccion transaccion, ID idRegistroIndice, Set<LSN> undoNextLSNs) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] cantidadLSNs = LogHelper.enteroAByteArray(undoNextLSNs.size());
        evento = LogHelper.concatenar(evento, cantidadLSNs);
        for (LSN undoNextLSN : undoNextLSNs) {
            byte[] lsn = LogHelper.LSNAByteArray(undoNextLSN);
            evento = LogHelper.concatenar(evento, lsn);
        }
        byte[] registroIndice = LogHelper.IdRegistroIndiceAByteArray(idRegistroIndice);
        evento = LogHelper.concatenar(evento, registroIndice);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.CLR_INSERT_INDEX);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | idRegistroIndice | idRegistro
	 */
    public LSN escribirDeleteIndex(Transaccion transaccion, RegistroIndice registroIndice) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] registroIndiceBs = LogHelper.IdRegistroIndiceAByteArray(registroIndice.id());
        evento = LogHelper.concatenar(evento, registroIndiceBs);
        byte[] registro = LogHelper.IdRegistroAByteArray(registroIndice.registroReferenciado());
        evento = LogHelper.concatenar(evento, registro);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.DELETE_INDEX);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }

    /**
	 * Operacion | Longitud | idTransaccion | prevLSN | idRegistroIndice | idRegistro
	 */
    public LSN escribirInsertIndex(Transaccion transaccion, ID idRegistroIndice, servidor.tabla.Registro.ID idRegistro) {
        byte[] transaccionBytes = LogHelper.IdTransaccionAByteArray(transaccion.id());
        LSN ultimoLSN = transaccion.ultimoLSN();
        byte[] prevLSN = LogHelper.LSNAByteArray(ultimoLSN);
        byte[] evento = LogHelper.concatenar(transaccionBytes, prevLSN);
        byte[] registroIndice = LogHelper.IdRegistroIndiceAByteArray(idRegistroIndice);
        evento = LogHelper.concatenar(evento, registroIndice);
        byte[] registro = LogHelper.IdRegistroAByteArray(idRegistro);
        evento = LogHelper.concatenar(evento, registro);
        byte[] longitud = LogHelper.enteroAByteArray(evento.length);
        evento = LogHelper.concatenar(longitud, evento);
        byte[] operacion = LogHelper.OperacionAByteArray(Operacion.INSERT_INDEX);
        evento = LogHelper.concatenar(operacion, evento);
        return this.escribir(evento);
    }
}
