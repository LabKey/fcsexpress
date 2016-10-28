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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractTsvAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: Sep 7, 2010
 */
public class FCSExpressAssayProvider extends AbstractTsvAssayProvider
{
    private static final Set<String> participantImportAliases;
    private static final Set<String> specimenImportAliases;
    private static final Set<String> visitImportAliases;
    private static final Set<String> dateImportAliases;

    static
    {
        // XXX: duplicated from TsvAssayProvider
        // this is the static lists of import aliases used in the default template
        participantImportAliases = PageFlowUtil.set("ptid", "participantId");
        specimenImportAliases = PageFlowUtil.set("specId", "vialId", "vialId1", "vial1_id", "guspec");
        visitImportAliases = PageFlowUtil.set("visitNo", "visit_no");
        dateImportAliases = PageFlowUtil.set("drawDt", "draw_date", "drawDate");
    }

    public static final String PROTOCOL_PREFIX = "FCSExpressAssayProtocol";
    public static final String RUN_PREFIX = "FCSExpressAssayRun";
    public static final String NAME = "FCSExpress";

    public FCSExpressAssayProvider()
    {
        super(PROTOCOL_PREFIX, RUN_PREFIX, (AssayDataType) ExperimentService.get().getDataType(FCSExpressAssayDataHandler.NAMESPACE), ModuleLoader.getInstance().getModule(FCSExpressModule.class));
    }

    /*
    @Override
    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        return null;
    }
    */

    @Override
    public String getName()
    {
        return NAME;
    }

    // XXX: duplicated from TsvAssayProvider
    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                null,
                FieldKey.fromParts("Run"),
                FieldKey.fromParts(AbstractTsvAssayProvider.ROW_ID_COLUMN_NAME));
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("Data files must be exported from FCSExpress.");
    }

    // XXX: duplicated from TsvAssayProvider
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Pair<Domain, Map<DomainProperty, Object>> resultDomain = createResultDomain(c, user);
        if (resultDomain != null)
            result.add(resultDomain);
        return result;
    }

    // XXX: duplicated from TsvAssayProvider
    protected Pair<Domain,Map<DomainProperty,Object>> createResultDomain(Container c, User user)
    {
        Domain dataDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ExpProtocol.ASSAY_DOMAIN_DATA + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":" + ASSAY_NAME_SUBSTITUTION, "Data Fields");
        dataDomain.setDescription("The user is prompted to enter data values for row of data associated with a run, typically done as uploading a file.  This is part of the second step of the upload process.");
        DomainProperty specimenID = addProperty(dataDomain, SPECIMENID_PROPERTY_NAME,  SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING, "When a matching specimen exists in a study, can be used to identify subject and timepoint for assay. Alternately, supply " + PARTICIPANTID_PROPERTY_NAME + " and either " + VISITID_PROPERTY_NAME + " or " + DATE_PROPERTY_NAME + ".");
        specimenID.setImportAliasSet(specimenImportAliases);

        DomainProperty participantID = addProperty(dataDomain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING, "Used with either " + VISITID_PROPERTY_NAME + " or " + DATE_PROPERTY_NAME + " to identify subject and timepoint for assay.");
        participantID.setConceptURI(org.labkey.api.gwt.client.ui.PropertyType.PARTICIPANT_CONCEPT_URI);
        participantID.setImportAliasSet(participantImportAliases);

        DomainProperty visitID = addProperty(dataDomain, VISITID_PROPERTY_NAME,  VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE, "Used with " + PARTICIPANTID_PROPERTY_NAME + " to identify subject and timepoint for assay.");
        visitID.setImportAliasSet(visitImportAliases);

        DomainProperty dateProperty = addProperty(dataDomain, DATE_PROPERTY_NAME,  DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME, "Used with " + PARTICIPANTID_PROPERTY_NAME + " to identify subject and timepoint for assay.");
        dateProperty.setImportAliasSet(dateImportAliases);

        return new Pair<>(dataDomain, Collections.emptyMap());
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new FCSExpressProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.emptyList();
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        // Not yet hooked up when browsing files
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Import flow assay data from FCS Express.";
    }

    @Override
    public AssayRunCreator getRunCreator()
    {
        return new FCSExpressAssayRunCreator(this);
    }

    @Override
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, FCSExpressController.ImportMessageAction.class);
    }

}
