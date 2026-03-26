package com.example.leadflow.lead

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.ofbiz.AuditStamp
import com.example.leadflow.ofbiz.OfbizSequenceService
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeadCommandService(
    private val jdbcClient: JdbcClient,
    private val sequenceService: OfbizSequenceService,
    private val properties: OfbizProperties,
) {
    @Transactional
    fun createLead(request: OpportunityCreateRequest): LeadRecord {
        val stamp = AuditStamp()
        ensureRole(properties.leadOwnerPartyId, "OWNER")

        val leadPartyId = sequenceService.nextId("Party")
        insertParty(
            partyId = leadPartyId,
            partyTypeId = "PERSON",
            statusId = "LEAD_ASSIGNED",
            dataSourceId = request.dataSourceId,
            stamp = stamp,
        )
        insertPartyStatus(leadPartyId, "PARTY_ENABLED", stamp)
        insertPartyStatus(leadPartyId, "LEAD_ASSIGNED", stamp)
        jdbcClient.sql(
            """
            insert into person (party_id, first_name, last_name, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp)
            values (:partyId, :firstName, :lastName, :stamp, :stamp, :stamp, :stamp)
            """.trimIndent(),
        ).param("partyId", leadPartyId)
            .param("firstName", request.firstName.trim())
            .param("lastName", request.lastName.trim())
            .param("stamp", stamp.now)
            .update()
        ensureRole(leadPartyId, "LEAD")
        insertPrimaryEmail(leadPartyId, request.email.trim(), stamp)
        insertOwnerRelationship(
            partyIdFrom = properties.leadOwnerPartyId,
            roleTypeIdFrom = "OWNER",
            partyIdTo = leadPartyId,
            roleTypeIdTo = "LEAD",
            relationshipTypeId = "LEAD_OWNER",
            positionTitle = null,
            stamp = stamp,
        )
        insertPartyDataSource(leadPartyId, request.dataSourceId, stamp)

        var companyPartyId: String? = null
        var companyName: String? = null
        if (!request.companyName.isNullOrBlank()) {
            companyPartyId = sequenceService.nextId("Party")
            companyName = request.companyName.trim()
            insertParty(
                partyId = companyPartyId,
                partyTypeId = "PARTY_GROUP",
                statusId = "PARTY_ENABLED",
                dataSourceId = request.dataSourceId,
                stamp = stamp,
            )
            insertPartyStatus(companyPartyId, "PARTY_ENABLED", stamp)
            jdbcClient.sql(
                """
                insert into party_group (party_id, group_name, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp)
                values (:partyId, :groupName, :stamp, :stamp, :stamp, :stamp)
                """.trimIndent(),
            ).param("partyId", companyPartyId)
                .param("groupName", companyName)
                .param("stamp", stamp.now)
                .update()
            ensureRole(companyPartyId, "ACCOUNT_LEAD")
            insertOwnerRelationship(
                partyIdFrom = companyPartyId,
                roleTypeIdFrom = "ACCOUNT_LEAD",
                partyIdTo = leadPartyId,
                roleTypeIdTo = "LEAD",
                relationshipTypeId = "EMPLOYMENT",
                positionTitle = request.title?.trim()?.ifBlank { null },
                stamp = stamp,
            )
            insertOwnerRelationship(
                partyIdFrom = properties.leadOwnerPartyId,
                roleTypeIdFrom = "OWNER",
                partyIdTo = companyPartyId,
                roleTypeIdTo = "ACCOUNT_LEAD",
                relationshipTypeId = "LEAD_OWNER",
                positionTitle = null,
                stamp = stamp,
            )
        }

        return LeadRecord(
            partyId = leadPartyId,
            firstName = request.firstName.trim(),
            lastName = request.lastName.trim(),
            email = request.email.trim(),
            companyPartyId = companyPartyId,
            companyName = companyName,
        )
    }

    private fun insertParty(
        partyId: String,
        partyTypeId: String,
        statusId: String,
        dataSourceId: String?,
        stamp: AuditStamp,
    ) {
        jdbcClient.sql(
            """
            insert into party (
              party_id, party_type_id, status_id, created_date, created_by_user_login,
              last_modified_date, last_modified_by_user_login, data_source_id,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyId, :partyTypeId, :statusId, :stamp, :userLoginId,
              :stamp, :userLoginId, :dataSourceId,
              :stamp, :stamp, :stamp, :stamp
            )
            """.trimIndent(),
        ).param("partyId", partyId)
            .param("partyTypeId", partyTypeId)
            .param("statusId", statusId)
            .param("stamp", stamp.now)
            .param("userLoginId", properties.createdByUserLoginId)
            .param("dataSourceId", dataSourceId)
            .update()
    }

    private fun insertPartyStatus(
        partyId: String,
        statusId: String,
        stamp: AuditStamp,
    ) {
        jdbcClient.sql(
            """
            insert into party_status (
              status_id, party_id, status_date, change_by_user_login_id,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :statusId, :partyId, :stamp, :userLoginId,
              :stamp, :stamp, :stamp, :stamp
            )
            """.trimIndent(),
        ).param("statusId", statusId)
            .param("partyId", partyId)
            .param("stamp", stamp.now)
            .param("userLoginId", properties.createdByUserLoginId)
            .update()
    }

    private fun ensureRole(
        partyId: String,
        roleTypeId: String,
    ) {
        val exists =
            jdbcClient.sql("select count(*) from party_role where party_id = :partyId and role_type_id = :roleTypeId")
                .param("partyId", partyId)
                .param("roleTypeId", roleTypeId)
                .query(Long::class.java)
                .single() > 0L
        if (!exists) {
            jdbcClient.sql(
                """
                insert into party_role (party_id, role_type_id, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp)
                values (:partyId, :roleTypeId, current_timestamp, current_timestamp, current_timestamp, current_timestamp)
                """.trimIndent(),
            ).param("partyId", partyId)
                .param("roleTypeId", roleTypeId)
                .update()
        }
    }

    private fun insertOwnerRelationship(
        partyIdFrom: String,
        roleTypeIdFrom: String,
        partyIdTo: String,
        roleTypeIdTo: String,
        relationshipTypeId: String,
        positionTitle: String?,
        stamp: AuditStamp,
    ) {
        jdbcClient.sql(
            """
            insert into party_relationship (
              party_id_from, party_id_to, role_type_id_from, role_type_id_to, from_date,
              party_relationship_type_id, position_title,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyIdFrom, :partyIdTo, :roleTypeIdFrom, :roleTypeIdTo, :fromDate,
              :relationshipTypeId, :positionTitle,
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("partyIdFrom", partyIdFrom)
            .param("partyIdTo", partyIdTo)
            .param("roleTypeIdFrom", roleTypeIdFrom)
            .param("roleTypeIdTo", roleTypeIdTo)
            .param("fromDate", stamp.now)
            .param("relationshipTypeId", relationshipTypeId)
            .param("positionTitle", positionTitle)
            .update()
    }

    private fun insertPrimaryEmail(
        partyId: String,
        email: String,
        stamp: AuditStamp,
    ) {
        val contactMechId = sequenceService.nextId("ContactMech")
        jdbcClient.sql(
            """
            insert into contact_mech (contact_mech_id, contact_mech_type_id, info_string, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp)
            values (:contactMechId, 'EMAIL_ADDRESS', :email, :stamp, :stamp, :stamp, :stamp)
            """.trimIndent(),
        ).param("contactMechId", contactMechId)
            .param("email", email)
            .param("stamp", stamp.now)
            .update()
        jdbcClient.sql(
            """
            insert into party_contact_mech (
              party_id, contact_mech_id, from_date, verified,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyId, :contactMechId, :fromDate, 'Y',
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("partyId", partyId)
            .param("contactMechId", contactMechId)
            .param("fromDate", stamp.now)
            .update()
        jdbcClient.sql(
            """
            insert into party_contact_mech_purpose (
              party_id, contact_mech_id, contact_mech_purpose_type_id, from_date,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyId, :contactMechId, 'PRIMARY_EMAIL', :fromDate,
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("partyId", partyId)
            .param("contactMechId", contactMechId)
            .param("fromDate", stamp.now)
            .update()
    }

    private fun insertPartyDataSource(
        partyId: String,
        dataSourceId: String?,
        stamp: AuditStamp,
    ) {
        if (dataSourceId.isNullOrBlank()) {
            return
        }
        jdbcClient.sql(
            """
            insert into party_data_source (
              party_id, data_source_id, from_date, is_create,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyId, :dataSourceId, :fromDate, 'Y',
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("partyId", partyId)
            .param("dataSourceId", dataSourceId)
            .param("fromDate", stamp.now)
            .update()
    }
}
