package com.cotti.franchise.tools.brush.servlet;

import com.alibaba.fastjson.JSON;
import com.cotti.franchise.tools.brush.util.NameUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@WebServlet(urlPatterns = "/tree")
public class TreeServlet extends BaseServlet {

	private static final long serialVersionUID = -1L;

	protected static String URL = null;

	@Override
	public String getTplResourcePath() {
		return "META-INF/template/tree.js";
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
		
		StringBuilder buffer = new StringBuilder(tpl);
		
		int index = -1;

		String dbSchema = "dbnames";
		
		String dbNameUpper = NameUtils.hungaryToCamelCase(dbSchema, true);
		String dbNameLower = NameUtils.hungaryToCamelCase(dbSchema, false);

		int upperLen = "${db_name_upper}".length();
		int lowerLen = "${db_name_lower}".length();

		while ((index = buffer.indexOf("${db_name_upper}")) > -1) {
			buffer.replace(index, index + upperLen, dbNameUpper);
		}

		while ((index = buffer.indexOf("${db_name_lower}")) > -1) {
			buffer.replace(index, index + lowerLen, dbNameLower);
		}

		int len = "${base_url}".length();
		while ((index = buffer.indexOf("${base_url}")) > -1) {
			buffer.replace(index, index + len, URL + "?db=" + dbSchema);
		}

		len = "${db}".length();
		while ((index = buffer.indexOf("${db}")) > -1) {
			buffer.replace(index, index + len, dbSchema);
		}

		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader("Connection", "Keep-Alive");
		response.setHeader("Content-Type", "text/javascript");
		response.setHeader("Content-Disposition", "filename=tree.js");
		try {
			PrintWriter out = response.getWriter();
			out.println(buffer.toString());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 查询
	 */
	private void list(HttpServletRequest request, HttpServletResponse response) {

		List<String> dbs = getDBs(request);
		
		List<TreeNode> nodes = new ArrayList<>(dbs.size());
		
		for (String db : dbs) {
			
			TreeNode dbNode = new TreeNode("db_" + db, db);
			
			Connection conn = getConnection(db, request);
			
			List<String> tableNames = dialect.getTableNames(conn, db);
			
			try {
				conn.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			conn = getConnection(db, request);
			
			List<String> tableComments = dialect.getTableComments(conn, db, tableNames);
			
			try {
				conn.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			int size = tableComments.size();
			
			for (int i = 0; i < size; i++) {
//				nodes.add(new TreeNode(tableNames.get(i), tableComments.get(i)));
				dbNode.getChildren().add(new TreeNode(tableNames.get(i), tableNames.get(i)));
			}
			
			nodes.add(dbNode);
			
		}
		
		response.setContentType("text/plain");
		try {
			String json = JSON.toJSONString(nodes);
			
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
		
	}

	/**
	 * 修改
	 */
	private void modify(HttpServletRequest request, HttpServletResponse response) {
		
	}

	/**
	 * 查看
	 */
	private void load(HttpServletRequest request, HttpServletResponse response) {
		
	}

	/**
	 * 删除
	 */
	private void remove(HttpServletRequest request, HttpServletResponse response) {
		
	}

	private void error(HttpServletRequest request, HttpServletResponse response) {
	}

	public static class TreeNode {
		private String id, text;
		private boolean leaf = true;
		private LinkedList<TreeNode> children = new LinkedList<TreeNode>();
		
		public TreeNode(String id, String text) {
			super();
			this.id = id;
			this.text = text;
		}
		
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public boolean isLeaf() {
			leaf = children.isEmpty();
			return leaf;
		}
		public void setLeaf(boolean leaf) {
			this.leaf = leaf;
		}
		public LinkedList<TreeNode> getChildren() {
			return children;
		}
		public void setChildren(LinkedList<TreeNode> children) {
			this.children = children;
		}
		
	}

}
