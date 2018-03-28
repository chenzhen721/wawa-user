package com.wawa.api.interceptor;

import com.mongodb.*;
import com.wawa.base.data.SimpleJsonView;
import com.wawa.base.persistent.KGS;
import com.wawa.common.doc.ParamKey;
import groovy.transform.CompileStatic;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Oauth2 认证登录
 * <p/>
 * date: 12-8-24 下午1:52
 *
 * @author: yangyang.cong@ttpod.com
 */
@CompileStatic
public class OAuth2SimpleInterceptor extends HandlerInterceptorAdapter {

    public void setMainMongo(MongoTemplate mainMongo) {
        this.mainMongo = mainMongo;
    }

    public void setMainRedis(StringRedisTemplate mainRedis) {
        this.mainRedis = mainRedis;
    }

    @Resource
    MongoTemplate mainMongo;
    @Resource
    StringRedisTemplate mainRedis;
    @Resource
    KGS userKGS;

    @Resource
    WriteConcern writeConcern;

    static final Logger log = LoggerFactory.getLogger(OAuth2SimpleInterceptor.class);

    private static final ThreadLocal<Map<String, Object>> sessionHolder = new ThreadLocal<Map<String, Object>>();

    public static void setSession(Map<String, String> session)
    {
        sessionHolder.set((Map)session);
    }

    public static Map<String, Object> getSession()
    {
        Map<String, Object> map =  sessionHolder.get() ;

        return map ;
    }

    public void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
            throws Exception {
        sessionHolder.remove();
    }

    static final Integer ROBOT_MAX = 1023956;

    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws ServletException, IOException {

        String tokenValue = parseToken(request);
        handleNotAuthorized(request, response, notAuthorized);
        return false;

    }
   // final String nullToken = "{\"code\":30406,\"msg\":\"ACCESS_TOKEN为NULL\"}";
    final String notAuthorized = "{\"code\":30405,\"msg\":\"ACCESS_TOKEN无效\"}";
    final String notAllowed = "{\"code\":30418,\"msg\":\"账户已禁用\"}";
    final String regTooOften = "{\"code\":30423,\"msg\":\"注册太频繁\"}";
    final String banned = "{\"code\":30421,\"msg\":\"恶意访问，ip，设备被禁\"}";
    final String notAuthorized_null = "{\"code\":30460,\"msg\":\"ACCESS_TOKEN获取用户系统信息为Null\"}";



    protected void handleNotAuthorized(HttpServletRequest request, HttpServletResponse response, String json)
            throws ServletException, IOException {
        String callback = request.getParameter(ParamKey.In.callback);
        if (StringUtils.isNotBlank(callback)) {
            json = callback + '(' + json + ')';
        }
        SimpleJsonView.rennderJson(json, response);
    }

    public static String parseToken(HttpServletRequest request) {

        // bearer type allows a request parameter as well
        String token = request.getParameter(ACCESS_TOKEN);
        if (token == null) {
            token = parseHeaderToken(request);
        }
        String queryString = request.getQueryString();
        if(null == token && StringUtils.isNotBlank(queryString))
        {
            try
            {
                String queryStringParse = null ;
                String [] tmp  = queryString.split("&");
                String access_token =  tmp[0] ;
                String [] tmp2 = access_token.split("=") ;
                queryStringParse = tmp2[1];
                token =  queryStringParse ;
               // log.info("parseToken Parse token----------->:{}",queryStringParse);
            }
            catch(Exception ex)
            {
                log.error("parseToken parse is Exception");
                ex.printStackTrace();
            }
        }
        return token;
    }

    static String ACCESS_TOKEN = "access_token";

    static final String BEARER_TYPE = "bearer";

    static String EXPIRES_IN = "expires_in";

    /**
     * Parse the OAuth header parameters. The parameters will be oauth-decoded.
     *
     * @param request The request.
     * @return The parsed parameters, or null if no OAuth authorization header was supplied.
     */
    static String parseHeaderToken(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Enumeration<String> headers = request.getHeaders("Authorization");
        while (headers.hasMoreElements()) { // typically there is only one (most servers enforce that)
            String value = headers.nextElement();
            if ((value.toLowerCase().startsWith(BEARER_TYPE))) {
                String authHeaderValue = value.substring(BEARER_TYPE.length()).trim();
                int commaIndex = authHeaderValue.indexOf(',');
                if (commaIndex > 0) {
                    authHeaderValue = authHeaderValue.substring(0, commaIndex);
                }
                return authHeaderValue;
            } else {
                // support additional authorization schemes for different token types, e.g. "MAC" specified by
                // http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token
            }
        }

        return null;
    }


    //http://192.168.1.181/redmine/projects/xinyuan/wiki/%E6%98%9F%E6%84%BFAPI%E6%96%87%E6%A1%A3#完成任务
    static final DBObject complete_mission = new BasicDBObject();
    static final DBObject MONEY = new BasicDBObject();
    static final Map<String,String>  QD_USER = new HashMap<String,String>() ;


    static final DBObject REG_LIMIT = new BasicDBObject();
    public static final Long THREE_DAY_SECONDS = 3 * 24 * 3600L;

    //http://192.168.1.181/redmine/projects/ttpod-ttus/wiki/%E5%AE%A2%E6%88%B7%E7%AB%AF40%E7%94%A8%E6%88%B7%E7%B3%BB%E7%BB%9F%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3#根据access_token获取用户基本信息


    static final String[] needFields = {"user_name", "via", "sex", "pic"};

    static final Long REG_LIMIT_SECONDS = 600L;
    static final String TOTAL_REG_PER_IP = "10";


    private boolean clientIsLimited(String clientId) {
        //
        //10分钟注册一个
        return false;
/*        String regLimit = KeyUtils.USER.regLimit(clientId);
        if (!mainRedis.opsForValue().setIfAbsent(regLimit, "")) {
            if (mainRedis.getExpire(regLimit) < 0) { //make sure crash between the first SETNX and the EXPIRE will cause a deadlock.
                mainRedis.expire(regLimit, REG_LIMIT_SECONDS, TimeUnit.SECONDS);
            }

            return true;
        } else {
            mainRedis.expire(regLimit, REG_LIMIT_SECONDS, TimeUnit.SECONDS);
        }

        String totalIpLimit = KeyUtils.USER.regLimitTotalPerIp(clientId);
        mainRedis.opsForValue().setIfAbsent(totalIpLimit, TOTAL_REG_PER_IP);
        if (mainRedis.getExpire(totalIpLimit) < 0) {
            Calendar self = Calendar.getInstance();
            self.set(Calendar.HOUR_OF_DAY, 24);
            self.clear(Calendar.MINUTE);
            self.clear(Calendar.SECOND);
            mainRedis.expireAt(totalIpLimit, self.getTime());
        }
        return mainRedis.opsForValue().increment(totalIpLimit, -1L).intValue() < 0;*/

    }
}
