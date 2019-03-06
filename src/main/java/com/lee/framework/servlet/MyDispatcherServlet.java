package com.lee.framework.servlet;

import com.lee.framework.annotation.MyController;
import com.lee.framework.annotation.MyRequestMapping;
import com.lee.framework.annotation.MyRequestParam;
import com.lee.framework.context.MyApplicationContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class MyDispatcherServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MyDispatcherServlet.class);

    //初始化容器的配置文件路径 的<init-param>的参数名
    private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
    //配置文件中 放置html模板页面的目录 的参数名
    private static final String TEMPLATE_ROOT = "templateRoot";

    //    private Map<Pattern, Handler> handlerMapping = new HashMap<>();
    // 存储(支持正则的)urlPattern与handler的映射关系 ,与spring一样的数据结构
    private List<Handler> handlerMapping = new ArrayList<>();

    // 保存处理这个handler的adapter
    private Map<Handler, HandlerAdapter> handlerAdapterMapping = new HashMap<>();

    //存储所有视图的视图名和对应的模板文件
    private List<ViewResolver> viewResolvers = new ArrayList<>();

    /**
     * 初始化自己的ioc容器
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、从web.xml读取初始化ioc容器的配置文件的位置,初始化ioc容器,spring是注入的,此处简化为new
        MyApplicationContext context = new MyApplicationContext(config.getInitParameter(CONTEXT_CONFIG_LOCATION));

        //解析请求 是否复合请求
        initMultipartResolver(context);
        //国际化
        initLocaleResolver(context);
        initThemeResolver(context);

//========================关键============================
        initHandlerMappings(context);  //解析url和handler方法的映射关系
        initHandlerAdapters(context);  //适配器（url与映射关系handlerMapping匹配的过程）
//========================关键============================

        //异常解析
        initHandlerExceptionResolvers(context);
        //根据视图名匹配一个具体视图解析器
        initRequestToViewNameTranslator(context);
        //解析模板中的内容（拿到服务器返回的model生成html代码）
        initViewResolvers(context);
        initFlashMapManager(context);

        logger.info("MySpringMVC初始化成功...");
    }

    /**
     * 在此调用自己写的controller方法
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatcher(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception : " + Arrays.toString(e.getStackTrace()));
        }
    }


    //调用doPost方法
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }


    //解析请求 是否复合请求
    public void initMultipartResolver(MyApplicationContext context) {

    }

    //国际化
    public void initLocaleResolver(MyApplicationContext context) {

    }

    public void initThemeResolver(MyApplicationContext context) {

    }

    //解析url和handler方法的映射关系
    public void initHandlerMappings(MyApplicationContext context) {
        Map<String, Object> ioc = context.getAll();
        // 若容器为空直接返回
        if (ioc.isEmpty()) {
            return;
        }
        // 遍历ioc容器  key:beanId , value:bean
        ioc.forEach((beanId, bean) -> {
            Class<?> clazz = bean.getClass();
            // 若不是Controller则返回
            if (!clazz.isAnnotationPresent(MyController.class)) {
                return;
            }
            //--------是controller--------
            String baseUrl = "";
            // 若标注了@MyRequestMapping
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                //controller级别的url前缀
                baseUrl = clazz.getAnnotation(MyRequestMapping.class).value();
            }

            //遍历每个handler方法,求出对应的映射路径mappingUrl
            for (Method method : clazz.getMethods()) {
                // 若标注了@MyRequestMapping
                if (method.isAnnotationPresent(MyRequestMapping.class)) {
                    String mappingUrl = baseUrl + method.getAnnotation(MyRequestMapping.class).value();
                    // 增加urlMapping正则表达式的支持
                    String regex = mappingUrl.replaceAll("/+", "/");//去除连续(重复)的|"/"
                    Pattern urlPattern = Pattern.compile(regex);  //编译为正则


                    handlerMapping.add(new Handler(bean, method, urlPattern));
                    logger.info("urlMapping: " + regex + " -> " + method.toString());
                }
            }
        });

    }

    //初始化适配器（保存handler的参数信息）
    public void initHandlerAdapters(MyApplicationContext context) {
        if (handlerMapping.isEmpty()) {
            return;
        }
        //记录handler方法的参数信息, key：参数名称  value：方法中参数所在的index
        Map<String, Integer> paramMapping = new HashMap<>();
        //遍历获取每个handler方法上的所有参数
        handlerMapping.forEach(handler -> {
            //1、第一个for循环遍历每个参数的class,处理HttpServletRequest和HttpServletResponse等默认注入的参数
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
//                System.out.println("parameterTypes:" + i + parameterTypes[i]);
                Class<?> parameterType = parameterTypes[i];
                if (parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class) {
                    //记录第index个参数的class
                    paramMapping.put(parameterType.getName(), i);
                }
            }
            //2、第二个for循环记录第index个参数的注解指定的paramName
            Annotation[][] parameterAnnotations = handler.method.getParameterAnnotations();
            //循环每个参数
            for (int i = 0; i < parameterAnnotations.length; i++) {
//                System.out.println("parameterAnnotations:" + i + parameterAnnotations[i]);
                //循当前参数的每个注解
                for (Annotation annotation : parameterAnnotations[i]) {
                    //若是其实例,则转型
                    if (annotation instanceof MyRequestParam) {
                        String paramName = ((MyRequestParam) annotation).value();
                        //记录第index个参数的注解指定的paramName
                        if (StringUtils.isNotBlank(paramName)) {
                            paramMapping.put(paramName, i);
                        }
                    }
                }
            }
            //保存处理这个handler的adapter
            handlerAdapterMapping.put(handler, new HandlerAdapter(paramMapping));
        });


    }

    //异常解析
    public void initHandlerExceptionResolvers(MyApplicationContext context) {

    }

    //根据视图名匹配一个具体视图解析器
    public void initRequestToViewNameTranslator(MyApplicationContext context) {

    }

    //解析模板中的内容（拿到服务器返回的model生成html代码）
    public void initViewResolvers(MyApplicationContext context) {
        //1、读取配置文件中模板的根目录
        String templateRoot = context.getConfig().getProperty(TEMPLATE_ROOT);
        //2、递归读取根目录下的每一个模板文件
        String rootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        File rootDir = new File(rootPath);
        for (File template : rootDir.listFiles()) {
            //保存viewName(此处以模板文件名充当)和模版文件
            viewResolvers.add(new ViewResolver(template.getName(), template));
        }
    }

    public void initFlashMapManager(MyApplicationContext context) {

    }


    /**
     * 处理请求
     */
    public void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            //1、 Determine handler for the current request.
            Handler handler = getHandler(req); //找出处理该请求的handler方法
            if (handler == null) {  //没找到此请求url的handler方法
                resp.getWriter().write("404 Not Found !");
            }
            //2、 Determine handler adapter for the handler.
            HandlerAdapter handlerAdapter = getHandlerAdapter(handler);

            //3、 Actually invoke the handler.
            MyModelAndView modelAndView = handlerAdapter.handle(req, resp, handler);

            //4、解析视图，输出网页
            applyDefaultViewName(resp, modelAndView);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Return the Handler for this request.
     */
    private Handler getHandler(HttpServletRequest req) {

        //若不存在url与handler的映射关系
        if (handlerMapping.isEmpty()) {
            return null;
        }
        //web项目的根路径,一般是工程名，如果工程映射为/，此处返回则为空串
        String contextPath = req.getContextPath();
        //把requestURI (域名端口号后的部分) 去除项目名,把多个"/"替换为1个
        String url = req.getRequestURI().replace(contextPath, "")
                .replaceAll("/+", "/");

        //遍历handlerMapping的urlPattern正则匹配请求的url
        for (Handler handler : handlerMapping) {
            //若匹配则返回对应的handler
            if (handler.urlPattern.matcher(url).matches()) {
                return handler;
            }
        }

        //都不匹配则返回404
        return null;
    }

    /**
     * Return the HandlerAdapter for this handler object.
     *
     * @param handler the handler object to find an adapter for
     */
    private HandlerAdapter getHandlerAdapter(Handler handler) {
        return handlerAdapterMapping.isEmpty() ? null : handlerAdapterMapping.get(handler);
    }


    /**
     * 解析视图，输出网页
     */
    public void applyDefaultViewName(HttpServletResponse resp, MyModelAndView mv) throws Exception {
        if (null == mv) return;
        if (viewResolvers.isEmpty()) return;

        //匹配viewName
        for (ViewResolver viewResolver : viewResolvers) {
            //视图名不匹配
            if (!mv.getView().equals(viewResolver.getViewName())) continue;
            //解析成String并输出
            String result = viewResolver.parse(mv);
            if (null != result) {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().write(result);
                break;
            }
        }
    }


}
