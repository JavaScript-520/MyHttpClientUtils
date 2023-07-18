package cn.xw.utils.httpTools.constant;

/**
 * HTTP状态码常量类，截至版本1.1。
 *
 * @author Anhui AntLaddie（博客园蚂蚁小哥）
 * @version 1.0
 */
public final class HttpStatus {

    /* 2XX: 通常代表"OK" */
    // HTTP状态码 200：请求成功。
    public static final int HTTP_OK = 200;
    // HTTP状态码 201：已创建。
    public static final int HTTP_CREATED = 201;
    // HTTP状态码 202：已接受。
    public static final int HTTP_ACCEPTED = 202;
    // HTTP状态码 203：非授权信息。
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    // HTTP状态码 204：无内容。
    public static final int HTTP_NO_CONTENT = 204;
    // HTTP状态码 205：重置内容。
    public static final int HTTP_RESET = 205;
    // HTTP状态码 206：部分内容。
    public static final int HTTP_PARTIAL = 206;

    /* 3XX: 重定向 */
    // HTTP状态码 300：多种选择。
    public static final int HTTP_MULTIPLE_CHOICE = 300;
    // HTTP状态码 301：永久移动。
    public static final int HTTP_MOVED_PERM = 301;
    // HTTP状态码 302：临时移动。
    public static final int HTTP_MOVED_TEMP = 302;
    // HTTP状态码 303：查看其他位置。
    public static final int HTTP_SEE_OTHER = 303;
    // HTTP状态码 304：未修改。
    public static final int HTTP_NOT_MODIFIED = 304;
    // HTTP状态码 305：使用代理。
    public static final int HTTP_USE_PROXY = 305;

    /* 4XX: 客户端错误 */
    // HTTP状态码 400：错误请求。
    public static final int HTTP_BAD_REQUEST = 400;
    // HTTP状态码 401：未授权。
    public static final int HTTP_UNAUTHORIZED = 401;
    // HTTP状态码 402：需要付款。
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    // HTTP状态码 403：禁止访问。
    public static final int HTTP_FORBIDDEN = 403;
    // HTTP状态码 404：未找到。
    public static final int HTTP_NOT_FOUND = 404;
    // HTTP状态码 405：方法不允许。
    public static final int HTTP_BAD_METHOD = 405;
    // HTTP状态码 406：不可接受。
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    // HTTP状态码 407：需要代理身份验证。
    public static final int HTTP_PROXY_AUTH = 407;
    // HTTP状态码 408：请求超时。
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    // HTTP状态码 409：冲突。
    public static final int HTTP_CONFLICT = 409;
    // HTTP状态码 410：已删除。
    public static final int HTTP_GONE = 410;
    // HTTP状态码 411：需要有效长度。
    public static final int HTTP_LENGTH_REQUIRED = 411;
    // HTTP状态码 412：未满足前提条件。
    public static final int HTTP_PRECONDITIONS_FAILED = 412;
    // HTTP状态码 413：请求实体过大。
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    // HTTP状态码 414：请求的URI过长。
    public static final int HTTP_REQ_TOO_LONG = 414;
    // HTTP状态码 415：不支持的媒体类型。
    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /* 5XX: 服务器错误 */
    // HTTP状态码 500：内部服务器错误。
    public static final int HTTP_INTERNAL_ERROR = 500;
    // HTTP状态码 501：未实现。
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    // HTTP状态码 502：错误网关。
    public static final int HTTP_BAD_GATEWAY = 502;
    // HTTP状态码 503：服务不可用。
    public static final int HTTP_UNAVAILABLE = 503;
    // HTTP状态码 504：网关超时。
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    // HTTP状态码 505：不支持的HTTP版本。
    public static final int HTTP_VERSION = 505;
}
