package com.chatdemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class QueryTest {
 @Autowired MockMvc mvc;
 @Test void summary() throws Exception { mvc.perform(get("/api/summary")).andExpect(status().isOk()).andExpect(jsonPath("$.TOTAL").value(12)).andExpect(jsonPath("$.CUSTOMERS").value(7)); }
 @Test void searchAndPagination() throws Exception { mvc.perform(get("/api/orders").param("keyword","星河").param("status","待处理")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].ORDER_NO").value("BZ202610012")); mvc.perform(get("/api/orders").param("page","2")).andExpect(jsonPath("$.items.length()").value(4)); }
 @Test void literalWildcardAndInjection() throws Exception { for(String keyword: new String[]{"%","_","' OR 1=1 --"}) mvc.perform(get("/api/orders").param("keyword",keyword)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0)); }
 @Test void detailAndValidation() throws Exception { mvc.perform(get("/api/orders/1")).andExpect(status().isOk()).andExpect(jsonPath("$.CUSTOMER").value("星河科技有限公司")); mvc.perform(get("/api/orders/999")).andExpect(status().isNotFound());mvc.perform(get("/api/orders").param("page","0")).andExpect(status().isBadRequest());mvc.perform(get("/api/orders").param("status","unknown")).andExpect(status().isBadRequest()); }
}
