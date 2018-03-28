package com.wawa.base;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.wawa.AppProperties;
import com.wawa.api.Web;
import com.wawa.base.persistent.KGS;
import com.wawa.base.support.ControllerSupport7;
import groovy.transform.CompileStatic;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.regex.Pattern;

import static com.wawa.common.util.MsgDigestUtil.MD5;

@CompileStatic
@Slf4j
public abstract class BaseController extends ControllerSupport7 {

    public static final String API_DOMAIN = AppProperties.get("api.domain");

    public static final String SITE_DOMAIN = AppProperties.get("site.domain");

    @Resource
    public MongoTemplate userMongo;

    @Resource
    public KGS userKGS;

    public void setUserKGS(KGS userKGS) {
        this.userKGS = userKGS;
    }

    public static final String SHOW_URL = AppProperties.get("site.domain");
    public static final StringRedisTemplate mainRedis = Web.mainRedis;

    static final Logger logs = LoggerFactory.getLogger(BaseController.class);
    @Resource
    public WriteConcern writeConcern;

    public DBCollection users() {
        return userMongo.getCollection("users");
    }

    public DBCollection pwd_logs() {
        return userMongo.getCollection("pwd_logs");
    }

    public DBCollection smscode_los() {
        return userMongo.getCollection("smscode_logs");
    }

    public static final Pattern VALID_USERNAME = Pattern.compile("^(?!^\\d+$)[0-9a-zA-Z_]{6,14}$");

    public static final Pattern VALID_PWD = Pattern.compile("^[\\w~!@#$%^&*()+`='{}<>,./\\?\\\\\\|\\[\\]\\-;:'\"]{6,20}$");

    public static final Pattern VALID_MOBILE = Pattern.compile("^(13[0-9]|14[0-9]|15[0-9]|17[0-9]|18[0-9])\\d{8}$");

    public static final Pattern VALID_EMAIL= Pattern.compile("^(\\w)+(\\.\\w+)*@(\\w)+((\\.\\w+)+)$");

    public static final BasicDBObject USER_FIELD = new BasicDBObject("token",1).append("pwd",1).append("mission",1);

    private static final String MM_KEY = "mm#&*0630";

    public static String generateToken(String content){
        return MD5.digest2HEX(MM_KEY + Base64.encodeBase64String(content.getBytes()) + System.currentTimeMillis() + RandomUtils.nextInt(999999999), true);
    }
}


