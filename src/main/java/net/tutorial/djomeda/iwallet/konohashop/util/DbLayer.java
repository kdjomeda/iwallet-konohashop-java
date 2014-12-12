package net.tutorial.djomeda.iwallet.konohashop.util;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.skife.jdbi.v2.util.StringMapper;

public class DbLayer {
	
	private DBI db;
	private String driverName;
	private String username;
	private String password;
	private String host;
	private String database;
	
	public DbLayer() {
		
	}
	
	public DbLayer(String driverName, String username, String password, String host, String database ) {
		this.driverName = driverName;
		this.username = username;
		this.password = password;
		this.host = host;
		this.database = database;
		
		db = this.getConnection();

	}
	
	private DBI getConnection() {
		DBI	dbi = null;
		try {
			Class.forName(driverName);
			dbi = new DBI("jdbc:mysql://"+host+"/"+database, username, password);
		
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dbi;
	}
	
	public void setFixtures() {

		if (null == db) {
			db = this.getConnection();
		}
		db.withHandle(new HandleCallback<Void>(){

			public Void withHandle(Handle handle) throws Exception {
				
				handle.execute("insert into category(id,name,description) values(1,'food','all sort of comestible item'),(2,'ninja tools','any sort of tool according to konoha classifications')");
				handle.execute("insert into product(id,category_id,product_id,name,price,in_stock,comment) values(1,1,'ra_0001','ramen',30,20,''),(2,2,'we_0001','shuriken',120,100,''),(3,2,'we_0002','kunai',62,95,'')");
				
				return null;
			}
			
		});
	}
	
	public void tearDown() {
		if (null == db) {
			db = this.getConnection();
		}
		db.withHandle(new HandleCallback<Void>(){

			public Void withHandle(Handle handle) throws Exception {
				handle.execute("SET FOREIGN_KEY_CHECKS = 0");
				handle.execute("truncate table product");
				handle.execute("truncate table category");
				handle.execute("truncate table order_product_map");
				handle.execute("truncate table `order`");
				handle.execute("SET FOREIGN_KEY_CHECKS = 1");
				return null;
			}
			
		});
	}

	
	public List<Map<String, Object>> getAllItems() {
		
		if (null == db) {
			db = this.getConnection();
		}
		List<Map<String, Object>> rows = db.withHandle(new HandleCallback<List<Map<String, Object>>>(){

			public List<Map<String, Object>> withHandle(Handle handle) throws Exception {
	
				return handle.createQuery("select c.name as category_name, p.name ,p.id, p.price,p.product_id ,p.comment from product p inner join category c on c.id = p.category_id where in_stock <> 0").list();
			}
			
		});
		
		return rows;
	}
	
	public void createOrder(final String orderId, final String paymentToken, final List<String> productIdList) {
		if (null == db) {
			db = this.getConnection();
		}
		db.withHandle(new HandleCallback<Void>(){

			public Void withHandle(Handle handle) throws Exception {
				handle.begin();
				
				handle.createStatement("insert into `order`(order_id,payment_token) values (:id, :token)")
				.bind("id", orderId)
				.bind("token", paymentToken)
				.execute();
				
				String order_product_map_query = orderProductMapQueryBuilder(orderId, productIdList);
				handle.execute("insert into order_product_map values "+order_product_map_query);
				
				handle.commit();
				return null;
			}
			
		});
	}
	
	
	
	public void updateOrder(final String orderId, final String paymentTransactionId, final String paymentStatus) {
		if (null == db) {
			db = this.getConnection();
		}
		db.withHandle(new HandleCallback<Void>(){

			public Void withHandle(Handle handle) throws Exception {
				handle.createStatement("update `order` set payment_common_id=:transacId, order_status=:status, date_modified=:date where order_id=:orderId")
				.bind("transacId", paymentTransactionId)
				.bind("status", paymentStatus)
				.bind("orderId", orderId)
				.bind("date", new Date())
				.execute();
				return null;
			}
			
		});
	}
	
	
	private String orderProductMapQueryBuilder(String orderId, List<String> productList) {
		String queryPartString = "";
		StringBuffer stringBuffer = new StringBuffer();
		for(String prodId: productList) {
			stringBuffer.append("('").append(orderId).append("',").append(String.valueOf(prodId)).append(")");
			stringBuffer.append(",");
			
		}
		
		queryPartString = stringBuffer.toString();
		if(!queryPartString.isEmpty()) {
			queryPartString = queryPartString.substring(0, queryPartString.lastIndexOf(","));
		}
		return queryPartString;
	}
	
	public Map<String, Object>getProductById(final int productId) {
		
		if (null == db) {
			db = this.getConnection();
		}
		
		Map<String, Object> row = db.withHandle(new HandleCallback<Map<String, Object>>(){

			public Map<String, Object> withHandle(Handle handle) throws Exception {
	
				return handle.createQuery("select * from product where id=:id")
						.bind("id", productId)
						.first();
			}
			
		});
		
		return row;
		
	}
	
	
	public String countValidTransaction(final String paymentToken) {
		if (null == db) {
			db = this.getConnection();
		}
		
		String count = db.withHandle(new HandleCallback<String>() {

			public String withHandle(Handle handle) throws Exception {
				return handle.createQuery("select order_id from `order` where payment_token= :token and order_status='PENDING'")
				.map(StringMapper.FIRST)
				.bind("token", paymentToken)
				.first();
			}
		});
		return count;
	}
}
