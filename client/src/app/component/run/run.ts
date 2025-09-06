import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Modal} from 'bootstrap';
import {ToasterService} from '../../service/toaster.service';
import {RunService} from '../../service/run.service';
import {catchError, concatMap, delayWhen, expand, map, Observable, of, timer} from 'rxjs';
import {ReponseRunInitial} from '../../entity/reponse-run-initial';
import {ReponseRunSuivante} from '../../entity/reponse-run-suivante';

@Component({
  selector: 'app-run',
  imports: [
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './run.html',
  styleUrl: './run.scss'
})
export class Run {

  @ViewChild('toastEl', {static: true}) majVersionEl!: ElementRef;

  private majVersionModal!: Modal;

  private groupeId: string = "";
  private nomProjet: string = "";
  private toasterService = inject(ToasterService);
  messages: string [] = [];
  private runService = inject(RunService);


  public show(groupeId: string, nomProjet: string) {
    this.groupeId = groupeId;
    this.nomProjet = nomProjet;
    this.majVersionModal = new Modal(this.majVersionEl.nativeElement);
    this.majVersionModal.show();
    this.run();
  }

  enregistrer($event: MouseEvent) {

  }

  private run() {
    if (this.groupeId && this.nomProjet) {
      this.messages = [];
      this.pollApiDataWithId(this.groupeId, this.nomProjet, 'dependance', 1000).subscribe({
        next: (data) => {
          console.log('resultat', data);
          this.toasterService.show("run OK");
          console.log(" ajout de messages", this.messages);
          if (data.length > 0) {
            let liste: string[] = data;
            this.messages = this.messages.concat(liste);
            console.log("messages total:", this.messages);
          }
        },
        error: (error) => {
          console.error(error);
        }
      })
    }
  }

  /**
   * Appelle une première API, puis fait des appels en boucle à une seconde API
   * en utilisant un ID retourné par le premier appel.
   * @param firstUrl L'URL du premier appel API.
   * @param pollUrlTemplate L'URL pour le polling, avec un placeholder pour l'ID.
   * @returns Un Observable qui émet les données de l'API de polling.
   */
  private pollApiDataWithId(
    groupeId: string,
    nomProjet: string,
    action: string,
    temporisation: number
  ): Observable<string[]> {
    // 1. Premier appel API
    return this.runService.getRun(groupeId, nomProjet, action).pipe(
      // 2. Traitement de la réponse pour extraire l'ID.
      // L'opérateur map est utilisé pour transformer le JSON de la réponse.
      // map((response) => response.json()),
      concatMap((firstResponse: ReponseRunInitial) => {
        // 3. Extrait l'ID de la réponse pour construire la nouvelle URL.
        const jobId = firstResponse.id;
        // const pollUrl = pollUrlTemplate.replace('{id}', jobId);

        // 4. Démarre le processus de polling avec l'URL mise à jour.
        return this.runService.getRunSuivant(jobId).pipe(
          // temporisation de 2 secondes avant d'émettre le résultat suivant
          delayWhen(() => timer(temporisation)),
          expand((response: ReponseRunSuivante) => {
            // L'observable continue tant que `isDone` n'est pas vrai.
            if (response.terminer) {
              return of();
            }
            // On continue le polling.
            return this.runService.getRunSuivant(jobId);
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
}
