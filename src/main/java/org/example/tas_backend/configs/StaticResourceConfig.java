package org.example.tas_backend.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${storage.upload-root:uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadsPath = Path.of(uploadRoot).toAbsolutePath().normalize();
        String location = uploadsPath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
