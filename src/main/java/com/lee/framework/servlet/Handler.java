package com.lee.framework.servlet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

@AllArgsConstructor
@Getter
public class Handler {
    // ioc容器中controller的bean对象
    protected Object controller;
    // 该controller的handler方法对象
    protected Method method;
    // 存储此handler对应的urlPattern
    protected Pattern urlPattern;
}