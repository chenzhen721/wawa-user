package com.ttpod.user.web

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.ttpod.rest.AppProperties
import com.ttpod.rest.anno.Rest
import com.ttpod.user.common.util.AuthCode
import com.ttpod.user.common.util.KeyUtils
import com.ttpod.user.model.Code
import com.ttpod.user.model.SmsCode
import com.ttpod.user.model.User
import com.ttpod.user.web.BaseController
import com.ttpod.user.web.api.Web
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.*
import static com.ttpod.rest.common.doc.MongoKey.*;
/**
 * @author: jiao.li@ttpod.com
 * Date: 14-6-16 下午1:39
 */
@Rest
class RegisterController extends BaseController {

    Logger logger = LoggerFactory.getLogger(RegisterController.class)

    static final boolean isTest = API_DOMAIN.contains("test.")

    private static final String TMPUSER_KEY= "meme#@&tempavh2014"

    static final String[] USER_INFO_FIELD=["qd", "via", "from", "pic", "nickname", "invite_code", "email"]
    /**
     * 用户名注册
     * @param req
     * @return
     */
    def uname(HttpServletRequest req) {
        //Long costTime = registCostTime(req);

/* TODO 2016/9/8 关闭用户注册接口
if(!Web.codeVeri(req)){
            return [code: Code.验证码验证失败]
        }
        def username = req['username']
        def pwd = req['pwd']
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(pwd)){
            return [code: Code.参数无效]
        }
        if(!VALID_USERNAME.matcher(username).matches()){
            return [code: Code.用户名格式错误]
        }
        if(!VALID_PWD.matcher(pwd).matches()){
            return [code: Code.密码格式错误]
        }
        //用户名是否已经存在
        if(userNameExist(username)){
            return [code: Code.用户名已存在]
        }
        DBObject user = buildUser(req, username, null, null, pwd,null,null,null)
        if(user == null)
            return [code: Code.ERROR]
        //logger.info("register user tuid : {} cost time :{}", user[_id], costTime.toString())
        [code: Code.OK, data: [token:user['token']]]
*/
        return [code: Code.ERROR]
    }

    private Long registCostTime(HttpServletRequest req){
        String auth_key = req.getParameter("auth_key");

        String key = KeyUtils.AUTHCODE.register(auth_key);
        String red_code = mainRedis.opsForValue().get(key);
        //注册消耗时间
        String cost_time_key = "register:costtime:"+red_code;
        String cost_time = mainRedis.opsForValue().get(cost_time_key);
        if(StringUtils.isNotBlank(cost_time))
            return System.currentTimeMillis() - Long.valueOf(cost_time);
        return 0;
    }

    def robot(HttpServletRequest req) {
        if(isTest){
            def username = req['username']
            def pwd = req['pwd']
            if(StringUtils.isEmpty(username) || StringUtils.isEmpty(pwd)){
                return [code: Code.参数无效]
            }
            //用户名是否已经存在
            if(userNameExist(username)){
                return [code: Code.用户名已存在, error:'用户名已存在']
            }
            DBObject user = buildUser(req, username, null, null, pwd,null,null,null)
            if(user == null)
                return [code: Code.ERROR]

            [code: Code.OK, data: [access_token:user['token'], _id:user[_id]]]
        }

    }

    /**
     * 手机号码注册
     * @param req
     * @return
     */
    def mobile(HttpServletRequest req) {
        def mobile = req['mobile']
        def email = req['email']
        def sms_code = req['sms_code']
        def pwd = req['pwd']
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(sms_code)
                || StringUtils.isEmpty(pwd)){
            return [code: Code.参数无效]
        }
        /*
        if(!VALID_MOBILE.matcher(mobile).matches()){
            return [code: Code.手机号格式错误]
        }*/
        if(Web.smsCodeVeri(SmsCode.注册, req)){
            return [code : Code.短信验证码无效]
        }
        if(!VALID_PWD.matcher(pwd).matches()){
            return [code: Code.密码格式错误]
        }
        if(StringUtils.isNotEmpty(email) ){
            if(!VALID_EMAIL.matcher(email).matches()){
                return [code: Code.邮箱格式错误]
            }
            //邮箱是否存在
            if(emailExist(email)){
                return [code: Code.邮箱已经存在]
            }
        }
        //手机号码是否已经存在
        if(mobileExist(mobile)){
            return [code: Code.手机号码已存在]
        }
        DBObject user = buildUser(req, null, mobile, null, pwd, null,null,null)
        if(user == null)
            return [code: Code.ERROR]

        [code: Code.OK, data: [token:user['token']]]
    }

    /**
     * 创建联运用户账号
     * @param req
     */
    def union_user(HttpServletRequest req){
        //联运用户ID
        def u_id = req['u_id']
        def username = req['username']
        def pwd = req['pwd']
        def mm_no = req['mm_no']
        if(StringUtils.isEmpty(u_id) || StringUtils.isEmpty(username)
                || StringUtils.isEmpty(mm_no) || StringUtils.isEmpty(pwd)){
            return [code: Code.参数无效]
        }
        if(!VALID_USERNAME.matcher(username).matches()){
            return [code: Code.用户名格式错误]
        }
        if(!VALID_PWD.matcher(pwd).matches()){
            return [code: Code.密码格式错误]
        }
        //用户名是否已经存在
        if(userNameExist(username)){
            return [code: Code.用户名已存在]
        }
        DBObject user = buildUser(req, username, null, null, pwd, u_id,mm_no,null)
        if(user == null)
            return [code: Code.ERROR]

        [code: Code.OK, data: [token:user['token']]]
    }

    /**
     * 自动创建临时用户
     * @param req
     */
    def tmpUser(HttpServletRequest req){
        //设备号
        def cid = req['cid']
        def sign = req['sign']
        if(StringUtils.isEmpty(cid) || StringUtils.isEmpty(sign)){
            return [code: Code.参数无效]
        }
        logger.debug("tmpUser {}",MD5.digest2HEX(cid+TMPUSER_KEY))
        if(!sign.equals(MD5.digest2HEX(cid+TMPUSER_KEY))){
            return [code: Code.参数无效]
        }
        DBObject user = users().findOne($$('cid':cid),USER_FIELD)
        if(user == null)
            user =buildUser(req, null, null, cid, cid, null,null,null)

        [code: Code.OK, data: [token:user['token']]]
    }


    private DBObject buildUser(HttpServletRequest req, String userName, String mobile, String cid,
                               String pwd, String u_id, String mm_no, Long costTime){
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
        if(StringUtils.isNotEmpty(userName)){
            info.put('userName',userName.toLowerCase())
            //info.put('nickname',userName.toLowerCase())
        }
        if(StringUtils.isNotEmpty(mobile))
            info.put('mobile',mobile)
        if(StringUtils.isNotEmpty(cid))
            info.put('cid',cid)

        String password = MD5.digest2HEX(pwd + user_id)
        long time = System.currentTimeMillis()
        info.put('regTime',time)
        info.put('pwd',password)
        String token = generateToken(password + user_id)
        info.put('token',token)
        if(costTime != null){
            info.put("costTime", costTime)
        }
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

    private final static String NICK_NAME_PREFIX = "萌新"
    public static buildDefaultNickName(){
        return NICK_NAME_PREFIX + AuthCode.randomNumber(6);
    }

    def checkname(HttpServletRequest req) {

        String name = req['userName']
        if(StringUtils.isEmpty(name))
            return [code : Code.参数无效]
        [code : userNameExist(name) ? Code.用户名已存在 : 1]
    }

    def checkmobile(HttpServletRequest req) {
        String mobile = req['mobile']
        if(StringUtils.isEmpty(mobile))
            return [code : Code.参数无效]
        [code : mobileExist(mobile) ? Code.手机号码已存在 : 1]
    }

    def checkemail(HttpServletRequest req) {
        String email = req['email']
        if(StringUtils.isEmpty(email))
            return [code : Code.参数无效]
        [code : emailExist(email) ? Code.邮箱已经存在 : 1]
    }

    public boolean userNameExist(String userName){
        return users().count($$(userName: userName.toLowerCase())) > 0
    }

    public boolean mobileExist(String mobile){
        return users().count($$(mobile: mobile)) > 0
    }
    public boolean emailExist(String email){
        return users().count($$(email: email)) > 0
    }
}