package bashkirov.store_original.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class GptConfig {
    @Value("${gpt.url}")
    private String url;
    @Value("${gpt.version}")
    private String version;
    @Value("${gpt.secretKey}")
    private String secretKey;
}
