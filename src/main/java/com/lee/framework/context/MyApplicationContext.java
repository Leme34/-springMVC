package com.lee.framework.context;

import com.lee.framework.annotation.MyAutowired;
import com.lee.framework.annotation.MyController;
import com.lee.framework.annotation.MyService;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自己的ioc容器
 */
public class MyApplicationContext {

    //ioc容器,spring是用工厂模式实现的，应该使用getBean()获取ioc中的bean对象,此处简化为map.get(beanId)
    private Map<String, Object> instanceMapping = new ConcurrentHashMap<>();

    //配置文件中 需要被扫描注解的包名
    private static final String SCAN_PACKAGE = "scanPackage";

    //缓存扫描出来的所有符合条件的类名
    private List<String> classCache = new ArrayList<>();

    //存放application.properties中的配置
    @Getter
    private Properties config = new Properties();

    public MyApplicationContext(String contextConfigLocation) {
        InputStream is;
        try {
            //1、定位（配置文件）
            is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:",""));
            //2、载入（配置文件）
            config.load(is);

            //3、注册,从配置读取扫描的包 并 把其中所有需要扫描的类的类名保存到classCache中（可理解为登记bean的信息）
            String packageName = config.getProperty(SCAN_PACKAGE);
            doRegister(packageName);

            //4、初始化ioc容器,只要循环实例化需要注入的类的对象 并put到ioc容器中
            doCreateBean();

            //5、依赖注入,为初始化ioc容器时实例化的bean注入属性值
            populate();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 依赖注入
     * 为初始化ioc容器时实例化的bean注入（标注了@MyAutowired的Controller或Service...）的属性的值
     */
    private void populate() {
        // 若ioc容器中没有bean
        if (instanceMapping.isEmpty()) {
            return;
        }
        // 遍历ioc容器, key是beanId, value是bean对象
        instanceMapping.forEach((key, value) -> {
            // 反射bean对象的所有属性
            Field[] fields = value.getClass().getFields();
            // 遍历所有属性
            for (Field field : fields) {
                // 若此属性没有标@MyAutowired注解,则无需注入
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    return;
                }
                // 获取该属性的注解
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                // 获取指定的beanName
                String beanId = myAutowired.value().trim();
                // 若没有指定beanName,则默认使用类名
                if (StringUtils.isBlank(beanId)) {
                    beanId = field.getType().getSimpleName();
                }
                // 开放私有变量的访问权限
                field.setAccessible(true);
                // 为ioc容器中的此bean对象注入此属性
                try {
                    field.set(value, instanceMapping.get(beanId));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 把此package中所有需要扫描的类的类名保存到classCache中(可理解为登记配置文件中<bean>的信息)
     * spring使用BeanDefinition保存类名、注册bean的类型（Map/List/Set/ref/parent）
     * 此处简化为List<String> classCache,只记录类名
     *
     * @param packageName 以"."分隔的包名
     */
    private void doRegister(String packageName) {
        // 此package所在的url = "classpath:" + "/"+ 把包名中的 "." -> "/" 后的文件夹路径
        URL url = this.getClass().getClassLoader().getResource("/" +
                packageName.replace(".", "/"));

        String fileName = url.getFile();  //获取此url对应的文件名
        File dir = new File(fileName);
        //递归扫描此package下的所有文件夹
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doRegister(packageName + "." + file.getName());
            } else {
                // 全类名加入缓存
                classCache.add(packageName + "." +
                        file.getName().replace(".class", "").trim());
            }
        }
    }

    /**
     * 初始化ioc容器,只要循环创建需要注入的类的对象 并put到ioc容器中
     */
    public void doCreateBean() {
        //若没有扫描到需要注册bean的class,直接返回
        if (CollectionUtils.isEmpty(classCache)) {
            return;
        }

        //遍历classCache 对加了不同注解的类进行不同的注册操作
        classCache.forEach(className -> {

            try {
                //反射需要被注册bean的类
                Class<?> clazz = Class.forName(className);
                //只要加了注解（@Service @Controller...)都要初始化

                if (clazz.isAnnotationPresent(MyController.class)) {    //若加了@MyController
                    //beanName为默认为首字母小写的simpleClassName
                    String beanId = lowerFirstChar(clazz.getSimpleName());
                    //put到ioc容器中
                    instanceMapping.put(beanId, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(MyService.class)) {  //若加了@MyService

                    //若设置了beanName则使用其自定义的beanName
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanId = myService.value();
                    if (StringUtils.isNoneBlank(beanId)) {
                        //put到ioc容器中
                        instanceMapping.put(beanId, clazz.newInstance());
                        return;
                    }

                    //否则使用默认规则：
                    //TODO 1、类名首字母小写
                    //2、若此类实现了接口,则用其接口的类名作为id
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        //给每一个接口都注册一个bean到ioc容器中
                        instanceMapping.put(i.getSimpleName(), clazz.newInstance());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });


    }

    public Object getBean(String beanName) {
        return null;
    }

    /**
     * 获取ioc容器
     */
    public Map<String, Object> getAll() {
        return instanceMapping;
    }


    //把str的首字母变为小写
    private String lowerFirstChar(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
