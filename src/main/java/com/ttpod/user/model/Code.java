package com.ttpod.user.model;

/**
 * date: 13-8-5 下午1:56
 *
 *
 */
public interface Code {


    Integer OK = 1;
    Integer ERROR = 0;

    Integer 余额不足 = 30412;
    Integer 权限不足 = 30413;
    Integer 验证码验证失败 = 30419;
    Integer 用户名或密码不正确 = 30402 ;
    Integer 参数无效 = 30406;
    Integer 用户名已存在 = 30408 ;
    Integer 用户名格式错误 = 30409 ;
    Integer 密码格式错误 = 30411;
    Integer 手机号格式错误 = 30432;
    Integer 短信验证码无效 = 30431;
    Integer 手机号码已存在 = 30433;
    Integer 短信验证间隔太短 = 30435;
    Integer 手机号码不存在 = 30436;
    Integer 短信验证码每日次数超过限制 = 30437;
    Integer 无效的修改密码链接 = 30438;
    Integer 邮箱已经存在 = 30414;
    Integer 邮箱格式错误 = 30415;

    //家族

    //好友


}