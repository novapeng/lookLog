package novayoung.log;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import sun.reflect.Reflection;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;

/**
 *
 * Created by pengchangguo on 16/12/23.
 */
@SuppressWarnings("restriction")
class TemplateUtil {

    private TemplateUtil() {
    }

    static {
        Velocity.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
        Velocity.setProperty(VelocityEngine.INTERPOLATE_STRINGLITERALS, "true");
        Velocity.init();
    }

    @SuppressWarnings("deprecation")
    static String render(String templateClassPath, Map<String, Object> params) {
        VelocityContext context = new VelocityContext();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        StringWriter writer = new StringWriter();
        try {
            Velocity.evaluate(context, writer, "", new InputStreamReader(Reflection.getCallerClass(2).getResourceAsStream(templateClassPath), "UTF-8"));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return writer.toString();
    }


}
