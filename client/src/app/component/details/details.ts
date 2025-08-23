import {AfterViewInit, Component, effect, ElementRef, inject, signal, ViewChild} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {ProjetService} from '../../service/projet.service';
import {Projet} from '../../entity/projet';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {DetailsProjet} from '../details-projet/details-projet';
import { Toast } from 'bootstrap';
import {Toaster} from '../toaster/toaster';
import {ToasterService} from '../../service/toaster.service';

@Component({
  selector: 'app-details',
  imports: [
    ReactiveFormsModule,
    DetailsProjet,
    RouterLink
  ],
  templateUrl: './details.html',
  styleUrl: './details.scss'
})
export class Details {

  private activatedRoute = inject(ActivatedRoute);
  nomProjet = signal('');
  groupeProjet = signal('');
  private projetService = inject(ProjetService);
  projet = signal<Projet | null>(null);
  private toasterService = inject(ToasterService);

  choixForm = new FormGroup({
    choix: new FormControl('1')
  });

  constructor() {
    //Access route parameters
    this.activatedRoute.params.subscribe((params) => {
      this.nomProjet.set(params['nomProjet']);
      this.groupeProjet.set(params['groupeProjet']);
    });

    effect(() => {
      this.rechargerProjet();
    });

    this.choixForm.get('choix')?.valueChanges.subscribe(value => {
      console.log('Choix sélectionné :', value);
      // tu peux réagir ici : appeler une API, changer un affichage, etc.
    });

  }

  private complete(data: Projet) {
    if (data.dependencies) {
      for (let i = 0; i < data.dependencies.length; i++) {
        const element = data.dependencies[i];
        element.id = i + 1;
      }
    }
    if (data.projetEnfants) {
      for (let i = 0; i < data.projetEnfants.length; i++) {
        let projetEnfant = data.projetEnfants[i];
        if (projetEnfant.dependencies) {
          this.complete(projetEnfant);
        }
      }
    }
  }

  majVersion($event: MouseEvent) {
    $event.preventDefault();
  }

  listConfig($event: MouseEvent) {
    $event.preventDefault();
  }

  recharger($event: MouseEvent) {
    $event?.preventDefault();
    this.rechargerProjet();
  }

  private rechargerProjet() {
    let nomProjet = this.nomProjet();
    let groupeProjet = this.groupeProjet();
    console.log('detail:', groupeProjet, nomProjet);
    if (groupeProjet && nomProjet) {
      this.projetService.getProjet(groupeProjet, nomProjet)
        .subscribe({
          next: (data) => {
            console.log('projet' + nomProjet, data);
            this.complete(data);
            this.projet.set(data);
            this.alerte("OK");
          },
          error: (error) => {
            console.error(error);
            this.alerte("Erreur pour charger le projet");
          }
        });
    }
  }

  private alerte(message: string) {
    this.toasterService.show("message : " + message);
  }
}
