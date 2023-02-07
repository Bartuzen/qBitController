package dev.bartuzen.qbitcontroller.model

import java.util.ArrayDeque

data class TorrentFileNode(
    val name: String,
    val file: TorrentFile?,
    val separator: String,
    val children: MutableList<TorrentFileNode>?
) {
    val isFile get() = children == null

    val isFolder get() = children != null

    fun findChildNode(childList: ArrayDeque<String>): TorrentFileNode? {
        var currentNode = this
        for (child in childList.descendingIterator()) {
            currentNode = currentNode.children?.find { it.name == child } ?: return null
        }
        return currentNode
    }

    fun getFolderProperties(): FolderProperties {
        var size = 0L
        var progressSum = 0.0
        var fileCount = 0
        var priority: TorrentFilePriority? = null
        var isMixedPriority = false

        children?.forEach { node ->
            if (node.file != null) {
                size += node.file.size
                progressSum += node.file.progress
                fileCount++

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
                progressSum += properties.progressSum
                fileCount += properties.fileCount

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

        return FolderProperties(size, progressSum, fileCount, priority)
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
    val progressSum: Double,
    val fileCount: Int,
    val priority: TorrentFilePriority?
)
