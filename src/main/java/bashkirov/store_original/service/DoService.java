package bashkirov.store_original.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DoService {
    private final AmazonS3 amazonS3;

    @SneakyThrows
    public void saveFile(MultipartFile multipartFile, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getInputStream().available());

        if (multipartFile.getContentType() != null && !"".equals(multipartFile.getContentType())) {
            metadata.setContentType(multipartFile.getContentType());
        }
        amazonS3.putObject(new PutObjectRequest("", key, multipartFile.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead)
        );
    }
}
