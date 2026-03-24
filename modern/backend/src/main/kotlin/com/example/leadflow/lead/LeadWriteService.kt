package com.example.leadflow.lead

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.ofbiz.AuditStamp
import com.example.leadflow.ofbiz.OfbizSequenceService
import com.example.leadflow.ofbiz.OfbizTables
import com.example.leadflow.shared.NotFoundException
import com.example.leadflow.workflow.WorkflowReadRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeadWriteService(
    private val dsl: DSLContext,
    private val sequenceService: OfbizSequenceService,
    private val properties: OfbizProperties,
    private val workflowReadRepository: WorkflowReadRepository,
) {

    @Transactional
    fun createLead(request: LeadCreateRequest): LeadDetail {
        val stamp = AuditStamp()
        ensureRole(properties.leadOwnerPartyId, "OWNER", stamp)

        val partyId = sequenceService.nextId("Party")

        dsl.insertInto(OfbizTables.Party.TABLE)
            .set(OfbizTables.Party.PARTY_ID, partyId)
            .set(OfbizTables.Party.PARTY_TYPE_ID, "PERSON")
            .set(OfbizTables.Party.STATUS_ID, "LEAD_ASSIGNED")
            .set(OfbizTables.Party.CREATED_DATE, stamp.now)
            .set(OfbizTables.Party.CREATED_BY_USER_LOGIN, properties.createdByUserLoginId)
            .set(OfbizTables.Party.LAST_MODIFIED_DATE, stamp.now)
            .set(OfbizTables.Party.LAST_MODIFIED_BY_USER_LOGIN, properties.createdByUserLoginId)
            .set(OfbizTables.Party.DATA_SOURCE_ID, request.dataSourceId)
            .set(OfbizTables.Party.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.Party.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.Party.CREATED_STAMP, stamp.now)
            .set(OfbizTables.Party.CREATED_TX_STAMP, stamp.now)
            .execute()

        dsl.insertInto(OfbizTables.Person.TABLE)
            .set(OfbizTables.Person.PARTY_ID, partyId)
            .set(OfbizTables.Person.FIRST_NAME, request.firstName.trim())
            .set(OfbizTables.Person.LAST_NAME, request.lastName.trim())
            .set(OfbizTables.Person.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.Person.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.Person.CREATED_STAMP, stamp.now)
            .set(OfbizTables.Person.CREATED_TX_STAMP, stamp.now)
            .execute()

        ensureRole(partyId, "LEAD", stamp)
        insertOwnerRelationship(
            partyIdFrom = properties.leadOwnerPartyId,
            roleTypeIdFrom = "OWNER",
            partyIdTo = partyId,
            roleTypeIdTo = "LEAD",
            relationshipTypeId = "LEAD_OWNER",
            positionTitle = null,
            stamp = stamp,
        )

        insertPrimaryEmail(partyId = partyId, email = request.email.trim(), stamp = stamp)
        insertPartyDataSource(partyId = partyId, dataSourceId = request.dataSourceId, stamp = stamp)

        if (!request.companyName.isNullOrBlank()) {
            val companyPartyId = sequenceService.nextId("Party")
            dsl.insertInto(OfbizTables.Party.TABLE)
                .set(OfbizTables.Party.PARTY_ID, companyPartyId)
                .set(OfbizTables.Party.PARTY_TYPE_ID, "PARTY_GROUP")
                .set(OfbizTables.Party.STATUS_ID, "LEAD_ASSIGNED")
                .set(OfbizTables.Party.CREATED_DATE, stamp.now)
                .set(OfbizTables.Party.CREATED_BY_USER_LOGIN, properties.createdByUserLoginId)
                .set(OfbizTables.Party.LAST_MODIFIED_DATE, stamp.now)
                .set(OfbizTables.Party.LAST_MODIFIED_BY_USER_LOGIN, properties.createdByUserLoginId)
                .set(OfbizTables.Party.DATA_SOURCE_ID, request.dataSourceId)
                .set(OfbizTables.Party.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.Party.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.Party.CREATED_STAMP, stamp.now)
                .set(OfbizTables.Party.CREATED_TX_STAMP, stamp.now)
                .execute()

            dsl.insertInto(OfbizTables.PartyGroup.TABLE)
                .set(OfbizTables.PartyGroup.PARTY_ID, companyPartyId)
                .set(OfbizTables.PartyGroup.GROUP_NAME, request.companyName.trim())
                .set(OfbizTables.PartyGroup.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.PartyGroup.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.PartyGroup.CREATED_STAMP, stamp.now)
                .set(OfbizTables.PartyGroup.CREATED_TX_STAMP, stamp.now)
                .execute()

            ensureRole(companyPartyId, "ACCOUNT_LEAD", stamp)
            insertPartyDataSource(companyPartyId, request.dataSourceId, stamp)
            insertOwnerRelationship(
                partyIdFrom = companyPartyId,
                roleTypeIdFrom = "ACCOUNT_LEAD",
                partyIdTo = partyId,
                roleTypeIdTo = "LEAD",
                relationshipTypeId = "EMPLOYMENT",
                positionTitle = request.title,
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

        return workflowReadRepository.getLead(partyId)
    }

    fun requireLead(partyId: String): LeadDetail = workflowReadRepository.getLead(partyId)

    private fun ensureRole(partyId: String, roleTypeId: String, stamp: AuditStamp) {
        val exists = dsl.fetchExists(
            OfbizTables.PartyRole.TABLE,
            OfbizTables.PartyRole.PARTY_ID.eq(partyId)
                .and(OfbizTables.PartyRole.ROLE_TYPE_ID.eq(roleTypeId))
        )
        if (!exists) {
            dsl.insertInto(OfbizTables.PartyRole.TABLE)
                .set(OfbizTables.PartyRole.PARTY_ID, partyId)
                .set(OfbizTables.PartyRole.ROLE_TYPE_ID, roleTypeId)
                .set(OfbizTables.PartyRole.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.PartyRole.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.PartyRole.CREATED_STAMP, stamp.now)
                .set(OfbizTables.PartyRole.CREATED_TX_STAMP, stamp.now)
                .execute()
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
        dsl.insertInto(OfbizTables.PartyRelationship.TABLE)
            .set(OfbizTables.PartyRelationship.PARTY_ID_FROM, partyIdFrom)
            .set(OfbizTables.PartyRelationship.ROLE_TYPE_ID_FROM, roleTypeIdFrom)
            .set(OfbizTables.PartyRelationship.PARTY_ID_TO, partyIdTo)
            .set(OfbizTables.PartyRelationship.ROLE_TYPE_ID_TO, roleTypeIdTo)
            .set(OfbizTables.PartyRelationship.FROM_DATE, stamp.now)
            .set(OfbizTables.PartyRelationship.PARTY_RELATIONSHIP_TYPE_ID, relationshipTypeId)
            .set(OfbizTables.PartyRelationship.POSITION_TITLE, positionTitle)
            .set(OfbizTables.PartyRelationship.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.PartyRelationship.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.PartyRelationship.CREATED_STAMP, stamp.now)
            .set(OfbizTables.PartyRelationship.CREATED_TX_STAMP, stamp.now)
            .execute()
    }

    private fun insertPrimaryEmail(partyId: String, email: String, stamp: AuditStamp) {
        val contactMechId = sequenceService.nextId("ContactMech")

        dsl.insertInto(OfbizTables.ContactMech.TABLE)
            .set(OfbizTables.ContactMech.CONTACT_MECH_ID, contactMechId)
            .set(OfbizTables.ContactMech.CONTACT_MECH_TYPE_ID, "EMAIL_ADDRESS")
            .set(OfbizTables.ContactMech.INFO_STRING, email)
            .set(OfbizTables.ContactMech.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.ContactMech.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.ContactMech.CREATED_STAMP, stamp.now)
            .set(OfbizTables.ContactMech.CREATED_TX_STAMP, stamp.now)
            .execute()

        dsl.insertInto(OfbizTables.PartyContactMech.TABLE)
            .set(OfbizTables.PartyContactMech.PARTY_ID, partyId)
            .set(OfbizTables.PartyContactMech.CONTACT_MECH_ID, contactMechId)
            .set(OfbizTables.PartyContactMech.FROM_DATE, stamp.now)
            .set(OfbizTables.PartyContactMech.VERIFIED, "Y")
            .set(OfbizTables.PartyContactMech.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.PartyContactMech.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.PartyContactMech.CREATED_STAMP, stamp.now)
            .set(OfbizTables.PartyContactMech.CREATED_TX_STAMP, stamp.now)
            .execute()

        dsl.insertInto(OfbizTables.PartyContactMechPurpose.TABLE)
            .set(OfbizTables.PartyContactMechPurpose.PARTY_ID, partyId)
            .set(OfbizTables.PartyContactMechPurpose.CONTACT_MECH_ID, contactMechId)
            .set(OfbizTables.PartyContactMechPurpose.CONTACT_MECH_PURPOSE_TYPE_ID, "PRIMARY_EMAIL")
            .set(OfbizTables.PartyContactMechPurpose.FROM_DATE, stamp.now)
            .set(OfbizTables.PartyContactMechPurpose.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.PartyContactMechPurpose.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.PartyContactMechPurpose.CREATED_STAMP, stamp.now)
            .set(OfbizTables.PartyContactMechPurpose.CREATED_TX_STAMP, stamp.now)
            .execute()
    }

    private fun insertPartyDataSource(partyId: String, dataSourceId: String?, stamp: AuditStamp) {
        if (dataSourceId.isNullOrBlank()) {
            return
        }

        dsl.insertInto(OfbizTables.PartyDataSource.TABLE)
            .set(OfbizTables.PartyDataSource.PARTY_ID, partyId)
            .set(OfbizTables.PartyDataSource.DATA_SOURCE_ID, dataSourceId)
            .set(OfbizTables.PartyDataSource.FROM_DATE, stamp.now)
            .set(OfbizTables.PartyDataSource.IS_CREATE, "Y")
            .set(OfbizTables.PartyDataSource.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.PartyDataSource.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.PartyDataSource.CREATED_STAMP, stamp.now)
            .set(OfbizTables.PartyDataSource.CREATED_TX_STAMP, stamp.now)
            .execute()
    }
}
