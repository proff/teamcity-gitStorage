package com.proff.teamcity.gitStorage



/*class GitController(controllerManager: WebControllerManager) : BaseController() {
    init {
        //TODO: self hosted git server
        //controllerManager.registerController("/git/", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val g = GitServlet()
        g.setRepositoryResolver(RepositoryResolver { req, name ->
            try {
                return@RepositoryResolver Git.open(File("d:/gitStorage")).repository
            } catch (e: IOException) {
                throw RepositoryNotFoundException(req.requestURI, e)
            }
        })
        g.setReceivePackFactory { req, db -> createReceivePack(req, db) }
        g.setUploadPackFactory { req, db -> createUploadPack(req, db) }
        g.init(Config(request.servletContext))

        g.service(request, response)

        return simpleView("")
    }

    private fun createUploadPack(req: HttpServletRequest?, db: Repository?): UploadPack? {
        return UploadPack(db)
    }

    private fun createReceivePack(req: HttpServletRequest?, db: Repository?): ReceivePack? {
        return ReceivePack(db)
    }
}

class Config(val context: ServletContext) : ServletConfig {
    override fun getInitParameter(name: String?): String? {
        return null
    }

    override fun getInitParameterNames(): Enumeration<String> {
        return Vector<String>().elements()
    }

    override fun getServletName(): String {
        return ""
    }

    override fun getServletContext(): ServletContext {
        return context
    }

}*/