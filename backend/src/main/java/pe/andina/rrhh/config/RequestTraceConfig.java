package pe.andina.rrhh.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.andina.rrhh.adapter.in.web.trace.RequestTraceFilter;
import pe.andina.rrhh.adapter.in.web.trace.RequestTraceInterceptor;

@Configuration
public class RequestTraceConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<RequestTraceFilter> requestTraceFilterRegistration() {
        FilterRegistrationBean<RequestTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestTraceFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("requestTraceFilter");
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestTraceInterceptor()).addPathPatterns("/api/**");
    }
}
