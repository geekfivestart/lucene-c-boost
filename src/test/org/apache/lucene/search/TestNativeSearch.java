package org.apache.lucene.search;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NativeMMapDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class TestNativeSearch extends LuceneTestCase {

  private void assertSameHits(TopDocs expected, TopDocs actual) {
    assertEquals(expected.totalHits, actual.totalHits);
    assertEquals(expected.getMaxScore(), actual.getMaxScore(), 0.000001f);
    assertEquals(expected.scoreDocs.length, actual.scoreDocs.length);
    for(int i=0;i<expected.scoreDocs.length;i++) {
      assertEquals("hit " + i, expected.scoreDocs[i].doc, actual.scoreDocs[i].doc);
      // nocommit why not exactly the same?
      //assertEquals("hit " + i, expected.scoreDocs[i].score, actual.scoreDocs[i].score, 0.0f);
      assertEquals("hit " + i, expected.scoreDocs[i].score, actual.scoreDocs[i].score, 0.00001f);
    }
  }

  private void assertSameHits(IndexSearcher s, Query q) throws IOException {
    // First with only top 10:
    TopDocs expected = s.search(q, 10);
    TopDocs actual = NativeSearch.searchNative(s, q, 10);
    assertSameHits(expected, actual);
    
    // First with only top 10:
    int maxDoc = s.getIndexReader().maxDoc();
    expected = s.search(q, maxDoc);
    actual = NativeSearch.searchNative(s, q, maxDoc);
    assertSameHits(expected, actual);
  }

  public void testBasicBooleanQuery() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "a b c", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "a b x", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "x", Field.Store.NO));
    w.addDocument(doc);
    IndexReader r = DirectoryReader.open(w, true);
    w.close();
    IndexSearcher s = new IndexSearcher(r);

    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "c")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "x")), BooleanClause.Occur.SHOULD);

    assertSameHits(s, bq);

    // no matches:
    bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("field", "p")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "p")), BooleanClause.Occur.SHOULD);
    assertSameHits(s, bq);
    
    r.close();
    dir.close();
  }

  public void testBasicBooleanQueryWithDeletes() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "a b c", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "a b x", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "x", Field.Store.NO));
    doc.add(new StringField("id", "0", Field.Store.NO));
    w.addDocument(doc);
    w.deleteDocuments(new Term("id", "0"));
    IndexReader r = DirectoryReader.open(w, true);
    w.close();
    IndexSearcher s = new IndexSearcher(r);

    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "c")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "x")), BooleanClause.Occur.SHOULD);

    assertSameHits(s, bq);

    // no matches:
    bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("field", "p")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "p")), BooleanClause.Occur.SHOULD);
    assertSameHits(s, bq);
    
    r.close();
    dir.close();
  }

  public void testBasicBooleanQuery2() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    int numDocs = 140;
    for(int i=0;i<numDocs;i++) {
      Document doc = new Document();
      doc.add(new TextField("field", "a b c", Field.Store.NO));
      w.addDocument(doc);
    }

    IndexReader r = DirectoryReader.open(w, true);
    w.close();

    IndexSearcher s = new IndexSearcher(r);

    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "c")), BooleanClause.Occur.SHOULD);

    assertSameHits(s, bq);

    r.close();
    dir.close();
  }

  public void testBasicBooleanQuery3() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    int numDocs = atLeast(10000);
    for(int i=0;i<numDocs;i++) {
      Document doc = new Document();
      StringBuilder content = new StringBuilder();
      content.append(" a");
      if ((i % 2) == 0) {
        content.append(" b");
      }
      if ((i % 4) == 0) {
        content.append(" c");
      }
      if ((i % 8) == 0) {
        content.append(" d");
      }
      if ((i % 16) == 0) {
        content.append(" e");
      }
      if ((i % 32) == 0) {
        content.append(" f");
      }
      doc.add(new TextField("field", content.toString(), Field.Store.NO));
      w.addDocument(doc);
    }
    w.commit();
    w.close();

    while(true) {
      
      //System.out.println("\nTEST: open reader");
      IndexReader r = DirectoryReader.open(dir);
      IndexSearcher s = new IndexSearcher(r);

      for(int i=0;i<10000;i++) {
        //if (i % 100 == 0) {
        //System.out.println("  " + i + "...");
        //}
        BooleanQuery bq = new BooleanQuery();
        bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
        bq.add(new TermQuery(new Term("field", "b")), BooleanClause.Occur.SHOULD);
        assertSameHits(s, bq);

        assertSameHits(s, new ConstantScoreQuery(bq));

        bq = new BooleanQuery();
        bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
        bq.add(new TermQuery(new Term("field", "c")), BooleanClause.Occur.SHOULD);
        assertSameHits(s, bq);

        bq = new BooleanQuery();
        bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
        bq.add(new TermQuery(new Term("field", "d")), BooleanClause.Occur.SHOULD);
        assertSameHits(s, bq);

        bq = new BooleanQuery();
        bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
        bq.add(new TermQuery(new Term("field", "e")), BooleanClause.Occur.SHOULD);
        assertSameHits(s, bq);

        bq = new BooleanQuery();
        bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
        bq.add(new TermQuery(new Term("field", "f")), BooleanClause.Occur.SHOULD);
        assertSameHits(s, bq);

        assertSameHits(s, new TermQuery(new Term("field", "a")));
        assertSameHits(s, new TermQuery(new Term("field", "b")));
        assertSameHits(s, new TermQuery(new Term("field", "c")));
        assertSameHits(s, new TermQuery(new Term("field", "d")));
        assertSameHits(s, new TermQuery(new Term("field", "e")));
        assertSameHits(s, new TermQuery(new Term("field", "f")));

        assertSameHits(s, new ConstantScoreQuery(new TermQuery(new Term("field", "a"))));
        // comment out to test for mem leaks:
        break;
      }

      //System.out.println("\nTEST: close reader");

      r.close();
      // comment out to test for mem leaks:
      break;
    }

    dir.close();
  }

  public void testBasicTermQuery() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "a b c", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "a b x", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "x", Field.Store.NO));
    w.addDocument(doc);
    IndexReader r = DirectoryReader.open(w, true);
    w.close();
    IndexSearcher s = new IndexSearcher(r);

    assertSameHits(s, new TermQuery(new Term("field", "a")));

    r.close();
    dir.close();
  }

  public void testConstantScoreTermQuery() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "a b c", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "a b x", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "x", Field.Store.NO));
    w.addDocument(doc);
    IndexReader r = DirectoryReader.open(w, true);
    w.close();
    IndexSearcher s = new IndexSearcher(r);

    assertSameHits(s, new ConstantScoreQuery(new TermQuery(new Term("field", "a"))));

    r.close();
    dir.close();
  }

  public void testConstantScoreBooleanQuery() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "a b c", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "a b x", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "x", Field.Store.NO));
    w.addDocument(doc);
    IndexReader r = DirectoryReader.open(w, true);
    w.close();
    IndexSearcher s = new IndexSearcher(r);

    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("field", "x")), BooleanClause.Occur.SHOULD);
    assertSameHits(s, new ConstantScoreQuery(bq));

    r.close();
    dir.close();
  }

  public void testTermQueryWithDelete() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "a b c", Field.Store.NO));
    w.addDocument(doc);
    doc = new Document();
    doc.add(new TextField("field", "a b x", Field.Store.NO));
    doc.add(new StringField("id", "0", Field.Store.NO));
    w.addDocument(doc);
    w.deleteDocuments(new Term("id", "0"));
    doc = new Document();
    doc.add(new TextField("field", "x", Field.Store.NO));
    w.addDocument(doc);
    IndexReader r = DirectoryReader.open(w, true);
    w.close();
    IndexSearcher s = new IndexSearcher(r);

    assertSameHits(s, new TermQuery(new Term("field", "x")));

    r.close();
    dir.close();
  }

  public void testPrefixQuery() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    int numDocs = atLeast(10000);
    for(int i=0;i<numDocs;i++) {
      Document doc = new Document();
      String s = null;
      while(true) {
        s = _TestUtil.randomSimpleString(random());
        if (s.length() > 0) {
          break;
        }
      }
      doc.add(new TextField("field", s, Field.Store.NO));
      w.addDocument(doc);
    }

    IndexReader r = DirectoryReader.open(w, true);
    w.close();

    IndexSearcher s = new IndexSearcher(r);
    assertSameHits(s, new PrefixQuery(new Term("field", "a")));
    r.close();
    dir.close();
  }

  public void testFuzzyQuery() throws Exception {
    File tmpDir = _TestUtil.getTempDir("nativesearch");
    Directory dir = new NativeMMapDirectory(tmpDir);
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setCodec(Codec.forName("Lucene42"));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("field", "lucene", Field.Store.NO));
    w.addDocument(doc);

    IndexReader r = DirectoryReader.open(w, true);
    w.close();

    IndexSearcher s = new IndexSearcher(r);
    assertSameHits(s, new FuzzyQuery(new Term("field", "lucne"), 1));
    assertSameHits(s, new FuzzyQuery(new Term("field", "luce"), 2));
    r.close();
    dir.close();
  }
}