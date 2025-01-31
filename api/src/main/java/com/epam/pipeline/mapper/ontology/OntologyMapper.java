/*
 * Copyright 2017-2020 EPAM Systems, Inc. (https://www.epam.com/)
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

package com.epam.pipeline.mapper.ontology;

import com.epam.pipeline.dto.ontology.Ontology;
import com.epam.pipeline.entity.ontology.OntologyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OntologyMapper {

    OntologyEntity toEntity(Ontology ontology);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Ontology toDto(OntologyEntity entity);
}
