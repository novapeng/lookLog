package novayoung.log;

import org.slf4j.MDC;

import java.util.UUID;

/**
 *
 * Created by pengchangguo on 17/1/4.
 */
class TraceIdUtil {


    private static final String TRACE_ID = "traceId";

    private static final char[] finalChars = new char[]{'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z'};



    private TraceIdUtil() {

    }



    static String beginTrace() {

        String traceId = MDC.get(TRACE_ID);

        if (traceId != null && !"".equals(traceId)) {
            return traceId;
        }

        traceId = buildTraceId();
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    static void closeTrace() {
        MDC.remove(TRACE_ID);
    }

    static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    private static String buildTraceId() {
        return uuid(8);
    }

    private static String uuid(int length) {

        int pages = (4 * length + 32 - 1) / 32;

        String uuid = buildUUId(pages);

        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            chars[i] = finalChars[x % 0x3E];
        }
        return String.valueOf(chars);
    }

    private static String buildUUId(int pages) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pages; i++) {
            stringBuilder.append(UUID.randomUUID().toString().replaceAll("-", ""));
        }
        return stringBuilder.toString();
    }

}
