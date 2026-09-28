package pe.andina.rrhh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Activa o desactiva el filtro. */
    private boolean enabled = true;

    /** Tope general de requests por minuto por IP (rutas /api/**). */
    private int apiPerMinute = 120;

    /** Tope estricto para login/refresh por minuto por IP. */
    private int authPerMinute = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getApiPerMinute() {
        return apiPerMinute;
    }

    public void setApiPerMinute(int apiPerMinute) {
        this.apiPerMinute = apiPerMinute;
    }

    public int getAuthPerMinute() {
        return authPerMinute;
    }

    public void setAuthPerMinute(int authPerMinute) {
        this.authPerMinute = authPerMinute;
    }
}
