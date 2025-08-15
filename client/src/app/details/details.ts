import {Component, effect, inject, signal} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ProjetService} from '../service/projet.service';
import {JsonPipe, KeyValuePipe} from '@angular/common';

@Component({
  selector: 'app-details',
  imports: [
    JsonPipe,
    KeyValuePipe
  ],
  templateUrl: './details.html',
  styleUrl: './details.scss'
})
export class Details {

  private activatedRoute = inject(ActivatedRoute);
  nomProjet = signal('');
  private projetService = inject(ProjetService);
  projet = signal<Projet | null>(null);

  constructor() {
    //Access route parameters
    this.activatedRoute.params.subscribe((params) => {
      this.nomProjet.set(params['nomProjet']);
    });

    effect(() => {
      let nomProjet=this.nomProjet()
      console.log(nomProjet);
      this.projetService.getProjet(nomProjet)
        .subscribe({
          next: (data) => {
            console.log('projet'+nomProjet,data);
            this.projet.set(data);
          },
          error: (error) => {
            console.error(error);
          }
        });
    });

  }
}
