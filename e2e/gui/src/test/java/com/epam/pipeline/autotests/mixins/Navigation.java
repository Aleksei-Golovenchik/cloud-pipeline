/*
 * Copyright 2017-2019 EPAM Systems, Inc. (https://www.epam.com/)
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
package com.epam.pipeline.autotests.mixins;

import com.epam.pipeline.autotests.ao.ClusterMenuAO;
import com.epam.pipeline.autotests.ao.NavigationHomeAO;
import com.epam.pipeline.autotests.ao.NavigationMenuAO;
import com.epam.pipeline.autotests.ao.PipelinesLibraryAO;
import com.epam.pipeline.autotests.ao.RunsMenuAO;
import com.epam.pipeline.autotests.ao.ToolsPage;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selenide.$;
import static com.epam.pipeline.autotests.utils.Conditions.selectedMenuItem;
import static com.epam.pipeline.autotests.utils.Utils.click;
import static com.epam.pipeline.autotests.utils.Utils.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

public interface Navigation {

    default NavigationMenuAO navigationMenu() {
        final By pipelinesPageSelector = byId("navigation-button-pipelines");
        click(pipelinesPageSelector);
        $(pipelinesPageSelector).shouldBe(selectedMenuItem);
        return new NavigationMenuAO();
    }

    default NavigationHomeAO home() {
        final By homePageSelector = byId("navigation-button-home");
        click(homePageSelector);
        sleep(1, SECONDS);
        $(homePageSelector).shouldBe(selectedMenuItem);
        return new NavigationHomeAO();
    }

    default PipelinesLibraryAO library() {
        return navigationMenu().library();
    }

    default ToolsPage tools() {
        final By toolsPageSelector = byId("navigation-button-tools");
        click(toolsPageSelector);
        $(toolsPageSelector).shouldBe(selectedMenuItem);
        return new ToolsPage();
    }

    default ClusterMenuAO clusterMenu() {
        final By clusterPageSelector = byId("navigation-button-cluster");
        click(clusterPageSelector);
        $(clusterPageSelector).shouldBe(selectedMenuItem);
        return new ClusterMenuAO();
    }

    default RunsMenuAO runsMenu() {
        final By runsPageSelector = byId("navigation-button-runs");
        click(runsPageSelector);
        $(runsPageSelector).shouldBe(selectedMenuItem);
        return new RunsMenuAO();
    }
}
