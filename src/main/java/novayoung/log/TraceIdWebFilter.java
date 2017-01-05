package novayoung.log;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * Created by pengchangguo on 16/11/11.
 */
@Component
@WebFilter(
        urlPatterns = {"/*"},
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD}
)
public class TraceIdWebFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdWebFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	logger.info("TraceIdFilter Init...");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        try {

            /**
             *  Build TraceId And Put Into Log MDC
             */
            String traceId = TraceIdUtil.beginTrace();


            /**
             *  Do Next Filter Chain !
             */
            filterChain.doFilter(servletRequest, servletResponse);


            /**
             * Append TraceId In Response Header
             */
            if (StringUtils.isNotBlank(traceId)) {
            	HttpServletResponse response = (HttpServletResponse) servletResponse;
                response.addHeader("traceId", traceId);
            }
            
        } finally {

            /**
             *  Clear The TraceId In MDC
             */
            //noinspection ThrowFromFinallyBlock
            TraceIdUtil.closeTrace();

        }

    }


    @Override
    public void destroy() {
        logger.info("TraceFilter Destroy...");
    }



    public static String getTraceId() {
        return TraceIdUtil.getTraceId();
    }



}
