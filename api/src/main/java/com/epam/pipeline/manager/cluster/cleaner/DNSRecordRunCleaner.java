/*
 * Copyright 2017-2020 EPAM Systems, Inc. (https://www.epam.com/)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.pipeline.manager.cluster.cleaner;

import com.epam.pipeline.entity.cloud.InstanceDNSRecord;
import com.epam.pipeline.entity.pipeline.PipelineRun;
import com.epam.pipeline.manager.cloud.aws.Route53Helper;
import com.epam.pipeline.manager.preference.PreferenceManager;
import com.epam.pipeline.manager.preference.SystemPreferences;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DNSRecordRunCleaner implements RunCleaner {

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String DELIMETER = "/";

    private final PreferenceManager preferenceManager;
    private final Route53Helper route53Helper = new Route53Helper();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${edge.external.host:}")
    private final String edgeExternalHost;

    @Override
    public void cleanResources(final PipelineRun run) {
        final String serviceUrls = run.getServiceUrl();
        if (!StringUtils.isEmpty(serviceUrls)) {

            final String hostZoneId = preferenceManager.getPreference(SystemPreferences.INSTANCE_DNS_HOSTED_ZONE_ID);
            final String hostZoneUrlBase = preferenceManager.getPreference(SystemPreferences.INSTANCE_DNS_HOSTED_ZONE_BASE);
            Assert.isTrue(
                    !StringUtils.isEmpty(hostZoneId) && !StringUtils.isEmpty(hostZoneUrlBase),
                    ""
            );

            try {
                final JsonNode serviceUrlsNode = mapper.readTree(serviceUrls);
                serviceUrlsNode.iterator().forEachRemaining(jsonNode -> {
                    final Map<String, String> map = mapper.convertValue(
                            jsonNode,
                            new TypeReference<Map<String, String>>(){}
                    );

                    final String url = map.get("url");
                    if (!StringUtils.isEmpty(url) && url.contains(hostZoneUrlBase)) {
                        route53Helper.removeDNSRecord(hostZoneId, new InstanceDNSRecord(unify(url), edgeExternalHost, null));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String unify(final String url) {
        return url.trim()
                .replace(HTTP, "")
                .replace(HTTPS, "")
                .split(DELIMETER)[0];
    }

}
