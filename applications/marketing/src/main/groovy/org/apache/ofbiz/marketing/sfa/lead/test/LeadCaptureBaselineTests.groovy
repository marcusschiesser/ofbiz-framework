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

import org.apache.ofbiz.service.ServiceUtil

class LeadCaptureBaselineTests extends AbstractLeadCaptureTestCase {

    LeadCaptureBaselineTests(String name) {
        super(name)
    }

    void testCreateLeadPersonOnlyBaseline() {
        String suffix = uniqueSuffix()
        String email = "lead-parity-person-${suffix}@example.com"

        Map serviceResult = createLead(
                firstName: 'Parity',
                lastName: "Person${suffix}",
                emailAddress: email
        )

        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.partyId
        assert !serviceResult.partyGroupPartyId

        LeadCaptureSnapshot captured = snapshot(serviceResult.partyId as String)
        assert captured.lead == new LeadSnapshot(
                'Parity',
                "Person${suffix}",
                'LEAD_ASSIGNED',
                ['LEAD'],
                email,
                [],
                ['LEAD_ASSIGNED', 'PARTY_ENABLED'],
                [new RelationshipSnapshot(lookupUserPartyId(), 'LEAD_OWNER')],
                []
        )
        assert captured.company == null
        assert captured.request == null
    }

    void testCreateLeadWithCompanyBaseline() {
        String suffix = uniqueSuffix()
        String email = "lead-parity-company-${suffix}@example.com"
        String companyName = "Parity Company ${suffix}"

        Map serviceResult = createLead(
                firstName: 'Parity',
                lastName: "Company${suffix}",
                emailAddress: email,
                groupName: companyName
        )

        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.partyId
        assert serviceResult.partyGroupPartyId

        LeadCaptureSnapshot captured = snapshot(serviceResult.partyId as String, serviceResult.partyGroupPartyId as String)
        assert captured.lead == new LeadSnapshot(
                'Parity',
                "Company${suffix}",
                'LEAD_ASSIGNED',
                ['LEAD'],
                email,
                [],
                ['LEAD_ASSIGNED', 'PARTY_ENABLED'],
                [new RelationshipSnapshot(lookupUserPartyId(), 'LEAD_OWNER')],
                [new EmploymentSnapshot(companyName, null)]
        )
        assert captured.company == new CompanySnapshot(
                companyName,
                'PARTY_ENABLED',
                ['ACCOUNT_LEAD'],
                [],
                ['PARTY_ENABLED'],
                [new RelationshipSnapshot(lookupUserPartyId(), 'LEAD_OWNER')]
        )
        assert captured.request == null
    }

    void testCreateLeadWithDataSourceBaseline() {
        String suffix = uniqueSuffix()
        String email = "lead-parity-source-${suffix}@example.com"

        Map serviceResult = createLead(
                firstName: 'Parity',
                lastName: "Source${suffix}",
                emailAddress: email,
                dataSourceId: 'WEB_SITE'
        )

        assert ServiceUtil.isSuccess(serviceResult)

        LeadCaptureSnapshot captured = snapshot(serviceResult.partyId as String)
        assert captured.lead.dataSourceIds == ['WEB_SITE']
    }

    void testCreateLeadWithTitleOnEmploymentRelationshipBaseline() {
        String suffix = uniqueSuffix()
        String email = "lead-parity-title-${suffix}@example.com"
        String companyName = "Parity Title Co ${suffix}"
        String title = 'Procurement Lead'

        Map serviceResult = createLead(
                firstName: 'Parity',
                lastName: "Title${suffix}",
                emailAddress: email,
                groupName: companyName,
                title: title
        )

        assert ServiceUtil.isSuccess(serviceResult)

        LeadCaptureSnapshot captured = snapshot(serviceResult.partyId as String, serviceResult.partyGroupPartyId as String)
        assert captured.lead.companyEmployment == [new EmploymentSnapshot(companyName, title)]
    }

    void testCreateRequestForLeadBaseline() {
        String suffix = uniqueSuffix()
        Map leadResult = createLead(
                firstName: 'Request',
                lastName: "Create${suffix}",
                emailAddress: "lead-request-create-${suffix}@example.com",
                groupName: "Request Create Co ${suffix}"
        )
        assert ServiceUtil.isSuccess(leadResult)

        Map requestResult = createRequest(
                leadResult.partyId as String,
                leadResult.partyGroupPartyId as String,
                "Parity Request ${suffix}",
                'Customer needs pricing for warehouse equipment.',
                [
                        [
                                description: 'Round gizmo package',
                                quantity: 2G,
                                maximumAmount: 49.00G,
                                story: 'Primary request line'
                        ],
                        [
                                description: 'Backup calibration pack',
                                quantity: 1G,
                                maximumAmount: 12.00G,
                                story: 'Secondary request line'
                        ]
                ]
        )

        assert ServiceUtil.isSuccess(requestResult)
        assert requestResult.custRequestId

        LeadCaptureSnapshot captured = snapshot(
                leadResult.partyId as String,
                leadResult.partyGroupPartyId as String,
                requestResult.custRequestId as String
        )
        assert captured.request == new RequestSnapshot(
                "Parity Request ${suffix}",
                'Customer needs pricing for warehouse equipment.',
                'CRQ_SUBMITTED',
                ['CRQ_SUBMITTED'],
                ['ACCOUNT_LEAD', 'LEAD'],
                [
                        new RequestItemSnapshot(null, 'Customer needs pricing for warehouse equipment.', null, null, null, 'CRQ_SUBMITTED'),
                        new RequestItemSnapshot(
                                null, 'Round gizmo package', 2G, 49G, 'Primary request line', 'CRQ_SUBMITTED'
                        ),
                        new RequestItemSnapshot(
                                null, 'Backup calibration pack', 1G, 12G, 'Secondary request line', 'CRQ_SUBMITTED'
                        )
                ]
        )
    }

    void testUpdateRequestForLeadBaseline() {
        String suffix = uniqueSuffix()
        Map leadResult = createLead(
                firstName: 'Request',
                lastName: "Update${suffix}",
                emailAddress: "lead-request-update-${suffix}@example.com",
                groupName: "Request Update Co ${suffix}"
        )
        assert ServiceUtil.isSuccess(leadResult)

        Map requestResult = createRequest(
                leadResult.partyId as String,
                leadResult.partyGroupPartyId as String,
                "Parity Request ${suffix}",
                'Initial request body.',
                [[
                         description: 'Initial line',
                         quantity: 2G,
                         maximumAmount: 49.00G,
                         story: 'Initial story'
                 ]]
        )
        assert ServiceUtil.isSuccess(requestResult)

        String custRequestId = requestResult.custRequestId as String
        String itemSeqId = requestResult.itemSeqIds[0] as String

        Map updateRequestResult = dispatcher.runSync('updateCustRequest', [
                custRequestId: custRequestId,
                custRequestName: "Parity Request ${suffix} Revised",
                description: 'Updated request body.',
                story: 'Updated story',
                userLogin: lookupUserLogin()
        ])
        assert ServiceUtil.isSuccess(updateRequestResult)

        Map updateItemResult = dispatcher.runSync('updateCustRequestItem', [
                custRequestId: custRequestId,
                custRequestItemSeqId: itemSeqId,
                description: 'Updated line',
                quantity: 3G,
                maximumAmount: 36.00G,
                statusId: 'CRQ_SUBMITTED',
                userLogin: lookupUserLogin()
        ])
        assert ServiceUtil.isSuccess(updateItemResult)

        LeadCaptureSnapshot captured = snapshot(
                leadResult.partyId as String,
                leadResult.partyGroupPartyId as String,
                custRequestId
        )
        assert captured.request == new RequestSnapshot(
                "Parity Request ${suffix} Revised",
                'Updated request body.',
                'CRQ_SUBMITTED',
                ['CRQ_SUBMITTED'],
                ['ACCOUNT_LEAD', 'LEAD'],
                [
                        new RequestItemSnapshot(null, 'Initial request body.', null, null, 'Updated story', 'CRQ_SUBMITTED'),
                        new RequestItemSnapshot(null, 'Updated line', 3G, 36G, 'Initial story', 'CRQ_SUBMITTED')
                ]
        )
    }

}
