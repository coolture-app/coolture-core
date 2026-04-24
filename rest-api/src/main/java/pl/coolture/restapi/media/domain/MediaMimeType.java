
package pl.coolture.restapi.media.domain;

import java.util.Set;
import pl.coolture.restapi.common.exceptions.ForbiddenException;

/**
 * Allowed MIME types for user-uploaded media.
 */
public final class MediaMimeType {

    public static final Set<String> IMAGES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/avif"
    );

    public static final Set<String> VIDEOS = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
    );

    private static final Set<String> ALLOWED = union(IMAGES, VIDEOS);

    public static void validate(String mimeType) {
        if (!ALLOWED.contains(mimeType)) {
            throw new ForbiddenException(
                    "Unsupported media type '%s'. Allowed: images and videos.".formatted(mimeType));
        }
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        var result = new java.util.HashSet<>(a);
        result.addAll(b);
        return Set.copyOf(result);
    }

    private MediaMimeType() {}
}