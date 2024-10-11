package bashkirov.store_original.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DoConfig {
    @Value("${do.key}")
    private String key;

    @Value("${do.secret}")
    private String secretKey;

    @Value("${do.endpoint}")
    private String endpoint;

    @Value("${do.region}")
    private String region;

    @Bean
    public AmazonS3 getS3() {
        BasicAWSCredentials creds = new BasicAWSCredentials(key, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
