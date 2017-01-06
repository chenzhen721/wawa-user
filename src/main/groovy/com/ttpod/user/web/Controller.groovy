package com.ttpod.user.web

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.Rest
import com.ttpod.user.common.util.AuthCode
import com.ttpod.user.common.util.KeyUtils
import com.ttpod.user.model.Code
import com.ttpod.user.web.BaseController
import com.ttpod.user.web.api.Web
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.lang.StringUtils
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.*
import static com.ttpod.rest.common.doc.MongoKey.*;
/**
 * @author: jiao.li@ttpod.com
 * Date: 14-6-16 下午1:39
 */
@Rest
class Controller extends BaseController {

    Logger logger = LoggerFactory.getLogger(Controller.class)

    static final boolean isTest = API_DOMAIN.contains("test.")

    private static final Integer FROM_TYPE_IOS = 3
    private static final Integer FROM_TYPE_ANDRIOD = 2
    private static final Integer FROM_TYPE_WEB = 1

    /**
     * 用户登录
     * @param req
     * @return
     */
    def login(HttpServletRequest req) {
        String clientId = Web.getClientId(req);
        String request_key = req['key']
        def from = ServletRequestUtils.getIntParameter(req, 'f', 1);

        logger.info(req.getServletPath()+"request key : {} ip : {}   from:{} ", request_key, clientId,from.toString())

        //非IOS登录需要验证码
        /*if(from != FROM_TYPE_IOS){
            //验证码重复多次
            if(Web.clientIsLoginLimited(req) &&  !Web.codeVeri(req)){
                return [code: Code.验证码验证失败]
            }
        }*/
        //验证码
        if(!Web.codeVeri(req)){
            return [code: Code.验证码验证失败]
        }
        //手机 用户名 靓号
        def login_name = req['login_name']
        def pwd = req['pwd']
        if(StringUtils.isEmpty(login_name) || StringUtils.isEmpty(pwd)){
            return [code: Code.参数无效]
        }
        DBObject user = users().findOne($$('userName':login_name.toLowerCase()),USER_FIELD)
        if(user == null)
            user = users().findOne($$('mobile':login_name),USER_FIELD)
        if(user == null)
            user = users().findOne($$('mm_no':login_name),USER_FIELD)
        if(user == null)
            return [code:Code.用户名或密码不正确]

        String password = user.get('pwd')
        String id = user.get(_id)
        if(password == null || !MD5.digest2HEX(pwd + id).equals(password))
            return [code:Code.用户名或密码不正确]

        [code: Code.OK, data: [token:user['token']]]
    }

    /**
     * 机器人登录
     * @param req
     * @return
     */
    def login_robot(HttpServletRequest req) {
        //手机 用户名 靓号
        def login_name = req['user_name']
        def pwd = req['password']
        if(StringUtils.isEmpty(login_name) || StringUtils.isEmpty(pwd)){
            return [code: Code.参数无效]
        }
        DBObject user = users().findOne($$('userName':login_name.toLowerCase()),USER_FIELD)
        if(user == null)
            return [code:Code.用户名或密码不正确]

        String password = user.get('pwd')
        String id = user.get(_id)
        if(password == null || !MD5.digest2HEX(pwd + id).equals(password))
            return [code:Code.用户名或密码不正确]

        [code: Code.OK, data: [token:user['token']]]
    }

    def show(HttpServletRequest req){
        def access_token = req['access_token']
        if(StringUtils.isEmpty(access_token)){
            return [code: Code.参数无效]
        }
        DBObject user = users().findOne($$('token':access_token),$$(pic:1,userName:1,mobile:1,via:1,nickname:1,invite_code:1,email:1))
        if(user == null)
            return [code: Code.ERROR]

        def user_name = user['userName']?:user['mobile']?:user['nickname']
        def nickname = user['nickname']
        def mobile_bind = StringUtils.isNotEmpty((user['mobile'] ?:"") as String)
        def uname_bind = StringUtils.isNotEmpty((user['userName'] ?:"") as String)
        [code: Code.OK, data: [tuid:user['_id'], user_name:user_name,uname_bind:uname_bind,nickname:nickname, email:user['email'],
                               mobile_bind:mobile_bind,via:user['via'], pic:user['pic'], mobile:user['mobile'], invite_code:user['invite_code']]]
    }
}