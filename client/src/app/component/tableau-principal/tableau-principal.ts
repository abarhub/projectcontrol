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
import {ModuleDetailsCellAgGrid} from './cell/module-details-cell-aggrid';
import {RechercheService} from '../../service/recherche.service';
import {LigneResultat} from '../../entity/LigneResultat';
import {catchError, concatMap, delayWhen, expand, map, Observable, of, timer} from 'rxjs';
import {ReponseRechercheInitial} from '../../entity/reponse-recherche-initial';
import {ReponseRechercheSuivante} from '../../entity/reponse-recherche-suivante'; // Column Definition Type Interface


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

  AFFICHAGE_TABLEAU = 'tableau';
  AFFICHAGE_RECHERCHE = 'recherche';

  TYPE_RECHERCHE_TEXTE = 'texte';
  TYPE_RECHERCHE_REGEXP = 'regexp';
  TYPE_RECHERCHE_CHEMIN = 'chemin';
  TYPE_RECHERCHE_XPATH = 'xpath';

  listeProjet: Projet[] = [];
  tableau: LigneTableauPrincipal[] = [];
  chargement = false;
  groupeProjet: GroupeProjet = new GroupeProjet();
  formGrouId = new FormGroup({
    groupeId: new FormControl("")
  });
  formGrouId2 = new FormGroup({
    affichage: new FormControl(this.AFFICHAGE_TABLEAU)
  });
  formGrouId3 = new FormGroup({
    recherche: new FormControl(''),
    typeRecherche: new FormControl(this.TYPE_RECHERCHE_TEXTE)
  });
  groupeIdSelected = signal('');
  pagination = true;
  paginationPageSize = 15;
  paginationPageSizeSelector = [15, 50, 100, 300];
  critereRecherche: string = '';
  resultatRecherche: Map<number, LigneResultat> = new Map<number, LigneResultat>();
  chargementRecherche: boolean = false;

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

  constructor(private projetService: ProjetService, private rechercheService: RechercheService) {

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
        ligne.id = projet.id;
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

  rechercher($event: MouseEvent) {
    $event.preventDefault();
    const {groupeId} = this.formGrouId.value;
    let texte = this.formGrouId3.value.recherche;
    let typeRecherche = this.formGrouId3.value.typeRecherche;
    if (groupeId && texte && typeRecherche) {
      this.chargementRecherche = true;
      this.resultatRecherche = new Map<number, LigneResultat>();

      this.pollApiDataWithId(groupeId, texte, typeRecherche, 1000).subscribe({
        next: (data) => {
          console.log('resultat', data);
          this.ajouteTableau(data);
        },
        error: (error) => {
          console.error(error);
        },
        complete: () => {
          this.chargementRecherche = false;
        }
      })
    }
  }

  private ajouteTableau(data: LigneResultat[]) {
    let tableau: Map<number, LigneResultat> = new Map<number, LigneResultat>();
    let i0 = 0;
    for (let [key, value] of this.resultatRecherche) {
      // console.log(key, value);
      tableau.set(key, value);
      if (key > i0) {
        i0 = key;
      }
    }
    let no = 0;
    for (let i = 0; i < data.length; i++) {
      let resultat = data[i];
      if (resultat.lignes) {
        for (let j = 0; j < resultat.lignes.length; j++) {
          no++;
          let item = resultat.lignes[j];
          let ligne: LigneResultat = new LigneResultat();
          ligne.ligne = item;
          ligne.noLigne = no;
          ligne.fichier = resultat.fichier;
          tableau.set(no, ligne);
        }
      } else if (resultat.ligne) {
        no++;
        tableau.set(no, resultat);
      }
      //tableau.set(i0 + i + 1, resultat);
    }
    this.resultatRecherche = tableau;
  }

  /**
   * Appelle une première API, puis fait des appels en boucle à une seconde API
   * en utilisant un ID retourné par le premier appel.
   * @param firstUrl L'URL du premier appel API.
   * @param pollUrlTemplate L'URL pour le polling, avec un placeholder pour l'ID.
   * @returns Un Observable qui émet les données de l'API de polling.
   */
  private pollApiDataWithId(
    //firstUrl: string,
    //pollUrlTemplate: string,
    groupeId: string,
    texte: string,
    typeRecherche: string,
    temporisation: number
  ): Observable<LigneResultat[]> {
    // 1. Premier appel API
    return this.rechercheService.getRecherche(groupeId, texte, typeRecherche).pipe(
      // 2. Traitement de la réponse pour extraire l'ID.
      // L'opérateur map est utilisé pour transformer le JSON de la réponse.
      // map((response) => response.json()),
      concatMap((firstResponse: ReponseRechercheInitial) => {
        // 3. Extrait l'ID de la réponse pour construire la nouvelle URL.
        const jobId = firstResponse.id;
        // const pollUrl = pollUrlTemplate.replace('{id}', jobId);

        // 4. Démarre le processus de polling avec l'URL mise à jour.
        return this.rechercheService.getRechercheSuivant(jobId).pipe(
          // temporisation de 2 secondes avant d'émettre le résultat suivant
          delayWhen(() => timer(temporisation)),
          expand((response: ReponseRechercheSuivante) => {
            // L'observable continue tant que `isDone` n'est pas vrai.
            if (response.terminer) {
              return of();
            }
            // On continue le polling.
            return this.rechercheService.getRechercheSuivant(jobId);
          }),
          // On s'assure de bien extraire le JSON à chaque tour de boucle.
          map((response) => response.listeLignes),
          // Pour gérer les erreurs à n'importe quelle étape.
          catchError((error) => {
            console.error('API call failed:', error);
            throw error;
          })
        );
      })
    );
  }

  // rechercher0($event: MouseEvent) {
  //   $event.preventDefault();
  //   const {groupeId} = this.formGrouId.value;
  //   let texte = this.formGrouId3.value.recherche;
  //   let typeRecherche = this.formGrouId3.value.typeRecherche;
  //
  //   if (groupeId && texte && typeRecherche) {
  //     this.chargementRecherche = true;
  //     this.rechercheService.getRecherche(groupeId, texte, typeRecherche).subscribe({
  //       next: (data) => {
  //         console.log('resultat', data);
  //         let tableau: Map<number, LigneResultat> = new Map<number, LigneResultat>();
  //         for (let i = 0; i < data.length; i++) {
  //           let resultat = data[i];
  //           tableau.set(resultat.noLigne, resultat);
  //         }
  //         this.resultatRecherche = tableau;
  //         this.chargementRecherche = false;
  //       },
  //       error: (error) => {
  //         console.error(error);
  //         this.chargementRecherche = false;
  //       }
  //     })
  //   }
  //
  // }
}
