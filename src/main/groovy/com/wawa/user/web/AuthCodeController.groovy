package com.wawa.user.web

import com.wawa.base.BaseController
import com.wawa.base.anno.Rest
import com.wawa.common.msg.HttpSender
import com.wawa.common.util.AuthCode
import com.wawa.common.util.KeyUtils
import com.wawa.model.Code
import com.wawa.model.SmsCode
import com.wawa.api.Web
import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit

import static com.wawa.common.util.WebUtils.$$

/**
 */
@Rest
class AuthCodeController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(AuthCodeController.class)

    def fetch(HttpServletRequest req) {
//        String key = AuthCode.random(8 + ((int) System.currentTimeMillis() & 1)).toLowerCase()
        String key = System.currentTimeMillis() + AuthCode.random(4);
//        String key = AuthCode.random(4);
        String url = "${API_DOMAIN}authcode/image?key=${key}".toString()
        String clientId = Web.getClientId(req);
        String request_key = req.getParameter('key')
        //logger.info(req.getServletPath()+"request key : {} ip : {}   ", request_key, clientId )
        [code: 1, data: [auth_key: key, auth_url: url]]
    }


    /*def image(HttpServletRequest req, HttpServletResponse response) {
        String code = AuthCode.random(4 + ((int) System.currentTimeMillis() & 1))
        String request_key = req['key']
        def key = KeyUtils.AUTHCODE.register(request_key)
        String clientId = Web.getClientId(req);
        if(mainRedis.opsForValue().setIfAbsent(key, code)){
            mainRedis.expire(key, 2*60L, TimeUnit.SECONDS)
            response.addHeader('Content-Type', "image/png")
            AuthCode.draw(code, 160, 48, response.getOutputStream())
        }
    }*/

    /*def send_mobile(HttpServletRequest req) {
        logger.debug('Received send_mobile params is {}',req.getParameterMap())
        def length = ServletRequestUtils.getIntParameter(req,'length',0)
        def mobile = req['mobile'] as String
        //
        Integer type = ServletRequestUtils.getIntParameter(req, "type", SmsCode.登录.ordinal())
        if(StringUtils.isEmpty(mobile)){
            [code : Code.参数无效]
        }

        if(!VALID_MOBILE.matcher(mobile).matches()){
            return [code: Code.手机号格式错误]
        }
        def key = KeyUtils.AUTHCODE.registerSms(mobile)
        String content = SMS_SEND_CONTENT
        if(type == SmsCode.找回密码.ordinal()){
            key = KeyUtils.AUTHCODE.pwdSms(mobile)
            content = SMS_SEND_CONTENT_FIND
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            key = KeyUtils.AUTHCODE.exchangeSms(mobile)
            content = SMS_SEND_CONTENT_EXCHANGE
        }else if(type == SmsCode.绑定手机号.ordinal()){
            key = KeyUtils.AUTHCODE.bindMobileSms(mobile)
            content = SMS_SEND_CONTENT_BIND_MOBILE
        }
        //时间间隔60秒
        if(mainRedis.getExpire(key) > 0
                && (SEND_MOBILE_EXPIRES - mainRedis.getExpire(key)) <= SEND_MOBILE_LIMIT){
            return [code : Code.短信验证间隔太短]
        }
        //手机号的限制 如：一个手机号码一天只能发10条
        Integer limit = Web.smsSendMobileLimited(mobile)
        if(limit < 0)
            return [code : Code.短信验证码每日次数超过限制]
        //IP限制，如：一个IP一天只发20条
        if(Web.smsSendIpLimited(req))
            return [code : Code.短信验证码每日次数超过限制]
        //手机号发送验证码10次未注册用户
        if(!Web.smsSendMobileWithoutReg(mobile))
            return [code : Code.短信验证码每日次数超过限制]

        //if(!sendMobile(req, key, mobile, content, length, (limit%2))){
        [code: Code.OK]
    }*/


    /**
     * 发送验证码
     * @param req
     * @param mobile
     * @param type
     * @return
     */
    /*public Boolean sendMobile(HttpServletRequest req, String mobile, Integer type){
        def key = KeyUtils.AUTHCODE.registerSms(mobile)
        String content = SMS_SEND_CONTENT
        if(type == SmsCode.找回密码.ordinal()){
            key = KeyUtils.AUTHCODE.pwdSms(mobile)
            content = SMS_SEND_CONTENT_FIND
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            key = KeyUtils.AUTHCODE.exchangeSms(mobile)
            content = SMS_SEND_CONTENT_EXCHANGE
        }
//        return sendMobile(req, key, mobile, content)
        false
    }*/

    /**
     * 2016/7/7 启用
     * 创蓝短信验证码
     * @param req
     * @param key
     * @param mobile
     * @param content
     * @return
     */
    /*public Boolean sendMobile(HttpServletRequest req, String key, String mobile, String content){
        return sendMobile(req, key, mobile, content, Boolean.TRUE)
    }*/



    /*public Boolean sendMobile(HttpServletRequest req, String key, String mobile, String content, Boolean china){
        // todo 位数由客户端传
        def code = AuthCode.randomNumber(6)
        mainRedis.opsForValue().set(key, code, SEND_MOBILE_EXPIRES, TimeUnit.SECONDS)

        //发送手机验证码
        content = content.replace("{code}", code)
        try {
            String[] sendMobile = [mobile]
            String retCode= HttpSender.batchSend(sendMobile, content)
            String ip = Web.getClientId(req)
            logger.info("[ip: {}, send sms mobile: {} : {}, retCode:{}]", ip, mobile, code, retCode)
            if(retCode == "0"){
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
    }*/

    /**
     * 重载 发验证码方法
     * 通过客户端传递参数判断发送多少4/6位
     * @param req
     * @param key
     * @param mobile
     * @param content
     * @param length
     * @param channel 多短信频道 1创蓝 0梦网
     * @return
     */
    /*public Boolean sendMobile(HttpServletRequest req, String key, String mobile, String content, Integer length, Integer channel){
        def authLength = length == 1 ? 4 : 6
        def code = AuthCode.randomNumber(authLength)
        mainRedis.opsForValue().set(key, code, SEND_MOBILE_EXPIRES, TimeUnit.SECONDS)

        //发送手机验证码
        content = content.replace("{code}", code)
        try {
            String[] sendMobile = [mobile]
            Boolean success = Boolean.FALSE;
            String retCode;
            if(channel == SmsChannel.创蓝.ordinal()) {
                retCode = HttpSender.batchSend(sendMobile, content);
                success = retCode.equals("0");
            } else {
                retCode = MontnetSmsUtil.send(mobile, content);
                success = !retCode || retCode.length() < 15
            }
            String ip = Web.getClientId(req)
            logger.info("[ip: {}, channel:{}, send sms mobile: {} : {}, retCode:{}]", ip, channel, mobile, code, retCode)
            if(success){
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
    }*/

    /**
     * 通过手机号获得验证码
     * @param mobile
     * @param type
     * @return
     */
    /*public String getAuthCodeByMobile(String mobile, Integer type){
        def key = KeyUtils.AUTHCODE.registerSms(mobile)
        if(type == SmsCode.找回密码.ordinal()){
            key = KeyUtils.AUTHCODE.pwdSms(mobile)
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            key = KeyUtils.AUTHCODE.exchangeSms(mobile)
        }
        return mainRedis.opsForValue().get(key)
    }*/

    /**
     * 推送: 参考star-main项目java/push_user_validate
     *
     * 用户活跃测试验证 检测是否为僵尸（挂机）用户
     *
     */
    /*def user_validate(HttpServletRequest req){
        def id = req['id']  as Integer
        if(id == null) return Web.missParam();
        //验证通过
        if(Web.codeVeri(req)){
            //TODO 处理逻辑
            mainRedis.delete(KeyUtils.Actives.forbiddenUser(id));
            return [code : 1]
        }
        return [code : Code.验证码验证失败]
    }*/


}