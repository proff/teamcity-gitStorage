package com.proff.teamcity.gitStorage

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.transport.RefSpec
import java.io.File

class JGit(repository: File) : IGit {
    override fun archiveSize(id: String, path: String): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun archive(id: String, path: String, stream: javax.servlet.http.HttpServletResponse) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cat(id: String, path: File) {
        val objectReader = repo.repository.newObjectReader()
        objectReader.use {
            val objectLoader = objectReader.open(ObjectId.fromString(id))
            path.writeBytes(objectLoader.bytes)
        }
    }

    override fun push(branchName: String) {
        repo.push().setRemote("origin").setRefSpecs(RefSpec(branchName + ":" + branchName)).setForce(true).call()
    }

    override fun fetch(branchName: String) {
        repo.fetch().setRemote("origin").setRefSpecs(RefSpec(branchName + ":" + branchName)).setForceUpdate(true).call()
    }

    val repo: Git

    init {
        repo = Git.open(repository)
    }

    override fun addFiles(files: List<File>): Map<File, String> {
        val result = files.asIterable().pmap {
            val inserter = repo.repository.newObjectInserter()
            val id = inserter.insert(Constants.OBJ_BLOB, it.readBytes())
            Pair(it, id.name)
        }.toMap()
        return result
    }
}