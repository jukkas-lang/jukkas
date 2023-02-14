/*
 * Copyright 2023 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ormr.jukkas

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import net.ormr.jukkas.ast.Node
import net.ormr.jukkas.reporter.Message
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

infix fun Node.shouldBeStructurallyEquivalentTo(other: Node) {
    this should beStructurallyEquivalentTo(other)
}

// JukkasResult.Failure
inline infix fun JukkasResult<*>.shouldBeFailure(fn: (messages: List<Message>) -> Unit) {
    this.shouldBeFailure()
    fn(this.messages)
}

@OptIn(ExperimentalContracts::class)
fun JukkasResult<*>.shouldBeFailure() {
    contract {
        returns() implies (this@shouldBeFailure is JukkasResult.Failure)
    }
    this should beFailure()
}

// JukkasResult.Success
@Suppress("UNCHECKED_CAST")
inline infix fun <A> JukkasResult<A>.shouldBeSuccess(fn: (value: A, messages: List<Message>) -> Unit) {
    this.shouldBeSuccess()
    fn(this.value as A, this.messages)
}

@OptIn(ExperimentalContracts::class)
fun JukkasResult<*>.shouldBeSuccess() {
    contract {
        returns() implies (this@shouldBeSuccess is JukkasResult.Success<*>)
    }
    this should beSuccess()
}

fun beStructurallyEquivalentTo(other: Node) = object : Matcher<Node> {
    override fun test(value: Node): MatcherResult = MatcherResult(
        value.isStructurallyEquivalent(other),
        { "<$value> should be equivalent to <$other>" },
        { "<$value> should not be structurally equivalent to <$other>" },
    )
}

fun beFailure() = object : Matcher<JukkasResult<*>> {
    override fun test(value: JukkasResult<*>): MatcherResult = MatcherResult(
        value is JukkasResult.Failure,
        { "Expected Failure, got Success: <$value>" },
        { "Expected Success, got Failure(s): <${errorMessages(value)}>" },
    )
}

fun <A> beSuccess() = object : Matcher<JukkasResult<A>> {
    override fun test(value: JukkasResult<A>): MatcherResult = MatcherResult(
        value is JukkasResult.Success,
        { "Expected Success, got Failure(s): <${errorMessages(value)}>" },
        { "Expected Failure, got Success: <$value>" },
    )
}

private fun errorMessages(value: JukkasResult<*>): String =
    (value as JukkasResult.Failure).messages.joinToString { "\"${it.message}\"" }