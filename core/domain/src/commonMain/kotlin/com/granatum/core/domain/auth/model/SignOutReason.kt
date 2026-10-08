package com.granatum.core.domain.auth.model

/** Why a session ended without the person ending it. Drives the message on the login screen. */
enum class SignOutReason {
    /** The backend says the person is no longer active (`EMPLEADO_INACTIVO`). */
    INACTIVE,

    /**
     * The backend no longer accepts the session (`TOKEN_RENOVACION_INVALIDO`, `NO_AUTENTICADO`).
     * Also what a password change on another device looks like, since it revokes every other
     * session of the account.
     */
    SESSION_REJECTED
}
