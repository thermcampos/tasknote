package br.com.tasknoteapp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Service to retrieve application version information. */
@Service
public class AppVersionService {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final BuildProperties buildProperties;

  /**
   * Constructor for AppVersionService.
   *
   * @param buildProperties the build properties injected by Spring Boot
   */
  public AppVersionService(@Autowired(required = false) BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  /** Logs the application version once the application context is fully started. */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    logger.info("Task Note API started successfully - Version: {}", getVersion());
  }

  /**
   * Retrieves the application version combined with the build time.
   *
   * @return a string representing the application version and build time
   */
  public String getVersion() {
    if (buildProperties == null) {
      return "unknown";
    }
    return buildProperties.getVersion() + "-" + buildProperties.getTime();
  }
}
