package org.projectcontrol.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.projectcontrol.server.dto.ProjetDto;
import org.projectcontrol.server.vo.Projet;
import org.projectcontrol.server.vo.ProjetPom;

@Mapper(componentModel = "spring")
public interface ProjetMapper {

    @Mapping(target="parent",source = "projetPom.parent")
    @Mapping(target="artifact",source = "projetPom.artifact")
    @Mapping(target="properties",source = "projetPom.properties")
    @Mapping(target="dependencies",source = "projetPom.dependencies")
    @Mapping(target="projetEnfants",source = "projetPom.projetPomEnfants")
    @Mapping(target="modules",source = "modules")
    @Mapping(target="infoNode",source = "projetNode")
    @Mapping(target="infoGit",source = "projetGit")
    @Mapping(target="id",source = "id")
    void projetToProjetDto(Projet projet, @MappingTarget ProjetDto projetDto);

    @Mapping(target="infoNode",source = "projetNode")
    ProjetDto projetPomToProjetDto(ProjetPom projet);

}
