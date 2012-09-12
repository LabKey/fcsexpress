package org.labkey.fcsexpress;

import org.apache.commons.io.input.BOMInputStream;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.iterator.CloseableIterator;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

/**
 * User: kevink
 * Date: 9/3/12
 */
public class FCSExpressDataLoader extends DataLoader
{
    Reader _reader;

    public FCSExpressDataLoader(File inputFile) throws IOException
    {
        super();
        setSource(inputFile);
    }

    public FCSExpressDataLoader(File inputFile, Container mvIndicatorContainer) throws IOException
    {
        super(mvIndicatorContainer);
        setSource(inputFile);
    }

    public FCSExpressDataLoader(Reader reader, Container mvIndicatorContainer) throws IOException
    {
        super(mvIndicatorContainer);
        if (reader.markSupported())
            _reader = reader;
        else
            _reader = new BufferedReader(reader);
    }

    @Override
    public String[][] getFirstNLines(int n) throws IOException
    {
        /*
        ArrayList<String[]> rows = new ArrayList<String[]>(n);
        for (Iterator<Map<String, Object>> iter = iterator(); n > 0 && iter.hasNext(); n--)
        {
            Map<String, Object> row = iter.next();
            if (row == null)
                break;


        }
        */
        return new String[0][];
    }

    @Override
    protected void initializeColumns() throws IOException
    {
        if (null == _columns)
            inferColumnInfo();
    }

    private void inferColumnInfo() throws IOException
    {
        XMLStreamReader xml = null;
        try
        {
            xml = getReader();
            FCSExpressStreamReader reader = new FCSExpressStreamReader(xml);
            Map<String, Object> row = reader.readFieldMap();
            if (row == null)
            {
                _columns = new ColumnDescriptor[0];
            }
            else
            {
                ArrayList<ColumnDescriptor> cols = new ArrayList<ColumnDescriptor>(row.size());

                for (Map.Entry<String, Object> entry : row.entrySet())
                {
                    ColumnDescriptor cd = new ColumnDescriptor(entry.getKey(), entry.getValue().getClass());
                    cols.add(cd);
                }

                _columns = cols.toArray(new ColumnDescriptor[cols.size()]);
            }
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (xml != null) try { xml.close(); } catch (Exception e) { }
        }
    }

    @Override
    public CloseableIterator<Map<String, Object>> iterator()
    {
        try
        {
            return new FCSExpressIterator();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close()
    {
        if (_reader != null)
        {
            try
            {
                _reader.close();
            }
            catch (IOException e)
            {
                // Ignore
            }
        }
    }

    protected XMLStreamReader getReader() throws IOException, XMLStreamException
    {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        Reader r;
        if (null != _reader)
        {
            // We don't close handed in readers
            _reader.reset();
            r = _reader;
        }
        else
        {
            FileInputStream fis = new FileInputStream(_file);
            r = new BufferedReader(new InputStreamReader(new BOMInputStream(fis), "UTF-8"));
        }

        return factory.createXMLStreamReader(r);
    }

    protected class FCSExpressIterator extends DataLoaderIterator
    {
        private FCSExpressStreamReader _parser;

        protected FCSExpressIterator() throws IOException, XMLStreamException
        {
            super(0, false);
            init();

            _parser = new FCSExpressStreamReader(getReader(), _activeColumns);
        }

        protected void init()
        {
        }

        @Override
        protected Object[] readFields() throws IOException
        {
            try
            {
                return _parser.readFields();
            }
            catch (XMLStreamException e)
            {
                throw new IOException(e.getMessage(), e);
            }
        }

    }

    protected static class FCSExpressStreamReader
    {
        private final XMLStreamReader _reader;
        private ColumnDescriptor[] _activeColumns;

        protected FCSExpressStreamReader(XMLStreamReader reader) throws IOException, XMLStreamException
        {
            _reader = reader;
            _activeColumns = null;
        }

        protected FCSExpressStreamReader(XMLStreamReader reader, ColumnDescriptor[] activeColumns) throws IOException, XMLStreamException
        {
            _reader = reader;
            _activeColumns = activeColumns;
        }

        protected Object[] readFields() throws XMLStreamException
        {
            Map<String, Object> row = readFieldMap();
            if (row == null)
                return null;

            Collection<Object> values = row.values();
            return values.toArray(new Object[values.size()]);
        }

        /*
        // Called when inferring fields
        protected Map<String, Object> readFieldMap() throws XMLStreamException
        {
            if (!_reader.hasNext())
                return null;

            while (_reader.hasNext())
            {
                switch (_reader.nextTag())
                {
                    case START_ELEMENT:
                    {
                        String name = _reader.getLocalName();
                        if ("iteration".equals(name))
                            return _readIteration();
                    }
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        // XMLBeans parsing
        private Map<String, Object> _readIteration() throws XMLStreamException
        {
            XmlOptions options = XmlBeansUtil.getDefaultParseOptions();
            options.setLoadSubstituteNamespaces(Collections.singletonMap("", "http://denovosoftware.com/FCSExpress/v4.0"));

            Map<String, Object> ret = new LinkedHashMap<String, Object>(_activeColumns != null ? _activeColumns.length*2 : 10);
            try
            {
                FcsExpressResultsType results = FcsExpressResultsType.Factory.parse(_reader, options);
                IterationType iteration = results.getIterationArray(0);
                int number = iteration.getNumber();
                ExportedItemType[] items = iteration.getExportedItemArray();
                for (int i = 0; i < items.length; i++)
                {
                    ExportedItemType item = items[i];

                    String name = item.getName();
                    if (name == null)
                        throw new IllegalArgumentException("Expected item name");

                    Object value = null;
                    switch (item.getType().intValue())
                    {
                        case ExportedItemType.Type.INT_TOKEN:
                            value = _readTokenItem(item);
                            break;

                        case ExportedItemType.Type.INT_PDF:
                        case ExportedItemType.Type.INT_PPT:
                        case ExportedItemType.Type.INT_LAYOUT:
                        case ExportedItemType.Type.INT_PUBLISH:
                        case ExportedItemType.Type.INT_DATAFILE:
                            //_readFileItem();
                            break;

                        case ExportedItemType.Type.INT_PICTURE:
                            //_readPictureItem();
                            break;

                        default:
                            throw new IllegalArgumentException("Unexpected item type: " + item.getType().toString());
                    }

                    ret.put(name, value);
                }
            }
            catch (XmlException e)
            {
                throw new RuntimeException(e);
            }

            return ret;
        }

        private Object _readTokenItem(ExportedItemType item)
        {
            TokenType token = (TokenType) item.changeType(TokenType.type);
            return token.getValue();
        }
        */

        private void expectStartTag(String name)
        {
            if (_reader.isStartElement() && !name.equalsIgnoreCase(_reader.getLocalName()))
                throw new IllegalArgumentException("Expected start element " + name);
        }

        private void expectEndTag(String name)
        {
            if (_reader.isEndElement() && !name.equalsIgnoreCase(_reader.getLocalName()))
                throw new IllegalArgumentException("Expected end element " + name);
        }

        // Called when inferring fields
        protected Map<String, Object> readFieldMap() throws XMLStreamException
        {
            if (!_reader.hasNext())
                return null;

            Map<String, Object> ret = null;
            while (_reader.hasNext())
            {
                switch (_reader.next())
                {
                    case START_ELEMENT:
                        expectStartTag("fcs_express_results");
                        ret = _readResults();
                        break;

                    case END_ELEMENT:
                        expectEndTag("fcs_express_results");
                        return ret;

                    case END_DOCUMENT:
                        return null;
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected Map<String, Object> _readResults() throws XMLStreamException
        {
            expectStartTag("fcs_express_results");

            while (_reader.hasNext())
            {
                switch (_reader.nextTag())
                {
                    case START_ELEMENT:
                        expectStartTag("iteration");
                        Map<String, Object>ret = _readIteration();
                        expectEndTag("iteration");
                        return ret;
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected Map<String, Object> _readIteration() throws XMLStreamException
        {
            expectStartTag("iteration");
            Map<String, Object> iteration = new LinkedHashMap<String, Object>(_activeColumns == null ? 10 : _activeColumns.length*2);

            while (_reader.hasNext())
            {
                switch (_reader.nextTag())
                {
                    case START_ELEMENT:
                    {
                        expectStartTag("exported_item");
                        String itemName = _reader.getAttributeValue(null, "name");
                        if (_shouldLoadColumn(itemName))
                        {
                            Object value = _readExportedItem();
                            iteration.put(itemName, value);
                        }
                        break;
                    }

                    case END_ELEMENT:
                    {
                        String name = _reader.getLocalName();
                        if ("iteration".equalsIgnoreCase(name))
                            return iteration;
                        break;
                    }
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected boolean _shouldLoadColumn(String name)
        {
            // XXX: check ColumnDescriptor.load here
            return true;
        }

        protected Object _readExportedItem() throws XMLStreamException
        {
            expectStartTag("exported_item");

            String type = _reader.getAttributeValue(null, "type");
            if ("token".equals(type))
                return _readToken();
            else if (isFileType(type))
                return null; //_readExportedFile();
            else if ("picture".equalsIgnoreCase(type))
                return null; //_readExportedImage();
            else
                throw new IllegalArgumentException("Unknown exported item type '" + type + "'");
        }

        protected boolean isFileType(String type)
        {
            return "pdf".equalsIgnoreCase(type) ||
                    "layout".equalsIgnoreCase(type) ||
                    "publish".equalsIgnoreCase(type) ||
                    "datafile".equalsIgnoreCase(type) ||
                    "ppt".equalsIgnoreCase(type);
        }

        // <exported_item type="token" name="% of gated cells">
        //   <value>3.54</value>
        // </exported_item>
        protected Object _readToken() throws XMLStreamException
        {
            expectStartTag("exported_item");

            Object value = null;
            while (_reader.hasNext())
            {
                switch (_reader.nextTag())
                {
                    case START_ELEMENT:
                    {
                        String name = _reader.getLocalName();
                        if ("value".equals(name))
                            value = _readTokenValue();
                        break;
                    }

                    case END_ELEMENT:
                    {
                        String name = _reader.getLocalName();
                        if ("exported_item".equalsIgnoreCase(name))
                            return value;
                        break;
                    }
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected Object _readTokenValue() throws XMLStreamException
        {
            expectStartTag("value");
            return _reader.getElementText();
        }

    }

    public static class TestCase
    {
        @Test
        public void parse() throws Exception
        {
            File file = new File("/Users/kevink/data/DeNovo/BatchExport 2.xml");
            FCSExpressDataLoader loader = new FCSExpressDataLoader(file);
            FCSExpressStreamReader r = new FCSExpressStreamReader(loader.getReader());

            Map<String, Object> row = r.readFieldMap();
            Assert.assertEquals("ApoMono.PBS10'1mMCa+AnnPI", row.get("Sample ID File 1"));
        }

        @Test
        public void load() throws IOException
        {
            File file = new File("/Users/kevink/data/DeNovo/BatchExport 2.xml");
            FCSExpressDataLoader loader = new FCSExpressDataLoader(file);

            ColumnDescriptor[] cd = loader.getColumns();
            Assert.assertEquals("Sample ID File 1", cd[0].name);

            List<Map<String, Object>> rows = loader.load();
            Assert.assertEquals("ApoMono.PBS10'1mMCa+AnnPI", rows.get(0).get("Sample ID File 1"));
        }
    }
}
