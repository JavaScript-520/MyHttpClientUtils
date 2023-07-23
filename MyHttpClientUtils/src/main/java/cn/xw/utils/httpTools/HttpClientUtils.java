package cn.xw.utils.httpTools;

import cn.xw.utils.httpTools.constant.HttpConstant;
import cn.xw.utils.httpTools.entity.HttpResult;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 一个基于Apache HttpClient-5.2.1版本封装的简易工具，方便操作<br/>
 * <p style="color:#f61">基本功能：发送GET、POST、PUT、DELETE四大请求</p>
 * <p style="color:#f61">特殊功能：文件上传下载、SSL请求发送和双向认证</p>
 *
 * @author Anhui AntLaddie <a href="https://www.cnblogs.com/antLaddie/">(博客园蚂蚁小哥)</a>
 * @version 1.0
 **/
public class HttpClientUtils {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpUtils.class);

    // 连接池管理器的一些配置常量
    // 设置请求建立时长（建立连接的最长等待时间）注：在连接池管理器内设置（优先级低与requestConfig）
    private static final Long REQUEST_CONNECT_TIMEOUT = 3L;
    // 设置本次请求响应总时长（从被建立连接到响应数据返回的这段时间）注：在连接池管理器内设置（优先级低与requestConfig）
    private static final Long SOCKET_TIMEOUT = 20L;
    // 设置连接和响应时长单位（默认：秒）
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    // 单次请求配置的一些RequestConfig常量值（优先级比连接池管理器高）
    // 每个发送请求时从连接池管理器获取连接时的超时时间 单位/秒
    private static final Long CONNECTION_POOL_MANAGER_TIMEOUT = 3L;
    // 响应超时时间 单位/秒
    private static final Long RESPONSE_TIMEOUT = 10L;

    // 构建HttpClient连接池管理器
    private static PoolingHttpClientConnectionManager connectionManager = null;
    // 初始化一个基本可关闭的HttpClient
    // 说明：若需要自定义创建CloseableHttpClient对象则调用：
    //      createCloseableHttpClient和自定义customCreateCloseableHttpClient两个方法
    private static CloseableHttpClient closeableHttpClient = null;

    // 其它配置
    // 可以文件下载的类型
    private static final List<String> FILE_CONTENT_TYPE = Arrays.asList("image/jpeg", "image/png", "image/gif",
            "audio/mpeg", "audio/wav", "video/mp4", "video/mpeg", "application/pdf", "application/msword");

    // 关于SSL配置信息（手动修改这里的配置）
    // 说明：如果对方服务器不校验咱们客户端信息，我们无需设置我们自己的密钥库信息
    // 密钥库库信息（内包含证书、密钥），主要用来提取密钥库内的证书，建立连接后发送给对方服务器，校验安全（可以做SSL双向认证）
    // 类路径下：资源放在resources下的file目录里则可以写成：./file/certificate.p12
    private static final String KEYSTORE_FILE_STR = "client/certificate.p12";
    // 密钥库密码
    private static final String KEYSTORE_PASSWORD = "$$ED12^a*8(&E6^PR";
    // 密钥库内私钥密码
    private static final String KEYSTORE_PRI_KEY_PASSWORD = "h3J^*a#12@Klp$%z";


    // 关于Http请求发送的一些基本参数设置（通过Builder来构建）
    private final String requestUrl;                    //  请求URL（必设置）
    private final String requestType;                   //  请求类型（必设置）
    private final Map<String, String> params;           //  请求参数
    private final Map<String, String> reqHeaders;       //  请求头参数
    private final String requestBody;                   //  请求体参数（常用于POST、PUT）
    private RequestConfig requestConfig;                //  每次请求体配置如：超时时间、是否可重定向、重定向次数等
    private final Map<String, InputStream> uploadFiles; //  文件上传流信息（只有上传文件时使用）key文件名 value文件流

    static {
        // 初始化HttpClient连接池管理器
        createPoolingHttpClientConnectionManager(null);
    }

    /**
     * 提供给Builder方式的构造器
     *
     * @param builder 构建对象信息
     */
    private HttpClientUtils(Builder builder) {
        this.requestUrl = builder.requestUrl;
        this.requestType = builder.requestType;
        this.params = builder.params;
        this.reqHeaders = builder.reqHeaders;
        this.requestBody = builder.requestBody;
        this.requestConfig = builder.requestConfig;
        this.uploadFiles = builder.uploadFiles;
    }

    /**
     * 创建HttpClient连接池管理器，其实参数默认传null即可，不推荐传socketFactoryRegistry
     * <p style="color:#f61">没有特殊需求不建议重写socketFactoryRegistry注册表，若想重写则参考SSL方法</p>
     *
     * @param socketFactoryRegistry 连接注册表信息
     */
    public static void createPoolingHttpClientConnectionManager(Registry<ConnectionSocketFactory>
                                                                        socketFactoryRegistry) {
        // 若注册表信息不为空则设置到连接池管理器中（一般可以指定https、ftp、smtp等）
        if (socketFactoryRegistry != null) {
            // 构建带有传入的注册表的HttpClient连接池管理器
            connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } else {
            // 构建默认HttpClient连接池管理器及注册表信息
            connectionManager = createDefaultSocketFactoryRegistry();
        }

        // 设置连接池的最大连接数
        connectionManager.setMaxTotal(200);
        // 默认每个路由的最大连接数
        // 说明：ip+端口为一个路由，同一个路由最多可以并发10次相同时间访问，额外请调用customMaxPerRoute()方法
        connectionManager.setDefaultMaxPerRoute(10);
        // 设置创建连接池的基本连接配置（设置连接时长和本次请求响应总时长）
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(REQUEST_CONNECT_TIMEOUT, TIME_UNIT)
                .setSocketTimeout(SOCKET_TIMEOUT.intValue(), TIME_UNIT).build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
    }

    /**
     * 最重要的私有方法，若需要自定义配置https、http、ftp、socks、smtp、imap、pop3等策略的，参考下面的方法编写
     *
     * @return 连接池管理器
     */
    private static PoolingHttpClientConnectionManager createDefaultSocketFactoryRegistry() {
        // =========================== 配置自定义 https 策略 =========================== //
        // 配置SSL的证书信任策略及双向认证策略
        SSLContext sslContext = null;
        try {
            // 初始化SSL上下文生成器
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            // 自定义证书信任策略（可以在这里自定义实现）
            TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> {
                // 在Lambda表达式中定义你的信任策略逻辑
                // 在多个证书中，返回true表示信任该证书，返回false表示不信任该证书
                // 可以根据自己的需求编写逻辑，例如只信任特定的证书颁发机构、特定的主体等
                // 示例：根据传递的值，信任特定的证书颁发机
                // 可信任的发布者公司信息（其实这个就是域名信息）
                //  List<String> companyList = Arrays.asList("antLaddieCompany", "yang");
                //  boolean flag = false;   // 假定不信任
                //  for (X509Certificate certificate : chain) {
                //      // 解析证书请参考：https://gitee.com/ant-laddie/hard-working-ant.git JWTDemo工具
                //      CertificateMessage certificateMessage = BaseParsing.certificateParsing(certificate, "");
                //      String company = certificateMessage.getIssuer().getCompany();
                //      flag = companyList.contains(company);
                //  }
                //  return flag;
                return true;
            };
            // 设置客户端信任服务器端证书的逻辑（当前这里算客户端调用服务端）
            sslContextBuilder.loadTrustMaterial(trustStrategy);
            // 密钥库不为空构建
            if (KEYSTORE_FILE_STR != null && !"".equals(KEYSTORE_FILE_STR)) {
                URL url = HttpClientUtils.class.getClassLoader().getResource(KEYSTORE_FILE_STR);
                if (url != null) {
                    // 设置客户端的私钥和相关证书信息，和服务端建立SSL连接时发送客户端证书（可以保证服务端和客户端双向认证）
                    sslContextBuilder.loadKeyMaterial(url, KEYSTORE_PASSWORD.toCharArray(),
                            KEYSTORE_PRI_KEY_PASSWORD.toCharArray());
                } else {
                    log.warn("客户端密钥库信息为空，可能无法实现双向SSL/TLS认证，请检查地址是否正确！");
                }
            }
            sslContext = sslContextBuilder.build();
        } catch (NoSuchAlgorithmException e) {
            log.warn("当请求的加密算法在当前环境中不可用时抛出此异常：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            log.warn("操作密钥库时发生问题：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            log.warn("密钥无法从密钥库中恢复常：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            log.warn("证书相关发生异常，可能证书格式错误、证书过期等：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.warn("输入输出操作异常：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            log.warn("当密钥管理异常：{}", e.getMessage());
            throw new RuntimeException(e);
        }
        // 构建SSL连接套接字工厂生成器
        SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder =
                SSLConnectionSocketFactoryBuilder.create();
        // 通过工厂Builder构建SSL连接套接字工厂，并设置SSL上下文
        SSLConnectionSocketFactory sslConnectionSocketFactory = sslConnectionSocketFactoryBuilder
                .setSslContext(sslContext).build();

        // =========================== 配置自定义 http 策略 =========================== //
        PlainConnectionSocketFactory plainConnectionSocketFactory = new PlainConnectionSocketFactory() {
            @Override
            public Socket createSocket(HttpContext context) {
                try {
                    // 空实现
                    Socket socket = super.createSocket(context);
                    //socket.setSoTimeout(5000);
                    return socket;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // 创建Registry对象，并注册自定义的SSLConnectionSocketFactory
        // 说明：这里我只展开"https"和”http“介绍，其实还有如下可选类型配置到注册表（部分）
        //      "http"： 表示使用 HTTP 协议进行普通连接。
        //      "ftp"：  表示使用 FTP 协议进行文件传输连接。
        //      "socks"：表示使用 SOCKS 协议进行代理连接。
        //      "smtp"： 表示使用 SMTP 协议进行邮件发送连接。
        //      "imap"： 表示使用 IMAP 协议进行邮件接收连接。
        //      "pop3"： 表示使用 POP3 协议进行邮件接收连接。
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", sslConnectionSocketFactory)
                        .register("http", plainConnectionSocketFactory)
                        .build();
        createPoolingHttpClientConnectionManager(socketFactoryRegistry);
        return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    }

    /**
     * 自定义HttpClient连接池管理器的每种路由（端口+ip）的最大连接数，若不设置默认为10
     *
     * @param customRouteMap 自定义路由连接数 如：key（路由） value（最大连接数）
     */
    public static void customMaxPerRoute(Map<String, Integer> customRouteMap) {
        // 示例数据
        // customRouteMap.put("http://192.168.0.1:8888",10);
        // customRouteMap.put("https://192.168.0.2:9999",20);
        // customRouteMap.put("https://www.xiaofeng.com:80",15);
        if (customRouteMap == null || customRouteMap.isEmpty()) {
            log.warn("传入自定义路由最大连接数据为空！");
        } else {
            for (Map.Entry<String, Integer> customRoute : customRouteMap.entrySet()) {
                URI uri = URI.create(customRoute.getKey());
                HttpRoute route = new HttpRoute(new HttpHost(uri.getHost(), uri.getPort()));
                connectionManager.setMaxPerRoute(route, customRoute.getValue());
            }
        }
    }

    /**
     * 创建一个基本可关闭的HttpClient，并设置基本配置
     *
     * @param httpRequestInterceptor  请求开始拦截器（若传入则使用，若不传入则使用默认）
     * @param httpResponseInterceptor 响应开始拦截器（若传入则使用，若不传入则使用默认）
     */
    public static void createCloseableHttpClient(HttpRequestInterceptor httpRequestInterceptor,
                                                 HttpResponseInterceptor httpResponseInterceptor) {
        // 校验请求开始拦截器若为null则执行默认拦截器，若不想要拦截器则设置空实现
        if (httpRequestInterceptor == null) {
            httpRequestInterceptor = (httpRequest, entityDetails, httpContext) -> {
                // 说明：请求拦截
                // httpRequest：它包含了即将发送的HTTP请求的所有信息，例如请求方法、URL、头部信息、请求体等
                // entityDetails：请求体的实体细节；包含了请求体的相关信息，如内容长度、类型等，有请求体时才会有信息，否则为null
                // httpContext：请求的上下文对象，提供了各种有关请求和响应的上下文信息，例如连接、Cookie、认证信息，URL等
                // 如：
                // 请求URL：httpRequest.getUri();
                // 请求类型：httpRequest.getMethod();
                // 获取请求URL参数：httpRequest.getUri().getQuery();（xxx=yyy&aa=bb）可自己解析
                // 获取所有请求头：httpRequest.getHeaders();
                // 获取请求体参数：一般POST或PUT会传请求体信息
                //     InputStream content = ((StringEntity) entityDetails).getContent();
                //     StringBuilder sb = new StringBuilder();
                //     try (BufferedReader reader = new BufferedReader(new InputStreamReader(content))) {
                //         String line;
                //         while ((line = reader.readLine()) != null) {
                //             sb.append(line).append("\n");
                //         }
                //     }
                // 日志打印
                log.info("请求类型：{} ===> 请求URL：{}", httpRequest.getMethod(),
                        httpRequest.getScheme() + ":" + httpRequest.getAuthority() + httpRequest.getPath());
                HttpEntity entity = ((BasicClassicHttpRequest) httpRequest).getEntity();
                if (entity != null && entity.getContentType().contains("application/json")) {
                    try {
                        log.info("请求体信息：{}", EntityUtils.toString(entity, StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        log.info("请求体日志打印失败：{}", e.getMessage());
                    }
                }
            };
        }
        // 校验响应开始拦截器若为null则执行默认拦截器，若不想要拦截器则设置空实现
        if (httpResponseInterceptor == null) {
            httpResponseInterceptor = (httpResponse, entityDetails, httpContext) -> {
                // 说明：响应拦截，和上面一样
                log.info("响应码：{}", httpResponse.getCode());
            };
        }

        // 创建 HttpClient 对象
        closeableHttpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .addRequestInterceptorFirst(httpRequestInterceptor)
                .addResponseInterceptorFirst(httpResponseInterceptor)
                .build();
    }

    /**
     * 自定义构建一个基本可关闭的HttpClient
     *
     * @param httpClientBuilder 自定义HttpClient构造（无需设置HttpClient连接池管理器，方法内部会设置）
     */
    public static void customCreateCloseableHttpClient(HttpClientBuilder httpClientBuilder) {
        // 通过传入的HttpClientBuilder生成HttpClient并设置静态全局CloseableHttp变量
        closeableHttpClient = httpClientBuilder.setConnectionManager(connectionManager).build();
    }

    /**
     * 构建请求的URL地址信息和设置请求参数拼接到URL地址后
     *
     * @return URI地址（就是请求地址）
     * @throws NullPointerException 空指针异常（URL地址为空）
     * @throws URISyntaxException   URI语法异常（生成URI的时候）
     */
    private URI createRequestUrl() throws URISyntaxException {
        // 校验请求地址
        if (requestUrl == null || "".equals(requestUrl)) {
            throw new NullPointerException("请求URL不可为空！");
        }
        // 构建URL生成对象
        URIBuilder uriBuilder = new URIBuilder(requestUrl);
        // 若请求参数不为null则添加请求参数，如?aa=xx&bb=xx
        if (params != null && !params.isEmpty()) {
            // 添加地址参数
            for (Map.Entry<String, String> param : params.entrySet()) {
                uriBuilder.addParameter(param.getKey(), param.getValue());
            }
        }
        return uriBuilder.build();
    }

    /**
     * 设置自定义的请求配置，每个请求都有自己的配置，若设置null或不调用方法则有默认的配置<br/>
     * 注：设置这个requestConfig超时时间是高于连接池管理器的超时时间
     *
     * @param httpRequest Http请求信息，其子类是HttpGet、HttpPost、HttpPut、HttpDelete
     */
    private void setRequestConfig(ClassicHttpRequest httpRequest) {
        // 校验是否请求设置自定义配置，若没有则初始化一个
        if (requestConfig == null) {
            // 设置单次请求的配置信息
            requestConfig = RequestConfig.custom()
                    // 连接管理器获取连接的超时时间
                    .setConnectionRequestTimeout(CONNECTION_POOL_MANAGER_TIMEOUT, TIME_UNIT)
                    .setResponseTimeout(RESPONSE_TIMEOUT, TIME_UNIT)    // 响应超时时间
                    .setContentCompressionEnabled(true)                 // 是否设置请求内容压缩
                    .setRedirectsEnabled(true)                          // 是否允许重定向
                    .setMaxRedirects(3)                                 // 最大重定向次数
                    .setCircularRedirectsAllowed(true)                  // 是否允许循环重定向
                    .build();
        }
        // 设置配置信息
        if (httpRequest instanceof HttpGet) {
            ((HttpGet) httpRequest).setConfig(requestConfig);
        } else if (httpRequest instanceof HttpPost) {
            ((HttpPost) httpRequest).setConfig(requestConfig);
        } else if (httpRequest instanceof HttpPut) {
            ((HttpPut) httpRequest).setConfig(requestConfig);
        } else {
            ((HttpDelete) httpRequest).setConfig(requestConfig);
        }
    }

    /**
     * 前置请求方法，用来封装请求路径和设置请求头以及请求配置
     *
     * @return ClassicHttpRequest Http请求对象
     */
    private ClassicHttpRequest beforeSending() {
        try {
            // 校验请求类型
            if (requestType == null || "".equals(requestType)) {
                throw new NullPointerException("请求类型不可为空！");
            }
            // 获取地址URL并根据地址构建请求对象
            URI uri = createRequestUrl();
            // JDK 14+语法
            // ClassicHttpRequest httpRequest = switch (requestType.toUpperCase()) {
            //     case HttpConstant.HTTP_GET -> new HttpGet(uri);
            //     case HttpConstant.HTTP_POST -> new HttpPost(uri);
            //     case HttpConstant.HTTP_PUT -> new HttpPut(uri);
            //     case HttpConstant.HTTP_DELETE -> new HttpDelete(uri);
            //     default -> throw new RuntimeException("请求类型无法识别！");
            // };
            ClassicHttpRequest httpRequest = null;
            switch (requestType.toUpperCase()) {
                case HttpConstant.HTTP_GET:
                    httpRequest = new HttpGet(uri);
                    break;
                case HttpConstant.HTTP_POST:
                    httpRequest = new HttpPost(uri);
                    break;
                case HttpConstant.HTTP_PUT:
                    httpRequest = new HttpPut(uri);
                    break;
                case HttpConstant.HTTP_DELETE:
                    httpRequest = new HttpDelete(uri);
                    break;
                default:
                    throw new RuntimeException("请求类型无法识别！");
            }
            // 设置请求基本配置，如响应超时...
            setRequestConfig(httpRequest);
            // 添加常规请求头信息
            httpRequest.addHeader(new BasicHeader("Accept", "application/json, text/plain, */*"));
            httpRequest.addHeader(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.9,en;"));
            httpRequest.addHeader(new BasicHeader("Connection", "keep-alive"));
            httpRequest.addHeader(new BasicHeader("User-Agent",
                    "Apache-HttpClient/5.2.1 (Java/1.8.0_161)"));
            // 若存在额外请求头则也添加
            if (reqHeaders != null && !reqHeaders.isEmpty()) {
                for (Map.Entry<String, String> reqHeader : reqHeaders.entrySet()) {
                    httpRequest.addHeader(new BasicHeader(reqHeader.getKey(), reqHeader.getValue()));
                }
            }
            return httpRequest;
        } catch (URISyntaxException e) {
            log.warn("http请求地址异常：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /***
     * 发送常用请求（JSON类型请求体）
     * @return HttpResult响应数据（响应码、响应头、响应体）
     */
    public HttpResult send() {
        // 调用请求前置操作获取Http请求对象
        ClassicHttpRequest httpRequest = beforeSending();
        // 添加请求体信息
        if (requestBody != null && !"".equals(requestBody)) {
            // 要发什么样的数据通过修改 ContentType.APPLICATION_JSON
            httpRequest.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        }
        return sendToHttpResult(httpRequest);
    }

    /***
     * 发送请求（表单方式提交）
     * @return HttpResult响应数据（响应码、响应头、响应体）
     */
    public HttpResult sendFrom() {
        // 调用请求前置操作获取Http请求对象
        ClassicHttpRequest httpRequest = beforeSending();
        // 添加请求体信息
        if (requestBody != null && !"".equals(requestBody)) {
            httpRequest.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED));
        }
        return sendToHttpResult(httpRequest);
    }

    /**
     * 发送请求（单个或批量文件上传）<br/>
     * 注：服务端接收单个文件可以通过这个方法获取：xxx(@RequestParam("file") MultipartFile file)<br/>
     * 注：服务端接收多个文件可以通过这个方法获取：xxx(@RequestParam("files") MultipartFile [] files)
     *
     * @param paramName 上传文件时对应对方服务端接收参数的名称，一一对应
     * @return HttpResult响应数据（响应码、响应头、响应体）
     */
    public HttpResult sendFile(String paramName) {
        // 调用请求前置操作获取Http请求对象
        ClassicHttpRequest httpRequest = beforeSending();
        // 构建多部分实体对象
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        // 使用严格的MIME多部分实体格式
        builder.setMode(HttpMultipartMode.STRICT);
        // 文件不为空则构建文件信息
        if (uploadFiles != null && !uploadFiles.isEmpty()) {
            for (Map.Entry<String, InputStream> file : uploadFiles.entrySet()) {
                // 文件名称编码，防止中文文件名称乱码
                String fileName = null;
                try {
                    fileName = URLEncoder.encode(file.getKey(), StandardCharsets.UTF_8.name());
                    // 服务器那边需要对文件名称解码：
                    // URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    log.info("文件名称编码异常，不支持此类编码格式！");
                }
                builder.addBinaryBody(paramName, file.getValue(), ContentType.DEFAULT_BINARY, fileName);
            }
        } else {
            throw new RuntimeException("上传文件不可为空！");
        }
        // 设置请求体
        HttpEntity multipart = builder.build();
        httpRequest.setEntity(multipart);
        return sendToHttpResult(httpRequest);
    }

    /**
     * 发送请求，并获取结果信息封装到HttpResult返回
     *
     * @param httpRequest Http请求对象；其子类是HttpGet、HttpPost、HttpPut、HttpDelete
     * @return HttpResult响应数据（响应码、响应头、响应体，注：下载文件时存在：文件名、文件流）
     */
    private HttpResult sendToHttpResult(ClassicHttpRequest httpRequest) {
        try {
            // 判断当前的Http的客户端对象是否存在，不存在则默认创建
            if (closeableHttpClient == null) {
                createCloseableHttpClient(null, null);
            }
            // 发送请求并封装数据到HttpResult对象
            return closeableHttpClient.execute(httpRequest, classicHttpResponse -> {
                // 初始化自定义的请求返回对象，并设置Code、响应头、响应体
                HttpResult httpResult = new HttpResult();
                httpResult.setCode(classicHttpResponse.getCode());
                // 响应头信息处理（获取到响应头信息，并封装到HttpResult自定义返回对象中）
                Map<String, List<String>> respHeaders = new HashMap<>();
                httpResult.setRespHeaders(respHeaders);
                for (Header header : classicHttpResponse.getHeaders()) {
                    if (respHeaders.containsKey(header.getName())) {
                        respHeaders.get(header.getName().toLowerCase()).add(header.getValue());
                    } else {
                        ArrayList<String> headerValue = new ArrayList<>();
                        headerValue.add(header.getValue());
                        respHeaders.put(header.getName().toLowerCase(), headerValue);
                    }
                }

                // 校验请求头，判断是否为文件类型，文件类型封装fileName、fileInputStream属性
                List<String> types = respHeaders.get("content-type");
                if (types == null || types.isEmpty()) {
                    throw new RuntimeException("未知的响应类型，无法解析。");
                }
                String contentType = types.get(0);
                if (FILE_CONTENT_TYPE.contains(contentType)) {
                    // 获取 Content-Disposition 头部字段的值
                    List<String> values = respHeaders.get("content-disposition");
                    if (values == null || values.isEmpty()) {
                        // 未读取到文件名称
                        httpResult.setFileName(UUID.randomUUID() + "." + contentType.split("/")[1]);
                    } else {
                        String headerMsg = values.get(0);
                        if (headerMsg != null && !headerMsg.isEmpty()) {
                            String fileName = headerMsg.substring(headerMsg.indexOf(";filename=") + 10);
                            // 解码（因为传输中文文件名，服务端可能会编码）
                            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
                            if (fileName.isEmpty()) {
                                // 文件名称为空则自定义
                                fileName = UUID.randomUUID() + "." + contentType.split("/")[1];
                            }
                            // 若文件名为 如：".jpg"则需要添加前缀
                            if (fileName.contains(".") && "".equals(fileName.split("\\.")[0])) {
                                fileName = UUID.randomUUID() + fileName;
                            }
                            // 若文件名为 如 "aaa"则添加后缀
                            if (!fileName.contains(".")) {
                                fileName = fileName + "." + contentType.split("/")[1];
                            }
                            httpResult.setFileName(fileName);
                        }
                    }
                    // 因为当前请求一旦结束则关闭全部流，包含请求来的文件，既然我需要放到HttpResult对象，则需要深拷贝
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = classicHttpResponse.getEntity().getContent().read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    byte[] data = outputStream.toByteArray();
                    // 设置文件流信息
                    httpResult.setFileInputStream(new ByteArrayInputStream(data));
                } else {
                    // 否则将响应信息转换为字符串放在respBody属性内
                    httpResult.setRespBody(EntityUtils.toString(classicHttpResponse.getEntity(), StandardCharsets.UTF_8));
                    log.info("响应数据：{}", httpResult.getRespBody());
                }
                return httpResult;
            });
        } catch (IOException e) {
            log.warn("响应IO流操作失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 一个基本的Builder代码，通过Builder来构建对象的属性
     */
    public static class Builder {
        private String requestUrl;              //  Builder请求URL
        private String requestType;             //  Builder请求类型
        private Map<String, String> params;     //  Builder请求参数
        private Map<String, String> reqHeaders; //  Builder请求头参数
        private String requestBody;             //  Builder请求体参数（常用于POST、PUT的文本方式）
        private RequestConfig requestConfig;    //  Builder每次请求体配置如：超时时间、是否可重定向、重定向次数等
        private Map<String, InputStream> uploadFiles; // Builder文件上传流信息（只有上传文件时使用）key文件名 value文件流


        public Builder setUrl(String requestUrl) {
            this.requestUrl = requestUrl;
            return this;
        }

        public Builder setRequestType(String requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder setParams(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder setReqHeaders(Map<String, String> reqHeaders) {
            this.reqHeaders = reqHeaders;
            return this;
        }

        public Builder setRequestBody(String requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder setRequestConfig(RequestConfig requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        public Builder setUploadFiles(Map<String, InputStream> uploadFiles) {
            this.uploadFiles = uploadFiles;
            return this;
        }

        public HttpClientUtils build() {
            return new HttpClientUtils(this);
        }
    }
}

