<%@page import="net.tutorial.djomeda.iwallet.konohashop.util.DbLayer"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ page import = "java.util.ResourceBundle" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>fixtures page</title>
</head>
<body>
	<h3>Starting fixture execution</h3>
	<% 
		ResourceBundle resource = ResourceBundle.getBundle("local");
		String driver = resource.getString("database.driver");
		String username = resource.getString("username");
		String password = resource.getString("password");
		String host = resource.getString("host");
		String database = resource.getString("database");
		
		DbLayer dbLayer = new DbLayer(driver,username,password,host,database);
		
		dbLayer.tearDown();
		dbLayer.setFixtures();
	
	%>
</body>
</html>