package com.cotti.franchise.tools.brush.servlet;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.fastjson.JSON;
import com.cotti.franchise.tools.brush.configuration.BrushProperties;
import com.cotti.franchise.tools.brush.model.Column;
import com.cotti.franchise.tools.brush.model.Result;
import com.cotti.franchise.tools.brush.model.Table;
import com.google.common.base.Throwables;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet(urlPatterns = "/stmt")
public class StmtServlet extends BaseServlet {

	private static final long serialVersionUID = 8767989100087031294L;

	@Override
	public String getTplResourcePath() {
		return null;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		request.setCharacterEncoding(encoding);
		response.setCharacterEncoding(encoding);
		
		String db = request.getParameter("db");
		
		String sql = request.getParameter("stmt");
		
		System.out.println(sql);

		sql = sql.trim();

		if (sql.startsWith("select ") || sql.startsWith("show ") || sql.startsWith("explain ")
		        || sql.startsWith("select\t") || sql.startsWith("show\t") || sql.startsWith("explain\t")
		        || sql.startsWith("select\n") || sql.startsWith("show\n") || sql.startsWith("explain\n")
				|| sql.startsWith("SELECT ") || sql.startsWith("SHOW ") || sql.startsWith("EXPLAIN ")
				|| sql.startsWith("SELECT\t") || sql.startsWith("SHOW\t") || sql.startsWith("EXPLAIN\t")
				|| sql.startsWith("SELECT\n") || sql.startsWith("SHOW\n") || sql.startsWith("EXPLAIN\n")) {
			executeQuery(request, response, db, sql);
		} else {
			executeUpdate(request, response, db, sql);
		}
	}

	public void executeUpdate(HttpServletRequest request, HttpServletResponse response, String db, String sql) throws ServletException, IOException {
		Result result = new Result();

		Connection conn = getConnection(db, request);

		try {
			conn.setAutoCommit(false);

			SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, DbType.mysql);
			if (sqlStatement instanceof MySqlUpdateStatement) {
				MySqlUpdateStatement updateStatement = (MySqlUpdateStatement) sqlStatement;
				SQLExpr where = updateStatement.getWhere();
				if (where == null) {
					result.success = true;
					result.props.put("exMsg", Collections.singletonList("update语句必须包含where条件"));
					outputJson(response, result);
					return;
				}

				List<TableReference> tables = new ArrayList<>();
				extractFromTables(updateStatement.getTableSource(), false, tables);
				Map<String, TableReference> tableMap = new HashMap<>();
				for (TableReference table : tables) {
					tableMap.put(table.getTableName(), table);
					if (table.getAlias() != null) {
						tableMap.put(table.getAlias(), table);
					}
				}

				Set<TableReference> ownerTables = new HashSet<>();

				if (tables.size() == 1) {
					ownerTables.addAll(tables);
				} else {
					List<String> noOwnerColumns = new LinkedList<>();
					List<SQLUpdateSetItem> items = updateStatement.getItems();
					for (SQLUpdateSetItem item : items) {
						SQLExpr columnExpr = item.getColumn();
						if (columnExpr instanceof SQLIdentifierExpr) {
							noOwnerColumns.add(((SQLIdentifierExpr) columnExpr).getName());
						} else if (columnExpr instanceof SQLPropertyExpr) {
							SQLPropertyExpr column = (SQLPropertyExpr) item.getColumn();
							if (column.getOwner() != null) {
								String ownerName = column.getOwnerName();
								TableReference tableReference = tableMap.get(ownerName);
								ownerTables.add(tableReference);
							} else {
								noOwnerColumns.add(column.getName());
							}
						} else {
							result.success = true;
							result.props.put("exMsg", Collections.singletonList("无法解析set子句中的" + columnExpr + "，类型为：" + columnExpr.getClass().getName()));
							outputJson(response, result);
							return;
						}
					}
					if (!noOwnerColumns.isEmpty()) {
						result.success = true;
						result.props.put("exMsg", Collections.singletonList("下列要更新的字段没有指定表名：" + noOwnerColumns));
						outputJson(response, result);
						return;
					}
				}
				for (TableReference ownerTable : ownerTables) {
					if ("t_backup_table".equals(ownerTable.getTableName())) {
						result.success = true;
						result.props.put("exMsg", Collections.singletonList("不允许修改备份表"));
						outputJson(response, result);
						return;
					}
				}

				for (TableReference ownerTable : ownerTables) {
					Table table = getTable(db, ownerTable.getTableName(), request);

					SQLSelectStatement selectStatement = new SQLSelectStatement();
					SQLSelect select = new SQLSelect();

					SQLSelectQueryBlock queryBlock = new SQLSelectQueryBlock();
					SQLExpr columnExpr;
					if (tables.size() == 1) {
						columnExpr = new SQLIdentifierExpr("*");
					} else {
						columnExpr = new SQLPropertyExpr(ownerTable.getReference(), "*");
					}
					SQLSelectItem selectItem = new SQLSelectItem(columnExpr);
					queryBlock.addSelectItem(selectItem);

					queryBlock.setFrom(updateStatement.getTableSource());
					if (updateStatement.getWhere() != null) {
						queryBlock.setWhere(updateStatement.getWhere());
					}
					if (updateStatement.getLimit() != null) {
						queryBlock.setLimit(updateStatement.getLimit());
					}

					select.setQuery(queryBlock);
					selectStatement.setSelect(select);

					String selectSql = SQLUtils.toMySqlString(selectStatement);

					Statement selectStmt = conn.createStatement();
					ResultSet rs = selectStmt.executeQuery(selectSql);
					List<Map<String, String>> list = new LinkedList<>();
					while (rs.next()) {
						Map<String, String> data = new LinkedHashMap<>();
						for (int i = 0, size = table.getColumns().size(); i < size; i++) {
							Column column = table.getColumns().get(i);
							data.put(column.getName(), rs.getString(column.getName()));
						}
						list.add(data);
					}
					rs.close();
					selectStmt.close();

					String dbMsg = "";
					if (!list.isEmpty()) {
						StringBuilder buffer = new StringBuilder();
						buffer.append("insert into ")
							  .append(db)
							  .append(".t_backup_table (database_name, table_name, table_id, table_data, create_time) values ");
						for (Map<String, String> map : list) {
							buffer.append("('")
								  .append(db)
								  .append("', '")
								  .append(table.getName()).append("', '")
								  .append(map.get("id"))
								  .append("', '")
								  .append(JSON.toJSONString(map))
								  .append("', now()),");
						}
						buffer.deleteCharAt(buffer.length() - 1);
						String insertSql = buffer.toString();
						Statement insertStatement = conn.createStatement();
						int rows = insertStatement.executeUpdate(insertSql);
						insertStatement.close();
						dbMsg += table.getName() + "表成功备份" + rows + "行<hr />";
					}
					result.props.put("dbMsg", dbMsg);
				}
			} else if (sqlStatement instanceof SQLDeleteStatement) {
				result.success = true;
				result.props.put("exMsg", Collections.singletonList("不允许执行delete语句"));
				outputJson(response, result);
				return;
			} else if (sqlStatement instanceof SQLInsertStatement || sqlStatement instanceof SQLReplaceStatement) {
				// 允许执行
			} else {
				result.success = true;
				result.props.put("exMsg", Collections.singletonList("不支持的SQL语句"));
				outputJson(response, result);
				return;
			}

			Statement stmt = conn.createStatement();

			int rows = stmt.executeUpdate(sql);

			conn.commit();

			String dbMsg = (String) result.props.get("dbMsg");
			if (dbMsg == null) {
				result.props.put("dbMsg", "执行成功，影响行数：" + rows);
			} else {
				result.props.put("dbMsg", dbMsg + "更新语句执行成功，影响行数：" + rows);
			}

			result.success = true;

			stmt.close();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException ex) {
				e = ex;
			}

			String stackTraceAsString = Throwables.getStackTraceAsString(e);
			String[] arr = {stackTraceAsString.replace(" ", "&nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;").replace("\n", "<br />")};
			result.success = true;
			result.props.put("exMsg", arr);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		outputJson(response, result, true);
	}

	public void executeQuery(HttpServletRequest request, HttpServletResponse response, String db, String sql) throws ServletException, IOException {
		Result result = new Result();

		try (Connection conn = getConnection(db, request)) {
			ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
			BrushProperties properties = applicationContext.getBean(BrushProperties.class);

			String newSql = dialect.autoPagination(sql, 0, properties.getQueryLimit());

			Statement stmt = conn.createStatement();

			long l1 = System.currentTimeMillis();

			ResultSet rs = stmt.executeQuery(newSql);

			long l2 = System.currentTimeMillis();

			ResultSetMetaData rsmd = rs.getMetaData();

			int columnCount = rsmd.getColumnCount();

			String[] columnNames = new String[columnCount];

			for (int i = 1; i <= columnCount; i ++) {
				columnNames[i - 1] = rsmd.getColumnLabel(i);
			}

			List<Map<String, Object>> rows = new ArrayList<>(columnCount);

			while (rs.next()) {
				Map<String, Object> cols = new HashMap<>();

				for (int i = 1; i <= columnCount; i++) {
					Object value = rs.getObject(i);

					if (value instanceof String) {
						// value = HtmlUtils.htmlEscape((String) value);
						value = filterUnicodeString((String) value);
					}

					cols.put(columnNames[i - 1], value);
				}

				rows.add(cols);
			}

			long l3 = System.currentTimeMillis();

			// return column names and list
			result.props.put("columns", columnNames);
			result.props.put("data", rows);
			result.props.put("info", "查询耗时：" + (l2 -l1) + "毫秒，总耗时：" + (l3 - l1) + "毫秒，条数：" + rows.size() + "（最多可查询" + properties.getQueryLimit() + "条）");
			result.success = true;

			stmt.close();

			outputJson(response, result);
		} catch (Exception e) {
			String stackTraceAsString = Throwables.getStackTraceAsString(e);
			String[] arr = {stackTraceAsString.replace(" ", "&nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;").replace("\n", "<br />")};
			result.success = true;
			result.props.put("exMsg", arr);

			outputJson(response, result, true);
		}
	}

	public static String filterUnicodeString(String value) {
		if (value == null) {
			return null;
		}
		char[] xmlChar = value.toCharArray();
		for (int i = 0; i < xmlChar.length; i++) {
			if (xmlChar[i] > 0xFFFD) {
				xmlChar[i] = ' ';// 用空格替换
			} else if (xmlChar[i] < 0x20 && xmlChar[i] != 't'
					& xmlChar[i] != 'n' & xmlChar[i] != 'r') {
				xmlChar[i] = ' ';// 用空格替换
			}
		}
		return new String(xmlChar);
	}

	/**
	 * 从SQL中提取所有的表名/别名
	 * @param sqlTableSource SQL表
	 * @param isOuterJoin 是否是外连接
	 * @param list 结果集
	 */
	private static void extractFromTables(SQLTableSource sqlTableSource, boolean isOuterJoin, List<TableReference> list) {
		// 单表
		if (sqlTableSource instanceof SQLExprTableSource) {
			SQLExprTableSource exprTableSource = (SQLExprTableSource) sqlTableSource;
			// 收集表引用
			list.add(new TableReference(exprTableSource.getTableName(), exprTableSource.getAlias(), isOuterJoin));
		} else if (sqlTableSource instanceof SQLJoinTableSource) {
			// 关联表，包括 from a, b 和 from a join b 的情况
			SQLJoinTableSource joinTableSource = (SQLJoinTableSource) sqlTableSource;
			SQLTableSource left = joinTableSource.getLeft();
			SQLTableSource right = joinTableSource.getRight();
			// 递归处理左右两边的表
			extractFromTables(left, joinTableSource.getJoinType() == SQLJoinTableSource.JoinType.RIGHT_OUTER_JOIN, list);
			extractFromTables(right, joinTableSource.getJoinType() == SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN, list);
		}
	}

	private static class TableReference {

		/**
		 * 表名
		 */
		private final String tableName;

		/**
		 * 别名
		 */
		private final String alias;

		/**
		 * 是否是外连接
		 */
		private final boolean outerJoin;

		public TableReference(String tableName, String alias, boolean outerJoin) {
			this.tableName = tableName;
			this.alias = alias;
			this.outerJoin = outerJoin;
		}

		/**
		 * 获取引用 优先获取别名，如果没有别名则获取表名
		 * @return 引用
		 */
		public String getReference() {
			return Optional.ofNullable(alias).orElse(tableName);
		}

		public String getTableName() {
			return tableName;
		}

		public String getAlias() {
			return alias;
		}

		public boolean isOuterJoin() {
			return outerJoin;
		}

		@Override
		public String toString() {
			if (alias != null) {
				return tableName + " " + alias;
			} else {
				return tableName;
			}
		}
	}

}
