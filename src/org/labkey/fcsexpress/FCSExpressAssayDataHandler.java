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

import org.apache.log4j.Logger;
import org.labkey.api.collections.Sets;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.MvColumn;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 9/1/12
 */
public class FCSExpressAssayDataHandler extends AbstractAssayTsvDataHandler
{
    public static final DataType TRANSFORMED_DATA_TYPE = new DataType("FCSExpressAssayTransformedData"); // a marker data type

    public static final String NAMESPACE = "FCSExpressAssayData";
    public static final AssayDataType DATA_TYPE =
            new AssayDataType(NAMESPACE, new FileType("BogusExtension"));

    @Override
    public DataType getDataType()
    {
        return DATA_TYPE;
    }

    @Override
    protected boolean allowEmptyData()
    {
        return false;
    }

    @Override
    protected boolean shouldAddInputMaterials()
    {
        return false;
    }

    @Override
    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    // XXX: This method is duplicated from TsvDataHandler -- factor appropriately into AbstractAssayTsvDataHandler
    @Override
    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        ExpProtocol protocol = data.getRun().getProtocol();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        Domain dataDomain = provider.getResultsDomain(protocol);

        // XXX: Extract out configuring the loader so AbstractAssayTsvDataHandler subclasses can use it.
        List<? extends DomainProperty> columns = dataDomain.getProperties();
        Map<String, DomainProperty> aliases = dataDomain.createImportMap(false);
        Set<String> mvEnabledColumns = Sets.newCaseInsensitiveHashSet();
        Set<String> mvIndicatorColumns = Sets.newCaseInsensitiveHashSet();

        for (DomainProperty col : columns)
        {
            if (col.isMvEnabled())
            {
                mvEnabledColumns.add(col.getName());
                mvIndicatorColumns.add(col.getName() + MvColumn.MV_INDICATOR_SUFFIX);
            }
        }
        DataLoader loader = null;
        try
        {
            loader = createLoader(dataFile);

            // XXX: Extract out configuring the loader so AbstractAssayTsvDataHandler subclasses can use it.
            loader.setThrowOnErrors(settings.isThrowOnErrors());
            for (ColumnDescriptor column : loader.getColumns())
            {
                if (mvEnabledColumns.contains(column.name))
                {
                    column.setMvEnabled(dataDomain.getContainer());
                }
                else if (mvIndicatorColumns.contains(column.name))
                {
                    column.setMvIndicator(dataDomain.getContainer());
                    column.clazz = String.class;
                }
                DomainProperty prop = aliases.get(column.name);
                if (prop != null)
                    column.clazz = prop.getPropertyDescriptor().getPropertyType().getJavaType();
                else
                {
                    // It's not an expected column. Is it an MV indicator column?
                    if (!mvIndicatorColumns.contains(column.name))
                    {
                        column.load = false;
                    }
                }
                if (settings.isBestEffortConversion())
                    column.errorValues = DataLoader.ERROR_VALUE_USE_ORIGINAL;
                else
                    column.errorValues = ERROR_VALUE;
            }
            Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
            List<Map<String, Object>> dataRows = loader.load();

            // loader did not parse any rows
            if (dataRows.isEmpty() && !settings.isAllowEmptyData() && columns.size() > 0)
                throw new ExperimentException("Unable to load any rows from the input data. Please check the format of the input data to make sure it matches the assay data columns.");
            //if (!dataRows.isEmpty())
            //    adjustFirstRowOrder(dataRows, loader);

            datas.put(DATA_TYPE, dataRows);
            return datas;
        }
        catch (IOException ioe)
        {
            throw new ExperimentException(ioe);
        }
        finally
        {
            if (loader != null)
                loader.close();
        }
    }

    protected DataLoader createLoader(File dataFile) throws IOException
    {
        String baseName = FileUtil.getBaseName(dataFile);
        File extractFileRoot = new File(dataFile.getParentFile(), baseName);
        if (!extractFileRoot.exists())
            if (!extractFileRoot.mkdirs())
                throw new IOException("Failed to create directory for extrating binary files");

        return new FCSExpressDataLoader(dataFile, extractFileRoot);
    }
}
