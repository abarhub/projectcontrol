import {Component, input} from '@angular/core';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {KeyValuePipe, NgClass} from '@angular/common';
import {LigneResultat} from '../../entity/LigneResultat';
import {catchError, concatMap, delayWhen, expand, map, Observable, of, timer} from 'rxjs';
import {ReponseRechercheInitial} from '../../entity/reponse-recherche-initial';
import {ReponseRechercheSuivante} from '../../entity/reponse-recherche-suivante';
import {LigneGrep} from '../../entity/ligne-grep';
import {RechercheService} from '../../service/recherche.service';

@Component({
  selector: 'app-recherche',
  imports: [
    FormsModule,
    KeyValuePipe,
    ReactiveFormsModule,
    NgClass
  ],
  templateUrl: './recherche.html',
  styleUrl: './recherche.scss'
})
export class Recherche {

  TYPE_RECHERCHE_TEXTE = 'texte';
  TYPE_RECHERCHE_REGEXP = 'regexp';
  TYPE_RECHERCHE_CHEMIN = 'chemin';
  TYPE_RECHERCHE_XPATH = 'xpath';

  formGrouId3 = new FormGroup({
    recherche: new FormControl(''),
    typeRecherche: new FormControl(this.TYPE_RECHERCHE_TEXTE)
  });


  critereRecherche: string = '';
  resultatRecherche: Map<number, LigneResultat> = new Map<number, LigneResultat>();
  chargementRecherche: boolean = false;

  groupeId = input<string | null>(null);

  constructor(private rechercheService: RechercheService) {

  }

  rechercher($event: MouseEvent) {
    $event.preventDefault();
    const groupeId = this.groupeId();
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
      } else if (resultat.lignes2) {
        let ligne: LigneResultat = new LigneResultat();
        ligne.listeLigneGrep = resultat.lignes2;
        ligne.fichier = resultat.fichier;
        this.analyse(ligne.listeLigneGrep);
        tableau.set(no, ligne);
        no++;
        // for (let j = 0; j < resultat.lignes2.length; j++) {
        //   no++;
        //   let item = resultat.lignes2[j];
        //   let ligne: LigneResultat = new LigneResultat();
        //   ligne.ligne = item.ligne;
        //   ligne.noLigne = item.noLigne;
        //   ligne.fichier = resultat.fichier;
        //   ligne.trouve = item.trouve;
        //   ligne.ligneGrep = item;
        //   tableau.set(no, ligne);
        // }
      }
      //tableau.set(i0 + i + 1, resultat);
    }
    console.log('tableau :', tableau);
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
  private analyse(listeLigneGrep: LigneGrep[]) {
    for (let i = 0; i < listeLigneGrep.length; i++) {
      let ligneGrep = listeLigneGrep[i];
      if (ligneGrep.trouve) {
        if (ligneGrep.range) {
          let liste: string[] = [];
          let debutRouge = false;
          let pos = 0;
          for (let i = 0; i < ligneGrep.range.length; i++) {
            let range = ligneGrep.range[i];
            if (range.debut == pos) {
              let s = ligneGrep.ligne.substring(pos, range.fin + 1);
              liste.push(s);
              pos = range.fin + 1;
              if (i == 0) {
                debutRouge = true;
              }
            } else {
              let s = ligneGrep.ligne.substring(pos, range.debut - 1 + 1);
              liste.push(s);
              s = ligneGrep.ligne.substring(range.debut, range.fin + 1);
              liste.push(s);
              pos = range.fin + 1;
              if (i == 0) {
                debutRouge = false;
              }
            }
          }
          if (pos < ligneGrep.ligne.length) {
            let s = ligneGrep.ligne.substring(pos, ligneGrep.ligne.length);
            liste.push(s);
          }
          if (liste) {
            ligneGrep.lignesDecoupees = liste;
            ligneGrep.debutTrouve = debutRouge;
          }
        }
      }
    }

  }
}
