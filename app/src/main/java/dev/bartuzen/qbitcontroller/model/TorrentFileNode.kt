package dev.bartuzen.qbitcontroller.model

import java.util.ArrayDeque

data class TorrentFileNode(
    val name: String,
    val file: TorrentFile?,
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

    companion object {
        fun fromFileList(fileList: Collection<TorrentFile>): TorrentFileNode {
            val node = TorrentFileNode("", null, mutableListOf())

            fileList.forEach { file ->
                val pathList = file.name.split("/")
                var currentNode = node

                for ((i, pathItem) in pathList.withIndex()) {
                    if (i == pathList.lastIndex) {
                        currentNode.children?.add(TorrentFileNode(pathItem, file, null))
                    } else {
                        currentNode = currentNode.children?.find { it.name == pathItem } ?: currentNode.children.let {
                            val newFile = TorrentFileNode(pathItem, null, mutableListOf())
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
