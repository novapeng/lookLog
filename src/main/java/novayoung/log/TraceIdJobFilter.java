package novayoung.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * <br/>
 *
 * Tracking On Job !
 *
 * <br/>
 *
 *
 * Example : <br/>
 *
 * <pre class="code">
 *
 *     public class SimpleJob {
 *
 *
 *         // attention please !! "@novayoung.log.TraceIdJobFilter.TraceId" is required !
 *         &#064;novayoung.log.TraceIdJobFilter.TraceId
 *         public void doJob() {
 *
 *             //job code ...
 *
 *         }
 *
 *
 *     }
 *
 *
 * </pre>
 *
 *
 *
 * Created by pengchangguo on 17/1/4.
 */
@Component
@Aspect
public class TraceIdJobFilter {


    @Around(value = "@annotation(novayoung.log.TraceIdJobFilter.TraceId)")
    public Object invocation(ProceedingJoinPoint joinPoint) throws Throwable {

        try {

            /**
             * Build TraceId !
             */

            TraceIdUtil.beginTrace();

            return joinPoint.proceed();

        } finally {

            /**
             * Clear TraceId !
             */

            TraceIdUtil.closeTrace();

        }
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface TraceId {

    }
}
