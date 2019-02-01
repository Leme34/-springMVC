package com.lee.framework.servlet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class MyModelAndView {

    /** View instance or view name String. */
    private String view;

    /** Model Map. 携带到页面的model数据 */
    private Map<String,Object> model;

    public MyModelAndView(String view) {
        this.view = view;
    }
}
