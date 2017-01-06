package com.ttpod.user.web

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.rest.common.util.JSONUtil
import com.ttpod.rest.common.util.http.HttpClientUtil4_3
import com.ttpod.user.common.util.HttpClientUtil
import com.ttpod.user.common.util.TenpayHttpClient
import com.ttpod.user.model.Code
import com.ttpod.user.model.User
import com.ttpod.user.web.BaseController
import com.ttpod.user.web.api.Web
import com.xiaomi.utils.XMUtil
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.lang.StringUtils
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

    private final static String WEIXIN_APP_ID = "wxc3074c6fb652a29a"
    private final static String WEIXIN_APP_SECRET = "e49fe2063ae15a08d073f4f96189af93"
    private final static String WEIXIN_URL = "https://api.weixin.qq.com/sns/"

    private final static String WEIXIN_PC_ID = Web.isTest ? "wx8120ce5a486794d7"  : "wxcd8011dbaf7a24ca"
    private final static String WEIXIN_PC_SECRET = Web.isTest ? "666e078f9804095ec04d72d1ee540714" : "dcd7626f840eee103ba7546075332d9b"

    private static final String WEIXIN_H5_APP_ID = Web.isTest ? "wx27a01ce6c6c3e0e8"  : "wx50485b4158037776"
    private static final String WEIXIN_H5_APP_Secret = Web.isTest ? "6d8d88703396a68d6dff50caef7c0491" : "1c8909b64f7b3eb939da2b4e90dae4e3"

    //微信公众号
    private final static String WEIXIN_GZ_ID = Web.isTest ? "wx27a01ce6c6c3e0e8"  : "wx719ebd95f287f861"
    private final static String WEIXIN_GZ_SECRET = Web.isTest ? "6d8d88703396a68d6dff50caef7c0491" : "99d752078b8989f09de1ceb288c56a77"

    //应用宝
    private final static String WEIXIN_BAO_APP_ID = "1105417683"
    private final static String WEIXIN_BAO_APP_SECRET = "iQXUJ1WVzBYS77aU"

    private final static String QQ_APP_ID = "101118713"
    private final static String QQ_APP_KEY = "cc284c27f10b6a09606deb78655e4b6f"
    //应用宝
    private final static String QQ_BAO_APP_ID = "1105417683"
    private final static String QQ_BAO_APP_KEY = "iQXUJ1WVzBYS77aU"

    //午夜交友 聚玩wifi
    private final static Map<String, String> QQ_APP_ID_KEYS = ['101118713':QQ_APP_KEY,
                                                               '1105628717': '2WXtU0Fo2kxcmThS',
                                                               '1105723298': '25ygiMN04ixizrNK',
                                                               '1105724485': 'Xy1RZeqXm9yFc2bX' //同城交友
    ]

    private final static String QQ_URL = "https://graph.qq.com/oauth2.0/"

    private final static String WEIBO_APP_KEY = "3338801716"
    private final static String WEIBO_APP_SECRET = "12bb39fac27508d6b323a2dc547821e5"
    private final static String WEIBO_URL = "https://api.weibo.com/oauth2/"
    private final static String WEIBO_INFO_URL = "https://api.weibo.com/2/"

    private final static String XIAOMI_APP_ID = "2882303761517295066"
    private final static String XIAOMI_APP_KEY = "5101729524066"
    private final static String XIAOMI_APP_SECRET = "0MsyptSEkCryG7nbY9wUvw=="
    private final static String XIAOMI_DOMAIN = "https://open.account.xiaomi.com"

    static final String[] USER_INFO_FIELD=["qd", "via", "from", "pic", "invite_code"]
    /**
     * QQ登录
     * @param req
     */
    def qq(HttpServletRequest req, HttpServletResponse response) {
        String app_id = ServletRequestUtils.getStringParameter(req, "app_id", QQ_APP_ID)
        return qq_login(req, response, app_id, QQ_APP_ID_KEYS[app_id])
    }

    private qq_login(HttpServletRequest req, HttpServletResponse response, String app_id, String app_key){
        def code = req["code"]
        def back_url = req["url"]

        //app client
        //def openid = req["openid"]
        /*if(StringUtils.isNotEmpty(openid)){
            user = users().findOne($$('qq_openid':openid),USER_FIELD)
        }*/
        def access_token = req["access_token"]

        logger.debug("qq login code: {}", code)
        logger.debug("qq login access_token: {}", access_token)
        if(StringUtils.isEmpty(code) && StringUtils.isEmpty(access_token))
            return [code : Code.参数无效]

        DBObject user = null

        //是否首次登录
        Boolean first_login = Boolean.FALSE
        if(user == null){
            if(StringUtils.isEmpty(access_token)){
                def redirect_uri = URLEncoder.encode(SHOW_URL, "utf-8")
                //通过code 获取用户token
                def token_url = QQ_URL + "token?grant_type=authorization_code" +
                        "&client_id=${app_id}&redirect_uri=${redirect_uri}&client_secret=${app_key}&code=${code}"
                logger.debug("qq login token_url: {}", token_url)
                String resp = HttpClientUtil4_3.get(token_url, null, HttpClientUtil4_3.UTF8)
                Map<String, String> respMap =HttpClientUtil.queryString2Map(resp)
                logger.debug("qq login token_url respMap: {}", respMap)
                access_token = respMap['access_token']
            }

            if(StringUtils.isEmpty(access_token)){
                return [code: Code.ERROR]
            }

            /* 使用Access Token来获取用户的OpenID "https://graph.qq.com/oauth2.0/me?access_token="*/
            def openid_url = QQ_URL + "me?access_token=${access_token}&unionid=1"
            String openidResp = HttpClientUtil4_3.get(openid_url, null, HttpClientUtil4_3.UTF8)
            logger.debug("qq login openidResp: {}", openidResp)
            Map<String, Object> openidMaps = JSONUtil.jsonToMap(StringUtils.substringBetween(openidResp, "(", ")"))
            String openId = openidMaps['openid'] as String
            String unionid = openidMaps['unionid'] as String
            logger.debug("qq login openid: {}", openId)
            logger.debug("qq login unionid: {}", unionid)

            if(StringUtils.isEmpty(openId)){
                return [code: Code.ERROR]
            }
            if(StringUtils.isEmpty(unionid)){
                return [code: Code.ERROR]
            }

            //获取用户信息
            user = users().findOne($$('qq_unionid':unionid),USER_FIELD)
            //
            if(user == null){
                //同步历史用户unionid数据
                qqOpenidToUnionId(openId, unionid)
                user = users().findOne($$('qq_unionid':unionid),USER_FIELD)
            }
            //首次登录同步用户信息https://graph.qq.com/user/get_user_info?
            if(user == null){
                //首次登录
                first_login = Boolean.TRUE
                def userInfo_url = "https://graph.qq.com/user/get_user_info?access_token=${access_token}&oauth_consumer_key=${app_id}&openid=${openId}"
                String userInfoResp = HttpClientUtil4_3.get(userInfo_url, null, HttpClientUtil4_3.UTF8)
                logger.debug("qq login userInfoResp: {}", userInfoResp)
                Map<String, Object> userInfoMaps = JSONUtil.jsonToMap(userInfoResp)
                if(!userInfoMaps['ret'].equals(0)){
                    return [code : Code.参数无效]
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
                if(user == null)
                    return [code: Code.ERROR]
            }
        }
        //PC端转跳
        if(StringUtils.isNotEmpty(back_url)) {
            back_url = getRedirectByBackUrl(back_url, user['token'] as String)
            response.sendRedirect(back_url)
            return
        }
        return [code: Code.OK, data: [token:user['token'], first_login:first_login]]
    }


    /**
     * QQ登录之前老用户Openid更新为UnionId
     * @param openid
     * @param unionid
     */
    private void qqOpenidToUnionId(String openid, String unionid){
        def query = $$('qq_openid':openid, 'qq_unionid': null)
        DBObject user = users().findOne(query, $$(qq_unionid:1))
        if(user == null)return;
        logger.debug("qqOpenidToUnionId : {}", user )
        if(StringUtils.isNotEmpty(user['qq_unionid'] as String))return;
        logger.debug("qqOpenidToUnionId : query {}", query )
        users().update(query, $$($set :['qq_unionid': unionid]), false, false, writeConcern)
    }

    /**
     * 微信PC登录
     *
     * 前端使用
     * 文档:https://open.weixin.qq.com/cgi-bin/showdocument?action=dir_list&t=resource/res_list&verify=1&id=open1419316505&token=8afff91a049fa972822db1091b34f74a1c33e320&lang=zh_CN
     * 前端转跳链接: https://open.weixin.qq.com/connect/qrconnect?appid=wxcd8011dbaf7a24ca&redirect_uri=https%3A%2F%2Fwww.imeme.tv&response_type=code&scope=snsapi_login&state=1#wechat_redirect
     *
     * @param req
     */
    def weixin_pc(HttpServletRequest req, HttpServletResponse response) {
        def token_url = WEIXIN_URL + "oauth2/access_token?grant_type=authorization_code&appid=${WEIXIN_PC_ID}&secret=${WEIXIN_PC_SECRET}"
        logger.debug("weixin login unionid pc login..")
        return weixin_login(req,response,token_url)
    }

    /**
     * 微信手机登录
     * @param req
     */
    def weixin(HttpServletRequest req, HttpServletResponse response) {
        Integer type = ServletRequestUtils.getIntParameter(req, "type", 0)//0:么么直播官网 1:应用宝
        def token_url = WEIXIN_URL + "oauth2/access_token?grant_type=authorization_code&appid=${WEIXIN_APP_ID}&secret=${WEIXIN_APP_SECRET}"
        logger.debug("weixin login unionid app login..")
        if(type.equals(1)){
            token_url = WEIXIN_URL + "oauth2/access_token?grant_type=authorization_code&appid=${WEIXIN_BAO_APP_ID}&secret=${WEIXIN_BAO_APP_SECRET}"
        }
        return weixin_login(req,response,token_url)
    }

    /**
     * 微信h5登录
     * @param req
     */
    def weixin_h5(HttpServletRequest req, HttpServletResponse response) {
        def token_url = WEIXIN_URL + "oauth2/access_token?grant_type=authorization_code&appid=${WEIXIN_H5_APP_ID}&secret=${WEIXIN_H5_APP_Secret}"
        logger.debug("weixin login unionid h5 login..")
        return weixin_login(req,response,token_url)
    }

    /**
     * 微信公众号用户静默授权
     * @param req
     */
    def weixin_gz(HttpServletRequest req, HttpServletResponse response) {
        logger.debug("weixin login gz params.. {}", req.getParameterMap())
        String appId = ServletRequestUtils.getStringParameter(req, 'app_id', WEIXIN_GZ_ID)
        String secret = ServletRequestUtils.getStringParameter(req, 'secret', WEIXIN_GZ_SECRET)
        def token_url = WEIXIN_URL + "oauth2/access_token?grant_type=authorization_code&appid=${appId}&secret=${secret}"
        return weixin_login(req,response,token_url)
    }

    /**
     * 微信公众号用户授权code回调转跳页面
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

    private weixin_login (HttpServletRequest req, HttpServletResponse response, String token_url){
        //PC Authorization Code
        def code = req["code"]
        def back_url = req["url"]

        //app client
        def openid = req["openid"]
        def access_token = req["access_token"]

        logger.debug("weixin login code: {}", code)
        logger.debug("weixin login back_url: {}", back_url)
        logger.debug("weixin login access_token: {}", access_token)
        if(StringUtils.isEmpty(code) && StringUtils.isEmpty(access_token))
            return [code : Code.参数无效]

        //是否首次登录
        Boolean first_login = Boolean.FALSE
        def unionid = "";
        if(StringUtils.isEmpty(access_token)){
            token_url =  token_url+"&code=${code}"
            logger.debug("weixin login token_url: {}",token_url)
            String resp = HttpClientUtil4_3.get(token_url, null, HttpClientUtil4_3.UTF8)
            Map respMap = JSONUtil.jsonToMap(resp)
            logger.debug("weixin login token_url respMap: {}", respMap)
            access_token = respMap['access_token'] as String
            openid = respMap['openid'] as String
            unionid = respMap['unionid'] as String
        }

        if(StringUtils.isEmpty(access_token)){
            return [code: Code.ERROR]
        }

        if(StringUtils.isEmpty(openid)){
            return [code: Code.ERROR]
        }
        def userInfo_url = WEIXIN_URL + "userinfo?access_token=${access_token}&openid=${openid}"
        String userInfoResp = HttpClientUtil4_3.get(userInfo_url, null, HttpClientUtil4_3.UTF8)
        Map<String, Object> userInfoMaps = JSONUtil.jsonToMap(userInfoResp)
        logger.debug("weixin login userInfoMaps: {}", userInfoMaps)
        unionid = userInfoMaps['unionid'] as String
        logger.debug("weixin login unionid: {}", unionid)
        if(StringUtils.isEmpty(unionid)){
            return [code: Code.ERROR]
        }
        //获取用户信息
        DBObject user = users().findOne($$('weixin_unionid':unionid),USER_FIELD)

        weixinOpenidToUnionId(openid, unionid)

        //生成用户信息
        if(user == null){
            //首次登录
            first_login = Boolean.TRUE
            Map userInfos = new HashMap();
            userInfos.put("weixin_openid", openid)
            userInfos.put("weixin_access_token", access_token)
            userInfos.put("weixin_unionid", unionid)
            userInfos.put("pic", userInfoMaps['headimgurl']?:"")
            userInfos.put("nickname", userInfoMaps['nickname'])
            userInfos.put("via", "weixin")
            user = buildThirdUser(userInfos, openid, req)
            if(user == null)
                return [code: Code.ERROR]
        }

        //PC端转跳
        if(StringUtils.isNotEmpty(back_url)) {
            back_url = getRedirectByBackUrl(back_url, user['token'] as String)
            response.sendRedirect(back_url)
            return
        }
        return [code: Code.OK, data: [token:user['token'], first_login:first_login, openid:openid]]
    }

    /**
     * 之前老用户Openid更新为UnionId
     * @param openid
     * @param unionid
     */
    private void weixinOpenidToUnionId(String openid, String unionid){
        DBObject user = users().findOne($$('weixin_openid':openid, 'weixin_unionid': null), $$(weixin_unionid:1))
        if(user == null) return;
        if(StringUtils.isNotEmpty(user['weixin_unionid'] as String)) return;
        users().update($$('weixin_openid':openid,'weixin_unionid': null), $$($set :['weixin_unionid': unionid]), false, false, writeConcern)
    }

    /**
     * 新浪微博  * @param req
     */
    def sina(HttpServletRequest req, HttpServletResponse response) {
        //Authorization Code
        def code = req["code"]
        def back_url = req["url"]
        String access_token = req["access_token"]
        String uid = req["uid"]

        logger.debug("sina login code: {}", code)
        if(StringUtils.isEmpty(code) && StringUtils.isEmpty(access_token))
            return [code : Code.参数无效]

/*        def token_url = WEIBO_URL + "access_token?grant_type=authorization_code" +
                "&client_id=${WEIBO_APP_KEY}&redirect_uri=${redirect_uri}&client_secret=${WEIBO_APP_SECRET}&code=${code}"*/

        if(StringUtils.isEmpty(access_token)){
            Map<String,String> params = new HashMap<>();
            params.put("grant_type","authorization_code")
            params.put("client_id",WEIBO_APP_KEY)
            params.put("client_secret",WEIBO_APP_SECRET)
            params.put("redirect_uri",SHOW_URL)
            params.put("code",code)
            logger.debug("weibo login params: {}", params)
            String resp = HttpClientUtil4_3.post(WEIBO_URL+"access_token", params, null)
            logger.debug("weibo login token_url resp: {}", resp)
            Map<String, Object> respInfoMaps = JSONUtil.jsonToMap(resp)
            access_token = respInfoMaps.get("access_token")  as String
            uid = respInfoMaps.get("uid") as String
        }

        if(StringUtils.isEmpty(access_token)){
            return [code: Code.ERROR]
        }

        //通过token获取uid
        if(StringUtils.isEmpty(uid)){
            def uid_url = "${WEIBO_INFO_URL}account/get_uid.json?access_token=${access_token}"
            String uidResp = HttpClientUtil4_3.get(uid_url, null, HttpClientUtil4_3.UTF8)
            Map<String, Object> respInfoMaps = JSONUtil.jsonToMap(uidResp)
            uid = respInfoMaps.get("uid") as String
        }

        //获取用户信息
        DBObject user = users().findOne($$('sina_uid':uid), USER_FIELD)
        //是否首次登录
        Boolean first_login = Boolean.FALSE

        //首次登录同步用户信息 生成用户信息
        if(user == null){
            def userInfo_url = "${WEIBO_INFO_URL}users/show.json?access_token=${access_token}&uid=${uid}"
            String userInfoResp = HttpClientUtil4_3.get(userInfo_url, null, HttpClientUtil4_3.UTF8)
            logger.debug("sina login userInfoResp: {}", userInfoResp)
            Map<String, Object> userInfoMaps = JSONUtil.jsonToMap(userInfoResp)
            if(userInfoMaps == null || !userInfoMaps["idstr"].equals(uid)){
                return [code : Code.参数无效]
            }
            //首次登录
            first_login = Boolean.TRUE
            Map userInfos = new HashMap();
            userInfos.put("sina_uid", uid)
            userInfos.put("sina_access_token", access_token)
            userInfos.put("pic", userInfoMaps['avatar_large'])
            userInfos.put("nickname", userInfoMaps['name'])
            userInfos.put("via", "sina")
            user = buildThirdUser(userInfos, uid, req)
            if(user == null)
                return [code: Code.ERROR]
        }
        //PC端转跳
        if(StringUtils.isNotEmpty(back_url)){
            back_url = getRedirectByBackUrl(back_url, user['token'] as String)
            response.sendRedirect(back_url)
            return
        }
        /* def out =  response.getWriter()
         response.setCharacterEncoding("utf-8")
         response.setContentType('text/html;charset=utf-8')
         out.with {
             println("<script type=\"text/javascript\"> " +
                     "window.UserSystem.setUserInfo('{\"code\":1, \"msg\":\"登录成功\",\"data\":{\"access_token\":\"${user['token']}\",\"first_login\":\"${first_login}\"}}')</script>")
             out.close()
         }*/
        return [code: Code.OK, data: [token:user['token'], first_login:first_login]]
    }

    /**
     * 小米登录
     * @param req
     * @param res
     */
    def xiaomi(HttpServletRequest req, HttpServletResponse res) {
        def token = req.getParameter('access_token') as String
        def mackey = req.getParameter('mac_key') as String
        if (StringUtils.isBlank(token) || StringUtils.isBlank(mackey)) {
            return [code: Code.ERROR]
        }
        //获取小米用户ID
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("clientId", String.valueOf(XIAOMI_APP_ID)));
        params.add(new BasicNameValuePair("token", token));

        String nonce = XMUtil.generateNonce();
        String qs = URLEncodedUtils.format(params, "UTF-8");
        String mac = XMUtil.getMacAccessTokenSignatureString(nonce, "GET", "open.account.xiaomi.com", "/user/profile", qs,
                mackey, "HmacSHA1");
        Map<String, String> macHeader = XMUtil.buildMacRequestHead(token, nonce, mac);
        def user_url = XIAOMI_DOMAIN + "/user/profile?clientId=${XIAOMI_APP_ID}&token=${token}".toString()
        String userInfoResp = null
        try {
            userInfoResp = HttpClientUtil4_3.get(user_url, macHeader)
        } catch (Exception e) {
            logger.debug("xiaomi login user/profile access error:{}", e)
        }
        Map<String, Object> userInfoMaps = null
        def xiaomId = ""
        if(userInfoResp!=null) {
            userInfoMaps = JSONUtil.jsonToMap(userInfoResp)
            xiaomId = (userInfoMaps['data'] as Map)?.get('userId') as String
        }
        if (userInfoMaps == null || !'ok'.equals(userInfoMaps['result']) || StringUtils.isBlank(xiaomId)) {
            return [code: Code.ERROR]
        }
        //是否首次登录
        Boolean first_login = Boolean.FALSE
        //获取用户信息 nerver modify xiaomi_uid because of creating index in mongo
        DBObject user = users().findOne($$('xiaomi_uid': xiaomId), USER_FIELD)
        if (user == null) {
            //首次登录
            first_login = Boolean.TRUE
            Map userInfos = new HashMap();
            userInfos.put("xiaomi_uid", xiaomId)
            userInfos.put("pic", userInfoMaps['miliaoIcon'])
            userInfos.put("nickname", userInfoMaps['miliaoNick'])
            userInfos.put("via", "xiaomi")
            user = buildThirdUser(userInfos, xiaomId, req)
            if(user == null)
                return [code: Code.ERROR]
        }
        return [code: Code.OK, data: [token:user['token'], first_login:first_login]]
    }

    @Resource
    RegisterController registerController

    private DBObject buildThirdUser(Map userInfos, String tuid, HttpServletRequest req){
        Integer user_id = userKGS.nextId();
        DBObject info  = new BasicDBObject(_id, user_id)
        long time = System.currentTimeMillis()
        info.put('regTime',time)
        String token = generateToken(tuid + user_id)
        info.put('token',token)
        userInfos.each {String k, Object v ->
            info.put(k,v)
        }
        for(String field : USER_INFO_FIELD){
            if(StringUtils.isNotEmpty(req.getParameter(field)))
                info.put(field,req.getParameter(field))
        }
        if(StringUtils.isEmpty(info["pic"] as String))
            info.put("pic", User.DEFAULT_PIC)

        info.put('nickname',RegisterController.buildDefaultNickName())

        try{
            if(users().update($$(_id: user_id),info, true, false, writeConcern).getN() != 1){
                return null
            }
        }catch(Exception e){
            logger.error("Thirdlogin buildUser error :{}", e)
            return null
        }
        return info
    }

    private final static String TOKEN_FIELD = '{access_token}'
    private static String getRedirectByBackUrl(String back_url, String token){
        if(StringUtils.isNotEmpty(back_url)) {
            back_url = back_url+(back_url.contains("?") ? "&" : "?")
            back_url = URLDecoder.decode(back_url, "UTF-8");
            logger.debug("back_url origin :{}", back_url)
            if(StringUtils.contains(back_url, TOKEN_FIELD)){
                back_url = back_url.replace(TOKEN_FIELD, token)
            }else{
                back_url = back_url+"access_token=${token}"
            }
            logger.debug("back_url after replace :{}", back_url)
            return back_url
        }
        return "";
    }
}