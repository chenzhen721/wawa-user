package com.ttpod.user.persistent;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.BitSet;

import static com.ttpod.rest.common.util.WebUtils.$$;

/**
 *
 * 获取数据库里面创建好的 靓号
 *
 * date: 13-3-11 上午11:55
 *
 * @author: yangyang.cong@ttpod.com
 */
public class PrettyBitSetFactoryBean extends AbstractFactoryBean<BitSet> {
    static final Logger logger = LoggerFactory.getLogger(PrettyBitSetFactoryBean.class);
    public Class<?> getObjectType() {
        return BitSet.class;
    }

    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private MongoTemplate mongoTemplate;

    protected BitSet createInstance() throws Exception {
        BitSet set = new BitSet(10000000);

        DBCursor cur = mongoTemplate.getCollection("pretty1000").find(new BasicDBObject("_id", new BasicDBObject("$gte", 11891553))).batchSize(100000);
        while (cur.hasNext()){
            Integer id = (Integer) cur.next().get("_id");
           // logger.info("id---------->:"+id);
            set.set(id);
        }
        cur.close();
        return set;
    }
}
