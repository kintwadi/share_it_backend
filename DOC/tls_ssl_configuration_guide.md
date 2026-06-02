# Production TLS/SSL Configuration for Spring Boot (No Spring Security)

This guide outlines how to implement TLS/SSL termination directly within a Spring Boot REST application while maintaining production security standards.

## 1. Prepare the External Keystore
Do not package the keystore inside the JAR. Generate a PKCS12 keystore and place it on the production server.

```bash
# Generate a self-signed cert for testing (Use a CA-signed cert for production)
keytool -genkeypair -alias springboot -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

## 2. Configure application.properties
Use external file paths and environment variables for sensitive passwords.

These values are an example production configuration and are meant to override the local defaults (which run HTTP on a non-privileged port).

```properties
# Port and SSL Activation
server.port=443
server.ssl.enabled=true

# External Keystore Location
server.ssl.key-store=file:/etc/ssl/certs/keystore.p12
security.keystore.password=\${SSL_PASSWORD}
server.ssl.key-store-password=\${security.keystore.password}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=springboot

# Hardening Protocols and Ciphers
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
server.http2.enabled=true
```

## 3. Programmatic HTTP to HTTPS Redirect
Since Spring Security is not used, add this configuration bean to handle the redirect from port 80 to 443.

```java
@Configuration
public class HttpToHttpsConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(80); 
        connector.setSecure(false);
        connector.setRedirectPort(443); 
        return connector;
    }
}
```

## 4. Manual HSTS Implementation
Inject the Strict-Transport-Security header manually via a Web Filter.

```java
@Component
public class HstsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        chain.doFilter(req, res);
    }
}
```

## 5. Deployment (Docker Example)
Mount the keystore as a read-only volume.

```yaml
services:
  api:
    image: your-app-image
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./certs/keystore.p12:/etc/ssl/certs/keystore.p12:ro
    environment:
      - SSL_PASSWORD=your_secure_password
```

---

