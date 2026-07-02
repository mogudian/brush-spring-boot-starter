package com.mogudiandian.brush.dialect;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumericLiteralExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MySQLDialect implements Dialect {

	private static String CONNECTION_STRING_TEMPLATE = "jdbc:mysql://${host}:${port}/${db_name}?useUnicode=true&characterEncoding=utf8&transformedBitIsBoolean=yes&autoReconnect=true";

	@Override
	public int getDefaultPort() {
		return 3306;
	}
	
	@Override
	public String getConnectionString(String host, String database) {
		return getConnectionString(host, getDefaultPort(), database);
	}

	@Override
	public String getConnectionString(String host, int port, String database) {
		StringBuilder builder = new StringBuilder(CONNECTION_STRING_TEMPLATE);
		int index = -1;
		builder.replace(index = builder.indexOf("${host}"), index + "${host}".length(), host);
		builder.replace(index = builder.indexOf("${port}"), index + "${port}".length(), Integer.toString(port));
		builder.replace(index = builder.indexOf("${db_name}"), index + "${db_name}".length(), database);
		return builder.toString();
	}

	@Override
	public Connection newConnection(String connectionString, String username,
			String password) {
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		if (password == null) {
			password = "";
		}
		
		try {
			return DriverManager.getConnection(connectionString, username, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<String> getTableNames(Connection conn, String schema) {
		String sql = "select table_name from information_schema.tables where table_schema=?";

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(sql);

			stmt.setString(1, schema);

			rs = stmt.executeQuery();

			List<String> tableNames = new ArrayList<>();

			while (rs.next()) {
				String tableName = rs.getString(1);
				tableNames.add(tableName);
			}

			return tableNames;
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
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	public String getTableComment(Connection conn, String schema,
			String tableName) {
		String sql = "select table_comment from information_schema.tables where table_schema=? and table_name=?";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, tableName);
			rs = stmt.executeQuery();
			String comment = rs.next() ? rs.getString(1) : tableName;
			return comment;
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
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return tableName;
	}
	
	@Override
	public List<String> getTableComments(Connection conn, String schema, List<String> tableNames) {
		
		String sql = "select table_name, table_comment from information_schema.tables where table_schema=?";
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, schema);
			rs = stmt.executeQuery();
			
			int size = tableNames.size();
			
			HashMap<String, String> map = new HashMap<>(size);
			
			while (rs.next()) {
				String s1 = rs.getString(1);
				String s2 = rs.getString(2);
				map.put(s1, s2);
			}
			
			List<String> comments = new ArrayList<>(size);

			String name = null;
			
			for (int i = 0; i < size; i++) {
				name = tableNames.get(i);
				String comment = map.get(name);
				if (comment == null || comment.isEmpty()) {
					comment = name;
				}
				comments.add(comment);
			}
			
			return comments;
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
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return tableNames;
	}

	@Override
	public String createQueryString(String tableName) {
		StringBuilder builder = new StringBuilder();
		builder.append("select * from ");
		builder.append(tableName);
		return builder.toString();
	}

	@Override
	public String createPageQueryString(String queryString, String pk, int start, int limit) {
		StringBuilder builder = new StringBuilder();
		builder.append(queryString);
		builder.append(" limit ");
		builder.append(start);
		builder.append(", ");
		builder.append(limit);
		return builder.toString();
	}

	@Override
	public List<List<Object>> executeProc(Connection conn, String procName, Object... values) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public String autoPagination(String sql, int start, int limit) {
		SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
		if (sqlStatement instanceof SQLSelectStatement) {
			SQLSelectStatement selectStatement = (SQLSelectStatement) sqlStatement;
			boolean modified = processQuery(selectStatement.getSelect().getQuery(), start, limit);
			if (modified) {
				return SQLUtils.toMySqlString(selectStatement);
			}
		}
		return sql;
	}

	private boolean processQuery(SQLSelectQuery query, int start, int limit) {
		if (query instanceof SQLSelectQueryBlock) {
			return processQueryBlock((SQLSelectQueryBlock) query, start, limit);
		} else if (query instanceof SQLUnionQuery) {
			SQLUnionQuery union = (SQLUnionQuery) query;
			/*
			 * union的每一个子句都要加limit
			 * 但是druid输出union第一个子句不会加括号，导致生成的SQL有问题
			 * 生成的SQL：select * from a limit 0, 1000 union all (select * from b limit 0, 1000)
			 * 所以这里强制加上括号，改为 (select * from a limit 0, 1000) union al (select * from b limit 0, 1000)
			 */
			if (!(union.getLeft() instanceof SQLUnionQuery)) {
				union.getLeft().setParenthesized(true);
			}
			return processQuery(union.getLeft(), start, limit) && processQuery(union.getRight(), start, limit);
		}
		return false;
	}

	private boolean processQueryBlock(SQLSelectQueryBlock queryBlock, int start, int limit) {
		List<SQLSelectItem> selectList = queryBlock.getSelectList();
		for (SQLSelectItem sqlSelectItem : selectList) {
			// 判断是否是聚合函数
			SQLExpr expr = sqlSelectItem.getExpr();
			if (expr instanceof SQLAggregateExpr) {
				// 有聚合函数则不处理
				return false;
			}
		}
		// 没有聚合函数再校验
		SQLLimit sqlLimit = queryBlock.getLimit();
		// 如果有limit则校验limit数量
		if (sqlLimit != null) {
			SQLExpr rowCount = sqlLimit.getRowCount();
			long srcLimit = ((SQLNumericLiteralExpr) rowCount).getNumber().longValue();
			// 原始如果没超过limit则不处理
            if (srcLimit <= limit) {
                return false;
            }
			// 如果数量超过了limit则修改limit数量
            sqlLimit.setRowCount(limit);
			return true;
        } else {
			// 如果没有limit则添加limit
			sqlLimit = new SQLLimit();
			sqlLimit.setOffset(start);
			sqlLimit.setRowCount(limit);
			queryBlock.setLimit(sqlLimit);
			return true;
		}
	}
}
