/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.CodeOnlyModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.reader.DataLoaderService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;

public class FCSExpressModule extends CodeOnlyModule
{
    public String getName()
    {
        return "FCSExpress";
    }

    @NotNull
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

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }
}
