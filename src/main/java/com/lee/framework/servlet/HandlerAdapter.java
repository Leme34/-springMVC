package com.lee.framework.servlet;

import lombok.AllArgsConstructor;
import com.lee.framework.servlet.Handler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

@AllArgsConstructor
public class HandlerAdapter {
    //记录handler方法的参数信息，key：默认注入的参数的类名或自定义参数名称  value：方法中参数所在的index
    //按照index才能注入handler方法的第index个参数的第index个参数值
    private Map<String, Integer> paramMapping;

    /**
     * Use the given handler to handle this request.即url与映射关系handlerMapping匹配的过程
     * 1、根据下标获取到参数的类型
     * 2、把第index个参数转为对应类型
     * 3、根据下标index放入参数数组对应的位置
     * 4、使用转型后的参数反射调用此handler方法
     *
     * @param req     用于获取请求传过来的所有参数
     * @param resp    响应
     * @param handler 用于反射调用controller的这个handler方法(method)
     * @return 返回视图数据和页面
     */
    public MyModelAndView handle(HttpServletRequest req, HttpServletResponse resp, Handler handler) throws Exception {
        //第index个参数应转为的类型
        Class<?>[] parameterTypes = handler.method.getParameterTypes();
        //用于一次性按顺序注入所有参数值的数组
        Object[] temParamsValueArray = new Object[parameterTypes.length];

        //获取所有请求传过来的所有参数 key：参数名，value：此参数的多个值  例:msg=1&msg=2&msg=3
        Map<String, String[]> reqParams = req.getParameterMap();
        //遍历每个请求参数键值对
        reqParams.forEach((reqParam, reqParamsValueArray) -> {
            //把参数的值String[]数组 -> 以","分隔的参数的值字符串  例:[value1, value2, value3] -> value1,value2,value3
            String reqParamsValues = Arrays.toString(reqParamsValueArray)
                    .replaceAll("\\[|\\]", "")  //去除"["或者"]"
                    .replaceAll(",\\s", ",");   //去除","后边的空格
            //若paramMapping中没有匹配到
            if (!this.paramMapping.containsKey(reqParam)) return;
            //-----------paramMapping中匹配到------------------
            Integer index = this.paramMapping.get(reqParam);  //method中参数所在的index
            //把 所有参数值字符串的值 转为method中第index个参数的类型 放入 temParamsValueArray的index个位置
            temParamsValueArray[index] = castStringValue(reqParamsValues, parameterTypes[index]);
        });
        //若参数中有HttpServletRequest和HttpServletResponse参数，则把他们放入执行参数
        String reqClassName = HttpServletRequest.class.getName();
        if (this.paramMapping.containsKey(reqClassName)) {
            Integer reqIndex = this.paramMapping.get(reqClassName);
            temParamsValueArray[reqIndex] = req;
        }
        String respClassName = HttpServletResponse.class.getName();
        if (this.paramMapping.containsKey(respClassName)) {
            Integer respIndex = this.paramMapping.get(respClassName);
            temParamsValueArray[respIndex] = resp;
        }
        //反射调用此handler方法，加上参数(用于一次性按顺序注入所有参数值的数组)
        Object result = handler.method.invoke(handler.controller, temParamsValueArray);
        //若该handler返回视图 MyModelAndView
        if (handler.method.getReturnType() == MyModelAndView.class) {
            return (MyModelAndView) result;
        } else {    //若非返回视图则无需返回数据
            return null;
        }
    }

    /**
     * 把 String类型的value -> clazz类型的值 返回  ,此处只考虑转为Integer和int
     */
    private Object castStringValue(String value, Class<?> clazz) {
        if (clazz == String.class) {   //无需类型转换
            return value;
        } else if (clazz == Integer.class) {  //需要转为Integer
            return Integer.valueOf(value);
        } else if (clazz == int.class) {  //需要转为int
            return Integer.parseInt(value);
        } else return null;                //其他clazz返回null
    }
}