package com.example.leadflow.ofbiz

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class OfbizSequenceService(
    private val jdbcClient: JdbcClient,
) {
    @Transactional
    fun nextId(seqName: String, bankSize: Long = 10): String {
        val current =
            jdbcClient.sql("select seq_id from sequence_value_item where seq_name = :seqName")
                .param("seqName", seqName)
                .query(BigDecimal::class.java)
                .optional()

        val currentValue =
            current.orElseGet {
                jdbcClient.sql(
                    """
                    insert into sequence_value_item (seq_name, seq_id, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp)
                    values (:seqName, :seqId, current_timestamp, current_timestamp, current_timestamp, current_timestamp)
                    """.trimIndent(),
                ).param("seqName", seqName)
                    .param("seqId", BigDecimal.valueOf(10000))
                    .update()
                BigDecimal.valueOf(10000)
            }

        jdbcClient.sql(
            """
            update sequence_value_item
            set seq_id = :nextValue,
                last_updated_stamp = current_timestamp,
                last_updated_tx_stamp = current_timestamp
            where seq_name = :seqName
            """.trimIndent(),
        ).param("seqName", seqName)
            .param("nextValue", currentValue.add(BigDecimal.valueOf(bankSize)))
            .update()

        return currentValue.stripTrailingZeros().toPlainString()
    }

    fun nextSubSequence(
        tableName: String,
        sequenceColumn: String,
        matchColumns: Map<String, String>,
        padding: Int = 5,
    ): String {
        val whereClause = matchColumns.keys.joinToString(" and ") { "$it = :$it" }
        val sql = "select $sequenceColumn from $tableName where $whereClause"
        val existing =
            jdbcClient.sql(sql)
                .apply {
                    matchColumns.forEach { (name, value) -> param(name, value) }
                }.query(String::class.java)
                .list()
                .mapNotNull { it?.toIntOrNull() }
                .maxOrNull()
                ?: 0

        return (existing + 1).toString().padStart(padding, '0')
    }
}
