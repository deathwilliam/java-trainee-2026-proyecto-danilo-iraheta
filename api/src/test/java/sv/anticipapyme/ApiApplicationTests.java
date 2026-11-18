package sv.anticipapyme;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // use el puerto 5433 porque en el 5432 ya tenía otra instancia de postgresql de otra version que uso en la universidad
        "spring.datasource.url=jdbc:postgresql://localhost:${DB_PUERTO_HOST:5432}/anticipapyme_test",
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.clave=clave-de-prueba-anticipapyme-para-tests-0123456789"
})
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
