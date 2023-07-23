package cn.xw.utils.httpTools;

import cn.xw.utils.httpTools.constant.HttpConstant;
import cn.xw.utils.httpTools.entity.HttpResult;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试案例
 *
 * @author Anhui AntLaddie <a href="https://www.cnblogs.com/antLaddie/">(博客园蚂蚁小哥)</a>
 * @version 1.0
 **/
public class TestCasesDemo {

    public static void main(String[] args) throws IOException {
        // 基本数据准备
        // 请求头信息
        Map<String, String> headers = new HashMap<>();
        headers.put("token", "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx");
        // 请求URL参数 评价地址后的参数
        Map<String, String> requestParam = new HashMap<>();
        requestParam.put("address", "安徽六安");
        requestParam.put("name", "李小二");
        // 请求体参数
        String reqBody = "{\"id\":1,\"name\":\"张三\",\"sex\":\"男\",\"age\":20,\"address\":\"北京市\"}";

        // 基本测试方法
        getOne(headers);
        //getOneByStudent(requestParam, reqBody);
        //saveGet(requestParam);
        //savePost(reqBody);
        //saveAjax(reqBody);
        //updateData(reqBody);
        //getPage();
        //uploadFile();
        //uploadFiles();

        // 说明：本工具类可以支持https请求，若服务端需要认证我们客户端时，我们可以设置HttpClientUtils类里的
        //  KEYSTORE_FILE_STR、KEYSTORE_PASSWORD、KEYSTORE_PRI_KEY_PASSWORD参数
        // 示例(只要地址正确，参数按照需求传入)
        //getSSL(headers);
    }

    /**
     * 测试GET请求（无URL参数，无请求体）
     */
    public static void getOne(Map<String, String> headers) {
        // 全局构建HttpClient连接池管理器的每种路由（端口+ip）的最大连接数
        Map<String, Integer> customRouteMap = new HashMap<>();
        // 如：http://192.168.0.1:8888
        // //    请求这类资源时，最多访问10个连接同时访问，第11次访问时得等之前的同路由下的请求归还连接池管理器
        customRouteMap.put("http://192.168.0.1:8888", 10);
        customRouteMap.put("https://192.168.0.2:9999", 20);
        customRouteMap.put("https://www.xiaofeng.com:80", 15);
        HttpClientUtils.customMaxPerRoute(customRouteMap);

        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/getOne")
                .setReqHeaders(headers)
                .setRequestType("GET").build().send();
        System.out.println("响应信息：" + get);
    }

    // 测试GET请求（有URL参数，无请求体，有请求头）
    public static void getOneByStudent(Map<String, String> requestParam, String reqBody) {
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/getOneByStudent")
                .setRequestType("GET")
                .setParams(requestParam)
                .setRequestBody(reqBody).build().send();
        System.out.println("响应信息：" + get);
    }

    // 测试GET请求表单方式（有URL参数，无请求体，无请求头）
    public static void saveGet(Map<String, String> requestParam) {
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/saveGet")
                .setRequestType("GET")
                .setParams(requestParam).build().sendFrom();
        System.out.println("响应信息：" + get);
    }

    // 测试POST请求（无URL参数，有请求体，无请求头）
    public static void savePost(String reqBody) {
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/savePost")
                .setRequestType("POST")
                .setRequestBody(reqBody).build().send();
        System.out.println("响应信息：" + get);
    }

    // 测试POST请求（无URL参数，有请求体，无请求头）
    public static void saveAjax(String reqBody) {
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/saveAjax")
                .setRequestType("POST")
                .setRequestBody(reqBody).build().send();
        System.out.println("响应信息：" + get);
    }

    // 测试PUT请求（无URL参数，有请求体，无请求头）
    public static void updateData(String reqBody) {
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/updateData")
                .setRequestType("PUT")
                .setRequestBody(reqBody).build().send();
        System.out.println("响应信息：" + get);
    }

    // 测试GET请求（有Restful参数，无URL参数，无请求体，无请求头）
    public static void getPage() {
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/getPage/1/10")
                .setRequestType("GET").build().send();
        System.out.println("响应信息：" + get);
    }

    // 测试POST请求上传文件
    public static void uploadFile() throws IOException {
        // 项目跟目录
        URL resource = TestCasesDemo.class.getClassLoader().getResource("");

        //文件信息
        Map<String, InputStream> files = new HashMap<>();
        assert resource != null;
        files.put("风景图.jpg", Files.newInputStream(Paths.get(resource.getFile() + "client/风景图.jpg")));

        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("https://YZ.antladdie.asia:8443/testDemo/uploadFile")
                .setRequestType("POST")
                .setUploadFiles(files).build().sendFile("file");
        System.out.println("响应信息：" + get);
        // 写出图片
        OutputStream outputStream =
                Files.newOutputStream(Paths.get(resource.getFile() + "client/下载" + get.getFileName()));
        byte[] bytes = new byte[2048];
        int index = 0;
        while ((index = get.getFileInputStream().read(bytes)) != -1) {
            outputStream.write(bytes, 0, index);
        }
        outputStream.close();
        get.getFileInputStream().close();
    }

    // 测试POST请求上传多个文件
    public static void uploadFiles() throws IOException {
        // 项目跟目录
        URL resource = TestCasesDemo.class.getClassLoader().getResource("");
        //文件信息
        Map<String, InputStream> files = new HashMap<>();
        assert resource != null;
        files.put("风景图.jpg", Files.newInputStream(Paths.get(resource.getFile() + "client/风景图.jpg")));
        files.put("测试图.jpg", Files.newInputStream(Paths.get(resource.getFile() + "client/测试图.jpg")));
        HttpResult get = new HttpClientUtils.Builder()
                .setUrl("http://localhost:8080/testDemo/uploadFiles")
                .setRequestType("POST")
                .setUploadFiles(files).build().sendFile("files");
        System.out.println("响应信息：" + get);
    }

    /***
     * 测试基本的SSL
     */
    public static void getSSL(Map<String, String> headers) {
        HttpResult send1 = new HttpClientUtils.Builder()
                .setUrl("https://YZ.antladdie.asia:8443/testDemo/getOne")
                .setRequestType(HttpConstant.HTTP_GET)
                .setReqHeaders(headers)
                .build().send();
        System.out.println(send1);
    }
}
