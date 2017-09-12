package org.camunda.bpm.swagger.maven;

import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.camunda.bpm.swagger.docs.DocumentationYaml;
import org.camunda.bpm.swagger.docs.model.DtoDocumentation;
import org.camunda.bpm.swagger.maven.fn.ReflectionsFactory;
import org.camunda.bpm.swagger.maven.fn.ScanRestServices;
import org.camunda.bpm.swagger.maven.generator.SwaggerServiceModelGenerator;
import org.camunda.bpm.swagger.maven.model.CamundaRestService;
import org.camunda.bpm.swagger.maven.model.ModelRepository;
import org.camunda.bpm.swagger.maven.spi.CodeGenerator;
import org.reflections.Reflections;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.camunda.bpm.swagger.maven.GenerateSwaggerServicesMojo.GOAL;

@Mojo(name = GOAL, defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME, threadSafe = false)
public class GenerateSwaggerServicesMojo extends AbstractMojo {

  public DocumentationYaml documentation;

  public static final String CAMUNDA_REST_ROOT_PKG = "org.camunda.bpm.engine.rest";
  public static final String GOAL = "generate-swagger-services";

  private ModelRepository modelRepository;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.directory}/generated-sources/java")
  protected File outputDirectory;

  @Parameter(property = "codeGeneratorFactory", required = true, defaultValue = "org.camunda.bpm.swagger.maven.generator.SwaggerCodeGeneratorFactory")
  protected String codeGeneratorFactoryClass;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/camunda-rest-dto-docs.yaml", required = true, readonly = true)
  protected File yamlFile;

  @Override
  @SneakyThrows
  public void execute() throws MojoExecutionException, MojoFailureException {

    documentation = new DocumentationYaml();
    modelRepository = new ModelRepository(documentation);

    if (!outputDirectory.exists()) {
      Files.createDirectories(outputDirectory.toPath());
    }
    Optional.ofNullable(project).ifPresent(p -> p.addCompileSourceRoot(outputDirectory.getPath()));

    final Reflections reflections = new ReflectionsFactory().get();
    final ScanRestServices scanRestServices = new ScanRestServices(reflections, modelRepository);

    final Set<CamundaRestService> camundaRestServices = scanRestServices.get();

    getLog().info("==================");

    camundaRestServices.stream().map(service -> new SwaggerServiceModelGenerator(service)).forEach(CodeGenerator::generate);

    // write all models
    modelRepository.getModels().forEach(r -> r.write(outputDirectory));

    getLog().info("==================");

    if (modelRepository.getDtoDocs().isEmpty()) {
      throw new IllegalStateException("No docs has been provided.");
    }

    if (yamlFile != null) {
      final Yaml yaml = new Yaml();
      final FileWriter writer = new FileWriter(yamlFile);
      yaml.dump(new DtoDocumentation(modelRepository.getDtoDocs()), writer);
    }
  }

}
