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

interface StructurallyComparable {
    /**
     * Returns `true` if [other] is structurally equivalent to `this` instance.
     *
     * Two instances being structurally equivalent does *not* guarantee that they will also be [equal][Any.equals].
     *
     * Structural equivalence checks are intended for use via unit tests, and should probably not be used outside
     * unit tests.
     */
    fun isStructurallyEquivalent(other: StructurallyComparable): Boolean
}