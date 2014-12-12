package net.tutorial.djomeda.iwallet.konohashop.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tutorial.djomeda.iwallet.konohashop.util.DbLayer;

import com.dreamoval.iwallet.connector.Integrator;
import com.i_walletlive.paylive.OrderResult;

/**
 * Servlet implementation class receivecallback
 */
public class receivecallback extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Properties pros = new Properties();
	Integrator iwalletIntegrator;
	DbLayer dbLayer;

    /**
     * Default constructor. 
     */
    public receivecallback() {
        if (null == iwalletIntegrator) {
        	 try {
        			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("local.properties");
        			pros.load(inputStream);
        			dbLayer = new DbLayer(pros.getProperty("database.driver"), pros.getProperty("username"), pros.getProperty("password"), pros.getProperty("host"), pros.getProperty("database"));
        			iwalletIntegrator = new Integrator(pros.getProperty("api.iwallet.merchantKey"), pros.getProperty("api.iwallet.merchantEmail"), Boolean.valueOf(pros.getProperty("api.iwallet.integrationMode")), pros.getProperty("api.iwallet.serviceType"), pros.getProperty("api.iwallet.version"));
        		} catch (Exception e) {
        			e.printStackTrace();
        		}  
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
	
	private void doWork(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String paramString = "status=0&transac_id=3493-455454-EEEE&pay_token=cbf4a3db-5426-4835-a6a4-bc62c0e47b06&cust_ref=f2bbfcaa-236a-43ec-94cb-220d949b477e";
		
		String statusCode = request.getParameter("status");
		String transactionId = request.getParameter("transac_id");
		String orderId = request.getParameter("cust_ref");
		String paymentToken = request.getParameter("pay_token");
		
		if(null == statusCode || null == orderId || null== paymentToken) {
			response.getOutputStream().print("Not good, details are missing or someone is messing with the code");
			return;
		}
		String paymentStatus = parseTransactionStatusCode(statusCode);
		
		
		
		if(null == transactionId || transactionId.isEmpty()) {
			dbLayer.updateOrder(orderId, "", "FAILED");
			response.getOutputStream().println("Empty or Null transaction Id");
			return;
		}
		
		if(!checkValidity(paymentToken, orderId)) {
				response.getOutputStream().println("There is no transaction corresponding to the received payment token. Please contact iWallet support");
				return;
		}
			
		OrderResult trueStatusOfPayment = iwalletIntegrator.verifyMobilePayment(orderId);
		
		if(!trueStatusOfPayment.isSuccess()) {
				dbLayer.updateOrder(orderId, transactionId, paymentStatus);
				//do another process like initiate shipping and email and sms notification
				iwalletIntegrator.confirmTransaction(paymentToken, transactionId);
				
				response.getOutputStream().println("Yatta!! Your order is on the way");
		}else {
			response.getOutputStream().println("Something seems to be wrong with your order, Kindly start afresh");
			iwalletIntegrator.cancelTransaction(paymentToken, transactionId);
		}
		
		
   }
	
	private Boolean checkValidity(String paymentToken, String orderId) {
		String savedOrderIdString = dbLayer.countValidTransaction(paymentToken);
		if (null== savedOrderIdString || savedOrderIdString.isEmpty()) {
			return false;
		}
		if (!orderId.equals(savedOrderIdString)) {
			return false;
		}
		return true;
	}
	
	
	private String parseTransactionStatusCode(String statusCode) {
		String status = "";
        switch (statusCode){
            case "0":
                status = "success";
                break;
            case "-2":
                status = "cancelled";
                break;
            case "-1":
                status = "error";
                break;
            default:
            	status = "unknown";
           
        }
        return status;
	}
	
	

}
