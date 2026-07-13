package team2.goodsmap.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import team2.goodsmap.store.dto.request.PresignedUploadRequest;
import team2.goodsmap.store.dto.response.PresignedUploadResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_IMAGE_SIZE =
            10L * 1024 * 1024; // 10MB

    private static final Duration UPLOAD_URL_DURATION =
            Duration.ofMinutes(5);

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public PresignedUploadResponse createUploadUrl(
            Long storeId,
            Long storeGoodsId,
            PresignedUploadRequest request
    ) {
        validateRequest(request);

        String extension =
                resolveExtension(request.contentType());

        String objectKey = """
                stores/%d/goods/%d/images/%s.%s
                """.formatted(
                storeId,
                storeGoodsId,
                UUID.randomUUID(),
                extension
        ).trim();

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(request.contentType())
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(UPLOAD_URL_DURATION)
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest =
                s3Presigner.presignPutObject(presignRequest);

        Instant expiresAt =
                Instant.now().plus(UPLOAD_URL_DURATION);

        return new PresignedUploadResponse(
                presignedRequest.url().toString(),
                objectKey,
                expiresAt
        );
    }

    private void validateRequest(
            PresignedUploadRequest request
    ) {
        if (!ALLOWED_CONTENT_TYPES.contains(
                request.contentType()
        )) {
            throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다."
            );
        }

        if (request.fileSize() <= 0
                || request.fileSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "이미지는 10MB 이하만 업로드할 수 있습니다."
            );
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다."
            );
        };
    }
}