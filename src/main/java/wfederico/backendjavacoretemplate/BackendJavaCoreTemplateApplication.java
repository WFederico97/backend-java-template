package wfederico.backendjavacoretemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(excludeName = {
        "org.springframework.boot.actuate.autoconfigure.metrics.export.datadog.DatadogMetricsExportAutoConfiguration"
})
@EnableJpaAuditing
public class BackendJavaCoreTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendJavaCoreTemplateApplication.class, args);
    }

}
