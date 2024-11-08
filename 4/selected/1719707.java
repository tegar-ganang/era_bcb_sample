package org.rishi.framework;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.rishi.framework.annotation.HttpMethodType;
import org.rishi.framework.annotation.JndiResource;
import org.rishi.framework.annotation.Restful;
import org.rishi.framework.ioc.IocContextManager;
import org.rishi.framework.ioc.JRestModule;
import org.rishi.framework.ioc.JndiProvider;
import org.rishi.framework.ioc.JndiServiceInfo;
import org.rishi.framework.ioc.ModelMap;
import org.rishi.framework.ioc.RestServiceExecutor;
import org.rishi.framework.util.ClassPathScanner;
import org.rishi.framework.util.ClassPathScanner.ClassFilter;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

@SuppressWarnings("unchecked")
public class RestFilter implements Filter {

    public static final String METHOD_OF_GET = "get";

    public static final String METHOD_OF_POST = "post";

    public static final String METHOD_OF_PUT = "put";

    public static final String METHOD_OF_DELETE = "delete";

    private ResourceRegistry resourceReg = new ResourceRegistry();

    public static Injector injector;

    @Override
    public void doFilter(ServletRequest servletReqest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletReqest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        request.setCharacterEncoding("UTF-8");
        ModelMap params = new ModelMap<String, String>();
        String uri = request.getRequestURI();
        uri = uri.replace(request.getContextPath(), "");
        String _uri = uri.trim().toLowerCase();
        if (_uri.endsWith(".js") || _uri.endsWith(".css") || _uri.endsWith(".jpg") || _uri.endsWith(".png") || _uri.endsWith(".gif") || _uri.endsWith(".flash")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (uri == null || "".equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }
        IocContextManager.setContext(request, response, params);
        try {
            Object service = resourceReg.getResource(request, response, uri, params);
            if (service == null) filterChain.doFilter(servletReqest, servletResponse); else {
                fillParameters(request, params);
                String method = request.getMethod();
                if (METHOD_OF_GET.equalsIgnoreCase(method)) writeResult(response, RestServiceExecutor.execute(service, HttpMethodType.GET)); else if (METHOD_OF_POST.equalsIgnoreCase(method)) writeResult(response, RestServiceExecutor.execute(service, HttpMethodType.POST)); else if (METHOD_OF_PUT.equalsIgnoreCase(method)) writeResult(response, RestServiceExecutor.execute(service, HttpMethodType.PUT)); else if (METHOD_OF_DELETE.equalsIgnoreCase(method)) writeResult(response, RestServiceExecutor.execute(service, HttpMethodType.DELETE)); else filterChain.doFilter(servletReqest, servletResponse);
            }
        } catch (Exception e) {
        } finally {
            IocContextManager.clearContext();
        }
    }

    /**
	 * 填充参数
	 * 
	 * @modelMap request
	 * @modelMap params
	 */
    private void fillParameters(HttpServletRequest request, ModelMap params) {
        Enumeration names = request.getAttributeNames();
        String name;
        while (names.hasMoreElements()) {
            name = names.nextElement().toString();
            params.put(name, request.getAttribute(name));
        }
        names = request.getParameterNames();
        while (names.hasMoreElements()) {
            name = names.nextElement().toString();
            params.put(name, request.getParameter(name));
        }
        try {
            String charset = request.getCharacterEncoding();
            if (charset == null) charset = "UTF-8";
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream(), charset));
            CharArrayWriter data = new CharArrayWriter();
            char buf[] = new char[4096];
            int ret;
            while ((ret = in.read(buf, 0, 4096)) != -1) data.write(buf, 0, ret);
            String content = URLDecoder.decode(data.toString().trim(), charset);
            if (content != "") {
                String[] param_pairs = content.split("&");
                String[] kv;
                for (String p : param_pairs) {
                    kv = p.split("=");
                    if (kv.length > 1) params.put(kv[0], kv[1]);
                }
            }
            data.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        String resListFile = config.getInitParameter("resourceListFile");
        if ("".equals(resListFile)) {
            System.out.println("[WARNING]No resource specified");
            return;
        }
        List<Class<?>> resources = new ArrayList<Class<?>>(0);
        String resource_package = config.getInitParameter("resource-package");
        if (resource_package != null && !resource_package.trim().equals("")) {
            String[] packages = resource_package.split(",");
            for (String packageName : packages) this.scanResource(resources, packageName);
        }
        String guiceModuleClass = config.getInitParameter("GuiceModuleClass");
        final List<Module> modules = new ArrayList<Module>(0);
        modules.add(new JRestModule());
        modules.add(this.generateGuiceProviderModule(resources));
        try {
            if (guiceModuleClass != null && !guiceModuleClass.trim().equals("")) {
                modules.add((Module) Class.forName(guiceModuleClass).newInstance());
            }
        } catch (Exception e) {
            throw new ServletException("初始化RestFilter错误：\n" + e.getMessage());
        }
        injector = Guice.createInjector(new Iterable<Module>() {

            @Override
            public Iterator<Module> iterator() {
                return modules.iterator();
            }
        });
        this.registResource(resources);
    }

    private void scanResource(List<Class<?>> resources, String packageName) {
        List<Class<?>> list = new ClassPathScanner(packageName, new ClassFilter() {

            public boolean accept(Class<?> clazz) {
                return clazz.isAnnotationPresent(Restful.class);
            }
        }).scan();
        resources.addAll(list);
    }

    /**
	 * 生成Guice的Provider模块
	 * 
	 * @modelMap resources
	 * @return
	 */
    private Module generateGuiceProviderModule(List<Class<?>> resources) {
        final Set<JndiServiceInfo> jndiServiceInfos = new HashSet<JndiServiceInfo>(0);
        JndiResource annotation;
        for (Class<?> clazz : resources) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                annotation = field.getAnnotation(JndiResource.class);
                if (annotation != null) {
                    jndiServiceInfos.add(new JndiServiceInfo(field.getType(), annotation.jndi()));
                }
            }
        }
        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                Class serviceClass;
                for (JndiServiceInfo info : jndiServiceInfos) {
                    serviceClass = info.getServiceClass();
                    binder.bind(serviceClass).toProvider(JndiProvider.fromJndi(serviceClass, info.getJndiName()));
                }
            }
        };
        return module;
    }

    private void registResource(List<Class<?>> resources) {
        for (Class<?> clazz : resources) {
            String[] uri = clazz.getAnnotation(Restful.class).uri();
            for (String s : uri) {
                resourceReg.registerResource(s, clazz);
            }
        }
    }

    private void writeResult(HttpServletResponse response, Object result) {
        if (result == null) return;
        try {
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
    }
}
