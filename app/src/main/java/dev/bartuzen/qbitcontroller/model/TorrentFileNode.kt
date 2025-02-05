package dev.bartuzen.qbitcontroller.model

data class TorrentFileNode(
    val name: String,
    val file: TorrentFile?,
    val separator: String,
    val children: MutableList<TorrentFileNode>?,
) {
    val isFile get() = children == null

    val isFolder get() = children != null

    fun findChildNode(childList: ArrayDeque<String>): TorrentFileNode? {
        var currentNode = this
        for (child in childList) {
            currentNode = currentNode.children?.find { it.name == child } ?: return null
        }
        return currentNode
    }

    fun getFolderProperties(): FolderProperties {
        var size = 0L
        var downloadedSize = 0L
        var priority: TorrentFilePriority? = null
        var isMixedPriority = false

        children?.forEach { node ->
            if (node.file != null) {
                if (node.file.priority != TorrentFilePriority.DO_NOT_DOWNLOAD) {
                    size += node.file.size
                    downloadedSize += (node.file.progress * node.file.size).toLong()
                }

                if (!isMixedPriority) {
                    if (priority == null) {
                        priority = node.file.priority
                    } else if (priority != node.file.priority) {
                        priority = null
                        isMixedPriority = true
                    }
                }
            } else {
                val properties = node.getFolderProperties()
                size += properties.size
                downloadedSize += properties.downloadedSize

                if (!isMixedPriority) {
                    if (properties.priority == null) {
                        priority = null
                        isMixedPriority = true
                    } else if (priority == null) {
                        priority = properties.priority
                    } else if (priority != properties.priority) {
                        priority = null
                        isMixedPriority = true
                    }
                }
            }
        }

        return FolderProperties(size, downloadedSize, priority)
    }

    companion object {
        fun fromFileList(fileList: Collection<TorrentFile>): TorrentFileNode {
            val separator = if (fileList.any { it.name.contains("/") }) "/" else "\\"

            val node = TorrentFileNode("", null, separator, mutableListOf())

            fileList.forEach { file ->
                val pathList = file.name.split(separator)
                var currentNode = node

                for ((i, pathItem) in pathList.withIndex()) {
                    if (i == pathList.lastIndex) {
                        currentNode.children?.add(TorrentFileNode(pathItem, file, separator, null))
                    } else {
                        currentNode = currentNode.children?.find { it.name == pathItem } ?: currentNode.children.let {
                            val newFile = TorrentFileNode(pathItem, null, separator, mutableListOf())
                            it?.add(newFile)
                            newFile
                        }
                    }
                }
            }
            return node
        }
    }
}

data class FolderProperties(
    val size: Long,
    val downloadedSize: Long,
    val priority: TorrentFilePriority?,
)
