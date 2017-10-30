package com.ttpod.user.web

import com.mongodb.DBObject
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.common.util.http.HttpClientUtil4_3
import com.ttpod.user.common.util.HttpClientUtil
import com.ttpod.user.model.Code
import com.ttpod.user.model.User
import com.ttpod.user.web.api.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * @author: jiao.li@ttpod.com
 * Date: 14-4-24 下午1:39
 */
@Rest
class ThirdloginController extends BaseController {

    static Logger logger = LoggerFactory.getLogger(ThirdloginController.class)

    // 微信app属性
    //private final static String WEIXIN_APP_ID = "wx45d43a50adf5a470"
    private final static String WEIXIN_APP_ID = "wx85c1789a23ef15f9"
    private final static String WEIXIN_APP_SECRET = "4b9628580a15224181505883d588ed30"
    private final static String WEIXIN_URL = "https://api.weixin.qq.com/sns/"

    // 微信h5属性
    private static final String WEIXIN_H5_APP_ID = "wx45d43a50adf5a470"
    private static final String WEIXIN_H5_APP_SECRET = "40e8dc2daac9f04bfbac32a64eb6dfff"

    // qq app id
    private final static String QQ_APP_ID = '1106155396'
    private final static String QQ_APP_PC_ID = '101421372'

    // qq app key
    private final static String QQ_APP_KEY = 'eWQV6GKzP9RIfnEX'

    // qq id和key
    private final static Map<String,String> QQ_APP_ID_KEYS = ['1106155396': 'eWQV6GKzP9RIfnEX', '101421372':'6a1107ce3882e5bdb44176aead76e032']

    private final static String TOKEN_FIELD = '{access_token}'

    private final static String QQ_URL = "https://graph.qq.com/oauth2.0/"

    static final String[] USER_INFO_FIELD = ["qd", "via", "from", "pic", "invite_code"]

    /**
     * QQ登录
     * @param req
     */
    def qq(HttpServletRequest req, HttpServletResponse response) {
        logger.debug('Received qq params is {}',req.getParameterMap())
        String appId = ServletRequestUtils.getStringParameter(req, "app_id", QQ_APP_ID)
        String key = QQ_APP_ID_KEYS[appId]
        return qq_login(req, response, appId, key)
    }

    def qq_pc(HttpServletRequest req, HttpServletResponse response) {
        logger.debug('Received qq params is {}',req.getParameterMap())
        String appId = ServletRequestUtils.getStringParameter(req, "app_id", QQ_APP_PC_ID)
        String key = QQ_APP_ID_KEYS[appId]
        return qq_login(req, response, appId, key)
    }

    /**
     * 微信手机登录
     * @param req
     */
    def weixin(HttpServletRequest req, HttpServletResponse response) {
        logger.debug('Received weixin params is {}', req.getParameterMap())
        def token_url = "${WEIXIN_URL}oauth2/access_token?grant_type=authorization_code&appid=${WEIXIN_APP_ID}&secret=${WEIXIN_APP_SECRET}"
        return weixin_login(req, response, token_url)
    }

    /**
     * 微信h5登录
     * @param req
     */
    def weixin_h5(HttpServletRequest req, HttpServletResponse response) {
        def token_url = "${WEIXIN_URL}oauth2/access_token?grant_type=authorization_code&appid=${WEIXIN_H5_APP_ID}&secret=${WEIXIN_H5_APP_SECRET}"
        return weixin_login(req, response, token_url)
    }

    /**
     * 微信auth_code
     */
    def weixin_code_redirect(HttpServletRequest req, HttpServletResponse response){
        def code = req["code"]
        def back_url = req["url"]
        if(StringUtils.isNotEmpty(code) && StringUtils.isNotEmpty(back_url)) {
            //response.sendRedirect(back_url + "?access_token=${user['token']}")
            back_url = back_url+(back_url.contains("?") ? "&" : "?")
            back_url = URLDecoder.decode(back_url, "UTF-8");
            back_url = back_url.replace('{code}', code)
            logger.debug("weixin_code_redirect back_url: {}", back_url)
            response.sendRedirect(back_url)
            return
        }
        [code : 0]
    }

    /**
     * qq登陆接口
     * @param req
     * @param response
     * @param app_id
     * @param app_key
     * @return
     */
    private qq_login(HttpServletRequest req, HttpServletResponse response, String app_id, String app_key) {
        logger.debug('Receive qq_login params req is {},app_id is {},app_key is {}', req.getParameterMap(), app_id, app_key)
        def back_url = req["url"]
        def code = req["code"]
        def access_token = req["access_token"]

        if (StringUtils.isBlank(code) && StringUtils.isBlank(access_token)) {
            return [code: Code.参数无效]
        }

        //是否首次登录
        Boolean first_login = Boolean.FALSE

        if (StringUtils.isBlank(access_token)) {
            def redirect_uri = URLEncoder.encode(API_DOMAIN, "utf-8")
            //通过code 获取用户token
            def token_url = "${QQ_URL}token?grant_type=authorization_code&client_id=${app_id}&redirect_uri=${redirect_uri}&client_secret=${app_key}&code=${code}"
            logger.debug("qq login token_url: {}", token_url)
            String resp = HttpClientUtil4_3.get(token_url, null, HttpClientUtil4_3.UTF8)
            Map<String, String> respMap = HttpClientUtil.queryString2Map(resp)
            logger.debug("qq login token_url respMap: {}", respMap)
            access_token = respMap['access_token']
            if (StringUtils.isBlank(access_token)) {
                return [code: Code.ERROR]
            }
        }

        // 使用Access Token来获取用户的OpenID "https://graph.qq.com/oauth2.0/me?access_token="
        def openid_url = "${QQ_URL}me?access_token=${access_token}&unionid=1"
        String openidResp = HttpClientUtil4_3.get(openid_url, null, HttpClientUtil4_3.UTF8)
        logger.debug("qq login openidResp: {}", openidResp)
        Map<String, Object> qq_info = JSONUtil.jsonToMap(StringUtils.substringBetween(openidResp, "(", ")"))
        def openId = qq_info['openid'] as String
        def unionid = qq_info['unionid'] as String
        if (StringUtils.isBlank(unionid)) {
            logger.error('unionid is is null ..')
            return [code: Code.ERROR]
        }

        //获取用户信息
        logger.debug('unionid is {}',unionid)

        def user = users().findOne($$('qq_unionid': unionid), USER_FIELD)

        //首次登录同步用户信息https://graph.qq.com/user/get_user_info?
        if (user == null) {
            //首次登录
            first_login = Boolean.TRUE
            def userInfo_url = "https://graph.qq.com/user/get_user_info?access_token=${access_token}&oauth_consumer_key=${app_id}&openid=${openId}"
            String userInfoResp = HttpClientUtil4_3.get(userInfo_url, null, HttpClientUtil4_3.UTF8)
            logger.debug("qq login userInfoResp: {}", userInfoResp)
            Map<String, Object> userInfoMaps = JSONUtil.jsonToMap(userInfoResp)
            if (!userInfoMaps['ret'].equals(0)) {
                return [code: Code.参数无效]
            }
            Map userInfos = new HashMap();
            userInfos.put("qq_openid", openId)
            userInfos.put("qq_unionid", unionid)
            userInfos.put("qq_access_token", access_token)
            userInfos.put("pic", userInfoMaps['figureurl_qq_2'])
            userInfos.put("nickname", userInfoMaps['nickname'])
            userInfos.put("via", "qq")
            //生成用户信息
            user = buildThirdUser(userInfos, openId, req)
            if (user == null)
                return [code: Code.ERROR]
        }
        //PC端转跳
        if(StringUtils.isNotEmpty(back_url)) {
            back_url = getRedirectByBackUrl(back_url, user['token'] as String)
            response.sendRedirect(back_url)
            return
        }
        return [code: Code.OK, data: [token: user['token'], first_login: first_login,'openid':openId]]
    }



    /**
     * 微信登陆接口
     * @param req
     * @param response
     * @param token_url
     * @return
     */
    private weixin_login(HttpServletRequest req, HttpServletResponse response, String token_url) {
        logger.debug('Received weixin_login params req is {}.token_url is {}', req.getParameterMap(), token_url)
        def openid = req['openid']
        def code = req['code']
        def back_url = req["url"]
        def access_token = req['access_token']
        def first_login = Boolean.FALSE

        if (StringUtils.isBlank(code) && StringUtils.isBlank(access_token)) {
            return [code: Code.参数无效]
        }

        if(StringUtils.isNotBlank(code) && StringUtils.isEmpty(access_token)){
            token_url =  token_url+"&code=${code}"
            logger.debug("weixin login token_url: {}",token_url)
            String resp = HttpClientUtil4_3.get(token_url, null, HttpClientUtil4_3.UTF8)
            Map respMap = JSONUtil.jsonToMap(resp)
            logger.debug("weixin login token_url respMap: {}", respMap)
            access_token = respMap['access_token'] as String
            openid = respMap['openid'] as String
        }

        if(StringUtils.isEmpty(access_token) && StringUtils.isEmpty(openid)){
            return [code: Code.参数无效]
        }

        def userInfo_url = WEIXIN_URL + "userinfo?access_token=${access_token}&openid=${openid}"
        String userInfoResp = HttpClientUtil4_3.get(userInfo_url, null, HttpClientUtil4_3.UTF8)
        Map<String, Object> userInfoMaps = JSONUtil.jsonToMap(userInfoResp)
        logger.debug("weixin login userInfoMaps: {}", userInfoMaps)
        def unionId = userInfoMaps['unionid'] as String
        logger.debug("weixin login unionId: {}", unionId)
        if (StringUtils.isBlank(unionId)) {
            return [code: Code.ERROR]
        }
        //获取用户信息
        def user = users().findOne($$('weixin_unionid': unionId), USER_FIELD)

        //生成用户信息
        if (user == null) {
            //首次登录
            first_login = Boolean.TRUE
            Map userInfos = new HashMap();
            userInfos.put("weixin_openid", openid)
            userInfos.put("weixin_access_token", access_token)
            userInfos.put("weixin_unionid", unionId)
            userInfos.put("pic", userInfoMaps['headimgurl'] ?: "")
            userInfos.put("nickname", userInfoMaps['nickname'])
            userInfos.put("via", "weixin")
            user = buildThirdUser(userInfos, openid, req)
            if (user == null)
                return [code: Code.ERROR]
        }
        //PC端转跳
        if(StringUtils.isNotEmpty(back_url)) {
            back_url = getRedirectByBackUrl(back_url, user['token'] as String)
            response.sendRedirect(back_url)
            return
        }
        return [code: Code.OK, data: [token: user['token'], first_login: first_login, openid: openid]]
    }


    @Resource
    RegisterController registerController

    /**
     * 构建第三方登陆用户
     * @param userInfos
     * @param tuid
     * @param req
     * @return
     */
    private DBObject buildThirdUser(Map userInfos, String tuid, HttpServletRequest req) {
        Integer user_id = userKGS.nextId();
        DBObject info = $$(_id, user_id)
        long time = System.currentTimeMillis()
        info.put('regTime', time)
        String token = generateToken(tuid + user_id)
        info.put('token', token)
        info.putAll(userInfos)
        for (String field : USER_INFO_FIELD) {
            if (StringUtils.isNotBlank(req.getParameter(field)))
                info.put(field, req.getParameter(field))
        }
        if (StringUtils.isBlank(info["pic"] as String)) {
            info.put("pic", User.DEFAULT_PIC)
        }

        if (users().update($$(_id: user_id), info, true, false, writeConcern).getN()) {
            return info
        }

        logger.error("ThirdLogin buildUser error")
        return null
    }

    /**
     * 获取重定向url地址
     * @param back_url
     * @param token
     * @return
     */
    private static String getRedirectByBackUrl(String back_url, String token) {
        back_url = back_url + (back_url.contains("?") ? "&" : "?")
        back_url = URLDecoder.decode(back_url, "UTF-8");
        logger.debug("back_url origin :{}", back_url)
        if (StringUtils.contains(back_url, TOKEN_FIELD)) {
            back_url = back_url.replace(TOKEN_FIELD, token)
        } else {
            back_url = back_url + "access_token=${token}"
        }
        logger.debug("back_url after replace :{}", back_url)
        return back_url
    }

}