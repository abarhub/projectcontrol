import {Component, signal} from '@angular/core';
import {ProjetService} from '../../service/projet.service';
import {Projet} from '../../entity/projet';
import {GroupeProjet} from '../../entity/groupe-projet';
import {KeyValuePipe} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {LigneTableauPrincipal} from './ligne-tableau-principal';
import {AgGridAngular} from 'ag-grid-angular'; // Angular Data Grid Component
import type {ColDef} from 'ag-grid-community';
import {LinkCellAgGrid} from './cell/link-cell-aggrid';
import {GitCellAgGrid} from './cell/git-cell-aggrid';
import {ModuleCellAgGrid} from './cell/module-cell-aggrid';
import {ModuleDetailsCellAgGrid} from './cell/module-details-cell-aggrid'; // Column Definition Type Interface


@Component({
  selector: 'app-tableau-principal',
  imports: [
    KeyValuePipe,
    ReactiveFormsModule,
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
  paginationPageSizeSelector = [15, 50, 100, 300];

  // Column Definitions: Defines the columns to be displayed.
  colDefs: ColDef[] = [
    {
      field: "nom",
      cellRenderer: LinkCellAgGrid,
      cellRendererParams: {
        groupId: this.groupeIdSelected()
      }
    },
    {field: "version"},
    {field: "parent"},
    {field: "description"},
    {
      field: "git",
      cellRenderer: GitCellAgGrid,
      autoHeight: true
    },
    {
      field: "dateModification",
      headerName: "date derniere modification",
      cellDataType: 'dateTime'
    },
    {
      field: "modules",
      cellRenderer: ModuleCellAgGrid
    },
    {
      field: "detailModules",
      cellRenderer: ModuleDetailsCellAgGrid,
      autoHeight: true
    },
    {field: "repertoire"},
  ];

  gridOptions = {
    columnDefs: this.colDefs,
    rowData: this.listeProjet,
    pagination: this.pagination,
    paginationPageSize: this.paginationPageSize,
    paginationPageSizeSelector: this.paginationPageSizeSelector,
    // rowHeight: 150
  };

  // // Row Data: The data to be displayed.
  // rowData = [
  //   {make: "Tesla", model: "Model Y", price: 64950, electric: true},
  //   {make: "Ford", model: "F-Series", price: 33850, electric: false},
  //   {make: "Toyota", model: "Corolla", price: 29600, electric: false},
  // ];
  //
  // // Column Definitions: Defines the columns to be displayed.
  // colDefs2: ColDef[] = [
  //   {field: "make"},
  //   {field: "model"},
  //   {field: "price"},
  //   {field: "electric"}
  // ];

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
      let listeLignes: LigneTableauPrincipal[] = [];
      for (let i = 0; i < data.length; i++) {
        let projet = data[i];
        let ligne: LigneTableauPrincipal = new LigneTableauPrincipal();
        ligne.id=projet.id;
        ligne.nom = projet.nom;
        ligne.groupeId = groupeId;
        ligne.repertoire = projet.repertoire;
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
          ligne.infoGitIdCommit = projet.infoGit.idCommit;
          ligne.infoGitBranche = projet.infoGit.branche;
          ligne.infoGitMessage = projet.infoGit.message;
        }
        if (projet.dateModification) {
          ligne.dateModification = new Date(projet.dateModification);
        }
        ligne.modules = projet.modules;
        ligne.detailModules = projet.detailModules;
        listeLignes.push(ligne);
      }
      this.tableau = listeLignes;
      console.log('tableau 2 :', this.tableau);
    }
  }
}
