package com.proff.teamcity.gitStorage

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.*
import jetbrains.buildServer.agent.artifacts.AgentArtifactHelper
import jetbrains.buildServer.artifacts.ArtifactDataInstance
import jetbrains.buildServer.util.EventDispatcher
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.FileMode.REGULAR_FILE
import org.eclipse.jgit.lib.TreeFormatter
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.*
import java.io.BufferedReader


/*
git gc --prune="1 days ago"
 */


class GitArtifactsPublisher(dispatcher: EventDispatcher<AgentLifeCycleListener>, private val tracker: CurrentBuildTracker, private val helper: AgentArtifactHelper) : ArtifactsPublisher {
    init {
        dispatcher.addListener(object : AgentLifeCycleAdapter() {
            override fun buildStarted(build: AgentRunningBuild) {
                publishedArtifacts.clear()
            }
        })
    }

    private val publishedArtifacts = arrayListOf<ArtifactDataInstance>()

    override fun isEnabled() = true

    override fun publishFiles(filePathMap: Map<File, String>): Int {
        val result = 0
        val build = tracker.currentBuild
        try {
            val remotePath = tracker.currentBuild.artifactStorageSettings[GitConstants.PARAM_REMOTE_PATH]
            val repoName = tracker.currentBuild.artifactStorageSettings[GitConstants.PARAM_NAME]
            tracker.currentBuild.buildLogger.threadLogger.message("repo name: $repoName, remote path: $remotePath")

            val localPath = File(tracker.currentBuild.agentConfiguration.systemDirectory, "gitStorage/" + repoName)
            val repository = createGit(/*tracker.currentBuild, */localPath)
            if (!localPath.exists()) {
                Git.init().setDirectory(localPath).setBare(true).call()
                Git.open(localPath).remoteAdd().setName("origin").setUri(URIish(remotePath)).call()
            }
            val heads = Git.lsRemoteRepository().setHeads(true).setRemote(tracker.currentBuild.artifactStorageSettings["remotePath"]).call()
            val last = heads.sortedByDescending { it.name.substring(it.name.lastIndexOf('/') + 1).toInt() }.firstOrNull()

            Git.open(localPath).use { repo ->
                if (last != null) {
                    val msg = "fetch last commit: " + last.objectId.name
                    tracker.currentBuild.buildLogger.threadLogger.activityStarted(msg, "gitStorage")
                    tracker.currentBuild.buildLogger.threadLogger.message(msg)
                    repository.fetch(last.name)
                    tracker.currentBuild.buildLogger.threadLogger.activityFinished(msg, "gitStorage")
                }

                var msg = "write files to local repository"
                tracker.currentBuild.buildLogger.threadLogger.activityStarted(msg, "gitStorage")
                tracker.currentBuild.buildLogger.threadLogger.message(msg)
                val hashes = repository.addFiles(filePathMap.map { it.key.absoluteFile })
                tracker.currentBuild.buildLogger.threadLogger.activityFinished(msg, "gitStorage")

                val branchName = build.buildTypeExternalId + "/" + build.buildId.toString()
                tracker.currentBuild.buildLogger.threadLogger.message("create tree and commit")
                val tree = TreeFormatter()
                val r = repo.repository.resolve("refs/heads/$branchName")
                if (r != null) {
                    val logs = repo.log().add(r).call()
                    if (logs != null) {
                        val log = logs.firstOrNull()
                        if (log != null) {
                            val treeWalk = TreeWalk(repo.repository)
                            treeWalk.addTree(log.tree)
                            while (treeWalk.next()) {
                                if (!treeWalk.isSubtree) {
                                    treeWalk.objectReader.use {
                                        tree.append(treeWalk.pathString, REGULAR_FILE, treeWalk.getObjectId(0))
                                    }
                                }
                            }
                        }
                    }
                }

                for (file in filePathMap) {
                    val name = (if (file.value.isBlank()) "" else (file.value + "/")) + file.key.name
                    tree.append(name, REGULAR_FILE, ObjectId.fromString(hashes[file.key]))
                    publishedArtifacts.add(ArtifactDataInstance.create(name, file.key.length()))
                }
                val zipMap = tracker.currentBuild.sharedConfigParameters["gitStorage.zipMap"]!!
                val zipMapParsed = zipMap.split("\n").filter { it.isNotEmpty() }.map { it.split("=>").map { it.trim() } }.map { it[0] to it[1] }.toMap()
                tracker.currentBuild.buildLogger.message("map: $zipMap")


                val inserter = repo.repository.newObjectInserter()
                val treeId = inserter.insert(tree)
                val commitBuilder = CommitBuilder()
                commitBuilder.setTreeId(treeId)
                commitBuilder.message = "artifacts"
                val person = PersonIdent("teamcity", "teamcity")
                commitBuilder.author = person
                commitBuilder.committer = person
                val commitId = inserter.insert(commitBuilder)
                inserter.flush()
                tracker.currentBuild.buildLogger.threadLogger.message("commit ${commitId.name} created")

                repo.branchCreate().setName(branchName).setForce(true).setStartPoint(commitId.name).call()
                for (item in zipMapParsed) {
                    var tree: RevTree? = null
                    RevWalk(repo.repository).use {
                        tree = it.lookupTree(treeId)
                    }

                    /*val id = getObjectId(repo, tree!!, item.value)
                    val size = repository.archiveSize(id.name, item.value)
                    tracker.currentBuild.buildLogger.error("${id.name}, ${item.value}")*/

                    var file = File.createTempFile("gitStorage-zipMap", ".zip")
                    //throw Exception(file.absolutePath)
                    file.outputStream().use { oStream ->
                        zip(repo, branchName, oStream, item.value)
                    }
                    publishedArtifacts.add(ArtifactDataInstance.create(item.key, file.length()))
                    file.delete()
                }
                helper.publishArtifactList(publishedArtifacts, mapOf("branch" to branchName, "zipMap" to zipMap).plus(tracker.currentBuild.artifactStorageSettings))

                msg = "push commit ${commitId.name} to remote repository"
                tracker.currentBuild.buildLogger.threadLogger.activityStarted(msg, "gitStorage")
                tracker.currentBuild.buildLogger.threadLogger.message(msg)
                repository.push(branchName)
                tracker.currentBuild.buildLogger.threadLogger.activityFinished(msg, "gitStorage")
            }
        } catch (e: Throwable) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val message = e.toString() + "\n" + sw.toString()
            LOG.warnAndDebugDetails(message, e)
            build.buildLogger.error(message)
            throw ArtifactPublishingFailedException(message, false, e)
        }
        return result

    }

    override fun getType() = GitConstants.STORAGE_TYPE

    companion object {
        private val LOG = Logger.getInstance(GitArtifactsPublisher::class.java.name)
    }

    private fun exec(cmd: Array<String>, inputString: String?) {
        //build.activity("executing $nametoLog").use {
        //build.message(toLog ?: cmd)
        val proc = Runtime.getRuntime().exec(cmd)
        if (inputString != null) {
            proc.outputStream.write(inputString.toByteArray())
        }
        var rdr = BufferedReader(InputStreamReader(proc.inputStream))
        rdr.use {
            while (true) {
                val line = rdr.readLine() ?: break
                tracker.currentBuild.buildLogger.message(line)
            }
        }
        rdr = BufferedReader(InputStreamReader(proc.errorStream))
        rdr.use {
            while (true) {
                val line = rdr.readLine() ?: break
                //build.warning(line)
            }
        }
        val result = proc.waitFor()
        if (result != 0) {
            //build.warning("error")
            //build.stopBuild("error executing $cmd")
        }
        //}
    }
}