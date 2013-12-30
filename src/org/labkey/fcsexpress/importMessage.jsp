<%
/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.study.actions.ProtocolIdForm" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.data.Container" %>

<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ProtocolIdForm> me = (JspView<ProtocolIdForm>) HttpView.currentView();
    ProtocolIdForm form = me.getModelBean();
    Container c = form.getContainer();

    String importURL = new ActionURL("assay", "importRun.api", c).getURIString(false);
%>
<p>Please copy and paste the following URL into the <em>LabKey Server URL</em> field<br>
    and enter '<code><%=form.getRowId()%></code>' into the <em>Protocol ID</em> field
    of the <em>Export to LabKey Options</em> dialog within FCS Express.<br>
</p>
<input style="font-family:monospace" type="text" readonly size="<%=importURL.length()+20%>" value="<%=h(importURL)%>">
<p>
    For more information on importing FCSExpress data into LabKey Server, please
    read the <%=helpLink("FCSExpress", "online documentation")%>.
</p>
<div style="display:inline-block; padding-top:2em;">
    <img src="<%=getContextPath()%>/fcsexpress/ExportToLabKey.png" alt="FCS Express: Export to LabKey Options">
    <div style="text-align:center">Screenshot of <em>FCS Express: Export to LabKey Options</em> dialog.</div>
</div>

