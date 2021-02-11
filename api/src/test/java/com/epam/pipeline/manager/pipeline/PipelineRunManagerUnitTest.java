/*
 * Copyright 2017-2021 EPAM Systems, Inc. (https://www.epam.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.pipeline.manager.pipeline;

import com.epam.pipeline.acl.folder.FolderApiService;
import com.epam.pipeline.common.MessageHelper;
import com.epam.pipeline.controller.vo.PagingRunFilterVO;
import com.epam.pipeline.controller.vo.PipelineRunFilterVO;
import com.epam.pipeline.controller.vo.TagsVO;
import com.epam.pipeline.dao.pipeline.PipelineRunDao;
import com.epam.pipeline.entity.configuration.RunConfiguration;
import com.epam.pipeline.entity.pipeline.DiskAttachRequest;
import com.epam.pipeline.entity.pipeline.DockerRegistry;
import com.epam.pipeline.entity.pipeline.Folder;
import com.epam.pipeline.entity.pipeline.Pipeline;
import com.epam.pipeline.entity.pipeline.PipelineRun;
import com.epam.pipeline.entity.pipeline.PipelineRunWithTool;
import com.epam.pipeline.entity.pipeline.RunInstance;
import com.epam.pipeline.entity.pipeline.TaskStatus;
import com.epam.pipeline.entity.pipeline.Tool;
import com.epam.pipeline.manager.cluster.NodesManager;
import com.epam.pipeline.manager.docker.DockerRegistryManager;
import com.epam.pipeline.entity.pipeline.run.parameter.PipelineRunParameter;
import com.epam.pipeline.manager.cluster.NodesManager;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.pipeline.test.creator.CommonCreatorConstants.ID;
import static com.epam.pipeline.test.creator.CommonCreatorConstants.ID_2;
import static com.epam.pipeline.test.creator.CommonCreatorConstants.ID_3;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.IMAGE1;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.IMAGE2;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.REGISTRY1;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.REGISTRY2;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.VERSION;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.getDockerRegistry;
import static com.epam.pipeline.test.creator.docker.DockerCreatorUtils.getTool;
import static com.epam.pipeline.test.creator.pipeline.PipelineCreatorUtils.getPipelineRun;
import static com.epam.pipeline.util.CustomAssertions.assertThrows;
import static com.epam.pipeline.util.CustomMatchers.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.epam.pipeline.test.creator.CommonCreatorConstants.TEST_STRING;
import static com.epam.pipeline.util.CustomAssertions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PipelineRunManagerUnitTest {
    private static final Long RUN_ID = 1L;
    private static final long NOT_EXISTING_RUN_ID = -1L;
    private static final String NODE_NAME = "node_name";
    private static final Long SIZE = 10L;

    private static final Long ID_4 = 4L;
    private static final String OWNER = "USER";

    private static final String PARAM_NAME_1 = "param-1";
    private static final String ENV_VAR_NAME = "TEST_ENV";
    private static final String ENV_VAR_VALUE = "value";


    @Mock
    private NodesManager nodesManager;

    @Mock
    private PipelineRunDao pipelineRunDao;

    @Mock
    @SuppressWarnings("PMD.UnusedPrivateField")
    private MessageHelper messageHelper;

    @Mock
    private PipelineRunCRUDService runCRUDService;

    @Mock
    private DockerRegistryManager dockerRegistryManager;

    @Mock
    private ToolManager toolManager;

    @Mock
    private FolderApiService folderApiService;


    @InjectMocks
    private PipelineRunManager pipelineRunManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTerminateNotExistingRun() {
        assertThrows(() -> pipelineRunManager.terminateRun(NOT_EXISTING_RUN_ID));
    }

    @Test
    public void testTerminateNotPausedRun() {
        when(pipelineRunDao.loadPipelineRun(eq(RUN_ID))).thenReturn(run(TaskStatus.RUNNING));

        assertThrows(() -> pipelineRunManager.terminateRun(RUN_ID));
    }

    @Test
    public void testTerminatePausedRunTerminatesInstanceNode() {
        when(pipelineRunDao.loadPipelineRun(eq(RUN_ID))).thenReturn(run(TaskStatus.PAUSED));

        pipelineRunManager.terminateRun(RUN_ID);

        verify(nodesManager).terminateRun(argThat(matches(run -> run.getId().equals(RUN_ID))));
    }

    @Test
    public void testTerminatePausedRunChangesRunStatusToStopped() {
        when(pipelineRunDao.loadPipelineRun(eq(RUN_ID))).thenReturn(run(TaskStatus.PAUSED));

        pipelineRunManager.terminateRun(RUN_ID);

        verify(runCRUDService).updateRunStatus(argThat(matches(run -> run.getStatus() == TaskStatus.STOPPED)));
    }

    @Test
    public void testAttachDiskToNotExistingRun() {
        assertAttachFails(NOT_EXISTING_RUN_ID, diskAttachRequest());
    }

    @Test
    public void testAttachDiskToInvalidRuns() {
        assertAttachFails(run(TaskStatus.STOPPED));
        assertAttachFails(run(TaskStatus.FAILURE));
        assertAttachFails(run(TaskStatus.SUCCESS));
    }

    @Test
    public void testAttachDiskWithInvalidSize() {
        assertAttachFails(diskAttachRequest(null));
        assertAttachFails(diskAttachRequest(-SIZE));
    }

    @Test
    public void testAttachDiskToValidRuns() {
        assertAttachSucceed(run(TaskStatus.RUNNING));
        assertAttachSucceed(run(TaskStatus.PAUSING));
        assertAttachSucceed(run(TaskStatus.PAUSED));
        assertAttachSucceed(run(TaskStatus.RESUMING));
    }

    @Test
    public void shouldLoadRunsWithTools() {
        final List<Long> runIds = Arrays.asList(ID, ID_2, ID_3, ID_4);
        doReturn(Arrays.asList(dockerRegistry(ID, REGISTRY1), dockerRegistry(ID_2, REGISTRY2)))
                .when(dockerRegistryManager).loadAllDockerRegistry();
        doReturn(Arrays.asList(
                pipelineRun(ID, buildDockerImage(REGISTRY1, IMAGE1 + VERSION)),
                pipelineRun(ID_2, buildDockerImage(REGISTRY2, IMAGE1)),
                pipelineRun(ID_3, buildDockerImage(REGISTRY1, IMAGE2)),
                pipelineRun(ID_4, buildDockerImage(REGISTRY1, IMAGE2))))
                .when(runCRUDService).loadRunsByIds(runIds);
        doReturn(Arrays.asList(tool(ID, REGISTRY1, IMAGE1), tool(ID_2, REGISTRY1, IMAGE2))).when(toolManager)
                .loadAllByRegistryAndImageIn(eq(ID), any());
        doReturn(Collections.singletonList(tool(ID, REGISTRY2, IMAGE1))).when(toolManager)
                .loadAllByRegistryAndImageIn(eq(ID_2), any());

        final List<PipelineRunWithTool> result = pipelineRunManager.loadRunsWithTools(runIds);
        assertThat(result).hasSize(4);
        final Map<Long, PipelineRunWithTool> resultByRunId = result.stream()
                .collect(Collectors.toMap(runWithTool -> runWithTool.getPipelineRun().getId(), Function.identity()));
        verifyRunWithTool(resultByRunId.get(ID), REGISTRY1, IMAGE1);
        verifyRunWithTool(resultByRunId.get(ID_2), REGISTRY2, IMAGE1);
        verifyRunWithTool(resultByRunId.get(ID_3), REGISTRY1, IMAGE2);
        verifyRunWithTool(resultByRunId.get(ID_4), REGISTRY1, IMAGE2);
    }

    @Test
    public void testResolveProjectFiltering() {
        final Folder project = new Folder();
        project.setId(1L);

        final Folder child = new Folder();
        project.setId(2L);
        project.getChildFolders().add(child);

        final Pipeline pipeline1 = new Pipeline();
        pipeline1.setId(2L);
        project.getPipelines().add(pipeline1);

        final Pipeline pipeline2 = new Pipeline();
        pipeline2.setId(3L);
        child.getPipelines().add(pipeline2);

        final RunConfiguration configuration1 = new RunConfiguration();
        configuration1.setId(4L);
        project.getConfigurations().add(configuration1);

        final RunConfiguration configuration2 = new RunConfiguration();
        configuration2.setId(5L);
        child.getConfigurations().add(configuration2);

        when(folderApiService.load(project.getId())).thenReturn(project);

        final PagingRunFilterVO filterVO = new PagingRunFilterVO();
        filterVO.setProjectIds(Collections.singletonList(project.getId()));

        final PipelineRunFilterVO.ProjectFilter projectFilter = pipelineRunManager.resolveProjectFiltering(filterVO);
        assertEquals(2, projectFilter.getPipelineIds().size());
        assertEquals(2, projectFilter.getConfigurationIds().size());
    }

    @Test
    public void shouldThrowExceptionOnInexistentRunTagUpdate() {
        doReturn(null).when(pipelineRunDao).loadPipelineRun(eq(NOT_EXISTING_RUN_ID));

        final Runnable result = () -> pipelineRunManager.updateTags(NOT_EXISTING_RUN_ID, new TagsVO(null));
        assertThrows(IllegalArgumentException.class, result);
    }

    @Test
    public void shouldReplaceEnvVarsWithEmptyCollections() {
        final List<PipelineRunParameter> actualParameters =
                pipelineRunManager.replaceParametersWithEnvVars(new ArrayList<>(), new HashMap<>());
        assertTrue(CollectionUtils.isEmpty(actualParameters));
    }

    @Test
    public void shouldReplaceEnvVarsWithEmptyEnvVars() {
        final List<PipelineRunParameter> parameters = new ArrayList<>();
        parameters.add(new PipelineRunParameter(PARAM_NAME_1, TEST_STRING));

        final List<PipelineRunParameter> actualParameters =
                pipelineRunManager.replaceParametersWithEnvVars(parameters, new HashMap<>());
        assertEquals(parameters, actualParameters);
    }

    @Test
    public void shouldReplaceEnvVarsWithEmptyParams() {
        final Map<String, String> envVars = new HashMap<>();
        envVars.put(ENV_VAR_NAME, ENV_VAR_VALUE);

        final List<PipelineRunParameter> actualParameters =
                pipelineRunManager.replaceParametersWithEnvVars(new ArrayList<>(), envVars);
        assertTrue(CollectionUtils.isEmpty(actualParameters));
    }

    @Test
    public void shouldNotReplaceEnvVarsIfNoNeeded() {
        final List<PipelineRunParameter> parameters = new ArrayList<>();
        parameters.add(new PipelineRunParameter(PARAM_NAME_1, TEST_STRING));
        final Map<String, String> envVars = new HashMap<>();
        envVars.put(ENV_VAR_NAME, ENV_VAR_VALUE);

        final List<PipelineRunParameter> actualParameters
                = pipelineRunManager.replaceParametersWithEnvVars(parameters, envVars);
        assertEquals(TEST_STRING, actualParameters.get(0).getValue());
    }

    @Test
    public void shouldReplaceEnvVarsWithTestEnv() {
        testEnvVarsReplacement("test/${%s}", "test/%s");
    }

    @Test
    public void shouldReplaceEnvVarsWithTestEnvAtTheEndOfTheLine() {
        testEnvVarsReplacement("test/$%s", "test/%s");
    }

    @Test
    public void shouldReplaceEnvVarsWithTestEnvAtTheMiddleOfTheLine() {
        testEnvVarsReplacement("test/$%s/", "test/%s/");
    }

    @Test
    public void shouldReplaceEnvVarsWithTestEnvWithSeveralVariables() {
        final Map<String, String> envVars = new HashMap<>();
        envVars.put(ENV_VAR_NAME, ENV_VAR_VALUE);

        final String paramValue = String.format("test/$%s/${%s}/$%s/", ENV_VAR_NAME, ENV_VAR_NAME, ENV_VAR_NAME);
        final String expectedValue = String.format("test/%s/%s/%s/", ENV_VAR_VALUE, ENV_VAR_VALUE, ENV_VAR_VALUE);
        final List<PipelineRunParameter> actualParameters = pipelineRunManager.replaceParametersWithEnvVars(
                Collections.singletonList(new PipelineRunParameter(PARAM_NAME_1, paramValue)), envVars);
        checkResolvedValue(actualParameters, paramValue, expectedValue);
    }

    private void testEnvVarsReplacement(final String paramValuePattern, final String expectedValuePattern) {
        final Map<String, String> envVars = new HashMap<>();
        envVars.put(ENV_VAR_NAME, ENV_VAR_VALUE);

        final String paramValue = String.format(paramValuePattern, ENV_VAR_NAME);
        final String expectedValue = String.format(expectedValuePattern, ENV_VAR_VALUE);
        final List<PipelineRunParameter> actualParameters = pipelineRunManager.replaceParametersWithEnvVars(
                Collections.singletonList(new PipelineRunParameter(PARAM_NAME_1, paramValue)), envVars);
        checkResolvedValue(actualParameters, paramValue, expectedValue);
    }

    private void checkResolvedValue(List<PipelineRunParameter> actualParameters, String paramValue,
                                    String expectedValue) {
        assertEquals(expectedValue, actualParameters.get(0).getResolvedValue());
        assertEquals(paramValue, actualParameters.get(0).getValue());
    }

    private void assertAttachFails(final DiskAttachRequest request) {
        assertAttachFails(RUN_ID, request);
    }

    private void assertAttachFails(final Long runId, final DiskAttachRequest request) {
        assertThrows(() -> pipelineRunManager.attachDisk(runId, request));
    }

    private void assertAttachFails(final PipelineRun run) {
        when(pipelineRunDao.loadPipelineRun(eq(RUN_ID))).thenReturn(run);
        assertAttachFails(diskAttachRequest());
    }

    private void assertAttachSucceed(final PipelineRun run) {
        when(pipelineRunDao.loadPipelineRun(eq(RUN_ID))).thenReturn(run);
        pipelineRunManager.attachDisk(RUN_ID, diskAttachRequest());
        verify(nodesManager).attachDisk(argThat(matches(r -> r.getStatus() == run.getStatus())),
                eq(diskAttachRequest()));
    }

    private PipelineRun run(final TaskStatus status) {
        final PipelineRun run = run();
        run.setStatus(status);
        return run;
    }

    private PipelineRun run() {
        final PipelineRun run = new PipelineRun();
        run.setId(RUN_ID);
        final RunInstance instance = new RunInstance();
        instance.setNodeName(NODE_NAME);
        run.setInstance(instance);
        return run;
    }

    private DiskAttachRequest diskAttachRequest() {
        return diskAttachRequest(SIZE);
    }

    private DiskAttachRequest diskAttachRequest(final Long size) {
        return new DiskAttachRequest(size);
    }

    private DockerRegistry dockerRegistry(final Long id, final String path) {
        final DockerRegistry registry = getDockerRegistry(id, OWNER);
        registry.setPath(path);
        return registry;
    }

    private PipelineRun pipelineRun(final Long id, final String dockerImage) {
        final PipelineRun pipelineRun = getPipelineRun(id, OWNER);
        pipelineRun.setDockerImage(dockerImage);
        return pipelineRun;
    }

    private Tool tool(final Long id, final String registry, final String image) {
        final Tool tool = getTool(id, OWNER);
        tool.setImage(image);
        tool.setRegistry(registry);
        return tool;
    }

    private void verifyRunWithTool(final PipelineRunWithTool result, final String expectedRegistry,
                                   final String expectedImage) {
        assertThat(result.getPipelineRun().getDockerImage())
                .contains(expectedRegistry)
                .contains(expectedImage);
        assertNotNull(result.getTool());
        assertThat(result.getTool().getImage()).contains(expectedImage);
    }

    private String buildDockerImage(final String registry, final String image) {
        return String.format("%s/%s", registry, image);
    }
}
