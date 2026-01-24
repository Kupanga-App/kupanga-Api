package com.kupanga.api.minio.service.impl;

import com.kupanga.api.minio.service.MinioService;
import io.minio.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.kupanga.api.minio.constant.MinioConstant.URL_MINIO;

@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    public MinioServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void createBucketIfNotExists(String bucketName, boolean publicRead) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            if (publicRead) {
                String policyJson = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\"s3:GetObject\"],\n" +
                        "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policyJson)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du bucket MinIO: " + bucketName, e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file, String bucketName) {
        try {
            // Crée le bucket si nécessaire
            createBucketIfNotExists(bucketName, true);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return URL_MINIO + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de upload vers MinIO", e);
        }
    }
}
