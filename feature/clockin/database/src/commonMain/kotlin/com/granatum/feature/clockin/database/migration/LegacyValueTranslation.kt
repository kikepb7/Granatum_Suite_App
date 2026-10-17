package com.granatum.feature.clockin.database.migration

/**
 * Spanish values stored up to version 3, and the English ones the contract uses since version 4
 * (docs/english-naming.md). Pure data and functions so [Migration3To4] can be tested without a
 * database.
 */
object LegacyValueTranslation {

    /** `clock_event.breakType`, and the `type` of each break inside `server_shift.breaksJson`. */
    val breakTypes: Map<String, String> = mapOf(
        "COMIDA" to "MEAL",
        "DESCANSO" to "REST",
        "OTRO" to "OTHER"
    )

    /** `server_shift.status` (was `estado`). */
    val shiftStatuses: Map<String, String> = mapOf(
        "EN_CURSO" to "IN_PROGRESS",
        "CERRADO" to "CLOSED",
        "INCOMPLETO" to "INCOMPLETE"
    )

    /**
     * `clock_event.rejectionCode`: the server's `code` when a punch was refused, or one of the
     * app's own local codes. Codes already in English (and `HTTP_<status>`) are left as they are.
     */
    val rejectionCodes: Map<String, String> = mapOf(
        // Local codes of this app.
        "JORNADA_NO_REGISTRADA" to "SHIFT_NOT_REGISTERED",
        "RESPUESTA_ILEGIBLE" to "UNREADABLE_RESPONSE",
        // Server codes a punch can be refused with.
        "DESVIACION_RELOJ" to "CLOCK_SKEW",
        "EMPLEADO_INACTIVO" to "EMPLOYEE_INACTIVE",
        "PAUSA_YA_ABIERTA" to "BREAK_ALREADY_OPEN",
        "PAUSA_NO_ABIERTA" to "BREAK_NOT_OPEN",
        "PAUSA_ABIERTA_AL_CERRAR" to "BREAK_OPEN_ON_CLOSE",
        "FICHAJE_NO_EN_CURSO" to "SHIFT_NOT_IN_PROGRESS",
        "FICHAJE_YA_EN_CURSO" to "SHIFT_ALREADY_IN_PROGRESS",
        "FICHAJE_NOT_FOUND" to "SHIFT_NOT_FOUND",
        "FICHAJE_NO_FINALIZADO" to "SHIFT_NOT_FINISHED",
        "FICHAJE_INMUTABLE" to "SHIFT_IMMUTABLE",
        "CLIENT_EVENT_ID_REUTILIZADO" to "CLIENT_EVENT_ID_REUSED",
        "VALIDACION" to "VALIDATION",
        "EMPLEADO_NOT_FOUND" to "EMPLOYEE_NOT_FOUND",
        "EMPLEADO_NO_ENCONTRADO" to "EMPLOYEE_NOT_FOUND",
        "NO_AUTENTICADO" to "NOT_AUTHENTICATED",
        "TOKEN_ACCESO_EXPIRADO" to "ACCESS_TOKEN_EXPIRED",
        "DEMASIADAS_PETICIONES" to "TOO_MANY_REQUESTS",
        "SERVICIO_SATURADO" to "SERVICE_BUSY",
        "PETICION_DEMASIADO_GRANDE" to "REQUEST_TOO_LARGE",
        "ERROR_INTERNO" to "INTERNAL_ERROR",
        "RECURSO_NO_ENCONTRADO" to "RESOURCE_NOT_FOUND",
        "METODO_NO_PERMITIDO" to "METHOD_NOT_ALLOWED",
        "TIPO_NO_ADMITIDO" to "UNSUPPORTED_MEDIA_TYPE"
    )

    /** `server_shift` columns, version 3 name to version 4 name. */
    val serverShiftColumns: List<Pair<String, String>> = listOf(
        "entradaEpochMillis" to "clockInEpochMillis",
        "salidaEpochMillis" to "clockOutEpochMillis",
        "estado" to "status",
        "minutosTrabajados" to "workedMinutes",
        "fueIncompleto" to "wasIncomplete",
        "pausasJson" to "breaksJson",
        "corregido" to "corrected",
        "reconstruido" to "reconstructed"
    )

    private val legacyBreakKeys = mapOf("inicio" to "start", "tipo" to "type", "fin" to "end")
    private val legacyBreakKey = Regex("\"(inicio|tipo|fin)\"(\\s*):")
    private val breakTypeValue = Regex("(\"type\"\\s*:\\s*)\"([A-Z_]+)\"")

    /**
     * A shift's breaks as version 3 cached them (`[{"id","inicio","tipo","fin"}]`, Spanish types)
     * to the contract's shape (`[{"id","start","type","end"}]`, English types). Values are UUIDs
     * and ISO instants, so the keys cannot appear inside them. Idempotent.
     */
    fun translateBreaksJson(json: String): String =
        json
            .replace(legacyBreakKey) { "\"${legacyBreakKeys.getValue(it.groupValues[1])}\"${it.groupValues[2]}:" }
            .replace(breakTypeValue) { match ->
                val value = match.groupValues[2]
                "${match.groupValues[1]}\"${breakTypes[value] ?: value}\""
            }

    /** One UPDATE that rewrites every known legacy value of [column] and leaves the rest alone. */
    fun updateValuesSql(table: String, column: String, values: Map<String, String>): String {
        val cases = values.entries.joinToString(" ") { (old, new) -> "WHEN '$old' THEN '$new'" }
        val olds = values.keys.joinToString(", ") { "'$it'" }
        return "UPDATE `$table` SET `$column` = CASE `$column` $cases ELSE `$column` END WHERE `$column` IN ($olds)"
    }
}
