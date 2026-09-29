package br.com.tasknoteapp.server.hint;

import br.com.tasknoteapp.server.service.ResendEmailRequest;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * This class creates RuntimeHints for the Resend email request record to ensure Jackson can
 * serialize it at runtime in native images.
 */
@Configuration
@ImportRuntimeHints(ResendEmailRuntimeHints.ResendEmailHintsRegistrar.class)
public class ResendEmailRuntimeHints {

  static class ResendEmailHintsRegistrar implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints
          .reflection()
          .registerType(
              ResendEmailRequest.class,
              MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
              MemberCategory.INVOKE_PUBLIC_METHODS,
              MemberCategory.DECLARED_FIELDS);
    }
  }
}
