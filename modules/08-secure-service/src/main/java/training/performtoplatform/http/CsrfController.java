package training.performtoplatform.http;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CsrfController {
    @GetMapping("/csrf")
    CsrfToken csrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }
}
