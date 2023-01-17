package com.matyrobbrt.codecutils.test

import com.mojang.serialization.DataResult
import groovy.transform.CompileStatic
import org.assertj.core.api.Assertions
import org.assertj.core.api.OptionalAssert

@CompileStatic
class CustomAssertions {
    static <T> OptionalAssert<T> assertThat(DataResult<T> result) {
        Assertions.assertThat(result.result())
    }
}
