package com.ttpod.user.web

import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.user.model.Code
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$;

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
     * 手机登陆 login
     * @param req
     * @return
     */
    def login_mobile(HttpServletRequest req) {
        logger.debug('Received login params is {}', req.getParameterMap())
        def mobile = req['mobile']
        def pwd = req['pwd']

        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(pwd)) {
            return [code: Code.参数无效]
        }

        def user = users().findOne($$('mobile': mobile), USER_FIELD)
        if (user == null) {
            return [code: Code.用户名或密码不正确]
        }
        def password = user['pwd']
        def id = user['_id']
        if (MD5.digest2HEX(pwd + id) != password) {
            return [code: Code.用户名或密码不正确]
        }

        [code: Code.OK, data: [token: user['token']]]
    }

    /**
     * 机器人登录
     * @param req
     * @return
     */
    def login_robot(HttpServletRequest req) {
        //手机 用户名 靓号
        def login_name = req['user_name']
        def password = req['password']
        if (StringUtils.isBlank(login_name) || StringUtils.isBlank(password)) {
            return [code: Code.参数无效]
        }
        DBObject user = users().findOne($$('user_name': login_name.toLowerCase()), USER_FIELD)
        if (user == null) {
            return [code: Code.用户名或密码不正确]
        }


        String pwd = user.get('pwd')
        String id = user.get(_id)
        if (pwd == null || MD5.digest2HEX(password + id) != pwd) {
            return [code: Code.用户名或密码不正确]
        }

        [code: Code.OK, data: [token: user['token']]]
    }

    def show(HttpServletRequest req) {
        def access_token = req['access_token']
        if (StringUtils.isBlank(access_token)) {
            return [code: Code.参数无效]
        }
        DBObject user = users().findOne($$('token': access_token), $$(pic: 1, user_name: 1, mobile: 1, via: 1, nickname: 1, invite_code: 1, email: 1))
        if (user == null)
            return [code: Code.ERROR]

        def user_name = user['user_name'] ?: user['mobile'] ?: user['nickname']
        def nickname = user['nickname']
        def mobile_bind = StringUtils.isNotEmpty((user['mobile'] ?: "") as String)
        def uname_bind = StringUtils.isNotEmpty((user['user_name'] ?: "") as String)
        [code: Code.OK, data: [tuid       : user['_id'], user_name: user_name, uname_bind: uname_bind, nickname: nickname, email: user['email'],
                               mobile_bind: mobile_bind, via: user['via'], pic: user['pic'], mobile: user['mobile'], invite_code: user['invite_code']]]
    }
}