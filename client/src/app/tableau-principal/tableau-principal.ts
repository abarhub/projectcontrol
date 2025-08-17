import {Component, signal} from '@angular/core';
import {ProjetService} from '../service/projet.service';
import {RouterLink} from '@angular/router';
import {Projet} from '../entity/projet';
import {GroupeProjet} from '../entity/groupe-projet';
import {DatePipe, KeyValuePipe} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {LigneTableauPrincipal} from './ligne-tableau-principal';
import { AgGridAngular } from 'ag-grid-angular'; // Angular Data Grid Component
import type { ColDef } from 'ag-grid-community';
import {LinkCellAgGrid} from './link-cell-aggrid'; // Column Definition Type Interface


@Component({
  selector: 'app-tableau-principal',
  imports: [
    RouterLink,
    KeyValuePipe,
    ReactiveFormsModule,
    DatePipe,
    AgGridAngular
  ],
  templateUrl: './tableau-principal.html',
  styleUrl: './tableau-principal.scss'
})
export class TableauPrincipal {

  listeProjet: Projet[] = [];
  tableau: LigneTableauPrincipal[] = [];
  chargement = false;
  groupeProjet: GroupeProjet = new GroupeProjet();
  formGrouId = new FormGroup({
    groupeId: new FormControl("")
  });
  groupeIdSelected = signal('');
  pagination = true;
  paginationPageSize = 15;
  paginationPageSizeSelector = [15, 20, 30];

  // Column Definitions: Defines the columns to be displayed.
  colDefs: ColDef[] = [
    { field: "nom",
      cellRenderer: LinkCellAgGrid,
      cellRendererParams: {
        groupId: this.groupeIdSelected()
      }
    },
    { field: "version" },
    { field: "parent" },
    { field: "description" }
  ];

  // Row Data: The data to be displayed.
  rowData = [
    { make: "Tesla", model: "Model Y", price: 64950, electric: true },
    { make: "Ford", model: "F-Series", price: 33850, electric: false },
    { make: "Toyota", model: "Corolla", price: 29600, electric: false },
  ];

  // Column Definitions: Defines the columns to be displayed.
  colDefs2: ColDef[] = [
    { field: "make" },
    { field: "model" },
    { field: "price" },
    { field: "electric" }
  ];

  constructor(private projetService: ProjetService) {

    projetService.getGroupeProjet().subscribe({
      next: (data) => {
        console.log(data);
        this.groupeProjet = data;
      },
      error: (error) => {
        console.error(error);
      }
    })

  }


  public recharger(e: MouseEvent) {
    e.preventDefault();
    this.chargement = true;

    const {groupeId} = this.formGrouId.value;
    console.log('form : ', groupeId);

    if (groupeId) {
      this.groupeIdSelected.set(groupeId);
      this.projetService.getProjets(groupeId).subscribe({
          next: (data) => {
            console.log(data);
            this.listeProjet = data;
            this.calculTableau(data, groupeId);
          },
          error: (error) => {
            console.error(error);
          },
          complete: () => {
            this.chargement = false;
          }
        }
      );
    }
  }

  private calculTableau(data: Projet[], groupeId: string) {
    if (data && data.length > 0) {
      let listeLignes: LigneTableauPrincipal[]=[];
      for (let i = 0; i < data.length; i++) {
        let projet = data[i];
        let ligne: LigneTableauPrincipal = new LigneTableauPrincipal();
        ligne.nom = projet.nom;
        ligne.groupeId=groupeId;
        if (projet.artifact) {
          ligne.version = projet.artifact.version;
        }
        if (projet.parent) {
          if (projet.parent.artefactId == 'spring-boot-starter-parent' && projet.parent.groupId == 'org.springframework.boot') {
            ligne.parent = 'Spring boot ' + projet.parent.version;
          } else {
            ligne.parent = projet.parent.groupId + ':' + projet.parent.artefactId;
          }
        }
        ligne.description = projet.description;
        if (projet.infoGit) {
          ligne.infoGitDate = projet.infoGit.date;
          ligne.infoGitBranche = projet.infoGit.branche;
          ligne.infoGitMessage = projet.infoGit.message;
        }
        listeLignes.push(ligne);
      }
      this.tableau = listeLignes;
      console.log('tableau 2 :',this.tableau);
    }
  }
}
