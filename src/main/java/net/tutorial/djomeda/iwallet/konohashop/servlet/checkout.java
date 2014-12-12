package net.tutorial.djomeda.iwallet.konohashop.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tutorial.djomeda.iwallet.konohashop.util.DbLayer;

import com.dreamoval.iwallet.connector.Integrator;
import com.i_walletlive.paylive.ArrayOfOrderItem;
import com.i_walletlive.paylive.OrderItem;
import com.mysql.fabric.xmlrpc.base.Array;

/**
 * Servlet implementation class checkout
 */
public class checkout extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Properties pros = new Properties();
	Integrator iwalletIntegrator ;
	DbLayer dbLayer;

    /**
     * Default constructor. Used to initialize properties file located in resource folder
     * 
     */
    public checkout() {
      try {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("local.properties");
		pros.load(inputStream);
		dbLayer = new DbLayer(pros.getProperty("database.driver"), pros.getProperty("username"), pros.getProperty("password"), pros.getProperty("host"), pros.getProperty("database"));
		iwalletIntegrator = new Integrator(pros.getProperty("api.iwallet.merchantKey"), pros.getProperty("api.iwallet.merchantEmail"), Boolean.valueOf(pros.getProperty("api.iwallet.integrationMode")), pros.getProperty("api.iwallet.serviceType"), pros.getProperty("api.iwallet.version"));
	} catch (Exception e) {
		e.printStackTrace();
	}  
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doWork(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doWork(request, response);
	}
	
	/**
	 * Method for boilerplate work. Its purpose is to get the parameters(passed product id list from form) out of the request 
	 * and to create every OrderItem with the help of the method orderItemBuilder. doWork feeds orderItemBuilder by getting 
	 * details about each product by querying the database using DbLayer.getProductById method. After looping through product
	 * id list while creating each OrderItem object, it adds it to the ArrayOfOrderItem object then calls processIwalletOrder
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 */
	public void doWork(HttpServletRequest request, HttpServletResponse response) {
		String[] productList = request.getParameterValues("orderItems");
		ArrayOfOrderItem items = new ArrayOfOrderItem();
		for (int i = 0; i < productList.length; i++) {
			Map<String, Object> productMap = dbLayer.getProductById(Integer.valueOf(productList[i]));
			//for now since we are not using any cart, we will assume one can only buy a type of item one at a time so quantity would be 1
			items.getOrderItem().add(orderItemBuilder((String)productMap.get("name"),(String) productMap.get("product_id"), (BigDecimal)productMap.get("price"), 1));
		}
		
		if(productList.length >0) {
			
			processIwalletOrder(productList,items, response);
		}
	}
	
	/**
	 * 
	 * @param items
	 * @param response
	 */
	private void processIwalletOrder(String[] productIds,ArrayOfOrderItem items, HttpServletResponse response) {
		String orderId = UUID.randomUUID().toString();
		
		try {
			
		
		BigDecimal grandSubtotal = grandSubTotalCalculator(items);
		BigDecimal flatShippingCost = new BigDecimal(pros.getProperty("shippingcost"));
		BigDecimal tax = new BigDecimal(pros.getProperty("taxes"));
		BigDecimal taxAmount = grandSubtotal.multiply(tax).divide(BigDecimal.valueOf(100));
		BigDecimal total = grandSubtotal.add(flatShippingCost).add(taxAmount);
		
		
		String tokenString = iwalletIntegrator.processPaymentOrder(orderId, grandSubtotal, flatShippingCost, taxAmount, total, "konohashop item", "", items);
		
		if(tokenChecker(tokenString)) {
			dbLayer.createOrder(orderId, tokenString, Arrays.asList(productIds));
			response.sendRedirect(pros.getProperty("api.iwallet.redirecturl")+tokenString);
		} else {
			response.getWriter().print("Payment not successful");
		}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param itemName
	 * @param itemCode
	 * @param itemUnitPrice
	 * @param itemQuantity
	 * @return
	 */
	private OrderItem orderItemBuilder(String itemName, String itemCode, BigDecimal itemUnitPrice, int itemQuantity) {
		OrderItem item = new OrderItem();
		item.setItemName(itemName);
		item.setItemCode(itemCode);
		item.setQuantity(itemQuantity);
		item.setUnitPrice(itemUnitPrice);
		item.setSubTotal(BigDecimal.valueOf(itemQuantity).multiply(itemUnitPrice));
		return item;
	}
	
	
	/**
	 * 
	 * @param items
	 * @return
	 */
	private BigDecimal grandSubTotalCalculator(ArrayOfOrderItem items) {
		BigDecimal subTotal = BigDecimal.ZERO;
		
		for (OrderItem item: items.getOrderItem()) {
			subTotal = subTotal.add(item.getSubTotal());
			
		}
		return subTotal;
		
	}
	
	/**
	 * 
	 * @param token
	 * @return
	 */
	private boolean tokenChecker(String token) {
		 try {
	            UUID.fromString(token);
	            return true;
	        } catch (Exception ex) {
	            return false;
	        }
		
	}

}
