package com.proff.teamcity.gitStorage

import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.artifacts.StoredBuildArtifactInfo
import jetbrains.buildServer.web.openapi.artifacts.ArtifactDownloadProcessor
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class GitArtifactDownloadProcessor : ArtifactDownloadProcessor {

    override fun processDownload(artifactInfo: StoredBuildArtifactInfo,
                                 buildPromotion: BuildPromotion,
                                 request: HttpServletRequest,
                                 response: HttpServletResponse): Boolean {
        try {
            val artifactData = artifactInfo.artifactData
                    ?: throw IOException("Can not process artifact download request for a folder")

            val branchName = artifactInfo.commonProperties["branch"]
            val path = artifactInfo.storageSettings[GitConstants.PARAM_LOCAL_PATH]
                    ?: artifactInfo.storageSettings[GitConstants.PARAM_REMOTE_PATH]
            /*if (artifactInfo.commonProperties.containsKey("zipMap")) {
                val map = artifactInfo.commonProperties["zipMap"]!!.split("\n").filter { it.isNotEmpty() }.map { it.split("=>").map { it.trim() } }.map { it[0] to it[1] }.toMap()
                if (map.containsKey(artifactData.path)) {
                    Git.open(File(path)).use { git ->
                        val tree = revTree(path!!, branchName!!, git!!)
                        //throw Exception(map[artifactData.path]!!)
                        ZipOutputStream(response.outputStream).use { zip ->
                            val treeWalk = TreeWalk(git.repository)
                            treeWalk.addTree(tree)
                            while (treeWalk.next()) {
                                if (!treeWalk.isSubtree) {
                                    treeWalk.objectReader.use {
                                        if (treeWalk.pathString.startsWith(map[artifactData.path] + "/")) {
                                            zip.putNextEntry(ZipEntry(treeWalk.pathString.removeRange(0, map[artifactData.path]!!.length)))
                                            val objectReader = git.repository.newObjectReader()
                                            objectReader.use {
                                                val objectLoader = objectReader.open(treeWalk.getObjectId(0))
                                                zip.write(objectLoader.bytes)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //createGit(File(path)).archive(tree!!.id.name, map[artifactData.path]!!, response)
                    return true
                }
            }*/

            val bytes = getContent(path!!, branchName!!, artifactData.path).first

            response.setContentLength(bytes.count())

            response.outputStream.use {
                response.outputStream.write(bytes)
                response.outputStream.flush()
            }
            response.flushBuffer()

            return true
        } catch (e: Throwable) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val message = e.toString() + "\n" + sw.toString()
            throw Exception(message)
        }
    }

    override fun getType() = GitConstants.STORAGE_TYPE
}