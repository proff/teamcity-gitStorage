package com.proff.teamcity.gitStorage

import jetbrains.buildServer.a.e
import jetbrains.buildServer.artifacts.ArtifactListData
import jetbrains.buildServer.artifacts.ServerArtifactStorageSettingsProvider
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.artifacts.ServerArtifactHelper
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext
import jetbrains.buildServer.serverSide.cleanup.CleanupExtension
import jetbrains.buildServer.serverSide.cleanup.CleanupProcessState
import jetbrains.buildServer.serverSide.impl.cleanup.HistoryRetentionPolicy
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.positioning.PositionAware
import jetbrains.buildServer.util.positioning.PositionConstraint
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class GitCleanupExtension(private val helper: ServerArtifactHelper/*,
                          private val settingsProvider: ServerArtifactStorageSettingsProvider*/)
    : CleanupExtension, PositionAware {

    override fun getOrderId() = GitConstants.STORAGE_TYPE

    override fun cleanupBuildsData(cleanupContext: BuildCleanupContext) {
        //try {
            //File("d:/gitStorage/cleanup.txt").appendText("started\n\n")
        val paths = mutableListOf<String>()
        for (build in cleanupContext.builds) {
            val artifactsInfo = helper.getArtifactList(build) ?: continue
            if (artifactsInfo.commonProperties["storage.type"] != GitConstants.STORAGE_TYPE)
                continue
            val repoPath = artifactsInfo.commonProperties[GitConstants.PARAM_LOCAL_PATH]
                    ?: artifactsInfo.commonProperties[GitConstants.PARAM_REMOTE_PATH]
            if (!paths.contains(repoPath)) {
                paths.add(repoPath!!)
            }
            val branchName = artifactsInfo.commonProperties["branch"]

            val patterns = getPatternsForBuild(cleanupContext, build)
            val toDelete = getPathsToDelete(artifactsInfo, patterns)
            if (toDelete.isEmpty()) continue
            //File("d:/gitStorage/cleanup.txt").appendText(artifactsInfo.commonProperties.map { it.key + "=" + it.value }.joinToString(", ") + "\n" + repoPath + "\n" + branchName + "\n" + toDelete.joinToString("\n") + "\n\n\n")

            val repo = Git.open(File(repoPath))
            val tree: RevTree = revTree(repoPath!!, branchName!!, repo)!!
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
            for (path in toDelete) {
                map.remove(path)
            }
            if (map.count() == 0) {
                repo.branchDelete().setBranchNames(branchName).setForce(true).call()
            } else {
                val tree = TreeFormatter()
                for (item in map) {
                    tree.append(item.key, FileMode.REGULAR_FILE, ObjectId.fromString(item.value))
                }
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
                repo.branchCreate().setName(branchName).setForce(true).setStartPoint(commitId.name).call()
            }
            helper.removeFromArtifactList(build, toDelete)
        }
        for (path in paths){
            //File("d:/gitStorage/cleanup.txt").appendText("gc $path\n\n")
            ProcessBuilder("C:\\Program Files\\Git\\bin\\git.exe", "gc").directory(File(path)).start()
        }
        /*} catch (e: Throwable) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val message = e.toString() + "\n" + sw.toString()
            File("d:/gitStorage/cleanupException.txt").appendText(message + "\n\n\n")
        }*/
    }

    override fun afterCleanup(cleanupState: CleanupProcessState) {
    }

    override fun getConstraint() = PositionConstraint.first()

    private fun getPatternsForBuild(cleanupContext: BuildCleanupContext, build: SBuild): String {
        if (cleanupContext.cleanupLevel.isCleanHistoryEntry) return StringUtil.EMPTY
        val policy = cleanupContext.getCleanupPolicyForBuild(build.buildId)
        return StringUtil.emptyIfNull(policy.parameters[HistoryRetentionPolicy.ARTIFACT_PATTERNS_PARAM])
    }

    private fun getPathsToDelete(artifactsInfo: ArtifactListData, patterns: String): List<String> {
        val keys = artifactsInfo.artifactList.map { it.path }
        return PathPatternFilter(patterns).filterPaths(keys)
    }
}