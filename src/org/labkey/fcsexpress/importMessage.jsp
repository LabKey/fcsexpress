<%
/*
 * Copyright (c) 2012 LabKey Corporation
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
%>
<p>
    Please copy and paste the following URL into the FCSExpress batch action dialog:
    <br>
    <br>
    <%=h(new ActionURL("assay", "importRun.api", c).getURIString(false))%>
    <br>
    <br>
    and enter '<em><%=form.getRowId()%></em>' as the assay id.
</p>
<p>
    For more information on importing FCSExpress data into LabKey Server, please
    read the <a href="<%=text("#undone")%>">online documentation</a>.
</p>
