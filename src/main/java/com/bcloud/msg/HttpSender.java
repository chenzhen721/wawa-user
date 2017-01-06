package com.bcloud.msg;

import java.util.HashMap;
import java.util.Map;

import com.ttpod.rest.common.util.http.HttpClientUtil4_3;
import com.ttpod.user.common.util.HttpClientUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 创蓝发送验证码接口
 */
public class HttpSender{

    static Logger logger = LoggerFactory.getLogger(HttpSender.class);
    private static final String account = "VIP-meme21";
    private static final String pswd = "Txb123456";
    private static final String api_url = "http://222.73.117.156/msg/HttpBatchSendSM"+"?account="+account+"&pswd="+pswd;

    /**
     * 国际短信接口
     */
    private static final String inter_account = "I9154031";
    private static final String inter_pswd = "Psbea402";
    private static final String inter_api_url = "http://222.73.117.140:8044/mt?dc=15&rf=1&tf=3"+"&un="+inter_account+"&pw="+inter_pswd;
    /**
     *
     * @param mobile
     *            手机号码，多个号码使用","分割
     * @param msg
     *            短信内容
     * @param needstatus
     *            是否需要状态报告，需要true，不需要false
     * @return 返回值定义参见HTTP协议文档
     * @throws Exception
     */
    public static String batchSend(String mobile, String msg, boolean needstatus, String extno) throws Exception{
        String resp = "";
        try{
            resp = HttpClientUtil4_3.get(api_url+"&mobile="+mobile+"&needstatus="+needstatus+"&msg="+msg, null, HttpClientUtil4_3.UTF8);
            System.out.println("resp : "+resp);
            if(StringUtils.isNotEmpty(resp)){
                String[] resps = StringUtils.split(resp, ",");
                if(resps[1].startsWith("0")){
                    return "0";
                }

            }
        }catch (Exception e){
            throw  e;
        }
        return resp;
    }

    /**
     * 国际验证码
     * @param mobile
     * @param msg
     * @return
     * @throws Exception
     */
    public static String batchSendInter(String mobile, String msg) throws Exception{
        String resp = "";
        try{
            resp = HttpClientUtil4_3.get(inter_api_url+"&da="+mobile+"&sm="+msg, null, HttpClientUtil4_3.UTF8);
            System.out.println("resp : "+resp);
            if(StringUtils.contains(resp, "id=")){
                return "0";
            }
        }catch (Exception e){
            throw  e;
        }
        return resp;
    }

    public static void main (String[] args)throws Exception{
        //HttpSender.batchSend("15021031149", "【么么直播】正在进行手机验证操作，验证码：123456。请勿将验证码泄露给他人。", Boolean.TRUE, null);
        System.out.println(HttpSender.batchSendInter("11231031149", "国际-【么么直播】正在进行手机验证操作，验证码：123456。请勿将验证码泄露给他人。"));
    }
}