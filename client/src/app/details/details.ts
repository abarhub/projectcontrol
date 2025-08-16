import {Component, effect, inject, signal} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ProjetService} from '../service/projet.service';
import {KeyValuePipe} from '@angular/common';
import {Projet} from '../entity/projet';

@Component({
  selector: 'app-details',
  imports: [
    KeyValuePipe
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

  constructor() {
    //Access route parameters
    this.activatedRoute.params.subscribe((params) => {
      this.nomProjet.set(params['nomProjet']);
      this.groupeProjet.set(params['groupeProjet']);
    });

    effect(() => {
      let nomProjet = this.nomProjet();
      let groupeProjet = this.groupeProjet();
      console.log('detail:', groupeProjet, nomProjet);
      if (groupeProjet && nomProjet) {
        this.projetService.getProjet(groupeProjet, nomProjet)
          .subscribe({
            next: (data) => {
              console.log('projet' + nomProjet, data);
              this.projet.set(data);
            },
            error: (error) => {
              console.error(error);
            }
          });
      }
    });

  }
}
