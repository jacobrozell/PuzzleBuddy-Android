package com.jacobrozell.puzzlebuddy.domain.importing

object CSVTable {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        var field = StringBuilder()
        var index = 0
        var inQuotes = false
        val chars = text

        while (index < chars.length) {
            val character = chars[index]
            when {
                inQuotes -> {
                    if (character == '"') {
                        if (index + 1 < chars.length && chars[index + 1] == '"') {
                            field.append('"')
                            index++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(character)
                    }
                }
                character == '"' -> inQuotes = true
                character == ',' || character == ';' -> {
                    row.add(field.toString())
                    field = StringBuilder()
                }
                character == '\n' || character == '\r' -> {
                    if (character == '\r' && index + 1 < chars.length && chars[index + 1] == '\n') {
                        index++
                    }
                    row.add(field.toString())
                    field = StringBuilder()
                    if (row.any { it.trim().isNotEmpty() }) {
                        rows += row.toList()
                    }
                    row = mutableListOf()
                }
                else -> field.append(character)
            }
            index++
        }
        row.add(field.toString())
        if (row.any { it.trim().isNotEmpty() }) {
            rows += row.toList()
        }
        return rows
    }

    fun parseDelimitedRows(text: String): Pair<List<String>, List<Map<String, String>>> {
        val normalized = text.trim()
            .replace("\uFEFF", "")

        var rows = parse(normalized)
        if (rows.firstOrNull()?.size == 1 && rows.first()[0].contains(';')) {
            rows = normalized.lines().map { line ->
                line.split(';').map { it.trim() }
            }
        }

        val headerRow = rows.firstOrNull() ?: return emptyList<String>() to emptyList()
        val headers = headerRow.map { it.trim() }
        val records = rows.drop(1).map { values ->
            buildMap {
                headers.forEachIndexed { offset, header ->
                    if (header.isNotEmpty()) {
                        put(header, values.getOrNull(offset)?.trim().orEmpty())
                    }
                }
            }
        }
        return headers to records
    }
}
