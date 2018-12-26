package com.proff.teamcity.gitStorage

import com.google.gson.JsonParser
import com.intellij.openapi.util.io.StreamUtil
import jetbrains.buildServer.agent.CurrentBuildTracker
import jetbrains.buildServer.artifacts.URLContentRetriever
import jetbrains.buildServer.util.FileUtil
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.*
import java.net.URLDecoder

class GitTransport(val tracker: CurrentBuildTracker, val httpClient: HttpClient) : URLContentRetriever {

    override fun getDigest(url: String): String? {
        if (url.endsWith("teamcity-ivy.xml"))
            return null
        val regex = Regex("/httpAuth/repository/download/(.*)\\.tcbuildid/(.*)")
        val match = regex.find(url)
        val branchName = match!!.groups[1]!!.value
        val path = URLDecoder.decode(match!!.groups[2]!!.value, "UTF-8")
        return getDigest(state!!.remotePath!!, branchName, path)
    }

    override fun downloadUrlTo(url: String, target: File): String? {
        try {
            val regex = Regex("/httpAuth/repository/download/(.*)\\.tcbuildid/(.*)")
            val match = regex.find(url)
            val branchName = match!!.groups[1]!!.value
            val path = URLDecoder.decode(match!!.groups[2]!!.value, "UTF-8")

            val data: ByteArray
            if (url.endsWith("teamcity-ivy.xml")) {
                state = State()
                state!!.logger = tracker.currentBuild.buildLogger.threadLogger
                try {
                    data = download(url.removeSuffix("teamcity-ivy.xml") + ".teamcity/artifacts.json")
                } catch (e: IOException) {
                    state = null
                    return null
                }
                val params = JsonParser().parse(String(data)).asJsonObject["properties"].asJsonObject
                state!!.remotePath = params[GitConstants.PARAM_REMOTE_PATH].asString
                state!!.name = params[GitConstants.PARAM_NAME].asString
                state!!.localPath = File(tracker.currentBuild.agentConfiguration.systemDirectory, "gitStorage/" + state!!.name)
                tracker.currentBuild.buildLogger.threadLogger.message("repo name: ${state!!.name}, remote path: ${state!!.remotePath}")

                var repo: Git
                var repository: IGit
                if (!state!!.localPath!!.exists()) {
                    Git.init().setDirectory(state!!.localPath).setBare(true).call()
                    repo = Git.open(state!!.localPath)
                    repo.remoteAdd().setName("origin").setUri(URIish(state!!.remotePath)).call()
                    repository = createGit(/*tracker.currentBuild, */state!!.localPath!!)

                } else {
                    repo = Git.open(state!!.localPath)
                    repository = createGit(/*tracker.currentBuild, */state!!.localPath!!)
                }

                repo.use {

                    val list = repo.branchList().call()
                    if (!list.any { it.name == "refs/heads/$branchName" }) {
                        state!!.logger!!.message("fetch $branchName from remote repository")
                        repository.fetch(branchName)
                    }


                    val tree: RevTree? = revTree(state!!.localPath!!.absolutePath, branchName, repo)
                    if (tree == null) {
                        tracker.currentBuild.buildLogger.threadLogger.error("branch $branchName is not found in ${state!!.localPath!!}")
                        return null
                    }
                    val treeWalk = TreeWalk(repo.repository)
                    treeWalk.addTree(tree)
                    val map = mutableMapOf<String, String>()
                    while (treeWalk.next()) {
                        if (!treeWalk.isSubtree) {
                            treeWalk.objectReader.use {
                                map[treeWalk.pathString] = treeWalk.getObjectId(0).toObjectId().name
                            }
                        }
                    }
                    state!!.tree = map
                }

                return null
            }
            if (state == null || state!!.localPath == null)
                return null


            target.parentFile.mkdirs()
            val id = state!!.tree[path]
            Git.open(state!!.localPath).use { repo ->
                val objectReader = repo.repository.newObjectReader()
                if (id == null) {
                    state!!.logger!!.error("$path is not fount in branch $branchName")
                    return null
                }
                objectReader.use {
                    val objectLoader = objectReader.open(ObjectId.fromString(id))
                    target.writeBytes(objectLoader.bytes)
                }
                return id
            }
        } catch (e: Throwable) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            tracker.currentBuild.buildLogger.error(sw.toString())
            throw e
        }
    }

    override fun interrupt() {
    }

    @Throws(IOException::class)
    protected fun download(urlString: String): ByteArray {
        val getMethod = GetMethod(urlString)
        var `in`: InputStream? = null
        try {
            httpClient.executeMethod(getMethod)
            if (getMethod.statusCode != HttpStatus.SC_OK) {
                throw IOException(String.format("Problem [%d] while downloading %s: %s", getMethod.statusCode, urlString, getMethod.statusText))
            }
            `in` = getMethod.responseBodyAsStream
            val bOut = ByteArrayOutputStream()
            StreamUtil.copyStreamContent(`in`!!, bOut)
            return bOut.toByteArray()
        } finally {
            FileUtil.close(`in`)
            getMethod.releaseConnection()
        }
    }

}