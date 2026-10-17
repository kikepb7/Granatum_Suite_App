package com.granatum.core.domain.auth.model

/** Why a session ended without the person ending it. Drives the message on the login screen. */
enum class SignOutReason {
    /** The backend says the person is no longer active (`EMPLOYEE_INACTIVE`). */
    INACTIVE,

    /**
     * The backend no longer accepts the session (`INVALID_REFRESH_TOKEN`, `NOT_AUTHENTICATED`).
     * Also what a password change on another device looks like, since it revokes every other
     * session of the account.
     */
    SESSION_REJECTED
}
