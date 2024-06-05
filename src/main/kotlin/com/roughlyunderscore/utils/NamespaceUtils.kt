// Copyright 2024 RoughlyUnderscore
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.roughlyunderscore.utils

// Namespace methods taken (and adapted) from org.bukkit.NamespacedKey because of course it's private why wouldn't it be
internal fun isValidNamespaceChar(c: Char): Boolean {
  return (c in 'a'..'z') || (c in '0'..'9') || c == '.' || c == '_' || c == '-'
}

internal fun isValidNamespace(namespace: String): Boolean {
  val len = namespace.length
  if (len == 0) return false

  for (i in 0..< len) {
    if (!isValidNamespaceChar(namespace[i].lowercaseChar())) return false
  }

  return true
}