package com.proff.teamcity.gitStorage

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}