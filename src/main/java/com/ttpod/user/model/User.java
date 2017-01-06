package com.ttpod.user.model;


/**
 * 用户相关内容
 * date: 12-8-17 上午11:50
 *
 * @author: yangyang.cong@ttpod.com
 */
public interface User {


    /**
     *    喇叭,后台赠送,商店可买
     *
     */
    String horn = "horn";

    String access_token = "access_token" ;
    /**
     * 用户关注
     */
    String following = "following";
     /**
     * 用户粉丝
     */
    String followers = "followers";

    /**
     * 时间线。微博墙
     */
    String timeline = "timeline";

    /**
     * 默认头像
     */
    String DEFAULT_PIC = "http://img.sumeme.com/22/6/1403510731734.jpg";


    interface VIP {
        String vip  = "vip"; // 2...
        String vip_expires = "vip_expires"; // timestamp
        String vip_hiding = "vip_hiding"; // 0 1

        String vip_normal  = "vip_normal"; //1...
        String vip_expires_normal = "vip_expires_normal"; // timestamp
        String vip_hiding_normal = "vip_hiding_normal"; // 0 1

        Integer HIGH_LEVEL = 2;
        Integer NORMAL_LEVEL = 1;


        // 10 times  to shutup or forbid erverday.
        Integer MANAGE_LIMIT = 3;
    }


}
