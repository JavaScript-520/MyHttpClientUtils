package cn.xw.utils.httpTools;

import cn.xw.utils.httpTools.constant.HttpConstant;
import cn.xw.utils.httpTools.entity.HttpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * <h3>原生简单Http请求封装</h3>
 * JDK 11版本前对URLConnection接口子类(HttpURLConnection)的封装（简易小工具，使用此类无需导入其它依赖）<br>
 * <p style="color:#f69;">本工具只支持GET、POST、PUT、DELETE这四种请求模式</p>
 * <p style="color:#f00;">说明：在GET请求下不可包含请求体，这样的话HttpURLConnection对象会强行切换为POST请求</p>
 * <p style="color:#ff0;">若GET方式传参数则拼接到URL地址后方，如：xxx/xxx?name=tom&age=22</p>
 *
 * @author Anhui AntLaddie <a href="https://www.cnblogs.com/antLaddie/">(博客园蚂蚁小哥)</a>
 * @version 1.0
 **/
public class SimpleHttpUtils {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpUtils.class);

    // 连接服务器的超时时间和读取服务器返回的超时时间/ms（设置3分钟）
    private static final Integer CONNECT_TIMEOUT = 1000 * 60 * 3;
    private static final Integer READ_TIMEOUT = 1000 * 60 * 3;

    // 设置请求体（requestBody）发送时的缓冲流大小
    private static final Integer REQUEST_BODY_OUT_BUFFER = 2048;
    // 是否记录请求日志和响应日志
    private static final boolean IS_LOG = false;

    /*
        示例代码：
        String url = "http://192.168.0.100:20130/tMachineryType/list";
        String requestBody = "{\"machineryName\":\",\"type\":\"加工机器\",\"model\":\",\"current\":1,\"size\":5}";
        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Authorization","Bearer <token信息>");
        HttpResult httpResult = sendPost(url, null, reqHeaders, requestBody);
        System.out.println(httpResult);
     */

    /**
     * GET请求发送
     *
     * @param urlStr     标准URL地址
     * @param params     请求参数，拼在URL后 （有参数就传，没有传null）
     * @param reqHeaders 请求头信息（有自定义需要的可传，否则传null）
     * @return 返回Http响应信息（响应码、响应头、响应体）
     */
    public static HttpResult sendGet(String urlStr, Map<String, String> params,
                                     Map<String, String> reqHeaders) {
        return request(urlStr, HttpConstant.HTTP_GET, reqHeaders, params, null);
    }

    /**
     * POST 请求发送
     *
     * @param urlStr     标准URL地址
     * @param params     请求参数，拼在URL后 （有参数就传，没有传null）
     * @param reqHeaders 请求头信息（有自定义需要就传，否则传null）
     * @param reqBody    请求体信息（有请全体就传，没有就null）
     * @return 返回Http响应信息（响应码、响应头、响应体）
     */
    public static HttpResult sendPost(String urlStr, Map<String, String> params,
                                      Map<String, String> reqHeaders, String reqBody) {
        return request(urlStr, HttpConstant.HTTP_POST, reqHeaders, params, reqBody);
    }

    /**
     * PUT 请求发送
     *
     * @param urlStr     标准URL地址
     * @param params     请求参数，拼在URL后 （有参数就传，没有传null）
     * @param reqHeaders 请求头信息（有自定义需要就传，否则传null）
     * @param reqBody    请求体信息（有请全体就传，没有就null）
     * @return 返回Http响应信息（响应码、响应头、响应体）
     */
    public static HttpResult sendPut(String urlStr, Map<String, String> params,
                                     Map<String, String> reqHeaders, String reqBody) {
        return request(urlStr, HttpConstant.HTTP_PUT, reqHeaders, params, reqBody);
    }

    /**
     * DELETE请求发送
     *
     * @param urlStr     标准URL地址
     * @param params     请求参数，拼在URL后 （有参数就传，没有传null）
     * @param reqHeaders 请求头信息（有自定义需要的可传，否则传null）
     * @return 返回Http响应信息（响应码、响应头、响应体）
     */
    public static HttpResult sendDelete(String urlStr, Map<String, String> params,
                                        Map<String, String> reqHeaders) {
        return request(urlStr, HttpConstant.HTTP_DELETE, reqHeaders, params, null);
    }

    /**
     * 基础请求发送，根据传入的参数信息来判断发送的请求方式
     * <p style="color:#f69;">若是GET、DELETE则存在请求信息则提前设置到urlStr后，这种拼接在URL后的，
     * 若传集合数组则：params.put("hobby","抓鱼,爬树,"写字")</p>
     *
     * @param urlStr     请求的URL；注：若是url参数拼接或restful请求，则请求的信息拼接到URL上
     * @param mode       请求的方式，如GET、POST、PUT、DELETE...
     * @param reqHeaders 请求头信息，若没有则空着传null
     * @param params     请求参数，一般在GET、DELETE时传入，最终拼接为：./getFarmer?name=tom&age=22...方式
     * @param reqBody    请求体参数信息
     * @return 返回Http响应信息（响应码、响应头、响应体）
     */
    public static HttpResult request(String urlStr, String mode, Map<String, String> reqHeaders,
                                     Map<String, String> params, String reqBody) {
        // URL请求参数处理（一般在GET、DELETE时传入params值，POST其实也行，但是不符合规范）
        if (params != null && !params.isEmpty()) {
            StringBuilder sb = new StringBuilder("?");
            for (Map.Entry<String, String> param : params.entrySet()) {
                sb.append(param.getKey()).append("=").append(param.getValue()).append("&");
            }
            // 拼接请求参数并对地址进行URL编码
            urlStr = encodeUrl(urlStr + sb);
        }

        // 日志记录
        log.info("HTTP调用：{} 方式开始请求==> 请求URL：{}", mode, urlStr);
        if (IS_LOG) {
            log.info("请求头信息：{}", reqHeaders);
            log.info("请求体信息：{}", reqBody);
        }

        // 初始化Http连接对象
        HttpURLConnection conn = null;
        try {
            // 创建URL对象，指定要发送请求的目标网址
            URL url = new URL(urlStr);
            // 使用URL对象打开一个连接，因为是网络统一资源定位地址，所以打开为HttpURLConnection连接。
            conn = (HttpURLConnection) url.openConnection();

            // 设置请求的一些基本配置信息
            // 设置请求方式
            conn.setRequestMethod(mode);
            // 连接服务器的超时时间和读取服务器返回的超时时间/ms
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            // 设置是否允许通过连接输入数据，就是接收对方传来的数据，若false则无法获取对方响应的数据
            conn.setDoInput(true);
            // 设置请求标头信息
            setRequestHeader(conn, reqHeaders);

            // 当POST、PUT发送请求体信息
            if (HttpConstant.HTTP_PUT.equals(mode) || HttpConstant.HTTP_POST.equals(mode)) {
                // 注：如果你后续通过conn.getOutputStream()获取输出流，向连接中写入数据，则实际上就变成了POST请求。
                // 所以我只对PUT、POST请求设置setDoOutput()，要不然请求GET也会变POST
                if (reqBody != null && !"".equals(reqBody)) {
                    // 设置是否允许通过连接输出数据到对方接口，若false则无法发送数据过去
                    conn.setDoOutput(true);
                    // 向对方服务器写入请求体数据
                    setRequestBody(conn, reqBody);
                }
            }
        } catch (IOException e) {
            log.info("远程接口调用出现异常：{}", e.getMessage());
            throw new RuntimeException(e);
        }
        // 处理接收请求体、请求头信息并返回
        HttpResult httpResult = responseBodyAndHeaders(conn);
        // 日志记录
        log.info("HTTP调用：{} 方式结束请求==> 响应结果：{}", mode, httpResult.getCode());
        if (IS_LOG) {
            log.info("响应头信息：{}", httpResult.getRespHeaders());
            log.info("响应体信息：{}", httpResult.getRespBody());
        }
        return httpResult;
    }

    /***
     * 根据响应的状态信息来处理获取响应体信息和响应头信息
     * @param conn 请求连接对象
     * @return http响应信息
     */
    private static HttpResult responseBodyAndHeaders(HttpURLConnection conn) {
        // 初始化返回信息
        HttpResult httpResult = new HttpResult();

        try {
            // 获取响应码，并设置（注，执行getResponseCode方法才真正调用请求）
            int responseCode = conn.getResponseCode();
            httpResult.setCode(responseCode);

            // 响应体信息处理（成功响应处理）
            if (responseCode >= 200 && responseCode < 300) {
                // 请求成功，读取响应内容
                InputStream inputStream = conn.getInputStream();
                String responseSuccess = parsingInStream(inputStream);
                httpResult.setRespBody(responseSuccess);
            } else {
                // 响应体信息处理（失败响应处理，请求失败，读取错误信息）
                InputStream errorStream = conn.getErrorStream();
                String responseError = parsingInStream(errorStream);
                httpResult.setRespBody(responseError);
            }
        } catch (IOException e) {
            log.info("获取响应体信息出现异常：{}", e.getMessage());
            throw new RuntimeException(e);
        }

        // 响应头信息处理封装
        httpResult.setRespHeaders(conn.getHeaderFields());
        return httpResult;
    }

    /***
     * 设置请求头信息，一般不需要更改，如需要自定义则请设置reqHeaders
     * @param conn       请求连接对象
     * @param reqHeaders 自定义的一些请求头信息，若默认存在则覆盖，自定义可以设置如 Authorization: Bearer <token>
     */
    private static void setRequestHeader(HttpURLConnection conn, Map<String, String> reqHeaders) {
        //  setRequestProperty()方法会覆盖原有的值，
        //  addRequestProperty()方法会将新的值追加到原有的值之后。
        // 设置一些标准的默认标头信息（若requestHeader存在则可以覆盖）
        conn.setRequestProperty("accept", "application/json, text/plain, */*");
        conn.setRequestProperty("accept-language", "zh-CN,zh;q=0.9,en;");
        conn.setRequestProperty("connection", "keep-alive");
        conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("accept-charset", HttpConstant.CHARACTER_SET.name());
        conn.setRequestProperty("content-type", "application/json;charset=" + HttpConstant.CHARACTER_SET.name() + ";");
        // 若reqHeaders不为空则代表有自定义标头信息添加（这里我没复杂考虑一个头信息包含多个值的情况）
        if (reqHeaders != null && !reqHeaders.isEmpty()) {
            for (Map.Entry<String, String> reqHeader : reqHeaders.entrySet()) {
                conn.setRequestProperty(reqHeader.getKey(), reqHeader.getValue());
            }
        }
    }

    /***
     * 设置请求体信息，若请求体信息不为空则进行发送数据到对方的服务器里
     * @param conn    请求连接对象
     * @param reqBody 请求体信息
     */
    private static void setRequestBody(HttpURLConnection conn, String reqBody) {
        try (
                // 写在括号内是因为，结束整个流程后会强行关闭资源
                // 获取输出流，并写出数据（就是往对方服务器发RequestBody数据）
                OutputStream requestOutputStream = conn.getOutputStream();
                // 请求体数据加载到输入流对象中
                InputStream requestInputStream = new ByteArrayInputStream(reqBody.getBytes())
        ) {
            // 若存在请求体信息则进行输出信息
            if (!"".equals(reqBody)) {
                // 缓冲数组，并循环输出信息
                byte[] buffer = new byte[REQUEST_BODY_OUT_BUFFER];
                int bytesRead;
                while ((bytesRead = requestInputStream.read(buffer, 0, buffer.length)) != -1) {
                    requestOutputStream.write(buffer, 0, bytesRead);
                }
                // 刷新资源
                requestOutputStream.flush();
            }
        } catch (IOException e) {
            log.info("请求头信息发送失败，标准的GET、DELETE请求无法设置请求体：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /***
     * 解析流信息（解析成字符串信息）
     * @param inputStream 输入流信息
     * @return 解析后的字符串信息
     */
    private static String parsingInStream(InputStream inputStream) {

        // 初始化返回的结果信息
        StringBuilder sb = null;
        try (// 对流进行转换，转换为缓冲流
             BufferedReader responseBufferedReader =
                     new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            // 记录接收的数据
            sb = new StringBuilder();
            String responseMsg = null;
            while ((responseMsg = responseBufferedReader.readLine()) != null) {
                sb.append(responseMsg);
            }
        } catch (IOException e) {
            log.info("响应体信息接收失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    /***
     * 将带有中文字符的URL转换为标准的URL地址，使用UTF-8编码汉字
     * @param originalUrl 原始的 URL
     * @return 编码后的URL 如：...getFarmer?likeName=%E6%99%93&...
     */
    private static String encodeUrl(String originalUrl) {
        String encodedUrl = null;
        // 将原始 URL 拆分为基础URL和查询参数部分
        String[] parts = originalUrl.split("\\?");
        String baseUrl = parts[0];  // 基础URL部分（不包含查询参数）
        String query = parts[1];    // 查询参数部分
        // 对查询参数部分进行拆分
        String[] queryParams = query.split("&");
        StringBuilder encodedQuery = new StringBuilder();
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            String key = keyValue[0];
            // 低版本JDK8语法
            // String value = null;
            // try {
            //     value = URLEncoder.encode(keyValue[1], HttpConstant.CHARACTER_SET.name());// 使用UTF-8编码
            // } catch (UnsupportedEncodingException e) {
            //     log.info("异常编码问题：{}", e.getMessage());
            //     throw new RuntimeException(e);
            // }
            // 高版本语法
            String value = URLEncoder.encode(keyValue[1], HttpConstant.CHARACTER_SET); // 使用UTF-8编码
            encodedQuery.append(key).append("=").append(value).append("&");
        }
        // 删除最后一个 "&" 符号
        encodedQuery.deleteCharAt(encodedQuery.length() - 1);
        // 拼接编码后的查询参数和基础 URL
        encodedUrl = baseUrl + "?" + encodedQuery;
        return encodedUrl;
    }
}
