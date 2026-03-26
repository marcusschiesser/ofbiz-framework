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

class LeadCaptureParityTests extends AbstractLeadCaptureTestCase {

    private LeadflowBackendHttpClient backendClient

    LeadCaptureParityTests(String name) {
        super(name)
    }

    void testCreateLeadPersonOnlyParity() {
        String suffix = uniqueSuffix()
        Map createPayload = [
                firstName: 'Parity',
                lastName: "Person${suffix}",
                email: "lead-parity-person-${suffix}@example.com"
        ]

        Map legacyResult = createLead(
                firstName: createPayload.firstName,
                lastName: createPayload.lastName,
                emailAddress: createPayload.email
        )
        assert ServiceUtil.isSuccess(legacyResult)
        LeadCaptureSnapshot legacySnapshot = snapshot(legacyResult.partyId as String)

        Map modernResult = backendClient.createOpportunity(createPayload)
        LeadCaptureSnapshot modernSnapshot = snapshot(modernResult.partyId as String)

        assert modernSnapshot == legacySnapshot
    }

    void testCreateLeadWithCompanyParity() {
        String suffix = uniqueSuffix()
        Map createPayload = [
                firstName: 'Parity',
                lastName: "Company${suffix}",
                email: "lead-parity-company-${suffix}@example.com",
                companyName: "Parity Company ${suffix}"
        ]

        Map legacyResult = createLead(
                firstName: createPayload.firstName,
                lastName: createPayload.lastName,
                emailAddress: createPayload.email,
                groupName: createPayload.companyName
        )
        assert ServiceUtil.isSuccess(legacyResult)
        LeadCaptureSnapshot legacySnapshot = snapshot(legacyResult.partyId as String, legacyResult.partyGroupPartyId as String)

        Map modernResult = backendClient.createOpportunity(createPayload)
        LeadCaptureSnapshot modernSnapshot = snapshot(modernResult.partyId as String, modernResult.companyPartyId as String)

        assert modernSnapshot == legacySnapshot
    }

    void testCreateLeadWithDataSourceParity() {
        String suffix = uniqueSuffix()
        Map createPayload = [
                firstName: 'Parity',
                lastName: "Source${suffix}",
                email: "lead-parity-source-${suffix}@example.com",
                dataSourceId: 'WEB_SITE'
        ]

        Map legacyResult = createLead(
                firstName: createPayload.firstName,
                lastName: createPayload.lastName,
                emailAddress: createPayload.email,
                dataSourceId: createPayload.dataSourceId
        )
        assert ServiceUtil.isSuccess(legacyResult)
        LeadCaptureSnapshot legacySnapshot = snapshot(legacyResult.partyId as String)

        Map modernResult = backendClient.createOpportunity(createPayload)
        LeadCaptureSnapshot modernSnapshot = snapshot(modernResult.partyId as String)

        assert modernSnapshot == legacySnapshot
    }

    void testCreateLeadWithEmploymentTitleParity() {
        String suffix = uniqueSuffix()
        Map createPayload = [
                firstName: 'Parity',
                lastName: "Title${suffix}",
                email: "lead-parity-title-${suffix}@example.com",
                companyName: "Parity Title Co ${suffix}",
                title: 'Procurement Lead'
        ]

        Map legacyResult = createLead(
                firstName: createPayload.firstName,
                lastName: createPayload.lastName,
                emailAddress: createPayload.email,
                groupName: createPayload.companyName,
                title: createPayload.title
        )
        assert ServiceUtil.isSuccess(legacyResult)
        LeadCaptureSnapshot legacySnapshot = snapshot(legacyResult.partyId as String, legacyResult.partyGroupPartyId as String)

        Map modernResult = backendClient.createOpportunity(createPayload)
        LeadCaptureSnapshot modernSnapshot = snapshot(modernResult.partyId as String, modernResult.companyPartyId as String)

        assert modernSnapshot == legacySnapshot
    }

    void testCreateFirstRequestParity() {
        String suffix = uniqueSuffix()
        Map leadPayload = [
                firstName: 'Request',
                lastName: "Create${suffix}",
                email: "lead-request-create-${suffix}@example.com",
                companyName: "Request Create Co ${suffix}"
        ]
        Map requestPayload = [
                name: "Parity Request ${suffix}",
                description: 'Customer needs pricing for warehouse equipment.',
                lines: [
                        [
                                description: 'Round gizmo package',
                                quantity: '2',
                                unitPrice: '24.50',
                                story: 'Primary request line'
                        ],
                        [
                                description: 'Backup calibration pack',
                                quantity: '1',
                                unitPrice: '12.00',
                                story: 'Secondary request line'
                        ]
                ]
        ]

        Map legacyLead = createLead(
                firstName: leadPayload.firstName,
                lastName: leadPayload.lastName,
                emailAddress: leadPayload.email,
                groupName: leadPayload.companyName
        )
        assert ServiceUtil.isSuccess(legacyLead)
        Map legacyRequest = createRequest(
                legacyLead.partyId as String,
                legacyLead.partyGroupPartyId as String,
                requestPayload.name as String,
                requestPayload.description as String,
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
        assert ServiceUtil.isSuccess(legacyRequest)
        LeadCaptureSnapshot legacySnapshot = snapshot(
                legacyLead.partyId as String,
                legacyLead.partyGroupPartyId as String,
                legacyRequest.custRequestId as String
        )

        Map modernLead = backendClient.createOpportunity(leadPayload)
        Map modernDetail = backendClient.saveRequest(modernLead.partyId as String, requestPayload)
        LeadCaptureSnapshot modernSnapshot = snapshot(
                modernLead.partyId as String,
                modernLead.companyPartyId as String,
                ((Map) modernDetail.request).requestId as String
        )

        assert modernSnapshot == legacySnapshot
    }

    void testRewriteRequestBeforeQuoteParity() {
        String suffix = uniqueSuffix()
        Map leadPayload = [
                firstName: 'Request',
                lastName: "Update${suffix}",
                email: "lead-request-update-${suffix}@example.com",
                companyName: "Request Update Co ${suffix}"
        ]
        Map initialRequestPayload = [
                name: "Parity Request ${suffix}",
                description: 'Initial request body.',
                lines: [[
                        description: 'Initial line',
                        quantity: '2',
                        unitPrice: '24.50',
                        story: 'Initial story'
                              ]]
        ]
        Map updatedRequestPayload = [
                name: "Parity Request ${suffix} Revised",
                description: 'Updated request body.',
                story: 'Updated story',
                lines: [[
                        description: 'Updated line',
                        quantity: '3',
                        unitPrice: '12.00'
                              ]]
        ]

        Map legacyLead = createLead(
                firstName: leadPayload.firstName,
                lastName: leadPayload.lastName,
                emailAddress: leadPayload.email,
                groupName: leadPayload.companyName
        )
        assert ServiceUtil.isSuccess(legacyLead)
        Map legacyRequest = createRequest(
                legacyLead.partyId as String,
                legacyLead.partyGroupPartyId as String,
                initialRequestPayload.name as String,
                initialRequestPayload.description as String,
                [[
                         description: 'Initial line',
                         quantity: 2G,
                         maximumAmount: 49.00G,
                         story: 'Initial story'
                 ]]
        )
        assert ServiceUtil.isSuccess(legacyRequest)

        String legacyRequestId = legacyRequest.custRequestId as String
        String legacyItemSeqId = legacyRequest.itemSeqIds[0] as String

        Map updateRequestResult = dispatcher.runSync('updateCustRequest', [
                custRequestId: legacyRequestId,
                custRequestName: updatedRequestPayload.name,
                description: updatedRequestPayload.description,
                story: updatedRequestPayload.story,
                userLogin: lookupUserLogin()
        ])
        assert ServiceUtil.isSuccess(updateRequestResult)

        Map updateItemResult = dispatcher.runSync('updateCustRequestItem', [
                custRequestId: legacyRequestId,
                custRequestItemSeqId: legacyItemSeqId,
                description: 'Updated line',
                quantity: toBigDecimal(3),
                maximumAmount: toBigDecimal(36.00G),
                statusId: 'CRQ_SUBMITTED',
                userLogin: lookupUserLogin()
        ])
        assert ServiceUtil.isSuccess(updateItemResult)

        LeadCaptureSnapshot legacySnapshot = snapshot(
                legacyLead.partyId as String,
                legacyLead.partyGroupPartyId as String,
                legacyRequestId
        )

        Map modernLead = backendClient.createOpportunity(leadPayload)
        backendClient.saveRequest(modernLead.partyId as String, initialRequestPayload)
        Map modernDetail = backendClient.saveRequest(modernLead.partyId as String, updatedRequestPayload)
        LeadCaptureSnapshot modernSnapshot = snapshot(
                modernLead.partyId as String,
                modernLead.companyPartyId as String,
                ((Map) modernDetail.request).requestId as String
        )

        assert modernSnapshot == legacySnapshot
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp()
        backendClient = new LeadflowBackendHttpClient(LeadflowBackendTestServer.ensureStarted())
    }

    @Override
    protected void tearDown() throws Exception {
        backendClient = null
        super.tearDown()
    }

}
