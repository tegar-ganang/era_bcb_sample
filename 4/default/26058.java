import java.util.Locale;
import org.mentawai.action.LogoutAction;
import org.mentawai.authorization.AuthorizationManager;
import org.mentawai.authorization.Group;
import org.mentawai.core.ActionConfig;
import org.mentawai.core.Context;
import org.mentawai.core.Forward;
import org.mentawai.core.Redirect;
import org.mentawai.core.StreamConsequence;
import org.mentawai.filter.AuthenticationFilter;
import org.mentawai.filter.AuthorizationFilter;
import org.mentawai.filter.RedirectAfterLoginFilter;
import org.mentawai.i18n.LocaleManager;
import br.com.sse.actions.EscolherPacienteAction;
import br.com.sse.actions.EscolherProdutoAction;
import br.com.sse.actions.EstoqueAtualAction;
import br.com.sse.actions.MovimentoAction;
import br.com.sse.actions.PacienteAction;
import br.com.sse.actions.ProdutoAction;
import br.com.sse.actions.RelatorioHistoricoAction;
import br.com.sse.actions.RelatorioPacienteProdutoAction;
import br.com.sse.actions.RemoverMovimentoAction;
import br.com.sse.actions.TipoPacienteAction;
import br.com.sse.actions.UsuarioAction;
import br.com.sse.authentication.LoginAction;
import br.com.sse.filters.ConversionFilterMovimento;
import br.com.sse.filters.ConversionFilterPaciente;
import br.com.sse.filters.ConversionFilterProduto;
import br.com.sse.validation.CadastroPacienteValidator;
import br.com.sse.validation.CadastroTipoPacienteValidator;
import br.com.sse.validation.MovimentoValidationFilter;
import br.com.sse.validation.ProdutoValidationFilter;

public class ApplicationManager extends org.mentawai.core.ApplicationManager {

    public void loadActions() {
        addGlobalFilter(new AuthenticationFilter());
        addGlobalConsequence(AuthenticationFilter.LOGIN, new Redirect("login.jsp"));
        addGlobalConsequence(AuthorizationFilter.ACCESSDENIED, new Redirect("acessoNegado.jsp"));
        ActionConfig ac = new ActionConfig("pages/Login", LoginAction.class);
        ac.addConsequence(LoginAction.SUCCESS, new Redirect("sucesso.jsp"));
        ac.addConsequence(LoginAction.ERROR, new Forward("/pages/error.jsp"));
        addActionConfig(ac);
        ac.addFilter(new RedirectAfterLoginFilter());
        ac.addConsequence(RedirectAfterLoginFilter.REDIR, new Redirect());
        ac = new ActionConfig("/Logout", LogoutAction.class);
        ac.addConsequence(LogoutAction.SUCCESS, new Redirect("/index.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/Usuario", UsuarioAction.class);
        ac.addConsequence(UsuarioAction.SUCCESS, new Forward("/pages/cadastroUsuario.jsp"));
        ac.addConsequence(UsuarioAction.ERROR, new Forward("/pages/error.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/Produto", ProdutoAction.class);
        ac.addConsequence(ProdutoAction.SUCCESS, new Redirect("/pages/cadastroProduto.jsp"));
        ac.addConsequence(ProdutoAction.ERROR, new Forward("/pages/cadastroProduto.jsp"));
        addActionConfig(ac);
        ac.addFilter(new ProdutoValidationFilter());
        ac.addFilter(new ConversionFilterProduto());
        ac = new ActionConfig("pages/Paciente", PacienteAction.class);
        ac.addConsequence(PacienteAction.SUCCESS, new Forward("/pages/cadastroPaciente.jsp"));
        ac.addConsequence(PacienteAction.ERROR, new Forward("/pages/cadastroPaciente.jsp"));
        addActionConfig(ac);
        ac.addFilter(new CadastroPacienteValidator());
        ac.addFilter(new ConversionFilterPaciente());
        ac = new ActionConfig("pages/TipoPaciente", TipoPacienteAction.class);
        ac.addConsequence(TipoPacienteAction.SUCCESS, new Forward("/pages/cadastroTipoPaciente.jsp"));
        ac.addConsequence(TipoPacienteAction.ERROR, new Forward("/pages/cadastroTipoPaciente.jsp"));
        addActionConfig(ac);
        ac.addFilter(new CadastroTipoPacienteValidator());
        ac = new ActionConfig("pages/Movimento", MovimentoAction.class);
        ac.addConsequence(MovimentoAction.SUCCESS, new Redirect("/pages/cadastroMovimento.jsp"));
        ac.addConsequence(MovimentoAction.ERROR, new Forward("/pages/cadastroMovimento.jsp"));
        addActionConfig(ac);
        ac.addFilter(new MovimentoValidationFilter());
        ac.addFilter(new ConversionFilterMovimento());
        ac = new ActionConfig("pages/EscolherProduto", EscolherProdutoAction.class);
        ac.addConsequence(EscolherProdutoAction.SUCCESS, new Forward("pages/cadastroProduto.jsp"));
        ac.addConsequence(EscolherProdutoAction.ERROR, new Forward("pages/escolherProduto.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/EscolherPaciente", EscolherPacienteAction.class);
        ac.addConsequence(EscolherPacienteAction.SUCCESS, new Forward("pages/cadastroPaciente.jsp"));
        ac.addConsequence(EscolherPacienteAction.ERROR, new Forward("pages/escolherPaciente.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/Historico", RelatorioHistoricoAction.class);
        ac.addConsequence(RelatorioHistoricoAction.SUCCESS, new StreamConsequence("application/pdf"));
        ac.addConsequence(RelatorioHistoricoAction.ERROR, new Forward("pages/verificarHistorico.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/EstoqueAtual", EstoqueAtualAction.class);
        ac.addConsequence(EstoqueAtualAction.SUCCESS, new StreamConsequence("application/pdf"));
        ac.addConsequence(EstoqueAtualAction.ERROR, new Forward("pages/estoqueAtual.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/RelatorioProdutoPaciente", RelatorioPacienteProdutoAction.class);
        ac.addConsequence(RelatorioPacienteProdutoAction.SUCCESS, new StreamConsequence("application/pdf"));
        ac.addConsequence(RelatorioPacienteProdutoAction.ERROR, new Forward("pages/relatorioPacienteProduto.jsp"));
        addActionConfig(ac);
        ac = new ActionConfig("pages/RemoverMovimento", RemoverMovimentoAction.class);
        ac.addConsequence(RemoverMovimentoAction.SUCCESS, new Forward("pages/verificarHistorico.jsp"));
        ac.addConsequence(RemoverMovimentoAction.ERROR, new Forward("pages/verificarHistorico.jsp"));
        addActionConfig(ac);
    }

    public void loadLocales() {
        LocaleManager.add(new Locale("pt", "BR"));
    }

    public void init(Context application) {
        Group admins = new Group("admins");
        admins.addPermission("read").addPermission("write").addPermission("delete");
        AuthorizationManager.addGroup(admins);
        Group users = new Group("users");
        users.addPermission("read");
        AuthorizationManager.addGroup(users);
    }
}
