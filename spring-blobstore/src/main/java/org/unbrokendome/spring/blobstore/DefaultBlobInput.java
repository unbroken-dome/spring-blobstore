package org.unbrokendome.spring.blobstore;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalLong;


final class DefaultBlobInput implements BlobInput {

    @Nullable
    private final Long size;
    @Nonnull
    private final MimeType contentType;
    @Nonnull
    private final Publisher<DataBuffer> data;


    DefaultBlobInput(@Nonnull Publisher<DataBuffer> data, @Nullable Long size, @Nonnull MimeType contentType) {
        this.size = size;
        this.contentType = contentType;
        this.data = data;
    }


    @Override
    @Nonnull
    public Publisher<DataBuffer> getData() {
        return data;
    }


    @Override
    @Nonnull
    public OptionalLong getSize() {
        return size != null ? OptionalLong.of(size) : OptionalLong.empty();
    }


    @Override
    @Nonnull
    public MimeType getContentType() {
        return contentType;
    }
}
