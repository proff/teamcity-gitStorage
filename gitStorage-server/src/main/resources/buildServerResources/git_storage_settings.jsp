<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="params" class="com.proff.teamcity.gitStorage.GitParametersProvider"/>

<style type="text/css">
    .runnerFormTable {
        margin-top: 1em;
    }
</style>

<l:settingsGroup title="Storage Credentials">
    <tr>
        <th class="noBorder"><label for="${params.login}">Account name: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${params.login}" className="longField"/>
            </div>
            <span class="error" id="error_${params.login}"></span>
            <span class="smallNote">Specify the account name.</span>
        </td>
    </tr>
    <tr>
        <th class="noBorder"><label for="${params.password}">Account key: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:passwordProperty name="${params.password}" className="longField"/>
            </div>
            <span class="error" id="error_${params.password}"></span>
            <span class="smallNote">Specify the key to access storage account.</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Storage Parameters">
    <tr class="advancedSetting">
        <th class="noBorder"><label for="${params.name}">Name:</label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${params.name}" className="longField"/>
            </div>
            <span class="smallNote">Specify the container name. If <em>&lt;Auto&gt;</em> is chosen container will be created automatically.<br/>
                You can override default path prefix via build parameter <em><c:out value="${params.name}"/></em>
            </span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th class="noBorder"><label for="${params.remotePath}">Remote path:</label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${params.remotePath}" className="longField"/>
            </div>
            <span class="smallNote">Specify the container name. If <em>&lt;Auto&gt;</em> is chosen container will be created automatically.<br/>
                You can override default path prefix via build parameter <em><c:out value="${params.remotePath}"/></em>
            </span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th class="noBorder"><label for="${params.localPath}">Local path:</label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${params.localPath}" className="longField"/>
            </div>
            <span class="smallNote">Specify the container name. If <em>&lt;Auto&gt;</em> is chosen container will be created automatically.<br/>
                You can override default path prefix via build parameter <em><c:out value="${params.localPath}"/></em>
            </span>
        </td>
    </tr>
</l:settingsGroup>