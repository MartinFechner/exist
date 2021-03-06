/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2017 The eXist Project
 * http://exist-db.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.exist.util.serializer.json;

import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.util.serializer.SAXSerializer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * Created by aretter on 16/05/2017.
 */
public class JSONWriterTest {

    private static final String EOL = System.getProperty("line.separator");
    private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    static {
        documentBuilderFactory.setIgnoringElementContentWhitespace(false);
    }
    private static final TransformerFactory transformerFactory = new net.sf.saxon.TransformerFactoryImpl();

    @Test
    public void whitespaceTextNodes() throws IOException, TransformerException, ParserConfigurationException, SAXException {

        final Node xmlDoc = parseXml(
                "<a z='99'>" + EOL +
                "    <b x='1'/>" + EOL +
                "    <b x='2'></b>" + EOL +
                "    <b x='3'>stuff</b>" + EOL +
                "    <b x='4'>\t\r\n   \r\n</b>" + EOL +
                "</a>");

        final Properties properties = new Properties();
        properties.setProperty(OutputKeys.METHOD, "json");
        properties.setProperty(OutputKeys.INDENT, "no");

        final SAXSerializer serializer = new SAXSerializer();
        try(final StringWriter writer = new StringWriter()) {
            serializer.setOutput(writer, properties);
            final Transformer transformer = transformerFactory.newTransformer();
            final SAXResult saxResult = new SAXResult(serializer);
            transformer.transform(new DOMSource(xmlDoc), saxResult);

            final String result = writer.toString();

            assertEquals("{\"z\":\"99\",\"#text\":[\"\\n    \",\"\\n    \",\"\\n    \",\"\\n    \",\"\\n\"],\"b\":[{\"x\":\"1\"},{\"x\":\"2\"},{\"x\":\"3\",\"#text\":\"stuff\"},{\"x\":\"4\",\"#text\":\"\\t\\n   \\n\"}]}", result);
        }
    }

    @Test
    public void ignoreWhitespaceTextNodes() throws IOException, TransformerException, ParserConfigurationException, SAXException {

        final Node xmlDoc = parseXml(
                "<a z='99'>" + EOL +
                        "    <b x='1'/>" + EOL +
                        "    <b x='2'></b>" + EOL +
                        "    <b x='3'>stuff</b>" + EOL +
                        "    <b x='4'>\t\r\n   \r\n</b>" + EOL +
                        "</a>");

        final Properties properties = new Properties();
        properties.setProperty(OutputKeys.METHOD, "json");
        properties.setProperty(EXistOutputKeys.JSON_IGNORE_WHITESPACE_TEXT_NODES, "yes");
        properties.setProperty(OutputKeys.INDENT, "no");

        final SAXSerializer serializer = new SAXSerializer();
        try(final StringWriter writer = new StringWriter()) {
            serializer.setOutput(writer, properties);
            final Transformer transformer = transformerFactory.newTransformer();
            final SAXResult saxResult = new SAXResult(serializer);
            transformer.transform(new DOMSource(xmlDoc), saxResult);

            final String result = writer.toString();

            assertEquals("{\"z\":\"99\",\"b\":[{\"x\":\"1\"},{\"x\":\"2\"},{\"x\":\"3\",\"#text\":\"stuff\"},{\"x\":\"4\"}]}", result);
        }
    }

    private Document parseXml(final String xmlStr) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try(final InputStream is = new ByteArrayInputStream(xmlStr.getBytes(UTF_8))) {
            return documentBuilder.parse(is);
        }
    }
}
