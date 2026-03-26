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

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.security.Security
import org.apache.ofbiz.security.SecurityFactory
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

abstract class AbstractLeadCaptureTestCase extends OFBizTestCase {
    protected final LeadCaptureSnapshotReader snapshotReader = new LeadCaptureSnapshotReader()

    protected AbstractLeadCaptureTestCase(String name) {
        super(name)
    }

    protected LeadCaptureSnapshot snapshot(
            String leadPartyId,
            String companyPartyId = null,
            String requestId = null
    ) {
        return snapshotReader.read(delegator, leadPartyId, companyPartyId, requestId)
    }

    protected Map createLead(Map overrides) {
        Map context = [
                firstName: 'Lead',
                lastName: "Parity${uniqueSuffix()}",
                emailAddress: "lead-parity-${uniqueSuffix()}@example.com",
                userLogin: lookupUserLogin()
        ]
        context.putAll(overrides)
        return dispatcher.runSync('createLead', context)
    }

    protected Map createRequest(
            String leadPartyId,
            String companyPartyId,
            String name,
            String description,
            List<Map> items
    ) {
        Map createRequestResult = dispatcher.runSync('createCustRequest', [
                custRequestTypeId: 'RF_QUOTE',
                fromPartyId: lookupUserPartyId(),
                statusId: 'CRQ_SUBMITTED',
                custRequestName: name,
                description: description,
                userLogin: lookupUserLogin()
        ])
        assert ServiceUtil.isSuccess(createRequestResult)

        String custRequestId = createRequestResult.custRequestId as String

        Map leadPartyResult = dispatcher.runSync('createCustRequestParty', [
                custRequestId: custRequestId,
                partyId: leadPartyId,
                roleTypeId: 'LEAD',
                userLogin: lookupUserLogin()
        ])
        assert ServiceUtil.isSuccess(leadPartyResult)

        if (companyPartyId) {
            Map companyPartyResult = dispatcher.runSync('createCustRequestParty', [
                    custRequestId: custRequestId,
                    partyId: companyPartyId,
                    roleTypeId: 'ACCOUNT_LEAD',
                    userLogin: lookupUserLogin()
            ])
            assert ServiceUtil.isSuccess(companyPartyResult)
        }

        List<String> itemSeqIds = []
        items.each { Map item ->
            Map createItemResult = dispatcher.runSync('createCustRequestItem', [
                    custRequestId: custRequestId,
                    description: item.description,
                    story: item.story,
                    quantity: item.quantity,
                    maximumAmount: item.maximumAmount,
                    statusId: 'CRQ_SUBMITTED',
                    userLogin: lookupUserLogin()
            ])
            assert ServiceUtil.isSuccess(createItemResult)
            itemSeqIds.add(createItemResult.custRequestItemSeqId as String)
        }

        return [custRequestId: custRequestId, itemSeqIds: itemSeqIds]
    }

    protected GenericValue lookupUserLogin() {
        GenericValue systemLogin = getUserLogin('system')
        assert systemLogin != null
        Security security = SecurityFactory.getInstance(delegator)
        security.clearUserData(systemLogin)
        delegator.clearCacheLineByCondition('UserLoginSecurityGroup',
                EntityCondition.makeCondition('userLoginId', EntityOperator.EQUALS, systemLogin.userLoginId))
        delegator.clearCacheLineByCondition('SecurityGroupPermission',
                EntityCondition.makeCondition('groupId', EntityOperator.EQUALS, 'SUPER'))
        GenericValue login = delegator.makeValue('UserLogin', systemLogin.getAllFields())
        login.partyId = 'LeadParitySystem'
        login.enabled = 'Y'
        assert login != null
        return login
    }

    protected String lookupUserPartyId() {
        return lookupUserLogin().partyId as String
    }

    protected static String uniqueSuffix() {
        return Long.toString(System.nanoTime()).takeRight(10)
    }
}
