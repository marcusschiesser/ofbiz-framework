#!/usr/bin/env bash
#####################################################################
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#####################################################################

set -euo pipefail

rendered_entityengine="$(mktemp)"

sed \
  --expression="s/@HOST@/${OFBIZ_POSTGRES_HOST}/;" \
  --expression="s/@OFBIZ_DB@/${OFBIZ_POSTGRES_OFBIZ_DB}/;" \
  --expression="s/@OFBIZ_USERNAME@/${OFBIZ_POSTGRES_OFBIZ_USER}/;" \
  --expression="s/@OFBIZ_PASSWORD@/${OFBIZ_POSTGRES_OFBIZ_PASSWORD}/;" \
  --expression="s/@OLAP_DB@/${OFBIZ_POSTGRES_OLAP_DB}/;" \
  --expression="s/@OLAP_USERNAME@/${OFBIZ_POSTGRES_OLAP_USER}/;" \
  --expression="s/@OLAP_PASSWORD@/${OFBIZ_POSTGRES_OLAP_PASSWORD}/;" \
  --expression="s/@TENANT_DB@/${OFBIZ_POSTGRES_TENANT_DB}/;" \
  --expression="s/@TENANT_USERNAME@/${OFBIZ_POSTGRES_TENANT_USER}/;" \
  --expression="s/@TENANT_PASSWORD@/${OFBIZ_POSTGRES_TENANT_PASSWORD}/;" \
  /ofbiz/templates/postgres-entityengine.xml > "${rendered_entityengine}"

cp "${rendered_entityengine}" /ofbiz/framework/entity/config/entityengine.xml
cp "${rendered_entityengine}" /ofbiz/config/entityengine.xml

rm -f "${rendered_entityengine}"
