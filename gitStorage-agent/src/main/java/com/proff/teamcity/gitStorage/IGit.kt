package com.proff.teamcity.gitStorage

import java.io.File

interface IGit {
    fun addFiles(files: List<File>): Map<File, String>
    fun push(branchName: String)
    fun fetch(branchName: String)
    fun cat(id: String, path: File)
    fun archive(id: String, path: String, stream: javax.servlet.http.HttpServletResponse)
    fun archiveSize(id: String, path: String): Long
}