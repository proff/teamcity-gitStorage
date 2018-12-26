package com.proff.teamcity.gitStorage

import jetbrains.buildServer.serverSide.artifacts.ArtifactContentProvider
import jetbrains.buildServer.serverSide.artifacts.StoredBuildArtifactInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GitArtifactContentProvider : ArtifactContentProvider {
    override fun getContent(artifactInfo: StoredBuildArtifactInfo): InputStream {
        try {
            val artifactData = artifactInfo.artifactData
                    ?: throw IOException("Can not process artifact download request for a folder")

            val branchName = artifactInfo.commonProperties["branch"]
            val path = artifactInfo.storageSettings[GitConstants.PARAM_LOCAL_PATH]
                    ?: artifactInfo.storageSettings[GitConstants.PARAM_REMOTE_PATH]
            //throw Exception(artifactInfo.commonProperties.map { it.key + "=" + it.value }.joinToString(", "))
            if (artifactInfo.commonProperties.containsKey("zipMap")) {
                val map = artifactInfo.commonProperties["zipMap"]!!.split("\n").filter { it.isNotEmpty() }.map { it.split("=>").map { it.trim() } }.map { it[0] to it[1] }.toMap()
                //throw Exception(map.map { it.key + "=" + it.value }.joinToString(", "))
                if (map.containsKey(artifactData.path)) {
                    var file = File.createTempFile("gitStorage-zipMap", ".zip")
                    //throw Exception(file.absolutePath)
                    file.outputStream().use { oStream ->
                        Git.open(File(path)).use { git ->
                            zip(git, branchName!!, oStream, map[artifactData.path]!!)
                        }
                    }
                    //createGit(File(path)).archive(tree!!.id.name, map[artifactData.path]!!, response)
                    return file.inputStream()
                }
            }

            val bytes = getContent(path!!, branchName!!, artifactData.path).first

            return ByteArrayInputStream(bytes)
        } catch (e: Throwable) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val message = e.toString() + "\n" + sw.toString()
            throw Exception(message)
        }
    }

    override fun getType() = GitConstants.STORAGE_TYPE
}