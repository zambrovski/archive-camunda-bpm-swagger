package org.camunda.bpm.swagger.example.springboot;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
@EnableProcessApplication
public class MainApplication {

  public static void main(String[] args) {
    SpringApplication.run(MainApplication.class, args);
  }

  @Bean
  WebMvcConfigurerAdapter webMvcConfigurerAdapter() {
    return new WebMvcConfigurerAdapter() {
      @Override
      public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger", "/webjars/swagger-ui/3.1.4");
        super.addViewControllers(registry);
      }
    };
  }

  @EventListener
  public void run(PostDeployEvent event) {
    event.getProcessEngine().getRepositoryService().createDeployment()
      .addModelInstance("dummy.bpmn", Bpmn.createExecutableProcess("dummy")
        .startEvent()
        .userTask("task").name("Do Stuff")
        .endEvent()
      .done())
      .deploy();

  }

}
