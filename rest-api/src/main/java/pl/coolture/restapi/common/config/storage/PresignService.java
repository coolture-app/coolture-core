package pl.coolture.restapi.common.config.storage;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Generates presigned S3 URLs for client-side uploads and media access.
 * Both URL types use the public endpoint
 *
 * TODO: Cache presigned GET URLs server-side using
 *  Caffeine for example with a TTL
 *  slightly shorter than GET_EXPIRY to avoid
 *  re-signing the same object on every request.
 *  Key: mediaId, value: presigned URL string.
 */
@Service
@RequiredArgsConstructor
public class PresignService {

    /** Client has this much time to PUT the file after receiving the upload URL. */
    private static final Duration PUT_EXPIRY = Duration.ofMinutes(15);

    /** How long a presigned GET URL stays valid. */
    private static final Duration GET_EXPIRY = Duration.ofHours(1);

    private final S3Presigner presigner;
    private final S3Properties props;

    /**
     * Presigned PUT URL handed to the client for direct upload.
     * The Content-Type header is locked into the signature so the client
     * cannot upload a different MIME type without invalidating it.
     */
    public PresignedPutObjectRequest presignPut(String objectKey, String mimeType, long sizeBytes) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(props.getBucketName())
                .key(objectKey)
                .contentType(mimeType)
                .contentLength(sizeBytes)
                .build();

        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(PUT_EXPIRY)
                .putObjectRequest(putRequest)
                .build());
    }

    /**
     * Presigned GET URL for reading a stored object.
     * Returned in every MediaResourceDto so clients can display media
     * directly from S3 without routing through the API.
     */
    public PresignedGetObjectRequest presignGet(String objectKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(props.getBucketName())
                .key(objectKey)
                .build();

        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(GET_EXPIRY)
                .getObjectRequest(getRequest)
                .build());
    }
}