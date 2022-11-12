package com.proff.teamcity.gitStorage

import java.io.File

fun createGit(/*build: AgentRunningBuild, */repo: File): IGit {
    /*val path = build.sharedBuildParameters.environmentVariables["TEAMCITY_GIT_PATH"]
    if (path.isNullOrBlank())
        return JGit(repo, build.buildLogger)*/
    return CliGit(repo, File("/usr/bin/git")/*, build.buildLogger*/)
}