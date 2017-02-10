package com.ttpod.user.web

import com.mongodb.BasicDBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.user.common.util.AuthCode
import com.ttpod.user.model.Code
import com.ttpod.user.model.SmsCode
import com.ttpod.user.model.User
import com.ttpod.user.web.api.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$

/**
 * @author: jiao.li@ttpod.com
 * Date: 14-6-16 下午1:39
 */
@Rest
class RegisterController extends BaseController {

    Logger logger = LoggerFactory.getLogger(RegisterController.class)

    static final boolean isTest = API_DOMAIN.contains("test.")

    private final static String NICK_NAME_PREFIX = "萌新"

    /**
     * 手机号码注册
     * @param req
     * @return
     */
    def mobile(HttpServletRequest req) {
        logger.debug('Received mobile params is {}', req.getParameterMap())
        def mobile = req['mobile']
        def sms_code = req['sms_code']
        String pwd = req['pwd']

        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(sms_code) || StringUtils.isBlank(pwd)) {
            return Web.missParam()
        }

        if (pwd.length() < 6 || pwd.length() >= 12) {
            return [code: Code.密码格式错误]
        }
        if (!VALID_MOBILE.matcher(mobile).matches()) {
            return [code: Code.手机号格式错误]
        }

        if (Web.smsCodeVeri(SmsCode.注册, req)) {
            return [code: Code.短信验证码无效]
        }

        if (mobileExist(mobile)) {
            return [code: Code.手机号码已存在]
        }

        def user = buildUser(mobile, pwd) as BasicDBObject
        if (user == null) {
            return [code: Code.ERROR]
        }

        logger.debug('user is {}', user)
        [code: Code.OK, data: [token: user['token']]]
    }

    /**
     * 机器人注册
     * @param req
     * @return
     */
    def robot(HttpServletRequest req) {
        if (isTest) {
            def username = req['username']
            def pwd = req['pwd']
            if (StringUtils.isBlank(username) || StringUtils.isBlank(pwd)) {
                return [code: Code.参数无效]
            }
            //用户名是否已经存在
            if (userNameExist(username)) {
                return [code: Code.用户名已存在, error: '用户名已存在']
            }
            BasicDBObject user = buildUser(username, pwd)
            if (user == null) {
                return [code: Code.ERROR]
            }

            [code: Code.OK, data: [access_token: user['token'], _id: user['_id']]]
        }
    }

    /**
     * 创建用户
     * @param username
     * @param pwd
     * @return
     */
    private BasicDBObject buildUser(String username, String pwd) {
        def now = new Date().getTime()
        def userId = userKGS.nextId()
        def token = generateToken(pwd + userId) as String
        def password = MD5.digest2HEX(pwd + userId)
        def pic = User.DEFAULT_PIC
        def user = $$('_id': userId, 'nickname': buildDefaultNickName(),
                'mobile': username, 'pwd': password, 'regTime': now, 'token': token, 'via': 'mobile', 'pic': pic, 'username': username)
        if (!users().insert(user).getN()) {
            return user
        }

        return null
    }

    /**
     * 判断手机号是否存在
     * @param req
     * @return
     */
    def check_mobile(HttpServletRequest req) {
        String mobile = req['mobile']
        if (StringUtils.isBlank(mobile)) {
            return Web.missParam()
        }

        [code: mobileExist(mobile) ? Code.手机号码已存在 : 1]
    }

    /**
     * 创建默认的用户昵称
     * @return
     */
    private static buildDefaultNickName() {
        return NICK_NAME_PREFIX + AuthCode.randomNumber(6);
    }

    /**
     * 判断用户是否存在
     * @param userName
     * @return
     */
    private boolean userNameExist(String userName) {
        return users().count($$('userName': userName.toLowerCase())) > 0
    }

    /**
     * 判断手机号码是否存在
     * @param mobile
     * @return
     */
    private boolean mobileExist(String mobile) {
        return users().count($$('mobile': mobile)) > 0
    }

}