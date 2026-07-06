package de.serav.cv.assistant;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.TargetElement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Push
@Inline(value = "og-meta.html", target = TargetElement.HEAD)
@SpringBootApplication
public class CvAssistent implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(CvAssistent.class, args);
    }

}