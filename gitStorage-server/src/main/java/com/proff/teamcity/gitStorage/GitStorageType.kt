package com.proff.teamcity.gitStorage

import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageType
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageTypeRegistry
import jetbrains.buildServer.web.openapi.PluginDescriptor

class GitStorageType(registry: ArtifactStorageTypeRegistry, private val descriptor: PluginDescriptor) : ArtifactStorageType() {
    init {
        registry.registerStorageType(this)
    }
    override fun getEditStorageParametersPath() = descriptor.getPluginResourcesPath(GitConstants.SETTINGS_PATH + ".jsp")

    override fun getName() = "Git Storage"

    override fun getDescription() = "Provides Git storage support for TeamCity artifacts"

    override fun getType() = GitConstants.STORAGE_TYPE
}