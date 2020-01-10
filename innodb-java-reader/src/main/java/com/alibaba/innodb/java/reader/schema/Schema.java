/*
 * Copyright (C) 1999-2019 Alibaba Group Holding Limited
 */
package com.alibaba.innodb.java.reader.schema;

import com.alibaba.innodb.java.reader.MysqlCharset;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.alibaba.innodb.java.reader.column.ColumnType.CHAR;

/**
 * Schema description, like <code>SHOW CREATE TABLE LIKE 'TTT'</code>
 *
 * @author xu.zx
 */
@Slf4j
public class Schema {

  public static final String DEFAULT_CHARSET = "UTF-8";

  private List<String> columnNames;

  private List<Column> columnList;

  private Map<String, Field> nameToFieldMap;

  private Column primaryKeyColumn;

  private int nullableColumnNum = 0;

  private int variableLengthColumnNum = 0;

  private List<Column> nullableColumnList;

  private List<Column> variableLengthColumnList;

  private int pos = 0;

  /**
   * for decoding string in Java
   */
  private String charset = DEFAULT_CHARSET;

  /**
   * table DDL charset, for example can be latin(ISO8895-1), utf8(UTF-8), utf8mb4(UTF-8)
   */
  private String tableCharset = "utf8";

  /**
   * // TODO this is a workaround.
   * by default if table charset set to utf8, then it will consume up to 3 bytes for one character.
   * if it is utf8mb4, then it must be set to 4
   */
  private int maxBytesForOneChar = MysqlCharset.TABLE_CHARSET_TO_MAX_BYTES_ONE_CHAR_MAP.get(tableCharset);

  public Schema() {
    this.columnList = new ArrayList<>();
    this.columnNames = new ArrayList<>();
    this.nameToFieldMap = new HashMap<>();
    this.nullableColumnList = new ArrayList<>();
    this.variableLengthColumnList = new ArrayList<>();
  }

  public void validate() {
    checkState(CollectionUtils.isNotEmpty(columnList), "no column is specified");
    if (primaryKeyColumn == null) {
      log.warn("primary key is not specified, and default rowid will be used by MySQL for each row");
    }
  }

  public boolean containsVariableLengthColumn() {
    return variableLengthColumnNum > 0;
  }

  public boolean containsNullColumn() {
    return nullableColumnNum > 0;
  }

  public List<Column> getColumnList() {
    return columnList;
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public int getColumnNum() {
    return columnNames.size();
  }

  public int getNullableColumnNum() {
    return nullableColumnNum;
  }

  public int getVariableLengthColumnNum() {
    return variableLengthColumnNum;
  }

  public Schema addColumn(Column column) {
    checkNotNull(column, "column should not be null");
    checkArgument(StringUtils.isNotEmpty(column.getName()), "column name is empty");
    checkArgument(StringUtils.isNotEmpty(column.getType()), "column type is empty");
    if (column.isPrimaryKey()) {
      checkState(primaryKeyColumn == null, "primary key is already defined");
      primaryKeyColumn = column;
    }
    if (column.isNullable()) {
      nullableColumnList.add(column);
      nullableColumnNum++;
    }
    if (column.isVariableLength()) {
      variableLengthColumnList.add(column);
      variableLengthColumnNum++;
    } else if (CHAR.equals(column.getType()) && maxBytesForOneChar > 1) {
      // 多字符集则设置为varchar的读取方式
      column.setVarLenChar(true);
      variableLengthColumnList.add(column);
      variableLengthColumnNum++;
    }
    columnList.add(column);
    columnNames.add(column.getName());
    nameToFieldMap.put(column.getName(), new Field(pos++, column.getName(), column));
    return this;
  }

  public Field getField(String columnName) {
    return nameToFieldMap.get(columnName);
  }

  public Column getPrimaryKeyColumn() {
    return primaryKeyColumn;
  }

  public void setPrimaryKeyColumn(Column primaryKeyColumn) {
    this.primaryKeyColumn = primaryKeyColumn;
  }

  public List<Column> getVariableLengthColumnList() {
    return variableLengthColumnList;
  }

  public List<Column> getNullableColumnList() {
    return nullableColumnList;
  }

  public String getCharset() {
    return charset;
  }

  public Schema setCharset(String charset) {
    this.charset = charset;
    return this;
  }

  public String getTableCharset() {
    return tableCharset;
  }

  public Schema setTableCharset(String tableCharset) {
    this.tableCharset = tableCharset;
    if (!MysqlCharset.TABLE_CHARSET_TO_MAX_BYTES_ONE_CHAR_MAP.containsKey(tableCharset)) {
      throw new IllegalArgumentException("table charset not supported " + tableCharset);
    }
    this.maxBytesForOneChar = MysqlCharset.TABLE_CHARSET_TO_MAX_BYTES_ONE_CHAR_MAP.get(tableCharset);
    return this;
  }

  public int getMaxBytesForOneChar() {
    return maxBytesForOneChar;
  }

  public Schema setMaxBytesForOneChar(int maxBytesForOneChar) {
    this.maxBytesForOneChar = maxBytesForOneChar;
    return this;
  }

  @Data
  public class Field {
    private int pos;
    private String name;
    private Column column;

    public Field(int pos, String name, Column column) {
      this.pos = pos;
      this.name = name;
      this.column = column;
    }
  }

  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean multiLine) {
    StringBuilder sb = new StringBuilder();
    sb.append("Table schema");
    sb.append(" (tableCharset=");
    sb.append(tableCharset);
    sb.append("):");
    for (Column column : columnList) {
      sb.append(multiLine ? "\n" : ",");
      sb.append(column.getName()).append(" ");
      sb.append(column.getType());
      //TODO add extra info like maxVarLen or decimal scale and precision
      sb.append(" ");
      if (!column.isNullable()) {
        sb.append("NOT NULL ");
      }
      if (column.isPrimaryKey()) {
        sb.append("PRIMARY KEY");
      }
    }
    return sb.toString();
  }
}