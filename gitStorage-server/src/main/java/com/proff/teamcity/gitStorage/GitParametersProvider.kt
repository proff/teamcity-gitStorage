package com.proff.teamcity.gitStorage

class GitParametersProvider {
    val login: String
        get() = GitConstants.PARAM_LOGIN
    val password: String
        get() = GitConstants.PARAM_PASSWORD
    val key: String
        get() = GitConstants.PARAM_KEY
    val name: String
        get() = GitConstants.PARAM_NAME
    val remotePath: String
        get() = GitConstants.PARAM_REMOTE_PATH
    val localPath: String
        get() = GitConstants.PARAM_LOCAL_PATH
}