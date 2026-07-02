package com.cotti.franchise.tools.brush.servlet;

import com.cotti.franchise.tools.brush.model.Result;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = "/db")
public class DbServlet extends BaseServlet {

	private static final long serialVersionUID = -1L;

	@Override
	public String getTplResourcePath() {
		return null;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		List<String> dbs = getDBs(request);
		Result result = new Result();
		result.success = true;
		result.props.put("dbs", dbs);
		outputJson(response, result);
	}

}
