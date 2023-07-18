package cn.xw.utils.httpTools.entity;

import lombok.Data;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Http响应结果信息封装，内部封装响应头、响应体等信息
 *
 * @author Anhui AntLaddie（博客园蚂蚁小哥）
 * @version 1.0
 **/
@Data
public class HttpResult {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应头信息
     */
    private Map<String, List<String>> respHeaders;

    /**
     * 响应体信息（一般服务器交互以JSON方式，JSON字符串自己解析）
     */
    private String respBody;

    /**
     * 下载的文件名称（文件下载时字段不为空）
     */
    private String fileName;

    /**
     * 下载的文件流信息（文件下载时字段不为空）
     */
    private InputStream fileInputStream;
}
