package com.mogudiandian.brush.servlet;

import com.alibaba.fastjson.JSON;
import com.mogudiandian.brush.model.Column;
import com.mogudiandian.brush.model.Table;
import com.mogudiandian.brush.util.NameUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.*;
import java.util.*;

@WebServlet(urlPatterns = "/grid")
public class GridServlet extends BaseServlet {

	private static final long serialVersionUID = -1L;

	protected static String URL = null;

	@Override
	public String getTplResourcePath() {
		return "META-INF/template/grid.js";
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		request.setCharacterEncoding(encoding);
		response.setCharacterEncoding(encoding);
		
		if (URL == null) {
			URL = request.getRequestURI();
		}
		
		String method = request.getParameter("method");
		
		if (method == null || method.length() == 0) {
			page(request, response);
		} else if ("list".equals(method)){
			list(request, response);
		} else if ("add".equals(method)){
			add(request, response);
		} else if ("modify".equals(method)){
			modify(request, response);
		} else if ("load".equals(method)){
			load(request, response);
		} else if ("remove".equals(method)){
			remove(request, response);
		} else {
			error(request, response);
		}
	}

	/**
	 * 生成网页
	 */
	private void page(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		
		StringBuilder buffer = new StringBuilder(tpl);
		
		int index = -1;

		String tableNameUpper = NameUtils.hungaryToCamelCase(table.getName(), true);
		String tableNameLower = NameUtils.hungaryToCamelCase(table.getName(), false);

		int upperLen = "${table_name_upper}".length();
		int lowerLen = "${table_name_lower}".length();

		while ((index = buffer.indexOf("${table_name_upper}")) > -1) {
			buffer.replace(index, index + upperLen, tableNameUpper);
		}

		while ((index = buffer.indexOf("${table_name_lower}")) > -1) {
			buffer.replace(index, index + lowerLen, tableNameLower);
		}

		int len = "${base_url}".length();
		while ((index = buffer.indexOf("${base_url}")) > -1) {
			buffer.replace(index, index + len, URL + "?db=" + db + "&table=" + table.getName());
		}

		len = "${table_comment}".length();
		String comment = table.getComment();
		if (comment == null || comment.isEmpty()) {
			comment = table.getName();
		}
		while ((index = buffer.indexOf("${table_comment}")) > -1) {
			buffer.replace(index, index + len, table.getName());
		}

		index = buffer.indexOf("${page_size}");
		buffer.replace(index, index + "${page_size}".length(), "30");

		StringBuilder columns = new StringBuilder();
		
		List<Column> list = table.getColumns();
		
		for (int i = 0, size = list.size(); i < size; i++) {
			columns.append(getColumn(list.get(i)));
			columns.append(',');
		}
		if (columns.length() > 0) {
			columns.deleteCharAt(columns.length() - 1);
		}
		
		index = buffer.indexOf("${columns}");
		buffer.replace(index, index + "${columns}".length(), columns.toString());

		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader("Connection", "Keep-Alive");
		response.setHeader("Content-Type", "text/javascript");
		response.setHeader("Content-Disposition", "filename=grid.js");
		try {
			PrintWriter out = response.getWriter();
			out.println(buffer);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getColumn(Column column) {
		String xtype = convertType(column.getType());

		StringBuilder buffer = new StringBuilder(256);
		buffer.append('{');
		buffer.append("header: ");
		buffer.append('\'');
//		if (column.getComment() != null && column.getComment().length() > 0) {
//			buffer.append(column.getComment());
//		} else {
//			buffer.append(column.getName());
//		}
		buffer.append(column.getName());
		buffer.append('\'');
		buffer.append(',');
		buffer.append("dataIndex: ");
		buffer.append('\'');
		if (column.isPk()) {
			buffer.append("id");
		} else {
			buffer.append(column.getName());
		}
		buffer.append('\'');
		buffer.append(',');

		buffer.append("fieldLabel: ");
		buffer.append('\'');
//		if (column.getComment() != null && column.getComment().length() > 0) {
//			buffer.append(column.getComment());
//		} else {
//			buffer.append(column.getName());
//		}
		buffer.append(column.getName());
		buffer.append('\'');
		buffer.append(',');


		if (column.isAuto()) {
			buffer.append("autoIncr: true");
			buffer.append(',');
		}

		/*if ("numberfield".equals(xtype) && column.getName().toLowerCase().startsWith("is_")) {
			xtype = "radiogroup";
		}*/

		if ("radiogroup".equals(xtype)) {
			buffer.append("name: ");
			buffer.append('\'');
			buffer.append(column.getName());
			buffer.append('_');
			buffer.append(column.getName());
			buffer.append('\'');
			buffer.append(',');
			buffer.append("items: [{name: '");
			buffer.append(column.getName());
			buffer.append("', boxLabel: '0', inputValue: 0, xtype: 'radio'}, {name: '");
			buffer.append(column.getName());
			buffer.append("', boxLabel: '1', inputValue: 1, xtype: 'radio'}],");
		} else if ("combo".equals(xtype)) {
		} else {
			//可能是hiddenName
			buffer.append("name: ");
			buffer.append('\'');
			buffer.append(column.getName());
			buffer.append('\'');
			buffer.append(',');
		}

		if ("textfield".equals(xtype) || "numberfield".equals(xtype) || "datefield".equals(xtype) || "datetimefield".equals(xtype) || "datetimepicker".equals(xtype)) {
			if (column.isNotnull()) {
				buffer.append("allowBlank: false");
				buffer.append(',');
			}
			if ("datefield".equals(xtype)) {
				buffer.append("editable: false, format: 'Y-m-d'");
				buffer.append(',');
			} else if ("datetimefield".equals(xtype)) {
				buffer.append("dateFormat: 'Y-m-d', timeFormat: 'H:i:s', picker: {timePicker: new Ext.ux.ExBaseTimePicker()}");
				buffer.append(',');
			}
		}

		buffer.append("xtype: ");
		buffer.append('\'');
		buffer.append(xtype);
		buffer.append('\'');

		buffer.append('}');
		return buffer.toString();
	}

	/**
	 * 查询
	 */
	private void list(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		String tableName = request.getParameter("table");
		String query = request.getParameter("query");
		int start = Integer.parseInt(request.getParameter("start"));
		int limit = Integer.parseInt(request.getParameter("limit"));

		Table table = getTable(db, request.getParameter("table"), request);

		String sql = "select _ from " + tableName;
		if (query != null && !query.isEmpty()) {
			StringBuilder buffer = new StringBuilder(sql);
			buffer.append(" where ");
			if (query.length() > "where ".length() && query.substring(0, "where ".length()).equalsIgnoreCase("where ")) {
				query = query.substring("where ".length());
			}
			buffer.append(query);
			// 下面代码用来做全文搜索
			/*boolean isFirst = true;
			List<Column> columns = table.getColumns();
            for (Column column : columns) {
                if (!isFirst) {
                    buffer.append(" or ");
                }
                buffer.append(column.getName());
                buffer.append(" like '%");
                buffer.append(query);
                buffer.append("%'");
                isFirst = false;
            }*/
			sql = buffer.toString();
		}

		String selectSql = dialect.createPageQueryString(sql.replaceFirst("_", "*"), table.getPk(), start, limit);
		String countSql = sql.replaceFirst("_", "count(1)");
		
		Map<String, Object> map = new LinkedHashMap<>();

		boolean success = false;

		Statement stmt = null;
		ResultSet rs = null;
		

		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(countSql);
			if (rs.next()) {
				map.put("totalCount", rs.getLong(1));
			}
			success = true;
		} catch (Exception e) {
			success = false;
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

		if (success) {
			try (Connection conn = getConnection(db, request)) {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(selectSql);
				List<Map<String, String>> list = new LinkedList<>();
				while (rs.next()) {
					Map<String, String> obj = new LinkedHashMap<>();
					List<Column> columns = table.getColumns();
					for (Column column : columns) {
						String value = null;
						String xtype = convertType(column.getType());
						if ("datetimefield".equals(xtype)) {
							Timestamp date = rs.getTimestamp(column.getName());
							if (date != null) {
								value = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(date);
							}
						} else if ("datefield".equals(xtype)) {
							Date date = rs.getDate(column.getName());
							if (date != null) {
								value = FastDateFormat.getInstance("yyyy-MM-dd").format(date);
							}
						}
						if (value == null) {
							value = rs.getString(column.getName());
						}
						if ("datefield".equals(xtype) && value != null && value.indexOf(' ') > 0) {
							// 时间因为没有组件
							// value = value.substring(0, value.indexOf(' '));
						} else if ("radiogroup".equals(xtype)) {
							if ("0".equals(value) || "false".equals(value)) {
								value = "0";
							} else {
								value = "1";
							}
						}
						if (column.isPk()) {
							obj.put("id", value);
						}
						obj.put(column.getName(), value);
					}
					list.add(obj);
				}
				map.put("list", list);
				success = true;
			} catch (Exception e) {
				success = false;
				throw new RuntimeException(selectSql);
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
		} else {
			map.put("list", Collections.emptyList());
			if (query != null && !query.isEmpty()) {
				map.put("msg", "查询条件错误，请检查");
			} else {
				map.put("msg", "查询失败");
			}
		}
		map.put("success", success);
		response.setContentType("text/plain");
		try {
			String json = JSON.toJSONString(map);
			
			json = json.replaceAll("<", "&lt").replaceAll(">", "&gt;");
			
			PrintWriter out = response.getWriter();
			out.println(json);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 添加
	 */
	private void add(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		StringBuilder buffer = new StringBuilder();
		buffer.append("insert into ");
		buffer.append(table.getName());
		buffer.append("(");
		List<Column> columns = table.getColumns();
        for (Column column : columns) {
            //分析
            if (!column.isAuto()) {
                buffer.append(column.getName());
                buffer.append(',');
            }
        }
		buffer.deleteCharAt(buffer.length() - 1);
		buffer.append(") values (");
        for (Column column : columns) {
            String value = request.getParameter(column.getName());
            //分析
            if (!column.isAuto()) {
                String xtype = convertType(column.getType());
                if (value == null || value.isEmpty()) {
                    buffer.append("null");
                } else {
                    if ("radiogroup".equals(xtype) || "numberfield".equals(xtype)) {
                        buffer.append(value);
                    } else {
                        buffer.append('\'');
                        buffer.append(value);
                        buffer.append('\'');
                    }
                }
                buffer.append(',');
            }
        }
		buffer.deleteCharAt(buffer.length() - 1);
		buffer.append(")");
		String sql = buffer.toString();
		Statement stmt = null;
		
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		boolean success = false;
		

		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			int rows = stmt.executeUpdate(sql);
			map.put("success", success = rows > 0);
			map.put("msg", success ? "添加成功!" : "添加失败!");
		} catch (SQLException e) {
			map.put("success", false);
			map.put("msg", e.getMessage());
			e.printStackTrace();
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		outputJson(response, map, success);
	}

	private void backup(HttpServletRequest request, String db, Table table, String id) throws SQLException {
		Map<String, Object> data = getDataById(request, db, table, id);
		String jsonString = JSON.toJSONString(data);
		String sql = "insert into " + db + ".t_backup_table (database_name, table_name, table_id, table_data, create_time) values (?, ?, ?, ?, ?)";

		PreparedStatement stmt = null;
		try (Connection conn = getConnection(db, request)) {
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, db);
			stmt.setString(2, table.getName());
			stmt.setString(3, id);
			stmt.setString(4, jsonString);
			stmt.setDate(5, new java.sql.Date(System.currentTimeMillis()));
			int rows = stmt.executeUpdate();
			if (rows <= 0) {
				throw new SQLException("备份" + table.getName() + "[" + table.getPk() + "=" + id + "]失败!");
			}
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 修改
	 */
	private void modify(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		String id = request.getParameter("id");

		try {
			backup(request, db, table, id);
		} catch (SQLException e) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("success", false);
			map.put("msg", e.getMessage());
			e.printStackTrace();
			outputJson(response, map, false);
			return;
		}

		Map<String, String> types = new HashMap<>(table.getColumns().size());
		String idXtype = null;
		List<Column> columns = table.getColumns();
        for (Column column : columns) {
            if (column.isPk()) {
                idXtype = convertType(column.getType());
            } else {
                types.put(column.getName(), column.getType());
            }
        }
		Map<String, String[]> params = request.getParameterMap();

		StringBuilder buffer = new StringBuilder();
		buffer.append("update ");
		buffer.append(table.getName());
		buffer.append(" set ");
		Set<String> set = params.keySet();

		for (String key : set) {
			if ("id".equals(key)) {
				id = params.get(key)[0];
				continue;
			}
			if (types.containsKey(key)) {
				String[] value = params.get(key);
				buffer.append(key);
				buffer.append("=");
				if (value == null || value.length == 0 || value[0] == null || value[0].isEmpty()) {
					buffer.append("null");
				} else {
					String xtype = convertType(types.get(key));
					if ("radiogroup".equals(xtype) || "numberfield".equals(xtype)) {
						buffer.append(value[0]);
					} else {
						buffer.append('\'');
						buffer.append(value[0]);
						buffer.append('\'');
					}
				}
				buffer.append(',');
			}
		}
		buffer.deleteCharAt(buffer.length() - 1);
		buffer.append(" where ");
		buffer.append(table.getPk());
		buffer.append("=");

		if ("radiogroup".equals(idXtype) || "numberfield".equals(idXtype)) {
			buffer.append(id);
		} else {
			buffer.append('\'');
			buffer.append(id);
			buffer.append('\'');
		}
		String sql = buffer.toString();
		Statement stmt = null;

		Map<String, Object> map = new LinkedHashMap<>();
		boolean success = false;
		
		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			int rows = stmt.executeUpdate(sql);
			map.put("success", success = rows > 0);
			map.put("msg", success ? "修改成功!" : "修改失败!");
		} catch (SQLException e) {
			map.put("success", false);
			map.put("msg", e.getMessage());
			e.printStackTrace();
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		outputJson(response, map, success);
	}

	private Map<String, Object> getDataById(HttpServletRequest request, String db, Table table, String id) throws SQLException {
		StringBuilder buffer = new StringBuilder();
		buffer.append("select * from ");
		buffer.append(table.getName());
		buffer.append(" where ");
		buffer.append(table.getPk());
		buffer.append("=");
		String xtype = null;
		List<Column> columns = table.getColumns();
        for (Column column : columns) {
            if (table.getPk().equals(column.getName())) {
                xtype = convertType(column.getType());
                break;
            }
        }
		if ("numberfield".equals(xtype)) {
			buffer.append(id);
		} else {
			buffer.append('\'');
			buffer.append(id);
			buffer.append('\'');
		}
		String sql = buffer.toString();
		Statement stmt = null;
		ResultSet rs = null;

		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			Map<String, Object> data = new LinkedHashMap<>();
			while (rs.next()) {
                for (Column column : columns) {
                    data.put(column.getName(), rs.getObject(column.getName()));
                }
			}
			return data;
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
	}

	/**
	 * 查看
	 */
	private void load(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		String id = request.getParameter("id");

		Map<String, Object> map = new LinkedHashMap<>();
		boolean success = false;
		
		try {
			Map<String, Object> data = getDataById(request, db, table, id);
			for (Column column : table.getColumns()) {
				Object value = data.get(column.getName());
				if ("datefield".equals(convertType(column.getType())) && value != null && (value instanceof String) && ((String) value).indexOf(' ') > 0) {
					// 时间因为没有组件
					// value = value.substring(0, value.indexOf(' '));
					// data.put(column.getName(), value);
				}
			}
			success = true;
			map.put("success", success);
			data.put("id", data.get(table.getPk()));
			map.put("data", data);
		} catch (SQLException e) {
			map.put("success", false);
			map.put("msg", "Cannot Load Such Entity with Identifier: " + id);
			e.printStackTrace();
		}
		outputJson(response, map, success);
	}

	/**
	 * 删除
	 */
	private void remove(HttpServletRequest request, HttpServletResponse response) {
		if (1 > 0) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("success", false);
			map.put("msg", "不支持删除");
			outputJson(response, map, false);
			return;
		}

		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		String id = request.getParameter("id");

		try {
			backup(request, db, table, id);
		} catch (SQLException e) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("success", false);
			map.put("msg", e.getMessage());
			e.printStackTrace();
			outputJson(response, map, false);
			return;
		}

		String idXtype = null;
		boolean isNumber = true;
		List<Column> columns = table.getColumns();
		Column column = null;
		for (int i = 0, size = columns.size(); i < size; i++) {
			column = columns.get(i);
			if (column.isPk()) {
				idXtype = convertType(column.getType());
				isNumber = "numberfield".equals(idXtype);
				break;
			}
		}
		StringBuilder buffer = new StringBuilder();
		buffer.append("delete from ");
		buffer.append(table.getName());
		buffer.append(" where ");
		buffer.append(table.getPk());
		buffer.append(" in (");
		String[] ids = request.getParameterValues("id");
		if (isNumber) {
            for (String s : ids) {
                buffer.append(s);
                buffer.append(',');
            }
		} else {
            for (String s : ids) {
                buffer.append('\'');
                buffer.append(s);
                buffer.append('\'');
                buffer.append(',');
            }

		}
		buffer.deleteCharAt(buffer.length() - 1);
		buffer.append(")");
		String sql = buffer.toString();
		Statement stmt = null;
		
		Map<String, Object> map = new LinkedHashMap<>();
		boolean success = false;
		
		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			int rows = stmt.executeUpdate(sql);
			map.put("success", success = rows > 0);
			map.put("msg", success ? "删除成功!" : "删除失败!");
		} catch (SQLException e) {
			map.put("success", false);
			map.put("msg", e.getMessage());
			e.printStackTrace();
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		outputJson(response, map, success);
	}

	private void error(HttpServletRequest request, HttpServletResponse response) {
	}

}
