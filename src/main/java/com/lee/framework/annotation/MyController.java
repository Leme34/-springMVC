package com.lee.framework.annotation;

import java.lang.annotation.*;

/**
 * @Target: 注解的作用目标
 * @Retention： 注解的保留位置　
 * @Document： 说明该注解将被包含在javadoc中
 */
@Target(ElementType.TYPE)            //接口、类、枚举、注解  上的注解
@Retention(RetentionPolicy.RUNTIME)  // 注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Documented
public @interface MyController {   //定义一个注解 @MyController,一个注解是一个类

    String value() default "";

}
