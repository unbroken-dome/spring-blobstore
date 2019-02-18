package org.unbrokendome.spring.blobstore

import org.springframework.core.NestedRuntimeException
import java.nio.file.Path


abstract class BlobStoreException(msg: String, cause: Throwable? = null) : NestedRuntimeException(msg, cause)


class BlobNotFoundException(path: Path, cause: Throwable? = null)
    : BlobStoreException("The blob $path does not exist in this BlobStore", cause)


class BlobAlreadyExistsException(path: Path, cause: Throwable? = null)
    : BlobStoreException("The blob $path already exists in this BlobStore", cause)
