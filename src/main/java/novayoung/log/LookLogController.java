package novayoung.log;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Lookup The Log.
 *
 * Created by pengchangguo on 16/11/11.
 */
@RestController
@RequestMapping("/lookLog")
public class LookLogController {


    private static final Logger logger = LoggerFactory.getLogger(LookLogController.class);

	@Autowired
    private LookLogAppender.CacheLogHandler cacheLogHandler;

    @Autowired
    private LookLogConfig lookLogConfig;


    @RequestMapping()
    public String index(
                        @RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "startTime", required = false) String startTime,
                        @RequestParam(value = "endTime", required = false) String endTime,
                        @RequestParam(value = "traceId", required = false) String traceId,
                        @RequestParam(value = "limit", required = false) Integer limit,
                        @RequestParam(value = "order", required = false) Integer order,
                        @RequestParam(value = "level", required = false) String[] level
    ) throws ParseException {

        Date startDateTime = StringUtils.isBlank(startTime) ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startTime);
        Date endDateTime   = StringUtils.isBlank(endTime) ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endTime);

        Integer limitParam = limit;
        if (limitParam == null) {
            limitParam = 200;
        }

        if (limitParam > lookLogConfig.getLookLogMaxLimit()) {
            limitParam = lookLogConfig.getLookLogMaxLimit();
        }

        Integer orderParam = order;
        if (order == null || (orderParam != -1 && orderParam != 1)) {
            orderParam = -1;
        }

        List<String> contentList = getLogs(traceId, keyword, startDateTime, endDateTime, level, orderParam, limitParam);

        Map<String, Object> renderArgs = new HashMap<>();
        renderArgs.put("keyword", keyword);
        renderArgs.put("startTime", startTime);
        renderArgs.put("endTime", endTime);
        renderArgs.put("level", level == null ? new ArrayList<>() : Arrays.asList(level));
        renderArgs.put("traceId", traceId);
        renderArgs.put("content", StringUtils.join(contentList, "\r\n"));
        renderArgs.put("size", contentList.size());
        renderArgs.put("order", orderParam);
        renderArgs.put("limit", limitParam);

        return TemplateUtil.render("ui.html", renderArgs);

    }


    private List<String> getLogs(String traceId, String keyword, Date startTime, Date endTime, String[] level, Integer order, Integer limit) {
        try {

            Map<String, Object> map = new HashMap<>();
            map.put("traceId", traceId);
            map.put("keyword", keyword);
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("level", level);

            return cacheLogHandler.getLogs(map, order, limit);

        } catch (Exception e) {

            logger.warn("查询日志失败", e);

            throw e;

        }
    }

}
