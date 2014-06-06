/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;

import java.util.Map;

/**
 * User: kevink
 */
public class FCSExpressAssayRunCreator extends DefaultAssayRunCreator<FCSExpressAssayProvider>
{
    public FCSExpressAssayRunCreator(FCSExpressAssayProvider provider)
    {
        super(provider);
    }

    @Override
    protected void addInputMaterials(AssayRunUploadContext<FCSExpressAssayProvider> context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        super.addInputMaterials(context, inputMaterials, resolverType);
    }

    // XXX: Can we add the File/Attachment columns as output datas here?
    @Override
    protected void addOutputDatas(AssayRunUploadContext<FCSExpressAssayProvider> context, Map<ExpData, String> inputDatas, Map<ExpData, String> outputDatas, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        /*
        ExpData data = ExperimentService.get().createData(context.getContainer(), getProvider().getDataType(), "Illumina output placeholder");
        data.setLSID(ExperimentService.get().generateGuidLSID(context.getContainer(), getProvider().getDataType()));
        outputDatas.put(data, ExpDataRunInput.DEFAULT_ROLE);
        */
        super.addOutputDatas(context, inputDatas, outputDatas, resolverType);
    }

}
