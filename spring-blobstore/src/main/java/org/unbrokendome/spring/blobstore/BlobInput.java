package org.unbrokendome.spring.blobstore;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalLong;


/**
 * Provides input for creating a Blob.
 */
public interface BlobInput {

    /**
     * Gets the blob data as a {@link Publisher} of {@link DataBuffer}s.
     *
     * @return the blob data
     */
    @Nonnull
    Publisher<DataBuffer> getData();


    /**
     * Gets the size of the blob {@link #getData data} in bytes, if known. May be an empty {@link OptionalLong} if the
     * size is not known in advance.
     *
     * @return the size in bytes as an {@link OptionalLong} (may be empty)
     */
    @Nonnull
    OptionalLong getSize();


    /**
     * Gets the content type of the blob data.
     *
     * @return the content type as a {@link MimeType}
     */
    @Nonnull
    MimeType getContentType();


    /**
     * Creates a new {@code BlobInput}.
     *
     * @param data        the blob data as a {@link Publisher} of {@link DataBuffer}s
     * @param size        the size of the blob {@code data} in bytes, or {@code null} if not known
     * @param contentType the content type of the blob data
     * @return a {@link BlobInput}
     */
    static BlobInput create(
            Publisher<DataBuffer> data,
            @Nullable Long size,
            MimeType contentType
    ) {
        return new DefaultBlobInput(data, size, contentType);
    }


    /**
     * Creates a new {@code BlobInput}.
     * <p>
     * The returned {@code BlobInput} will have {@code application/octet-stream} as its content type.
     *
     * @param data the blob data as a {@link Publisher} of {@link DataBuffer}s
     * @param size the size of the blob {@code data} in bytes, or {@code null} if not known
     * @return a {@link BlobInput}
     */
    static BlobInput create(
            Publisher<DataBuffer> data,
            @Nullable Long size
    ) {
        return new DefaultBlobInput(data, size, MimeType.valueOf("application/octet-stream"));
    }


    /**
     * Creates a new {@code BlobInput}.
     *
     * @param data        the blob data as a {@link Publisher} of {@link DataBuffer}s
     * @param size        the size of the blob {@code data} in bytes, or an empty {@code OptionalLong} if not known
     * @param contentType the content type of the blob data
     * @return a {@link BlobInput}
     */
    static BlobInput create(
            Publisher<DataBuffer> data,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalLong size,
            MimeType contentType
    ) {
        return create(data, size.isPresent() ? size.getAsLong() : null, contentType);
    }


    /**
     * Creates a new {@code BlobInput}.
     * <p>
     * The returned {@code BlobInput} will have {@code application/octet-stream} as its content type.
     *
     * @param data the blob data as a {@link Publisher} of {@link DataBuffer}s
     * @param size the size of the blob {@code data} in bytes, or an empty {@code OptionalLong} if not known
     * @return a {@link BlobInput}
     */
    static BlobInput create(
            Publisher<DataBuffer> data,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalLong size
    ) {
        return create(data, size.isPresent() ? size.getAsLong() : null);
    }
}
