import {Component, signal} from '@angular/core';
import {ProjetService} from '../service/projet.service';
import {RouterLink} from '@angular/router';
import {Projet} from '../entity/projet';
import {GroupeProjet} from '../entity/groupe-projet';
import {KeyValuePipe} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';


@Component({
  selector: 'app-tableau-principal',
  imports: [
    RouterLink,
    KeyValuePipe,
    ReactiveFormsModule
  ],
  templateUrl: './tableau-principal.html',
  styleUrl: './tableau-principal.scss'
})
export class TableauPrincipal {

  listeProjet: Projet[] = [];
  chargement = false;
  groupeProjet: GroupeProjet = new GroupeProjet();
  formGrouId = new FormGroup({
    groupeId: new FormControl("")
  });
  groupeIdSelected = signal('');

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
}
