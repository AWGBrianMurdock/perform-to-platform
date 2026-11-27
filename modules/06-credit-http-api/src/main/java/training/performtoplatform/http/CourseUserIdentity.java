package training.performtoplatform.http;

import org.springframework.stereotype.Component;

@Component
final class CourseUserIdentity {
    String userId() {
        return "USER-COURSE-001";
    }
}
