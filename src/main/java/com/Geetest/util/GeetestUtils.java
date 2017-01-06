package com.Geetest.util;

import com.ttpod.rest.common.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Java SDK
 * 
 */
public class GeetestUtils {

    final static Logger logger = LoggerFactory.getLogger(GeetestUtils.class) ;

    //极验证的客户端私钥
    private static final String CAPTCHA_ID= "a896b3c9a8acc36b0387c5db32204367";
    private static final String PRIVATEKEY= "92dbdc70bae22faa60edb5dbe16faefb";
    //pc端私钥
    private static final String PC_CAPTCHA_ID= "d4d90211ba3eef2eed84fdb9185856af";
    private static final String PC_PRIVATEKEY= "7c6cab3f9e93fc5aac944930f349b672";

    private static final GeetestLib appGeetest = new GeetestLib(CAPTCHA_ID, PRIVATEKEY);
    private static final GeetestLib pcGeetest = new GeetestLib(PC_CAPTCHA_ID, PC_PRIVATEKEY);

    public static Boolean OPEN_GEETEST = Boolean.TRUE;

    /**
     * 生成极验证码
     * @return
     */
    public static Map generate_captcha(Boolean isPc){
        Map res = Collections.emptyMap();
        GeetestLib geetest = isPc ? pcGeetest : appGeetest;
        try{
            //进行验证预处理
            int gtServerStatus = geetest.preProcess();

            //将服务器状态正常
            if (gtServerStatus == 1) {
                String resStr = geetest.getResponseStr();
                res = JSONUtil.jsonToMap(resStr);
                OPEN_GEETEST = Boolean.TRUE;
            }else{
                OPEN_GEETEST = Boolean.FALSE;
            }
        }catch (Exception e){
            OPEN_GEETEST = Boolean.FALSE;
            logger.error("getest generate_captcha Exception : {}", e);
        }
        return res;
    }
    /**
     * 二次验证是否正确
     * @param request
     * @return
     */
    public static boolean geetest(HttpServletRequest request){
        try{
            String challenge = request.getParameter(GeetestLib.fn_geetest_challenge);
            String validate = request.getParameter(GeetestLib.fn_geetest_validate);
            String seccode = request.getParameter(GeetestLib.fn_geetest_seccode);
            int gtResult = appGeetest.enhencedValidateRequest(challenge, validate, seccode);
            if (gtResult == 1) {
                // 验证成功
                return Boolean.TRUE;
            }
            gtResult = pcGeetest.enhencedValidateRequest(challenge, validate, seccode);
            if (gtResult == 1) {
                // 验证成功
                return Boolean.TRUE;
            }
        }catch (Exception e){
            logger.error("geetest Exception : {}", e);
            return Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

}
