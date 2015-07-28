/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2007 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 *  $Id$
 *  
 *  @author Pierrick Brihaye <pierrick.brihaye@free.fr>
 */
package org.exist.indexing.geospatial;

import java.io.File;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.exist.EXistException;
import org.exist.dom.persistent.NodeSet;
import org.exist.indexing.geospatial.AbstractGMLJDBCIndex.SpatialOperator;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.security.xacml.AccessContext;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xmldb.IndexQueryService;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.geotools.gml.GMLFilterDocument;
import org.geotools.gml.GMLFilterGeometry;
import org.geotools.gml.GMLHandlerJTS;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 */
public class GMLIndexTest extends TestCase {

    private final static String FILES[] = { "15385-SS7886-5i1.gml" };
    static File existDir;
    static {
        String existHome = System.getProperty("exist.home");
        existDir = existHome == null ? new File(".") : new File(existHome);
    }
    
    private final static File RESOURCE_DIR_DIR = new File(existDir, "webapp/geospatial/resources");
    private static final String TEST_COLLECTION_NAME = "test-geospatial-index";

    private static String COLLECTION_CONFIG =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
        "   <index>" +
        "        <gml/>" +
        "   </index>" +
        "   <validation mode=\"no\"/> " +
    	"</collection>";

    String IN_MEMORY_GML = "<gml:Polygon xmlns:gml = 'http://www.opengis.net/gml' srsName='osgb:BNG'>" +
    "  <gml:outerBoundaryIs><gml:LinearRing><gml:coordinates>" +
    "278515.400,187060.450 278515.150,187057.950 278516.350,187057.150 " +
    "278546.700,187054.000 278580.550,187050.900 278609.500,187048.100 " +
    "278609.750,187051.250 278574.750,187054.650 278544.950,187057.450 " +
    "278515.400,187060.450 " +
    "   </gml:coordinates></gml:LinearRing></gml:outerBoundaryIs>" +
    "</gml:Polygon>";
    
    String WKT_POLYGON = "POLYGON ((-3.7530493069563913 51.5695210244188, " +
    "-3.7526220716233705 51.569500427086325, -3.752191300029012 51.569481679670055, " +
    "-3.7516853221460167 51.5694586575048, -3.751687839470607 51.569430291017945, " +
    "-3.752106350923544 51.56944922336166, -3.752595638781826 51.5694697950237, " +
    "-3.753034464037513 51.56949156828257, -3.753052048201362 51.56949850020053, " +
    "-3.7530493069563913 51.5695210244188))";
    
    private Database database;
    private Collection testCollection;
    private Geometry currentGeometry;

    public void testIndexDocument() {
        BrokerPool pool = null;
        DBBroker broker = null;	
        try {
            for (int i = 0; i < FILES.length; i++) {
                XMLResource doc =
                    (XMLResource) testCollection.createResource(
                            FILES[i], "XMLResource" );
                //Doh ! Setting a new content doesn't remove the old one if any !
                if (testCollection.getResource(FILES[i]) != null)
                    testCollection.removeResource(doc);
                doc.setContent(new File(RESOURCE_DIR_DIR, FILES[i]));
                testCollection.storeResource(doc);
                assertNotNull(testCollection.getResource(FILES[i]));
            }
            
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            assertNotNull(broker);
            GMLHSQLIndexWorker indexWorker = (GMLHSQLIndexWorker)broker.getIndexController().getWorkerByIndexId(AbstractGMLJDBCIndex.ID);
            //Unplugged
            if (indexWorker != null) {
                try {
                    Connection conn = null;
                    try {
                        conn = indexWorker.acquireConnection();	
                        for (int i = 0; i < FILES.length; i++) {
                            XMLResource doc =
                                (XMLResource) testCollection.getResource(FILES[i]);
                            PreparedStatement ps = conn.prepareStatement(
                                "SELECT * FROM " + GMLHSQLIndex.TABLE_NAME + " WHERE DOCUMENT_URI = ?;"
                            );
                            ps.setString(1, testCollection.getName() + "/" + doc.getDocumentId());
                            ResultSet rs = ps.executeQuery();
                            while (rs.next()) {
                                //Let be sure we have the right count
                            }
                            int count = rs.getRow();
                            ps.close();
                            assertTrue(count > 0);
                        }
                    } finally {
                        indexWorker.releaseConnection(conn);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        } catch (XMLDBException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (EXistException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (pool !=null)
                pool.release(broker);
        }
    }
    
    public void testCheckIndex() {
        BrokerPool pool = null;
        DBBroker broker = null;
        try {	
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            AbstractGMLJDBCIndex index = (AbstractGMLJDBCIndex)pool.getIndexManager().getIndexById(AbstractGMLJDBCIndex.ID);
            //Unplugged
            if (index != null) {
                assertTrue(index.checkIndex(broker));
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (pool !=null)
                pool.release(broker);
        }
    }
    
    public void testScanIndex() {
        BrokerPool pool = null;
        DBBroker broker = null;
        try {
            pool = BrokerPool.getInstance();
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            XQuery xquery = pool.getXQueryService();
            assertNotNull(xquery);
            Sequence seq = xquery.execute(
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "declare function local:key-callback($term as xs:string, $data as xs:int+) as element() { " +
                "   <entry>" +
                "     <term>{$term}</term>" +
                "     <frequency>{$data[1]}</frequency>" +
                "     <documents>{$data[2]}</documents>" +
                "     <position>{$data[3]}</position>" +
                "   </entry> " +
                "}; " +
                //"util:index-keys(//gml:*, '', util:function(xs:QName('local:key-callback'), 2), 1000, 'geospatial-index')[entry/frequency > 1] ",
                "util:index-keys(//gml:*, '', util:function(xs:QName('local:key-callback'), 2), 1000, 'geospatial-index') ",
                null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (pool!=null) pool.release(broker);
        }
    }
    
    public void testLowLevelSearch() {
    	GMLHandlerJTS geometryHandler = new GeometryHandler();
        GMLFilterGeometry geometryFilter = new GMLFilterGeometry(geometryHandler);
        GMLFilterDocument handler = new GMLFilterDocument(geometryFilter);
        BrokerPool pool = null;
        DBBroker broker = null;
        try {
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            assertNotNull(broker);
            AbstractGMLJDBCIndexWorker indexWorker = (AbstractGMLJDBCIndexWorker)broker.getIndexController().getWorkerByIndexId(AbstractGMLJDBCIndex.ID);
            //Unplugged
            if (indexWorker != null) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                InputSource src = new InputSource(new StringReader(IN_MEMORY_GML));
                SAXParser parser = factory.newSAXParser();
                XMLReader reader = parser.getXMLReader();
                SAXAdapter adapter = new SAXAdapter();
                reader.setContentHandler(handler);
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", adapter);
                reader.parse(src);
                
                Geometry EPSG4326_geometry = indexWorker.transformGeometry(currentGeometry, "osgb:BNG", "EPSG:4326");
                assertNotNull(EPSG4326_geometry);
                
                NodeSet ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.EQUALS);
                assertTrue(ns.getLength() > 0);
                ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.DISJOINT);
                assertTrue(ns.getLength() > 0);
                ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.INTERSECTS);
                assertTrue(ns.getLength() > 0);
                ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.TOUCHES);
                //assertTrue(ns.getLength() > 0);
                ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.CROSSES);
                //assertTrue(ns.getLength() > 0);
                ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.WITHIN);
                assertTrue(ns.getLength() > 0);
                ns = indexWorker.search(broker, null, EPSG4326_geometry, SpatialOperator.CONTAINS);
                assertTrue(ns.getLength() > 0);
                //ns = ((GMLIndexWorker)index.getWorker()).search(broker, EPSG4326_geometry, SpatialOperator.OVERLAPS);
                //assertTrue(ns.getLength() > 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage()); 
        } finally {
            if (pool !=null)
                pool.release(broker);
        }
    }
  
    public void testHighLevelSearch() {
        BrokerPool pool = null;
        DBBroker broker = null;
        try {
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            XQuery xquery = pool.getXQueryService();
            assertNotNull(xquery);
            String query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:equals(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:disjoint(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:intersects(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:touches(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            //assertTrue(seq.getItemCount() > 0); 
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:crosses(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            //assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:within(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:contains(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:overlaps(//gml:*, //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            //assertTrue(seq.getItemCount() > 0);
            
            //Tests with empty sequences
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:equals(//gml:*, ())";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:overlaps((), //gml:Point[gml:coordinates[. = '278697.450,187740.900']])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            
            //In-memory test
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:equals(//gml:*, " + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (pool !=null)
                pool.release(broker);
        }
    }
    
    public void testGeometricProperties() {
        BrokerPool pool = null;
        DBBroker broker = null;
        try {
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            XQuery xquery = pool.getXQueryService();
            assertNotNull(xquery);
            String query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKT((//gml:Polygon)[1])";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKB((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);	
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMinX((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMaxX((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMinY((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMaxY((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getCentroidX((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getCentroidY((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getArea((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326WKT((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326WKB((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MinX((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MaxX((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MinY((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MaxY((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326CentroidX((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326CentroidY((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326Area((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getSRS((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getGeometryType((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:isClosed((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:isSimple((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:isValid((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);

            //Tests with empty sequences
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKT(())";	
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getArea(())";	
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() == 0);
            //In-memory tests
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKT(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKB(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);	        
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMinX(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMaxX(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMinY(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getMaxY(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getCentroidX(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getCentroidY(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getArea(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326WKT(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326WKB(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MinX(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MaxX(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MinY(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326MaxY(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326CentroidX(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326CentroidY(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getEPSG4326Area(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getSRS(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getGeometryType(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:isClosed(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:isSimple(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:isValid(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage()); 
        } finally {
            if (pool !=null)
                pool.release(broker);
        }
    }
    
    public void testGMLProducers() {
        BrokerPool pool = null;
        DBBroker broker = null;
        try {
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            XQuery xquery = pool.getXQueryService();
            assertNotNull(xquery);
            String query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:transform((//gml:Polygon)[1], 'EPSG:4326')";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKT((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:buffer((//gml:Polygon)[1], 100)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:buffer((//gml:Polygon)[1], 100, 1)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getBbox((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:convexHull((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:boundary((//gml:Polygon)[1])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:intersection((//gml:Polygon)[1], (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:union((//gml:Polygon)[1], (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:difference((//gml:Polygon)[1], (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:symetricDifference((//gml:Polygon)[1], (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            
            //Tests with empty sequences
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:transform((), 'EPSG:4326')";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getWKT(())";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:buffer((), 100)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:buffer((), 100, 1)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getBbox(())";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:convexHull(())";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:boundary(())";	
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:union((), ())";	
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:union((//gml:Polygon)[1], ())";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 1);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:union((), (//gml:Polygon)[1])";	
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 1);

            //In-memory tests
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:transform(" + IN_MEMORY_GML + ", 'EPSG:4326')";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:buffer(" + IN_MEMORY_GML + ", 100)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:buffer(" + IN_MEMORY_GML + ", 100, 1)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:getBbox(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:convexHull(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:boundary(" + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:intersection(" + IN_MEMORY_GML + ", (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:union(" + IN_MEMORY_GML + ", (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:difference(" + IN_MEMORY_GML + ", (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:symetricDifference(" + IN_MEMORY_GML + ", (//gml:Polygon)[2])";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:intersection((//gml:Polygon)[1]," + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:union((//gml:Polygon)[1]," + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:difference((//gml:Polygon)[1]," + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "geospatial:symetricDifference((//gml:Polygon)[1]," + IN_MEMORY_GML + ")";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() > 0);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage()); 
        } finally {	
            if (pool !=null)
                pool.release(broker);
        }
    }    

    public void testUpdate() {
        BrokerPool pool = null;
        DBBroker broker = null;
        try {
            pool = BrokerPool.getInstance();
            assertNotNull(pool);
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            XQuery xquery = pool.getXQueryService();
            assertNotNull(xquery);
            String query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "(# exist:force-index-use #) { " +
                "geospatial:getArea((//gml:Polygon)[1]) " +
                "}";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);		
            assertTrue(seq.getItemCount() == 1);
            String area1 = seq.toString();
            query =	"import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "update value (//gml:Polygon)[1]/gml:outerBoundaryIs/gml:LinearRing/gml:coordinates " +
                "(: strip decimals :) " +
                "with fn:replace((//gml:Polygon)[1], '(\\d+).(\\d+)', '$1')";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 0);
            query = "import module namespace geospatial='http://exist-db.org/xquery/geospatial' " +
                "at 'java:org.exist.examples.indexing.geospatial.module.GeoSpatialModule'; " +
                "declare namespace gml = 'http://www.opengis.net/gml'; " +
                "(# exist:force-index-use #) { " +
                "geospatial:getArea((//gml:Polygon)[1])" +
                "}";
            seq = xquery.execute(query, null, AccessContext.TEST);
            assertNotNull(seq);
            assertTrue(seq.getItemCount() == 1);
            String area2 = seq.toString();
            assertFalse(area1.equals(area2));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (pool !=null)
                pool.release(broker);
        }
    }

    @Override
    protected void setUp() {
        try {
            // initialize driver
            Class<?> cl = Class.forName("org.exist.xmldb.DatabaseImpl");
            database = (Database) cl.newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);
            
            Collection root =
                DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "");
            CollectionManagementService service =
                (CollectionManagementService) root.getService(
                        "CollectionManagementService",
                "1.0");
            testCollection = root.getChildCollection(TEST_COLLECTION_NAME);
            if (testCollection == null) {
                testCollection = service.createCollection(TEST_COLLECTION_NAME);
                assertNotNull(testCollection);
                IndexQueryService idxConf = (IndexQueryService)
                testCollection.getService("IndexQueryService", "1.0");
                idxConf.configureCollection(COLLECTION_CONFIG);
            }
            
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (XMLDBException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() {	
        try {
            DatabaseManager.deregisterDatabase(database);
        } catch (XMLDBException e) {
            e.printStackTrace();
        }
    }
    
    private class GeometryHandler extends XMLFilterImpl implements GMLHandlerJTS {
        public void geometry(Geometry geometry) {
            currentGeometry = geometry;
        }
    }
}
