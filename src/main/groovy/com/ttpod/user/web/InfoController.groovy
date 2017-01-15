package com.ttpod.user.web

import com.mongodb.DBObject
import com.ttpod.rest.anno.Rest
import com.ttpod.user.model.Code
import com.ttpod.user.model.SmsCode
import com.ttpod.user.web.api.Web
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.servlet.http.HttpServletRequest

import static com.ttpod.rest.common.doc.MongoKey.$set
import static com.ttpod.rest.common.doc.MongoKey._id
import static com.ttpod.rest.common.util.MsgDigestUtil.MD5
import static com.ttpod.rest.common.util.WebUtils.$$;
/**
 * @author: jiao.li@ttpod.com
 * Date: 14-6-16 下午1:39
 */
@Rest
class InfoController extends BaseController {

    Logger logger = LoggerFactory.getLogger(InfoController.class)


    private final static String PRIV_KEY = "meme#*&07071zhibo";
    /**
     * 么么直播账号同步 加密
     * @param req
     * @return
     */
    def synNo(HttpServletRequest req) {
        //手机 用户名 靓号
        def id = req['tuid']
        def mm_no = req['mm_no']
        String sign = req['sign']

        if(StringUtils.isEmpty(id) || StringUtils.isEmpty(mm_no)){
            return [code: Code.参数无效]
        }

        def needSign = MD5.digest2HEX("${PRIV_KEY}${id}${mm_no}".toString())
        if(!sign.equals(needSign)){
            return [code : Code.ERROR]
        }
        def userId =  NumberUtils.isNumber(id.toString()) ? id as Integer : id as String
        def code = users().update($$(_id, userId),
                $$($set, $$('mm_no',mm_no as String)), false,false, writeConcern).getN()
        [code: code]
    }

    /**
     * 绑定手机号
     */
    def bindMobile(HttpServletRequest req){
        def id = req['tuid']
        def token = req['access_token']
        def mobile = req['mobile']
        def sms_code = req['sms_code']
        def pwd = req['pwd']
        def type = req['type']

        if(StringUtils.isBlank(mobile) || StringUtils.isBlank(sms_code) || StringUtils.isBlank(token) || StringUtils.isBlank(type)){
            return [code: Code.参数无效]
        }

        if(Integer.valueOf(type) != SmsCode.绑定手机号.ordinal()){
            return [code: Code.参数无效]
        }

        if(Web.smsCodeVeri(SmsCode.绑定手机号, req)){
            return [code : Code.短信验证码无效]
        }
        if(users().count($$(mobile: mobile)) > 0){
            return [code : Code.手机号码已存在]
        }
        def userId = null
        if(StringUtils.isNotBlank(id)){
            userId =  NumberUtils.isNumber(id.toString()) ? id as Integer : id as String
        }

        DBObject user = users().findOne($$('token':token),USER_FIELD)
        if(user == null)
            user = users().findOne($$(_id, userId),USER_FIELD)

        if(user == null)
            return [code : Code.参数无效]
        def updateInfo = $$('mobile': mobile)

        if(StringUtils.isNotEmpty(pwd)){
            if(!VALID_PWD.matcher(pwd).matches()){
                return [code: Code.密码格式错误]
            }
            String password = MD5.digest2HEX(pwd + user[_id] as String)
            updateInfo.append('pwd', password)
        }
        def uid = user.get(_id)
        uid =  NumberUtils.isNumber(uid.toString()) ? uid as Integer : uid as String
        if(users().update($$(_id, uid),
                $$($set:updateInfo), false, false , writeConcern).getN() == 1){
            return [code : Code.OK]
        }
        [code : Code.ERROR]

    }

    /**
     * 解除绑定手机号
     */
    def unbindMobile(HttpServletRequest req){
        def id = req['tuid']
        def token = req['access_token']
        def sms_code = req['sms_code']

        if(StringUtils.isEmpty(id) || StringUtils.isEmpty(sms_code) ||
                StringUtils.isEmpty(token)){
            return [code: Code.参数无效]
        }
        def userId = NumberUtils.isNumber(id.toString()) ? id as Integer : id as String
        DBObject user = users().findOne($$('token':token),$$(_id : 1, mobile:1))
        if(user == null)
            user = users().findOne($$(_id, userId),USER_FIELD)

        if(user == null)
            return [code : Code.参数无效]

        def mobile = user['mobile'] as String
        if(mobile == null){
            return [code : Code.手机号码不存在]
        }
        if(Web.smsCodeVeri(SmsCode.注册, sms_code, mobile)){
            return [code : Code.短信验证码无效]
        }

        def uid = user.get(_id)
        uid =  NumberUtils.isNumber(uid.toString()) ? uid as Integer : uid as String
        if(user != null){
            if(users().update($$(_id, uid),
                    $$($unset:$$(mobile:1)), false, false , writeConcern).getN() == 1){
                pwd_logs().insert($$(uid:userId, type:"unbindMobile", mobile:mobile, timestamp:System.currentTimeMillis()))
                return [code : Code.OK]
            }
        }
        [code : Code.ERROR]

    }

    /**
     * 绑定用户名
     */
    def bindUserName(HttpServletRequest req){
        def token = req['access_token']
        def userName = req['username']
        def pwd = req['pwd']
        if(StringUtils.isEmpty(userName) ||StringUtils.isEmpty(token)){
            return [code: Code.参数无效]
        }
        if(!VALID_USERNAME.matcher(userName).matches()){
            return [code: Code.用户名格式错误]
        }
        if(users().count($$(userName: userName.toLowerCase())) > 0){
            return [code : Code.用户名已存在]
        }
        DBObject user = users().findOne($$('token':token),USER_FIELD)
        if(user == null)
            return [code : Code.参数无效]

        def updateInfo = $$('userName': userName.toLowerCase())

        if(StringUtils.isNotEmpty(pwd)){
            if(!VALID_PWD.matcher(pwd).matches()){
                return [code: Code.密码格式错误]
            }
            String password = MD5.digest2HEX(pwd + user[_id] as String)
            updateInfo.append('pwd', password)
        }
        def uid = user.get(_id)
        uid =  NumberUtils.isNumber(uid.toString()) ? uid as Integer : uid as String
        if(users().update($$(_id, uid),
                $$($set:updateInfo), false, false , writeConcern).getN() == 1){
            return [code : Code.OK]
        }
        [code : Code.ERROR]
    }

    /**
     * 绑定邮箱
     */
    def bindUserEmail(HttpServletRequest req){
        def token = req['access_token']
        def email = req['email']
        def pwd = req['pwd']
        if(StringUtils.isEmpty(email) ||StringUtils.isEmpty(token)){
            return [code: Code.参数无效]
        }
        if(!VALID_EMAIL.matcher(email).matches()){
            return [code: Code.邮箱格式错误]
        }
        if(users().count($$(email : email)) > 0){
            return [code : Code.邮箱已经存在]
        }
        DBObject user = users().findOne($$('token':token),USER_FIELD)
        if(user == null)
            return [code : Code.参数无效]

        def updateInfo = $$('email': email)

        if(StringUtils.isNotEmpty(pwd)){
            if(!VALID_PWD.matcher(pwd).matches()){
                return [code: Code.密码格式错误]
            }
            String password = MD5.digest2HEX(pwd + user[_id] as String)
            updateInfo.append('pwd', password)
        }
        def uid = user.get(_id)
        uid =  NumberUtils.isNumber(uid.toString()) ? uid as Integer : uid as String
        if(users().update($$(_id, uid),
                $$($set:updateInfo), false, false , writeConcern).getN() == 1){
            return [code : Code.OK]
        }
        [code : Code.ERROR]
    }

    /**
     * 验证短信密码
     * @param req
     * @return
     */
    def verify_code(HttpServletRequest req) {
        Integer type = ServletRequestUtils.getIntParameter(req, "type", 1)
        def id = req['tuid']
        def token = req['access_token']
        def sms_code = req['sms_code']
        def userId = null
        if(StringUtils.isNotEmpty(id)){
            userId =  NumberUtils.isNumber(id.toString()) ? id as Integer : id as String
        }
        DBObject user = users().findOne($$('token':token),$$(mobile:1))
        if(user == null)
            user = users().findOne($$(_id, userId),$$(mobile:1))

        if(user == null)
            return [code : Code.参数无效]

        def mobile = user['mobile'] as String

        if(StringUtils.isEmpty(mobile)){
            return [code : Code.手机号码不存在]
        }
        /*if(!VALID_MOBILE.matcher(mobile).matches()){
            return [code: Code.手机号格式错误]
        }*/
        SmsCode smsType = SmsCode.注册
        if(type == SmsCode.找回密码.ordinal()){
            smsType = SmsCode.找回密码
        }else if(type == SmsCode.兑换柠檬.ordinal()){
            smsType = SmsCode.兑换柠檬
        }
        if(Web.smsCodeVeri(smsType, sms_code, mobile)){
            return [code : Code.短信验证码无效]
        }
        [code: Code.OK]
    }

}