package com.ttpod.user.web

import cn.b2m.eucp.utils.SingletonClient
import com.Geetest.util.GeetestUtils
import com.bcloud.msg.HttpSender
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.doc.ParamKey
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.common.util.http.HttpClientUtil4_3
import com.ttpod.user.common.util.AuthCode
import com.ttpod.user.common.util.KeyUtils
import com.ttpod.user.common.util.TenpayHttpClient
import com.ttpod.user.common.util.XMLUtil
import com.ttpod.user.model.Code
import com.ttpod.user.model.SmsCode
import com.ttpod.user.web.BaseController
import com.ttpod.user.web.api.Web
import groovy.xml.XmlUtil
import org.apache.commons.codec.Encoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.lang.StringUtils
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.charset.Charset
import java.rmi.RemoteException
import java.security.Key
import java.util.concurrent.TimeUnit
import static com.ttpod.rest.common.util.WebUtils.*
import static com.ttpod.rest.common.doc.MongoKey.*;

/**
 * @author: jiao.li@ttpod.com
 * Date: 14-6-16 下午1:39
 */
@Rest
class AuthCodeController extends BaseController {

    Logger logger = LoggerFactory.getLogger(AuthCodeController.class)

    def fetch(HttpServletRequest req) {
        //String key = AuthCode.random(8 + ((int) System.currentTimeMillis() & 1)).toLowerCase()
        String key = System.currentTimeMillis() + AuthCode.random(4);
        String url = API_DOMAIN + "authcode/image?key=" + key
        String clientId = Web.getClientId(req);
        String request_key = req['key']
        //logger.info(req.getServletPath()+"request key : {} ip : {}   ", request_key, clientId )
        [code: 1, data: [auth_key: key, auth_url: url]]
    }


    def image(HttpServletRequest req, HttpServletResponse response) {
        String code = AuthCode.random(4 + ((int) System.currentTimeMillis() & 1))
        String request_key = req['key']
        def key = KeyUtils.AUTHCODE.register(request_key)
        String clientId = Web.getClientId(req);
        //logger.info("AuthCode image ip : {} request key : {}  code : {}", clientId, request_key, code)
        if(mainRedis.opsForValue().setIfAbsent(key, code)){
            mainRedis.expire(key, 2*60L, TimeUnit.SECONDS)
            //注册消耗时间
            /*def cost_time_key = "register:costtime:${code}".toString()
            mainRedis.opsForValue().set(cost_time_key, System.currentTimeMillis().toString(), 2*60L, TimeUnit.SECONDS)*/
            response.addHeader('Content-Type', "image/png")
            AuthCode.draw(code, 160, 48, response.getOutputStream())
        }
    }

    /**
     * 获取验证码(极验证 or 么么验证码)
     * @param req
     * @return
     */
    def geettest(HttpServletRequest req){
        Boolean isPc = ServletRequestUtils.getBooleanParameter(req, 'pc', Boolean.FALSE)
        Map resutl = GeetestUtils.generate_captcha(isPc)
        resutl.putAll([code : 1, data: [is_open : GeetestUtils.OPEN_GEETEST]]);
        //resutl.putAll([code : 1, data: [is_open : Boolean.FALSE]]);
        return resutl
    }

    /**
     * 验证码是否正确
     * @param req
     * @return
     */
    def validate_authcode(HttpServletRequest req){
        return [code : 1, data: [validate: Web.codeVeri(req)]] ;
    }

    private static final Long SEND_MOBILE_LIMIT = 60
    private static final Long SEND_MOBILE_EXPIRES= 30 * 60L
    private static final String SMS_SEND_CONTENT="正在进行手机验证操作，验证码：{code}。请勿将验证码泄露给他人。"
    private static final String SMS_SEND_CONTENT_FIND="正在进行找回密码操作，验证码：{code}。请勿将验证码泄露给他人。"
    private static final String SMS_SEND_CONTENT_EXCHANGE="正在进行兑换柠檬操作，验证码：{code}。请勿将验证码泄露给他人。"

    private static final String INTER_SMS_SEND_CONTENT="国际-正在进行手机验证操作，验证码：{code}。请勿将验证码泄露给他人。"
    private static final String INTER_SMS_SEND_CONTENT_FIND="国际-正在进行找回密码操作，验证码：{code}。请勿将验证码泄露给他人。"
    private static final String INTER_SMS_SEND_CONTENT_EXCHANGE="国际-正在进行兑换柠檬操作，验证码：{code}。请勿将验证码泄露给他人。"

    def send_mobile(HttpServletRequest req) {
        def mobile = req['mobile']
        Integer type = ServletRequestUtils.getIntParameter(req, "type", SmsCode.注册.ordinal())
        Boolean china = ServletRequestUtils.getBooleanParameter(req, "china", Boolean.TRUE)
        if(StringUtils.isEmpty(mobile)){
            [code : Code.参数无效]
        }
        //国内手机才验证
        if(china && (!VALID_MOBILE.matcher(mobile).matches())){
            return [code: Code.手机号格式错误]
        }
        def key = KeyUtils.AUTHCODE.registerSms(mobile)
        String content = china ? SMS_SEND_CONTENT : INTER_SMS_SEND_CONTENT
        if(type == SmsCode.找回密码.ordinal()){
            key = KeyUtils.AUTHCODE.pwdSms(mobile)
            content = china ? SMS_SEND_CONTENT_FIND : INTER_SMS_SEND_CONTENT_FIND
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            key = KeyUtils.AUTHCODE.exchangeSms(mobile)
            content = china ? SMS_SEND_CONTENT_EXCHANGE : INTER_SMS_SEND_CONTENT_EXCHANGE
        }
        //时间间隔60秒
        if(mainRedis.getExpire(key) > 0
                && (SEND_MOBILE_EXPIRES - mainRedis.getExpire(key)) <= SEND_MOBILE_LIMIT){
            return [code : Code.短信验证间隔太短]
        }
        //手机号的限制 如：一个手机号码一天只能发5条
        if(Web.smsSendMobileLimited(mobile))
            return [code : Code.短信验证码每日次数超过限制]
        //IP限制，如：一个IP一天只发20条
        if(Web.smsSendIpLimited(req))
            return [code : Code.短信验证码每日次数超过限制]
        //手机号发送验证码10次未注册用户
        if(!Web.smsSendMobileWithoutReg(mobile))
            return [code : Code.短信验证码每日次数超过限制]

        if(!sendMobile(req, key, mobile, content, china)){
            [code: Code.ERROR]
        }
        [code: Code.OK]
    }


    /**
     * 发送验证码
     * @param req
     * @param mobile
     * @param type
     * @return
     */
    public Boolean sendMobile(HttpServletRequest req, String mobile, Integer type){
        def key = KeyUtils.AUTHCODE.registerSms(mobile)
        String content = SMS_SEND_CONTENT
        if(type == SmsCode.找回密码.ordinal()){
            key = KeyUtils.AUTHCODE.pwdSms(mobile)
            content = SMS_SEND_CONTENT_FIND
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            key = KeyUtils.AUTHCODE.exchangeSms(mobile)
            content = SMS_SEND_CONTENT_EXCHANGE
        }
        return sendMobile(req, key, mobile, content)
    }

    /**
     * 2015/5/11 启动亿美软通支持
     * 2016/7/7 停用
     */
 /*   public Boolean old_sendMobile(HttpServletRequest req, String key, String mobile, String content){
        def code = AuthCode.randomNumber(6)
        mainRedis.opsForValue().set(key, code, SEND_MOBILE_EXPIRES, TimeUnit.SECONDS)

        //发送手机验证码
        content = content.replace("{code}", code)
        try {
            String[] mobiles = [mobile] as String[]
            int retCode =  SingletonClient.getClient().sendSMS(mobiles, content, "",5);
            logger.info("ip: {}, send sms mobile: {}, retCode:{}", Web.getClientId(req), mobile, retCode)
            if(retCode == 0){
                return Boolean.TRUE
            }
        }catch (Exception e){
            logger.error("send sms code error: {}", e)
            return Boolean.FALSE
        }
        return Boolean.FALSE
    }
*/
    /**
     * 2016/7/7 启用
     * 创蓝短信验证码
     * @param req
     * @param key
     * @param mobile
     * @param content
     * @return
     */
    public Boolean sendMobile(HttpServletRequest req, String key, String mobile, String content){
        return sendMobile(req, key, mobile, content, Boolean.TRUE)
    }

    public Boolean sendMobile(HttpServletRequest req, String key, String mobile, String content, Boolean china){
        def code = AuthCode.randomNumber(6)
        mainRedis.opsForValue().set(key, code, SEND_MOBILE_EXPIRES, TimeUnit.SECONDS)

        //发送手机验证码
        content = content.replace("{code}", code)
        try {
            String[] sendMobile = {mobile}
            String retCode= china ? HttpSender.batchSend(sendMobile, content) : HttpSender.batchSendInter(mobile, content);
            String ip = Web.getClientId(req)
            logger.info("[ip: {}, send sms mobile: {}, retCode:{}]", ip, mobile, retCode)
            if(retCode.equals("0")){
                try{
                    logger.debug("insert logs")
                    smscode_los().insert($$(mobile:mobile, "used":Boolean.FALSE, "sms_code":code, ip : ip, timestamp:System.currentTimeMillis(),
                            "type":ServletRequestUtils.getIntParameter(req, "type", SmsCode.注册.ordinal())))
                }catch (Exception e){
                    logger.error("record log exception : {}",e)
                }
                return Boolean.TRUE
            }
        }catch (Exception e){
            logger.error("send sms code error: {}", e)
            return Boolean.FALSE
        }
        return Boolean.FALSE
    }

    /**
     * 通过手机号获得验证码
     * @param mobile
     * @param type
     * @return
     */
    public String getAuthCodeByMobile(String mobile, Integer type){
        def key = KeyUtils.AUTHCODE.registerSms(mobile)
        if(type == SmsCode.找回密码.ordinal()){
            key = KeyUtils.AUTHCODE.pwdSms(mobile)
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            key = KeyUtils.AUTHCODE.exchangeSms(mobile)
        }
        return mainRedis.opsForValue().get(key)
    }

    /**
     * 推送: 参考star-main项目java/push_user_validate
     *
     * 用户活跃测试验证 检测是否为僵尸（挂机）用户
     *
     */
    def user_validate(HttpServletRequest req){
        def id = req['id']  as Integer
        if(id == null) return Web.missParam();
        //验证通过
        if(Web.codeVeri(req)){
            //TODO 处理逻辑
            mainRedis.delete(KeyUtils.Actives.forbiddenUser(id));
            return [code : 1]
        }
        return [code : Code.验证码验证失败]
    }


}