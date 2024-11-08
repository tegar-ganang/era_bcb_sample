package pe.com.bn.sach.mantenimiento.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.multiaction.ParameterMethodNameResolver;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import pe.com.bn.mq.service.invoke.ServiceReniec;
import pe.com.bn.mq.service.reniec.Identidad2;
import pe.com.bn.sach.cliente.form.ClienteForm;
import pe.com.bn.sach.desembolso.controller.EjecutarDesembolsoController;
import pe.com.bn.sach.domain.Bnchf10Instruccion;
import pe.com.bn.sach.domain.Bnchf08Ubigeo;
import pe.com.bn.sach.domain.BnhbReniec;
import pe.com.bn.sach.seguridad.DatosSesion;
import pe.com.bn.sach.service.ClienteService;
import pe.com.bn.sach.service.ReniecService;
import pe.com.bn.sach.service.SelectService;
import pe.com.bn.sach.domain.Bnchf02Cliente;

/**
 * @author ce_dpcreditos07
 *
 * TODO Para cambiar la plantilla de este comentario generado, vaya a
 * Ventana - Preferencias - Java - Estilo de c�digo - Plantillas de c�digo
 */
public class ReniecController extends MultiActionController {

    private static Logger logger = Logger.getLogger(EjecutarDesembolsoController.class.getName());

    private String listar;

    private ReniecService servicioreniec;

    private SelectService servicioSelect;

    private ClienteService servicioCliente;

    private String commandName;

    private Class commandClass;

    private static final boolean READ_ONLY = false;

    private static final boolean ALLOW_UPLOAD = true;

    private static final boolean RESTRICT_BROWSING = false;

    private static final boolean RESTRICT_WHITELIST = false;

    private static final String RESTRICT_PATH = "/etc;/var";

    private static final int UPLOAD_MONITOR_REFRESH = 2;

    private static final int EDITFIELD_COLS = 85;

    private static final int EDITFIELD_ROWS = 30;

    private static final boolean USE_POPUP = true;

    /** Logger for this class and subclasses */
    public ReniecController() {
        ParameterMethodNameResolver resolver = new ParameterMethodNameResolver();
        resolver.setParamName("method");
        resolver.setDefaultMethodName("buscarCliente");
        setMethodNameResolver(resolver);
    }

    public void setServicioreniec(ReniecService servicioreniec) {
        this.servicioreniec = servicioreniec;
    }

    public ModelAndView BuscaReniec(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("cliente/cli_ConsultarReniecTitular");
        DatosSesion datosSesion = new DatosSesion();
        datosSesion = (DatosSesion) request.getSession().getAttribute("datosSesion");
        ClienteForm frmCliente = new ClienteForm();
        Bnchf08Ubigeo ubinacimiento = new Bnchf08Ubigeo();
        Bnchf08Ubigeo ubicacion = new Bnchf08Ubigeo();
        mav.addObject("lstsexo", servicioSelect.listItemSexo());
        mav.addObject("lstdni", servicioSelect.listItemDNI());
        mav.addObject("lstestcivil", servicioSelect.listItemEstadoCivil());
        mav.addObject("lst_instruccion", servicioSelect.listInstruccion(new Bnchf10Instruccion()));
        Object command = getCommandObject(request);
        frmCliente = (ClienteForm) command;
        try {
            Bnchf02Cliente bnchf02Cliente = new Bnchf02Cliente();
            bnchf02Cliente = servicioreniec.consultaConsolidada(frmCliente.getTxtNroDocumento(), datosSesion.getNombreUsuario());
            if (bnchf02Cliente.getBnchf01Persona().getF01NumDoc() != null) {
                frmCliente = frmCliente.getClienteForm(frmCliente, bnchf02Cliente);
                frmCliente.setTxtDomicilio(bnchf02Cliente.getDirecDomic());
                mav.addObject(bnchf02Cliente);
                request.setAttribute("frmCliente", frmCliente);
                Bnchf10Instruccion bnchf10Instruccion = new Bnchf10Instruccion();
                List linstruccion = new ArrayList();
                for (int i = 0; i < linstruccion.size(); i++) {
                    bnchf10Instruccion = (Bnchf10Instruccion) linstruccion.get(i);
                    if (bnchf10Instruccion.getF10IdInstruccion() == bnchf02Cliente.getBnchf01Persona().getBnchf10Instruccion().getF10IdInstruccion()) {
                        mav.addObject("idInstruccion", bnchf10Instruccion.getF10IdInstruccion());
                        mav.addObject("descInstruccion", bnchf10Instruccion.getF10DescInstrucc());
                    }
                }
            }
            return mav;
        } catch (Exception e) {
            logger.error(e);
            e.getStackTrace();
            frmCliente = new ClienteForm();
            request.setAttribute("mensaje", "El sistema no se encuentra activo en estos momentos");
            request.setAttribute("frmCliente", frmCliente);
            mav.addObject(new Bnchf02Cliente());
            return mav;
        }
    }

    public ModelAndView cargaConsultaReniec(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("cliente/cli_ConsultarReniecTitularFrame");
        return mav;
    }

    public ModelAndView cargaConsultaBDUCTITULARFRAME(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("cliente/cli_ConsultarReniecTitular");
        return BuscaReniec(request, response);
    }

    public ModelAndView cargaConsultaReniecFamiliar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("cliente/cli_ConsultarReniecFamiliarFrame");
        return mav;
    }

    public ModelAndView cargaConsultaBDUCFAMILIARFRAME(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("cliente/cli_ConsultarReniecFamiliar");
        mav.addObject("lstsexo", servicioSelect.listItemSexo());
        mav.addObject("lstdni", servicioSelect.listItemDNI());
        mav.addObject("lstestcivil", servicioSelect.listItemEstadoCivil());
        return mav;
    }

    public ModelAndView BuscaReniecFamiliar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        DatosSesion datosSesion = new DatosSesion();
        ModelAndView mav = new ModelAndView("cliente/cli_ConsultarReniecFamiliar");
        ClienteForm frmCliente = new ClienteForm();
        Bnchf08Ubigeo ubinacimiento = new Bnchf08Ubigeo();
        Bnchf08Ubigeo ubicacion = new Bnchf08Ubigeo();
        mav.addObject("lstsexo", servicioSelect.listItemSexo());
        mav.addObject("lstdni", servicioSelect.listItemDNI());
        mav.addObject("lstestcivil", servicioSelect.listItemEstadoCivil());
        Object command = getCommandObject(request);
        frmCliente = (ClienteForm) command;
        try {
            Bnchf02Cliente bnchf02Cliente = new Bnchf02Cliente();
            datosSesion = (DatosSesion) request.getSession().getAttribute("datosSesion");
            bnchf02Cliente = servicioreniec.consultaConsolidada(frmCliente.getTxtNroDocumento(), datosSesion.getNombreUsuario());
            if (bnchf02Cliente.getBnchf01Persona().getF01NumDoc() != null) {
                frmCliente = frmCliente.getClienteForm(frmCliente, bnchf02Cliente);
                frmCliente.setTxtDomicilio(bnchf02Cliente.getDirecDomic());
                mav.addObject(bnchf02Cliente);
                request.setAttribute("frmCliente", frmCliente);
                Bnchf10Instruccion bnchf10Instruccion = new Bnchf10Instruccion();
                List linstruccion = new ArrayList();
                linstruccion = servicioSelect.listInstruccion(new Bnchf10Instruccion());
                for (int i = 0; i < linstruccion.size(); i++) {
                    bnchf10Instruccion = (Bnchf10Instruccion) linstruccion.get(i);
                    if (bnchf10Instruccion.getF10IdInstruccion() == bnchf02Cliente.getBnchf01Persona().getBnchf10Instruccion().getF10IdInstruccion()) {
                        mav.addObject("idInstruccion", bnchf10Instruccion.getF10IdInstruccion());
                        mav.addObject("descInstruccion", bnchf10Instruccion.getF10DescInstrucc());
                    }
                }
            }
            return mav;
        } catch (Exception e) {
            logger.error(e);
            frmCliente = new ClienteForm();
            request.setAttribute("mensaje", "El sistema no se encuentra activo en estos momentos");
            request.setAttribute("frmCliente", frmCliente);
            mav.addObject(new Bnchf02Cliente());
            return mav;
        }
    }

    public ModelAndView streamImageContent(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String dni = request.getParameter("dni");
        try {
            try {
                BnhbReniec bnchf29Reniec = new BnhbReniec();
                bnchf29Reniec = servicioCliente.buscarFoto(dni);
                if (bnchf29Reniec == null) {
                    ServiceReniec mqManager = new ServiceReniec();
                    mqManager.consulta3(dni);
                }
                this.servicioreniec.streamImage(dni, response.getOutputStream());
            } catch (Exception ex) {
                logger.error(ex);
                ex.printStackTrace();
                response.setContentType("image/gif");
                getImageDefault("1", response.getOutputStream());
            }
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            response.setContentType("image/gif");
            getImageDefault("1", response.getOutputStream());
        }
        return null;
    }

    public ModelAndView streamImageFirma(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String dni = request.getParameter("dni");
        try {
            try {
                BnhbReniec bnchf29Reniec = new BnhbReniec();
                bnchf29Reniec = servicioCliente.buscarFirma(dni);
                if (bnchf29Reniec == null) {
                    ServiceReniec mqManager = new ServiceReniec();
                    mqManager.consulta4(dni);
                }
                this.servicioreniec.streamImageFirma(dni, response.getOutputStream());
            } catch (Exception ex) {
                logger.error(ex);
                ex.printStackTrace();
                response.setContentType("image/gif");
                getImageDefault("2", response.getOutputStream());
            }
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            response.setContentType("image/gif");
            getImageDefault("2", response.getOutputStream());
        }
        return null;
    }

    public void getImageDefault(String tipoImage, OutputStream out_s) {
        String archivo = null;
        try {
            if (tipoImage.equals("1")) {
                archivo = getServletContext().getRealPath("") + "recursos/imagenes/otros/sinFoto.jpg";
            } else {
                archivo = getServletContext().getRealPath("") + "recursos/imagenes/otros/sinFirma.jpg";
            }
            File f = new File(archivo);
            if (!isAllowed(f, false)) {
                logger.error("No se tiene permisos de acceso a la lectura de archivos" + f.getAbsoluteFile());
            } else if (f.exists() && f.canRead()) {
                BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(f));
                byte buffer[] = new byte[100 * 1024];
                copyStreamsWithoutClose(fileInput, out_s, buffer);
                fileInput.close();
                out_s.flush();
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void copyStreamsWithoutClose(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int b;
        while ((b = in.read(buffer)) != -1) out.write(buffer, 0, b);
    }

    static boolean isAllowed(File path, boolean write) throws IOException {
        if (READ_ONLY && write) return false;
        if (RESTRICT_BROWSING) {
            StringTokenizer stk = new StringTokenizer(RESTRICT_PATH, ";");
            while (stk.hasMoreTokens()) {
                if (path != null && path.getCanonicalPath().startsWith(stk.nextToken())) return RESTRICT_WHITELIST;
            }
            return !RESTRICT_WHITELIST;
        } else return true;
    }

    public Object getCommandObject(HttpServletRequest request) throws Exception {
        Object command = formBackingObject(request);
        bind(request, command);
        return command;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        return commandClass.newInstance();
    }

    public Class getCommandClass() {
        return commandClass;
    }

    public void setCommandClass(Class commandClass) {
        this.commandClass = commandClass;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public void setServicioSelect(SelectService servicioSelect) {
        this.servicioSelect = servicioSelect;
    }

    public void setServicioCliente(ClienteService servicioCliente) {
        this.servicioCliente = servicioCliente;
    }
}
