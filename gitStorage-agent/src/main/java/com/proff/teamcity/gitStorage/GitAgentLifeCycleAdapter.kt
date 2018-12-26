package com.proff.teamcity.gitStorage

import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildAgent
import jetbrains.buildServer.util.EventDispatcher

class GitAgentLifeCycleAdapter(val dispatcher: EventDispatcher<AgentLifeCycleListener>) : AgentLifeCycleAdapter() {
    init {
        dispatcher.addListener(this)
    }

    override fun buildStarted(runningBuild: AgentRunningBuild) {
        state = State()
        state!!.logger = runningBuild.buildLogger.threadLogger
    }

    override fun dependenciesDownloaded(runningBuild: AgentRunningBuild) {

    }

}