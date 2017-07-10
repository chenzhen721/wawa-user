package com.ttpod.user.web

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
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

    static final boolean isTest = API_DOMAIN.contains("test-")

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
        if(!VALID_PWD.matcher(pwd).matches()){
            logger.debug('密码格式错误')
            return [code: Code.密码格式错误]
        }
        if (!VALID_MOBILE.matcher(mobile).matches()) {
            logger.debug('手机号格式错误')
            return [code: Code.手机号格式错误]
        }

        if (Web.smsCodeVeri(SmsCode.注册, req)) {
            logger.debug('短信验证码无效')
            return [code: Code.短信验证码无效]
        }

        if (mobileExist(mobile)) {
            logger.debug('手机号码已存在')
            return [code: Code.手机号码已存在]
        }

        DBObject user = buildUser(req, null, mobile, null, pwd, null,null)
        if(user == null)
            return [code: Code.ERROR]

        [code: Code.OK, data: [token: user['token']]]
    }

    /**
     * 手机号码注册+登录
     * @param req
     * @return
     */
    def mobile_login(HttpServletRequest req) {
        logger.debug('Received mobile params is {}', req.getParameterMap())
        def mobile = req['mobile']
        def sms_code = req['sms_code']

        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(sms_code)) {
            return Web.missParam()
        }
        if (!VALID_MOBILE.matcher(mobile).matches()) {
            logger.debug('手机号格式错误')
            return [code: Code.手机号格式错误]
        }

<<<<<<< HEAD
        if (Web.smsCodeVeri(SmsCode.注册, req)) {
=======
        if (Web.smsCodeVeri(SmsCode.登录, req)) {
>>>>>>> dev
            logger.debug('短信验证码无效')
            return [code: Code.短信验证码无效]
        }
        //是否首次登录
        Boolean first_login = Boolean.FALSE
        def user = users().findOne($$('mobile': mobile), USER_FIELD)
        if(user == null){
            first_login = Boolean.TRUE
            user = buildUser(req, null, mobile, null, null, null,null)
        }
        if(user == null)
            return [code: Code.ERROR]
        [code: Code.OK, data: [token: user['token'], first_login: first_login]]
    }

    /**
     * 机器人注册
     * @param req
     * @return
     */
    def robot(HttpServletRequest req) {
        def username = req['username']
        def mobile = req['mobile']
        def pwd = req['pwd']
        if(StringUtils.isEmpty(username) && StringUtils.isEmpty(mobile)){
            return [code: Code.参数无效]
        }
        //用户名是否已经存在
        if(StringUtils.isNotEmpty(username) && userNameExist(username)){
            return [code: Code.用户名已存在, error:'用户名已存在']
        }
        if(StringUtils.isNotEmpty(mobile) &&mobileExist(mobile)) {
            return [code: Code.手机号码已存在, error:'手机号已经存在']
        }
        DBObject user = buildUser(req, username, mobile, null, pwd,null,null)
        if(user == null)
            return [code: Code.ERROR]

        [code: Code.OK, data: [access_token:user['token'], _id:user[_id], nick_name:user['nickname'], pic:user['pic']]]

    }

    static final String[] USER_INFO_FIELD=["qd", "via", "from", "pic", "nickname", "invite_code", "email"]

    private DBObject buildUser(HttpServletRequest req, String user_name, String mobile, String cid,
                               String pwd, String u_id, String mm_no){
        Object user_id = null
        if(StringUtils.isNotEmpty(u_id)){
            user_id = u_id
        }
        else{
            user_id = userKGS.nextId()
        }
        logger.debug("buildUser user_id : {}", user_id)
        DBObject info  = new BasicDBObject(_id, user_id)

        info.put('nickname',buildDefaultNickName())
        if(StringUtils.isNotEmpty(user_name)){
            info.put('user_name',user_name.toLowerCase())
        }
        if(StringUtils.isNotEmpty(mobile))
            info.put('mobile',mobile)
        if(StringUtils.isNotEmpty(cid))
            info.put('cid',cid)

        if(StringUtils.isNotEmpty(pwd)){
            String password = MD5.digest2HEX(pwd + user_id)
            info.put('pwd',password)
        }
        long time = System.currentTimeMillis()
        info.put('regTime',time)
        String token = generateToken(user_id as String)
        info.put('token',token)
        if(StringUtils.isEmpty(info['pic'] as String))
            info.put("pic", User.DEFAULT_PIC)
        if(StringUtils.isNotEmpty(mm_no))
            info.put('mm_no',mm_no)

        for(String field : USER_INFO_FIELD){
            if(StringUtils.isNotEmpty(req.getParameter(field)))
                info.put(field,req.getParameter(field))
        }
        if(StringUtils.isEmpty(info['via'] as String))
            info.put('via','local')
        try{
            if(users().count($$(_id: user_id)) == 0 &&
                    users().update($$(_id: user_id),info, true, false, writeConcern).getN() != 1){
                return null
            }
        }catch(Exception e){
            logger.error("register buildUser error :{}", e)
            return null
        }
        return info
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
        return users().count($$('user_name': userName.toLowerCase())) > 0
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