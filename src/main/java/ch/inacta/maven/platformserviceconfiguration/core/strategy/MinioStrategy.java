package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;

/**
 * Strategy to handle MinIO specific configuration tasks.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class MinioStrategy {

    private final Plugin plugin;
    private final String bucket;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    MinioStrategy(final Plugin plugin) {

        this.plugin = plugin;
        this.bucket = plugin.getBucket();
    }

    /**
     * Creates a bucket and uploads files if configured.
     */
    void createBucketAndUploadFiles() throws MojoExecutionException {

        if (this.bucket.isEmpty()) {
            throw new MojoExecutionException("Tag 'bucket' has to be defined for configuring MinIO!");
        }

        final MinioClient minioClient = MinioClient.builder().endpoint(this.plugin.getEndpoint().toString())
                .credentials(this.plugin.getAuthorization().get(USERNAME), this.plugin.getAuthorization().get(PASSWORD)).build();

        try {

            createBucket(minioClient);

            uploadFiles(minioClient);

        } catch (final MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {

            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void createBucket(final MinioClient minioClient)
            throws InvalidKeyException, IOException, NoSuchAlgorithmException, MojoExecutionException, MinioException {

        final boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(this.bucket).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(this.bucket).build());
            this.plugin.getLog().info(format("Bucket created: [%s]", this.bucket));
        } else {
            throw new MojoExecutionException("Bucket " + this.bucket + " already exists.");
        }
    }

    private void uploadFiles(final MinioClient minioClient)
            throws InvalidKeyException, IOException, NoSuchAlgorithmException, MojoExecutionException, MinioException {

        for (final File file : this.plugin.getFilesToProcess()) {

            final String minioPath = getMinioPath(file);
            minioClient.uploadObject(UploadObjectArgs.builder().bucket(this.bucket).object(minioPath).filename(file.getAbsolutePath()).build());

            this.plugin.getLog().info(format("File successfully uploaded: [%s]", minioPath));
        }
    }

    private String getMinioPath(final File file) {

        // file will be uploaded to 'resource' path, or if not specified to bucket root
        return this.plugin.getResource().isBlank() ? file.getName() : join("/", this.plugin.getResource(), file.getName());
    }
}
