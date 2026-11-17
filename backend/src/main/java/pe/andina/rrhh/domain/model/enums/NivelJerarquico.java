package pe.andina.rrhh.domain.model.enums;

/**
 * Nivel del organigrama inferido desde el nombre del cargo.
 * No es un enum de BD: se calcula en runtime para armar el árbol.
 */
public enum NivelJerarquico {
    GERENTE_GENERAL,
    SUBGERENTE_GENERAL,
    JEFE_AREA,
    COLABORADOR
}
