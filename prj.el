(jde-project-file-version "1.0")
(jde-set-variables
 '(jde-sourcepath
 (quote ("./java/src")))
 '(jde-global-classpath
 (quote ("./build/classes" "./java/lib" "../eXist-db-git/tools/aspectj/lib/aspectjrt-1.7.1.jar" "../eXist-db-git/lib/optional/commons-lang3-3.1.jar" "../eXist-db-git/extensions/debuggee/lib/mina-core-2.0.5.jar" "../eXist-db-git/extensions/security/openid/lib/openid4java-0.9.6.jar" "../eXist-db-git/extensions/security/openid/lib/guice-2.0.jar" "../eXist-db-git/extensions/contentextraction/lib/tika-parsers-1.2.jar" "../eXist-db-git/extensions/contentextraction/lib/tika-core-1.2.jar" "../eXist-db-git/extensions/exquery/lib/exquery-xquery3-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/webdav/lib/milton-servlet-1.8.1.3.jar" "../eXist-db-git/extensions/webdav/lib/milton-client-1.8.1.3.jar" "../eXist-db-git/extensions/webdav/lib/milton-api-1.8.1.3.jar" "../eXist-db-git/lib/optional/servlet-api-3.0.jar" "../eXist-db-git/tools/jetty/lib/jetty-xml-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-websocket-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-webapp-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-util-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-servlets-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-servlet-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-server-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-security-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-plus-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-jmx-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-io-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-http-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/jetty-continuation-8.1.8.v20121106.jar" "../eXist-db-git/tools/jetty/lib/existdb-favicon.jar" "../eXist-db-git/extensions/indexes/lucene/lib/lucene-queries-4.4.0.jar" "../eXist-db-git/extensions/indexes/lucene/lib/lucene-core-4.4.0.jar" "../eXist-db-git/extensions/indexes/lucene/lib/lucene-analyzers-4.4.0.jar" "../eXist-db-git/extensions/contentextraction/lib/metadata-extractor-2.4.0-beta-1.jar" "../eXist-db-git/lib/optional/commons-net-2.2.jar" "../eXist-db-git/lib/core/gnu-crypto-2.0.1-min.jar" "../eXist-db-git/lib/core/commons-codec-1.7.jar" "../eXist-db-git/extensions/security/oauth/lib/org.json.jar" "../eXist-db-git/extensions/security/oauth/lib/scribe-1.3.0.jar" "../eXist-db-git/lib/optional/xqjapi-1.0-fr.jar" "../eXist-db-git/lib/optional/isorelax-20041111.jar" "../eXist-db-git/lib/optional/httpmime-4.1.1.jar" "../eXist-db-git/lib/optional/httpcore-4.1.jar" "../eXist-db-git/lib/optional/httpclient-cache-4.1.1.jar" "../eXist-db-git/lib/optional/httpclient-4.1.jar" "../eXist-db-git/lib/user/icu4j-4_8_1_1.jar" "../eXist-db-git/lib/user/exificient-0.8.jar" "../eXist-db-git/tools/ant/lib/ant-antunit-1.1.jar" "../eXist-db-git/lib/user/xmlgraphics-commons-1.4.jar" "../eXist-db-git/lib/optional/commons-compress-1.4.1.jar" "../eXist-db-git/extensions/webdav/lib/mime-util-2.1.3.jar" "../eXist-db-git/extensions/webdav/lib/jdom-1.1.jar" "../eXist-db-git/lib/user/calabash.jar" "../eXist-db-git/lib/user/ojdbc14.jar" "../eXist-db-git/lib/user/memcached-2.5.jar" "../eXist-db-git/lib/user/svnkit-cli.jar" "../eXist-db-git/lib/user/saxon9he.jar" "../eXist-db-git/lib/test/easymockclassextension-2.5.2.jar" "../eXist-db-git/lib/test/easymock-2.5.2.jar" "../eXist-db-git/lib/user/svnkit.jar" "../eXist-db-git/lib/user/jcommon-1.0.16.jar" "../eXist-db-git/lib/user/jfreechart-1.0.13.jar" "../eXist-db-git/extensions/indexes/spatial/lib/hsqldb-1.8.1.2.jar" "../eXist-db-git/lib/user/nekohtml-1.9.15.jar" "../eXist-db-git/lib/user/batik-all-1.7.jar" "../eXist-db-git/lib/user/activation-1.1.1.jar" "../eXist-db-git/tools/ant/lib/xmlunit-1.3.jar" "../eXist-db-git/lib/user/avalon-framework-impl-4.3.jar" "../eXist-db-git/lib/user/avalon-framework-api-4.3.jar" "../eXist-db-git/extensions/xqdoc/lib/xqdoc_conv.jar" "../eXist-db-git/lib/optional/jing-20091111.jar" "../eXist-db-git/tools/ant/lib/ant-launcher.jar" "../eXist-db-git/lib/user/mail-1.4.4.jar" "../eXist-db-git/lib/user/fop.jar" "../eXist-db-git/lib/optional/commons-discovery-0.5.jar" "../eXist-db-git/lib/core/commons-collections-3.2.1.jar" "../eXist-db-git/lib/core/commons-logging-1.1.1.jar" "../eXist-db-git/lib/core/slf4j-log4j12-1.7.2.jar" "../eXist-db-git/lib/core/slf4j-api-1.7.2.jar" "../eXist-db-git/lib/core/quartz-1.8.6.jar" "../eXist-db-git/lib/core/jta-1.1.jar" "../eXist-db-git/tools/wrapper/lib/wrapper.jar" "../eXist-db-git/tools/ircbot/lib/pircbot.jar" "../eXist-db-git/lib/test/junit-4.8.2.jar" "../eXist-db-git/tools/ant/lib/ant.jar" "../eXist-db-git/lib/optional/wsdl4j-1.5.1.jar" "../eXist-db-git/lib/optional/saaj-1.2.jar" "../eXist-db-git/lib/optional/jaxrpc-1.1.jar" "../eXist-db-git/lib/core/commons-io-2.4.jar" "../eXist-db-git/lib/optional/commons-httpclient-3.1.jar" "../eXist-db-git/lib/optional/commons-fileupload-1.2.2.jar" "../eXist-db-git/lib/optional/axis-1.4.jar" "../eXist-db-git/lib/optional/axis-schema-1.3.jar" "../eXist-db-git/lib/optional/axis-ant-1.4.jar" "../eXist-db-git/lib/core/xmldb.jar" "../eXist-db-git/lib/core/sunxacml-1.2.jar" "../eXist-db-git/lib/core/log4j-1.2.17.jar" "../eXist-db-git/lib/core/jline-0.9.94.jar" "../eXist-db-git/lib/core/excalibur-cli-1.0.jar" "../eXist-db-git/lib/core/antlr-2.7.7.jar" "../eXist-db-git/extensions/replication/lib/activemq-all-5.6.0.jar" "../eXist-db-git/extensions/modules/lib/cql-java-1.10.jar" "../eXist-db-git/extensions/modules/lib/batik-all-1.7.jar" "../eXist-db-git/lib/core/clj-ds-0.0.1.jar" "../eXist-db-git/extensions/exquery/restxq/lib/exquery-restxq-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/restxq/lib/exquery-restxq-api-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/lib/exquery-xquery-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/lib/exquery-serialization-annotations-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/lib/exquery-serialization-annotations-api-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/lib/exquery-annotations-common-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/lib/exquery-annotations-common-api-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/exquery/lib/exquery-common-1.0-SNAPSHOT.jar" "../eXist-db-git/extensions/expath/lib/http-client-java-0.3.0.jar" "../eXist-db-git/extensions/modules/lib/sac-1.3.jar" "../eXist-db-git/extensions/modules/lib/cssparser-0.9.5.jar" "../eXist-db-git/lib/core/xmlrpc-server-3.1.3.jar" "../eXist-db-git/lib/core/xmlrpc-common-3.1.3.jar" "../eXist-db-git/lib/core/xmlrpc-client-3.1.3.jar" "../eXist-db-git/lib/core/ws-commons-util-1.0.2.jar" "../eXist-db-git/lib/core/commons-pool-1.6.jar" "../eXist-db-git/extensions/fluent/lib/jmock-junit4-2.4.0.jar" "../eXist-db-git/extensions/fluent/lib/jmock-2.4.0.jar" "../eXist-db-git/extensions/fluent/lib/hamcrest-library-1.1.jar" "../eXist-db-git/extensions/fluent/lib/hamcrest-core-1.1.jar" "../eXist-db-git/lib/endorsed/xalan-2.7.1.jar" "../eXist-db-git/lib/endorsed/resolver-1.2.jar" "../eXist-db-git/lib/endorsed/xercesImpl-2.11.0.jar" "../eXist-db-git/lib/endorsed/serializer-2.7.1.jar" "../eXist-db-git/lib/endorsed/xml-apis-1.4.01.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-epsg-extension-2.4-M3.jar" "../eXist-db-git/extensions/indexes/spatial/lib/geoapi-nogenerics-2.1-M3.jar" "../eXist-db-git/extensions/indexes/spatial/lib/vecmath-1.3.1.jar" "../eXist-db-git/extensions/indexes/spatial/lib/jts-1.8.jar" "../eXist-db-git/extensions/indexes/spatial/lib/jsr108-0.01.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-xml-2.4-M1.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-referencing-2.4-M1.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-metadata-2.4-M1.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-main-2.4-M1.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-epsg-wkt-2.4-M1.jar" "../eXist-db-git/extensions/indexes/spatial/lib/gt2-api-2.4-M1.jar" "../eXist-db-git/lib/core/cglib-nodep-2.2.jar" "../eXist-db-git/lib/core/jEdit-syntax.jar" "../eXist-db-git/lib/user/smackx.jar" "../eXist-db-git/lib/user/smack.jar" "../eXist-db-git/lib/core/pkg-repo.jar"))))
