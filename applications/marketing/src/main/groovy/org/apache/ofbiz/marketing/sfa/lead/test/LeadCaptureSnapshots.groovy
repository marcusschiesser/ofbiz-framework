/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.marketing.sfa.lead.test

import groovy.transform.Canonical
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery

@Canonical
class RelationshipSnapshot {

    String partyIdFrom
    String relationshipTypeId

}

@Canonical
class EmploymentSnapshot {

    String companyName
    String positionTitle

}

@Canonical
class RequestItemSnapshot {

    String productId
    String description
    BigDecimal quantity
    BigDecimal maximumAmount
    String story
    String statusId

}

@Canonical
class LeadSnapshot {

    String firstName
    String lastName
    String statusId
    List<String> roles
    String primaryEmail
    List<String> dataSourceIds
    List<String> statusHistory
    List<RelationshipSnapshot> ownerRelationships
    List<EmploymentSnapshot> companyEmployment

}

@Canonical
class CompanySnapshot {

    String groupName
    String statusId
    List<String> roles
    List<String> dataSourceIds
    List<String> statusHistory
    List<RelationshipSnapshot> ownerRelationships

}

@Canonical
class RequestSnapshot {

    String custRequestName
    String description
    String statusId
    List<String> statusHistory
    List<String> partyRoles
    List<RequestItemSnapshot> items

}

@Canonical
class LeadCaptureSnapshot {

    LeadSnapshot lead
    CompanySnapshot company
    RequestSnapshot request

}

class LeadCaptureSnapshotReader {

    LeadCaptureSnapshot read(
            Delegator delegator,
            String leadPartyId,
            String companyPartyId = null,
            String requestId = null
    ) {
        return new LeadCaptureSnapshot(
                readLead(delegator, leadPartyId),
                companyPartyId ? readCompany(delegator, companyPartyId) : null,
                requestId ? readRequest(delegator, requestId) : null
        )
    }

    LeadSnapshot readLead(Delegator delegator, String partyId) {
        GenericValue party = query(delegator, 'Party', [partyId: partyId]).queryOne()
        GenericValue person = query(delegator, 'Person', [partyId: partyId]).queryOne()
        List<String> roles = query(delegator, 'PartyRole', [partyId: partyId]).queryList()*.roleTypeId
                .findAll { it == 'LEAD' }
                .sort() as List<String>
        List<String> statusHistory = query(delegator, 'PartyStatus', [partyId: partyId])
                .orderBy('statusDate')
                .queryList()*.statusId
                .findAll { it != null }
                .sort() as List<String>

        List<RelationshipSnapshot> ownerRelationships = query(delegator, 'PartyRelationship', [
                partyIdTo: partyId,
                roleTypeIdTo: 'LEAD',
                roleTypeIdFrom: 'OWNER',
                partyRelationshipTypeId: 'LEAD_OWNER'
        ]).filterByDate()
                .queryList()
                .collect { GenericValue rel ->
                    new RelationshipSnapshot(rel.partyIdFrom as String, rel.partyRelationshipTypeId as String)
                }
                .sort { RelationshipSnapshot left, RelationshipSnapshot right ->
                    left.partyIdFrom <=> right.partyIdFrom
                } as List<RelationshipSnapshot>

        List<EmploymentSnapshot> companyEmployment = query(delegator, 'PartyRelationship', [
                partyIdTo: partyId,
                roleTypeIdTo: 'LEAD',
                roleTypeIdFrom: 'ACCOUNT_LEAD',
                partyRelationshipTypeId: 'EMPLOYMENT'
        ]).filterByDate()
                .queryList()
                .collect { GenericValue rel ->
                    GenericValue company = query(delegator, 'PartyGroup', [partyId: rel.partyIdFrom]).queryOne()
                    new EmploymentSnapshot(company?.groupName as String, rel.positionTitle as String)
                }
                .sort { EmploymentSnapshot left, EmploymentSnapshot right ->
                    (left.companyName ?: '') <=> (right.companyName ?: '')
                } as List<EmploymentSnapshot>

        List<String> dataSourceIds = query(delegator, 'PartyDataSource', [partyId: partyId]).queryList()*.dataSourceId
                .findAll { it != null }
                .unique()
                .sort() as List<String>

        return new LeadSnapshot(
                person.firstName as String,
                person.lastName as String,
                party.statusId as String,
                roles,
                findPrimaryEmail(delegator, partyId),
                dataSourceIds,
                statusHistory,
                ownerRelationships,
                companyEmployment
        )
    }

    CompanySnapshot readCompany(Delegator delegator, String partyId) {
        GenericValue party = query(delegator, 'Party', [partyId: partyId]).queryOne()
        GenericValue partyGroup = query(delegator, 'PartyGroup', [partyId: partyId]).queryOne()
        List<String> roles = query(delegator, 'PartyRole', [partyId: partyId]).queryList()*.roleTypeId
                .findAll { it == 'ACCOUNT_LEAD' }
                .sort() as List<String>
        List<String> statusHistory = query(delegator, 'PartyStatus', [partyId: partyId])
                .orderBy('statusDate')
                .queryList()*.statusId
                .findAll { it != null }
                .sort() as List<String>
        List<String> dataSourceIds = query(delegator, 'PartyDataSource', [partyId: partyId]).queryList()*.dataSourceId
                .findAll { it != null }
                .unique()
                .sort() as List<String>
        List<RelationshipSnapshot> ownerRelationships = query(delegator, 'PartyRelationship', [
                partyIdTo: partyId,
                roleTypeIdTo: 'ACCOUNT_LEAD',
                roleTypeIdFrom: 'OWNER',
                partyRelationshipTypeId: 'LEAD_OWNER'
        ]).filterByDate()
                .queryList()
                .collect { GenericValue rel ->
                    new RelationshipSnapshot(rel.partyIdFrom as String, rel.partyRelationshipTypeId as String)
                }
                .sort { RelationshipSnapshot left, RelationshipSnapshot right ->
                    left.partyIdFrom <=> right.partyIdFrom
                } as List<RelationshipSnapshot>

        return new CompanySnapshot(
                partyGroup.groupName as String,
                party.statusId as String,
                roles,
                dataSourceIds,
                statusHistory,
                ownerRelationships
        )
    }

    RequestSnapshot readRequest(Delegator delegator, String requestId) {
        GenericValue request = query(delegator, 'CustRequest', [custRequestId: requestId]).queryOne()
        List<String> statusHistory = query(delegator, 'CustRequestStatus', [custRequestId: requestId])
                .orderBy('statusDate')
                .queryList()*.statusId
                .findAll { it != null }
                .sort() as List<String>
        List<String> partyRoles = query(delegator, 'CustRequestParty', [custRequestId: requestId]).queryList()*.roleTypeId
                .findAll { it != null }
                .unique()
                .sort() as List<String>
        List<RequestItemSnapshot> items = query(delegator, 'CustRequestItem', [custRequestId: requestId])
                .orderBy('custRequestItemSeqId')
                .queryList()
                .collect { GenericValue item ->
                    new RequestItemSnapshot(
                            item.productId as String,
                            item.description as String,
                            normalizeDecimal(item.quantity),
                            normalizeDecimal(item.maximumAmount),
                            item.story as String,
                            item.statusId as String
                    )
                } as List<RequestItemSnapshot>

        return new RequestSnapshot(
                request.custRequestName as String,
                request.description as String,
                request.statusId as String,
                statusHistory,
                partyRoles,
                items
        )
    }

    private static String findPrimaryEmail(Delegator delegator, String partyId) {
        GenericValue purpose = query(delegator, 'PartyContactMechPurpose', [
                partyId: partyId,
                contactMechPurposeTypeId: 'PRIMARY_EMAIL'
        ]).filterByDate()
                .orderBy('-fromDate')
                .queryFirst()
        if (!purpose) {
            return null
        }
        return query(delegator, 'ContactMech', [contactMechId: purpose.contactMechId]).queryOne()?.infoString as String
    }

    private static EntityQuery query(Delegator delegator, String entityName, Map<String, ?> fields) {
        return EntityQuery.use(delegator).from(entityName).where(fields)
    }

    private static BigDecimal normalizeDecimal(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros()
        }
        return new BigDecimal(value.toString()).stripTrailingZeros()
    }

}
