package org.labkey.fcsexpress;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class FCSExpressProtocolSchema extends AssayProtocolSchema
{
    public FCSExpressProtocolSchema(User user, Container container, ExpProtocol protocol, Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public FilteredTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        return new AssayResultTable(this, includeCopiedToStudyColumns);
    }
}
