package com.lee.framework.servlet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视图解析器，spring中为了支持多种模板引擎写的是接口，此处已简化
 */
@Getter
@AllArgsConstructor
public class ViewResolver {
    private String viewName;  //视图名
    private File file;        //模版文件

    /**
     * 解析视图，输出网页
     */
    protected String parse(MyModelAndView modelAndView) throws Exception {
        StringBuffer sb = new StringBuffer();
        //只读文件
        try (RandomAccessFile ra = new RandomAccessFile(this.file, "r")) {
            String line = null;
            // 读入模板的每一行
            while (null != (line = ra.readLine())) {
                //匹配此行中的myml模板语法，此处为@{}
                Matcher matcher = this.myMatcher(line);
                //部分匹配，查找输入串中与模式匹配的子串
                while (matcher.find()) {
                    //groupCount()返回子模式匹配的结果数,包括模式@{paramName}本身编号为0，然后小括号()包装的子模式从左到右的匹配结果从1开始计数
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        //每一组的值就是"@{}"中的参数名
                        String paramName = matcher.group(i);
                        //取出后台传给前端的model中这个参数的值
                        Object paramValue = modelAndView.getModel().get(paramName);
                        if (null == paramValue) continue;
                        //若参数值不为null，则模板中的所有这个"@{paramName}"替换为参数值
                        line = line.replaceAll("@\\{" + paramName + "\\}", paramValue.toString());
                    }
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }

    //返回匹配自定义模板语法的正则匹配器，此处自定义模板".myml"文件语法为"@{}"
    private Matcher myMatcher(String str) {
        // 模式中用()来表示捕获组，并且根据圆括号从左到右来编号。一个给定的正则表达式完整部分编号为0，然后()从左到右分别从1开始计数。我们可以得到任意捕获组的内容
        // 在量词后面加上一个问号就是非贪婪模式:一旦匹配成功不再向右尝试
        // (.+?)表示分组匹配"@{}"模式的字符串,"@{}"中的字符可以一个或多个
        Pattern pattern = Pattern.compile("@\\{(.+?)\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(str);
        return matcher;
    }

}