package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * `/api/sheets…` (ADMIN only), following the backend spec 012 contract, without Google:
 *
 * - a link containing `no-compartida` is not shared with the account (`422 NOT_SHARED`), one with
 *   `solo-lectura` is shared read only (`READ_ONLY`) and one with `no-existe` does not exist;
 * - a task is written on the next read (`GET /tasks/{id}` or `/status`): `DONE`, with the rows of
 *   the demo shifts or confirmed invoices of its month;
 * - the status starts with a failed receipt write, «hoja de recibos sin permiso de edición».
 */
class DemoSheetsRoutes(
    private val state: DemoState,
    private val invoicing: DemoInvoicingRoutes,
) {
    private class Task(
        val id: String,
        val kind: String,
        val reference: String,
        val createdAt: Instant,
    ) {
        var status = "PENDING"
        var attempts = 0
        var rowCount: Int? = null
        var lastNamesPending: Int? = null
        var lastError: String? = null
        var updatedAt: Instant = createdAt
        var completedAt: Instant? = null
    }

    private var shiftsId: String? = null
    private var invoicesId: String? = null
    private var configuredAt: Instant? = null
    private var configuredBy: String? = null
    private var lastSuccessAt: Instant? = null
    private val tasks = mutableListOf<Task>()

    init {
        val failedAt = state.now() - 2.hours
        tasks +=
            Task(state.newId(), "INVOICE", state.newId(), failedAt - 30.minutes).apply {
                status = "FAILED"
                attempts = 8
                lastError = READ_ONLY_MESSAGE
                updatedAt = failedAt
            }
    }

    fun route(request: DemoRequest): DemoReply? {
        if (!request.isAdmin) return forbidden
        val s = request.segments
        val get = request.method == HttpMethod.Get
        val post = request.method == HttpMethod.Post
        return when {
            s.size == 2 && s[1] == "settings" && get -> json(settingsJson())
            s.size == 2 && s[1] == "settings" && request.method == HttpMethod.Put -> save(request)
            s.size == 4 && s[1] == "shifts" && post -> enqueueMonth("SHIFT_MONTH", s[2], s[3], shiftsId)
            s.size == 5 && s[1] == "invoices" && s[4] == "resync" && post -> enqueueMonth("INVOICE_MONTH", s[2], s[3], invoicesId)
            s.size == 2 && s[1] == "status" && get -> {
                work()
                json(statusJson())
            }
            s.size == 3 && s[1] == "tasks" && s[2] == "retry-failed" && post -> {
                val failed = tasks.filter { it.status == "FAILED" }
                failed.forEach { requeue(it) }
                json(buildJsonObject { put("requeued", failed.size) }, HttpStatusCode.Accepted)
            }
            s.size == 3 && s[1] == "tasks" && get -> {
                val task = task(s[2]) ?: return taskNotFound(s[2])
                work()
                json(task.toJson())
            }
            s.size == 4 && s[1] == "tasks" && s[3] == "retry" && post -> {
                val task = task(s[2]) ?: return taskNotFound(s[2])
                if (task.status != "FAILED") {
                    return error(HttpStatusCode.Conflict, "TASK_NOT_FAILED", "Solo se pueden reintentar las tareas fallidas")
                }
                requeue(task)
                json(task.toJson(), HttpStatusCode.Accepted)
            }
            else -> null
        }
    }

    private fun save(request: DemoRequest): DemoReply {
        val shifts =
            request.body
                .str("shiftsSpreadsheet")
                ?.trim()
                .orEmpty()
        val invoices =
            request.body
                .str("invoicesSpreadsheet")
                ?.trim()
                .orEmpty()
        if (shifts.isEmpty()) return invalid("fichajes")
        if (invoices.isEmpty()) return invalid("recibos")
        // Any link with these words stands for a spreadsheet Google refuses, whatever its shape.
        notAccessible("shifts", "fichajes", shifts)?.let { return it }
        notAccessible("invoices", "recibos", invoices)?.let { return it }
        val shiftsSheet = spreadsheetId(shifts) ?: return invalid("fichajes")
        val invoicesSheet = spreadsheetId(invoices) ?: return invalid("recibos")
        if (shiftsSheet == invoicesSheet) {
            return error(
                HttpStatusCode.Conflict,
                "SAME_SPREADSHEET",
                "Las hojas de fichajes y de recibos deben ser distintas: las dos usan una pestaña por mes con el mismo nombre",
            )
        }
        shiftsId = shiftsSheet
        invoicesId = invoicesSheet
        configuredAt = state.now()
        configuredBy = request.caller?.employeeId
        return json(settingsJson())
    }

    /** The id inside a spreadsheet link, or the value itself when it already looks like one. */
    private fun spreadsheetId(value: String): String? {
        if (value.isEmpty() || value.length > 500) return null
        val fromLink = value.substringAfter("/spreadsheets/d/", "").takeWhile { it != '/' && it != '#' && it != '?' }
        val id = fromLink.ifEmpty { value }
        return id.takeIf { ID.matches(it) }
    }

    private fun invalid(spanish: String) =
        error(
            HttpStatusCode.BadRequest,
            "VALIDATION",
            "El enlace o identificador de la hoja de $spanish no es válido: pega el enlace de la hoja de Google o su identificador",
        )

    private fun notAccessible(
        role: String,
        spanish: String,
        value: String,
    ): DemoReply? {
        val (reason, why) =
            when {
                "no-compartida" in value -> "NOT_SHARED" to "la hoja no está compartida con la cuenta técnica de Google"
                "solo-lectura" in value -> "READ_ONLY" to "la hoja está compartida con la cuenta técnica de Google solo como lectora"
                "no-existe" in value -> "NOT_FOUND" to "la hoja no existe o la cuenta técnica de Google no la ve"
                else -> return null
            }
        return DemoReply.Error(
            HttpStatusCode.UnprocessableEntity,
            "SPREADSHEET_NOT_ACCESSIBLE",
            "No se puede escribir en la hoja de $spanish: $why. Compártela con $SERVICE_ACCOUNT como editora.",
            mapOf("spreadsheet" to role, "reason" to reason),
        )
    }

    private fun enqueueMonth(
        kind: String,
        yearText: String,
        monthText: String,
        spreadsheet: String?,
    ): DemoReply {
        val year = yearText.toIntOrNull()
        val month = monthText.toIntOrNull()
        val today = state.today()
        val current = today.year * 12 + today.month.number - 1
        val asked = if (year != null && month != null && month in 1..12) year * 12 + month - 1 else null
        if (asked == null || asked > current || asked <= current - RETENTION_MONTHS) {
            return error(
                HttpStatusCode.BadRequest,
                "INVALID_PERIOD",
                "El mes no existe, es posterior al actual o está fuera del plazo de conservación",
            )
        }
        if (spreadsheet == null) {
            val which = if (kind == "SHIFT_MONTH") "fichajes" else "recibos"
            return error(HttpStatusCode.Conflict, "SHEETS_NOT_CONFIGURED", "Falta la hoja de $which en la configuración de Google Sheets")
        }
        val reference = "$year-${month.toString().padStart(2, '0')}"
        // A pending task of the same month takes the request, as on the server.
        val task =
            tasks.firstOrNull { it.kind == kind && it.reference == reference && it.status == "PENDING" }
                ?: Task(state.newId(), kind, reference, state.now()).also { tasks += it }
        return json(buildJsonObject { put("taskId", task.id) }, HttpStatusCode.Accepted)
    }

    /** The worker's pass: every pending task is written at once. */
    private fun work() {
        val now = state.now()
        tasks.filter { it.status == "PENDING" }.forEach { task ->
            task.attempts += 1
            task.status = "DONE"
            task.lastError = null
            task.updatedAt = now
            task.completedAt = now
            when (task.kind) {
                "SHIFT_MONTH" -> {
                    task.rowCount = shiftsIn(task.reference)
                    // The demo's way of showing the warning: people still waiting for their last names.
                    task.lastNamesPending = state.employees.count { it.active && it.lastName == null }
                }
                "INVOICE_MONTH" -> {
                    val (year, month) = task.reference.split('-').map { it.toInt() }
                    task.rowCount = invoicing.confirmedIn(year, month)
                }
                else -> task.rowCount = 1
            }
            lastSuccessAt = now
        }
    }

    private fun shiftsIn(reference: String): Int =
        state.shifts.count { shift ->
            val day = shift.clockIn.toLocalDateTime(state.madrid).date
            "${day.year}-${day.month.number.toString().padStart(2, '0')}" == reference
        }

    private fun requeue(task: Task) {
        task.status = "PENDING"
        task.attempts = 0
        task.lastError = null
        task.updatedAt = state.now()
    }

    private fun task(id: String) = tasks.firstOrNull { it.id == id }

    private fun taskNotFound(id: String) = notFound("TASK_NOT_FOUND").copy(message = "No existe la tarea $id")

    private fun settingsJson() =
        buildJsonObject {
            put("available", true)
            put("serviceAccountEmail", SERVICE_ACCOUNT)
            put("shiftsSpreadsheetId", shiftsId)
            put("shiftsSpreadsheetUrl", shiftsId?.let(::url))
            put("invoicesSpreadsheetId", invoicesId)
            put("invoicesSpreadsheetUrl", invoicesId?.let(::url))
            put("configuredAt", configuredAt?.toString())
            put("configuredBy", configuredBy)
        }

    private fun statusJson() =
        buildJsonObject {
            val dayAgo = state.now() - 24.hours
            put("lastSuccessAt", lastSuccessAt?.toString())
            put("pending", tasks.count { it.status == "PENDING" || it.status == "RUNNING" })
            put("failed", tasks.count { it.status == "FAILED" })
            put(
                "recent",
                JsonArray(
                    tasks
                        .filter { it.status != "DONE" || (it.completedAt ?: it.updatedAt) >= dayAgo }
                        .sortedByDescending { it.updatedAt }
                        .take(20)
                        .map { it.toJson() },
                ),
            )
        }

    private fun Task.toJson() =
        buildJsonObject {
            put("id", id)
            put("kind", kind)
            put("reference", reference)
            put("status", status)
            put("attempts", attempts)
            put("rowCount", rowCount)
            put("lastNamesPending", lastNamesPending)
            put("lastError", lastError)
            put("createdAt", createdAt.toString())
            put("updatedAt", updatedAt.toString())
            put("completedAt", completedAt?.toString())
        }

    private fun url(id: String) = "https://docs.google.com/spreadsheets/d/$id/edit"

    companion object {
        const val SERVICE_ACCOUNT = "granatum-demo@demo.iam.gserviceaccount.com"
        private const val READ_ONLY_MESSAGE = "La hoja está compartida con la cuenta técnica de Google solo como lectora"
        private const val RETENTION_MONTHS = 48
        private val ID = Regex("^[A-Za-z0-9_-]{5,}$")
    }
}
