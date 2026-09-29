package pe.andina.rrhh.domain.model.enums;

/**
 * Régimen laboral según Ley 32353 (MYPE) y régimen general.
 * Determina beneficios como vacaciones, gratificaciones, CTS, y aporte a salud.
 */
public enum RegimenLaboral {
    /**
     * Régimen general (D. Leg. 728): 30 días vacaciones, 2 gratificaciones completas,
     * CTS completo, EsSalud 9%, AFP/ONP estándar.
     */
    GENERAL,
    
    /**
     * MYPE Microempresa (hasta 150 UIT, Ley 32353): 15 días vacaciones,
     * sin gratificaciones obligatorias, sin CTS, SIS en lugar de EsSalud.
     */
    MYPE_MICRO,
    
    /**
     * MYPE Pequeña Empresa (hasta 1,700 UIT, Ley 32353): 15 días vacaciones,
     * media gratificación julio y diciembre, medio CTS semestral, EsSalud 9%.
     */
    MYPE_PEQUENA
}
