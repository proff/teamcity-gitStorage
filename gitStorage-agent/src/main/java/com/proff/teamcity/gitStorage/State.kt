package com.proff.teamcity.gitStorage

import jetbrains.buildServer.agent.FlowLogger
import java.io.File

var state: State? = null

class State {
    var logger: FlowLogger? = null
    var name: String? = null
    var remotePath: String? = null
    var tree: Map<String, String> = mapOf()
    var localPath: File? = null
}