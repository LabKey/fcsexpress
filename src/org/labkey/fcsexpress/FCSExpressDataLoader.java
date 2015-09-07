/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.input.BOMInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.iterator.CloseableIterator;
import org.labkey.api.reader.AbstractDataLoaderFactory;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.XMLFileType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
    public static final XMLFileType FILE_TYPE = new XMLFileType("", "fcs_express_results");

    public static class Factory extends AbstractDataLoaderFactory
    {
        @NotNull
        @Override
        public DataLoader createLoader(InputStream is, boolean hasColumnHeaders, Container mvIndicatorContainer) throws IOException
        {
            return new FCSExpressDataLoader(is, null, mvIndicatorContainer);
        }

        @NotNull
        @Override
        public DataLoader createLoader(File file, boolean hasColumnHeaders, Container mvIndicatorContainer) throws IOException
        {
            return new FCSExpressDataLoader(file, null, mvIndicatorContainer);
        }

        @NotNull
        @Override
        public FileType getFileType()
        {
            return FILE_TYPE;
        }
    }

    InputStream _is;
    File _extractFileRoot;

    public FCSExpressDataLoader(File inputFile, File extractFileRoot) throws IOException
    {
        super();
        setSource(inputFile);
        setScrollable(true);

        if (extractFileRoot != null && !extractFileRoot.isDirectory())
            throw new IllegalArgumentException("Extract directory doesn't exist");
        _extractFileRoot = extractFileRoot;
    }

    public FCSExpressDataLoader(File inputFile, File extractFileRoot, Container mvIndicatorContainer) throws IOException
    {
        super(mvIndicatorContainer);
        setSource(inputFile);
        setScrollable(true);

        if (extractFileRoot != null && !extractFileRoot.isDirectory())
            throw new IllegalArgumentException("Extract directory doesn't exist");
        _extractFileRoot = extractFileRoot;
    }

    public FCSExpressDataLoader(InputStream is, File extractFileRoot, Container mvIndicatorContainer) throws IOException
    {
        super(mvIndicatorContainer);
        if (is.markSupported())
            _is = is;
        else
            _is = new BufferedInputStream(is);
        setScrollable(false);

        if (extractFileRoot != null && !extractFileRoot.isDirectory())
            throw new IllegalArgumentException("Extract directory doesn't exist");
        _extractFileRoot = extractFileRoot;
    }

    @Override
    public String[][] getFirstNLines(int n) throws IOException
    {
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
            // While inferring files, use null extractFileRoot so binary files aren't saved.
            FCSExpressStreamReader reader = new FCSExpressStreamReader(xml, null);
            Map<String, Object> row = reader.readFieldMap(null);
            if (row == null)
            {
                _columns = new ColumnDescriptor[0];
            }
            else
            {
                ArrayList<ColumnDescriptor> cols = new ArrayList<>(row.size());

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
        if (_is != null)
        {
            try
            {
                _is.close();
            }
            catch (IOException e)
            {
                // Ignore
            }
        }
    }

    // TODO: Replace with ReaderFactory similar to TabLoader
    protected XMLStreamReader getReader() throws IOException, XMLStreamException
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        InputStream is;
        if (null != _is)
        {
            // We don't close handed in readers
            _is.reset();
            is = new BufferedInputStream(_is)
            {
                @Override
                public void close() throws IOException
                {
                }
            };
        }
        else
        {
            FileInputStream fis = new FileInputStream(_file);
            is = new BufferedInputStream(new BOMInputStream(fis));
        }

        return factory.createXMLStreamReader(is);
    }

    protected class FCSExpressIterator extends DataLoaderIterator
    {
        private FCSExpressStreamReader _parser;

        protected FCSExpressIterator() throws IOException, XMLStreamException
        {
            super(0);
            init();

            _parser = new FCSExpressStreamReader(getReader(), _extractFileRoot, _activeColumns);
        }

        protected void init()
        {
        }

        @Override
        protected Object[] readFields() throws IOException
        {
            try
            {
                return _parser.readFields(_columns);
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
        // Setting extract file root to null won't extract any files while reading.
        private final File _extractFileRoot;

        protected FCSExpressStreamReader(XMLStreamReader reader, File extractFileRoot) throws IOException, XMLStreamException
        {
            _reader = reader;
            _extractFileRoot = extractFileRoot;
            _activeColumns = null;
        }

        protected FCSExpressStreamReader(XMLStreamReader reader, File extractFileRoot, ColumnDescriptor[] activeColumns) throws IOException, XMLStreamException
        {
            _reader = reader;
            _extractFileRoot = extractFileRoot;
            _activeColumns = activeColumns;
        }

        protected Object[] readFields(@Nullable ColumnDescriptor[] columns) throws XMLStreamException
        {
            Map<String, Object> row = readFieldMap(columns);
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

        private void expectStartTag(String name) throws XMLStreamException
        {
            if (_reader.isStartElement() && !name.equalsIgnoreCase(_reader.getLocalName()))
            {
                String actual = _reader.hasName() ? _reader.getLocalName() : null;
                throw new IllegalArgumentException("Expected start element '" + name + "', found '" + actual + "'");
            }
        }

        private void expectEndTag(String name) throws XMLStreamException
        {
            if (_reader.isEndElement() && !name.equalsIgnoreCase(_reader.getLocalName()))
            {
                String actual = _reader.hasName() ? _reader.getLocalName() : null;
                throw new IllegalArgumentException("Expected end element '" + name + "', found '" + actual + "'");
            }
        }

        // Called when inferring fields
        protected Map<String, Object> readFieldMap(@Nullable ColumnDescriptor[] columns) throws XMLStreamException
        {
            if (!_reader.hasNext())
                return null;

            while (_reader.hasNext())
            {
                switch (_reader.next())
                {
                    case START_ELEMENT:
                    {
                        if ("fcs_express_results".equals(_reader.getLocalName()))
                            continue;

                        if ("iteration".equals(_reader.getLocalName()))
                            return _readIteration(columns);

                        throw new IllegalArgumentException("Unexpected start element: " + _reader.getLocalName());
                    }

                    case END_ELEMENT:
                    {
                        // End of empty iteration
                        if ("iteration".equals(_reader.getLocalName()))
                            return null;

                        // End of results
                        if ("fcs_express_results".equals(_reader.getLocalName()))
                            return null;

                        throw new IllegalArgumentException("Unexpected end element: " + _reader.getLocalName());
                    }

                    case END_DOCUMENT:
                        return null;
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected Map<String, Object> _readIteration(@Nullable ColumnDescriptor[] columns) throws XMLStreamException
        {
            expectStartTag("iteration");
            Map<String, Object> iteration = new LinkedHashMap<>(_activeColumns == null ? 10 : _activeColumns.length*2);

            int number = Integer.parseInt(_reader.getAttributeValue(null, "number"));
            int colIndex = 0;

            while (_reader.hasNext())
            {
                switch (_reader.nextTag())
                {
                    case START_ELEMENT:
                    {
                        expectStartTag("exported_item");
                        String itemName = _reader.getAttributeValue(null, "name");

                        boolean loadThisColumn = null==columns || colIndex >= columns.length || columns[colIndex].load;
                        Object value = _readExportedItem(number, itemName, loadThisColumn);
                        if (loadThisColumn)
                            iteration.put(itemName, value);

                        colIndex++;
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

        protected Object _readExportedItem(int iterationNumber, String itemName, boolean loadThisColumn) throws XMLStreamException
        {
            expectStartTag("exported_item");

            String type = _reader.getAttributeValue(null, "type");
            if ("token".equals(type))
                return _readToken();
            else if (isFileType(type))
                return _readExportedFile(iterationNumber, itemName, type, loadThisColumn);
            else if ("picture".equalsIgnoreCase(type))
                return _readExportedImage(iterationNumber, itemName, loadThisColumn);
            else
                throw new IllegalArgumentException("Unknown exported item type '" + type + "'");
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

        protected String _readTokenValue() throws XMLStreamException
        {
            expectStartTag("value");
            return _reader.getElementText();
        }

        protected boolean isFileType(String type)
        {
            return "pdf".equalsIgnoreCase(type) ||
                    "ppt".equalsIgnoreCase(type) ||
                    "txt".equalsIgnoreCase(type) ||
                    "layout".equalsIgnoreCase(type) ||
                    "publish".equalsIgnoreCase(type) ||
                    "datafile".equalsIgnoreCase(type);
        }

        protected String extensionForType(String type)
        {
            if ("pdf".equalsIgnoreCase(type))
                return ".pdf";
            if ("ppt".equalsIgnoreCase(type))
                return ".ppt";
            if ("txt".equalsIgnoreCase(type))
                return ".txt";
            if ("layout".equalsIgnoreCase(type))
                return ".fey";
            if ("publish".equalsIgnoreCase(type))
                return ".fey";
            if ("datafile".equalsIgnoreCase(type))
                return ".fcs";
            return "";
        }

        protected File _readExportedFile(int iterationNumber, String itemName, String type, boolean loadThisColumn) throws XMLStreamException
        {
            expectStartTag("exported_item");

            String filename = "iteration" + iterationNumber + "/" + FileUtil.makeLegalName(itemName) + extensionForType(type);
            File file = new File(_extractFileRoot, filename);

            while (_reader.hasNext())
            {
                switch (_reader.nextTag())
                {
                    case START_ELEMENT:
                        expectStartTag("data_source");
                        _readDataSource(file, loadThisColumn);
                        break;

                    case END_ELEMENT:
                        expectStartTag("data_source");
                        return file;
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected String extensionForPictureFormat(String format)
        {
            if (format.equals("PNG"))
                return ".png";
            if (format.equals("TIF") || format.equals("TIFF"))
                return ".tif";
            if (format.equals("JPG") || format.equals("JPEG"))
                return ".jpg";
            if (format.equals("Bitmap") || format.equals("BMP"))
                return ".bmp";
            if (format.equals("Metafile"))
                return ".emf";
            return "." + format.toLowerCase();
        }

        protected File _readExportedImage(int iterationNumber, String itemName, boolean loadThisColumn) throws XMLStreamException
        {
            expectStartTag("exported_item");

            String format = null;
            int resolution;
            int width;
            int height;
            File file = null;

            while (_reader.hasNext())
            {
                int event = _reader.nextTag();
                String localName = _reader.getLocalName();
                switch (event)
                {
                    case START_ELEMENT:
                        if ("format".equals(localName))
                            format = _reader.getElementText();
                        else if ("resolution".equals(localName))
                            resolution = Integer.parseInt(_reader.getElementText());
                        else if ("width".equals(localName))
                            width = Integer.parseInt(_reader.getElementText());
                        else if ("height".equals(localName))
                            height = Integer.parseInt(_reader.getElementText());
                        else if ("data_source".equals(localName))
                        {
                            if (format == null || format.equals(""))
                                throw new IllegalStateException("Picture <format> element must appear before <data_source> element");
                            String filename = "iteration" + iterationNumber + "/" + FileUtil.makeLegalName(itemName) + extensionForPictureFormat(format);
                            file = new File(_extractFileRoot, filename);
                            _readDataSource(file, loadThisColumn);
                        }
                        break;

                    case END_ELEMENT:
                        expectStartTag("data_source");
                        if (file == null)
                            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
                        return file;
                }
            }

            throw new IllegalArgumentException("Failed to parse FCSExpress export xml");
        }

        protected void _readDataSource(File file, boolean loadThisColumn) throws XMLStreamException
        {
            expectStartTag("data_source");

            String type = _reader.getAttributeValue(null, "type");
            if (type == null || type.length() == 0)
                throw new IllegalArgumentException("Expected data_source type 'base64'");
            if (!"base64".equals(type))
                throw new IllegalArgumentException("Unexpected data_source type '" + type + "'");

            while (_reader.hasNext())
            {
                switch (_reader.next())
                {
                    case START_ELEMENT:
                        expectStartTag("base64_data");
                        // Only save data if we have an extraction root directory.
                        if (_extractFileRoot != null && loadThisColumn)
                        {
                            _writeBase64(file);
                            expectEndTag("base64_data");
                            return;
                        }
                        break;

                    case END_ELEMENT:
                        // If we're not extracting data, continue reading until we see the base64_data end element.
                        expectEndTag("base64_data");
                        return;
                }
            }
        }

        protected void _writeBase64(File file) throws XMLStreamException
        {
            expectStartTag("base64_data");
            assert _extractFileRoot != null && _extractFileRoot.isDirectory();

            // Advance to text content
            _reader.next();
            if (!_reader.isCharacters())
                throw new IllegalArgumentException("Expected base64 encoded data");

            File parent = file.getParentFile();
            if (!parent.exists())
                if (!parent.mkdirs())
                    throw new RuntimeException("Failed to create directory '" + parent + "'");

            try (PrintWriter pw = new PrintWriter(new Base64OutputStream(new BufferedOutputStream(new FileOutputStream(file)), false)))
            {
                // Base64OutputStream configured to decode while writing to file.
                do
                {
                    if (!_reader.isCharacters())
                        break;
                    String text = _reader.getText();
                    pw.write(text);
                }
                while (_reader.next() == XMLStreamConstants.CHARACTERS);

                pw.flush();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            expectEndTag("base64_data");
        }
    }

    public static class TestCase
    {
        @Test
        public void parse() throws Exception
        {
            File file = new File("/Users/kevink/flow/DeNovo/BatchExport.xml");
            FCSExpressDataLoader loader = new FCSExpressDataLoader(file, null);
            FCSExpressStreamReader r = new FCSExpressStreamReader(loader.getReader(), null);

            Map<String, Object> row = r.readFieldMap(null);
            Assert.assertEquals("ApoMono.PBS10'1mMCa+AnnPI", row.get("Sample ID"));
            Assert.assertEquals(new File("iteration1/Gate 1 text.txt"), row.get("Gate 1 text"));
        }

        @Test
        public void load() throws IOException
        {
            File file = new File("/Users/kevink/flow/DeNovo/BatchExport.xml");
            FCSExpressDataLoader loader = new FCSExpressDataLoader(file, new File("/Users/kevink/BatchExport"));

            ColumnDescriptor[] cd = loader.getColumns();
            Assert.assertEquals("FCSFile", cd[0].name);
            Assert.assertEquals("Sample ID", cd[1].name);

            Assert.assertEquals("Ungated PNG", cd[4].name);
            Assert.assertEquals(File.class, cd[4].clazz);

            List<Map<String, Object>> rows = loader.load();
            Assert.assertEquals("ApoMono.PBS10'1mMCa+AnnPI", rows.get(0).get("Sample ID"));
            Assert.assertEquals(new File("iteration1/Ungated PNG.png"), rows.get(0).get("Ungated PNG"));
        }
    }
}
