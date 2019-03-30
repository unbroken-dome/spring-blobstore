package org.unbrokendome.spring.blobstore.filesystem

class FileSystemBlobStoreProperties {

    var basePath: String? = null

    var digestAlgorithm: String = "SHA-256"

    var bufferSize: Int = 4096
}
