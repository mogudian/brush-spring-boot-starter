package com.cotti.franchise.tools.brush.dialect;

import java.sql.Connection;
import java.util.List;

public interface Dialect {

	int getDefaultPort();

	String getConnectionString(String host, String database);
	
	String getConnectionString(String host, int port, String database);
	
	Connection newConnection(String connectionString, String username, String password);
	
	List<String> getTableNames(Connection conn, String schema);
	
	String getTableComment(Connection conn, String schema, String tableName);
	
	List<String> getTableComments(Connection conn, String schema, List<String> tableNames);

	String createQueryString(String tableName);

	String createPageQueryString(String tableName, String pk, int start, int limit);

	List<List<Object>> executeProc(Connection conn, String procName, Object... values);

	String autoPagination(String sql, int start, int limit);

}
