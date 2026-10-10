package com.granatum.core.data.demo

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DemoUser(
    val employeeId: String,
    var email: String,
    var password: String,
    var role: String,
    var mustChangePassword: Boolean = false,
)

class DemoEmployee(
    val id: String,
    var name: String,
    val document: String,
    var position: String,
    var contract: String,
    var startDate: LocalDate,
    var active: Boolean = true,
)

class DemoPause(
    val id: String,
    val type: String,
    val start: Instant,
    var end: Instant? = null,
)

class DemoShift(
    val id: String,
    val employeeId: String,
    var entry: Instant,
    var exit: Instant? = null,
    val pauses: MutableList<DemoPause> = mutableListOf(),
    var incomplete: Boolean = false,
) {
    val state: String get() = if (exit == null) "EN_CURSO" else "CERRADO"
    val workedMinutes: Int?
        get() =
            exit?.let { out ->
                ((out - entry) - pauses.sumOf { ((it.end ?: out) - it.start).inWholeMinutes }.minutes).inWholeMinutes.toInt()
            }
}

class DemoCorrection(
    val id: String,
    val shiftId: String,
    val requesterId: String,
    val reason: String,
    val proposed: kotlinx.serialization.json.JsonObject,
    val createdAt: Instant,
)

/** The demo's data, seeded with a small flower shop. */
@OptIn(ExperimentalUuidApi::class)
class DemoState(
    val clock: Clock = Clock.System,
) {
    val madrid = TimeZone.of("Europe/Madrid")

    fun now(): Instant = clock.now()

    fun today(): LocalDate = now().toLocalDateTime(madrid).date

    fun newId(): String = Uuid.random().toString()

    val employees =
        mutableListOf(
            DemoEmployee(ADMIN_ID, DemoMode.ADMIN.displayName, "12345678Z", "Dirección", "JORNADA_COMPLETA", LocalDate(2023, 3, 1)),
            DemoEmployee(EMPLOYEE_ID, DemoMode.EMPLOYEE.displayName, "87654321X", "Florista", "PARCIAL", LocalDate(2024, 9, 16)),
            DemoEmployee(MANAGER_ID, "Elena Ríos", "11111111H", "Encargada de tienda", "JORNADA_COMPLETA", LocalDate(2024, 1, 8)),
            DemoEmployee(newId(), "Pablo Soler", "X1234567L", "Repartidor", "POR_HORAS", LocalDate(2025, 5, 5), active = false),
        )

    val users =
        mutableListOf(
            DemoUser(ADMIN_ID, DemoMode.ADMIN.email, DemoMode.ADMIN.password, "ADMIN"),
            DemoUser(EMPLOYEE_ID, DemoMode.EMPLOYEE.email, DemoMode.EMPLOYEE.password, "EMPLEADO"),
            DemoUser(MANAGER_ID, "encargada@demo.granatum.es", "Demo-Encargada-2026!", "ENCARGADO"),
        )

    private val accessTokens = mutableMapOf<String, String>()
    private val refreshTokens = mutableMapOf<String, String>()

    /**
     * Tokens issued in this run, or a demo token from an earlier run (this backend forgets its
     * tokens when the app closes): its subject still names a demo person, so it keeps working.
     */
    fun userForToken(token: String): DemoUser? {
        val id = accessTokens[token]
            ?: token.takeIf { it.endsWith(".demo") }?.let { com.granatum.core.data.auth.token.AccessTokenClaims.parse(it)?.employeeId }
        return id?.let { employeeId -> users.firstOrNull { it.employeeId == employeeId } }
    }

    fun userForRefresh(token: String): DemoUser? = refreshTokens.remove(token)?.let { id -> users.firstOrNull { it.employeeId == id } }

    fun employee(id: String) = employees.firstOrNull { it.id == id }

    /** A JWT the app can read (`sub`, `role`, `pwd_change`); unsigned, as nothing verifies it here. */
    @OptIn(ExperimentalEncodingApi::class)
    fun issueTokens(user: DemoUser): Pair<String, String> {
        val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        val header = b64.encode("""{"alg":"none","typ":"JWT"}""".encodeToByteArray())
        val pwd = if (user.mustChangePassword) ""","pwd_change":true""" else ""
        val payload =
            b64.encode(
                """{"sub":"${user.employeeId}","role":"${user.role}","type":"access","jti":"${newId()}"$pwd}""".encodeToByteArray(),
            )
        val access = "$header.$payload.demo"
        val refresh = "refresh-${newId()}"
        accessTokens[access] = user.employeeId
        refreshTokens[refresh] = user.employeeId
        return access to refresh
    }

    fun revokeAll(employeeId: String) {
        accessTokens.values.removeAll { it == employeeId }
        refreshTokens.values.removeAll { it == employeeId }
    }

    val shifts: MutableList<DemoShift> =
        mutableListOf<DemoShift>().apply {
            // Three weeks of past working days for the two demo people.
            for (daysAgo in 1..21) {
                val day = today().minus(DatePeriod(days = daysAgo))
                if (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) continue
                add(shift(ADMIN_ID, day, LocalTime(8, 30), LocalTime(16, 0), lunch = true))
                add(shift(EMPLOYEE_ID, day, LocalTime(9, 0), LocalTime(13, 30 + (daysAgo % 3) * 5), lunch = false))
            }
        }

    private fun shift(
        employeeId: String,
        day: LocalDate,
        start: LocalTime,
        end: LocalTime,
        lunch: Boolean,
    ): DemoShift {
        val entry = day.atTime(start).toInstant(madrid)
        val exit = day.atTime(end).toInstant(madrid)
        val pauses = mutableListOf(DemoPause(newId(), "DESCANSO", entry + 150.minutes, entry + 165.minutes))
        if (lunch) pauses += DemoPause(newId(), "COMIDA", entry + 270.minutes, entry + 300.minutes)
        return DemoShift(newId(), employeeId, entry, exit, pauses)
    }

    val corrections = mutableListOf<DemoCorrection>()

    /** clientEventId -> shift id: a resent punch gets the same shift back, as on the server. */
    val processedEvents = mutableMapOf<String, String>()

    companion object {
        const val ADMIN_ID = "0b6f4f3e-0000-4000-8000-00000000a001"
        const val EMPLOYEE_ID = "0b6f4f3e-0000-4000-8000-00000000e001"
        const val MANAGER_ID = "0b6f4f3e-0000-4000-8000-00000000c001"
    }
}
