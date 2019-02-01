package com.lee.demo;

import com.lee.framework.annotation.MyController;
import com.lee.framework.annotation.MyRequestMapping;
import com.lee.framework.annotation.MyRequestParam;
import com.lee.framework.annotation.MyResponseBody;
import com.lee.framework.servlet.MyModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@MyController("testController")
public class TestController {

    //TODO  @MyResponseBody未实现, 扫描到该注解直接使用response设置contentType输出即可
    @MyRequestMapping("/test1/.*.json")    // 正则urlPattern匹配"/test1/*.json"
    public void test1(HttpServletRequest request, HttpServletResponse response,
                     @MyRequestParam(value = "msg", required = false) String msg) throws IOException {
        msg = (msg == null) ? "" : msg;
        response.getWriter().write(msg);
    }

    @MyRequestMapping("/testModelAndView.json")
    public MyModelAndView testModelAndView(HttpServletRequest request, HttpServletResponse response,
                                 @MyRequestParam(value = "name", required = false) String name,
                                 @MyRequestParam(value = "description", required = false) String description) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("name",name);
        model.put("description",description);
        return new MyModelAndView("test.myml",model);
    }

}
