import {Component} from '@angular/core';
import {ProjetService} from '../service/projet.service';
import {RouterLink} from '@angular/router';


@Component({
  selector: 'app-tableau-principal',
  imports: [
    RouterLink
  ],
  templateUrl: './tableau-principal.html',
  styleUrl: './tableau-principal.scss'
})
export class TableauPrincipal {

  listeProjet: Projet[] = [];
  chargement = false;

  constructor(private projetService: ProjetService) {
  }


  public recharger() {
    this.chargement=true;
    this.projetService.getProjets().subscribe({
        next: (data) => {
          console.log(data);
          this.listeProjet = data;
        },
        error: (error) => {
          console.error(error);
        },
      complete: () => {
          this.chargement=false;
      }
      }
    )
  }
}
