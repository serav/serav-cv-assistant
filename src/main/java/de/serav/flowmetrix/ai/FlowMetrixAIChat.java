package de.serav.flowmetrix.ai;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Push
@SpringBootApplication
public class FlowMetrixAIChat implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FlowMetrixAIChat.class, args);
    }

}