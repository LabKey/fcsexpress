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

package org.labkey.fcsexpress;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.exp.Identifiable;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.LsidManager;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.reader.DataLoaderService;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;

public class FCSExpressModule extends DefaultModule
{
    public String getName()
    {
        return "FCSExpress";
    }

    public double getVersion()
    {
        return 12.20;
    }

    public boolean hasScripts()
    {
        return false;
    }

    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    protected void init()
    {
        addController("fcsexpress", FCSExpressController.class);
    }

    public void doStartup(ModuleContext moduleContext)
    {
        DataLoaderService.get().registerFactory(new FCSExpressDataLoader.Factory());
        ExperimentService.get().registerExperimentDataHandler(new FCSExpressAssayDataHandler());
        AssayService.get().registerAssayProvider(new FCSExpressAssayProvider());
    }

    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }
}
