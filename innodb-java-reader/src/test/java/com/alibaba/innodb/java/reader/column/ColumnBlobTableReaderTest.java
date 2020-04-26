package com.alibaba.innodb.java.reader.column;

import com.alibaba.innodb.java.reader.AbstractTest;
import com.alibaba.innodb.java.reader.page.index.GenericRecord;
import com.alibaba.innodb.java.reader.schema.Column;
import com.alibaba.innodb.java.reader.schema.TableDef;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author xu.zx
 */
public class ColumnBlobTableReaderTest extends AbstractTest {

  public TableDef getTableDef() {
    return new TableDef()
        .addColumn(new Column().setName("id").setType("int(11)").setNullable(false).setPrimaryKey(true))
        .addColumn(new Column().setName("a").setType("TINYBLOB").setNullable(false))
        .addColumn(new Column().setName("b").setType("BLOB").setNullable(false))
        .addColumn(new Column().setName("c").setType("MEDIUMBLOB").setNullable(false))
        .addColumn(new Column().setName("d").setType("LONGBLOB").setNullable(false));
  }

  @Test
  public void testBlobColumnMysql56() {
    assertTestOf(this)
        .withMysql56()
        .withTableDef(getTableDef())
        .checkAllRecordsIs(expected());
  }

  @Test
  public void testBlobColumnMysql57() {
    assertTestOf(this)
        .withMysql57()
        .withTableDef(getTableDef())
        .checkAllRecordsIs(expected());
  }

  @Test
  public void testBlobColumnMysql80() {
    assertTestOf(this)
        .withMysql80()
        .withTableDef(getTableDef())
        .checkAllRecordsIs(expected());
  }

  public Consumer<List<GenericRecord>> expected() {
    return recordList -> {

      assertThat(recordList.size(), is(10));

      int index = 0;
      for (int i = 1; i <= 10; i++) {
        GenericRecord record = recordList.get(index++);
        Object[] values = record.getValues();
        System.out.println(Arrays.asList(values));

        assertThat(((byte[]) record.get("a")).length, is(201));
        assertThat(record.get("a"), is(getContent((byte) (97 + i), (byte) 0x0a, 200)));

        // TODO mysql8.0 lob is not supported
        if (!isMysql8Flag.get()) {
          assertThat(((byte[]) record.get("b")).length, is(60001));
          assertThat(record.get("b"), is(getContent((byte) (97 + i), (byte) 0x0b, 60000)));

          assertThat(((byte[]) record.get("c")).length, is(80001));
          assertThat(record.get("c"), is(getContent((byte) (97 + i), (byte) 0x0c, 80000)));

          assertThat(((byte[]) record.get("d")).length, is(100001));
          assertThat(record.get("d"), is(getContent((byte) (97 + i), (byte) 0x0d, 100000)));
        }
      }
    };
  }
}
