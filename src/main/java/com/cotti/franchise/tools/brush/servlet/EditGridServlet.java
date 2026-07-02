package com.cotti.franchise.tools.brush.servlet;

import com.alibaba.fastjson.JSON;
import com.cotti.franchise.tools.brush.model.Column;
import com.cotti.franchise.tools.brush.model.Table;
import com.cotti.franchise.tools.brush.util.NameUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@WebServlet(urlPatterns = "/editgrid")
public class EditGridServlet extends BaseServlet {

	private static final long serialVersionUID = -1L;

	protected static String URL = null;

	@Override
	public String getTplResourcePath() {
		return "META-INF/template/editgrid.js";
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding(encoding);
		response.setCharacterEncoding(encoding);
		
		if (URL == null) {
			URL = request.getRequestURI();
		}
		
		String method = request.getParameter("method");
		
		if (method == null || method.isEmpty()) {
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
			buffer.replace(index, index + len, URL + "?table=" + table.getName());
		}

		len = "${table_comment}".length();
		String comment = table.getComment();
		if (comment == null || comment.isEmpty()) {
			comment = table.getName();
		}
		while ((index = buffer.indexOf("${table_comment}")) > -1) {
			buffer.replace(index, index + len, comment);
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
		response.setHeader("Content-Disposition", "filename=editgrid.js");
		try {
			PrintWriter out = response.getWriter();
			out.println(buffer.toString());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getColumn(Column column) {
		String xtype = convertType(column.getType());

		StringBuilder buffer = new StringBuilder();
		buffer.append('{');
		buffer.append("header: ");
		buffer.append('\'');
		if (column.getComment() != null && !column.getComment().isEmpty()) {
			buffer.append(column.getComment());
		} else {
			buffer.append(column.getName());
		}
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

		if (column.isAuto()) {
			buffer.append("autoIncr: true");
			buffer.append(',');
		}

		buffer.append("editor: {");
		if ("radiogroup".equals(xtype)) {
			xtype = "checkbox";
		} else if ("textfield".equals(xtype) || "numberfield".equals(xtype) || "datefield".equals(xtype) || "datetimefield".equals(xtype) || "datetimepicker".equals(xtype)) {
			buffer.append("listeners: {focus: function(text, e) {text.selectText();}},");
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
		} else if ("combo".equals(xtype)) {
		}

		if (column.isAuto()) {
			buffer.append("disabled: true,");
		}

		buffer.append("xtype: ");
		buffer.append('\'');
		buffer.append(xtype);
		buffer.append('\'');
		buffer.append("},");

		if ("checkbox".equals(xtype)) {
			buffer.append("xtype: 'booleancolumn',trueText: '是',falseText: '否',");
		}

		buffer.append("foo: 0");
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
			boolean isFirst = true;
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
            }
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
			rs = stmt.executeQuery(selectSql);
			List<Map<String, Object>> list = new LinkedList<>();
			while (rs.next()) {
				Map<String, Object> obj = new LinkedHashMap<>();
				List<Column> columns = table.getColumns();
				Column column = null;
				for (int i = 0, size = columns.size(); i < size; i++) {
					column = columns.get(i);
					Object value = rs.getString(column.getName());
					if ("datefield".equals(convertType(column.getType())) && value != null && ((String) value).indexOf(' ') > 0) {
						value = ((String) value).substring(0, ((String) value).indexOf(' '));
					} else if ("radiogroup".equals(convertType(column.getType()))) {
						value = !(value == null || "0".equals(value) || "false".equalsIgnoreCase(((String) value)));
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
		} catch (SQLException e) {
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

		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(countSql);
			if (rs.next()) {
				map.put("totalCount", rs.getLong(1));
			}
			success = true;
		} catch (SQLException e) {
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
		Map<String, Object> map = new LinkedHashMap<>();
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

	/**
	 * 修改
	 */
	private void modify(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
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
		String id = null;
		for (String key : set) {
			if ("id".equals(key)) {
				id = params.get(key)[0];
				continue;
			}
			if (types.containsKey(key)) {
				String[] value = params.get(key);
				buffer.append(key);
				buffer.append("=");
				if (value == null || value.length == 0 || value[0] == null || value[0].length() == 0) {
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

	/**
	 * 查看
	 */
	private void load(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		String id = request.getParameter("id");
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
		Map<String, Object> map = new LinkedHashMap<>();
		boolean success = false;

		try (Connection conn = getConnection(db, request)) {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			Map<String, String> data = new LinkedHashMap<>();
			while (rs.next()) {
				success = true;
                for (Column column : columns) {
                    data.put(column.getName(), rs.getString(column.getName()));
                }
			}
			map.put("success", success);
			if (success) {
				data.put("id", data.get(table.getPk()));
				map.put("data", data);

			} else {
				map.put("msg", "Cannot Load Such Entity with Identifier: " + id);
			}
		} catch (SQLException e) {
			map.put("success", false);
			map.put("msg", "Cannot Load Such Entity with Identifier: " + id);
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
		outputJson(response, map, success);
	}

	/**
	 * 删除
	 */
	private void remove(HttpServletRequest request, HttpServletResponse response) {
		String db = request.getParameter("db");
		Table table = getTable(db, request.getParameter("table"), request);
		String idXtype = null;
		boolean isNumber = true;
		List<Column> columns = table.getColumns();
        for (Column column : columns) {
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
			for (int i = 0, len = ids.length; i < len; i++) {
				buffer.append(ids[i]);
				buffer.append(',');
			}
		} else {
			for (int i = 0, len = ids.length; i < len; i++) {
				buffer.append('\'');
				buffer.append(ids[i]);
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
