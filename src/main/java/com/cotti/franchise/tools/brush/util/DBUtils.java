package com.cotti.franchise.tools.brush.util;

import com.cotti.franchise.tools.brush.dialect.Dialect;
import com.cotti.franchise.tools.brush.model.Column;
import com.cotti.franchise.tools.brush.model.Table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * API未测试，未优化
 */
public class DBUtils {

	private static Map<Connection, Map<String, Foo>> cache = new LinkedHashMap<>();

	static {
		new Thread() {
			@Override
			public void run() {
				List<String> garbages = new LinkedList<>();
				while (true) {
					try {
						sleep(1000 * 60 * 30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					long current = System.currentTimeMillis();
					Set<Connection> keys = cache.keySet();
					for (Connection key : keys) {
						Map<String, Foo> value = cache.get(key);
						Set<String> tns = value.keySet();
						for (String tn : tns) {
							Foo foo = value.get(tn);
							if (current - foo.getMillis() > 0) {
								garbages.add(tn);
							}
						}
						for (Iterator<String> it = garbages.iterator(); it.hasNext();) {
							String g = it.next();
							it.remove();
							value.remove(g);
						}
					}
				}
			}
		}.start();
	}
	
	private static class Foo {
		private Table table;
		private long millis;
		private Foo(Table table) {
			this.table = table;
			this.millis = System.currentTimeMillis();
		}
		public Table getTable() {
			return table;
		}
		public long getMillis() {
			return millis;
		}
		public void setMillis(long millis) {
			this.millis = millis;
		}
	}

	public static Table getTable(Dialect dialect, Connection conn, String schema, String tableName, boolean useCache) {
		boolean hasConn = false;
		if (useCache && cache.containsKey(conn)) {
			hasConn = true;
			Map<String, Foo> map = cache.get(conn);
			if (map.containsKey(tableName)) {
				Foo foo = map.get(tableName);
				foo.setMillis(System.currentTimeMillis());
				return foo.getTable();
			}
		}
		Table table = new Table();
		table.setName(tableName);
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;
		try {
			dbmd = conn.getMetaData();
			rs = dbmd.getTables(schema, schema, tableName, null);
			while (rs.next()) {
				table.setComment(rs.getString("REMARKS"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			rs = dbmd.getPrimaryKeys(schema, schema, tableName);
			while (rs.next()) {
				table.setPk(rs.getString("COLUMN_NAME"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
		try {
			rs = dbmd.getColumns(schema, schema, tableName, null);
			while (rs.next()) {
				String name = rs.getString("COLUMN_NAME");
				String type = rs.getString("TYPE_NAME");
				String size = rs.getString("COLUMN_SIZE");
				int length = 0;
				if (size != null && !"".equals(size)) {
					length = Integer.parseInt(size);
				}
				boolean notnull = "0".equals(rs.getString("NULLABLE"));
				String comment = rs.getString("REMARKS");
				boolean auto = "YES".equals(rs.getString("IS_AUTOINCREMENT").toUpperCase());
				boolean isPk = name.equals(table.getPk());
				Column column = new Column(name, type, length, notnull, comment, auto, isPk);
				table.addColumn(column);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
		table.setComment(dialect.getTableComment(conn, schema, tableName));
		
		if (useCache) {
			Map<String, Foo> map = null;
			if (!hasConn) {
				cache.put(conn, map = new LinkedHashMap<>());
			} else {
				map = cache.get(conn);
			}
			map.put(tableName, new Foo(table));
		}
		
		return table;
	}
}
