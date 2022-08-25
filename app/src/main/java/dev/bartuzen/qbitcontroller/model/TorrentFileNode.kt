package dev.bartuzen.qbitcontroller.model

data class TorrentFileNode(
    val name: String,
    val children: MutableList<TorrentFileNode>?
) {
    val isFile get() = children == null

    val isFolder get() = children != null

    fun findChildNode(childList: Collection<String>): TorrentFileNode? {
        var currentNode = this
        childList.forEach { child ->
            currentNode = currentNode.children?.find {
                it.name == child
            } ?: return null
        }
        return currentNode
    }

    companion object {
        fun fromFileList(fileList: Collection<String>): TorrentFileNode {
            val node = TorrentFileNode("", mutableListOf())

            fileList.forEach { file ->
                val pathList = file.split("/")
                var currentNode = node

                for ((i, pathItem) in pathList.withIndex()) {
                    if (i == pathList.lastIndex) {
                        currentNode.children?.add(TorrentFileNode(pathItem, null))
                    } else {
                        currentNode = currentNode.children?.find {
                            it.name == pathItem
                        } ?: currentNode.children.let {
                            val newFile = TorrentFileNode(pathItem, mutableListOf())
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