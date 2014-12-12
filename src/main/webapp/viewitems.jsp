<%@page import="java.math.BigDecimal"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="net.tutorial.djomeda.iwallet.konohashop.util.DbLayer"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Choose Items to buy</title>
</head>
<body>
	<% 
		DbLayer dbLayer = new DbLayer("com.mysql.jdbc.Driver","root","letmein","localhost","konohashop"); 
		List<Map<String, Object>> allrows = dbLayer.getAllItems();
		pageContext.setAttribute("allrows",allrows);
	%>
<form action="checkout" method="post">
<table>
	<c:forEach items="${allrows}" var="rows">
		<tr>
				<td width="130px"><input type="checkbox" name="orderItems" value='<c:out value="${rows['id']}"/>' /> <c:out value="${rows['name']}"/></td>
				<td width="130px"> <c:out value="${rows['price']}"/></td>
				<td width="130px"> <c:out value="${rows['comment']}"/></td>
		</tr>	
	</c:forEach>
	<tr>
		<td colspan="1"><input type="reset" value="Cancel"/></td>
		<td colspan="2"><input type="submit" value="Checkout"/></td>
	</tr>
</table>
</form>	
</body>
</html>