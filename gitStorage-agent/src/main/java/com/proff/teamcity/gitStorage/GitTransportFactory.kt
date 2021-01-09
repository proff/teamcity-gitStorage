package com.proff.teamcity.gitStorage

import jetbrains.buildServer.agent.CurrentBuildTracker
import jetbrains.buildServer.artifacts.TransportFactoryExtension
import jetbrains.buildServer.artifacts.URLContentRetriever
import jetbrains.buildServer.http.HttpUtil
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope

class GitTransportFactory(private val tracker: CurrentBuildTracker/*, private val config: BuildAgentConfigurationEx*/) : TransportFactoryExtension {
    override fun getTransport(parameters: MutableMap<String, String>): URLContentRetriever? {
        return GitTransport(tracker, createHttpClient())
    }

    private fun createHttpClient(): HttpClient {
        val userName = tracker.currentBuild.accessUser
        val password = tracker.currentBuild.accessCode
        val connectionTimeout = 60
        val client = HttpUtil.createHttpClient(connectionTimeout)
        client.params.isAuthenticationPreemptive = true
        val defaultcreds = UsernamePasswordCredentials(userName, password)
        client.state.setCredentials(AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), defaultcreds)
        //todo
        /*val proxyHost = config.serverProxyHost
        if (proxyHost != null) {
            HttpUtil.configureProxy(client, proxyHost, config.serverProxyPort, config.serverProxyCredentials)
        }*/
        return client
    }

}