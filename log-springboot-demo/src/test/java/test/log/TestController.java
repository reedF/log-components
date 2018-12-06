package test.log;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.reed.log.demo.LogDemoApplication;
import com.reed.log.demo.test.AnnotationTestService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = LogDemoApplication.class)
public class TestController {
	@Autowired
	private AnnotationTestService bean;

	@Test
	public void contextLoads() {
	}

	private MockMvc mockMvc; // 模拟MVC对象，通过MockMvcBuilders.webAppContextSetup(this.wac).build()初始化。

	@Autowired
	private WebApplicationContext wac; // 注入WebApplicationContext

	@Before // 在测试开始前初始化工作
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	// 同时测试@LogAopPointCut
	@Test
	public void test() throws Exception {
		MvcResult result = mockMvc.perform(post("/obj").param("name", "中文").param("uri", "eee"))
				.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();// 返回执行请求的结果
	}

	@Test
	public void testAnn() {
		bean.test(null, "ddddd");
	}
}