package com.ttpod.user.web.api;

import com.Geetest.util.GeetestLib;
import com.Geetest.util.GeetestUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import com.ttpod.rest.AppProperties;
import com.ttpod.rest.common.doc.IMessageCode;
import com.ttpod.rest.common.util.WebUtils;
import com.ttpod.rest.web.StaticSpring;
import com.ttpod.user.common.doc.Param;
import com.ttpod.user.common.util.KeyUtils;
import com.ttpod.user.model.SmsCode;
import com.ttpod.user.web.interceptor.OAuth2SimpleInterceptor;
import groovy.transform.CompileStatic;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ttpod.rest.common.doc.MongoKey._id;

@CompileStatic
public abstract class Web  extends WebUtils{


    public static final boolean isTest = AppProperties.get("api.domain").contains("test.");

    final static  Logger logger = LoggerFactory.getLogger(Web.class) ;
    /**
     * 配合 nginx URL 重写
     * rewrite      /([a-z]+/[a-z_]+)/([a-z0-9\-]+)/(\d+)/?([\d_]+)?\??(.*)
     * @return
     */
    public static Integer roomId(HttpServletRequest request){
        return firstNumber(request);
    }

    public static Integer userId(HttpServletRequest request){
        return secondNumber(request);
    }

    public static Integer secondNumber(HttpServletRequest request)
    {
        Integer secondNumber = 0 ;
        try
        {
            String id2 = request.getParameter(Param.second);
            if(StringUtils.isNotBlank(id2))
                secondNumber = Integer.valueOf(id2);
        }
        catch(Exception ex)
        {
            logger.error("secondNumber String cast Integer Exception");
            ex.printStackTrace();
        }
        return  secondNumber ;
       // return Integer.valueOf(request.getParameter(Param.second));
    }

    public static Integer firstNumber(HttpServletRequest request)
    {
         Integer firstNumber = 0 ;
         try
         {
             String id1 = request.getParameter(Param.first) ;
             if(StringUtils.isNotBlank(id1))
                 firstNumber = Integer.valueOf(id1);
         }
         catch(Exception ex)
         {
            logger.error("firstNumber String cast Integer Exception");
            ex.printStackTrace();
         }
        return  firstNumber ;
    }

    public static String firstParam(HttpServletRequest request){
        return request.getParameter(Param.first);
    }

    public static String secondParam(HttpServletRequest request){
        return request.getParameter(Param.second);
    }


    public static Map getSession(){

        return OAuth2SimpleInterceptor.getSession();
    }

    private  static Map<String,Object> missParam = new HashMap<String, Object>();
    private  static Map<String,Object> notAllowed = new HashMap<String, Object>();
    private  static Map<String,Object> ok = new HashMap<String, Object>();

    static {
        missParam.put("code",30406);
        missParam.put("msg","丢失必需参数");
        notAllowed.put("code",30413);
        notAllowed.put("msg","权限不足");
        ok.put("code",1);
    }
    public static Map missParam(){
        return missParam;
    }
    public static Map notAllowed(){
        return notAllowed;
    }
    public static final Map OK =  IMessageCode.OK;
   // public static final Map OK =  Collections.unmodifiableMap(ok);

    public static String hexSeconds(){
       return Long.toHexString(System.currentTimeMillis()/1000);
    }


    /**
     * 验证码验证
     * @param req
     * @return
     */
    public static boolean codeVeri(HttpServletRequest req) {
        //极验证
        if(GeetestUtils.OPEN_GEETEST && GeetestUtils.geetest(req)) return Boolean.TRUE;

        String auth_code = req.getParameter("auth_code");
        String auth_key = req.getParameter("auth_key");


        String key = KeyUtils.AUTHCODE.register(auth_key);
        String red_code = mainRedis.opsForValue().get(key);
        logger.info("AuthCode codeVeri key : {}  auth_code: {}, code : {}", auth_key, auth_code, red_code);
        mainRedis.delete(key);
        if (null == auth_code || !auth_code.equalsIgnoreCase(red_code)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * 手机短信验证码验证
     * @param req
     * @return
     */
    public static boolean smsCodeVeri(SmsCode smsType,HttpServletRequest req) {
        String sms_code = req.getParameter("sms_code");
        String mobile = req.getParameter("mobile");
        return smsCodeVeri(smsType, sms_code, mobile);

    }

    /**
     * 验证码验证是否无效
     * @return
     */
    public static boolean smsCodeVeri(SmsCode smsType,String sms_code, String mobile) {
        String key = KeyUtils.AUTHCODE.registerSms(mobile);
        if(smsType == SmsCode.注册){
            key = KeyUtils.AUTHCODE.registerSms(mobile);
        }else if(smsType == SmsCode.找回密码) {
            key = KeyUtils.AUTHCODE.pwdSms(mobile);
        } else if(smsType == SmsCode.兑换柠檬) {
            key = KeyUtils.AUTHCODE.exchangeSms(mobile);
        }else if(smsType == SmsCode.绑定手机号){
            key = KeyUtils.AUTHCODE.bindMobileSms(mobile);
        }
        String red_code = mainRedis.opsForValue().get(key);
        mainRedis.delete(key);
        //无效验证码
        if (null == sms_code || !sms_code.equalsIgnoreCase(red_code)) {
            return true;
        }
        //标记验证码被使用状态
        try{
            userMongo.getCollection("smscode_logs")
                            .update($$("mobile", mobile)
                            .append("used",Boolean.FALSE)
                            .append("sms_code",sms_code)
                            .append("type",smsType.ordinal()), $$("$set", $$("used",Boolean.TRUE)));
        }catch (Exception e){
            logger.error("sms code log exception : {}", e);
        }
        return false;
    }

    static final String TOTAL_LGOIN_PER_IP = "5";

    static final String TOTAL_SMSSEND_PER_IP = "20";

    static final String TOTAL_SMSSENDPER_MOBILE = "5";

    static final Long TOTAL_SMS_SEND_MOBILE = 10l;

    /**
     * 每天IP发送短信验证码次数
     * @param req
     * @return
     */
    public static boolean smsSendIpLimited(HttpServletRequest req) {
        String clientId = Web.getClientId(req);
        String totalIpLimit = KeyUtils.AUTHCODE.smsLimitTotalPerIp(clientId);
        mainRedis.opsForValue().setIfAbsent(totalIpLimit, TOTAL_SMSSEND_PER_IP);
        if (mainRedis.getExpire(totalIpLimit) < 0) {
            Calendar self = Calendar.getInstance();
            self.set(Calendar.HOUR_OF_DAY, 24);
            self.clear(Calendar.MINUTE);
            self.clear(Calendar.SECOND);
            mainRedis.expireAt(totalIpLimit, self.getTime());
        }
        logger.debug("smsSendIpLimited ip:{}", clientId);
        return mainRedis.opsForValue().increment(totalIpLimit, -1L).intValue() < 0;
    }

    /**
     * 每天手机号发送短信验证码次数
     * @return
     */
    public static boolean smsSendMobileLimited(String mobile) {
        String totalLimit = KeyUtils.AUTHCODE.smsLimitTotalPerMobile(mobile);
        mainRedis.opsForValue().setIfAbsent(totalLimit, TOTAL_SMSSENDPER_MOBILE);
        if (mainRedis.getExpire(totalLimit) < 0) {
            Calendar self = Calendar.getInstance();
            self.set(Calendar.HOUR_OF_DAY, 24);
            self.clear(Calendar.MINUTE);
            self.clear(Calendar.SECOND);
            mainRedis.expireAt(totalLimit, self.getTime());
        }
        logger.debug("smsSendMobileLimited:{}", mobile);
        return mainRedis.opsForValue().increment(totalLimit, -1L).intValue() < 0;
    }

    /**
     * 手机号验证码发送超过10次未注册用户
     * @return
     */
    public static boolean smsSendMobileWithoutReg(String mobile) {

        //判断手机号是否在黑名单里
        if(mainRedis.opsForSet().isMember(KeyUtils.AUTHCODE.smsLimitMobileList(), mobile)){
            logger.debug("smsSendMobileWithoutReg is false:{}", mobile);
            return Boolean.FALSE;
        }
        //记录手机号历史次数
        String totalLimit = KeyUtils.AUTHCODE.smsLimitTotalMobile(mobile);
        //判断是否注册过
        if(userMongo.getCollection("users").count(new BasicDBObject("mobile",mobile)) == 1){
            mainRedis.delete(totalLimit);
            return Boolean.TRUE;
        }
        Long total = mainRedis.opsForValue().increment(totalLimit, 1);
        //如果超过次数，记录到黑名单列表里,删除历史次数记录
        if(total >= TOTAL_SMS_SEND_MOBILE){
            mainRedis.delete(totalLimit);
            if(userMongo.getCollection("users").count(new BasicDBObject("mobile",mobile)) == 0){
                mainRedis.opsForSet().add(KeyUtils.AUTHCODE.smsLimitMobileList(), mobile);
                logger.debug("smsSendMobileWithoutReg smsLimitMobileList is false :{}", mobile);
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }
    /**
     * 每天登录次数
     * @param req
     * @return
     */
    public static boolean clientIsLoginLimited(HttpServletRequest req) {
        String clientId = Web.getClientId(req);
        String totalIpLimit = KeyUtils.USER.loginLimitTotalPerIp(clientId);
        mainRedis.opsForValue().setIfAbsent(totalIpLimit, TOTAL_LGOIN_PER_IP);
        if (mainRedis.getExpire(totalIpLimit) < 0) {
            Calendar self = Calendar.getInstance();
            self.set(Calendar.HOUR_OF_DAY, 24);
            self.clear(Calendar.MINUTE);
            self.clear(Calendar.SECOND);
            mainRedis.expireAt(totalIpLimit, self.getTime());
        }
        return mainRedis.opsForValue().increment(totalIpLimit, -1L).intValue() < 0;
    }

    public static Date getEtime(HttpServletRequest request){
        return getTime(request,"etime");
    }

    public static Date getStime(HttpServletRequest request){
        return getTime(request,"stime");
    }
    public static final String DFMT = "yyyy-MM-dd";
    private static Date getTime(HttpServletRequest request,String key)  {
        String str = request.getParameter(key);
        if(StringUtils.isNotBlank(str)){
            try {
                return new SimpleDateFormat(DFMT).parse(str);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static QueryBuilder fillTimeBetween(HttpServletRequest req){
        QueryBuilder query = QueryBuilder.start();
        Date stime = getStime(req);
        Date etime = getEtime(req);
        if (stime !=null || etime !=null){
            query.and("timestamp");
            if(stime != null){
                query.greaterThanEquals(stime.getTime());
            }
            if (etime != null){
                query.lessThan(etime.getTime());
            }
        }
        return query;
    }

    public static final StringRedisTemplate mainRedis = (StringRedisTemplate) StaticSpring.get("mainRedis");
    public static final MongoTemplate userMongo = (MongoTemplate) StaticSpring.get("userMongo");

    public static String getClientId(HttpServletRequest req){
        String client_id = req.getParameter(Param.uid);
        if(StringUtils.isNotBlank(client_id)){
            return client_id;
        }
        return getClientIp(req);
    }

    public static String getClientIp(HttpServletRequest req){
        String ip = req.getHeader(Param.XFF);
        //String hxff = req.getHeader(Param.HXFF);
        //logger.debug("X-FORWARDED-FOR Ip: {}", xff);
        //logger.debug("http_x_forwarded_for Ip: {}", hxff);
        if(StringUtils.isBlank(ip)){
            ip = req.getRemoteAddr();
        }
        ip = StringUtils.remove(ip, "192.168.1.34");
        ip = StringUtils.remove(ip, "192.168.1.35");
        return ip;
    }

    public static Map currentUser(){
        return getSession();
    }

    public static Integer getCurrentUserId(){
        Integer uid = 0 ;
        uid = Integer.valueOf(currentUserId());
        return uid ;
    }

    public static String currentUserId()
    {
        Map map = getSession();
        String userId = "0" ;
        if(null == map)
            logger.error("currentUserId:OAuth2SimpleInterceptor.getSession is----->: null");
        else
            userId = map.get(_id).toString() ;

        return userId;
    }

}
