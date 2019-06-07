package org.unbrokendome.spring.blobstore

import org.springframework.core.NestedRuntimeException
import java.nio.file.Path


/**
 * Base class for all blob store exceptions.
 */
abstract class BlobStoreException(
    msg: String,
    cause: Throwable? = null
) : NestedRuntimeException(msg, cause)


/**
 * Thrown when a blob was not found in the blob store.
 */
class BlobNotFoundException(
    path: Path,
    cause: Throwable? = null
) : BlobStoreException("The blob $path does not exist in this BlobStore", cause)


/**
 * Thrown when a blob already exists in the blob store and cannot be overwritten.
 */
class BlobAlreadyExistsException(path: Path, cause: Throwable? = null) :
    BlobStoreException("The blob $path already exists in this BlobStore", cause)


/**
 * Thrown for general errors when accessing the blob store. This is mostly used as a wrapper exception,
 * where the original [cause] is specific to the underlying blob store implementation.
 */
class GeneralBlobStoreException(
    msg: String,
    cause: Throwable
) : BlobStoreException(msg, cause)
