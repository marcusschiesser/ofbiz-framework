package com.example.leadflow.ofbiz

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class OfbizSequenceService(
    private val dsl: DSLContext,
) {
    fun nextId(
        seqName: String,
        bankSize: Long = DEFAULT_BANK_SIZE,
    ): String =
        dsl.transactionResult { configuration ->
            val tx = DSL.using(configuration)
            val stamp = AuditStamp()
            val current =
                tx
                    .select(OfbizTables.SequenceValueItem.SEQ_ID)
                    .from(OfbizTables.SequenceValueItem.TABLE)
                    .where(OfbizTables.SequenceValueItem.SEQ_NAME.eq(seqName))
                    .forUpdate()
                    .fetchOne(OfbizTables.SequenceValueItem.SEQ_ID)
                    ?: run {
                        tx
                            .insertInto(OfbizTables.SequenceValueItem.TABLE)
                            .set(OfbizTables.SequenceValueItem.SEQ_NAME, seqName)
                            .set(OfbizTables.SequenceValueItem.SEQ_ID, BigDecimal.valueOf(START_SEQ_ID))
                            .set(OfbizTables.SequenceValueItem.LAST_UPDATED_STAMP, stamp.now)
                            .set(OfbizTables.SequenceValueItem.LAST_UPDATED_TX_STAMP, stamp.now)
                            .set(OfbizTables.SequenceValueItem.CREATED_STAMP, stamp.now)
                            .set(OfbizTables.SequenceValueItem.CREATED_TX_STAMP, stamp.now)
                            .execute()
                        BigDecimal.valueOf(START_SEQ_ID)
                    }

            tx
                .update(OfbizTables.SequenceValueItem.TABLE)
                .set(OfbizTables.SequenceValueItem.SEQ_ID, current.add(BigDecimal.valueOf(bankSize)))
                .set(OfbizTables.SequenceValueItem.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.SequenceValueItem.LAST_UPDATED_TX_STAMP, stamp.now)
                .where(OfbizTables.SequenceValueItem.SEQ_NAME.eq(seqName))
                .execute()

            current.stripTrailingZeros().toPlainString()
        }

    fun nextSubSequence(
        tableName: String,
        sequenceColumn: String,
        matchColumns: Map<String, String>,
        padding: Int = 5,
        incrementBy: Int = 1,
    ): String {
        val table = DSL.table(DSL.name(tableName))
        val sequenceField = DSL.field(DSL.name(sequenceColumn), String::class.java)

        val conditions =
            matchColumns.entries.map { (column, value) ->
                DSL.field(DSL.name(column), String::class.java).eq(value)
            }

        val highest =
            dsl
                .select(sequenceField)
                .from(table)
                .where(conditions)
                .fetch(sequenceField)
                .mapNotNull { it?.toIntOrNull() }
                .maxOrNull()
                ?: 0

        return (highest + incrementBy).toString().padStart(padding, '0')
    }

    companion object {
        private const val START_SEQ_ID = 10000L
        private const val DEFAULT_BANK_SIZE = 10L
    }
}
