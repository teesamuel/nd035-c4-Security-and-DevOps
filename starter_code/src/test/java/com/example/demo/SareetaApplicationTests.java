package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.LoginRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SareetaApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	public void contextLoads() {
	}

	@Autowired
	UserController userController;

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	CartController cartController;

	private String base_uri ="http://localhost:";

	TestRestTemplate restTemplate = new TestRestTemplate();


	@Test
	public void TestTokenReturned(){
		createUser();
		String token = LoginGetToken();
		Assert.assertNotNull(token);
		Assert.assertTrue(token.contains("Bearer"));
	}

	@Test
	public void TestInconsistentPassword() throws UnsupportedOperationException {
		CreateUserRequest req = new CreateUserRequest();
		req.setUsername("canCreateUserTest");
		req.setPassword("1234");
		req.setConfirmPassword("4321");
		ResponseEntity<User> res = userController.createUser(req);
	}
	@Test
	public void TestInvalidLogin(){
		HttpEntity entity = new HttpEntity(wrongLoginUserData(), getHeader());
		String url = base_uri + port  + "/login";
		ResponseEntity<Object> res = restTemplate.postForEntity(url, entity, Object.class);
		Assert.assertTrue(res.getStatusCode().is4xxClientError());
		Assert.assertFalse(res.getHeaders().containsKey("Authorization"));
	}
	private ResponseEntity<Object> createUser(){
		String url = base_uri + port  + "/api/user/create";
		HttpEntity entity = new HttpEntity(createUserData(), getHeader());
		ResponseEntity<Object> res = restTemplate.postForEntity(url, entity, Object.class);
		return res;
	}

	private Map<?,?> createUserData(){
		Map<String, String> body = new HashMap<>();
		body.put("username", "testuser");
		body.put("password", "password");
		body.put("confirmPassword", "password");
		return body;
	}
//	private Map<?,?> loginUserData(){
//
//		Map<String, String> body = new HashMap<>();
//		body.put("username", "testuser");
//		body.put("password", "password");
//		return body;
//	}

	private LoginRequest loginUserData(){
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setPassword("password");
		loginRequest.setUsername("testuser");
		return loginRequest;
	}

	private ModifyCartRequest getModifyCartData(){
		ModifyCartRequest cart = new ModifyCartRequest();
		cart.setUsername("testuser");
		cart.setQuantity(5);
		cart.setItemId(2);
		return cart;
	}

	private Map<?,?> wrongLoginUserData(){
		Map<String, String> body = new HashMap<>();
		body.put("username", "testuser3");
		body.put("password", "password");
		return body;
	}

	private  HttpHeaders getHeader(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
	private  HttpHeaders getHeader(String token){
		HttpHeaders headers = getHeader();
		headers.setBearerAuth(token);
		return headers;
	}

	private String LoginGetToken() {
		String result = "";
		createUser();
		HttpEntity entity = new HttpEntity(loginUserData(), getHeader());

		try {
			String url = base_uri + port  + "/login";
			ResponseEntity<Object> res = restTemplate.postForEntity(url, entity, Object.class);
			if (res.getHeaders().containsKey("Authorization")) {
				result = res.getHeaders().get("Authorization").get(0);
			}
		} catch (Exception e) {
			throw new UnsupportedOperationException(e.getMessage());
		}
		return result;
	}

	/*  Cart Controller */

	@Test
	public void TestforbidenAddToCart(){
		String url = base_uri + port  + "/api/cart/addToCart";
		ResponseEntity<Object> res = restTemplate.getForEntity(url, Object.class);
		System.out.println(res.getStatusCode());
		Assert.assertTrue(res.getStatusCode() == HttpStatus.FORBIDDEN);
	}
	@Test
	public void TestAddToCart(){
		addToCart();
	}

	public void addToCart(){
		String url = base_uri + port  + "/api/cart/addToCart";
		String token = LoginGetToken();

		Map body = new HashMap();
		body.put("username", "testuser");
		body.put("itemId", 2);
		body.put("quantity", 1);

		HttpEntity entity = new HttpEntity(body, getHeader(token));
		ResponseEntity res = restTemplate.postForEntity(url, entity, Object.class);
	System.out.println(res.getStatusCode());
		Assert.assertEquals(res.getStatusCode(), HttpStatus.OK);
	}

	@Test
	public void TestModifyCart(){
		String url = base_uri + port  + "/api/cart/addToCart";
		addToCart();
		String token = LoginGetToken();
		HttpEntity entity = new HttpEntity(getModifyCartData(), getHeader(token));
		ResponseEntity res = restTemplate.postForEntity(url, entity, Object.class);
		Assert.assertEquals(res.getStatusCode(), HttpStatus.OK);
	}

	@Test
	public void TestRemoveToCart(){
		String url = base_uri + port  + "/api/cart/removeFromCart";
		String token = LoginGetToken();
		System.out.println(token);
		Map body = new HashMap();
		body.put("username", "testuser");
		body.put("itemId", 2);
		body.put("quantity", 1);



		HttpEntity entity = new HttpEntity(body, getHeader(token));

		ResponseEntity res = restTemplate.postForEntity(url, entity, Object.class);
		System.out.println(res.getStatusCode());
		Assert.assertEquals(res.getStatusCode(), HttpStatus.OK);
	}

	/*  OrderController */
	@Test
	public void TestUnauthorisedOrderRequest(){
		String urlSubmit = base_uri + port  + "api/order/submit/testuser";
		String urlHistory = base_uri + port  + "api/order/history/testuser";
		HttpEntity entity = null;

		ResponseEntity fail1 = restTemplate.getForEntity(urlHistory, Object.class);
		ResponseEntity fail2 = restTemplate.postForEntity(urlSubmit, entity, Object.class);

		// should block without auth
		Assert.assertTrue(fail1.getStatusCode() == HttpStatus.UNAUTHORIZED || fail1.getStatusCode() == HttpStatus.FORBIDDEN);
		Assert.assertTrue(fail2.getStatusCode() == HttpStatus.UNAUTHORIZED || fail2.getStatusCode() == HttpStatus.FORBIDDEN);
	}
	@Test
	public void TestSubmitAndListOrder(){

		String urlSubmit = base_uri + port  + "api/order/submit/testuser";
		String urlHistory = base_uri + port  + "api/order/history/testuser";


		String token = LoginGetToken();
		System.out.println(token);
		Map body = new HashMap();
		body.put("username", "testuser");
		body.put("itemId", 2);
		body.put("quantity", 1);



		HttpEntity entity = new HttpEntity(body, getHeader(token));

		ResponseEntity success1 = restTemplate.exchange(urlHistory, HttpMethod.GET, entity, Object.class);
		ResponseEntity success2 = restTemplate.exchange(urlSubmit, HttpMethod.POST, entity, Object.class);

		//  200 or 404 instead of 403 forbidden due to authentication
		Assert.assertTrue(success1.getStatusCode() == HttpStatus.OK || success1.getStatusCode() == HttpStatus.NOT_FOUND);
		Assert.assertTrue(success2.getStatusCode() == HttpStatus.OK || success2.getStatusCode() == HttpStatus.NOT_FOUND);
	}


	@Test
	public void TestUnauthorisedItemList(){
		String urlById = base_uri + port  + "api/item/1";
		String urlByName = base_uri + port  + "api/item/name/Round Widget";

		ResponseEntity fail1 = restTemplate.getForEntity(urlById, Object.class);
		ResponseEntity fail2 = restTemplate.getForEntity(urlByName, Object.class);

		// should fail without auth
		Assert.assertTrue(fail1.getStatusCode() == HttpStatus.UNAUTHORIZED || fail1.getStatusCode() == HttpStatus.FORBIDDEN);
		Assert.assertTrue(fail2.getStatusCode() == HttpStatus.UNAUTHORIZED || fail2.getStatusCode() == HttpStatus.FORBIDDEN);
	}

	@Test
	public void TestItemList(){

		String urlById = base_uri + port  + "api/item/1";
		String urlByName = base_uri + port  + "api/item/name/Round Widget";

		String token = LoginGetToken();
		Map body = new HashMap();

		HttpEntity entity =  new HttpEntity(body, getHeader(token));

		ResponseEntity success1 = restTemplate.exchange(urlById, HttpMethod.GET, entity, Object.class);
		ResponseEntity success2 = restTemplate.exchange(urlByName, HttpMethod.GET, entity, Object.class);

		// returns 200 or 404 instead of 403 forbidden when you are not authenticated
		Assert.assertTrue(success1.getStatusCode() == HttpStatus.OK || success1.getStatusCode() == HttpStatus.NOT_FOUND);
		Assert.assertTrue(success2.getStatusCode() == HttpStatus.OK || success2.getStatusCode() == HttpStatus.NOT_FOUND);
	}
}
