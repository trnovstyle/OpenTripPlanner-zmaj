package org.opentripplanner.standalone.datastore.gcs;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Google Cloud Storage Data source (blob).
 */
class BlobPath {
    /**
     * GCS URL pattern for the Scheme Specific Part, without the 'gs:' prefix
     * Not all rules are validated here, but the following is:
     * <ul>
     *     <li>Bucket names must contain only lowercase letters, numbers, dashes (-),
     *     underscores (_), and dots (.)
     *     <li>Bucket names must contain 3 to 222 characters.
     *     <li>Object names must be at least one character.
     *     <li>Object names should avoid using control characters
     *     this is enforced here, and is strictly just a strong recommendation.
     * </ul>
     */
    private static final Pattern GS_URL_PATTERN = Pattern.compile(
            "//([\\p{Lower}\\d_.-]{3,222})/([^\\p{Cntrl}]+)"
    );
    /**
     * Bucket name
     */
    @NotNull
    final String bucket;

    /**
     * The name of the object inside the bucket.
     */
    @NotNull
    final String objectName;


    private BlobPath(String bucket, String objectName) {
        this.bucket = bucket;
        this.objectName = objectName;
    }

    static BlobPath parse(URI uri) {
        Matcher m = GS_URL_PATTERN.matcher(uri.getSchemeSpecificPart());

        if(m.matches()) {
            return new BlobPath(m.group(1), m.group(2));
        }
        throw new IllegalArgumentException(
                "The '" + uri + "' is not a legal Google Cloud Storage "
                        + "URL on format: 'gs://bucket-name/object-name'."
        );
    }

    String uri() {
        return "gs://" + bucket+ '/' + objectName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, objectName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        BlobPath gcsSource = (BlobPath) o;
        return bucket.equals(gcsSource.bucket) && objectName.equals(gcsSource.objectName);
    }

    @Override
    public String toString() {
        return uri();
    }
}
