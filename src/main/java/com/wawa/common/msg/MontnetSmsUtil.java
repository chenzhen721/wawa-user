package com.wawa.common.msg;

import com.wawa.common.util.HttpClientUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * 梦网发送验证码接口
 */
public class MontnetSmsUtil {

    static Logger logger = LoggerFactory.getLogger(MontnetSmsUtil.class);
    private static final String userId = "JS2601";
    private static final String password = "102356";
    private static final String api_url = "http://61.145.229.26:8086/MWGate/wmgw.asmx/MongateCsSpSendSmsNew?userId=" + userId + "&password=" + password + "&pszMobis=%s&pszMsg=%s&iMobiCount=1&pszSubPort=*";
    private static final SAXBuilder saxBuilder = new SAXBuilder();

    public static String send(String mobile, String content) {
        String url = String.format(api_url, mobile, content);
        try {
            String result = HttpClientUtils.get(url, null);
            Document doc = saxBuilder.build(new StringReader(result));
            return doc.getContent(0).getValue();
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
        return "0";
    }

    public static void main(String[] args) throws Exception {
        String mobile = "15021031149";
//        String msg = "主人,您有一个新的订单.NICK_NAME要您在CALL_TIME叫醒ta.打开甜心叫醒查看详情-甜心叫醒";
        String msg = "tes,testt";
        System.out.println(send(mobile, msg));
    }
}
