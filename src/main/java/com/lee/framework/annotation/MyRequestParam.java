package com.lee.framework.annotation;

import java.lang.annotation.*;

/**
 * @Target: 注解的作用目标
 * @Retention： 注解的保留位置　
 * @Document： 说明该注解将被包含在javadoc中
 */
@Target(ElementType.PARAMETER)           // 方法参数 上的注解
@Retention(RetentionPolicy.RUNTIME)  // 注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Documented
public @interface MyRequestParam {  //定义一个注解 @MyRequestParam,一个注解是一个类

    String value() default "";

    boolean required() default true;

}
