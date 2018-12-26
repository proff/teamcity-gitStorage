package com.proff.teamcity.gitStorage

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import java.io.OutputStream
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.withLock

/*val repos = mutableMapOf<String, Git>()
val trees = mutableMapOf<String, RevTree>()*/
fun getContent(repoPath: String, branchName: String, path: String): Pair<ByteArray, String> {
    val repo: Git = git(repoPath)
    val tree: RevTree = revTree(repoPath, branchName, repo)!!
    val blobId = getObjectId(repo, tree, path)
    val objectReader = repo.repository.newObjectReader()
    objectReader.use {
        val objectLoader = objectReader.open(blobId)
        return Pair(objectLoader.bytes, blobId.name)
    }
}

fun getDigest(repoPath: String, branchName: String, path: String): String {
    val repo: Git = git(repoPath)
    val tree: RevTree = revTree(path, branchName, repo)!!
    val blobId = getObjectId(repo, tree, path)
    return blobId.name
}

fun revTree(repoPath: String, branchName: String, repo: Git): RevTree? {
    val tree: RevTree
    val key = "$repoPath;$branchName"
    /*if (trees.containsKey(key)) {
        tree = trees[key]!!
    } else {*/
    val id = repo.repository.resolve("refs/heads/$branchName")
    if (id == null)
        return null
    tree = repo.log().add(id).call().first().tree
    /*trees[key] = tree
}*/
    return tree
}

fun git(repoPath: String): Git {
    val repo: Git
    /*if (repos.containsKey(repoPath)) {
        repo = repos[repoPath]!!
    } else {*/
    repo = Git.open(File(repoPath))
    /*repos[repoPath] = repo
}*/
    return repo
}

fun getObjectId(repo: Git, tree: RevTree, path: String): ObjectId {
    val treeWalk = TreeWalk.forPath(repo.repository, path, tree)
    treeWalk.use {
        val blobId = treeWalk.getObjectId(0)
        return blobId
    }
}

fun zip(git: Git, branchName: String, stream: OutputStream, path: String) {
    val tree = revTree(path!!, branchName!!, git!!)
    ZipOutputStream(stream).use { zip ->
        val treeWalk = TreeWalk(git.repository)
        treeWalk.addTree(tree)
        while (treeWalk.next()) {
            if (!treeWalk.isSubtree) {
                treeWalk.objectReader.use {
                    if (treeWalk.pathString.startsWith(path + "/")) {
                        zip.putNextEntry(ZipEntry(treeWalk.pathString.removeRange(0, path.length)))
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