package br.com.tasknoteapp.server.config;

import br.com.tasknoteapp.server.hint.HttpServletRequestRuntimeHint;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/** This class contains configurations for the GraalVM Cloud Native image. */
@Configuration
@RegisterReflectionForBinding({
  io.jsonwebtoken.Claims.class,
  io.jsonwebtoken.Jwts.class,
  io.jsonwebtoken.Jwts.SIG.class,
  io.jsonwebtoken.Jwts.ENC.class,
  io.jsonwebtoken.Jwts.KEY.class,
  io.jsonwebtoken.impl.security.KeysBridge.class,
  io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms.class,
  io.jsonwebtoken.impl.security.StandardKeyOperations.class,
  io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms.class,
  io.jsonwebtoken.impl.security.StandardKeyAlgorithms.class,
  io.jsonwebtoken.impl.io.StandardCompressionAlgorithms.class,
  io.jsonwebtoken.impl.DefaultClaimsBuilder.class,
  io.jsonwebtoken.impl.DefaultJwtParserBuilder.class,
  io.jsonwebtoken.impl.DefaultJwtBuilder.class,
  io.jsonwebtoken.lang.Supplier.class,
})
@ImportRuntimeHints(value = {HttpServletRequestRuntimeHint.class})
public class CloudNativeConfig {}
