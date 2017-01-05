package novayoung.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.mongodb.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/**
 * 缓存日志, 用于界面查看日志
 *
 * Created by pengchangguo on 16/11/23.
 */
public class LookLogAppender extends OutputStreamAppender<ILoggingEvent> {

    private static final String SPLIT = " \\~\\|--_--\\|\\~ ";

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        LookLogAppender.applicationContext = applicationContext;
    }

    @Override
    public void start() {

        setOutputStream(new BufferedOutputStream(new ByteArrayOutputStream()){

            @Override
            public void write(byte[] b) throws IOException {

                /**
                 * If Bytes Is Null Or Empty, Do Nothing !
                 */
                if (b == null || b.length == 0) {
                    return;
                }


                /**
                 * If Spring Is Not Initialized, Do Nothing !
                 *
                 */
                if (applicationContext == null) {
                    return;
                }


                /**
                 * If Handler Is Not Found In Spring Beans, Do Nothing !
                 */
                CacheLogHandler cacheLogHandler = applicationContext.getBean(CacheLogHandler.class);
                if (cacheLogHandler == null || !cacheLogHandler.lookLogConfig.isEnable()) {
                    return;
                }


                /**
                 * Invoke By Handler ...
                 */
                String formattedMessage = new String(b, "UTF-8");
                cacheLogHandler.handle(parse(formattedMessage));
            }
        });

        super.start();
    }


    /**
     * Parse The Log Message To LogDto
     *
     * @param formattedMessage
     *        Full logMessage, @see the pattern in logback.config.xml
     *
     * @return LogDto
     */
    private LogDto parse(String formattedMessage) {

        String[] arr = formattedMessage.split(SPLIT);

        if (arr.length != 8) {
            return null;
        }

        LogDto logDto = new LogDto();
        logDto.setFormattedMessage(StringUtils.join(arr, " "));
        logDto.setTime(arr[0]);
        logDto.setThread(arr[1]);
        logDto.setTraceId(arr[2]);
        logDto.setLogLevel(arr[3] == null ? null : arr[3].trim());
        logDto.setLoggerName(arr[4]);
        logDto.setLine(arr[5]);
        logDto.setMessage(arr[7]);
        return logDto;
    }


    private static class LogDto implements Serializable {

        private static final long serialVersionUID = 1703639553933543532L;

        private String formattedMessage;

        private String traceId;

        private String time;

        private String thread;

        private String logLevel;

        private String loggerName;

        private String line;

        private String message;

        private Date createTime = new Date();


        String getFormattedMessage() {
            return formattedMessage;
        }

        void setFormattedMessage(String formattedMessage) {
            this.formattedMessage = formattedMessage;
        }

        String getTraceId() {
            return traceId;
        }

        void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        @SuppressWarnings("unused")
		public String getTime() {
            return time;
        }

        void setTime(String time) {
            this.time = time;
        }

        @SuppressWarnings("unused")
		public String getThread() {
            return thread;
        }

        void setThread(String thread) {
            this.thread = thread;
        }

        @SuppressWarnings("unused")
		public String getLogLevel() {
            return logLevel;
        }

        void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        @SuppressWarnings("unused")
		public String getLoggerName() {
            return loggerName;
        }

        void setLoggerName(String loggerName) {
            this.loggerName = loggerName;
        }

        @SuppressWarnings("unused")
		public String getLine() {
            return line;
        }

        void setLine(String line) {
            this.line = line;
        }

        @SuppressWarnings("unused")
		public String getMessage() {
            return message;
        }

        void setMessage(String message) {
            this.message = message;
        }

        @SuppressWarnings("unused")
		public Date getCreateTime() {
            return createTime;
        }

        @SuppressWarnings("unused")
		public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
    }



    @Component
    public static class CacheLogHandler {

        private BlockingQueue<LogDto> queue;

        @Autowired
        private LookLogConfig lookLogConfig;


        @Autowired
        @Qualifier("mongoDbCacheOperator")
        private CacheOperator cacheOperator;


        @PostConstruct
        public void init() {

            /**
             * If UnEnable, Do Nothing !
             */
            if (disable()) {
                return;
            }


            /**
             * Initialize Buffer Queue
             */
            queue = new LinkedBlockingQueue<>(lookLogConfig.getQueueSize());


            /**
             * Initialize CacheOperator
             */
            cacheOperator.init();


            /**
             * Start A Thread To Listen Buffer Queue
             */
            listeningQueue();
        }

        private boolean disable() {
            return !lookLogConfig.isEnable() || !cacheOperator.enable();
        }


        @PreDestroy
        public void destroy() {
            if (cacheOperator != null) {
                cacheOperator.destroy();
            }
        }


        private void listeningQueue() {

            if (cacheOperator == null ) {
                return;
            }

            Thread thread = new Thread(){

                @Override
                public void run() {

                    //noinspection InfiniteLoopStatement
                    while (true) {

                        try {

                            /**
                             * Receive Log Message, Put Into Cache !
                             */
                            writeLog(queue.take());

                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();

                        } catch (Exception e) {

                            e.printStackTrace();

                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e1) {
                                Thread.currentThread().interrupt();
                            }

                        }

                    }

                }
            };

            thread.setName("LookLogAppender-Listen");

            thread.start();

        }


        List<String> getLogs(String traceId) {

            if (cacheOperator == null || disable()) {
                return new ArrayList<>();
            }

            return cacheOperator.getLogs(traceId);

        }

        List<String> getLogs(Map<String, Object> conditions, Integer order, Integer limit) {
            if (cacheOperator == null || disable()) {
                return new ArrayList<>();
            }

            return cacheOperator.getLogs(conditions, order, limit);
        }

        private void writeLog(LogDto logDto) {

            if (StringUtils.isBlank(logDto.getFormattedMessage()) ||
                    cacheOperator == null || disable()) {
                return;
            }

            cacheOperator.putLog(logDto);

        }

        private void handle(LogDto logDto) {


            /**
             * If UnEnable, Do Nothing !
             */
            if (disable()) {
                return;
            }

            if (logDto == null) {
                return;
            }


            /**
             * Put Log Message Into Queue, If Queue Is Full, Do Nothing !
             */
            queue.offer(logDto);

        }

    }


    interface CacheOperator {

        void init();

        boolean enable();

        void putLog(LogDto logDto);

        List<String> getLogs(String traceId);

        List<String> getLogs(Map<String, Object> conditions, Integer order, Integer limit);

        void destroy();

    }

    @Component("redisCacheOperator")
    public static class RedisCacheOperator implements CacheOperator {

        @Autowired
        private LookLogConfig lookLogConfig;

        private JedisPool jedisPool;

        @Override
        public void init() {

            if (StringUtils.isBlank(lookLogConfig.getRedisHost())) {
                return;
            }

            /**
             * Build JedisPool !
             */
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(lookLogConfig.getRedisPoolMaxActive());
            jedisPoolConfig.setMaxIdle(lookLogConfig.getRedisPoolMaxIdle());
            jedisPoolConfig.setMinIdle(lookLogConfig.getRedisPoolMinIdle());
            jedisPoolConfig.setMaxWaitMillis(lookLogConfig.getRedisPoolMaxWait());

            if (StringUtils.isBlank(lookLogConfig.getRedisPassword())) {
                jedisPool = new JedisPool(
                        jedisPoolConfig,
                        lookLogConfig.getRedisHost(),
                        lookLogConfig.getRedisPort(),
                        lookLogConfig.getRedisTimeout());

            } else {
                jedisPool = new JedisPool(
                        jedisPoolConfig,
                        lookLogConfig.getRedisHost(),
                        lookLogConfig.getRedisPort(),
                        lookLogConfig.getRedisTimeout(),
                        lookLogConfig.getRedisPassword());
            }



            /**
             * Test Jedis Connection !
             */
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                String key = "lookLogApender#OnlyTestKey123";
                jedis.set(key, "1");
                jedis.del(key);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }

        }

        @Override
        public boolean enable() {
            return StringUtils.isNotBlank(lookLogConfig.getRedisHost());
        }

        @Override
        public void putLog(LogDto logDto) {

            Jedis jedis = null;
            try {

                jedis = jedisPool.getResource();

                String key = lookLogConfig.getKeyPrefix() + logDto.getTraceId();

                Long len = jedis.rpush(key, logDto.getFormattedMessage());

                if (len == 1L && lookLogConfig.getCachedSecond() != null && lookLogConfig.getCachedSecond().intValue() > 0) {
                    jedis.expire(key, lookLogConfig.getCachedSecond().intValue());
                }
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }

        @Override
        public List<String> getLogs(String traceId) {
            Jedis jedis = null;

            try {

                jedis = jedisPool.getResource();

                String key = lookLogConfig.getKeyPrefix() + traceId;

                Long len = jedis.llen(key);

                if (len == null || len == 0) {
                    return new ArrayList<>();
                }

                return jedis.lrange(key, 0, -1);

            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }

        @Override
        public List<String> getLogs(Map<String, Object> conditions, Integer order, Integer limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroy() {
            if (jedisPool != null) {
                jedisPool.destroy();
            }
        }
    }

    @Component("mongoDbCacheOperator")
    public static class MongoDbCacheOperator implements CacheOperator {

        @Autowired
        private LookLogConfig lookLogConfig;

        @Autowired
        private MongoTemplate mongoTemplate;


        @Override
        public void init() {

            DBCollection collection = mongoTemplate.getCollection(lookLogConfig.getMongoDbCollectionName());

            if (lookLogConfig.getCachedSecond() != null && lookLogConfig.getCachedSecond() > 0) {

                List<DBObject> indexs = collection.getIndexInfo();

                String indexFiledName = "createTime";
                String indexName = "createTime_expire";

                for (DBObject dbObject : indexs) {
                    if (indexName.equals(dbObject.get("name"))) {
                        return;
                    }
                }

                BasicDBObject options = new BasicDBObject("name", indexName);
                options.append("expireAfterSeconds", lookLogConfig.getCachedSecond());

                collection.createIndex(new BasicDBObject(indexFiledName, 1), options);
            }
        }

        @Override
        public boolean enable() {
            return StringUtils.isNotBlank(lookLogConfig.getMongoDbCollectionName());
        }

        @Override
        public void putLog(LogDto logDto) {

            mongoTemplate.insert(logDto, lookLogConfig.getMongoDbCollectionName());

        }

        @Override
        public List<String> getLogs(String traceId) {


            List<String> list = new ArrayList<>();

            DBCollection collection = mongoTemplate.getCollection(lookLogConfig.getMongoDbCollectionName());

            DBCursor dbCursor;

            if (StringUtils.isNotBlank(traceId)) {

                Pattern pattern = Pattern.compile("^.*" + traceId + ".*$", Pattern.CASE_INSENSITIVE);

                dbCursor = collection.find(new BasicDBObject("traceId", pattern));

            } else {

                dbCursor = collection.find().sort(new BasicDBObject("createTime", -1));

            }

            dbCursor = dbCursor.limit(lookLogConfig.getLookLogMaxLimit());

            while (dbCursor.hasNext()) {
                DBObject dbObject = dbCursor.next();
                list.add((String) dbObject.get("formattedMessage"));
            }

            return list;
        }

        @Override
        public List<String> getLogs(Map<String, Object> conditions, Integer order, Integer limit) {

            String keyword = (String) conditions.get("keyword");
            String traceId = (String) conditions.get("traceId");
            Date startTime = (Date) conditions.get("startTime");
            Date endTime = (Date) conditions.get("endTime");
            String[] level = (String[]) conditions.get("level");

            List<String> list = new ArrayList<>();

            DBCollection collection = mongoTemplate.getCollection(lookLogConfig.getMongoDbCollectionName());


            BasicDBObject basicDBObject = new BasicDBObject();

            if (StringUtils.isNotBlank(traceId)) {
                basicDBObject.append("traceId", Pattern.compile(traceId));
            }

            if (StringUtils.isNotBlank(keyword)) {
                basicDBObject.append("formattedMessage", Pattern.compile(keyword));
            }

            String createTimeFiledName = "createTime";

            if (startTime != null) {
                basicDBObject.append(createTimeFiledName, new BasicDBObject(QueryOperators.GTE, startTime));
            }

            if (endTime != null) {
                basicDBObject.append(createTimeFiledName, new BasicDBObject(QueryOperators.LTE, endTime));
            }

            if (level != null && level.length > 0) {
                BasicDBList values = new BasicDBList();
                values.addAll(Arrays.asList(level));
                basicDBObject.append("logLevel", new BasicDBObject(QueryOperators.IN, values));
            }


            DBCursor dbCursor;

            if (basicDBObject.isEmpty()) {
                dbCursor = collection.find().sort(new BasicDBObject("createTime", order));
            } else {
                dbCursor = collection.find(basicDBObject).sort(new BasicDBObject("createTime", order));
            }

            dbCursor = dbCursor.limit(limit == null || limit <= 0 ? lookLogConfig.getLookLogMaxLimit() : limit);

            while (dbCursor.hasNext()) {
                DBObject dbObject = dbCursor.next();
                list.add((String) dbObject.get("formattedMessage"));
            }

            return list;
        }

        @Override
        public void destroy() {

            //Do Nothing !

        }
    }




}
