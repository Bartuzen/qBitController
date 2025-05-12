package dev.bartuzen.qbitcontroller.model

sealed class TorrentFileNode(
    open val name: String,
    open val separator: String,
    open val level: Int,
    open val path: String,
    open val priority: TorrentFilePriority?,
    open val size: Long,
    open val downloadedSize: Long,
    open val progress: Double,
) : Comparable<TorrentFileNode> {
    override fun compareTo(other: TorrentFileNode): Int {
        if (this is Folder && other is File) return -1
        if (this is File && other is Folder) return 1

        return this.name.compareTo(other.name, ignoreCase = true)
    }

    data class File(
        private val file: TorrentFile,
        override val name: String,
        override val separator: String,
        override val level: Int,
        override val path: String,
    ) : TorrentFileNode(
        name = name,
        separator = separator,
        level = level,
        path = path,
        priority = file.priority,
        size = file.size,
        downloadedSize = (file.progress * file.size).toLong(),
        progress = file.progress,
    ) {
        val index = file.index
    }

    data class Folder(
        override val name: String,
        override val separator: String,
        override val level: Int,
        override val path: String,
        val children: MutableList<TorrentFileNode> = mutableListOf(),
    ) : TorrentFileNode(name, separator, level, path, null, 0, 0, 0.0) {
        override val size: Long by lazy {
            children.sumOf { node ->
                if (node.priority != TorrentFilePriority.DO_NOT_DOWNLOAD) node.size else 0
            }
        }

        override val downloadedSize: Long by lazy {
            children.sumOf { node ->
                if (node.priority != TorrentFilePriority.DO_NOT_DOWNLOAD) node.downloadedSize else 0
            }
        }

        override val progress by lazy { (downloadedSize / size.toDouble()).takeUnless { it.isNaN() } ?: 1.0 }

        override val priority: TorrentFilePriority? by lazy {
            var currentPriority: TorrentFilePriority? = null
            var isMixedPriority = false

            for (node in children) {
                val nodePriority = node.priority

                if (nodePriority == null) {
                    isMixedPriority = true
                    break
                }

                if (currentPriority == null) {
                    currentPriority = nodePriority
                } else if (currentPriority != nodePriority) {
                    isMixedPriority = true
                    break
                }
            }

            if (isMixedPriority) null else currentPriority
        }

        fun findChildNode(path: String): TorrentFileNode? {
            if (path.isEmpty()) return this

            val pathItems = path.split(separator)
            var currentNode: TorrentFileNode = this

            for (i in 0 until pathItems.size - 1) {
                if (currentNode is Folder) {
                    currentNode = currentNode.children.find { it.name == pathItems[i] } ?: return null
                } else {
                    return null
                }
            }

            return if (currentNode is Folder) {
                currentNode.children.find { it.name == pathItems.last() }
            } else {
                null
            }
        }

        fun findAllFiles(paths: List<String>): List<File> {
            val result = mutableListOf<File>()

            for (path in paths) {
                val node = findChildNode(path) ?: continue

                when (node) {
                    is File -> result.add(node)
                    is Folder -> {
                        val folderQueue = ArrayDeque<Folder>()
                        folderQueue.add(node)

                        while (folderQueue.isNotEmpty()) {
                            val currentFolder = folderQueue.removeFirst()

                            for (child in currentFolder.children) {
                                when (child) {
                                    is File -> result.add(child)
                                    is Folder -> folderQueue.add(child)
                                }
                            }
                        }
                    }
                }
            }

            return result
        }
    }

    companion object {
        fun fromFileList(fileList: Collection<TorrentFile>): Folder {
            val separator = if (fileList.any { it.name.contains("/") }) "/" else "\\"

            val rootNode = Folder(
                name = "",
                separator = separator,
                level = 0,
                path = "",
            )

            fileList.forEach { file ->
                val pathList = file.name.split(separator)
                var currentNode = rootNode

                for ((i, pathItem) in pathList.withIndex()) {
                    if (i == pathList.lastIndex) {
                        currentNode.children.add(
                            File(
                                name = pathItem,
                                file = file,
                                separator = separator,
                                level = i + 1,
                                path = pathList.joinToString("/"),
                            ),
                        )
                    } else {
                        val existingNode = currentNode.children.find { it.name == pathItem } as? Folder
                        currentNode = existingNode ?: Folder(
                            name = pathItem,
                            separator = separator,
                            level = i + 1,
                            path = pathList.take(i + 1).joinToString("/"),
                        ).also {
                            currentNode.children.add(it)
                        }
                    }
                }
            }

            sortNodeTree(rootNode)

            return rootNode
        }

        private fun sortNodeTree(rootNode: Folder) {
            val stack = ArrayDeque<Folder>()
            stack.add(rootNode)

            while (stack.isNotEmpty()) {
                val node = stack.removeFirst()
                node.children.sort()

                node.children.filterIsInstance<Folder>().forEach { folder ->
                    stack.add(folder)
                }
            }
        }
    }
}
