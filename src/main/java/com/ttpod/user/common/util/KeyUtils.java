package com.ttpod.user.common.util;


import com.ttpod.rest.ext.RestExtension;
import com.ttpod.user.model.User;
import groovy.transform.CompileStatic;

/**
 *
 * 约定的key 值
 *
 * date: 12-8-17 上午11:34
 */
@CompileStatic
public  abstract class KeyUtils {
    public  static byte[] serializer(String string){
        return RestExtension.asBytes(string);
    }

    public  static String decode(byte[] data){
        return RestExtension.asString(data);
    }

    /**
     * 用作redis的标记值
     */
    public static final String MARK_VAL = "";

    private static final String SPLIT_CHAR = ":";

    public static String accessToken(String token){
        return "token:"+token;
    }

    public static class USER {

        public static final String USER = "user:";

        public static String hash(Object uid) {
            return USER + uid ;
        }


        public static String token(Object uid) {
            return USER + uid + SPLIT_CHAR+ User.access_token ;
        }


        public static String blackClient(String uid) {
            return "uidblack:"+uid;
        }

        /**
         * 限制注册 10分钟1个
         * @param uid
         * @return
         */
        public static String regLimit(String uid) {
            return "reg:"+uid;
        }

        /**
         * 同一IP一天限注册10个帐号
         */
        public static String regLimitTotalPerIp(String ip) {
            return "regip:"+ip;
        }

        /**
         * 同一IP一天登录次数
         */
        public static String loginLimitTotalPerIp(String ip) {
            return "loginip:"+ip;
        }

        public static String onlyToken2id(String token) {
            return "ot2id:"+token;
        }

    }
    /**
     * 验证码
     */
    public static class AUTHCODE {
        public static final String AUTHCODE = "authcode:" ;

        //修改密码时间戳
        public static String changePwd(Object uid) {
            return AUTHCODE+ "changepwd:" + uid;
        }

        public static String register(Object client_key) {
            return AUTHCODE+ "register:" + client_key;
        }

        //短信验证码
        public static String registerSms(Object mobile) {
            return AUTHCODE+ "register:sms:" + mobile;
        }

        //找回密码短信验证码
        public static String pwdSms(Object mobile) {
            return AUTHCODE+ "pwd:sms:" + mobile;
        }

        //兑换柠檬验证码
        public static String exchangeSms(Object mobile) {
            return AUTHCODE+ "exchange:sms:" + mobile;
        }

        //绑定手机号
        public static String bindMobileSms(Object mobile) {
            return AUTHCODE+ "bindmobile:sms:" + mobile;
        }

        public static String smsLimitTotalPerIp(String ip) {
            return AUTHCODE+ "smsip:"+ip;
        }
        public static String smsLimitTotalPerMobile(String mobile) {
            return AUTHCODE+ "smsmobile:"+mobile;
        }
        public static String smsLimitTotalMobile(String mobile) {
            return AUTHCODE+ "smsmobile:total:"+mobile;
        }
        public static String smsLimitMobileList() {
            return AUTHCODE+ "limit:smsmobile:list";
        }
    }

    /**
     * 用户昵称 -> id 转换
     */
    public static final String NAME2ID_NS = "_name2id_:";

    public static class Actives
    {
        public static final String ACTIVES = "Actives:" ;
        /**
         * 活动送礼ip限制
         * @param ip
         * @return
         */
        public static String LimitTotalPerIp(String ip) {
            return ACTIVES +"limit:"+ip;
        }

        /**
         * 未通过验证的挂机用户
         * @param userId
         * @return
         */
        public static String forbiddenUser(Object userId) {
            return ACTIVES +"forbidden:user:"+userId;
        }

        /**
         * 用户一定时间类累计送达么么哒数量
         * @param activityId
         * @param userId
         * @return
         */
        public static String LimitTotalPerUser(Object activityId, Object userId) {
            return ACTIVES +"limit:total:user:"+activityId+SPLIT_CHAR+userId;
        }
    }
}
