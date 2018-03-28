package com.wawa.common.util;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-11-27
 * Time: 下午3:04
 * To change this template use File | Settings | File Templates.
 */

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class TenpayHttpClient
{
    private static final String USER_AGENT_VALUE =
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows XP)";

    private static final String JKS_CA_FILENAME =
            "tenpay_cacert.jks";

    private static final String JKS_CA_ALIAS = "tenpay";

    private static final String JKS_CA_PASSWORD = "";

    /** ca证书文件 */
    private File caFile;

    /** 证书文件 */
    private File certFile;

    /** 证书密码 */
    private String certPasswd;

    /** 请求内容，无论post和get，都用get方式提供 */
    private String reqContent;

    /** 应答内容 */
    private String resContent;

    /** 请求方法 */
    private String method;

    /** 错误信息 */
    private String errInfo;

    /** 超时时间,以秒为单位 */
    private int timeOut;

    /** http应答编码 */
    private int responseCode;

    /** 字符编码 */
    private String charset;

    private InputStream inputStream;

    public TenpayHttpClient() {
        this.caFile = null;
        this.certFile = null;
        this.certPasswd = "";

        this.reqContent = "";
        this.resContent = "";
        this.method = "POST";
        this.errInfo = "";
        this.timeOut = 30;//30秒

        this.responseCode = 0;
        this.charset = "GBK";

        this.inputStream = null;
    }

    public void setCharset(String charset){
        this.charset = charset;
    }

    /**
     * 设置证书信息
     * @param certFile 证书文件
     * @param certPasswd 证书密码
     */
    public void setCertInfo(File certFile, String certPasswd) {
        this.certFile = certFile;
        this.certPasswd = certPasswd;
    }

    /**
     * 设置ca
     * @param caFile
     */
    public void setCaInfo(File caFile) {
        this.caFile = caFile;
    }

    /**
     * 设置请求内容
     * @param reqContent 表求内容
     */
    public void setReqContent(String reqContent) {
        this.reqContent = reqContent;
    }

    /**
     * 获取结果内容
     * @return String
     * @throws java.io.IOException
     */
    public String getResContent() {
        try {
            this.doResponse();
        } catch (IOException e) {
            this.errInfo = e.getMessage();
            //return "";
        }

        return this.resContent;
    }

    /**
     * 设置请求方法post或者get
     * @param method 请求方法post/get
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 获取错误信息
     * @return String
     */
    public String getErrInfo() {
        return this.errInfo;
    }

    /**
     * 设置超时时间,以秒为单位
     * @param timeOut 超时时间,以秒为单位
     */
    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    /**
     * 获取http状态码
     * @return int
     */
    public int getResponseCode() {
        return this.responseCode;
    }

    /**
     * 执行http调用。true:成功 false:失败
     * @return boolean
     */
    public boolean call() {

        boolean isRet = false;

        //http
        if(null == this.caFile && null == this.certFile) {
            try {
                this.callHttp();
                isRet = true;
            } catch (IOException e) {
                this.errInfo = e.getMessage();
            }
            return isRet;
        }

        //https
        try {
            this.callHttps();
            isRet = true;
        } catch (UnrecoverableKeyException e) {
            this.errInfo = e.getMessage();
        } catch (KeyManagementException e) {
            this.errInfo = e.getMessage();
        } catch (CertificateException e) {
            this.errInfo = e.getMessage();
        } catch (KeyStoreException e) {
            this.errInfo = e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            this.errInfo = e.getMessage();
        } catch (IOException e) {
            this.errInfo = e.getMessage();
        }

        return isRet;

    }

    protected void callHttp() throws IOException {

        if("POST".equals(this.method.toUpperCase())) {
            String url = HttpClientUtil.getURL(this.reqContent);
            String queryString = HttpClientUtil.getQueryString(this.reqContent);
            byte[] postData = queryString.getBytes(this.charset);
            this.httpPostMethod(url, postData);

            return ;
        }

        this.httpGetMethod(this.reqContent);

    }

    protected void callHttps() throws IOException, CertificateException,
            KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, KeyManagementException {

        // ca目录
        String caPath = this.caFile.getParent();

        File jksCAFile = new File(caPath + "/"
                + TenpayHttpClient.JKS_CA_FILENAME);
        if (!jksCAFile.isFile()) {
            X509Certificate cert = (X509Certificate) HttpClientUtil
                    .getCertificate(this.caFile);

            FileOutputStream out = new FileOutputStream(jksCAFile);

            // store jks file
            HttpClientUtil.storeCACert(cert, TenpayHttpClient.JKS_CA_ALIAS,
                    TenpayHttpClient.JKS_CA_PASSWORD, out);

            out.close();

        }

        FileInputStream trustStream = new FileInputStream(jksCAFile);
        FileInputStream keyStream = new FileInputStream(this.certFile);

        SSLContext sslContext = HttpClientUtil.getSSLContext(trustStream,
                TenpayHttpClient.JKS_CA_PASSWORD, keyStream, this.certPasswd);

        //关闭流
        keyStream.close();
        trustStream.close();

        if("POST".equals(this.method.toUpperCase())) {
            String url = HttpClientUtil.getURL(this.reqContent);
            String queryString = HttpClientUtil.getQueryString(this.reqContent);
            byte[] postData = queryString.getBytes(this.charset);

            this.httpsPostMethod(url, postData, sslContext);

            return ;
        }

        this.httpsGetMethod(this.reqContent, sslContext);

    }

    /**
     * 以http post方式通信
     * @param url
     * @param postData
     * @throws java.io.IOException
     */
    protected void httpPostMethod(String url, byte[] postData)
            throws IOException {

        HttpURLConnection conn = HttpClientUtil.getHttpURLConnection(url);

        this.doPost(conn, postData);
    }

    /**
     * 以http get方式通信
     *
     * @param url
     * @throws java.io.IOException
     */
    protected void httpGetMethod(String url) throws IOException {

        HttpURLConnection httpConnection =
                HttpClientUtil.getHttpURLConnection(url);

        this.setHttpRequest(httpConnection);

        httpConnection.setRequestMethod("GET");

        this.responseCode = httpConnection.getResponseCode();

        this.inputStream = httpConnection.getInputStream();

    }

    /**
     * 以https get方式通信
     * @param url
     * @param sslContext
     * @throws java.io.IOException
     */
    protected void httpsGetMethod(String url, SSLContext sslContext)
            throws IOException {

        SSLSocketFactory sf = sslContext.getSocketFactory();

        HttpsURLConnection conn = HttpClientUtil.getHttpsURLConnection(url);

        conn.setSSLSocketFactory(sf);

        this.doGet(conn);

    }

    protected void httpsPostMethod(String url, byte[] postData,
                                   SSLContext sslContext) throws IOException {

        SSLSocketFactory sf = sslContext.getSocketFactory();

        HttpsURLConnection conn = HttpClientUtil.getHttpsURLConnection(url);

        conn.setSSLSocketFactory(sf);

        this.doPost(conn, postData);

    }

    /**
     * 设置http请求默认属性
     * @param httpConnection
     */
    protected void setHttpRequest(HttpURLConnection httpConnection) {

        //设置连接超时时间
        httpConnection.setConnectTimeout(this.timeOut * 1000);

        //User-Agent
        httpConnection.setRequestProperty("User-Agent",
                TenpayHttpClient.USER_AGENT_VALUE);

        //不使用缓存
        httpConnection.setUseCaches(false);

        //允许输入输出
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);

    }

    /**
     * 处理应答
     * @throws java.io.IOException
     */
    protected void doResponse() throws IOException {

        if(null == this.inputStream) {
            return;
        }

        //获取应答内容
        this.resContent=HttpClientUtil.InputStreamTOString(this.inputStream,this.charset);

        //关闭输入流
        this.inputStream.close();

    }

    /**
     * post方式处理
     * @param conn
     * @param postData
     * @throws java.io.IOException
     */
    protected void doPost(HttpURLConnection conn, byte[] postData)
            throws IOException {

        // 以post方式通信
        conn.setRequestMethod("POST");
        // 设置请求默认属性
        this.setHttpRequest(conn);

        // Content-Type
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        BufferedOutputStream out = new BufferedOutputStream(conn
                .getOutputStream());

        final int len = 1024; // 1KB
        HttpClientUtil.doOutput(out, postData, len);

        // 关闭流
        out.close();

        // 获取响应返回状态码
        this.responseCode = conn.getResponseCode();

        // 获取应答输入流
        this.inputStream = conn.getInputStream();

    }

    /**
     * get方式处理
     * @param conn
     * @throws java.io.IOException
     */
    protected void doGet(HttpURLConnection conn) throws IOException {

        //以GET方式通信
        conn.setRequestMethod("GET");

        //设置请求默认属性
        this.setHttpRequest(conn);

        //获取响应返回状态码
        this.responseCode = conn.getResponseCode();

        //获取应答输入流
        this.inputStream = conn.getInputStream();
    }


    public static void main(String []args) throws  Exception{
       /* String redirect_uri = URLEncoder.encode("www.2339.com", "utf-8");
        String nvp = "grant_type=authorization_code&client_id=3338801716&client_secret=12bb39fac27508d6b323a2dc547821e5&redirect_uri="+
                redirect_uri +"";
        TenpayHttpClient httpClient = new TenpayHttpClient(); //创建TenpayHttpClient，后台通信
        httpClient.setCharset("UTF-8");
        httpClient.setReqContent("https://api.weibo.com/oauth2/access_token?"+nvp);
        httpClient.setMethod("POST");
        httpClient.call();
        String respString = httpClient.getResContent();
        System.out.println(respString);
        System.out.println(httpClient.getErrInfo());
        System.out.println(httpClient.getResponseCode());
        System.out.println(httpClient.getResContent());*/

        System.out.println(URLEncoder.encode("么么直播-柠檬充值","gb2312"));
        System.out.println(URLDecoder.decode("%C3%B4%C3%B4%D6%B1%B2%A5-%C4%FB%C3%CA%B3%E4%D6%B5","gb2312"));
    }

}
