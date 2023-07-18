package cn.xw.utils.httpTools.constant;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Http的一些基本常量信息
 * @author Anhui AntLaddie（博客园蚂蚁小哥）
 * @version 1.0
 **/
public final class HttpConstant {

    /* HTTP请求方式 */
    // HTTP请求方式（RESTFUL风格：查询）：GET
    public static final String HTTP_GET = "GET";
    // HTTP请求方式（RESTFUL风格：创建）：POST
    public static final String HTTP_POST = "POST";
    // HTTP请求方式（RESTFUL风格：更新）：PUT
    public static final String HTTP_PUT = "PUT";
    // HTTP请求方式（RESTFUL风格：删除）：DELETE
    public static final String HTTP_DELETE = "DELETE";

    // HTTP请求字符集信息
    public static final Charset CHARACTER_SET = StandardCharsets.UTF_8;
}
