package com.jacobrozell.puzzlebuddy.data.repository

interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
