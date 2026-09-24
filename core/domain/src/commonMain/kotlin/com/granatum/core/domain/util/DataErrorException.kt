package com.granatum.core.domain.util

class DataErrorException(
    val error: DataError
): Exception()