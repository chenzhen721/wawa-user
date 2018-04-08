package com.wawa.user.web

import com.mongodb.DBObject
import com.wawa.base.BaseController
import com.wawa.base.anno.Rest
import com.wawa.common.util.AuthCode
import com.wawa.common.util.KeyUtils
import com.wawa.model.Code
import com.wawa.model.SmsCode
import com.wawa.api.Web
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.util.concurrent.TimeUnit

import static com.wawa.common.util.MsgDigestUtil.MD5
import static com.wawa.common.util.WebUtils.$$
import static com.wawa.common.doc.MongoKey._id

/**
 * @author: jiao.li@ttpod.com
 * Date: 14-6-16 下午1:39
 */
@Rest
class PwdController extends BaseController {

    Logger logger = LoggerFactory.getLogger(PwdController.class)

    /**
     * 找回密码
     * @param req
     * @return
     */
    def find(HttpServletRequest req) {
        logger.debug('Received find params is {}',req.getParameterMap())
        def mobile = req['mobile'] as String
        def sms_code = req['sms_code'] as String
        def pwd = req['pwd'] as String
        if(StringUtils.isBlank(mobile) || StringUtils.isBlank(sms_code) || StringUtils.isBlank(pwd)){
            return [code: Code.参数无效]
        }
        /*if(!VALID_MOBILE.matcher(mobile).matches()){
            return [code: Code.手机号格式错误]
        }*/
        if(Web.smsCodeVeri(SmsCode.找回密码, req)){
            return [code : Code.短信验证码无效]
        }
        DBObject user = users().findOne($$('mobile':mobile),USER_FIELD)
        if(user == null)
            return [code : Code.手机号码不存在]

        String id = user.get(_id)
        String old_token = user.get("token")
        String new_password = MD5.digest2HEX(pwd + id)
        String newToken = generateToken(pwd + id)
        def uid =  NumberUtils.isNumber(id.toString()) ? id as Integer : id as String
        def query = $$('_id',uid)
        def update_query = $$($set:$$('pwd': new_password, token:newToken))
        if(users().update(query,update_query).getN() == 1){
            def insert_query = $$('uid':uid,'type':'find','timestamp':System.currentTimeMillis())
            pwd_logs().insert(insert_query)
            return [code : Code.OK, data: [token:newToken, old_token:old_token]]
        }

        [code: Code.ERROR]
    }

    /**
     * 修改密码
     * @param req
     */
    def change(HttpServletRequest req) {
        def token = req['access_token'] as String
        def oldpwd = req['oldpwd'] as String
        def newpwd = req['newpwd'] as String

        if(StringUtils.isEmpty(token) || StringUtils.isEmpty(oldpwd) ||
                StringUtils.isEmpty(newpwd)){
            return [code: Code.参数无效]
        }
        if(oldpwd.equals(newpwd)){
            return [code: Code.参数无效]
        }
        if(!VALID_PWD.matcher(newpwd).matches()){
            return [code: Code.密码格式错误]
        }

        DBObject user = users().findOne($$('token':token),USER_FIELD)
        if(user == null)
            return [code : Code.用户名或密码不正确]

        String password = user.get('pwd')
        String id = user.get(_id)
        if(password == null || !MD5.digest2HEX(oldpwd + id).equals(password))
            return [code:Code.用户名或密码不正确]

        String new_password = MD5.digest2HEX(newpwd + id)
        String newToken = generateToken(newpwd + id)
        def userId =  NumberUtils.isNumber(id.toString()) ? id as Integer : id as String
        if(users().update($$(_id, userId),
                $$($set:$$('pwd': new_password, token:newToken)), false, false , writeConcern).getN() == 1){
            pwd_logs().insert($$(uid:userId, type:"change", timestamp:System.currentTimeMillis()))
            return [code : Code.OK, data: [token:newToken]]
        }
        [code : Code.ERROR]
    }

    /**
     * 用户重置密码
     * @param req
     * @return
     */
    def reset(HttpServletRequest req) {
        def mm_no = req['uid'] as String
        def newpwd = req['pwd'] as String
        def sign = req['s'] as String

        if(StringUtils.isEmpty(mm_no) || StringUtils.isEmpty(newpwd) ||
                StringUtils.isEmpty(sign)){
            return [code: Code.参数无效]
        }

        def key = KeyUtils.AUTHCODE.changePwd(mm_no)
        def timestamp = mainRedis.opsForValue().get(key);

        if(timestamp == null){
            return [code : Code.无效的修改密码链接]
        }

        def needSign = MD5.digest2HEX("${mm_no}${PRIV_KEY}${timestamp}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.无效的修改密码链接]
        }

        if(!VALID_PWD.matcher(newpwd).matches()){
            return [code: Code.密码格式错误]
        }

        DBObject user = users().findOne($$('mm_no':mm_no),USER_FIELD)
        if(user != null){
            def userId = user[_id]
            userId =  NumberUtils.isNumber(userId.toString()) ? userId as Integer : userId as String
            String password = MD5.digest2HEX(newpwd + userId)
            String newToken = generateToken(newpwd + userId)
            if(users().update($$(_id, userId),
                    $$($set:$$('pwd': password, token:newToken)), false, false , writeConcern).getN() == 1){
                pwd_logs().insert($$(uid:userId, type:"reset", timestamp:System.currentTimeMillis()))
                mainRedis.delete(key) //删除重置密码时间戳
                return [code : Code.OK]
            }
        }

        return [code : Code.无效的修改密码链接]
    }



    //TODO 提供接口给管理后台使用 ==============================

    private final static String PRIV_KEY = "meme#*&07071zhibo";
    private static final String RAN_TOKEN_SEED = "#@#meme${new Date().format("yMMdd")}%xi>YY".toString()

    def refresh_token(HttpServletRequest req) {

        def userId = req[_id]
        userId =  NumberUtils.isNumber(userId.toString()) ? userId as Integer : userId as String

        String sign = req['sign']

        def needSign = MD5.digest2HEX("${PRIV_KEY}&userId=${userId}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        DBObject user = users().findOne($$(_id:userId),USER_FIELD)
        if(user != null){
            String old_token = user.get('token')

            String newToken = generateToken(RAN_TOKEN_SEED + old_token + userId);
            if(users().update($$(_id, userId),
                    $$($set:$$(token:newToken)), false, false , writeConcern).getN() == 1){
                return [code : Code.OK, data: [token:newToken, old_token:old_token]]
            }
        }
        [code : Code.ERROR]
    }

    /**
     * 客服通过ID获得token
     */
    def token_by_id(HttpServletRequest req){
        def id = req[_id] as String
        Integer userId = null
        String qd_userId = id
        if(id.isNumber())
            userId = ServletRequestUtils.getIntParameter(req, _id)
        String sign = req['sign']

        def needSign = MD5.digest2HEX("${PRIV_KEY}&userId=${id}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        DBObject user = null
        if(userId)
            user = users().findOne($$(_id:userId),USER_FIELD)
        if(user == null)
            user = users().findOne($$(_id:qd_userId),USER_FIELD)

        String old_token = user.get('token')

        return [code : Code.OK, data: [token:old_token]]
    }

    /**
     * 解绑手机号
     * @param req
     */
    def unbind_mobile(HttpServletRequest req){
        def userId = req[_id]
        userId =  NumberUtils.isNumber(userId.toString()) ? userId as Integer : userId as String
        String sign = req['sign']

        def needSign = MD5.digest2HEX("${PRIV_KEY}&userId=${userId}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        DBObject user = users().findOne($$(_id:userId),$$(_id : 1, mobile:1))
        if(user != null){
            if(users().update($$(_id, userId),
                    $$($unset:$$(mobile:1)), false, false , writeConcern).getN() == 1){
                def mobile = user['mobile'] as String
                pwd_logs().insert($$(uid:userId, type:"admin_unbindMobile", mobile:mobile, timestamp:System.currentTimeMillis()))
                return [code : Code.OK]
            }
        }

        return [code : Code.ERROR]
    }

    /**
     * 发送手机验证码
     * @param req
     * @return
     */
    def get_code_by_mobile(HttpServletRequest req) {
        /*TODO 暂时关闭获取验证码
        String mobile = req['mobile']
        String sign = req['sign']
        Integer type = ServletRequestUtils.getIntParameter(req, "type", 1)
        def needSign = MD5.digest2HEX("${PRIV_KEY}&mobile=${mobile}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        [code: Code.OK, data:[auth_code: authCodeController.getAuthCodeByMobile(mobile, type)]]
        */
    }

    /**
     * 客服重置用户密码
     * @param req
     * @return
     */
    def reset_pwd(HttpServletRequest req) {
        def userId = req[_id]
        userId =  NumberUtils.isNumber(userId.toString()) ? userId as Integer : userId as String
        String sign = req['sign']

        def needSign = MD5.digest2HEX("${PRIV_KEY}&userId=${userId}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        DBObject user = users().findOne($$(_id:userId),USER_FIELD)
        if(user != null){
            def pwd = AuthCode.random(8).toLowerCase()
            String password = MD5.digest2HEX(pwd + userId)
            String newToken = generateToken(pwd + userId)
            if(users().update($$(_id, userId),
                    $$($set:$$('pwd': password, token:newToken)), false, false , writeConcern).getN() == 1){
                pwd_logs().insert($$(uid:userId, type:"admin_reset_pwd", password:password, timestamp:System.currentTimeMillis()))
                return [code : Code.OK, data: [pwd:pwd,token:newToken]]
            }
        }

        return [code : Code.ERROR]
    }


    private static final Long RESET_PWD_EXPIRES= 30 * 60L
    /**
     * 生成重置密码链接
     * @param req
     * @return
     */
    def generate_pwd_url(HttpServletRequest req){
        def userId = req[_id]
        userId =  NumberUtils.isNumber(userId.toString()) ? userId as Integer : userId as String
        String sign = req['sign']

        def needSign = MD5.digest2HEX("${PRIV_KEY}&userId=${userId}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        DBObject user = users().findOne($$(_id:userId),USER_FIELD.append('mm_no',1))
        if(user != null){
            def mm_no = user.get('mm_no')
            def key = KeyUtils.AUTHCODE.changePwd(mm_no)
            def timestamp = System.currentTimeMillis().toString()
            mainRedis.opsForValue().set(key, timestamp, RESET_PWD_EXPIRES, TimeUnit.SECONDS);
            def s = MD5.digest2HEX("${mm_no}${PRIV_KEY}${timestamp}".toString())
            return [code : Code.OK, data: [url:"?uid=${mm_no}&s=${s}".toString()]]
        }
        return [code : Code.ERROR]
    }
}