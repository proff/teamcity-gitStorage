package com.proff.teamcity.gitStorage

import java.io.File
import javax.servlet.http.HttpServletResponse

class CliGit(val repo: File, val gitPath: File) : IGit {
    override fun archiveSize(id: String, path: String): Long {
        val outFile = File.createTempFile("gitStorageGitOut", ".tmp")
        ProcessBuilder(gitPath.absolutePath, "archive", id + ":" + path).directory(repo).redirectOutput(outFile).start().waitFor()
        return outFile.length()
    }

    override fun archive(id: String, path: String, response: HttpServletResponse) {
        val outFile = File.createTempFile("gitStorageGitOut", ".tmp")
        ProcessBuilder(gitPath.absolutePath, "archive", id + ":" + path).directory(repo).redirectOutput(outFile).start().waitFor()
        val b = outFile.readBytes()
        response.setContentLength(b.size)

        throw Exception(b.size.toString())
        response.outputStream.write(b)
    }

    override fun cat(id: String, path: File) {
        ProcessBuilder(gitPath.absolutePath, "cat-file", "blob", id).directory(repo).redirectOutput(path).start().waitFor()
    }

    override fun push(branchName: String) {
        ProcessBuilder(gitPath.absolutePath, "push", "-f", "origin", "$branchName:$branchName").directory(repo).start().waitFor()
    }

    override fun fetch(branchName: String) {
        ProcessBuilder(gitPath.absolutePath, "fetch", "-f", "origin", "$branchName:$branchName").directory(repo).start().waitFor()
    }

    override fun addFiles(files: List<File>): Map<File, String> {
        val count = 1//Runtime.getRuntime().availableProcessors()
        val lists = mutableListOf<MutableList<File>>()
        val parts = mutableListOf<addFilesPart>()
        for (i in 0 until count) {
            lists.add(mutableListOf())
        }
        var current = 0
        for (i in 0 until files.count()) {
            lists[current].add(files[i])
            current = (current + 1) % count
        }
        for (list in lists) {
            if (list.count() == 0)
                continue
            val inFile = File.createTempFile("gitStorageGitIn", ".tmp")
            val outFile = File.createTempFile("gitStorageGitOut", ".tmp")
            inFile.writeText(list.joinToString("\n"))
            val pb = ProcessBuilder(gitPath.absolutePath, "hash-object", "-w", "--stdin-paths").directory(repo).redirectInput(inFile).redirectOutput(outFile)
            parts.add(addFilesPart(list, inFile, outFile, pb.start()))
        }
        val map = mutableMapOf<File, String>()
        for (part in parts) {
            part.process.waitFor()
            val hashes = part.outFile.readText().trim().split("\n").map { it.trim() }
            if (hashes.count() != part.files.count()) {
                /*logger.error("expected ${part.files.count()} hashes but found ${hashes.count()}")
                logger.error("in: ${part.inFile.readText()}")
                logger.error("out: ${part.outFile.readText()}")*/
            }
            for (i in 0 until part.files.count()) {
                map[part.files[i]] = hashes[i]
            }
        }
        return map
    }
}

class addFilesPart(var files: List<File>, var inFile: File, var outFile: File, var process: Process)